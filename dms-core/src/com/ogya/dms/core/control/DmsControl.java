package com.ogya.dms.core.control;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.hibernate.HibernateException;

import com.google.gson.JsonSyntaxException;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.core.common.AudioCenter;
import com.ogya.dms.core.common.AudioCenter.AudioCenterListener;
import com.ogya.dms.core.common.CommonConstants;
import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.common.SoundPlayer;
import com.ogya.dms.core.database.DbManager;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.Member;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.dmsclient.DmsClient;
import com.ogya.dms.core.dmsclient.intf.DmsClientListener;
import com.ogya.dms.core.factory.DmsFactory;
import com.ogya.dms.core.intf.DmsHandle;
import com.ogya.dms.core.intf.exceptions.DbException;
import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.ContactSelectionHandle;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.GroupSelectionHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;
import com.ogya.dms.core.intf.handles.impl.ActiveGroupsHandleImpl;
import com.ogya.dms.core.intf.handles.impl.ContactHandleImpl;
import com.ogya.dms.core.intf.handles.impl.FileHandleImpl;
import com.ogya.dms.core.intf.handles.impl.GroupHandleImpl;
import com.ogya.dms.core.intf.handles.impl.ListHandleImpl;
import com.ogya.dms.core.intf.handles.impl.MessageHandleImpl;
import com.ogya.dms.core.intf.handles.impl.ObjectHandleImpl;
import com.ogya.dms.core.intf.handles.impl.OnlineContactsHandleImpl;
import com.ogya.dms.core.intf.listeners.DmsGuiListener;
import com.ogya.dms.core.intf.listeners.DmsListener;
import com.ogya.dms.core.model.Model;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.ContactMap;
import com.ogya.dms.core.structures.FilePojo;
import com.ogya.dms.core.structures.GroupMessageStatus;
import com.ogya.dms.core.structures.GroupUpdate;
import com.ogya.dms.core.structures.MessageDirection;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.MessageSubType;
import com.ogya.dms.core.structures.MessageType;
import com.ogya.dms.core.structures.ReceiverType;
import com.ogya.dms.core.structures.WaitStatus;
import com.ogya.dms.core.view.DmsPanel;
import com.ogya.dms.core.view.ReportsDialog;
import com.ogya.dms.core.view.ReportsPane.ReportsListener;
import com.ogya.dms.core.view.intf.AppListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class DmsControl implements DmsClientListener, AppListener, ReportsListener, AudioCenterListener, DmsHandle {

	private static final int MIN_MESSAGES_PER_PAGE = 50;

	private static final Object FILE_SYNC = new Object();

	private final DbManager dbManager;

	private final Model model;

	private final DmsPanel dmsPanel;
	private final JFXPanel dmsPanelSwing;

	private final ReportsDialog reportsDialog;

	private final GroupSelectionHandle activeGroupsHandle;
	private final ContactSelectionHandle onlineContactsHandle;

	private final DmsClient dmsClient;

	private final AudioCenter audioCenter = new AudioCenter();
	private final SoundPlayer soundPlayer = new SoundPlayer();

	private final List<DmsListener> dmsListeners = Collections.synchronizedList(new ArrayList<DmsListener>());
	private final List<DmsGuiListener> dmsGuiListeners = Collections.synchronizedList(new ArrayList<DmsGuiListener>());

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();
	private final ExecutorService listenerTaskQueue = DmsFactory.newSingleThreadExecutorService();

	public DmsControl(String username, String password) throws DbException {

		dbManager = new DbManager(username, password);

		Contact identity = dbManager.getIdentity();

		model = new Model(identity);

		dmsPanelSwing = new JFXPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				super.updateUI();
				Platform.runLater(() -> {
					dmsPanel.updateUI();
					reportsDialog.updateUI();
				});
			}

		};

		dmsPanelSwing.addAncestorListener(new AncestorListener() {

			@Override
			public void ancestorRemoved(AncestorEvent arg0) {

			}

			@Override
			public void ancestorMoved(AncestorEvent arg0) {

			}

			@Override
			public void ancestorAdded(AncestorEvent arg0) {

				dmsPanelSwing.updateUI();

			}

		});

		dmsPanel = new DmsPanel();

		dmsPanel.addListener(this);

		//

		activeGroupsHandle = new ActiveGroupsHandleImpl(dmsPanel.getActiveGroupsPanel());
		onlineContactsHandle = new OnlineContactsHandleImpl(dmsPanel.getOnlineContactsPanel());

		//

		reportsDialog = new ReportsDialog(CommonMethods.getReportTemplates());

		reportsDialog.addReportsListener(this);

		//

		audioCenter.addAudioCenterListener(this);

		//

		initDatabase();
		initModel();
		initGUI();

		dmsClient = new DmsClient(identity.getUuid(), CommonConstants.SERVER_IP, CommonConstants.SERVER_PORT, this);

	}

	private void initDatabase() {

		dbManager.fetchAllContacts().forEach(contact -> {
			contact.setStatus(Availability.OFFLINE);
			dbManager.addUpdateContact(contact);
		});

		dbManager.fetchAllGroups().forEach(group -> {
			if (!Objects.equals(model.getLocalUuid(), group.getOwner().getUuid())) {
				group.setStatus(Availability.OFFLINE);
				dbManager.addUpdateGroup(group);
			}
		});

	}

	private void initModel() {

		dbManager.fetchAllContacts().forEach(contact -> model.addContact(contact));
		dbManager.fetchAllGroups().forEach(group -> model.addGroup(group));

	}

	private void initGUI() {

		Platform.runLater(() -> {

			Scene dmsScene = new Scene(dmsPanel);
			dmsScene.getStylesheets().add("/resources/css/style.css");
			dmsPanelSwing.setScene(dmsScene);
			dmsPanel.setIdentity(model.getIdentity());

		});

		List<Message> firstMessages = new ArrayList<Message>();

		model.getContacts().forEach((id, contact) -> {

			Platform.runLater(() -> dmsPanel.updateContact(contact));

			List<Message> dbMessages = dbManager.getAllPrivateMessagesSinceFirstUnreadMessage(id);

			firstMessages.addAll(dbMessages);

			if (dbMessages.size() == 0) {

				firstMessages.addAll(dbManager.getLastPrivateMessages(id, MIN_MESSAGES_PER_PAGE));

			} else if (dbMessages.size() < MIN_MESSAGES_PER_PAGE) {

				firstMessages.addAll(dbManager.getLastPrivateMessagesBeforeId(id, dbMessages.get(0).getId(),
						MIN_MESSAGES_PER_PAGE - dbMessages.size()));

			}

		});

		model.getGroups().forEach((id, group) -> {

			Platform.runLater(() -> dmsPanel.updateGroup(group));

			List<Message> dbMessages = dbManager.getAllGroupMessagesSinceFirstUnreadMessage(id);

			firstMessages.addAll(dbMessages);

			if (dbMessages.size() == 0) {

				firstMessages.addAll(dbManager.getLastGroupMessages(id, MIN_MESSAGES_PER_PAGE));

			} else if (dbMessages.size() < MIN_MESSAGES_PER_PAGE) {

				firstMessages.addAll(dbManager.getLastGroupMessagesBeforeId(id, dbMessages.get(0).getId(),
						MIN_MESSAGES_PER_PAGE - dbMessages.size()));

			}

		});

		Collections.sort(firstMessages, new Comparator<Message>() {

			@Override
			public int compare(Message arg0, Message arg1) {

				return Long.compare(arg0.getId(), arg1.getId());

			}

		});

		firstMessages.forEach(message -> addMessageToPane(message));

	}

	private void addMessageToPane(final Message message) {

		Platform.runLater(() -> dmsPanel.addMessage(message));

	}

	private void sendBeacon(String name, String comment, Availability status, Double lattitude, Double longitude,
			String secretId) {

		if (!model.isServerConnected())
			return;

		Beacon beacon = new Beacon(model.getLocalUuid(), name, comment, status == null ? null : status.index(),
				lattitude, longitude, secretId);

		dmsClient.sendBeacon(beacon.toJson());

	}

	private void contactDisconnected(final Contact contact) {

		taskQueue.execute(() -> {

			final Long id = contact.getId();

			contact.setStatus(Availability.OFFLINE);

			try {

				final Contact newContact = dbManager.addUpdateContact(contact);

				model.addContact(newContact);

				Platform.runLater(() -> dmsPanel.updateContact(newContact));

				listenerTaskQueue.execute(() -> dmsListeners
						.forEach(listener -> listener.contactUpdated(new ContactHandleImpl(newContact))));

			} catch (HibernateException e) {

				e.printStackTrace();

			}

			try {

				dbManager.getAllActiveGroupsOfContact(id).forEach(group -> {

					group.setStatus(Availability.OFFLINE);

					try {

						final Dgroup newGroup = dbManager.addUpdateGroup(group);

						model.addGroup(newGroup);

						Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

						listenerTaskQueue.execute(() -> dmsListeners
								.forEach(listener -> listener.groupUpdated(new GroupHandleImpl(newGroup))));

					} catch (HibernateException e) {

						e.printStackTrace();

					}

				});

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	private void clearMessageProgresses() {

		taskQueue.execute(() -> {

			model.getPrivateMessageProgresses().forEach((id, messageIdProgress) -> messageIdProgress.forEach((messageId,
					progress) -> Platform.runLater(() -> dmsPanel.updatePrivateMessageProgress(id, messageId, -1))));

			model.clearPrivateMessageProgresses();

			Long detailedGroupMessageId = model.getDetailedGroupMessageId();

			if (model.getGroupMessageProgresses(detailedGroupMessageId) != null)
				model.getGroupMessageProgresses(detailedGroupMessageId).forEach(
						(uuid, progress) -> Platform.runLater(() -> dmsPanel.updateDetailedMessageProgress(uuid, -1)));

			model.clearGroupMessageProgresses();

		});

	}

	private Message createOutgoingMessage(String messageTxt, Contact contact, Dgroup group,
			Set<StatusReport> statusReports, ReceiverType receiverType, MessageType messageType,
			MessageSubType messageSubType, Message refMessage, Integer apiFlag) throws Exception {

		Message outgoingMessage = new Message(contact, group, receiverType, messageType, messageTxt);

		outgoingMessage.setOwner(model.getIdentity());
		outgoingMessage.setMessageDirection(MessageDirection.OUT);

		outgoingMessage.setMessageSubType(messageSubType);

		outgoingMessage.setMessageStatus(MessageStatus.FRESH);

		if (statusReports != null)
			statusReports.forEach(statusReport -> outgoingMessage.addStatusReport(statusReport));

		outgoingMessage.setWaitStatus(WaitStatus.WAITING);

		outgoingMessage.setRefMessage(refMessage);

		outgoingMessage.setApiFlag(apiFlag);

		Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

		return newMessage;

	}

	private Set<StatusReport> createStatusReports(Dgroup group) {

		if (group == null)
			return null;

		Set<StatusReport> statusReports = new HashSet<StatusReport>();

		if (!Objects.equals(group.getOwner().getUuid(), model.getLocalUuid()))
			statusReports.add(new StatusReport(group.getOwner().getId(), MessageStatus.FRESH));

		group.getMembers().forEach(contact -> {

			if (Objects.equals(contact.getUuid(), model.getLocalUuid()))
				return;

			statusReports.add(new StatusReport(contact.getId(), MessageStatus.FRESH));

		});

		return statusReports;

	}

	private Set<StatusReport> createStatusReports(Set<Contact> contacts) {

		if (contacts == null)
			return null;

		Set<StatusReport> statusReports = new HashSet<StatusReport>();

		contacts.forEach(contact -> statusReports.add(new StatusReport(contact.getId(), MessageStatus.FRESH)));

		return statusReports;

	}

	private Dgroup createUpdateGroup(Dgroup group, String groupName, boolean isActive, Set<Contact> contactsToBeAdded,
			Set<Contact> contactsToBeRemoved) throws Exception {

		Contact owner = group.getOwner();

		if (groupName != null)
			group.setName(groupName);

		group.setActive(isActive);

		if (!isActive)
			group.getMembers().clear();

		if (isActive && contactsToBeAdded != null) {

			group.getMembers().addAll(contactsToBeAdded);

		}

		if (isActive && contactsToBeRemoved != null) {

			group.getMembers().removeAll(contactsToBeRemoved);

		}

		if (!isActive)
			group.setStatus(Availability.OFFLINE);
		else if (Objects.equals(group.getOwner().getUuid(), model.getLocalUuid()))
			group.setStatus(Availability.AVAILABLE);
		else
			group.setStatus(
					!group.getActive() || Objects.equals(owner.getStatus(), Availability.OFFLINE) ? Availability.OFFLINE
							: Availability.LIMITED);

		List<String> contactNames = new ArrayList<String>();
		group.getMembers().forEach(contact -> contactNames.add(contact.getName()));
		Collections.sort(contactNames, model.getCaseInsensitiveStringSorter());
		if (!Objects.equals(owner.getUuid(), model.getLocalUuid()))
			contactNames.add(0, owner.getName());
		group.setComment(String.join(",", contactNames));

		Dgroup newGroup = dbManager.addUpdateGroup(group);

		listenerTaskQueue
				.execute(() -> dmsListeners.forEach(listener -> listener.groupUpdated(new GroupHandleImpl(newGroup))));

		return newGroup;

	}

	private String getGroupUpdate(String groupName, boolean active, Set<Contact> contactsToBeAdded,
			Set<Contact> contactsToBeRemoved) {

		GroupUpdate groupUpdate = new GroupUpdate();

		groupUpdate.name = groupName;
		groupUpdate.active = active ? null : false;

		if (!(contactsToBeAdded == null || contactsToBeAdded.isEmpty())) {
			groupUpdate.add = new ArrayList<ContactMap>();
			contactsToBeAdded.forEach(contact -> groupUpdate.add
					.add(new ContactMap(contact.getId(), contact.getUuid(), contact.getName())));
		}

		if (!(contactsToBeRemoved == null || contactsToBeRemoved.isEmpty())) {
			groupUpdate.remove = new ArrayList<Long>();
			contactsToBeRemoved.forEach(contact -> groupUpdate.remove.add(contact.getId()));
		}

		return groupUpdate.toJson();

	}

	private void sendPrivateMessage(Message message) {

		String receiverUuid = message.getContact().getUuid();

		if (model.isContactOnline(receiverUuid))
			dmsSendMessage(message, receiverUuid);

	}

	private void sendGroupMessage(Message message) {

		Dgroup group = message.getDgroup();

		if (group == null)
			return;

		final List<String> onlineUuids = new ArrayList<String>();

		if (Objects.equals(group.getOwner().getUuid(), model.getLocalUuid())) {
			// It's my group, so I have to send this message to all the members except the
			// original sender.

			for (Contact contact : group.getMembers()) {

				String receiverUuid = contact.getUuid();

				// Skip the original sender
				if (Objects.equals(message.getContact().getUuid(), receiverUuid))
					continue;

				if (model.isContactOnline(receiverUuid))
					onlineUuids.add(receiverUuid);

			}

			if (onlineUuids.size() > 0) {

				dmsSendMessage(message, onlineUuids);

			}

		} else {
			// It's not my group, so I will send this message to the group owner only.

			String receiverUuid = group.getOwner().getUuid();

			if (model.isContactOnline(receiverUuid)) {

				dmsSendMessage(message, receiverUuid);

			}

		}

	}

	private void sendGroupMessage(Message message, String receiverUuid) {

		if (model.isContactOnline(receiverUuid)) {

			dmsSendMessage(message, receiverUuid);

		}

	}

	private void sendGroupMessage(Message message, Set<Contact> receivers) {

		final List<String> onlineUuids = new ArrayList<String>();

		for (Contact receiver : receivers) {

			String receiverUuid = receiver.getUuid();

			if (model.isContactOnline(receiverUuid))
				onlineUuids.add(receiverUuid);

		}

		if (onlineUuids.size() > 0) {

			dmsSendMessage(message, onlineUuids);

		}

	}

	private void dmsSendMessage(Message message, String receiverUuid) {

		dmsSendMessage(message, updatedMessage -> dmsClient.sendMessage(updatedMessage, receiverUuid, message.getId()));

	}

	private void dmsSendMessage(Message message, Iterable<String> receiverUuids) {

		dmsSendMessage(message,
				updatedMessage -> dmsClient.sendMessage(updatedMessage, receiverUuids, message.getId()));

	}

	private void dmsSendMessage(Message message, Consumer<Message> consumer) {

		Message copyMessage = new Message(message);

		copyMessage.setMessageRefId(null);

		Message refMessage = copyMessage.getRefMessage();

		if (refMessage != null) {

			Message newRefMessage = new Message();

			newRefMessage.setId(refMessage.getId());
			newRefMessage.setMessageRefId(refMessage.getMessageRefId());

			copyMessage.setRefMessage(newRefMessage);

		}

		if (copyMessage.getDgroup() != null)
			copyMessage.setGroupRefId(copyMessage.getDgroup().getGroupRefId());

		if (Objects.equals(copyMessage.getMessageDirection(), MessageDirection.IN)
				&& Objects.equals(copyMessage.getReceiverType(), ReceiverType.GROUP_OWNER))
			copyMessage.setReceiverType(ReceiverType.GROUP_MEMBER);

		if (Objects.equals(copyMessage.getMessageDirection(), MessageDirection.IN)
				&& Objects.equals(copyMessage.getReceiverType(), ReceiverType.GROUP_MEMBER))
			copyMessage.setContactRefId(copyMessage.getContact().getId());

		switch (copyMessage.getMessageType()) {

		case FILE:
		case AUDIO:

			try {

				copyMessage.setContent(decomposeFile(Paths.get(copyMessage.getContent())).toJson());

			} catch (Exception e) {

				copyMessage.setContent("");

				e.printStackTrace();

			}

			break;

		default:

			break;

		}

		consumer.accept(copyMessage);

	}

	private void privateMessageReceived(Message message) throws Exception {

		switch (message.getMessageType()) {

		case TEXT:
		case FILE:
		case AUDIO:

			addMessageToPane(message);

			if (!model.isMessagePaneOpen(message.getContact().getId()))
				soundPlayer.playDuoTone();

			break;

		default:

			break;

		}

		listenerTaskQueue.execute(() -> {

			switch (message.getMessageType()) {

			case TEXT:

				dmsGuiListeners.forEach(guiListener -> guiListener.guiMessageReceived(message.getContent(),
						message.getContact().getId(), null));

				break;

			case FILE:

				if (Objects.equals(message.getMessageSubType(), MessageSubType.FILE_REPORT)) {
					dmsGuiListeners.forEach(guiListener -> guiListener
							.guiReportReceived(Paths.get(message.getContent()), message.getContact().getId(), null));
				} else {
					dmsGuiListeners.forEach(guiListener -> guiListener.guiFileReceived(Paths.get(message.getContent()),
							message.getContact().getId(), null));
				}

				break;

			case AUDIO:

				dmsGuiListeners.forEach(guiListener -> guiListener.guiAudioReceived(Paths.get(message.getContent()),
						message.getContact().getId(), null));

				break;

			default:

				break;

			}

		});

	}

	private void groupMessageReceived(Message message) throws Exception {

		switch (message.getMessageType()) {

		case TEXT:
		case FILE:
		case AUDIO:

			addMessageToPane(message);

			if (!model.isMessagePaneOpen(-message.getDgroup().getId()))
				soundPlayer.playTriTone();

			break;

		default:

			break;

		}

		listenerTaskQueue.execute(() -> {

			switch (message.getMessageType()) {

			case TEXT:

				dmsGuiListeners.forEach(guiListener -> guiListener.guiMessageReceived(message.getContent(),
						message.getContact().getId(), message.getDgroup().getId()));

				break;

			case FILE:

				if (Objects.equals(message.getMessageSubType(), MessageSubType.FILE_REPORT)) {
					dmsGuiListeners
							.forEach(guiListener -> guiListener.guiReportReceived(Paths.get(message.getContent()),
									message.getContact().getId(), message.getDgroup().getId()));
				} else {
					dmsGuiListeners.forEach(guiListener -> guiListener.guiFileReceived(Paths.get(message.getContent()),
							message.getContact().getId(), message.getDgroup().getId()));
				}

				break;

			case AUDIO:

				dmsGuiListeners.forEach(guiListener -> guiListener.guiAudioReceived(Paths.get(message.getContent()),
						message.getContact().getId(), message.getDgroup().getId()));

				break;

			default:

				break;

			}

		});

	}

	private void updateMessageReceived(Message message) throws Exception {

		switch (message.getReceiverType()) {

		case CONTACT:

			if (Objects.equals(message.getMessageSubType(), MessageSubType.UPDATE_CANCEL_MESSAGE)) {

				Long messageId = Long.parseLong(message.getContent());

				Message dbMessage = dbManager.getMessageBySender(message.getContact().getUuid(), messageId);

				if (dbMessage != null)
					cancelMessage(dbMessage);

			}

			break;

		case GROUP_MEMBER:

			if (Objects.equals(message.getMessageSubType(), MessageSubType.UPDATE_GROUP)) {

				GroupUpdate groupUpdate = GroupUpdate.fromJson(message.getContent());

				groupUpdateReceived(message.getContact().getUuid(), message.getGroupRefId(), groupUpdate);

			}

			break;

		default:

			break;

		}

	}

	private void groupUpdateReceived(final String ownerUuid, final Long groupRefId, final GroupUpdate groupUpdate)
			throws Exception {

		Dgroup group = model.getGroup(ownerUuid, groupRefId);

		Contact owner = getContact(ownerUuid);

		if (group == null) {
			if (owner == null || groupRefId == null)
				return;
			group = new Dgroup(owner, groupRefId);
		}

		final Set<Contact> contactsToBeAdded = new HashSet<Contact>();
		final Set<Contact> contactsToBeRemoved = new HashSet<Contact>();

		if (groupUpdate.add != null) {

			groupUpdate.add.forEach(contactMap -> {

				String uuid = contactMap.uuid;

				if (Objects.equals(model.getLocalUuid(), uuid))
					return;

				Contact contact = model.getContact(uuid);
				if (contact == null) {
					contact = new Contact(uuid);
					contact.setName(contactMap.name);
					contact.setStatus(Availability.OFFLINE);
					final Contact newContact = dbManager.addUpdateContact(contact);
					model.addContact(newContact);
					Platform.runLater(() -> dmsPanel.updateContact(newContact));
					listenerTaskQueue.execute(() -> dmsListeners
							.forEach(listener -> listener.contactUpdated(new ContactHandleImpl(newContact))));
					contactsToBeAdded.add(newContact);
				} else {
					contactsToBeAdded.add(contact);
				}

				dbManager.addUpdateMember(new Member(owner, contactMap.refId, model.getContact(uuid)));

			});

		}

		if (groupUpdate.remove != null) {

			groupUpdate.remove.forEach(refId -> {

				Member member = dbManager.getMember(ownerUuid, refId);

				if (member == null)
					return;

				contactsToBeRemoved.add(member.getContact());

			});

		}

		final Dgroup newGroup = createUpdateGroup(group, groupUpdate.name,
				groupUpdate.active == null ? true : groupUpdate.active, contactsToBeAdded, contactsToBeRemoved);

		if (newGroup == null)
			return;

		model.addGroup(newGroup);

		Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

	}

	private MessageStatus computeMessageStatus(Message message) {

		if (Objects.equals(message.getMessageType(), MessageType.UPDATE))
			return MessageStatus.READ;

		switch (message.getReceiverType()) {

		case CONTACT:

			return model.isMessagePaneOpen(message.getContact().getId()) ? MessageStatus.READ : MessageStatus.RECEIVED;

		case GROUP_OWNER:
		case GROUP_MEMBER:

			return model.isMessagePaneOpen(-message.getDgroup().getId()) ? MessageStatus.READ : MessageStatus.RECEIVED;

		}

		return MessageStatus.READ;

	}

	private Path getDstFile(Path dstFolder, String fileName) throws IOException {

		if (Files.notExists(dstFolder))
			Files.createDirectories(dstFolder);

		int lastDotIndex = fileName.lastIndexOf('.');
		String fileNameBase = lastDotIndex < 0 ? fileName : fileName.substring(0, lastDotIndex);
		String extension = lastDotIndex < 0 ? "" : fileName.substring(lastDotIndex);

		int fileIndex = 1;

		while (Files.exists(dstFolder.resolve(fileName))) {

			fileName = fileNameBase + " (" + ++fileIndex + ")" + extension;

		}

		Path dstFile = dstFolder.resolve(fileName);

		return dstFile;

	}

	private Path copyFileToSendFolder(Path srcFile) throws IOException {

		Path dstFolder = Paths.get(CommonConstants.SEND_FOLDER).normalize().toAbsolutePath();

		String fileName = srcFile.getFileName().toString();

		Path dstFile = getDstFile(dstFolder, fileName);

		Files.copy(srcFile, dstFile);

		return dstFile;

	}

	private void updateMessageStatus(Message message, List<Long> contactIds, MessageStatus messageStatus)
			throws Exception {

		if (Objects.equals(message.getWaitStatus(), WaitStatus.CANCELED))
			return;

		String contactUuid = message.getContact().getUuid();

		// Send this status to the original sender too.
		if (Objects.equals(message.getMessageDirection(), MessageDirection.IN)
				&& Objects.equals(message.getReceiverType(), ReceiverType.GROUP_OWNER)
				&& model.isContactOnline(contactUuid)) {

			GroupMessageStatus groupMessageStatus = new GroupMessageStatus(messageStatus, contactIds);

			dmsClient.feedGroupMessageStatus(contactUuid, message.getMessageRefId(), groupMessageStatus);

		}

		switch (message.getReceiverType()) {

		case CONTACT:

		{

			message.setMessageStatus(messageStatus);

			message.setWaitStatus(
					Objects.equals(messageStatus, MessageStatus.READ) ? WaitStatus.DONE : WaitStatus.WAITING);

			final Message newMessage = dbManager.addUpdateMessage(message);

			if (!Objects.equals(newMessage.getMessageType(), MessageType.UPDATE))
				Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage));

			if (Objects.equals(messageStatus, MessageStatus.FRESH))
				sendPrivateMessage(newMessage);

			break;

		}

		case GROUP_OWNER:
		case GROUP_MEMBER:

		{

			// This is a group message. I am either the group owner or the message sender.
			// If I am the group owner and the message is not received remotely, I will have
			// to re-send it.

			Dgroup group = message.getDgroup();

			if (group == null)
				break;

			Map<Long, StatusReport> statusReportMap = new HashMap<Long, StatusReport>();

			message.getStatusReports()
					.forEach(statusReport -> statusReportMap.put(statusReport.getContactId(), statusReport));

			for (Long contactId : contactIds) {

				if (Objects.equals(contactId, message.getContact().getId()))
					message.setWaitStatus(
							Objects.equals(messageStatus, MessageStatus.READ) ? WaitStatus.DONE : WaitStatus.WAITING);

				if (statusReportMap.containsKey(contactId)) {

					statusReportMap.get(contactId).setMessageStatus(messageStatus);

				} else {

					message.addStatusReport(new StatusReport(contactId, messageStatus));

				}

				if (Objects.equals(message.getId(), model.getDetailedGroupMessageId()))
					Platform.runLater(() -> dmsPanel.updateDetailedMessageStatus(contactId, messageStatus));

			}

			MessageStatus overallMessageStatus = message.getOverallStatus();

			if (Objects.equals(group.getOwner().getUuid(), model.getLocalUuid()))
				message.setWaitStatus(Objects.equals(overallMessageStatus, MessageStatus.READ) ? WaitStatus.DONE
						: WaitStatus.WAITING);

			// If I am the owner, update the message status too
			if (Objects.equals(message.getMessageDirection(), MessageDirection.OUT))
				message.setMessageStatus(overallMessageStatus);

			final Message newMessage = dbManager.addUpdateMessage(message);

			if (!Objects.equals(newMessage.getMessageType(), MessageType.UPDATE))
				Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage));

			if (Objects.equals(messageStatus, MessageStatus.FRESH)) {
				// If the message is not received remotely and;
				// I am the group owner
				// or
				// I am the message owner and receiver is the group owner
				// then re-send this message (if flag set to true).

				for (Long contactId : contactIds) {

					if (Objects.equals(group.getOwner().getUuid(), model.getLocalUuid())
							|| (Objects.equals(message.getMessageDirection(), MessageDirection.OUT)
									&& Objects.equals(contactId, group.getOwner().getId())))
						sendGroupMessage(newMessage, model.getContact(contactId).getUuid());

				}

			}

			break;

		}

		}

		if (message.getApiFlag() != null) {

			Long messageId = message.getId();

			contactIds.forEach(contactId -> listenerTaskQueue.execute(() -> dmsListeners
					.forEach(listener -> listener.guiMessageStatusUpdated(messageId, messageStatus, contactId))));

		}

	}

	private Message cancelMessage(Message message) throws Exception {

		if (Objects.equals(message.getWaitStatus(), WaitStatus.CANCELED))
			return message;

		message.setWaitStatus(WaitStatus.CANCELED);

		Message newMessage = dbManager.addUpdateMessage(message);

		if (model.isServerConnected())
			dmsClient.cancelMessage(message.getId());

		if (Objects.equals(message.getReceiverType(), ReceiverType.CONTACT))
			return newMessage;

		Dgroup group = message.getDgroup();

		String ownerUuid = group.getOwner().getUuid();

		if (group == null || Objects.equals(ownerUuid, model.getLocalUuid()))
			return newMessage;

		sendPrivateMessage(createOutgoingMessage(String.valueOf(message.getId()), message.getContact(), null, null,
				ReceiverType.CONTACT, MessageType.UPDATE, MessageSubType.UPDATE_CANCEL_MESSAGE, null, null));

		return newMessage;

	}

	private Contact getContact(String uuid) {

		if (uuid == null)
			return null;

		Contact contact = model.getContact(uuid);

		if (contact != null)
			return contact;

		contact = new Contact(uuid);
		contact.setStatus(Availability.OFFLINE);

		try {

			final Contact newContact = dbManager.addUpdateContact(contact);

			model.addContact(newContact);

			Platform.runLater(() -> dmsPanel.updateContact(newContact));

			listenerTaskQueue.execute(
					() -> dmsListeners.forEach(listener -> listener.contactUpdated(new ContactHandleImpl(newContact))));

			return newContact;

		} catch (Exception e) {

		}

		return null;

	}

	private Path composeFile(FilePojo filePojo) throws IOException {

		synchronized (FILE_SYNC) {

			Path dstFolder = Paths.get(CommonConstants.RECEIVE_FOLDER).normalize().toAbsolutePath();

			Path dstFile = getDstFile(dstFolder, filePojo.fileName);

			Files.write(dstFile, Base64.getDecoder().decode(filePojo.fileContent));

			return dstFile;

		}

	}

	private FilePojo decomposeFile(Path path) throws IOException {

		synchronized (FILE_SYNC) {

			byte[] fileBytes = Files.readAllBytes(path);

			return new FilePojo(path.getFileName().toString(), Base64.getEncoder().encodeToString(fileBytes));

		}

	}

	@Override
	public void beaconReceived(String message) {

		taskQueue.execute(() -> {

			try {

				Beacon beacon = Beacon.fromJson(message);

				final String userUuid = beacon.uuid;

				if (userUuid == null)
					return;

				boolean wasOnline = model.isContactOnline(userUuid);

				Contact incomingContact = new Contact(beacon.uuid, beacon.name, beacon.comment,
						Availability.of(beacon.status), beacon.lattitude, beacon.longitude, beacon.secretId);

				final Contact newContact = dbManager.addUpdateContact(incomingContact);

				newContact.setRemoteInterfaces(beacon.remoteInterfaces);
				newContact.setLocalInterfaces(beacon.localInterfaces);

				model.addContact(newContact);

				Long contactId = newContact.getId();

				Platform.runLater(() -> dmsPanel.updateContact(newContact));

				listenerTaskQueue.execute(() -> dmsListeners
						.forEach(listener -> listener.contactUpdated(new ContactHandleImpl(newContact))));

				if (!wasOnline) {

					// If the contact has just been online, send all things waiting for it, adjust
					// its groups' availability.
					taskQueue.execute(() -> {

						// START WITH PRIVATE MESSAGES
						try {

							for (Message waitingMessage : dbManager.getPrivateMessagesWaitingToContact(contactId)) {

								switch (waitingMessage.getMessageStatus()) {

								case FRESH:

									sendPrivateMessage(waitingMessage);

									break;

								case SENT:
								case RECEIVED:

									dmsClient.claimMessageStatus(waitingMessage.getId(), userUuid);

									break;

								default:

									break;

								}

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

						// SEND WAITING GROUP MESSAGES
						try {

							for (Message waitingMessage : dbManager.getGroupMessagesWaitingToContact(contactId)) {

								waitingMessage.getStatusReports().stream()
										.filter(statusReport -> Objects.equals(statusReport.getContactId(), contactId))
										.forEach(statusReport -> {

											switch (statusReport.getMessageStatus()) {

											case FRESH:

												sendGroupMessage(waitingMessage, userUuid);

												break;

											case SENT:
											case RECEIVED:

												dmsClient.claimMessageStatus(waitingMessage.getId(), userUuid);

												break;

											default:

												break;

											}

										});

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

						// CLAIM WAITING STATUS REPORTS
						try {

							for (Message waitingMessage : dbManager.getGroupMessagesNotReadToItsGroup(contactId)) {

								dmsClient.claimStatusReport(waitingMessage.getId(), userUuid);

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

						// MODIFY GROUP STATUS
						try {

							for (Dgroup group : dbManager.getAllActiveGroupsOfContact(contactId)) {

								group.setStatus(Availability.LIMITED);

								try {

									final Dgroup newGroup = dbManager.addUpdateGroup(group);

									model.addGroup(newGroup);

									Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

									listenerTaskQueue.execute(() -> dmsListeners
											.forEach(listener -> listener.groupUpdated(new GroupHandleImpl(newGroup))));

								} catch (HibernateException e) {

									e.printStackTrace();

								}

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

					});

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void remoteIpsReceived(String message) {

		String[] remoteIps = message.isEmpty() ? new String[0] : message.split(";");

		Platform.runLater(() -> dmsPanel.updateRemoteIps(remoteIps));

	}

	@Override
	public void progressMessageReceived(final Long messageId, final String[] remoteUuids, final int progress) {
		// messageId = local database id of the message (not the message id, which is
		// the local
		// database id of the sender)

		if (messageId == null)
			return;

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessageById(messageId);

				if (message == null || Objects.equals(message.getWaitStatus(), WaitStatus.CANCELED))
					return;

				List<Long> remoteIds = new ArrayList<Long>();

				for (String remoteUuid : remoteUuids) {

					Contact contact = getContact(remoteUuid);

					if (contact != null)
						remoteIds.add(contact.getId());

				}

				if (progress == 100) {

					// Update status in database and view; send update to message owner if necessary

					updateMessageStatus(message, remoteIds, MessageStatus.SENT);

				}

				// Update view only

				if (Objects.equals(message.getMessageType(), MessageType.UPDATE)
						|| Objects.equals(message.getMessageDirection(), MessageDirection.IN))
					return;

				switch (message.getReceiverType()) {

				case CONTACT:

				{

					for (Long id : remoteIds) {

						model.storePrivateMessageProgress(id, messageId, progress);

						Platform.runLater(() -> dmsPanel.updatePrivateMessageProgress(id, messageId, progress));

					}

					break;

				}

				case GROUP_OWNER:
				case GROUP_MEMBER:

				{

					for (Long id : remoteIds) {

						model.storeGroupMessageProgress(messageId, id, progress);

						if (Objects.equals(messageId, model.getDetailedGroupMessageId()))
							Platform.runLater(() -> dmsPanel.updateDetailedMessageProgress(id, progress));

					}

					break;

				}

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void progressTransientReceived(final Long trackingId, final String[] remoteUuids, final int progress) {

		taskQueue.execute(() -> {

			List<Long> remoteIds = new ArrayList<Long>();

			for (String remoteUuid : remoteUuids) {

				Contact contact = getContact(remoteUuid);

				if (contact != null)
					remoteIds.add(contact.getId());

			}

			listenerTaskQueue
					.execute(() -> dmsListeners.forEach(listener -> listener.messageFailed(trackingId, remoteIds)));

		});

	}

	@Override
	public void messageReceived(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				Message incomingMessage = Message.fromJson(message);
				incomingMessage.setMessageRefId(incomingMessage.getId());
				incomingMessage.setId(null);

				Message dbMessage = dbManager.getMessageBySender(remoteUuid, incomingMessage.getMessageRefId());

				if (dbMessage != null)
					return;

				incomingMessage.setContact(getContact(remoteUuid));

				if (Objects.equals(incomingMessage.getMessageType(), MessageType.UPDATE))
					updateMessageReceived(incomingMessage);

				boolean messageToBeRedirected = false;

				switch (incomingMessage.getReceiverType()) {

				case GROUP_OWNER: {

					Dgroup dgroup = model.getGroup(incomingMessage.getGroupRefId());

					incomingMessage.setDgroup(dgroup);

					dgroup.getMembers().forEach(member -> {

						if (Objects.equals(member.getUuid(), remoteUuid))
							return;

						incomingMessage.addStatusReport(new StatusReport(member.getId(), MessageStatus.FRESH));

					});

					messageToBeRedirected = incomingMessage.getStatusReports().size() > 0;

					break;

				}

				case GROUP_MEMBER: {

					Dgroup dgroup = model.getGroup(remoteUuid, incomingMessage.getGroupRefId());

					if (dgroup == null) {
						groupUpdateReceived(remoteUuid, incomingMessage.getGroupRefId(), new GroupUpdate());
						dgroup = model.getGroup(remoteUuid, incomingMessage.getGroupRefId());
					}

					Member member = dbManager.getMember(remoteUuid, incomingMessage.getContactRefId());

					if (member != null) {
						incomingMessage.setOwner(member.getContact());
					}

					incomingMessage.setDgroup(dgroup);

					break;

				}

				default:

					break;

				}

				switch (incomingMessage.getMessageType()) {

				case FILE:
				case AUDIO:

					try {

						incomingMessage
								.setContent(composeFile(FilePojo.fromJson(incomingMessage.getContent())).toString());

					} catch (Exception e) {

						incomingMessage.setContent("");

						e.printStackTrace();

					}

					break;

				default:

					break;

				}

				incomingMessage.setMessageStatus(computeMessageStatus(incomingMessage));

				incomingMessage.setWaitStatus(messageToBeRedirected ? WaitStatus.WAITING : WaitStatus.DONE);

				incomingMessage.setMessageDirection(MessageDirection.IN);

				Message newMessage = dbManager.addUpdateMessage(incomingMessage);

				switch (newMessage.getReceiverType()) {

				case CONTACT:

					privateMessageReceived(newMessage);

					dmsClient.feedMessageStatus(remoteUuid, newMessage.getMessageRefId(),
							newMessage.getMessageStatus());

					break;

				case GROUP_OWNER:

					groupMessageReceived(newMessage);

					dmsClient.feedMessageStatus(remoteUuid, newMessage.getMessageRefId(),
							newMessage.getMessageStatus());

					break;

				case GROUP_MEMBER:

					groupMessageReceived(newMessage);

					dmsClient.feedMessageStatus(remoteUuid, newMessage.getMessageRefId(),
							newMessage.getMessageStatus());

					break;

				}

				if (messageToBeRedirected) {

					sendGroupMessage(newMessage);

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void userDisconnected(String uuid) {

		Contact contact = getContact(uuid);

		if (contact == null)
			return;

		contactDisconnected(contact);

	}

	@Override
	public void serverConnStatusUpdated(boolean connStatus) {

		model.setServerConnStatus(connStatus);

		if (connStatus) {

			Contact identity = model.getIdentity();

			sendBeacon(identity.getName(), identity.getComment(), identity.getStatus(), identity.getLattitude(),
					identity.getLongitude(), identity.getSecretId());

			dmsClient.claimStartInfo();

		} else {

			model.getContacts().forEach((id, contact) -> {

				contactDisconnected(contact);

			});

			clearMessageProgresses();

		}

		Platform.runLater(() -> dmsPanel.serverConnStatusUpdated(connStatus));

	}

	@Override
	public void messageStatusClaimed(final Long messageId, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				Message incomingMessage = dbManager.getMessageBySender(remoteUuid, messageId);

				if (incomingMessage == null) {

					// Not received
					dmsClient.feedMessageStatus(remoteUuid, messageId, MessageStatus.FRESH);

				} else {

					dmsClient.feedMessageStatus(remoteUuid, messageId, incomingMessage.getMessageStatus());

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messageStatusFed(final Long messageId, final MessageStatus messageStatus, String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				Message dbMessage = dbManager.getMessageById(messageId);

				if (dbMessage == null)
					return;

				Contact contact = getContact(remoteUuid);

				if (contact != null)
					updateMessageStatus(dbMessage, Arrays.asList(contact.getId()), messageStatus);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void groupMessageStatusFed(final Long messageId, final GroupMessageStatus groupMessageStatus,
			String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				Message dbMessage = dbManager.getMessageById(messageId);

				if (dbMessage == null)
					return;

				List<Long> ids = new ArrayList<Long>();

				groupMessageStatus.refIds.forEach(refId -> {

					Member member = dbManager.getMember(remoteUuid, refId);

					if (member != null)
						ids.add(member.getContact().getId());

				});

				updateMessageStatus(dbMessage, ids, groupMessageStatus.messageStatus);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusReportClaimed(final Long messageId, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				Message dbMessage = dbManager.getMessageBySender(remoteUuid, messageId);

				if (dbMessage == null)
					return;

				dmsClient.feedStatusReport(messageId, dbMessage.getStatusReports(), remoteUuid);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusReportFed(final Long messageId, final StatusReport[] statusReports) {

		taskQueue.execute(() -> {

			try {

				// A status report can only be claimed by a group message owner. So at this
				// point, I must be a group message owner. Let's find that message.

				Message dbMessage = dbManager.getMessageById(messageId);

				if (dbMessage == null || Objects.equals(dbMessage.getWaitStatus(), WaitStatus.CANCELED))
					return;

				Dgroup group = dbMessage.getDgroup();

				if (group == null)
					return;

				Map<Long, StatusReport> statusReportMap = new HashMap<Long, StatusReport>();

				dbMessage.getStatusReports()
						.forEach(statusReport -> statusReportMap.put(statusReport.getContactId(), statusReport));

				// Clean status reports to refill it (so to discard the unnecessary ones)
				statusReportMap.forEach((contactId, statusReport) -> {

					if (Objects.equals(dbMessage.getContact().getId(), contactId))
						return;

					dbMessage.removeStatusReport(statusReport);

				});

				for (StatusReport statusReport : statusReports) {

					Member member = dbManager.getMember(group.getOwner().getUuid(), statusReport.getContactId());

					if (member == null)
						continue;

					Long contactId = member.getContact().getId();

					StatusReport oldStatusReport = statusReportMap.get(contactId);

					if (oldStatusReport != null) {

						oldStatusReport.setMessageStatus(statusReport.getMessageStatus());

						dbMessage.addStatusReport(oldStatusReport);

					} else {

						dbMessage.addStatusReport(new StatusReport(contactId, statusReport.getMessageStatus()));

					}

				}

				// I just update my db and view. I don't do anything else like re-sending the
				// message etc.

				dbMessage.setMessageStatus(dbMessage.getOverallStatus());

				final Message newMessage = dbManager.addUpdateMessage(dbMessage);

				if (!Objects.equals(newMessage.getMessageType(), MessageType.UPDATE))
					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage));

				if (Objects.equals(newMessage.getId(), model.getDetailedGroupMessageId())) {
					newMessage.getStatusReports()
							.forEach(statusReport -> Platform
									.runLater(() -> dmsPanel.updateDetailedMessageStatus(statusReport.getContactId(),
											statusReport.getMessageStatus())));
				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void transientMessageReceived(String message, String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				MessageHandleImpl incomingMessage = MessageHandleImpl.fromJson(message);

				if (incomingMessage.getReceiverType() == null)
					incomingMessage.setReceiverType(ReceiverType.CONTACT);

				if (incomingMessage.getFlag() != null) {

					transientMessageStatusReceived(incomingMessage, remoteUuid);

					return;

				}

				Long contactId = getContact(remoteUuid).getId();
				Long groupId = incomingMessage.getGroupRefId();

				if (groupId != null) {

					switch (incomingMessage.getReceiverType()) {

					case GROUP_MEMBER: {

						Dgroup dgroup = model.getGroup(remoteUuid, groupId);

						if (dgroup == null)
							break;

						groupId = dgroup.getId();

						Member member = dbManager.getMember(remoteUuid, incomingMessage.getContactRefId());

						if (member != null)
							contactId = member.getContact().getId();

						break;

					}

					case GROUP_OWNER: {

						Dgroup dgroup = model.getGroup(groupId);

						if (dgroup == null)
							break;

						List<String> contactUuids = new ArrayList<String>();

						dgroup.getMembers().forEach(contact -> {
							if (Objects.equals(contact.getUuid(), remoteUuid))
								return;
							contactUuids.add(contact.getUuid());
						});

						if (contactUuids.size() == 0)
							break;

						incomingMessage.setContactRefId(contactId);

						MessageHandleImpl outgoingMessage = new MessageHandleImpl(incomingMessage);
						outgoingMessage.setReceiverType(ReceiverType.GROUP_MEMBER);
						outgoingMessage.setContactRefId(contactId);
						outgoingMessage.setGroupRefId(groupId);

						dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids, null);

						break;

					}

					default:
						break;

					}

				}

				FileHandleImpl fileHandle = (FileHandleImpl) incomingMessage.getFileHandle();

				if (fileHandle != null) {

					try {

						incomingMessage.setFileHandle(
								new FileHandleImpl(fileHandle.getFileCode(), composeFile(fileHandle.getFilePojo())));

					} catch (IOException e) {

						incomingMessage.setFileHandle(null);

					}

				}

				final Long finalContactId = contactId;
				final Long finalGroupId = groupId;

				listenerTaskQueue.execute(() -> dmsListeners
						.forEach(listener -> listener.messageReceived(incomingMessage, finalContactId, finalGroupId)));

				sendTransientMessageStatus(incomingMessage, remoteUuid);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	private void transientMessageStatusReceived(MessageHandleImpl incomingMessage, String remoteUuid) throws Exception {

		final Long trackingId = incomingMessage.getTrackingId();

		if (trackingId == null)
			return;

		Long contactId = getContact(remoteUuid).getId();

		switch (incomingMessage.getReceiverType()) {

		case GROUP_MEMBER: {

			Long contactRefId = incomingMessage.getContactRefId();

			Member member = dbManager.getMember(remoteUuid, contactRefId);

			if (member != null)
				contactId = member.getContact().getId();

			break;

		}

		case GROUP_OWNER: {

			Long contactRefId = incomingMessage.getContactRefId();

			if (contactRefId != null) {

				incomingMessage.setReceiverType(ReceiverType.GROUP_MEMBER);
				incomingMessage.setContactRefId(contactId);

				dmsClient.sendTransientMessage(incomingMessage.toJson(),
						Arrays.asList(model.getContact(contactRefId).getUuid()), null);

				return;

			}

			break;

		}

		default:
			break;

		}

		final Long finalContactId = contactId;

		listenerTaskQueue.execute(
				() -> dmsListeners.forEach(listener -> listener.messageTransmitted(trackingId, finalContactId)));

	}

	private void sendTransientMessageStatus(MessageHandleImpl incomingMessage, String remoteUuid) throws Exception {

		Long trackingId = incomingMessage.getTrackingId();

		if (trackingId == null)
			return;

		MessageHandleImpl response = new MessageHandleImpl(null, null);
		response.setTrackingId(trackingId);
		response.setFlag(1);

		switch (incomingMessage.getReceiverType()) {

		case GROUP_MEMBER: {

			response.setReceiverType(ReceiverType.GROUP_OWNER);

			response.setContactRefId(incomingMessage.getContactRefId());

			break;

		}

		case GROUP_OWNER: {

			response.setReceiverType(ReceiverType.GROUP_MEMBER);

			break;

		}

		default:
			break;

		}

		dmsClient.sendTransientMessage(response.toJson(), Arrays.asList(remoteUuid), null);

	}

	@Override
	public void commentUpdateRequested(String comment) {

		taskQueue.execute(() -> {

			try {

				Contact identity = model.getIdentity();

				if (Objects.equals(identity.getComment(), comment))
					return;

				identity.setComment(comment);

				Contact newIdentity = dbManager.updateIdentity(identity);

				model.updateComment(comment);

				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));

				sendBeacon(null, comment, null, null, null, null);

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusUpdateRequested(final Availability availability) {

		taskQueue.execute(() -> {

			try {

				Contact identity = model.getIdentity();

				if (Objects.equals(identity.getStatus(), availability))
					return;

				identity.setStatus(availability);

				Contact newIdentity = dbManager.updateIdentity(identity);

				model.updateStatus(newIdentity.getStatus());

				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));

				sendBeacon(null, null, newIdentity.getStatus(), null, null, null);

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messagePaneOpened(final Long id) {

		taskQueue.execute(() -> {

			model.messagePaneOpened(id);

			if (id > 0) {

				contactMessagePaneOpened(id);

			} else {

				groupMessagePaneOpened(-id);

			}

		});

	}

	private void contactMessagePaneOpened(final Long id) {

		try {

			List<Message> messagesWaitingFromContact = dbManager.getPrivateMessagesWaitingFromContact(id);

			for (Message incomingMessage : messagesWaitingFromContact) {

				try {

					incomingMessage.setMessageStatus(MessageStatus.READ);

					final Message newMessage = dbManager.addUpdateMessage(incomingMessage);

					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage));

					dmsClient.feedMessageStatus(newMessage.getContact().getUuid(), newMessage.getMessageRefId(),
							MessageStatus.READ);

				} catch (JsonSyntaxException | HibernateException e) {

					e.printStackTrace();

				}

			}

			Platform.runLater(() -> dmsPanel.scrollPaneToMessage(id,
					messagesWaitingFromContact.size() > 0 ? messagesWaitingFromContact.get(0).getId() : -1L));

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

	private void groupMessagePaneOpened(Long id) {

		try {

			List<Message> messagesWaitingFromGroup = dbManager.getMessagesWaitingFromGroup(id);

			for (Message incomingMessage : messagesWaitingFromGroup) {

				try {

					incomingMessage.setMessageStatus(MessageStatus.READ);

					final Message newMessage = dbManager.addUpdateMessage(incomingMessage);

					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage));

					Dgroup group = model.getGroup(id);

					if (group == null)
						return;

					if (Objects.equals(newMessage.getReceiverType(), ReceiverType.GROUP_OWNER)) {

						dmsClient.feedMessageStatus(newMessage.getContact().getUuid(), newMessage.getMessageRefId(),
								newMessage.getMessageStatus());

					} else if (Objects.equals(newMessage.getReceiverType(), ReceiverType.GROUP_MEMBER)) {

						dmsClient.feedMessageStatus(group.getOwner().getUuid(), newMessage.getMessageRefId(),
								newMessage.getMessageStatus());

					}

				} catch (JsonSyntaxException | HibernateException e) {

					e.printStackTrace();

				}

			}

			Platform.runLater(() -> dmsPanel.scrollPaneToMessage(-id,
					messagesWaitingFromGroup.size() > 0 ? messagesWaitingFromGroup.get(0).getId() : -1L));

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void messagePaneClosed(final Long id) {

		taskQueue.execute(() -> {

			model.messagePaneClosed(id);

		});

	}

	@Override
	public void sendMessageClicked(final Long id, final String messageTxt, final Long refMessageId) {

		taskQueue.execute(() -> {

			try {

				if (id > 0) {

					sendPrivateMessageClaimed(id, messageTxt, refMessageId, MessageType.TEXT, null, null);

					listenerTaskQueue.execute(() -> dmsGuiListeners
							.forEach(guiListener -> guiListener.guiMessageSent(messageTxt, id, null)));

				} else {

					sendGroupMessageClaimed(-id, messageTxt, refMessageId, MessageType.TEXT, null, null);

					listenerTaskQueue.execute(() -> dmsGuiListeners
							.forEach(guiListener -> guiListener.guiMessageSent(messageTxt, null, -id)));

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	private Long sendPrivateMessageClaimed(final Long contactId, final String messageTxt, final Long refMessageId,
			MessageType messageType, MessageSubType messageSubType, Integer apiFlag) throws Exception {

		Contact contact = model.getContact(contactId);

		Message refMessage = dbManager.getMessageById(refMessageId);

		Message newMessage = createOutgoingMessage(messageTxt, contact, null, null, ReceiverType.CONTACT, messageType,
				messageSubType, refMessage, apiFlag);

		addMessageToPane(newMessage);

		sendPrivateMessage(newMessage);

		return newMessage.getId();

	}

	private Long sendGroupMessageClaimed(final Long groupId, final String messageTxt, final Long refMessageId,
			MessageType messageType, MessageSubType messageSubType, Integer apiFlag) throws Exception {

		Dgroup group = model.getGroup(groupId);

		Message refMessage = dbManager.getMessageById(refMessageId);

		Message newMessage = createOutgoingMessage(messageTxt, group.getOwner(), group, createStatusReports(group),
				Objects.equals(group.getOwner().getUuid(), model.getLocalUuid()) ? ReceiverType.GROUP_MEMBER
						: ReceiverType.GROUP_OWNER,
				messageType, messageSubType, refMessage, apiFlag);

		addMessageToPane(newMessage);

		sendGroupMessage(newMessage);

		return newMessage.getId();

	}

	@Override
	public void paneScrolledToTop(final Long id, final Long topMessageId) {

		if (topMessageId == Long.MAX_VALUE)
			return;

		taskQueue.execute(() -> {

			try {

				if (id > 0) {

					contactPaneScrolledToTop(id, topMessageId);

				} else {

					groupPaneScrolledToTop(id, topMessageId);

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	private void contactPaneScrolledToTop(final Long id, final Long topMessageId) throws HibernateException {

		if (topMessageId < 0)
			return;

		List<Message> lastMessagesBeforeId = dbManager.getLastPrivateMessagesBeforeId(id, topMessageId,
				MIN_MESSAGES_PER_PAGE);

		if (lastMessagesBeforeId.size() == 0)
			return;

		Platform.runLater(() -> dmsPanel.savePosition(id, topMessageId));

		lastMessagesBeforeId.forEach(message -> addMessageToPane(message));

		Platform.runLater(() -> dmsPanel.scrollToSavedPosition(id));

	}

	private void groupPaneScrolledToTop(final Long id, final Long topMessageId) throws HibernateException {

		if (topMessageId < 0)
			return;

		List<Message> lastMessagesBeforeId = dbManager.getLastGroupMessagesBeforeId(-id, topMessageId,
				MIN_MESSAGES_PER_PAGE);

		if (lastMessagesBeforeId.size() == 0)
			return;

		Platform.runLater(() -> dmsPanel.savePosition(id, topMessageId));

		lastMessagesBeforeId.forEach(message -> addMessageToPane(message));

		Platform.runLater(() -> dmsPanel.scrollToSavedPosition(id));

	}

	@Override
	public void messagesClaimed(final Long id, final Long lastMessageIdExcl, final Long firstMessageIdIncl) {

		if (lastMessageIdExcl == Long.MAX_VALUE)
			return;

		taskQueue.execute(() -> {

			try {

				if (id > 0) {

					contactMessagesClaimed(id, lastMessageIdExcl, firstMessageIdIncl);

				} else {

					groupMessagesClaimed(id, lastMessageIdExcl, firstMessageIdIncl);

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	public void contactMessagesClaimed(final Long id, final Long lastMessageIdExcl, final Long firstMessageIdIncl)
			throws HibernateException {

		long topMessageId = lastMessageIdExcl;

		List<Message> lastMessagesBetweenIds = new ArrayList<Message>();

		while (firstMessageIdIncl < topMessageId) {

			List<Message> lastMessagesBeforeId = dbManager.getLastPrivateMessagesBeforeId(id, topMessageId,
					MIN_MESSAGES_PER_PAGE);

			if (lastMessagesBeforeId.size() == 0)
				break;

			lastMessagesBetweenIds.addAll(lastMessagesBeforeId);

			topMessageId = lastMessagesBeforeId.get(lastMessagesBeforeId.size() - 1).getId();

		}

		if (lastMessagesBetweenIds.size() == 0)
			return;

		lastMessagesBetweenIds.forEach(message -> addMessageToPane(message));

		Platform.runLater(() -> {
			dmsPanel.savePosition(id, firstMessageIdIncl);
			dmsPanel.scrollToSavedPosition(id);
		});

	}

	public void groupMessagesClaimed(final Long id, final Long lastMessageIdExcl, final Long firstMessageIdIncl)
			throws HibernateException {

		long topMessageId = lastMessageIdExcl;

		List<Message> lastMessagesBetweenIds = new ArrayList<Message>();

		while (firstMessageIdIncl < topMessageId) {

			List<Message> lastMessagesBeforeId = dbManager.getLastGroupMessagesBeforeId(-id, topMessageId,
					MIN_MESSAGES_PER_PAGE);

			if (lastMessagesBeforeId.size() == 0)
				break;

			lastMessagesBetweenIds.addAll(lastMessagesBeforeId);

			topMessageId = lastMessagesBeforeId.get(lastMessagesBeforeId.size() - 1).getId();

		}

		if (lastMessagesBetweenIds.size() == 0)
			return;

		lastMessagesBetweenIds.forEach(message -> addMessageToPane(message));

		Platform.runLater(() -> {
			dmsPanel.savePosition(id, firstMessageIdIncl);
			dmsPanel.scrollToSavedPosition(id);
		});

	}

	@Override
	public void showAddUpdateGroupClicked(Long id) {

		Dgroup group = id == null ? null : model.getGroup(-id);

		model.setGroupToBeUpdated(group);

		if (group == null) {
			// New group

			Platform.runLater(() -> dmsPanel.showAddUpdateGroupPane(null, null, true));

		} else {
			// Update group

			Set<String> uuids = new HashSet<String>();
			group.getMembers().forEach(contact -> uuids.add(contact.getUuid()));

			Platform.runLater(() -> dmsPanel.showAddUpdateGroupPane(group.getName(), uuids, false));

		}

	}

	@Override
	public void addUpdateGroupRequested(String groupName, Set<String> selectedUuids) {

		taskQueue.execute(() -> {

			try {

				Dgroup group = model.getGroupToBeUpdated();
				model.setGroupToBeUpdated(null);

				Set<Contact> selectedContacts = new HashSet<Contact>();
				selectedUuids.forEach(uuid -> {
					Contact contact = getContact(uuid);
					if (contact != null)
						selectedContacts.add(contact);
				});

				if (group == null) {
					// New group

					group = new Dgroup();
					group.setOwner(model.getIdentity());

					final Dgroup newGroup = createUpdateGroup(group, groupName, true, selectedContacts, null);

					if (newGroup == null)
						return;

					model.addGroup(newGroup);

					Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

					// Gruba eleman ekleme mesajini olusturup grup uyelerine gonder

					String groupUpdate = getGroupUpdate(newGroup.getName(), true, newGroup.getMembers(), null);

					sendGroupMessage(createOutgoingMessage(groupUpdate, newGroup.getOwner(), newGroup,
							createStatusReports(newGroup), ReceiverType.GROUP_MEMBER, MessageType.UPDATE,
							MessageSubType.UPDATE_GROUP, null, null));

				} else {
					// Update group

					Set<Contact> residentContacts = new HashSet<Contact>(group.getMembers());
					Set<Contact> contactsToBeAdded = new HashSet<Contact>(selectedContacts);
					Set<Contact> contactsToBeRemoved = new HashSet<Contact>(group.getMembers());

					contactsToBeAdded.removeAll(residentContacts);
					contactsToBeRemoved.removeAll(selectedContacts);
					residentContacts.removeAll(contactsToBeRemoved);

					// The group hasn't changed, nothing to do:
					if (Objects.equals(group.getName(), groupName) && contactsToBeAdded.isEmpty()
							&& contactsToBeRemoved.isEmpty())
						return;

					String newGroupName = Objects.equals(group.getName(), groupName) ? null : groupName;

					Dgroup newGroup = createUpdateGroup(group, newGroupName, group.getActive(), contactsToBeAdded,
							contactsToBeRemoved);

					if (newGroup == null)
						return;

					model.addGroup(newGroup);

					Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

					if (!contactsToBeAdded.isEmpty()) {

						String groupUpdateToAddedContacts = getGroupUpdate(newGroup.getName(), true,
								newGroup.getMembers(), null);

						sendGroupMessage(
								createOutgoingMessage(groupUpdateToAddedContacts, newGroup.getOwner(), newGroup,
										createStatusReports(contactsToBeAdded), ReceiverType.GROUP_MEMBER,
										MessageType.UPDATE, MessageSubType.UPDATE_GROUP, null, null),
								contactsToBeAdded);

					}

					if (!contactsToBeRemoved.isEmpty()) {

						String groupUpdateToRemovedContacts = getGroupUpdate(null, false, null, null);

						sendGroupMessage(
								createOutgoingMessage(groupUpdateToRemovedContacts, newGroup.getOwner(), newGroup,
										createStatusReports(contactsToBeRemoved), ReceiverType.GROUP_MEMBER,
										MessageType.UPDATE, MessageSubType.UPDATE_GROUP, null, null),
								contactsToBeRemoved);

					}

					if (!residentContacts.isEmpty()) {

						String groupUpdateToResidentContacts = getGroupUpdate(newGroupName, true, contactsToBeAdded,
								contactsToBeRemoved);

						sendGroupMessage(createOutgoingMessage(groupUpdateToResidentContacts, newGroup.getOwner(),
								newGroup, createStatusReports(residentContacts), ReceiverType.GROUP_MEMBER,
								MessageType.UPDATE, MessageSubType.UPDATE_GROUP, null, null), residentContacts);

					}

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void deleteGroupRequested() {

		taskQueue.execute(() -> {

			Dgroup group = model.getGroupToBeUpdated();
			model.setGroupToBeUpdated(null);

			if (group == null)
				return;

			group.setActive(false);

			Set<Contact> contacts = new HashSet<Contact>(group.getMembers());

			try {

				Dgroup newGroup = createUpdateGroup(group, null, group.getActive(), null, null);

				if (newGroup == null)
					return;

				model.addGroup(newGroup);

				Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

				String groupUpdate = getGroupUpdate(null, newGroup.getActive(), null, null);

				sendGroupMessage(
						createOutgoingMessage(groupUpdate, newGroup.getOwner(), newGroup, createStatusReports(contacts),
								ReceiverType.GROUP_MEMBER, MessageType.UPDATE, MessageSubType.UPDATE_GROUP, null, null),
						contacts);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void fileSelected(final Long id, final Path file, final Long refMessageId) {

		taskQueue.execute(() -> {

			try {

				Path dstFile = copyFileToSendFolder(file);

				// Now to the send operations

				if (id > 0) {

					sendPrivateMessageClaimed(id, dstFile.toString(), refMessageId, MessageType.FILE, null, null);

					listenerTaskQueue.execute(
							() -> dmsGuiListeners.forEach(guiListener -> guiListener.guiFileSent(dstFile, id, null)));

				} else {

					sendGroupMessageClaimed(-id, dstFile.toString(), refMessageId, MessageType.FILE, null, null);

					listenerTaskQueue.execute(
							() -> dmsGuiListeners.forEach(guiListener -> guiListener.guiFileSent(dstFile, null, -id)));

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messageClicked(Long messageId) {

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessageById(messageId);

				if (!Objects.equals(message.getMessageType(), MessageType.FILE))
					return;

				Path file = Paths.get(message.getContent());

				if (CommonConstants.AUTO_OPEN_FILE) {

					new ProcessBuilder().directory(file.getParent().toFile())
							.command("cmd", "/C", file.getFileName().toString()).start();

				} else {

					listenerTaskQueue.execute(() -> dmsListeners.forEach(listener -> listener.fileClicked(file)));

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void infoClicked(final Long messageId) {

		taskQueue.execute(() -> {

			if (model.getDetailedGroupMessageId() > 0)
				return;

			try {

				Message message = dbManager.getMessageById(messageId);

				if (message == null || Objects.equals(message.getReceiverType(), ReceiverType.CONTACT)
						|| Objects.equals(message.getMessageDirection(), MessageDirection.IN))
					return;

				Dgroup group = message.getDgroup();

				if (group == null)
					return;

				model.setDetailedGroupMessageId(messageId);

				List<Contact> contacts = new ArrayList<Contact>();

				Long groupOwnerId = group.getOwner().getId();

				message.getStatusReports().forEach(statusReport -> {

					if (Objects.equals(statusReport.getContactId(), groupOwnerId))
						return;

					Contact contact = model.getContact(statusReport.getContactId());

					if (contact == null)
						return;

					contacts.add(contact);

				});

				contacts.sort(model.getContactSorter());

				message.getStatusReports().stream()
						.filter(statusReport -> Objects.equals(statusReport.getContactId(), groupOwnerId))
						.forEach(statusReport -> {

							Contact contact = model.getContact(groupOwnerId);

							if (contact != null)
								contacts.add(0, contact);

						});

				Platform.runLater(() -> dmsPanel.showStatusInfoPane(contacts));

				message.getStatusReports().forEach(statusReport -> Platform.runLater(() -> dmsPanel
						.updateDetailedMessageStatus(statusReport.getContactId(), statusReport.getMessageStatus())));

				if (model.getGroupMessageProgresses(messageId) != null)
					model.getGroupMessageProgresses(messageId).forEach((id, progress) -> Platform
							.runLater(() -> dmsPanel.updateDetailedMessageProgress(id, progress)));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void cancelClicked(Long messageId) {

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessageById(messageId);

				Message newMessage = cancelMessage(message);

				Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusInfoClosed() {

		taskQueue.execute(() -> {

			model.setDetailedGroupMessageId(-1L);

		});

	}

	@Override
	public void addIpClicked(String ip) {

		dmsClient.addRemoteIp(ip);

	}

	@Override
	public void removeIpClicked(String ip) {

		dmsClient.removeRemoteIp(ip);

	}

	@Override
	public void recordButtonPressed(final Long id) {

		taskQueue.execute(() -> {

			try {

				audioCenter.prepareRecording();

				Platform.runLater(() -> dmsPanel.recordingStarted(id));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void recordEventTriggered(final Long id, final Long refMessageId) {

		taskQueue.execute(() -> {

			try {

				String fileName = String.format("audio_%s.wav",
						CommonConstants.DATE_TIME_FORMATTER.format(LocalDateTime.now()));
				Path dstFolder = Paths.get(CommonConstants.SEND_FOLDER).normalize().toAbsolutePath();

				Path dstFile = getDstFile(dstFolder, fileName);

				audioCenter.startRecording(id, dstFile, refMessageId);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void recordButtonReleased(final Long id) {

		taskQueue.execute(() -> {

			audioCenter.stopRecording();

			Platform.runLater(() -> dmsPanel.recordingStopped(id));

		});

	}

	@Override
	public void recordingStopped(final Long id, final Path path, final Long refId) {

		taskQueue.execute(() -> {

			boolean recordSuccessful = path != null && Files.exists(path);

			try {

				if (id > 0) {

					Platform.runLater(() -> dmsPanel.recordingStopped(id));

					if (!recordSuccessful)
						return;

					sendPrivateMessageClaimed(id, path.toString(), refId, MessageType.AUDIO, null, null);

					listenerTaskQueue.execute(
							() -> dmsGuiListeners.forEach(guiListener -> guiListener.guiAudioSent(path, id, null)));

				} else {

					Platform.runLater(() -> dmsPanel.recordingStopped(id));

					if (!recordSuccessful)
						return;

					sendGroupMessageClaimed(-id, path.toString(), refId, MessageType.AUDIO, null, null);

					listenerTaskQueue.execute(
							() -> dmsGuiListeners.forEach(guiListener -> guiListener.guiAudioSent(path, null, -id)));

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void reportClicked(final Long id) {

		Platform.runLater(() -> reportsDialog.display(id));

	}

	@Override
	public void sendReportClicked(final Long id, final String reportHeading, final List<String> reportParagraphs) {

		taskQueue.execute(() -> {

			Platform.runLater(() -> reportsDialog.hideAndReset());

			try {

				String fileName = String.format("%s_%s.pdf", reportHeading,
						CommonConstants.DATE_TIME_FORMATTER.format(LocalDateTime.now()));
				Path dstFolder = Paths.get(CommonConstants.SEND_FOLDER).normalize().toAbsolutePath();

				Path dstFile = getDstFile(dstFolder, fileName);

				CommonMethods.writeReport(dstFile, reportHeading, reportParagraphs);

				boolean writeSuccessful = dstFile != null && Files.exists(dstFile);

				if (!writeSuccessful)
					return;

				if (id > 0) {

					sendPrivateMessageClaimed(id, dstFile.toString(), dmsPanel.getRefMessageId(id), MessageType.FILE,
							MessageSubType.FILE_REPORT, null);

					listenerTaskQueue.execute(
							() -> dmsGuiListeners.forEach(guiListener -> guiListener.guiReportSent(dstFile, id, null)));

				} else {

					sendGroupMessageClaimed(-id, dstFile.toString(), dmsPanel.getRefMessageId(id), MessageType.FILE,
							MessageSubType.FILE_REPORT, null);

					listenerTaskQueue.execute(() -> dmsGuiListeners
							.forEach(guiListener -> guiListener.guiReportSent(dstFile, null, -id)));

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public JComponent getDmsPanel() {

		return dmsPanelSwing;

	}

	@Override
	public void addListener(DmsListener listener) {

		dmsListeners.add(listener);

	}

	@Override
	public void removeListener(DmsListener listener) {

		dmsListeners.remove(listener);

	}

	@Override
	public void addGuiListener(DmsGuiListener guiListener) {

		dmsGuiListeners.add(guiListener);

	}

	@Override
	public void removeGuiListener(DmsGuiListener guiListener) {

		dmsGuiListeners.remove(guiListener);

	}

	@Override
	public void setCoordinates(Double lattitude, Double longitude) {

		taskQueue.execute(() -> {

			try {

				Contact identity = model.getIdentity();

				if (Objects.equals(lattitude, identity.getLattitude())
						&& Objects.equals(longitude, identity.getLongitude()))
					return;

				identity.setLattitude(lattitude);
				identity.setLongitude(longitude);

				Contact newIdentity = dbManager.updateIdentity(identity);

				model.updateCoordinates(lattitude, longitude);

				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));

				sendBeacon(null, null, null, lattitude, longitude, null);

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void setComment(String comment) {

		if (comment.length() > 40)
			comment = comment.substring(0, 40);

		Platform.runLater(() -> dmsPanel.setCommentEditable(false));

		commentUpdateRequested(comment);

	}

	@Override
	public void setAvailability(Availability availability) {

		statusUpdateRequested(availability);

	}

	@Override
	public void setSecretId(String secretId) {

		if (secretId == null)
			secretId = "";

		if (secretId.length() > 8)
			secretId = secretId.substring(0, 8);

		final String finalSecretId = secretId;

		taskQueue.execute(() -> {

			try {

				Contact identity = model.getIdentity();

				if (Objects.equals(finalSecretId, identity.getSecretId()))
					return;

				identity.setSecretId(finalSecretId);

				dbManager.updateIdentity(identity);

				model.updateSecretId(finalSecretId);

				sendBeacon(null, null, null, null, null, finalSecretId);

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public ContactHandle getMyContactHandle() {

		Contact identity = model.getIdentity();

		return new ContactHandleImpl(identity);

	}

	@Override
	public GroupSelectionHandle getActiveGroupsHandle() {

		return activeGroupsHandle;

	}

	@Override
	public ContactSelectionHandle getOnlineContactsHandle() {

		return onlineContactsHandle;

	}

	@Override
	public ContactHandle getContactHandle(Long contactId) {

		try {

			Contact contact = model.getContact(contactId);

			return new ContactHandleImpl(contact);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	@Override
	public List<ContactHandle> getAllContactHandles() {

		List<ContactHandle> allContactHandles = new ArrayList<ContactHandle>();

		model.getContacts().forEach((id, contact) -> allContactHandles.add(new ContactHandleImpl(contact)));

		return allContactHandles;

	}

	@Override
	public List<GroupHandle> getAllGroupHandles() {

		List<GroupHandle> allGroupHandles = new ArrayList<GroupHandle>();

		model.getGroups().forEach((id, group) -> allGroupHandles.add(new GroupHandleImpl(group)));

		return allGroupHandles;

	}

	@Override
	public GroupHandle getGroupHandle(Long groupId) {

		Dgroup group = model.getGroup(groupId);

		if (group == null)
			return null;

		return new GroupHandleImpl(group);

	}

	@Override
	public List<Long> getIdsByAddress(InetAddress address) {

		return model.getIdsByAddress(address);

	}

	@Override
	public List<Long> getIdsByAddressAndName(InetAddress address, String name) {

		return model.getIdsByAddressAndName(address, name);

	}

	@Override
	public MessageHandle createMessageHandle(String message, Integer messageCode) {

		return new MessageHandleImpl(messageCode, message);

	}

	@Override
	public FileHandle createFileHandle(Path path, Integer fileCode) {

		return new FileHandleImpl(fileCode, path);

	}

	@Override
	public ObjectHandle createObjectHandle(Object object, Integer objectCode) {

		return new ObjectHandleImpl(objectCode, CommonMethods.toJson(object));

	}

	@Override
	public <T> ListHandle createListHandle(List<T> list, Class<T> elementType, Integer listCode) {

		return new ListHandleImpl(listCode,
				CommonMethods.convertListJsonToCommon(CommonMethods.toJson(list), elementType.getSimpleName()));

	}

	@Override
	public boolean sendMessageToContacts(MessageHandle messageHandle, List<Long> contactIds) throws IOException {

		return sendMessageToContacts(messageHandle, contactIds, null);

	}

	@Override
	public boolean sendMessageToGroup(MessageHandle messageHandle, Long groupId) throws Exception {

		return sendMessageToGroup(messageHandle, groupId, null);

	}

	@Override
	public boolean sendMessageToContacts(MessageHandle messageHandle, List<Long> contactIds,
			InetAddress useLocalInterface) throws IOException {

		if (!model.isServerConnected())
			return false;

		MessageHandleImpl outgoingMessage = new MessageHandleImpl(messageHandle);

		FileHandle fileHandle = outgoingMessage.getFileHandle();

		if (fileHandle != null) {

			outgoingMessage
					.setFileHandle(new FileHandleImpl(fileHandle.getFileCode(), decomposeFile(fileHandle.getPath())));

		}

		List<String> contactUuids = new ArrayList<String>();

		contactIds.forEach(id -> {
			Contact contact = model.getContact(id);
			if (contact == null)
				return;
			contactUuids.add(contact.getUuid());
		});

		dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids, useLocalInterface);

		return true;

	}

	@Override
	public boolean sendMessageToGroup(MessageHandle messageHandle, Long groupId, InetAddress useLocalInterface)
			throws Exception {

		if (!model.isServerConnected())
			return false;

		Dgroup group = model.getGroup(groupId);

		if (group == null || Objects.equals(group.getStatus(), Availability.OFFLINE))
			return false;

		boolean master = Objects.equals(group.getOwner().getUuid(), model.getLocalUuid());

		MessageHandleImpl outgoingMessage = new MessageHandleImpl(messageHandle);

		outgoingMessage.setReceiverType(master ? ReceiverType.GROUP_MEMBER : ReceiverType.GROUP_OWNER);
		outgoingMessage.setGroupRefId(group.getGroupRefId());

		FileHandle fileHandle = outgoingMessage.getFileHandle();

		if (fileHandle != null) {

			outgoingMessage
					.setFileHandle(new FileHandleImpl(fileHandle.getFileCode(), decomposeFile(fileHandle.getPath())));

		}

		List<String> contactUuids = new ArrayList<String>();

		if (master)
			group.getMembers().forEach(contact -> contactUuids.add(contact.getUuid()));
		else
			contactUuids.add(group.getOwner().getUuid());

		dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids, useLocalInterface);

		return true;

	}

	@Override
	public Future<Long> sendGuiMessageToContact(String message, Long contactId) {

		return taskQueue.submit(() -> {

			return sendPrivateMessageClaimed(contactId, message, null, MessageType.TEXT, null, 1);

		});

	}

	@Override
	public Future<Long> sendGuiMessageToGroup(String message, Long groupId) {

		return taskQueue.submit(() -> {

			return sendGroupMessageClaimed(groupId, message, null, MessageType.TEXT, null, 1);

		});

	}

	@Override
	public Future<Long> sendGuiFileToContact(Path path, Long contactId) {

		return taskQueue.submit(() -> {

			Path dstFile = copyFileToSendFolder(path);

			return sendPrivateMessageClaimed(contactId, dstFile.toString(), null, MessageType.FILE, null, 1);

		});

	}

	@Override
	public Future<Long> sendGuiFileToGroup(Path path, Long groupId) {

		return taskQueue.submit(() -> {

			Path dstFile = copyFileToSendFolder(path);

			return sendGroupMessageClaimed(groupId, dstFile.toString(), null, MessageType.FILE, null, 1);

		});

	}

	@Override
	public Future<Long> sendGuiReportToContact(Path path, Long contactId) {

		return taskQueue.submit(() -> {

			Path dstFile = copyFileToSendFolder(path);

			return sendPrivateMessageClaimed(contactId, dstFile.toString(), null, MessageType.FILE,
					MessageSubType.FILE_REPORT, 1);

		});

	}

	@Override
	public Future<Long> sendGuiReportToGroup(Path path, Long groupId) {

		return taskQueue.submit(() -> {

			Path dstFile = copyFileToSendFolder(path);

			return sendGroupMessageClaimed(groupId, dstFile.toString(), null, MessageType.FILE,
					MessageSubType.FILE_REPORT, 1);

		});

	}

}

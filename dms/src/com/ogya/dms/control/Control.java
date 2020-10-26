package com.ogya.dms.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.hibernate.HibernateException;

import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.AudioCenter;
import com.ogya.dms.common.AudioCenter.AudioCenterListener;
import com.ogya.dms.common.AudioCenter.RecordObject;
import com.ogya.dms.common.CommonConstants;
import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.common.SoundPlayer;
import com.ogya.dms.database.DbManager;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.dmsclient.DmsClient;
import com.ogya.dms.dmsclient.intf.DmsClientListener;
import com.ogya.dms.factory.DmsFactory;
import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.exceptions.DbException;
import com.ogya.dms.intf.handles.ContactHandle;
import com.ogya.dms.intf.handles.ContactSelectionHandle;
import com.ogya.dms.intf.handles.FileHandle;
import com.ogya.dms.intf.handles.GroupHandle;
import com.ogya.dms.intf.handles.GroupSelectionHandle;
import com.ogya.dms.intf.handles.ListHandle;
import com.ogya.dms.intf.handles.MessageHandle;
import com.ogya.dms.intf.handles.ObjectHandle;
import com.ogya.dms.intf.handles.impl.ContactHandleImpl;
import com.ogya.dms.intf.handles.impl.FileHandleImpl;
import com.ogya.dms.intf.handles.impl.GroupHandleImpl;
import com.ogya.dms.intf.handles.impl.ListHandleImpl;
import com.ogya.dms.intf.handles.impl.MessageHandleImpl;
import com.ogya.dms.intf.handles.impl.MyActiveGroupsHandleImpl;
import com.ogya.dms.intf.handles.impl.ObjectHandleImpl;
import com.ogya.dms.intf.handles.impl.OnlineContactsHandleImpl;
import com.ogya.dms.intf.listeners.DmsGuiListener;
import com.ogya.dms.intf.listeners.DmsListener;
import com.ogya.dms.model.Model;
import com.ogya.dms.structures.Availability;
import com.ogya.dms.structures.FilePojo;
import com.ogya.dms.structures.GroupUpdate;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.ReceiverType;
import com.ogya.dms.structures.StatusReport;
import com.ogya.dms.structures.WaitStatus;
import com.ogya.dms.view.DmsPanel;
import com.ogya.dms.view.ReportsDialog;
import com.ogya.dms.view.ReportsPane.ReportsListener;
import com.ogya.dms.view.intf.AppListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Control implements DmsClientListener, AppListener, ReportsListener, AudioCenterListener, DmsHandle {

	private static final Map<String, Control> INSTANCES = Collections.synchronizedMap(new HashMap<String, Control>());

	private static final int MIN_MESSAGES_PER_PAGE = 50;

	private final DbManager dbManager;

	private final Model model;

	private final DmsPanel dmsPanel;
	private final JFXPanel dmsPanelSwing;

	private final ReportsDialog reportsDialog;

	private final GroupSelectionHandle myActiveGroupsHandle;
	private final ContactSelectionHandle onlineContactsHandle;

	private final DmsClient dmsClient;

	private final AudioCenter audioCenter = new AudioCenter();
	private final SoundPlayer soundPlayer = new SoundPlayer();

	private final List<DmsListener> dmsListeners = Collections.synchronizedList(new ArrayList<DmsListener>());
	private final List<DmsGuiListener> dmsGuiListeners = Collections.synchronizedList(new ArrayList<DmsGuiListener>());

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();
	private final ExecutorService listenerTaskQueue = DmsFactory.newSingleThreadExecutorService();

	private Control(String username, String password) throws DbException {

		dbManager = new DbManager(username, password);

		Identity identity = dbManager.getIdentity();

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

		myActiveGroupsHandle = new MyActiveGroupsHandleImpl(dmsPanel.getMyActiveGroupsPanel());
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

	public static Control getInstance(String username, String password) throws DbException {

		INSTANCES.putIfAbsent(username, new Control(username, password));

		return INSTANCES.get(username);

	}

	private void initDatabase() {

		dbManager.fetchAllContacts().forEach(contact -> {
			contact.setStatus(Availability.OFFLINE);
			dbManager.addUpdateContact(contact);
		});

		dbManager.fetchAllGroups().forEach(group -> {
			if (!Objects.equals(model.getLocalUuid(), group.getOwnerUuid())) {
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

		List<DmsEntity> dmsEntities = new ArrayList<DmsEntity>();

		model.getContacts().forEach((uuid, contact) -> dmsEntities.add(new DmsEntity(uuid, ReceiverType.PRIVATE)));
		model.getGroups().forEach((uuid, group) -> dmsEntities.add(new DmsEntity(uuid, ReceiverType.GROUP)));

		dmsEntities.forEach(dmsEntity -> {

			String uuid = dmsEntity.uuid;

			switch (dmsEntity.receiverType) {

			case PRIVATE: {

				List<Message> dbMessages = new ArrayList<Message>();

				dbMessages.addAll(dbManager.getAllPrivateMessagesSinceFirstUnreadMessage(model.getLocalUuid(), uuid));

				if (dbMessages.size() == 0) {

					dbManager.getLastPrivateMessages(model.getLocalUuid(), uuid, MIN_MESSAGES_PER_PAGE)
							.forEach(message -> dbMessages.add(0, message)); // inverse order

				} else if (dbMessages.size() < MIN_MESSAGES_PER_PAGE) {

					dbManager
							.getLastPrivateMessagesBeforeId(model.getLocalUuid(), uuid, dbMessages.get(0).getId(),
									MIN_MESSAGES_PER_PAGE - dbMessages.size())
							.forEach(message -> dbMessages.add(0, message)); // inverse order

				}

				dmsEntity.messages.addAll(dbMessages);

				break;

			}
			case GROUP: {

				List<Message> dbMessages = new ArrayList<Message>();

				dbMessages.addAll(dbManager.getAllGroupMessagesSinceFirstUnreadMessage(model.getLocalUuid(), uuid));

				if (dbMessages.size() == 0) {

					dbManager.getLastGroupMessages(uuid, MIN_MESSAGES_PER_PAGE)
							.forEach(message -> dbMessages.add(0, message)); // inverse order

				} else if (dbMessages.size() < MIN_MESSAGES_PER_PAGE) {

					dbManager
							.getLastGroupMessagesBeforeId(uuid, dbMessages.get(0).getId(),
									MIN_MESSAGES_PER_PAGE - dbMessages.size())
							.forEach(message -> dbMessages.add(0, message)); // inverse order

				}

				dmsEntity.messages.addAll(dbMessages);

				break;

			}

			}

		});

		Collections.sort(dmsEntities, new Comparator<DmsEntity>() {

			@Override
			public int compare(DmsEntity arg0, DmsEntity arg1) {

				Long maxMessageId0 = arg0.messages.size() > 0 ? arg0.messages.get(arg0.messages.size() - 1).getId() : 0;
				Long maxMessageId1 = arg1.messages.size() > 0 ? arg1.messages.get(arg1.messages.size() - 1).getId() : 0;

				return (int) Math.signum(maxMessageId0 - maxMessageId1);

			}

		});

		dmsEntities.forEach(dmsEntity -> {

			String uuid = dmsEntity.uuid;

			switch (dmsEntity.receiverType) {

			case PRIVATE: {

				Contact contact = model.getContact(uuid);

				Platform.runLater(() -> dmsPanel.updateContact(contact));

				dmsEntity.messages.forEach(message -> addPrivateMessageToPane(message, true));

				break;

			}
			case GROUP: {

				Dgroup group = model.getGroup(uuid);

				Platform.runLater(() -> dmsPanel.updateGroup(group));

				dmsEntity.messages.forEach(message -> addGroupMessageToPane(message, true));

				break;

			}

			}

		});

	}

	private void addPrivateMessageToPane(final Message message, final boolean newMessageToBottom) {

		if (Objects.equals(model.getLocalUuid(), message.getOwnerUuid())) {

			final String remoteUuid = message.getReceiverUuid();

			model.addMessageId(remoteUuid, message.getId());

			Platform.runLater(() -> {
				if (newMessageToBottom)
					dmsPanel.addMessageToBottom(message, "", MessageDirection.OUTGOING, remoteUuid);
				else
					dmsPanel.addMessageToTop(message, "", MessageDirection.OUTGOING, remoteUuid);
			});

		} else if (Objects.equals(model.getLocalUuid(), message.getReceiverUuid())) {

			final String remoteUuid = message.getOwnerUuid();

			model.addMessageId(remoteUuid, message.getId());

			Platform.runLater(() -> {
				if (newMessageToBottom)
					dmsPanel.addMessageToBottom(message, "", MessageDirection.INCOMING, remoteUuid);
				else
					dmsPanel.addMessageToTop(message, "", MessageDirection.INCOMING, remoteUuid);
			});

		}

	}

	private void addGroupMessageToPane(final Message message, final boolean newMessageToBottom) {

		final String groupUuid = message.getReceiverUuid();

		model.addMessageId(groupUuid, message.getId());

		if (Objects.equals(model.getLocalUuid(), message.getOwnerUuid())) {

			Platform.runLater(() -> {
				if (newMessageToBottom)
					dmsPanel.addMessageToBottom(message, "", MessageDirection.OUTGOING, groupUuid);
				else
					dmsPanel.addMessageToTop(message, "", MessageDirection.OUTGOING, groupUuid);
			});

		} else {

			Contact owner = model.getContact(message.getOwnerUuid());

			Platform.runLater(() -> {
				if (newMessageToBottom)
					dmsPanel.addMessageToBottom(message, owner == null ? "" : owner.getName(),
							MessageDirection.INCOMING, groupUuid);
				else
					dmsPanel.addMessageToTop(message, owner == null ? "" : owner.getName(), MessageDirection.INCOMING,
							groupUuid);
			});

		}

	}

	private void sendBeacon() {

		if (model.isServerConnected())
			dmsClient.sendBeacon(model.getIdentity().toJson());

	}

	private void contactDisconnected(Contact contact) {

		taskQueue.execute(() -> {

			final String uuid = contact.getUuid();

			contact.setStatus(Availability.OFFLINE);

			try {

				final Contact newContact = dbManager.addUpdateContact(contact);

				model.addContact(newContact);

				Platform.runLater(() -> dmsPanel.updateContact(newContact));

				listenerTaskQueue.execute(() -> dmsListeners.forEach(listener -> listener
						.contactUpdated(new ContactHandleImpl(uuid, newContact.getName(), newContact.getComment(),
								newContact.getLattitude(), newContact.getLongitude(), false))));

			} catch (HibernateException e) {

				e.printStackTrace();

			}

			try {

				dbManager.getAllActiveGroupsOfUuid(uuid).forEach(group -> {

					group.setStatus(Availability.OFFLINE);

					try {

						final Dgroup newGroup = dbManager.addUpdateGroup(group);

						model.addGroup(newGroup);

						Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

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

			model.getPrivateMessageProgresses()
					.forEach((uuid, messageIdProgress) -> messageIdProgress.forEach((messageId, progress) -> Platform
							.runLater(() -> dmsPanel.updatePrivateMessageProgress(uuid, messageId, -1))));

			model.clearPrivateMessageProgresses();

			Long detailedGroupMessageId = model.getDetailedGroupMessageId();

			if (model.getGroupMessageProgresses(detailedGroupMessageId) != null)
				model.getGroupMessageProgresses(detailedGroupMessageId).forEach(
						(uuid, progress) -> Platform.runLater(() -> dmsPanel.updateDetailedMessageProgress(uuid, -1)));

			model.clearGroupMessageProgresses();

		});

	}

	private Message createOutgoingMessage(String messageTxt, String receiverUuid, String statusReportStr,
			ReceiverType receiverType, MessageType messageType, Integer messageCode) throws Exception {

		Message outgoingMessage = new Message(model.getLocalUuid(), receiverUuid, receiverType, messageType,
				messageTxt);

		outgoingMessage.setSenderUuid(model.getLocalUuid());

		if (messageCode != null)
			outgoingMessage.setMessageCode(messageCode);

		outgoingMessage.setMessageStatus(MessageStatus.FRESH);

		outgoingMessage.setStatusReportStr(statusReportStr);

		outgoingMessage.setWaitStatus(WaitStatus.WAITING);

		Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

		return newMessage;

	}

	private String createStatusReportStr(Dgroup group) {

		if (group == null)
			return null;

		StatusReport statusReport = new StatusReport();

		if (!Objects.equals(group.getOwnerUuid(), model.getLocalUuid()))
			statusReport.uuidStatus.put(group.getOwnerUuid(), MessageStatus.FRESH);

		group.getContacts().forEach(contact -> {

			if (Objects.equals(contact.getUuid(), model.getLocalUuid()))
				return;

			statusReport.uuidStatus.put(contact.getUuid(), MessageStatus.FRESH);

		});

		return statusReport.toJson();

	}

	private String createStatusReportStr(Set<Contact> contacts) {

		if (contacts == null)
			return null;

		StatusReport statusReport = new StatusReport();

		contacts.forEach(contact -> statusReport.uuidStatus.put(contact.getUuid(), MessageStatus.FRESH));

		return statusReport.toJson();

	}

	private Dgroup createUpdateGroup(String groupUuid, String groupName, String ownerUuid, boolean isActive,
			Set<Contact> contactsToBeAdded, Set<Contact> contactsToBeRemoved) throws Exception {

		Dgroup group = model.getGroup(groupUuid);

		if (group == null) {
			if (groupName == null || ownerUuid == null)
				return null;
			group = new Dgroup(ownerUuid);
			group.setUuid(groupUuid);
		}

		if (groupName != null)
			group.setName(groupName);

		group.setActive(isActive);

		if (!isActive)
			group.getContacts().clear();

		if (isActive && contactsToBeAdded != null) {

			group.getContacts().addAll(contactsToBeAdded);

		}

		if (isActive && contactsToBeRemoved != null) {

			group.getContacts().removeAll(contactsToBeRemoved);

		}

		if (!isActive)
			group.setStatus(Availability.OFFLINE);
		else if (Objects.equals(group.getOwnerUuid(), model.getLocalUuid()))
			group.setStatus(Availability.AVAILABLE);
		else
			group.setStatus(!group.isActive()
					|| Objects.equals(model.getContact(group.getOwnerUuid()).getStatus(), Availability.OFFLINE)
							? Availability.OFFLINE
							: Availability.LIMITED);

		List<String> contactNames = new ArrayList<String>();
		group.getContacts().forEach(contact -> contactNames.add(contact.getName()));
		Collections.sort(contactNames, model.getCaseInsensitiveStringSorter());
		if (!Objects.equals(group.getOwnerUuid(), model.getLocalUuid()))
			contactNames.add(0, model.getContact(group.getOwnerUuid()).getName());
		group.setComment(String.join(",", contactNames));

		Dgroup newGroup = dbManager.addUpdateGroup(group);

		return newGroup;

	}

	private String getGroupUpdate(String groupName, boolean active, Set<Contact> contactsToBeAdded,
			Set<Contact> contactsToBeRemoved) {

		GroupUpdate groupUpdate = new GroupUpdate();

		groupUpdate.name = groupName;
		groupUpdate.active = active ? null : false;

		if (!(contactsToBeAdded == null || contactsToBeAdded.isEmpty())) {
			groupUpdate.add = new HashMap<String, String>();
			contactsToBeAdded.forEach(contact -> groupUpdate.add.put(contact.getUuid(), contact.getName()));
		}

		if (!(contactsToBeRemoved == null || contactsToBeRemoved.isEmpty())) {
			groupUpdate.remove = new HashMap<String, String>();
			contactsToBeRemoved.forEach(contact -> groupUpdate.remove.put(contact.getUuid(), contact.getName()));
		}

		return groupUpdate.toJson();

	}

	private void sendPrivateMessage(Message message) {

		String receiverUuid = message.getReceiverUuid();

		if (model.isContactOnline(receiverUuid))
			dmsSendMessage(message, receiverUuid);

	}

	private void sendGroupMessage(Message message) {

		String groupUuid = message.getReceiverUuid();

		Dgroup group = model.getGroup(groupUuid);

		if (group == null)
			return;

		final List<String> onlineUuids = new ArrayList<String>();

		if (Objects.equals(group.getOwnerUuid(), model.getLocalUuid())) {
			// It's my group, so I have to send this message to all the members except the
			// original sender.

			for (Contact contact : group.getContacts()) {

				String receiverUuid = contact.getUuid();

				// Skip the original sender
				if (Objects.equals(message.getOwnerUuid(), receiverUuid))
					continue;

				if (model.isContactOnline(receiverUuid))
					onlineUuids.add(receiverUuid);

			}

			if (onlineUuids.size() > 0) {

				dmsSendMessage(message, onlineUuids);

			}

		} else {
			// It's not my group, so I will send this message to the group owner only.

			String receiverUuid = group.getOwnerUuid();

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

		dmsSendMessage(message, () -> dmsClient.sendMessage(message.toJson(), receiverUuid, message.getId()));

	}

	private void dmsSendMessage(Message message, Iterable<String> receiverUuids) {

		dmsSendMessage(message, () -> dmsClient.sendMessage(message.toJson(), receiverUuids, message.getId()));

	}

	private void dmsSendMessage(Message message, Runnable runnable) {

		message.setMessageId(message.getId());

		switch (message.getMessageType()) {

		case FILE:
		case AUDIO:

			String messageContent = message.getContent();

			Path path = Paths.get(messageContent);

			try {

				byte[] fileBytes = Files.readAllBytes(path);

				message.setContent(
						new FilePojo(path.getFileName().toString(), Base64.getEncoder().encodeToString(fileBytes))
								.toJson());

			} catch (Exception e) {

				message.setContent("");

				e.printStackTrace();

			}

			runnable.run();

			message.setContent(messageContent);

			break;

		default:

			runnable.run();

			break;

		}

	}

	private void privateMessageReceived(Message message) throws Exception {

		switch (message.getMessageType()) {

		case TEXT:
		case FILE:
		case AUDIO:

			addPrivateMessageToPane(message, true);

			soundPlayer.playDuoTone();

			break;

		default:

			break;

		}

		listenerTaskQueue.execute(() -> {

			switch (message.getMessageType()) {

			case TEXT:

				listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(
						guiListener -> guiListener.messageReceived(new MessageHandleImpl(message.getMessageCode(),
								message.getContent(), message.getOwnerUuid(), null))));

				break;

			case FILE:

				listenerTaskQueue.execute(() -> dmsGuiListeners
						.forEach(guiListener -> guiListener.fileReceived(new FileHandleImpl(message.getMessageCode(),
								Paths.get(message.getContent()), message.getOwnerUuid(), null))));

				break;

			case AUDIO:

				listenerTaskQueue.execute(() -> dmsGuiListeners
						.forEach(guiListener -> guiListener.audioReceived(new FileHandleImpl(message.getMessageCode(),
								Paths.get(message.getContent()), message.getOwnerUuid(), null))));

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

			addGroupMessageToPane(message, true);

			soundPlayer.playTriTone();

			break;

		default:

			break;

		}

		listenerTaskQueue.execute(() -> {

			switch (message.getMessageType()) {

			case TEXT:

				listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(
						guiListener -> guiListener.messageReceived(new MessageHandleImpl(message.getMessageCode(),
								message.getContent(), message.getOwnerUuid(), message.getReceiverUuid()))));

				break;

			case FILE:

				listenerTaskQueue.execute(() -> dmsGuiListeners
						.forEach(guiListener -> guiListener.fileReceived(new FileHandleImpl(message.getMessageCode(),
								Paths.get(message.getContent()), message.getOwnerUuid(), message.getReceiverUuid()))));

				break;

			case AUDIO:

				listenerTaskQueue.execute(() -> dmsGuiListeners
						.forEach(guiListener -> guiListener.audioReceived(new FileHandleImpl(message.getMessageCode(),
								Paths.get(message.getContent()), message.getOwnerUuid(), message.getReceiverUuid()))));

				break;

			default:

				break;

			}

		});

	}

	private void updateMessageReceived(Message message) throws Exception {

		switch (message.getReceiverType()) {

		case PRIVATE:

			if (Objects.equals(message.getMessageCode(), CommonConstants.CODE_CANCEL_MESSAGE)) {

				Long messageId = Long.parseLong(message.getContent());

				Message dbMessage = dbManager.getMessage(message.getSenderUuid(), messageId);

				if (dbMessage != null)
					cancelMessage(dbMessage);

			}

			break;

		case GROUP:

			if (Objects.equals(message.getMessageCode(), CommonConstants.CODE_UPDATE_GROUP)) {

				GroupUpdate groupUpdate = GroupUpdate.fromJson(message.getContent());

				groupUpdateReceived(message.getReceiverUuid(), groupUpdate, message.getOwnerUuid());

			}

			break;

		}

	}

	private void groupUpdateReceived(final String groupUuid, final GroupUpdate groupUpdate, String ownerUuid)
			throws Exception {

		final Set<Contact> contactsToBeAdded = new HashSet<Contact>();
		final Set<Contact> contactsToBeRemoved = new HashSet<Contact>();

		if (groupUpdate.add != null) {

			groupUpdate.add.forEach((uuid, name) -> {

				if (Objects.equals(model.getLocalUuid(), uuid))
					return;

				Contact contact = model.getContact(uuid);
				if (contact == null) {
					contact = new Contact();
					contact.setUuid(uuid);
					contact.setName(name);
					contact.setStatus(Availability.OFFLINE);
					Contact newContact = dbManager.addUpdateContact(contact);
					model.addContact(newContact);
					Platform.runLater(() -> dmsPanel.updateContact(newContact));
					listenerTaskQueue.execute(() -> dmsListeners.forEach(listener -> listener
							.contactUpdated(new ContactHandleImpl(uuid, newContact.getName(), newContact.getComment(),
									newContact.getLattitude(), newContact.getLongitude(), false))));
					contactsToBeAdded.add(newContact);
				} else {
					contactsToBeAdded.add(contact);
				}

			});

		}

		if (groupUpdate.remove != null) {

			groupUpdate.remove.forEach((uuid, name) -> {

				if (Objects.equals(model.getLocalUuid(), uuid))
					return;

				Contact contact = model.getContact(uuid);
				if (contact == null) {
					contact = new Contact();
					contact.setUuid(uuid);
					contact.setName(name);
					contact.setStatus(Availability.OFFLINE);
					Contact newContact = dbManager.addUpdateContact(contact);
					model.addContact(newContact);
					Platform.runLater(() -> dmsPanel.updateContact(newContact));
					listenerTaskQueue.execute(() -> dmsListeners.forEach(listener -> listener
							.contactUpdated(new ContactHandleImpl(uuid, newContact.getName(), newContact.getComment(),
									newContact.getLattitude(), newContact.getLongitude(), false))));
					contactsToBeRemoved.add(newContact);
				} else {
					contactsToBeRemoved.add(contact);
				}

			});

		}

		final Dgroup newGroup = createUpdateGroup(groupUuid, groupUpdate.name, ownerUuid,
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

		case PRIVATE:

			return model.isMessagePaneOpen(message.getOwnerUuid()) ? MessageStatus.READ : MessageStatus.RECEIVED;

		case GROUP:

			return model.isMessagePaneOpen(message.getReceiverUuid()) ? MessageStatus.READ : MessageStatus.RECEIVED;

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

	private void updateMessageStatus(Message message, String[] remoteUuids, MessageStatus messageStatus,
			boolean resendIfNecessary) throws Exception {

		if (Objects.equals(message.getWaitStatus(), WaitStatus.CANCELED))
			return;

		String ownerUuid = message.getOwnerUuid();

		// Send this status to the original sender too.
		if (!Objects.equals(model.getLocalUuid(), ownerUuid) && model.isContactOnline(ownerUuid))
			dmsClient.feedMessageStatus(remoteUuids, ownerUuid, message.getMessageId(), messageStatus);

		switch (message.getReceiverType()) {

		case PRIVATE:

		{

			for (String remoteUuid : remoteUuids) {

				message.setMessageStatus(messageStatus);

				message.setWaitStatus(
						Objects.equals(messageStatus, MessageStatus.READ) ? WaitStatus.DONE : WaitStatus.WAITING);

				final Message newMessage = dbManager.addUpdateMessage(message);

				if (!Objects.equals(newMessage.getMessageType(), MessageType.UPDATE))
					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, remoteUuid));

				if (resendIfNecessary && Objects.equals(messageStatus, MessageStatus.FRESH))
					sendPrivateMessage(newMessage);

			}

			break;

		}

		case GROUP:

		{

			// This is a group message. I am either the group owner or the message sender.
			// If I am the group owner and the message is not received remotely, I will have
			// to re-send it.

			String groupUuid = message.getReceiverUuid();

			Dgroup group = model.getGroup(groupUuid);

			if (group == null)
				break;

			StatusReport statusReport = StatusReport.fromJson(message.getStatusReportStr());

			for (String remoteUuid : remoteUuids) {

				statusReport.uuidStatus.put(remoteUuid, messageStatus);

				if (Objects.equals(remoteUuid, group.getOwnerUuid()) && Objects.equals(ownerUuid, model.getLocalUuid()))
					message.setWaitStatus(
							Objects.equals(messageStatus, MessageStatus.READ) ? WaitStatus.DONE : WaitStatus.WAITING);

				if (Objects.equals(message.getId(), model.getDetailedGroupMessageId()))
					Platform.runLater(() -> dmsPanel.updateDetailedMessageStatus(remoteUuid, messageStatus));

			}

			message.setStatusReportStr(statusReport.toJson());

			MessageStatus overallMessageStatus = statusReport.getOverallStatus();

			if (Objects.equals(group.getOwnerUuid(), model.getLocalUuid()))
				message.setWaitStatus(Objects.equals(overallMessageStatus, MessageStatus.READ) ? WaitStatus.DONE
						: WaitStatus.WAITING);

			// If I am the owner, update the message status too
			if (Objects.equals(ownerUuid, model.getLocalUuid()))
				message.setMessageStatus(overallMessageStatus);

			final Message newMessage = dbManager.addUpdateMessage(message);

			if (!Objects.equals(newMessage.getMessageType(), MessageType.UPDATE))
				Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, groupUuid));

			if (resendIfNecessary && Objects.equals(messageStatus, MessageStatus.FRESH)) {
				// If the message is not received remotely and;
				// I am the group owner
				// or
				// I am the message owner and receiver is the group owner
				// then re-send this message (if flag set to true).

				for (String remoteUuid : remoteUuids) {

					if (Objects.equals(group.getOwnerUuid(), model.getLocalUuid())
							|| (Objects.equals(ownerUuid, model.getLocalUuid())
									&& Objects.equals(remoteUuid, group.getOwnerUuid())))
						sendGroupMessage(newMessage, remoteUuid);

				}

			}

			break;

		}

		}

	}

	private Message cancelMessage(Message message) throws Exception {

		if (Objects.equals(message.getWaitStatus(), WaitStatus.CANCELED))
			return message;

		message.setWaitStatus(WaitStatus.CANCELED);

		Message newMessage = dbManager.addUpdateMessage(message);

		if (model.isServerConnected())
			dmsClient.cancelMessage(message.getId());

		if (!Objects.equals(message.getReceiverType(), ReceiverType.GROUP))
			return newMessage;

		Dgroup group = model.getGroup(message.getReceiverUuid());

		String ownerUuid = group.getOwnerUuid();

		if (group == null || Objects.equals(ownerUuid, model.getLocalUuid()))
			return newMessage;

		sendPrivateMessage(createOutgoingMessage(String.valueOf(message.getId()), ownerUuid, null, ReceiverType.PRIVATE,
				MessageType.UPDATE, CommonConstants.CODE_CANCEL_MESSAGE));

		return newMessage;

	}

	@Override
	public void beaconReceived(String message) {

		taskQueue.execute(() -> {

			try {

				Contact incomingContact = Contact.fromJson(message);

				final String uuid = incomingContact.getUuid();
				boolean wasOnline = model.isContactOnline(uuid);

				final Contact newContact = dbManager.addUpdateContact(incomingContact);

				model.addContact(newContact);

				Platform.runLater(() -> dmsPanel.updateContact(newContact));

				listenerTaskQueue.execute(() -> dmsListeners
						.forEach(listener -> listener.contactUpdated(new ContactHandleImpl(uuid, newContact.getName(),
								newContact.getComment(), newContact.getLattitude(), newContact.getLongitude(), true))));

				if (!wasOnline) {

					// If the contact has just been online, send all things waiting for it, adjust
					// its groups' availability.
					taskQueue.execute(() -> {

						// START WITH PRIVATE MESSAGES
						try {

							for (Message waitingMessage : dbManager.getPrivateMessagesWaitingToContact(uuid)) {

								switch (waitingMessage.getMessageStatus()) {

								case FRESH:

									sendPrivateMessage(waitingMessage);

									break;

								case SENT:
								case RECEIVED:

									dmsClient.claimMessageStatus(waitingMessage.getId(), uuid);

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

							for (Message waitingMessage : dbManager
									.getGroupMessagesWaitingToContact(model.getLocalUuid(), uuid)) {

								StatusReport statusReport;

								try {
									statusReport = StatusReport.fromJson(waitingMessage.getStatusReportStr());
								} catch (Exception e) {
									continue;
								}

								MessageStatus messageStatus = statusReport.uuidStatus.get(uuid);

								switch (messageStatus) {

								case FRESH:

									sendGroupMessage(waitingMessage, uuid);

									break;

								case SENT:
								case RECEIVED:

									dmsClient.claimMessageStatus(waitingMessage.getId(), uuid);

									break;

								default:

									break;

								}

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

						// CLAIM WAITING STATUS REPORTS
						try {

							for (Message waitingMessage : dbManager
									.getGroupMessagesNotReadToItsGroup(model.getLocalUuid(), uuid)) {

								dmsClient.claimStatusReport(waitingMessage.getId(), uuid);

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

						// MODIFY GROUP STATUS
						try {

							for (Dgroup group : dbManager.getAllActiveGroupsOfUuid(uuid)) {

								group.setStatus(Availability.LIMITED);

								try {

									final Dgroup newGroup = dbManager.addUpdateGroup(group);

									model.addGroup(newGroup);

									Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

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
	public void progressReceived(final Long messageId, final String[] remoteUuids, final int progress) {
		// messageId = local database id of the message (not the message id, which is
		// the local
		// database id of the sender)

		if (messageId == null)
			return;

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessage(messageId);

				if (message == null)
					return;

				if (progress == 100) {

					// Update status in database and view; send update to message owner if necessary

					updateMessageStatus(message, remoteUuids, MessageStatus.SENT, false);

				}

				// Update view only

				if (Objects.equals(message.getMessageType(), MessageType.UPDATE)
						|| !Objects.equals(message.getOwnerUuid(), model.getLocalUuid()))
					return;

				switch (message.getReceiverType()) {

				case PRIVATE:

				{

					for (String uuid : remoteUuids) {

						model.storePrivateMessageProgress(uuid, messageId, progress);

						Platform.runLater(() -> dmsPanel.updatePrivateMessageProgress(uuid, messageId, progress));

					}

					break;

				}

				case GROUP:

				{

					for (String uuid : remoteUuids) {

						model.storeGroupMessageProgress(messageId, uuid, progress);

						if (Objects.equals(messageId, model.getDetailedGroupMessageId()))
							Platform.runLater(() -> dmsPanel.updateDetailedMessageProgress(uuid, progress));

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
	public void messageReceived(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				Message incomingMessage = Message.fromJson(message);

				incomingMessage.setSenderUuid(remoteUuid);

				Message dbMessage = dbManager.getMessage(incomingMessage.getSenderUuid(),
						incomingMessage.getMessageId());

				if (dbMessage != null)
					return;

				switch (incomingMessage.getMessageType()) {

				case FILE:
				case AUDIO:

					try {

						FilePojo filePojo = FilePojo.fromJson(incomingMessage.getContent());

						Path dstFolder = Paths.get(CommonConstants.RECEIVE_FOLDER).normalize().toAbsolutePath();

						String fileName = filePojo.fileName;

						Path dstFile = getDstFile(dstFolder, fileName);

						Files.write(dstFile, Base64.getDecoder().decode(filePojo.fileContent));

						incomingMessage.setContent(dstFile.toString());

					} catch (Exception e) {

						incomingMessage.setContent("");

						e.printStackTrace();

					}

					break;

				default:

					break;

				}

				incomingMessage.setMessageStatus(computeMessageStatus(incomingMessage));

				boolean messageToBeRedirected = false;

				if (Objects.equals(incomingMessage.getReceiverType(), ReceiverType.GROUP)) {

					Dgroup group = model.getGroup(incomingMessage.getReceiverUuid());

					if (group != null && Objects.equals(group.getOwnerUuid(), model.getLocalUuid())) {

						StatusReport statusReport = new StatusReport();

						group.getContacts().forEach(contact -> {

							if (Objects.equals(contact.getUuid(), incomingMessage.getOwnerUuid()))
								return;

							statusReport.uuidStatus.put(contact.getUuid(), MessageStatus.FRESH);

						});

						incomingMessage.setStatusReportStr(statusReport.toJson());

						messageToBeRedirected = statusReport.uuidStatus.size() > 0;

					}

				}

				incomingMessage.setWaitStatus(messageToBeRedirected ? WaitStatus.WAITING : WaitStatus.DONE);

				if (Objects.equals(incomingMessage.getMessageType(), MessageType.UPDATE))
					updateMessageReceived(incomingMessage);

				Message newMessage = dbManager.addUpdateMessage(incomingMessage);

				switch (newMessage.getReceiverType()) {

				case PRIVATE:

					privateMessageReceived(newMessage);

					break;

				case GROUP:

					groupMessageReceived(newMessage);

					break;

				}

				dmsClient.feedMessageStatus(model.getLocalUuid(), newMessage.getSenderUuid(), newMessage.getMessageId(),
						newMessage.getMessageStatus());

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

		Contact contact = model.getContact(uuid);

		if (contact == null)
			return;

		contactDisconnected(contact);

	}

	@Override
	public void serverConnStatusUpdated(boolean connStatus) {

		model.setServerConnStatus(connStatus);

		if (connStatus) {

			sendBeacon();

			dmsClient.claimAllBeacons();
			dmsClient.claimRemoteIps();

		} else {

			model.getContacts().forEach((uuid, contact) -> {

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

				Message incomingMessage = dbManager.getMessage(remoteUuid, messageId);

				if (incomingMessage == null) {

					// Not received
					dmsClient.feedMessageStatus(model.getLocalUuid(), remoteUuid, messageId, MessageStatus.FRESH);

				} else {

					dmsClient.feedMessageStatus(model.getLocalUuid(), remoteUuid, messageId,
							incomingMessage.getMessageStatus());

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messageStatusFed(final Long messageId, final MessageStatus messageStatus, final String[] remoteUuids) {

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessage(messageId);

				if (message == null)
					return;

				updateMessageStatus(message, remoteUuids, messageStatus, true);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusReportClaimed(final Long messageId, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				Message dbMessage = dbManager.getMessage(remoteUuid, messageId);

				if (dbMessage == null)
					return;

				StatusReport statusReport = StatusReport.fromJson(dbMessage.getStatusReportStr());

				dmsClient.feedStatusReport(messageId, statusReport.toJson(), remoteUuid);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusReportFed(final Long messageId, final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				StatusReport newStatusReport = StatusReport.fromJson(message);

				// A status report can only be claimed by a group message owner. So at this
				// point, I must be a group message owner. Let's find that message.

				Message dbMessage = dbManager.getMessage(messageId);

				if (dbMessage == null || Objects.equals(dbMessage.getWaitStatus(), WaitStatus.CANCELED))
					return;

				String groupUuid = dbMessage.getReceiverUuid();

				StatusReport statusReport = StatusReport.fromJson(dbMessage.getStatusReportStr());

				statusReport.uuidStatus.putAll(newStatusReport.uuidStatus);

				// I just update my db and view. I don't do anything else like re-sending the
				// message etc.

				dbMessage.setStatusReportStr(statusReport.toJson());
				dbMessage.setMessageStatus(statusReport.getOverallStatus());

				final Message newMessage = dbManager.addUpdateMessage(dbMessage);

				if (!Objects.equals(newMessage.getMessageType(), MessageType.UPDATE))
					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, groupUuid));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void transientMessageReceived(String message, String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				Message incomingMessage = Message.fromJson(message);

				switch (incomingMessage.getMessageType()) {

				case TEXT:

					final MessageHandle messageHandle = new MessageHandleImpl(incomingMessage.getMessageCode(),
							incomingMessage.getContent(), incomingMessage.getOwnerUuid(),
							Objects.equals(incomingMessage.getReceiverType(), ReceiverType.GROUP)
									? incomingMessage.getReceiverUuid()
									: null);

					listenerTaskQueue
							.execute(() -> dmsListeners.forEach(listener -> listener.messageReceived(messageHandle)));

					break;

				case FILE:

					try {

						FilePojo filePojo = FilePojo.fromJson(incomingMessage.getContent());

						Path dstFolder = Paths.get(CommonConstants.RECEIVE_FOLDER).normalize().toAbsolutePath();

						String fileName = filePojo.fileName;

						Path dstFile = getDstFile(dstFolder, fileName);

						Files.write(dstFile, Base64.getDecoder().decode(filePojo.fileContent));

						final FileHandle fileHandle = new FileHandleImpl(incomingMessage.getMessageCode(), dstFile,
								incomingMessage.getOwnerUuid(),
								Objects.equals(incomingMessage.getReceiverType(), ReceiverType.GROUP)
										? incomingMessage.getReceiverUuid()
										: null);

						listenerTaskQueue
								.execute(() -> dmsListeners.forEach(listener -> listener.fileReceived(fileHandle)));

					} catch (Exception e) {

						e.printStackTrace();

					}

					break;

				case OBJECT:

					final ObjectHandle objectHandle = new ObjectHandleImpl(incomingMessage.getMessageCode(),
							incomingMessage.getContent(), incomingMessage.getOwnerUuid(),
							Objects.equals(incomingMessage.getReceiverType(), ReceiverType.GROUP)
									? incomingMessage.getReceiverUuid()
									: null);

					listenerTaskQueue
							.execute(() -> dmsListeners.forEach(listener -> listener.objectReceived(objectHandle)));

					break;

				case LIST:

					final ListHandle listHandle = new ListHandleImpl(incomingMessage.getMessageCode(),
							incomingMessage.getContent(), incomingMessage.getOwnerUuid(),
							Objects.equals(incomingMessage.getReceiverType(), ReceiverType.GROUP)
									? incomingMessage.getReceiverUuid()
									: null);

					listenerTaskQueue
							.execute(() -> dmsListeners.forEach(listener -> listener.listReceived(listHandle)));

					break;

				default:

					break;

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void commentUpdated(String comment) {

		taskQueue.execute(() -> {

			try {

				Identity identity = model.getIdentity();

				if (Objects.equals(comment, identity.getComment()))
					return;

				identity.setComment(comment);

				Identity newIdentity = dbManager.updateIdentity(identity);

				model.updateComment(comment);

				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));

				sendBeacon();

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void updateStatusClicked() {

		taskQueue.execute(() -> {

			try {

				Identity identity = model.getIdentity();

				if (Objects.equals(identity.getStatus(), Availability.AVAILABLE)) {

					identity.setStatus(Availability.BUSY);

				} else if (Objects.equals(identity.getStatus(), Availability.BUSY)) {

					identity.setStatus(Availability.AVAILABLE);

				}

				Identity newIdentity = dbManager.updateIdentity(identity);

				model.updateStatus(newIdentity.getStatus());

				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));

				sendBeacon();

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messagePaneOpened(final String uuid, final ReceiverType receiverType) {

		taskQueue.execute(() -> {

			model.messagePaneOpened(uuid);

			switch (receiverType) {

			case PRIVATE:

				contactMessagePaneOpened(uuid);

				break;

			case GROUP:

				groupMessagePaneOpened(uuid);

				break;

			}

		});

	}

	private void contactMessagePaneOpened(final String uuid) {

		try {

			List<Message> messagesWaitingFromContact = dbManager.getPrivateMessagesWaitingFromContact(uuid);

			for (Message incomingMessage : messagesWaitingFromContact) {

				try {

					incomingMessage.setMessageStatus(MessageStatus.READ);

					final Message newMessage = dbManager.addUpdateMessage(incomingMessage);

					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, uuid));

					dmsClient.feedMessageStatus(model.getLocalUuid(), newMessage.getSenderUuid(),
							newMessage.getMessageId(), MessageStatus.READ);

				} catch (JsonSyntaxException | HibernateException e) {

					e.printStackTrace();

				}

			}

			Platform.runLater(() -> dmsPanel.scrollPaneToMessage(uuid,
					messagesWaitingFromContact.size() > 0 ? messagesWaitingFromContact.get(0).getId() : -1L,
					ReceiverType.PRIVATE));

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

	private void groupMessagePaneOpened(String groupUuid) {

		try {

			List<Message> messagesWaitingFromGroup = dbManager.getMessagesWaitingFromGroup(model.getLocalUuid(),
					groupUuid);

			for (Message incomingMessage : messagesWaitingFromGroup) {

				try {

					incomingMessage.setMessageStatus(MessageStatus.READ);

					final Message newMessage = dbManager.addUpdateMessage(incomingMessage);

					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, groupUuid));

					Dgroup group = model.getGroup(groupUuid);

					if (group == null)
						return;

					dmsClient.feedMessageStatus(model.getLocalUuid(), newMessage.getSenderUuid(),
							newMessage.getMessageId(), newMessage.getMessageStatus());

				} catch (JsonSyntaxException | HibernateException e) {

					e.printStackTrace();

				}

			}

			Platform.runLater(() -> dmsPanel.scrollPaneToMessage(groupUuid,
					messagesWaitingFromGroup.size() > 0 ? messagesWaitingFromGroup.get(0).getId() : -1L,
					ReceiverType.GROUP));

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void messagePaneClosed(final String uuid) {

		taskQueue.execute(() -> {

			model.messagePaneClosed(uuid);

		});

	}

	@Override
	public void sendMessageClicked(final String messageTxt, final String receiverUuid,
			final ReceiverType receiverType) {

		taskQueue.execute(() -> {

			switch (receiverType) {

			case PRIVATE:

				sendPrivateMessageClicked(messageTxt, receiverUuid);

				break;

			case GROUP:

				sendGroupMessageClicked(messageTxt, receiverUuid);

				break;

			}

		});

	}

	private void sendPrivateMessageClicked(final String messageTxt, final String receiverUuid) {

		try {

			Message newMessage = createOutgoingMessage(messageTxt, receiverUuid, null, ReceiverType.PRIVATE,
					MessageType.TEXT, null);

			addPrivateMessageToPane(newMessage, true);

			sendPrivateMessage(newMessage);

			listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(guiListener -> guiListener.messageSent(
					new MessageHandleImpl(newMessage.getMessageCode(), newMessage.getContent(), receiverUuid, null))));

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private void sendGroupMessageClicked(String messageTxt, String groupUuid) {

		try {

			Message newMessage = createOutgoingMessage(messageTxt, groupUuid,
					createStatusReportStr(model.getGroup(groupUuid)), ReceiverType.GROUP, MessageType.TEXT, null);

			addGroupMessageToPane(newMessage, true);

			sendGroupMessage(newMessage);

			listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(guiListener -> guiListener.messageSent(
					new MessageHandleImpl(newMessage.getMessageCode(), newMessage.getContent(), null, groupUuid))));

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	@Override
	public void showFoldersClicked(final String uuid, final ReceiverType receiverType) {

		taskQueue.execute(() -> {

			model.setReferenceUuid(new SimpleEntry<ReceiverType, String>(receiverType, uuid));

		});

	}

	@Override
	public void paneScrolledToTop(final String uuid, final ReceiverType receiverType) {

		taskQueue.execute(() -> {

			switch (receiverType) {

			case PRIVATE:

				contactPaneScrolledToTop(uuid);

				break;

			case GROUP:

				groupPaneScrolledToTop(uuid);

				break;

			}

		});

	}

	private void contactPaneScrolledToTop(final String uuid) {

		Long previousMinMessageId = model.getMinMessageId(uuid);

		if (previousMinMessageId < 0)
			return;

		List<Message> lastMessagesBeforeId = dbManager.getLastPrivateMessagesBeforeId(model.getLocalUuid(), uuid,
				previousMinMessageId, MIN_MESSAGES_PER_PAGE);

		if (lastMessagesBeforeId.size() == 0)
			return;

		Platform.runLater(() -> dmsPanel.savePosition(uuid, previousMinMessageId, ReceiverType.PRIVATE));

		lastMessagesBeforeId.forEach(message -> addPrivateMessageToPane(message, false));

		Platform.runLater(() -> dmsPanel.scrollToSavedPosition(uuid, ReceiverType.PRIVATE));

	}

	private void groupPaneScrolledToTop(String groupUuid) {

		Long previousMinMessageId = model.getMinMessageId(groupUuid);

		if (previousMinMessageId < 0)
			return;

		List<Message> lastMessagesBeforeId = dbManager.getLastGroupMessagesBeforeId(groupUuid, previousMinMessageId,
				MIN_MESSAGES_PER_PAGE);

		if (lastMessagesBeforeId.size() == 0)
			return;

		Platform.runLater(() -> dmsPanel.savePosition(groupUuid, previousMinMessageId, ReceiverType.GROUP));

		lastMessagesBeforeId.forEach(message -> addGroupMessageToPane(message, false));

		Platform.runLater(() -> dmsPanel.scrollToSavedPosition(groupUuid, ReceiverType.GROUP));

	}

	@Override
	public void showAddUpdateGroupClicked(String groupUuid) {

		Dgroup group = model.getGroup(groupUuid);

		model.setGroupToBeUpdated(group);

		if (group == null) {
			// New group

			Platform.runLater(() -> dmsPanel.showAddUpdateGroupPane(null, null, true));

		} else {
			// Update group

			Set<String> uuids = new HashSet<String>();
			group.getContacts().forEach(contact -> uuids.add(contact.getUuid()));

			Platform.runLater(() -> dmsPanel.showAddUpdateGroupPane(group.getName(), uuids, false));

		}

	}

	@Override
	public void addUpdateGroupRequested(String groupName, Set<String> selectedUuids) {

		taskQueue.execute(() -> {

			Dgroup group = model.getGroupToBeUpdated();
			model.setGroupToBeUpdated(null);

			Set<Contact> selectedContacts = new HashSet<Contact>();
			selectedUuids.forEach(uuid -> {
				Contact contact = model.getContact(uuid);
				if (contact != null)
					selectedContacts.add(contact);
			});

			if (group == null) {
				// New group

				try {

					final Dgroup newGroup = createUpdateGroup(null, groupName, model.getLocalUuid(), true,
							selectedContacts, null);

					if (newGroup == null)
						return;

					model.addGroup(newGroup);

					Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

					// Gruba eleman ekleme mesajini olusturup grup uyelerine gonder

					String groupUpdate = getGroupUpdate(newGroup.getName(), true, newGroup.getContacts(), null);

					sendGroupMessage(
							createOutgoingMessage(groupUpdate, newGroup.getUuid(), createStatusReportStr(newGroup),
									ReceiverType.GROUP, MessageType.UPDATE, CommonConstants.CODE_UPDATE_GROUP));

				} catch (Exception e) {

					e.printStackTrace();

				}

			} else {
				// Update group

				Set<Contact> residentContacts = new HashSet<Contact>(group.getContacts());
				Set<Contact> contactsToBeAdded = new HashSet<Contact>(selectedContacts);
				Set<Contact> contactsToBeRemoved = new HashSet<Contact>(group.getContacts());

				contactsToBeAdded.removeAll(residentContacts);
				contactsToBeRemoved.removeAll(selectedContacts);
				residentContacts.removeAll(contactsToBeRemoved);

				// The group hasn't changed, nothing to do:
				if (Objects.equals(group.getName(), groupName) && contactsToBeAdded.isEmpty()
						&& contactsToBeRemoved.isEmpty())
					return;

				String newGroupName = Objects.equals(group.getName(), groupName) ? null : groupName;

				try {

					Dgroup newGroup = createUpdateGroup(group.getUuid(), newGroupName, group.getOwnerUuid(),
							group.isActive(), contactsToBeAdded, contactsToBeRemoved);

					if (newGroup == null)
						return;

					model.addGroup(newGroup);

					Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

					if (!contactsToBeAdded.isEmpty()) {

						String groupUpdateToAddedContacts = getGroupUpdate(newGroup.getName(), true,
								newGroup.getContacts(), null);

						sendGroupMessage(createOutgoingMessage(groupUpdateToAddedContacts, newGroup.getUuid(),
								createStatusReportStr(contactsToBeAdded), ReceiverType.GROUP, MessageType.UPDATE,
								CommonConstants.CODE_UPDATE_GROUP), contactsToBeAdded);

					}

					if (!contactsToBeRemoved.isEmpty()) {

						String groupUpdateToRemovedContacts = getGroupUpdate(null, false, null, null);

						sendGroupMessage(createOutgoingMessage(groupUpdateToRemovedContacts, newGroup.getUuid(),
								createStatusReportStr(contactsToBeRemoved), ReceiverType.GROUP, MessageType.UPDATE,
								CommonConstants.CODE_UPDATE_GROUP), contactsToBeRemoved);

					}

					if (!residentContacts.isEmpty()) {

						String groupUpdateToResidentContacts = getGroupUpdate(newGroupName, true, contactsToBeAdded,
								contactsToBeRemoved);

						sendGroupMessage(createOutgoingMessage(groupUpdateToResidentContacts, newGroup.getUuid(),
								createStatusReportStr(residentContacts), ReceiverType.GROUP, MessageType.UPDATE,
								CommonConstants.CODE_UPDATE_GROUP), residentContacts);

					}

				} catch (Exception e) {

					e.printStackTrace();

				}

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

			Set<Contact> contacts = new HashSet<Contact>(group.getContacts());

			try {

				Dgroup newGroup = createUpdateGroup(group.getUuid(), null, null, group.isActive(), null, null);

				if (newGroup == null)
					return;

				model.addGroup(newGroup);

				Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

				String groupUpdate = getGroupUpdate(null, newGroup.isActive(), null, null);

				sendGroupMessage(createOutgoingMessage(groupUpdate, newGroup.getUuid(), createStatusReportStr(contacts),
						ReceiverType.GROUP, MessageType.UPDATE, CommonConstants.CODE_UPDATE_GROUP), contacts);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void showFoldersCanceled() {

		taskQueue.execute(() -> {

			model.setReferenceUuid(null);

		});

	}

	@Override
	public void fileSelected(Path file) {

		taskQueue.execute(() -> {

			Entry<ReceiverType, String> referenceUuid = model.getReferenceUuid();
			model.setReferenceUuid(null);

			if (referenceUuid == null)
				return;

			try {

				Path srcFile = file;
				Path dstFolder = Paths.get(CommonConstants.SEND_FOLDER).normalize().toAbsolutePath();

				String fileName = srcFile.getFileName().toString();

				Path dstFile = getDstFile(dstFolder, fileName);

				Files.copy(srcFile, dstFile);

				// Now to the send operations

				switch (referenceUuid.getKey()) {

				case PRIVATE:

				{

					String contactUuid = referenceUuid.getValue();

					Message newMessage = createOutgoingMessage(dstFile.toString(), contactUuid, null,
							ReceiverType.PRIVATE, MessageType.FILE, null);

					addPrivateMessageToPane(newMessage, true);

					sendPrivateMessage(newMessage);

					listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(guiListener -> guiListener
							.fileSent(new FileHandleImpl(newMessage.getMessageCode(), dstFile, contactUuid, null))));

					break;

				}

				case GROUP:

				{

					String groupUuid = referenceUuid.getValue();

					Message newMessage = createOutgoingMessage(dstFile.toString(), groupUuid,
							createStatusReportStr(model.getGroup(groupUuid)), ReceiverType.GROUP, MessageType.FILE,
							null);

					addGroupMessageToPane(newMessage, true);

					sendGroupMessage(newMessage);

					listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(guiListener -> guiListener
							.fileSent(new FileHandleImpl(newMessage.getMessageCode(), dstFile, null, groupUuid))));

					break;

				}

				default:

					break;

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

				Message message = dbManager.getMessage(messageId);

				if (Objects.equals(message.getMessageType(), MessageType.FILE)) {

					Path file = Paths.get(message.getContent());

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

				Message message = dbManager.getMessage(messageId);

				if (message == null || !Objects.equals(message.getReceiverType(), ReceiverType.GROUP)
						|| !Objects.equals(message.getOwnerUuid(), model.getLocalUuid()))
					return;

				Dgroup group = model.getGroup(message.getReceiverUuid());

				if (group == null)
					return;

				model.setDetailedGroupMessageId(messageId);

				StatusReport statusReport = StatusReport.fromJson(message.getStatusReportStr());

				List<Contact> contacts = new ArrayList<Contact>();

				statusReport.uuidStatus.forEach((uuid, messageStatus) -> {

					if (Objects.equals(uuid, group.getOwnerUuid()))
						return;

					Contact contact = model.getContact(uuid);

					if (contact == null)
						return;

					contacts.add(contact);

				});

				contacts.sort(model.getContactSorter());

				if (statusReport.uuidStatus.containsKey(group.getOwnerUuid())) {

					Contact contact = model.getContact(group.getOwnerUuid());

					if (contact != null)
						contacts.add(0, contact);

				}

				Platform.runLater(() -> dmsPanel.showStatusInfoPane(contacts));

				statusReport.uuidStatus.forEach((uuid, messageStatus) -> Platform
						.runLater(() -> dmsPanel.updateDetailedMessageStatus(uuid, messageStatus)));

				if (model.getGroupMessageProgresses(messageId) != null)
					model.getGroupMessageProgresses(messageId).forEach((uuid, progress) -> Platform
							.runLater(() -> dmsPanel.updateDetailedMessageProgress(uuid, progress)));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void cancelClicked(Long messageId) {

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessage(messageId);

				Message newMessage = cancelMessage(message);

				Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, newMessage.getReceiverUuid()));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusInfoClosed() {

		taskQueue.execute(() -> {

			model.setDetailedGroupMessageId(0);

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
	public void recordButtonPressed(final String uuid, final ReceiverType receiverType) {

		taskQueue.execute(() -> {

			try {

				audioCenter.prepareRecording();

				model.setReferenceUuid(new SimpleEntry<ReceiverType, String>(receiverType, uuid));

				Platform.runLater(() -> dmsPanel.recordingStarted(uuid, receiverType));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void recordEventTriggered() {

		taskQueue.execute(() -> {

			Entry<ReceiverType, String> referenceUuid = model.getReferenceUuid();

			if (referenceUuid == null)
				return;

			try {

				String fileName = String.format("audio_%s.wav",
						CommonConstants.DATE_TIME_FORMATTER.format(LocalDateTime.now()));
				Path dstFolder = Paths.get(CommonConstants.SEND_FOLDER).normalize().toAbsolutePath();

				Path dstFile = getDstFile(dstFolder, fileName);

				audioCenter.startRecording(new RecordObject(dstFile, referenceUuid.getValue(), referenceUuid.getKey()));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void recordButtonReleased() {

		taskQueue.execute(() -> {

			audioCenter.stopRecording();

			Entry<ReceiverType, String> referenceUuid = model.getReferenceUuid();
			model.setReferenceUuid(null);

			if (referenceUuid == null)
				return;

			switch (referenceUuid.getKey()) {

			case PRIVATE:

			{

				String uuid = referenceUuid.getValue();

				Platform.runLater(() -> dmsPanel.recordingStopped(uuid, ReceiverType.PRIVATE));

				break;

			}

			case GROUP:

			{

				String groupUuid = referenceUuid.getValue();

				Platform.runLater(() -> dmsPanel.recordingStopped(groupUuid, ReceiverType.GROUP));

				break;

			}

			default:

				break;

			}

		});

	}

	@Override
	public void recordingStopped(RecordObject recordObject) {

		taskQueue.execute(() -> {

			boolean recordSuccessful = recordObject.path != null && Files.exists(recordObject.path);

			try {

				switch (recordObject.receiverType) {

				case PRIVATE:

				{

					String uuid = recordObject.uuid;

					Platform.runLater(() -> dmsPanel.recordingStopped(uuid, ReceiverType.PRIVATE));

					if (!recordSuccessful)
						break;

					Message newMessage = createOutgoingMessage(recordObject.path.toString(), uuid, null,
							ReceiverType.PRIVATE, MessageType.AUDIO, null);

					addPrivateMessageToPane(newMessage, true);

					sendPrivateMessage(newMessage);

					listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(guiListener -> guiListener.audioSent(
							new FileHandleImpl(newMessage.getMessageCode(), recordObject.path, uuid, null))));

					break;

				}

				case GROUP:

				{

					String groupUuid = recordObject.uuid;

					Platform.runLater(() -> dmsPanel.recordingStopped(groupUuid, ReceiverType.GROUP));

					if (!recordSuccessful)
						break;

					Message newMessage = createOutgoingMessage(recordObject.path.toString(), groupUuid,
							createStatusReportStr(model.getGroup(groupUuid)), ReceiverType.GROUP, MessageType.AUDIO,
							null);

					addGroupMessageToPane(newMessage, true);

					sendGroupMessage(newMessage);

					listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(guiListener -> guiListener.audioSent(
							new FileHandleImpl(newMessage.getMessageCode(), recordObject.path, null, groupUuid))));

					break;

				}

				default:

					break;

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void reportClicked(final String uuid, final ReceiverType receiverType) {

		taskQueue.execute(() -> {

			model.setReferenceUuid(new SimpleEntry<ReceiverType, String>(receiverType, uuid));

			Platform.runLater(() -> reportsDialog.display());

		});

	}

	@Override
	public void sendReportClicked(String reportHeading, List<String> reportParagraphs) {

		taskQueue.execute(() -> {

			Platform.runLater(() -> reportsDialog.hideAndReset());

			Entry<ReceiverType, String> referenceUuid = model.getReferenceUuid();
			model.setReferenceUuid(null);

			if (referenceUuid == null)
				return;

			try {

				String fileName = String.format("%s_%s.pdf", reportHeading,
						CommonConstants.DATE_TIME_FORMATTER.format(LocalDateTime.now()));
				Path dstFolder = Paths.get(CommonConstants.SEND_FOLDER).normalize().toAbsolutePath();

				Path dstFile = getDstFile(dstFolder, fileName);

				CommonMethods.writeReport(dstFile, reportHeading, reportParagraphs);

				boolean writeSuccessful = dstFile != null && Files.exists(dstFile);

				if (!writeSuccessful)
					return;

				try {

					switch (referenceUuid.getKey()) {

					case PRIVATE:

					{

						String uuid = referenceUuid.getValue();

						Message newMessage = createOutgoingMessage(dstFile.toString(), uuid, null, ReceiverType.PRIVATE,
								MessageType.FILE, CommonConstants.CODE_REPORT);

						addPrivateMessageToPane(newMessage, true);

						sendPrivateMessage(newMessage);

						listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(guiListener -> guiListener
								.fileSent(new FileHandleImpl(newMessage.getMessageCode(), dstFile, uuid, null))));

						break;

					}

					case GROUP:

					{

						String groupUuid = referenceUuid.getValue();

						Message newMessage = createOutgoingMessage(dstFile.toString(), groupUuid,
								createStatusReportStr(model.getGroup(groupUuid)), ReceiverType.GROUP, MessageType.FILE,
								CommonConstants.CODE_REPORT);

						addGroupMessageToPane(newMessage, true);

						sendGroupMessage(newMessage);

						listenerTaskQueue.execute(() -> dmsGuiListeners.forEach(guiListener -> guiListener
								.fileSent(new FileHandleImpl(newMessage.getMessageCode(), dstFile, null, groupUuid))));

						break;

					}

					default:

						break;

					}

				} catch (Exception e) {

					e.printStackTrace();

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void cancelReportClicked() {

		taskQueue.execute(() -> {

			Platform.runLater(() -> reportsDialog.hideAndReset());

			model.setReferenceUuid(null);

		});

	}

	@Override
	public JComponent getDmsPanel() {

		return dmsPanelSwing;

	}

	@Override
	public void addListener(DmsListener listener) {

		taskQueue.execute(() -> {

			dmsListeners.add(listener);

			model.getContacts().forEach((uuid, contact) -> {

				listener.contactUpdated(
						new ContactHandleImpl(uuid, contact.getName(), contact.getComment(), contact.getLattitude(),
								contact.getLongitude(), !Objects.equals(contact.getStatus(), Availability.OFFLINE)));

			});

		});

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

				Identity identity = model.getIdentity();

				if (Objects.equals(lattitude, identity.getLattitude())
						&& Objects.equals(longitude, identity.getLongitude()))
					return;

				identity.setLattitude(lattitude);
				identity.setLongitude(longitude);

				Identity newIdentity = dbManager.updateIdentity(identity);

				model.updateCoordinates(lattitude, longitude);

				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));

				sendBeacon();

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void setComment(String comment) {

		Platform.runLater(() -> dmsPanel.setCommentEditable(false));

		commentUpdated(comment);

	}

	@Override
	public ContactHandle getMyContactHandle() {

		Identity identity = model.getIdentity();

		return new ContactHandleImpl(identity.getUuid(), identity.getName(), identity.getComment(),
				identity.getLattitude(), identity.getLongitude(), true);

	}

	@Override
	public GroupSelectionHandle getMyActiveGroupsHandle() {

		return myActiveGroupsHandle;

	}

	@Override
	public ContactSelectionHandle getOnlineContactsHandle() {

		return onlineContactsHandle;

	}

	@Override
	public ContactHandle getContactHandle(String uuid) {

		Contact contact = model.getContact(uuid);

		if (contact == null)
			return null;

		return new ContactHandleImpl(contact.getUuid(), contact.getName(), contact.getComment(), contact.getLattitude(),
				contact.getLongitude(), !Objects.equals(contact.getStatus(), Availability.OFFLINE));

	}

	@Override
	public GroupHandle getGroupHandle(String groupUuid) {

		Dgroup group = model.getGroup(groupUuid);

		if (group == null)
			return null;

		List<String> contactUuids = new ArrayList<String>();
		group.getContacts().forEach(contact -> contactUuids.add(contact.getUuid()));

		return new GroupHandleImpl(group.getUuid(), group.getName(), group.getComment(), contactUuids);

	}

	@Override
	public boolean sendMessageToContacts(String message, Integer messageCode, List<String> contactUuids) {

		if (!model.isServerConnected())
			return false;

		Message outgoingMessage = new Message(model.getLocalUuid(), null, ReceiverType.PRIVATE, MessageType.TEXT,
				message);

		outgoingMessage.setMessageCode(messageCode);

		dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids);

		return true;

	}

	@Override
	public boolean sendMessageToGroup(String message, Integer messageCode, String groupUuid) {

		if (!model.isServerConnected())
			return false;

		Dgroup group = model.getGroup(groupUuid);

		if (group == null)
			return false;

		Message outgoingMessage = new Message(model.getLocalUuid(), groupUuid, ReceiverType.GROUP, MessageType.TEXT,
				message);

		outgoingMessage.setMessageCode(messageCode);

		List<String> contactUuids = new ArrayList<String>();

		group.getContacts().forEach(contact -> contactUuids.add(contact.getUuid()));

		dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids);

		return true;

	}

	@Override
	public boolean sendObjectToContacts(Object object, Integer objectCode, List<String> contactUuids) {

		if (!model.isServerConnected())
			return false;

		Message outgoingMessage = new Message(model.getLocalUuid(), null, ReceiverType.PRIVATE, MessageType.OBJECT,
				CommonMethods.toJson(object));

		outgoingMessage.setMessageCode(objectCode);

		dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids);

		return true;

	}

	@Override
	public boolean sendObjectToGroup(Object object, Integer objectCode, String groupUuid) {

		if (!model.isServerConnected())
			return false;

		Dgroup group = model.getGroup(groupUuid);

		if (group == null)
			return false;

		Message outgoingMessage = new Message(model.getLocalUuid(), groupUuid, ReceiverType.GROUP, MessageType.OBJECT,
				CommonMethods.toJson(object));

		outgoingMessage.setMessageCode(objectCode);

		List<String> contactUuids = new ArrayList<String>();

		group.getContacts().forEach(contact -> contactUuids.add(contact.getUuid()));

		dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids);

		return true;

	}

	@Override
	public <T> boolean sendListToContacts(List<T> list, Class<T> elementType, Integer listCode,
			List<String> contactUuids) {

		if (!model.isServerConnected())
			return false;

		Message outgoingMessage = new Message(model.getLocalUuid(), null, ReceiverType.PRIVATE, MessageType.LIST,
				CommonMethods.convertListJsonToCommon(CommonMethods.toJson(list), elementType.getSimpleName()));

		outgoingMessage.setMessageCode(listCode);

		dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids);

		return true;

	}

	@Override
	public <T> boolean sendListToGroup(List<T> list, Class<T> elementType, Integer listCode, String groupUuid) {

		if (!model.isServerConnected())
			return false;

		Dgroup group = model.getGroup(groupUuid);

		if (group == null)
			return false;

		Message outgoingMessage = new Message(model.getLocalUuid(), groupUuid, ReceiverType.GROUP, MessageType.LIST,
				CommonMethods.convertListJsonToCommon(CommonMethods.toJson(list), elementType.getSimpleName()));

		outgoingMessage.setMessageCode(listCode);

		List<String> contactUuids = new ArrayList<String>();

		group.getContacts().forEach(contact -> contactUuids.add(contact.getUuid()));

		dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids);

		return true;

	}

	@Override
	public boolean sendFileToContacts(Path path, Integer fileCode, List<String> contactUuids) {

		if (!model.isServerConnected())
			return false;

		try {

			byte[] fileBytes = Files.readAllBytes(path);

			Message outgoingMessage = new Message(model.getLocalUuid(), null, ReceiverType.PRIVATE, MessageType.FILE,
					new FilePojo(path.getFileName().toString(), Base64.getEncoder().encodeToString(fileBytes))
							.toJson());

			outgoingMessage.setMessageCode(fileCode);

			dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return true;

	}

	@Override
	public boolean sendFileToGroup(Path path, Integer fileCode, String groupUuid) {

		if (!model.isServerConnected())
			return false;

		Dgroup group = model.getGroup(groupUuid);

		if (group == null)
			return false;

		try {

			byte[] fileBytes = Files.readAllBytes(path);

			Message outgoingMessage = new Message(model.getLocalUuid(), groupUuid, ReceiverType.GROUP, MessageType.FILE,
					new FilePojo(path.getFileName().toString(), Base64.getEncoder().encodeToString(fileBytes))
							.toJson());

			outgoingMessage.setMessageCode(fileCode);

			List<String> contactUuids = new ArrayList<String>();

			group.getContacts().forEach(contact -> contactUuids.add(contact.getUuid()));

			dmsClient.sendTransientMessage(outgoingMessage.toJson(), contactUuids);

			return true;

		} catch (Exception e) {

			e.printStackTrace();

		}

		return false;

	}

	private class DmsEntity {

		private final String uuid;
		private final ReceiverType receiverType;
		private final List<Message> messages = new ArrayList<Message>();

		private DmsEntity(String uuid, ReceiverType receiverType) {

			this.uuid = uuid;
			this.receiverType = receiverType;

		}

	}

}

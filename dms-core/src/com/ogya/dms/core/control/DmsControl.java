package com.ogya.dms.core.control;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.core.database.DbManager;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.ContactRef;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.dmsclient.DmsClient;
import com.ogya.dms.core.dmsclient.intf.DmsClientListener;
import com.ogya.dms.core.factory.DmsFactory;
import com.ogya.dms.core.intf.DmsHandle;
import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.ContactSelectionHandle;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.GroupSelectionHandle;
import com.ogya.dms.core.intf.handles.ListHandle;
import com.ogya.dms.core.intf.handles.MessageHandle;
import com.ogya.dms.core.intf.handles.ObjectHandle;
import com.ogya.dms.core.intf.handles.impl.ActiveContactsHandleImpl;
import com.ogya.dms.core.intf.handles.impl.ActiveGroupsHandleImpl;
import com.ogya.dms.core.intf.handles.impl.ContactHandleImpl;
import com.ogya.dms.core.intf.handles.impl.FileHandleImpl;
import com.ogya.dms.core.intf.handles.impl.GroupHandleImpl;
import com.ogya.dms.core.intf.handles.impl.ListHandleImpl;
import com.ogya.dms.core.intf.handles.impl.MessageHandleImpl;
import com.ogya.dms.core.intf.handles.impl.ObjectHandleImpl;
import com.ogya.dms.core.intf.listeners.DmsDownloadListener;
import com.ogya.dms.core.intf.listeners.DmsFileServer;
import com.ogya.dms.core.intf.listeners.DmsGuiListener;
import com.ogya.dms.core.intf.listeners.DmsListener;
import com.ogya.dms.core.intf.tools.ContactId;
import com.ogya.dms.core.intf.tools.GroupId;
import com.ogya.dms.core.intf.tools.MessageRules;
import com.ogya.dms.core.intf.tools.impl.ContactIdImpl;
import com.ogya.dms.core.intf.tools.impl.GroupIdImpl;
import com.ogya.dms.core.intf.tools.impl.MessageRulesImpl;
import com.ogya.dms.core.model.Model;
import com.ogya.dms.core.structures.AttachmentType;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.ContactMap;
import com.ogya.dms.core.structures.DownloadPojo;
import com.ogya.dms.core.structures.FileBuilder;
import com.ogya.dms.core.structures.GroupMessageStatus;
import com.ogya.dms.core.structures.GroupUpdate;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.UpdateType;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.util.AudioCenter;
import com.ogya.dms.core.util.AudioCenter.AudioCenterListener;
import com.ogya.dms.core.util.CheckedSingleThreadExecutorService;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.util.SoundPlayer;
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

	private final Runnable logoutListener;

	private final DbManager dbManager;

	private final Model model;

	private final DmsPanel dmsPanel;
	private final JFXPanel dmsPanelSwing;

	private final ReportsDialog reportsDialog;

	private final GroupSelectionHandle activeGroupsHandle;
	private final ContactSelectionHandle activeContactsHandle;

	private final DmsClient dmsClient;

	private final Semaphore sessionLock = new Semaphore(1);

	private final AudioCenter audioCenter = new AudioCenter(this);
	private final SoundPlayer soundPlayer = new SoundPlayer();

	private final List<DmsListener> dmsListeners = Collections.synchronizedList(new ArrayList<DmsListener>());
	private final List<DmsGuiListener> dmsGuiListeners = Collections.synchronizedList(new ArrayList<DmsGuiListener>());
	private final List<DmsDownloadListener> dmsDownloadListeners = Collections
			.synchronizedList(new ArrayList<DmsDownloadListener>());
	private final AtomicReference<DmsFileServer> dmsFileServer = new AtomicReference<DmsFileServer>();

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutor();
	private final ExecutorService auxTaskQueue = DmsFactory.newSingleThreadExecutor();
	private final ExecutorService downloadTaskQueue = DmsFactory.newSingleThreadExecutor();
	private final CheckedSingleThreadExecutorService listenerTaskQueue = new CheckedSingleThreadExecutorService();

	public DmsControl(String username, String password, Runnable logoutListener) throws Exception {

		this.logoutListener = logoutListener;

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

		activeGroupsHandle = new ActiveGroupsHandleImpl(dmsPanel.getActiveGroupsPane());
		activeContactsHandle = new ActiveContactsHandleImpl(dmsPanel.getActiveContactsPane());

		//

		reportsDialog = new ReportsDialog(Commons.REPORT_TEMPLATES);

		reportsDialog.addReportsListener(this);

		//

		initDbModelAndGui();

		dmsClient = new DmsClient(identity.getUuid(), Commons.SERVER_IP, Commons.SERVER_PORT, this);

		//

		sessionLock.acquireUninterruptibly();

	}

	private void initDbModelAndGui() throws Exception {

		Platform.runLater(() -> {

			Scene dmsScene = new Scene(dmsPanel);
			dmsScene.getStylesheets().add("/resources/css/style.css");
			dmsPanelSwing.setScene(dmsScene);
			dmsPanel.setIdentity(model.getIdentity());

		});

		dbManager.fetchAllContacts().forEach(contact -> {

			if (contact.getStatus() != Availability.OFFLINE) {
				contact.setStatus(Availability.OFFLINE);
				try {
					contact = dbManager.addUpdateContact(contact);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			final Contact newContact = contact;
			Long id = newContact.getId();
			boolean entityDeleted = newContact.getViewStatus() == ViewStatus.DELETED;

			if (!entityDeleted)
				model.addUpdateContact(newContact);

			try {

				List<Message> firstMessages = new ArrayList<Message>(
						dbManager.getAllPrivateMessagesSinceFirstUnreadMessage(id));

				if (firstMessages.isEmpty()) {

					firstMessages.addAll(dbManager.getLastPrivateMessages(id, MIN_MESSAGES_PER_PAGE));

				} else if (firstMessages.size() < MIN_MESSAGES_PER_PAGE) {

					firstMessages.addAll(dbManager.getLastPrivateMessagesBeforeId(id, firstMessages.get(0).getId(),
							MIN_MESSAGES_PER_PAGE - firstMessages.size()));

				}

				if (entityDeleted && firstMessages.isEmpty())
					return;

				Platform.runLater(() -> dmsPanel.updateContact(newContact));

				model.registerMessages(firstMessages);

				if (firstMessages.size() < MIN_MESSAGES_PER_PAGE)
					Platform.runLater(() -> dmsPanel.allMessagesLoaded(newContact.getEntityId()));

				firstMessages.forEach(message -> addMessageToPane(message, false));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

		dbManager.fetchAllGroups().forEach(group -> {

			if (group.getStatus() == Availability.LIMITED) {
				group.setStatus(Availability.OFFLINE);
				try {
					group = dbManager.addUpdateGroup(group);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			final Dgroup newGroup = group;
			Long id = newGroup.getId();
			boolean entityDeleted = newGroup.getViewStatus() == ViewStatus.DELETED;

			if (!entityDeleted)
				model.addUpdateGroup(newGroup);

			try {

				List<Message> firstMessages = new ArrayList<Message>(
						dbManager.getAllGroupMessagesSinceFirstUnreadMessage(id));

				if (firstMessages.isEmpty()) {

					firstMessages.addAll(dbManager.getLastGroupMessages(id, MIN_MESSAGES_PER_PAGE));

				} else if (firstMessages.size() < MIN_MESSAGES_PER_PAGE) {

					firstMessages.addAll(dbManager.getLastGroupMessagesBeforeId(id, firstMessages.get(0).getId(),
							MIN_MESSAGES_PER_PAGE - firstMessages.size()));

				}

				if (entityDeleted && firstMessages.isEmpty())
					return;

				Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

				model.registerMessages(firstMessages);

				if (firstMessages.size() < MIN_MESSAGES_PER_PAGE)
					Platform.runLater(() -> dmsPanel.allMessagesLoaded(newGroup.getEntityId()));

				firstMessages.forEach(message -> addMessageToPane(message, false));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

		Platform.runLater(() -> dmsPanel.sortEntities());

		// ARCHIVED MESSAGES
		try {

			archivedMessagesLoaded(dbManager.getLastArchivedMessages(MIN_MESSAGES_PER_PAGE));

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private void addMessageToPane(final Message message, final boolean moveToTop) {

		Platform.runLater(() -> dmsPanel.addMessage(message, moveToTop));

	}

	private void updateMessageInPane(final Message message) {

		Platform.runLater(() -> dmsPanel.updateMessage(message));

	}

	private void archivedMessagesLoaded(List<Message> loadedArchivedMessages) {

		if (loadedArchivedMessages.isEmpty() || loadedArchivedMessages.size() % MIN_MESSAGES_PER_PAGE != 0) {
			Platform.runLater(() -> dmsPanel.allArchivedMessagesLoaded());
			model.setMinArchivedMessageId(-1L);
		} else {
			model.setMinArchivedMessageId(loadedArchivedMessages.get(loadedArchivedMessages.size() - 1).getId());
		}

		loadedArchivedMessages.forEach(message -> Platform.runLater(() -> dmsPanel.addUpdateArchivedMessage(message)));

	}

	private void sendBeacon(String name, String comment, Availability status, Double latitude, Double longitude,
			String secretId) {

		if (!model.isServerConnected())
			return;

		Beacon beacon = new Beacon(model.getLocalUuid(), name, comment, status == null ? null : status.index(),
				latitude, longitude, secretId);

		dmsClient.sendBeacon(beacon);

	}

	private void contactDisconnected(final Contact contact) {

		final Long id = contact.getId();

		try {

			dbManager.getAllActiveGroupsOfContact(id).forEach(group -> {

				if (group.getStatus() == Availability.OFFLINE)
					return;

				group.setStatus(Availability.OFFLINE);

				try {

					final Dgroup newGroup = dbManager.addUpdateGroup(group);

					model.addUpdateGroup(newGroup);

					Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

					dmsListeners.forEach(listener -> listenerTaskQueue
							.execute(() -> listener.groupUpdated(new GroupHandleImpl(newGroup))));

				} catch (Exception e) {

					e.printStackTrace();

				}

			});

		} catch (Exception e) {

			e.printStackTrace();

		}

		contact.setStatus(Availability.OFFLINE);

		try {

			final Contact newContact = dbManager.addUpdateContact(contact);

			newContact.setLocalRemoteServerIps(null);

			model.addUpdateContact(newContact);

			Platform.runLater(() -> dmsPanel.updateContact(newContact));

			dmsListeners.forEach(listener -> listenerTaskQueue
					.execute(() -> listener.contactUpdated(new ContactHandleImpl(newContact))));

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private Message createOutgoingMessage(String content, String attachmentName, Path attachmentPath,
			AttachmentType attachmentType, Message refMessage, Integer messageCode, Contact contact, Dgroup group,
			Set<StatusReport> statusReports, Boolean apiFlag) throws Exception {

		MessageStatus messageStatus = MessageStatus.FRESH;
		if (attachmentType != null && attachmentPath == null) {
			messageStatus = MessageStatus.PREP;
		}

		Message outgoingMessage = new Message(content, refMessage, messageCode, messageStatus, contact,
				model.getIdentity(), group, apiFlag);

		outgoingMessage.setLocal(true);

		outgoingMessage.setAttachmentType(attachmentType);
		outgoingMessage.setAttachmentName(attachmentName);
		if (attachmentPath != null)
			outgoingMessage.setAttachmentPath(attachmentPath.toString());

		if (statusReports != null)
			statusReports.forEach(statusReport -> outgoingMessage.addStatusReport(statusReport));

		Message newMessage = dbManager.addUpdateMessage(outgoingMessage);
		model.registerMessage(newMessage);

		return newMessage;

	}

	private Message createOutgoingUpdate(String content, UpdateType updateType, Contact contact, Dgroup group,
			Set<StatusReport> statusReports) throws Exception {

		Message outgoingMessage = new Message(content, null, null, MessageStatus.FRESH, contact, model.getIdentity(),
				group, null);

		outgoingMessage.setLocal(true);

		outgoingMessage.setUpdateType(updateType);

		if (statusReports != null)
			statusReports.forEach(statusReport -> outgoingMessage.addStatusReport(statusReport));

		Message newMessage = dbManager.addUpdateMessage(outgoingMessage);
		model.registerMessage(newMessage);

		return newMessage;

	}

	private Set<StatusReport> createStatusReports(Dgroup group) {

		Set<StatusReport> statusReports = new HashSet<StatusReport>();

		if (!group.isLocal())
			statusReports.add(new StatusReport(group.getOwner().getId(), MessageStatus.FRESH));

		group.getMembers().forEach(contact -> {

			if (Objects.equals(contact.getUuid(), model.getLocalUuid()))
				return;

			statusReports.add(new StatusReport(contact.getId(), MessageStatus.FRESH));

		});

		return statusReports;

	}

	private Set<StatusReport> createStatusReports(Set<Contact> contacts) {

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

		if (isActive && contactsToBeAdded != null)
			group.getMembers().addAll(contactsToBeAdded);

		if (isActive && contactsToBeRemoved != null)
			group.getMembers().removeAll(contactsToBeRemoved);

		if (!isActive)
			group.setStatus(Availability.OFFLINE);
		else if (group.isLocal())
			group.setStatus(Availability.AVAILABLE);
		else
			group.setStatus(
					model.isContactOnline(group.getOwner().getUuid()) ? Availability.LIMITED : Availability.OFFLINE);

		List<String> contactNames = new ArrayList<String>();
		group.getMembers().forEach(contact -> contactNames.add(contact.getName()));
		Collections.sort(contactNames, model.getCaseInsensitiveStringSorter());
		if (!group.isLocal())
			contactNames.add(0, owner.getName());
		group.setComment(String.join(", ", contactNames));

		Dgroup newGroup = dbManager.addUpdateGroup(group);

		model.addUpdateGroup(newGroup);

		Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

		dmsListeners.forEach(
				listener -> listenerTaskQueue.execute(() -> listener.groupUpdated(new GroupHandleImpl(newGroup))));

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

		return Base64.getEncoder().encodeToString(DmsPackingFactory.pack(groupUpdate));

	}

	private void sendPrivateMessage(Message message) {

		String receiverUuid = message.getContact().getUuid();

		if (model.isContactOnline(receiverUuid))
			dmsSendMessage(message, Arrays.asList(receiverUuid));

	}

	private void sendGroupMessage(Message message) {

		Dgroup group = message.getDgroup();

		if (group == null)
			return;

		final List<String> onlineUuids = new ArrayList<String>();

		if (group.isLocal()) {
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

			if (!onlineUuids.isEmpty())
				dmsSendMessage(message, onlineUuids);

		} else {
			// It's not my group, so I will send this message to the group owner only.

			String receiverUuid = group.getOwner().getUuid();

			if (model.isContactOnline(receiverUuid))
				dmsSendMessage(message, Arrays.asList(receiverUuid));

		}

	}

	private void sendGroupMessage(Message message, String receiverUuid) {

		if (model.isContactOnline(receiverUuid))
			dmsSendMessage(message, Arrays.asList(receiverUuid));

	}

	private void sendGroupMessage(Message message, Set<Contact> receivers) {

		final List<String> onlineUuids = new ArrayList<String>();

		for (Contact receiver : receivers) {

			String receiverUuid = receiver.getUuid();

			if (model.isContactOnline(receiverUuid))
				onlineUuids.add(receiverUuid);

		}

		if (!onlineUuids.isEmpty())
			dmsSendMessage(message, onlineUuids);

	}

	private void dmsSendMessage(Message message, List<String> receiverUuids) {

		Message copyMessage = new Message(message);

		copyMessage.setId(null);
		copyMessage.setMessageRefId(null);

		Message refMessage = copyMessage.getRefMessage();

		if (refMessage != null) {
			Message newRefMessage = new Message();
			newRefMessage.setId(refMessage.getId());
			newRefMessage.setMessageRefId(refMessage.getMessageRefId());
			copyMessage.setRefMessage(newRefMessage);
		}

		Dgroup group = copyMessage.getDgroup();
		if (group != null) {
			copyMessage.setSenderGroupOwner(group.isLocal());
			copyMessage.setGroupRefId(group.getGroupRefId());
		}

		if (!copyMessage.isLocal() && Boolean.TRUE.equals(copyMessage.getSenderGroupOwner()))
			copyMessage.setContactRefId(copyMessage.getContact().getId());

		dmsClient.sendMessage(copyMessage, receiverUuids, message.getId());

	}

	private void updateMessageReceived(Message message) throws Exception {

		if (message.getUpdateType() == UpdateType.CANCEL_MESSAGE) {

			Long messageId = Long.parseLong(message.getContent());

			Message dbMessage = dbManager.getMessageBySender(message.getContact().getUuid(), messageId);

			if (dbMessage != null)
				cancelMessage(dbMessage);

		} else if (message.getUpdateType() == UpdateType.UPDATE_GROUP) {

			GroupUpdate groupUpdate = DmsPackingFactory.unpack(Base64.getDecoder().decode(message.getContent()),
					GroupUpdate.class);

			Contact owner = message.getContact();
			Long groupRefId = message.getGroupRefId();
			Dgroup group = getRemoteGroup(owner.getUuid(), groupRefId);
			if (group == null)
				group = new Dgroup(owner, groupRefId);

			groupUpdateReceived(group, groupUpdate);

		}

	}

	private Dgroup groupUpdateReceived(final Dgroup group, final GroupUpdate groupUpdate) throws Exception {

		if (group.getGroupRefId() == null)
			throw new Exception("Group reference id cannot be null.");

		Contact owner = group.getOwner();
		String ownerUuid = owner.getUuid();

		final Set<Contact> contactsToBeAdded = new HashSet<Contact>();
		final Set<Contact> contactsToBeRemoved = new HashSet<Contact>();

		if (groupUpdate.add != null) {

			for (ContactMap contactMap : groupUpdate.add) {

				String uuid = contactMap.uuid;

				if (Objects.equals(model.getLocalUuid(), uuid))
					continue;

				Contact contact = getContact(uuid, contactMap.name);

				contactsToBeAdded.add(contact);

				dbManager.addUpdateContactRef(new ContactRef(owner, contactMap.refId, contact));

			}

		}

		if (groupUpdate.remove != null) {

			for (Long refId : groupUpdate.remove) {

				ContactRef contactRef = dbManager.getContactRef(ownerUuid, refId);

				if (contactRef == null)
					continue;

				contactsToBeRemoved.add(contactRef.getContact());

			}

		}

		return createUpdateGroup(group, groupUpdate.name, groupUpdate.active == null ? true : groupUpdate.active,
				contactsToBeAdded, contactsToBeRemoved);

	}

	private MessageStatus computeMessageStatus(Message message) {

		if (message.getUpdateType() != null)
			return MessageStatus.READ;

		return model.isEntityOpen(message.getEntity().getEntityId()) ? MessageStatus.READ : MessageStatus.RECEIVED;

	}

	private Path getDstFile(Path dstFolder, String fileName) throws IOException {

		if (Files.notExists(dstFolder))
			Files.createDirectories(dstFolder);

		if (fileName == null) {
			fileName = String.valueOf(System.currentTimeMillis());
		}

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

		Path dstFolder = Paths.get(Commons.SEND_FOLDER);

		if (Objects.equals(dstFolder.normalize().toAbsolutePath(), srcFile.normalize().toAbsolutePath().getParent())) {
			return srcFile;
		}

		String fileName = srcFile.getFileName().toString();

		Path dstFile = getDstFile(dstFolder, fileName);

		Files.copy(srcFile, dstFile);

		return dstFile;

	}

	private void updateMessageStatus(Message message, List<Long> contactIds, MessageStatus messageStatus)
			throws Exception {

		String contactUuid = message.getContact().getUuid();

		Dgroup group = message.getDgroup();

		// Send this status to the original sender too.
		if (!message.isLocal() && group != null && group.isLocal() && model.isContactOnline(contactUuid)) {

			GroupMessageStatus groupMessageStatus = new GroupMessageStatus(messageStatus, contactIds);

			dmsClient.feedGroupMessageStatus(Collections.singletonMap(message.getMessageRefId(), groupMessageStatus),
					contactUuid);

		}

		if (group == null) {

			message.setMessageStatus(messageStatus);

			if (!message.isDone())
				message.setDone(messageStatus == MessageStatus.READ);

			final Message newMessage = dbManager.addUpdateMessage(message);
			model.registerMessage(newMessage);

			if (newMessage.getUpdateType() == null)
				updateMessageInPane(newMessage);

			if (messageStatus == MessageStatus.FRESH)
				sendPrivateMessage(newMessage);

		} else {
			// This is a group message. I am either the group owner or the message sender.
			// If I am the group owner and the message is not received remotely, I will have
			// to re-send it.

			Map<Long, StatusReport> statusReportMap = new HashMap<Long, StatusReport>();

			message.getStatusReports()
					.forEach(statusReport -> statusReportMap.put(statusReport.getContactId(), statusReport));

			for (Long contactId : contactIds) {

				if (statusReportMap.containsKey(contactId)) {

					statusReportMap.get(contactId).setMessageStatus(messageStatus);

				} else {

					message.addStatusReport(new StatusReport(contactId, messageStatus));

				}

				if (Objects.equals(message.getId(), model.getDetailedGroupMessageId()))
					Platform.runLater(() -> dmsPanel.updateDetailedMessageStatus(contactId, messageStatus));

			}

			MessageStatus overallMessageStatus = message.getOverallStatus();

			if (!message.isDone())
				message.setDone(overallMessageStatus == MessageStatus.READ);

			// If I am the owner, update the message status too
			if (message.isLocal())
				message.setMessageStatus(overallMessageStatus);

			final Message newMessage = dbManager.addUpdateMessage(message);
			model.registerMessage(newMessage);

			if (newMessage.getUpdateType() == null)
				updateMessageInPane(newMessage);

			if (messageStatus == MessageStatus.FRESH) {
				// If the message is not received remotely and;
				// I am the group owner
				// or
				// I am the message owner and receiver is the group owner
				// then re-send this message (if flag set to true).

				for (Long contactId : contactIds) {

					if (group.isLocal() || (message.isLocal() && Objects.equals(contactId, group.getOwner().getId())))
						sendGroupMessage(newMessage, getContact(contactId).getUuid());

				}

			}

		}

		if (message.getUpdateType() == null) {
			contactIds.forEach(contactId -> dmsGuiListeners.forEach(listener -> listenerTaskQueue.execute(() -> listener
					.guiMessageStatusUpdated(message.getId(), messageStatus, ContactIdImpl.of(contactId)))));
		}

	}

	private Message cancelMessage(Message message) throws Exception {

		message.setDone(true);

		Message newMessage = dbManager.addUpdateMessage(message);
		model.registerMessage(newMessage);

		dispatchCancellation(newMessage);

		return newMessage;

	}

	private void dispatchCancellation(Message message) {

		if (model.isServerConnected())
			dmsClient.cancelMessage(message.getId());

		Dgroup group = message.getDgroup();

		if (group == null || group.isLocal())
			return;

		try {
			sendPrivateMessage(createOutgoingUpdate(String.valueOf(message.getId()), UpdateType.CANCEL_MESSAGE,
					message.getContact(), null, null));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void archiveMessages(List<Message> messages) throws Exception {

		final List<Long> deletedMessageIds = new ArrayList<Long>();

		if (messages.stream().allMatch(message -> message.getViewStatus() == ViewStatus.ARCHIVED)) {

			messages.forEach(message -> {

				if (message.getEntity().getViewStatus() == ViewStatus.DELETED) {
					preDeleteMessage(message);
					deletedMessageIds.add(message.getId());
				} else {
					message.setViewStatus(ViewStatus.DEFAULT);
				}

			});

		} else {

			messages.removeIf(message -> message.getViewStatus() != ViewStatus.DEFAULT);

			messages.forEach(message -> message.setViewStatus(ViewStatus.ARCHIVED));

		}

		List<Message> newMessages = dbManager.addUpdateMessages(messages);
		model.registerMessages(newMessages);

		newMessages.forEach(message -> {
			updateMessageInPane(message);
			if (message.getId() < model.getMinArchivedMessageId()) {
				return;
			}
			Platform.runLater(() -> dmsPanel.addUpdateArchivedMessage(message));
		});

		if (!deletedMessageIds.isEmpty()) {
			dmsGuiListeners.forEach(listener -> listenerTaskQueue
					.execute(() -> listener.guiMessagesDeleted(deletedMessageIds.toArray(new Long[0]))));
		}

	}

	private Long[] deleteMessages(List<Message> messages) throws Exception {

		messages.forEach(message -> preDeleteMessage(message));

		List<Message> newMessages = dbManager.addUpdateMessages(messages);
		model.registerMessages(newMessages);

		newMessages.forEach(message -> updateMessageInPane(message));

		return newMessages.stream().map(message -> message.getId()).toArray(Long[]::new);

	}

	private void preDeleteMessage(Message message) {

		if (message.getMessageStatus() == MessageStatus.FRESH && !message.isDone())
			dispatchCancellation(message);

		message.setDone(true);
		message.setMessageStatus(MessageStatus.READ);
		message.setViewStatus(ViewStatus.DELETED);

	}

	private Contact getContact(Long id) throws Exception {

		Contact contact = model.getContact(id);

		if (contact == null)
			contact = dbManager.getContactById(id);

		if (contact == null)
			throw new Exception(String.format("Contact #%d does not exist!", id));

		return contact;

	}

	private Contact getContact(String uuid) throws Exception {

		return getContact(uuid, null);

	}

	private Contact getContact(String uuid, String name) throws Exception {

		Contact contact = model.getContact(uuid);

		if (contact != null)
			return contact;

		contact = dbManager.getContact(uuid);

		if (contact == null) {
			contact = new Contact(uuid);
			contact.setName(name);
			contact.setStatus(Availability.OFFLINE);
			final Contact newContact = dbManager.addUpdateContact(contact);
			contact = newContact;
			model.addUpdateContact(newContact);
			Platform.runLater(() -> dmsPanel.updateContact(newContact));
			dmsListeners.forEach(listener -> listenerTaskQueue
					.execute(() -> listener.contactUpdated(new ContactHandleImpl(newContact))));
		}

		return contact;

	}

	private Dgroup getGroup(Long id) throws Exception {

		Dgroup group = model.getGroup(id);

		if (group == null)
			group = dbManager.getGroupById(id);

		return group;

	}

	private Dgroup getRemoteGroup(String ownerUuid, Long groupRefId) throws Exception {

		Dgroup group = model.getRemoteGroup(ownerUuid, groupRefId);

		if (group == null)
			group = dbManager.getGroupByOwner(ownerUuid, groupRefId);

		return group;

	}

	private Path moveFileToReceiveFolder(Path path, String fileName) throws IOException {
		synchronized (FILE_SYNC) {
			Path dstFolder = Paths.get(Commons.RECEIVE_FOLDER);
			Path dstFile = getDstFile(dstFolder, fileName);
			if (path == null) {
				Files.createFile(dstFile);
			} else {
				Files.move(path, dstFile);
			}
			return dstFile;
		}
	}

	@Override
	public void beaconReceived(final Beacon beacon) {

		taskQueue.execute(() -> {

			try {

				final String userUuid = beacon.uuid;

				if (userUuid == null)
					return;

				Contact incomingContact = new Contact(beacon.uuid, beacon.name, beacon.comment,
						Availability.of(beacon.status), beacon.latitude, beacon.longitude, beacon.secretId);

				final Contact newContact = dbManager.addUpdateContact(incomingContact);

				newContact.setLocalRemoteServerIps(beacon.localRemoteServerIps);

				boolean wasOffline = !model.isContactOnline(userUuid);

				model.addUpdateContact(newContact);

				Platform.runLater(() -> dmsPanel.updateContact(newContact));

				List<Dgroup> updatedGroups = new ArrayList<Dgroup>();

				final Long contactId = newContact.getId();

				if (wasOffline) {
					// Contact has just been online
					// MODIFY GROUP STATUSES
					try {

						for (Dgroup group : dbManager.getAllActiveGroupsOfContact(contactId)) {

							group.setStatus(Availability.LIMITED);

							try {

								final Dgroup newGroup = dbManager.addUpdateGroup(group);

								model.addUpdateGroup(newGroup);

								Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

								updatedGroups.add(newGroup);

							} catch (Exception e) {

								e.printStackTrace();

							}

						}

					} catch (Exception e) {

						e.printStackTrace();

					}

					List<Long> messageIds = new ArrayList<Long>();

					// CHECK PRIVATE MESSAGES
					try {

						for (Message waitingMessage : dbManager.getPrivateMessagesWaitingToContact(contactId)) {

							switch (waitingMessage.getMessageStatus()) {

							case FRESH:

								sendPrivateMessage(waitingMessage);

								break;

							case SENT:
							case RECEIVED:

								messageIds.add(waitingMessage.getId());

								break;

							default:

								break;

							}

						}

					} catch (Exception e) {

						e.printStackTrace();

					}

					// CHECK GROUP MESSAGES
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

											messageIds.add(waitingMessage.getId());

											break;

										default:

											break;

										}

									});

						}

					} catch (Exception e) {

						e.printStackTrace();

					}

					if (!messageIds.isEmpty())
						dmsClient.claimMessageStatus(messageIds.toArray(new Long[0]), userUuid);

					// CHECK STATUS REPORTS
					try {

						messageIds.clear();

						for (Message waitingMessage : dbManager.getGroupMessagesWaitingToItsGroup(contactId)) {

							messageIds.add(waitingMessage.getId());

						}

						if (!messageIds.isEmpty())
							dmsClient.claimStatusReport(messageIds.toArray(new Long[0]), userUuid);

					} catch (Exception e) {

						e.printStackTrace();

					}

					// CHECK DOWNLOADS
					downloadTaskQueue.execute(() -> {
						List<DownloadPojo> waitingDownloads = model.getWaitingDownloads(userUuid);
						waitingDownloads.forEach(downloadPojo -> dmsClient.sendDownloadRequest(downloadPojo, userUuid));
					});

				}

				dmsListeners.forEach(listener -> listenerTaskQueue
						.execute(() -> listener.contactUpdated(new ContactHandleImpl(newContact))));

				updatedGroups.forEach(group -> dmsListeners.forEach(listener -> listenerTaskQueue
						.execute(() -> listener.groupUpdated(new GroupHandleImpl(group)))));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void remoteIpsReceived(final InetAddress[] remoteIps) {

		Platform.runLater(() -> dmsPanel.updateRemoteIps(
				Arrays.asList(remoteIps).stream().map(ip -> ip.getHostAddress()).toArray(String[]::new)));

	}

	@Override
	public void progressMessageReceived(final Long messageId, final List<String> remoteUuids, final int progress) {
		// messageId = local database id of the message (not the message id, which is
		// the local database id of the sender)

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessageById(messageId);

				if (message == null)
					return;

				List<EntityId> entityIds = new ArrayList<EntityId>();

				for (String remoteUuid : remoteUuids) {

					try {
						entityIds.add(getContact(remoteUuid).getEntityId());
					} catch (Exception e) {
						e.printStackTrace();
					}

				}

				if (progress == 100) {

					// Update status in database and view; send update to message owner if necessary

					updateMessageStatus(message,
							entityIds.stream().map(entityId -> entityId.getId()).collect(Collectors.toList()),
							MessageStatus.SENT);

				}

				// Update view only

				if (!(message.getUpdateType() == null && message.isLocal()))
					return;

				if (message.getDgroup() == null) {

					for (EntityId entityId : entityIds) {

						Platform.runLater(() -> dmsPanel.updateMessageProgress(entityId, messageId, progress));

					}

				} else {

					for (EntityId entityId : entityIds) {

						model.storeGroupMessageProgress(messageId, entityId.getId(), progress);

						if (Objects.equals(messageId, model.getDetailedGroupMessageId()))
							Platform.runLater(() -> dmsPanel.updateDetailedMessageProgress(entityId.getId(), progress));

					}

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void progressTransientReceived(final Long trackingId, final List<String> remoteUuids, final int progress) {

		taskQueue.execute(() -> {

			List<ContactId> remoteIds = new ArrayList<ContactId>();

			for (String remoteUuid : remoteUuids) {

				try {
					remoteIds.add(ContactIdImpl.of(getContact(remoteUuid).getId()));
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			if (progress < 0) {
				dmsListeners.forEach(
						listener -> listenerTaskQueue.execute(() -> listener.messageFailed(trackingId, remoteIds)));
			}

		});

	}

	@Override
	public void messageReceived(final Message message, final Path attachment, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				// If message already received, feed status and return
				Message dbMessage = dbManager.getMessageBySender(remoteUuid, message.getMessageRefId());
				if (dbMessage != null) {
					dmsClient.feedMessageStatus(
							Collections.singletonMap(dbMessage.getMessageRefId(), dbMessage.getMessageStatus()),
							remoteUuid);
					return;
				}

				Contact contact = getContact(remoteUuid);
				if (contact.getViewStatus() != ViewStatus.DEFAULT) {
					contact.setViewStatus(ViewStatus.DEFAULT);
					final Contact newContact = dbManager.addUpdateContact(contact);
					contact = newContact;
					model.addUpdateContact(newContact);
					Platform.runLater(() -> dmsPanel.updateContact(newContact));
				}

				message.setContact(contact);

				if (message.getUpdateType() != null)
					updateMessageReceived(message);

				boolean messageToBeRedirected = false;

				if (Boolean.FALSE.equals(message.getSenderGroupOwner())) {

					Dgroup group = getGroup(message.getGroupRefId());
					if (group == null) {
						return;
					} else if (group.getViewStatus() != ViewStatus.DEFAULT) {
						group.setViewStatus(ViewStatus.DEFAULT);
						final Dgroup newGroup = dbManager.addUpdateGroup(group);
						group = newGroup;
						model.addUpdateGroup(newGroup);
						Platform.runLater(() -> dmsPanel.updateGroup(newGroup));
					}

					message.setDgroup(group);

					group.getMembers().forEach(member -> {

						if (Objects.equals(member.getUuid(), remoteUuid))
							return;

						message.addStatusReport(new StatusReport(member.getId(), MessageStatus.FRESH));

					});

					messageToBeRedirected = !message.getStatusReports().isEmpty();

				} else if (Boolean.TRUE.equals(message.getSenderGroupOwner())) {

					Dgroup group = getRemoteGroup(remoteUuid, message.getGroupRefId());
					if (group == null) {
						group = groupUpdateReceived(new Dgroup(contact, message.getGroupRefId()), new GroupUpdate());
					} else if (group.getViewStatus() != ViewStatus.DEFAULT) {
						group.setViewStatus(ViewStatus.DEFAULT);
						final Dgroup newGroup = dbManager.addUpdateGroup(group);
						group = newGroup;
						model.addUpdateGroup(newGroup);
						Platform.runLater(() -> dmsPanel.updateGroup(newGroup));
					}

					ContactRef contactRef = dbManager.getContactRef(remoteUuid, message.getContactRefId());
					if (contactRef != null) {
						message.setOwner(contactRef.getContact());
					}

					message.setDgroup(group);

				}

				if (message.getAttachmentType() != null) {
					message.setAttachmentPath(
							moveFileToReceiveFolder(attachment, message.getAttachmentName()).toString());
				}

				message.setMessageStatus(computeMessageStatus(message));

				message.setDone(!messageToBeRedirected);

				Message newMessage = dbManager.addUpdateMessage(message);
				model.registerMessage(newMessage);

				if (newMessage.getUpdateType() == null) {

					addMessageToPane(newMessage, true);

					if (model.isAudioOn() && !model.isEntityOpen(newMessage.getEntity().getEntityId())) {
						if (newMessage.getEntity().getEntityId().isGroup())
							soundPlayer.playTriTone();
						else
							soundPlayer.playDuoTone();
					}

					dmsGuiListeners.forEach(listener -> listenerTaskQueue.execute(() -> {

						if (newMessage.getAttachmentType() == null) {

							listener.guiMessageReceived(newMessage.getId(), newMessage.getContent(),
									ContactIdImpl.of(newMessage.getContact().getId()), GroupIdImpl.of(
											newMessage.getDgroup() == null ? null : newMessage.getDgroup().getId()));

						} else if (newMessage.getAttachmentType() == AttachmentType.FILE) {

							listener.guiFileReceived(newMessage.getId(), newMessage.getContent(),
									Paths.get(newMessage.getAttachmentPath()),
									ContactIdImpl.of(newMessage.getContact().getId()), GroupIdImpl.of(
											newMessage.getDgroup() == null ? null : newMessage.getDgroup().getId()));

						} else if (newMessage.getAttachmentType() == AttachmentType.AUDIO) {

							listener.guiAudioReceived(newMessage.getId(), Paths.get(newMessage.getAttachmentPath()),
									ContactIdImpl.of(newMessage.getContact().getId()), GroupIdImpl.of(
											newMessage.getDgroup() == null ? null : newMessage.getDgroup().getId()));

						} else if (newMessage.getAttachmentType() == AttachmentType.REPORT) {

							listener.guiReportReceived(newMessage.getId(), newMessage.getContent(),
									newMessage.getMessageCode(), Paths.get(newMessage.getAttachmentPath()),
									ContactIdImpl.of(newMessage.getContact().getId()), GroupIdImpl.of(
											newMessage.getDgroup() == null ? null : newMessage.getDgroup().getId()));

						}

					}));

				}

				dmsClient.feedMessageStatus(
						Collections.singletonMap(newMessage.getMessageRefId(), newMessage.getMessageStatus()),
						remoteUuid);

				if (messageToBeRedirected)
					sendGroupMessage(newMessage);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void userDisconnected(final String uuid) {

		taskQueue.execute(() -> {

			try {
				contactDisconnected(getContact(uuid));
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

	@Override
	public void serverConnStatusUpdated(final boolean connStatus, final boolean logout) {

		taskQueue.execute(() -> {

			model.setServerConnStatus(connStatus);

			if (connStatus) {

				Contact identity = model.getIdentity();

				sendBeacon(identity.getName(), identity.getComment(), identity.getStatus(), identity.getLatitude(),
						identity.getLongitude(), identity.getSecretId());

				dmsClient.claimStartInfo();

				Runnable task;
				while ((task = model.consumeServerTask()) != null) {
					task.run();
				}

			} else {

				model.getContacts().forEach((id, contact) -> contactDisconnected(contact));

				if (logout) {
					closeSession();
				}

			}

			Platform.runLater(() -> dmsPanel.serverConnStatusUpdated(connStatus));

		});

	}

	private void closeSession() {

		auxTaskQueue.execute(() -> {
			taskQueue.execute(() -> {
				audioCenter.close();
				soundPlayer.close();
				//
				downloadTaskQueue.execute(() -> {
					dmsFileServer.set(null);
					clearAllDownloads();
				});
				downloadTaskQueue.shutdown();
				//
				listenerTaskQueue.execute(() -> {
					dmsListeners.clear();
					dmsGuiListeners.clear();
					dmsDownloadListeners.clear();
				});
				listenerTaskQueue.shutdown();
				//
				try {
					dbManager.close();
				} catch (Exception e) {

				}
				//
				sessionLock.release();
			});
			taskQueue.shutdown();
		});
		auxTaskQueue.shutdown();

	}

	private void clearAllDownloads() {
		model.getAllDownloads().forEach(downloadPojo -> deleteFile(downloadPojo.path));
		model.clearAllDownloads();
	}

	@Override
	public void messageStatusClaimed(final Long[] messageIds, final String remoteUuid) {

		taskQueue.execute(() -> {

			Map<Long, MessageStatus> messageIdStatusMap = new HashMap<Long, MessageStatus>();

			for (Long messageId : messageIds) {

				try {

					Message incomingMessage = dbManager.getMessageBySender(remoteUuid, messageId);

					if (incomingMessage == null) {

						// Not received
						messageIdStatusMap.put(messageId, MessageStatus.FRESH);

					} else {

						messageIdStatusMap.put(messageId, incomingMessage.getMessageStatus());

					}

				} catch (Exception e) {

					e.printStackTrace();

				}

			}

			if (!messageIdStatusMap.isEmpty())
				dmsClient.feedMessageStatus(messageIdStatusMap, remoteUuid);

		});

	}

	@Override
	public void messageStatusFed(final Map<Long, MessageStatus> messageIdStatusMap, final String remoteUuid) {

		taskQueue.execute(() -> {

			messageIdStatusMap.forEach((messageId, messageStatus) -> {

				try {

					Message dbMessage = dbManager.getMessageById(messageId);

					if (dbMessage == null)
						return;

					updateMessageStatus(dbMessage, Arrays.asList(getContact(remoteUuid).getId()), messageStatus);

				} catch (Exception e) {

					e.printStackTrace();

				}

			});

		});

	}

	@Override
	public void groupMessageStatusFed(final Map<Long, GroupMessageStatus> messageIdGroupStatusMap,
			final String remoteUuid) {

		taskQueue.execute(() -> {

			messageIdGroupStatusMap.forEach((messageId, groupMessageStatus) -> {

				try {

					Message dbMessage = dbManager.getMessageById(messageId);

					if (dbMessage == null)
						return;

					List<Long> ids = new ArrayList<Long>();

					groupMessageStatus.refIds.forEach(refId -> {

						try {

							ContactRef contactRef = dbManager.getContactRef(remoteUuid, refId);

							if (contactRef != null)
								ids.add(contactRef.getContact().getId());

						} catch (Exception e) {

							e.printStackTrace();

						}

					});

					updateMessageStatus(dbMessage, ids, groupMessageStatus.messageStatus);

				} catch (Exception e) {

					e.printStackTrace();

				}

			});

		});

	}

	@Override
	public void statusReportClaimed(final Long[] messageIds, final String remoteUuid) {

		taskQueue.execute(() -> {

			Map<Long, Set<StatusReport>> messageIdStatusReportsMap = new HashMap<Long, Set<StatusReport>>();

			for (Long messageId : messageIds) {

				try {

					Message dbMessage = dbManager.getMessageBySender(remoteUuid, messageId);

					if (dbMessage != null)
						messageIdStatusReportsMap.put(messageId, dbMessage.getStatusReports());

				} catch (Exception e) {

					e.printStackTrace();

				}

			}

			if (!messageIdStatusReportsMap.isEmpty())
				dmsClient.feedStatusReport(messageIdStatusReportsMap, remoteUuid);

		});

	}

	@Override
	public void statusReportFed(final Map<Long, StatusReport[]> messageIdStatusReportsMap) {

		taskQueue.execute(() -> {

			messageIdStatusReportsMap.forEach((messageId, statusReports) -> {

				try {

					// A status report can only be claimed by a group message owner. So at this
					// point, I must be a group message owner. Let's find that message.

					Message dbMessage = dbManager.getMessageById(messageId);

					if (dbMessage == null)
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

						ContactRef contactRef = dbManager.getContactRef(group.getOwner().getUuid(),
								statusReport.getContactId());

						if (contactRef == null)
							continue;

						Long contactId = contactRef.getContact().getId();

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
					model.registerMessage(newMessage);

					if (newMessage.getUpdateType() == null)
						updateMessageInPane(newMessage);

					if (Objects.equals(newMessage.getId(), model.getDetailedGroupMessageId())) {
						newMessage.getStatusReports().forEach(statusReport -> Platform
								.runLater(() -> dmsPanel.updateDetailedMessageStatus(statusReport.getContactId(),
										statusReport.getMessageStatus())));
					}

				} catch (Exception e) {

					e.printStackTrace();

				}

			});

		});

	}

	@Override
	public void transientMessageReceived(final MessageHandleImpl message, final Path attachment,
			final String remoteUuid, final Long trackingId) {

		taskQueue.execute(() -> {

			try {

				final Long contactId = getContact(remoteUuid).getId();

				FileHandleImpl fileHandle = (FileHandleImpl) message.getFileHandle();

				if (fileHandle != null) {
					message.setFileHandle(new FileHandleImpl(fileHandle.getFileCode(),
							moveFileToReceiveFolder(attachment, fileHandle.getFileName())));
				}

				dmsListeners.forEach(listener -> listenerTaskQueue
						.execute(() -> listener.messageReceived(message, ContactIdImpl.of(contactId))));

				if (trackingId != null) {
					dmsClient.sendTransientMessageStatus(trackingId, remoteUuid);
				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void transientMessageStatusReceived(final Long trackingId, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				final Long contactId = getContact(remoteUuid).getId();

				dmsListeners.forEach(listener -> listenerTaskQueue
						.execute(() -> listener.messageTransmitted(trackingId, ContactIdImpl.of(contactId))));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void downloadRequested(final DownloadPojo downloadPojo, final String remoteUuid) {

		downloadTaskQueue.execute(() -> {

			DmsFileServer fileServer = dmsFileServer.get();
			if (fileServer == null) {
				dmsClient.sendServerNotFound(downloadPojo.downloadId, remoteUuid);
				return;
			}
			Path path = null;
			try {
				path = fileServer.fileRequested(downloadPojo.fileId);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			if (path != null && !Files.exists(path)) {
				path = null;
			}
			if (path == null) {
				dmsClient.sendFileNotFound(downloadPojo.downloadId, remoteUuid);
				return;
			}
			dmsClient.uploadFile(path, remoteUuid, downloadPojo.downloadId, downloadPojo.position);

		});

	}

	@Override
	public void cancelDownloadRequested(final Long downloadId, final String remoteUuid) {

		downloadTaskQueue.execute(() -> {

			dmsClient.cancelUpload(remoteUuid, downloadId);

		});

	}

	@Override
	public void serverNotFound(final Long downloadId) {

		downloadTaskQueue.execute(() -> {

			DownloadPojo downloadPojo = model.removeDownload(downloadId);
			if (downloadPojo == null) {
				return;
			}
			deleteFile(downloadPojo.path);
			dmsDownloadListeners
					.forEach(listener -> listenerTaskQueue.execute(() -> listener.fileServerNotFound(downloadId)));

		});

	}

	@Override
	public void fileNotFound(final Long downloadId) {

		downloadTaskQueue.execute(() -> {

			DownloadPojo downloadPojo = model.removeDownload(downloadId);
			if (downloadPojo == null) {
				return;
			}
			deleteFile(downloadPojo.path);
			dmsDownloadListeners
					.forEach(listener -> listenerTaskQueue.execute(() -> listener.fileNotFound(downloadId)));

		});

	}

	@Override
	public void downloadingFile(final Long downloadId, final int progress) {

		downloadTaskQueue.execute(() -> {

			if (!model.isDownloadActive(downloadId)) {
				return;
			}
			dmsDownloadListeners.forEach(
					listener -> listenerTaskQueue.execute(() -> listener.downloadingFile(downloadId, progress)));

		});

	}

	@Override
	public void fileDownloaded(final Long downloadId, final Path tmpPath, final String fileName,
			final boolean partial) {

		downloadTaskQueue.execute(() -> {

			DownloadPojo downloadPojo = model.getDownload(downloadId);
			if (downloadPojo == null) {
				deleteFile(tmpPath);
				return;
			}

			downloadPojo.paused.set(partial && downloadPojo.pausing.getAndSet(false));
			if (downloadPojo.paused.get()) {
				dmsDownloadListeners
						.forEach(listener -> listenerTaskQueue.execute(() -> listener.downloadPaused(downloadId)));
			}

			if (tmpPath == null && partial) {
				return;
			}

			Path path = tmpPath;

			try {
				if (path == null) {
					path = Files.createTempFile("dms", null);
					path.toFile().deleteOnExit();
				}
				if (downloadPojo.path != null) {
					try (FileChannel fileChannelRead = FileChannel.open(downloadPojo.path, StandardOpenOption.READ);
							FileChannel fileChannelWrite = FileChannel.open(path, StandardOpenOption.WRITE)) {
						ByteBuffer buffer = ByteBuffer.allocate(Commons.CHUNK_SIZE);
						while (fileChannelRead.read(buffer) > 0) {
							buffer.flip();
							fileChannelWrite.write(buffer);
							buffer.rewind();
						}
					}
					deleteFile(downloadPojo.path);
				}
				if (partial) {
					downloadPojo.path = path;
					downloadPojo.position = Files.size(downloadPojo.path);
					return;
				}
				downloadPojo.path = moveFileToReceiveFolder(path, fileName);
				model.removeDownload(downloadId);
				dmsDownloadListeners.forEach(listener -> listenerTaskQueue
						.execute(() -> listener.fileDownloaded(downloadId, downloadPojo.path)));
			} catch (Exception e) {
				model.removeDownload(downloadId);
				deleteFile(path);
				deleteFile(downloadPojo.path);
				dmsDownloadListeners
						.forEach(listener -> listenerTaskQueue.execute(() -> listener.downloadFailed(downloadId)));
			}

		});

	}

	@Override
	public void downloadFailed(final Long downloadId) {

		downloadTaskQueue.execute(() -> {

			DownloadPojo downloadPojo = model.getDownload(downloadId);
			if (downloadPojo == null) {
				return;
			}
			downloadPojo.paused.set(downloadPojo.pausing.getAndSet(false));
			if (downloadPojo.paused.get()) {
				dmsDownloadListeners
						.forEach(listener -> listenerTaskQueue.execute(() -> listener.downloadPaused(downloadId)));
				return;
			}
			model.removeDownload(downloadId);
			deleteFile(downloadPojo.path);
			dmsDownloadListeners
					.forEach(listener -> listenerTaskQueue.execute(() -> listener.downloadFailed(downloadId)));

		});

	}

	private void deleteFile(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (Exception e) {

		}
	}

	@Override
	public void commentUpdateRequested(final String comment) {

		taskQueue.execute(() -> {

			Contact identity = model.getIdentity();
			String oldComment = identity.getComment();

			if (Objects.equals(oldComment, comment)) {
				return;
			}

			try {
				identity.setComment(comment);
				Contact newIdentity = dbManager.updateIdentity(identity);
				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));
				sendBeacon(null, comment, null, null, null, null);
			} catch (Exception e) {
				model.updateComment(oldComment);
			}

		});

	}

	@Override
	public void statusUpdateRequested(final Availability availability) {

		taskQueue.execute(() -> {

			Contact identity = model.getIdentity();
			Availability oldAvailability = identity.getStatus();

			if (oldAvailability == availability) {
				return;
			}

			try {
				identity.setStatus(availability);
				Contact newIdentity = dbManager.updateIdentity(identity);
				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));
				sendBeacon(null, null, availability, null, null, null);
			} catch (Exception e) {
				model.updateStatus(oldAvailability);
			}

		});

	}

	@Override
	public void messagePaneOpened(final EntityId entityId) {

		taskQueue.execute(() -> {

			model.entityOpened(entityId);

			messagesRead(entityId);

		});

	}

	private void messagesRead(final EntityId entityId) {

		Set<Message> unreadMessagesOfEntity = model.getUnreadMessagesOfEntity(entityId);

		if (unreadMessagesOfEntity == null)
			return;

		try {

			if (entityId.isGroup())
				groupMessagesRead(entityId, unreadMessagesOfEntity);
			else
				privateMessagesRead(entityId, unreadMessagesOfEntity);

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private void privateMessagesRead(final EntityId entityId, final Set<Message> unreadMessagesOfEntity)
			throws Exception {

		Contact contact = getContact(entityId.getId());

		List<Message> newMessages = dbManager.addUpdateMessages(unreadMessagesOfEntity.stream()
				.peek(message -> message.setMessageStatus(MessageStatus.READ)).collect(Collectors.toList()));

		if (newMessages.isEmpty())
			return;

		model.registerMessages(newMessages);

		Platform.runLater(() -> dmsPanel.scrollPaneToMessage(entityId, newMessages.get(0).getId()));

		Map<Long, MessageStatus> messageIdStatusMap = new HashMap<Long, MessageStatus>();

		for (Message message : newMessages) {

			updateMessageInPane(message);

			messageIdStatusMap.put(message.getMessageRefId(), message.getMessageStatus());

		}

		if (!messageIdStatusMap.isEmpty())
			dmsClient.feedMessageStatus(messageIdStatusMap, contact.getUuid());

	}

	private void groupMessagesRead(final EntityId entityId, final Set<Message> unreadMessagesOfEntity)
			throws Exception {

		Dgroup group = getGroup(entityId.getId());

		if (group == null)
			return;

		List<Message> newMessages = dbManager.addUpdateMessages(unreadMessagesOfEntity.stream()
				.peek(message -> message.setMessageStatus(MessageStatus.READ)).collect(Collectors.toList()));

		if (newMessages.isEmpty())
			return;

		model.registerMessages(newMessages);

		Platform.runLater(() -> dmsPanel.scrollPaneToMessage(entityId, newMessages.get(0).getId()));

		Map<String, Map<Long, MessageStatus>> contactUuidMessageIdStatusMap = new HashMap<String, Map<Long, MessageStatus>>();

		for (Message message : newMessages) {

			updateMessageInPane(message);

			String contactUuid = group.isLocal() ? message.getContact().getUuid() : group.getOwner().getUuid();

			contactUuidMessageIdStatusMap.putIfAbsent(contactUuid, new HashMap<Long, MessageStatus>());
			contactUuidMessageIdStatusMap.get(contactUuid).put(message.getMessageRefId(), message.getMessageStatus());

		}

		contactUuidMessageIdStatusMap.forEach(
				(contactUuid, messageIdStatusMap) -> dmsClient.feedMessageStatus(messageIdStatusMap, contactUuid));

	}

	@Override
	public void messagePaneClosed() {

		taskQueue.execute(() -> {

			model.entityClosed();

		});

	}

	@Override
	public void sendMessageClicked(final String messageTxt, final FileBuilder fileBuilder, final Long refMessageId) {

		taskQueue.execute(() -> {

			EntityId entityId = model.getOpenEntityId();
			if (entityId == null)
				return;

			try {

				if (entityId.isGroup()) {
					sendGroupMessageClaimed(messageTxt, fileBuilder, refMessageId, entityId.getId(), null, false);
				} else {
					sendPrivateMessageClaimed(messageTxt, fileBuilder, refMessageId, entityId.getId(), null, false);
				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	private Long sendPrivateMessageClaimed(final String content, final FileBuilder fileBuilder, final Long refMessageId,
			final Long contactId, Boolean apiFlag, boolean onePass) throws Exception {

		Contact contact = getContact(contactId);

		Message refMessage = dbManager.getMessageById(refMessageId);

		String attachmentName = null;
		Path attachmentPath = null;
		AttachmentType attachmentType = null;
		Integer messageCode = null;
		if (fileBuilder != null) {
			attachmentName = fileBuilder.getFileName();
			attachmentType = fileBuilder.getAttachmentType();
			messageCode = fileBuilder.getFileCode();
			if (onePass) {
				attachmentPath = fileBuilder.buildAndGet();
			}
		}

		Message message = createOutgoingMessage(content, attachmentName, attachmentPath, attachmentType, refMessage,
				messageCode, contact, null, null, apiFlag);

		addMessageToPane(message, true);

		Long messageId = message.getId();

		if (onePass || fileBuilder == null) {
			sendPrivateMessage(message);
			messageSentToListeners(message, attachmentPath, contactId, null);
			return messageId;
		}

		auxTaskQueue.execute(() -> {
			try {
				Path newAttachmentPath = fileBuilder.buildAndGet();
				updatePathAndSend(messageId, newAttachmentPath, contactId, null);
				return;
			} catch (Exception e) {

			}
			try {
				deleteMessages(Collections.singletonList(message));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return messageId;

	}

	private Long sendGroupMessageClaimed(final String content, final FileBuilder fileBuilder, final Long refMessageId,
			final Long groupId, Boolean apiFlag, boolean onePass) throws Exception {

		Dgroup group = getGroup(groupId);

		Message refMessage = dbManager.getMessageById(refMessageId);

		String attachmentName = null;
		Path attachmentPath = null;
		AttachmentType attachmentType = null;
		Integer messageCode = null;
		if (fileBuilder != null) {
			attachmentName = fileBuilder.getFileName();
			attachmentType = fileBuilder.getAttachmentType();
			messageCode = fileBuilder.getFileCode();
			if (onePass) {
				attachmentPath = fileBuilder.buildAndGet();
			}
		}

		Message message = createOutgoingMessage(content, attachmentName, attachmentPath, attachmentType, refMessage,
				messageCode, group.getOwner(), group, createStatusReports(group), apiFlag);

		addMessageToPane(message, true);

		Long messageId = message.getId();

		if (onePass || fileBuilder == null) {
			sendGroupMessage(message);
			messageSentToListeners(message, attachmentPath, null, groupId);
			return messageId;
		}

		auxTaskQueue.execute(() -> {
			try {
				Path newAttachmentPath = fileBuilder.buildAndGet();
				updatePathAndSend(messageId, newAttachmentPath, null, groupId);
				return;
			} catch (Exception e) {

			}
			try {
				deleteMessages(Collections.singletonList(message));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return messageId;

	}

	private void updatePathAndSend(final Long messageId, final Path attachmentPath, final Long contactId,
			final Long groupId) {
		taskQueue.execute(() -> {
			try {
				Message message = dbManager.getMessageById(messageId);
				if (message == null || message.getViewStatus() == ViewStatus.DELETED) {
					return;
				}
				message.setMessageStatus(MessageStatus.FRESH);
				message.setAttachmentPath(attachmentPath.toString());
				Message newMessage = dbManager.addUpdateMessage(message);
				model.registerMessage(newMessage);
				updateMessageInPane(newMessage);
				if (newMessage.getViewStatus() == ViewStatus.ARCHIVED) {
					Platform.runLater(() -> dmsPanel.addUpdateArchivedMessage(newMessage));
				}
				if (groupId != null) {
					sendGroupMessage(newMessage);
				} else {
					sendPrivateMessage(newMessage);
				}
				messageSentToListeners(newMessage, attachmentPath, contactId, groupId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void messageSentToListeners(final Message message, final Path attachment, final Long contactId,
			final Long groupId) {

		dmsGuiListeners.forEach(listener -> listenerTaskQueue.execute(() -> {

			Long messageId = message.getId();
			String messageTxt = message.getContent();
			Integer messageCode = message.getMessageCode();
			AttachmentType attachmentType = message.getAttachmentType();

			if (attachmentType == null) {

				listener.guiMessageSent(messageId, messageTxt, ContactIdImpl.of(contactId), GroupIdImpl.of(groupId));

			} else if (attachmentType == AttachmentType.FILE) {

				listener.guiFileSent(messageId, messageTxt, attachment, ContactIdImpl.of(contactId),
						GroupIdImpl.of(groupId));

			} else if (attachmentType == AttachmentType.AUDIO) {

				listener.guiAudioSent(messageId, attachment, ContactIdImpl.of(contactId), GroupIdImpl.of(groupId));

			} else if (attachmentType == AttachmentType.REPORT) {

				listener.guiReportSent(messageId, messageTxt, messageCode, attachment, ContactIdImpl.of(contactId),
						GroupIdImpl.of(groupId));

			}

		}));

	}

	@Override
	public void paneScrolledToTop(final Long topMessageId) {

		taskQueue.execute(() -> {

			EntityId entityId = model.getOpenEntityId();
			if (entityId == null)
				return;

			try {

				List<Message> lastMessagesBeforeId = entityId.isGroup()
						? dbManager.getLastGroupMessagesBeforeId(entityId.getId(), topMessageId, MIN_MESSAGES_PER_PAGE)
						: dbManager.getLastPrivateMessagesBeforeId(entityId.getId(), topMessageId,
								MIN_MESSAGES_PER_PAGE);

				if (lastMessagesBeforeId.size() < MIN_MESSAGES_PER_PAGE)
					Platform.runLater(() -> dmsPanel.allMessagesLoaded(entityId));

				if (lastMessagesBeforeId.isEmpty())
					return;

				Platform.runLater(() -> dmsPanel.savePosition(entityId, topMessageId));

				lastMessagesBeforeId.forEach(message -> addMessageToPane(message, false));

				Platform.runLater(() -> dmsPanel.scrollToSavedPosition(entityId));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messagesClaimed(final Long lastMessageIdExcl, final Long firstMessageIdIncl) {

		taskQueue.execute(() -> {

			EntityId entityId = model.getOpenEntityId();
			if (entityId == null)
				return;

			try {

				long topMessageId = lastMessageIdExcl;

				List<Message> lastMessagesBetweenIds = new ArrayList<Message>();

				while (firstMessageIdIncl < topMessageId) {

					List<Message> lastMessagesBeforeId = entityId.isGroup()
							? dbManager.getLastGroupMessagesBeforeId(entityId.getId(), topMessageId,
									MIN_MESSAGES_PER_PAGE)
							: dbManager.getLastPrivateMessagesBeforeId(entityId.getId(), topMessageId,
									MIN_MESSAGES_PER_PAGE);

					if (lastMessagesBeforeId.isEmpty())
						break;

					lastMessagesBetweenIds.addAll(lastMessagesBeforeId);

					topMessageId = lastMessagesBeforeId.get(lastMessagesBeforeId.size() - 1).getId();

				}

				if (lastMessagesBetweenIds.isEmpty())
					return;

				lastMessagesBetweenIds.forEach(message -> addMessageToPane(message, false));

				Platform.runLater(() -> dmsPanel.scrollPaneToMessage(entityId, firstMessageIdIncl));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void showAddUpdateGroupClicked() {

		taskQueue.execute(() -> {

			EntityId entityId = model.getOpenEntityId();

			try {

				Dgroup group = entityId == null || !entityId.isGroup() ? null : getGroup(entityId.getId());

				model.setGroupToBeUpdated(group);

				if (group == null) {
					// New group

					Platform.runLater(() -> dmsPanel.showAddUpdateGroupPane(null, true));

				} else {
					// Update group

					Platform.runLater(() -> dmsPanel.showAddUpdateGroupPane(group, false));

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void addUpdateGroupRequested(String groupName, Set<Long> selectedIds) {

		taskQueue.execute(() -> {

			try {

				Dgroup group = model.getGroupToBeUpdated();
				model.setGroupToBeUpdated(null);

				Set<Contact> selectedContacts = new HashSet<Contact>();
				selectedIds.forEach(id -> {
					try {
						selectedContacts.add(getContact(id));
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

				if (group == null) {
					// New group

					group = new Dgroup();
					group.setOwner(model.getIdentity());
					group.setLocal(true);

					final Dgroup newGroup = createUpdateGroup(group, groupName, true, selectedContacts, null);

					// Gruba eleman ekleme mesajini olusturup grup uyelerine gonder

					String groupUpdate = getGroupUpdate(newGroup.getName(), true, newGroup.getMembers(), null);

					sendGroupMessage(createOutgoingUpdate(groupUpdate, UpdateType.UPDATE_GROUP, newGroup.getOwner(),
							newGroup, createStatusReports(newGroup)));

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

					Dgroup newGroup = createUpdateGroup(group, newGroupName, group.isActive(), contactsToBeAdded,
							contactsToBeRemoved);

					if (!contactsToBeAdded.isEmpty()) {

						String groupUpdateToAddedContacts = getGroupUpdate(newGroup.getName(), true,
								newGroup.getMembers(), null);

						sendGroupMessage(
								createOutgoingUpdate(groupUpdateToAddedContacts, UpdateType.UPDATE_GROUP,
										newGroup.getOwner(), newGroup, createStatusReports(contactsToBeAdded)),
								contactsToBeAdded);

					}

					if (!contactsToBeRemoved.isEmpty()) {

						String groupUpdateToRemovedContacts = getGroupUpdate(null, false, null, null);

						sendGroupMessage(
								createOutgoingUpdate(groupUpdateToRemovedContacts, UpdateType.UPDATE_GROUP,
										newGroup.getOwner(), newGroup, createStatusReports(contactsToBeRemoved)),
								contactsToBeRemoved);

					}

					if (!residentContacts.isEmpty()) {

						String groupUpdateToResidentContacts = getGroupUpdate(newGroupName, true, contactsToBeAdded,
								contactsToBeRemoved);

						sendGroupMessage(
								createOutgoingUpdate(groupUpdateToResidentContacts, UpdateType.UPDATE_GROUP,
										newGroup.getOwner(), newGroup, createStatusReports(residentContacts)),
								residentContacts);

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

				Dgroup newGroup = createUpdateGroup(group, null, group.isActive(), null, null);

				String groupUpdate = getGroupUpdate(null, newGroup.isActive(), null, null);

				sendGroupMessage(createOutgoingUpdate(groupUpdate, UpdateType.UPDATE_GROUP, newGroup.getOwner(),
						newGroup, createStatusReports(contacts)), contacts);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void fileSelected(final Path file) {

		dmsPanel.addAttachment(new FileBuilder(file.getFileName().toString(), AttachmentType.FILE, null,
				() -> copyFileToSendFolder(file)));

	}

	@Override
	public void attachmentClicked(Long messageId) {

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessageById(messageId);

				String attachmentPath = message.getAttachmentPath();

				if (attachmentPath == null)
					return;

				Path file = Paths.get(attachmentPath);

				if (Commons.AUTO_OPEN_FILE) {

					new ProcessBuilder().directory(file.getParent().toFile())
							.command("cmd", "/C", file.getFileName().toString()).start();

				} else {

					dmsListeners.forEach(listener -> listenerTaskQueue.execute(() -> listener.fileClicked(file)));

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

				Dgroup group = message.getDgroup();

				if (message == null || group == null || !message.isLocal())
					return;

				model.setDetailedGroupMessageId(messageId);

				List<Contact> contacts = new ArrayList<Contact>();

				Long groupOwnerId = group.getOwner().getId();

				message.getStatusReports().forEach(statusReport -> {

					if (Objects.equals(statusReport.getContactId(), groupOwnerId))
						return;

					try {
						contacts.add(getContact(statusReport.getContactId()));
					} catch (Exception e) {
						e.printStackTrace();
					}

				});

				contacts.sort(model.getContactSorter());

				message.getStatusReports().stream()
						.filter(statusReport -> Objects.equals(statusReport.getContactId(), groupOwnerId))
						.forEach(statusReport -> {

							try {
								contacts.add(0, getContact(groupOwnerId));
							} catch (Exception e) {
								e.printStackTrace();
							}

						});

				Platform.runLater(() -> dmsPanel.showStatusInfoPane(contacts));

				message.getStatusReports().forEach(statusReport -> Platform.runLater(() -> dmsPanel
						.updateDetailedMessageStatus(statusReport.getContactId(), statusReport.getMessageStatus())));

				Map<Long, Integer> groupMessageProgresses = model.getGroupMessageProgresses(messageId);
				if (groupMessageProgresses != null)
					groupMessageProgresses.forEach((contactId, progress) -> Platform
							.runLater(() -> dmsPanel.updateDetailedMessageProgress(contactId, progress)));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void forwardMessagesRequested(final EntityId entityId, final Long[] messageIds) {

		taskQueue.execute(() -> {

			try {

				final Contact contact = entityId.isGroup() ? null : getContact(entityId.getId());
				final Dgroup group = entityId.isGroup() ? getGroup(entityId.getId()) : null;

				List<Message> forwardedMessages = new ArrayList<Message>();

				dbManager.getMessagesById(messageIds).forEach(message -> {

					if (message.getViewStatus() == ViewStatus.DELETED) {
						return;
					}

					try {

						Message outgoingMessage = new Message(message.getContent(), null, message.getMessageCode(),
								MessageStatus.FRESH, group == null ? contact : group.getOwner(), model.getIdentity(),
								group, null);

						outgoingMessage.setLocal(true);

						outgoingMessage.setAttachmentType(message.getAttachmentType());
						outgoingMessage.setAttachmentName(message.getAttachmentName());
						outgoingMessage.setAttachmentPath(message.getAttachmentPath());

						if (group != null) {
							createStatusReports(group)
									.forEach(statusReport -> outgoingMessage.addStatusReport(statusReport));
						}

						outgoingMessage.setForwardCount(message.getForwardCount());
						if (outgoingMessage.getForwardCount() == null) {
							outgoingMessage.setForwardCount(0);
						}
						if (!message.isLocal()) {
							outgoingMessage.setForwardCount(outgoingMessage.getForwardCount() + 1);
						}

						Message newMessage = dbManager.addUpdateMessage(outgoingMessage);
						model.registerMessage(newMessage);

						forwardedMessages.add(newMessage);

						addMessageToPane(newMessage, true);

						if (entityId.isGroup()) {
							sendGroupMessage(newMessage);
						} else {
							sendPrivateMessage(newMessage);
						}

						Path attachmentPath = null;
						if (newMessage.getAttachmentPath() != null) {
							attachmentPath = Paths.get(newMessage.getAttachmentPath());
						}

						messageSentToListeners(newMessage, attachmentPath, contact == null ? null : contact.getId(),
								group == null ? null : group.getId());

					} catch (Exception e) {

						e.printStackTrace();

					}

				});

				if (!forwardedMessages.isEmpty()) {
					Platform.runLater(() -> {
						dmsPanel.showMessagePane(entityId);
						dmsPanel.scrollPaneToMessage(entityId, forwardedMessages.get(0).getId());
					});
				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void archiveMessagesRequested(final Long[] messageIds) {

		taskQueue.execute(() -> {

			try {

				archiveMessages(dbManager.getMessagesById(messageIds));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void deleteMessagesRequested(final Long[] messageIds) {

		taskQueue.execute(() -> {

			try {

				List<Message> messages = dbManager.getMessagesById(messageIds).stream()
						.filter(message -> message.getViewStatus() != ViewStatus.ARCHIVED).collect(Collectors.toList());
				if (messages.isEmpty())
					return;

				final Long[] deletedMessageIds = deleteMessages(messages);

				if (deletedMessageIds.length > 0) {
					dmsGuiListeners.forEach(listener -> listenerTaskQueue
							.execute(() -> listener.guiMessagesDeleted(deletedMessageIds)));
				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void clearConversationRequested() {

		taskQueue.execute(() -> {

			EntityId entityId = model.getOpenEntityId();
			if (entityId == null)
				return;

			final Long id = entityId.getId();

			try {

				if (entityId.isGroup())
					clearGroupConversationRequested(id);
				else
					clearPrivateConversationRequested(id);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	private void clearPrivateConversationRequested(final Long contactId) throws Exception {

		List<Message> deletableMessages = dbManager.getAllDeletablePrivateMessages(contactId);
		if (deletableMessages.isEmpty())
			return;

		final Long[] deletedMessageIds = deleteMessages(deletableMessages);

		if (deletedMessageIds.length > 0) {
			dmsGuiListeners.forEach(listener -> listenerTaskQueue.execute(
					() -> listener.guiPrivateConversationCleared(ContactIdImpl.of(contactId), deletedMessageIds)));
		}

	}

	private void clearGroupConversationRequested(final Long groupId) throws Exception {

		List<Message> deletableMessages = dbManager.getAllDeletableGroupMessages(groupId);
		if (deletableMessages.isEmpty())
			return;

		final Long[] deletedMessageIds = deleteMessages(deletableMessages);

		if (deletedMessageIds.length > 0) {
			dmsGuiListeners.forEach(listener -> listenerTaskQueue
					.execute(() -> listener.guiGroupConversationCleared(GroupIdImpl.of(groupId), deletedMessageIds)));
		}

	}

	@Override
	public void statusInfoClosed() {

		taskQueue.execute(() -> {

			model.setDetailedGroupMessageId(-1L);

		});

	}

	@Override
	public void addIpClicked(String ip) {

		try {

			dmsClient.addRemoteIps(InetAddress.getByName(ip));

		} catch (UnknownHostException e) {

		}

	}

	@Override
	public void removeIpClicked(String ip) {

		try {

			dmsClient.removeRemoteIps(InetAddress.getByName(ip));

		} catch (UnknownHostException e) {

		}

	}

	@Override
	public void recordButtonPressed() {

		taskQueue.execute(() -> {

			try {

				audioCenter.prepareRecording();

			} catch (Exception e) {

				Platform.runLater(() -> dmsPanel.recordingStopped());

				e.printStackTrace();

			}

		});

	}

	@Override
	public void recordEventTriggered(final Long refMessageId) {

		taskQueue.execute(() -> {

			EntityId entityId = model.getOpenEntityId();
			if (entityId == null)
				return;

			try {

				String fileName = String.format("audio_%s.wav",
						Commons.DATE_TIME_FORMATTER.format(LocalDateTime.now()));
				Path dstFolder = Paths.get(Commons.SEND_FOLDER);

				Path dstFile = getDstFile(dstFolder, fileName);

				audioCenter.startRecording(entityId, dstFile, refMessageId);

			} catch (Exception e) {

				Platform.runLater(() -> dmsPanel.recordingStopped());

				e.printStackTrace();

			}

		});

	}

	@Override
	public void recordButtonReleased() {

		taskQueue.execute(() -> {

			audioCenter.stopRecording();

		});

	}

	@Override
	public void recordingStopped(final EntityId entityId, final Path path, final Long refId) {

		taskQueue.execute(() -> {

			Platform.runLater(() -> dmsPanel.recordingStopped());

			boolean recordSuccessful = path != null && Files.exists(path);

			if (!recordSuccessful)
				return;

			FileBuilder fileBuilder = new FileBuilder(path.getFileName().toString(), AttachmentType.AUDIO, null,
					() -> path);

			try {

				if (entityId.isGroup()) {
					sendGroupMessageClaimed(null, fileBuilder, refId, entityId.getId(), null, true);
				} else {
					sendPrivateMessageClaimed(null, fileBuilder, refId, entityId.getId(), null, true);
				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void reportClicked() {

		Platform.runLater(() -> reportsDialog.display());

	}

	@Override
	public void sendReportClicked(final Integer reportId, final String reportHeading,
			final List<String> reportParagraphs) {

		Platform.runLater(() -> {

			reportsDialog.hideAndReset();

			final String fileName = String.format("%s_%s.pdf", reportHeading,
					Commons.DATE_TIME_FORMATTER.format(LocalDateTime.now()));

			dmsPanel.addAttachment(new FileBuilder(fileName, AttachmentType.REPORT, reportId, () -> {

				Path dstFolder = Paths.get(Commons.SEND_FOLDER);

				Path dstFile = getDstFile(dstFolder, fileName);

				Commons.writeReport(dstFile, reportHeading, reportParagraphs);

				if (dstFile != null && Files.exists(dstFile))
					return dstFile;

				return null;

			}));

		});

	}

	@Override
	public void showEntityRequested(final EntityId entityId) {

		taskQueue.execute(() -> {

			try {

				updateViewStatusRequested(entityId, ViewStatus.DEFAULT);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void hideEntityRequested(final EntityId entityId) {

		taskQueue.execute(() -> {

			try {

				// Messages will be read normally, but as a precaution...
				Set<Message> unreadMessagesOfEntity = model.getUnreadMessagesOfEntity(entityId);
				if (unreadMessagesOfEntity != null) {
					if (entityId.isGroup())
						groupMessagesRead(entityId, unreadMessagesOfEntity);
					else
						privateMessagesRead(entityId, unreadMessagesOfEntity);
				}

				updateViewStatusRequested(entityId, ViewStatus.ARCHIVED);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void removeEntityRequested(final EntityId entityId) {

		taskQueue.execute(() -> {

			final Long id = entityId.getId();

			try {

				// Messages will be read normally, but as a precaution...
				Set<Message> unreadMessagesOfEntity = model.getUnreadMessagesOfEntity(entityId);
				if (unreadMessagesOfEntity != null) {
					if (entityId.isGroup())
						groupMessagesRead(entityId, unreadMessagesOfEntity);
					else
						privateMessagesRead(entityId, unreadMessagesOfEntity);
				}

				// Delete all messages
				if (entityId.isGroup()) {
					List<Message> deletableMessages = dbManager.getAllDeletableGroupMessages(id);
					final Long[] deletedMessageIds = deletableMessages.isEmpty() ? new Long[0]
							: deleteMessages(deletableMessages);
					dmsGuiListeners.forEach(listener -> listenerTaskQueue.execute(
							() -> listener.guiGroupConversationDeleted(GroupIdImpl.of(id), deletedMessageIds)));
				} else {
					List<Message> deletableMessages = dbManager.getAllDeletablePrivateMessages(id);
					final Long[] deletedMessageIds = deletableMessages.isEmpty() ? new Long[0]
							: deleteMessages(deletableMessages);
					dmsGuiListeners.forEach(listener -> listenerTaskQueue.execute(
							() -> listener.guiPrivateConversationDeleted(ContactIdImpl.of(id), deletedMessageIds)));
				}

				updateViewStatusRequested(entityId, ViewStatus.DELETED);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	private void updateViewStatusRequested(EntityId entityId, ViewStatus status) throws Exception {

		if (entityId.isGroup()) {

			Dgroup group = getGroup(entityId.getId());
			if (group == null || group.getStatus() != Availability.OFFLINE)
				return;

			group.setViewStatus(status);
			Dgroup newGroup = dbManager.addUpdateGroup(group);
			model.addUpdateGroup(newGroup);

			Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

		} else {

			Contact contact = getContact(entityId.getId());
			if (contact.getStatus() != Availability.OFFLINE)
				return;

			contact.setViewStatus(status);
			Contact newContact = dbManager.addUpdateContact(contact);
			model.addUpdateContact(newContact);

			Platform.runLater(() -> dmsPanel.updateContact(newContact));

		}

	}

	@Override
	public void moreArchivedMessagesRequested(Long minMessageId) {

		taskQueue.execute(() -> {

			try {

				archivedMessagesLoaded(dbManager.getLastArchivedMessagesBeforeId(minMessageId, MIN_MESSAGES_PER_PAGE));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void searchRequested(final String fulltext) {

		taskQueue.execute(() -> {

			EntityId entityId = model.getOpenEntityId();
			if (entityId == null)
				return;

			final Long id = entityId.getId();

			try {

				if (entityId.isGroup()) {
					List<Message> hits = dbManager.searchInGroupConversation(id, fulltext);
					Platform.runLater(() -> dmsPanel.showSearchResults(hits));
				} else {
					List<Message> hits = dbManager.searchInPrivateConversation(id, fulltext);
					Platform.runLater(() -> dmsPanel.showSearchResults(hits));
				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void archiveSearchRequested(final String fulltext) {

		taskQueue.execute(() -> {

			try {

				List<Message> hits = dbManager.searchInArchivedMessages(fulltext);
				Platform.runLater(() -> dmsPanel.showArchiveSearchResults(hits));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void archivedMessagesClaimed(final Long lastMessageIdExcl, final Long firstMessageIdIncl) {

		taskQueue.execute(() -> {

			try {

				long bottomMessageId = lastMessageIdExcl;

				List<Message> lastMessagesBetweenIds = new ArrayList<Message>();

				while (firstMessageIdIncl < bottomMessageId) {

					List<Message> lastMessagesBeforeId = dbManager.getLastArchivedMessagesBeforeId(bottomMessageId,
							MIN_MESSAGES_PER_PAGE);

					if (lastMessagesBeforeId.isEmpty())
						break;

					lastMessagesBetweenIds.addAll(lastMessagesBeforeId);

					bottomMessageId = lastMessagesBeforeId.get(lastMessagesBeforeId.size() - 1).getId();

				}

				archivedMessagesLoaded(lastMessagesBetweenIds);

				Platform.runLater(() -> dmsPanel.scrollArchivePaneToMessage(firstMessageIdIncl));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void searchInAllMessagesClaimed(String fulltext) {

		taskQueue.execute(() -> {

			try {

				List<Message> hits = dbManager.searchInAllMessages(fulltext);
				Platform.runLater(() -> dmsPanel.showSearchInAllMessagesResults(hits));

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
	public void logout() {

		logoutListener.run();
		dmsClient.close();
		sessionLock.acquireUninterruptibly();

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
	public void addDownloadListener(DmsDownloadListener downloadListener) {

		dmsDownloadListeners.add(downloadListener);

	}

	@Override
	public void removeDownloadListener(DmsDownloadListener downloadListener) {

		dmsDownloadListeners.remove(downloadListener);

	}

	@Override
	public void registerFileServer(DmsFileServer fileServer) {

		dmsFileServer.set(fileServer);

	}

	@Override
	public void unregisterFileServer() {

		dmsFileServer.set(null);

	}

	@Override
	public void setCoordinates(Double latitude, Double longitude) throws UnsupportedOperationException {

		if (latitude != null && (latitude < -90.0 || latitude > 90.0))
			throw new UnsupportedOperationException("Invalid coordinates.");

		if (longitude != null && (longitude < -180.0 || longitude > 180.0))
			throw new UnsupportedOperationException("Invalid coordinates.");

		taskQueue.execute(() -> {

			Contact identity = model.getIdentity();
			Double oldLatitude = identity.getLatitude();
			Double oldLongitude = identity.getLongitude();

			if (Objects.equals(oldLatitude, latitude) && Objects.equals(oldLongitude, longitude)) {
				return;
			}

			try {
				identity.setLatitude(latitude);
				identity.setLongitude(longitude);
				Contact newIdentity = dbManager.updateIdentity(identity);
				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));
				sendBeacon(null, null, null, latitude, longitude, null);
			} catch (Exception e) {
				model.updateCoordinates(oldLatitude, oldLongitude);
			}

		});

	}

	@Override
	public void setComment(String comment) throws UnsupportedOperationException {

		if (comment != null && comment.length() > 40)
			throw new UnsupportedOperationException("Comment cannot exceed 40 characters.");

		Platform.runLater(() -> dmsPanel.setCommentEditable(false));

		commentUpdateRequested(comment);

	}

	@Override
	public void setAvailability(Availability availability) throws UnsupportedOperationException {

		switch (availability) {
		case AVAILABLE:
		case AWAY:
		case BUSY:
			break;
		default:
			throw new UnsupportedOperationException("Only AVAILABLE, AWAY and BUSY allowed.");
		}

		statusUpdateRequested(availability);

	}

	@Override
	public void setSecretId(final String secretId) throws UnsupportedOperationException {

		if (secretId != null && secretId.length() > 16)
			throw new UnsupportedOperationException("Secret ID cannot exceed 16 characters.");

		taskQueue.execute(() -> {

			Contact identity = model.getIdentity();
			String oldSecretId = identity.getSecretId();

			if (Objects.equals(oldSecretId, secretId)) {
				return;
			}

			try {
				identity.setSecretId(secretId);
				dbManager.updateIdentity(identity);
				sendBeacon(null, null, null, null, null, secretId);
			} catch (Exception e) {
				model.updateSecretId(oldSecretId);
			}

		});

	}

	@Override
	public void addRemoteIps(InetAddress... remoteIps) {

		if (remoteIps.length == 0)
			return;

		taskQueue.execute(() -> {

			Runnable task = () -> dmsClient.addRemoteIps(remoteIps);

			if (model.isServerConnected())
				task.run();
			else
				model.addServerTaskToQueue(task);

		});

	}

	@Override
	public void clearRemoteIps() {

		taskQueue.execute(() -> {

			Runnable task = () -> dmsClient.removeRemoteIps();

			if (model.isServerConnected())
				task.run();
			else
				model.addServerTaskToQueue(task);

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
	public ContactSelectionHandle getActiveContactsHandle() {

		return activeContactsHandle;

	}

	@Override
	public ContactHandle getContactHandle(ContactId contactId) {

		try {
			return new ContactHandleImpl(getContact(contactId.getValue()));
		} catch (Exception e) {

		}

		return null;

	}

	@Override
	public GroupHandle getGroupHandle(GroupId groupId) {

		try {
			Dgroup group = getGroup(groupId.getValue());
			if (group != null) {
				return new GroupHandleImpl(group);
			}
		} catch (Exception e) {

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
	public List<ContactHandle> getContactHandles(final Predicate<ContactHandle> filter) {

		List<ContactHandle> contactHandles = new ArrayList<ContactHandle>();

		model.getContacts().forEach((id, contact) -> {
			ContactHandle contactHandle = new ContactHandleImpl(contact);
			if (filter.test(contactHandle))
				contactHandles.add(contactHandle);
		});

		return contactHandles;

	}

	@Override
	public List<GroupHandle> getGroupHandles(final Predicate<GroupHandle> filter) {

		List<GroupHandle> groupHandles = new ArrayList<GroupHandle>();

		model.getGroups().forEach((id, group) -> {
			GroupHandle groupHandle = new GroupHandleImpl(group);
			if (filter.test(groupHandle))
				groupHandles.add(groupHandle);
		});

		return groupHandles;

	}

	@Override
	public List<ContactId> getIdsByServerIp(InetAddress remoteServerIp) {

		return model.getIdsByServerIp(remoteServerIp);

	}

	@Override
	public List<ContactId> getIdsByServerIpAndName(InetAddress remoteServerIp, String name) {

		return model.getIdsByServerIpAndName(remoteServerIp, name);

	}

	@Override
	public List<ContactId> getIdsByServerIpAndSecretId(InetAddress remoteServerIp, String secretId) {

		return model.getIdsByServerIpAndSecretId(remoteServerIp, secretId);

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

		return new ObjectHandleImpl(objectCode, DmsPackingFactory.pack(object));

	}

	@Override
	public <T> ListHandle createListHandle(List<T> list, Integer listCode) {

		return new ListHandleImpl(listCode, DmsPackingFactory.pack(list));

	}

	@Override
	public MessageRules createMessageRules() {

		return new MessageRulesImpl();

	}

	@Override
	public boolean sendMessageToContacts(MessageHandle messageHandle, List<ContactId> contactIds) {

		return sendMessageToContacts(messageHandle, contactIds, null);

	}

	@Override
	public boolean sendMessageToGroup(MessageHandle messageHandle, GroupId groupId) {

		return sendMessageToGroup(messageHandle, groupId, null);

	}

	@Override
	public boolean sendMessageToContacts(MessageHandle messageHandle, List<ContactId> contactIds,
			MessageRules messageRules) {

		if (!model.isServerConnected()) {
			return false;
		}

		List<String> contactUuids = new ArrayList<String>();

		contactIds.forEach(contactId -> {
			try {
				contactUuids.add(getContact(contactId.getValue()).getUuid());
			} catch (Exception e) {

			}
		});

		if (contactUuids.isEmpty()) {
			return false;
		}

		sendMessageToUuids(messageHandle, contactUuids, messageRules);

		return true;

	}

	@Override
	public boolean sendMessageToGroup(MessageHandle messageHandle, GroupId groupId, MessageRules messageRules) {

		if (!model.isServerConnected()) {
			return false;
		}

		Dgroup group = null;
		try {
			group = getGroup(groupId.getValue());
		} catch (Exception e) {

		}

		if (group == null) {
			return false;
		}

		List<String> contactUuids = new ArrayList<String>();

		group.getMembers().forEach(contact -> contactUuids.add(contact.getUuid()));
		if (!group.isLocal()) {
			contactUuids.add(group.getOwner().getUuid());
		}

		if (contactUuids.isEmpty()) {
			return false;
		}

		sendMessageToUuids(messageHandle, contactUuids, messageRules);

		return true;

	}

	private void sendMessageToUuids(MessageHandle messageHandle, List<String> contactUuids, MessageRules messageRules) {

		MessageHandleImpl outgoingMessage = new MessageHandleImpl(messageHandle);

		MessageRulesImpl messageRulesImpl = messageRules == null ? new MessageRulesImpl()
				: (MessageRulesImpl) messageRules;

		dmsClient.sendTransientMessage(outgoingMessage, contactUuids, messageRulesImpl.getTrackingId(),
				messageRulesImpl.getTimeout(), messageRulesImpl.getLocalInterface());

	}

	@Override
	public void cancelMessage(Long trackingId) {

		dmsClient.cancelTransientMessage(trackingId);

	}

	@Override
	public Long downloadFile(Long fileId, ContactId contactId) {

		try {
			Contact contact = getContact(contactId.getValue());
			DownloadPojo downloadPojo = model.registerDownload(contact.getUuid(), fileId);
			if (contact.getStatus() != Availability.OFFLINE) {
				dmsClient.sendDownloadRequest(downloadPojo, downloadPojo.senderUuid);
			}
			return downloadPojo.downloadId;
		} catch (Exception e) {

		}

		return null;

	}

	@Override
	public void cancelDownload(Long downloadId) {

		downloadTaskQueue.execute(() -> {

			DownloadPojo downloadPojo = model.removeDownload(downloadId);
			if (downloadPojo == null) {
				return;
			}
			if (!downloadPojo.pausing.get() && !downloadPojo.paused.get()
					&& model.isContactOnline(downloadPojo.senderUuid)) {
				dmsClient.cancelDownloadRequest(downloadId, downloadPojo.senderUuid);
			}
			deleteFile(downloadPojo.path);

		});

	}

	@Override
	public void pauseDownload(Long downloadId) {

		downloadTaskQueue.execute(() -> {

			DownloadPojo downloadPojo = model.getDownload(downloadId);
			if (downloadPojo == null || downloadPojo.pausing.get() || downloadPojo.paused.get()) {
				return;
			}
			downloadPojo.pausing.set(true);
			if (model.isContactOnline(downloadPojo.senderUuid)) {
				dmsClient.cancelDownloadRequest(downloadId, downloadPojo.senderUuid);
			}

		});

	}

	@Override
	public void resumeDownload(Long downloadId) {

		downloadTaskQueue.execute(() -> {

			DownloadPojo downloadPojo = model.getDownload(downloadId);
			if (downloadPojo == null || !downloadPojo.paused.get()) {
				return;
			}
			downloadPojo.paused.set(false);
			if (model.isContactOnline(downloadPojo.senderUuid)) {
				dmsClient.sendDownloadRequest(downloadPojo, downloadPojo.senderUuid);
			}

		});

	}

	@Override
	public Future<Long> sendGuiMessageToContact(final String message, final ContactId contactId) {

		return newUncancellableFuture(taskQueue.submit(() -> {

			return sendPrivateMessageClaimed(message, null, null, contactId.getValue(), Boolean.TRUE, false);

		}));

	}

	@Override
	public Future<Long> sendGuiMessageToGroup(final String message, final GroupId groupId) {

		return newUncancellableFuture(taskQueue.submit(() -> {

			return sendGroupMessageClaimed(message, null, null, groupId.getValue(), Boolean.TRUE, false);

		}));

	}

	@Override
	public Future<Long> sendGuiFileToContact(final String message, final Path path, final ContactId contactId) {

		return newUncancellableFuture(taskQueue.submit(() -> {

			FileBuilder fileBuilder = new FileBuilder(path.getFileName().toString(), AttachmentType.FILE, null,
					() -> copyFileToSendFolder(path));

			return sendPrivateMessageClaimed(message, fileBuilder, null, contactId.getValue(), Boolean.TRUE, false);

		}));

	}

	@Override
	public Future<Long> sendGuiFileToGroup(final String message, final Path path, final GroupId groupId) {

		return newUncancellableFuture(taskQueue.submit(() -> {

			FileBuilder fileBuilder = new FileBuilder(path.getFileName().toString(), AttachmentType.FILE, null,
					() -> copyFileToSendFolder(path));

			return sendGroupMessageClaimed(message, fileBuilder, null, groupId.getValue(), Boolean.TRUE, false);

		}));

	}

	@Override
	public Future<Long> sendGuiReportToContact(final String message, final Integer reportId, final Path path,
			final ContactId contactId) {

		return newUncancellableFuture(taskQueue.submit(() -> {

			FileBuilder fileBuilder = new FileBuilder(path.getFileName().toString(), AttachmentType.REPORT, reportId,
					() -> copyFileToSendFolder(path));

			return sendPrivateMessageClaimed(message, fileBuilder, null, contactId.getValue(), Boolean.TRUE, false);

		}));

	}

	@Override
	public Future<Long> sendGuiReportToGroup(final String message, final Integer reportId, final Path path,
			final GroupId groupId) {

		return newUncancellableFuture(taskQueue.submit(() -> {

			FileBuilder fileBuilder = new FileBuilder(path.getFileName().toString(), AttachmentType.REPORT, reportId,
					() -> copyFileToSendFolder(path));

			return sendGroupMessageClaimed(message, fileBuilder, null, groupId.getValue(), Boolean.TRUE, false);

		}));

	}

	private <T> Future<T> newUncancellableFuture(Future<T> future) {

		return new Future<T>() {

			@Override
			public boolean cancel(boolean arg0) {
				return false;
			}

			@Override
			public T get() throws InterruptedException, ExecutionException {
				return future.get();
			}

			@Override
			public T get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
				return future.get(arg0, arg1);
			}

			@Override
			public boolean isCancelled() {
				return future.isCancelled();
			}

			@Override
			public boolean isDone() {
				return future.isDone();
			}

		};

	}

	@Override
	public void clearGuiPrivateConversation(final ContactId contactId) {

		taskQueue.execute(() -> {

			try {
				clearPrivateConversationRequested(contactId.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

	@Override
	public void clearGuiGroupConversation(final GroupId groupId) {

		taskQueue.execute(() -> {

			try {
				clearGroupConversationRequested(groupId.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

	@Override
	public void markPrivateMessagesAsRead(ContactId contactId) {

		taskQueue.execute(() -> {

			try {
				messagesRead(getContact(contactId.getValue()).getEntityId());
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

	@Override
	public void markGroupMessagesAsRead(GroupId groupId) {

		taskQueue.execute(() -> {

			try {
				messagesRead(getGroup(groupId.getValue()).getEntityId());
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

	@Override
	public void switchAudio(boolean on) {

		model.setAudioOn(on);

	}

	@Override
	public boolean isServerConnected() {

		return model.isServerConnected();

	}

}

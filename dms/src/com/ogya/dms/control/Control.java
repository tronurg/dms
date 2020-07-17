package com.ogya.dms.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JComponent;

import org.hibernate.HibernateException;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.CommonConstants;
import com.ogya.dms.database.DbManager;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.dmsclient.DmsClient;
import com.ogya.dms.dmsclient.intf.DmsClientListener;
import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.exceptions.DbException;
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
import com.ogya.dms.view.DmsPanel;
import com.ogya.dms.view.intf.AppListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Control implements AppListener, DmsClientListener, DmsHandle {

	private static final Map<String, Control> INSTANCES = Collections.synchronizedMap(new HashMap<String, Control>());

	private static final int MIN_MESSAGES_PER_PAGE = 50;

	private final DbManager dbManager;

	private final Model model;

	private final DmsPanel dmsPanel;
	private final JFXPanel dmsPanelSwing;

	private final DmsClient dmsClient;

	private final Object beaconSyncObj = new Object();

	private final List<DmsListener> dmsListeners = Collections.synchronizedList(new ArrayList<DmsListener>());

	private final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {
			Thread thread = new Thread(arg0);
			thread.setDaemon(true);
			return thread;
		}

	});

	private Control(String username, String password) throws DbException {

		dbManager = new DbManager(username, password);

		Identity identity = dbManager.getIdentity();

		model = new Model(identity);

		dmsPanelSwing = new JFXPanel();
		dmsPanel = new DmsPanel();

		dmsPanel.addListener(this);

		initDatabase();
		initModel();
		initGUI();

		dmsClient = new DmsClient(identity.getUuid(), CommonConstants.SERVER_IP, CommonConstants.SERVER_PORT, this);

		init();

	}

	public static Control getInstance(String username, String password) throws DbException {

		INSTANCES.putIfAbsent(username, new Control(username, password));

		return INSTANCES.get(username);

	}

	private void init() {

		Thread publishBeaconThread = new Thread(this::publishBeacon);
		publishBeaconThread.setDaemon(true);
		publishBeaconThread.start();

	}

	private void initDatabase() {

		dbManager.fetchAllContacts().forEach(contact -> {
			contact.setStatus(Availability.OFFLINE);
			dbManager.addUpdateContact(contact);
		});

		dbManager.fetchAllGroups().forEach(group -> {
			if (!model.getLocalUuid().equals(group.getOwnerUuid())) {
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
			dmsPanelSwing.setScene(new Scene(dmsPanel));
			dmsPanel.setIdentity(model.getIdentity());
		});

		// Add contacts
		model.getContacts().forEach((uuid, contact) -> Platform.runLater(() -> dmsPanel.updateContact(contact)));

		// Add private messages
		model.getContacts().forEach((uuid, contact) -> {

			String remoteUuid = uuid;

			List<Message> dbMessages = new ArrayList<Message>();

			dbMessages.addAll(dbManager.getAllPrivateMessagesSinceFirstUnreadMessage(model.getLocalUuid(), remoteUuid));

			if (dbMessages.size() == 0) {

				dbManager.getLastPrivateMessages(model.getLocalUuid(), remoteUuid, MIN_MESSAGES_PER_PAGE)
						.forEach(message -> dbMessages.add(0, message)); // inverse order

			} else if (dbMessages.size() < MIN_MESSAGES_PER_PAGE) {

				dbManager
						.getLastPrivateMessagesBeforeId(model.getLocalUuid(), remoteUuid, dbMessages.get(0).getId(),
								MIN_MESSAGES_PER_PAGE - dbMessages.size())
						.forEach(message -> dbMessages.add(0, message)); // inverse order

			}

			dbMessages.forEach(message -> addPrivateMessageToPane(message, true));

		});

		// Add groups
		model.getGroups().forEach((uuid, group) -> Platform.runLater(() -> dmsPanel.updateGroup(group)));

		// Add group messages
		model.getGroups().forEach((uuid, group) -> {

			String groupUuid = uuid;

			List<Message> dbMessages = new ArrayList<Message>();

			dbMessages.addAll(dbManager.getAllGroupMessagesSinceFirstUnreadMessage(model.getLocalUuid(), groupUuid));

			if (dbMessages.size() == 0) {

				dbManager.getLastGroupMessages(groupUuid, MIN_MESSAGES_PER_PAGE)
						.forEach(message -> dbMessages.add(0, message)); // inverse order

			} else if (dbMessages.size() < MIN_MESSAGES_PER_PAGE) {

				dbManager
						.getLastGroupMessagesBeforeId(groupUuid, dbMessages.get(0).getId(),
								MIN_MESSAGES_PER_PAGE - dbMessages.size())
						.forEach(message -> dbMessages.add(0, message)); // inverse order

			}

			dbMessages.forEach(message -> addGroupMessageToPane(message, true));

		});

	}

	private void addPrivateMessageToPane(final Message message, final boolean newMessageToBottom) {

		if (model.getLocalUuid().equals(message.getOwnerUuid())) {

			final String remoteUuid = message.getReceiverUuid();

			model.addMessageId(remoteUuid, message.getId());

			Platform.runLater(() -> {
				if (newMessageToBottom)
					dmsPanel.addMessageToBottom(message, "", MessageDirection.OUTGOING, remoteUuid);
				else
					dmsPanel.addMessageToTop(message, "", MessageDirection.OUTGOING, remoteUuid);
			});

		} else if (model.getLocalUuid().equals(message.getReceiverUuid())) {

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

		if (model.getLocalUuid().equals(message.getOwnerUuid())) {

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

	private void publishBeacon() {

		while (true) {

			synchronized (beaconSyncObj) {

				if (model.isServerConnected())
					dmsClient.sendBeacon(model.getIdentity().toJson());

				try {

					beaconSyncObj.wait(CommonConstants.BEACON_INTERVAL_MS);

				} catch (InterruptedException e) {

				}

			}

		}

	}

	private void contactDisconnected(Contact contact) {

		taskQueue.execute(() -> {

			contact.setStatus(Availability.OFFLINE);

			try {

				final Contact newContact = dbManager.addUpdateContact(contact);

				model.addContact(newContact);

				Platform.runLater(() -> dmsPanel.updateContact(newContact));

			} catch (HibernateException e) {

				e.printStackTrace();

			}

			try {

				dbManager.getAllActiveGroupsOfUuid(contact.getUuid()).forEach(group -> {

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

	private Message createOutgoingMessage(String messageTxt, String receiverUuid, ReceiverType receiverType,
			MessageType messageType, Integer messageCode) throws Exception {

		Message outgoingMessage = new Message(model.getLocalUuid(), receiverUuid, receiverType, messageType,
				messageTxt);

		outgoingMessage.setSenderUuid(model.getLocalUuid());

		if (messageCode != null)
			outgoingMessage.setMessageCode(messageCode);

		outgoingMessage.setMessageStatus(MessageStatus.FRESH);

		if (receiverType.equals(ReceiverType.GROUP) && model.getGroup(receiverUuid) != null) {

			Dgroup group = model.getGroup(receiverUuid);

			StatusReport statusReport = new StatusReport();

			if (!group.getOwnerUuid().equals(model.getLocalUuid()))
				statusReport.uuidStatus.put(group.getOwnerUuid(), MessageStatus.FRESH);

			group.getContacts().forEach(contact -> {

				if (contact.getUuid().equals(model.getLocalUuid()))
					return;

				statusReport.uuidStatus.put(contact.getUuid(), MessageStatus.FRESH);

			});

			outgoingMessage.setStatusReportStr(statusReport.toJson());

		}

		outgoingMessage.setWaiting(true);

		Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

		return newMessage;

	}

	private Dgroup createUpdateGroup(String groupUuid, String groupName, String masterUuid, boolean isActive,
			Set<Contact> contactsToBeAdded, Set<Contact> contactsToBeRemoved) throws Exception {

		Dgroup group = model.getGroup(groupUuid);

		if (group == null) {
			if (groupName == null || masterUuid == null)
				return null;
			group = new Dgroup(masterUuid);
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
		else if (group.getOwnerUuid().equals(model.getLocalUuid()))
			group.setStatus(Availability.AVAILABLE);
		else
			group.setStatus(
					!group.isActive() || model.getContact(group.getOwnerUuid()).getStatus().equals(Availability.OFFLINE)
							? Availability.OFFLINE
							: Availability.LIMITED);

		List<String> contactNames = new ArrayList<String>();
		group.getContacts().forEach(contact -> contactNames.add(contact.getName()));
		Collections.sort(contactNames);
		if (!group.getOwnerUuid().equals(model.getLocalUuid()))
			contactNames.add(0, model.getContact(group.getOwnerUuid()).getName());
		group.setComment(String.join(",", contactNames));

		Dgroup newGroup = dbManager.addUpdateGroup(group);

		return newGroup;

	}

	private String getGroupUpdate(String groupUuid, String groupName, boolean active, Set<Contact> contactsToBeAdded,
			Set<Contact> contactsToBeRemoved) {

		GroupUpdate groupUpdate = new GroupUpdate(groupUuid);

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

	private void sendMessage(Message message) {

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

		if (group.getOwnerUuid().equals(model.getLocalUuid())) {
			// It's my group, so I have to send this message to all the members except the
			// original sender.

			for (Contact contact : group.getContacts()) {

				String receiverUuid = contact.getUuid();

				// Skip the original sender
				if (message.getOwnerUuid().equals(receiverUuid))
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

		String messageContent = message.getContent();

		if (message.getMessageType().equals(MessageType.FILE)) {

			Path path = Paths.get(messageContent);

			try {

				byte[] fileBytes = Files.readAllBytes(path);

				message.setContent(new FilePojo(path.getFileName().toString(),
						Base64.getEncoder().encodeToString(Zstd.compress(fileBytes)), fileBytes.length).toJson());

			} catch (ZstdException | IOException e) {

				message.setContent("");

				e.printStackTrace();

			}

		}

		runnable.run();

		if (message.getMessageType().equals(MessageType.FILE)) {

			message.setContent(messageContent);

		}

	}

	private void privateMessageReceived(Message message) {

		switch (message.getMessageType()) {

		case TEXT:
		case FILE:

			addPrivateMessageToPane(message, true);

			break;

		default:

			break;

		}

	}

	private void groupMessageReceived(Message message) {

		switch (message.getMessageType()) {

		case TEXT:
		case FILE:

			addGroupMessageToPane(message, true);

			break;

		case UPDATE:

			if (message.getMessageCode() == CommonConstants.CODE_UPDATE_GROUP) {

				try {

					GroupUpdate groupUpdate = GroupUpdate.fromJson(message.getContent());

					groupUpdateReceived(groupUpdate, message.getOwnerUuid());

				} catch (Exception e) {

					e.printStackTrace();

				}

			}

			break;

		default:

			break;

		}

	}

	private void groupUpdateReceived(final GroupUpdate groupUpdate, String masterUuid) {

		try {

			final Set<Contact> contactsToBeAdded = new HashSet<Contact>();
			final Set<Contact> contactsToBeRemoved = new HashSet<Contact>();

			if (groupUpdate.add != null) {

				groupUpdate.add.forEach((uuid, name) -> {

					if (model.getLocalUuid().equals(uuid))
						return;

					Contact contact = model.getContact(uuid);
					if (contact == null) {
						contact = new Contact();
						contact.setUuid(uuid);
						contact.setName(name);
						contact.setStatus(Availability.OFFLINE);
						try {
							Contact newContact = dbManager.addUpdateContact(contact);
							model.addContact(newContact);
							contactsToBeAdded.add(newContact);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						contactsToBeAdded.add(contact);
					}

				});

			}

			if (groupUpdate.remove != null) {

				groupUpdate.remove.forEach((uuid, name) -> {

					if (model.getLocalUuid().equals(uuid))
						return;

					Contact contact = model.getContact(uuid);
					if (contact == null) {
						contact = new Contact();
						contact.setUuid(uuid);
						contact.setName(name);
						contact.setStatus(Availability.OFFLINE);
						try {
							Contact newContact = dbManager.addUpdateContact(contact);
							model.addContact(newContact);
							contactsToBeRemoved.add(newContact);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						contactsToBeRemoved.add(contact);
					}

				});

			}

			final Dgroup newGroup = createUpdateGroup(groupUpdate.uuid, groupUpdate.name, masterUuid,
					groupUpdate.active == null ? true : groupUpdate.active, contactsToBeAdded, contactsToBeRemoved);

			if (newGroup == null)
				return;

			model.addGroup(newGroup);

			Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private MessageStatus computeMessageStatus(Message message) {

		if (message.getMessageType().equals(MessageType.UPDATE))
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

	private void updateMessageStatus(Long messageId, MessageStatus messageStatus, String[] remoteUuids,
			boolean resendIfNecessary) {

		try {

			Message dbMessage = dbManager.getMessage(messageId);

			if (dbMessage == null)
				return;

			String ownerUuid = dbMessage.getOwnerUuid();

			// Send this status to the original sender too.
			if (!model.getLocalUuid().equals(ownerUuid) && model.isContactOnline(ownerUuid))
				dmsClient.feedMessageStatus(remoteUuids, ownerUuid, dbMessage.getMessageId(), messageStatus);

			switch (dbMessage.getReceiverType()) {

			case PRIVATE:

			{

				if (remoteUuids.length != 1)
					break;

				String remoteUuid = remoteUuids[0];

				dbMessage.setMessageStatus(messageStatus);

				dbMessage.setWaiting(!messageStatus.equals(MessageStatus.READ));

				final Message newMessage = dbManager.addUpdateMessage(dbMessage);

				if (!newMessage.getMessageType().equals(MessageType.UPDATE))
					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, remoteUuid));

				if (resendIfNecessary && messageStatus.equals(MessageStatus.FRESH))
					sendMessage(newMessage);

				break;

			}

			case GROUP:

			{

				// This is a group message. I am either the group owner or the message sender.
				// If I am the group owner and the message is not received remotely, I will have
				// to re-send it.

				String groupUuid = dbMessage.getReceiverUuid();

				Dgroup group = model.getGroup(groupUuid);

				if (group == null)
					break;

				StatusReport statusReport = StatusReport.fromJson(dbMessage.getStatusReportStr());

				for (String remoteUuid : remoteUuids) {

					statusReport.uuidStatus.put(remoteUuid, messageStatus);

					if (remoteUuid.equals(group.getOwnerUuid()) && ownerUuid.equals(model.getLocalUuid()))
						dbMessage.setWaiting(!messageStatus.equals(MessageStatus.READ));

				}

				dbMessage.setStatusReportStr(statusReport.toJson());

				MessageStatus overallMessageStatus = statusReport.getOverallStatus();

				if (group.getOwnerUuid().equals(model.getLocalUuid()))
					dbMessage.setWaiting(!overallMessageStatus.equals(MessageStatus.READ));

				// If I am the owner, update the message status too
				if (ownerUuid.equals(model.getLocalUuid()))
					dbMessage.setMessageStatus(overallMessageStatus);

				final Message newMessage = dbManager.addUpdateMessage(dbMessage);

				if (!newMessage.getMessageType().equals(MessageType.UPDATE))
					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, groupUuid));

				if (resendIfNecessary && messageStatus.equals(MessageStatus.FRESH)) {
					// If the message is not received remotely and;
					// I am the group owner
					// or
					// I am the message owner and receiver is the group owner
					// then re-send this message (if flag set to true).

					for (String remoteUuid : remoteUuids) {

						if (group.getOwnerUuid().equals(model.getLocalUuid())
								|| (ownerUuid.equals(model.getLocalUuid()) && remoteUuid.equals(group.getOwnerUuid())))
							sendGroupMessage(newMessage, remoteUuid);

					}

				}

				break;

			}

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

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

				if (!wasOnline) {

					// If the contact has just been online, send all things waiting for it, adjust
					// its groups' availability.
					taskQueue.execute(() -> {

						// START WITH PRIVATE MESSAGES
						try {

							for (Message waitingMessage : dbManager.getPrivateMessagesWaitingToContact(uuid)) {

								switch (waitingMessage.getMessageStatus()) {

								case FRESH:

									sendMessage(waitingMessage);

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

							for (Message waitingMessage : dbManager.getGroupMessagesWaitingToContact(uuid)) {

								StatusReport statusReport;

								try {
									statusReport = StatusReport.fromJson(waitingMessage.getStatusReportStr());
								} catch (Exception e) {
									continue;
								}

								MessageStatus messageStatus = statusReport.uuidStatus.get(uuid);

								Dgroup group = model.getGroup(waitingMessage.getReceiverUuid());

								if (group == null)
									continue;

								if (!(group.getOwnerUuid().equals(model.getLocalUuid())
										|| group.getOwnerUuid().equals(uuid)))
									continue;

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
	public void progressReceived(final Long messageId, final String[] remoteUuids, final int progress) {
		// messageId = local database id of the message (not the message id, which is
		// the local
		// database id of the sender)

		if (messageId == null)
			return;

		taskQueue.execute(() -> {

			if (progress == 100) {

				updateMessageStatus(messageId, MessageStatus.SENT, remoteUuids, false);

			} else {

				try {

					Message message = dbManager.getMessage(messageId);

					if (message == null || message.getMessageType().equals(MessageType.UPDATE)
							|| !message.getOwnerUuid().equals(model.getLocalUuid()))
						return;

					switch (message.getReceiverType()) {

					case PRIVATE:

						if (remoteUuids.length != 1)
							break;

						String remoteUuid = remoteUuids[0];

						Platform.runLater(() -> dmsPanel.updateMessageProgress(message, remoteUuid, progress));

						break;

					case GROUP:

						String groupUuid = message.getReceiverUuid();

						Dgroup group = model.getGroup(groupUuid);

						if (group == null)
							break;

						// TODO: There is room for improvment.

						Platform.runLater(() -> dmsPanel.updateMessageProgress(message, groupUuid, progress));

						break;

					}

				} catch (Exception e) {

					e.printStackTrace();

				}

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

				if (incomingMessage.getMessageType().equals(MessageType.FILE)) {

					FilePojo filePojo = FilePojo.fromJson(incomingMessage.getContent());

					try {

						Path dstFolder = Paths.get(CommonConstants.RECEIVE_FOLDER).normalize().toAbsolutePath();

						String fileName = filePojo.fileName;

						Path dstFile = getDstFile(dstFolder, fileName);

						Files.write(dstFile, Zstd.decompress(Base64.getDecoder().decode(filePojo.fileContent),
								filePojo.originalLength));

						incomingMessage.setContent(dstFile.toString());

					} catch (Exception e) {

						incomingMessage.setContent("");

						e.printStackTrace();

					}

				}

				incomingMessage.setMessageStatus(computeMessageStatus(incomingMessage));

				incomingMessage.setWaiting(false);

				boolean messageToBeRedirected = false;

				if (incomingMessage.getReceiverType().equals(ReceiverType.GROUP)) {

					Dgroup group = model.getGroup(incomingMessage.getReceiverUuid());

					if (group != null && group.getOwnerUuid().equals(model.getLocalUuid())) {

						StatusReport statusReport = new StatusReport();

						group.getContacts().forEach(contact -> {

							if (contact.getUuid().equals(incomingMessage.getOwnerUuid()))
								return;

							statusReport.uuidStatus.put(contact.getUuid(), MessageStatus.FRESH);

						});

						incomingMessage.setStatusReportStr(statusReport.toJson());

						messageToBeRedirected = statusReport.uuidStatus.size() > 0;

						incomingMessage.setWaiting(messageToBeRedirected);

					}

				}

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

			synchronized (beaconSyncObj) {

				beaconSyncObj.notify();

			}

			dmsClient.claimAllBeacons();

		} else {

			model.getContacts().forEach((uuid, contact) -> {

				contactDisconnected(contact);

			});

		}

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

			updateMessageStatus(messageId, messageStatus, remoteUuids, true);

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

				if (dbMessage == null)
					return;

				String groupUuid = dbMessage.getReceiverUuid();

				StatusReport statusReport = StatusReport.fromJson(dbMessage.getStatusReportStr());

				statusReport.uuidStatus.putAll(newStatusReport.uuidStatus);

				// I just update my db and view. I don't do anything else like re-sending the
				// message etc.

				dbMessage.setStatusReportStr(statusReport.toJson());
				dbMessage.setMessageStatus(statusReport.getOverallStatus());

				final Message newMessage = dbManager.addUpdateMessage(dbMessage);

				if (!newMessage.getMessageType().equals(MessageType.UPDATE))
					Platform.runLater(() -> dmsPanel.updateMessageStatus(newMessage, groupUuid));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void transientMessageReceived(String message, String remoteUuid) {

		// TODO Auto-generated method stub

	}

	@Override
	public JComponent getDmsPanel() {

		return dmsPanelSwing;

	}

	@Override
	public void commentUpdated(String comment) {

		taskQueue.execute(() -> {

			try {

				Identity identity = model.getIdentity();

				if (comment.equals(identity.getComment()))
					return;

				identity.setComment(comment);

				Identity newIdentity = dbManager.updateIdentity(identity);

				model.updateComment(comment);

				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));

				synchronized (beaconSyncObj) {

					beaconSyncObj.notify();

				}

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

				if (identity.getStatus().equals(Availability.AVAILABLE)) {

					identity.setStatus(Availability.BUSY);

				} else if (identity.getStatus().equals(Availability.BUSY)) {

					identity.setStatus(Availability.AVAILABLE);

				}

				Identity newIdentity = dbManager.updateIdentity(identity);

				model.updateStatus(newIdentity.getStatus());

				Platform.runLater(() -> dmsPanel.setIdentity(newIdentity));

				synchronized (beaconSyncObj) {

					beaconSyncObj.notify();

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void contactMessagePaneOpened(final String uuid) {

		taskQueue.execute(() -> {

			model.messagePaneOpened(uuid);

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

		});

	}

	@Override
	public void contactMessagePaneClosed(final String uuid) {

		taskQueue.execute(() -> {

			model.messagePaneClosed(uuid);

		});

	}

	@Override
	public void sendPrivateMessageClicked(final String messageTxt, final String receiverUuid) {

		taskQueue.execute(() -> {

			try {

				Message newMessage = createOutgoingMessage(messageTxt, receiverUuid, ReceiverType.PRIVATE,
						MessageType.TEXT, null);

				addPrivateMessageToPane(newMessage, true);

				sendMessage(newMessage);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void privateShowFoldersClicked(String uuid) {

		taskQueue.execute(() -> {

			model.setFileSelectionUuid(new SimpleEntry<ReceiverType, String>(ReceiverType.PRIVATE, uuid));

		});

	}

	@Override
	public void contactPaneScrolledToTop(final String uuid) {

		taskQueue.execute(() -> {

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

		});

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

					String groupUpdate = getGroupUpdate(newGroup.getUuid(), newGroup.getName(), true,
							newGroup.getContacts(), null);

					sendGroupMessage(createOutgoingMessage(groupUpdate, newGroup.getUuid(), ReceiverType.GROUP,
							MessageType.UPDATE, CommonConstants.CODE_UPDATE_GROUP));

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
				if (group.getName().equals(groupName) && contactsToBeAdded.isEmpty() && contactsToBeRemoved.isEmpty())
					return;

				String newGroupName = group.getName().equals(groupName) ? null : groupName;

				try {

					Dgroup newGroup = createUpdateGroup(group.getUuid(), newGroupName, group.getOwnerUuid(),
							group.isActive(), contactsToBeAdded, contactsToBeRemoved);

					if (newGroup == null)
						return;

					model.addGroup(newGroup);

					Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

					if (!contactsToBeAdded.isEmpty()) {

						String groupUpdateToAddedContacts = getGroupUpdate(newGroup.getUuid(), newGroup.getName(), true,
								newGroup.getContacts(), null);

						sendGroupMessage(
								createOutgoingMessage(groupUpdateToAddedContacts, newGroup.getUuid(),
										ReceiverType.GROUP, MessageType.UPDATE, CommonConstants.CODE_UPDATE_GROUP),
								contactsToBeAdded);

					}

					if (!contactsToBeRemoved.isEmpty()) {

						String groupUpdateToRemovedContacts = getGroupUpdate(newGroup.getUuid(), null, false, null,
								null);

						sendGroupMessage(
								createOutgoingMessage(groupUpdateToRemovedContacts, newGroup.getUuid(),
										ReceiverType.GROUP, MessageType.UPDATE, CommonConstants.CODE_UPDATE_GROUP),
								contactsToBeRemoved);

					}

					if (!residentContacts.isEmpty()) {

						String groupUpdateToResidentContacts = getGroupUpdate(newGroup.getUuid(), newGroupName, true,
								contactsToBeAdded, contactsToBeRemoved);

						sendGroupMessage(
								createOutgoingMessage(groupUpdateToResidentContacts, newGroup.getUuid(),
										ReceiverType.GROUP, MessageType.UPDATE, CommonConstants.CODE_UPDATE_GROUP),
								residentContacts);

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

				String groupUpdate = getGroupUpdate(newGroup.getUuid(), null, newGroup.isActive(), null, null);

				sendGroupMessage(createOutgoingMessage(groupUpdate, newGroup.getUuid(), ReceiverType.GROUP,
						MessageType.UPDATE, CommonConstants.CODE_UPDATE_GROUP), contacts);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void groupMessagePaneOpened(String groupUuid) {

		taskQueue.execute(() -> {

			model.messagePaneOpened(groupUuid);

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

		});

	}

	@Override
	public void groupMessagePaneClosed(String groupUuid) {

		taskQueue.execute(() -> {

			model.messagePaneClosed(groupUuid);

		});

	}

	@Override
	public void sendGroupMessageClicked(String messageTxt, String groupUuid) {

		taskQueue.execute(() -> {

			try {

				Message newMessage = createOutgoingMessage(messageTxt, groupUuid, ReceiverType.GROUP, MessageType.TEXT,
						null);

				addGroupMessageToPane(newMessage, true);

				sendGroupMessage(newMessage);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void groupShowFoldersClicked(String groupUuid) {

		taskQueue.execute(() -> {

			model.setFileSelectionUuid(new SimpleEntry<ReceiverType, String>(ReceiverType.GROUP, groupUuid));

		});

	}

	@Override
	public void groupPaneScrolledToTop(String groupUuid) {

		taskQueue.execute(() -> {

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

		});

	}

	@Override
	public void showFoldersCanceled() {

		taskQueue.execute(() -> {

			model.setFileSelectionUuid(null);

		});

	}

	@Override
	public void fileSelected(Path file) {

		taskQueue.execute(() -> {

			Entry<ReceiverType, String> fileSelectionUuid = model.getFileSelectionUuid();
			model.setFileSelectionUuid(null);

			if (fileSelectionUuid == null)
				return;

			try {

				Path srcFile = file;
				Path dstFolder = Paths.get(CommonConstants.SEND_FOLDER).normalize().toAbsolutePath();

				String fileName = srcFile.getFileName().toString();

				Path dstFile = getDstFile(dstFolder, fileName);

				Files.copy(srcFile, dstFile);

				// Now to the send operations

				switch (fileSelectionUuid.getKey()) {

				case PRIVATE:

				{

					Message newMessage = createOutgoingMessage(dstFile.toString(), fileSelectionUuid.getValue(),
							ReceiverType.PRIVATE, MessageType.FILE, null);

					addPrivateMessageToPane(newMessage, true);

					sendMessage(newMessage);

					break;

				}

				case GROUP:

				{

					Message newMessage = createOutgoingMessage(dstFile.toString(), fileSelectionUuid.getValue(),
							ReceiverType.GROUP, MessageType.FILE, null);

					addGroupMessageToPane(newMessage, true);

					sendGroupMessage(newMessage);

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

				if (message.getMessageType().equals(MessageType.FILE)) {

					Path file = Paths.get(message.getContent());

					dmsListeners.forEach(listener -> listener.fileClicked(file));

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void infoClicked(Long messageId) {

		taskQueue.execute(() -> {

			try {

				Message message = dbManager.getMessage(messageId);

				System.out.println(message.getContent());

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void addListener(DmsListener listener) {

		dmsListeners.add(listener);

	}

	@Override
	public void removeListener(DmsListener listener) {

		dmsListeners.remove(listener);

	}

}

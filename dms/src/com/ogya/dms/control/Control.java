package com.ogya.dms.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import javax.swing.JComponent;

import org.hibernate.HibernateException;

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
import com.ogya.dms.model.Model;
import com.ogya.dms.structures.ContactStatus;
import com.ogya.dms.structures.GroupUpdate;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageIdentifier;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageStatusUpdate;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.ReceiverType;
import com.ogya.dms.structures.StatusReport;
import com.ogya.dms.structures.StatusReportUpdate;
import com.ogya.dms.view.DmsPanel;
import com.ogya.dms.view.intf.AppListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Control implements AppListener, DmsClientListener, DmsHandle {

	private static final Map<String, Control> INSTANCES = Collections.synchronizedMap(new HashMap<String, Control>());

	private static final int MIN_MESSAGES_PER_PAGE = 10;

	private final DbManager dbManager;

	private final Model model;

	private final DmsPanel dmsPanel;
	private final JFXPanel dmsPanelSwing;

	private final DmsClient dmsClient;

	private final Object beaconSyncObj = new Object();

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
			contact.setStatus(ContactStatus.OFFLINE);
			dbManager.addUpdateContact(contact);
		});

	}

	private void initModel() {

		dbManager.fetchAllContacts().forEach(contact -> model.addContact(contact));
		dbManager.fetchAllGroups().forEach(group -> model.addGroup(group));

		// TODO: Add waiting group messages

	}

	private void initGUI() {

		Platform.runLater(() -> {
			dmsPanelSwing.setScene(new Scene(dmsPanel));
			dmsPanel.setIdentity(model.getIdentity());
		});

		model.getContacts().forEach((uuid, contact) -> {

			String remoteUuid = uuid;

			Platform.runLater(() -> dmsPanel.updateContact(contact));

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

		model.getGroups().forEach((uuid, group) -> {

			Platform.runLater(() -> dmsPanel.updateGroup(group));

			//

		});

	}

	private void addPrivateMessageToPane(Message message, final boolean newMessageToBottom) {

		if (model.getLocalUuid().equals(message.getSenderUuid())) {

			final String remoteUuid = message.getReceiverUuid();

			model.addMessageId(remoteUuid, message.getId());

			Platform.runLater(() -> {
				if (newMessageToBottom)
					dmsPanel.addPrivateMessageToBottom(message, "", MessageDirection.OUTGOING, remoteUuid);
				else
					dmsPanel.addPrivateMessageToTop(message, "", MessageDirection.OUTGOING, remoteUuid);
			});

		} else if (model.getLocalUuid().equals(message.getReceiverUuid())) {

			final String remoteUuid = message.getSenderUuid();

			model.addMessageId(remoteUuid, message.getId());

			Platform.runLater(() -> {
				if (newMessageToBottom)
					dmsPanel.addPrivateMessageToBottom(message, "", MessageDirection.INCOMING, remoteUuid);
				else
					dmsPanel.addPrivateMessageToTop(message, "", MessageDirection.INCOMING, remoteUuid);
			});

		}

	}

	private void addGroupMessageToPane(Message message) {

		// TODO

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

			contact.setStatus(ContactStatus.OFFLINE);

			try {

				final Contact newContact = dbManager.addUpdateContact(contact);

				model.addContact(newContact);

				Platform.runLater(() -> dmsPanel.updateContact(newContact));

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	private Message createIncomingMessage(String message, Function<Message, MessageStatus> messageStatusFunction)
			throws Exception {

		Message incomingMessage = Message.fromJson(message);

		MessageStatus messageStatus = messageStatusFunction.apply(incomingMessage);

		incomingMessage.setMessageStatus(messageStatus);

		incomingMessage.setWaiting(false);

		Message newMessage = dbManager.addUpdateMessage(incomingMessage);

		return newMessage;

	}

	private Message createOutgoingMessage(String message, String receiverUuid, ReceiverType receiverType,
			MessageType messageType) throws Exception {

		Message outgoingMessage = new Message(model.getLocalUuid(), receiverUuid, receiverType, messageType, message);

		outgoingMessage.setMessageStatus(MessageStatus.FRESH);

		outgoingMessage.setWaiting(true);

		Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

		return newMessage;

	}

	private Message sendMessage(Message message) {

		String receiverUuid = message.getReceiverUuid();

		if (!model.isContactOnline(receiverUuid))
			return message;

		dmsClient.sendMessage(message.toJson(), receiverUuid);

		message.setMessageStatus(MessageStatus.SENT);

		message.setWaiting(true);

		try {

			Message newMessage = dbManager.addUpdateMessage(message);

			return newMessage;

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		return message;

	}

	private Dgroup createUpdateGroup(String groupUuid, String groupName, String uuidOwner,
			List<String> contactsToBeAdded, List<String> contactsToBeRemoved) throws Exception {

		Dgroup group = model.getGroup(groupUuid);

		if (group == null) {
			group = new Dgroup(groupName, uuidOwner);
			group.setUuid(groupUuid);
		}

		if (contactsToBeAdded != null) {

			for (String uuid : contactsToBeAdded) {

				try {

					Contact contact = model.getContact(uuid);

					if (contact != null)
						group.getContacts().add(contact);

				} catch (HibernateException e) {

					e.printStackTrace();

				}

			}

		}

		if (contactsToBeRemoved != null) {

			for (String uuid : contactsToBeRemoved) {

				try {

					Contact contact = model.getContact(uuid);

					if (contact != null)
						group.getContacts().remove(contact);

				} catch (HibernateException e) {

					e.printStackTrace();

				}

			}

		}

		Dgroup newGroup = dbManager.addUpdateGroup(group);

		return newGroup;

	}

	private String getGroupUpdate(String groupUuid, String groupName, Set<Contact> contactsToBeAdded,
			Set<Contact> contactsToBeRemoved) {

		GroupUpdate groupUpdate = new GroupUpdate(groupUuid);

		if (groupName != null)
			groupUpdate.groupName = groupName;

		if (contactsToBeAdded != null)
			contactsToBeAdded
					.forEach(contact -> groupUpdate.contactUuidNameToBeAdded.put(contact.getUuid(), contact.getName()));

		if (contactsToBeRemoved != null)
			contactsToBeRemoved.forEach(
					contact -> groupUpdate.contactUuidNameToBeRemoved.put(contact.getUuid(), contact.getName()));

		return groupUpdate.toJson();

	}

	private Message sendGroupMessage(Message message) {

		String groupUuid = message.getReceiverUuid();

		Dgroup group = model.getGroup(groupUuid);

		if (group == null)
			return message;

		final StatusReport statusReport = new StatusReport();
		final List<String> onlineUuids = new ArrayList<String>();

		if (model.isMyGroup(groupUuid)) {
			// It's my group, so I have to send this message to all the members except the
			// original sender.

			int receivers = 0;

			for (Contact contact : group.getContacts()) {

				String receiverUuid = contact.getUuid();

				// Skip the original sender
				if (message.getSenderUuid().equals(receiverUuid))
					continue;

				++receivers;

				if (!model.isContactOnline(receiverUuid)) {

					statusReport.uuidStatus.put(receiverUuid, MessageStatus.FRESH);

					model.updateWaitingGroupMessageToContact(receiverUuid, message.getSenderUuid(),
							message.getMessageId(), MessageStatus.FRESH);

				} else {

					statusReport.uuidStatus.put(receiverUuid, MessageStatus.SENT); // Assuming that the message will be
																					// sent...
					onlineUuids.add(receiverUuid);

					model.updateWaitingGroupMessageToContact(receiverUuid, message.getSenderUuid(),
							message.getMessageId(), MessageStatus.SENT);

				}

			}

			if (onlineUuids.size() > 0)
				dmsClient.sendMessage(message.toJson(), onlineUuids);

			message.setWaiting(receivers > 0);

		} else {
			// It's not my group, so I will send this message to the group owner only, but
			// keep track of all other group members as well.

			group.getContacts().forEach(contact -> {

				String receiverUuid = contact.getUuid();

				// Skip the original sender (me)
				if (message.getSenderUuid().equals(receiverUuid))
					return;

				statusReport.uuidStatus.put(receiverUuid, MessageStatus.FRESH);

			});

			String receiverUuid = group.getUuidOwner();

			if (!model.isContactOnline(receiverUuid)) {

				statusReport.uuidStatus.put(receiverUuid, MessageStatus.FRESH);

				model.updateWaitingGroupMessageToContact(receiverUuid, message.getSenderUuid(), message.getMessageId(),
						MessageStatus.FRESH);

			} else {

				dmsClient.sendMessage(message.toJson(), receiverUuid);

				statusReport.uuidStatus.put(receiverUuid, MessageStatus.SENT);

				model.updateWaitingGroupMessageToContact(receiverUuid, message.getSenderUuid(), message.getMessageId(),
						MessageStatus.SENT);

			}

			message.setWaiting(true);

		}

		message.setStatusReportStr(statusReport.toJson());

		// If I am the sender, I shall keep the message status updated from the status
		// report.
		if (model.getLocalUuid().equals(message.getSenderUuid()))
			message.setMessageStatus(statusReport.getOverallStatus());

		try {

			Message newMessage = dbManager.addUpdateMessage(message);

			return newMessage;

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		return message;

	}

	private Message sendGroupMessage(Message message, String receiverUuid) {

		if (!model.isContactOnline(receiverUuid))
			return message;

		dmsClient.sendMessage(message.toJson(), receiverUuid);

		model.updateWaitingGroupMessageToContact(receiverUuid, message.getSenderUuid(), message.getMessageId(),
				MessageStatus.SENT);

		StatusReport statusReport = StatusReport.fromJson(message.getStatusReportStr());

		statusReport.uuidStatus.put(receiverUuid, MessageStatus.SENT);

		message.setStatusReportStr(statusReport.toJson());

		MessageStatus overallMessageStatus = statusReport.getOverallStatus();

		// If I am the sender, update the message status too
		if (model.getLocalUuid().equals(message.getSenderUuid()))
			message.setMessageStatus(overallMessageStatus);

		message.setWaiting(true);

		try {

			Message newMessage = dbManager.addUpdateMessage(message);

			return newMessage;

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		return message;

	}

	private void privateMessageReceived(Message message) {

		addPrivateMessageToPane(message, true);

	}

	private void groupMessageReceived(Message message) {

		switch (message.getMessageType()) {

		case TEXT:

			// If it's my group, I'm supposed to send this message to all group members
			// (except the original sender).
			if (model.isMyGroup(message.getReceiverUuid()))
				sendGroupMessage(message);

			addGroupMessageToPane(message);

			break;

		case UPDATE:

			GroupUpdate groupUpdate = GroupUpdate.fromJson(message.getContent());

			groupUpdateReceived(groupUpdate, message.getSenderUuid());

			break;

		}

	}

	private void groupUpdateReceived(final GroupUpdate groupUpdate, String uuidOwner) {

		taskQueue.execute(() -> {

			try {

				final List<String> contactsToBeAdded = new ArrayList<String>();
				final List<String> contactsToBeRemoved = new ArrayList<String>();

				if (groupUpdate.contactUuidNameToBeAdded != null) {

					groupUpdate.contactUuidNameToBeAdded.forEach((uuid, name) -> {

						if (model.getLocalUuid().equals(uuid))
							return;

						Contact contact = model.getContact(uuid);
						if (contact == null) {
							contact = new Contact();
							contact.setUuid(uuid);
							contact.setName(name);
							contact.setStatus(ContactStatus.OFFLINE);
						}

						try {
							Contact newContact = dbManager.addUpdateContact(contact);
							model.addContact(newContact);
							contactsToBeAdded.add(uuid);
						} catch (Exception e) {
							e.printStackTrace();
						}

					});

				}

				if (groupUpdate.contactUuidNameToBeRemoved != null) {

					groupUpdate.contactUuidNameToBeRemoved.forEach((uuid, name) -> {

						if (model.getLocalUuid().equals(uuid))
							return;

						Contact contact = model.getContact(uuid);
						if (contact == null) {
							contact = new Contact();
							contact.setUuid(uuid);
							contact.setName(name);
							contact.setStatus(ContactStatus.OFFLINE);
							try {
								Contact newContact = dbManager.addUpdateContact(contact);
								model.addContact(newContact);
								contactsToBeRemoved.add(uuid);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					});

				}

				final Dgroup newGroup = createUpdateGroup(groupUpdate.groupUuid, groupUpdate.groupName, uuidOwner,
						contactsToBeAdded, contactsToBeRemoved);

				model.addGroup(newGroup);

				Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

			} catch (Exception e) {
				e.printStackTrace();
			}

		});

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
					// Simdi cevrimici olduysa bekleyen mesajlarini gonder

					taskQueue.execute(() -> {

						try {

							for (final Message waitingMessage : dbManager.getMessagesWaitingToContact(uuid)) {

								switch (waitingMessage.getMessageStatus()) {

								case FRESH:

									final Message newMessage = sendMessage(waitingMessage);

									if (!newMessage.getMessageStatus().equals(MessageStatus.SENT))
										break;

									Platform.runLater(() -> dmsPanel.updatePrivateMessage(newMessage, uuid));

									break;

								case SENT:
								case RECEIVED:

									dmsClient.claimMessageStatus(new MessageStatusUpdate(waitingMessage.getSenderUuid(),
											waitingMessage.getReceiverUuid(), waitingMessage.getMessageId()).toJson(),
											uuid);

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

							for (MessageIdentifier messageIdentifier : model.getGroupMessagesWaitingToContact(uuid)) {

								Message waitingMessage = dbManager.getMessage(messageIdentifier.senderUuid,
										messageIdentifier.messageId);

								switch (messageIdentifier.messageStatus) {

								case FRESH:

									final Message newMessage = sendGroupMessage(waitingMessage, uuid);

									if (!newMessage.getMessageStatus().equals(MessageStatus.SENT))
										break;

									Platform.runLater(() -> dmsPanel.updateGroupMessage(newMessage, uuid));

									break;

								case SENT:
								case RECEIVED:

									dmsClient.claimMessageStatus(new MessageStatusUpdate(waitingMessage.getSenderUuid(),
											waitingMessage.getReceiverUuid(), waitingMessage.getMessageId()).toJson(),
											uuid);

									break;

								default:

									break;

								}

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

					});

				}

			} catch (JsonSyntaxException | HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messageReceived(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				final Message newMessage = createIncomingMessage(message, msg -> {
					switch (msg.getReceiverType()) {
					case PRIVATE:
						return model.isMessagePaneOpen(msg.getSenderUuid()) ? MessageStatus.READ
								: MessageStatus.RECEIVED;
					case GROUP:
						return msg.getMessageType().equals(MessageType.UPDATE) ? MessageStatus.READ
								: (model.isMessagePaneOpen(msg.getReceiverUuid()) ? MessageStatus.READ
										: MessageStatus.RECEIVED);
					}
					return null;
				});

				switch (newMessage.getReceiverType()) {
				case PRIVATE:
					privateMessageReceived(newMessage);
					break;
				case GROUP:
					groupMessageReceived(newMessage);
					break;
				}

				MessageStatusUpdate messageStatusUpdate = new MessageStatusUpdate(newMessage.getSenderUuid(),
						model.getLocalUuid(), newMessage.getMessageId());
				messageStatusUpdate.messageStatus = newMessage.getMessageStatus();
				dmsClient.feedMessageStatus(messageStatusUpdate.toJson(), remoteUuid);

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
	public void messageStatusClaimed(String message, String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				MessageStatusUpdate messageStatusUpdate = MessageStatusUpdate.fromJson(message);

				Message incomingMessage = dbManager.getMessage(messageStatusUpdate.senderUuid,
						messageStatusUpdate.messageId);

				if (incomingMessage == null) {

					// Not received
					messageStatusUpdate.messageStatus = MessageStatus.FRESH;

				} else {

					messageStatusUpdate.messageStatus = incomingMessage.getMessageStatus();

				}

				dmsClient.feedMessageStatus(messageStatusUpdate.toJson(), remoteUuid);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messageStatusFed(String message, String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				MessageStatusUpdate messageStatusUpdate = MessageStatusUpdate.fromJson(message);

				String senderUuid = messageStatusUpdate.senderUuid;
				String receiverUuid = messageStatusUpdate.receiverUuid;
				Long messageId = messageStatusUpdate.messageId;
				MessageStatus messageStatus = messageStatusUpdate.messageStatus;

				// Send this report to the original sender too.
				if (!model.getLocalUuid().equals(senderUuid) && model.isContactOnline(senderUuid))
					dmsClient.feedMessageStatus(message, senderUuid);

				final Message outgoingMessage = dbManager.getMessage(senderUuid, messageId);

				if (outgoingMessage == null)
					return;

				if (outgoingMessage.getReceiverType().equals(ReceiverType.GROUP)) {
					// This is a group message. I am either the group owner or the message sender.
					// If I am the group owner and the message is not received remotely, I will have
					// to re-send it.

					String groupUuid = outgoingMessage.getReceiverUuid();

					Dgroup group = model.getGroup(groupUuid);

					if (group == null)
						return;

					StatusReport statusReport = StatusReport.fromJson(outgoingMessage.getStatusReportStr());

					statusReport.uuidStatus.put(receiverUuid, messageStatus);

					outgoingMessage.setStatusReportStr(statusReport.toJson());

					MessageStatus overallMessageStatus = statusReport.getOverallStatus();

					if (model.isMyGroup(groupUuid)) {

						model.updateWaitingGroupMessageToContact(receiverUuid, senderUuid, messageId, messageStatus);

						outgoingMessage.setWaiting(!overallMessageStatus.equals(MessageStatus.READ));

					} else if (senderUuid.equals(model.getLocalUuid()) && receiverUuid.equals(group.getUuidOwner())) {

						model.updateWaitingGroupMessageToContact(receiverUuid, senderUuid, messageId, messageStatus);

						outgoingMessage.setWaiting(!messageStatus.equals(MessageStatus.READ));

					}

					// If I am the sender, update the message status too
					if (senderUuid.equals(model.getLocalUuid()))
						outgoingMessage.setMessageStatus(overallMessageStatus);

					if (messageStatus.equals(MessageStatus.FRESH) && (model.getLocalUuid().equals(group.getUuidOwner())
							|| (model.getLocalUuid().equals(senderUuid)
									&& receiverUuid.equals(group.getUuidOwner())))) {
						// If the message is not received remotely and;
						// I am the group owner
						// or
						// I am the message owner and receiver is the group owner
						// then re-send this message.

						final Message newMessage = sendGroupMessage(outgoingMessage, receiverUuid);

						Platform.runLater(() -> dmsPanel.updateGroupMessage(newMessage, groupUuid));

					} else {

						final Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

						Platform.runLater(() -> dmsPanel.updateGroupMessage(newMessage, groupUuid));

					}

				} else if (outgoingMessage.getReceiverType().equals(ReceiverType.PRIVATE)) {

					outgoingMessage.setMessageStatus(messageStatus);

					if (messageStatus.equals(MessageStatus.FRESH)) {

						final Message newMessage = sendMessage(outgoingMessage);

						Platform.runLater(() -> dmsPanel.updatePrivateMessage(newMessage, receiverUuid));

					} else {

						outgoingMessage.setWaiting(!messageStatus.equals(MessageStatus.READ));

						final Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

						Platform.runLater(() -> dmsPanel.updatePrivateMessage(newMessage, receiverUuid));

					}

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusReportClaimed(String message, String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				StatusReportUpdate statusReportUpdate = StatusReportUpdate.fromJson(message);

				Message incomingMessage = dbManager.getMessage(statusReportUpdate.senderUuid,
						statusReportUpdate.messageId);

				if (incomingMessage == null) {

					// Not received. Send back a message status update instead.

					MessageStatusUpdate messageStatusUpdate = new MessageStatusUpdate(statusReportUpdate.senderUuid,
							model.getLocalUuid(), statusReportUpdate.messageId);
					messageStatusUpdate.messageStatus = MessageStatus.FRESH;

					dmsClient.feedMessageStatus(messageStatusUpdate.toJson(), remoteUuid);

				} else {

					StatusReport statusReport = StatusReport.fromJson(incomingMessage.getStatusReportStr());

					statusReportUpdate.statusReport = statusReport;

					dmsClient.feedStatusReport(statusReportUpdate.toJson(), remoteUuid);

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void statusReportFed(String message, String remoteUuid) {

		taskQueue.execute(() -> {

			try {

				StatusReportUpdate statusReportUpdate = StatusReportUpdate.fromJson(message);

				// A status report can only be claimed by a group message owner. So at this
				// point, I must be a group message owner. Let's find that message.

				Message outgoingMessage = dbManager.getMessage(statusReportUpdate.senderUuid,
						statusReportUpdate.messageId);

				if (outgoingMessage == null)
					return;

				String groupUuid = outgoingMessage.getReceiverUuid();

				StatusReport statusReport = StatusReport.fromJson(outgoingMessage.getStatusReportStr());

				statusReport.uuidStatus.putAll(statusReportUpdate.statusReport.uuidStatus);

				// I just update my db and view. I don't do anything else like re-sending the
				// message etc.

				MessageStatus overallMessageStatus = statusReport.getOverallStatus();

				outgoingMessage.setStatusReportStr(statusReport.toJson());
				outgoingMessage.setMessageStatus(overallMessageStatus);

				final Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

				Platform.runLater(() -> dmsPanel.updateGroupMessage(newMessage, groupUuid));

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

				if (identity.getStatus().equals(ContactStatus.AVAILABLE)) {

					identity.setStatus(ContactStatus.BUSY);

				} else if (identity.getStatus().equals(ContactStatus.BUSY)) {

					identity.setStatus(ContactStatus.AVAILABLE);

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

				List<Message> messagesWaitingFromContact = dbManager.getMessagesWaitingFromContact(uuid);

				for (final Message incomingMessage : messagesWaitingFromContact) {

					try {

						incomingMessage.setMessageStatus(MessageStatus.READ);

						final Message newMessage = dbManager.addUpdateMessage(incomingMessage);

						Platform.runLater(() -> dmsPanel.updatePrivateMessage(newMessage, uuid));

						MessageStatusUpdate messageStatusUpdate = new MessageStatusUpdate(newMessage.getSenderUuid(),
								model.getLocalUuid(), newMessage.getMessageId());
						messageStatusUpdate.messageStatus = MessageStatus.READ;

						dmsClient.feedMessageStatus(messageStatusUpdate.toJson(), newMessage.getSenderUuid());

					} catch (JsonSyntaxException | HibernateException e) {

						e.printStackTrace();

					}

				}

				Platform.runLater(() -> dmsPanel.scrollPaneToMessage(uuid,
						messagesWaitingFromContact.size() > 0 ? messagesWaitingFromContact.get(0).getId() : -1L));

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
	public void sendPrivateMessageClicked(final String message, final String receiverUuid) {

		taskQueue.execute(() -> {

			try {

				final Message newMessage = sendMessage(
						createOutgoingMessage(message, receiverUuid, ReceiverType.PRIVATE, MessageType.TEXT));

				addPrivateMessageToPane(newMessage, true);

			} catch (Exception e) {

				e.printStackTrace();

			}

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

			Platform.runLater(() -> dmsPanel.savePosition(uuid, previousMinMessageId));

			lastMessagesBeforeId.forEach(message -> addPrivateMessageToPane(message, false));

			Platform.runLater(() -> dmsPanel.scrollToSavedPosition(uuid));

		});

	}

	@Override
	public void createGroupRequested(String groupName, List<String> selectedUuids) {

		taskQueue.execute(() -> {

			try {

				final Dgroup newGroup = createUpdateGroup(null, groupName, model.getLocalUuid(), selectedUuids, null);

				model.addGroup(newGroup);

				Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

				// Gruba eleman ekleme mesajini olusturup grup uyelerine gonder

				String groupUpdate = getGroupUpdate(newGroup.getUuid(), newGroup.getName(), newGroup.getContacts(),
						null);

				sendGroupMessage(
						createOutgoingMessage(groupUpdate, newGroup.getUuid(), ReceiverType.GROUP, MessageType.UPDATE));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

}

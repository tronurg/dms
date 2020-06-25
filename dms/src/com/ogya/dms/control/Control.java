package com.ogya.dms.control;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JComponent;

import org.hibernate.HibernateException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
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
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.view.DmsPanel;
import com.ogya.dms.view.intf.AppListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Control implements AppListener, DmsClientListener, DmsHandle {

	private static final Map<String, Control> INSTANCES = Collections.synchronizedMap(new HashMap<String, Control>());

	private static final int MIN_MESSAGES_PER_PAGE = 50;

	private final String localUuid;

	private final DbManager dbManager;

	private final Model model;

	private final DmsPanel dmsPanel;
	private final JFXPanel dmsPanelSwing;

	private final DmsClient dmsClient;

	private final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return arg0.getName().equals("id") || arg0.getName().equals("messageStatusStr");
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).create();

	private final Type gsonHashMapType = new TypeToken<HashMap<String, MessageStatus>>() {
	}.getType();

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
		localUuid = identity.getUuid();

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
		dbManager.fetchAllGroups().forEach(dgroup -> model.addGroup(dgroup));

	}

	private void initGUI() {

		Platform.runLater(() -> {
			dmsPanelSwing.setScene(new Scene(dmsPanel));
			dmsPanel.setIdentity(model.getIdentity());
		});

		model.getContacts().forEach((uuid, contact) -> Platform.runLater(() -> dmsPanel.updateContact(contact)));

		model.getGroups().forEach((uuid, dgroup) -> Platform.runLater(() -> dmsPanel.updateGroup(dgroup)));

		try {

			Set<String> remoteUuids = dbManager.getAllUuidsMessagingWithUuid(localUuid);

			remoteUuids.forEach(remoteUuid -> {

				List<Message> dbMessages = new ArrayList<Message>();

				dbMessages.addAll(dbManager.getAllMessagesSinceFirstUnreadMessage(localUuid, remoteUuid));

				if (dbMessages.size() == 0) {

					dbMessages.addAll(dbManager.getLastMessages(localUuid, remoteUuid, MIN_MESSAGES_PER_PAGE));

				} else if (dbMessages.size() < MIN_MESSAGES_PER_PAGE) {

					dbMessages.addAll(dbManager.getLastMessagesBeforeId(localUuid, remoteUuid,
							dbMessages.get(0).getId(), MIN_MESSAGES_PER_PAGE - dbMessages.size()));

				}

				dbMessages.forEach(message -> addMessageToPane(message));

			});

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

	private void addMessageToPane(Message message) {

		if (localUuid.equals(message.getSenderUuid())) {

			final String remoteUuid = message.getReceiverUuid();

			model.addMessageId(remoteUuid, message.getId());

			Platform.runLater(() -> dmsPanel.addMessage(message, MessageDirection.OUTGOING, remoteUuid));

		} else if (localUuid.equals(message.getReceiverUuid())) {

			final String remoteUuid = message.getSenderUuid();

			model.addMessageId(remoteUuid, message.getId());

			Platform.runLater(() -> dmsPanel.addMessage(message, MessageDirection.INCOMING, remoteUuid));

		}

	}

	private void publishBeacon() {

		while (true) {

			synchronized (beaconSyncObj) {

				if (model.isServerConnected())
					dmsClient.sendBeacon(gson.toJson(model.getIdentity()));

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

	private Message createOutgoingMessage(String message, String receiverUuid, MessageType messageType)
			throws Exception {

		Message outgoingMessage = new Message(localUuid, receiverUuid, messageType, message);

		outgoingMessage.setMessageStatus(MessageStatus.CREATED);

		Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

		return newMessage;

	}

	private Message createIncomingMessage(String message) throws Exception {

		Message incomingMessage = gson.fromJson(message, Message.class);

		if (model.isMessagePaneOpen(incomingMessage.getSenderUuid())) {

			incomingMessage.setMessageStatus(MessageStatus.READ);

		} else {

			incomingMessage.setMessageStatus(MessageStatus.REACHED);

		}

		Message newMessage = dbManager.addUpdateMessage(incomingMessage);

		return newMessage;

	}

	private Message sendMessage(Message message) {

		String receiverUuid = message.getReceiverUuid();

		if (!model.isContactOnline(receiverUuid))
			return message;

		dmsClient.sendMessage(gson.toJson(message), receiverUuid);

		try {

			message.setMessageStatus(MessageStatus.SENT);

			Message newMessage = dbManager.addUpdateMessage(message);

			return newMessage;

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		return message;

	}

	private Dgroup createGroup(String groupName, String uuidOwner, List<String> selectedUuids) throws Exception {

		Dgroup group = new Dgroup(groupName, uuidOwner);

		selectedUuids.forEach(uuid -> {

			try {

				Contact contact = dbManager.getContact(uuid);

				if (contact != null)
					group.getContacts().add(contact);

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

		Dgroup newGroup = dbManager.addUpdateGroup(group);

		return newGroup;

	}

	private String getAddToGroupMessage(Set<Contact> contacts) {

		Map<String, String> uuidName = new LinkedHashMap<String, String>();

		contacts.forEach(contact -> uuidName.put(contact.getUuid(), contact.getName()));

		String addToGroupMessage = gson.toJson(uuidName);

		return addToGroupMessage;

	}

	private Message createOutgoingGroupMessage(String message, Dgroup group, MessageType messageType) throws Exception {

		Message outgoingMessage = new Message(localUuid, group.getUuid(), messageType, message);

		final Map<String, MessageStatus> messageStatus = new HashMap<String, MessageStatus>();

		// If I'm not the group owner, then follow the group owner's message status too.
		if (!localUuid.equals(group.getUuidOwner()))
			messageStatus.put(group.getUuidOwner(), MessageStatus.CREATED);

		group.getContacts().forEach(contact -> {

			// Skip the original sender (don't follow my message status for I'm its sender)
			if (localUuid.equals(contact.getUuid()))
				return;

			messageStatus.put(contact.getUuid(), MessageStatus.CREATED);

		});

		outgoingMessage.setMessageStatus(MessageStatus.CREATED);

		outgoingMessage.setMessageStatusStr(gson.toJson(messageStatus));

		Message newMessage = dbManager.addUpdateMessage(outgoingMessage);

		return newMessage;

	}

	private Message sendGroupMessage(Message message, Dgroup group) {

		final Map<String, MessageStatus> messageStatus = gson.fromJson(message.getMessageStatusStr(), gsonHashMapType);
		final List<String> onlineUuids = new ArrayList<String>();

		if (localUuid.equals(group.getUuidOwner())) {
			// It's my group, so I have to send this message to all the members except the
			// original sender.

			group.getContacts().forEach(contact -> {

				// Skip the original sender
				if (contact.getUuid().equals(message.getSenderUuid()))
					return;

				String receiverUuid = contact.getUuid();

				if (!model.isContactOnline(receiverUuid))
					return;

				messageStatus.put(receiverUuid, MessageStatus.SENT); // Assuming that the message will be sent...
				onlineUuids.add(receiverUuid);

			});

			if (onlineUuids.size() < 1)
				return message;

			dmsClient.sendGroupMessage(gson.toJson(message), onlineUuids);

		} else {
			// It's not my group, so I will send this message to the group owner only.

			String receiverUuid = group.getUuidOwner();

			if (!model.isContactOnline(receiverUuid))
				return message;

			dmsClient.sendGroupMessage(gson.toJson(message), receiverUuid);

			messageStatus.put(receiverUuid, MessageStatus.SENT);

		}

		try {

			message.setMessageStatus(MessageStatus.SENT);

			message.setMessageStatusStr(gson.toJson(messageStatus));

			Message newMessage = dbManager.addUpdateMessage(message);

			return newMessage;

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		return message;

	}

	@Override
	public void beaconReceived(String message) {

		taskQueue.execute(() -> {

			try {

				Contact incomingContact = gson.fromJson(message, Contact.class);

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

								case CREATED:

									final Message newMessage = sendMessage(waitingMessage);

									if (!newMessage.getMessageStatus().equals(MessageStatus.SENT))
										break;

									Platform.runLater(() -> dmsPanel.updateMessage(newMessage, uuid));

									break;

								case SENT:
								case REACHED:

									dmsClient.claimMessageStatus(Long.toString(waitingMessage.getMessageId()),
											waitingMessage.getReceiverUuid());

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
	public void messageReceived(final String message) {

		taskQueue.execute(() -> {

			try {

				final Message newMessage = createIncomingMessage(message);

				addMessageToPane(newMessage);

				if (newMessage.getMessageStatus().equals(MessageStatus.REACHED))
					dmsClient.sendReceived(Long.toString(newMessage.getMessageId()), newMessage.getSenderUuid());
				else if (newMessage.getMessageStatus().equals(MessageStatus.READ))
					dmsClient.sendRead(Long.toString(newMessage.getMessageId()), newMessage.getSenderUuid());

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void groupMessageReceived(final String message) {

		taskQueue.execute(() -> {

			// TODO

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

		final Long messageId = Long.parseLong(message);

		taskQueue.execute(() -> {

			try {

				Message incomingMessage = dbManager.getMessage(remoteUuid, messageId);

				if (incomingMessage == null) {

					dmsClient.sendNotReceived(message, remoteUuid);

				} else if (incomingMessage.getMessageStatus().equals(MessageStatus.REACHED)) {

					dmsClient.sendReceived(message, remoteUuid);

				} else if (incomingMessage.getMessageStatus().equals(MessageStatus.READ)) {

					dmsClient.sendRead(message, remoteUuid);

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messageNotReceivedRemotely(String message, String remoteUuid) {

		final Long messageId = Long.parseLong(message);

		taskQueue.execute(() -> {

			try {

				Message waitingMessage = dbManager.updateMessageStatus(localUuid, messageId, MessageStatus.CREATED);

				if (waitingMessage == null)
					return;

				// Mesaji tekrar gonder

				final Message newMessage = sendMessage(waitingMessage);

				Platform.runLater(() -> dmsPanel.updateMessage(newMessage, remoteUuid));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messageReceivedRemotely(String message, String remoteUuid) {

		final Long messageId = Long.parseLong(message);

		taskQueue.execute(() -> {

			try {

				final Message outgoingMessage = dbManager.updateMessageStatus(localUuid, messageId,
						MessageStatus.REACHED);

				if (outgoingMessage == null)
					return;

				Platform.runLater(() -> dmsPanel.updateMessage(outgoingMessage, remoteUuid));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void messageReadRemotely(String message, String remoteUuid) {

		final Long messageId = Long.parseLong(message);

		taskQueue.execute(() -> {

			try {

				final Message outgoingMessage = dbManager.updateMessageStatus(localUuid, messageId, MessageStatus.READ);

				if (outgoingMessage == null)
					return;

				Platform.runLater(() -> dmsPanel.updateMessage(outgoingMessage, remoteUuid));

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

						Platform.runLater(() -> dmsPanel.updateMessage(newMessage, uuid));

						dmsClient.sendRead(Long.toString(newMessage.getMessageId()), newMessage.getSenderUuid());

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
	public void sendMessageClicked(final String message, final String receiverUuid) {

		taskQueue.execute(() -> {

			try {

				final Message newMessage = sendMessage(createOutgoingMessage(message, receiverUuid, MessageType.TEXT));

				addMessageToPane(newMessage);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void paneScrolledToTop(final String uuid) {

		taskQueue.execute(() -> {

			Long previousMinMessageId = model.getMinMessageId(uuid);

			if (previousMinMessageId < 0)
				return;

			List<Message> lastMessagesBeforeId = dbManager.getLastMessagesBeforeId(localUuid, uuid,
					previousMinMessageId, MIN_MESSAGES_PER_PAGE);

			if (lastMessagesBeforeId.size() == 0)
				return;

			Platform.runLater(() -> dmsPanel.savePosition(uuid, previousMinMessageId));

			lastMessagesBeforeId.forEach(message -> addMessageToPane(message));

			Platform.runLater(() -> dmsPanel.scrollToSavedPosition(uuid));

		});

	}

	@Override
	public void createGroupRequested(String groupName, List<String> selectedUuids) {

		taskQueue.execute(() -> {

			try {

				final Dgroup newGroup = createGroup(groupName, localUuid, selectedUuids);

				model.addGroup(newGroup);

				Platform.runLater(() -> dmsPanel.updateGroup(newGroup));

				// Gruba eleman ekleme mesajini olusturup grup uyelerine gonder

				String addToGroupMessage = getAddToGroupMessage(newGroup.getContacts());

				sendGroupMessage(createOutgoingGroupMessage(addToGroupMessage, newGroup, MessageType.GROUP_ADD),
						newGroup);

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

}

package com.ogya.dms.server.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.structures.ContentType;
import com.ogya.dms.common.structures.MessagePojo;
import com.ogya.dms.server.model.intf.ModelListener;

public class Model {

	private final ModelListener listener;

	private final Gson gson = new Gson();

	private final Map<String, LocalUser> localUsers = Collections.synchronizedMap(new HashMap<String, LocalUser>());
	private final Map<String, User> remoteUsers = Collections.synchronizedMap(new HashMap<String, User>());

	private final Map<String, Set<User>> remoteServerUsers = Collections
			.synchronizedMap(new HashMap<String, Set<User>>());

	private final Map<AtomicBoolean, Set<String>> statusReceiverMap = Collections
			.synchronizedMap(new HashMap<AtomicBoolean, Set<String>>());

	private final Set<String> remoteIps = Collections.synchronizedSet(new HashSet<String>());

	public Model(ModelListener listener) {

		this.listener = listener;

		init();

	}

	private void init() {

		Path ipDatPath = Paths.get("./ip.dat");

		if (Files.notExists(ipDatPath))
			return;

		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(ipDatPath))) {

			for (String ip : (String[]) ois.readObject()) {

				remoteIps.add(ip);

			}

		} catch (IOException | ClassNotFoundException e) {

			e.printStackTrace();

		}

	}

	public boolean isLive() {

		return !localUsers.isEmpty();

	}

	public void localMessageReceived(final String messagePojoStr) {

		try {

			MessagePojo messagePojo = gson.fromJson(messagePojoStr, MessagePojo.class);

			String senderUuid = messagePojo.senderUuid;
			Long messageId = messagePojo.messageId;

			switch (messagePojo.contentType) {

			case BCON:

				boolean fresh = localUsers.isEmpty();

				localUsers.putIfAbsent(senderUuid, new LocalUser(senderUuid));

				if (fresh)
					listener.publishImmediately();

				if (!messagePojoStr.equals(localUsers.get(senderUuid).beacon)) {

					boolean isNew = localUsers.get(senderUuid).beacon == null;

					// Yerel uuid yeni eklendi veya guncellendi.
					// Beacon, yerel beacon'lara eklenecek.
					// Yeni beacon tum yerel ve uzak kullanicilara dagitilacak.

					localUsers.get(senderUuid).beacon = messagePojoStr;
					localUsers.forEach((receiverUuid, user) -> {

						if (receiverUuid.equals(senderUuid))
							return;

						listener.sendToLocalUser(receiverUuid, messagePojoStr);

					});
					listener.sendToAllRemoteServers(messagePojoStr);

					if (isNew) {

						sendAllBeaconsToLocalUser(senderUuid);

						sendRemoteIpsToLocalUser(senderUuid);

					}

				}

				break;

			case REQ_BCON:

				sendAllBeaconsToLocalUser(senderUuid);

				break;

			case ADD_IP:

				addRemoteIp(messagePojo.message);

				break;

			case REMOVE_IP:

				removeRemoteIp(messagePojo.message);

				break;

			case REQ_IP:

				sendRemoteIpsToLocalUser(senderUuid);

				break;

			case CANCEL: {

				if (messageId == null)
					break;

				LocalUser sender = localUsers.get(senderUuid);

				if (sender == null)
					break;

				if (sender.sendStatusMap.containsKey(messageId))
					sender.sendStatusMap.get(messageId).set(false);

				break;

			}

			default: {

				if (messagePojo.receiverUuid == null)
					break;

				final LocalUser sender = localUsers.get(senderUuid);

				if (sender == null)
					break;

				String[] receiverUuids = messagePojo.receiverUuid.split(";");

				List<String> localReceiverUuids = new ArrayList<String>();
				Map<String, Set<String>> remoteServerReceiverUuids = new HashMap<String, Set<String>>();

				for (String receiverUuid : receiverUuids) {

					if (localUsers.containsKey(receiverUuid)) {

						localReceiverUuids.add(receiverUuid);
						listener.sendToLocalUser(receiverUuid, messagePojoStr);

					} else if (remoteUsers.containsKey(receiverUuid)) {

						User remoteUser = remoteUsers.get(receiverUuid);

						if (remoteUser.dmsUuid == null)
							continue;

						remoteServerReceiverUuids.putIfAbsent(remoteUser.dmsUuid, new HashSet<String>());
						remoteServerReceiverUuids.get(remoteUser.dmsUuid).add(receiverUuid);

					}

				}

				if (messagePojo.contentType.equals(ContentType.MESSAGE) && localReceiverUuids.size() > 0
						&& messageId != null) {

					MessagePojo progressMessagePojo = new MessagePojo(String.valueOf(100),
							String.join(";", localReceiverUuids), senderUuid, ContentType.PROGRESS, messageId);

					if (localUsers.containsKey(senderUuid))
						listener.sendToLocalUser(senderUuid, gson.toJson(progressMessagePojo));

				}

				remoteServerReceiverUuids.forEach((dmsUuid, uuidList) -> {

					AtomicBoolean sendStatus = new AtomicBoolean(true);

					sender.sendStatusMap.put(messageId, sendStatus);
					statusReceiverMap.put(sendStatus, uuidList);

					listener.sendToRemoteServer(dmsUuid, messagePojoStr, sendStatus,
							!messagePojo.contentType.equals(ContentType.MESSAGE) || messageId == null ? null
									: progress -> {

										if ((progress < 0 || progress == 100)) {

											sender.sendStatusMap.remove(messageId);
											statusReceiverMap.remove(sendStatus);

										}

										MessagePojo progressMessagePojo = new MessagePojo(String.valueOf(progress),
												String.join(";", uuidList), senderUuid, ContentType.PROGRESS,
												messageId);

										if (localUsers.containsKey(senderUuid))
											listener.sendToLocalUser(senderUuid, gson.toJson(progressMessagePojo));

									});

				});

				break;

			}

			}

		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}

	}

	public void remoteMessageReceived(String messagePojoStr, String dmsUuid) {

		try {

			MessagePojo messagePojo = gson.fromJson(messagePojoStr, MessagePojo.class);

			String senderUuid = messagePojo.senderUuid;

			switch (messagePojo.contentType) {

			case BCON:

				remoteUsers.putIfAbsent(senderUuid, new User(senderUuid));

				checkRemoteUserServer(remoteUsers.get(senderUuid), dmsUuid);

				if (!messagePojoStr.equals(remoteUsers.get(senderUuid).beacon)) {

					// Uzak uuid yeni eklendi veya guncellendi.
					// Beacon, uzak beacon'larda guncellenecek.
					// Yeni beacon tum yerel kullanicilara dagitilacak.

					remoteUsers.get(senderUuid).beacon = messagePojoStr;
					localUsers.forEach((receiverUuid, user) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

				}

				break;

			case UUID_DISCONNECTED:

				remoteUserDisconnected(remoteUsers.get(messagePojo.message));

				break;

			default:

				if (messagePojo.receiverUuid == null)
					break;

				String[] receiverUuids = messagePojo.receiverUuid.split(";");

				for (String receiverUuid : receiverUuids) {

					if (localUsers.containsKey(receiverUuid))
						listener.sendToLocalUser(receiverUuid, messagePojoStr);

				}

				break;

			}

		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}

	}

	public void processAllLocalBeacons(Consumer<String> consumer) {

		localUsers.forEach((uuid, user) -> consumer.accept(user.beacon));

	}

	public void testAllLocalUsers() {

		localUsers.forEach((uuid, user) -> listener.sendToLocalUser(uuid, ""));

	}

	public void remoteServerDisconnected(String dmsUuid) {

		if (!remoteServerUsers.containsKey(dmsUuid))
			return;

		remoteServerUsers.remove(dmsUuid).forEach(user -> remoteUserDisconnected(user));

	}

	public void localUuidDisconnected(String uuid) {

		localUserDisconnected(localUsers.get(uuid));

	}

	private void localUserDisconnected(LocalUser user) {

		if (user == null)
			return;

		user.sendStatusMap.forEach((messageId, status) -> status.set(false));

		String messagePojoStr = gson.toJson(new MessagePojo(user.uuid, "", ContentType.UUID_DISCONNECTED, null));

		localUsers.remove(user.uuid);

		localUsers.forEach((receiverUuid, localUser) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

		listener.sendToAllRemoteServers(messagePojoStr);

	}

	private void checkRemoteUserServer(User remoteUser, String dmsUuid) {

		if (dmsUuid.equals(remoteUser.dmsUuid))
			return;

		// Registered to another server before
		if (remoteUser.dmsUuid != null && remoteServerUsers.containsKey(remoteUser.dmsUuid))
			remoteServerUsers.get(remoteUser.dmsUuid).remove(remoteUser);

		remoteUser.dmsUuid = dmsUuid;
		remoteServerUsers.putIfAbsent(dmsUuid, new HashSet<User>());
		remoteServerUsers.get(dmsUuid).add(remoteUser);

	}

	private void remoteUserDisconnected(User user) {

		if (user == null)
			return;

		statusReceiverMap.forEach((sendStatus, receiverUuids) -> {

			if (receiverUuids.contains(user.uuid) && receiverUuids.size() == 1)
				sendStatus.set(false);

		});

		String messagePojoStr = gson.toJson(new MessagePojo(user.uuid, "", ContentType.UUID_DISCONNECTED, null));

		remoteUsers.remove(user.uuid);

		localUsers.forEach((receiverUuid, localUser) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

	}

	public Set<String> getRemoteIps() {

		return remoteIps;

	}

	private void sendAllBeaconsToLocalUser(String receiverUuid) {

		localUsers.forEach((uuid, user) -> {

			if (receiverUuid.equals(uuid))
				return;

			listener.sendToLocalUser(receiverUuid, user.beacon);

		});

		remoteUsers.forEach((uuid, user) -> {

			listener.sendToLocalUser(receiverUuid, user.beacon);

		});

	}

	public void addRemoteIp(String ip) {

		if (remoteIps.contains(ip))
			return;

		remoteIps.add(ip);

		persistRemoteIps();

		sendRemoteIpsToAllLocalUsers();

	}

	private void removeRemoteIp(String ip) {

		if (!remoteIps.contains(ip))
			return;

		remoteIps.remove(ip);

		persistRemoteIps();

		sendRemoteIpsToAllLocalUsers();

	}

	private void persistRemoteIps() {

		try (ObjectOutputStream ois = new ObjectOutputStream(Files.newOutputStream(Paths.get("./ip.dat")))) {

			ois.writeObject(remoteIps.toArray(new String[remoteIps.size()]));

			ois.flush();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	private void sendRemoteIpsToLocalUser(String receiverUuid) {

		String messagePojoStr = gson.toJson(new MessagePojo(String.join(";", remoteIps), null, ContentType.IP, null));

		listener.sendToLocalUser(receiverUuid, messagePojoStr);

	}

	private void sendRemoteIpsToAllLocalUsers() {

		String messagePojoStr = gson.toJson(new MessagePojo(String.join(";", remoteIps), null, ContentType.IP, null));

		localUsers.forEach((receiverUuid, user) -> {

			listener.sendToLocalUser(receiverUuid, messagePojoStr);

		});

	}

	private class User {

		final String uuid;
		String beacon;
		String dmsUuid;

		User(String uuid) {
			this.uuid = uuid;
		}

	}

	private class LocalUser extends User {

		final Map<Long, AtomicBoolean> sendStatusMap = Collections.synchronizedMap(new HashMap<Long, AtomicBoolean>());

		LocalUser(String uuid) {
			super(uuid);
		}

	}

}

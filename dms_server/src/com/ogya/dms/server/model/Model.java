package com.ogya.dms.server.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ogya.dms.common.structures.Beacon;
import com.ogya.dms.common.structures.ContentType;
import com.ogya.dms.common.structures.MessagePojo;
import com.ogya.dms.server.common.CommonMethods;
import com.ogya.dms.server.model.intf.ModelListener;

public class Model {

	private final ModelListener listener;

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

			MessagePojo messagePojo = CommonMethods.fromJson(messagePojoStr, MessagePojo.class);

			String senderUuid = messagePojo.senderUuid;
			Long messageId = messagePojo.messageId;

			switch (messagePojo.contentType) {

			case BCON:

				Beacon beacon = CommonMethods.fromJson(messagePojo.message, Beacon.class);

				String userUuid = beacon.uuid;

				if (userUuid == null)
					break;

				boolean fresh = localUsers.isEmpty();

				localUsers.putIfAbsent(userUuid, new LocalUser(userUuid));

				if (fresh)
					listener.publishImmediately();

				boolean isNew = localUsers.get(userUuid).beacon.uuid == null;

				copyBeacon(beacon, localUsers.get(userUuid).beacon);
				localUsers.forEach((receiverUuid, user) -> {

					if (Objects.equals(receiverUuid, userUuid))
						return;

					listener.sendToLocalUser(receiverUuid, messagePojoStr);

				});
				listener.sendToAllRemoteServers(messagePojoStr);

				if (isNew) {

					sendAllBeaconsToLocalUser(userUuid);

					sendRemoteIpsToLocalUser(userUuid);

				}

				break;

			case REQ_STRT:

				sendAllBeaconsToLocalUser(senderUuid);

				sendRemoteIpsToLocalUser(senderUuid);

				break;

			case ADD_IP:

				addRemoteIp(messagePojo.message);

				break;

			case REMOVE_IP:

				removeRemoteIp(messagePojo.message);

				break;

			case CANCEL: {

				if (messageId == null)
					break;

				LocalUser sender = localUsers.get(senderUuid);

				if (sender == null)
					break;

				AtomicBoolean sendStatus = sender.sendStatusMap.get(messageId);
				if (sendStatus != null)
					sendStatus.set(false);

				break;

			}

			default: {

				if (messagePojo.receiverUuid == null)
					break;

				final LocalUser sender = localUsers.get(senderUuid);

				final boolean trackedMessage = Objects.equals(messagePojo.contentType, ContentType.MESSAGE)
						&& sender != null && messageId != null;

				// This piece of code is disabled and commented out on purpose to remind that
				// in some cases, like conveying a status report, the sender is virtually set to
				// represent some remote users, which naturally do not appear on local users
				// list
//				if (sender == null)
//					break;

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

				if (trackedMessage && localReceiverUuids.size() > 0) {

					MessagePojo progressMessagePojo = new MessagePojo(String.valueOf(100),
							String.join(";", localReceiverUuids), senderUuid, ContentType.PROGRESS, messageId);

					listener.sendToLocalUser(senderUuid, CommonMethods.toJson(progressMessagePojo));

				}

				remoteServerReceiverUuids.forEach((dmsUuid, uuidList) -> {

					AtomicBoolean sendStatus = new AtomicBoolean(true);

					if (trackedMessage)
						sender.sendStatusMap.put(messageId, sendStatus);
					statusReceiverMap.put(sendStatus, uuidList);

					listener.sendToRemoteServer(dmsUuid, messagePojoStr, sendStatus, trackedMessage ? progress -> {

						if (progress < 0 || progress == 100) {

							sender.sendStatusMap.remove(messageId);
							statusReceiverMap.remove(sendStatus);

						}

						MessagePojo progressMessagePojo = new MessagePojo(String.valueOf(progress),
								String.join(";", uuidList), senderUuid, ContentType.PROGRESS, messageId);

						if (localUsers.containsKey(senderUuid))
							listener.sendToLocalUser(senderUuid, CommonMethods.toJson(progressMessagePojo));

					} : progress -> {

						if (progress < 0 || progress == 100)
							statusReceiverMap.remove(sendStatus);

					});

				});

				break;

			}

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public void remoteMessageReceived(String messagePojoStr, String dmsUuid) {

		try {

			MessagePojo messagePojo = CommonMethods.fromJson(messagePojoStr, MessagePojo.class);

			switch (messagePojo.contentType) {

			case BCON:

				Beacon beacon = CommonMethods.fromJson(messagePojo.message, Beacon.class);

				String userUuid = beacon.uuid;

				if (userUuid == null)
					break;

				remoteUsers.putIfAbsent(userUuid, new User(userUuid));

				checkRemoteUserServer(remoteUsers.get(userUuid), dmsUuid);

				// Uzak uuid yeni eklendi veya guncellendi.
				// Beacon, uzak beacon'larda guncellenecek.
				// Yeni beacon tum yerel kullanicilara dagitilacak.

				copyBeacon(beacon, remoteUsers.get(userUuid).beacon);
				localUsers.forEach((receiverUuid, user) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

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

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public void processAllLocalBeacons(Consumer<String> consumer) {

		localUsers.forEach((uuid, user) -> {

			consumer.accept(CommonMethods
					.toRemoteJson(new MessagePojo(CommonMethods.toJson(user.beacon), null, ContentType.BCON, null)));

		});

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

		synchronized (user.sendStatusMap) {
			user.sendStatusMap.forEach((messageId, status) -> status.set(false));
		}

		String userUuid = user.beacon.uuid;

		String messagePojoStr = CommonMethods
				.toJson(new MessagePojo(userUuid, "", ContentType.UUID_DISCONNECTED, null));

		localUsers.remove(userUuid);

		localUsers.forEach((receiverUuid, localUser) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

		listener.sendToAllRemoteServers(messagePojoStr);

	}

	private void checkRemoteUserServer(User remoteUser, String dmsUuid) {

		if (Objects.equals(dmsUuid, remoteUser.dmsUuid))
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

		String userUuid = user.beacon.uuid;

		synchronized (statusReceiverMap) {
			statusReceiverMap.forEach((sendStatus, receiverUuids) -> {
				if (receiverUuids.contains(userUuid) && receiverUuids.size() == 1)
					sendStatus.set(false);
			});
		}

		String messagePojoStr = CommonMethods
				.toJson(new MessagePojo(userUuid, "", ContentType.UUID_DISCONNECTED, null));

		remoteUsers.remove(userUuid);

		localUsers.forEach((receiverUuid, localUser) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

	}

	public Set<String> getRemoteIps() {

		return remoteIps;

	}

	private void sendAllBeaconsToLocalUser(String receiverUuid) {

		localUsers.forEach((uuid, user) -> {

			if (Objects.equals(receiverUuid, uuid))
				return;

			listener.sendToLocalUser(receiverUuid, CommonMethods
					.toJson(new MessagePojo(CommonMethods.toJson(user.beacon), null, ContentType.BCON, null)));

		});

		remoteUsers.forEach((uuid, user) -> {

			listener.sendToLocalUser(receiverUuid, CommonMethods
					.toJson(new MessagePojo(CommonMethods.toJson(user.beacon), null, ContentType.BCON, null)));

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

		String messagePojoStr = CommonMethods
				.toJson(new MessagePojo(String.join(";", remoteIps), null, ContentType.IP, null));

		listener.sendToLocalUser(receiverUuid, messagePojoStr);

	}

	private void sendRemoteIpsToAllLocalUsers() {

		String messagePojoStr = CommonMethods
				.toJson(new MessagePojo(String.join(";", remoteIps), null, ContentType.IP, null));

		localUsers.forEach((receiverUuid, user) -> {

			listener.sendToLocalUser(receiverUuid, messagePojoStr);

		});

	}

	private void copyBeacon(Beacon fromBeacon, Beacon toBeacon) {

		if (fromBeacon.name != null)
			toBeacon.name = fromBeacon.name;

		if (fromBeacon.comment != null)
			toBeacon.comment = fromBeacon.comment;

		if (fromBeacon.status != null)
			toBeacon.status = fromBeacon.status;

		if (fromBeacon.lattitude != null)
			toBeacon.lattitude = fromBeacon.lattitude;

		if (fromBeacon.longitude != null)
			toBeacon.longitude = fromBeacon.longitude;

	}

	private class User {

		protected final Beacon beacon;
		protected String dmsUuid;

		private User(String userUuid) {

			this.beacon = new Beacon(userUuid);

		}

	}

	private class LocalUser extends User {

		private final Map<Long, AtomicBoolean> sendStatusMap = Collections
				.synchronizedMap(new HashMap<Long, AtomicBoolean>());

		private LocalUser(String userUuid) {

			super(userUuid);

			try {

				beacon.addresses.add(InetAddress.getByName("localhost"));

			} catch (UnknownHostException e) {

				e.printStackTrace();

			}

		}

	}

}

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ogya.dms.common.structures.Beacon;
import com.ogya.dms.common.structures.ContentType;
import com.ogya.dms.common.structures.MessagePojo;
import com.ogya.dms.server.common.CommonMethods;
import com.ogya.dms.server.model.intf.ModelListener;

public class Model {

	private final ModelListener listener;

	private final Map<String, LocalUser> localUsers = Collections.synchronizedMap(new HashMap<String, LocalUser>());
	private final Map<String, RemoteUser> remoteUsers = Collections.synchronizedMap(new HashMap<String, RemoteUser>());

	private final Map<String, DmsServer> remoteServers = Collections.synchronizedMap(new HashMap<String, DmsServer>());

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

				copyBeacon(beacon, localUsers.get(userUuid).beacon);

				sendBeaconToLocalUsers(localUsers.get(userUuid).beacon);

				sendBeaconToRemoteServers(messagePojoStr);

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

						RemoteUser remoteUser = remoteUsers.get(receiverUuid);

						remoteServerReceiverUuids.putIfAbsent(remoteUser.dmsServer.dmsUuid, new HashSet<String>());
						remoteServerReceiverUuids.get(remoteUser.dmsServer.dmsUuid).add(receiverUuid);

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

				remoteServers.putIfAbsent(dmsUuid, new DmsServer(dmsUuid));
				remoteUsers.putIfAbsent(userUuid, new RemoteUser(userUuid, remoteServers.get(dmsUuid)));
				remoteServers.get(dmsUuid).users.putIfAbsent(userUuid, remoteUsers.get(userUuid));

				copyBeacon(beacon, remoteUsers.get(userUuid).beacon);

				sendBeaconToLocalUsers(remoteUsers.get(userUuid).beacon);

				break;

			case UUID_DISCONNECTED:

				String uuid = messagePojo.message;

				remoteUserDisconnected(remoteUsers.get(uuid));

				DmsServer dmsServer = remoteServers.get(dmsUuid);

				if (dmsServer != null)
					dmsServer.users.remove(uuid);

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

	public void testAllLocalUsers() {

		localUsers.forEach((uuid, user) -> listener.sendToLocalUser(uuid, ""));

	}

	public void serverConnectionsUpdated(String dmsUuid, List<InetAddress> addresses) {

		if (addresses.size() == 0) {

			remoteServerDisconnected(dmsUuid);

			return;

		}

		remoteServers.putIfAbsent(dmsUuid, new DmsServer(dmsUuid));

		DmsServer dmsServer = remoteServers.get(dmsUuid);

		if (dmsServer.addresses.size() == 0) {
			// Connection just established with the server

			sendAllBeaconsToRemoteServer(dmsUuid);

		}

		dmsServer.addresses.clear();
		dmsServer.addresses.addAll(addresses);

		dmsServer.users.forEach((uuid, user) -> sendBeaconToLocalUsers(user.beacon));

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
				.toJson(new MessagePojo(userUuid, null, ContentType.UUID_DISCONNECTED, null));

		localUsers.remove(userUuid);

		localUsers.forEach((receiverUuid, localUser) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

		listener.sendToAllRemoteServers(messagePojoStr);

	}

	private void remoteServerDisconnected(String dmsUuid) {

		if (!remoteServers.containsKey(dmsUuid))
			return;

		remoteServers.remove(dmsUuid).users.forEach((userUuid, user) -> remoteUserDisconnected(user));

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
				.toJson(new MessagePojo(userUuid, null, ContentType.UUID_DISCONNECTED, null));

		remoteUsers.remove(userUuid);

		localUsers.forEach((receiverUuid, localUser) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

	}

	public Set<String> getRemoteIps() {

		return remoteIps;

	}

	private void sendBeaconToLocalUsers(Beacon beacon) {

		String beaconStr = CommonMethods
				.toJson(new MessagePojo(CommonMethods.toJson(beacon), null, ContentType.BCON, null));

		localUsers.forEach((uuid, user) -> {

			if (Objects.equals(beacon.uuid, uuid))
				return;

			listener.sendToLocalUser(uuid, beaconStr);

		});

	}

	private void sendAllBeaconsToLocalUser(String receiverUuid) {

		localUsers.forEach((uuid, user) -> {

			if (Objects.equals(receiverUuid, uuid))
				return;

			String beaconStr = CommonMethods
					.toJson(new MessagePojo(CommonMethods.toJson(user.beacon), null, ContentType.BCON, null));

			listener.sendToLocalUser(receiverUuid, beaconStr);

		});

		remoteUsers.forEach((uuid, user) -> {

			String beaconStr = CommonMethods
					.toJson(new MessagePojo(CommonMethods.toJson(user.beacon), null, ContentType.BCON, null));

			listener.sendToLocalUser(receiverUuid, beaconStr);

		});

	}

	private void sendBeaconToRemoteServers(String beaconStr) {

		listener.sendToAllRemoteServers(beaconStr);

	}

	/**
	 * 
	 * @param dmsUuid
	 * @apiNote Call when remote server just connected
	 */
	private void sendAllBeaconsToRemoteServer(String dmsUuid) {

		localUsers.forEach((uuid, user) -> {

			String beaconStr = CommonMethods
					.toRemoteJson(new MessagePojo(CommonMethods.toJson(user.beacon), null, ContentType.BCON, null));

			listener.sendToRemoteServer(dmsUuid, beaconStr, null, null);

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

	private abstract class User {

		protected final Beacon beacon;

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

				beacon.addresses = Arrays.asList(InetAddress.getByName("localhost"));

			} catch (UnknownHostException e) {

				e.printStackTrace();

			}

		}

	}

	private class RemoteUser extends User {

		private final DmsServer dmsServer;

		private RemoteUser(String userUuid, DmsServer dmsServer) {

			super(userUuid);

			this.dmsServer = dmsServer;

			this.beacon.addresses = dmsServer.addresses;

		}

	}

	private class DmsServer {

		private final String dmsUuid;
		private final Map<String, User> users = Collections.synchronizedMap(new HashMap<String, User>());
		private final List<InetAddress> addresses = Collections.synchronizedList(new ArrayList<InetAddress>());

		private DmsServer(String dmsUuid) {

			this.dmsUuid = dmsUuid;

		}

	}

}

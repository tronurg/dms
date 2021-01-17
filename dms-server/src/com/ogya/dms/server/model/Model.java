package com.ogya.dms.server.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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
import java.util.concurrent.atomic.AtomicInteger;

import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.model.intf.ModelListener;

public class Model {

	private static final AtomicInteger MAP_ID = new AtomicInteger(0);

	private final ModelListener listener;

	private final Map<String, LocalUser> localUsers = Collections.synchronizedMap(new HashMap<String, LocalUser>());
	private final Map<String, RemoteUser> remoteUsers = Collections.synchronizedMap(new HashMap<String, RemoteUser>());

	private final Map<String, User> mappedUsers = Collections.synchronizedMap(new HashMap<String, User>());
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

			MessagePojo messagePojo = MessagePojo.fromJson(messagePojoStr);

			String senderUuid = messagePojo.senderUuid;
			Long messageId = messagePojo.messageId;

			switch (messagePojo.contentType) {

			case BCON:

				Beacon beacon = Beacon.fromJson(messagePojo.message);

				String userUuid = beacon.uuid;

				if (userUuid == null)
					break;

				boolean fresh = localUsers.isEmpty();

				localUsers.putIfAbsent(userUuid, new LocalUser(userUuid, String.valueOf(MAP_ID.getAndIncrement())));

				if (fresh)
					listener.publishImmediately();

				User localUser = localUsers.get(userUuid);

				mappedUsers.putIfAbsent(localUser.mapId, localUser);

				copyBeacon(beacon, localUser.beacon);

				sendBeaconToLocalUsers(localUser.beacon);

				sendBeaconToRemoteServers(new MessagePojo(messagePojo.message, localUser.mapId,
						messagePojo.receiverUuid, messagePojo.contentType, messagePojo.messageId).toJson());

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

					listener.sendToLocalUser(senderUuid, progressMessagePojo.toJson());

				}

				remoteServerReceiverUuids.forEach((dmsUuid, uuidList) -> {

					AtomicBoolean sendStatus = new AtomicBoolean(true);

					if (trackedMessage)
						sender.sendStatusMap.put(messageId, sendStatus);
					statusReceiverMap.put(sendStatus, uuidList);

					String senderMapId = sender == null ? messagePojo.senderUuid : sender.mapId;
					List<String> receiverMapIdList = new ArrayList<String>();
					uuidList.forEach(uuid -> receiverMapIdList.add(remoteUsers.get(uuid).mapId));
					String remoteMessagePojoStr = new MessagePojo(messagePojo.message, senderMapId,
							String.join(";", receiverMapIdList), messagePojo.contentType, messagePojo.messageId)
									.toJson();

					listener.sendToRemoteServer(dmsUuid, remoteMessagePojoStr, sendStatus,
							trackedMessage ? progress -> {

								if (progress < 0 || progress == 100) {

									sender.sendStatusMap.remove(messageId);
									statusReceiverMap.remove(sendStatus);

								}

								MessagePojo progressMessagePojo = new MessagePojo(String.valueOf(progress),
										String.join(";", uuidList), senderUuid, ContentType.PROGRESS, messageId);

								if (localUsers.containsKey(senderUuid))
									listener.sendToLocalUser(senderUuid, progressMessagePojo.toJson());

							} : progress -> {

								if (progress < 0 || progress == 100)
									statusReceiverMap.remove(sendStatus);

							}, messagePojo.useLocalAddress);

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

			MessagePojo messagePojo = MessagePojo.fromJson(messagePojoStr);

			DmsServer remoteServer = remoteServers.get(dmsUuid);

			if (!(Objects.equals(messagePojo.contentType, ContentType.BCON) || remoteServer == null)) {

				User remoteUser = remoteServer.mappedUsers.get(messagePojo.senderUuid);
				String senderUuid = remoteUser == null ? messagePojo.senderUuid : remoteUser.beacon.uuid;
				List<String> receiverUuids = new ArrayList<String>();
				if (messagePojo.receiverUuid != null) {
					for (String receiverMapId : messagePojo.receiverUuid.split(";")) {
						User localUser = mappedUsers.get(receiverMapId);
						receiverUuids.add(localUser == null ? receiverMapId : localUser.beacon.uuid);
					}
				}
				String receiverUuid = String.join(";", receiverUuids);

				messagePojo = new MessagePojo(messagePojo.message, senderUuid, receiverUuid, messagePojo.contentType,
						messagePojo.messageId);

				messagePojoStr = messagePojo.toJson();

			}

			switch (messagePojo.contentType) {

			case BCON:

				Beacon beacon = Beacon.fromJson(messagePojo.message);

				String userUuid = beacon.uuid;

				if (userUuid == null)
					break;

				String mapId = messagePojo.senderUuid;

				remoteServers.putIfAbsent(dmsUuid, new DmsServer(dmsUuid));
				remoteUsers.putIfAbsent(userUuid, new RemoteUser(userUuid, mapId, remoteServers.get(dmsUuid)));
				remoteServers.get(dmsUuid).mappedUsers.putIfAbsent(mapId, remoteUsers.get(userUuid));

				copyBeacon(beacon, remoteUsers.get(userUuid).beacon);

				sendBeaconToLocalUsers(remoteUsers.get(userUuid).beacon);

				break;

			case UUID_DISCONNECTED:

				String uuid = messagePojo.senderUuid;

				User remoteUser = remoteUsers.get(uuid);

				DmsServer dmsServer = remoteServers.get(dmsUuid);

				if (dmsServer != null)
					dmsServer.mappedUsers.remove(remoteUser.mapId);

				remoteUserDisconnected(remoteUser);

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

	public void serverConnectionsUpdated(String dmsUuid, List<InetAddress> remoteAddresses,
			List<InetAddress> localAddresses) {

		if (remoteAddresses.size() == 0) {

			remoteServerDisconnected(dmsUuid);

			return;

		}

		remoteServers.putIfAbsent(dmsUuid, new DmsServer(dmsUuid));

		DmsServer dmsServer = remoteServers.get(dmsUuid);

		// This block is commented out upon a half-open connection error. See below.
//		if (dmsServer.addresses.size() == 0) {
//			// Connection just established with the server
//
//			sendAllBeaconsToRemoteServer(dmsUuid);
//
//		}

		dmsServer.remoteAddresses.clear();
		dmsServer.remoteAddresses.addAll(remoteAddresses);
		dmsServer.localAddresses.clear();
		dmsServer.localAddresses.addAll(localAddresses);

		// This block is added upon a half-open connection error.
		sendAllBeaconsToRemoteServer(dmsUuid);

		dmsServer.mappedUsers.forEach((mapId, user) -> sendBeaconToLocalUsers(user.beacon));

	}

	public void localUuidDisconnected(String uuid) {

		LocalUser user = localUsers.get(uuid);

		if (user == null)
			return;

		synchronized (user.sendStatusMap) {
			user.sendStatusMap.forEach((messageId, status) -> status.set(false));
		}

		mappedUsers.remove(user.mapId);

		String userUuid = user.beacon.uuid;

		String messagePojoStr = new MessagePojo(null, userUuid, ContentType.UUID_DISCONNECTED, null).toJson();

		localUsers.remove(userUuid);

		localUsers.forEach((receiverUuid, localUser) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

		String remoteMessagePojoStr = new MessagePojo(null, user.mapId, ContentType.UUID_DISCONNECTED, null).toJson();

		listener.sendToAllRemoteServers(remoteMessagePojoStr);

	}

	private void remoteServerDisconnected(String dmsUuid) {

		if (!remoteServers.containsKey(dmsUuid))
			return;

		remoteServers.remove(dmsUuid).mappedUsers.forEach((mapId, user) -> remoteUserDisconnected(user));

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

		String messagePojoStr = new MessagePojo(null, userUuid, ContentType.UUID_DISCONNECTED, null).toJson();

		remoteUsers.remove(userUuid);

		localUsers.forEach((receiverUuid, localUser) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

	}

	public Set<String> getRemoteIps() {

		return remoteIps;

	}

	private void sendBeaconToLocalUsers(Beacon beacon) {

		String beaconStr = new MessagePojo(beacon.toJson(), null, ContentType.BCON, null).toJson();

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

			String beaconStr = new MessagePojo(user.beacon.toJson(), null, ContentType.BCON, null).toJson();

			listener.sendToLocalUser(receiverUuid, beaconStr);

		});

		remoteUsers.forEach((uuid, user) -> {

			String beaconStr = new MessagePojo(user.beacon.toJson(), null, ContentType.BCON, null).toJson();

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

			String beaconStr = new MessagePojo(user.beacon.toRemoteJson(), user.mapId, ContentType.BCON, null).toJson();

			listener.sendToRemoteServer(dmsUuid, beaconStr, null, null, null);

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

		String messagePojoStr = new MessagePojo(String.join(";", remoteIps), null, ContentType.IP, null).toJson();

		listener.sendToLocalUser(receiverUuid, messagePojoStr);

	}

	private void sendRemoteIpsToAllLocalUsers() {

		String messagePojoStr = new MessagePojo(String.join(";", remoteIps), null, ContentType.IP, null).toJson();

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
		protected final String mapId;

		private User(String userUuid, String mapId) {

			this.beacon = new Beacon(userUuid);

			this.mapId = mapId;

		}

	}

	private class LocalUser extends User {

		private final Map<Long, AtomicBoolean> sendStatusMap = Collections
				.synchronizedMap(new HashMap<Long, AtomicBoolean>());

		private LocalUser(String userUuid, String mapId) {

			super(userUuid, mapId);

			try {

				List<InetAddress> inetAddresses = new ArrayList<InetAddress>();
				for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
					for (InetAddress ia : Collections.list(ni.getInetAddresses())) {
						if (!(ia instanceof Inet4Address) || ia.isLoopbackAddress())
							continue;
						inetAddresses.add(ia);
					}
				}

				beacon.remoteInterfaces = inetAddresses;
				beacon.localInterfaces = inetAddresses;

			} catch (SocketException e) {

				e.printStackTrace();

			}

		}

	}

	private class RemoteUser extends User {

		private final DmsServer dmsServer;

		private RemoteUser(String userUuid, String mapId, DmsServer dmsServer) {

			super(userUuid, mapId);

			this.dmsServer = dmsServer;

			this.beacon.remoteInterfaces = dmsServer.remoteAddresses;
			this.beacon.localInterfaces = dmsServer.localAddresses;

		}

	}

	private class DmsServer {

		private final String dmsUuid;
		private final Map<String, User> mappedUsers = Collections.synchronizedMap(new HashMap<String, User>());
		private final List<InetAddress> remoteAddresses = Collections.synchronizedList(new ArrayList<InetAddress>());
		private final List<InetAddress> localAddresses = Collections.synchronizedList(new ArrayList<InetAddress>());

		private DmsServer(String dmsUuid) {

			this.dmsUuid = dmsUuid;

		}

	}

}

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.model.intf.ModelListener;

public class Model {

	private static final byte[] TEST_DATA = new byte[0];
	private static final AtomicInteger MAP_ID = new AtomicInteger(0);

	private final ModelListener listener;

	private final Map<String, LocalUser> localUsers = Collections.synchronizedMap(new HashMap<String, LocalUser>());
	private final Map<String, RemoteUser> remoteUsers = Collections.synchronizedMap(new HashMap<String, RemoteUser>());
	private final Map<String, LocalUser> localMappedUsers = Collections
			.synchronizedMap(new HashMap<String, LocalUser>());
	private final Map<String, DmsServer> remoteServers = Collections.synchronizedMap(new HashMap<String, DmsServer>());

	private final Set<InetAddress> remoteIps = Collections.synchronizedSet(new LinkedHashSet<InetAddress>());
	private final Set<InetAddress> remoteAddresses = new HashSet<InetAddress>();

	private final AtomicInteger messageCounter = new AtomicInteger(1);

	public Model(ModelListener listener) {
		this.listener = listener;
		init();
	}

	private void init() {

		Path ipDatPath = Paths.get("./ip.dat");

		if (Files.notExists(ipDatPath))
			return;

		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(ipDatPath))) {

			for (InetAddress ip : (InetAddress[]) ois.readObject()) {

				remoteIps.add(ip);

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public boolean isLive() {

		return !localUsers.isEmpty();

	}

	public void localMessageReceived(int messageNumber, byte[] data, String userUuid) {

		LocalUser localUser = localUsers.get(userUuid);
		if (localUser == null) {
			localUser = new LocalUser(userUuid, String.valueOf(MAP_ID.getAndIncrement()));
			localUsers.put(userUuid, localUser);
			localMappedUsers.put(localUser.mapId, localUser);
		}

		localUser.messageReceived(messageNumber, data);

	}

	public void remoteMessageReceived(int messageNumber, byte[] data, String dmsUuid) {

		DmsServer remoteServer = remoteServers.get(dmsUuid);
		if (remoteServer == null) {
			remoteServer = new DmsServer(dmsUuid);
			remoteServers.put(dmsUuid, remoteServer);
		}

		remoteServer.messageReceived(messageNumber, data);

	}

	private void localMessageReceived(final int messageNumber, final MessagePojo messagePojo) {

		try {

			switch (messagePojo.contentType) {

			case BCON: {

				Beacon beacon = DmsPackingFactory.unpack(messagePojo.payload, Beacon.class);
				beacon.serverUuid = CommonConstants.DMS_UUID;

				String userUuid = beacon.uuid;

				if (userUuid == null)
					break;

				if (localUsers.values().stream().noneMatch(user -> user.beacon.status != null))
					listener.publishImmediately();

				User localUser = localUsers.get(userUuid);

				copyBeacon(beacon, localUser.beacon);

				sendBeaconToLocalUsers(localUser.beacon);

				sendRemoteMessageToAll(messageNumber,
						DmsPackingFactory.packServerToServer(new MessagePojo(messagePojo.payload, localUser.mapId, null,
								null, messagePojo.contentType, null, null)));

				break;

			}

			case REQ_STRT: {

				sendAllBeaconsToLocalUser(messagePojo.senderUuid);

				sendRemoteIpsToLocalUser(messagePojo.senderUuid);

				break;

			}

			case ADD_IPS: {

				addRemoteIps(DmsPackingFactory.unpack(messagePojo.payload, InetAddress[].class));

				break;

			}

			case REMOVE_IPS: {

				InetAddress[] ips = DmsPackingFactory.unpack(messagePojo.payload, InetAddress[].class);

				if (ips.length == 0)
					clearRemoteIps();
				else
					removeRemoteIps(ips);

				break;

			}

			default: {

				if (messagePojo.receiverAddress == null || messagePojo.receiverUuids == null) {
					break;
				}

				LocalUser sender = localUsers.get(messagePojo.senderUuid);

				if (CommonConstants.DMS_UUID.equals(messagePojo.receiverAddress)) {
					sendLocalMessage(messageNumber, DmsPackingFactory.packServerToClient(messagePojo),
							messagePojo.receiverUuids);
				} else {
					messagePojo.senderUuid = sender == null ? messagePojo.senderUuid : sender.mapId;
					messagePojo.receiverUuids.replaceAll(uuid -> {
						RemoteUser remoteUser = remoteUsers.get(uuid);
						if (remoteUser == null) {
							return uuid;
						}
						return remoteUser.mapId;
					});
					sendRemoteMessage(messageNumber, DmsPackingFactory.packServerToServer(messagePojo),
							messagePojo.receiverAddress);
				}

				break;

			}

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public void testAllLocalUsers() {

		sendLocalMessageToAll(0, TEST_DATA);

	}

	public void serverConnectionsUpdated(String dmsUuid, Map<InetAddress, InetAddress> localRemoteIps,
			boolean beaconsRequested) {

		if (localRemoteIps.isEmpty()) {
			remoteServerDisconnected(dmsUuid);
			return;
		}

		DmsServer dmsServer = remoteServers.get(dmsUuid);
		if (dmsServer == null) {
			dmsServer = new DmsServer(dmsUuid);
			remoteServers.put(dmsUuid, dmsServer);
		}

		dmsServer.localRemoteIps.clear();
		dmsServer.localRemoteIps.putAll(localRemoteIps);

		// This block is added upon a half-open connection error.
		if (beaconsRequested)
			sendAllBeaconsToRemoteServer(dmsUuid);

		dmsServer.remoteMappedUsers.forEach((mapId, user) -> sendBeaconToLocalUsers(user.beacon));

	}

	public void localUuidDisconnected(String uuid) {

		LocalUser user = localUsers.remove(uuid);

		if (user == null)
			return;

		localMappedUsers.remove(user.mapId);

		MessagePojo messagePojo = new MessagePojo(null, uuid, null, null, ContentType.UUID_DISCONNECTED, null, null);
		sendLocalMessageToAll(0, DmsPackingFactory.packServerToClient(messagePojo));

		MessagePojo remoteMessagePojo = new MessagePojo(null, user.mapId, null, null, ContentType.UUID_DISCONNECTED,
				null, null);
		sendRemoteMessageToAll(0, DmsPackingFactory.packServerToServer(remoteMessagePojo));

	}

	private void remoteServerDisconnected(String dmsUuid) {

		DmsServer dmsServer = remoteServers.remove(dmsUuid);

		if (dmsServer == null)
			return;

		dmsServer.remoteMappedUsers.forEach((mapId, user) -> remoteUserDisconnected(user));

	}

	private void remoteUserDisconnected(User user) {

		if (user == null)
			return;

		String userUuid = user.beacon.uuid;

		remoteUsers.remove(userUuid);

		MessagePojo messagePojo = new MessagePojo(null, userUuid, null, null, ContentType.UUID_DISCONNECTED, null,
				null);
		sendLocalMessageToAll(0, DmsPackingFactory.packServerToClient(messagePojo));

	}

	public Set<InetAddress> getRemoteAddresses() {

		remoteAddresses.clear();
		remoteIps.forEach(remoteIp -> remoteAddresses.add(remoteIp));
		return remoteAddresses;

	}

	private void sendBeaconToLocalUsers(Beacon beacon) {

		MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(beacon), null, null, null,
				ContentType.BCON, null, null);
		List<String> receiverUuids = new ArrayList<String>(localUsers.keySet());
		receiverUuids.remove(beacon.uuid);
		sendLocalMessage(0, DmsPackingFactory.packServerToClient(beaconPojo), receiverUuids);

	}

	private void sendAllBeaconsToLocalUser(final String receiverUuid) {

		localUsers.forEach((uuid, user) -> {

			if (user.beacon.status == null || Objects.equals(receiverUuid, uuid))
				return;

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(user.beacon), null, null,
					null, ContentType.BCON, null, null);
			sendLocalMessage(0, DmsPackingFactory.packServerToClient(beaconPojo),
					Collections.singletonList(receiverUuid));

		});

		remoteUsers.forEach((uuid, user) -> {

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(user.beacon), null, null,
					null, ContentType.BCON, null, null);
			sendLocalMessage(0, DmsPackingFactory.packServerToClient(beaconPojo),
					Collections.singletonList(receiverUuid));

		});

	}

	/**
	 * 
	 * @param dmsUuid
	 * @apiNote Call when remote server just connected
	 */
	private void sendAllBeaconsToRemoteServer(String dmsUuid) {

		localUsers.forEach((uuid, user) -> {

			if (user.beacon.status == null)
				return;

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToServer(user.beacon), user.mapId,
					null, null, ContentType.BCON, null, null);
			sendRemoteMessage(0, DmsPackingFactory.packServerToServer(beaconPojo), dmsUuid);

		});

	}

	public void addRemoteIps(InetAddress... ips) {

		boolean changed = false;

		for (InetAddress ip : ips) {

			if (remoteIps.contains(ip))
				continue;

			changed = true;

			remoteIps.add(ip);

		}

		if (!changed)
			return;

		persistRemoteIps();

		sendRemoteIpsToAllLocalUsers();

		listener.publishImmediately();

	}

	private void removeRemoteIps(InetAddress... ips) {

		boolean changed = false;

		for (InetAddress ip : ips) {

			if (!remoteIps.contains(ip))
				continue;

			changed = true;

			remoteIps.remove(ip);

		}

		if (!changed)
			return;

		persistRemoteIps();

		sendRemoteIpsToAllLocalUsers();

	}

	private void clearRemoteIps() {

		if (remoteIps.isEmpty())
			return;

		remoteIps.clear();

		persistRemoteIps();

		sendRemoteIpsToAllLocalUsers();

	}

	private void persistRemoteIps() {

		try (ObjectOutputStream ois = new ObjectOutputStream(Files.newOutputStream(Paths.get("./ip.dat")))) {

			ois.writeObject(remoteIps.toArray(new InetAddress[0]));

			ois.flush();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	private void sendRemoteIpsToLocalUser(String receiverUuid) {

		LocalUser localUser = localUsers.get(receiverUuid);
		if (localUser == null) {
			return;
		}

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(remoteIps), null, null, null, ContentType.IPS,
				null, null);
		sendLocalMessage(0, DmsPackingFactory.packServerToClient(messagePojo), Collections.singletonList(receiverUuid));

	}

	private void sendRemoteIpsToAllLocalUsers() {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(remoteIps), null, null, null, ContentType.IPS,
				null, null);
		sendLocalMessageToAll(0, DmsPackingFactory.packServerToClient(messagePojo));

	}

	private void copyBeacon(Beacon fromBeacon, Beacon toBeacon) {

		if (fromBeacon.name != null)
			toBeacon.name = fromBeacon.name;

		if (fromBeacon.comment != null)
			toBeacon.comment = fromBeacon.comment;

		if (fromBeacon.status != null)
			toBeacon.status = fromBeacon.status;

		if (fromBeacon.latitude != null)
			toBeacon.latitude = fromBeacon.latitude;

		if (fromBeacon.longitude != null)
			toBeacon.longitude = fromBeacon.longitude;

		if (fromBeacon.secretId != null)
			toBeacon.secretId = fromBeacon.secretId;

	}

	private int mapMessageNumber(int messageNumber) {
		if (messageNumber == 0) {
			return 0;
		}
		int mappedMessageNumber = messageCounter.getAndIncrement();
		if (messageCounter.get() < 0) {
			messageCounter.set(1);
		}
		return mappedMessageNumber;
	}

	private void sendLocalMessage(int messageNumber, byte[] data, List<String> receiverUuids) {
		listener.sendToLocalUsers(messageNumber, data, receiverUuids);
	}

	private void sendLocalMessageToAll(int messageNumber, byte[] data) {
		sendLocalMessage(messageNumber, data, new ArrayList<String>(localUsers.keySet()));
	}

	private void sendRemoteMessage(int messageNumber, byte[] data, String receiverAddress) {
		listener.sendToRemoteServer(messageNumber, data, receiverAddress);
	}

	private void sendRemoteMessageToAll(int messageNumber, byte[] data) {
		sendRemoteMessage(messageNumber, data, null);
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

		private final Map<Integer, MessageInfo> messageMap = Collections
				.synchronizedMap(new HashMap<Integer, MessageInfo>());

		private LocalUser(String userUuid, String mapId) {

			super(userUuid, mapId);

			try {

				Map<InetAddress, InetAddress> localRemoteIps = new HashMap<InetAddress, InetAddress>();
				for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
					for (InetAddress ia : Collections.list(ni.getInetAddresses())) {
						if ((ia instanceof Inet4Address) && !ia.isLoopbackAddress())
							localRemoteIps.put(ia, ia);
					}
				}

				beacon.localRemoteServerIps = localRemoteIps;

			} catch (SocketException e) {

				e.printStackTrace();

			}

		}

		private void messageReceived(int messageNumber, byte[] data) {
			int sign = Integer.signum(messageNumber);
			int absMessageNumber = Math.abs(messageNumber);
			MessageInfo messageInfo = messageMap.get(absMessageNumber);
			if (messageInfo != null) {
				if (CommonConstants.DMS_UUID.equals(messageInfo.receiverAddress)) {
					sendLocalMessage(sign * messageInfo.mappedMessageNumber, data, messageInfo.receiverUuids);
				} else {
					sendRemoteMessage(sign * messageInfo.mappedMessageNumber, data, messageInfo.receiverAddress);
				}
				if (sign < 0) {
					messageMap.remove(absMessageNumber);
				}
				return;
			}
			try {
				MessagePojo messagePojo = DmsPackingFactory.unpack(data, MessagePojo.class);
				int mappedMessageNumber = mapMessageNumber(absMessageNumber);
				if (sign > 0) {
					messageMap.put(absMessageNumber, new MessageInfo(mappedMessageNumber, messagePojo.receiverAddress,
							messagePojo.receiverUuids));
				}
				localMessageReceived(sign * mappedMessageNumber, messagePojo);
			} catch (Exception e) {

			}
		}

	}

	private class RemoteUser extends User {

		private RemoteUser(String userUuid, String mapId, DmsServer dmsServer) {

			super(userUuid, mapId);

			this.beacon.localRemoteServerIps = dmsServer.localRemoteIps;

		}

	}

	private class DmsServer {

		private final String dmsUuid;
		private final Map<String, RemoteUser> remoteMappedUsers = Collections
				.synchronizedMap(new HashMap<String, RemoteUser>());
		private final Map<InetAddress, InetAddress> localRemoteIps = Collections
				.synchronizedMap(new HashMap<InetAddress, InetAddress>());

		private final Map<Integer, MessageInfo> messageMap = Collections
				.synchronizedMap(new HashMap<Integer, MessageInfo>());

		private DmsServer(String dmsUuid) {

			this.dmsUuid = dmsUuid;

		}

		private void messageReceived(int messageNumber, byte[] data) {
			int sign = Integer.signum(messageNumber);
			int absMessageNumber = Math.abs(messageNumber);
			MessageInfo messageInfo = messageMap.get(absMessageNumber);
			if (messageInfo != null) {
				if (CommonConstants.DMS_UUID.equals(messageInfo.receiverAddress)) {
					sendLocalMessage(sign * messageInfo.mappedMessageNumber, data, messageInfo.receiverUuids);
				} else {
					sendRemoteMessage(sign * messageInfo.mappedMessageNumber, data, messageInfo.receiverAddress);
				}
				if (sign < 0) {
					messageMap.remove(absMessageNumber);
				}
				return;
			}
			try {
				MessagePojo messagePojo = DmsPackingFactory.unpack(data, MessagePojo.class);
				int mappedMessageNumber = mapMessageNumber(absMessageNumber);
				if (sign > 0) {
					messageMap.put(absMessageNumber, new MessageInfo(mappedMessageNumber, messagePojo.receiverAddress,
							messagePojo.receiverUuids));
				}
				remoteMessageReceived(sign * mappedMessageNumber, messagePojo);
			} catch (Exception e) {

			}
		}

		private void remoteMessageReceived(final int messageNumber, final MessagePojo messagePojo) {

			try {

				String mapId = messagePojo.senderUuid;

				RemoteUser sender = remoteMappedUsers.get(mapId);
				if (sender != null) {
					messagePojo.senderUuid = sender.beacon.uuid;
				}

				if (messagePojo.receiverUuids != null) {
					messagePojo.receiverUuids.replaceAll(uuid -> {
						User localUser = remoteMappedUsers.get(uuid);
						if (localUser == null) {
							return uuid;
						}
						return localUser.beacon.uuid;
					});
				}

				switch (messagePojo.contentType) {

				case BCON: {

					Beacon beacon = DmsPackingFactory.unpack(messagePojo.payload, Beacon.class);
					beacon.serverUuid = dmsUuid;

					String userUuid = beacon.uuid;

					if (userUuid == null)
						break;

					RemoteUser remoteUser = remoteUsers.get(userUuid);
					if (remoteUser == null) {
						remoteUser = new RemoteUser(userUuid, mapId, this);
						remoteUsers.put(userUuid, remoteUser);
						remoteMappedUsers.put(mapId, remoteUser);
					}

					copyBeacon(beacon, remoteUsers.get(userUuid).beacon);

					sendBeaconToLocalUsers(remoteUsers.get(userUuid).beacon);

					break;

				}

				case UUID_DISCONNECTED: {

					String uuid = messagePojo.senderUuid;

					User remoteUser = remoteUsers.get(uuid);

					remoteMappedUsers.remove(remoteUser.mapId);

					remoteUserDisconnected(remoteUser);

					break;

				}

				default: {

					if (messagePojo.receiverAddress == null || messagePojo.receiverUuids == null) {
						break;
					}

					sendLocalMessage(messageNumber, DmsPackingFactory.packServerToClient(messagePojo),
							messagePojo.receiverUuids);

					break;

				}

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		}

	}

	private class MessageInfo {

		private final int mappedMessageNumber;
		private final String receiverAddress;
		private final List<String> receiverUuids;

		public MessageInfo(int mappedMessageNumber, String receiverAddress, List<String> receiverUuids) {
			this.mappedMessageNumber = mappedMessageNumber;
			this.receiverAddress = receiverAddress;
			this.receiverUuids = receiverUuids;
		}

	}

}

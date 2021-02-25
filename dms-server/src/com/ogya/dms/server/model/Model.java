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
import java.util.function.Consumer;

import com.ogya.dms.commons.DmsMessageFactory;
import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.model.intf.ModelListener;

public class Model {

	private static final MessagePojo TEST = new MessagePojo();

	private static final AtomicInteger MAP_ID = new AtomicInteger(0);

	private final ModelListener listener;

	private final Map<String, LocalUser> localUsers = Collections.synchronizedMap(new HashMap<String, LocalUser>());
	private final Map<String, RemoteUser> remoteUsers = Collections.synchronizedMap(new HashMap<String, RemoteUser>());

	private final Map<String, User> mappedUsers = Collections.synchronizedMap(new HashMap<String, User>());
	private final Map<String, DmsServer> remoteServers = Collections.synchronizedMap(new HashMap<String, DmsServer>());

	private final List<SendStatus> sendStatuses = Collections.synchronizedList(new ArrayList<SendStatus>());

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

	public void localMessageReceived(byte[] data, String userUuid) {

		if (localUsers.isEmpty())
			listener.publishImmediately();

		LocalUser localUser = localUsers.get(userUuid);
		if (localUser == null) {
			localUser = new LocalUser(userUuid, String.valueOf(MAP_ID.getAndIncrement()), this::localMessageReceived);
			localUsers.put(userUuid, localUser);
			mappedUsers.put(localUser.mapId, localUser);
		}

		localUser.messageFactory.inFeed(data);

	}

	private void localMessageReceived(final MessagePojo messagePojo) {

		try {

			switch (messagePojo.contentType) {

			case BCON:

				Beacon beacon = DmsPackingFactory.unpack(messagePojo.payload, Beacon.class);

				String userUuid = beacon.uuid;

				if (userUuid == null)
					break;

				User localUser = localUsers.get(userUuid);

				copyBeacon(beacon, localUser.beacon);

				sendBeaconToLocalUsers(localUser.beacon);

				sendMessageToRemoteServers(new MessagePojo(messagePojo.payload, localUser.mapId,
						messagePojo.receiverUuid, messagePojo.contentType, messagePojo.messageId, null, null, null));

				break;

			case REQ_STRT:

				sendAllBeaconsToLocalUser(messagePojo.senderUuid);

				sendRemoteIpsToLocalUser(messagePojo.senderUuid);

				break;

			case ADD_IPS:

				addRemoteIps(DmsPackingFactory.unpack(messagePojo.payload, String[].class));

				break;

			case REMOVE_IPS:

				String[] ips = DmsPackingFactory.unpack(messagePojo.payload, String[].class);

				if (ips.length == 0)
					clearRemoteIps();
				else
					removeRemoteIps(ips);

				break;

			case CANCEL: {

				sendStatuses.stream()
						.filter(sendStatus -> Objects.equals(sendStatus.trackingId, messagePojo.useTrackingId)
								&& Objects.equals(sendStatus.senderUuid, messagePojo.senderUuid))
						.forEach(sendStatus -> sendStatus.status.set(false));

				break;

			}

			default: {

				if (messagePojo.receiverUuid == null)
					break;

				final LocalUser sender = localUsers.get(messagePojo.senderUuid);

				final boolean trackedMessage = messagePojo.useTrackingId != null
						&& Objects.equals(messagePojo.contentType, ContentType.MESSAGE);
				final boolean trackedTransientMessage = messagePojo.useTrackingId != null
						&& Objects.equals(messagePojo.contentType, ContentType.TRANSIENT);

				// This piece of code is disabled and commented out on purpose to remind that
				// in some cases, like conveying a status report, the sender is virtually set to
				// represent some remote users, which naturally do not appear on local users
				// list
//				if (sender == null)
//					break;

				String[] receiverUuids = messagePojo.receiverUuid.split(";");

				SendStatus sendStatus = new SendStatus(
						Objects.equals(messagePojo.contentType, ContentType.MESSAGE) ? messagePojo.useTrackingId : null,
						messagePojo.senderUuid);

				List<String> localReceiverUuids = new ArrayList<String>();
				Map<String, Set<String>> remoteServerReceiverUuids = new HashMap<String, Set<String>>();
				List<String> unreachableUuids = new ArrayList<String>();

				for (String receiverUuid : receiverUuids) {

					if (localUsers.containsKey(receiverUuid)) {

						localReceiverUuids.add(receiverUuid);

						sendStatus.receiverUuids.add(receiverUuid);

					} else if (remoteUsers.containsKey(receiverUuid)) {

						RemoteUser remoteUser = remoteUsers.get(receiverUuid);

						remoteServerReceiverUuids.putIfAbsent(remoteUser.dmsServer.dmsUuid, new HashSet<String>());
						remoteServerReceiverUuids.get(remoteUser.dmsServer.dmsUuid).add(receiverUuid);

						sendStatus.receiverUuids.add(receiverUuid);

					} else {

						unreachableUuids.add(receiverUuid);

					}

				}

				if (!sendStatus.receiverUuids.isEmpty())
					sendStatuses.add(sendStatus);

				if (sendStatus.receiverUuids.isEmpty() && messagePojo.attachment != null) {
					try {
						Files.deleteIfExists(messagePojo.attachment);
					} catch (Exception e) {

					}
				}

				if (!localReceiverUuids.isEmpty()) {

					listener.sendToLocalUsers(messagePojo, sendStatus.status, (uuidList, progress) -> {

						clean: synchronized (sendStatus) {

							if (!(progress < 0 || progress == 100))
								break clean;

							sendStatus.receiverUuids.removeAll(localReceiverUuids);

							if (!sendStatus.receiverUuids.isEmpty())
								break clean;

							sendStatuses.remove(sendStatus);

							if (messagePojo.attachment == null)
								break clean;

							try {
								Files.deleteIfExists(messagePojo.attachment);
							} catch (Exception e) {

							}

						}

						if (trackedMessage) {

							MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(progress),
									String.join(";", uuidList), null, ContentType.PROGRESS_MESSAGE, null,
									messagePojo.useTrackingId, null, null);

							listener.sendToLocalUsers(progressMessagePojo, null, null, messagePojo.senderUuid);

						} else if (trackedTransientMessage && progress < 0) {

							MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(progress),
									String.join(";", uuidList), null, ContentType.PROGRESS_TRANSIENT, null,
									messagePojo.useTrackingId, null, null);

							listener.sendToLocalUsers(progressMessagePojo, null, null, messagePojo.senderUuid);

						}

					}, localReceiverUuids.toArray(new String[0]));

				}

				if (trackedTransientMessage && !unreachableUuids.isEmpty()) {

					MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(-1),
							String.join(";", unreachableUuids), null, ContentType.PROGRESS_TRANSIENT, null,
							messagePojo.useTrackingId, null, null);

					listener.sendToLocalUsers(progressMessagePojo, null, null, messagePojo.senderUuid);

				}

				remoteServerReceiverUuids.forEach((dmsUuid, uuidList) -> {

					String senderMapId = sender == null ? messagePojo.senderUuid : sender.mapId;
					List<String> receiverMapIdList = new ArrayList<String>();
					uuidList.forEach(uuid -> receiverMapIdList.add(remoteUsers.get(uuid).mapId));
					MessagePojo remoteMessagePojo = new MessagePojo(messagePojo.payload, senderMapId,
							String.join(";", receiverMapIdList), messagePojo.contentType, messagePojo.messageId,
							messagePojo.useTrackingId, messagePojo.useTimeout, messagePojo.useLocalAddress);
					remoteMessagePojo.attachment = messagePojo.attachment;

					listener.sendToRemoteServer(dmsUuid, remoteMessagePojo, sendStatus.status, progress -> {

						clean: synchronized (sendStatus) {

							if (!(progress < 0 || progress == 100))
								break clean;

							sendStatus.receiverUuids.removeAll(uuidList);

							if (!sendStatus.receiverUuids.isEmpty())
								break clean;

							sendStatuses.remove(sendStatus);

							if (messagePojo.attachment == null)
								break clean;

							try {
								Files.deleteIfExists(messagePojo.attachment);
							} catch (Exception e) {

							}

						}

						if (trackedMessage) {

							MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(progress),
									String.join(";", uuidList), null, ContentType.PROGRESS_MESSAGE, null,
									messagePojo.useTrackingId, null, null);

							listener.sendToLocalUsers(progressMessagePojo, null, null, messagePojo.senderUuid);

						} else if (trackedTransientMessage && progress < 0) {

							MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(progress),
									String.join(";", uuidList), null, ContentType.PROGRESS_TRANSIENT, null,
									messagePojo.useTrackingId, null, null);

							listener.sendToLocalUsers(progressMessagePojo, null, null, messagePojo.senderUuid);

						}

					});

				});

				break;

			}

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public void remoteMessageReceived(MessagePojo messagePojo, String dmsUuid) {

		try {

			byte[] payload = messagePojo.payload;

			List<String> receiverUuids = new ArrayList<String>();

			DmsServer remoteServer = remoteServers.get(dmsUuid);

			if (!(Objects.equals(messagePojo.contentType, ContentType.BCON) || remoteServer == null)) {

				User remoteUser = remoteServer.mappedUsers.get(messagePojo.senderUuid);
				String senderUuid = remoteUser == null ? messagePojo.senderUuid : remoteUser.beacon.uuid;
				if (messagePojo.receiverUuid != null) {
					for (String receiverMapId : messagePojo.receiverUuid.split(";")) {
						User localUser = mappedUsers.get(receiverMapId);
						receiverUuids.add(localUser == null ? receiverMapId : localUser.beacon.uuid);
					}
				}

				Path attachment = messagePojo.attachment;
				messagePojo = new MessagePojo(payload, senderUuid, null, messagePojo.contentType, messagePojo.messageId,
						null, null, null);
				messagePojo.attachment = attachment;

			}

			switch (messagePojo.contentType) {

			case BCON:

				Beacon beacon = DmsPackingFactory.unpack(payload, Beacon.class);

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

				if (messagePojo.receiverUuid == null || receiverUuids.isEmpty())
					break;

				final Path attachment = messagePojo.attachment;

				listener.sendToLocalUsers(messagePojo, null, (uuidList, progress) -> {

					if ((progress < 0 || progress == 100) && attachment != null) {
						try {
							Files.deleteIfExists(attachment);
						} catch (Exception e) {

						}
					}

				}, receiverUuids.stream().filter(receiverUuid -> localUsers.containsKey(receiverUuid))
						.toArray(String[]::new));

				break;

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public void testAllLocalUsers() {

		listener.sendToLocalUsers(TEST, null, null, localUsers.keySet().toArray(new String[0]));

	}

	public void serverConnectionsUpdated(String dmsUuid, List<InetAddress> remoteAddresses,
			List<InetAddress> localAddresses) {

		if (remoteAddresses.isEmpty()) {

			remoteServerDisconnected(dmsUuid);

			return;

		}

		remoteServers.putIfAbsent(dmsUuid, new DmsServer(dmsUuid));

		DmsServer dmsServer = remoteServers.get(dmsUuid);

		// This block is commented out upon a half-open connection error. See below.
//		if (dmsServer.addresses.isEmpty()) {
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

		sendStatuses.stream().filter(sendStatus -> Objects.equals(uuid, sendStatus.senderUuid))
				.forEach(sendStatus -> sendStatus.status.set(false));

		mappedUsers.remove(user.mapId);

		String userUuid = user.beacon.uuid;

		MessagePojo messagePojo = new MessagePojo(null, userUuid, null, ContentType.UUID_DISCONNECTED, null, null, null,
				null);

		localUsers.remove(userUuid).messageFactory.deleteResources();

		listener.sendToLocalUsers(messagePojo, null, null, localUsers.keySet().toArray(new String[0]));

		MessagePojo remoteMessagePojo = new MessagePojo(null, user.mapId, null, ContentType.UUID_DISCONNECTED, null,
				null, null, null);

		listener.sendToAllRemoteServers(remoteMessagePojo);

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

		MessagePojo messagePojo = new MessagePojo(null, userUuid, null, ContentType.UUID_DISCONNECTED, null, null, null,
				null);

		remoteUsers.remove(userUuid);

		listener.sendToLocalUsers(messagePojo, null, null, localUsers.keySet().toArray(new String[0]));

	}

	public Set<String> getRemoteIps() {

		return remoteIps;

	}

	private void sendBeaconToLocalUsers(Beacon beacon) {

		MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.pack(beacon), null, null, ContentType.BCON, null,
				null, null, null);

		listener.sendToLocalUsers(beaconPojo, null, null,
				localUsers.keySet().stream().filter(uuid -> !Objects.equals(beacon.uuid, uuid)).toArray(String[]::new));

	}

	private void sendAllBeaconsToLocalUser(final String receiverUuid) {

		localUsers.forEach((uuid, user) -> {

			if (user.beacon.status == null || Objects.equals(receiverUuid, uuid))
				return;

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.pack(user.beacon), null, null, ContentType.BCON,
					null, null, null, null);

			listener.sendToLocalUsers(beaconPojo, null, null, receiverUuid);

		});

		remoteUsers.forEach((uuid, user) -> {

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.pack(user.beacon), null, null, ContentType.BCON,
					null, null, null, null);

			listener.sendToLocalUsers(beaconPojo, null, null, receiverUuid);

		});

	}

	private void sendMessageToRemoteServers(MessagePojo messagePojo) {

		listener.sendToAllRemoteServers(messagePojo);

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

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packRemote(user.beacon), user.mapId, null,
					ContentType.BCON, null, null, null, null);

			listener.sendToRemoteServer(dmsUuid, beaconPojo, null, null);

		});

	}

	public void addRemoteIps(String... ips) {

		boolean changed = false;

		for (String ip : ips) {

			if (remoteIps.contains(ip))
				continue;

			changed = true;

			remoteIps.add(ip);

		}

		if (!changed)
			return;

		persistRemoteIps();

		sendRemoteIpsToAllLocalUsers();

	}

	private void removeRemoteIps(String... ips) {

		boolean changed = false;

		for (String ip : ips) {

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

			ois.writeObject(remoteIps.toArray(new String[remoteIps.size()]));

			ois.flush();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	private void sendRemoteIpsToLocalUser(String receiverUuid) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(remoteIps), null, null, ContentType.IPS, null,
				null, null, null);

		listener.sendToLocalUsers(messagePojo, null, null, receiverUuid);

	}

	private void sendRemoteIpsToAllLocalUsers() {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(remoteIps), null, null, ContentType.IPS, null,
				null, null, null);

		listener.sendToLocalUsers(messagePojo, null, null, localUsers.keySet().toArray(new String[0]));

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

		if (fromBeacon.secretId != null)
			toBeacon.secretId = fromBeacon.secretId;

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

		private final DmsMessageFactory messageFactory;

		private LocalUser(String userUuid, String mapId, Consumer<MessagePojo> messageConsumer) {

			super(userUuid, mapId);

			this.messageFactory = new DmsMessageFactory(messageConsumer);

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

	private class SendStatus {

		private final Long trackingId;
		private final String senderUuid;
		private final AtomicBoolean status = new AtomicBoolean(true);
		private final Set<String> receiverUuids = Collections.synchronizedSet(new HashSet<String>());

		private SendStatus(Long trackingId, String senderUuid) {

			this.trackingId = trackingId;
			this.senderUuid = senderUuid;

		}

	}

}

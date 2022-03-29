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
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.ogya.dms.commons.DmsMessageReceiver;
import com.ogya.dms.commons.DmsMessageReceiver.DmsMessageReceiverListener;
import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.AttachmentPojo;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.common.MessageContainerLocal;
import com.ogya.dms.server.common.MessageSorter;
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

	private final Set<InetAddress> remoteIps = Collections.synchronizedSet(new LinkedHashSet<InetAddress>());
	private final Set<InetAddress> remoteAddresses = new HashSet<InetAddress>();

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
			mappedUsers.put(localUser.mapId, localUser);
		}

		localUser.messageReceiver.inFeed(messageNumber, data);

	}

	private void localMessageReceived(final MessagePojo messagePojo) {

		try {

			switch (messagePojo.contentType) {

			case BCON: {

				Beacon beacon = DmsPackingFactory.unpack(messagePojo.payload, Beacon.class);

				String userUuid = beacon.uuid;

				if (userUuid == null)
					break;

				if (localUsers.values().stream().noneMatch(user -> user.beacon.status != null))
					listener.publishImmediately();

				User localUser = localUsers.get(userUuid);

				copyBeacon(beacon, localUser.beacon);

				sendBeaconToLocalUsers(localUser.beacon);

				sendMessageToRemoteServers(new MessagePojo(messagePojo.payload, localUser.mapId,
						messagePojo.receiverUuid, messagePojo.contentType, null, null, null));

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

			case CANCEL_MESSAGE: {

				sendStatuses.forEach(sendStatus -> {
					if (Objects.equals(sendStatus.trackingId, messagePojo.trackingId)
							&& sendStatus.contentType == ContentType.MESSAGE
							&& Objects.equals(sendStatus.senderUuid, messagePojo.senderUuid)) {
						sendStatus.status.set(false);
					}
				});

				break;

			}

			case CANCEL_TRANSIENT: {

				sendStatuses.forEach(sendStatus -> {
					if (Objects.equals(sendStatus.trackingId, messagePojo.trackingId)
							&& sendStatus.contentType == ContentType.TRANSIENT
							&& Objects.equals(sendStatus.senderUuid, messagePojo.senderUuid)) {
						sendStatus.status.set(false);
					}
				});

				break;

			}

			case CANCEL_UPLOAD: {

				sendStatuses.forEach(sendStatus -> {
					if (Objects.equals(sendStatus.trackingId, messagePojo.trackingId)
							&& sendStatus.contentType == ContentType.UPLOAD
							&& sendStatus.receiverUuids.contains(messagePojo.receiverUuid)) {
						sendStatus.status.set(false);
					}
				});

				break;

			}

			default: {

				if (messagePojo.receiverUuid == null)
					break;

				final LocalUser sender = localUsers.get(messagePojo.senderUuid);
				final Path attachment = messagePojo.getAttachmentLink();

				// This piece of code is disabled and commented out on purpose to remind that
				// in some cases, like conveying a status report, the sender is virtually set to
				// represent some remote users, which naturally do not appear on local users
				// list
//				if (sender == null)
//					break;

				String[] receiverUuids = messagePojo.receiverUuid.split(";");

				LocalUser localUser;
				RemoteUser remoteUser;

				List<LocalUser> localReceivers = new ArrayList<LocalUser>();
				Map<String, Set<String>> remoteServerReceiverUuids = new HashMap<String, Set<String>>();
				List<String> unsuccessfulUuids = new ArrayList<String>();

				for (String receiverUuid : receiverUuids) {
					if ((localUser = localUsers.get(receiverUuid)) != null) {
						localReceivers.add(localUser);
					} else if ((remoteUser = remoteUsers.get(receiverUuid)) != null) {
						remoteServerReceiverUuids.putIfAbsent(remoteUser.dmsServer.dmsUuid, new HashSet<String>());
						remoteServerReceiverUuids.get(remoteUser.dmsServer.dmsUuid).add(receiverUuid);
					} else {
						unsuccessfulUuids.add(receiverUuid);
					}
				}

				final AtomicInteger partySize = new AtomicInteger(
						localReceivers.size() + remoteServerReceiverUuids.size());

				if (partySize.get() == 0) {
					deleteFile(attachment);
				}

				if (messagePojo.contentType == ContentType.TRANSIENT && !unsuccessfulUuids.isEmpty()
						&& sender != null) {
					MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(-1),
							String.join(";", unsuccessfulUuids), null, ContentType.PROGRESS_TRANSIENT,
							messagePojo.trackingId, null, null);
					sender.sendMessage(progressMessagePojo, null, null);
				}

				localReceivers.forEach(user -> {
					final String uuid = user.beacon.uuid;
					final SendStatus sendStatus = new SendStatus(messagePojo.trackingId, messagePojo.contentType,
							messagePojo.senderUuid);
					sendStatus.receiverUuids.add(uuid);
					sendStatuses.add(sendStatus);
					MessagePojo localMessagePojo = new MessagePojo(messagePojo.payload, messagePojo.senderUuid, null,
							messagePojo.contentType, messagePojo.trackingId, messagePojo.useTimeout,
							messagePojo.useLocalAddress);
					localMessagePojo.attachment = new AttachmentPojo(attachment);
					user.sendMessage(localMessagePojo, sendStatus.status, progress -> {
						if (progress < 0 || progress == 100) {
							sendStatuses.remove(sendStatus);
							if (partySize.decrementAndGet() == 0) {
								deleteFile(attachment);
							}
						}
						sendProgress(uuid, progress, sender, messagePojo.trackingId, messagePojo.contentType);
					});
				});

				remoteServerReceiverUuids.forEach((dmsUuid, uuidList) -> {
					final SendStatus sendStatus = new SendStatus(messagePojo.trackingId, messagePojo.contentType,
							messagePojo.senderUuid);
					sendStatus.receiverUuids.addAll(uuidList);
					sendStatuses.add(sendStatus);
					String senderMapId = sender == null ? messagePojo.senderUuid : sender.mapId;
					List<String> receiverMapIdList = new ArrayList<String>();
					uuidList.forEach(uuid -> receiverMapIdList.add(remoteUsers.get(uuid).mapId));
					MessagePojo remoteMessagePojo = new MessagePojo(messagePojo.payload, senderMapId,
							String.join(";", receiverMapIdList), messagePojo.contentType, messagePojo.trackingId,
							messagePojo.useTimeout, messagePojo.useLocalAddress);
					remoteMessagePojo.attachment = new AttachmentPojo(attachment);
					listener.sendToRemoteServer(dmsUuid, remoteMessagePojo, sendStatus.status, progress -> {
						if (progress < 0 || progress == 100) {
							sendStatuses.remove(sendStatus);
							if (partySize.decrementAndGet() == 0) {
								deleteFile(attachment);
							}
						}
						sendProgress(String.join(";", uuidList), progress, sender, messagePojo.trackingId,
								messagePojo.contentType);
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

			if (!(messagePojo.contentType == ContentType.BCON || remoteServer == null)) {
				User remoteUser = remoteServer.mappedUsers.get(messagePojo.senderUuid);
				String senderUuid = remoteUser == null ? messagePojo.senderUuid : remoteUser.beacon.uuid;
				messagePojo.senderUuid = senderUuid;
				if (messagePojo.receiverUuid != null) {
					for (String receiverMapId : messagePojo.receiverUuid.split(";")) {
						User localUser = mappedUsers.get(receiverMapId);
						receiverUuids.add(localUser == null ? receiverMapId : localUser.beacon.uuid);
					}
				}
			}

			switch (messagePojo.contentType) {

			case BCON: {

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

			}

			case UUID_DISCONNECTED: {

				String uuid = messagePojo.senderUuid;

				User remoteUser = remoteUsers.get(uuid);

				DmsServer dmsServer = remoteServers.get(dmsUuid);

				if (dmsServer != null)
					dmsServer.mappedUsers.remove(remoteUser.mapId);

				remoteUserDisconnected(remoteUser);

				break;

			}

			default: {

				final Path attachment = messagePojo.getAttachmentLink();
				List<LocalUser> localReceivers = new ArrayList<LocalUser>();

				for (String receiverUuid : receiverUuids) {
					LocalUser localUser = localUsers.get(receiverUuid);
					if (localUser == null) {
						continue;
					}
					localReceivers.add(localUser);
				}

				final AtomicInteger partySize = new AtomicInteger(localReceivers.size());

				if (partySize.get() == 0) {
					deleteFile(attachment);
				}

				localReceivers.forEach(user -> {
					MessagePojo localMessagePojo = new MessagePojo(messagePojo.payload, messagePojo.senderUuid, null,
							messagePojo.contentType, messagePojo.trackingId, null, null);
					localMessagePojo.attachment = new AttachmentPojo(attachment);
					user.sendMessage(localMessagePojo, null, progress -> {
						if (progress < 0 || progress == 100) {
							if (partySize.decrementAndGet() == 0) {
								deleteFile(attachment);
							}
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

	public void testAllLocalUsers() {

		localUsers.forEach((localUuid, localUser) -> localUser.sendMessage(TEST, null, null));

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

		dmsServer.mappedUsers.forEach((mapId, user) -> sendBeaconToLocalUsers(user.beacon));

	}

	public void localUuidDisconnected(String uuid) {

		LocalUser user = localUsers.get(uuid);

		if (user == null)
			return;

		sendStatuses.forEach(sendStatus -> {
			if (Objects.equals(uuid, sendStatus.senderUuid)
					|| (sendStatus.receiverUuids.remove(uuid) && sendStatus.receiverUuids.isEmpty())) {
				sendStatus.status.set(false);
			}
		});

		mappedUsers.remove(user.mapId);

		MessagePojo messagePojo = new MessagePojo(null, uuid, null, ContentType.UUID_DISCONNECTED, null, null, null);

		localUsers.remove(uuid).close();

		localUsers.forEach((localUuid, localUser) -> localUser.sendMessage(messagePojo, null, null));

		MessagePojo remoteMessagePojo = new MessagePojo(null, user.mapId, null, ContentType.UUID_DISCONNECTED, null,
				null, null);

		listener.sendToAllRemoteServers(remoteMessagePojo);

	}

	private void sendProgress(String senderUuid, Integer progress, LocalUser receiverUser, Long trackingId,
			ContentType contentType) {

		if (receiverUser == null) {
			return;
		}

		switch (contentType) {
		case MESSAGE: {
			MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(progress), senderUuid, null,
					ContentType.PROGRESS_MESSAGE, trackingId, null, null);
			receiverUser.sendMessage(progressMessagePojo, null, null);
			break;
		}
		case TRANSIENT: {
			if (progress < 0) {
				MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(progress), senderUuid, null,
						ContentType.PROGRESS_TRANSIENT, trackingId, null, null);
				receiverUser.sendMessage(progressMessagePojo, null, null);
			}
			break;
		}
		default: {
			break;
		}
		}

	}

	private void remoteServerDisconnected(String dmsUuid) {

		DmsServer dmsServer = remoteServers.remove(dmsUuid);

		if (dmsServer == null)
			return;

		dmsServer.mappedUsers.forEach((mapId, user) -> remoteUserDisconnected(user));

	}

	private void remoteUserDisconnected(User user) {

		if (user == null)
			return;

		String userUuid = user.beacon.uuid;

		sendStatuses.forEach(sendStatus -> {
			if (sendStatus.receiverUuids.remove(userUuid) && sendStatus.receiverUuids.isEmpty()) {
				sendStatus.status.set(false);
			}
		});

		MessagePojo messagePojo = new MessagePojo(null, userUuid, null, ContentType.UUID_DISCONNECTED, null, null,
				null);

		remoteUsers.remove(userUuid);

		localUsers.forEach((localUuid, localUser) -> localUser.sendMessage(messagePojo, null, null));

	}

	public Set<InetAddress> getRemoteAddresses() {

		remoteAddresses.clear();
		remoteIps.forEach(remoteIp -> remoteAddresses.add(remoteIp));
		return remoteAddresses;

	}

	private void sendBeaconToLocalUsers(Beacon beacon) {

		MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(beacon), null, null,
				ContentType.BCON, null, null, null);

		localUsers.forEach((localUuid, localUser) -> {
			if (Objects.equals(beacon.uuid, localUuid)) {
				return;
			}
			localUser.sendMessage(beaconPojo, null, null);
		});

	}

	private void sendAllBeaconsToLocalUser(final String receiverUuid) {

		LocalUser localUser = localUsers.get(receiverUuid);
		if (localUser == null) {
			return;
		}

		localUsers.forEach((uuid, user) -> {

			if (user.beacon.status == null || Objects.equals(receiverUuid, uuid))
				return;

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(user.beacon), null, null,
					ContentType.BCON, null, null, null);

			localUser.sendMessage(beaconPojo, null, null);

		});

		remoteUsers.forEach((uuid, user) -> {

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(user.beacon), null, null,
					ContentType.BCON, null, null, null);

			localUser.sendMessage(beaconPojo, null, null);

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

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToServer(user.beacon), user.mapId,
					null, ContentType.BCON, null, null, null);

			listener.sendToRemoteServer(dmsUuid, beaconPojo, null, null);

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

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(remoteIps), null, null, ContentType.IPS, null,
				null, null);

		localUser.sendMessage(messagePojo, null, null);

	}

	private void sendRemoteIpsToAllLocalUsers() {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(remoteIps), null, null, ContentType.IPS, null,
				null, null);

		localUsers.forEach((localUuid, localUser) -> localUser.sendMessage(messagePojo, null, null));

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

	private void deleteFile(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (Exception e) {
			path.toFile().deleteOnExit();
		}
	}

	public MessageContainerLocal getNextMessage(String uuid) {
		LocalUser user = localUsers.get(uuid);
		if (user == null) {
			return null;
		}
		return user.getNextMessage();
	}

	public void queueMessage(String uuid, MessageContainerLocal messageContainer) {
		LocalUser user = localUsers.get(uuid);
		if (user == null) {
			return;
		}
		user.queueMessage(messageContainer);
	}

	public void closeMessage(String uuid, MessageContainerLocal messageContainer) {
		LocalUser user = localUsers.get(uuid);
		if (user == null) {
			return;
		}
		user.closeMessage(messageContainer);
	}

	private abstract class User {

		protected final Beacon beacon;
		protected final String mapId;

		private User(String userUuid, String mapId) {

			this.beacon = new Beacon(userUuid);

			this.mapId = mapId;

		}

	}

	private class LocalUser extends User implements DmsMessageReceiverListener {

		private final DmsMessageReceiver messageReceiver;
		private final AtomicInteger messageCounter = new AtomicInteger(0);
		private final PriorityBlockingQueue<MessageContainerLocal> messageQueue = new PriorityBlockingQueue<MessageContainerLocal>(
				11, new MessageSorter());
		private final Map<Integer, MessageContainerLocal> stopMap = Collections
				.synchronizedMap(new HashMap<Integer, MessageContainerLocal>());

		private LocalUser(String userUuid, String mapId) {

			super(userUuid, mapId);

			this.messageReceiver = new DmsMessageReceiver(this);

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

		private void sendMessage(MessagePojo messagePojo, AtomicBoolean sendStatus,
				Consumer<Integer> progressConsumer) {
			MessageContainerLocal messageContainer = new MessageContainerLocal(messageCounter.getAndIncrement(),
					messagePojo, sendStatus, progressConsumer);
			stopMap.put(messageContainer.messageNumber, messageContainer);
			queueMessage(messageContainer);
			listener.localMessageReady(beacon.uuid);
		}

		private void queueMessage(MessageContainerLocal messageContainer) {
			messageQueue.put(messageContainer);
		}

		private void closeMessage(MessageContainerLocal messageContainer) {
			messageContainer.close();
			stopMap.remove(messageContainer.messageNumber);
			if (messageContainer.progressConsumer != null && messageContainer.progressPercent.get() < 100) {
				messageContainer.progressConsumer.accept(-1);
			}
		}

		private MessageContainerLocal getNextMessage() {
			return messageQueue.poll();
		}

		private void stopSending(Integer messageNumber) {
			MessageContainerLocal messageContainer = stopMap.get(messageNumber);
			if (messageContainer == null) {
				return;
			}
			messageContainer.markAsDone();
		}

		private void close() {
			messageReceiver.deleteResources();
			MessageContainerLocal messageContainer;
			while ((messageContainer = messageQueue.poll()) != null) {
				closeMessage(messageContainer);
			}
		}

		@Override
		public void messageReceived(MessagePojo messagePojo) {
			if (messagePojo.contentType == ContentType.SEND_NOMORE) {
				try {
					Integer messageNumber = DmsPackingFactory.unpack(messagePojo.payload, Integer.class);
					stopSending(messageNumber);
				} catch (Exception e) {

				}
				return;
			}
			localMessageReceived(messagePojo);
		}

		@Override
		public void messageFailed(int messageNumber) {
			sendMessage(new MessagePojo(DmsPackingFactory.pack(messageNumber), null, null, ContentType.SEND_NOMORE,
					null, null, null), null, null);
		}

	}

	private class RemoteUser extends User {

		private final DmsServer dmsServer;

		private RemoteUser(String userUuid, String mapId, DmsServer dmsServer) {

			super(userUuid, mapId);

			this.dmsServer = dmsServer;

			this.beacon.localRemoteServerIps = dmsServer.localRemoteIps;

		}

	}

	private class DmsServer {

		private final String dmsUuid;
		private final Map<String, User> mappedUsers = Collections.synchronizedMap(new HashMap<String, User>());
		private final Map<InetAddress, InetAddress> localRemoteIps = Collections
				.synchronizedMap(new HashMap<InetAddress, InetAddress>());

		private DmsServer(String dmsUuid) {

			this.dmsUuid = dmsUuid;

		}

	}

	private class SendStatus {

		private final Long trackingId;
		private final ContentType contentType;
		private final String senderUuid;
		private final AtomicBoolean status = new AtomicBoolean(true);
		private final Set<String> receiverUuids = Collections.synchronizedSet(new HashSet<String>());

		private SendStatus(Long trackingId, ContentType contentType, String senderUuid) {

			this.trackingId = trackingId;
			this.contentType = contentType;
			this.senderUuid = senderUuid;

		}

	}

}

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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.ogya.dms.commons.DmsMessageReceiver;
import com.ogya.dms.commons.DmsMessageReceiver.DmsMessageReceiverListener;
import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.AttachmentPojo;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.model.intf.ModelListener;

public class Model {

	private static final MessagePojo TEST = new MessagePojo();
	private static final AtomicInteger MAP_ID = new AtomicInteger(0);

	private final Path ioTempDir = Paths.get(CommonConstants.IO_TMP_DIR);
	private final Path sharedTempDir = Paths.get(CommonConstants.SHARED_TMP_DIR);

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

	public void localMessageReceived(byte[] data, String userUuid) {

		LocalUser localUser = localUsers.get(userUuid);
		if (localUser == null) {
			localUser = new LocalUser(userUuid, String.valueOf(MAP_ID.getAndIncrement()));
			localUsers.put(userUuid, localUser);
			mappedUsers.put(localUser.mapId, localUser);
		}

		localUser.messageReceiver.inFeed(0, data);

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

				List<String> localReceiverUuids = new ArrayList<String>();
				Map<String, Set<String>> remoteServerReceiverUuids = new HashMap<String, Set<String>>();
				List<String> unsuccessfulUuids = new ArrayList<String>();

				for (String receiverUuid : receiverUuids) {

					if ((localUser = localUsers.get(receiverUuid)) != null) {

						if (Boolean.TRUE.equals(localUser.beacon.local) && attachment != null) {

							try {
								Files.createDirectories(sharedTempDir);
								Path copyOfAttachment = Files.copy(attachment,
										Files.createTempFile(sharedTempDir, "dms", null),
										StandardCopyOption.REPLACE_EXISTING);
								final SendStatus sendStatus = new SendStatus(messagePojo.trackingId,
										messagePojo.contentType, messagePojo.senderUuid);
								sendStatus.receiverUuids.add(receiverUuid);
								sendStatuses.add(sendStatus);
								final MessagePojo localMessagePojo = new MessagePojo(messagePojo.payload,
										messagePojo.senderUuid, null, messagePojo.contentType, messagePojo.trackingId,
										messagePojo.useTimeout, messagePojo.useLocalAddress);
								localMessagePojo.attachment = new AttachmentPojo(copyOfAttachment, true);
								listener.sendToLocalUsers(localMessagePojo, sendStatus.status, (uuidList, progress) -> {

									if (progress < 0) {
										sendStatuses.remove(sendStatus);
										deleteFileInFolder(copyOfAttachment, sharedTempDir);
									} else if (progress == 100) {
										sendStatuses.remove(sendStatus);
									}

									sendProgress(uuidList, progress, messagePojo.senderUuid, messagePojo.trackingId,
											messagePojo.contentType);

								}, receiverUuid);
							} catch (Exception e) {
								unsuccessfulUuids.add(receiverUuid);
							}

							continue;

						}

						localReceiverUuids.add(receiverUuid);

					} else if ((remoteUser = remoteUsers.get(receiverUuid)) != null) {

						remoteServerReceiverUuids.putIfAbsent(remoteUser.dmsServer.dmsUuid, new HashSet<String>());
						remoteServerReceiverUuids.get(remoteUser.dmsServer.dmsUuid).add(receiverUuid);

					} else {

						unsuccessfulUuids.add(receiverUuid);

					}

				}

				if (localReceiverUuids.isEmpty() && remoteServerReceiverUuids.isEmpty()) {
					deleteFileInFolder(attachment, ioTempDir);
				}

				final AtomicInteger partySize = new AtomicInteger(remoteServerReceiverUuids.size());

				if (!localReceiverUuids.isEmpty()) {

					partySize.incrementAndGet();

					final SendStatus sendStatus = new SendStatus(messagePojo.trackingId, messagePojo.contentType,
							messagePojo.senderUuid);
					sendStatus.receiverUuids.addAll(localReceiverUuids);
					sendStatuses.add(sendStatus);

					final MessagePojo localMessagePojo = new MessagePojo(messagePojo.payload, messagePojo.senderUuid,
							null, messagePojo.contentType, messagePojo.trackingId, messagePojo.useTimeout,
							messagePojo.useLocalAddress);
					localMessagePojo.attachment = new AttachmentPojo(attachment, false);

					listener.sendToLocalUsers(localMessagePojo, sendStatus.status, (uuidList, progress) -> {

						if (progress < 0 || progress == 100) {
							sendStatuses.remove(sendStatus);
							if (partySize.decrementAndGet() == 0) {
								deleteFileInFolder(attachment, ioTempDir);
							}
						}

						sendProgress(uuidList, progress, messagePojo.senderUuid, messagePojo.trackingId,
								messagePojo.contentType);

					}, localReceiverUuids.toArray(new String[0]));

				}

				if (messagePojo.contentType == ContentType.TRANSIENT && !unsuccessfulUuids.isEmpty()) {

					MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(-1),
							String.join(";", unsuccessfulUuids), null, ContentType.PROGRESS_TRANSIENT,
							messagePojo.trackingId, null, null);

					listener.sendToLocalUsers(progressMessagePojo, null, null, messagePojo.senderUuid);

				}

				remoteServerReceiverUuids.forEach((dmsUuid, uuidList) -> {

					final SendStatus sendStatus = new SendStatus(messagePojo.trackingId, messagePojo.contentType,
							messagePojo.senderUuid);
					sendStatus.receiverUuids.addAll(uuidList);
					sendStatuses.add(sendStatus);

					String senderMapId = sender == null ? messagePojo.senderUuid : sender.mapId;
					List<String> receiverMapIdList = new ArrayList<String>();
					uuidList.forEach(uuid -> receiverMapIdList.add(remoteUsers.get(uuid).mapId));
					final MessagePojo remoteMessagePojo = new MessagePojo(messagePojo.payload, senderMapId,
							String.join(";", receiverMapIdList), messagePojo.contentType, messagePojo.trackingId,
							messagePojo.useTimeout, messagePojo.useLocalAddress);
					remoteMessagePojo.attachment = new AttachmentPojo(attachment, false);

					listener.sendToRemoteServer(dmsUuid, remoteMessagePojo, sendStatus.status, progress -> {

						if (progress < 0 || progress == 100) {
							sendStatuses.remove(sendStatus);
							if (partySize.decrementAndGet() == 0) {
								deleteFileInFolder(attachment, ioTempDir);
							}
						}

						sendProgress(uuidList, progress, messagePojo.senderUuid, messagePojo.trackingId,
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

				final Path attachment = messagePojo.getAttachmentLink();
				List<String> localReceiverUuids = new ArrayList<String>();

				for (String receiverUuid : receiverUuids) {
					LocalUser localUser = localUsers.get(receiverUuid);
					if (localUser == null)
						continue;
					if (Boolean.TRUE.equals(localUser.beacon.local) && attachment != null) {
						try {
							Files.createDirectories(sharedTempDir);
							Path copyOfAttachment = Files.copy(attachment,
									Files.createTempFile(sharedTempDir, "dms", null),
									StandardCopyOption.REPLACE_EXISTING);
							MessagePojo localMessagePojo = new MessagePojo(messagePojo.payload, messagePojo.senderUuid,
									null, messagePojo.contentType, messagePojo.trackingId, null, null);
							localMessagePojo.attachment = new AttachmentPojo(copyOfAttachment, true);
							listener.sendToLocalUsers(localMessagePojo, null, (uuidList, progress) -> {

								if (progress < 0) {
									deleteFileInFolder(copyOfAttachment, sharedTempDir);
								}

							}, receiverUuid);
						} catch (Exception e) {

						}
						continue;
					}
					localReceiverUuids.add(receiverUuid);
				}

				if (localReceiverUuids.isEmpty()) {
					deleteFileInFolder(attachment, ioTempDir);
					break;
				}

				MessagePojo localMessagePojo = new MessagePojo(messagePojo.payload, messagePojo.senderUuid, null,
						messagePojo.contentType, messagePojo.trackingId, null, null);
				localMessagePojo.attachment = new AttachmentPojo(attachment, false);

				listener.sendToLocalUsers(localMessagePojo, null, (uuidList, progress) -> {

					if ((progress < 0 || progress == 100)) {
						deleteFileInFolder(attachment, ioTempDir);
					}

				}, localReceiverUuids.toArray(new String[0]));

				break;

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public void testAllLocalUsers() {

		listener.sendToLocalUsers(TEST, null, null, localUsers.keySet().toArray(new String[0]));

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

		localUsers.remove(uuid).messageReceiver.deleteResources();

		listener.sendToLocalUsers(messagePojo, null, null, localUsers.keySet().toArray(new String[0]));

		MessagePojo remoteMessagePojo = new MessagePojo(null, user.mapId, null, ContentType.UUID_DISCONNECTED, null,
				null, null);

		listener.sendToAllRemoteServers(remoteMessagePojo);

	}

	private void sendProgress(Collection<String> uuidList, Integer progress, String senderUuid, Long trackingId,
			ContentType contentType) {

		switch (contentType) {
		case MESSAGE: {
			MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(progress),
					String.join(";", uuidList), null, ContentType.PROGRESS_MESSAGE, trackingId, null, null);
			listener.sendToLocalUsers(progressMessagePojo, null, null, senderUuid);
			break;
		}
		case TRANSIENT: {
			if (progress < 0) {
				MessagePojo progressMessagePojo = new MessagePojo(DmsPackingFactory.pack(progress),
						String.join(";", uuidList), null, ContentType.PROGRESS_TRANSIENT, trackingId, null, null);
				listener.sendToLocalUsers(progressMessagePojo, null, null, senderUuid);
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

		listener.sendToLocalUsers(messagePojo, null, null, localUsers.keySet().toArray(new String[0]));

	}

	public Set<InetAddress> getRemoteAddresses() {

		remoteAddresses.clear();
		remoteIps.forEach(remoteIp -> remoteAddresses.add(remoteIp));
		return remoteAddresses;

	}

	private void sendBeaconToLocalUsers(Beacon beacon) {

		MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(beacon), null, null,
				ContentType.BCON, null, null, null);

		listener.sendToLocalUsers(beaconPojo, null, null,
				localUsers.keySet().stream().filter(uuid -> !Objects.equals(beacon.uuid, uuid)).toArray(String[]::new));

	}

	private void sendAllBeaconsToLocalUser(final String receiverUuid) {

		localUsers.forEach((uuid, user) -> {

			if (user.beacon.status == null || Objects.equals(receiverUuid, uuid))
				return;

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(user.beacon), null, null,
					ContentType.BCON, null, null, null);

			listener.sendToLocalUsers(beaconPojo, null, null, receiverUuid);

		});

		remoteUsers.forEach((uuid, user) -> {

			MessagePojo beaconPojo = new MessagePojo(DmsPackingFactory.packServerToClient(user.beacon), null, null,
					ContentType.BCON, null, null, null);

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

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(remoteIps), null, null, ContentType.IPS, null,
				null, null);

		listener.sendToLocalUsers(messagePojo, null, null, receiverUuid);

	}

	private void sendRemoteIpsToAllLocalUsers() {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(remoteIps), null, null, ContentType.IPS, null,
				null, null);

		listener.sendToLocalUsers(messagePojo, null, null, localUsers.keySet().toArray(new String[0]));

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

		if (fromBeacon.local != null)
			toBeacon.local = fromBeacon.local;

	}

	private void deleteFileInFolder(Path path, Path folder) {
		if (path == null || !path.getParent().equals(folder))
			return;
		try {
			Files.deleteIfExists(path);
		} catch (Exception e) {
			path.toFile().deleteOnExit();
		}
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

		@Override
		public void messageReceived(MessagePojo messagePojo) {
			localMessageReceived(messagePojo);
		}

		@Override
		public void messageFailed() {

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

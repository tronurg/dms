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

	private final Map<String, String> localUserBeacon = Collections.synchronizedMap(new HashMap<String, String>());
	private final Map<String, String> remoteUserBeacon = Collections.synchronizedMap(new HashMap<String, String>());

	private final Map<String, Map<Long, AtomicBoolean>> senderStatusMap = Collections
			.synchronizedMap(new HashMap<String, Map<Long, AtomicBoolean>>());

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

		} catch (IOException e) {

			e.printStackTrace();

		} catch (ClassNotFoundException e) {

			e.printStackTrace();

		}

	}

	public void localMessageReceived(final String messagePojoStr) {

		try {

			MessagePojo messagePojo = gson.fromJson(messagePojoStr, MessagePojo.class);

			String senderUuid = messagePojo.senderUuid;
			Long messageId = messagePojo.messageId;

			switch (messagePojo.contentType) {

			case BCON:

				if (!messagePojoStr.equals(localUserBeacon.get(senderUuid))) {

					boolean isNew = !localUserBeacon.containsKey(senderUuid);

					// Yerel uuid yeni eklendi veya guncellendi.
					// Beacon, yerel beacon'lara eklenecek.
					// Yeni beacon tum yerel ve uzak kullanicilara dagitilacak.

					localUserBeacon.put(senderUuid, messagePojoStr);
					localUserBeacon.forEach((receiverUuid, message) -> {

						if (receiverUuid.equals(senderUuid))
							return;

						listener.sendToLocalUser(receiverUuid, messagePojoStr);

					});
					listener.sendToAllRemoteUsers(messagePojoStr);

					if (isNew) {

						sendAllBeaconsToLocalUser(senderUuid);

						sendRemoteIpsToLocalUser(senderUuid);

					}

				}

				// Gonderen uuid agda yayinlanacak
				listener.publishUuid(senderUuid);

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

			case CANCEL:

				if (messageId == null)
					break;

				synchronized (senderStatusMap) {
					if (senderStatusMap.containsKey(senderUuid)
							&& senderStatusMap.get(senderUuid).containsKey(messageId))
						senderStatusMap.get(senderUuid).get(messageId).set(false);
				}

				break;

			default:

				if (messagePojo.receiverUuid == null)
					break;

				String[] receiverUuids = messagePojo.receiverUuid.split(";");

				List<String> localReceiverUuids = new ArrayList<String>();
				List<String> remoteReceiverUuids = new ArrayList<String>();

				for (String receiverUuid : receiverUuids) {

					if (localUserBeacon.containsKey(receiverUuid)) {

						localReceiverUuids.add(receiverUuid);
						listener.sendToLocalUser(receiverUuid, messagePojoStr);

					} else if (remoteUserBeacon.containsKey(receiverUuid)) {

						remoteReceiverUuids.add(receiverUuid);

					}

				}

				if (messagePojo.contentType.equals(ContentType.MESSAGE) && localReceiverUuids.size() > 0
						&& messageId != null) {

					MessagePojo progressMessagePojo = new MessagePojo(String.valueOf(100),
							String.join(";", localReceiverUuids), senderUuid, ContentType.PROGRESS, messageId);

					if (localUserBeacon.containsKey(senderUuid))
						listener.sendToLocalUser(senderUuid, gson.toJson(progressMessagePojo));

				}

				if (remoteReceiverUuids.size() == 1) {

					final String remoteReceiverUuid = remoteReceiverUuids.get(0);

					AtomicBoolean sendStatus = new AtomicBoolean(true);

					synchronized (senderStatusMap) {
						senderStatusMap.putIfAbsent(senderUuid,
								Collections.synchronizedMap(new HashMap<Long, AtomicBoolean>()));
						senderStatusMap.get(senderUuid).put(messageId, sendStatus);
					}

					listener.sendToRemoteUser(remoteReceiverUuid, messagePojoStr, sendStatus,
							!messagePojo.contentType.equals(ContentType.MESSAGE) || messageId == null ? null
									: progress -> {

										synchronized (senderStatusMap) {
											if ((progress < 0 || progress == 100)
													&& senderStatusMap.containsKey(senderUuid)) {
												senderStatusMap.get(senderUuid).remove(messageId);
												if (senderStatusMap.get(senderUuid).isEmpty())
													senderStatusMap.remove(senderUuid);
											}
										}

										MessagePojo progressMessagePojo = new MessagePojo(String.valueOf(progress),
												remoteReceiverUuid, senderUuid, ContentType.PROGRESS, messageId);

										if (localUserBeacon.containsKey(senderUuid))
											listener.sendToLocalUser(senderUuid, gson.toJson(progressMessagePojo));

									});

				} else if (remoteReceiverUuids.size() > 0) {

					AtomicBoolean sendStatus = new AtomicBoolean(true);

					synchronized (senderStatusMap) {
						senderStatusMap.putIfAbsent(senderUuid,
								Collections.synchronizedMap(new HashMap<Long, AtomicBoolean>()));
						senderStatusMap.get(senderUuid).put(messageId, sendStatus);
					}

					listener.sendToRemoteUsers(remoteReceiverUuids, messagePojoStr, sendStatus,
							!messagePojo.contentType.equals(ContentType.MESSAGE) || messageId == null ? null
									: (uuidList, progress) -> {

										synchronized (senderStatusMap) {
											if ((progress < 0 || progress == 100)
													&& senderStatusMap.containsKey(senderUuid)) {
												senderStatusMap.get(senderUuid).remove(messageId);
												if (senderStatusMap.get(senderUuid).isEmpty())
													senderStatusMap.remove(senderUuid);
											}
										}

										MessagePojo progressMessagePojo = new MessagePojo(String.valueOf(progress),
												String.join(";", uuidList), senderUuid, ContentType.PROGRESS,
												messageId);

										if (localUserBeacon.containsKey(senderUuid))
											listener.sendToLocalUser(senderUuid, gson.toJson(progressMessagePojo));

									});

				}

				break;

			}

		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}

	}

	public void remoteMessageReceived(String messagePojoStr) {

		try {

			MessagePojo messagePojo = gson.fromJson(messagePojoStr, MessagePojo.class);

			String senderUuid = messagePojo.senderUuid;

			switch (messagePojo.contentType) {

			case BCON:

				if (!messagePojoStr.equals(remoteUserBeacon.get(senderUuid))) {

					// Uzak uuid yeni eklendi veya guncellendi.
					// Beacon, uzak beacon'larda guncellenecek.
					// Yeni beacon tum yerel kullanicilara dagitilacak.

					remoteUserBeacon.put(senderUuid, messagePojoStr);
					localUserBeacon
							.forEach((receiverUuid, message) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

				}

				break;

			case UUID_DISCONNECTED:

				remoteUserDisconnected(messagePojo.message);

				break;

			default:

				if (messagePojo.receiverUuid == null)
					break;

				String[] receiverUuids = messagePojo.receiverUuid.split(";");

				for (String receiverUuid : receiverUuids) {

					if (localUserBeacon.containsKey(receiverUuid))
						listener.sendToLocalUser(receiverUuid, messagePojoStr);

				}

				break;

			}

		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}

	}

	public void processAllLocalBeacons(Consumer<String> consumer) {

		localUserBeacon.forEach((receiverUuid, message) -> consumer.accept(message));

	}

	public void testAllLocalUsers() {

		localUserBeacon.forEach((receiverUuid, message) -> listener.sendToLocalUser(receiverUuid, ""));

	}

	public void remoteUserDisconnected(String uuid) {

		if (!remoteUserBeacon.containsKey(uuid))
			return;

		String messagePojoStr = gson.toJson(new MessagePojo(uuid, "", ContentType.UUID_DISCONNECTED, null));

		remoteUserBeacon.remove(uuid);

		localUserBeacon.forEach((receiverUuid, message) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

	}

	public void localUserDisconnected(String uuid) {

		if (!localUserBeacon.containsKey(uuid))
			return;

		String messagePojoStr = gson.toJson(new MessagePojo(uuid, "", ContentType.UUID_DISCONNECTED, null));

		localUserBeacon.remove(uuid);

		localUserBeacon.forEach((receiverUuid, message) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

		listener.sendToAllRemoteUsers(messagePojoStr);

	}

	public Set<String> getRemoteIps() {

		return remoteIps;

	}

	public void addUnicastIp(String ip) {

		if (remoteIps.contains(ip))
			return;

		addRemoteIp(ip);

	}

	private void sendAllBeaconsToLocalUser(String receiverUuid) {

		localUserBeacon.forEach((uuid, beacon) -> {

			if (receiverUuid.equals(uuid))
				return;

			listener.sendToLocalUser(receiverUuid, beacon);

		});

		remoteUserBeacon.forEach((uuid, beacon) -> {

			listener.sendToLocalUser(receiverUuid, beacon);

		});

	}

	private void addRemoteIp(String ip) {

		remoteIps.add(ip);

		persistRemoteIps();

		sendRemoteIpsToAllLocalUsers();

	}

	private void removeRemoteIp(String ip) {

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

		localUserBeacon.forEach((receiverUuid, message) -> {

			listener.sendToLocalUser(receiverUuid, messagePojoStr);

		});

	}

}

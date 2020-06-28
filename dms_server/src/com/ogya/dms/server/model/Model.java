package com.ogya.dms.server.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	public Model(ModelListener listener) {

		this.listener = listener;

	}

	public void localMessageReceived(final String messagePojoStr) {

		try {

			MessagePojo messagePojo = gson.fromJson(messagePojoStr, MessagePojo.class);

			String senderUuid = messagePojo.senderUuid;

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

					if (isNew)
						sendAllBeaconsToLocalUser(senderUuid);

				}

				// Gonderen uuid agda yayinlanacak
				listener.publishUuid(senderUuid);

				break;

			case REQ_BCON:

				sendAllBeaconsToLocalUser(senderUuid);

				break;

			default:

				String proxyUuid = messagePojo.proxyUuid;

				if (proxyUuid == null || localUserBeacon.containsKey(proxyUuid)) {

					String[] receiverUuids = messagePojo.receiverUuid.split(";");

					List<String> remoteReceiverUuids = new ArrayList<String>();

					for (String receiverUuid : receiverUuids) {

						if (localUserBeacon.containsKey(receiverUuid))
							listener.sendToLocalUser(receiverUuid, messagePojoStr);
						else if (remoteUserBeacon.containsKey(receiverUuid))
							remoteReceiverUuids.add(receiverUuid);

					}

					if (remoteReceiverUuids.size() == 1)
						listener.sendToRemoteUser(remoteReceiverUuids.get(0), messagePojoStr);
					else if (remoteReceiverUuids.size() > 0)
						listener.sendToRemoteUsers(remoteReceiverUuids, messagePojoStr);

				} else if (remoteUserBeacon.containsKey(proxyUuid)) {

					listener.sendToRemoteUser(proxyUuid, messagePojoStr);

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

				String[] receiverUuids = messagePojo.receiverUuid.split(";");

				List<String> remoteReceiverUuids = new ArrayList<String>();

				for (String receiverUuid : receiverUuids) {

					if (localUserBeacon.containsKey(receiverUuid))
						listener.sendToLocalUser(receiverUuid, messagePojoStr);
					else if (remoteUserBeacon.containsKey(receiverUuid))
						remoteReceiverUuids.add(receiverUuid);

				}

				if (remoteReceiverUuids.size() == 1)
					listener.sendToRemoteUser(remoteReceiverUuids.get(0), messagePojoStr);
				else if (remoteReceiverUuids.size() > 0)
					listener.sendToRemoteUsers(remoteReceiverUuids, messagePojoStr);

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

		String messagePojoStr = gson.toJson(new MessagePojo(uuid, "", ContentType.UUID_DISCONNECTED));

		remoteUserBeacon.remove(uuid);

		localUserBeacon.forEach((receiverUuid, message) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

	}

	public void localUserDisconnected(String uuid) {

		if (!localUserBeacon.containsKey(uuid))
			return;

		String messagePojoStr = gson.toJson(new MessagePojo(uuid, "", ContentType.UUID_DISCONNECTED));

		localUserBeacon.remove(uuid);

		localUserBeacon.forEach((receiverUuid, message) -> listener.sendToLocalUser(receiverUuid, messagePojoStr));

		listener.sendToAllRemoteUsers(messagePojoStr);

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

}

package com.ogya.dms.server.model.intf;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface ModelListener {

	void sendToLocalUser(String receiverUuid, String message);

	void sendToRemoteServer(String dmsUuid, String message, AtomicBoolean sendStatus, Consumer<Integer> progressMethod,
			long timeout, InetAddress useLocalAddress);

	void sendToAllRemoteServers(String message);

	void publishImmediately();

}

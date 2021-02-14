package com.ogya.dms.server.model.intf;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface ModelListener {

	void sendToLocalUser(String receiverUuid, byte[] message);

	void sendToRemoteServer(String dmsUuid, byte[] message, AtomicBoolean sendStatus, Consumer<Integer> progressMethod,
			long timeout, InetAddress useLocalAddress);

	void sendToAllRemoteServers(byte[] message);

	void publishImmediately();

}

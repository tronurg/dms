package com.ogya.dms.server.model.intf;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public interface ModelListener {

	void sendToLocalUsers(MessagePojo messagePojo, String... receiverUuids);

	void sendToRemoteServer(String dmsUuid, MessagePojo messagePojo, AtomicBoolean sendStatus,
			Consumer<Integer> progressMethod, long timeout, InetAddress useLocalAddress);

	void sendToAllRemoteServers(MessagePojo messagePojo);

	void publishImmediately();

}

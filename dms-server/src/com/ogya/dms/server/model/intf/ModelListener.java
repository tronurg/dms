package com.ogya.dms.server.model.intf;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public interface ModelListener {

	void sendToLocalUsers(MessagePojo messagePojo, Consumer<Set<String>> failConsumer, String... receiverUuids);

	void sendToRemoteServer(String dmsUuid, MessagePojo messagePojo, AtomicBoolean sendStatus,
			Consumer<Integer> progressConsumer, long timeout, InetAddress useLocalAddress);

	void sendToAllRemoteServers(MessagePojo messagePojo);

	void publishImmediately();

}

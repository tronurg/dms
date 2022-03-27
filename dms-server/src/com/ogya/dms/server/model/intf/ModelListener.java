package com.ogya.dms.server.model.intf;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public interface ModelListener {

	void localMessageReady(String receiverUuid);

	void sendToRemoteServer(String dmsUuid, MessagePojo messagePojo, AtomicBoolean sendStatus,
			Consumer<Integer> progressConsumer);

	void sendToAllRemoteServers(MessagePojo messagePojo);

	void publishImmediately();

}

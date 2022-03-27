package com.ogya.dms.server.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public class MessageContainerLocal extends MessageContainerBase {

	public final Consumer<Integer> progressConsumer;

	public MessageContainerLocal(int messageNumber, MessagePojo messagePojo, AtomicBoolean sendStatus,
			Consumer<Integer> progressConsumer) {
		super(messageNumber, messagePojo, Direction.SERVER_TO_CLIENT, sendStatus);
		this.progressConsumer = progressConsumer;
	}

}

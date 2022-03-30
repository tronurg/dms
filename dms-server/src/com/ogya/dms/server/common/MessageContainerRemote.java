package com.ogya.dms.server.common;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public class MessageContainerRemote extends MessageContainerBase {

	private static final MessagePojo END_MESSAGE = new MessagePojo();
	private static final int UPDATE_TURNS = 10;

	public final InetAddress useLocalAddress;
	private final boolean endMessage;
	public BiFunction<Integer, ByteBuffer, Boolean> sendFunction;
	private int updateCounter = 0;

	public MessageContainerRemote(int messageNumber, MessagePojo messagePojo, AtomicBoolean sendStatus,
			Consumer<Integer> progressConsumer) {
		super(messageNumber, messagePojo, Direction.SERVER_TO_SERVER, sendStatus, progressConsumer);
		this.useLocalAddress = messagePojo.useLocalAddress;
		this.endMessage = messagePojo == END_MESSAGE;
	}

	public static MessageContainerRemote getEndMessage() {
		return new MessageContainerRemote(-1, END_MESSAGE, null, null);
	}

	public boolean isUpdateReady() {
		return updateCounter > UPDATE_TURNS;
	}

	public boolean updateSendFunction(BiFunction<Integer, ByteBuffer, Boolean> sendFunction) {
		boolean updated = this.sendFunction != sendFunction;
		this.sendFunction = sendFunction;
		updateCounter = 0;
		return updated;
	}

	public boolean isEndMessage() {
		return endMessage;
	}

	@Override
	public Chunk next() {
		++updateCounter;
		return super.next();
	}

	@Override
	public void rewind() {
		updateCounter = UPDATE_TURNS;
		super.rewind();
	}

}

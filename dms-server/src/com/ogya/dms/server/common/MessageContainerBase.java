package com.ogya.dms.server.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.ogya.dms.commons.DmsMessageSender;
import com.ogya.dms.commons.structures.MessagePojo;

public abstract class MessageContainerBase extends DmsMessageSender {

	public final int messageNumber;
	public final AtomicBoolean sendStatus;
	public final Consumer<Integer> progressConsumer;
	public final Long useTimeout;

	public final boolean bigFile;
	public final long startTime = System.currentTimeMillis();
	public final AtomicInteger progressPercent = new AtomicInteger(-1);
	public long checkInTime = startTime;

	public MessageContainerBase(int messageNumber, MessagePojo messagePojo, Direction direction,
			AtomicBoolean sendStatus, Consumer<Integer> progressConsumer) {
		super(messagePojo, direction);
		this.messageNumber = messageNumber;
		this.sendStatus = sendStatus;
		this.progressConsumer = progressConsumer;
		this.useTimeout = messagePojo.useTimeout;
		this.bigFile = isFileSizeGreaterThan(CommonConstants.SMALL_FILE_LIMIT);
	}

	@Override
	public Chunk next() {
		checkInTime = System.currentTimeMillis();
		health.set((sendStatus == null || sendStatus.get())
				&& (useTimeout == null || checkInTime - startTime < useTimeout));
		return super.next();
	}

	@Override
	public void close() {
		super.close();
		if (progressConsumer != null && progressPercent.get() < 100) {
			progressConsumer.accept(-1);
		}
	}

}

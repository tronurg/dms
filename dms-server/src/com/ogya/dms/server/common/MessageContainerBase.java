package com.ogya.dms.server.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.ogya.dms.commons.DmsMessageSender;
import com.ogya.dms.commons.structures.MessagePojo;

public abstract class MessageContainerBase extends DmsMessageSender {

	public final int messageNumber;
	public final AtomicBoolean sendStatus;
	public final Long useTimeout;

	public final boolean bigFile;
	public final long startTime = System.currentTimeMillis();
	public final AtomicInteger progressPercent = new AtomicInteger(-1);
	public long checkInTime = startTime;

	public MessageContainerBase(int messageNumber, MessagePojo messagePojo, Direction direction,
			AtomicBoolean sendStatus) {
		super(messagePojo, direction);
		this.messageNumber = messageNumber;
		this.sendStatus = sendStatus;
		this.useTimeout = messagePojo.useTimeout;
		this.bigFile = isFileSizeGreaterThan(CommonConstants.SMALL_FILE_LIMIT);
	}

	@Override
	public Chunk next() {
		checkInTime = System.currentTimeMillis();
		health.set(sendStatus.get() && (useTimeout == null || checkInTime - startTime < useTimeout));
		return super.next();
	}

	@Override
	public void reset() {
		super.reset();
		progressPercent.set(-1);
	}

}

package com.ogya.dms.server.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.ogya.dms.commons.DmsMessageSender;
import com.ogya.dms.commons.DmsMessageSender.Direction;
import com.ogya.dms.commons.structures.MessagePojo;

public abstract class MessageContainerBase {

	public final int messageNumber;
	public final DmsMessageSender messageSender;
	public final AtomicBoolean sendStatus;
	public final Long useTimeout;
	public final boolean bigFile;
	public final long startTime = System.currentTimeMillis();
	public final AtomicBoolean health = new AtomicBoolean(true);
	public final AtomicInteger progressPercent = new AtomicInteger(-1);
	public long checkInTime = startTime;

	public MessageContainerBase(int messageNumber, MessagePojo messagePojo, Direction direction,
			AtomicBoolean sendStatus) {
		this.messageNumber = messageNumber;
		this.messageSender = new DmsMessageSender(messagePojo, health, direction);
		this.sendStatus = sendStatus;
		this.useTimeout = messagePojo.useTimeout;
		this.bigFile = this.messageSender.isFileSizeGreaterThan(CommonConstants.SMALL_FILE_LIMIT);
	}

	public void checkIn() {
		checkInTime = System.currentTimeMillis();
		health.set(sendStatus.get() && (useTimeout == null || checkInTime - startTime < useTimeout));
	}

	public void reset() {
		messageSender.reset();
		progressPercent.set(-1);
	}

}

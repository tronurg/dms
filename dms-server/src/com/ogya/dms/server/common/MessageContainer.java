package com.ogya.dms.server.common;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.ogya.dms.commons.DmsMessageFactory;
import com.ogya.dms.commons.DmsMessageFactory.MessageSender;
import com.ogya.dms.commons.structures.MessagePojo;

public class MessageContainer {

	public final int messageNumber;
	public final MessageSender messageSender;
	public final AtomicBoolean sendStatus;
	public final Long useTimeout;
	public final InetAddress useLocalAddress;
	public final Consumer<Integer> progressConsumer;
	public final boolean bigFile;
	public final long startTime = System.currentTimeMillis();
	public final AtomicBoolean health = new AtomicBoolean(true);
	public final AtomicInteger progressPercent = new AtomicInteger(-1);
	public long checkInTime = startTime;
	public BiFunction<Integer, byte[], Boolean> sendFunction;

	public MessageContainer(int messageNumber, MessagePojo messagePojo, AtomicBoolean sendStatus,
			Consumer<Integer> progressConsumer) {
		this.messageNumber = messageNumber;
		this.messageSender = DmsMessageFactory.outFeedRemote(messagePojo, health);
		this.sendStatus = sendStatus;
		this.useTimeout = messagePojo.useTimeout;
		this.useLocalAddress = messagePojo.useLocalAddress;
		this.progressConsumer = progressConsumer;
		this.bigFile = this.messageSender.fileSizeGreaterThan(CommonConstants.SMALL_FILE_LIMIT);
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

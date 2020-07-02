package com.ogya.dms.structures;

import javafx.scene.paint.Color;

public enum MessageStatus {

	FRESH(Color.TRANSPARENT, Color.TRANSPARENT, 0), SENT(Color.DARKGRAY, Color.TRANSPARENT, 1),
	RECEIVED(Color.DARKGRAY, Color.DARKGRAY, 2), READ(Color.DEEPSKYBLUE, Color.DEEPSKYBLUE, 3);

	private static final MessageStatus[] orderedStatus = new MessageStatus[] { FRESH, SENT, RECEIVED, READ };

	private final Color waitingColor;
	private final Color transmittedColor;
	private final int logicalOrder;

	private MessageStatus(Color waitingColor, Color transmittedColor, int logicalOrder) {

		this.waitingColor = waitingColor;
		this.transmittedColor = transmittedColor;
		this.logicalOrder = logicalOrder;

	}

	public Color getWaitingColor() {

		return waitingColor;

	}

	public Color getTransmittedColor() {

		return transmittedColor;

	}

	public int getLogicalOrder() {

		return logicalOrder;

	}

	public static MessageStatus getMessageStatusByLogicalOrder(int logicalOrder) {

		if (logicalOrder < 0 || !(logicalOrder < orderedStatus.length))
			return null;

		return orderedStatus[logicalOrder];

	}

}

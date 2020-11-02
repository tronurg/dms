package com.ogya.dms.structures;

import javafx.scene.paint.Color;

public enum MessageStatus {

	FRESH(Color.TRANSPARENT, Color.TRANSPARENT), SENT(Color.DARKGRAY, Color.TRANSPARENT),
	RECEIVED(Color.DARKGRAY, Color.DARKGRAY), READ(Color.DEEPSKYBLUE, Color.DEEPSKYBLUE);

	private final Color waitingColor;
	private final Color transmittedColor;

	private MessageStatus(Color waitingColor, Color transmittedColor) {

		this.waitingColor = waitingColor;
		this.transmittedColor = transmittedColor;

	}

	public Color getWaitingColor() {

		return waitingColor;

	}

	public Color getTransmittedColor() {

		return transmittedColor;

	}

}

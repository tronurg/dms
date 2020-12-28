package com.ogya.dms.core.structures;

import com.ogya.dms.core.common.CommonMethods;

import javafx.scene.paint.Color;

public enum MessageStatus {

	FRESH(Color.TRANSPARENT, Color.TRANSPARENT), SENT(Color.DARKGRAY, Color.TRANSPARENT),
	RECEIVED(Color.DARKGRAY, Color.DARKGRAY), READ(Color.DEEPSKYBLUE, Color.DEEPSKYBLUE);

	private transient final Color waitingColor;
	private transient final Color transmittedColor;

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

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static MessageStatus fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, MessageStatus.class);
	}

}

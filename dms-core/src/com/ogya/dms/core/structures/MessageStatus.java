package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javafx.scene.paint.Color;

public enum MessageStatus {

	FRESH(0, Color.TRANSPARENT, Color.TRANSPARENT), SENT(1, Color.DARKGRAY, Color.TRANSPARENT),
	RECEIVED(2, Color.DARKGRAY, Color.DARKGRAY), READ(3, Color.DEEPSKYBLUE, Color.DEEPSKYBLUE);

	private static final Map<Integer, MessageStatus> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, MessageStatus>());

	@JsonValue
	private final int index;
	private transient final Color waitingColor;
	private transient final Color transmittedColor;

	static {
		for (MessageStatus value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private MessageStatus(int index, Color waitingColor, Color transmittedColor) {

		this.index = index;
		this.waitingColor = waitingColor;
		this.transmittedColor = transmittedColor;

	}

	@JsonCreator
	public static MessageStatus of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

	public Color getWaitingColor() {

		return waitingColor;

	}

	public Color getTransmittedColor() {

		return transmittedColor;

	}

}

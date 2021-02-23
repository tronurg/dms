package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {

	TEXT(0), FILE(1), AUDIO(2), UPDATE(3);

	private static final Map<Integer, MessageType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, MessageType>());

	@JsonValue
	private final int index;

	static {
		for (MessageType value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private MessageType(int index) {

		this.index = index;

	}

	public static MessageType of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

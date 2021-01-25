package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum MessageType {

	TEXT(0), FILE(1), AUDIO(2), OBJECT(3), LIST(4), UPDATE(5);

	private static final Map<Integer, MessageType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, MessageType>());

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

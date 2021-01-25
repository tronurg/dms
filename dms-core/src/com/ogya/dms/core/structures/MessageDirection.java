package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum MessageDirection {

	IN(0), OUT(1);

	private static final Map<Integer, MessageDirection> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, MessageDirection>());

	private final int index;

	static {
		for (MessageDirection value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private MessageDirection(int index) {

		this.index = index;

	}

	public static MessageDirection of(int index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

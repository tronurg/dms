package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum MessageSubType {

	FILE_REPORT(0), UPDATE_GROUP(1), UPDATE_CANCEL_MESSAGE(2);

	private static final Map<Integer, MessageSubType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, MessageSubType>());

	private final int index;

	static {
		for (MessageSubType value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private MessageSubType(int index) {

		this.index = index;

	}

	public static MessageSubType of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ReceiverType {

	CONTACT(0), GROUP_OWNER(1), GROUP_MEMBER(2);

	private static final Map<Integer, ReceiverType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, ReceiverType>());

	private final int index;

	static {
		for (ReceiverType value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private ReceiverType(int index) {

		this.index = index;

	}

	public static ReceiverType of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

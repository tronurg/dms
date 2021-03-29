package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GroupReceiverType {

	GROUP_OWNER(0), GROUP_MEMBER(1);

	private static final Map<Integer, GroupReceiverType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, GroupReceiverType>());

	@JsonValue
	private final int index;

	static {
		for (GroupReceiverType value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private GroupReceiverType(int index) {

		this.index = index;

	}

	public static GroupReceiverType of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

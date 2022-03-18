package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UpdateType {

	UPDATE_GROUP(0), CANCEL_MESSAGE(1);

	private static final Map<Integer, UpdateType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, UpdateType>());

	@JsonValue
	private final int index;

	static {
		for (UpdateType value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private UpdateType(int index) {

		this.index = index;

	}

	@JsonCreator
	public static UpdateType of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

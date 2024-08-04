package com.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AttachmentType {

	FILE(0), AUDIO(1), REPORT(2);

	private static final Map<Integer, AttachmentType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, AttachmentType>());

	@JsonValue
	private final int index;

	static {
		for (AttachmentType value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private AttachmentType(int index) {

		this.index = index;

	}

	@JsonCreator
	public static AttachmentType of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

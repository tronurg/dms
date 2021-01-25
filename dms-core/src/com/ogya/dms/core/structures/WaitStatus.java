package com.ogya.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum WaitStatus {

	WAITING(0), CANCELED(1), DONE(2);

	private static final Map<Integer, WaitStatus> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, WaitStatus>());

	private final int index;

	static {
		for (WaitStatus value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private WaitStatus(int index) {

		this.index = index;

	}

	public static WaitStatus of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

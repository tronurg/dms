package com.dms.core.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ViewStatus {

	DEFAULT(0), ARCHIVED(1), DELETED(2);

	private static final Map<Integer, ViewStatus> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, ViewStatus>());

	private final int index;

	static {
		for (ViewStatus value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private ViewStatus(int index) {

		this.index = index;

	}

	public static ViewStatus of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

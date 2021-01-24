package com.ogya.dms.commons.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ContentType {

	MESSAGE(0), BCON(1), TRANSIENT(2), CLAIM_MESSAGE_STATUS(3), FEED_MESSAGE_STATUS(4), FEED_GROUP_MESSAGE_STATUS(5),
	CLAIM_STATUS_REPORT(6), FEED_STATUS_REPORT(7), CANCEL(8), UUID_DISCONNECTED(9), REQ_STRT(10), IP(11), ADD_IP(12),
	REMOVE_IP(13), PROGRESS(14);

	private static final Map<Integer, ContentType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, ContentType>());

	private final int index;

	static {
		for (ContentType value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private ContentType(int index) {

		this.index = index;

	}

	public static ContentType of(int index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

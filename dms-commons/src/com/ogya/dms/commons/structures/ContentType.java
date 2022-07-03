package com.ogya.dms.commons.structures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentType {

	BCON(0), UUID_DISCONNECTED(1), STATUS_INFO(2),

	MESSAGE(10), TRANSIENT(11),

	CLAIM_MESSAGE_STATUS(20), CLAIM_STATUS_REPORT(21),

	FEED_MESSAGE_STATUS(30), FEED_GROUP_MESSAGE_STATUS(31), FEED_STATUS_REPORT(32), FEED_TRANSIENT_STATUS(33),

	DOWNLOAD_REQUEST(40), CANCEL_DOWNLOAD_REQUEST(41),

	SERVER_NOT_FOUND(50), FILE_NOT_FOUND(51), UPLOAD(52),

	REQ_STRT(60), IPS(61), ADD_IPS(62), REMOVE_IPS(63);

	private static final Map<Integer, ContentType> INDEX_MAP = Collections
			.synchronizedMap(new HashMap<Integer, ContentType>());

	@JsonValue
	private final int index;

	static {
		for (ContentType value : values()) {
			INDEX_MAP.put(value.index, value);
		}
	}

	private ContentType(int index) {

		this.index = index;

	}

	@JsonCreator
	public static ContentType of(Integer index) {
		return INDEX_MAP.get(index);
	}

	public int index() {
		return index;
	}

}

package com.ogya.dms.structures;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.ogya.dms.common.CommonMethods;

public class StatusReport {

	@SerializedName(value = "a")
	public final Map<Long, MessageStatus> idStatus = new HashMap<Long, MessageStatus>();

	public MessageStatus getOverallStatus() {

		if (idStatus.size() == 0)
			return MessageStatus.READ;

		int minOrder = Integer.MAX_VALUE;

		for (MessageStatus messageStatus : idStatus.values()) {

			minOrder = Math.min(minOrder, messageStatus.ordinal());

		}

		return MessageStatus.values()[minOrder];

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static StatusReport fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, StatusReport.class);
	}

}

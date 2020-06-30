package com.ogya.dms.structures;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.CommonMethods;

public class StatusReport {

	public final Map<String, MessageStatus> uuidStatus = new HashMap<String, MessageStatus>();

	public MessageStatus getOverallStatus() {

		if (uuidStatus.size() == 0)
			return MessageStatus.READ;

		int minOrder = Integer.MAX_VALUE;

		for (MessageStatus messageStatus : uuidStatus.values()) {

			minOrder = Math.min(minOrder, messageStatus.getLogicalOrder());

		}

		return MessageStatus.getMessageStatusByLogicalOrder(minOrder);

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static StatusReport fromJson(String json) throws JsonSyntaxException {
		return CommonMethods.fromJson(json, StatusReport.class);
	}

}

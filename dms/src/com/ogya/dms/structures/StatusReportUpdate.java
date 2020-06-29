package com.ogya.dms.structures;

import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.CommonMethods;

public class StatusReportUpdate {

	public final String senderUuid;
	public final long messageId;

	public StatusReport statusReport;

	public StatusReportUpdate(String senderUuid, long messageId) {

		this.senderUuid = senderUuid;
		this.messageId = messageId;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static StatusReportUpdate fromJson(String json) throws JsonSyntaxException {
		return CommonMethods.fromJson(json, StatusReportUpdate.class);
	}

}

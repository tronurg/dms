package com.ogya.dms.structures;

public class StatusReportUpdate {

	public final String senderUuid;
	public final long messageId;

	public StatusReport statusReport;

	public StatusReportUpdate(String senderUuid, long messageId) {

		this.senderUuid = senderUuid;
		this.messageId = messageId;

	}

}

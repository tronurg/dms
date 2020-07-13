package com.ogya.dms.structures;

import com.ogya.dms.common.CommonMethods;

public class MessageStatusUpdate {

	public final String senderUuid;
	public final Long messageId;

	public String receiverUuid;
	public MessageStatus messageStatus;

	public MessageStatusUpdate(String senderUuid, Long messageId) {

		this.senderUuid = senderUuid;
		this.messageId = messageId;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static MessageStatusUpdate fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, MessageStatusUpdate.class);
	}

}

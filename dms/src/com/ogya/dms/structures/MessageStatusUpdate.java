package com.ogya.dms.structures;

import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.CommonMethods;

public class MessageStatusUpdate {

	public final String senderUuid;
	public final String receiverUuid;
	public final long messageId;

	public MessageStatus messageStatus;

	public MessageStatusUpdate(String senderUuid, String receiverUuid, long messageId) {

		this.senderUuid = senderUuid;
		this.receiverUuid = receiverUuid;
		this.messageId = messageId;

	}

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static MessageStatusUpdate fromJson(String json) throws JsonSyntaxException {
		return CommonMethods.fromJson(json, MessageStatusUpdate.class);
	}

}

package com.ogya.dms.common.structures;

import com.google.gson.annotations.SerializedName;

public class MessagePojo {

	@SerializedName(value = "a")
	public final String message;
	@SerializedName(value = "b")
	public final String senderUuid;
	@SerializedName(value = "c")
	public final String receiverUuid;
	@SerializedName(value = "d")
	public final ContentType contentType;
	@SerializedName(value = "e")
	public final Long messageId;

	public MessagePojo(String message, String senderUuid, ContentType contentType, Long messageId) {

		this(message, senderUuid, null, contentType, messageId);

	}

	public MessagePojo(String message, String senderUuid, String receiverUuid, ContentType contentType,
			Long messageId) {

		this.message = message;
		this.senderUuid = senderUuid;
		this.receiverUuid = receiverUuid;
		this.contentType = contentType;
		this.messageId = messageId;

	}

	public String toJson() {
		return JsonFactory.toJson(this);
	}

	public static MessagePojo fromJson(String json) throws Exception {
		return JsonFactory.fromJson(json, MessagePojo.class);
	}

}

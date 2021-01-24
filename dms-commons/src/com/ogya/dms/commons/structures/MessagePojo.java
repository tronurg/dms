package com.ogya.dms.commons.structures;

import java.net.InetAddress;

import com.google.gson.annotations.SerializedName;

public class MessagePojo {

	@SerializedName("a")
	public final String message;
	@SerializedName("b")
	public final String senderUuid;
	@SerializedName("c")
	public final String receiverUuid;
	@SerializedName("d")
	public final ContentType contentType;
	@SerializedName("e")
	public final Long messageId;
	@SerializedName("f")
	public final InetAddress useLocalAddress;

	public MessagePojo(String message, String senderUuid, ContentType contentType, Long messageId) {

		this(message, senderUuid, null, contentType, messageId, null);

	}

	public MessagePojo(String message, String senderUuid, String receiverUuid, ContentType contentType,
			Long messageId) {

		this(message, senderUuid, receiverUuid, contentType, messageId, null);

	}

	public MessagePojo(String message, String senderUuid, String receiverUuid, ContentType contentType, Long messageId,
			InetAddress useLocalAddress) {

		this.message = message;
		this.senderUuid = senderUuid;
		this.receiverUuid = receiverUuid;
		this.contentType = contentType;
		this.messageId = messageId;
		this.useLocalAddress = useLocalAddress;

	}

	public String toJson() {
		return JsonFactory.toJson(this);
	}

	public static MessagePojo fromJson(String json) throws Exception {
		return JsonFactory.fromJson(json, MessagePojo.class);
	}

}

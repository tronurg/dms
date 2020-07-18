package com.ogya.dms.common.structures;

public class MessagePojo {

	public final String message;
	public final String senderUuid;
	public final String receiverUuid;
	public final ContentType contentType;
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

}

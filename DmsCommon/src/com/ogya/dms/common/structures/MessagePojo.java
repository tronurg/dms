package com.ogya.dms.common.structures;

public class MessagePojo {

	public final String message;
	public final String senderUuid;
	public final String proxyUuid;
	public final String receiverUuid;
	public final ContentType contentType;

	public MessagePojo(String message, String senderUuid, ContentType contentType) {

		this(message, senderUuid, "", contentType);

	}

	public MessagePojo(String message, String senderUuid, String receiverUuid, ContentType contentType) {

		this(message, senderUuid, receiverUuid, receiverUuid, contentType);

	}

	public MessagePojo(String message, String senderUuid, String proxyUuid, String receiverUuid,
			ContentType contentType) {

		this.message = message;
		this.senderUuid = senderUuid;
		this.proxyUuid = proxyUuid;
		this.receiverUuid = receiverUuid;
		this.contentType = contentType;

	}

}

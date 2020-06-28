package com.ogya.dms.structures;

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

}

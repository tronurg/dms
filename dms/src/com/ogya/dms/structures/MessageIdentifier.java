package com.ogya.dms.structures;

public class MessageIdentifier {

	public final String senderUuid;
	public final Long messageId;

	public MessageIdentifier(String senderUuid, Long messageId) {
		this.senderUuid = senderUuid;
		this.messageId = messageId;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MessageIdentifier))
			return false;
		MessageIdentifier messageIdentifier = (MessageIdentifier) obj;
		return senderUuid.equals(messageIdentifier.senderUuid) && messageId.equals(messageIdentifier.messageId);
	}

	@Override
	public int hashCode() {
		return (senderUuid + messageId).hashCode();
	}

}

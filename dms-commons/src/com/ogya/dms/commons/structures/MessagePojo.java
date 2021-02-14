package com.ogya.dms.commons.structures;

import java.net.InetAddress;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessagePojo {

	@JsonProperty("a")
	public final byte[] payload;
	@JsonProperty("b")
	public final String senderUuid;
	@JsonProperty("c")
	public final String receiverUuid;
	@JsonProperty("d")
	public final ContentType contentType;
	@JsonProperty("e")
	public final Long messageId;
	@JsonProperty("f")
	public final Long useTrackingId;
	@JsonProperty("g")
	public final Long useTimeout;
	@JsonProperty("h")
	public final InetAddress useLocalAddress;

	public MessagePojo(byte[] payload, String senderUuid, String receiverUuid, ContentType contentType, Long messageId,
			Long useTrackingId, Long useTimeout, InetAddress useLocalAddress) {

		this.payload = payload;
		this.senderUuid = senderUuid;
		this.receiverUuid = receiverUuid;
		this.contentType = contentType;
		this.messageId = messageId;
		this.useTrackingId = useTrackingId;
		this.useTimeout = useTimeout;
		this.useLocalAddress = useLocalAddress;

	}

}

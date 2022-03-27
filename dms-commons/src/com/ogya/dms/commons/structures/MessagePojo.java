package com.ogya.dms.commons.structures;

import java.net.InetAddress;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessagePojo {

	@JsonProperty("a")
	public byte[] payload;
	@JsonProperty("b")
	public String senderUuid;
	@JsonProperty("c")
	public String receiverUuid;
	@JsonProperty("d")
	public ContentType contentType;
	@JsonProperty("e")
	public Long trackingId;

	@JsonProperty("f")
	public Long useTimeout;
	@JsonProperty("g")
	public InetAddress useLocalAddress;

	@JsonProperty("h")
	public AttachmentPojo attachment;

	public MessagePojo() {
		super();
	}

	public MessagePojo(byte[] payload, String senderUuid, String receiverUuid, ContentType contentType, Long trackingId,
			Long useTimeout, InetAddress useLocalAddress) {

		this.payload = payload;
		this.senderUuid = senderUuid;
		this.receiverUuid = receiverUuid;
		this.contentType = contentType;
		this.trackingId = trackingId;
		this.useTimeout = useTimeout;
		this.useLocalAddress = useLocalAddress;

	}

	public Path getAttachmentLink() {
		if (attachment != null) {
			return attachment.path;
		}
		return null;
	}

}

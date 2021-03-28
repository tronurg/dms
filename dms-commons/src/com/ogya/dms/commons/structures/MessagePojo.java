package com.ogya.dms.commons.structures;

import java.net.InetAddress;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
	public Long useTrackingId;
	@JsonProperty("f")
	public Long useTimeout;
	@JsonProperty("g")
	public InetAddress useLocalAddress;

	@JsonIgnore
	public Path attachment;

	public MessagePojo() {
		super();
	}

	public MessagePojo(byte[] payload, String senderUuid, String receiverUuid, ContentType contentType,
			Long useTrackingId, Long useTimeout, InetAddress useLocalAddress) {

		this.payload = payload;
		this.senderUuid = senderUuid;
		this.receiverUuid = receiverUuid;
		this.contentType = contentType;
		this.useTrackingId = useTrackingId;
		this.useTimeout = useTimeout;
		this.useLocalAddress = useLocalAddress;

	}

}

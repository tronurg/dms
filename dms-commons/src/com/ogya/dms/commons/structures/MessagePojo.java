package com.ogya.dms.commons.structures;

import java.net.InetAddress;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessagePojo {

	@JsonProperty("a")
	public byte[] payload;
	@JsonProperty("b")
	public String senderUuid;
	@JsonProperty("c")
	public List<String> receiverUuids;
	@JsonProperty("d")
	public String receiverAddress;
	@JsonProperty("e")
	public ContentType contentType;
	@JsonProperty("f")
	public Long trackingId;
	@JsonProperty("g")
	public Long globalSize;

	@JsonProperty("h")
	public InetAddress useLocalAddress;

	public MessagePojo() {
		super();
	}

	public MessagePojo(byte[] payload, String senderUuid, List<String> receiverUuids, String receiverAddress,
			ContentType contentType, Long trackingId, InetAddress useLocalAddress) {

		this.payload = payload;
		this.senderUuid = senderUuid;
		this.receiverUuids = receiverUuids;
		this.receiverAddress = receiverAddress;
		this.contentType = contentType;
		this.trackingId = trackingId;
		this.useLocalAddress = useLocalAddress;

	}

}

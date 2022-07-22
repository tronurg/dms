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
	public ContentType contentType;
	@JsonProperty("e")
	public Long trackingId;
	@JsonProperty("f")
	public Long globalSize;
	@JsonProperty("g")
	public InetAddress useLocalAddress;

	@JsonProperty("h")
	public String address;

	public MessagePojo() {
		super();
	}

	public MessagePojo(byte[] payload, String senderUuid, List<String> receiverUuids, ContentType contentType,
			Long trackingId) {

		this.payload = payload;
		this.senderUuid = senderUuid;
		this.receiverUuids = receiverUuids;
		this.contentType = contentType;
		this.trackingId = trackingId;

	}

	public MessagePojo(byte[] payload, String senderUuid, List<String> receiverUuids, ContentType contentType,
			Long trackingId, InetAddress useLocalAddress, String address) {

		this(payload, senderUuid, receiverUuids, contentType, trackingId);
		this.useLocalAddress = useLocalAddress;
		this.address = address;

	}

}

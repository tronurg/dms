package com.ogya.dms.server.structures;

import java.util.ArrayList;
import java.util.List;

public class Chunk {

	public final int messageNumber;
	public final byte[] data;
	public final List<String> receiverUuids = new ArrayList<String>();
	public final SendMorePojo sendMore;

	public Chunk(int messageNumber, byte[] data, SendMorePojo sendMore) {
		this.messageNumber = messageNumber;
		this.data = data;
		this.sendMore = sendMore;
	}

	public Chunk(int messageNumber, byte[] data, List<String> receiverUuids, SendMorePojo sendMore) {
		this(messageNumber, data, sendMore);
		if (receiverUuids != null) {
			this.receiverUuids.addAll(receiverUuids);
		}
	}

}

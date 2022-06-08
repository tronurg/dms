package com.ogya.dms.server.structures;

import java.util.ArrayList;
import java.util.List;

public class LocalChunk extends Chunk {

	public final List<String> receiverUuids = new ArrayList<String>();

	public LocalChunk(int messageNumber, byte[] data, List<String> receiverUuids, SendMorePojo sendMore) {
		super(messageNumber, data, sendMore);
		if (receiverUuids != null) {
			this.receiverUuids.addAll(receiverUuids);
		}
	}

	public LocalChunk(RemoteChunk remoteChunk) {
		super(remoteChunk);
	}

}

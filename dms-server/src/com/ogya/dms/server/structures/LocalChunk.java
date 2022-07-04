package com.ogya.dms.server.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LocalChunk extends Chunk {

	public final List<String> receiverUuids = new ArrayList<String>();

	public LocalChunk(int messageNumber, byte[] data, List<String> receiverUuids, Consumer<Boolean> sendMore) {
		super(messageNumber, data, sendMore);
		if (receiverUuids != null) {
			this.receiverUuids.addAll(receiverUuids);
		}
	}

}

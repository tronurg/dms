package com.ogya.dms.server.structures;

import java.util.function.BiConsumer;

public class RemoteChunk extends Chunk {

	public final RemoteWork remoteWork;

	public RemoteChunk() {
		super();
		this.remoteWork = null;
	}

	public RemoteChunk(int messageNumber, byte[] data, RemoteWork remoteWork, BiConsumer<Integer, Boolean> sendMore) {
		super(messageNumber, data, sendMore);
		this.remoteWork = remoteWork;
	}

}

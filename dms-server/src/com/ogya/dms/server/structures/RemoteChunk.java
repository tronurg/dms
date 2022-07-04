package com.ogya.dms.server.structures;

import java.util.function.Consumer;

public class RemoteChunk extends Chunk {

	public final RemoteWork remoteWork;

	public RemoteChunk() {
		super();
		this.remoteWork = null;
	}

	public RemoteChunk(int messageNumber, byte[] data, RemoteWork remoteWork, Consumer<Boolean> sendMore) {
		super(messageNumber, data, sendMore);
		this.remoteWork = remoteWork;
	}

}

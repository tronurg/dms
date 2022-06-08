package com.ogya.dms.server.structures;

public class RemoteChunk extends Chunk {

	public final RemoteWork remoteWork;

	public RemoteChunk() {
		super();
		this.remoteWork = null;
	}

	public RemoteChunk(int messageNumber, byte[] data, SendMorePojo sendMore, RemoteWork remoteWork) {
		super(messageNumber, data, sendMore);
		this.remoteWork = remoteWork;
	}

}

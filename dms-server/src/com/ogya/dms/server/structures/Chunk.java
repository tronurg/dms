package com.ogya.dms.server.structures;

public abstract class Chunk {

	public final int messageNumber;
	public final byte[] data;
	public final SendMorePojo sendMore;

	public Chunk() {
		this(0, null, null);
	}

	public Chunk(int messageNumber, byte[] data, SendMorePojo sendMore) {
		this.messageNumber = messageNumber;
		this.data = data;
		this.sendMore = sendMore;
	}

	public Chunk(Chunk chunk) {
		this(chunk.messageNumber, chunk.data, chunk.sendMore);
	}

}

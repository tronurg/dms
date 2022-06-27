package com.ogya.dms.server.structures;

import java.util.function.BiConsumer;

public abstract class Chunk {

	public final int messageNumber;
	public final byte[] data;
	public final BiConsumer<Integer, Boolean> sendMore;

	public Chunk() {
		this(0, null, null);
	}

	public Chunk(int messageNumber, byte[] data, BiConsumer<Integer, Boolean> sendMore) {
		this.messageNumber = messageNumber;
		this.data = data;
		this.sendMore = sendMore;
	}

}

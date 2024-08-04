package com.dms.server.structures;

import java.util.function.Consumer;

public abstract class Chunk {

	public final int messageNumber;
	public final byte[] data;
	public final Consumer<Boolean> sendMore;

	public Chunk() {
		this(0, null, null);
	}

	public Chunk(int messageNumber, byte[] data, Consumer<Boolean> sendMore) {
		this.messageNumber = messageNumber;
		this.data = data;
		this.sendMore = sendMore;
	}

}

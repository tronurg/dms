package com.dms.core.structures;

import java.nio.ByteBuffer;

public class Chunk {

	public final ByteBuffer dataBuffer;
	public final int progress;

	public Chunk(ByteBuffer dataBuffer, int progress) {
		this.dataBuffer = dataBuffer;
		this.progress = progress;
	}

}

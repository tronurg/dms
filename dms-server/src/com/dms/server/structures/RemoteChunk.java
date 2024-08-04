package com.dms.server.structures;

import java.net.InetAddress;
import java.util.function.Consumer;

public class RemoteChunk extends Chunk {

	public final InetAddress useLocalAddress;

	public RemoteChunk() {
		super();
		this.useLocalAddress = null;
	}

	public RemoteChunk(int messageNumber, byte[] data, InetAddress useLocalAddress, Consumer<Boolean> sendMore) {
		super(messageNumber, data, sendMore);
		this.useLocalAddress = useLocalAddress;
	}

}

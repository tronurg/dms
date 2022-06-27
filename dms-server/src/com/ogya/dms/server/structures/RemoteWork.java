package com.ogya.dms.server.structures;

import java.net.InetAddress;
import java.util.function.BiFunction;

public class RemoteWork {

	public InetAddress useLocalAddress;
	public BiFunction<Integer, byte[], Boolean> sendFunction;
	public Chunk lastChunk;

	public RemoteWork() {
		this(null);
	}

	public RemoteWork(InetAddress useLocalAddress) {
		this.useLocalAddress = useLocalAddress;
	}

}

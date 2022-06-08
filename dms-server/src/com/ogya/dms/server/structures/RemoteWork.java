package com.ogya.dms.server.structures;

import java.net.InetAddress;
import java.util.function.BiFunction;

public class RemoteWork {

	public final InetAddress useLocalAddress;
	public final Runnable failureRun;

	public BiFunction<Integer, byte[], Boolean> sendFunction;
	public Chunk lastChunk;

	public RemoteWork() {
		this(null, null);
	}

	public RemoteWork(InetAddress useLocalAddress, Runnable failureRun) {
		this.useLocalAddress = useLocalAddress;
		this.failureRun = failureRun;
	}

}

package com.ogya.dms.server.communications.tcp.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.ogya.dms.server.util.Commons;

public final class TcpConnection implements AutoCloseable {

	private static final long HEARTBEAT_MS = (long) Commons.BEACON_INTERVAL_MS;
	private static final long HEALTH_CONTROL_NS = (long) (2e6 * HEARTBEAT_MS);

	private final Socket socket;
	private final BiConsumer<Integer, byte[]> messageConsumer;

	private final DataOutputStream messageOutputStream;

	private final AtomicLong lastSuccessfulSendTime = new AtomicLong();
	private final Queue<Long> latencyNsQueue = new ArrayDeque<Long>();

	TcpConnection(Socket socket, BiConsumer<Integer, byte[]> messageConsumer) throws Exception {

		socket.setSendBufferSize(1);
		socket.setKeepAlive(false);
		socket.setSoLinger(true, 0);

		this.socket = socket;
		this.messageConsumer = messageConsumer;

		messageOutputStream = new DataOutputStream(
				new BufferedOutputStream(new ZstdOutputStream(socket.getOutputStream())));

		for (int i = 0; i < 4; ++i) {
			sendHeartbeat();
		}

		new Thread(this::checkAlive).start();

	}

	private void checkAlive() {

		while (!socket.isClosed()) {

			try {
				Thread.sleep(HEARTBEAT_MS);
				sendHeartbeat();
			} catch (Exception e) {
				close();
			}

		}

	}

	private synchronized void sendHeartbeat() throws Exception {

		long startNs = System.nanoTime();
		messageOutputStream.writeInt(-1);
		messageOutputStream.flush();
		long endNs = System.nanoTime();
		lastSuccessfulSendTime.set(endNs);
		synchronized (latencyNsQueue) {
			latencyNsQueue.offer(endNs - startNs);
			while (latencyNsQueue.size() > 4) {
				latencyNsQueue.remove();
			}
		}

	}

	void listen() {

		try (DataInputStream messageInputStream = new DataInputStream(
				new BufferedInputStream(new ZstdInputStream(socket.getInputStream())))) {

			while (!socket.isClosed()) {

				int messageLength = messageInputStream.readInt();

				if (messageLength < 0)
					continue;

				byte[] message = new byte[messageLength];

				int messageNumber = messageInputStream.readInt();
				messageInputStream.readFully(message);

				messageConsumer.accept(messageNumber, message);

			}

		} catch (Exception e) {

		}

	}

	public synchronized boolean sendMessage(int messageNumber, byte[] data) {

		if (socket.isClosed())
			return false;

		try {

			int messageLen = data.length;
			messageOutputStream.writeInt(messageLen);
			messageOutputStream.writeInt(messageNumber);
			messageOutputStream.write(data);
			messageOutputStream.flush();
			lastSuccessfulSendTime.set(System.nanoTime());

			return true;

		} catch (Exception e) {
			close();
		}

		return false;

	}

	public InetAddress getRemoteAddress() {

		return socket.getInetAddress();

	}

	public InetAddress getLocalAddress() {

		return socket.getLocalAddress();

	}

	public boolean isHealthy() {

		return System.nanoTime() - lastSuccessfulSendTime.get() < HEALTH_CONTROL_NS;

	}

	public long getLatency() {

		synchronized (latencyNsQueue) {
			if (latencyNsQueue.isEmpty())
				return Long.MAX_VALUE;
			return Collections.min(latencyNsQueue);
		}

	}

	@Override
	public void close() {

		try {
			socket.close();
		} catch (IOException e) {

		}

	}

}

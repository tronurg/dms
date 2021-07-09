package com.ogya.dms.server.communications.tcp.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.ogya.dms.server.common.CommonConstants;

public final class TcpConnection implements AutoCloseable {

	private static final long HEARTBEAT_MS = (long) CommonConstants.BEACON_INTERVAL_MS;
	private static final long HEALTH_CONTROL_NS = (long) (2e6 * HEARTBEAT_MS);

	private final Socket socket;
	private final Consumer<byte[]> messageConsumer;

	private final DataOutputStream messageOutputStream;

	private final AtomicLong lastSuccessfulSendTime = new AtomicLong();
	private final AtomicLong latencyNs = new AtomicLong();

	TcpConnection(Socket socket, Consumer<byte[]> messageConsumer) throws Exception {

		socket.setSendBufferSize(1);
		socket.setKeepAlive(false);
		socket.setSoLinger(true, 0);

		this.socket = socket;
		this.messageConsumer = messageConsumer;

		messageOutputStream = new DataOutputStream(
				new BufferedOutputStream(new ZstdOutputStream(socket.getOutputStream())));

		sendHeartbeat();

		new Thread(this::checkAlive).start();

	}

	private void checkAlive() {

		while (!socket.isClosed()) {

			try {
				Thread.sleep(HEARTBEAT_MS);
				sendHeartbeat();
			} catch (Exception e) {

			}

		}

	}

	private synchronized void sendHeartbeat() throws Exception {

		long startNs = System.nanoTime();
		messageOutputStream.writeInt(-1);
		messageOutputStream.flush();
		long endNs = System.nanoTime();
		lastSuccessfulSendTime.set(endNs);
		latencyNs.set(endNs - startNs);

	}

	void listen() {

		try (DataInputStream messageInputStream = new DataInputStream(
				new BufferedInputStream(new ZstdInputStream(socket.getInputStream())))) {

			while (!socket.isClosed()) {

				int messageLength = messageInputStream.readInt();

				if (messageLength < 0)
					continue;

				byte[] message = new byte[messageLength];

				messageInputStream.readFully(message);

				messageConsumer.accept(message);

			}

		} catch (Exception e) {

		}

	}

	public synchronized boolean sendMessage(byte[] message) {

		if (socket.isClosed())
			return false;

		try {

			messageOutputStream.writeInt(message.length);
			messageOutputStream.write(message);
			messageOutputStream.flush();
			lastSuccessfulSendTime.set(System.nanoTime());

			return true;

		} catch (Exception e) {

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

		return latencyNs.get();

	}

	@Override
	public void close() {

		try {
			socket.close();
		} catch (IOException e) {

		}

	}

}

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
import java.util.function.Consumer;

import org.shortpasta.icmp2.IcmpPingResponse;
import org.shortpasta.icmp2.IcmpPingUtil;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.ogya.dms.server.common.CommonConstants;

public final class TcpConnection {

	private final Socket socket;
	private final Consumer<byte[]> messageConsumer;

	private final DataOutputStream messageOutputStream;

	private final Queue<Integer> lastPingTimes = new ArrayDeque<Integer>();

	TcpConnection(Socket socket, Consumer<byte[]> messageConsumer) throws Exception {

		this.socket = socket;
		this.messageConsumer = messageConsumer;

		messageOutputStream = new DataOutputStream(
				new BufferedOutputStream(new ZstdOutputStream(socket.getOutputStream())));

		for (int i = 0; i < 4 && isConnected(); ++i) {
			ping();
		}
		new Thread(this::checkAlive).start();

	}

	private boolean isConnected() {

		return !socket.isClosed();

	}

	void checkAlive() {

		while (isConnected()) {
			ping();
			if (!isConnected())
				break;
			try {
				Thread.sleep(CommonConstants.CONN_TIMEOUT_MS);
			} catch (InterruptedException e) {

			}
		}

	}

	void ping() {

		int pingTime = -1;

		for (int i = 0; i < 4 && pingTime < 0; ++i) {
			IcmpPingResponse pingResponse = IcmpPingUtil.executePingRequest(socket.getInetAddress().getHostAddress(),
					32, 1000);
			if (pingResponse.getSuccessFlag())
				pingTime = pingResponse.getRtt();
		}

		if (pingTime < 0) {
			close();
			return;
		}

		synchronized (lastPingTimes) {
			lastPingTimes.offer(pingTime);
			if (lastPingTimes.size() > 4)
				lastPingTimes.remove();
		}

	}

	void listen() {

		try (DataInputStream messageInputStream = new DataInputStream(
				new BufferedInputStream(new ZstdInputStream(socket.getInputStream())))) {

			while (!socket.isClosed()) {

				int messageLength = messageInputStream.readInt();

				byte[] message = new byte[messageLength];

				int receivedLength = messageInputStream.read(message);

				if (receivedLength != messageLength)
					break;

				messageConsumer.accept(message);

			}

		} catch (Exception e) {

		}

	}

	public int getMinPingTime() {

		synchronized (lastPingTimes) {
			if (lastPingTimes.isEmpty())
				return Integer.MAX_VALUE;
			return Collections.min(lastPingTimes);
		}

	}

	public synchronized boolean sendMessage(byte[] message) {

		if (socket.isClosed())
			return false;

		try {

			messageOutputStream.writeInt(message.length);

			messageOutputStream.write(message);

			messageOutputStream.flush();

			return true;

		} catch (Exception e) {

		}

		close();

		return false;

	}

	public InetAddress getRemoteAddress() {

		return socket.getInetAddress();

	}

	public InetAddress getLocalAddress() {

		return socket.getLocalAddress();

	}

	public void close() {

		try {

			socket.close();

		} catch (IOException e) {

		}

	}

}

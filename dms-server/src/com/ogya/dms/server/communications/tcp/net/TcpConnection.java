package com.ogya.dms.server.communications.tcp.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;

import org.shortpasta.icmp2.IcmpPingUtil;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.ogya.dms.server.common.CommonConstants;

public final class TcpConnection {

	private final Socket socket;
	private final Consumer<byte[]> messageConsumer;

	private final DataOutputStream messageOutputStream;

	TcpConnection(Socket socket, Consumer<byte[]> messageConsumer) throws Exception {

		socket.setKeepAlive(false);
		socket.setSoLinger(true, 0);

		this.socket = socket;
		this.messageConsumer = messageConsumer;

		messageOutputStream = new DataOutputStream(
				new BufferedOutputStream(new ZstdOutputStream(socket.getOutputStream())));

		new Thread(this::checkAlive).start();

	}

	void checkAlive() {

		boolean success;

		while (!socket.isClosed()) {

			success = false;
			for (int i = 0; i < 4 && !success; ++i) {
				success = IcmpPingUtil.executePingRequest(socket.getInetAddress().getHostAddress(), 32, 1000)
						.getSuccessFlag();
			}

			if (!success) {
				close();
				break;
			}

			try {
				Thread.sleep(CommonConstants.BEACON_INTERVAL_MS);
			} catch (InterruptedException e) {

			}

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
			socket.shutdownInput();
		} catch (IOException e) {

		}

	}

}

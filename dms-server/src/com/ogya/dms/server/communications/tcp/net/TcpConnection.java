package com.ogya.dms.server.communications.tcp.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

final class TcpConnection {

	private final Socket socket;
	private final Consumer<byte[]> messageConsumer;

	private final DataOutputStream messageOutputStream;

	TcpConnection(Socket socket, Consumer<byte[]> messageConsumer) throws Exception {

		this.socket = socket;
		this.messageConsumer = messageConsumer;

		messageOutputStream = new DataOutputStream(
				new BufferedOutputStream(new ZstdOutputStream(socket.getOutputStream())));

	}

	boolean isConnected() {

		return !socket.isClosed();

	}

	void listen() {

		try (DataInputStream messageInputStream = new DataInputStream(
				new BufferedInputStream(new ZstdInputStream(socket.getInputStream())))) {

			while (!socket.isClosed()) {

				int messageLength = messageInputStream.readInt();

				byte[] message = new byte[messageLength];

				messageInputStream.read(message);

				messageConsumer.accept(message);

			}

		} catch (Exception e) {

		}

	}

	synchronized boolean sendMessage(byte[] message) {

		try {

			messageOutputStream.writeInt(message.length);

			messageOutputStream.write(message);

			messageOutputStream.flush();

			return true;

		} catch (Exception e) {

		}

		return false;

	}

	InetAddress getRemoteAddress() {

		return socket.getInetAddress();

	}

	int getRemotePort() {

		return socket.getPort();

	}

	InetAddress getLocalAddress() {

		return socket.getLocalAddress();

	}

	int getLocalPort() {

		return socket.getLocalPort();

	}

	void close() {

		try {

			socket.close();

		} catch (IOException e) {

		}

	}

}

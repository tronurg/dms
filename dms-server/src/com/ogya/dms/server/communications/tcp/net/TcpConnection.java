package com.ogya.dms.server.communications.tcp.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

final class TcpConnection {

	private static final int HEADER_BYTE = 0;
	private static final int HEADER_FILE = 1;

	private final Socket socket;
	private final MessageListener messageListener;

	private final DataOutputStream messageOutputStream;

	TcpConnection(Socket socket, MessageListener messageListener) throws Exception {

		this.socket = socket;
		this.messageListener = messageListener;

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

				int messageType = messageInputStream.read();
				int messageLength = messageInputStream.readInt();

				switch (messageType) {
				case HEADER_BYTE:
					byte[] message = new byte[messageLength];
					messageInputStream.read(message);
					try {
						messageListener.messageReceived(message);
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case HEADER_FILE:

					break;
				default:
					break;
				}

			}

		} catch (Exception e) {

		}

	}

	synchronized boolean sendMessage(byte[] message, int chunkSize, AtomicBoolean sendCheck, Runnable success) {

		try {

			int messageLength = message.length;

			if (chunkSize <= 0)
				chunkSize = messageLength;

			if (sendCheck == null)
				sendCheck = new AtomicBoolean(true);

			messageOutputStream.write(HEADER_BYTE);
			messageOutputStream.writeInt(messageLength);

			for (int i = 0; sendCheck.get() && i < messageLength; i += chunkSize) {

				int len = Math.min(chunkSize, messageLength - i);

				messageOutputStream.write(message, i, len);

				messageOutputStream.flush();

				if (sendCheck.get() && success != null)
					success.run();

			}

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

interface MessageListener {

	void messageReceived(byte[] message);

	void fileReceived(Path path);

}

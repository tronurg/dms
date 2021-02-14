package com.ogya.dms.server.communications.tcp.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class TcpServer implements Runnable {

	private final ServerSocket serverSocket;

	private final AtomicInteger idRef = new AtomicInteger(0);

	private final List<TcpServerListener> listeners = Collections.synchronizedList(new ArrayList<TcpServerListener>());

	private final Map<Integer, TcpConnection> tcpConnections = Collections
			.synchronizedMap(new HashMap<Integer, TcpConnection>());

	private final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {

			Thread thread = new Thread(r);

			thread.setDaemon(true);

			return thread;

		}

	});

	public TcpServer(int port) throws IOException {

		serverSocket = new ServerSocket(port);

	}

	public void addListener(TcpServerListener listener) {

		listeners.add(listener);

	}

	public void acceptConnection() {

		if (serverSocket.isClosed())
			return;

		new Thread(this).start();

	}

	public boolean isConnected(int id) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		return tcpConnection != null && tcpConnection.isConnected();

	}

	public boolean sendMessage(int id, byte[] message, int chunkSize, AtomicBoolean sendCheck, Runnable success) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		if (tcpConnection == null)
			return false;

		return tcpConnection.sendMessage(message, chunkSize, sendCheck, success);

	}

	public InetAddress getRemoteAddress(int id) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		if (tcpConnection == null)
			return null;

		return tcpConnection.getRemoteAddress();

	}

	public int getRemotePort(int id) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		if (tcpConnection == null)
			return 0;

		return tcpConnection.getRemotePort();

	}

	public InetAddress getLocalAddress(int id) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		if (tcpConnection == null)
			return null;

		return tcpConnection.getLocalAddress();

	}

	public int getLocalPort(int id) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		if (tcpConnection == null)
			return 0;

		return tcpConnection.getLocalPort();

	}

	public void disconnect(int id) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		if (tcpConnection == null)
			return;

		tcpConnection.close();

	}

	public void close() {

		try {

			serverSocket.close();

		} catch (IOException e) {

		}

	}

	private void connectedToListeners(final int id) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.connected(id)));

	}

	private void disconnectedToListeners(final int id) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.disconnected(id)));

	}

	private MessageListener newMessageListener(final int id) {

		return new MessageListener() {

			@Override
			public void messageReceived(final byte[] message) {

				taskQueue.execute(() -> listeners.forEach(listener -> listener.messageReceived(id, message)));

			}

			@Override
			public void fileReceived(final Path path) {

				taskQueue.execute(() -> listeners.forEach(listener -> listener.fileReceived(id, path)));

			}

		};

	}

	@Override
	public void run() {

		try (Socket socket = serverSocket.accept()) {

			socket.setKeepAlive(false);

			int id = idRef.getAndIncrement();

			TcpConnection tcpConnection = new TcpConnection(socket, newMessageListener(id));
			tcpConnections.put(id, tcpConnection);

			connectedToListeners(id);

			tcpConnection.listen();

			tcpConnections.remove(id);

			disconnectedToListeners(id);

		} catch (IOException e) {

		}

	}

}

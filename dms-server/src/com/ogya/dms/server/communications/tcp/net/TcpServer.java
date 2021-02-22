package com.ogya.dms.server.communications.tcp.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.server.common.DmsSecurity;

public final class TcpServer implements Runnable {

	private final int port;

	private final AtomicReference<ServerSocket> serverSocketRef = new AtomicReference<ServerSocket>(null);

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

	public TcpServer(int port) {

		this.port = port;

	}

	public void addListener(TcpServerListener listener) {

		listeners.add(listener);

	}

	public void start() {

		if (isAlive())
			return;

		try {

			serverSocketRef.set(DmsSecurity.newSecureServerSocket(port));

			serverStartedToListeners();

		} catch (Exception e) {

			serverFailedToListeners();

		}

	}

	public boolean isAlive() {

		ServerSocket serverSocket = serverSocketRef.get();

		return serverSocket != null && !serverSocket.isClosed();

	}

	public void acceptConnection() {

		new Thread(this).start();

	}

	public boolean isConnected(int id) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		return tcpConnection != null && tcpConnection.isConnected();

	}

	public boolean sendMessage(int id, byte[] message) {

		TcpConnection tcpConnection = tcpConnections.get(id);

		if (tcpConnection == null)
			return false;

		return tcpConnection.sendMessage(message);

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

		ServerSocket serverSocket = serverSocketRef.get();

		if (serverSocket == null)
			return;

		try {

			serverSocket.close();

		} catch (IOException e) {

		}

		serverSocketRef.set(null);

	}

	private void serverStartedToListeners() {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.serverStarted()));

	}

	private void serverFailedToListeners() {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.serverFailed()));

	}

	private void connectedToListeners(final int id) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.connected(id)));

	}

	private void disconnectedToListeners(final int id) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.disconnected(id)));

	}

	private void messageReceivedToListeners(final int id, final byte[] message) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.messageReceived(id, message)));

	}

	@Override
	public void run() {

		try (Socket socket = serverSocketRef.get().accept()) {

			socket.setKeepAlive(false);

			int id = idRef.getAndIncrement();

			TcpConnection tcpConnection = new TcpConnection(socket, message -> messageReceivedToListeners(id, message));
			tcpConnections.put(id, tcpConnection);

			connectedToListeners(id);

			tcpConnection.listen();

			tcpConnections.remove(id);

			disconnectedToListeners(id);

		} catch (Exception e) {

			serverFailedToListeners();

		}

	}

}

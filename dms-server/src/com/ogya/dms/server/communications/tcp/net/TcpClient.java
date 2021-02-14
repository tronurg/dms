package com.ogya.dms.server.communications.tcp.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class TcpClient implements MessageListener, Runnable {

	private final InetAddress serverIp;
	private final int serverPort;
	private final InetAddress localIp;
	private final int localPort;

	private final List<TcpClientListener> listeners = Collections.synchronizedList(new ArrayList<TcpClientListener>());

	private final AtomicReference<TcpConnection> tcpConnectionRef = new AtomicReference<TcpConnection>(null);

	private final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {

			Thread thread = new Thread(r);

			thread.setDaemon(true);

			return thread;

		}

	});

	public TcpClient(InetAddress ip, int port) {

		this(ip, port, null, 0);

	}

	public TcpClient(InetAddress ip, int port, InetAddress localIp, int localPort) {

		this.serverIp = ip;
		this.serverPort = port;
		this.localIp = localIp;
		this.localPort = localPort;

	}

	public void addListener(TcpClientListener listener) {

		listeners.add(listener);

	}

	public void connect() {

		if (isConnected())
			return;

		new Thread(this).start();

	}

	public boolean isConnected() {

		TcpConnection tcpConnection = tcpConnectionRef.get();

		return tcpConnection != null && tcpConnection.isConnected();

	}

	public boolean sendMessage(byte[] message, int chunkSize, AtomicBoolean sendCheck, Runnable success) {

		TcpConnection tcpConnection = tcpConnectionRef.get();

		if (tcpConnection == null)
			return false;

		return tcpConnection.sendMessage(message, chunkSize, sendCheck, success);

	}

	public InetAddress getRemoteAddress() {

		TcpConnection tcpConnection = tcpConnectionRef.get();

		if (tcpConnection == null)
			return null;

		return tcpConnection.getRemoteAddress();

	}

	public int getRemotePort() {

		TcpConnection tcpConnection = tcpConnectionRef.get();

		if (tcpConnection == null)
			return 0;

		return tcpConnection.getRemotePort();

	}

	public InetAddress getLocalAddress() {

		TcpConnection tcpConnection = tcpConnectionRef.get();

		if (tcpConnection == null)
			return null;

		return tcpConnection.getLocalAddress();

	}

	public int getLocalPort() {

		TcpConnection tcpConnection = tcpConnectionRef.get();

		if (tcpConnection == null)
			return 0;

		return tcpConnection.getLocalPort();

	}

	public void disconnect() {

		TcpConnection tcpConnection = tcpConnectionRef.get();

		if (tcpConnection == null)
			return;

		tcpConnection.close();

	}

	private void connectedToListeners() {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.connected()));

	}

	private void couldNotConnectToListeners() {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.couldNotConnect()));

	}

	private void disconnectedToListeners() {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.disconnected()));

	}

	@Override
	public void messageReceived(final byte[] message) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.messageReceived(message)));

	}

	@Override
	public void fileReceived(final Path path) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.fileReceived(path)));

	}

	@Override
	public void run() {

		try (Socket socket = new Socket(serverIp, serverPort, localIp, localPort)) {

			socket.setKeepAlive(false);

			TcpConnection tcpConnection = new TcpConnection(socket, this);
			tcpConnectionRef.set(tcpConnection);

			connectedToListeners();

			tcpConnection.listen();

			tcpConnectionRef.set(null);

			disconnectedToListeners();

		} catch (IOException e) {

			couldNotConnectToListeners();

		}

	}

}

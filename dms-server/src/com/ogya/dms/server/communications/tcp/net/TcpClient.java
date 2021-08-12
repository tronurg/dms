package com.ogya.dms.server.communications.tcp.net;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.ogya.dms.server.common.DmsSecurity;

public final class TcpClient implements Runnable {

	private final InetAddress serverIp;
	private final int serverPort;
	private final InetAddress localIp;
	private final int localPort;

	private final List<TcpClientListener> listeners = Collections.synchronizedList(new ArrayList<TcpClientListener>());

	private final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			return thread;
		}

	});

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

		new Thread(this).start();

	}

	private void connectedToListeners(final TcpConnection tcpConnection) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.connected(tcpConnection)));

	}

	private void couldNotConnectToListeners() {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.couldNotConnect()));

	}

	private void disconnectedToListeners() {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.disconnected()));

	}

	public void messageReceivedToListeners(final int messageNumber, final byte[] message) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.messageReceived(messageNumber, message)));

	}

	@Override
	public void run() {

		try (Socket socket = DmsSecurity.newSecureSocket(serverIp, serverPort, localIp, localPort)) {
			TcpConnection tcpConnection = new TcpConnection(socket, this::messageReceivedToListeners);
			connectedToListeners(tcpConnection);
			tcpConnection.listen();
			disconnectedToListeners();
		} catch (Exception e) {
			couldNotConnectToListeners();
		}

	}

}

package com.ogya.dms.server.communications.tcp.net;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.common.DmsSecurity;

public final class TcpServer implements Runnable {

	private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

	private final int port;

	private final AtomicInteger idRef = new AtomicInteger(0);

	private final List<TcpServerListener> listeners = Collections.synchronizedList(new ArrayList<TcpServerListener>());

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

		THREAD_POOL.execute(this);

	}

	private void connectedToListeners(final int id, final TcpConnection tcpConnection) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.connected(id, tcpConnection)));

	}

	private void disconnectedToListeners(final int id) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.disconnected(id)));

	}

	private void messageReceivedToListeners(final int id, final int messageNumber, final byte[] message) {

		taskQueue.execute(() -> listeners.forEach(listener -> listener.messageReceived(id, messageNumber, message)));

	}

	@Override
	public void run() {

		while (!Thread.currentThread().isInterrupted()) {

			try (ServerSocket serverSocket = DmsSecurity.newSecureServerSocket(port)) {
				acceptConnections(serverSocket);
			} catch (Exception e) {

			}

			try {
				Thread.sleep(CommonConstants.CONN_TIMEOUT_MS);
			} catch (InterruptedException e) {

			}

		}

	}

	private void acceptConnections(ServerSocket serverSocket) throws Exception {

		while (!Thread.currentThread().isInterrupted()) {
			final Socket socket = serverSocket.accept();
			THREAD_POOL.execute(() -> connectionEstablished(socket));
		}

	}

	private void connectionEstablished(Socket socket) {

		int id = idRef.getAndIncrement();

		try (TcpConnection tcpConnection = new TcpConnection(socket,
				(messageNumber, message) -> messageReceivedToListeners(id, messageNumber, message))) {
			connectedToListeners(id, tcpConnection);
			tcpConnection.listen();
			disconnectedToListeners(id);
		} catch (Exception e) {

		}

	}

}

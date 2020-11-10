package com.ogya.dms.server.communications.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.ogya.communications.tcp.TcpClient;
import com.ogya.communications.tcp.TcpClientListener;
import com.ogya.communications.tcp.TcpServer;
import com.ogya.communications.tcp.TcpServerListener;
import com.ogya.dms.server.common.Encryption;
import com.ogya.dms.server.communications.intf.TcpManagerListener;
import com.ogya.dms.server.factory.DmsFactory;

public class TcpManager implements TcpServerListener {

	private static final int CHUNK_SIZE = 1024;

	private final int serverPort;
	private final int clientPortFrom;
	private final int clientPortTo;

	private final AtomicInteger nextPort = new AtomicInteger(0);

	private final TcpServer tcpServer;

	private final Map<Integer, InetAddress> serverIdAddress = Collections
			.synchronizedMap(new HashMap<Integer, InetAddress>());
	private final Map<String, DmsServer> dmsServers = Collections.synchronizedMap(new HashMap<String, DmsServer>());
	private final Map<InetAddress, Connection> connections = Collections
			.synchronizedMap(new HashMap<InetAddress, Connection>());

	private final List<TcpManagerListener> listeners = Collections
			.synchronizedList(new ArrayList<TcpManagerListener>());

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	public TcpManager(int serverPort, int clientPortFrom, int clientPortTo) throws IOException {

		this.serverPort = serverPort;
		this.clientPortFrom = clientPortFrom;
		this.clientPortTo = clientPortTo;

		nextPort.set(clientPortFrom);

		tcpServer = new TcpServer(serverPort);

		tcpServer.setBlocking(true);
		tcpServer.setKeepAlive(false);

		tcpServer.addListener(this);

		tcpServer.acceptConnection();

	}

	public void addListener(TcpManagerListener listener) {

		listeners.add(listener);

	}

	public void addConnection(final String dmsUuid, final InetAddress address, final TcpConnectionType connectionType) {

		taskQueue.execute(() -> {

			dmsServers.putIfAbsent(dmsUuid, new DmsServer(dmsUuid));

			final DmsServer dmsServer = dmsServers.get(dmsUuid);

			if (!connections.containsKey(address)) {

				Connection connection = new Connection(address);
				connection.dmsServer = dmsServer;

				connections.put(address, connection);

				if (Objects.equals(connectionType, TcpConnectionType.CLIENT)) {

					try {

						int port = claimPort();

						TcpClient tcpClient = new TcpClient(address, serverPort, null, port);

						tcpClient.setBlocking(true);
						tcpClient.setKeepAlive(false);

						tcpClient.addListener(new TcpClientListener() {

							@Override
							public void messageReceived(String arg0) {

								taskQueue.execute(() -> {

									messageReceivedToListeners(arg0, dmsUuid);

								});

							}

							@Override
							public void connected() {

								taskQueue.execute(() -> {

									connection.sendFunction = tcpClient::sendMessage;
									dmsServer.connections.add(connection);

									serverConnectionsUpdated(dmsServer);

								});

							}

							@Override
							public void couldNotConnect() {

								taskQueue.execute(() -> {

									connections.remove(address);

								});

							}

							@Override
							public void disconnected() {

								taskQueue.execute(() -> {

									connections.remove(address);
									dmsServer.connections.remove(connection);

									serverConnectionsUpdated(dmsServer);

								});

							}

						});

						tcpClient.connect();

					} catch (NoAvailablePortException e) {

						connections.remove(address);

					}

				}

			}

			final Connection connection = connections.get(address);

			if (connection != null && connection.dmsServer == null) {
				// Connection sunucu tarafindan olusturulmus
				// Iliskiler guncellenecek

				connection.dmsServer = dmsServer;
				dmsServer.connections.add(connection);

				serverConnectionsUpdated(dmsServer);

				while (!connection.waitingMessages.isEmpty()) {

					messageReceivedToListeners(connection.waitingMessages.poll(), dmsUuid);

				}

			}

		});

	}

	public void sendMessageToServer(final String dmsUuid, final String message, final AtomicBoolean sendStatus,
			final Consumer<Integer> progressMethod) {

		taskQueue.execute(() -> {

			DmsServer dmsServer = dmsServers.get(dmsUuid);

			if (dmsServer == null)
				return;

			try {

				String encryptedMessage = Encryption.compressAndEncryptToString(message);

				sendMessageToServer(dmsServer, encryptedMessage, sendStatus, progressMethod);

			} catch (Exception e) {

				if (progressMethod != null)
					progressMethod.accept(-1);

				e.printStackTrace();

			}

		});

	}

	public void sendMessageToAllServers(final String message) {

		taskQueue.execute(() -> {

			if (dmsServers.isEmpty())
				return;

			try {

				String encryptedMessage = Encryption.compressAndEncryptToString(message);

				dmsServers
						.forEach((dmsUuid, dmsServer) -> sendMessageToServer(dmsServer, encryptedMessage, null, null));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	private int claimPort() throws NoAvailablePortException {

		int port = nextPort.get();
		boolean isPortFound = false;

		for (int i = 0; i < (clientPortTo - clientPortFrom + 1); ++i) {

			try (Socket testSocket = new Socket()) {
				testSocket.bind(new InetSocketAddress(port));
				isPortFound = true;
				break;
			} catch (IOException e) {

			}

			++port;
			if (port > clientPortTo)
				port = clientPortFrom;

		}

		if (!isPortFound)
			throw new NoAvailablePortException();

		int portFound = port;

		nextPort.set(++port > clientPortTo ? clientPortFrom : port);

		return portFound;

	}

	private void sendMessageToServer(final DmsServer dmsServer, final String message, final AtomicBoolean sendStatus,
			final Consumer<Integer> progressMethod) {

		dmsServer.taskQueue.execute(() -> {

			// Send a progress of zero before starting
			if (progressMethod != null)
				progressMethod.accept(0);

			final int messageLength = message.length();

			boolean sent = false;

			synchronized (dmsServer.connections) {

				for (Connection connection : dmsServer.connections) {

					if (connection.sendFunction == null)
						continue;

					final AtomicInteger progress = new AtomicInteger(0);
					final AtomicInteger progressPercent = new AtomicInteger(0);

					sent = connection.sendFunction.send(message, CHUNK_SIZE, sendStatus,
							progressMethod == null ? null : () -> {

								int bytesSent = progress.addAndGet(CHUNK_SIZE);
								int bytesSentPercent = 100 * bytesSent / messageLength;

								if (bytesSentPercent > progressPercent.get() && bytesSentPercent < 100) {

									progressPercent.set(bytesSentPercent);

									progressMethod.accept(bytesSentPercent);

								}

							});

					if (sent)
						break;

				}

			}

			if (progressMethod == null)
				return;

			if (sent)
				progressMethod.accept(100);
			else
				progressMethod.accept(-1);

		});

	}

	private void serverConnectionsUpdated(DmsServer dmsServer) {

		List<InetAddress> addresses = new ArrayList<InetAddress>();

		dmsServer.connections.forEach(connection -> addresses.add(connection.remoteAddress));

		listeners.forEach(listener -> listener.serverConnectionsUpdated(dmsServer.dmsUuid, addresses));

		if (dmsServer.connections.size() == 0) {
			// remote server disconnected

			dmsServers.remove(dmsServer.dmsUuid).close();

		}

	}

	private void messageReceivedToListeners(String message, String dmsUuid) {

		try {

			String decryptedMessage = Encryption.decryptAndDecompressFromString(message);

			listeners.forEach(e -> e.messageReceivedFromRemoteServer(decryptedMessage, dmsUuid));

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	@Override
	public void disconnected(int id) {

		taskQueue.execute(() -> {

			InetAddress address = serverIdAddress.remove(id);

			if (address == null)
				return;

			Connection connection = connections.remove(address);

			if (connection == null)
				return;

			DmsServer dmsServer = connection.dmsServer;

			if (dmsServer == null)
				return;

			if (dmsServer.connections.contains(connection)) {

				dmsServer.connections.remove(connection);

				serverConnectionsUpdated(dmsServer);

			}

		});

	}

	@Override
	public void connected(final int id) {

		taskQueue.execute(() -> {

			InetAddress address = tcpServer.getRemoteAddress(id);

			serverIdAddress.put(id, address);

			connections.putIfAbsent(address, new Connection(address));

			Connection connection = connections.get(address);

			connection.sendFunction = (message, chunkSize, sendCheck, success) -> tcpServer.sendMessage(id, message,
					chunkSize, sendCheck, success);

			DmsServer dmsServer = connection.dmsServer;

			if (dmsServer != null) {
				// connection alinan bir uuid ile olusturulmus
				// connectionsi guncelleyip kontrol et

				dmsServer.connections.add(connection);

				serverConnectionsUpdated(dmsServer);

			}

		});

		tcpServer.acceptConnection();

	}

	@Override
	public void messageReceived(int arg0, String arg1) {

		taskQueue.execute(() -> {

			InetAddress address = serverIdAddress.get(arg0);

			if (address == null)
				return;

			Connection connection = connections.get(address);

			if (connection == null)
				return;

			DmsServer dmsServer = connection.dmsServer;

			if (dmsServer == null) {

				connection.waitingMessages.offer(arg1);

			} else {

				messageReceivedToListeners(arg1, dmsServer.dmsUuid);

			}

		});

	}

	private static class Connection {

		private static final AtomicInteger ORDER = new AtomicInteger(0);

		final int order;

		final InetAddress remoteAddress;

		final Queue<String> waitingMessages = new ArrayDeque<String>();

		DmsServer dmsServer;

		SendFunction sendFunction;

		Connection(InetAddress remoteAddress) {
			order = ORDER.getAndIncrement();
			this.remoteAddress = remoteAddress;
		}

		int getPingTime() {
			int pingTime = 1000;
			try {
				long startTimeNanos = System.nanoTime();
				remoteAddress.isReachable(pingTime);
				long endTimeNanos = System.nanoTime();
				pingTime = (int) (endTimeNanos - startTimeNanos);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return pingTime;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof Connection))
				return false;
			return Objects.equals(((Connection) obj).remoteAddress, remoteAddress);
		}

	}

	private class DmsServer {

		final String dmsUuid;

		final Set<Connection> connections = Collections
				.synchronizedSortedSet(new TreeSet<Connection>(new Comparator<Connection>() {

					@Override
					public int compare(Connection arg0, Connection arg1) {
						if (Objects.equals(arg0, arg1))
							return 0;
						int pingTime0 = arg0.getPingTime();
						int pingTime1 = arg1.getPingTime();
						if (pingTime0 == pingTime1)
							return (int) Math.signum(arg0.order - arg1.order);
						return (int) Math.signum(pingTime0 - pingTime1);
					}

				}));

		protected final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

		DmsServer(String dmsUuid) {

			this.dmsUuid = dmsUuid;

		}

		void close() {

			taskQueue.shutdown();

		}

	}

	private interface SendFunction {

		boolean send(String message, int chunkSize, AtomicBoolean sendCheck, Runnable successMethod);

	}

	private class NoAvailablePortException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public NoAvailablePortException() {
			super("No available port found");
		}

	}

}

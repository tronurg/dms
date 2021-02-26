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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ogya.dms.commons.DmsMessageFactory;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.communications.intf.TcpManagerListener;
import com.ogya.dms.server.communications.tcp.net.TcpClient;
import com.ogya.dms.server.communications.tcp.net.TcpClientListener;
import com.ogya.dms.server.communications.tcp.net.TcpServer;
import com.ogya.dms.server.communications.tcp.net.TcpServerListener;
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

	public TcpManager(int serverPort, int clientPortFrom, int clientPortTo, TcpManagerListener listener) {

		this.serverPort = serverPort;
		this.clientPortFrom = clientPortFrom;
		this.clientPortTo = clientPortTo;

		nextPort.set(clientPortFrom);

		listeners.add(listener);

		tcpServer = new TcpServer(serverPort);

		tcpServer.addListener(this);

		tcpServer.start();

	}

	public void addConnection(final String dmsUuid, final InetAddress address, final TcpConnectionType connectionType) {

		taskQueue.execute(() -> {

			dmsServers.putIfAbsent(dmsUuid, new DmsServer(dmsUuid));

			final DmsServer dmsServer = dmsServers.get(dmsUuid);

			if (!connections.containsKey(address)) {

				Connection connection = new Connection(address, this::messageReceivedFromConnection);
				connection.dmsServer = dmsServer;

				connections.put(address, connection);

				if (Objects.equals(connectionType, TcpConnectionType.CLIENT)) {

					try {

						int port = claimPort();

						TcpClient tcpClient = new TcpClient(address, serverPort, null, port);

						tcpClient.addListener(new TcpClientListener() {

							@Override
							public void messageReceived(byte[] message) {

								taskQueue.execute(() -> {

									connection.messageFactory.inFeed(message);

								});

							}

							@Override
							public void connected() {

								taskQueue.execute(() -> {

									connection.localAddress = tcpClient.getLocalAddress();
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

									connections.remove(address).messageFactory.deleteResources();
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

	public void sendMessageToServer(final String dmsUuid, final MessagePojo messagePojo, final AtomicBoolean sendStatus,
			final Consumer<Integer> progressConsumer) {

		taskQueue.execute(() -> {

			DmsServer dmsServer = dmsServers.get(dmsUuid);

			if (dmsServer == null)
				return;

			sendMessageToServer(dmsServer, messagePojo, sendStatus == null ? new AtomicBoolean(true) : sendStatus,
					progressConsumer);

		});

	}

	public void sendMessageToAllServers(final MessagePojo messagePojo) {

		taskQueue.execute(() -> {

			dmsServers.forEach(
					(dmsUuid, dmsServer) -> sendMessageToServer(dmsServer, messagePojo, new AtomicBoolean(true), null));

		});

	}

	public void testAllServers() {

		taskQueue.execute(() -> {

			dmsServers.forEach((dmsUuid, dmsServer) -> {

				dmsServer.taskQueue.execute(() -> {

					synchronized (dmsServer.connections) {

						for (Connection connection : dmsServer.connections) {

							if (connection.sendFunction != null)
								connection.sendFunction.apply(new byte[0]);

						}

					}

				});

			});

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

	private void sendMessageToServer(final DmsServer dmsServer, final MessagePojo messagePojo,
			final AtomicBoolean sendStatus, final Consumer<Integer> progressConsumer) {

		dmsServer.taskQueue.execute(() -> {

			final long startTime = System.currentTimeMillis();

			final AtomicBoolean health = new AtomicBoolean(true);

			final AtomicBoolean sent = new AtomicBoolean(false);

			synchronized (dmsServer.connections) {

				for (Connection connection : dmsServer.connections) {

					health.set(sendStatus.get() && (messagePojo.useTimeout == null
							|| System.currentTimeMillis() - startTime < messagePojo.useTimeout));

					if (!health.get())
						break;

					if (!(messagePojo.useLocalAddress == null
							|| messagePojo.useLocalAddress.equals(connection.localAddress)))
						continue;

					if (connection.sendFunction == null)
						continue;

					sent.set(true); // Hypothesis

					final AtomicInteger progressPercent = new AtomicInteger(-1);

					DmsMessageFactory.outFeedRemote(messagePojo, CHUNK_SIZE, health, (data, progress) -> {

						if (!sent.get())
							return;

						sent.set(connection.sendFunction.apply(data));

						health.set(sendStatus.get()
								&& (messagePojo.useTimeout == null
										|| System.currentTimeMillis() - startTime < messagePojo.useTimeout)
								&& sent.get());

						if (!sent.get())
							return;

						boolean progressUpdated = progress > progressPercent.get();

						if (progressUpdated)
							progressPercent.set(progress);

						if (progressConsumer == null)
							return;

						if (progressUpdated)
							progressConsumer.accept(progress);

					});

					if (progressPercent.get() < 100)
						sent.set(false);

					if (sent.get())
						break;

				}

			}

			if (progressConsumer == null)
				return;

			if (!sent.get())
				progressConsumer.accept(-1);

		});

	}

	private void serverConnectionsUpdated(DmsServer dmsServer) {

		List<InetAddress> remoteAddresses = new ArrayList<InetAddress>();
		List<InetAddress> localAddresses = new ArrayList<InetAddress>();

		dmsServer.connections.forEach(connection -> {
			remoteAddresses.add(connection.remoteAddress);
			localAddresses.add(connection.localAddress);
		});

		listeners.forEach(
				listener -> listener.serverConnectionsUpdated(dmsServer.dmsUuid, remoteAddresses, localAddresses));

		if (dmsServer.connections.size() == 0) {
			// remote server disconnected

			dmsServers.remove(dmsServer.dmsUuid).close();

		}

	}

	private void messageReceivedFromConnection(MessagePojo messagePojo, Connection connection) {

		DmsServer dmsServer = connection.dmsServer;

		if (dmsServer == null) {

			connection.waitingMessages.offer(messagePojo);

		} else {

			messageReceivedToListeners(messagePojo, dmsServer.dmsUuid);

		}

	}

	private void messageReceivedToListeners(MessagePojo messagePojo, String dmsUuid) {

		listeners.forEach(e -> e.messageReceivedFromRemoteServer(messagePojo, dmsUuid));

	}

	@Override
	public void serverStarted() {

		tcpServer.acceptConnection();

	}

	@Override
	public void serverFailed() {

		new Thread(() -> {

			try {

				Thread.sleep(CommonConstants.CONN_TIMEOUT_MS);

			} catch (InterruptedException e) {

			}

			if (tcpServer.isAlive())
				tcpServer.acceptConnection();
			else
				tcpServer.start();

		}).start();

	}

	@Override
	public void disconnected(final int id) {

		taskQueue.execute(() -> {

			InetAddress address = serverIdAddress.remove(id);

			if (address == null)
				return;

			Connection connection = connections.get(address);

			if (connection == null || connection.id != id)
				return;

			connections.remove(address).messageFactory.deleteResources();

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

			connections.putIfAbsent(address, new Connection(address, this::messageReceivedFromConnection));

			Connection connection = connections.get(address);
			connection.localAddress = tcpServer.getLocalAddress(id);

			connection.id = id;

			connection.sendFunction = message -> tcpServer.sendMessage(id, message);

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
	public void messageReceived(final int id, final byte[] message) {

		taskQueue.execute(() -> {

			InetAddress address = serverIdAddress.get(id);

			if (address == null)
				return;

			Connection connection = connections.get(address);

			if (connection == null)
				return;

			connection.messageFactory.inFeed(message);

		});

	}

	private static class Connection {

		private static final AtomicInteger ORDER = new AtomicInteger(0);

		private final DmsMessageFactory messageFactory;

		private final int order;

		private final InetAddress remoteAddress;
		private InetAddress localAddress;

		private int id = -1;

		private final Queue<MessagePojo> waitingMessages = new ArrayDeque<MessagePojo>();

		private DmsServer dmsServer;

		private Function<byte[], Boolean> sendFunction;

		private Connection(InetAddress remoteAddress, BiConsumer<MessagePojo, Connection> messageConsumer) {
			this.messageFactory = new DmsMessageFactory(messagePojo -> messageConsumer.accept(messagePojo, this));
			order = ORDER.getAndIncrement();
			this.remoteAddress = remoteAddress;
		}

		private int getPingTime() {
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

		private final String dmsUuid;

		private final Set<Connection> connections = Collections
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

		private DmsServer(String dmsUuid) {

			this.dmsUuid = dmsUuid;

		}

		private void close() {

			taskQueue.shutdown();

		}

	}

	private class NoAvailablePortException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private NoAvailablePortException() {
			super("No available port found");
		}

	}

}

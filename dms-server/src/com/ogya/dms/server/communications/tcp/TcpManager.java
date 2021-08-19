package com.ogya.dms.server.communications.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ogya.dms.commons.DmsMessageFactory;
import com.ogya.dms.commons.DmsMessageFactory.Chunk;
import com.ogya.dms.commons.DmsMessageFactory.MessageSender;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.common.CommonMethods;
import com.ogya.dms.server.communications.intf.TcpManagerListener;
import com.ogya.dms.server.communications.tcp.net.TcpClient;
import com.ogya.dms.server.communications.tcp.net.TcpClientListener;
import com.ogya.dms.server.communications.tcp.net.TcpConnection;
import com.ogya.dms.server.communications.tcp.net.TcpServer;
import com.ogya.dms.server.communications.tcp.net.TcpServerListener;
import com.ogya.dms.server.factory.DmsFactory;

public class TcpManager implements TcpServerListener {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

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
	private final Set<InetAddress> connectedAddresses = new HashSet<InetAddress>();

	private final List<TcpManagerListener> listeners = Collections
			.synchronizedList(new ArrayList<TcpManagerListener>());

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	private final Comparator<Connection> connectionSorter = new Comparator<Connection>() {

		@Override
		public int compare(Connection arg0, Connection arg1) {
			if (Objects.equals(arg0, arg1))
				return 0;
			int healthPriority0 = arg0.tcpConnection.isHealthy() ? 0 : 1;
			int healthPriority1 = arg1.tcpConnection.isHealthy() ? 0 : 1;
			if (healthPriority0 != healthPriority1)
				return Integer.compare(healthPriority0, healthPriority1);
			int connectionPriority0 = arg0.priority;
			int connectionPriority1 = arg1.priority;
			if (connectionPriority0 != connectionPriority1)
				return Integer.compare(connectionPriority0, connectionPriority1);
			long latency0 = arg0.tcpConnection.getLatency();
			long latency1 = arg1.tcpConnection.getLatency();
			if (latency0 != latency1)
				return Long.compare(latency0, latency1);
			return Integer.compare(arg0.order, arg1.order);
		}

	};

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

			if (connections.containsKey(address) || Objects.equals(connectionType, TcpConnectionType.SERVER))
				return;

			try {

				int port = claimPort();

				connections.put(address, null);

				TcpClient tcpClient = new TcpClient(address, serverPort, null, port);

				tcpClient.addListener(new TcpClientListener() {

					@Override
					public void messageReceived(int messageNumber, byte[] message) {

						taskQueue.execute(() -> {

							Connection connection = connections.get(address);
							if (connection == null)
								return;

							connection.messageFactory.inFeed(messageNumber, message);

						});

					}

					@Override
					public void connected(final TcpConnection tcpConnection) {

						taskQueue.execute(() -> {

							tcpConnection.sendMessage(-1, CommonConstants.DMS_UUID.getBytes(CHARSET));

							Connection connection = new Connection(tcpConnection, -1,
									TcpManager.this::messageReceivedFromConnection);
							connections.put(address, connection);

							DmsServer dmsServer = dmsServers.get(dmsUuid);
							if (dmsServer == null) {
								dmsServer = new DmsServer(dmsUuid);
								dmsServers.put(dmsUuid, dmsServer);
							}
							connection.dmsServer = dmsServer;
							dmsServer.connections.add(connection);
							serverConnectionsUpdated(dmsServer, true);

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

							Connection connection = connections.remove(address);
							if (connection == null)
								return;

							connection.messageFactory.deleteResources();

							DmsServer dmsServer = dmsServers.get(dmsUuid);
							if (dmsServer == null)
								return;

							if (dmsServer.connections.remove(connection)) {
								serverConnectionsUpdated(dmsServer, false);
							}

						});

					}

				});

				tcpClient.connect();

			} catch (NoAvailablePortException e) {

			}

		});

	}

	public void sendMessageToServer(final String dmsUuid, final MessagePojo messagePojo, final AtomicBoolean sendStatus,
			final Consumer<Integer> progressConsumer) {

		taskQueue.execute(() -> {

			DmsServer dmsServer = dmsServers.get(dmsUuid);

			if (dmsServer != null) {
				sendMessageToServer(dmsServer, messagePojo, sendStatus == null ? new AtomicBoolean(true) : sendStatus,
						progressConsumer);
			} else if (progressConsumer != null) {
				progressConsumer.accept(-1);
			}

		});

	}

	public void sendMessageToAllServers(final MessagePojo messagePojo) {

		taskQueue.execute(() -> {

			dmsServers.forEach(
					(dmsUuid, dmsServer) -> sendMessageToServer(dmsServer, messagePojo, new AtomicBoolean(true), null));

		});

	}

	public Set<InetAddress> getConnectedAddresses() {

		connectedAddresses.clear();
		connections.forEach((address, connection) -> {
			if (connection == null)
				return;
			connectedAddresses.add(address);
		});
		return connectedAddresses;

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

			final int messageNumber = dmsServer.messageCounter.getAndIncrement();
			final long startTime = System.currentTimeMillis();
			final AtomicBoolean health = new AtomicBoolean(true);
			final AtomicBoolean sent = new AtomicBoolean(false);

			synchronized (dmsServer.connections) {

				dmsServer.connections.sort(connectionSorter);

				for (Connection connection : dmsServer.connections) {

					health.set(sendStatus.get() && (messagePojo.useTimeout == null
							|| System.currentTimeMillis() - startTime < messagePojo.useTimeout));

					if (!health.get())
						break;

					if (!(messagePojo.useLocalAddress == null
							|| messagePojo.useLocalAddress.equals(connection.tcpConnection.getLocalAddress())))
						continue;

					sent.set(true); // Hypothesis

					final AtomicInteger progressPercent = new AtomicInteger(-1);

					MessageSender messageSender = DmsMessageFactory.outFeedRemote(messagePojo, health);

					while (messageSender.hasNext()) {

						Chunk chunk = messageSender.next();

						if (!sent.get())
							continue;

						sent.set(connection.tcpConnection.sendMessage(messageNumber, chunk.data));

						health.set(sendStatus.get()
								&& (messagePojo.useTimeout == null
										|| System.currentTimeMillis() - startTime < messagePojo.useTimeout)
								&& sent.get());

						if (!sent.get())
							continue;

						boolean progressUpdated = chunk.progress > progressPercent.get();

						if (progressUpdated)
							progressPercent.set(chunk.progress);

						if (progressConsumer == null)
							continue;

						if (progressUpdated)
							progressConsumer.accept(chunk.progress);

					}

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

	private void serverConnectionsUpdated(DmsServer dmsServer, boolean beaconsRequested) {

		Map<InetAddress, InetAddress> localRemoteIps = new HashMap<InetAddress, InetAddress>();

		dmsServer.connections.forEach(connection -> localRemoteIps.put(connection.tcpConnection.getLocalAddress(),
				connection.tcpConnection.getRemoteAddress()));

		listeners.forEach(
				listener -> listener.serverConnectionsUpdated(dmsServer.dmsUuid, localRemoteIps, beaconsRequested));

		if (dmsServer.connections.size() == 0) {
			// remote server disconnected
			dmsServers.remove(dmsServer.dmsUuid).close();
		}

	}

	private void messageReceivedFromConnection(MessagePojo messagePojo, Connection connection) {

		DmsServer dmsServer = connection.dmsServer;

		if (dmsServer == null)
			return;

		messageReceivedToListeners(messagePojo, dmsServer.dmsUuid);

	}

	private void messageReceivedToListeners(MessagePojo messagePojo, String dmsUuid) {

		listeners.forEach(e -> e.messageReceivedFromRemoteServer(messagePojo, dmsUuid));

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

			if (dmsServer != null && dmsServer.connections.remove(connection)) {
				serverConnectionsUpdated(dmsServer, false);
			}

		});

	}

	@Override
	public void connected(final int id, final TcpConnection tcpConnection) {

		taskQueue.execute(() -> {

			InetAddress address = tcpConnection.getRemoteAddress();

			serverIdAddress.put(id, address);

			Connection connection = connections.get(address);
			DmsServer dmsServer = null;

			if (connection != null) {
				dmsServer = connection.dmsServer;
				connection.tcpConnection.close();
				connection.messageFactory.deleteResources();
			}
			if (dmsServer != null && dmsServer.connections.remove(connection)) {
				serverConnectionsUpdated(dmsServer, false);
			}

			connections.put(address, new Connection(tcpConnection, id, this::messageReceivedFromConnection));

		});

	}

	@Override
	public void messageReceived(final int id, final int messageNumber, final byte[] message) {

		taskQueue.execute(() -> {

			InetAddress address = serverIdAddress.get(id);

			if (address == null)
				return;

			Connection connection = connections.get(address);

			if (connection == null)
				return;

			if (connection.dmsServer == null) {
				try {
					String dmsUuid = UUID.fromString(new String(message, CHARSET)).toString();
					DmsServer dmsServer = dmsServers.get(dmsUuid);
					if (dmsServer == null) {
						dmsServer = new DmsServer(dmsUuid);
						dmsServers.put(dmsUuid, dmsServer);
					}
					connection.dmsServer = dmsServer;
					dmsServer.connections.add(connection);
					serverConnectionsUpdated(dmsServer, true);
				} catch (Exception e) {

				}
			} else {
				connection.messageFactory.inFeed(messageNumber, message);
			}

		});

	}

	private static class Connection {

		private static final AtomicInteger ORDER = new AtomicInteger(0);

		private final DmsMessageFactory messageFactory;

		private final int order;
		private final int priority;

		private final TcpConnection tcpConnection;
		private final int id;

		private DmsServer dmsServer;

		private Connection(TcpConnection tcpConnection, int id, BiConsumer<MessagePojo, Connection> messageConsumer) {
			this.messageFactory = new DmsMessageFactory(messagePojo -> messageConsumer.accept(messagePojo, this));
			this.order = ORDER.getAndIncrement();
			this.priority = CommonMethods.getLocalAddressPriority(tcpConnection.getLocalAddress());
			this.tcpConnection = tcpConnection;
			this.id = id;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof Connection))
				return false;
			return Objects.equals(((Connection) obj).order, order);
		}

	}

	private class DmsServer {

		private final String dmsUuid;
		private final AtomicInteger messageCounter = new AtomicInteger(0);
		private final List<Connection> connections = Collections.synchronizedList(new ArrayList<Connection>());
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

package com.dms.server.communications.tcp;

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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.dms.server.communications.intf.TcpManagerListener;
import com.dms.server.communications.tcp.net.TcpClient;
import com.dms.server.communications.tcp.net.TcpClientListener;
import com.dms.server.communications.tcp.net.TcpConnection;
import com.dms.server.communications.tcp.net.TcpServer;
import com.dms.server.communications.tcp.net.TcpServerListener;
import com.dms.server.factory.DmsFactory;
import com.dms.server.structures.Chunk;
import com.dms.server.structures.RemoteChunk;
import com.dms.server.util.Commons;

public class TcpManager implements TcpServerListener {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static final AtomicInteger CONNECTION_ORDER = new AtomicInteger(0);

	private static final Comparator<Connection> CONNECTION_SORTER = new Comparator<Connection>() {

		@Override
		public int compare(Connection arg0, Connection arg1) {
			if (Objects.equals(arg0, arg1)) {
				return 0;
			}
			int healthPriority0 = arg0.tcpConnection.isHealthy() ? 0 : 1;
			int healthPriority1 = arg1.tcpConnection.isHealthy() ? 0 : 1;
			if (healthPriority0 != healthPriority1) {
				return Integer.compare(healthPriority0, healthPriority1);
			}
			int connectionPriority0 = arg0.priority;
			int connectionPriority1 = arg1.priority;
			if (connectionPriority0 != connectionPriority1) {
				return Integer.compare(connectionPriority0, connectionPriority1);
			}
			long latency0 = arg0.tcpConnection.getLatency();
			long latency1 = arg1.tcpConnection.getLatency();
			if (latency0 != latency1) {
				return Long.compare(latency0, latency1);
			}
			return Integer.compare(arg0.order, arg1.order);
		}

	};

	private static final RemoteChunk END_CHUNK = new RemoteChunk();

	private final int serverPort;
	private final int clientPortFrom;
	private final int clientPortTo;
	private final TcpManagerListener listener;

	private final AtomicInteger nextPort = new AtomicInteger(0);

	private final TcpServer tcpServer;

	private final Map<Integer, AddressPair> serverIdAddress = Collections
			.synchronizedMap(new HashMap<Integer, AddressPair>());
	private final Map<String, DmsServer> dmsServers = Collections.synchronizedMap(new HashMap<String, DmsServer>());
	private final Map<AddressPair, Connection> connections = Collections
			.synchronizedMap(new HashMap<AddressPair, Connection>());
	private final Set<InetAddress> connectedAddresses = new HashSet<InetAddress>();

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	public TcpManager(int serverPort, int clientPortFrom, int clientPortTo, TcpManagerListener listener) {

		this.serverPort = serverPort;
		this.clientPortFrom = clientPortFrom;
		this.clientPortTo = clientPortTo;
		this.listener = listener;

		nextPort.set(clientPortFrom);

		tcpServer = new TcpServer(serverPort);
		tcpServer.addListener(this);
		tcpServer.start();

	}

	public void addConnection(final String dmsUuid, final InetAddress address, final TcpConnectionType connectionType) {

		taskQueue.execute(() -> {

			final AddressPair addrPair = new AddressPair(address, null);

			if (connections.containsKey(addrPair) || connectionType == TcpConnectionType.SERVER) {
				return;
			}

			try {

				int port = claimPort();

				connections.put(addrPair, null);

				TcpClient tcpClient = new TcpClient(address, serverPort, null, port);

				tcpClient.addListener(new TcpClientListener() {

					@Override
					public void messageReceived(int messageNumber, byte[] message) {

						taskQueue.execute(() -> {

							Connection connection = connections.get(addrPair);
							if (connection == null) {
								return;
							}

							DmsServer dmsServer = connection.dmsServer;
							if (dmsServer == null) {
								return;
							}

							listener.messageReceivedFromRemoteServer(messageNumber, message, dmsUuid);

						});

					}

					@Override
					public void connected(final TcpConnection tcpConnection) {

						taskQueue.execute(() -> {

							tcpConnection.sendMessage(-1, Commons.DMS_UUID.getBytes(CHARSET));

							Connection connection = new Connection(tcpConnection, -1);
							connections.put(addrPair, connection);

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

							connections.remove(addrPair);

						});

					}

					@Override
					public void disconnected() {

						taskQueue.execute(() -> {

							Connection connection = connections.remove(addrPair);
							if (connection == null) {
								return;
							}

							DmsServer dmsServer = dmsServers.get(dmsUuid);
							if (dmsServer == null) {
								return;
							}

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

	public void sendMessageToServer(final RemoteChunk chunk, final String dmsUuid) {

		taskQueue.execute(() -> {

			if (dmsUuid == null) {
				dmsServers.forEach((uuid, server) -> server.queueMessage(chunk));
				return;
			}
			DmsServer dmsServer = dmsServers.get(dmsUuid);
			if (dmsServer == null) {
				if (chunk.sendMore != null) {
					chunk.sendMore.accept(false);
				}
				return;
			}
			dmsServer.queueMessage(chunk);

		});

	}

	public Set<InetAddress> getConnectedAddresses() {

		connectedAddresses.clear();
		connections.forEach((addrPair, connection) -> {
			if (connection == null) {
				return;
			}
			connectedAddresses.add(addrPair.remoteAddress);
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
			if (port > clientPortTo) {
				port = clientPortFrom;
			}

		}

		if (!isPortFound) {
			throw new NoAvailablePortException();
		}

		int portFound = port;

		nextPort.set(++port > clientPortTo ? clientPortFrom : port);

		return portFound;

	}

	private void serverConnectionsUpdated(DmsServer dmsServer, boolean beaconsRequested) {

		Map<InetAddress, InetAddress> localRemoteIps = new HashMap<InetAddress, InetAddress>();

		dmsServer.connections.forEach(connection -> localRemoteIps.put(connection.tcpConnection.getLocalAddress(),
				connection.tcpConnection.getRemoteAddress()));

		listener.serverConnectionsUpdated(dmsServer.dmsUuid, localRemoteIps, beaconsRequested);

		if (dmsServer.connections.size() == 0) {
			// remote server disconnected
			dmsServers.remove(dmsServer.dmsUuid).close();
		}

	}

	@Override
	public void disconnected(final int id) {

		taskQueue.execute(() -> {

			AddressPair addrPair = serverIdAddress.remove(id);

			if (addrPair == null) {
				return;
			}

			Connection connection = connections.get(addrPair);

			if (connection == null || connection.id != id) {
				return;
			}

			connections.remove(addrPair);

			DmsServer dmsServer = connection.dmsServer;

			if (dmsServer != null && dmsServer.connections.remove(connection)) {
				serverConnectionsUpdated(dmsServer, false);
			}

		});

	}

	@Override
	public void connected(final int id, final TcpConnection tcpConnection) {

		taskQueue.execute(() -> {

			AddressPair addrPair = new AddressPair(tcpConnection.getRemoteAddress(), tcpConnection.getLocalAddress());

			serverIdAddress.put(id, addrPair);

			Connection connection = connections.get(addrPair);
			DmsServer dmsServer = null;

			if (connection != null) {
				dmsServer = connection.dmsServer;
				connection.tcpConnection.close();
			}
			if (dmsServer != null && dmsServer.connections.remove(connection)) {
				serverConnectionsUpdated(dmsServer, false);
			}

			connections.put(addrPair, new Connection(tcpConnection, id));

		});

	}

	@Override
	public void messageReceived(final int id, final int messageNumber, final byte[] message) {

		taskQueue.execute(() -> {

			AddressPair addrPair = serverIdAddress.get(id);

			if (addrPair == null) {
				return;
			}

			Connection connection = connections.get(addrPair);

			if (connection == null) {
				return;
			}

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
				listener.messageReceivedFromRemoteServer(messageNumber, message, connection.dmsServer.dmsUuid);
			}

		});

	}

	private class Connection {

		private final int order;
		private final int priority;

		private final TcpConnection tcpConnection;
		private final int id;

		private DmsServer dmsServer;

		private Connection(TcpConnection tcpConnection, int id) {
			this.order = CONNECTION_ORDER.getAndIncrement();
			this.priority = Commons.getLocalAddressPriority(tcpConnection.getLocalAddress());
			this.tcpConnection = tcpConnection;
			this.id = id;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof Connection)) {
				return false;
			}
			return ((Connection) obj).order == order;
		}

	}

	private class DmsServer {

		private final String dmsUuid;
		private final List<Connection> connections = Collections.synchronizedList(new ArrayList<Connection>());
		private final LinkedBlockingQueue<RemoteChunk> messageQueue = new LinkedBlockingQueue<RemoteChunk>();
		private final AtomicInteger updateCounter = new AtomicInteger();
		private TcpConnection bestConnection;
		private Chunk lastChunk;

		private DmsServer(String dmsUuid) {
			this.dmsUuid = dmsUuid;
			new Thread(this::consumeMessageQueue).start();
		}

		private void consumeMessageQueue() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					RemoteChunk chunk = messageQueue.take();
					if (chunk == END_CHUNK) {
						break;
					}

					boolean success = false;

					while (!success) {
						if (chunk.useLocalAddress != null) {
							TcpConnection connection = getConnection(chunk.useLocalAddress);
							if (connection == null) {
								break;
							}
							success = connection.sendMessage(chunk.messageNumber, chunk.data);
							if (!success) {
								break;
							}
						} else {
							boolean updated = checkBestConnection();
							if (bestConnection == null) {
								break;
							}
							if (updated && lastChunk != null) {
								success = bestConnection.sendMessage(lastChunk.messageNumber, lastChunk.data);
								if (!success) {
									bestConnection = null;
									continue;
								}
							}
							success = bestConnection.sendMessage(chunk.messageNumber, chunk.data);
							if (!success) {
								bestConnection = null;
								continue;
							}
							if (chunk.messageNumber > 0) {
								lastChunk = chunk;
							} else {
								lastChunk = null;
							}
						}
					}

					if (chunk.sendMore != null) {
						chunk.sendMore.accept(success);
					}
				} catch (Exception e) {

				}
			}
		}

		private void queueMessage(RemoteChunk chunk) {
			try {
				messageQueue.put(chunk);
			} catch (InterruptedException e) {

			}
		}

		private TcpConnection getConnection(InetAddress useLocalAddress) {
			synchronized (connections) {
				for (Connection connection : connections) {
					if (useLocalAddress.equals(connection.tcpConnection.getLocalAddress())) {
						return connection.tcpConnection;
					}
				}
			}
			return null;
		}

		private boolean checkBestConnection() {
			boolean updated = false;
			TcpConnection formerBestConnection = bestConnection;
			if (bestConnection == null || updateCounter.incrementAndGet() > 10) {
				updateCounter.set(0);
			}
			if (updateCounter.get() == 0) {
				synchronized (connections) {
					connections.sort(CONNECTION_SORTER);
					if (connections.isEmpty()) {
						bestConnection = null;
					} else {
						bestConnection = connections.get(0).tcpConnection;
					}
				}
			}
			updated = formerBestConnection != bestConnection;
			return updated;
		}

		private void close() {
			try {
				messageQueue.put(END_CHUNK);
			} catch (InterruptedException e) {

			}
		}

	}

	private class AddressPair {

		private final InetAddress remoteAddress;
		private final InetAddress localAddress;

		private AddressPair(InetAddress remoteAddress, InetAddress localAddress) {
			this.remoteAddress = remoteAddress;
			this.localAddress = localAddress;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof AddressPair)) {
				return false;
			}
			AddressPair addrPair = (AddressPair) obj;
			return Objects.equals(this.remoteAddress, addrPair.remoteAddress)
					&& Objects.equals(this.localAddress, addrPair.localAddress);
		}

		@Override
		public int hashCode() {
			return Objects.hash(remoteAddress, localAddress);
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

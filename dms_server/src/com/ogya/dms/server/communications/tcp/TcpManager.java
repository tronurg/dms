package com.ogya.dms.server.communications.tcp;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ogya.communications.tcp.TcpClient;
import com.ogya.communications.tcp.TcpClientListener;
import com.ogya.communications.tcp.TcpServer;
import com.ogya.communications.tcp.TcpServerListener;
import com.ogya.dms.server.common.Encryption;
import com.ogya.dms.server.communications.intf.TcpManagerListener;

public class TcpManager implements TcpServerListener {

	private static final String END_OF_TRANSMISSION = String.valueOf((char) 4);

	private final int serverPort;
	private final int clientPortFrom;
	private final int clientPortTo;

	private final AtomicInteger nextPort = new AtomicInteger(0);

	private final TcpServer tcpServer;

	private final Map<Integer, InetAddress> serverIdAddress = Collections
			.synchronizedMap(new HashMap<Integer, InetAddress>());
	private final Map<String, User> users = Collections.synchronizedMap(new HashMap<String, User>());
	private final Map<String, DmsServer> dmsServers = Collections.synchronizedMap(new HashMap<String, DmsServer>());
	private final Map<InetAddress, Connection> connections = Collections
			.synchronizedMap(new HashMap<InetAddress, Connection>());

	private final List<TcpManagerListener> listeners = Collections
			.synchronizedList(new ArrayList<TcpManagerListener>());

	private final Charset encryptionCharset = Charset.forName("UTF-8");

	private final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	public TcpManager(int serverPort, int clientPortFrom, int clientPortTo) throws IOException {

		this.serverPort = serverPort;
		this.clientPortFrom = clientPortFrom;
		this.clientPortTo = clientPortTo;

		nextPort.set(clientPortFrom);

		tcpServer = new TcpServer(serverPort);

		tcpServer.setBlocking(true);

		tcpServer.addListener(this);

		tcpServer.acceptConnection();

	}

	public void addListener(TcpManagerListener listener) {

		listeners.add(listener);

	}

	public void addConnection(final String dmsUuid, final String uuid, final InetAddress address,
			final TcpConnectionType connectionType) {

		taskQueue.execute(() -> {

			users.putIfAbsent(uuid, new User(uuid));
			dmsServers.putIfAbsent(dmsUuid, new DmsServer(dmsUuid));

			final User user = users.get(uuid);
			final DmsServer dmsServer = dmsServers.get(dmsUuid);

			if (user.dmsServer == null) {
				// User yeni katildi
				// Iliskiler guncellenecek

				user.dmsServer = dmsServer;
				dmsServer.users.add(user);

			} else if (user.dmsServer != dmsServer) {
				// Kullanicinin bagli oldugu sunucu degisti
				// Iliskiler guncellenecek

				user.dmsServer.users.remove(user);
				user.dmsServer = dmsServer;
				dmsServer.users.add(user);

			}

			if (!connections.containsKey(address)) {

				Connection connection = new Connection(address);
				connection.dmsServer = dmsServer;

				connections.put(address, connection);

				if (connectionType.equals(TcpConnectionType.CLIENT)) {

					try {

						int port = claimPort();

						TcpClient tcpClient = new TcpClient(address, serverPort, null, port);

						tcpClient.setBlocking(true);

						tcpClient.addListener(new TcpClientListener() {

							@Override
							public void messageReceived(String arg0) {

								taskQueue.execute(() -> {

									try {

										dmsServer.messageFeed.write(arg0.getBytes());

									} catch (IOException e) {

										e.printStackTrace();

									}

								});

							}

							@Override
							public void connected() {

								taskQueue.execute(() -> {

									connection.sendMethod = tcpClient::sendMessage;
									dmsServer.connections.add(connection);

									checkServer(dmsServer);

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

									checkServer(dmsServer);

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

			if (connection.dmsServer == null) {
				// Connection sunucu tarafindan olusturulmus
				// Iliskiler guncellenecek

				connection.dmsServer = dmsServer;
				dmsServer.connections.add(connection);

				checkServer(dmsServer);

			}

		});

	}

	public void sendMessageToUser(final String uuid, final String message, final AtomicBoolean sendStatus,
			final Consumer<Integer> progressMethod) {

		taskQueue.execute(() -> {

			User user = users.get(uuid);

			if (user == null)
				return;

			DmsServer dmsServer = user.dmsServer;

			if (dmsServer == null)
				return;

			try {

				String encryptedMessage = new String(Encryption.encrypt(message.getBytes(encryptionCharset)),
						encryptionCharset);

				sendMessageToServer(dmsServer, encryptedMessage, sendStatus, progressMethod);

			} catch (GeneralSecurityException | IOException e) {

				progressMethod.accept(-1);

				e.printStackTrace();

			}

		});

	}

	public void sendMessageToUsers(final List<String> uuids, final String message, final AtomicBoolean sendStatus,
			final BiConsumer<List<String>, Integer> progressMethod) {

		taskQueue.execute(() -> {

			final Map<DmsServer, List<String>> dmsServers = new HashMap<DmsServer, List<String>>();

			uuids.forEach(uuid -> {

				User user = users.get(uuid);

				if (user == null)
					return;

				DmsServer dmsServer = user.dmsServer;

				if (dmsServer == null)
					return;

				dmsServers.putIfAbsent(dmsServer, new ArrayList<String>());
				dmsServers.get(dmsServer).add(uuid);

			});

			if (dmsServers.isEmpty())
				return;

			try {

				String encryptedMessage = new String(Encryption.encrypt(message.getBytes(encryptionCharset)),
						encryptionCharset);

				dmsServers.forEach((dmsServer, uuidList) -> sendMessageToServer(dmsServer, encryptedMessage, sendStatus,
						progressMethod == null ? null : (progress -> progressMethod.accept(uuidList, progress))));

			} catch (GeneralSecurityException | IOException e) {

				progressMethod.accept(uuids, -1);

				e.printStackTrace();

			}

		});

	}

	public void sendMessageToServer(final String dmsUuid, final String message) {

		taskQueue.execute(() -> {

			DmsServer dmsServer = dmsServers.get(dmsUuid);

			if (dmsServer == null)
				return;

			try {

				String encryptedMessage = new String(Encryption.encrypt(message.getBytes(encryptionCharset)),
						encryptionCharset);

				sendMessageToServer(dmsServer, encryptedMessage, null, null);

			} catch (GeneralSecurityException | IOException e) {

				e.printStackTrace();

			}

		});

	}

	public void sendMessageToAllServers(final String message) {

		taskQueue.execute(() -> {

			if (dmsServers.isEmpty())
				return;

			try {

				String encryptedMessage = new String(Encryption.encrypt(message.getBytes(encryptionCharset)),
						encryptionCharset);

				dmsServers
						.forEach((dmsUuid, dmsServer) -> sendMessageToServer(dmsServer, encryptedMessage, null, null));

			} catch (GeneralSecurityException | IOException e) {

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
			progressMethod.accept(0);

			// Start sending...
			for (int i = 0; i < 1 && (sendStatus == null || sendStatus.get()); ++i) {

				boolean isSent = false;

				synchronized (dmsServer.connections) {

					for (Connection connection : dmsServer.connections) {

						if (connection.sendMethod == null)
							continue;

						isSent = connection.sendMethod.apply(message);

						if (isSent)
							break;

					}

				}

				if (isSent && progressMethod != null)
					progressMethod.accept(100);

			}

			// Send end of transmission
			synchronized (dmsServer.connections) {

				for (Connection connection : dmsServer.connections) {

					if (connection.sendMethod == null)
						continue;

					if (connection.sendMethod.apply(END_OF_TRANSMISSION))
						break;

				}

			}

			// Sending finished, send a progress less than zero
			progressMethod.accept(-1);

		});

	}

	private void checkServer(DmsServer dmsServer) {

		// sunucu bagli degilken connection eklenirse durumu bagliya cekilir
		// ve dinleyicilere haber verilir
		// sunucu bagliyken connectionsinin sayisi sifira duserse bagli degile cekilir
		// ve dinleyicilere haber verilir

		if (!dmsServer.isConnected.get() && dmsServer.connections.size() > 0) {

			dmsServer.isConnected.set(true);

			connectedToRemoteServerToListeners(dmsServer.dmsUuid);

		} else if (dmsServer.isConnected.get() && dmsServer.connections.size() == 0) {
			// sunucu koptu
			// sunucuyu listelerden cikar
			// tum uuid'lerin koptugunu bildir

			dmsServer.isConnected.set(false);

			dmsServer.users.forEach(user -> {

				remoteUserDisconnectedToListeners(user.uuid);

				users.remove(user.uuid);

			});

			dmsServers.remove(dmsServer.dmsUuid).close();

		}

	}

	private void remoteUserDisconnectedToListeners(final String uuid) {

		listeners.forEach(e -> e.remoteUserDisconnected(uuid));

	}

	private void connectedToRemoteServerToListeners(final String dmsUuid) {

		listeners.forEach(e -> e.connectedToRemoteServer(dmsUuid));

	}

	private void messageReceivedToListeners(String message) {

		try {

			String decryptedMessage = new String(Encryption.decrypt(message.getBytes(encryptionCharset)),
					encryptionCharset);

			listeners.forEach(e -> e.messageReceived(decryptedMessage));

		} catch (GeneralSecurityException | IOException e) {

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

				checkServer(dmsServer);

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

			connection.sendMethod = message -> tcpServer.sendMessage(id, message);

			DmsServer dmsServer = connection.dmsServer;

			if (dmsServer != null) {
				// connection alinan bir uuid ile olusturulmus
				// connectionsi guncelleyip kontrol et

				dmsServer.connections.add(connection);

				checkServer(dmsServer);

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

			if (dmsServer == null)
				return;

			try {

				dmsServer.messageFeed.write(arg1.getBytes());

			} catch (IOException e) {

				e.printStackTrace();

			}

		});

	}

	private class Connection {

		final InetAddress remoteAddress;

		DmsServer dmsServer;

		Function<String, Boolean> sendMethod;

		Connection(InetAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
		}

		int getPingTime() {
			int pingTime = 1000;
			try {
				long startTimeMillis = System.currentTimeMillis();
				remoteAddress.isReachable(pingTime);
				long endTimeMillis = System.currentTimeMillis();
				pingTime = (int) (endTimeMillis - startTimeMillis);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return pingTime;
		}

	}

	private class DmsServer {

		final String dmsUuid;

		final List<User> users = Collections.synchronizedList(new ArrayList<User>());
		final Set<Connection> connections = Collections
				.synchronizedSet(new TreeSet<Connection>(new Comparator<Connection>() {

					@Override
					public int compare(Connection arg0, Connection arg1) {
						return arg0.getPingTime() - arg1.getPingTime();
					}

				}));

		final AtomicBoolean isConnected = new AtomicBoolean(false);

		protected final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable arg0) {

				Thread thread = new Thread(arg0);

				thread.setDaemon(true);

				return thread;

			}

		});

		final PipedOutputStream messageFeed = new PipedOutputStream();

		private final Thread messageFeedThread = new Thread(() -> {

			try (PipedInputStream pipedInputStream = new PipedInputStream(messageFeed);
					Scanner scanner = new Scanner(pipedInputStream)) {

				scanner.useDelimiter(END_OF_TRANSMISSION);

				while (true) {

					messageReceivedToListeners(scanner.next());

				}

			} catch (IOException e) {

				e.printStackTrace();

			}

		});

		DmsServer(String dmsUuid) {
			this.dmsUuid = dmsUuid;
			messageFeedThread.start();
		}

		void close() {

			taskQueue.shutdown();

			try {

				messageFeed.close();

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

	}

	private class User {

		final String uuid;

		DmsServer dmsServer;

		User(String uuid) {
			this.uuid = uuid;
		}

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

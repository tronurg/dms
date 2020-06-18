package com.ogya.dms.server.comm.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.ogya.dms.server.comm.intf.TcpManagerListener;
import com.ogya.dms.server.common.Encryption;
import com.onurg.haberlesme.tcp.TcpIstemci;
import com.onurg.haberlesme.tcp.TcpIstemciDinleyici;
import com.onurg.haberlesme.tcp.TcpSunucu;
import com.onurg.haberlesme.tcp.TcpSunucuDinleyici;

public class TcpManager implements TcpSunucuDinleyici {

	private final int serverPort;
	private final int clientPortFrom;
	private final int clientPortTo;

	private final AtomicInteger nextPort = new AtomicInteger(0);

	private final TcpSunucu tcpServer;

	//

	private final Map<Integer, InetAddress> serverIdAddress = Collections
			.synchronizedMap(new HashMap<Integer, InetAddress>());
	private final Map<String, User> users = Collections.synchronizedMap(new HashMap<String, User>());
	private final Map<String, DmsServer> dmsServers = Collections.synchronizedMap(new HashMap<String, DmsServer>());
	private final Map<InetAddress, Connection> connections = Collections
			.synchronizedMap(new HashMap<InetAddress, Connection>());

	//

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

		tcpServer = new TcpSunucu(serverPort);

		tcpServer.setBlocking(true);

		tcpServer.dinleyiciEkle(this);

		tcpServer.baglantiKabulEt();

	}

	public void addListener(TcpManagerListener listener) {

		listeners.add(listener);

	}

	public void addConnection(final String dmsUuid, final String uuid, final InetAddress address,
			TcpConnectionType connectionType) {

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

				Connection connection = new Connection();
				connection.dmsServer = dmsServer;

				connections.put(address, connection);

				if (connectionType.equals(TcpConnectionType.CLIENT)) {

					try {

						int port = claimPort();

						TcpIstemci tcpClient = new TcpIstemci(address, serverPort, null, port);

						tcpClient.setBlocking(true);

						tcpClient.dinleyiciEkle(new TcpIstemciDinleyici() {

							@Override
							public void yeniMesajAlindi(String arg0) {

								taskQueue.execute(() -> {

									messageReceivedToListeners(arg0);

								});

							}

							@Override
							public void baglantiKuruldu() {

								taskQueue.execute(() -> {

									connection.sendMethod = tcpClient::mesajGonder;
									dmsServer.connections.add(connection);

									checkServer(dmsServer);

								});

							}

							@Override
							public void baglantiKurulamadi() {

								taskQueue.execute(() -> {

									connections.remove(address);

								});

							}

							@Override
							public void baglantiKoptu() {

								taskQueue.execute(() -> {

									connections.remove(address);
									dmsServer.connections.remove(connection);

									checkServer(dmsServer);

								});

							}

						});

						tcpClient.baglan();

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

	public void sendMessageToUser(String uuid, String message) {

		taskQueue.execute(() -> {

			User user = users.get(uuid);

			if (user == null)
				return;

			DmsServer dmsServer = user.dmsServer;

			if (dmsServer == null)
				return;

			sendMessageToServer(dmsServer, message);

		});

	}

	public void sendMessageToUsers(List<String> uuids, String message) {

		taskQueue.execute(() -> {

			final Set<DmsServer> dmsServers = new HashSet<DmsServer>();

			uuids.forEach(uuid -> {

				User user = users.get(uuid);

				if (user == null)
					return;

				DmsServer dmsServer = user.dmsServer;

				if (dmsServer == null)
					return;

				dmsServers.add(dmsServer);

			});

			dmsServers.forEach(dmsServer -> sendMessageToServer(dmsServer, message));

		});

	}

	public void sendMessageToServer(String dmsUuid, String message) {

		taskQueue.execute(() -> {

			DmsServer dmsServer = dmsServers.get(dmsUuid);

			if (dmsServer == null)
				return;

			sendMessageToServer(dmsServer, message);

		});

	}

	public void sendMessageToAllServers(final String message) {

		taskQueue.execute(() -> {

			dmsServers.forEach((dmsUuid, dmsServer) -> sendMessageToServer(dmsServer, message));

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

	private void sendMessageToServer(DmsServer dmsServer, String message) {

		try {

			String encryptedMessage = new String(Encryption.encrypt(message.getBytes(encryptionCharset)),
					encryptionCharset);

			dmsServer.taskQueue.execute(() -> {

				synchronized (dmsServer.connections) {

					for (Connection connection : dmsServer.connections) {

						if (connection.sendMethod == null)
							continue;

						boolean isSent = connection.sendMethod.apply(encryptedMessage);

						if (isSent)
							break;

					}

				}

			});

		} catch (GeneralSecurityException | IOException e1) {

		}

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

			dmsServers.remove(dmsServer.dmsUuid);

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

		} catch (GeneralSecurityException | IOException e1) {

		}

	}

	@Override
	public void baglantiKoptu(int id) {

		taskQueue.execute(() -> {

			InetAddress address = serverIdAddress.remove(id);

			if (address == null)
				return;

			Connection connection = connections.get(address);

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
	public void baglantiKuruldu(final int id) {

		taskQueue.execute(() -> {

			InetAddress address = tcpServer.getUzakAdres(id);

			serverIdAddress.put(id, address);

			connections.putIfAbsent(address, new Connection());

			Connection connection = connections.get(address);

			connection.sendMethod = message -> tcpServer.mesajGonder(id, message);

			DmsServer dmsServer = connection.dmsServer;

			if (dmsServer != null) {
				// connection alinan bir uuid ile olusturulmus
				// connectionsi guncelleyip kontrol et

				dmsServer.connections.add(connection);

				checkServer(dmsServer);

			}

		});

		tcpServer.baglantiKabulEt();

	}

	@Override
	public void yeniMesajAlindi(int arg0, String arg1) {

		taskQueue.execute(() -> {

			messageReceivedToListeners(arg1);

		});

	}

	private class Connection {

		DmsServer dmsServer;

		Function<String, Boolean> sendMethod;

	}

	private class DmsServer {

		final String dmsUuid;

		final List<User> users = Collections.synchronizedList(new ArrayList<User>());
		final List<Connection> connections = Collections.synchronizedList(new ArrayList<Connection>());

		final AtomicBoolean isConnected = new AtomicBoolean(false);

		protected final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable arg0) {

				Thread thread = new Thread(arg0);

				thread.setDaemon(true);

				return thread;

			}

		});

		DmsServer(String dmsUuid) {
			this.dmsUuid = dmsUuid;
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

package com.ogya.dms.server.control;

import java.io.IOException;
import java.net.InetAddress;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.communications.intf.TcpManagerListener;
import com.ogya.dms.server.communications.tcp.TcpConnectionType;
import com.ogya.dms.server.communications.tcp.TcpManager;
import com.ogya.dms.server.communications.udp.MulticastManager;
import com.ogya.dms.server.model.Model;
import com.ogya.dms.server.model.intf.ModelListener;

public class Control implements TcpManagerListener, ModelListener {

	private static final String DMS_UUID = UUID.randomUUID().toString();

	private static Control instance;

	private final Model model = new Model(this);

	private final int routerPort = CommonConstants.INTERCOM_PORT;
	private final String multicastGroup = CommonConstants.MULTICAST_IP;
	private final int multicastPort = CommonConstants.MULTICAST_PORT;
	private final int serverPort = CommonConstants.SERVER_PORT;
	private final int clientPortFrom = CommonConstants.CLIENT_PORT_FROM;
	private final int clientPortTo = CommonConstants.CLIENT_PORT_TO;
	private final int packetSize = CommonConstants.PACKET_SIZE;

	private final MulticastManager multicastManager = new MulticastManager(multicastGroup, multicastPort,
			this::receiveUdpMessage);

	private TcpManager tcpManager;

	private final ZContext context = new ZContext();

	private final LinkedBlockingQueue<SimpleEntry<String, String>> routerQueue = new LinkedBlockingQueue<SimpleEntry<String, String>>();

	private final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	private Control() {

	}

	public synchronized static Control getInstance() {

		if (instance == null) {

			instance = new Control();

		}

		return instance;

	}

	public void start() {

		new Thread(this::router).start();
		new Thread(this::inproc).start();
		new Thread(this::monitor).start();

	}

	private TcpManager getTcpManager() throws IOException {

		if (tcpManager == null) {

			tcpManager = new TcpManager(serverPort, clientPortFrom, clientPortTo, packetSize);

			tcpManager.addListener(this);

		}

		return tcpManager;

	}

	private void receiveUdpMessage(InetAddress senderAddress, String message) {

		String[] uuids = message.split(" ");
		if (uuids.length != 2 || DMS_UUID.equals(uuids[0]))
			return;

		try {

			getTcpManager().addConnection(uuids[0], uuids[1], senderAddress,
					uuids[0].compareTo(DMS_UUID) < 0 ? TcpConnectionType.SERVER : TcpConnectionType.CLIENT);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	private void router() {

		try (ZMQ.Socket routerSocket = context.createSocket(SocketType.ROUTER);
				ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			routerSocket.monitor("inproc://monitor", ZMQ.EVENT_DISCONNECTED);

			routerSocket.setRouterMandatory(true);
			routerSocket.bind("tcp://*:" + routerPort);
			inprocSocket.bind("inproc://router");

			ZMQ.Poller poller = context.createPoller(2);
			int pollRouter = poller.register(routerSocket, ZMQ.Poller.POLLIN);
			int pollInproc = poller.register(inprocSocket, ZMQ.Poller.POLLIN);

			while (!Thread.currentThread().isInterrupted()) {

				poller.poll();

				if (poller.pollin(pollRouter)) {

					routerSocket.recvStr(ZMQ.DONTWAIT);
					String messagePojoStr = routerSocket.recvStr(ZMQ.DONTWAIT);

					taskQueue.execute(() -> model.localMessageReceived(messagePojoStr));

				} else if (poller.pollin(pollInproc)) {

					String uuid = inprocSocket.recvStr(ZMQ.DONTWAIT);
					String message = inprocSocket.recvStr(ZMQ.DONTWAIT);

					try {

						routerSocket.send(uuid, ZMQ.SNDMORE | ZMQ.DONTWAIT);
						routerSocket.send(message, ZMQ.DONTWAIT);

					} catch (ZMQException e) {

						taskQueue.execute(() -> model.localUserDisconnected(uuid));

					}

				}
			}

		} catch (Exception e) {

			System.out.println(
					"Port (" + routerPort + ") in use. Unable to communicate with clients! Program will exit.");

			System.exit(-1);

		}

	}

	private void inproc() {

		try (ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			inprocSocket.connect("inproc://router");

			while (!Thread.currentThread().isInterrupted()) {

				SimpleEntry<String, String> receiverMessage = routerQueue.take();

				inprocSocket.sendMore(receiverMessage.getKey());
				inprocSocket.send(receiverMessage.getValue());

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private void monitor() {

		try (ZMQ.Socket monitorSocket = context.createSocket(SocketType.PAIR)) {

			monitorSocket.connect("inproc://monitor");

			while (!Thread.currentThread().isInterrupted()) {

				ZMQ.Event event = ZMQ.Event.recv(monitorSocket);

				switch (event.getEvent()) {

				case ZMQ.EVENT_DISCONNECTED:

					Thread.sleep(100);

					taskQueue.execute(() -> model.testAllLocalUsers());
					break;

				}

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	@Override
	public void connectedToRemoteServer(final String dmsUuid) {

		taskQueue.execute(() -> model.processAllLocalBeacons(beacon -> {

			try {

				getTcpManager().sendMessageToServer(dmsUuid, beacon);

			} catch (IOException e) {

				e.printStackTrace();

			}

		}));

	}

	@Override
	public void remoteUserDisconnected(String uuid) {

		taskQueue.execute(() -> model.remoteUserDisconnected(uuid));

	}

	@Override
	public void messageReceived(String message) {

		taskQueue.execute(() -> model.remoteMessageReceived(message));

	}

	@Override
	public void sendToLocalUser(String receiverUuid, String message) {

		routerQueue.offer(new SimpleEntry<String, String>(receiverUuid, message));

	}

	@Override
	public void sendToRemoteUser(String receiverUuid, String message, AtomicBoolean sendStatus,
			Consumer<Integer> progressMethod) {

		try {

			getTcpManager().sendMessageToUser(receiverUuid, message, sendStatus, progressMethod);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void sendToRemoteUsers(List<String> receiverUuids, String message, AtomicBoolean sendStatus,
			BiConsumer<List<String>, Integer> progressMethod) {

		try {

			getTcpManager().sendMessageToUsers(receiverUuids, message, sendStatus, progressMethod);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void sendToAllRemoteUsers(String message) {

		try {

			getTcpManager().sendMessageToAllServers(message);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void publishUuid(String uuid) {

		multicastManager.send(DMS_UUID + " " + uuid);

	}

}

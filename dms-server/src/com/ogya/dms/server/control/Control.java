package com.ogya.dms.server.control;

import java.io.IOException;
import java.net.InetAddress;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
import com.ogya.dms.server.factory.DmsFactory;
import com.ogya.dms.server.model.Model;
import com.ogya.dms.server.model.intf.ModelListener;

public class Control implements TcpManagerListener, ModelListener {

	private static final String DMS_UUID = UUID.randomUUID().toString();

	private static Control instance;

	private final Model model = new Model(this);

	private final int routerPort = CommonConstants.INTERCOM_PORT;
	private final String multicastGroup = CommonConstants.MULTICAST_IP;
	private final int multicastPort = CommonConstants.MULTICAST_PORT;
	private final int beaconIntervalMs = CommonConstants.BEACON_INTERVAL_MS;
	private final int serverPort = CommonConstants.SERVER_PORT;
	private final int clientPortFrom = CommonConstants.CLIENT_PORT_FROM;
	private final int clientPortTo = CommonConstants.CLIENT_PORT_TO;

	private final MulticastManager multicastManager = new MulticastManager(multicastGroup, multicastPort,
			this::receiveUdpMessage);

	private TcpManager tcpManager;

	private final ZContext context = new ZContext();

	private final LinkedBlockingQueue<SimpleEntry<String, String>> routerQueue = new LinkedBlockingQueue<SimpleEntry<String, String>>();

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	private final Object publishSyncObj = new Object();

	private Control() {

		// Try to initialize TcpManager
		try {
			getTcpManager();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public synchronized static Control getInstance() {

		if (instance == null) {

			instance = new Control();

		}

		return instance;

	}

	public void start() {

		new Thread(this::publishDmsUuid).start();
		new Thread(this::router).start();
		new Thread(this::inproc).start();
		new Thread(this::monitor).start();

	}

	public void publishDmsUuid() {

		while (!Thread.currentThread().isInterrupted()) {

			synchronized (publishSyncObj) {

				if (model.isLive())
					multicastManager.send(DMS_UUID, model.getRemoteIps());

				try {

					publishSyncObj.wait(beaconIntervalMs);

				} catch (InterruptedException e) {

				}

			}

		}

	}

	private synchronized TcpManager getTcpManager() throws IOException {

		if (tcpManager == null) {

			tcpManager = new TcpManager(serverPort, clientPortFrom, clientPortTo);

			tcpManager.addListener(this);

		}

		return tcpManager;

	}

	private void receiveUdpMessage(InetAddress senderAddress, String message, boolean isUnicast) {

		if (Objects.equals(message, DMS_UUID))
			return;

		try {

			getTcpManager().addConnection(message, senderAddress,
					message.compareTo(DMS_UUID) < 0 ? TcpConnectionType.SERVER : TcpConnectionType.CLIENT);

		} catch (IOException e) {

			e.printStackTrace();

		}

		if (isUnicast)
			taskQueue.execute(() -> model.addRemoteIp(senderAddress.getHostAddress()));

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

						taskQueue.execute(() -> model.localUuidDisconnected(uuid));

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
	public void serverConnectionsUpdated(String dmsUuid, List<InetAddress> remoteAddresses,
			List<InetAddress> localAddresses) {

		taskQueue.execute(() -> model.serverConnectionsUpdated(dmsUuid, remoteAddresses, localAddresses));

	}

	@Override
	public void messageReceivedFromRemoteServer(final String message, final String dmsUuid) {

		taskQueue.execute(() -> model.remoteMessageReceived(message, dmsUuid));

	}

	@Override
	public void sendToLocalUser(String receiverUuid, String message) {

		routerQueue.offer(new SimpleEntry<String, String>(receiverUuid, message));

	}

	@Override
	public void sendToRemoteServer(String dmsUuid, String message, AtomicBoolean sendStatus,
			Consumer<Integer> progressMethod) {

		try {

			getTcpManager().sendMessageToServer(dmsUuid, message, sendStatus, progressMethod);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void sendToAllRemoteServers(String message) {

		try {

			getTcpManager().sendMessageToAllServers(message);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void publishImmediately() {

		synchronized (publishSyncObj) {

			publishSyncObj.notify();

		}

	}

}

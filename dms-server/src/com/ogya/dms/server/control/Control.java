package com.ogya.dms.server.control;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.ogya.dms.commons.DmsMessageSender.Chunk;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.common.MessageContainerLocal;
import com.ogya.dms.server.communications.intf.TcpManagerListener;
import com.ogya.dms.server.communications.tcp.TcpConnectionType;
import com.ogya.dms.server.communications.tcp.TcpManager;
import com.ogya.dms.server.communications.udp.MulticastManager;
import com.ogya.dms.server.factory.DmsFactory;
import com.ogya.dms.server.model.Model;
import com.ogya.dms.server.model.intf.ModelListener;

public class Control implements TcpManagerListener, ModelListener {

	private static final String DMS_UUID = CommonConstants.DMS_UUID;

	private static final int routerPort = CommonConstants.INTERCOM_PORT;
	private static final String multicastGroup = CommonConstants.MULTICAST_IP;
	private static final int multicastPort = CommonConstants.MULTICAST_PORT;
	private static final int beaconIntervalMs = CommonConstants.BEACON_INTERVAL_MS;
	private static final int serverPort = CommonConstants.SERVER_PORT;
	private static final int clientPortFrom = CommonConstants.CLIENT_PORT_FROM;
	private static final int clientPortTo = CommonConstants.CLIENT_PORT_TO;

	private static Control instance;

	private final Model model = new Model(this);

	private final MulticastManager multicastManager = new MulticastManager(multicastGroup, multicastPort,
			this::receiveUdpMessage);
	private final TcpManager tcpManager = new TcpManager(serverPort, clientPortFrom, clientPortTo, this);
	private final ZContext context = new ZContext();
	private final LinkedBlockingQueue<String> signalQueue = new LinkedBlockingQueue<String>();
	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();
	private final Object publishSyncObj = new Object();

	private Control() {
		init();
	}

	public synchronized static Control getInstance() {

		if (instance == null) {

			instance = new Control();

		}

		return instance;

	}

	private void init() {
		try {
			Files.list(Paths.get(System.getProperty("java.io.tmpdir"))).forEach(path -> {
				if (path.getFileName().toString().startsWith("dms")) {
					try {
						Files.delete(path);
					} catch (Exception e) {

					}
				}
			});
		} catch (Exception e) {

		}
	}

	public void start() {

		new Thread(this::publishDmsUuid).start();
		new Thread(this::router).start();
		new Thread(this::inproc).start();
		new Thread(this::monitor).start();

	}

	private void publishDmsUuid() {

		while (!Thread.currentThread().isInterrupted()) {

			synchronized (publishSyncObj) {

				if (model.isLive()) {
					Set<InetAddress> remoteAddresses = model.getRemoteAddresses();
					remoteAddresses.removeAll(tcpManager.getConnectedAddresses());
					multicastManager.send(DMS_UUID, remoteAddresses);
				}

				try {
					publishSyncObj.wait(beaconIntervalMs);
				} catch (InterruptedException e) {

				}

			}

		}

	}

	private void receiveUdpMessage(InetAddress senderAddress, String message, boolean isUnicast) {

		if (DMS_UUID.equals(message))
			return;

		tcpManager.addConnection(message, senderAddress,
				message.compareTo(DMS_UUID) < 0 ? TcpConnectionType.SERVER : TcpConnectionType.CLIENT);

		if (isUnicast)
			taskQueue.execute(() -> model.addRemoteIps(senderAddress));

	}

	private void router() {

		try (ZMQ.Socket routerSocket = context.createSocket(SocketType.ROUTER);
				ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			routerSocket.monitor("inproc://monitor", ZMQ.EVENT_DISCONNECTED);

			routerSocket.setSndHWM(0);
			routerSocket.setRouterMandatory(true);
			routerSocket.bind("tcp://*:" + routerPort);
			inprocSocket.bind("inproc://router");

			ZMQ.Poller poller = context.createPoller(2);
			int pollRouter = poller.register(routerSocket, ZMQ.Poller.POLLIN);
			int pollInproc = poller.register(inprocSocket, ZMQ.Poller.POLLIN);

			while (!Thread.currentThread().isInterrupted()) {

				poller.poll();

				if (poller.pollin(pollRouter)) {

					final String userUuid = routerSocket.recvStr(ZMQ.DONTWAIT);
					int messageNumber = Integer.parseInt(routerSocket.recvStr(ZMQ.DONTWAIT));
					if (!routerSocket.hasReceiveMore()) {
						taskQueue.execute(() -> sendMore(userUuid));
						continue;
					}
					byte[] data = routerSocket.recv(ZMQ.DONTWAIT);
					taskQueue.execute(() -> model.localMessageReceived(messageNumber, data, userUuid));
					try {
						routerSocket.send(userUuid, ZMQ.SNDMORE | ZMQ.DONTWAIT);
						routerSocket.send(String.valueOf(0), ZMQ.DONTWAIT); // "Send more" signal
					} catch (ZMQException e) {

					}

				} else if (poller.pollin(pollInproc)) {

					final String uuid = inprocSocket.recvStr(ZMQ.DONTWAIT);
					MessageContainerLocal messageContainer = model.getNextMessage(uuid);

					if (messageContainer == null) {
						continue;
					}

					boolean disconnected = false;
					Chunk chunk = messageContainer.next();
					if (chunk == null) {
						taskQueue.execute(() -> sendMore(uuid)); // Pass the signal
					} else {
						try {
							routerSocket.send(uuid, ZMQ.SNDMORE | ZMQ.DONTWAIT);
							routerSocket.send(String.valueOf(messageContainer.messageNumber),
									ZMQ.SNDMORE | ZMQ.DONTWAIT);
							routerSocket.sendByteBuffer(chunk.dataBuffer, ZMQ.DONTWAIT);
							boolean progressUpdated = chunk.progress > messageContainer.progressPercent
									.getAndSet(chunk.progress);
							if (progressUpdated && messageContainer.progressConsumer != null) {
								messageContainer.progressConsumer.accept(chunk.progress);
							}
						} catch (ZMQException e) {
							disconnected = true;
							messageContainer.markAsDone();
						}
					}

					if (messageContainer.hasMore()) {
						model.queueMessage(uuid, messageContainer);
					} else {
						model.closeMessage(uuid, messageContainer);
					}

					if (disconnected) {
						model.localUuidDisconnected(uuid);
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

				inprocSocket.send(signalQueue.take());

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

	private void sendMore(String receiverUuid) {
		try {
			signalQueue.put(receiverUuid);
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void serverConnectionsUpdated(final String dmsUuid, final Map<InetAddress, InetAddress> localRemoteIps,
			final boolean beaconsRequested) {

		taskQueue.execute(() -> model.serverConnectionsUpdated(dmsUuid, localRemoteIps, beaconsRequested));

	}

	@Override
	public void messageReceivedFromRemoteServer(final MessagePojo messagePojo, final String dmsUuid) {

		taskQueue.execute(() -> model.remoteMessageReceived(messagePojo, dmsUuid));

	}

	@Override
	public void localMessageReady(String receiverUuid) {
		sendMore(receiverUuid);
	}

	@Override
	public void sendToRemoteServer(String dmsUuid, MessagePojo messagePojo, AtomicBoolean sendStatus,
			Consumer<Integer> progressConsumer) {

		tcpManager.sendMessageToServer(dmsUuid, messagePojo, sendStatus, progressConsumer);

	}

	@Override
	public void sendToAllRemoteServers(MessagePojo messagePojo) {

		tcpManager.sendMessageToAllServers(messagePojo);

	}

	@Override
	public void publishImmediately() {

		synchronized (publishSyncObj) {

			publishSyncObj.notify();

		}

	}

}

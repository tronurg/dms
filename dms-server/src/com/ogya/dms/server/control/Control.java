package com.ogya.dms.server.control;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.ogya.dms.commons.DmsMessageFactory;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.communications.intf.TcpManagerListener;
import com.ogya.dms.server.communications.tcp.TcpConnectionType;
import com.ogya.dms.server.communications.tcp.TcpManager;
import com.ogya.dms.server.communications.udp.MulticastManager;
import com.ogya.dms.server.factory.DmsFactory;
import com.ogya.dms.server.model.Model;
import com.ogya.dms.server.model.intf.ModelListener;

public class Control implements TcpManagerListener, ModelListener {

	private static final int CHUNK_SIZE = 8192;

	private static final String DMS_UUID = CommonConstants.DMS_UUID;

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

	private final TcpManager tcpManager = new TcpManager(serverPort, clientPortFrom, clientPortTo, this);

	private final ZContext context = new ZContext();

	private final LinkedBlockingQueue<byte[]> signalQueue = new LinkedBlockingQueue<byte[]>();

	private final LinkedBlockingQueue<LocalMessage> localMessageQueue = new LinkedBlockingQueue<LocalMessage>();

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	private final Object publishSyncObj = new Object();

	private Control() {

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

	private void publishDmsUuid() {

		while (!Thread.currentThread().isInterrupted()) {

			synchronized (publishSyncObj) {

				if (model.isLive())
					multicastManager.send(DMS_UUID, model.getUnconnectedRemoteIps());

				try {

					publishSyncObj.wait(beaconIntervalMs);

				} catch (InterruptedException e) {

				}

			}

		}

	}

	private void receiveUdpMessage(InetAddress senderAddress, String message, boolean isUnicast) {

		if (Objects.equals(message, DMS_UUID))
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

					String userUuid = routerSocket.recvStr(ZMQ.DONTWAIT);
					byte[] data = routerSocket.recv(ZMQ.DONTWAIT);

					taskQueue.execute(() -> model.localMessageReceived(data, userUuid));

				} else if (poller.pollin(pollInproc)) {

					inprocSocket.recv(ZMQ.DONTWAIT);
					LocalMessage localMessage = localMessageQueue.poll();

					if (localMessage == null)
						continue;

					List<String> successfulUuids = new ArrayList<String>(localMessage.receiverUuids);

					final long startTime = System.currentTimeMillis();

					final AtomicBoolean health = new AtomicBoolean(
							localMessage.sendStatus.get() && (localMessage.messagePojo.useTimeout == null
									|| System.currentTimeMillis() - startTime < localMessage.messagePojo.useTimeout));

					final AtomicInteger progressPercent = new AtomicInteger(-1);

					DmsMessageFactory.outFeed(localMessage.messagePojo, CHUNK_SIZE, health, (data, progress) -> {

						for (String receiverUuid : localMessage.receiverUuids) {

							if (!successfulUuids.contains(receiverUuid))
								continue;

							try {

								routerSocket.send(receiverUuid, ZMQ.SNDMORE | ZMQ.DONTWAIT);
								routerSocket.send(data, ZMQ.DONTWAIT);

								if (progress < 0)
									successfulUuids.remove(receiverUuid);

							} catch (ZMQException e) {

								successfulUuids.remove(receiverUuid);

								taskQueue.execute(() -> model.localUuidDisconnected(receiverUuid));

							}

						}

						health.set(localMessage.sendStatus.get()
								&& (localMessage.messagePojo.useTimeout == null
										|| System.currentTimeMillis() - startTime < localMessage.messagePojo.useTimeout)
								&& !successfulUuids.isEmpty());

						boolean progressUpdated = progress > progressPercent.get();

						if (progressUpdated)
							progressPercent.set(progress);

						if (localMessage.progressConsumer == null)
							return;

						if (progressUpdated && !successfulUuids.isEmpty())
							localMessage.progressConsumer.accept(successfulUuids, progress);

					});

					if (localMessage.progressConsumer == null)
						continue;

					if (successfulUuids.size() < localMessage.receiverUuids.size())
						localMessage.progressConsumer.accept(localMessage.receiverUuids.stream()
								.filter(receiverUuid -> !successfulUuids.contains(receiverUuid))
								.collect(Collectors.toList()), -1);

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
	public void sendToLocalUsers(MessagePojo messagePojo, AtomicBoolean sendStatus,
			BiConsumer<List<String>, Integer> progressConsumer, String... receiverUuids) {

		try {
			localMessageQueue.put(new LocalMessage(messagePojo, Arrays.asList(receiverUuids),
					sendStatus == null ? new AtomicBoolean(true) : sendStatus, progressConsumer));
			signalQueue.put(new byte[0]);
		} catch (InterruptedException e) {

		}

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

	private final class LocalMessage {

		private final MessagePojo messagePojo;
		private final List<String> receiverUuids;
		private final AtomicBoolean sendStatus;
		private final BiConsumer<List<String>, Integer> progressConsumer;

		private LocalMessage(MessagePojo messagePojo, List<String> receiverUuids, AtomicBoolean sendStatus,
				BiConsumer<List<String>, Integer> progressConsumer) {
			this.messagePojo = messagePojo;
			this.receiverUuids = receiverUuids;
			this.sendStatus = sendStatus;
			this.progressConsumer = progressConsumer;
		}

	}

}

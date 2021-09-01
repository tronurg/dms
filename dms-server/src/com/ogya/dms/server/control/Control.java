package com.ogya.dms.server.control;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
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
import com.ogya.dms.commons.DmsMessageFactory.Chunk;
import com.ogya.dms.commons.DmsMessageFactory.MessageSender;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.common.MessageContainerBase;
import com.ogya.dms.server.common.MessageSorter;
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

	private static final byte[] SIGNAL = new byte[0];

	private static Control instance;

	private final Model model = new Model(this);

	private final MulticastManager multicastManager = new MulticastManager(multicastGroup, multicastPort,
			this::receiveUdpMessage);
	private final TcpManager tcpManager = new TcpManager(serverPort, clientPortFrom, clientPortTo, this);
	private final ZContext context = new ZContext();
	private final LinkedBlockingQueue<byte[]> signalQueue = new LinkedBlockingQueue<byte[]>();
	private final AtomicInteger messageCounter = new AtomicInteger(0);
	private final PriorityBlockingQueue<MessageContainer> messageQueue = new PriorityBlockingQueue<MessageContainer>(11,
			new MessageSorter());
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
					MessageContainer messageContainer = messageQueue.poll();

					if (messageContainer == null)
						continue;

					messageContainer.checkIn();
					while (messageContainer.messageSender.hasNext()) {
						if (messageContainer.successfulUuids.isEmpty()) {
							messageContainer.messageSender.close();
							break;
						}
						messageContainer.checkIn();
						Chunk chunk = messageContainer.messageSender.next();
						for (String receiverUuid : messageContainer.receiverUuids) {
							if (!messageContainer.successfulUuids.contains(receiverUuid))
								continue;
							try {
								routerSocket.send(receiverUuid, ZMQ.SNDMORE | ZMQ.DONTWAIT);
								routerSocket.send(String.valueOf(messageContainer.messageNumber),
										ZMQ.SNDMORE | ZMQ.DONTWAIT);
								routerSocket.send(chunk.data, ZMQ.DONTWAIT);
								if (chunk.progress < 0)
									messageContainer.successfulUuids.remove(receiverUuid);
							} catch (ZMQException e) {
								messageContainer.successfulUuids.remove(receiverUuid);
								taskQueue.execute(() -> model.localUuidDisconnected(receiverUuid));
							}
						}
						if (!messageContainer.successfulUuids.isEmpty()) {
							boolean progressUpdated = chunk.progress > messageContainer.progressPercent
									.getAndSet(chunk.progress);
							if (progressUpdated && messageContainer.progressConsumer != null) {
								messageContainer.progressConsumer.accept(messageContainer.successfulUuids,
										chunk.progress);
							}
						} else {
							// Do nothing
						}
						if (messageContainer.bigFile && messageContainer.messageSender.hasNext()) {
							messageQueue.put(messageContainer);
							signalQueue.put(SIGNAL);
							break;
						}
					}

					if (!messageContainer.messageSender.hasNext() && messageContainer.progressConsumer != null
							&& messageContainer.successfulUuids.size() < messageContainer.receiverUuids.size()) {
						messageContainer.progressConsumer.accept(messageContainer.receiverUuids.stream()
								.filter(receiverUuid -> !messageContainer.successfulUuids.contains(receiverUuid))
								.collect(Collectors.toList()), -1);
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
			messageQueue.put(new MessageContainer(messageCounter.getAndIncrement(), messagePojo,
					sendStatus == null ? new AtomicBoolean(true) : sendStatus, Arrays.asList(receiverUuids),
					progressConsumer));
			signalQueue.put(SIGNAL);
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

	private final class MessageContainer extends MessageContainerBase {

		private final List<String> receiverUuids;
		private final BiConsumer<List<String>, Integer> progressConsumer;
		private final List<String> successfulUuids;

		private MessageContainer(int messageNumber, MessagePojo messagePojo, AtomicBoolean sendStatus,
				List<String> receiverUuids, BiConsumer<List<String>, Integer> progressConsumer) {
			super(messageNumber, messagePojo, sendStatus);
			this.receiverUuids = receiverUuids;
			this.progressConsumer = progressConsumer;
			this.successfulUuids = new ArrayList<String>(receiverUuids);
		}

		@Override
		protected MessageSender initMessageSender(MessagePojo messagePojo, AtomicBoolean health) {
			return DmsMessageFactory.outFeedServerToClient(messagePojo, health);
		}

	}

}

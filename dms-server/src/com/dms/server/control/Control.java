package com.dms.server.control;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZEvent;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.dms.server.communications.intf.TcpManagerListener;
import com.dms.server.communications.tcp.TcpConnectionType;
import com.dms.server.communications.tcp.TcpManager;
import com.dms.server.communications.udp.UdpManager;
import com.dms.server.factory.DmsFactory;
import com.dms.server.model.Model;
import com.dms.server.model.intf.ModelListener;
import com.dms.server.structures.LocalChunk;
import com.dms.server.structures.RemoteChunk;
import com.dms.server.util.Commons;

public class Control implements TcpManagerListener, ModelListener {

	private static final String DMS_UUID = Commons.DMS_UUID;
	private static final byte[] SIGNAL = new byte[0];

	private static Control instance;

	private final int routerPort = Commons.INTERCOM_PORT;
	private final String multicastGroup = Commons.MULTICAST_GROUP;
	private final int beaconPort = Commons.BEACON_PORT;
	private final int beaconIntervalMs = Commons.BEACON_INTERVAL_MS;
	private final int serverPort = Commons.SERVER_PORT;
	private final int clientPortFrom = Commons.CLIENT_PORT_FROM;
	private final int clientPortTo = Commons.CLIENT_PORT_TO;

	private final Model model = new Model(this);

	private final TcpManager tcpManager = new TcpManager(serverPort, clientPortFrom, clientPortTo, this);
	private final ZContext context = new ZContext();
	private final LinkedBlockingQueue<byte[]> signalQueue = new LinkedBlockingQueue<byte[]>();
	private final LinkedBlockingQueue<LocalChunk> messageQueue = new LinkedBlockingQueue<LocalChunk>();
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

		try {

			InetAddress multicastGroup = InetAddress.getByName(this.multicastGroup);
			if (!multicastGroup.isMulticastAddress()) {
				throw new Exception();
			}

			UdpManager udpManager = new UdpManager(multicastGroup, beaconPort, this::receiveUdpMessage);

			while (!Thread.currentThread().isInterrupted()) {

				synchronized (publishSyncObj) {

					try {

						if (model.isLive()) {
							Set<InetAddress> remoteAddresses = model.getRemoteAddresses();
							remoteAddresses.removeAll(tcpManager.getConnectedAddresses());
							udpManager.send(DMS_UUID, remoteAddresses);
						}

						publishSyncObj.wait(beaconIntervalMs);

					} catch (Exception e) {

					}

				}

			}

		} catch (Exception e) {
			exitWithMessage(String.format("Multicast group (%s) not recognized! Program will exit.", multicastGroup));
		}

	}

	private void receiveUdpMessage(InetAddress senderAddress, String message, boolean isUnicast) {

		if (DMS_UUID.equals(message)) {
			return;
		}

		tcpManager.addConnection(message, senderAddress,
				message.compareTo(DMS_UUID) < 0 ? TcpConnectionType.SERVER : TcpConnectionType.CLIENT);

		if (isUnicast) {
			taskQueue.execute(() -> model.addRemoteIps(senderAddress));
		}

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

				try {
					poller.poll();

					if (poller.pollin(pollRouter)) {

						final String userUuid = routerSocket.recvStr(ZMQ.DONTWAIT);
						final String address = routerSocket.recvStr(ZMQ.DONTWAIT);
						String messageNumberStr = routerSocket.recvStr(ZMQ.DONTWAIT);
						String progressStr = routerSocket.recvStr(ZMQ.DONTWAIT);
						final byte[] data = routerSocket.recv(ZMQ.DONTWAIT);
						final int messageNumber = Integer.parseInt(messageNumberStr);
						final int progress = Integer.parseInt(progressStr);
						taskQueue.execute(
								() -> model.localMessageReceived(address, messageNumber, progress, data, userUuid));

					} else if (poller.pollin(pollInproc)) {

						inprocSocket.recv(ZMQ.DONTWAIT);
						LocalChunk chunk = messageQueue.poll();

						if (chunk == null) {
							continue;
						}

						for (String uuid : chunk.receiverUuids) {
							try {
								routerSocket.send(uuid, ZMQ.SNDMORE | ZMQ.DONTWAIT);
								routerSocket.send(String.valueOf(chunk.messageNumber), ZMQ.SNDMORE | ZMQ.DONTWAIT);
								routerSocket.send(chunk.data, ZMQ.DONTWAIT);
							} catch (ZMQException e) {
								model.localUuidDisconnected(uuid);
							}
						}

						if (chunk.sendMore != null) {
							chunk.sendMore.accept(true);
						}

					}
				} catch (Exception e) {
					System.out.println("Message received from a mismatched client.");
				}

			}

		} catch (Exception e) {
			exitWithMessage(String.format("Port (%d) in use. Unable to communicate with clients! Program will exit.",
					routerPort));
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

				ZEvent event = ZEvent.recv(monitorSocket);

				switch (event.getEvent()) {

				case DISCONNECTED:

					Thread.sleep(100);
					taskQueue.execute(() -> model.testAllLocalUsers());

					break;

				default:
					break;

				}

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private void exitWithMessage(String message) {
		System.out.println(message);
		System.exit(-1);
	}

	@Override
	public void serverConnectionsUpdated(final String dmsUuid, final Map<InetAddress, InetAddress> localRemoteIps,
			final boolean beaconsRequested) {

		taskQueue.execute(() -> model.serverConnectionsUpdated(dmsUuid, localRemoteIps, beaconsRequested));

	}

	@Override
	public void messageReceivedFromRemoteServer(final int messageNumber, final byte[] data, final String dmsUuid) {

		taskQueue.execute(() -> model.remoteMessageReceived(messageNumber, data, dmsUuid));

	}

	@Override
	public void sendToLocalUsers(LocalChunk chunk) {
		try {
			messageQueue.put(chunk);
			signalQueue.put(SIGNAL);
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void sendToRemoteServer(RemoteChunk chunk, String dmsUuid) {
		tcpManager.sendMessageToServer(chunk, dmsUuid);
	}

	@Override
	public void publishImmediately() {

		synchronized (publishSyncObj) {

			publishSyncObj.notifyAll();

		}

	}

}

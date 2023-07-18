package com.ogya.dms.server.control;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.communications.intf.TcpManagerListener;
import com.ogya.dms.server.communications.tcp.TcpConnectionType;
import com.ogya.dms.server.communications.tcp.TcpManager;
import com.ogya.dms.server.communications.udp.UdpManager;
import com.ogya.dms.server.factory.DmsFactory;
import com.ogya.dms.server.model.Model;
import com.ogya.dms.server.model.intf.ModelListener;
import com.ogya.dms.server.structures.LocalChunk;
import com.ogya.dms.server.structures.RemoteChunk;

public class Control implements TcpManagerListener, ModelListener {

	private static final String DMS_UUID = CommonConstants.DMS_UUID;
	private static final byte[] SIGNAL = new byte[0];

	private static final int routerPort = CommonConstants.INTERCOM_PORT;
	private static final int beaconPort = CommonConstants.BEACON_PORT;
	private static final int beaconIntervalMs = CommonConstants.BEACON_INTERVAL_MS;
	private static final int serverPort = CommonConstants.SERVER_PORT;
	private static final int clientPortFrom = CommonConstants.CLIENT_PORT_FROM;
	private static final int clientPortTo = CommonConstants.CLIENT_PORT_TO;

	private static Control instance;

	private final Model model = new Model(this);

	private final UdpManager udpManager = new UdpManager(beaconPort, this::receiveUdpMessage);
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

		while (!Thread.currentThread().isInterrupted()) {

			synchronized (publishSyncObj) {

				if (model.isLive()) {
					Set<InetAddress> remoteAddresses = model.getRemoteAddresses();
					remoteAddresses.removeAll(tcpManager.getConnectedAddresses());
					udpManager.send(DMS_UUID, remoteAddresses);
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

			publishSyncObj.notify();

		}

	}

}

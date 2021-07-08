package com.ogya.dms.server.communications.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.common.DmsSecurity;
import com.ogya.dms.server.communications.intf.MulticastManagerListener;
import com.ogya.dms.server.factory.DmsFactory;

public class MulticastManager {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static final byte HEADER_MULTICAST = 0;
	private static final byte HEADER_UNICAST = 1;

	private static final int PACKET_SIZE = 128;
	private static final int SNDBUF_SIZE = (int) Math.pow(2, 13);
	private static final int RCVBUF_SIZE = (int) Math.pow(2, 21);

	private final String multicastGroup;
	private final int multicastPort;
	private final InetSocketAddress multicastAddress;

	private final List<MulticastManagerListener> listeners = Collections
			.synchronizedList(new ArrayList<MulticastManagerListener>());

	private MulticastSocket multicastSocket;

	private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<DatagramPacket>();

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	public MulticastManager(String multicastGroup, int multicastPort, MulticastManagerListener listener) {

		this.multicastGroup = multicastGroup;
		this.multicastPort = multicastPort;
		this.multicastAddress = new InetSocketAddress(multicastGroup, multicastPort);

		listeners.add(listener);

		new Thread(this::receive).start();
		new Thread(this::process).start();

	}

	public void send(final String dataStr, final Set<InetAddress> unicastIps) {

		taskQueue.execute(() -> {

			try {

				byte[] dataBytes = dataStr.getBytes(CHARSET);

				ByteBuffer multicastDataBuffer = ByteBuffer.allocate(dataBytes.length + 1).put(HEADER_MULTICAST)
						.put(dataBytes);
				byte[] encryptedMulticastData = DmsSecurity.encrypt(multicastDataBuffer.array());

				DatagramPacket sendPacket = new DatagramPacket(encryptedMulticastData, encryptedMulticastData.length,
						multicastAddress);

				send(sendPacket);

				if (unicastIps.isEmpty())
					return;

				ByteBuffer unicastDataBuffer = ByteBuffer.allocate(dataBytes.length + 1).put(HEADER_UNICAST)
						.put(dataBytes);
				byte[] encryptedUnicastData = DmsSecurity.encrypt(unicastDataBuffer.array());

				for (InetAddress unicastIp : unicastIps) {

					DatagramPacket unicastSendPacket = new DatagramPacket(encryptedUnicastData,
							encryptedUnicastData.length, new InetSocketAddress(unicastIp, multicastPort));

					send(unicastSendPacket);

				}

			} catch (Exception e) {

			}

		});

	}

	private void send(DatagramPacket packet) throws IOException {

		try {

			getMulticastSocket().send(packet);

		} catch (IOException e) {

			closeMulticastSocket();

			throw e;

		}

	}

	private synchronized MulticastSocket getMulticastSocket() throws IOException {

		if (multicastSocket == null) {

			multicastSocket = new MulticastSocket(multicastPort);

			multicastSocket.joinGroup(InetAddress.getByName(multicastGroup));

			multicastSocket.setTimeToLive(32);
			multicastSocket.setSendBufferSize(SNDBUF_SIZE);
			multicastSocket.setReceiveBufferSize(RCVBUF_SIZE);

		}

		return multicastSocket;

	}

	private synchronized void closeMulticastSocket() {

		if (multicastSocket != null) {

			multicastSocket.close();

			multicastSocket = null;

		}

	}

	private void receive() {

		while (!Thread.currentThread().isInterrupted()) {

			try {

				DatagramPacket receivePacket = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);

				getMulticastSocket().receive(receivePacket);

				receiveQueue.offer(receivePacket);

			} catch (IOException e) {

				closeMulticastSocket();

				try {

					Thread.sleep(CommonConstants.CONN_TIMEOUT_MS);

				} catch (InterruptedException e1) {

				}

			}

		}

	}

	private void process() {

		while (!Thread.currentThread().isInterrupted()) {

			try {

				DatagramPacket receivePacket = receiveQueue.take();

				ByteBuffer dataBuffer = ByteBuffer
						.wrap(DmsSecurity.decrypt(receivePacket.getData(), receivePacket.getLength()));

				boolean isUnicast = dataBuffer.get() == HEADER_UNICAST;
				byte[] dataBytes = new byte[dataBuffer.remaining()];
				dataBuffer.get(dataBytes);
				String dataStr = new String(dataBytes, CHARSET);

				InetAddress remoteAddress = receivePacket.getAddress();

				listeners.forEach(listener -> listener.udpMessageReceived(remoteAddress, dataStr, isUnicast));

			} catch (Exception e) {

				e.printStackTrace();

			}

		}

	}

}

package com.ogya.dms.server.communications.udp;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.common.DmsSecurity;
import com.ogya.dms.server.communications.intf.UdpManagerListener;

public class UdpManager {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static final byte HEADER_BROADCAST = 0;
	private static final byte HEADER_UNICAST = 1;

	private static final int PACKET_SIZE = 128;
	private static final int SNDBUF_SIZE = (int) Math.pow(2, 13);
	private static final int RCVBUF_SIZE = (int) Math.pow(2, 21);

	private final int udpPort;

	private final List<UdpManagerListener> listeners = Collections
			.synchronizedList(new ArrayList<UdpManagerListener>());

	private DatagramSocket datagramSocket;

	private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<DatagramPacket>();

	public UdpManager(int udpPort, UdpManagerListener listener) {

		this.udpPort = udpPort;

		listeners.add(listener);

		new Thread(this::receive).start();
		new Thread(this::process).start();

	}

	public void send(final String dataStr, final Set<InetAddress> unicastIps) {

		try {

			byte[] dataBytes = dataStr.getBytes(CHARSET);

			ByteBuffer broadcastDataBuffer = ByteBuffer.allocate(dataBytes.length + 1).put(HEADER_BROADCAST)
					.put(dataBytes);
			byte[] encryptedBroadcastData = DmsSecurity.encrypt(broadcastDataBuffer.array());

			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
					if (ia.getBroadcast() == null || ia.getAddress().isAnyLocalAddress()
							|| ia.getAddress().isLoopbackAddress())
						continue;
					DatagramPacket sendPacket = new DatagramPacket(encryptedBroadcastData,
							encryptedBroadcastData.length, ia.getBroadcast(), udpPort);
					send(sendPacket);
				}
			}

			if (unicastIps.isEmpty())
				return;

			ByteBuffer unicastDataBuffer = ByteBuffer.allocate(dataBytes.length + 1).put(HEADER_UNICAST).put(dataBytes);
			byte[] encryptedUnicastData = DmsSecurity.encrypt(unicastDataBuffer.array());

			for (InetAddress unicastIp : unicastIps) {

				DatagramPacket unicastSendPacket = new DatagramPacket(encryptedUnicastData, encryptedUnicastData.length,
						new InetSocketAddress(unicastIp, udpPort));

				send(unicastSendPacket);

			}

		} catch (IOException e) {

		} catch (Exception e) {
			System.out.println("Unable to send data packet, possibly due to an encryption failure.");
		}

	}

	private void send(DatagramPacket packet) throws IOException {

		try {

			getDatagramSocket().send(packet);

		} catch (BindException e) {

		} catch (IOException e) {

			closeDatagramSocket();

			throw e;

		}

	}

	private synchronized DatagramSocket getDatagramSocket() throws IOException {

		if (datagramSocket == null) {

			datagramSocket = new DatagramSocket(udpPort);

			datagramSocket.setBroadcast(true);
			datagramSocket.setSendBufferSize(SNDBUF_SIZE);
			datagramSocket.setReceiveBufferSize(RCVBUF_SIZE);

		}

		return datagramSocket;

	}

	private synchronized void closeDatagramSocket() {

		if (datagramSocket != null) {

			datagramSocket.close();

			datagramSocket = null;

		}

	}

	private void receive() {

		while (!Thread.currentThread().isInterrupted()) {

			try {

				DatagramPacket receivePacket = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);

				getDatagramSocket().receive(receivePacket);

				receiveQueue.offer(receivePacket);

			} catch (IOException e) {

				closeDatagramSocket();

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
				System.out.println("Unable to receive data packet, possibly from an unrecognized entity.");
			}

		}

	}

}

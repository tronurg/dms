package com.dms.server.communications.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
import java.util.concurrent.LinkedBlockingQueue;

import com.dms.server.communications.intf.UdpManagerListener;
import com.dms.server.util.Commons;
import com.dms.server.util.DmsSecurity;

public class UdpManager {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static final byte HEADER_MULTICAST = 0;
	private static final byte HEADER_UNICAST = 1;

	private static final int PACKET_SIZE = 128;
	private static final int TTL = 32;

	private final InetAddress multicastGroup;
	private final int udpPort;

	private final List<UdpManagerListener> listeners = Collections
			.synchronizedList(new ArrayList<UdpManagerListener>());

	private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<DatagramPacket>();

	public UdpManager(InetAddress multicastGroup, int udpPort, UdpManagerListener listener) {

		this.multicastGroup = multicastGroup;
		this.udpPort = udpPort;

		listeners.add(listener);

		new Thread(this::receive).start();
		new Thread(this::process).start();

	}

	public void send(final String dataStr, final Set<InetAddress> unicastIps) {

		try {

			byte[] dataBytes = dataStr.getBytes(CHARSET);

			sendMulticast(dataBytes);

			if (unicastIps == null || unicastIps.isEmpty()) {
				return;
			}

			sendUnicast(dataBytes, unicastIps);

		} catch (Exception e) {
			System.out.println("Unable to send data packet, possibly due to an encryption failure.");
		}

	}

	private void sendMulticast(final byte[] data) throws Exception {

		ByteBuffer dataBuffer = ByteBuffer.allocate(data.length + 1).put(HEADER_MULTICAST).put(data);
		byte[] encryptedData = DmsSecurity.encrypt(dataBuffer.array());

		DatagramPacket packet = new DatagramPacket(encryptedData, encryptedData.length, multicastGroup, udpPort);

		for (InetAddress addr : Commons.getLocalIPv4Addresses()) {
			try (MulticastSocket socket = new MulticastSocket(new InetSocketAddress(addr, udpPort))) {
				socket.setTimeToLive(TTL);
				socket.send(packet);
			} catch (Exception e) {

			}
		}

	}

	private void sendUnicast(final byte[] data, final Set<InetAddress> addrs) throws Exception {

		ByteBuffer dataBuffer = ByteBuffer.allocate(data.length + 1).put(HEADER_UNICAST).put(data);
		byte[] encryptedData = DmsSecurity.encrypt(dataBuffer.array());

		try (DatagramSocket socket = new DatagramSocket(null)) {
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(udpPort));
			for (InetAddress addr : addrs) {
				DatagramPacket packet = new DatagramPacket(encryptedData, encryptedData.length, addr, udpPort);
				try {
					socket.send(packet);
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {

		}

	}

	private void receive() {

		while (!Thread.currentThread().isInterrupted()) {

			try (MulticastSocket socket = new MulticastSocket(udpPort)) {

				socket.setLoopbackMode(true);
				socket.joinGroup(multicastGroup);

				while (!socket.isClosed()) {
					DatagramPacket receivePacket = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
					socket.receive(receivePacket);
					receiveQueue.offer(receivePacket);
				}

			} catch (Exception e) {

			}

			try {
				Thread.sleep(Commons.CONN_TIMEOUT_MS);
			} catch (InterruptedException e) {

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

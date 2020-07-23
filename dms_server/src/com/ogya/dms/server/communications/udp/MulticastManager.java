package com.ogya.dms.server.communications.udp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;

import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.common.Encryption;

public class MulticastManager {

	private static final int PACKET_SIZE = 128;
	private static final int SNDBUF_SIZE = (int) Math.pow(2, 13);
	private static final int RCVBUF_SIZE = (int) Math.pow(2, 21);

	private final String multicastGroup;
	private final int multicastPort;
	private final InetSocketAddress multicastAddress;

	private final BiConsumer<InetAddress, String> messageConsumer;

	private MulticastSocket multicastSocket;

	private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<DatagramPacket>();

	private final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	public MulticastManager(String multicastGroup, int multicastPort, BiConsumer<InetAddress, String> messageConsumer) {

		this.multicastGroup = multicastGroup;
		this.multicastPort = multicastPort;
		this.multicastAddress = new InetSocketAddress(multicastGroup, multicastPort);

		this.messageConsumer = messageConsumer;

		new Thread(this::receive).start();

		new Thread(this::process).start();

	}

	public void send(final String dataStr) {

		taskQueue.execute(() -> {

			try {

				byte[] encryptedData = Encryption.encrypt(dataStr);

				DatagramPacket sendPacket = new DatagramPacket(encryptedData, encryptedData.length, multicastAddress);

				getMulticastSocket().send(sendPacket);

			} catch (UnsupportedEncodingException e) {

			} catch (GeneralSecurityException e) {

			} catch (IOException e) {

				closeMulticastSocket();

			}

		});

	}

	private synchronized MulticastSocket getMulticastSocket() throws IOException {

		if (multicastSocket == null) {

			multicastSocket = new MulticastSocket(multicastPort);

			multicastSocket.joinGroup(InetAddress.getByName(multicastGroup));

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

		while (true) {

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

		while (true) {

			try {

				DatagramPacket receivePacket = receiveQueue.take();

				InetAddress remoteAddress = receivePacket.getAddress();
				String data = Encryption.decrypt(Arrays.copyOf(receivePacket.getData(), receivePacket.getLength()));

				messageConsumer.accept(remoteAddress, data);

			} catch (InterruptedException e) {

			} catch (GeneralSecurityException e) {

			} catch (IOException e) {

			}

		}

	}

}

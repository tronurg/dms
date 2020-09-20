package com.ogya.dms.server.communications.udp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import com.ogya.dms.server.common.CommonConstants;
import com.ogya.dms.server.common.Encryption;
import com.ogya.dms.server.communications.intf.MulticastManagerListener;

public class MulticastManager {

	private static final int PACKET_SIZE = 128;
	private static final int SNDBUF_SIZE = (int) Math.pow(2, 13);
	private static final int RCVBUF_SIZE = (int) Math.pow(2, 21);

	private final String multicastGroup;
	private final int multicastPort;
	private final InetSocketAddress multicastAddress;

	private final MulticastManagerListener listener;

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

	public MulticastManager(String multicastGroup, int multicastPort, MulticastManagerListener listener) {

		this.multicastGroup = multicastGroup;
		this.multicastPort = multicastPort;
		this.multicastAddress = new InetSocketAddress(multicastGroup, multicastPort);

		this.listener = listener;

		new Thread(this::receive).start();

		new Thread(this::process).start();

	}

	public void send(final String dataStr, final Set<String> unicastIps) {

		taskQueue.execute(() -> {

			try {

				byte[] encryptedData = Encryption.encrypt(dataStr);
				byte[] encryptedDataWithSuffix = Arrays.copyOf(encryptedData, encryptedData.length + 1);

				DatagramPacket sendPacket = new DatagramPacket(encryptedDataWithSuffix, encryptedDataWithSuffix.length,
						multicastAddress);

				getMulticastSocket().send(sendPacket);

				if (unicastIps.isEmpty())
					return;

				encryptedDataWithSuffix[encryptedDataWithSuffix.length - 1] = 1;

				for (String unicastIp : unicastIps) {

					DatagramPacket unicastSendPacket = new DatagramPacket(encryptedDataWithSuffix,
							encryptedDataWithSuffix.length, new InetSocketAddress(unicastIp, multicastPort));

					getMulticastSocket().send(unicastSendPacket);

				}

			} catch (UnsupportedEncodingException e) {

				e.printStackTrace();

			} catch (GeneralSecurityException e) {

				e.printStackTrace();

			} catch (IOException e) {

				closeMulticastSocket();

			}

		});

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

				byte[] encryptedDataWithSuffix = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
				boolean isUnicast = encryptedDataWithSuffix[encryptedDataWithSuffix.length - 1] == 1;

				byte[] encryptedData = Arrays.copyOf(encryptedDataWithSuffix, encryptedDataWithSuffix.length - 1);

				InetAddress remoteAddress = receivePacket.getAddress();
				String data = Encryption.decrypt(encryptedData);

				listener.udpMessageReceived(remoteAddress, data, isUnicast);

			} catch (InterruptedException e) {

				e.printStackTrace();

			} catch (GeneralSecurityException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

	}

}

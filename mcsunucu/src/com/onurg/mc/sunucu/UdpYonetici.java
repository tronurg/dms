package com.onurg.mc.sunucu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

class UdpYonetici {

	private static final int PACKET_SIZE = 512;
	private static final byte DELIMITER = 10;
	private static final int SNDBUF_SIZE = (int) Math.pow(2, 13);
	private static final int RCVBUF_SIZE = (int) Math.pow(2, 21);

	private final InetSocketAddress localSocketAddress;

	private final Consumer<UdpNesnesi> messageConsumer;

	private DatagramChannel udpChannel;

	private final BlockingQueue<SimpleEntry<InetSocketAddress, ByteBuffer>> receiveQueue = new LinkedBlockingQueue<SimpleEntry<InetSocketAddress, ByteBuffer>>();

	private final Map<InetSocketAddress, ByteArrayOutputStream> baosMap = Collections
			.synchronizedMap(new HashMap<InetSocketAddress, ByteArrayOutputStream>());

	private final ExecutorService out = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	UdpYonetici(String ip, int port, Consumer<UdpNesnesi> messageConsumer) throws UnknownHostException {

		localSocketAddress = new InetSocketAddress(ip, port);

		this.messageConsumer = messageConsumer;

		new Thread(this::receive).start();

		new Thread(this::process).start();

	}

	void gonder(final String dataStr, final String adres) {

		out.execute(() -> {

			try {

				InetSocketAddress remoteSocketAddress = new InetSocketAddress(adres, localSocketAddress.getPort());

				byte[] data = dataStr.getBytes("UTF-8");

				ByteBuffer sendBuffer = ByteBuffer.allocate(data.length + 1);
				sendBuffer.put(data);
				sendBuffer.put(DELIMITER);
				sendBuffer.rewind();

				int numberOfPackets = (sendBuffer.capacity() + PACKET_SIZE - 1) / PACKET_SIZE;

				for (int i = 0; i < numberOfPackets; i++) {

					int dataLength = Math.min(PACKET_SIZE, sendBuffer.capacity() - i * PACKET_SIZE);

					sendBuffer.limit(i * PACKET_SIZE + dataLength);

					getUdpChannel().send(sendBuffer, remoteSocketAddress);

				}

			} catch (UnsupportedEncodingException e) {

			} catch (IOException e) {

				closeUdpChannel();

			}

		});

	}

	private synchronized DatagramChannel getUdpChannel() throws IOException {

		if (udpChannel == null) {

			udpChannel = DatagramChannel.open();

			udpChannel.bind(localSocketAddress);

			udpChannel.configureBlocking(true);

			udpChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
			udpChannel.setOption(StandardSocketOptions.SO_SNDBUF, SNDBUF_SIZE);
			udpChannel.setOption(StandardSocketOptions.SO_RCVBUF, RCVBUF_SIZE);

		}

		return udpChannel;

	}

	private synchronized void closeUdpChannel() {

		if (udpChannel != null) {

			try {

				udpChannel.close();

			} catch (IOException e) {

			}

			udpChannel = null;

			baosMap.clear();

		}

	}

	private void receive() {

		while (true) {

			ByteBuffer receiveBuffer = ByteBuffer.allocate(PACKET_SIZE);

			try {

				InetSocketAddress remoteSocketAddress = (InetSocketAddress) getUdpChannel().receive(receiveBuffer);

				receiveQueue.offer(new SimpleEntry<InetSocketAddress, ByteBuffer>(remoteSocketAddress, receiveBuffer));

			} catch (IOException e) {

				closeUdpChannel();

				try {

					Thread.sleep(5000);

				} catch (InterruptedException e1) {

				}

			}

		}

	}

	private void process() {

		while (true) {

			try {

				SimpleEntry<InetSocketAddress, ByteBuffer> receive = receiveQueue.take();
				InetSocketAddress remoteSocketAddress = receive.getKey();
				ByteBuffer receiveBuffer = receive.getValue();

				if (!baosMap.containsKey(remoteSocketAddress))
					baosMap.put(remoteSocketAddress, new ByteArrayOutputStream());

				ByteArrayOutputStream baos = baosMap.get(remoteSocketAddress);

				receiveBuffer.flip();

				for (int i = 0; i < receiveBuffer.limit(); i++) {

					byte b = receiveBuffer.get();

					if (b == DELIMITER) {

						try {

							baos.flush();

						} catch (IOException e1) {

						}

						try {

							messageConsumer.accept(new UdpNesnesi(baos.toString("UTF-8"), remoteSocketAddress.getPort(),
									localSocketAddress.getPort()));

						} catch (UnsupportedEncodingException e) {

						}

						baosMap.remove(remoteSocketAddress);

						break;

					}

					baos.write(b);

				}

			} catch (InterruptedException e) {

			}

		}

	}

}

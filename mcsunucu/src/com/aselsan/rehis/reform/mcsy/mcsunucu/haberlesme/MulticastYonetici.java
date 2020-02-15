package com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;

import com.aselsan.rehis.reform.mcsy.mcsunucu.ortak.OrtakSabitler;
import com.aselsan.rehis.reform.mcsy.mcsunucu.ortak.Sifreleme;

public class MulticastYonetici {

	private static final int PACKET_SIZE = 512;
	private static final int PACKET_OVERHEAD = 4;
	private static final int PACKET_DATA_SIZE = PACKET_SIZE - PACKET_OVERHEAD - OrtakSabitler.ENCRYPTION_OVERHEAD;
	private static final int SNDBUF_SIZE = (int) Math.pow(2, 13);
	private static final int RCVBUF_SIZE = (int) Math.pow(2, 21);

	private final String multicastGroup;
	private final int multicastPort;
	private final InetSocketAddress multicastAddress;

	private final BiConsumer<String, String> messageConsumer;

	private MulticastSocket multicastSocket;

	private final ByteBuffer sendBuffer = ByteBuffer.allocate(PACKET_SIZE);

	private final BlockingQueue<DatagramPacket> receiveQueue = new LinkedBlockingQueue<DatagramPacket>();

	private final Map<InetAddress, Map<Integer, UdpMesaj>> mesajMap = Collections
			.synchronizedMap(new HashMap<InetAddress, Map<Integer, UdpMesaj>>());

	private final ExecutorService out = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	public MulticastYonetici(String multicastGroup, int multicastPort, BiConsumer<String, String> messageConsumer) {

		this.multicastGroup = multicastGroup;
		this.multicastPort = multicastPort;
		this.multicastAddress = new InetSocketAddress(multicastGroup, multicastPort);

		this.messageConsumer = messageConsumer;

		new Thread(this::receive).start();

		new Thread(this::process).start();

	}

	public void gonder(final String dataStr, final int gonderenId, final int paketId) {

		out.execute(() -> {

			try {

				byte[] data = dataStr.getBytes("UTF-8");

				int parcaSayisi = (data.length + PACKET_DATA_SIZE - 1) / PACKET_DATA_SIZE;

				for (int parcaNo = 0; parcaNo < parcaSayisi; parcaNo++) {

					int dataLength = Math.min(PACKET_DATA_SIZE, data.length - parcaNo * PACKET_DATA_SIZE);

					sendBuffer.clear();
					sendBuffer.put((byte) gonderenId);
					sendBuffer.put((byte) paketId);
					sendBuffer.put((byte) parcaSayisi);
					sendBuffer.put((byte) parcaNo);
					sendBuffer.put(data, parcaNo * PACKET_DATA_SIZE, dataLength);
					sendBuffer.flip();

					byte[] encryptedData = Sifreleme.encrypt(Arrays.copyOf(sendBuffer.array(), sendBuffer.limit()));

					DatagramPacket sendPacket = new DatagramPacket(encryptedData, encryptedData.length,
							multicastAddress);

					getMulticastSocket().send(sendPacket);

				}

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

			mesajMap.clear();

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

					Thread.sleep(5000);

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
				byte[] receivedData = Sifreleme
						.decrypt(Arrays.copyOf(receivePacket.getData(), receivePacket.getLength()));
				ByteBuffer receiveBuffer = ByteBuffer.wrap(receivedData);

				int gonderenId = receiveBuffer.get() & 0xFF;
				int mesajId = receiveBuffer.get() & 0xFF;
				int parcaSayisi = receiveBuffer.get() & 0xFF;
				int parcaNo = receiveBuffer.get() & 0xFF;
				byte[] mesaj = new byte[receiveBuffer.remaining()];
				receiveBuffer.get(mesaj);

				if (!mesajMap.containsKey(remoteAddress))
					mesajMap.put(remoteAddress, new HashMap<Integer, UdpMesaj>());
				UdpMesaj udpMesaj = mesajMap.get(remoteAddress).get(gonderenId);
				if (udpMesaj == null || !udpMesaj.karsilastir(mesajId, parcaSayisi)) {
					udpMesaj = new UdpMesaj(mesajId, parcaSayisi);
					mesajMap.get(remoteAddress).put(gonderenId, udpMesaj);
				}

				boolean tamamlandi = udpMesaj.mesajAlindi(parcaNo, mesaj);

				if (tamamlandi) {

					messageConsumer.accept(remoteAddress.getHostAddress(), new String(udpMesaj.mesajiAl(), "UTF-8"));

					mesajMap.get(remoteAddress).remove(gonderenId);

					if (mesajMap.get(remoteAddress).isEmpty())
						mesajMap.remove(remoteAddress);

				}

			} catch (InterruptedException e) {

			} catch (GeneralSecurityException e) {

			} catch (IOException e) {

			}

		}

	}

}

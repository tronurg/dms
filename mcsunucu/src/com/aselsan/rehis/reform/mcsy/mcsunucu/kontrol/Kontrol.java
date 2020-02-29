package com.aselsan.rehis.reform.mcsy.mcsunucu.kontrol;

import java.io.IOException;
import java.net.InetAddress;
import java.util.AbstractMap.SimpleEntry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.intf.TcpYoneticiDinleyici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.tcp.TcpBaglantiTipi;
import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.tcp.TcpYonetici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.udp.MulticastYonetici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.model.Model;
import com.aselsan.rehis.reform.mcsy.mcsunucu.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.ortak.OrtakSabitler;

public class Kontrol implements TcpYoneticiDinleyici, ModelDinleyici {

	private static final String MC_UUID = UUID.randomUUID().toString();

	private static Kontrol instance;

	private final Model model = new Model(this);

	private final int routerPort = OrtakSabitler.INTERCOM_PORT;
	private final int pubPort = OrtakSabitler.INTERCOM_PORT + 1;
	private final String multicastGroup = OrtakSabitler.MULTICAST_IP;
	private final int multicastPort = OrtakSabitler.MULTICAST_PORT;
	private final int comPortIlk = OrtakSabitler.COM_PORT_ILK;
	private final int comPortSon = OrtakSabitler.COM_PORT_SON;

	private final MulticastYonetici multicastYonetici = new MulticastYonetici(multicastGroup, multicastPort,
			this::receiveUdpMessage);

	private TcpYonetici tcpYonetici;

	private final ZContext context = new ZContext();

	private final LinkedBlockingQueue<SimpleEntry<String, String>> pubQueue = new LinkedBlockingQueue<SimpleEntry<String, String>>();

	private final ExecutorService islemKuyrugu = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	private Kontrol() {

	}

	public synchronized static Kontrol getInstance() {

		if (instance == null) {

			instance = new Kontrol();

		}

		return instance;

	}

	public void start() {

		new Thread(this::router).start();
		new Thread(this::pub).start();
		new Thread(this::monitor).start();

	}

	private TcpYonetici getTcpYonetici() throws IOException {

		if (tcpYonetici == null) {

			tcpYonetici = new TcpYonetici(comPortIlk, comPortIlk + 1, comPortSon);

			tcpYonetici.dinleyiciEkle(this);

		}

		return tcpYonetici;

	}

	private void receiveUdpMessage(InetAddress gonderenAdres, String mesaj) {

		String[] uuids = mesaj.split(" ");
		if (uuids.length != 2 || MC_UUID.equals(uuids[0]))
			return;

		try {

			getTcpYonetici().baglantiEkle(uuids[0], uuids[1], gonderenAdres,
					uuids[0].compareTo(MC_UUID) < 0 ? TcpBaglantiTipi.SUNUCU : TcpBaglantiTipi.ISTEMCI);

		} catch (IOException e) {

		}

	}

	private void router() {

		try (ZMQ.Socket routerSocket = context.createSocket(SocketType.ROUTER)) {

			routerSocket.monitor("inproc://monitor", ZMQ.EVENT_DISCONNECTED);

			routerSocket.setRouterMandatory(true);
			routerSocket.bind("tcp://*:" + routerPort);

			while (!Thread.currentThread().isInterrupted()) {

				String dealerId = routerSocket.recvStr();
				String mesajNesnesiStr = routerSocket.recvStr();

				if (MC_UUID.equals(dealerId)) {

					model.tumYerelBeaconlariAl().forEach((uuid, beacon) -> {

						try {

							routerSocket.sendMore(uuid);
							routerSocket.send("");

						} catch (ZMQException e) {

							islemKuyrugu.execute(() -> model.yerelKullaniciKoptu(uuid));

						}

					});

				} else {

					islemKuyrugu.execute(() -> model.yerelMesajAlindi(mesajNesnesiStr));

				}

			}

		} catch (Exception e) {

			System.out.println(routerPort + " portu kullaniliyor. Istemcilerden veri alinamayacak!");

		}

	}

	private void pub() {

		try (ZMQ.Socket pubSocket = context.createSocket(SocketType.PUB)) {

			pubSocket.bind("tcp://*:" + pubPort);

			while (!Thread.currentThread().isInterrupted()) {

				SimpleEntry<String, String> aliciMesaj = pubQueue.take();

				pubSocket.sendMore(aliciMesaj.getKey() + "\n");
				pubSocket.send(aliciMesaj.getValue());

			}

		} catch (Exception e) {

			System.out.println(pubPort + " portu kullaniliyor. Istemcilere veri gonderilemeyecek!");

		}

	}

	private void monitor() {

		try (ZMQ.Socket monitorSocket = context.createSocket(SocketType.PAIR);
				ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER)) {

			dealerSocket.setIdentity(MC_UUID.getBytes(ZMQ.CHARSET));
			dealerSocket.setImmediate(false);
			dealerSocket.connect("tcp://localhost:" + routerPort);

			monitorSocket.connect("inproc://monitor");

			while (!Thread.currentThread().isInterrupted()) {

				ZMQ.Event event = ZMQ.Event.recv(monitorSocket);
				switch (event.getEvent()) {
				case ZMQ.EVENT_DISCONNECTED:
					dealerSocket.send("", ZMQ.DONTWAIT);
					break;
				}

			}

		} catch (Exception e) {

			System.out.println(pubPort + " portu kullaniliyor. Istemcilere veri gonderilemeyecek!");

		}

	}

	@Override
	public void uzakSunucuyaBaglanildi(final String mcUuid) {

		model.tumYerelBeaconlariAl().forEach((uuid, beacon) -> {

			try {

				getTcpYonetici().sunucuyaMesajGonder(mcUuid, beacon);

			} catch (IOException e) {

			}

		});

	}

	@Override
	public void uzakKullaniciKoptu(String uuid) {

		islemKuyrugu.execute(() -> model.uzakKullaniciKoptu(uuid));

	}

	@Override
	public void mesajAlindi(String mesaj) {

		islemKuyrugu.execute(() -> model.uzakMesajAlindi(mesaj));

	}

	@Override
	public void yerelKullanicilaraGonder(String aliciUuid, String mesaj) {

		pubQueue.offer(new SimpleEntry<String, String>(aliciUuid, mesaj));

	}

	@Override
	public void tumUzakKullanicilaraGonder(String mesaj) {

		try {

			getTcpYonetici().tumSunucularaMesajGonder(mesaj);

		} catch (IOException e) {

		}

	}

	@Override
	public void uuidYayinla(String uuid) {

		multicastYonetici.gonder(MC_UUID + " " + uuid);

	}

}

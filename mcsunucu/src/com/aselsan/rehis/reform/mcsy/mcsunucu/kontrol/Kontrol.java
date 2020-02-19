package com.aselsan.rehis.reform.mcsy.mcsunucu.kontrol;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.intf.TcpYoneticiDinleyici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.tcp.TcpYonetici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.udp.MulticastYonetici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.ortak.OrtakSabitler;
import com.aselsan.rehis.reform.mcsy.mcsunucu.veriyapilari.MesajNesnesi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Kontrol implements TcpYoneticiDinleyici {

	private static final String MC_UUID = UUID.randomUUID().toString();

	private static Kontrol instance;

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

	private final LinkedBlockingQueue<MesajNesnesi> pubQueue = new LinkedBlockingQueue<MesajNesnesi>();

	private final Gson gson = new Gson();

	private final Map<String, String> dealerBeacon = new HashMap<String, String>();

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

			getTcpYonetici().baglantiEkle(uuids[0], uuids[1], gonderenAdres);

		} catch (IOException e) {

		}

	}

	private void router() {

		try (ZMQ.Socket routerSocket = context.createSocket(SocketType.ROUTER)) {

			routerSocket.bind("tcp://*:" + routerPort);

			while (!Thread.currentThread().isInterrupted()) {

				String dealerId = routerSocket.recvStr();
				String mesajNesnesiStr = routerSocket.recvStr();

				try {

					MesajNesnesi mesajNesnesi = gson.fromJson(mesajNesnesiStr, MesajNesnesi.class);

					switch (mesajNesnesi.tip) {

					case "BCON":

						// uuid'ye ait kimlik degismisse yeni kimligi kaydet ve diger islemleri yap
						if (!mesajNesnesiStr.equals(dealerBeacon.get(dealerId))) {

							dealerBeacon.put(dealerId, mesajNesnesiStr);

							pubQueue.offer(mesajNesnesi);

							try {

								getTcpYonetici().sunucudanTumBaglantilaraGonder(mesajNesnesiStr);

							} catch (IOException e) {

							}

						}

						multicastYonetici.gonder(MC_UUID + " " + dealerId);

						break;

					default:

					}

				} catch (JsonSyntaxException e) {

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

				try {

					MesajNesnesi mesajNesnesi = pubQueue.take();

					String mesaj = gson.toJson(mesajNesnesi);

					pubSocket.sendMore(mesajNesnesi.aliciUuid + "\n");
					pubSocket.send(mesaj);

				} catch (InterruptedException e) {

				}

			}

		} catch (Exception e) {

			System.out.println(pubPort + " portu kullaniliyor. Istemcilere veri gonderilemeyecek!");

		}

	}

	@Override
	public void baglantiKuruldu(final int id) {

		dealerBeacon.forEach((dealer, beacon) -> {

			try {

				getTcpYonetici().sunucudanMesajGonder(id, beacon);

			} catch (IOException e) {

			}

		});

	}

	@Override
	public void uuidKoptu(String uuid) {

		// TODO

	}

	@Override
	public void mesajAlindi(String mesaj) {

		try {

			MesajNesnesi mesajNesnesi = gson.fromJson(mesaj, MesajNesnesi.class);

			pubQueue.offer(mesajNesnesi);

		} catch (JsonSyntaxException e) {

		}

	}

}

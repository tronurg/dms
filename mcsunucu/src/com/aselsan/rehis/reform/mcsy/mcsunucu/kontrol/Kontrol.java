package com.aselsan.rehis.reform.mcsy.mcsunucu.kontrol;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.udp.MulticastYonetici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.ortak.OrtakSabitler;
import com.aselsan.rehis.reform.mcsy.mcsunucu.veriyapilari.MesajNesnesi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Kontrol {

	private static Kontrol instance;

	private final int routerPort = OrtakSabitler.INTERCOM_PORT;
	private final int pubPort = OrtakSabitler.INTERCOM_PORT + 1;
	private final String multicastGroup = OrtakSabitler.MULTICAST_IP;
	private final int multicastPort = OrtakSabitler.MULTICAST_PORT;
	private final int comPortIn = OrtakSabitler.COM_PORT;
	private final int comPortOut = OrtakSabitler.COM_PORT + 1;

	private MulticastYonetici multicastYonetici;

	private final ZContext context = new ZContext();

	private final LinkedBlockingQueue<MesajNesnesi> pubQueue = new LinkedBlockingQueue<MesajNesnesi>();

	private final Gson gson = new Gson();

	private final Map<String, String> beaconMap = new HashMap<String, String>();

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

	private MulticastYonetici getMulticastYonetici() {

		if (multicastYonetici == null) {

			multicastYonetici = new MulticastYonetici(multicastGroup, multicastPort, this::receiveUdpMessage);

		}

		return multicastYonetici;

	}

	private void receiveUdpMessage(InetAddress gonderenAdres, String mesaj) {

		try {

			MesajNesnesi mesajNesnesi = new MesajNesnesi(mesaj, "BCON");

			pubQueue.offer(mesajNesnesi);

		} catch (JsonSyntaxException e) {

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

						try {

							// uuid'ye ait kimlik degismisse yeni kimligi kaydet ve diger islemleri yap
							if (!mesajNesnesiStr.equals(beaconMap.get(dealerId))) {

								beaconMap.put(dealerId, mesajNesnesiStr);

								// TODO

							}

							getMulticastYonetici().gonder(dealerId);

						} catch (JsonSyntaxException e) {

						}

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

	private void cevrimiciKullanicilaraGonder() {

	}

}

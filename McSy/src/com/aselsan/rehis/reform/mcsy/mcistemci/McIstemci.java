package com.aselsan.rehis.reform.mcsy.mcistemci;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.aselsan.rehis.reform.mcsy.mcistemci.intf.McIstemciDinleyici;
import com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari.MesajNesnesi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class McIstemci {

	private final String uuid;

	private final ZContext context = new ZContext();

	private final String serverIp;
	private final int dealerPort;
	private final int subPort;

	private final McIstemciDinleyici dinleyici;

	private final LinkedBlockingQueue<String> dealerQueue = new LinkedBlockingQueue<String>();

	private final Gson gson = new Gson();

	private final ExecutorService out = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	public McIstemci(String uuid, String comIp, int comPort, McIstemciDinleyici dinleyici) {

		this.uuid = uuid;

		this.serverIp = comIp;
		this.dealerPort = comPort;
		this.subPort = comPort + 1;

		this.dinleyici = dinleyici;

		start();

	}

	private void start() {

		Thread dealerThread = new Thread(this::dealer);
		dealerThread.setDaemon(true);
		dealerThread.start();

		Thread subThread = new Thread(this::sub);
		subThread.setDaemon(true);
		subThread.start();

		Thread monitorThread = new Thread(this::monitor);
		monitorThread.setDaemon(true);
		monitorThread.start();

	}

	public void beaconGonder(String mesaj) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, "BCON")));

	}

	public void tumBeaconlariIste() {

		dealerQueue.offer(gson.toJson(new MesajNesnesi("", uuid, "BCON?")));

	}

	private void dealer() {

		try (ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER)) {

			dealerSocket.monitor("inproc://monitor", ZMQ.EVENT_CONNECTED | ZMQ.EVENT_DISCONNECTED);

			dealerSocket.setIdentity(uuid.getBytes(ZMQ.CHARSET));
			dealerSocket.setRcvHWM(1);
			dealerSocket.setImmediate(false);
			dealerSocket.connect("tcp://" + serverIp + ":" + dealerPort);

			while (!Thread.currentThread().isInterrupted()) {

				try {

					String mesaj = dealerQueue.take();

					dealerSocket.send(mesaj, ZMQ.DONTWAIT);

				} catch (InterruptedException e) {

				}

			}

		}

	}

	private void sub() {

		try (ZMQ.Socket subSocket = context.createSocket(SocketType.SUB)) {

			subSocket.connect("tcp://" + serverIp + ":" + subPort);
			subSocket.subscribe("\n");
			subSocket.subscribe(uuid + "\n");

			while (!Thread.currentThread().isInterrupted()) {

				subSocket.recvStr();
				String receiveStr = subSocket.recvStr();

				gelenMesajiIsle(receiveStr);

			}

		}

	}

	private void monitor() {

		try (ZMQ.Socket monitorSocket = context.createSocket(SocketType.PAIR)) {

			monitorSocket.connect("inproc://monitor");

			while (!Thread.currentThread().isInterrupted()) {

				ZMQ.Event event = ZMQ.Event.recv(monitorSocket);
				switch (event.getEvent()) {
				case ZMQ.EVENT_CONNECTED:
					dinleyiciyeSunucuBaglantiDurumuGuncellendi(true);
					break;
				case ZMQ.EVENT_DISCONNECTED:
					dinleyiciyeSunucuBaglantiDurumuGuncellendi(false);
					break;
				}

			}

		}

	}

	private void gelenMesajiIsle(String mesaj) {

		try {

			MesajNesnesi mesajNesnesi = gson.fromJson(mesaj, MesajNesnesi.class);

			if (uuid.equals(mesajNesnesi.gonderenUuid))
				return;

			switch (mesajNesnesi.tip) {

			case "BCON":

				dinleyiciyeBeaconAlindi(mesajNesnesi.mesaj);

				break;

			case "UUID_KOPTU":

				dinleyiciyeKullaniciKoptu(mesajNesnesi.mesaj);

				break;

			default:

			}

		} catch (JsonSyntaxException e) {

		}

	}

	private void dinleyiciyeBeaconAlindi(final String mesaj) {

		out.execute(() -> {

			dinleyici.beaconAlindi(mesaj);

		});

	}

	private void dinleyiciyeKullaniciKoptu(final String uuid) {

		out.execute(() -> {

			dinleyici.kullaniciKoptu(uuid);

		});

	}

	private void dinleyiciyeSunucuBaglantiDurumuGuncellendi(final boolean baglantiDurumu) {

		out.execute(() -> {

			dinleyici.sunucuBaglantiDurumuGuncellendi(baglantiDurumu);

		});

	}

}

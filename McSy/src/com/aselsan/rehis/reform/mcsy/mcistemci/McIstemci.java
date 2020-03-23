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
import com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari.MesajTipi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class McIstemci {

	private final String uuid;

	private final ZContext context = new ZContext();

	private final String serverIp;
	private final int dealerPort;

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

		this.dinleyici = dinleyici;

		start();

	}

	private void start() {

		Thread dealerThread = new Thread(this::dealer);
		dealerThread.setDaemon(true);
		dealerThread.start();

		Thread subThread = new Thread(this::inproc);
		subThread.setDaemon(true);
		subThread.start();

	}

	public void beaconGonder(String mesaj) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, MesajTipi.BCON)));

	}

	public void tumBeaconlariIste() {

		dealerQueue.offer(gson.toJson(new MesajNesnesi("", uuid, MesajTipi.REQ_BCON)));

	}

	public void mesajGonder(String mesaj, String aliciUuid) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, aliciUuid, MesajTipi.MESAJ)));

	}

	private void dealer() {

		try (ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER);
				ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR);
				ZMQ.Socket monitorSocket = context.createSocket(SocketType.PAIR)) {

			inprocSocket.bind("inproc://dealer");

			dealerSocket.monitor("inproc://monitor", ZMQ.EVENT_CONNECTED | ZMQ.EVENT_DISCONNECTED);

			dealerSocket.setIdentity(uuid.getBytes(ZMQ.CHARSET));
			dealerSocket.setImmediate(false);
			dealerSocket.connect("tcp://" + serverIp + ":" + dealerPort);
			monitorSocket.connect("inproc://monitor");

			ZMQ.Poller poller = context.createPoller(3);
			int pollDealer = poller.register(dealerSocket, ZMQ.Poller.POLLIN);
			int pollInproc = poller.register(inprocSocket, ZMQ.Poller.POLLIN);
			int pollMonitor = poller.register(monitorSocket, ZMQ.Poller.POLLIN);

			while (!Thread.currentThread().isInterrupted()) {

				poller.poll();

				if (poller.pollin(pollDealer)) {

					gelenMesajiIsle(dealerSocket.recvStr(ZMQ.DONTWAIT));

				} else if (poller.pollin(pollInproc)) {

					dealerSocket.send(inprocSocket.recvStr(ZMQ.DONTWAIT), ZMQ.DONTWAIT);

				} else if (poller.pollin(pollMonitor)) {

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

	}

	private void inproc() {

		try (ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			inprocSocket.connect("inproc://dealer");

			while (!Thread.currentThread().isInterrupted()) {

				try {

					inprocSocket.send(dealerQueue.take());

				} catch (InterruptedException e) {

					e.printStackTrace();

				}

			}

		}

	}

	private void gelenMesajiIsle(String mesaj) {

		if (mesaj.isEmpty())
			return;

		try {

			MesajNesnesi mesajNesnesi = gson.fromJson(mesaj, MesajNesnesi.class);

			if (uuid.equals(mesajNesnesi.gonderenUuid))
				return;

			switch (mesajNesnesi.mesajTipi) {

			case BCON:

				dinleyiciyeBeaconAlindi(mesajNesnesi.mesaj);

				break;

			case MESAJ:

				dinleyiciyeMesajAlindi(mesajNesnesi.mesaj);

				break;

			case UUID_KOPTU:

				dinleyiciyeKullaniciKoptu(mesajNesnesi.mesaj);

				break;

			default:

			}

		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}

	}

	private void dinleyiciyeBeaconAlindi(final String mesaj) {

		out.execute(() -> {

			dinleyici.beaconAlindi(mesaj);

		});

	}

	private void dinleyiciyeMesajAlindi(final String mesaj) {

		out.execute(() -> {

			dinleyici.mesajAlindi(mesaj);

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

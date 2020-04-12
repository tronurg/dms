package com.aselsan.rehis.reform.mcsy.mcistemci;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.aselsan.rehis.reform.mcsy.mcistemci.intf.McIstemciDinleyici;
import com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari.IcerikTipi;
import com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari.MesajNesnesi;
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

		Thread inprocThread = new Thread(this::inproc);
		inprocThread.setDaemon(true);
		inprocThread.start();

		Thread monitorThread = new Thread(this::monitor);
		monitorThread.setDaemon(true);
		monitorThread.start();

	}

	public void beaconGonder(String mesaj) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, IcerikTipi.BCON)));

	}

	public void tumBeaconlariIste() {

		dealerQueue.offer(gson.toJson(new MesajNesnesi("", uuid, IcerikTipi.REQ_BCON)));

	}

	public void mesajGonder(String mesaj, String aliciUuid) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, aliciUuid, IcerikTipi.MESAJ)));

	}

	public void mesajDurumuIste(String mesaj, String aliciUuid) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, aliciUuid, IcerikTipi.MESAJ_DURUMU_VER)));

	}

	public void alinmadiGonder(String mesaj, String aliciUuid) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, aliciUuid, IcerikTipi.ALINMADI)));

	}

	public void alindiGonder(String mesaj, String aliciUuid) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, aliciUuid, IcerikTipi.ALINDI)));

	}

	public void okunduGonder(String mesaj, String aliciUuid) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, uuid, aliciUuid, IcerikTipi.OKUNDU)));

	}

	private void dealer() {

		try (ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER);
				ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			inprocSocket.bind("inproc://dealer");

			dealerSocket.monitor("inproc://monitor", ZMQ.EVENT_HANDSHAKE_PROTOCOL | ZMQ.EVENT_DISCONNECTED);

			dealerSocket.setIdentity(uuid.getBytes(ZMQ.CHARSET));
			dealerSocket.setImmediate(false);
			dealerSocket.connect("tcp://" + serverIp + ":" + dealerPort);

			ZMQ.Poller poller = context.createPoller(2);
			int pollDealer = poller.register(dealerSocket, ZMQ.Poller.POLLIN);
			int pollInproc = poller.register(inprocSocket, ZMQ.Poller.POLLIN);

			while (!Thread.currentThread().isInterrupted()) {

				poller.poll();

				if (poller.pollin(pollDealer)) {

					String gelenMesaj = dealerSocket.recvStr(ZMQ.DONTWAIT);
					gelenMesajiIsle(gelenMesaj);

				} else if (poller.pollin(pollInproc)) {

					String gidenMesaj = inprocSocket.recvStr(ZMQ.DONTWAIT);
					dealerSocket.send(gidenMesaj, ZMQ.DONTWAIT);

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

	private void monitor() {

		try (ZMQ.Socket monitorSocket = context.createSocket(SocketType.PAIR)) {

			monitorSocket.connect("inproc://monitor");

			while (!Thread.currentThread().isInterrupted()) {

				ZMQ.Event event = ZMQ.Event.recv(monitorSocket);

				switch (event.getEvent()) {

				case ZMQ.EVENT_HANDSHAKE_PROTOCOL:

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

		if (mesaj.isEmpty())
			return;

		try {

			MesajNesnesi mesajNesnesi = gson.fromJson(mesaj, MesajNesnesi.class);

			if (uuid.equals(mesajNesnesi.gonderenUuid))
				return;

			switch (mesajNesnesi.icerikTipi) {

			case BCON:

				dinleyiciyeBeaconAlindi(mesajNesnesi.mesaj);

				break;

			case MESAJ:

				dinleyiciyeMesajAlindi(mesajNesnesi.mesaj);

				break;

			case UUID_KOPTU:

				dinleyiciyeKullaniciKoptu(mesajNesnesi.mesaj);

				break;

			case MESAJ_DURUMU_VER:

				dinleyiciyeMesajDurumuIstendi(mesajNesnesi.mesaj, mesajNesnesi.gonderenUuid);

				break;

			case ALINMADI:

				dinleyiciyeKarsiTarafMesajiAlmadi(mesajNesnesi.mesaj, mesajNesnesi.gonderenUuid);

				break;

			case ALINDI:

				dinleyiciyeKarsiTarafMesajiAldi(mesajNesnesi.mesaj, mesajNesnesi.gonderenUuid);

				break;

			case OKUNDU:

				dinleyiciyeKarsiTarafMesajiOkudu(mesajNesnesi.mesaj, mesajNesnesi.gonderenUuid);

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

	private void dinleyiciyeMesajDurumuIstendi(final String mesaj, final String karsiTarafUuid) {

		out.execute(() -> {

			dinleyici.mesajDurumuIstendi(mesaj, karsiTarafUuid);

		});

	}

	private void dinleyiciyeKarsiTarafMesajiAlmadi(final String mesaj, final String karsiTarafUuid) {

		out.execute(() -> {

			dinleyici.karsiTarafMesajiAlmadi(mesaj, karsiTarafUuid);

		});

	}

	private void dinleyiciyeKarsiTarafMesajiAldi(final String mesaj, final String karsiTarafUuid) {

		out.execute(() -> {

			dinleyici.karsiTarafMesajiAldi(mesaj, karsiTarafUuid);

		});

	}

	private void dinleyiciyeKarsiTarafMesajiOkudu(final String mesaj, final String karsiTarafUuid) {

		out.execute(() -> {

			dinleyici.karsiTarafMesajiOkudu(mesaj, karsiTarafUuid);

		});

	}

}

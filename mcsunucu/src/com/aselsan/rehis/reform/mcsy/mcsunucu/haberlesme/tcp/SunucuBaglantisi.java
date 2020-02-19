package com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.tcp;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.onurg.haberlesme.tcp.TcpIstemci;
import com.onurg.haberlesme.tcp.TcpIstemciDinleyici;

class SunucuBaglantisi {

	private final Set<String> uuidler = Collections.synchronizedSet(new TreeSet<String>());
	private final Map<InetAddress, TcpIstemci> adresIstemci = Collections
			.synchronizedMap(new HashMap<InetAddress, TcpIstemci>());

	private final List<SunucuBaglantisiDinleyici> dinleyiciler = Collections
			.synchronizedList(new ArrayList<SunucuBaglantisiDinleyici>());

	private final ExecutorService es = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	SunucuBaglantisi() {

		System.out.println("SunucuBaglantisi.SunucuBaglantisi()");

	}

	void dinleyiciEkle(SunucuBaglantisiDinleyici dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	void uuidEkle(String uuid) {

		System.out.println("SunucuBaglantisi.uuidEkle()");

		uuidler.add(uuid);

	}

	void uuidCikar(String uuid) {

		System.out.println("SunucuBaglantisi.uuidCikar()");

		uuidler.remove(uuid);

	}

	boolean tcpBaglantisiVarMi(InetAddress adres) {

		return adresIstemci.containsKey(adres);

	}

	void tcpBaglantisiEkle(final InetAddress adres, int port, InetAddress yerelAdres, int yerelPort) {

		System.out.println("SunucuBaglantisi.tcpBaglantisiEkle()");

		if (adresIstemci.containsKey(adres))
			return;

		TcpIstemci istemci = new TcpIstemci(adres, port, yerelAdres, yerelPort);

		istemci.setBlocking(true);

		adresIstemci.put(adres, istemci);

		istemci.dinleyiciEkle(new TcpIstemciDinleyici() {

			@Override
			public void yeniMesajAlindi(final String mesaj) {

				dinleyiciler.forEach(e -> e.mesajAlindi(mesaj));

			}

			@Override
			public void baglantiKuruldu() {

			}

			@Override
			public void baglantiKurulamadi() {

				istemciyiKaldir();

			}

			@Override
			public void baglantiKoptu() {

				istemciyiKaldir();

			}

			private void istemciyiKaldir() {

				adresIstemci.remove(adres);

				if (adresIstemci.size() == 0) {

					dinleyiciler.forEach(e -> e.sunucuBaglantisiKoptu());

				}

			}

		});

		istemci.baglan();

	}

	Set<String> getUuidler() {

		return uuidler;

	}

	void mesajGonder(final String mesaj) {

		es.execute(() -> {

			synchronized (adresIstemci) {

				for (TcpIstemci istemci : adresIstemci.values()) {

					boolean gonderildi = istemci.mesajGonder(mesaj);

					if (gonderildi)
						break;

				}

			}

		});

	}

}

interface SunucuBaglantisiDinleyici {

	void mesajAlindi(String mesaj);

	void sunucuBaglantisiKoptu();

}

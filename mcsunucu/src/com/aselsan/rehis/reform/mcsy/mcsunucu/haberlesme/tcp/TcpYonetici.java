package com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.intf.TcpYoneticiDinleyici;
import com.onurg.haberlesme.tcp.TcpIstemci;
import com.onurg.haberlesme.tcp.TcpIstemciDinleyici;
import com.onurg.haberlesme.tcp.TcpSunucu;
import com.onurg.haberlesme.tcp.TcpSunucuDinleyici;

public class TcpYonetici implements TcpSunucuDinleyici {

	private final int sunucuPort;
	private final int istemciPortBasl;
	private final int istemciPortBits;

	private final TcpSunucu tcpSunucu;

	private final Map<InetAddress, TcpIstemci> adresIstemci = Collections
			.synchronizedMap(new HashMap<InetAddress, TcpIstemci>());
	private final Map<String, List<TcpIstemci>> uuidIstemciler = Collections
			.synchronizedMap(new HashMap<String, List<TcpIstemci>>());
	private final Map<InetAddress, Set<String>> adresUuidler = Collections
			.synchronizedMap(new HashMap<InetAddress, Set<String>>());

	private final List<TcpYoneticiDinleyici> dinleyiciler = Collections
			.synchronizedList(new ArrayList<TcpYoneticiDinleyici>());

	public TcpYonetici(int sunucuPort, int istemciPortBasl, int istemciPortBits) throws IOException {

		this.sunucuPort = sunucuPort;
		this.istemciPortBasl = istemciPortBasl;
		this.istemciPortBits = istemciPortBits;

		tcpSunucu = new TcpSunucu(sunucuPort);

		tcpSunucu.dinleyiciEkle(this);

		tcpSunucu.baglantiKabulEt();

	}

	public void dinleyiciEkle(TcpYoneticiDinleyici dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	public void baglantiEkle(final String uuid, InetAddress adres) {

		adresUuidler.putIfAbsent(adres, new HashSet<String>());
		adresUuidler.get(adres).add(uuid);

		if (!adresIstemci.containsKey(adres)) {

			final TcpIstemci istemci = new TcpIstemci(adres, sunucuPort, null, 0); // !!!!!!!! yerel port degisecek !!!

			istemci.dinleyiciEkle(new TcpIstemciDinleyici() {

				@Override
				public void yeniMesajAlindi(String arg0) {

					dinleyiciler.forEach(e -> e.mesajAlindi(arg0));

				}

				@Override
				public void baglantiKuruldu() {

//					adresUuidler.get(adres).for

				}

				@Override
				public void baglantiKurulamadi() {
					// TODO Auto-generated method stub

				}

				@Override
				public void baglantiKoptu() {
					// TODO Auto-generated method stub

				}

			});

		}

//		istemci.baglan();

	}

	public void mesajGonder(String uuid, String mesaj) {

	}

	public void sunucudanMesajGonder(int id, String mesaj) {

		tcpSunucu.mesajGonder(id, mesaj);

	}

	@Override
	public void baglantiKoptu(int arg0) {

	}

	@Override
	public void baglantiKuruldu(final int arg0) {

		dinleyiciler.forEach(e -> e.baglantiKuruldu(arg0));

		tcpSunucu.baglantiKabulEt();

	}

	@Override
	public void yeniMesajAlindi(int arg0, String arg1) {

		dinleyiciler.forEach(e -> e.mesajAlindi(arg1));

	}

}

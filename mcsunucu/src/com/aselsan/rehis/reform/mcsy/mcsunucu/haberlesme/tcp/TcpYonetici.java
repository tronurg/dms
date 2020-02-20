package com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.intf.TcpYoneticiDinleyici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.ortak.Sifreleme;
import com.onurg.haberlesme.tcp.TcpSunucu;
import com.onurg.haberlesme.tcp.TcpSunucuDinleyici;

public class TcpYonetici implements TcpSunucuDinleyici {

	private final int sunucuPort;
	private final int istemciPortBasl;
	private final int istemciPortBits;

	private final TcpSunucu tcpSunucu;

	private final List<Integer> sunucuIdler = Collections.synchronizedList(new ArrayList<Integer>());

	private final Map<String, SunucuBaglantisi> mcUuidBaglanti = Collections
			.synchronizedMap(new HashMap<String, SunucuBaglantisi>());
	private final Map<String, SunucuBaglantisi> uuidBaglanti = Collections
			.synchronizedMap(new HashMap<String, SunucuBaglantisi>());

	private final List<TcpYoneticiDinleyici> dinleyiciler = Collections
			.synchronizedList(new ArrayList<TcpYoneticiDinleyici>());

	private final Charset sifrelemeCharset = Charset.forName("UTF-8");

	private final ExecutorService sunucuIslemKuyrugu = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

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

	public void baglantiEkle(final String mcUuid, final String uuid, final InetAddress adres) {

		sunucuIslemKuyrugu.execute(() -> {

			if (!mcUuidBaglanti.containsKey(mcUuid)) {

				final SunucuBaglantisi baglanti = new SunucuBaglantisi();

				mcUuidBaglanti.put(mcUuid, baglanti);

				baglanti.dinleyiciEkle(new SunucuBaglantisiDinleyici() {

					@Override
					public void mesajAlindi(String mesaj) {

						dinleyicilereMesajAlindi(mesaj);

					}

					@Override
					public void sunucuBaglantisiKoptu() {

						sunucuIslemKuyrugu.execute(() -> {

							mcUuidBaglanti.remove(mcUuid);

							baglanti.getUuidler().forEach(e -> {

								uuidBaglanti.remove(e);

								dinleyicilereUuidKoptu(e);

							});

						});

					}

				});

			}

			SunucuBaglantisi baglanti = mcUuidBaglanti.get(mcUuid);

			if (!uuidBaglanti.containsKey(uuid)) {

				baglanti.uuidEkle(uuid);

				uuidBaglanti.put(uuid, baglanti);

			} else if (!uuidBaglanti.get(uuid).equals(baglanti)) {

				uuidBaglanti.get(uuid).uuidCikar(uuid);

				baglanti.uuidEkle(uuid);

				uuidBaglanti.put(uuid, baglanti);

			}

			if (!baglanti.tcpBaglantisiVarMi(adres))
				baglanti.tcpBaglantisiEkle(adres, sunucuPort, null, 0); // TODO

		});

	}

	public void mesajGonder(String uuid, String mesaj) {

		SunucuBaglantisi baglanti = uuidBaglanti.get(uuid);

		if (baglanti == null)
			return;

		try {

			String mesajEncrypted = new String(Sifreleme.encrypt(mesaj.getBytes(sifrelemeCharset)), sifrelemeCharset);

			baglanti.mesajGonder(mesajEncrypted);

		} catch (GeneralSecurityException | IOException e1) {

		}

	}

	public void sunucudanMesajGonder(int id, String mesaj) {

		try {

			String mesajEncrypted = new String(Sifreleme.encrypt(mesaj.getBytes(sifrelemeCharset)), sifrelemeCharset);

			tcpSunucu.mesajGonder(id, mesajEncrypted);

		} catch (GeneralSecurityException | IOException e1) {

		}

	}

	public void sunucudanTumBaglantilaraGonder(String mesaj) {

		try {

			String mesajEncrypted = new String(Sifreleme.encrypt(mesaj.getBytes(sifrelemeCharset)), sifrelemeCharset);

			sunucuIdler.forEach(id -> tcpSunucu.mesajGonder(id, mesajEncrypted));

		} catch (GeneralSecurityException | IOException e1) {

		}

	}

	private void dinleyicilereBaglantiKuruldu(final int id) {

		dinleyiciler.forEach(e -> e.baglantiKuruldu(id));

	}

	private void dinleyicilereUuidKoptu(String uuid) {

		dinleyiciler.forEach(e -> e.uzakUuidKoptu(uuid));

	}

	private void dinleyicilereMesajAlindi(String mesaj) {

		try {

			String mesajDecrypted = new String(Sifreleme.decrypt(mesaj.getBytes(sifrelemeCharset)), sifrelemeCharset);

			dinleyiciler.forEach(e -> e.mesajAlindi(mesajDecrypted));

		} catch (GeneralSecurityException | IOException e1) {

		}

	}

	@Override
	public void baglantiKoptu(int id) {

		sunucuIdler.remove(Integer.valueOf(id));

	}

	@Override
	public void baglantiKuruldu(final int id) {

		sunucuIdler.add(id);

		dinleyicilereBaglantiKuruldu(id);

		tcpSunucu.baglantiKabulEt();

	}

	@Override
	public void yeniMesajAlindi(int arg0, String arg1) {

		dinleyicilereMesajAlindi(arg1);

	}

}

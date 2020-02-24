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
import java.util.concurrent.atomic.AtomicBoolean;

import com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.intf.TcpYoneticiDinleyici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.ortak.Sifreleme;
import com.onurg.haberlesme.tcp.TcpIstemci;
import com.onurg.haberlesme.tcp.TcpIstemciDinleyici;
import com.onurg.haberlesme.tcp.TcpSunucu;
import com.onurg.haberlesme.tcp.TcpSunucuDinleyici;

public class TcpYonetici implements TcpSunucuDinleyici {

	private final int sunucuPort;
	private final int istemciPortBasl;
	private final int istemciPortBits;

	private final TcpSunucu tcpSunucu;

	//

	private final Map<Integer, InetAddress> sunucuIdAdres = Collections
			.synchronizedMap(new HashMap<Integer, InetAddress>());
	private final Map<String, Kullanici> kullanicilar = Collections.synchronizedMap(new HashMap<String, Kullanici>());
	private final Map<String, McSunucu> mcSunucular = Collections.synchronizedMap(new HashMap<String, McSunucu>());
	private final Map<InetAddress, Baglanti> baglantilar = Collections
			.synchronizedMap(new HashMap<InetAddress, Baglanti>());

	//

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

		kullanicilar.putIfAbsent(uuid, new Kullanici());
		mcSunucular.putIfAbsent(mcUuid, new McSunucu());
		baglantilar.putIfAbsent(adres, new Baglanti(adres));

		final Kullanici kullanici = kullanicilar.get(uuid);
		final McSunucu mcSunucu = mcSunucular.get(mcUuid);
		final Baglanti baglanti = baglantilar.get(adres);

		if (kullanici.mcSunucu == null) {
			// Kullanici yeni katildi
			// Iliskiler guncellenecek

			kullanici.mcSunucu = mcSunucu;
			mcSunucu.kullanicilar.add(kullanici);

		} else if (kullanici.mcSunucu != mcSunucu) {
			// Kullanicinin bagli oldugu sunucu degisti
			// Iliskiler guncellenecek

			kullanici.mcSunucu.kullanicilar.remove(kullanici);
			kullanici.mcSunucu = mcSunucu;
			mcSunucu.kullanicilar.add(kullanici);

		}

		if (baglanti.mcSunucu == null) {
			// Baglanti yeni olustu
			// Iliskiler guncellenecek

			baglanti.mcSunucu = mcSunucu;

		} else if (baglanti.mcSunucu != mcSunucu) {
			// Baglantinin arkasindaki sunucu degisti
			// Iliskiler guncellenecek

			if (baglanti.mcSunucu.baglantilar.contains(baglanti)) {
				// eski sunucuya bu baglanti eklenmisse kaldir ve kontrol et

				baglanti.mcSunucu.baglantilar.remove(baglanti);

				sunucuyuKontrolEt(baglanti.mcSunucu);

			}

			baglanti.mcSunucu = mcSunucu;

		}

		if (baglanti.tcpIstemci == null) {

			baglanti.tcpIstemci = new TcpIstemci(adres, sunucuPort, null, 0); // TODO

			baglanti.tcpIstemci.setBlocking(true);

			baglanti.tcpIstemci.dinleyiciEkle(new TcpIstemciDinleyici() {

				@Override
				public void yeniMesajAlindi(String arg0) {

					dinleyicilereMesajAlindi(arg0);

				}

				@Override
				public void baglantiKuruldu() {

					baglantiyiKontrolEt(baglanti);

				}

				@Override
				public void baglantiKurulamadi() {

					baglanti.tcpIstemci = null;

					baglantiyiKontrolEt(baglanti);

				}

				@Override
				public void baglantiKoptu() {

					baglanti.tcpIstemci = null;

					baglantiyiKontrolEt(baglanti);

				}

			});

			baglanti.tcpIstemci.baglan();

		}

	}

	public void mesajGonder(String uuid, String mesaj) {

		Kullanici kullanici = kullanicilar.get(uuid);

		if (kullanici == null)
			return;

		McSunucu mcSunucu = kullanici.mcSunucu;

		if (mcSunucu == null)
			return;

		try {

			String mesajEncrypted = new String(Sifreleme.encrypt(mesaj.getBytes(sifrelemeCharset)), sifrelemeCharset);

			mcSunucu.islemKuyrugu.execute(() -> {

				synchronized (mcSunucu.baglantilar) {

					for (Baglanti baglanti : mcSunucu.baglantilar) {

						TcpIstemci tcpIstemci = baglanti.tcpIstemci;

						if (tcpIstemci == null)
							continue;

						boolean gonderildi = tcpIstemci.mesajGonder(mesajEncrypted);

						if (gonderildi)
							break;

					}

				}

			});

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

	public void sunucudanTumSunucularaGonder(String mesaj) {

		try {

			String mesajEncrypted = new String(Sifreleme.encrypt(mesaj.getBytes(sifrelemeCharset)), sifrelemeCharset);

			for (McSunucu mcSunucu : mcSunucular.values()) {

				mcSunucu.islemKuyrugu.execute(() -> {

					synchronized (mcSunucu.baglantilar) {

						for (Baglanti baglanti : mcSunucu.baglantilar) {

							Integer tcpSunucuId = baglanti.tcpSunucuId;

							if (tcpSunucuId == null)
								continue;

							boolean gonderildi = tcpSunucu.mesajGonder(tcpSunucuId, mesajEncrypted);

							if (gonderildi)
								break;

						}

					}

				});

			}

		} catch (GeneralSecurityException | IOException e1) {

		}

	}

	private void baglantiyiKontrolEt(Baglanti baglanti) {

		McSunucu mcSunucu = baglanti.mcSunucu;

		if (!(baglanti.tcpSunucuId == null || baglanti.tcpIstemci == null) && baglanti.tcpIstemci.bagliMi()) {
			// cift tarafli baglanti kuruldu

			if (!(mcSunucu == null || mcSunucu.baglantilar.contains(baglanti))) {
				// mcSunucu varsa ve baglantiyi icermiyorsa baglantiyi ekle

				mcSunucu.baglantilar.add(baglanti);

				sunucuyuKontrolEt(mcSunucu);

			}

		} else {
			// cift tarafli baglanti bozuldu

			if (mcSunucu != null && mcSunucu.baglantilar.contains(baglanti)) {
				// mcSunucu varsa ve baglantiyi iceriyorsa baglantiyi cikar

				mcSunucu.baglantilar.remove(baglanti);

				sunucuyuKontrolEt(mcSunucu);

			}

		}

		if (baglanti.tcpSunucuId == null && baglanti.tcpIstemci == null) {
			// baglanti tamamen koptu

			baglantilar.remove(baglanti.adres);

			if (mcSunucu != null && mcSunucu.baglantilar.contains(baglanti)) {
				// mcSunucu varsa ve baglantiyi iceriyorsa baglantiyi cikar

				mcSunucu.baglantilar.remove(baglanti);

				sunucuyuKontrolEt(mcSunucu);

			}
		}

	}

	private void sunucuyuKontrolEt(McSunucu sunucu) {

		// TODO
		// sunucu bagli degilken baglantilarinin sifirdan buyukse durumu bagliya cekilir
		// ve dinleyicilere haber verilir
		// sunucu bagliyken baglantilarinin sayisi sifira dusmusse bagli degile cekilir
		// ve dinleyicilere haber verilir

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

		InetAddress adres = sunucuIdAdres.remove(id);

		if (adres == null)
			return;

		Baglanti baglanti = baglantilar.get(adres);

		if (baglanti == null)
			return;

		baglanti.tcpSunucuId = null;

		baglantiyiKontrolEt(baglanti);

	}

	@Override
	public void baglantiKuruldu(final int id) {

		InetAddress adres = tcpSunucu.getUzakAdres(id);

		sunucuIdAdres.put(id, adres);

		baglantilar.putIfAbsent(adres, new Baglanti(adres));

		Baglanti baglanti = baglantilar.get(adres);

		baglanti.tcpSunucuId = id;

		baglantiyiKontrolEt(baglanti);

		tcpSunucu.baglantiKabulEt();

	}

	@Override
	public void yeniMesajAlindi(int arg0, String arg1) {

		dinleyicilereMesajAlindi(arg1);

	}

	private class Baglanti {

		private final InetAddress adres;

		Integer tcpSunucuId;
		TcpIstemci tcpIstemci;
		McSunucu mcSunucu;

		Baglanti(InetAddress adres) {

			this.adres = adres;

		}

	}

	private class McSunucu {

		final List<Kullanici> kullanicilar = Collections.synchronizedList(new ArrayList<Kullanici>());
		final List<Baglanti> baglantilar = Collections.synchronizedList(new ArrayList<Baglanti>());
		// sadece cift tarafli kurulmus baglantilari icerir

		private final AtomicBoolean bagliMi = new AtomicBoolean(false);

		private final ExecutorService islemKuyrugu = Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable arg0) {

				Thread thread = new Thread(arg0);

				thread.setDaemon(true);

				return thread;

			}

		});

	}

	private class Kullanici {

		McSunucu mcSunucu;

	}

}

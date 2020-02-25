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
import java.util.function.Function;

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

	private final ExecutorService islemKuyrugu = Executors.newSingleThreadExecutor(new ThreadFactory() {

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

	public void baglantiEkle(final String mcUuid, final String uuid, final InetAddress adres,
			TcpBaglantiTipi baglantiTipi) {

		islemKuyrugu.execute(() -> {

			kullanicilar.putIfAbsent(uuid, new Kullanici(uuid));
			mcSunucular.putIfAbsent(mcUuid, new McSunucu(mcUuid));

			final Kullanici kullanici = kullanicilar.get(uuid);
			final McSunucu mcSunucu = mcSunucular.get(mcUuid);

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

			if (!baglantilar.containsKey(adres)) {

				Baglanti baglanti = new Baglanti();
				baglanti.mcSunucu = mcSunucu;

				baglantilar.put(adres, baglanti);

				if (baglantiTipi.equals(TcpBaglantiTipi.ISTEMCI)) {

					TcpIstemci tcpIstemci = new TcpIstemci(adres, sunucuPort, null, bosPortIste());

					tcpIstemci.setBlocking(true);

					tcpIstemci.dinleyiciEkle(new TcpIstemciDinleyici() {

						@Override
						public void yeniMesajAlindi(String arg0) {

							islemKuyrugu.execute(() -> {

								dinleyicilereMesajAlindi(arg0);

							});

						}

						@Override
						public void baglantiKuruldu() {

							islemKuyrugu.execute(() -> {

								baglanti.gondermeMetodu = tcpIstemci::mesajGonder;
								mcSunucu.baglantilar.add(baglanti);

								sunucuyuKontrolEt(mcSunucu);

							});

						}

						@Override
						public void baglantiKurulamadi() {

							islemKuyrugu.execute(() -> {

								baglantilar.remove(adres);

							});

						}

						@Override
						public void baglantiKoptu() {

							islemKuyrugu.execute(() -> {

								baglantilar.remove(adres);
								mcSunucu.baglantilar.remove(baglanti);

								sunucuyuKontrolEt(mcSunucu);

							});

						}

					});

					tcpIstemci.baglan();

				}

			}

			final Baglanti baglanti = baglantilar.get(adres);

			if (baglanti.mcSunucu == null) {
				// Baglanti sunucu tarafindan olusturulmus
				// Iliskiler guncellenecek

				baglanti.mcSunucu = mcSunucu;
				mcSunucu.baglantilar.add(baglanti);

				sunucuyuKontrolEt(mcSunucu);

			}

		});

	}

	public void kullaniciyaMesajGonder(String uuid, String mesaj) {

		islemKuyrugu.execute(() -> {

			Kullanici kullanici = kullanicilar.get(uuid);

			if (kullanici == null)
				return;

			McSunucu mcSunucu = kullanici.mcSunucu;

			if (mcSunucu == null)
				return;

			sunucuyaMesajGonder(mcSunucu, mesaj);

		});

	}

	public void sunucuyaMesajGonder(String mcUuid, String mesaj) {

		islemKuyrugu.execute(() -> {

			McSunucu mcSunucu = mcSunucular.get(mcUuid);

			if (mcSunucu == null)
				return;

			sunucuyaMesajGonder(mcSunucu, mesaj);

		});

	}

	public void tumSunucularaMesajGonder(final String mesaj) {

		islemKuyrugu.execute(() -> {

			mcSunucular.forEach((mcUuid, mcSunucu) -> sunucuyaMesajGonder(mcSunucu, mesaj));

		});

	}

	private int bosPortIste() {

		// TODO

		return 0;

	}

	private void sunucuyaMesajGonder(McSunucu mcSunucu, String mesaj) {

		try {

			String mesajEncrypted = new String(Sifreleme.encrypt(mesaj.getBytes(sifrelemeCharset)), sifrelemeCharset);

			mcSunucu.islemKuyrugu.execute(() -> {

				synchronized (mcSunucu.baglantilar) {

					for (Baglanti baglanti : mcSunucu.baglantilar) {

						if (baglanti.gondermeMetodu == null)
							continue;

						boolean gonderildi = baglanti.gondermeMetodu.apply(mesajEncrypted);

						if (gonderildi)
							break;

					}

				}

			});

		} catch (GeneralSecurityException | IOException e1) {

		}

	}

	private void sunucuyuKontrolEt(McSunucu mcSunucu) {

		// sunucu bagli degilken baglanti eklenirse durumu bagliya cekilir
		// ve dinleyicilere haber verilir
		// sunucu bagliyken baglantilarinin sayisi sifira duserse bagli degile cekilir
		// ve dinleyicilere haber verilir

		if (!mcSunucu.bagliMi.get() && mcSunucu.baglantilar.size() > 0) {

			mcSunucu.bagliMi.set(true);

			dinleyicilereUzakSunucuyaBaglanildi(mcSunucu.mcUuid);

		} else if (mcSunucu.bagliMi.get() && mcSunucu.baglantilar.size() == 0) {
			// sunucu koptu
			// sunucuyu listelerden cikar
			// tum uuid'lerin koptugunu bildir

			mcSunucu.bagliMi.set(false);

			mcSunucu.kullanicilar.forEach(kullanici -> {

				dinleyicilereUzakKullaniciKoptu(kullanici.uuid);

				kullanicilar.remove(kullanici.uuid);

			});

			mcSunucular.remove(mcSunucu.mcUuid);

		}

	}

	private void dinleyicilereUzakKullaniciKoptu(final String uuid) {

		dinleyiciler.forEach(e -> e.uzakKullaniciKoptu(uuid));

	}

	private void dinleyicilereUzakSunucuyaBaglanildi(final String mcUuid) {

		dinleyiciler.forEach(e -> e.uzakSunucuyaBaglanildi(mcUuid));

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

		islemKuyrugu.execute(() -> {

			InetAddress adres = sunucuIdAdres.remove(id);

			if (adres == null)
				return;

			Baglanti baglanti = baglantilar.get(adres);

			if (baglanti == null)
				return;

			McSunucu mcSunucu = baglanti.mcSunucu;

			if (mcSunucu == null)
				return;

			if (mcSunucu.baglantilar.contains(baglanti)) {

				mcSunucu.baglantilar.remove(baglanti);

				sunucuyuKontrolEt(mcSunucu);

			}

		});

	}

	@Override
	public void baglantiKuruldu(final int id) {

		islemKuyrugu.execute(() -> {

			InetAddress adres = tcpSunucu.getUzakAdres(id);

			sunucuIdAdres.put(id, adres);

			baglantilar.putIfAbsent(adres, new Baglanti());

			Baglanti baglanti = baglantilar.get(adres);

			baglanti.gondermeMetodu = mesaj -> tcpSunucu.mesajGonder(id, mesaj);

			McSunucu mcSunucu = baglanti.mcSunucu;

			if (mcSunucu != null) {
				// baglanti alinan bir uuid ile olusturulmus
				// baglantilari guncelleyip kontrol et

				mcSunucu.baglantilar.add(baglanti);

				sunucuyuKontrolEt(mcSunucu);

			}

		});

		tcpSunucu.baglantiKabulEt();

	}

	@Override
	public void yeniMesajAlindi(int arg0, String arg1) {

		islemKuyrugu.execute(() -> {

			dinleyicilereMesajAlindi(arg1);

		});

	}

	private class Baglanti {

		McSunucu mcSunucu;

		Function<String, Boolean> gondermeMetodu;

	}

	private class McSunucu {

		final String mcUuid;

		final List<Kullanici> kullanicilar = Collections.synchronizedList(new ArrayList<Kullanici>());
		final List<Baglanti> baglantilar = Collections.synchronizedList(new ArrayList<Baglanti>());

		final AtomicBoolean bagliMi = new AtomicBoolean(false);

		protected final ExecutorService islemKuyrugu = Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable arg0) {

				Thread thread = new Thread(arg0);

				thread.setDaemon(true);

				return thread;

			}

		});

		McSunucu(String mcUuid) {
			this.mcUuid = mcUuid;
		}

	}

	private class Kullanici {

		final String uuid;

		McSunucu mcSunucu;

		Kullanici(String uuid) {
			this.uuid = uuid;
		}

	}

}

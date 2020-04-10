package com.aselsan.rehis.reform.mcsy.kontrol;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JComponent;

import org.hibernate.HibernateException;

import com.aselsan.rehis.reform.mcsy.arayuz.McHandle;
import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;
import com.aselsan.rehis.reform.mcsy.mcistemci.McIstemci;
import com.aselsan.rehis.reform.mcsy.mcistemci.intf.McIstemciDinleyici;
import com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari.MesajTipi;
import com.aselsan.rehis.reform.mcsy.model.Model;
import com.aselsan.rehis.reform.mcsy.ortak.OrtakSabitler;
import com.aselsan.rehis.reform.mcsy.sunum.McPanel;
import com.aselsan.rehis.reform.mcsy.sunum.intf.UygulamaDinleyici;
import com.aselsan.rehis.reform.mcsy.veritabani.VeritabaniYonetici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;
import com.aselsan.rehis.reform.mcsy.veriyapilari.KisiDurumu;
import com.aselsan.rehis.reform.mcsy.veriyapilari.MesajDurumu;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Kontrol implements UygulamaDinleyici, McIstemciDinleyici, McHandle {

	private static final Map<String, Kontrol> INSTANCES = Collections.synchronizedMap(new HashMap<String, Kontrol>());

	private final VeritabaniYonetici veritabaniYonetici;

	private final Model model;

	private final McPanel mcPanel;
	private final JFXPanel mcPanelSwing;

	private final McIstemci mcIstemci;

	private final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return arg0.getName().equals("id");
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).create();

	private final Object beaconSyncObj = new Object();

	private final ExecutorService islemKuyrugu = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {
			Thread thread = new Thread(arg0);
			thread.setDaemon(true);
			return thread;
		}

	});

	private Kontrol(String kullaniciAdi) throws VeritabaniHatasi {

		veritabaniYonetici = new VeritabaniYonetici(kullaniciAdi);

		Kimlik kimlik = veritabaniYonetici.getKimlik();

		model = new Model(kimlik);

		mcPanelSwing = new JFXPanel();
		mcPanel = new McPanel();

		mcPanel.dinleyiciEkle(this);

		initVeritabani();
		initModel();
		initGUI();

		mcIstemci = new McIstemci(kimlik.getUuid(), OrtakSabitler.SUNUCU_IP, OrtakSabitler.SUNUCU_PORT, this);

		ilklendir();

	}

	public static Kontrol getInstance(String kullaniciAdi) throws VeritabaniHatasi {

		INSTANCES.putIfAbsent(kullaniciAdi, new Kontrol(kullaniciAdi));

		return INSTANCES.get(kullaniciAdi);

	}

	private void ilklendir() {

		Thread beaconYayinlaThread = new Thread(this::beaconYayinla);
		beaconYayinlaThread.setDaemon(true);
		beaconYayinlaThread.start();

	}

	private void initVeritabani() {

		veritabaniYonetici.tumKisileriAl().forEach(kisi -> {
			kisi.setDurum(KisiDurumu.CEVRIMDISI);
			veritabaniYonetici.kisiEkleGuncelle(kisi);
		});

	}

	private void initModel() {

		veritabaniYonetici.tumKisileriAl().forEach(kisi -> model.addKisi(kisi));
		veritabaniYonetici.tumGruplariAl().forEach(grup -> model.addGrup(grup));

	}

	private void initGUI() {

		Platform.runLater(() -> {
			mcPanelSwing.setScene(new Scene(mcPanel));
			mcPanel.setKimlik(model.getKimlik());
		});

		model.getKisiler().forEach((uuid, kisi) -> Platform.runLater(() -> mcPanel.kisiGuncelle(kisi)));

		model.getGruplar().forEach((uuid, grup) -> Platform.runLater(() -> mcPanel.grupGuncelle(grup)));

		veritabaniYonetici.tumMesajlariAl().forEach(mesaj -> {

			if (model.getKimlik().getUuid().equals(mesaj.getGonderenUuid())) {

				Platform.runLater(() -> mcPanel.gidenMesajGuncelle(mesaj));

			} else if (model.getKimlik().getUuid().equals(mesaj.getAliciUuid())) {

				Platform.runLater(() -> mcPanel.gelenMesajGuncelle(mesaj));

			}

		});

	}

	private void beaconYayinla() {

		while (true) {

			synchronized (beaconSyncObj) {

				if (model.isSunucuBagli())
					mcIstemci.beaconGonder(gson.toJson(model.getKimlik()));

				try {

					beaconSyncObj.wait(OrtakSabitler.BEACON_ARALIK_MS);

				} catch (InterruptedException e) {

				}

			}

		}

	}

	private void kisiKoptu(Kisi kisi) {

		islemKuyrugu.execute(() -> {

			kisi.setDurum(KisiDurumu.CEVRIMDISI);

			try {

				final Kisi yeniKisi = veritabaniYonetici.kisiEkleGuncelle(kisi);

				model.addKisi(yeniKisi);

				Platform.runLater(() -> mcPanel.kisiGuncelle(yeniKisi));

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	private Mesaj gidenMesajOlustur(String mesaj, String aliciUuid) throws Exception {

		Mesaj gidenMesaj = new Mesaj(model.getKimlik().getUuid(), aliciUuid, MesajTipi.MESAJ, mesaj);

		gidenMesaj.setMesajDurumu(MesajDurumu.OLUSTURULDU);

		Mesaj yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(gidenMesaj);

		return yeniMesaj;

	}

	private Mesaj gelenMesajOlustur(String mesaj) throws Exception {

		Mesaj gelenMesaj = gson.fromJson(mesaj, Mesaj.class);

		if (model.isMesajPaneliAcik(gelenMesaj.getGonderenUuid())) {

			gelenMesaj.setMesajDurumu(MesajDurumu.OKUNDU);

		} else {

			gelenMesaj.setMesajDurumu(MesajDurumu.ULASTI);

		}

		Mesaj yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(gelenMesaj);

		return yeniMesaj;

	}

	private Mesaj mesajGonder(Mesaj mesaj) {

		String aliciUuid = mesaj.getAliciUuid();

		if (!model.isKisiCevrimici(aliciUuid))
			return mesaj;

		mcIstemci.mesajGonder(gson.toJson(mesaj), aliciUuid);

		try {

			mesaj.setMesajDurumu(MesajDurumu.GONDERILDI);

			Mesaj yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(mesaj);

			return yeniMesaj;

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		return mesaj;

	}

	@Override
	public void beaconAlindi(String mesaj) {

		islemKuyrugu.execute(() -> {

			try {

				Kisi gelenKisi = gson.fromJson(mesaj, Kisi.class);

				final String uuid = gelenKisi.getUuid();
				boolean wasCevrimici = model.isKisiCevrimici(uuid);

				final Kisi yeniKisi = veritabaniYonetici.kisiEkleGuncelle(gelenKisi);

				model.addKisi(yeniKisi);

				Platform.runLater(() -> mcPanel.kisiGuncelle(yeniKisi));

				if (!wasCevrimici) {
					// Simdi cevrimici olduysa bekleyen mesajlarini gonder

					islemKuyrugu.execute(() -> {

						try {

							for (final Mesaj bekleyenMesaj : veritabaniYonetici.getKisiyeGidenBekleyenMesajlar(uuid)) {

								switch (bekleyenMesaj.getMesajDurumu()) {

								case OLUSTURULDU:

									final Mesaj yeniMesaj = mesajGonder(bekleyenMesaj);

									if (!yeniMesaj.getMesajDurumu().equals(MesajDurumu.GONDERILDI))
										break;

									Platform.runLater(() -> mcPanel.gidenMesajGuncelle(yeniMesaj));

									break;

								case GONDERILDI:
								case ULASTI:

									mcIstemci.mesajDurumuIste(Long.toString(bekleyenMesaj.getMesajId()),
											bekleyenMesaj.getAliciUuid());

									break;

								default:

									break;

								}

							}

						} catch (HibernateException e) {

							e.printStackTrace();

						}

					});

				}

			} catch (JsonSyntaxException | HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void mesajAlindi(final String mesaj) {

		islemKuyrugu.execute(() -> {

			try {

				final Mesaj yeniMesaj = gelenMesajOlustur(mesaj);

				Platform.runLater(() -> mcPanel.gelenMesajGuncelle(yeniMesaj));

				if (yeniMesaj.getMesajDurumu().equals(MesajDurumu.ULASTI))
					mcIstemci.alindiGonder(Long.toString(yeniMesaj.getMesajId()), yeniMesaj.getGonderenUuid());
				else if (yeniMesaj.getMesajDurumu().equals(MesajDurumu.OKUNDU))
					mcIstemci.okunduGonder(Long.toString(yeniMesaj.getMesajId()), yeniMesaj.getGonderenUuid());

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void kullaniciKoptu(String uuid) {

		Kisi kisi = model.getKisi(uuid);

		if (kisi == null)
			return;

		kisiKoptu(kisi);

	}

	@Override
	public void sunucuBaglantiDurumuGuncellendi(boolean arg0) {

		model.setSunucuBaglantiDurumu(arg0);

		if (arg0) {

			synchronized (beaconSyncObj) {

				beaconSyncObj.notify();

			}

			mcIstemci.tumBeaconlariIste();

		} else {

			model.getKisiler().forEach((uuid, kisi) -> {

				kisiKoptu(kisi);

			});

		}

	}

	@Override
	public void mesajDurumuIstendi(String mesaj, String karsiTarafUuid) {

		final Long mesajId = Long.parseLong(mesaj);

		islemKuyrugu.execute(() -> {

			try {

				Mesaj gelenMesaj = veritabaniYonetici.getMesaj(karsiTarafUuid, mesajId);

				if (gelenMesaj == null) {

					mcIstemci.alinmadiGonder(mesaj, karsiTarafUuid);

				} else if (gelenMesaj.getMesajDurumu().equals(MesajDurumu.ULASTI)) {

					mcIstemci.alindiGonder(mesaj, karsiTarafUuid);

				} else if (gelenMesaj.getMesajDurumu().equals(MesajDurumu.OKUNDU)) {

					mcIstemci.okunduGonder(mesaj, karsiTarafUuid);

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void karsiTarafMesajiAlmadi(String mesaj, String karsiTarafUuid) {

		final Long mesajId = Long.parseLong(mesaj);

		islemKuyrugu.execute(() -> {

			try {

				Mesaj bekleyenMesaj = veritabaniYonetici.mesajDurumGuncelle(model.getKimlik().getUuid(), mesajId,
						MesajDurumu.OLUSTURULDU);

				if (bekleyenMesaj == null)
					return;

				// Mesaji tekrar gonder

				final Mesaj yeniMesaj = mesajGonder(bekleyenMesaj);

				Platform.runLater(() -> mcPanel.gidenMesajGuncelle(yeniMesaj));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void karsiTarafMesajiAldi(String mesaj, String karsiTarafUuid) {

		final Long mesajId = Long.parseLong(mesaj);

		islemKuyrugu.execute(() -> {

			try {

				final Mesaj gidenMesaj = veritabaniYonetici.mesajDurumGuncelle(model.getKimlik().getUuid(), mesajId,
						MesajDurumu.ULASTI);

				if (gidenMesaj == null)
					return;

				Platform.runLater(() -> mcPanel.gidenMesajGuncelle(gidenMesaj));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void karsiTarafMesajiOkudu(String mesaj, String karsiTarafUuid) {

		final Long mesajId = Long.parseLong(mesaj);

		islemKuyrugu.execute(() -> {

			try {

				final Mesaj gidenMesaj = veritabaniYonetici.mesajDurumGuncelle(model.getKimlik().getUuid(), mesajId,
						MesajDurumu.OKUNDU);

				if (gidenMesaj == null)
					return;

				Platform.runLater(() -> mcPanel.gidenMesajGuncelle(gidenMesaj));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public JComponent getMcPanel() {

		return mcPanelSwing;

	}

	@Override
	public void mesajGonderTiklandi(final String mesaj, final String aliciUuid) {

		islemKuyrugu.execute(() -> {

			try {

				final Mesaj yeniMesaj = mesajGonder(gidenMesajOlustur(mesaj, aliciUuid));

				Platform.runLater(() -> mcPanel.gidenMesajGuncelle(yeniMesaj));

			} catch (Exception e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void aciklamaGuncellendi(String aciklama) {

		islemKuyrugu.execute(() -> {

			try {

				Kimlik kimlik = model.getKimlik();

				if (aciklama.equals(kimlik.getAciklama()))
					return;

				kimlik.setAciklama(aciklama);

				Kimlik yeniKimlik = veritabaniYonetici.kimlikGuncelle(kimlik);

				model.aciklamaGuncelle(aciklama);

				Platform.runLater(() -> mcPanel.setKimlik(yeniKimlik));

				synchronized (beaconSyncObj) {

					beaconSyncObj.notify();

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void durumGuncelleTiklandi() {

		islemKuyrugu.execute(() -> {

			try {

				Kimlik kimlik = model.getKimlik();

				if (kimlik.getDurum().equals(KisiDurumu.MUSAIT)) {

					kimlik.setDurum(KisiDurumu.MESGUL);

				} else if (kimlik.getDurum().equals(KisiDurumu.MESGUL)) {

					kimlik.setDurum(KisiDurumu.MUSAIT);

				}

				Kimlik yeniKimlik = veritabaniYonetici.kimlikGuncelle(kimlik);

				model.durumGuncelle(yeniKimlik.getDurum());

				Platform.runLater(() -> mcPanel.setKimlik(yeniKimlik));

				synchronized (beaconSyncObj) {

					beaconSyncObj.notify();

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void kisiMesajPaneliAcildi(final String uuid) {

		islemKuyrugu.execute(() -> {

			model.mesajPaneliAcildi(uuid);

			try {

				for (final Mesaj gelenMesaj : veritabaniYonetici.getKisidenGelenBekleyenMesajlar(uuid)) {

					try {

						gelenMesaj.setMesajDurumu(MesajDurumu.OKUNDU);

						final Mesaj yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(gelenMesaj);

						Platform.runLater(() -> mcPanel.gelenMesajGuncelle(yeniMesaj));

						mcIstemci.okunduGonder(Long.toString(yeniMesaj.getMesajId()), yeniMesaj.getGonderenUuid());

					} catch (JsonSyntaxException | HibernateException e) {

						e.printStackTrace();

					}

				}

			} catch (HibernateException e) {

				e.printStackTrace();

			}

		});

	}

	@Override
	public void kisiMesajPaneliKapandi(final String uuid) {

		islemKuyrugu.execute(() -> {

			model.mesajPaneliKapandi(uuid);

		});

	}

}

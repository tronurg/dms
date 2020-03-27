package com.aselsan.rehis.reform.mcsy.kontrol;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.hibernate.HibernateException;

import com.aselsan.rehis.reform.mcsy.arayuz.McHandle;
import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;
import com.aselsan.rehis.reform.mcsy.mcistemci.McIstemci;
import com.aselsan.rehis.reform.mcsy.mcistemci.intf.McIstemciDinleyici;
import com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari.MesajTipi;
import com.aselsan.rehis.reform.mcsy.model.Model;
import com.aselsan.rehis.reform.mcsy.model.intf.ModelDinleyici;
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

public class Kontrol implements ModelDinleyici, UygulamaDinleyici, McIstemciDinleyici, McHandle {

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

	private Kontrol(String kullaniciAdi) throws VeritabaniHatasi {

		veritabaniYonetici = new VeritabaniYonetici(kullaniciAdi);

		Kimlik kimlik = veritabaniYonetici.getKimlik();

		model = new Model(kimlik);

		model.dinleyiciEkle(this);

		mcPanelSwing = new JFXPanel();
		mcPanel = new McPanel();

		mcPanel.dinleyiciEkle(this);

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

	private void initModel() {

		veritabaniYonetici.tumKisileriAl().forEach(kisi -> {
			kisi.setDurum(KisiDurumu.CEVRIMDISI);
			model.addKisi(kisi);
		});
		veritabaniYonetici.tumGruplariAl().forEach(grup -> model.addGrup(grup));
		veritabaniYonetici.tumMesajlariAl().forEach(mesaj -> model.addMesaj(mesaj));

	}

	private void initGUI() {

		Platform.runLater(() -> {
			mcPanelSwing.setScene(new Scene(mcPanel));
			mcPanel.setKimlik(model.getKimlik());
		});

		model.getKisiler().forEach((uuid, kisi) -> Platform.runLater(() -> mcPanel.kisiGuncelle(kisi)));

		model.getGruplar().forEach((uuid, grup) -> Platform.runLater(() -> mcPanel.grupGuncelle(grup)));

		model.getMesajlar().forEach((uuid, mesajMap) -> mesajMap.forEach((mesajId, mesaj) -> {

			if (model.getKimlik().getUuid().equals(mesaj.getGonderenUuid())) {

				Platform.runLater(() -> mcPanel.gidenMesajGuncelle(mesaj));

			} else if (model.getKimlik().getUuid().equals(mesaj.getAliciUuid())) {

				Platform.runLater(() -> mcPanel.gelenMesajGuncelle(mesaj));

			}

		}));

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

		kisi.setDurum(KisiDurumu.CEVRIMDISI);

		try {

			final Kisi yeniKisi = veritabaniYonetici.kisiEkleGuncelle(kisi);

			model.addKisi(yeniKisi);

			Platform.runLater(() -> mcPanel.kisiGuncelle(yeniKisi));

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

	private Mesaj gidenMesajOlustur(String mesaj, String aliciUuid) throws HibernateException {

		Mesaj gidenMesaj = new Mesaj(model.getKimlik().getUuid(), aliciUuid, MesajTipi.MESAJ, mesaj);

		gidenMesaj.setMesajDurumu(MesajDurumu.OLUSTURULDU);

		final Mesaj yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(gidenMesaj);

		model.addMesaj(yeniMesaj);

		Platform.runLater(() -> mcPanel.gidenMesajGuncelle(yeniMesaj));

		return yeniMesaj;

	}

	private void mesajGonder(Mesaj mesaj) {

		if (model.getKisi(mesaj.getAliciUuid()).getDurum().equals(KisiDurumu.CEVRIMDISI))
			return;

		mcIstemci.mesajGonder(gson.toJson(mesaj), mesaj.getAliciUuid());

		mesaj.setMesajDurumu(MesajDurumu.GONDERILDI);

		final Mesaj yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(mesaj);

		model.addMesaj(yeniMesaj);

		Platform.runLater(() -> mcPanel.gidenMesajGuncelle(yeniMesaj));

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
	public void beaconAlindi(String mesaj) {

		try {

			Kisi gelenKisi = gson.fromJson(mesaj, Kisi.class);
			final Kisi yeniKisi = veritabaniYonetici.kisiEkleGuncelle(gelenKisi);

			model.addKisi(yeniKisi);

			Platform.runLater(() -> mcPanel.kisiGuncelle(yeniKisi));

		} catch (JsonSyntaxException | HibernateException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void mesajAlindi(String mesaj) {

		// TODO

		try {

			Mesaj gelenMesaj = gson.fromJson(mesaj, Mesaj.class);

			gelenMesaj.setMesajDurumu(MesajDurumu.ULASTI);

			final Mesaj yeniMesaj = veritabaniYonetici.mesajEkleGuncelle(gelenMesaj);

			model.addMesaj(yeniMesaj);

			Platform.runLater(() -> mcPanel.gelenMesajGuncelle(yeniMesaj));

		} catch (JsonSyntaxException | HibernateException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void kullaniciKoptu(String uuid) {

		Kisi kisi = model.getKisi(uuid);

		if (kisi == null)
			return;

		kisiKoptu(kisi);

	}

	@Override
	public JComponent getMcPanel() {

		return mcPanelSwing;

	}

	@Override
	public void mesajGonderTiklandi(String mesaj, String aliciUuid) {

		try {

			mesajGonder(gidenMesajOlustur(mesaj, aliciUuid));

		} catch (HibernateException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void aciklamaGuncellendi(String aciklama) {

		try {

			Kimlik kimlik = model.getKimlik();

			if (aciklama.equals(kimlik.getAciklama()))
				return;

			kimlik.setAciklama(aciklama);

			Kimlik yeniKimlik = veritabaniYonetici.kimlikGuncelle(kimlik);

			model.aciklamaGuncelle(aciklama);

			Platform.runLater(() -> mcPanel.setKimlik(yeniKimlik));

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		synchronized (beaconSyncObj) {

			beaconSyncObj.notify();

		}

	}

}

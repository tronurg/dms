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
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Grup;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;
import com.aselsan.rehis.reform.mcsy.veriyapilari.KisiDurumu;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Kontrol implements ModelDinleyici, UygulamaDinleyici, McIstemciDinleyici, McHandle {

	private static final Map<String, Kontrol> INSTANCES = Collections.synchronizedMap(new HashMap<String, Kontrol>());

	private final VeritabaniYonetici veritabaniYonetici;

	private final Model model = new Model();

	private McPanel mcPanel;
	private JFXPanel mcPanelSwing;

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

		model.setKimlik(kimlik);

		model.dinleyiciEkle(this);

		veritabaniYonetici.tumKisileriAl().forEach(kisi -> {
			kisi.setDurum(KisiDurumu.CEVRIMDISI);
			model.kisiEkle(kisi);
		});
		veritabaniYonetici.tumGruplariAl().forEach(grup -> model.grupEkle(grup));
		veritabaniYonetici.tumMesajlariAl().forEach(mesaj -> model.mesajEkle(mesaj));

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

	private void initGUI() {

		mcPanelSwing.setScene(new Scene(mcPanel));

		mcPanel.setKimlikProperty(model.kimlikProperty());

		synchronized (model.getKisiler()) {

			model.getKisiler().forEach((uuid, kisi) -> Platform.runLater(() -> mcPanel.kisiGuncelle(kisi)));

			model.getKisiler().addListener(new MapChangeListener<String, Kisi>() {

				@Override
				public void onChanged(Change<? extends String, ? extends Kisi> arg0) {

					Platform.runLater(() -> mcPanel.kisiGuncelle(arg0.getValueAdded()));

				}

			});

		}

		synchronized (model.getGruplar()) {

			model.getGruplar().forEach((uuid, grup) -> Platform.runLater(() -> mcPanel.grupGuncelle(grup)));

			model.getGruplar().addListener(new MapChangeListener<String, Grup>() {

				@Override
				public void onChanged(Change<? extends String, ? extends Grup> arg0) {

					Platform.runLater(() -> mcPanel.grupGuncelle(arg0.getValueAdded()));

				}

			});

		}

		synchronized (model.getMesajlar()) {

			model.getMesajlar().forEach(mesaj -> Platform.runLater(() -> mcPanel.mesajGuncelle(mesaj)));

			model.getMesajlar().addListener(new ListChangeListener<Mesaj>() {

				@Override
				public void onChanged(Change<? extends Mesaj> arg0) {

					while (arg0.next()) {

						arg0.getAddedSubList().forEach(mesaj -> Platform.runLater(() -> mcPanel.mesajGuncelle(mesaj)));

					}

				}

			});

		}

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

			Kisi yeniKisi = veritabaniYonetici.kisiGuncelle(kisi);

			model.kisiEkle(yeniKisi);

		} catch (HibernateException e) {

			e.printStackTrace();

		}

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

			Kisi yeniKisi = gson.fromJson(mesaj, Kisi.class);
			Kisi eskiKisi = model.getKisi(yeniKisi.getUuid());

			if (eskiKisi == null) {

				veritabaniYonetici.kisiEkle(yeniKisi);

			} else {

				yeniKisi.setId(eskiKisi.getId());
				yeniKisi = veritabaniYonetici.kisiGuncelle(yeniKisi);

			}

			model.kisiEkle(yeniKisi);

		} catch (JsonSyntaxException | HibernateException e) {

			e.printStackTrace();

		}

	}

	@Override
	public void mesajAlindi(String mesaj) {

		// TODO

		try {

			Mesaj alinanMesaj = gson.fromJson(mesaj, Mesaj.class);

			model.mesajEkle(alinanMesaj);

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

		if (mcPanelSwing == null) {

			mcPanelSwing = new JFXPanel();
			mcPanel = new McPanel();

			mcPanel.dinleyiciEkle(this);

			Platform.runLater(() -> initGUI());

		}

		return mcPanelSwing;

	}

	@Override
	public void mesajGonderTiklandi(String mesaj, String aliciUuid) {

		// TODO

		Mesaj gidenMesaj = new Mesaj(model.getKimlik().getUuid(), aliciUuid, MesajTipi.MESAJ, mesaj);

		mcIstemci.mesajGonder(gson.toJson(gidenMesaj), aliciUuid);

	}

	@Override
	public void aciklamaGuncellendi(String aciklama) {

		try {

			Kimlik kimlik = model.getKimlik();

			if (aciklama.equals(kimlik.getAciklama()))
				return;

			kimlik.setAciklama(aciklama);

			Kimlik yeniKimlik = veritabaniYonetici.kimlikGuncelle(kimlik);

			model.setKimlik(yeniKimlik);

		} catch (HibernateException e) {

			e.printStackTrace();

		}

		synchronized (beaconSyncObj) {

			beaconSyncObj.notify();

		}

	}

}

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
import com.aselsan.rehis.reform.mcsy.model.Model;
import com.aselsan.rehis.reform.mcsy.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.ortak.OrtakSabitler;
import com.aselsan.rehis.reform.mcsy.sunum.McPanel;
import com.aselsan.rehis.reform.mcsy.veritabani.VeritabaniYonetici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Kontrol implements ModelDinleyici, McIstemciDinleyici, McHandle {

	private static final Map<String, Kontrol> INSTANCES = Collections.synchronizedMap(new HashMap<String, Kontrol>());

	private final VeritabaniYonetici veritabaniYonetici;

	private final Model model;

	private McPanel mcPanel;
	private JFXPanel mcPanelSwing;

	private final McIstemci mcIstemci;

	private final Gson gson = new Gson();

	private Kontrol(String kullaniciAdi) throws VeritabaniHatasi {

		veritabaniYonetici = new VeritabaniYonetici(kullaniciAdi);

		Kimlik kimlik = veritabaniYonetici.getKimlik();

		model = new Model(kimlik);

		model.dinleyiciEkle(this);

		veritabaniYonetici.tumKisileriAl().forEach(kisi -> model.kisiEkle(kisi));
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

		mcPanel.kimlikGuncelle(model.getKimlik());

		model.getKisiler().forEach((uuid, kisi) -> Platform.runLater(() -> mcPanel.kisiGuncelle(kisi)));

		model.getKisiler().addListener(new MapChangeListener<String, Kisi>() {

			@Override
			public void onChanged(Change<? extends String, ? extends Kisi> arg0) {

				Platform.runLater(() -> mcPanel.kisiGuncelle(arg0.getValueAdded()));
			}

		});

	}

	private void beaconYayinla() {

		while (true) {

			if (model.isSunucuBagli()) {

				Kimlik kimlik = model.getKimlik();
				kimlik.setId(null);

				mcIstemci.beaconGonder(gson.toJson(kimlik));

			}

			try {
				Thread.sleep(OrtakSabitler.BEACON_ARALIK_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public void sunucuBaglantiDurumuGuncellendi(boolean arg0) {

		model.setSunucuBaglantiDurumu(arg0);

		if (arg0)
			mcIstemci.tumBeaconlariIste();

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
				veritabaniYonetici.kisiGuncelle(yeniKisi);

			}

			model.kisiEkle(yeniKisi);

		} catch (JsonSyntaxException | HibernateException | VeritabaniHatasi e) {

			e.printStackTrace();

		}

		// TODO

		System.out.println(model.getKimlik().getIsim() + ": " + mesaj);

	}

	@Override
	public void mesajAlindi(String mesaj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void kullaniciKoptu(String uuid) {

		model.uuidKoptu(uuid);

	}

	@Override
	public JComponent getMcPanel() {

		if (mcPanelSwing == null) {

			mcPanelSwing = new JFXPanel();
			mcPanel = new McPanel();

			Platform.runLater(() -> initGUI());

		}

		return mcPanelSwing;

	}

}

package com.aselsan.rehis.reform.mcsy.kontrol;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

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

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class Kontrol implements ModelDinleyici, McIstemciDinleyici, McHandle {

	private static final Map<String, Kontrol> INSTANCES = Collections.synchronizedMap(new HashMap<String, Kontrol>());

	private final VeritabaniYonetici veritabaniYonetici;

	private final Model model;

	private McPanel mcPanel;
	private JFXPanel mcPanelSwing;

	private final McIstemci mcIstemci;

	private Kontrol(String kullaniciAdi) throws VeritabaniHatasi {

		veritabaniYonetici = new VeritabaniYonetici(kullaniciAdi);

		Kimlik kimlik = veritabaniYonetici.getKimlik();

		model = new Model(kimlik);

		model.dinleyiciEkle(this);

		mcIstemci = new McIstemci(kimlik.getUuid(), OrtakSabitler.SUNUCU_IP, OrtakSabitler.SUNUCU_PORT, this);

		ilklendir();

	}

	public static Kontrol getInstance(String kullaniciAdi) throws VeritabaniHatasi {

		INSTANCES.putIfAbsent(kullaniciAdi, new Kontrol(kullaniciAdi));

		return INSTANCES.get(kullaniciAdi);

	}

	private void ilklendir() {

		new Thread(() -> {

			while (true) {

				if (model.isSunucuBagli())
					mcIstemci.beaconGonder(model.getBeaconMesaji());

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}).start();

	}

	private void initGUI() {

		mcPanelSwing.setScene(new Scene(mcPanel));

		mcPanel.kimlikGuncelle(model.getKimlik());

		// TODO

	}

	@Override
	public void sunucuBaglantiDurumuGuncellendi(boolean arg0) {

		model.setSunucuBaglantiDurumu(arg0);

		if (arg0) {

			System.out.println("Sunucu baglandi.");

			mcIstemci.tumBeaconlariIste();

		} else {

			System.out.println("Sunucu koptu.");

		}

	}

	@Override
	public void beaconAlindi(String mesaj) {

		System.out.println(mesaj);

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

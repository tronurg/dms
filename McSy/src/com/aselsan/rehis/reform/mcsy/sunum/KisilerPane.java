package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakMetotlar;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

import javafx.beans.binding.Bindings;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

class KisilerPane extends TitledPane {

	private final VBox kisiler = new VBox();

	private final Map<String, KisiPane> uuidler = Collections.synchronizedMap(new HashMap<String, KisiPane>());

	private final List<IKisilerPane> dinleyiciler = Collections.synchronizedList(new ArrayList<IKisilerPane>());

	KisilerPane() {

		super();

		init();

	}

	private void init() {

		setText(OrtakMetotlar.cevir("KISILER"));
		setContent(kisiler);

		disableProperty().bind(Bindings.isEmpty(kisiler.getChildren()));

	}

	void dinleyiciEkle(IKisilerPane dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	void kisiGuncelle(Kisi kisi) {

		final String uuid = kisi.getUuid();

		if (!uuidler.containsKey(uuid)) {

			// Kisi karti ilk defa eklenecek
			kisiKartiEkle(uuid);

		}

		// Kisi guncellenecek
		uuidler.get(uuid).kisiGuncelle(kisi);

	}

	void gelenMesajGuncelle(String mesajId, Mesaj gelenMesaj) {

		String uuid = gelenMesaj.getGonderenUuid();

		if (!uuidler.containsKey(uuid)) {

			// Kisi karti ilk defa eklenecek
			kisiKartiEkle(uuid);

		}

		uuidler.get(uuid).gelenMesajGuncelle(mesajId, gelenMesaj);

	}

	void gidenMesajGuncelle(String mesajId, Mesaj gidenMesaj) {

		String uuid = gidenMesaj.getAliciUuid();

		if (!uuidler.containsKey(uuid)) {

			// Kisi karti ilk defa eklenecek
			kisiKartiEkle(uuid);

		}

		uuidler.get(uuid).gidenMesajGuncelle(mesajId, gidenMesaj);

	}

	private void kisiKartiEkle(String uuid) {

		// Kisi karti ilk defa eklenecek

		KisiPane kisiPane = new KisiPane();

		kisiPane.setOnMesajGonderAction(mesaj -> {

			dinleyiciler.forEach(dinleyici -> dinleyici.mesajGonderTiklandi(mesaj, uuid));

		});

		kisiPane.setOnMesajPaneGoster(mesajPane -> {

			dinleyiciler.forEach(dinleyici -> dinleyici.mesajPaneGoster(mesajPane));

		});

		uuidler.put(uuid, kisiPane);

		kisiler.getChildren().add(0, kisiPane);

		setExpanded(true);

	}

}

interface IKisilerPane {

	void mesajPaneGoster(MesajPane mesajPane);

	void mesajGonderTiklandi(String mesaj, String uuid);

}

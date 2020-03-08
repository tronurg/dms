package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakMetotlar;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;

import javafx.beans.binding.Bindings;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

class KisilerPane extends TitledPane {

	private final VBox kisiler = new VBox();

	private final Map<String, KisiPane> uuidler = Collections.synchronizedMap(new HashMap<String, KisiPane>());

	KisilerPane() {

		super();

		init();

	}

	private void init() {

		setText(OrtakMetotlar.cevir("KISILER"));
		setContent(kisiler);

		disableProperty().bind(Bindings.isEmpty(kisiler.getChildren()));

	}

	void kisiGuncelle(Kisi kisi) {

		if (!uuidler.containsKey(kisi.getUuid())) {
			// Kisi karti ilk defa eklenecek

			uuidler.put(kisi.getUuid(), new KisiPane());

			kisiler.getChildren().add(0, uuidler.get(kisi.getUuid()));

		}

		// Kisi guncellenecek
		uuidler.get(kisi.getUuid()).kisiGuncelle(kisi);

	}

}

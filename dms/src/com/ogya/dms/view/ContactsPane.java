package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

class ContactsPane extends TitledPane {

	private final VBox kisiler = new VBox();

	private final Map<String, ContactPane> uuidKisiPane = Collections.synchronizedMap(new HashMap<String, ContactPane>());

	private final List<IKisilerPane> dinleyiciler = Collections.synchronizedList(new ArrayList<IKisilerPane>());

	private final AtomicReference<Date> guncelTarih = new AtomicReference<Date>(new Date(0));

	ContactsPane() {

		super();

		init();

	}

	private void init() {

		setText(CommonMethods.cevir("KISILER"));

		kisiler.setPadding(new Insets(10.0));

		ScrollPane scrollPane = new ScrollPane(kisiler);
		scrollPane.setFitToWidth(true);

		scrollPane.setPadding(Insets.EMPTY);

		setContent(scrollPane);

		disableProperty().bind(Bindings.isEmpty(kisiler.getChildren()));

	}

	void dinleyiciEkle(IKisilerPane dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	void kisiGuncelle(Contact kisi) {

		getKisiPane(kisi.getUuid()).kisiGuncelle(kisi);

	}

	void mesajEkle(Message mesaj, MessageDirection mesajYonu, String uuid) {

		ContactPane kisiPane = getKisiPane(uuid);

		kisiPane.mesajEkle(mesaj, mesajYonu);

		Date mesajTarihi = mesaj.getTarih();

		if (guncelTarih.get().compareTo(mesajTarihi) < 0) {

			guncelTarih.set(mesajTarihi);

			kisiler.getChildren().remove(kisiPane);
			kisiler.getChildren().add(0, kisiPane);

		}

	}

	void mesajGuncelle(Message mesaj, String uuid) {

		ContactPane kisiPane = getKisiPane(uuid);

		kisiPane.mesajGuncelle(mesaj);

	}

	void ekraniMesajaKaydir(String uuid, Long mesajId) {

		ContactPane kisiPane = getKisiPane(uuid);

		kisiPane.ekraniMesajaKaydir(mesajId);

	}

	void konumuKaydet(String uuid, Long mesajId) {

		ContactPane kisiPane = getKisiPane(uuid);

		kisiPane.konumuKaydet(mesajId);

	}

	void kaydedilenKonumaGit(String uuid) {

		ContactPane kisiPane = getKisiPane(uuid);

		kisiPane.kaydedilenKonumaGit();

	}

	private ContactPane getKisiPane(final String uuid) {

		if (!uuidKisiPane.containsKey(uuid)) {

			ContactPane kisiPane = new ContactPane();

			kisiPane.setOnMesajPaneGoster(mesajPane -> {

				dinleyiciler.forEach(dinleyici -> dinleyici.mesajPaneGoster(mesajPane, uuid));

			});

			kisiPane.setOnMesajPaneGizle(mesajPane -> {

				dinleyiciler.forEach(dinleyici -> dinleyici.mesajPaneGizle(mesajPane, uuid));

			});

			kisiPane.setOnMesajGonderAction(mesajTxt -> {

				dinleyiciler.forEach(dinleyici -> dinleyici.mesajGonderTiklandi(mesajTxt, uuid));

			});

			kisiPane.setOnSayfaBasaKaydirildi(() -> {

				dinleyiciler.forEach(dinleyici -> dinleyici.sayfaBasaKaydirildi(uuid));

			});

			uuidKisiPane.put(uuid, kisiPane);

			kisiler.getChildren().add(0, kisiPane);

			setExpanded(true);

		}

		return uuidKisiPane.get(uuid);

	}

}

interface IKisilerPane {

	void mesajPaneGoster(MessagePane mesajPane, String uuid);

	void mesajPaneGizle(MessagePane mesajPane, String uuid);

	void mesajGonderTiklandi(String mesajTxt, String uuid);

	void sayfaBasaKaydirildi(String uuid);

}

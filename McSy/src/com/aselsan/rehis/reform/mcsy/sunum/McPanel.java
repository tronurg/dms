package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aselsan.rehis.reform.mcsy.sunum.intf.UygulamaDinleyici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Grup;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class McPanel extends StackPane implements IKisilerPane {

	private final VBox anaPane = new VBox();
	private final KimlikPane kimlikPane = new KimlikPane();
	private final KisilerPane kisilerPane = new KisilerPane();
	private final GruplarPane gruplarPane = new GruplarPane();
	private final Accordion kisilerGruplarPane = new Accordion();

	private final List<UygulamaDinleyici> dinleyiciler = Collections
			.synchronizedList(new ArrayList<UygulamaDinleyici>());

	public McPanel() {

		super();

		init();

	}

	private void init() {

		VBox.setMargin(kimlikPane, new Insets(10));

		kimlikPane.setOnAciklamaGuncellendi(aciklama -> dinleyicilereAciklamaGuncellendi(aciklama));

		kisilerPane.dinleyiciEkle(this);

		kisilerGruplarPane.getPanes().addAll(kisilerPane, gruplarPane);

		anaPane.getChildren().addAll(kimlikPane, kisilerGruplarPane);

		getChildren().add(anaPane);

	}

	public void dinleyiciEkle(UygulamaDinleyici dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	public void setKimlik(Kimlik kimlik) {

		kimlikPane.setKimlik(kimlik);

	}

	public void kisiGuncelle(Kisi kisi) {

		kisilerPane.kisiGuncelle(kisi);

	}

	public void grupGuncelle(Grup grup) {

		// TODO

	}

	public void gelenMesajGuncelle(Mesaj mesaj) {

		kisilerPane.gelenMesajGuncelle(mesaj);

	}

	public void gidenMesajGuncelle(Mesaj mesaj) {

		kisilerPane.gidenMesajGuncelle(mesaj);

	}

	private void dinleyicilereMesajGonderTiklandi(final String mesaj, final String uuid) {

		dinleyiciler.forEach(dinleyici -> dinleyici.mesajGonderTiklandi(mesaj, uuid));

	}

	private void dinleyicilereAciklamaGuncellendi(final String aciklama) {

		dinleyiciler.forEach(dinleyici -> dinleyici.aciklamaGuncellendi(aciklama));

	}

	@Override
	public void mesajPaneGoster(final MesajPane mesajPane) {

		mesajPane.setOnGeriAction(() -> getChildren().remove(mesajPane));

		getChildren().add(mesajPane);

	}

	@Override
	public void mesajGonderTiklandi(String mesaj, String uuid) {

		dinleyicilereMesajGonderTiklandi(mesaj, uuid);

	}

}

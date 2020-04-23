package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakMetotlar;
import com.aselsan.rehis.reform.mcsy.sunum.fabrika.SunumFabrika;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

class GruplarPane extends TitledPane {

	private final BorderPane borderPane = new BorderPane();
	private final Button grupOlusturBtn = SunumFabrika.newEkleBtn();
	private final VBox gruplar = new VBox();

	private final GrupOlusturPane grupOlusturPane = new GrupOlusturPane();

	private final List<IGruplarPane> dinleyiciler = Collections.synchronizedList(new ArrayList<IGruplarPane>());

	GruplarPane() {

		super();

		init();

	}

	private void init() {

		grupOlusturPane.setOnGeriAction(
				() -> dinleyiciler.forEach(dinleyici -> dinleyici.grupOlusturPaneGizle(grupOlusturPane)));
		grupOlusturPane.setOnGrupOlusturAction(
				() -> dinleyiciler.forEach(dinleyici -> dinleyici.grupOlusturTiklandi(grupOlusturPane)));

		initGrupOlusturBtn();

		setText(OrtakMetotlar.cevir("GRUPLAR"));

		gruplar.setPadding(new Insets(10.0));

		ScrollPane scrollPane = new ScrollPane(gruplar);
		scrollPane.setFitToWidth(true);

		borderPane.setTop(grupOlusturBtn);
		borderPane.setCenter(scrollPane);

		borderPane.setPadding(Insets.EMPTY);

		setContent(borderPane);

	}

	void dinleyiciEkle(IGruplarPane dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	void grupOlusturPaneKisiGuncelle(Kisi kisi) {

		grupOlusturPane.kisiGuncelle(kisi);

	}

	private void initGrupOlusturBtn() {

		grupOlusturBtn.setMnemonicParsing(false);
		grupOlusturBtn.setText(OrtakMetotlar.cevir("GRUP_OLUSTUR"));
		grupOlusturBtn.setTextFill(Color.GRAY);
		grupOlusturBtn.setPadding(new Insets(10.0));

		grupOlusturBtn
				.setOnAction(e -> dinleyiciler.forEach(dinleyici -> dinleyici.grupOlusturPaneGoster(grupOlusturPane)));

	}

}

interface IGruplarPane {

	void grupOlusturPaneGoster(GrupOlusturPane grupOlusturPane);

	void grupOlusturPaneGizle(GrupOlusturPane grupOlusturPane);

	void grupOlusturTiklandi(GrupOlusturPane grupOlusturPane);

	void grupMesajPaneGoster(MesajPane mesajPane, String uuid);

	void grupMesajPaneGizle(MesajPane mesajPane, String uuid);

	void grupMesajGonderTiklandi(String mesajTxt, String uuid);

	void grupSayfaBasaKaydirildi(String uuid);

}

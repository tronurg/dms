package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

class GroupsPane extends TitledPane {

	private final BorderPane borderPane = new BorderPane();
	private final Button grupOlusturBtn = ViewFactory.newEkleBtn();
	private final VBox gruplar = new VBox();

	private final CreateGroupPane grupOlusturPane = new CreateGroupPane();

	private final List<IGruplarPane> dinleyiciler = Collections.synchronizedList(new ArrayList<IGruplarPane>());

	GroupsPane() {

		super();

		init();

	}

	private void init() {

		grupOlusturPane.setOnGeriAction(
				() -> dinleyiciler.forEach(dinleyici -> dinleyici.grupOlusturPaneGizle(grupOlusturPane)));
		grupOlusturPane.setOnGrupOlusturAction(
				() -> dinleyiciler.forEach(dinleyici -> dinleyici.grupOlusturTiklandi(grupOlusturPane)));

		initGrupOlusturBtn();

		setText(CommonMethods.cevir("GRUPLAR"));

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

	void grupOlusturPaneKisiGuncelle(Contact kisi) {

		grupOlusturPane.kisiGuncelle(kisi);

	}

	private void initGrupOlusturBtn() {

		grupOlusturBtn.setMnemonicParsing(false);
		grupOlusturBtn.setText(CommonMethods.cevir("GRUP_OLUSTUR"));
		grupOlusturBtn.setTextFill(Color.GRAY);
		grupOlusturBtn.setPadding(new Insets(10.0));

		grupOlusturBtn
				.setOnAction(e -> dinleyiciler.forEach(dinleyici -> dinleyici.grupOlusturPaneGoster(grupOlusturPane)));

	}

}

interface IGruplarPane {

	void grupOlusturPaneGoster(CreateGroupPane grupOlusturPane);

	void grupOlusturPaneGizle(CreateGroupPane grupOlusturPane);

	void grupOlusturTiklandi(CreateGroupPane grupOlusturPane);

	void grupMesajPaneGoster(MessagePane mesajPane, String uuid);

	void grupMesajPaneGizle(MessagePane mesajPane, String uuid);

	void grupMesajGonderTiklandi(String mesajTxt, String uuid);

	void grupSayfaBasaKaydirildi(String uuid);

}

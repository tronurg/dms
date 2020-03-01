package com.aselsan.rehis.reform.mcsy.sunum;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakMetotlar;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

class KimlikPane extends GridPane {

	private Group profilResmi;
	private Circle durumCemberi;
	private Circle profilDairesi;

	private Label isimLabel;
	private TextField aciklamaTextField;
	private Label konumLabel;

	KimlikPane() {

		super();

		init();

	}

	private void init() {

		Separator sep = new Separator(Orientation.VERTICAL);
		setMargin(sep, new Insets(0, 5, 0, 5));

		setHgrow(getAciklamaTextField(), Priority.ALWAYS);

		add(getProfilResmi(), 0, 0, 1, 3);
		add(sep, 1, 0, 1, 3);
		add(getIsimLabel(), 2, 0, 1, 1);
		add(getAciklamaTextField(), 2, 1, 1, 1);
		add(getKonumLabel(), 2, 2, 1, 1);

	}

	void setIsim(String isim) {

	}

	void setAciklama(String aciklama) {

	}

	void setKonum(double enlem, double boylam) {

	}

	void setDurum(int durum) {

	}

	private Group getProfilResmi() {

		if (profilResmi == null) {

			profilResmi = new Group();

			profilResmi.getChildren().addAll(getDurumCemberi(), getProfilDairesi());

		}

		return profilResmi;

	}

	private Shape getDurumCemberi() {

		if (durumCemberi == null) {

			durumCemberi = new Circle(36);
			durumCemberi.setStrokeWidth(5);
			durumCemberi.setStroke(Color.GREEN);
			durumCemberi.setFill(Color.TRANSPARENT);

		}

		return durumCemberi;

	}

	private Shape getProfilDairesi() {

		if (profilDairesi == null) {

			profilDairesi = new Circle(30);
			profilDairesi.setFill(Color.DARKGRAY);

		}

		return profilDairesi;

	}

	private Label getIsimLabel() {

		if (isimLabel == null) {

			isimLabel = new Label();

		}

		return isimLabel;

	}

	private TextField getAciklamaTextField() {

		if (aciklamaTextField == null) {

			aciklamaTextField = new TextField();

			aciklamaTextField.setPromptText(OrtakMetotlar.cevir("ACIKLAMA_GIRINIZ"));

			aciklamaTextField.setEditable(false);

			aciklamaTextField.setOnMouseClicked(e -> aciklamaTextField.setEditable(true));
			aciklamaTextField.setOnKeyPressed(e -> {
				if (!e.getCode().equals(KeyCode.ENTER))
					return;
				aciklamaTextField.setEditable(false);
				// TODO
			});

			// TODO
			aciklamaTextField.disableProperty().bind(Bindings.not(aciklamaTextField.hoverProperty()));
			aciklamaTextField.setOnMouseDragEntered(e -> System.out.println("entered"));

		}

		return aciklamaTextField;

	}

	private Label getKonumLabel() {

		if (konumLabel == null) {

			konumLabel = new Label();

		}

		return konumLabel;

	}

}

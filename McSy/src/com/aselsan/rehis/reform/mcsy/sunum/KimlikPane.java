package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.concurrent.atomic.AtomicReference;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakMetotlar;
import com.aselsan.rehis.reform.mcsy.ortak.OrtakSabitler;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class KimlikPane extends GridPane {

	private Group profilResmi;
	private Circle durumCemberi;
	private Circle profilDairesi;
	private Label profilLabel;

	private Label isimLabel;
	private TextArea aciklamaTextArea;
	private Label konumLabel;

	KimlikPane() {

		super();

		init();

	}

	private void init() {

		Separator sep = new Separator(Orientation.VERTICAL);
		setMargin(sep, new Insets(0, 5, 0, 5));

		setValignment(getProfilResmi(), VPos.TOP);
		setHgrow(getAciklamaTextArea(), Priority.ALWAYS);

		add(getProfilResmi(), 0, 0, 1, 3);
		add(sep, 1, 0, 1, 3);
		add(getIsimLabel(), 2, 0, 1, 1);
		add(getAciklamaTextArea(), 2, 1, 1, 1);
		add(getKonumLabel(), 2, 2, 1, 1);

	}

	void kimlikGuncelle(Kimlik kimlik) {

		getDurumCemberi().setStroke(OrtakSabitler.DURUM_RENKLERI[kimlik.getDurum()]);
		getProfilLabel().setText(kimlik.getIsim().substring(0, 1).toUpperCase());

		getIsimLabel().setText(kimlik.getIsim());
		getAciklamaTextArea().setText(kimlik.getAciklama());
		getKonumLabel().setText(kimlik.getEnlem() == null || kimlik.getBoylam() == null ? ""
				: "(" + String.format("%.2f", kimlik.getEnlem()) + String.format("%.2f", kimlik.getEnlem()) + ")");

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

			profilResmi.getChildren().addAll(getDurumCemberi(), getProfilDairesi(), getProfilLabel());

		}

		return profilResmi;

	}

	private Shape getDurumCemberi() {

		if (durumCemberi == null) {

			durumCemberi = new Circle(35);
			durumCemberi.setStrokeWidth(5);
			durumCemberi.setStroke(Color.LIMEGREEN);
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

	private Label getProfilLabel() {

		if (profilLabel == null) {

			profilLabel = new Label();

			profilLabel.setFont(Font.font(null, FontWeight.BOLD, 35.0));

			profilLabel.translateXProperty().bind(Bindings
					.createDoubleBinding(() -> -profilLabel.widthProperty().get() / 2, profilLabel.widthProperty()));
			profilLabel.translateYProperty().bind(Bindings
					.createDoubleBinding(() -> -profilLabel.heightProperty().get() / 2, profilLabel.heightProperty()));

		}

		return profilLabel;

	}

	private Label getIsimLabel() {

		if (isimLabel == null) {

			isimLabel = new Label();

			isimLabel.setFont(Font.font(null, FontWeight.BOLD, 25.0));

		}

		return isimLabel;

	}

	private TextArea getAciklamaTextArea() {

		if (aciklamaTextArea == null) {

			aciklamaTextArea = new TextArea();

			final AtomicReference<String> sonAciklama = new AtomicReference<String>();

			aciklamaTextArea.setTextFormatter(
					new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

			aciklamaTextArea.setPrefRowCount(1);

			aciklamaTextArea.setWrapText(true);
			aciklamaTextArea.setPromptText(OrtakMetotlar.cevir("ACIKLAMA_GIRINIZ"));
			aciklamaTextArea.setFocusTraversable(false);
			aciklamaTextArea.setEditable(false);

			aciklamaTextArea.setOnMouseClicked(e -> {
				sonAciklama.set(aciklamaTextArea.getText());
				aciklamaTextArea.setEditable(true);
			});
			aciklamaTextArea.setOnKeyPressed(e -> {
				KeyCode code = e.getCode();
				if (!(code.equals(KeyCode.ENTER) || code.equals(KeyCode.ESCAPE)))
					return;

				if (e.getCode().equals(KeyCode.ESCAPE))
					aciklamaTextArea.setText(sonAciklama.get());

				aciklamaTextArea.setEditable(false);
				requestFocus();
				// TODO

			});

			// TODO: border

			aciklamaTextArea.setBackground(
					new Background(new BackgroundFill(Color.GREENYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));

		}

		return aciklamaTextArea;

	}

	private Label getKonumLabel() {

		if (konumLabel == null) {

			konumLabel = new Label();

		}

		return konumLabel;

	}

}

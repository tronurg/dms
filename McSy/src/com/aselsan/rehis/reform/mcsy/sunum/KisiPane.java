package com.aselsan.rehis.reform.mcsy.sunum;

import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import zmq.util.function.Consumer;

class KisiPane extends GridPane {

	private Group profilResmi;
	private Circle durumCemberi;
	private Circle profilDairesi;
	private Label profilLabel;

	private Label isimLabel;
	private Label aciklamaLabel;
	private Label konumLabel;

	private final MesajPane mesajPane = new MesajPane();

	KisiPane() {

		super();

		init();

	}

	private void init() {

		Separator sep = new Separator(Orientation.VERTICAL);
		setMargin(sep, new Insets(0, 5, 0, 5));

		setValignment(getProfilResmi(), VPos.TOP);
		setHgrow(getAciklamaLabel(), Priority.ALWAYS);

		add(getProfilResmi(), 0, 0, 1, 3);
		add(sep, 1, 0, 1, 3);
		add(getIsimLabel(), 2, 0, 1, 1);
		add(getAciklamaLabel(), 2, 1, 1, 1);
		add(getKonumLabel(), 2, 2, 1, 1);

	}

	void setOnMesajPaneGoster(Consumer<MesajPane> consumer) {

		setOnMouseClicked(e -> {

			if (!(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2 && e.isStillSincePress()))
				return;

			consumer.accept(mesajPane);

		});

	}

	void setOnMesajGonderAction(Consumer<String> consumer) {

		mesajPane.setOnMesajGonderAction(mesaj -> consumer.accept(mesaj));

	}

	void kisiGuncelle(Kisi kisi) {

		getDurumCemberi().setStroke(kisi.getDurum().getDurumRengi());
		getProfilLabel().setText(kisi.getIsim().substring(0, 1).toUpperCase());

		getIsimLabel().setText(kisi.getIsim());
		getAciklamaLabel().setText(kisi.getAciklama());
		getKonumLabel().setText(kisi.getEnlem() == null || kisi.getBoylam() == null ? ""
				: "(" + String.format("%.2f", kisi.getEnlem()) + String.format("%.2f", kisi.getEnlem()) + ")");

	}

	void mesajAlindi(String mesaj) {

		mesajPane.gelenMesajEkle(mesaj);

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

			durumCemberi = new Circle(28);
			durumCemberi.setStrokeWidth(4);
			durumCemberi.setStroke(Color.LIMEGREEN);
			durumCemberi.setFill(Color.TRANSPARENT);

		}

		return durumCemberi;

	}

	private Shape getProfilDairesi() {

		if (profilDairesi == null) {

			profilDairesi = new Circle(24);
			profilDairesi.setFill(Color.DARKGRAY);

		}

		return profilDairesi;

	}

	private Label getProfilLabel() {

		if (profilLabel == null) {

			profilLabel = new Label();

			profilLabel.setFont(Font.font(null, FontWeight.BOLD, 28.0));

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

			isimLabel.setFont(Font.font(null, FontWeight.BOLD, 20.0));

		}

		return isimLabel;

	}

	private Label getAciklamaLabel() {

		if (aciklamaLabel == null) {

			aciklamaLabel = new Label();

			aciklamaLabel.setWrapText(true);

		}

		return aciklamaLabel;

	}

	private Label getKonumLabel() {

		if (konumLabel == null) {

			konumLabel = new Label();

		}

		return konumLabel;

	}

}

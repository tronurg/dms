package com.aselsan.rehis.reform.mcsy.sunum;

import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import zmq.util.function.Consumer;

class KisiPane extends GridPane {

	private static final double SIZE = 24.0;

	private final Group profilResmi = new Group();
	private final Circle durumCemberi = new Circle(SIZE);
	private final Circle profilDairesi = new Circle(SIZE * 0.8);
	private final Label profilLabel = new Label();

	private final Label isimLabel = new Label();
	private final Label aciklamaLabel = new Label();
	private final Label konumLabel = new Label();

	private final MesajPane mesajPane = new MesajPane();

	private final ObjectProperty<Kisi> kisiProperty = new SimpleObjectProperty<Kisi>();

	KisiPane() {

		super();

		init();

	}

	private void init() {

		// Kisi properties

		durumCemberi.strokeProperty().bind(Bindings.createObjectBinding(
				() -> kisiProperty.get() == null ? null : kisiProperty.get().getDurum().getDurumRengi(), kisiProperty));
		profilLabel.textProperty().bind(Bindings.createStringBinding(
				() -> kisiProperty.get() == null ? null : kisiProperty.get().getIsim().substring(0, 1).toUpperCase(),
				kisiProperty));

		isimLabel.textProperty().bind(Bindings.createStringBinding(
				() -> kisiProperty.get() == null ? null : kisiProperty.get().getIsim(), kisiProperty));
		aciklamaLabel.textProperty().bind(Bindings.createStringBinding(
				() -> kisiProperty.get() == null ? null : kisiProperty.get().getAciklama(), kisiProperty));
		konumLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			Kisi kisi = kisiProperty.get();
			if (kisi == null)
				return null;
			return kisi.getEnlem() == null || kisi.getBoylam() == null ? ""
					: "(" + String.format("%.2f", kisi.getEnlem()) + String.format("%.2f", kisi.getEnlem()) + ")";
		}, kisiProperty));

		// Mesaj properties

		mesajPane.durumColorProperty().bind(Bindings.createObjectBinding(
				() -> kisiProperty.get() == null ? null : kisiProperty.get().getDurum().getDurumRengi(), kisiProperty));
		mesajPane.isimProperty().bind(Bindings.createStringBinding(
				() -> kisiProperty.get() == null ? null : kisiProperty.get().getIsim(), kisiProperty));

		//

		initProfilResmi();
		initDurumCemberi();
		initProfilDairesi();
		initProfilLabel();
		initIsimLabel();
		initAciklamaLabel();
		initKonumLabel();

		Separator sep = new Separator(Orientation.VERTICAL);
		setMargin(sep, new Insets(0, 5, 0, 5));

		setValignment(profilResmi, VPos.TOP);
		setHgrow(aciklamaLabel, Priority.ALWAYS);

		add(profilResmi, 0, 0, 1, 3);
		add(sep, 1, 0, 1, 3);
		add(isimLabel, 2, 0, 1, 1);
		add(aciklamaLabel, 2, 1, 1, 1);
		add(konumLabel, 2, 2, 1, 1);

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

		kisiProperty.set(kisi);

	}

	void gelenMesajGuncelle(String mesajId, Mesaj mesaj) {

		mesajPane.gelenMesajGuncelle(mesajId, mesaj);

	}

	void gidenMesajGuncelle(String mesajId, Mesaj mesaj) {

		mesajPane.gidenMesajGuncelle(mesajId, mesaj);

	}

	private void initProfilResmi() {

		profilResmi.getChildren().addAll(durumCemberi, profilDairesi, profilLabel);

	}

	private void initDurumCemberi() {

		durumCemberi.setStrokeWidth(SIZE * 0.2);
		durumCemberi.setFill(Color.TRANSPARENT);

	}

	private void initProfilDairesi() {

		profilDairesi.setFill(Color.DARKGRAY);

	}

	private void initProfilLabel() {

		profilLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		profilLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE));

		profilLabel.translateXProperty().bind(Bindings.createDoubleBinding(() -> -profilLabel.widthProperty().get() / 2,
				profilLabel.widthProperty()));
		profilLabel.translateYProperty().bind(Bindings
				.createDoubleBinding(() -> -profilLabel.heightProperty().get() / 2, profilLabel.heightProperty()));

	}

	private void initIsimLabel() {

		isimLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		isimLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE * 0.8));

	}

	private void initAciklamaLabel() {

		aciklamaLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private void initKonumLabel() {

		konumLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

}

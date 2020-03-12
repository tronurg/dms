package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakMetotlar;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class KimlikPane extends GridPane {

	private static final double SIZE = 30.0;

	private final Group profilResmi = new Group();
	private final Circle durumCemberi = new Circle(SIZE);
	private final Circle profilDairesi = new Circle(SIZE * 0.8);
	private final Label profilLabel = new Label();

	private final Label isimLabel = new Label();
	private final TextArea aciklamaTextArea = new TextArea();
	private final Label konumLabel = new Label();

	private final AtomicReference<Consumer<String>> aciklamaGuncellendiConsumer = new AtomicReference<Consumer<String>>();

	KimlikPane() {

		super();

		init();

	}

	private void init() {

		initProfilResmi();
		initDurumCemberi();
		initProfilDairesi();
		initProfilLabel();
		initIsimLabel();
		initAciklamaTextArea();
		initKonumLabel();

		Separator sep = new Separator(Orientation.VERTICAL);
		setMargin(sep, new Insets(0, 5, 0, 5));

		setValignment(profilResmi, VPos.TOP);
		setHgrow(aciklamaTextArea, Priority.ALWAYS);

		add(profilResmi, 0, 0, 1, 3);
		add(sep, 1, 0, 1, 3);
		add(isimLabel, 2, 0, 1, 1);
		add(aciklamaTextArea, 2, 1, 1, 1);
		add(konumLabel, 2, 2, 1, 1);

	}

	void setOnAciklamaGuncellendi(Consumer<String> consumer) {

		aciklamaGuncellendiConsumer.set(consumer);

	}

	void setKimlikProperty(ObjectProperty<Kimlik> kimlikProperty) {

		durumCemberi.strokeProperty()
				.bind(Bindings.createObjectBinding(
						() -> kimlikProperty.get() == null ? null : kimlikProperty.get().getDurum().getDurumRengi(),
						kimlikProperty));
		profilLabel.textProperty()
				.bind(Bindings
						.createStringBinding(
								() -> kimlikProperty.get() == null ? null
										: kimlikProperty.get().getIsim().substring(0, 1).toUpperCase(),
								kimlikProperty));

		isimLabel.textProperty().bind(Bindings.createStringBinding(
				() -> kimlikProperty.get() == null ? null : kimlikProperty.get().getIsim(), kimlikProperty));
		aciklamaTextArea.accessibleTextProperty().bind(Bindings.createStringBinding(
				() -> kimlikProperty.get() == null ? null : kimlikProperty.get().getAciklama(), kimlikProperty));
		konumLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			Kimlik kimlik = kimlikProperty.get();
			if (kimlik == null)
				return null;
			return kimlik.getEnlem() == null || kimlik.getBoylam() == null ? ""
					: "(" + String.format("%.2f", kimlik.getEnlem()) + String.format("%.2f", kimlik.getEnlem()) + ")";
		}, kimlikProperty));

		// Aciklamayi guncelle

		aciklamaTextArea.setText(kimlikProperty.get().getAciklama());

		kimlikProperty.addListener(new ChangeListener<Kimlik>() {

			@Override
			public void changed(ObservableValue<? extends Kimlik> arg0, Kimlik arg1, Kimlik arg2) {

				aciklamaTextArea.setText(arg2.getAciklama());

			}

		});

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

	private void initAciklamaTextArea() {

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

			e.consume();
			aciklamaTextArea.setEditable(false);
			requestFocus();

			if (aciklamaGuncellendiConsumer.get() != null)
				aciklamaGuncellendiConsumer.get().accept(aciklamaTextArea.getText());

		});

		aciklamaTextArea
				.setBackground(new Background(new BackgroundFill(Color.GREENYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));

	}

	private void initKonumLabel() {

		konumLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

}

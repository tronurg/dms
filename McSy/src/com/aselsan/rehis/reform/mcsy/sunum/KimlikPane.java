package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakMetotlar;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;

import javafx.beans.binding.Bindings;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
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
	private final TextField aciklamaTextField = new TextField();
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

		setHgap(5.0);
		setValignment(profilResmi, VPos.TOP);
		setHgrow(aciklamaTextField, Priority.ALWAYS);

		add(profilResmi, 0, 0, 1, 3);
		add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		add(isimLabel, 2, 0, 1, 1);
		add(aciklamaTextField, 2, 1, 1, 1);
		add(konumLabel, 2, 2, 1, 1);

	}

	void setOnAciklamaGuncellendi(Consumer<String> consumer) {

		aciklamaGuncellendiConsumer.set(consumer);

	}

	void setKimlik(Kimlik kimlik) {

		durumCemberi.setStroke(kimlik.getDurum().getDurumRengi());
		profilLabel.setText(kimlik.getIsim().substring(0, 1).toUpperCase());

		isimLabel.setText(kimlik.getIsim());
		aciklamaTextField.setText(kimlik.getAciklama());
		konumLabel.setText(kimlik.getEnlem() == null || kimlik.getBoylam() == null ? ""
				: "(" + String.format("%.2f", kimlik.getEnlem()) + String.format("%.2f", kimlik.getEnlem()) + ")");

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

		aciklamaTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

		aciklamaTextField.setPromptText(OrtakMetotlar.cevir("ACIKLAMA_GIRINIZ"));
		aciklamaTextField.setFocusTraversable(false);
		aciklamaTextField.setEditable(false);

		aciklamaTextField.setOnMouseClicked(e -> {

			if (aciklamaTextField.isEditable())
				return;

			if (!e.getButton().equals(MouseButton.PRIMARY))
				return;

			sonAciklama.set(aciklamaTextField.getText());
			aciklamaTextField.setEditable(true);

		});

		aciklamaTextField.setOnKeyPressed(e -> {

			KeyCode code = e.getCode();
			if (!(code.equals(KeyCode.ENTER) || code.equals(KeyCode.ESCAPE)))
				return;

			if (e.getCode().equals(KeyCode.ESCAPE))
				aciklamaTextField.setText(sonAciklama.get());

			e.consume();
			aciklamaTextField.setEditable(false);
			requestFocus();

			String aciklama = aciklamaTextField.getText();

			if (!(aciklama.equals(sonAciklama.get()) || aciklamaGuncellendiConsumer.get() == null))
				aciklamaGuncellendiConsumer.get().accept(aciklama);

		});

		aciklamaTextField.setBorder(new Border(new BorderStroke[] { new BorderStroke(Color.LIGHTGRAY,
				BorderStrokeStyle.SOLID, new CornerRadii(15.0), BorderWidths.DEFAULT) }));
		aciklamaTextField.setBackground(Background.EMPTY);

	}

	private void initKonumLabel() {

		konumLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

}

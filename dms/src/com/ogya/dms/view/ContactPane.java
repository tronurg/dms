package com.ogya.dms.view;

import java.util.HashSet;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageDirection;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import zmq.util.function.Consumer;

class ContactPane extends GridPane {

	private static final double SIZE = 24.0;

	private final Group profilResmi = new Group();
	private final Circle durumCemberi = new Circle(SIZE);
	private final Circle profilDairesi = new Circle(SIZE * 0.8);
	private final Label profilLabel = new Label();

	private final Label isimLabel = new Label();
	private final Label aciklamaLabel = new Label();
	private final Label konumLabel = new Label();

	private final Label okunmamisMesajlarLabel = new Label() {

		@Override
		public Orientation getContentBias() {
			return Orientation.VERTICAL;
		}

		@Override
		protected double computeMinWidth(double height) {
			return height;
		}

	};

	private final MessagePane mesajPane = new MessagePane();

	private final ObservableSet<Long> okunmamisMesajlar = FXCollections.observableSet(new HashSet<Long>());

	ContactPane() {

		super();

		init();

	}

	private void init() {

		initProfilResmi();
		initDurumCemberi();
		initProfilDairesi();
		initProfilLabel();
		initIsimLabel();
		initAciklamaLabel();
		initKonumLabel();
		initOkunmamisMesajlarLabel();

		setHgap(5.0);
		setValignment(profilResmi, VPos.TOP);
		setHgrow(aciklamaLabel, Priority.ALWAYS);

		add(profilResmi, 0, 0, 1, 3);
		add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		add(isimLabel, 2, 0, 1, 1);
		add(aciklamaLabel, 2, 1, 1, 1);
		add(konumLabel, 2, 2, 1, 1);
		add(okunmamisMesajlarLabel, 3, 0, 1, 2);

	}

	void kisiGuncelle(Contact kisi) {

		if (kisi == null)
			return;

		durumCemberi.setStroke(kisi.getDurum().getDurumRengi());
		profilLabel.setText(kisi.getIsim().substring(0, 1).toUpperCase());

		isimLabel.setText(kisi.getIsim());
		aciklamaLabel.setText(kisi.getAciklama());
		konumLabel.setText(kisi.getEnlem() == null || kisi.getBoylam() == null ? ""
				: "(" + String.format("%.2f", kisi.getEnlem()) + String.format("%.2f", kisi.getEnlem()) + ")");

		mesajPane.setDurumColor(kisi.getDurum().getDurumRengi());
		mesajPane.setIsim(kisi.getIsim());

	}

	void setOnMesajPaneGoster(Consumer<MessagePane> consumer) {

		setOnMouseClicked(e -> {

			if (!(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2 && e.isStillSincePress()))
				return;

			consumer.accept(mesajPane);

		});

	}

	void setOnMesajPaneGizle(Consumer<MessagePane> consumer) {

		mesajPane.setOnGeriAction(() -> consumer.accept(mesajPane));

	}

	void setOnMesajGonderAction(Consumer<String> consumer) {

		mesajPane.setOnMesajGonderAction(mesajTxt -> consumer.accept(mesajTxt));

	}

	void setOnSayfaBasaKaydirildi(Runnable runnable) {

		mesajPane.setOnSayfaBasaKaydirildi(() -> runnable.run());

	}

	void mesajEkle(Message mesaj, MessageDirection mesajYonu) {

		if (mesajYonu.equals(MessageDirection.GELEN) && !mesaj.getMesajDurumu().equals(MessageStatus.OKUNDU))
			okunmamisMesajlar.add(mesaj.getId());

		mesajPane.mesajEkle(mesaj, mesajYonu);

	}

	void mesajGuncelle(Message mesaj) {

		if (mesaj.getMesajDurumu().equals(MessageStatus.OKUNDU))
			okunmamisMesajlar.remove(mesaj.getId());

		mesajPane.mesajGuncelle(mesaj);

	}

	void ekraniMesajaKaydir(Long mesajId) {

		mesajPane.ekraniMesajaKaydir(mesajId);

	}

	void konumuKaydet(Long mesajId) {

		mesajPane.konumuKaydet(mesajId);

	}

	void kaydedilenKonumaGit() {

		mesajPane.kaydedilenKonumaGit();

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

	private void initOkunmamisMesajlarLabel() {

		okunmamisMesajlarLabel.backgroundProperty()
				.bind(Bindings.createObjectBinding(
						() -> new Background(new BackgroundFill(Color.RED,
								new CornerRadii(okunmamisMesajlarLabel.getHeight() / 2), Insets.EMPTY)),
						okunmamisMesajlarLabel.heightProperty()));

		okunmamisMesajlarLabel.setAlignment(Pos.CENTER);

		okunmamisMesajlarLabel.setFont(Font.font(null, FontWeight.BOLD, okunmamisMesajlarLabel.getFont().getSize()));
		okunmamisMesajlarLabel.setTextFill(Color.WHITE);

		okunmamisMesajlarLabel.visibleProperty().bind(Bindings.size(okunmamisMesajlar).greaterThan(0));
		okunmamisMesajlarLabel.textProperty().bind(Bindings.size(okunmamisMesajlar).asString());

	}

}

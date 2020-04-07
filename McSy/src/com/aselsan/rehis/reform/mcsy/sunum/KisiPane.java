package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.Iterator;
import java.util.LinkedHashSet;

import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;
import com.aselsan.rehis.reform.mcsy.veriyapilari.MesajDurumu;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
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

class KisiPane extends GridPane {

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

	private final MesajPane mesajPane = new MesajPane();

	private final ObservableSet<String> okunmamisMesajlar = FXCollections.observableSet(new LinkedHashSet<String>());

	KisiPane() {

		super();

		init();

	}

	private void init() {

		// Okunmamis mesaj sayisi properties
		okunmamisMesajlar.addListener(new SetChangeListener<String>() {

			@Override
			public void onChanged(Change<? extends String> arg0) {

				int okunmamisMesajSayisi = arg0.getSet().size();

				okunmamisMesajlarLabel.setText(okunmamisMesajSayisi == 0 ? "" : String.valueOf(okunmamisMesajSayisi));

			}

		});

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

	void kisiGuncelle(Kisi kisi) {

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

	void setOnMesajPaneGoster(Consumer<MesajPane> consumer) {

		setOnMouseClicked(e -> {

			if (!(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2 && e.isStillSincePress()))
				return;

			consumer.accept(mesajPane);

			Platform.runLater(() -> {

				Iterator<String> mesajIdIt = okunmamisMesajlar.iterator();

				if (mesajIdIt.hasNext()) {

					mesajPane.sayfayiMesajaKaydir(mesajIdIt.next());

				} else {

					mesajPane.sayfayiSonaKaydir();

				}

			});

		});

	}

	void setOnMesajPaneGizle(Consumer<MesajPane> consumer) {

		mesajPane.setOnGeriAction(() -> consumer.accept(mesajPane));

	}

	void setOnMesajGonderAction(Consumer<String> consumer) {

		mesajPane.setOnMesajGonderAction(mesaj -> consumer.accept(mesaj));

	}

	void gelenMesajGuncelle(String mesajId, Mesaj mesaj) {

		if (mesaj.getMesajDurumu().equals(MesajDurumu.OKUNDU)) {
			okunmamisMesajlar.remove(mesajId);
		} else {
			okunmamisMesajlar.add(mesajId);
		}

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

	private void initOkunmamisMesajlarLabel() {

		okunmamisMesajlarLabel.backgroundProperty()
				.bind(Bindings.createObjectBinding(
						() -> okunmamisMesajlarLabel.getText().isEmpty() ? null
								: new Background(new BackgroundFill(Color.RED,
										new CornerRadii(okunmamisMesajlarLabel.getHeight() / 2), Insets.EMPTY)),
						okunmamisMesajlarLabel.textProperty(), okunmamisMesajlarLabel.heightProperty()));

		okunmamisMesajlarLabel.setAlignment(Pos.CENTER);

		okunmamisMesajlarLabel.setFont(Font.font(null, FontWeight.BOLD, okunmamisMesajlarLabel.getFont().getSize()));
		okunmamisMesajlarLabel.setTextFill(Color.WHITE);

	}

}

package com.aselsan.rehis.reform.mcsy.sunum;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.aselsan.rehis.reform.mcsy.sunum.fabrika.SunumFabrika;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;
import com.aselsan.rehis.reform.mcsy.veriyapilari.MesajYonu;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class MesajPane extends BorderPane {

	private final HBox ustPane = new HBox(5.0);
	private final VBox ortaPane = new VBox(10.0);
	private final HBox altPane = new HBox(5.0);

	private final ScrollPane scrollPane = new ScrollPane(ortaPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button geriBtn = SunumFabrika.newGeriBtn();
	private final Circle durumCircle = new Circle(7.0);
	private final Label isimLabel = new Label();
	private final TextArea mesajArea = new TextArea();
	private final Button gonderBtn = SunumFabrika.newGonderBtn();

	private final Map<LocalDate, GunKutusu> gunKutulari = Collections
			.synchronizedMap(new HashMap<LocalDate, GunKutusu>());

	private final Map<Long, MesajBalonu> mesajBalonlari = Collections.synchronizedMap(new HashMap<Long, MesajBalonu>());

	private final AtomicBoolean otoKaydirma = new AtomicBoolean(true);

	private final Comparator<Node> gunKutusuSiralayici = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof GunKutusu && arg1 instanceof GunKutusu))
				return 0;

			GunKutusu gunKutusu0 = (GunKutusu) arg0;
			GunKutusu gunKutusu1 = (GunKutusu) arg1;

			return gunKutusu0.getTarih().compareTo(gunKutusu1.getTarih());

		}

	};

	MesajPane() {

		super();

		init();

	}

	private void init() {

		ustPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		altPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		ustPane.setPadding(new Insets(5.0));
		ortaPane.setPadding(new Insets(5.0));
		altPane.setPadding(new Insets(5.0));

		ustPane.setAlignment(Pos.CENTER_LEFT);

		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);

		mesajArea.setPrefRowCount(1);
		mesajArea.setWrapText(true);
		mesajArea.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 400 ? null : change));

		HBox.setHgrow(mesajArea, Priority.ALWAYS);

		mesajArea.setOnKeyPressed(e -> {

			if (e.getCode().equals(KeyCode.ENTER)) {

				gonderBtn.fire();

				e.consume();

			}

		});

		HBox.setMargin(durumCircle, new Insets(5.0, 5.0, 5.0, 15.0));
		isimLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0));

		ustPane.getChildren().addAll(geriBtn, durumCircle, isimLabel);
		altPane.getChildren().addAll(mesajArea, gonderBtn);

		setTop(ustPane);
		setCenter(scrollPane);
		setBottom(altPane);

		ortaPane.heightProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {

				if (otoKaydirma.get())
					sayfayiSonaKaydir();

			}

		});

		scrollPane.vvalueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {

				otoKaydirma.set((double) arg1 == scrollPane.getVmax());

			}

		});

	}

	void setDurumColor(Paint fill) {

		durumCircle.setFill(fill);

	}

	void setIsim(String isim) {

		isimLabel.setText(isim);

	}

	void mesajEkle(Mesaj mesaj, MesajYonu mesajYonu) {

		if (mesajBalonlari.containsKey(mesaj.getId()))
			return;

		Date mesajTarihi = mesaj.getTarih();

		MesajBalonu mesajBalonu = new MesajBalonu(mesaj.getIcerik(), mesajTarihi, mesajYonu);
		mesajBalonu.setIletiRenkeri(mesaj.getMesajDurumu().getBeklemeRengi(),
				mesaj.getMesajDurumu().getIletildiRengi());

		mesajBalonlari.put(mesaj.getId(), mesajBalonu);

		LocalDate mesajGunu = mesajTarihi.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if (!gunKutulari.containsKey(mesajGunu)) {
			GunKutusu gunKutusu = new GunKutusu(mesajGunu);
			gunKutulari.put(mesajGunu, gunKutusu);
			ortaPane.getChildren().add(gunKutusu);
			FXCollections.sort(ortaPane.getChildren(), gunKutusuSiralayici);
		}

		gunKutulari.get(mesajGunu).mesajBalonuEkle(mesajBalonu);

	}

	void mesajGuncelle(Mesaj mesaj) {

		if (!mesajBalonlari.containsKey(mesaj.getId()))
			return;

		mesajBalonlari.get(mesaj.getId()).setIletiRenkeri(mesaj.getMesajDurumu().getBeklemeRengi(),
				mesaj.getMesajDurumu().getIletildiRengi());

	}

	void sayfayiSonaKaydir() {

		scrollPane.setVvalue(scrollPane.getVmax());

	}

	void setOnGeriAction(final Runnable runnable) {

		geriBtn.setOnAction(e -> runnable.run());

	}

	void setOnSayfaBasaKaydirildi(final Runnable runnable) {

		// TODO: Iyilestirilecek

		scrollPane.setOnScroll(e -> {

			if (e.getDeltaY() < 0)
				return;

			runnable.run();

		});

	}

	void setOnMesajGonderAction(final Consumer<String> consumer) {

		gonderBtn.setOnAction(e -> {

			final String mesajTxt = mesajArea.getText().trim();

			mesajArea.setText("");

			if (mesajTxt.isEmpty())
				return;

			consumer.accept(mesajTxt);

		});

	}

	private static class MesajBalonu extends GridPane {

		private static final double RADIUS = 3.0;
		private static final SimpleDateFormat SAAT_DAKIKA = new SimpleDateFormat("HH:mm");

		private final Date tarih;
		private final MesajYonu mesajYonu;

		private final GridPane mesajPane = new GridPane();
		private final Label mesajLbl;
		private final Label zamanLbl;
		private final Group bilgiGrp = new Group();
		private final Circle beklemeCircle = new Circle(RADIUS, Color.TRANSPARENT);
		private final Circle iletildiCircle = new Circle(RADIUS, Color.TRANSPARENT);

		private final Region bosluk = new Region();

		MesajBalonu(String mesaj, Date tarih, MesajYonu mesajYonu) {

			super();

			this.tarih = tarih;
			this.mesajYonu = mesajYonu;

			mesajLbl = new Label(mesaj);
			zamanLbl = new Label(SAAT_DAKIKA.format(tarih));

			init();

		}

		private void init() {

			zamanLbl.setFont(Font.font(mesajLbl.getFont().getSize() * 0.75));
			zamanLbl.setTextFill(Color.DIMGRAY);

			ColumnConstraints colDar = new ColumnConstraints();
			colDar.setPercentWidth(20.0);
			ColumnConstraints colGenis = new ColumnConstraints();
			colGenis.setPercentWidth(80.0);

			mesajLbl.setWrapText(true);

			HBox.setHgrow(bosluk, Priority.ALWAYS);

			switch (mesajYonu) {

			case GELEN:

				initGelenMesajPane();

				getColumnConstraints().addAll(colGenis, colDar);

				mesajPane.setBackground(
						new Background(new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0), Insets.EMPTY)));

				GridPane.setHalignment(mesajPane, HPos.LEFT);
				GridPane.setHalignment(zamanLbl, HPos.LEFT);

				add(mesajPane, 0, 0, 1, 1);
				add(bosluk, 1, 0, 1, 1);

				break;

			case GIDEN:

				initGidenMesajPane();

				getColumnConstraints().addAll(colDar, colGenis);

				mesajPane.setBackground(
						new Background(new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0), Insets.EMPTY)));

				GridPane.setHalignment(mesajPane, HPos.RIGHT);
				GridPane.setHalignment(zamanLbl, HPos.RIGHT);

				add(bosluk, 0, 0, 1, 1);
				add(mesajPane, 1, 0, 1, 1);

				break;

			}

		}

		void setIletiRenkeri(Paint beklemeCircleColor, Paint iletildiCircleColor) {

			beklemeCircle.setFill(beklemeCircleColor);
			iletildiCircle.setFill(iletildiCircleColor);

		}

		Date getTarih() {

			return tarih;

		}

		private void initGelenMesajPane() {

			initMesajPane();

			mesajPane.add(mesajLbl, 0, 0, 1, 1);
			mesajPane.add(zamanLbl, 0, 1, 1, 1);

		}

		private void initGidenMesajPane() {

			initMesajPane();
			initBilgiGrp();

			mesajPane.add(mesajLbl, 0, 0, 2, 1);
			mesajPane.add(zamanLbl, 0, 1, 1, 1);
			mesajPane.add(bilgiGrp, 1, 1, 1, 1);

		}

		private void initMesajPane() {

			GridPane.setFillWidth(mesajPane, false);

			GridPane.setHgrow(zamanLbl, Priority.ALWAYS);

			mesajPane.setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
					new CornerRadii(10.0), BorderWidths.DEFAULT)));

			mesajPane.setPadding(new Insets(5.0));
			mesajPane.setHgap(5.0);

		}

		private void initBilgiGrp() {

			iletildiCircle.setLayoutX(2 * RADIUS);
			bilgiGrp.getChildren().addAll(beklemeCircle, iletildiCircle);

		}

	}

	private static class GunKutusu extends BorderPane {

		private static final DateTimeFormatter GUN_AY_YIL = DateTimeFormatter.ofPattern("dd.MM.uuuu");

		private final LocalDate tarih;

		private final Label tarihLabel;
		private final VBox mesajBox = new VBox(5.0);

		private final Comparator<Node> mesajBalonuSiralayici = new Comparator<Node>() {

			@Override
			public int compare(Node arg0, Node arg1) {

				if (!(arg0 instanceof MesajBalonu && arg1 instanceof MesajBalonu))
					return 0;

				MesajBalonu mesajBalonu0 = (MesajBalonu) arg0;
				MesajBalonu mesajBalonu1 = (MesajBalonu) arg1;

				return mesajBalonu0.getTarih().compareTo(mesajBalonu1.getTarih());

			}

		};

		GunKutusu(LocalDate tarih) {

			super();

			this.tarih = tarih;

			tarihLabel = new Label(GUN_AY_YIL.format(tarih));

			init();

		}

		private void init() {

			// init tarihLabel
			tarihLabel.setPadding(new Insets(0.0, 5.0, 0.0, 5.0));
			tarihLabel.setFont(Font.font(null, FontWeight.BOLD, tarihLabel.getFont().getSize()));
			tarihLabel.setTextFill(Color.GRAY);
			tarihLabel.setBackground(
					new Background(new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(5.0), Insets.EMPTY)));
			tarihLabel.setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
					new CornerRadii(5.0), BorderWidths.DEFAULT, Insets.EMPTY)));
			BorderPane.setAlignment(tarihLabel, Pos.CENTER);
			BorderPane.setMargin(tarihLabel, new Insets(0.0, 0.0, 5.0, 0.0));

			setTop(tarihLabel);
			setCenter(mesajBox);

		}

		void mesajBalonuEkle(MesajBalonu mesajBalonu) {

			mesajBox.getChildren().add(mesajBalonu);

			FXCollections.sort(mesajBox.getChildren(), mesajBalonuSiralayici);

		}

		LocalDate getTarih() {

			return tarih;

		}

	}

}

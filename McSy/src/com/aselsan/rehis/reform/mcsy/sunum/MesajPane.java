package com.aselsan.rehis.reform.mcsy.sunum;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.aselsan.rehis.reform.mcsy.sunum.fabrika.SunumFabrika;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
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

	private static final SimpleDateFormat GUN_AY_YIL = new SimpleDateFormat("dd.MM.yyyy");

	private final HBox ustPane = new HBox(5.0);
	private final VBox ortaPane = new VBox(5.0);
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

	private final Map<String, MesajBalonu> mesajBalonlari = Collections
			.synchronizedMap(new HashMap<String, MesajBalonu>());

	private final AtomicBoolean otoKaydirma = new AtomicBoolean(true);
	private final AtomicReference<String> sonEklenenTarih = new AtomicReference<String>(null);

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
		ortaPane.setAlignment(Pos.CENTER);

		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
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

	void gelenMesajGuncelle(String mesajId, Mesaj mesaj) {

		if (!mesajBalonlari.containsKey(mesajId)) {

			MesajBalonu gelenMesajBalonu = new MesajBalonu(mesaj.getIcerik(), mesaj.getTarih(), MesajTipi.GELEN);
			mesajBalonlari.put(mesajId, gelenMesajBalonu);

			String tarih = GUN_AY_YIL.format(mesaj.getTarih());
			if (!tarih.equals(sonEklenenTarih.getAndSet(tarih))) {
				ortaPane.getChildren().add(newTarihLabel(tarih));
			}

			ortaPane.getChildren().add(gelenMesajBalonu);

		}

	}

	void gidenMesajGuncelle(String mesajId, Mesaj mesaj) {

		if (!mesajBalonlari.containsKey(mesajId)) {

			MesajBalonu gidenMesajBalonu = new MesajBalonu(mesaj.getIcerik(), mesaj.getTarih(), MesajTipi.GIDEN);
			mesajBalonlari.put(mesajId, gidenMesajBalonu);

			String tarih = GUN_AY_YIL.format(mesaj.getTarih());
			if (!tarih.equals(sonEklenenTarih.getAndSet(tarih))) {
				ortaPane.getChildren().add(newTarihLabel(tarih));
			}

			ortaPane.getChildren().add(gidenMesajBalonu);

		}

		mesajBalonlari.get(mesajId).setIletiRenkeri(mesaj.getMesajDurumu().getBeklemeRengi(),
				mesaj.getMesajDurumu().getIletildiRengi());

	}

//	void sayfayiMesajaKaydir(String mesajId) {
//
//		MesajBalonu mesajBalonu = mesajBalonlari.get(mesajId);
//
//		if (mesajBalonu == null)
//			return;
//
//		scrollPane.setVvalue(scrollPane.getVmax() * Math.min(1.0, mesajBalonu.getLayoutY()
//				/ (ortaPane.getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight())));
//
//	}

	void sayfayiSonaKaydir() {

		scrollPane.setVvalue(scrollPane.getVmax());

	}

	void setOnGeriAction(Runnable runnable) {

		geriBtn.setOnAction(e -> runnable.run());

	}

	void setOnMesajGonderAction(Consumer<String> consumer) {

		gonderBtn.setOnAction(e -> {

			final String mesaj = mesajArea.getText().trim();

			mesajArea.setText("");

			if (mesaj.isEmpty())
				return;

			consumer.accept(mesaj);

		});

	}

	private Label newTarihLabel(String tarih) {

		Label tarihLabel = new Label(tarih);

		tarihLabel.setPadding(new Insets(0.0, 5.0, 0.0, 5.0));
		tarihLabel
				.setBackground(new Background(new BackgroundFill(Color.DARKGRAY, new CornerRadii(5.0), Insets.EMPTY)));
		tarihLabel.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, new CornerRadii(5.0),
				BorderWidths.DEFAULT, Insets.EMPTY)));

		return tarihLabel;

	}

	private static class MesajBalonu extends GridPane {

		private static final double RADIUS = 3.0;
		private static final SimpleDateFormat SAAT_DAKIKA = new SimpleDateFormat("HH:mm");

		private final MesajTipi mesajTipi;

		private final GridPane mesajPane = new GridPane();
		private final Label mesajLbl;
		private final Label zamanLbl;
		private final Group bilgiGrp = new Group();
		private final Circle beklemeCircle = new Circle(RADIUS, Color.TRANSPARENT);
		private final Circle iletildiCircle = new Circle(RADIUS, Color.TRANSPARENT);

		private final Region bosluk = new Region();

		MesajBalonu(String mesaj, Date tarih, MesajTipi mesajTipi) {

			super();

			this.mesajTipi = mesajTipi;

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

			switch (mesajTipi) {

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

		public void setIletiRenkeri(Paint beklemeCircleColor, Paint iletildiCircleColor) {

			beklemeCircle.setFill(beklemeCircleColor);
			iletildiCircle.setFill(iletildiCircleColor);

		}

		private void initBilgiGrp() {

			iletildiCircle.setLayoutX(2 * RADIUS);
			bilgiGrp.getChildren().addAll(beklemeCircle, iletildiCircle);

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

	}

	private static enum MesajTipi {

		GELEN, GIDEN

	}

}

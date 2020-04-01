package com.aselsan.rehis.reform.mcsy.sunum;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.aselsan.rehis.reform.mcsy.sunum.fabrika.SunumFabrika;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
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

	private final HBox ustPane = new HBox(5);
	private final VBox ortaPane = new VBox(5);
	private final HBox altPane = new HBox(5);

	private final ScrollPane scrollPane = new ScrollPane(ortaPane);
	private final Button geriBtn = SunumFabrika.newGeriBtn();
	private final Circle durumCircle = new Circle(7.0);
	private final Label isimLabel = new Label();
	private final TextArea mesajArea = new TextArea();
	private final Button gonderBtn = SunumFabrika.newGonderBtn();

	private final Map<String, MesajBalonu> gelenMesajBalonlari = Collections
			.synchronizedMap(new HashMap<String, MesajBalonu>());
	private final Map<String, MesajBalonu> gidenMesajBalonlari = Collections
			.synchronizedMap(new HashMap<String, MesajBalonu>());

	MesajPane() {

		super();

		init();

	}

	private void init() {

		ustPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		altPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		ustPane.setPadding(new Insets(5));
		ortaPane.setPadding(new Insets(5));
		altPane.setPadding(new Insets(5));

		ustPane.setAlignment(Pos.CENTER_LEFT);

		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setFitToWidth(true);

		mesajArea.setPrefRowCount(1);
		mesajArea.setWrapText(true);

		HBox.setHgrow(mesajArea, Priority.ALWAYS);

		mesajArea.setOnKeyPressed(e -> {

			if (e.getCode().equals(KeyCode.ENTER)) {

				gonderBtn.fire();

				e.consume();

			}

		});

		HBox.setMargin(durumCircle, new Insets(5, 5, 5, 15));
		isimLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0));

		ustPane.getChildren().addAll(geriBtn, durumCircle, isimLabel);
		altPane.getChildren().addAll(mesajArea, gonderBtn);

		setTop(ustPane);
		setCenter(scrollPane);
		setBottom(altPane);

	}

	ObjectProperty<Paint> durumColorProperty() {

		return durumCircle.fillProperty();

	}

	StringProperty isimProperty() {

		return isimLabel.textProperty();

	}

	void gelenMesajGuncelle(String mesajId, Mesaj mesaj) {

		if (!gelenMesajBalonlari.containsKey(mesajId)) {

			MesajBalonu gelenMesajBalonu = new MesajBalonu(mesaj.getIcerik(), MesajTipi.GELEN);
			gelenMesajBalonlari.put(mesajId, gelenMesajBalonu);

			ortaPane.getChildren().add(gelenMesajBalonu);

		}

	}

	void gidenMesajGuncelle(String mesajId, Mesaj mesaj) {

		if (!gidenMesajBalonlari.containsKey(mesajId)) {

			MesajBalonu gidenMesajBalonu = new MesajBalonu(mesaj.getIcerik(), MesajTipi.GIDEN);
			gidenMesajBalonlari.put(mesajId, gidenMesajBalonu);

			ortaPane.getChildren().add(gidenMesajBalonu);

		}

	}

	void sayfayiSonaKaydir() {

		scrollPane.setVvalue(1.0);

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

	private class MesajBalonu extends HBox {

		private final MesajTipi mesajTipi;

		private final Label mesajLbl;
		private final Region bosluk = new Region();

		MesajBalonu(String mesaj, MesajTipi mesajTipi) {

			super();

			this.mesajTipi = mesajTipi;

			mesajLbl = new Label(mesaj);

			init();

		}

		private void init() {

			mesajLbl.setWrapText(true);
			mesajLbl.setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(10),
					new BorderWidths(1))));

			HBox.setHgrow(bosluk, Priority.ALWAYS);

			mesajLbl.setPadding(new Insets(5, 10, 5, 10));

			switch (mesajTipi) {

			case GELEN:

				mesajLbl.setBackground(
						new Background(new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0), Insets.EMPTY)));
				getChildren().addAll(mesajLbl, bosluk);

				break;

			case GIDEN:

				mesajLbl.setBackground(
						new Background(new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0), Insets.EMPTY)));
				getChildren().addAll(bosluk, mesajLbl);

				break;

			}

			mesajLbl.maxWidthProperty().bind(Bindings.createDoubleBinding(() -> getWidth() * 0.80, widthProperty()));
			prefHeightProperty().bind(mesajLbl.heightProperty());

		}

	}

	private enum MesajTipi {

		GELEN, GIDEN

	}

}

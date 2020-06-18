package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CreateGroupPane extends BorderPane {

	private static final double GAP = 5.0;

	private final HBox ustPane = new HBox(GAP);

	private final Button geriBtn = ViewFactory.newGeriBtn();
	private final TextField grupAdiTextField = new TextField();

	private final VBox scrollableContent = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(scrollableContent) {
		@Override
		public void requestFocus() {
		}
	};
	private final VBox eklenenKisilerPane = new VBox();
	private final VBox eklenmemisKisilerPane = new VBox();
	private final TextField kisiAramaTextField = new TextField();

	private final Button grupOlusturBtn = new Button();

	private final List<String> uuidler = Collections.synchronizedList(new ArrayList<String>());
	private final ObservableSet<String> seciliUuidler = FXCollections.observableSet(new HashSet<String>());

	private final Comparator<Node> kisiSiralayici = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof Labeled && arg1 instanceof Labeled))
				return 0;

			Labeled labeled0 = (Labeled) arg0;
			Labeled labeled1 = (Labeled) arg1;

			return labeled0.getText().compareTo(labeled1.getText());

		}

	};

	CreateGroupPane() {

		super();

		init();

	}

	void setOnGeriAction(final Runnable runnable) {

		geriBtn.setOnAction(e -> runnable.run());

	}

	void setOnGrupOlusturAction(final Runnable runnable) {

		grupOlusturBtn.setOnAction(e -> runnable.run());

	}

	void kisiGuncelle(Contact kisi) {

		final String uuid = kisi.getUuid();
		final String isim = kisi.getIsim();

		if (!uuidler.contains(uuid)) {

			uuidler.add(uuid);

			final Button kisiEkleBtn = ViewFactory.newEkleBtn();
			initButon(kisiEkleBtn);
			kisiEkleBtn.setText(isim);

			final Button kisiCikarBtn = ViewFactory.newCikarBtn();
			initButon(kisiCikarBtn);
			kisiCikarBtn.setText(isim);

			BooleanBinding kisiEklemeBinding = Bindings.createBooleanBinding(() -> seciliUuidler.contains(uuid),
					seciliUuidler);
			BooleanBinding kisiAramaBinding = Bindings.createBooleanBinding(() -> {
				String kisiAramaStr = kisiAramaTextField.getText().toLowerCase();
				return kisiAramaStr.isEmpty() || isim.toLowerCase().startsWith(kisiAramaStr);
			}, kisiAramaTextField.textProperty());

			kisiEkleBtn.visibleProperty().bind(kisiAramaBinding.and(kisiEklemeBinding.not()));
			kisiCikarBtn.visibleProperty().bind(kisiEklemeBinding);

			kisiEkleBtn.setOnAction(e -> {

				eklenenKisilerPane.getChildren().remove(kisiCikarBtn);
				eklenenKisilerPane.getChildren().add(0, kisiCikarBtn);

				seciliUuidler.add(uuid);

			});

			kisiCikarBtn.setOnAction(e -> seciliUuidler.remove(uuid));

			eklenenKisilerPane.getChildren().add(kisiCikarBtn);
			eklenmemisKisilerPane.getChildren().add(kisiEkleBtn);
			FXCollections.sort(eklenmemisKisilerPane.getChildren(), kisiSiralayici);

		}

	}

	String getGrupAdi() {

		return grupAdiTextField.getText().trim();

	}

	List<String> getSeciliUuidler() {

		return new ArrayList<String>(seciliUuidler);

	}

	void reset() {

		kisiAramaTextField.setText("");
		grupAdiTextField.setText("");
		seciliUuidler.clear();

	}

	private void initButon(Button btn) {

		btn.setMnemonicParsing(false);
		btn.setFont(Font.font(null, FontWeight.BOLD, 18.0));
		btn.setPadding(new Insets(5.0));
		btn.managedProperty().bind(btn.visibleProperty());

	}

	private void init() {

		initUstPane();
		initScrollableContent();
		initGrupOlusturBtn();

		scrollPane.setFitToWidth(true);

		setTop(ustPane);
		setCenter(scrollPane);
		setBottom(grupOlusturBtn);

	}

	private void initUstPane() {

		ustPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		ustPane.setPadding(new Insets(GAP));
		ustPane.setAlignment(Pos.CENTER_LEFT);

		initGrupAdiTextField();

		ustPane.getChildren().addAll(geriBtn, grupAdiTextField);

	}

	private void initGrupAdiTextField() {

		grupAdiTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

		grupAdiTextField.setPromptText(CommonMethods.cevir("GRUP_ADI_GIR"));
		grupAdiTextField.setFocusTraversable(false);
		grupAdiTextField.setBackground(Background.EMPTY);
		grupAdiTextField.setFont(Font.font(null, FontWeight.BOLD, 18.0));

	}

	private void initScrollableContent() {

		initEklenenKisilerPane();
		initEklenmemisKisilerPane();
		initKisiAramaTextField();

		BorderPane eklenmemisKisilerBorderPane = new BorderPane();
		eklenmemisKisilerBorderPane.setPadding(Insets.EMPTY);
		eklenmemisKisilerBorderPane.setTop(kisiAramaTextField);
		eklenmemisKisilerBorderPane.setCenter(eklenmemisKisilerPane);

		TitledPane eklenenKisilerTitledPane = new TitledPane(CommonMethods.cevir("EKLENEN_KISILER"),
				eklenenKisilerPane);
		TitledPane eklenmemisKisilerTitledPane = new TitledPane(CommonMethods.cevir("TUM_KISILER"),
				eklenmemisKisilerBorderPane);

		eklenenKisilerTitledPane.setCollapsible(false);
		eklenmemisKisilerTitledPane.setCollapsible(false);

		scrollableContent.getChildren().addAll(eklenenKisilerTitledPane, eklenmemisKisilerTitledPane);

	}

	private void initEklenenKisilerPane() {

		eklenenKisilerPane.setPadding(Insets.EMPTY);

	}

	private void initEklenmemisKisilerPane() {

		eklenmemisKisilerPane.setPadding(Insets.EMPTY);

	}

	private void initKisiAramaTextField() {

		kisiAramaTextField.setPromptText(CommonMethods.cevir("ARA"));
		kisiAramaTextField.setFocusTraversable(false);

	}

	private void initGrupOlusturBtn() {

		grupOlusturBtn.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
		grupOlusturBtn.setTextFill(Color.ANTIQUEWHITE);
		grupOlusturBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0));

		grupOlusturBtn.setMnemonicParsing(false);
		grupOlusturBtn.setText(CommonMethods.cevir("GRUP_OLUSTUR"));

		grupOlusturBtn.setMaxWidth(Double.MAX_VALUE);

		grupOlusturBtn.disableProperty()
				.bind(Bindings.size(seciliUuidler).isEqualTo(0).or(grupAdiTextField.textProperty().isEmpty()));

	}

}

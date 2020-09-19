package com.ogya.dms.view;

import java.util.function.Consumer;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RemoteIpSettingsPane extends BorderPane {

	private static final double GAP = 5.0;

	private final HBox topPane = new HBox(GAP);
	private final VBox centerPane = new VBox(GAP);

	private final Button backBtn = ViewFactory.newBackBtn();
	private final Label headingLabel = new Label(CommonMethods.translate("EDIT_REMOTE_IPS"));

	private final HBox addIpPane = new HBox(GAP);

	private final IpField ipField = new IpField();
	private final Button addIpButton = ViewFactory.newAddBtn();

	private final VBox scrollableContent = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(scrollableContent) {
		@Override
		public void requestFocus() {
		}
	};

	RemoteIpSettingsPane() {

		super();

		init();

	}

	private void init() {

		initTopPane();
		initCenterPane();

		scrollPane.setFitToWidth(true);

		setTop(topPane);
		setCenter(centerPane);

	}

	void setOnBackAction(Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void setOnAddIpAction(Consumer<String> consumer) {

		addIpButton.setOnAction(e -> consumer.accept(ipField.getIP()));

	}

	void clearIp() {

		ipField.clearIP();

	}

	private void initTopPane() {

		initHeadingLabel();

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(GAP));
		topPane.setAlignment(Pos.CENTER_LEFT);

		topPane.getChildren().addAll(backBtn, headingLabel);

	}

	private void initCenterPane() {

		initAddIpPane();
		initScrollableContent();

		centerPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		centerPane.getChildren().addAll(addIpPane, new Separator(), scrollableContent);

	}

	private void initHeadingLabel() {

		headingLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0));

	}

	private void initAddIpPane() {

		initIpField();
		initAddIpButton();

		addIpPane.setPadding(new Insets(GAP));

		addIpPane.getChildren().addAll(ipField, addIpButton);

	}

	private void initIpField() {

		ipField.setFont(Font.font(null, FontWeight.BOLD, 16.0));

	}

	private void initAddIpButton() {

		addIpButton.setMaxHeight(Double.MAX_VALUE);
		addIpButton.disableProperty().bind(ipField.validProperty().not());

	}

	private void initScrollableContent() {

	}

}

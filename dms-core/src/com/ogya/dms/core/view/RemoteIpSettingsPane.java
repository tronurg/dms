package com.ogya.dms.core.view;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.view.factory.ViewFactory;

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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RemoteIpSettingsPane extends BorderPane {

	private final double gap = ViewFactory.getGap();

	private final double viewFactor = ViewFactory.getViewFactor();

	private final HBox topPane = new HBox(gap);
	private final VBox centerPane = new VBox(gap);

	private final Button backBtn = ViewFactory.newBackBtn();
	private final Label headingLabel = new Label(CommonMethods.translate("EDIT_REMOTE_IPS"));

	private final HBox addIpPane = new HBox(gap);

	private final IpField ipField = new IpField();
	private final Button addIpButton = ViewFactory.newAddBtn();

	private final VBox scrollableContent = new VBox(gap);
	private final ScrollPane scrollPane = new ScrollPane(scrollableContent) {
		@Override
		public void requestFocus() {
		}
	};

	private final AtomicReference<Consumer<String>> removeIpActionRef = new AtomicReference<Consumer<String>>();

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

	void setOnRemoveIpAction(Consumer<String> consumer) {

		removeIpActionRef.set(consumer);

	}

	void clearIpField() {

		ipField.clearIP();

	}

	void clearAll() {

		clearIpField();

		scrollableContent.getChildren().clear();

	}

	void updateIps(final String[] ips) {

		scrollableContent.getChildren().clear();

		for (String ip : ips) {

			if (ip.isEmpty())
				continue;

			HBox ipField = new HBox(gap);

			Label ipLabel = new Label(ip);
			ipLabel.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));

			Button removeIpButton = ViewFactory.newRemoveBtn();

			removeIpButton.setOnAction(e -> {

				Consumer<String> removeIpAction = removeIpActionRef.get();

				if (removeIpAction != null)
					removeIpAction.accept(ip);

			});

			ipField.getChildren().addAll(removeIpButton, ipLabel);

			scrollableContent.getChildren().add(ipField);

		}

	}

	private void initTopPane() {

		initHeadingLabel();

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(gap));
		topPane.setAlignment(Pos.CENTER_LEFT);

		topPane.getChildren().addAll(backBtn, headingLabel);

	}

	private void initCenterPane() {

		initAddIpPane();
		initScrollPane();

		centerPane.getChildren().addAll(addIpPane, new Separator(), scrollPane);

	}

	private void initHeadingLabel() {

		headingLabel.getStyleClass().add("black-label");
		headingLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0 * viewFactor));

	}

	private void initAddIpPane() {

		initIpField();
		initAddIpButton();

		addIpPane.setPadding(new Insets(gap));

		addIpPane.getChildren().addAll(ipField, addIpButton);

	}

	private void initScrollPane() {

		initScrollableContent();

		scrollPane.getStyleClass().add("edge-to-edge");
		VBox.setVgrow(scrollPane, Priority.ALWAYS);

	}

	private void initScrollableContent() {

		scrollableContent.setPadding(new Insets(gap));

	}

	private void initIpField() {

		ipField.setFont(Font.font(null, FontWeight.BOLD, 16.0 * viewFactor));

	}

	private void initAddIpButton() {

		addIpButton.setMaxHeight(Double.MAX_VALUE);
		addIpButton.disableProperty().bind(ipField.validProperty().not());

	}

}

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
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SettingsPane extends BorderPane {

	private static final double GAP = ViewFactory.GAP;

	private final double viewFactor = ViewFactory.getViewFactor();

	private final HBox topPane = new HBox(GAP);

	private final Button backBtn = ViewFactory.newBackBtn();
	private final Label headingLabel = new Label(CommonMethods.translate("SETTINGS"));

	private final VBox scrollableContent = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(scrollableContent) {
		@Override
		public void requestFocus() {
		}
	};

	private final AtomicReference<Consumer<Settings>> settingClickedActionRef = new AtomicReference<Consumer<Settings>>();

	SettingsPane() {

		super();

		init();

	}

	private void init() {

		initTopPane();
		initScrollableContent();

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setFitToWidth(true);

		setTop(topPane);
		setCenter(scrollPane);

	}

	void setOnBackAction(Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void setOnSettingClickedAction(Consumer<Settings> consumer) {

		settingClickedActionRef.set(consumer);

	}

	private void initTopPane() {

		initHeadingLabel();

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(GAP));
		topPane.setAlignment(Pos.CENTER_LEFT);

		topPane.getChildren().addAll(backBtn, headingLabel);

	}

	private void initHeadingLabel() {

		headingLabel.getStyleClass().add("black-label");
		headingLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0 * viewFactor));

	}

	private void initScrollableContent() {

		// EDIT_REMOTE_IPS
		Label editRemoteIpsLabel = new Label(CommonMethods.translate("EDIT_REMOTE_IPS"));
		editRemoteIpsLabel.getStyleClass().add("link-label");
		VBox.setMargin(editRemoteIpsLabel, new Insets(3 * GAP));
		editRemoteIpsLabel.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));
		editRemoteIpsLabel.setOnMouseClicked(e -> {
			Consumer<Settings> settingsClickedAction = settingClickedActionRef.get();

			if (settingsClickedAction != null)
				settingsClickedAction.accept(Settings.EDIT_REMOTE_IPS);
		});

		scrollableContent.getChildren().add(editRemoteIpsLabel);

	}

}

enum Settings {

	EDIT_REMOTE_IPS

}

package com.ogya.dms.core.view;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
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
	private static final double VIEW_FACTOR = ViewFactory.VIEW_FACTOR;

	private final HBox topPane = new HBox(2 * GAP);

	private final Button backBtn;

	private final VBox scrollableContent = new VBox(GAP);
	private final ScrollPane scrollPane = new ScrollPane(scrollableContent) {
		@Override
		public void requestFocus() {
		}
	};

	private final AtomicReference<Consumer<Settings>> settingClickedActionRef = new AtomicReference<Consumer<Settings>>();

	SettingsPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty);

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

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(GAP));
		topPane.setAlignment(Pos.CENTER_LEFT);

		topPane.getChildren().addAll(backBtn);

	}

	private void initScrollableContent() {

		scrollableContent.setPadding(new Insets(3 * GAP));

		// STARRED_MESSAGES
		Label starredMessagesLbl = new Label(CommonMethods.translate("STARRED_MESSAGES"));
		starredMessagesLbl.getStyleClass().add("link-label");
		starredMessagesLbl.setFont(Font.font(null, FontWeight.BOLD, 18.0 * VIEW_FACTOR));
		starredMessagesLbl.setOnMouseClicked(e -> {
			Consumer<Settings> settingsClickedAction = settingClickedActionRef.get();
			if (settingsClickedAction != null)
				settingsClickedAction.accept(Settings.STARRED_MESSAGES);
		});

		scrollableContent.getChildren().add(starredMessagesLbl);

		// HIDDEN_CONVERSATIONS
		Label hiddenConversationsLbl = new Label(CommonMethods.translate("HIDDEN_CONVERSATIONS"));
		hiddenConversationsLbl.getStyleClass().add("link-label");
		hiddenConversationsLbl.setFont(Font.font(null, FontWeight.BOLD, 18.0 * VIEW_FACTOR));
		hiddenConversationsLbl.setOnMouseClicked(e -> {
			Consumer<Settings> settingsClickedAction = settingClickedActionRef.get();
			if (settingsClickedAction != null)
				settingsClickedAction.accept(Settings.HIDDEN_CONVERSATIONS);
		});

		scrollableContent.getChildren().add(hiddenConversationsLbl);

		// EDIT_REMOTE_IPS
		Label editRemoteIpsLbl = new Label(CommonMethods.translate("EDIT_REMOTE_IPS"));
		editRemoteIpsLbl.getStyleClass().add("link-label");
		editRemoteIpsLbl.setFont(Font.font(null, FontWeight.BOLD, 18.0 * VIEW_FACTOR));
		editRemoteIpsLbl.setOnMouseClicked(e -> {
			Consumer<Settings> settingsClickedAction = settingClickedActionRef.get();
			if (settingsClickedAction != null)
				settingsClickedAction.accept(Settings.EDIT_REMOTE_IPS);
		});

		scrollableContent.getChildren().add(editRemoteIpsLbl);

	}

}

enum Settings {

	STARRED_MESSAGES, HIDDEN_CONVERSATIONS, EDIT_REMOTE_IPS

}

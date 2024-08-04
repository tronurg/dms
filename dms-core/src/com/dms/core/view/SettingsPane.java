package com.dms.core.view;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.dms.core.util.Commons;
import com.dms.core.view.component.DmsScrollPane;
import com.dms.core.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsPane extends BorderPane {

	private final HBox topPane = new HBox();

	private final Button backBtn;

	private final VBox scrollableContent = new VBox();
	private final ScrollPane scrollPane = new DmsScrollPane(scrollableContent);

	private final AtomicReference<Consumer<Settings>> settingClickedActionRef = new AtomicReference<Consumer<Settings>>();

	SettingsPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);

		init();

	}

	private void init() {

		initTopPane();
		initScrollableContent();

		scrollPane.getStyleClass().addAll("edge-to-edge");
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

		topPane.getStyleClass().addAll("top-pane");
		topPane.getChildren().addAll(backBtn);

	}

	private void initScrollableContent() {

		scrollableContent.getStyleClass().addAll("spacing-1", "padding-3");

		// SEARCH_IN_ALL_MESSAGES
		Label searchInAllMessagesLbl = new Label(Commons.translate("SEARCH_IN_ALL_MESSAGES"));
		searchInAllMessagesLbl.getStyleClass().addAll("link-label", "em12", "bold");
		searchInAllMessagesLbl.setOnMouseClicked(e -> {
			Consumer<Settings> settingsClickedAction = settingClickedActionRef.get();
			if (settingsClickedAction != null) {
				settingsClickedAction.accept(Settings.SEARCH_IN_ALL_MESSAGES);
			}
		});

		scrollableContent.getChildren().add(searchInAllMessagesLbl);

		// STARRED_MESSAGES
		Label starredMessagesLbl = new Label(Commons.translate("STARRED_MESSAGES"));
		starredMessagesLbl.getStyleClass().addAll("link-label", "em12", "bold");
		starredMessagesLbl.setOnMouseClicked(e -> {
			Consumer<Settings> settingsClickedAction = settingClickedActionRef.get();
			if (settingsClickedAction != null) {
				settingsClickedAction.accept(Settings.STARRED_MESSAGES);
			}
		});

		scrollableContent.getChildren().add(starredMessagesLbl);

		// HIDDEN_CONVERSATIONS
		Label hiddenConversationsLbl = new Label(Commons.translate("HIDDEN_CONVERSATIONS"));
		hiddenConversationsLbl.getStyleClass().addAll("link-label", "em12", "bold");
		hiddenConversationsLbl.setOnMouseClicked(e -> {
			Consumer<Settings> settingsClickedAction = settingClickedActionRef.get();
			if (settingsClickedAction != null) {
				settingsClickedAction.accept(Settings.HIDDEN_CONVERSATIONS);
			}
		});

		scrollableContent.getChildren().add(hiddenConversationsLbl);

		// EDIT_REMOTE_IPS
		Label editRemoteIpsLbl = new Label(Commons.translate("EDIT_REMOTE_IPS"));
		editRemoteIpsLbl.getStyleClass().addAll("link-label", "em12", "bold");
		editRemoteIpsLbl.setOnMouseClicked(e -> {
			Consumer<Settings> settingsClickedAction = settingClickedActionRef.get();
			if (settingsClickedAction != null) {
				settingsClickedAction.accept(Settings.EDIT_REMOTE_IPS);
			}
		});

		scrollableContent.getChildren().add(editRemoteIpsLbl);

	}

}

enum Settings {

	SEARCH_IN_ALL_MESSAGES, STARRED_MESSAGES, HIDDEN_CONVERSATIONS, EDIT_REMOTE_IPS

}

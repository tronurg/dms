package com.ogya.dms.core.view;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.AttachmentType;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.component.DmsMediaPlayer;
import com.ogya.dms.core.view.component.DmsScrollPane;
import com.ogya.dms.core.view.component.DmsScrollPaneSkin;
import com.ogya.dms.core.view.component.ImSearchField;
import com.ogya.dms.core.view.component.ImSearchField.ImSearchListener;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

class SearchInAllMessagesPane extends BorderPane {

	private static final DateTimeFormatter HOUR_MIN = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

	private static final double GAP = ViewFactory.GAP;
	private static final double SMALL_GAP = 2.0 * GAP / 5.0;
	private static final double VIEW_FACTOR = ViewFactory.VIEW_FACTOR;
	private static final int MAX_SEARCH_HIT = Commons.UNITS_PER_PAGE;

	private final Border messagePaneBorder = new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
			new CornerRadii(10.0 * VIEW_FACTOR), BorderWidths.DEFAULT));
	private final Background incomingBackground = new Background(
			new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0 * VIEW_FACTOR), Insets.EMPTY));
	private final Background outgoingBackground = new Background(
			new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0 * VIEW_FACTOR), Insets.EMPTY));

	private final HBox topPane = new HBox();
	private final VBox centerPane = new VBox(GAP);

	private final Button backBtn;
	private final ImSearchField imSearchField = new ImSearchField();

	private final ScrollPane scrollPane = new DmsScrollPane(centerPane);

	private final AtomicReference<Runnable> backActionRef = new AtomicReference<Runnable>();

	private final ObservableMap<Long, MessageBalloon> messageBalloons = FXCollections.observableHashMap();

	private final List<ISearchInAllMessagesPane> listeners = Collections
			.synchronizedList(new ArrayList<ISearchInAllMessagesPane>());

	private final AtomicReference<String> searchTextRef = new AtomicReference<String>();

	SearchInAllMessagesPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);

		init();

	}

	private void init() {

		initTopPane();
		initCenterPane();

		setTop(topPane);
		setCenter(scrollPane);

	}

	private void initTopPane() {

		topPane.getStyleClass().addAll("top-pane");

		initBackBtn();
		initImSearchField();

		topPane.getChildren().addAll(backBtn, imSearchField);

	}

	private void initCenterPane() {

		centerPane.setAlignment(Pos.CENTER);
		centerPane.setPadding(new Insets(GAP));

		scrollPane.getStyleClass().addAll("edge-to-edge");
		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		scrollPane.setSkin(new DmsScrollPaneSkin(scrollPane));

	}

	private void initBackBtn() {

		backBtn.setOnAction(e -> {
			clearSearch();
			imSearchField.clear();
			Runnable backAction = backActionRef.get();
			if (backAction == null) {
				return;
			}
			backAction.run();
		});

	}

	private void initImSearchField() {

		HBox.setHgrow(imSearchField, Priority.ALWAYS);
		imSearchField.setMaxWidth(Double.MAX_VALUE);

		imSearchField.setNavigationDisabled(true);

		imSearchField.addImSearchListener(new ImSearchListener() {

			@Override
			public void searchRequested(String fulltext) {
				clearSearch();
				searchTextRef.set(fulltext);
				addNotification(Commons.translate("SEARCHING_DOTS"));
				listeners.forEach(listener -> listener.searchInAllMessagesRequested(fulltext));
			}

			@Override
			public void upRequested() {

			}

			@Override
			public void downRequested() {

			}

		});

	}

	void addListener(ISearchInAllMessagesPane listener) {

		listeners.add(listener);

	}

	void setOnBackAction(final Runnable runnable) {

		backActionRef.set(runnable);

	}

	private void addMessage(Message message) {

		Long messageId = message.getId();

		MessageBalloon messageBalloon = newMessageBalloon(message);

		messageBalloons.put(messageId, messageBalloon);

		centerPane.getChildren().add(messageBalloon);

	}

	void updateMessage(Message message) {

		Long messageId = message.getId();

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {
			return;
		}

		if (message.getViewStatus() == ViewStatus.DELETED) {
			messageBalloons.remove(messageId);
			centerPane.getChildren().remove(messageBalloon);
			return;
		}

		messageBalloon.messageInfo.statusProperty.set(message.getMessageStatus());

	}

	void focusOnSearchField() {

		imSearchField.requestFocus();

	}

	void showSearchResults(String fulltext, List<Message> hits) {

		if (fulltext != searchTextRef.get()) {
			return;
		}

		centerPane.getChildren().clear();

		if (hits.isEmpty()) {
			addNotification(Commons.translate("NOT_FOUND"));
		} else {
			int index = 0;
			for (Message hit : hits) {
				if (++index > MAX_SEARCH_HIT) {
					addNotification(Commons.translate("TOO_MANY_RESULTS_NOTIFICATION"));
					break;
				}
				addMessage(hit);
			}
		}

	}

	private void addNotification(String text) {
		Label noteLabel = ViewFactory.newNoteLabel(text);
		VBox.setMargin(noteLabel, new Insets(GAP, 0.0, GAP, 0.0));
		centerPane.getChildren().add(noteLabel);
	}

	private void clearSearch() {
		messageBalloons.clear();
		centerPane.getChildren().clear();
		searchTextRef.set(null);
	}

	private Node getReferenceBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		Node referenceBalloon = newReferenceBalloon(messageInfo);
		referenceBalloon.getStyleClass().addAll("reference-balloon");

		InnerShadow shadow = new InnerShadow(2 * GAP, Color.DARKGRAY);
		referenceBalloon.setEffect(shadow);

		return referenceBalloon;

	}

	private Node newReferenceBalloon(MessageInfo messageInfo) {

		VBox referenceBalloon = new VBox(SMALL_GAP);
		referenceBalloon.setPadding(new Insets(GAP));

		Label nameLabel = new Label(messageInfo.senderName);
		nameLabel.getStyleClass().addAll("em08", "bold");
		nameLabel.setTextFill(messageInfo.nameColor);

		referenceBalloon.getChildren().add(nameLabel);

		if (messageInfo.attachmentType != null) {

			if (messageInfo.attachmentType == AttachmentType.AUDIO) {

				DmsMediaPlayer dummyPlayer = new DmsMediaPlayer(null);

				VBox.setMargin(dummyPlayer, new Insets(0.0, 0.0, 0.0, GAP));

				referenceBalloon.getChildren().add(dummyPlayer);

			} else {

				Label attachmentLabel = ViewFactory.newAttachLbl(0.4);
				attachmentLabel.getStyleClass().addAll("em08");
				attachmentLabel.setText(messageInfo.attachmentName);

				VBox.setMargin(attachmentLabel, new Insets(0.0, 0.0, 0.0, GAP));

				referenceBalloon.getChildren().add(attachmentLabel);

			}

		}

		if (messageInfo.content != null) {

			Label contentLbl = new Label(messageInfo.content) {
				@Override
				public Orientation getContentBias() {
					return Orientation.HORIZONTAL;
				}

				@Override
				protected double computePrefHeight(double arg0) {
					return Math.min(super.computePrefHeight(arg0), getFont().getSize() * 5.0);
				}

			};
			contentLbl.getStyleClass().addAll("black-label", "em08");
			contentLbl.setWrapText(true);

			VBox.setMargin(contentLbl, new Insets(0.0, 0.0, 0.0, GAP));

			referenceBalloon.getChildren().add(contentLbl);

		}

		return referenceBalloon;

	}

	private MessageBalloon newMessageBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		MessageBalloon messageBalloon = new MessageBalloon(messageInfo);

		if (message.getRefMessage() != null) {
			messageBalloon.addReferenceBalloon(getReferenceBalloon(message.getRefMessage()));
		}

		return messageBalloon;

	}

	private class MessageBalloon extends GridPane {

		private final MessageInfo messageInfo;

		private final GridPane messagePane = new GridPane();
		private final HBox headerPane = new HBox(3 * GAP);
		private final Label nameLbl = new Label();
		private final Label dateLbl;
		private final Label timeLbl;
		private final Button goToRefBtn = ViewFactory.newGoToRefBtn();

		private MessageBalloon(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			dateLbl = new Label(DAY_MONTH_YEAR.format(messageInfo.localDateTime));
			timeLbl = new Label(HOUR_MIN.format(messageInfo.localDateTime));

			init();

		}

		private void init() {

			initMessagePane();
			initGoToRefBtn();

			add(messagePane, 0, 0);
			add(goToRefBtn, 1, 0);

		}

		void addReferenceBalloon(Node referenceBalloon) {

			GridPane.setMargin(referenceBalloon, new Insets(GAP, 0, GAP, 0));
			GridPane.setHgrow(referenceBalloon, Priority.ALWAYS);

			messagePane.add(referenceBalloon, 0, 1);

		}

		private void initMessagePane() {

			messagePane.getStyleClass().addAll("min-width-6em");
			GridPane.setHgrow(messagePane, Priority.ALWAYS);
			GridPane.setFillWidth(messagePane, false);
			messagePane.setBorder(messagePaneBorder);
			messagePane.setBackground(messageInfo.isOutgoing ? outgoingBackground : incomingBackground);
			messagePane.setPadding(new Insets(GAP));

			if (messageInfo.attachmentType != null) {
				messagePane.add(getAttachmentArea(), 0, 2);
			}

			if (messageInfo.content != null) {
				messagePane.add(getContentArea(), 0, 3);
			}

			initHeaderPane();
			initTimeLbl();

			messagePane.add(headerPane, 0, 0);
			messagePane.add(timeLbl, 0, 4);

		}

		private void initHeaderPane() {

			GridPane.setHgrow(headerPane, Priority.ALWAYS);

			headerPane.setAlignment(Pos.BASELINE_CENTER);

			initNameLbl();
			initDateLbl();

			headerPane.getChildren().addAll(nameLbl, dateLbl);

		}

		private void initTimeLbl() {

			timeLbl.getStyleClass().addAll("em08");
			GridPane.setHgrow(timeLbl, Priority.ALWAYS);
			timeLbl.setMaxWidth(Double.MAX_VALUE);
			timeLbl.setTextFill(Color.DIMGRAY);

		}

		private void initGoToRefBtn() {

			GridPane.setMargin(goToRefBtn, new Insets(0, 0, 0, GAP));

			goToRefBtn.setOnAction(e -> listeners
					.forEach(listener -> listener.goToMessageClicked(messageInfo.entityId, messageInfo.messageId)));

		}

		private Node getContentArea() {

			Label contentLbl = new Label(messageInfo.content);
			contentLbl.getStyleClass().addAll("black-label");
			contentLbl.setWrapText(true);

			return contentLbl;

		}

		private Node getAttachmentArea() {

			if (messageInfo.attachmentType == AttachmentType.AUDIO && messageInfo.attachmentPath != null) {
				return new DmsMediaPlayer(Paths.get(messageInfo.attachmentPath));
			}

			Label attachmentLabel = ViewFactory.newAttachLbl(0.5);
			attachmentLabel.setText(messageInfo.attachmentName);
			attachmentLabel.setTooltip(new Tooltip(attachmentLabel.getText()));

			attachmentLabel.disableProperty().bind(messageInfo.statusProperty.isEqualTo(MessageStatus.PREP));

			attachmentLabel.setCursor(Cursor.HAND);

			attachmentLabel.setOnMouseClicked(
					e -> listeners.forEach(listener -> listener.attachmentClicked(messageInfo.messageId)));

			final Effect colorAdjust = new ColorAdjust(-0.75, 1.0, 0.25, 0.0);

			attachmentLabel.effectProperty().bind(Bindings.createObjectBinding(
					() -> attachmentLabel.isHover() ? colorAdjust : null, attachmentLabel.hoverProperty()));

			return attachmentLabel;

		}

		private void initNameLbl() {

			nameLbl.getStyleClass().addAll("bold");
			HBox.setHgrow(nameLbl, Priority.ALWAYS);
			nameLbl.setMaxWidth(Double.MAX_VALUE);
			nameLbl.setText(messageInfo.senderName + " \u00BB " + messageInfo.receiverName);
			nameLbl.setTextFill(messageInfo.nameColor);

		}

		private void initDateLbl() {

			dateLbl.getStyleClass().addAll("em08");
			dateLbl.setTextFill(Color.DIMGRAY);

		}

	}

	private static final class MessageInfo {

		final EntityId entityId;
		final Long messageId;
		final String content;
		final String attachmentName;
		final String attachmentPath;
		final boolean isOutgoing;
		final String senderName;
		final String receiverName;
		final LocalDateTime localDateTime;
		final AttachmentType attachmentType;
		final Color nameColor;
		final ObjectProperty<MessageStatus> statusProperty = new SimpleObjectProperty<MessageStatus>();

		MessageInfo(Message message) {

			this.entityId = message.getEntity().getEntityId();
			this.messageId = message.getId();
			this.content = message.getContent();
			this.attachmentName = message.getAttachmentName();
			this.attachmentPath = message.getAttachmentPath();
			this.isOutgoing = message.isLocal();
			this.senderName = isOutgoing ? Commons.translate("YOU") : message.getOwner().getName();
			this.receiverName = message.getDgroup() == null
					? (isOutgoing ? message.getContact().getName() : Commons.translate("YOU"))
					: message.getDgroup().getName();
			this.localDateTime = LocalDateTime.ofInstant(message.getDate().toInstant(), ZoneId.systemDefault());
			this.attachmentType = message.getAttachmentType();
			this.nameColor = ViewFactory.getColorForUuid(message.getOwner().getUuid());
			this.statusProperty.set(message.getMessageStatus());

		}

	}

}

interface ISearchInAllMessagesPane {

	void searchInAllMessagesRequested(String fulltext);

	void attachmentClicked(Long messageId);

	void goToMessageClicked(EntityId entityId, Long messageId);

}

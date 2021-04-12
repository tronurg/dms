package com.ogya.dms.core.view;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.AttachmentType;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.factory.ViewFactory;
import com.sun.javafx.scene.control.skin.ScrollPaneSkin;

import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.geometry.Bounds;
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
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class StarredMessagesPane extends BorderPane {

	private static final DateTimeFormatter HOUR_MIN = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

	private final double gap = ViewFactory.getGap();
	private final double smallGap = 2.0 * gap / 5.0;

	private final double viewFactor = ViewFactory.getViewFactor();

	private final Border messagePaneBorder = new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
			new CornerRadii(10.0 * viewFactor), BorderWidths.DEFAULT));
	private final Background incomingBackground = new Background(
			new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0 * viewFactor), Insets.EMPTY));
	private final Background outgoingBackground = new Background(
			new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0 * viewFactor), Insets.EMPTY));

	private final HBox topPane = new HBox(2 * gap);
	private final VBox centerPaneWithLoadBtn = new VBox(gap);
	private final VBox centerPane = new VBox(gap);

	private final Button backBtn;
	private final Label titleLbl = new Label(CommonMethods.translate("STARRED_MESSAGES"));
	private final Button selectAllBtn = ViewFactory.newSelectionBtn();
	private final Button starBtn = ViewFactory.newStarBtn(1.0);

	private final Button loadBtn = new Button(CommonMethods.translate("SHOW_MORE"));

	private final ScrollPane scrollPane = new ScrollPane(centerPaneWithLoadBtn) {
		@Override
		public void requestFocus() {
		}
	};

	private final ObservableSet<MessageBalloon> selectedBalloons = FXCollections.observableSet();

	private final AtomicReference<Runnable> backActionRef = new AtomicReference<Runnable>();

	private final ObservableMap<Long, MessageBalloon> messageBalloons = FXCollections.observableHashMap();

	private final List<IStarredMessagesPane> listeners = Collections
			.synchronizedList(new ArrayList<IStarredMessagesPane>());

	private final AtomicLong minMessageId = new AtomicLong(Long.MAX_VALUE);

	private final BooleanProperty selectionModeProperty = new SimpleBooleanProperty(false);

	private final LongPressTimer longPressTimer = new LongPressTimer();

	private final Comparator<Node> messagesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof MessageBalloon && arg1 instanceof MessageBalloon))
				return 0;

			MessageBalloon group0 = (MessageBalloon) arg0;
			MessageBalloon group1 = (MessageBalloon) arg1;

			return group1.messageInfo.messageId.compareTo(group0.messageInfo.messageId);

		}

	};

	StarredMessagesPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty);

		init();

	}

	private void init() {

		initTopPane();
		initCenterPaneWithLoadBtn();

		setTop(topPane);
		setCenter(scrollPane);

	}

	private void initTopPane() {

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(gap));
		topPane.setAlignment(Pos.CENTER_LEFT);

		initBackBtn();
		initTitleLbl();
		initSelectAllBtn();
		initStarBtn();

		topPane.getChildren().addAll(backBtn, titleLbl, selectAllBtn, starBtn);

	}

	private void initCenterPaneWithLoadBtn() {

		centerPaneWithLoadBtn.setPadding(new Insets(gap));
		centerPaneWithLoadBtn.setAlignment(Pos.CENTER);

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		scrollPane.setSkin(new ScrollPaneSkin(scrollPane) {
			@Override
			public void onTraverse(Node arg0, Bounds arg1) {

			}
		});

		initLoadBtn();

		centerPaneWithLoadBtn.getChildren().addAll(centerPane, loadBtn);

	}

	private void initBackBtn() {

		backBtn.setOnAction(e -> {
			if (selectionModeProperty.get()) {
				messageBalloons.values().stream().filter(messageBalloon -> messageBalloon.selectedProperty.get())
						.forEach(messageBalloon -> messageBalloon.selectedProperty.set(false));
				selectionModeProperty.set(false);
			} else {
				Runnable backAction = backActionRef.get();
				if (backAction == null)
					return;
				backAction.run();
			}
		});

	}

	private void initTitleLbl() {

		HBox.setHgrow(titleLbl, Priority.ALWAYS);

		titleLbl.getStyleClass().add("black-label");
		titleLbl.setFont(Font.font(null, FontWeight.BOLD, 22.0 * viewFactor));
		titleLbl.setMaxWidth(Double.MAX_VALUE);

	}

	private void initSelectAllBtn() {

		selectAllBtn.visibleProperty().bind(selectionModeProperty);
		selectAllBtn.managedProperty().bind(selectAllBtn.visibleProperty());
		selectAllBtn.opacityProperty().bind(Bindings.createDoubleBinding(
				() -> selectedBalloons.size() < messageBalloons.size() ? 0.5 : 1.0, selectedBalloons, messageBalloons));
		selectAllBtn.setOnAction(e -> {
			boolean willSelect = selectedBalloons.size() < messageBalloons.size();
			messageBalloons.values().forEach(messageBalloon -> messageBalloon.selectedProperty.set(willSelect));
		});

	}

	private void initStarBtn() {

		starBtn.visibleProperty().bind(selectionModeProperty);
		starBtn.managedProperty().bind(starBtn.visibleProperty());
		starBtn.setOnAction(e -> {
			Long[] selectedIds = selectedBalloons.stream().map(balloon -> balloon.messageInfo.messageId)
					.toArray(Long[]::new);
			backBtn.fire();
			listeners.forEach(listener -> listener.archiveMessagesRequested(selectedIds));
		});
		starBtn.setEffect(new DropShadow());
		starBtn.disableProperty().bind(Bindings.isEmpty(selectedBalloons));

	}

	private void initLoadBtn() {

		loadBtn.getStyleClass().addAll("dim-label", "link-label");
		loadBtn.managedProperty().bind(loadBtn.visibleProperty());
		loadBtn.setOnAction(
				e -> listeners.forEach(listener -> listener.moreArchivedMessagesRequested(minMessageId.get())));

	}

	void addListener(IStarredMessagesPane listener) {

		listeners.add(listener);

	}

	void setOnBackAction(final Runnable runnable) {

		backActionRef.set(runnable);

	}

	void scrollToTop() {

		scrollPane.setVvalue(scrollPane.getVmin());

	}

	void addUpdateMessage(Message message) {

		Long messageId = message.getId();

		if (messageBalloons.containsKey(messageId)) {
			updateMessage(message);
			return;
		}

		if (!Objects.equals(message.getViewStatus(), ViewStatus.ARCHIVED))
			return;

		MessageBalloon messageBalloon = newMessageBalloon(message);

		messageBalloons.put(messageId, messageBalloon);

		minMessageId.set(Math.min(minMessageId.get(), messageId));

		centerPane.getChildren().add(messageBalloon);
		FXCollections.sort(centerPane.getChildren(), messagesSorter);

	}

	private void updateMessage(Message message) {

		if (!Objects.equals(message.getViewStatus(), ViewStatus.ARCHIVED))
			deleteMessage(message);

	}

	private void deleteMessage(Message message) {

		Long messageId = message.getId();

		MessageBalloon messageBalloon = messageBalloons.remove(messageId);

		if (messageBalloon != null)
			centerPane.getChildren().remove(messageBalloon);

	}

	void allMessagesLoaded() {

		loadBtn.setVisible(false);

	}

	private Node getReferenceBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		Node referenceBalloon = newReferenceBalloon(messageInfo);
		referenceBalloon.getStyleClass().add("reference-balloon");

		InnerShadow shadow = new InnerShadow(2 * gap, Color.DARKGRAY);
		referenceBalloon.setEffect(shadow);

		return referenceBalloon;

	}

	private Node newReferenceBalloon(MessageInfo messageInfo) {

		VBox referenceBalloon = new VBox(smallGap);
		referenceBalloon.setPadding(new Insets(gap));

		Label nameLabel = new Label(messageInfo.senderName);
		nameLabel.setFont(Font.font(null, FontWeight.BOLD, nameLabel.getFont().getSize() * 0.8));
		nameLabel.setTextFill(messageInfo.nameColor);

		referenceBalloon.getChildren().add(nameLabel);

		if (messageInfo.attachment != null) {

			if (Objects.equals(messageInfo.attachmentType, AttachmentType.AUDIO)) {

				DmsMediaPlayer dummyPlayer = new DmsMediaPlayer(null);

				VBox.setMargin(dummyPlayer, new Insets(0.0, 0.0, 0.0, gap));

				referenceBalloon.getChildren().add(dummyPlayer);

			} else {

				Label attachmentLabel = new Label(Paths.get(messageInfo.attachment).getFileName().toString(),
						ViewFactory.newAttachGraph(0.4));
				attachmentLabel.getStyleClass().add("dim-label");
				attachmentLabel.setFont(Font.font(attachmentLabel.getFont().getSize() * 0.8));

				VBox.setMargin(attachmentLabel, new Insets(0.0, 0.0, 0.0, gap));

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
			contentLbl.getStyleClass().add("black-label");
			contentLbl.setFont(Font.font(contentLbl.getFont().getSize() * 0.8));
			contentLbl.setWrapText(true);

			VBox.setMargin(contentLbl, new Insets(0.0, 0.0, 0.0, gap));

			referenceBalloon.getChildren().add(contentLbl);

		}

		return referenceBalloon;

	}

	private MessageBalloon newMessageBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		MessageBalloon messageBalloon = new MessageBalloon(messageInfo);

		if (message.getRefMessage() != null)
			messageBalloon.addReferenceBalloon(getReferenceBalloon(message.getRefMessage()));

		messageBalloon.addEventFilter(MouseEvent.ANY, e -> {

			if (!(Objects.equals(e.getButton(), MouseButton.NONE)
					|| Objects.equals(e.getButton(), MouseButton.PRIMARY))) {

				e.consume();

			} else if (selectionModeProperty.get()) {

				if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_PRESSED))
					messageBalloon.selectedProperty.set(!messageBalloon.selectedProperty.get());

				if (!Objects.equals(e.getEventType(), MouseEvent.MOUSE_EXITED_TARGET))
					e.consume();

			} else if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_PRESSED)) {

				longPressTimer.start(messageBalloon);

			} else if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_DRAGGED)) {

				longPressTimer.stop();

			} else if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_RELEASED)) {

				longPressTimer.stop();

				if (!e.isStillSincePress())
					e.consume();

			} else if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_CLICKED)) {

				if (!e.isStillSincePress())
					e.consume();

			}

		});

		return messageBalloon;

	}

	private class MessageBalloon extends GridPane {

		private final MessageInfo messageInfo;

		private final GridPane messagePane = new GridPane();
		private final HBox headerPane = new HBox(3 * gap);
		private final HBox statusPane = new HBox(gap);
		private final Label nameLbl = new Label();
		private final Label dateLbl;
		private final Label timeLbl;
		private final Node starGraph = ViewFactory.newStarGraph(0.65);
		private final Button selectionBtn = ViewFactory.newSelectionBtn();
		private final Button goToRefBtn = ViewFactory.newGoToRefBtn();

		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private MessageBalloon(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			dateLbl = new Label(DAY_MONTH_YEAR.format(messageInfo.localDateTime));
			timeLbl = new Label(HOUR_MIN.format(messageInfo.localDateTime));

			selectedProperty.addListener((e0, e1, e2) -> {
				if (e2)
					selectedBalloons.add(this);
				else
					selectedBalloons.remove(this);
			});

			init();

		}

		private void init() {

			initSelectionBtn();
			initMessagePane();
			initGoToRefBtn();

			add(selectionBtn, 0, 0);
			add(messagePane, 1, 0);
			add(goToRefBtn, 2, 0);

		}

		void addReferenceBalloon(Node referenceBalloon) {

			GridPane.setMargin(referenceBalloon, new Insets(gap, 0, gap, 0));
			GridPane.setHgrow(referenceBalloon, Priority.ALWAYS);

			messagePane.add(referenceBalloon, 0, 1);

		}

		private void initSelectionBtn() {

			GridPane.setMargin(selectionBtn, new Insets(0, gap, 0, 0));

			selectionBtn.visibleProperty().bind(selectionModeProperty);
			selectionBtn.managedProperty().bind(selectionBtn.visibleProperty());
			selectionBtn.opacityProperty()
					.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

		}

		private void initMessagePane() {

			GridPane.setHgrow(messagePane, Priority.ALWAYS);
			GridPane.setFillWidth(messagePane, false);

			messagePane.setStyle("-fx-min-width: 6em;");

			messagePane.setBorder(messagePaneBorder);
			messagePane.setBackground(messageInfo.isOutgoing ? outgoingBackground : incomingBackground);

			messagePane.setPadding(new Insets(gap));

			if (messageInfo.attachment != null)
				messagePane.add(getAttachmentArea(), 0, 2);

			if (messageInfo.content != null)
				messagePane.add(getContentArea(), 0, 3);

			initHeaderPane();
			initStatusPane();

			messagePane.add(headerPane, 0, 0);
			messagePane.add(statusPane, 0, 4);

		}

		private void initHeaderPane() {

			GridPane.setHgrow(headerPane, Priority.ALWAYS);

			headerPane.setAlignment(Pos.BASELINE_CENTER);

			initNameLbl();
			initDateLbl();

			headerPane.getChildren().addAll(nameLbl, dateLbl);

		}

		private void initStatusPane() {

			GridPane.setHgrow(statusPane, Priority.ALWAYS);

			statusPane.setAlignment(Pos.CENTER);

			initTimeLbl();
			initStarGraph();

			statusPane.getChildren().addAll(timeLbl, starGraph);

		}

		private void initGoToRefBtn() {

			GridPane.setMargin(goToRefBtn, new Insets(0, 0, 0, gap));

			goToRefBtn.visibleProperty().bind(selectionModeProperty.not());
			goToRefBtn.managedProperty().bind(goToRefBtn.visibleProperty());
			goToRefBtn.setOnAction(e -> listeners
					.forEach(listener -> listener.goToMessageClicked(messageInfo.entityId, messageInfo.messageId)));

		}

		private Node getContentArea() {

			Label contentLbl = new Label(messageInfo.content);

			contentLbl.getStyleClass().add("black-label");
			contentLbl.setWrapText(true);

			return contentLbl;

		}

		private Node getAttachmentArea() {

			if (Objects.equals(messageInfo.attachmentType, AttachmentType.AUDIO))
				return new DmsMediaPlayer(Paths.get(messageInfo.attachment));

			Label attachmentLabel = new Label(Paths.get(messageInfo.attachment).getFileName().toString(),
					ViewFactory.newAttachGraph(0.5));

			attachmentLabel.getStyleClass().add("dim-label");
			attachmentLabel.setTooltip(new Tooltip(attachmentLabel.getText()));

			attachmentLabel.cursorProperty().bind(Bindings.createObjectBinding(
					() -> selectionModeProperty.get() ? Cursor.DEFAULT : Cursor.HAND, selectionModeProperty));

			attachmentLabel.setOnMouseClicked(
					e -> listeners.forEach(listener -> listener.attachmentClicked(messageInfo.messageId)));

			final Effect colorAdjust = new ColorAdjust(-0.75, 1.0, 0.25, 0.0);

			attachmentLabel.effectProperty().bind(Bindings.createObjectBinding(
					() -> attachmentLabel.hoverProperty().and(selectionModeProperty.not()).get() ? colorAdjust : null,
					attachmentLabel.hoverProperty(), selectionModeProperty));

			return attachmentLabel;

		}

		private void initNameLbl() {

			HBox.setHgrow(nameLbl, Priority.ALWAYS);

			nameLbl.setText(messageInfo.senderName + " \u00BB " + messageInfo.receiverName);
			nameLbl.setFont(Font.font(null, FontWeight.BOLD, nameLbl.getFont().getSize()));
			nameLbl.setTextFill(messageInfo.nameColor);
			nameLbl.setMaxWidth(Double.MAX_VALUE);

		}

		private void initDateLbl() {

			dateLbl.setFont(Font.font(11.25 * viewFactor));
			dateLbl.setTextFill(Color.DIMGRAY);

		}

		private void initStarGraph() {

			starGraph.setEffect(new DropShadow());

		}

		private void initTimeLbl() {

			HBox.setHgrow(timeLbl, Priority.ALWAYS);

			timeLbl.setFont(Font.font(11.25 * viewFactor));
			timeLbl.setTextFill(Color.DIMGRAY);
			timeLbl.setMaxWidth(Double.MAX_VALUE);

		}

	}

	private class LongPressTimer extends AnimationTimer {

		private long startTime;
		private final AtomicReference<MessageBalloon> messageBalloonRef = new AtomicReference<MessageBalloon>();

		@Override
		public void handle(long arg0) {
			if (arg0 - startTime < 500e6)
				return;
			stop();
			selectionModeProperty.set(true);
			MessageBalloon messageBalloon = messageBalloonRef.getAndSet(null);
			if (messageBalloon != null)
				messageBalloon.selectedProperty.set(true);
		}

		public void start(MessageBalloon messageBalloon) {
			messageBalloonRef.set(messageBalloon);
			startTime = System.nanoTime();
			start();
		};

	}

	private static final class MessageInfo {

		final EntityId entityId;
		final Long messageId;
		final String content;
		final String attachment;
		final boolean isOutgoing;
		final String senderName;
		final String receiverName;
		final LocalDateTime localDateTime;
		final AttachmentType attachmentType;
		final Color nameColor;

		MessageInfo(Message message) {

			this.entityId = message.getEntity().getEntityId();
			this.messageId = message.getId();
			this.content = message.getContent();
			this.attachment = message.getAttachment();
			this.isOutgoing = message.isLocal();
			this.senderName = isOutgoing ? CommonMethods.translate("YOU") : message.getOwner().getName();
			this.receiverName = message.getDgroup() == null
					? (isOutgoing ? message.getContact().getName() : CommonMethods.translate("YOU"))
					: message.getDgroup().getName();
			this.localDateTime = LocalDateTime.ofInstant(message.getDate().toInstant(), ZoneId.systemDefault());
			this.attachmentType = message.getAttachmentType();
			this.nameColor = ViewFactory.getColorForUuid(message.getOwner().getUuid());

		}

	}

}

interface IStarredMessagesPane {

	void moreArchivedMessagesRequested(Long bottomMessageId);

	void attachmentClicked(Long messageId);

	void archiveMessagesRequested(Long[] messageIds);

	void goToMessageClicked(EntityId entityId, Long messageId);

}

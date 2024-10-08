package com.dms.core.view;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.dms.core.database.tables.EntityId;
import com.dms.core.database.tables.Message;
import com.dms.core.structures.AttachmentType;
import com.dms.core.structures.MessageStatus;
import com.dms.core.structures.ViewStatus;
import com.dms.core.util.Commons;
import com.dms.core.view.component.DmsBox;
import com.dms.core.view.component.DmsMediaPlayer;
import com.dms.core.view.component.DmsScrollPane;
import com.dms.core.view.component.DmsScrollPaneSkin;
import com.dms.core.view.component.ImSearchField;
import com.dms.core.view.component.ImSearchField.ImSearchListener;
import com.dms.core.view.factory.ViewFactory;

import javafx.animation.AnimationTimer;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

class StarredMessagesPane extends BorderPane {

	private static final DateTimeFormatter HOUR_MIN = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

	private final HBox topPane = new HBox();
	private final VBox centerPaneWithLoadBtn = new VBox();
	private final VBox centerPane = new VBox();

	private final Button backBtn;
	private final Label titleLbl = new Label(Commons.translate("STARRED_MESSAGES"));
	private final ImSearchField imSearchField = new ImSearchField();
	private final Button searchBtn = ViewFactory.newSearchBtn();
	private final Button selectAllBtn = ViewFactory.newSelectionBtn();
	private final Button starBtn = ViewFactory.newStarBtn();

	private final Button loadBtn = new Button(Commons.translate("SHOW_MORE"));

	private final ScrollPane scrollPane = new DmsScrollPane(centerPaneWithLoadBtn);

	private final ObservableSet<MessageBalloon> selectedBalloons = FXCollections.observableSet();

	private final ObservableList<Message> searchHits = FXCollections.observableArrayList();
	private final IntegerProperty searchHitIndex = new SimpleIntegerProperty(0);

	private final AtomicReference<Runnable> backActionRef = new AtomicReference<Runnable>();

	private final ObservableMap<Long, MessageBalloon> messageBalloons = FXCollections.observableHashMap();

	private final AtomicBoolean scrollNodeToBottom = new AtomicBoolean(false);

	private final List<IStarredMessagesPane> listeners = Collections
			.synchronizedList(new ArrayList<IStarredMessagesPane>());

	private final AtomicLong minMessageId = new AtomicLong(Long.MAX_VALUE);

	private final BooleanProperty selectionModeProperty = new SimpleBooleanProperty(false);
	private final BooleanProperty searchModeProperty = new SimpleBooleanProperty(false);

	private final AtomicReference<String> searchTextRef = new AtomicReference<String>();

	private final LongPressTimer longPressTimer = new LongPressTimer();

	private final Comparator<Node> messagesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof MessageBalloon && arg1 instanceof MessageBalloon)) {
				return 0;
			}

			MessageBalloon group0 = (MessageBalloon) arg0;
			MessageBalloon group1 = (MessageBalloon) arg1;

			return Long.compare(group1.messageInfo.messageId, group0.messageInfo.messageId);

		}

	};

	StarredMessagesPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);

		init();

	}

	private void init() {

		addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (!searchModeProperty.get()) {
				return;
			}
			if (e.getCode() == KeyCode.UP) {
				imSearchField.fireSearchUp();
				e.consume();
			} else if (e.getCode() == KeyCode.DOWN) {
				imSearchField.fireSearchDown();
				e.consume();
			}
		});

		initTopPane();
		initCenterPaneWithLoadBtn();

		setTop(topPane);
		setCenter(scrollPane);

	}

	private void initTopPane() {

		topPane.getStyleClass().addAll("top-pane");

		initBackBtn();
		initTitleLbl();
		initImSearchField();
		initSearchBtn();
		initSelectAllBtn();
		initStarBtn();

		topPane.getChildren().addAll(backBtn, titleLbl, imSearchField, searchBtn, selectAllBtn, starBtn);

	}

	private void initCenterPaneWithLoadBtn() {

		centerPane.getStyleClass().addAll("spacing-1");
		centerPaneWithLoadBtn.getStyleClass().addAll("spacing-1", "padding-1");
		centerPaneWithLoadBtn.setAlignment(Pos.CENTER);

		scrollPane.getStyleClass().addAll("edge-to-edge");
		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		scrollPane.setSkin(new DmsScrollPaneSkin(scrollPane));

		initLoadBtn();

		centerPaneWithLoadBtn.getChildren().addAll(centerPane, loadBtn);

	}

	private void initBackBtn() {

		backBtn.setOnAction(e -> {
			if (selectionModeProperty.get()) {
				messageBalloons.values().stream().filter(messageBalloon -> messageBalloon.selectedProperty.get())
						.forEach(messageBalloon -> messageBalloon.selectedProperty.set(false));
				selectionModeProperty.set(false);
			} else if (searchModeProperty.get()) {
				searchModeProperty.set(false);
				imSearchField.clear();
			} else {
				Runnable backAction = backActionRef.get();
				if (backAction == null) {
					return;
				}
				backAction.run();
			}
		});

	}

	private void initTitleLbl() {

		titleLbl.getStyleClass().addAll("black-label", "em15", "bold");
		HBox.setHgrow(titleLbl, Priority.ALWAYS);
		titleLbl.setMaxWidth(Double.MAX_VALUE);
		titleLbl.visibleProperty().bind(searchModeProperty.not().or(selectionModeProperty));
		titleLbl.managedProperty().bind(titleLbl.visibleProperty());

	}

	private void initImSearchField() {

		HBox.setHgrow(imSearchField, Priority.ALWAYS);
		imSearchField.setMaxWidth(Double.MAX_VALUE);
		imSearchField.visibleProperty().bind(searchModeProperty.and(selectionModeProperty.not()));
		imSearchField.managedProperty().bind(imSearchField.visibleProperty());

		imSearchField.upDisableProperty().bind(Bindings.isEmpty(searchHits).or(searchHitIndex.isEqualTo(0)));
		imSearchField.downDisableProperty()
				.bind(Bindings.isEmpty(searchHits).or(searchHitIndex.isEqualTo(Bindings.size(searchHits).subtract(1))));

		imSearchField.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
				clearSearch();
			}

		});

		imSearchField.addImSearchListener(new ImSearchListener() {

			@Override
			public void searchRequested(String fulltext) {
				clearSearch();
				searchTextRef.set(fulltext);
				listeners.forEach(listener -> listener.archiveSearchRequested(fulltext));
			}

			@Override
			public void upRequested() {
				int hitIndex = searchHitIndex.get();
				if (searchHits.isEmpty() || hitIndex == 0) {
					return;
				}
				scrollNodeToBottom.set(true);
				if (hitIndex < 0) {
					hitIndex = -(hitIndex + 1) - 1;
				} else {
					hitIndex -= 1;
				}
				searchHitIndex.set(Math.max(0, Math.min(searchHits.size() - 1, hitIndex)));
				goToMessage(searchHits.get(searchHitIndex.get()).getId());
			}

			@Override
			public void downRequested() {
				int hitIndex = searchHitIndex.get();
				if (searchHits.isEmpty() || hitIndex == searchHits.size() - 1) {
					return;
				}
				if (hitIndex < 0) {
					hitIndex = -(hitIndex + 1);
				} else {
					hitIndex += 1;
				}
				searchHitIndex.set(Math.max(0, Math.min(searchHits.size() - 1, hitIndex)));
				goToMessage(searchHits.get(searchHitIndex.get()).getId());
			}

		});

	}

	private void initSearchBtn() {

		searchBtn.visibleProperty()
				.bind(topPane.hoverProperty().and(searchModeProperty.or(selectionModeProperty).not()));
		searchBtn.managedProperty().bind(searchBtn.visibleProperty());
		searchBtn.opacityProperty()
				.bind(Bindings.createDoubleBinding(() -> searchBtn.isHover() || searchModeProperty.get() ? 1.0 : 0.5,
						searchBtn.hoverProperty(), searchModeProperty));
		searchBtn.setOnAction(e -> {
			searchModeProperty.set(true);
			imSearchField.requestFocus();
		});

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

		if (message.getViewStatus() != ViewStatus.ARCHIVED) {
			return;
		}

		MessageBalloon messageBalloon = newMessageBalloon(message);

		messageBalloons.put(messageId, messageBalloon);

		minMessageId.set(Math.min(minMessageId.get(), messageId));

		centerPane.getChildren().add(messageBalloon);
		FXCollections.sort(centerPane.getChildren(), messagesSorter);

	}

	private void updateMessage(Message message) {

		if (message.getViewStatus() != ViewStatus.ARCHIVED) {
			deleteMessage(message);
			removeSearchHit(message);
			return;
		}

		Long messageId = message.getId();

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {
			return;
		}

		messageBalloon.messageInfo.statusProperty.set(message.getMessageStatus());

	}

	private void deleteMessage(Message message) {

		Long messageId = message.getId();

		MessageBalloon messageBalloon = messageBalloons.remove(messageId);

		if (messageBalloon != null) {
			centerPane.getChildren().remove(messageBalloon);
		}

	}

	private void removeSearchHit(Message message) {

		for (int i = 0; i < searchHits.size(); ++i) {
			Long hitId = searchHits.get(i).getId();
			if (!Objects.equals(hitId, message.getId())) {
				continue;
			}
			searchHits.remove(i);
			if (searchHitIndex.get() == i) {
				searchHitIndex.set(-searchHitIndex.get() - 1);
			} else if (searchHitIndex.get() > i) {
				searchHitIndex.set(searchHitIndex.get() - 1);
			}
			break;
		}

	}

	void scrollPaneToMessage(Long messageId) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {
			return;
		}

		scrollPane(messageBalloon, centerPane.getSpacing());

		messageBalloon.blink();

	}

	void allMessagesLoaded() {

		loadBtn.setVisible(false);

	}

	private void goToMessage(Long messageId) {

		if (messageBalloons.containsKey(messageId)) {
			scrollPaneToMessage(messageId);
		} else {
			listeners.forEach(listener -> listener.archivedMessagesClaimed(minMessageId.get(), messageId));
		}

	}

	void showSearchResults(String fulltext, List<Message> hits) {

		if (!searchModeProperty.get() || fulltext != searchTextRef.get()) {
			return;
		}

		searchHits.addAll(hits);

		if (searchHits.isEmpty()) {
			imSearchField.setError(true);
		} else {
			goToMessage(searchHits.get(searchHitIndex.get()).getId());
		}

	}

	private void clearSearch() {
		imSearchField.setError(false);
		searchHits.clear();
		searchHitIndex.set(0);
		searchTextRef.set(null);
	}

	private Node newReferenceBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);
		Node referenceBalloon = newReferenceBalloon(messageInfo);
		referenceBalloon.getStyleClass().addAll("reference-balloon");

		return referenceBalloon;

	}

	private Node newReferenceBalloon(MessageInfo messageInfo) {

		VBox referenceBalloon = new VBox();
		referenceBalloon.getStyleClass().addAll("spacing-05", "padding-1");

		Label nameLabel = new Label(messageInfo.senderName);
		nameLabel.getStyleClass().addAll("em08", "bold");
		nameLabel.setTextFill(messageInfo.nameColor);

		referenceBalloon.getChildren().add(nameLabel);

		if (messageInfo.attachmentType != null) {

			if (messageInfo.attachmentType == AttachmentType.AUDIO) {

				DmsMediaPlayer dummyPlayer = new DmsMediaPlayer(null);

				referenceBalloon.getChildren().add(DmsBox.wrap(dummyPlayer, Pos.CENTER_LEFT, "padding-0001"));

			} else {

				Label innerLbl = new Label(messageInfo.attachmentName);
				innerLbl.getStyleClass().addAll("em08");
				Label attachmentLbl = ViewFactory.newAttachLbl(0.4, innerLbl);

				referenceBalloon.getChildren().add(DmsBox.wrap(attachmentLbl, Pos.CENTER_LEFT, "padding-0001"));

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

			referenceBalloon.getChildren().add(DmsBox.wrap(contentLbl, Pos.CENTER_LEFT, "padding-0001"));

		}

		return referenceBalloon;

	}

	private void scrollPane(Node nodeToScrollTo, double bias) {

		boolean scrollToBottom = scrollNodeToBottom.getAndSet(false);

		Parent parent = getParent();
		if (parent == null) {
			return;
		}

		parent.applyCss();
		parent.layout();

		Bounds nodeBoundsInScene = nodeToScrollTo.localToScene(nodeToScrollTo.getLayoutBounds());

		if (scrollPane.localToScene(scrollPane.getLayoutBounds()).contains(nodeBoundsInScene)) {
			return;
		}

		Double centerPaneWithLoadBtnHeight = centerPaneWithLoadBtn.getHeight();
		Double scrollPaneViewportHeight = scrollPane.getViewportBounds().getHeight();

		if (centerPaneWithLoadBtnHeight < scrollPaneViewportHeight) {
			return;
		}

		Double scrollY = centerPaneWithLoadBtn.sceneToLocal(nodeBoundsInScene).getMinY() - bias;
		if (scrollToBottom) {
			scrollY = scrollY - scrollPaneViewportHeight + nodeBoundsInScene.getHeight() + 2.0 * bias;
		}

		Double ratioY = Math.min(1.0, scrollY / (centerPaneWithLoadBtnHeight - scrollPaneViewportHeight));

		scrollPane.setVvalue(scrollPane.getVmax() * ratioY);

	}

	private MessageBalloon newMessageBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		MessageBalloon messageBalloon = new MessageBalloon(messageInfo);

		if (message.getRefMessage() != null) {
			messageBalloon.addReferenceBalloon(newReferenceBalloon(message.getRefMessage()));
		}

		final Long messageId = messageInfo.messageId;

		messageBalloon.hitProperty.bind(Bindings.createObjectBinding(() -> {
			int index = searchHitIndex.get();
			if (index < 0) {
				return false;
			}
			if (!(index < searchHits.size())) {
				return false;
			}
			if (!Objects.equals(messageId, searchHits.get(index).getId())) {
				return false;
			}
			return true;
		}, searchHits, searchHitIndex));

		messageBalloon.addEventFilter(MouseEvent.ANY, e -> {

			if (!(e.getButton() == MouseButton.NONE || e.getButton() == MouseButton.PRIMARY)) {

				e.consume();

			} else if (selectionModeProperty.get()) {

				if (MouseEvent.MOUSE_PRESSED.equals(e.getEventType())) {
					messageBalloon.selectedProperty.set(!messageBalloon.selectedProperty.get());
				}

				if (!MouseEvent.MOUSE_EXITED_TARGET.equals(e.getEventType())) {
					e.consume();
				}

			} else if (MouseEvent.MOUSE_PRESSED.equals(e.getEventType())) {

				longPressTimer.start(messageBalloon);

			} else if (MouseEvent.MOUSE_DRAGGED.equals(e.getEventType())) {

				longPressTimer.stop();

			} else if (MouseEvent.MOUSE_RELEASED.equals(e.getEventType())) {

				longPressTimer.stop();

				if (!e.isStillSincePress()) {
					e.consume();
				}

			} else if (MouseEvent.MOUSE_CLICKED.equals(e.getEventType())) {

				if (!e.isStillSincePress()) {
					e.consume();
				}

			}

		});

		return messageBalloon;

	}

	private class MessageBalloon extends GridPane {

		private final MessageInfo messageInfo;

		private final GridPane messagePane = new GridPane();
		private final HBox headerPane = new HBox();
		private final HBox statusPane = new HBox();
		private final Label nameLbl = new Label();
		private final Label dateLbl;
		private final Label timeLbl;
		private final Node starLbl = ViewFactory.newStarLbl();
		private final Button selectionBtn = ViewFactory.newSelectionBtn();
		private final Button goToRefBtn = ViewFactory.newGoToRefBtn();

		private final InnerShadow shadow = new InnerShadow(0d, Color.TRANSPARENT);

		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private final BooleanProperty hitProperty = new SimpleBooleanProperty(false);
		private final PseudoClass hit = PseudoClass.getPseudoClass("hit");

		private MessageBalloon(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			dateLbl = new Label(DAY_MONTH_YEAR.format(messageInfo.localDateTime));
			timeLbl = new Label(HOUR_MIN.format(messageInfo.localDateTime));

			selectedProperty.addListener((e0, e1, e2) -> {
				if (e2) {
					selectedBalloons.add(this);
				} else {
					selectedBalloons.remove(this);
				}
			});

			init();

		}

		private void init() {

			getStyleClass().addAll("message-balloon");
			hitProperty.addListener((e0, e1, e2) -> pseudoClassStateChanged(hit, Boolean.TRUE.equals(e2)));

			initSelectionBtn();
			initMessagePane();
			initGoToRefBtn();

			add(DmsBox.wrap(selectionBtn, Pos.CENTER_LEFT, "padding-0100"), 0, 0);
			add(messagePane, 1, 0);
			add(DmsBox.wrap(goToRefBtn, Pos.CENTER_RIGHT, "padding-0001"), 2, 0);

		}

		void addReferenceBalloon(Node referenceBalloon) {

			messagePane.add(DmsBox.wrap(referenceBalloon, Pos.CENTER_LEFT, "padding-1010"), 0, 1);

		}

		void blink() {

			if (!Color.TRANSPARENT.equals(shadow.getColor())) {
				return;
			}

			new Transition() {

				{
					setCycleDuration(Duration.millis(1000.0));
				}

				@Override
				protected void interpolate(double arg0) {
					shadow.setColor(Color.MEDIUMBLUE.interpolate(Color.TRANSPARENT, arg0));
				}

			}.play();

		}

		private void initSelectionBtn() {

			selectionBtn.visibleProperty().bind(selectionModeProperty);
			selectionBtn.managedProperty().bind(selectionBtn.visibleProperty());
			selectionBtn.opacityProperty()
					.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

		}

		private void initMessagePane() {

			messagePane.getStyleClass().addAll("min-width-6em", "message-border", "padding-1",
					messageInfo.isOutgoing ? "out-bg" : "in-bg");
			GridPane.setHgrow(messagePane, Priority.ALWAYS);
			GridPane.setFillWidth(messagePane, false);
			shadow.radiusProperty()
					.bind(Bindings.createDoubleBinding(() -> backBtn.getFont().getSize(), backBtn.fontProperty()));
			messagePane.setEffect(shadow);

			if (messageInfo.attachmentType != null) {
				messagePane.add(newAttachmentArea(), 0, 2);
			}

			if (messageInfo.content != null) {
				messagePane.add(newContentArea(), 0, 3);
			}

			initHeaderPane();
			initStatusPane();

			messagePane.add(headerPane, 0, 0);
			messagePane.add(statusPane, 0, 4);

		}

		private void initHeaderPane() {

			headerPane.getStyleClass().addAll("spacing-3");
			GridPane.setHgrow(headerPane, Priority.ALWAYS);
			headerPane.setAlignment(Pos.BASELINE_CENTER);

			initNameLbl();
			initDateLbl();

			headerPane.getChildren().addAll(nameLbl, dateLbl);

		}

		private void initStatusPane() {

			statusPane.getStyleClass().addAll("spacing-1");
			GridPane.setHgrow(statusPane, Priority.ALWAYS);
			statusPane.setAlignment(Pos.CENTER);

			initTimeLbl();
			initStarLbl();

			statusPane.getChildren().addAll(timeLbl, starLbl);

		}

		private void initGoToRefBtn() {

			goToRefBtn.visibleProperty().bind(selectionModeProperty.not());
			goToRefBtn.managedProperty().bind(goToRefBtn.visibleProperty());
			goToRefBtn.setOnAction(e -> listeners
					.forEach(listener -> listener.goToMessageClicked(messageInfo.entityId, messageInfo.messageId)));

		}

		private Node newContentArea() {

			Label contentLbl = new Label(messageInfo.content);
			contentLbl.getStyleClass().addAll("black-label");
			contentLbl.setWrapText(true);

			return contentLbl;

		}

		private Node newAttachmentArea() {

			if (messageInfo.attachmentType == AttachmentType.AUDIO && messageInfo.attachmentPath != null) {
				return new DmsMediaPlayer(Paths.get(messageInfo.attachmentPath));
			}

			Label innerLbl = new Label(messageInfo.attachmentName);
			innerLbl.setTooltip(new Tooltip(innerLbl.getText()));
			Label attachmentLbl = ViewFactory.newAttachLbl(0.5, innerLbl);

			attachmentLbl.disableProperty().bind(messageInfo.statusProperty.isEqualTo(MessageStatus.PREP));

			attachmentLbl.cursorProperty().bind(Bindings.createObjectBinding(
					() -> selectionModeProperty.get() ? Cursor.DEFAULT : Cursor.HAND, selectionModeProperty));

			attachmentLbl.setOnMouseClicked(
					e -> listeners.forEach(listener -> listener.attachmentClicked(messageInfo.messageId)));

			final Effect colorAdjust = new ColorAdjust(-0.75, 1.0, 0.25, 0.0);

			attachmentLbl.effectProperty().bind(Bindings.createObjectBinding(
					() -> attachmentLbl.hoverProperty().and(selectionModeProperty.not()).get() ? colorAdjust : null,
					attachmentLbl.hoverProperty(), selectionModeProperty));

			return attachmentLbl;

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

		private void initStarLbl() {

			starLbl.setEffect(new DropShadow());

		}

		private void initTimeLbl() {

			timeLbl.getStyleClass().addAll("em08");
			HBox.setHgrow(timeLbl, Priority.ALWAYS);
			timeLbl.setMaxWidth(Double.MAX_VALUE);
			timeLbl.setTextFill(Color.DIMGRAY);

		}

	}

	private class LongPressTimer extends AnimationTimer {

		private long startTime;
		private final AtomicReference<MessageBalloon> messageBalloonRef = new AtomicReference<MessageBalloon>();

		@Override
		public void handle(long arg0) {
			if (arg0 - startTime < 500e6) {
				return;
			}
			stop();
			selectionModeProperty.set(true);
			MessageBalloon messageBalloon = messageBalloonRef.getAndSet(null);
			if (messageBalloon != null) {
				messageBalloon.selectedProperty.set(true);
			}
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

interface IStarredMessagesPane {

	void moreArchivedMessagesRequested(Long bottomMessageId);

	void attachmentClicked(Long messageId);

	void archiveMessagesRequested(Long[] messageIds);

	void goToMessageClicked(EntityId entityId, Long messageId);

	void archiveSearchRequested(String fulltext);

	void archivedMessagesClaimed(Long lastMessageIdExcl, Long firstMessageIdIncl);

}

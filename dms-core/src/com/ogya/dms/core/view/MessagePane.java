package com.ogya.dms.core.view;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.AttachmentType;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.FileBuilder;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.component.DmsMediaPlayer;
import com.ogya.dms.core.view.component.DmsScrollPane;
import com.ogya.dms.core.view.component.DmsScrollPaneSkin;
import com.ogya.dms.core.view.component.ImPane;
import com.ogya.dms.core.view.component.ImPane.ImListener;
import com.ogya.dms.core.view.component.ImSearchField;
import com.ogya.dms.core.view.component.ImSearchField.ImSearchListener;
import com.ogya.dms.core.view.component.RecordButton;
import com.ogya.dms.core.view.component.RecordButton.RecordListener;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.animation.AnimationTimer;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.util.Duration;

class MessagePane extends BorderPane {

	private static final DateTimeFormatter HOUR_MIN = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

	private static final double GAP = ViewFactory.GAP;
	private static final double SMALL_GAP = 2.0 * GAP / 5.0;

	private final EntityId entityId;

	private final HBox topPane = new HBox();
	private final StackPane centerPane = new StackPane();
	private final GridPane bottomPane = new GridPane();

	private final Button backBtn;
	private final Circle statusCircle = new Circle(7.0);
	private final Group statusCircleGraph = new Group(statusCircle);
	private final Label nameLabel = new Label();
	private final HBox nameBox = new HBox(2 * GAP, statusCircleGraph, nameLabel);
	private final ImSearchField imSearchField = new ImSearchField();
	private final Button searchBtn = ViewFactory.newSearchBtn();
	private final Button clearBtn = ViewFactory.newDeleteBtn();
	private final Button selectAllBtn = ViewFactory.newSelectionBtn();
	private final Button forwardBtn = ViewFactory.newForwardBtn();
	private final Button starBtn = ViewFactory.newStarBtn();
	private final Button deleteBtn = ViewFactory.newDeleteBtn();

	private final VBox messagesPane = new VBox(2 * GAP);
	private final ScrollPane scrollPane = new DmsScrollPane(messagesPane);
	private final Button scrollToUnreadBtn = ViewFactory.newScrollToUnreadBtn();

	private final Popup deleteSelectedPopup = new Popup();
	private final Popup clearConversationPopup = new Popup();
	private final Button deleteSelectedBtn = new Button(Commons.translate("DELETE_SELECTED"));
	private final Button clearConversationBtn = new Button(Commons.translate("CLEAR_CONVERSATION"));

	private final HBox referencePane = new HBox();
	private final Button closeReferenceBtn = ViewFactory.newCancelBtn();
	private final HBox attachmentArea = new HBox(GAP);
	private final ImPane imPane = new ImPane();
	private final StackPane btnPane = new StackPane();

	private final Label attachmentLbl = ViewFactory.newAttachLbl(0.5);
	private final Button removeAttachmentBtn = ViewFactory.newRemoveBtn(0.65);

	private final Button sendBtn = ViewFactory.newSendBtn();
	private final RecordButton recordBtn = new RecordButton();

	private final ObservableSet<MessageBalloon> selectedBalloons = FXCollections.observableSet();

	private final ObservableList<Message> searchHits = FXCollections.observableArrayList();
	private final IntegerProperty searchHitIndex = new SimpleIntegerProperty(0);

	private final ReplyGroup replyGroup = new ReplyGroup(12.0);

	private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty editableProperty = new SimpleBooleanProperty(false);
	private final ObjectProperty<Long> referenceMessageProperty = new SimpleObjectProperty<Long>(null);
	private final ObjectProperty<FileBuilder> attachmentProperty = new SimpleObjectProperty<FileBuilder>(null);
	private final ObjectProperty<Long> firstUnreadMessageIdProperty = new SimpleObjectProperty<Long>(null);

	private final List<DayBox> dayBoxes = Collections.synchronizedList(new ArrayList<DayBox>());

	private final ObservableMap<Long, MessageBalloon> messageBalloons = FXCollections.observableHashMap();
	private final Set<Long> referencedMessageIds = Collections.synchronizedSet(new HashSet<Long>());

	private final AtomicBoolean autoScroll = new AtomicBoolean(true);
	private final AtomicBoolean scrollNodeToBottom = new AtomicBoolean(false);

	private final AtomicReference<SimpleEntry<Node, Double>> savedNodeY = new AtomicReference<SimpleEntry<Node, Double>>(
			null);

	private final DoubleProperty dragPosProperty = new SimpleDoubleProperty(0.0);

	private final List<IMessagePane> listeners = Collections.synchronizedList(new ArrayList<IMessagePane>());

	private final AtomicLong minMessageId = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong maxMessageId = new AtomicLong(Long.MIN_VALUE);

	private final BooleanProperty selectionModeProperty = new SimpleBooleanProperty(false);
	private final BooleanProperty searchModeProperty = new SimpleBooleanProperty(false);

	private final AtomicReference<String> searchTextRef = new AtomicReference<String>();

	private final LongPressTimer longPressTimer = new LongPressTimer();

	private final ChangeListener<Number> scrollListener = (e0, e1, e2) -> {
		if (e2.doubleValue() == 0.0 && e1.doubleValue() != 0.0) {
			listeners.forEach(listener -> listener.paneScrolledToTop(minMessageId.get()));
		}
	};

	MessagePane(EntityId entityId, BooleanProperty unreadProperty) {

		super();

		this.entityId = entityId;
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
		initCenterPane();
		initBottomPane();

		setTop(topPane);
		setCenter(centerPane);
		setBottom(bottomPane);

	}

	private void initTopPane() {

		topPane.getStyleClass().addAll("top-pane");

		initBackBtn();
		initNameBox();
		initImSearchField();
		initSearchBtn();
		initClearBtn();
		initSelectAllBtn();
		initForwardBtn();
		initStarBtn();
		initDeleteBtn();

		topPane.getChildren().addAll(backBtn, nameBox, imSearchField, searchBtn, clearBtn, selectAllBtn, forwardBtn,
				starBtn, deleteBtn);

	}

	private void initCenterPane() {

		initScrollPane();
		initScrollToUnreadBtn();

		centerPane.getChildren().addAll(scrollPane, scrollToUnreadBtn);

	}

	private void initBottomPane() {

		bottomPane.setPadding(new Insets(GAP));
		bottomPane.setHgap(GAP);
		bottomPane.setVgap(GAP);
		bottomPane.managedProperty().bind(activeProperty);

		initReferencePane();
		initCloseReferenceBtn();
		initAttachmentArea();
		initImPane();
		initBtnPane();

		bottomPane.add(referencePane, 0, 0);
		bottomPane.add(closeReferenceBtn, 0, 0);
		bottomPane.add(attachmentArea, 0, 1);
		bottomPane.add(imPane, 0, 2);
		bottomPane.add(btnPane, 1, 2);

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
				listeners.forEach(listener -> listener.hideMessagePaneClicked());
			}
		});

	}

	private void initNameBox() {

		nameBox.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(nameBox, Priority.ALWAYS);
		nameBox.visibleProperty().bind(searchModeProperty.not().or(selectionModeProperty));
		nameBox.managedProperty().bind(nameBox.visibleProperty());
		statusCircle.setStyle(ViewFactory.getScaleCss(1d, 1d));
		nameLabel.getStyleClass().addAll("black-label", "em15", "bold");
		nameLabel.underlineProperty().bind(Bindings.and(editableProperty, nameLabel.hoverProperty()));
		nameLabel.setOnMouseClicked(e -> {
			if (e.getButton() != MouseButton.PRIMARY) {
				return;
			}
			if (editableProperty.get()) {
				listeners.forEach(listener -> listener.showAddUpdateGroupClicked());
			}
		});

	}

	private void initImSearchField() {

		HBox.setHgrow(imSearchField, Priority.ALWAYS);
		imSearchField.setMaxWidth(Double.MAX_VALUE);
		imSearchField.visibleProperty().bind(searchModeProperty.and(selectionModeProperty.not()));
		imSearchField.managedProperty().bind(imSearchField.visibleProperty());

		imSearchField.upDisableProperty()
				.bind(Bindings.isEmpty(searchHits).or(searchHitIndex.isEqualTo(Bindings.size(searchHits).subtract(1))));
		imSearchField.downDisableProperty().bind(Bindings.isEmpty(searchHits).or(searchHitIndex.isEqualTo(0)));

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
				listeners.forEach(listener -> listener.searchRequested(fulltext));
			}

			@Override
			public void upRequested() {
				int hitIndex = searchHitIndex.get();
				if (searchHits.isEmpty() || hitIndex == searchHits.size() - 1) {
					return;
				}
				scrollNodeToBottom.set(true);
				if (hitIndex < 0) {
					hitIndex = -(hitIndex + 1);
				} else {
					hitIndex += 1;
				}
				searchHitIndex.set(Math.max(0, Math.min(searchHits.size() - 1, hitIndex)));
				goToMessage(searchHits.get(searchHitIndex.get()).getId());
			}

			@Override
			public void downRequested() {
				int hitIndex = searchHitIndex.get();
				if (searchHits.isEmpty() || hitIndex == 0) {
					return;
				}
				if (hitIndex < 0) {
					hitIndex = -(hitIndex + 1) - 1;
				} else {
					hitIndex -= 1;
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

	private void initClearBtn() {

		initClearConversationPopup();

		clearBtn.visibleProperty().bind(topPane.hoverProperty().or(clearConversationPopup.showingProperty())
				.and(searchModeProperty.or(selectionModeProperty).not()));
		clearBtn.managedProperty().bind(clearBtn.visibleProperty());
		clearBtn.opacityProperty()
				.bind(Bindings.createDoubleBinding(
						() -> clearBtn.isHover() || clearConversationPopup.isShowing() ? 1.0 : 0.5,
						clearBtn.hoverProperty(), clearConversationPopup.showingProperty()));
		clearBtn.setOnAction(e -> {
			Point2D point = clearBtn.localToScreen(clearBtn.getWidth(), clearBtn.getHeight() + GAP);
			clearConversationPopup.show(clearBtn, point.getX(), point.getY());
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

	private void initForwardBtn() {

		forwardBtn.visibleProperty().bind(selectionModeProperty);
		forwardBtn.managedProperty().bind(forwardBtn.visibleProperty());
		forwardBtn.disableProperty()
				.bind(Bindings.isEmpty(selectedBalloons).or(Bindings.size(selectedBalloons).greaterThan(3)));
		forwardBtn.setOnAction(e -> {
			Long[] selectedIds = selectedBalloons.stream().map(balloon -> balloon.messageInfo.messageId).sorted()
					.toArray(Long[]::new);
			backBtn.fire();
			listeners.forEach(listener -> listener.forwardMessagesRequested(selectedIds));
		});

	}

	private void initStarBtn() {

		starBtn.visibleProperty().bind(selectionModeProperty);
		starBtn.managedProperty().bind(starBtn.visibleProperty());
		starBtn.setOnAction(e -> {
			Long[] selectedIds = selectedBalloons.stream().map(balloon -> balloon.messageInfo.messageId).sorted()
					.toArray(Long[]::new);
			backBtn.fire();
			listeners.forEach(listener -> listener.archiveMessagesRequested(selectedIds));
		});
		final Effect dropShadow = new DropShadow();
		starBtn.effectProperty()
				.bind(Bindings.createObjectBinding(
						() -> !selectedBalloons.isEmpty() && selectedBalloons.stream()
								.allMatch(balloon -> balloon.messageInfo.archivedProperty.get()) ? dropShadow : null,
						selectedBalloons));
		starBtn.disableProperty().bind(Bindings.isEmpty(selectedBalloons));

	}

	private void initDeleteBtn() {

		initDeleteSelectedPopup();

		deleteBtn.visibleProperty().bind(selectionModeProperty);
		deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());
		deleteBtn.setOnAction(e -> {
			Point2D point = deleteBtn.localToScreen(deleteBtn.getWidth(), deleteBtn.getHeight() + GAP);
			deleteSelectedPopup.show(deleteBtn, point.getX(), point.getY());
		});
		deleteBtn.disableProperty()
				.bind(Bindings.createBooleanBinding(
						() -> selectedBalloons.stream().allMatch(balloon -> balloon.messageInfo.archivedProperty.get()),
						selectedBalloons));

	}

	private void initScrollPane() {

		messagesPane.setPadding(new Insets(GAP));
		messagesPane.heightProperty().addListener((e0, e1, e2) -> {
			if (!autoScroll.get()) {
				return;
			}
			scrollPaneToBottom();
		});

		scrollPane.getStyleClass().addAll("edge-to-edge");
		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		scrollPane.setSkin(new DmsScrollPaneSkin(scrollPane));
		scrollPane.setVvalue(scrollPane.getVmax());
		scrollPane.vvalueProperty().addListener((e0, e1, e2) -> {
			autoScroll.set(e1.doubleValue() == scrollPane.getVmax() || e2.doubleValue() == scrollPane.getVmax());
			if (scrollPane.isNeedsLayout()) {
				return;
			}
			MessageBalloon firstUnreadMessageBalloon = messageBalloons.get(firstUnreadMessageIdProperty.get());
			if (firstUnreadMessageBalloon == null) {
				return;
			}
			Bounds nodeBoundsInScene = firstUnreadMessageBalloon
					.localToScene(firstUnreadMessageBalloon.getLayoutBounds());
			if (nodeBoundsInScene.getMinY() < scrollPane.localToScene(scrollPane.getLayoutBounds()).getMaxY()) {
				firstUnreadMessageIdProperty.set(null);
			}
		});
		scrollPane.vvalueProperty().addListener(scrollListener);

	}

	private void initScrollToUnreadBtn() {

		StackPane.setAlignment(scrollToUnreadBtn, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(scrollToUnreadBtn, new Insets(GAP, 4 * GAP, GAP, 4 * GAP));

		scrollToUnreadBtn.visibleProperty().bind(firstUnreadMessageIdProperty.isNotNull());

		scrollToUnreadBtn.setOnAction(e -> {
			Long firstUnreadMessageId = firstUnreadMessageIdProperty.get();
			if (firstUnreadMessageId == null) {
				return;
			}
			scrollPaneToMessage(firstUnreadMessageId);
		});

	}

	private void initReferencePane() {

		referencePane.getStyleClass().addAll("reference-pane");
		referenceMessageProperty.addListener((e0, e1, e2) -> {
			referencePane.getChildren().clear();
			if (e2 == null) {
				return;
			}
			MessageInfo messageInfo = messageBalloons.get(e2).messageInfo;
			referencePane.getChildren().add(newReferenceBalloon(messageInfo));
		});

	}

	private void initCloseReferenceBtn() {

		GridPane.setMargin(closeReferenceBtn, new Insets(GAP));
		GridPane.setHalignment(closeReferenceBtn, HPos.RIGHT);
		GridPane.setValignment(closeReferenceBtn, VPos.TOP);
		closeReferenceBtn.setOnAction(e -> referenceMessageProperty.set(null));
		closeReferenceBtn.visibleProperty().bind(referenceMessageProperty.isNotNull());
		closeReferenceBtn.managedProperty().bind(closeReferenceBtn.visibleProperty());

	}

	private void initAttachmentArea() {

		attachmentArea.getStyleClass().addAll("attachment-area");
		attachmentArea.setAlignment(Pos.CENTER);
		attachmentArea.setPadding(new Insets(GAP));
		attachmentArea.visibleProperty().bind(Bindings.isNotNull(attachmentProperty));
		attachmentArea.managedProperty().bind(attachmentArea.visibleProperty());

		initAttachmentLbl();
		initRemoveAttachmentBtn();

		attachmentArea.getChildren().addAll(attachmentLbl, removeAttachmentBtn);

	}

	private void initImPane() {

		GridPane.setHgrow(imPane, Priority.ALWAYS);
		imPane.disableProperty().bind(recordBtn.pressedProperty());
		imPane.visibleProperty().bind(activeProperty);

		imPane.addImListener(new ImListener() {

			@Override
			public void sendFired() {
				sendBtn.fire();
			}

			@Override
			public void showFoldersClicked() {
				listeners.forEach(listener -> listener.showFoldersClicked());
			}

			@Override
			public void reportClicked() {
				listeners.forEach(listener -> listener.reportClicked());
			}

		});

	}

	private void initBtnPane() {

		GridPane.setFillHeight(btnPane, false);
		GridPane.setValignment(btnPane, VPos.BOTTOM);

		btnPane.visibleProperty().bind(activeProperty);

		initSendBtn();
		initRecordBtn();

		btnPane.getChildren().addAll(sendBtn, recordBtn);

	}

	private void initDeleteSelectedPopup() {

		deleteSelectedPopup.setAutoHide(true);
		deleteSelectedPopup.setAnchorLocation(AnchorLocation.WINDOW_TOP_RIGHT);

		initDeleteSelectedBtn();

		deleteSelectedPopup.getContent().add(deleteSelectedBtn);

	}

	private void initClearConversationPopup() {

		clearConversationPopup.setAutoHide(true);
		clearConversationPopup.setAnchorLocation(AnchorLocation.WINDOW_TOP_RIGHT);

		initClearConversationBtn();

		clearConversationPopup.getContent().add(clearConversationBtn);

	}

	private void initAttachmentLbl() {

		HBox.setHgrow(attachmentLbl, Priority.ALWAYS);
		attachmentLbl.setMaxWidth(Double.MAX_VALUE);
		attachmentLbl.textProperty()
				.bind(Bindings.createStringBinding(
						() -> attachmentProperty.get() == null ? null : attachmentProperty.get().getFileName(),
						attachmentProperty));

	}

	private void initRemoveAttachmentBtn() {

		removeAttachmentBtn.setMaxHeight(Double.MAX_VALUE);
		removeAttachmentBtn.setOnAction(e -> attachmentProperty.set(null));

	}

	private void initSendBtn() {

		sendBtn.visibleProperty().bind(recordBtn.visibleProperty().not());

		sendBtn.setOnAction(e -> {
			String messageAreaText = imPane.getMessage().trim();
			final String mesajTxt = messageAreaText.isEmpty() ? null : messageAreaText;
			imPane.setMessage("");
			final FileBuilder fileBuilder = attachmentProperty.get();
			attachmentProperty.set(null);
			if (mesajTxt == null && fileBuilder == null) {
				return;
			}
			final Long referenceMessageId = referenceMessageProperty.get();
			referenceMessageProperty.set(null);
			listeners.forEach(listener -> listener.sendMessageClicked(mesajTxt, fileBuilder, referenceMessageId));
		});

	}

	private void initRecordBtn() {

		recordBtn.visibleProperty().bind(imPane.messageProperty().isEmpty().and(attachmentProperty.isNull()));

		recordBtn.addRecordListener(new RecordListener() {

			@Override
			public void recordButtonPressed() {
				listeners.forEach(listener -> listener.recordButtonPressed());
			}

			@Override
			public void recordEventTriggered() {
				listeners.forEach(listener -> listener.recordEventTriggered(referenceMessageProperty.get()));
			}

			@Override
			public void recordButtonReleased() {
				listeners.forEach(listener -> listener.recordButtonReleased());
			}

		});

	}

	private void initDeleteSelectedBtn() {

		deleteSelectedBtn.getStyleClass().addAll("red-bg", "em12", "bold");
		deleteSelectedBtn.setTextFill(Color.ANTIQUEWHITE);
		deleteSelectedBtn.setMnemonicParsing(false);
		deleteSelectedBtn.setOnAction(e -> {
			deleteSelectedPopup.hide();
			Long[] selectedIds = selectedBalloons.stream().map(balloon -> balloon.messageInfo.messageId).sorted()
					.toArray(Long[]::new);
			backBtn.fire();
			listeners.forEach(listener -> listener.deleteMessagesRequested(selectedIds));
		});

	}

	private void initClearConversationBtn() {

		clearConversationBtn.getStyleClass().addAll("red-bg", "em12", "bold");
		clearConversationBtn.setTextFill(Color.ANTIQUEWHITE);
		clearConversationBtn.setMnemonicParsing(false);
		clearConversationBtn.setOnAction(e -> {
			clearConversationPopup.hide();
			listeners.forEach(listener -> listener.clearConversationRequested());
		});

	}

	void addListener(IMessagePane listener) {

		listeners.add(listener);

	}

	void updateEntity(EntityBase entity) {

		statusCircle.setFill(entity.getStatus().getStatusColor());
		nameLabel.setText(entity.getName());

		if (!entity.getEntityId().isGroup()) {
			return;
		}

		activeProperty.setValue(entity.getStatus() != Availability.OFFLINE);
		editableProperty.set(entity.getStatus() == Availability.AVAILABLE);

		if (activeProperty.get()) {
			return;
		}

		referenceMessageProperty.set(null);
		attachmentProperty.set(null);

	}

	EntityId getEntityId() {

		return entityId;

	}

	Long getMaxMessageId() {

		return maxMessageId.get();

	}

	void addUpdateMessage(Message message) {

		Long messageId = message.getId();

		if (messageBalloons.containsKey(messageId)) {
			updateMessage(message);
			return;
		}

		if (message.getViewStatus() == ViewStatus.DELETED) {
			return;
		}

		MessageBalloon messageBalloon = newMessageBalloon(message);

		messageBalloons.put(messageId, messageBalloon);

		MessageInfo messageInfo = messageBalloon.messageInfo;

		LocalDate messageDay = messageInfo.localDateTime.toLocalDate();

		minMessageId.set(Math.min(minMessageId.get(), messageId));
		maxMessageId.set(Math.max(maxMessageId.get(), messageId));

		if (maxMessageId.get() == messageId) {

			if (dayBoxes.isEmpty() || !Objects.equals(dayBoxes.get(dayBoxes.size() - 1).day, messageDay)) {

				DayBox dayBox = new DayBox(messageDay);
				dayBox.addMessageBalloonToBottom(messageBalloon);
				dayBoxes.add(dayBox);
				messagesPane.getChildren().add(dayBox);

			} else {

				dayBoxes.get(dayBoxes.size() - 1).addMessageBalloonToBottom(messageBalloon);

			}

			if (messageInfo.isOutgoing) {
				scrollPaneToBottom();
			} else if (getParent() != null && scrollPane.getVvalue() != scrollPane.getVmax()
					&& firstUnreadMessageIdProperty.get() == null) {
				firstUnreadMessageIdProperty.set(messageId);
			}

		} else if (minMessageId.get() == messageId) {

			if (dayBoxes.isEmpty() || !Objects.equals(dayBoxes.get(0).day, messageDay)) {

				DayBox dayBox = new DayBox(messageDay);
				dayBox.addMessageBalloonToTop(messageBalloon);
				dayBoxes.add(0, dayBox);
				messagesPane.getChildren().add(0, dayBox);

			} else {

				dayBoxes.get(0).addMessageBalloonToTop(messageBalloon);

			}

		}

	}

	private void updateMessage(Message message) {

		if (message.getViewStatus() == ViewStatus.DELETED) {
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
		messageBalloon.messageInfo.archivedProperty.set(message.getViewStatus() == ViewStatus.ARCHIVED);

	}

	private void deleteMessage(Message message) {

		Long messageId = message.getId();

		if (Objects.equals(referenceMessageProperty.get(), messageId)) {
			referenceMessageProperty.set(null);
		}
		if (Objects.equals(firstUnreadMessageIdProperty.get(), messageId)) {
			firstUnreadMessageIdProperty.set(null);
		}

		referencedMessageIds.remove(messageId);

		MessageBalloon messageBalloon = messageBalloons.remove(messageId);

		if (messageBalloon == null) {
			return;
		}

		DayBox dayBox = messageBalloon.dayBox;

		if (dayBox == null) {
			return;
		}

		dayBox.removeMessageBalloon(messageBalloon);

		if (!dayBox.isEmpty()) {
			return;
		}

		dayBoxes.remove(dayBox);
		messagesPane.getChildren().remove(dayBox);

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

	void addAttachment(FileBuilder fileBuilder) {

		attachmentProperty.set(fileBuilder);

	}

	void updateMessageProgress(Long messageId, int progress) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {
			return;
		}

		messageBalloon.messageInfo.progressProperty.set(progress);

	}

	void scrollPaneToMessage(Long messageId) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {

			scrollPaneToBottom();

			return;

		}

		scrollPane(messageBalloon, SMALL_GAP);

		messageBalloon.blink();

	}

	void savePosition(Long messageId) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {
			return;
		}

		Double yNode = scrollPane.sceneToLocal(messageBalloon.localToScene(0.0, 0.0)).getY();

		savedNodeY.set(new SimpleEntry<Node, Double>(messageBalloon, yNode));

	}

	void scrollToSavedPosition() {

		SimpleEntry<Node, Double> nodeY = savedNodeY.getAndSet(null);

		if (nodeY == null) {
			return;
		}

		scrollPane(nodeY.getKey(), nodeY.getValue());

	}

	void allMessagesLoaded() {

		scrollPane.vvalueProperty().removeListener(scrollListener);

	}

	void recordingStopped() {

		referenceMessageProperty.set(null);
		recordBtn.stopAnimation();

	}

	void goToMessage(Long messageId) {

		if (messageBalloons.containsKey(messageId)) {
			scrollPaneToMessage(messageId);
		} else {
			listeners.forEach(listener -> listener.messagesClaimed(minMessageId.get(), messageId));
		}

	}

	void focusOnMessageArea() {

		imPane.focusOnMessageArea();

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

	private Node getReferenceBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		Node referenceBalloon = newReferenceBalloon(messageInfo);
		referenceBalloon.getStyleClass().addAll("reference-balloon");

		final InnerShadow shadow = new InnerShadow(2 * GAP, Color.DARKGRAY);
		referenceBalloon.setEffect(shadow);

		final Long messageId = messageInfo.messageId;

		referenceBalloon.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			if (referencedMessageIds.contains(messageId)) {
				goToMessage(messageId);
				return;
			}
			if (Color.DARKGRAY.equals(shadow.getColor())) {
				new Transition() {

					{
						setCycleDuration(Duration.millis(500.0));
					}

					@Override
					protected void interpolate(double arg0) {
						shadow.setColor(Color.RED.interpolate(Color.DARKGRAY, arg0));
					}

				}.play();
			}
		});

		if (message.getViewStatus() != ViewStatus.DELETED) {
			referencedMessageIds.add(messageId);
		}

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

		Double messagesPaneHeight = messagesPane.getHeight();
		Double scrollPaneViewportHeight = scrollPane.getViewportBounds().getHeight();

		if (messagesPaneHeight < scrollPaneViewportHeight) {
			return;
		}

		Double scrollY = messagesPane.sceneToLocal(nodeBoundsInScene).getMinY() - bias;
		if (scrollToBottom) {
			scrollY = scrollY - scrollPaneViewportHeight + nodeBoundsInScene.getHeight() + 2.0 * bias;
		}

		Double ratioY = Math.min(1.0, scrollY / (messagesPaneHeight - scrollPaneViewportHeight));

		scrollPane.setVvalue(scrollPane.getVmax() * ratioY);

	}

	private void scrollPaneToBottom() {

		firstUnreadMessageIdProperty.set(null);
		scrollPane.setVvalue(scrollPane.getVmax());

	}

	private MessageBalloon newMessageBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		MessageBalloon messageBalloon = new MessageBalloon(messageInfo);

		if (message.getRefMessage() != null) {
			messageBalloon.addReferenceBalloon(getReferenceBalloon(message.getRefMessage()));
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

				dragPosProperty.set(e.getSceneX());

			} else if (MouseEvent.MOUSE_DRAGGED.equals(e.getEventType())) {

				longPressTimer.stop();

				if (replyGroup.getParent() != messageBalloon) {
					messageBalloon.addReplyGroup(replyGroup);
				}
				double diff = e.getSceneX() - dragPosProperty.get();
				dragPosProperty.set(e.getSceneX());
				if (!activeProperty.get()) {
					return;
				}
				double radius = Math.max(0.0,
						Math.min(replyGroup.getOuterRadius(), replyGroup.getInnerRadius() + diff / 4.0));
				double translate = 4.0 * radius;
				messageBalloon.setTranslateX(translate);
				replyGroup.setInnerRadius(radius);

			} else if (MouseEvent.MOUSE_RELEASED.equals(e.getEventType())) {

				longPressTimer.stop();

				messageBalloon.setTranslateX(0.0);
				if (replyGroup.getInnerRadius() == replyGroup.getOuterRadius()) {
					referenceMessageProperty.set(messageId);
				}
				replyGroup.setInnerRadius(0.0);

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
		private final HBox statusPane = new HBox(GAP);
		private final Label timeLbl;
		private final Node starGraph = ViewFactory.newStarLbl();
		private final Button selectionBtn = ViewFactory.newSelectionBtn();

		private final InnerShadow shadow = new InnerShadow(3 * GAP, Color.TRANSPARENT);

		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private final BooleanProperty hitProperty = new SimpleBooleanProperty(false);
		private final PseudoClass hit = PseudoClass.getPseudoClass("hit");

		private DayBox dayBox;
		private MessageGroup messageGroup;

		private MessageBalloon(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

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

			ColumnConstraints col0 = new ColumnConstraints();
			ColumnConstraints col1 = new ColumnConstraints();
			ColumnConstraints col2 = new ColumnConstraints();
			ColumnConstraints col3 = new ColumnConstraints();
			getColumnConstraints().addAll(col0, col1, col2, col3);
			col0.setPercentWidth(0.0);
			col2.setPercentWidth(80.0);

			initSelectionBtn();
			initMessagePane();

			add(selectionBtn, 1, 0);
			add(messagePane, 2, 0);

			if (messageInfo.infoAvailable) {
				add(getInfoBtn(), 3, 0);
			}

			if (messageInfo.isOutgoing) {
				col1.setHgrow(Priority.ALWAYS);
			} else {
				col3.setHgrow(Priority.ALWAYS);
			}

		}

		void addNameLbl(Label nameLbl) {
			messagePane.add(nameLbl, 0, 0);
		}

		void removeNameLbl(Label nameLbl) {
			messagePane.getChildren().remove(nameLbl);
		}

		void addReplyGroup(ReplyGroup replyGroup) {
			add(replyGroup, 0, 0, 1, 1);
		}

		void addReferenceBalloon(Node referenceBalloon) {

			GridPane.setMargin(referenceBalloon, new Insets(0, 0, GAP, 0));
			GridPane.setHgrow(referenceBalloon, Priority.ALWAYS);

			messagePane.add(referenceBalloon, 0, 1);

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

			GridPane.setMargin(selectionBtn, new Insets(0, GAP, 0, 0));

			selectionBtn.visibleProperty().bind(selectionModeProperty);
			selectionBtn.managedProperty().bind(selectionBtn.visibleProperty());
			selectionBtn.opacityProperty()
					.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

		}

		private void initMessagePane() {

			messagePane.getStyleClass().addAll("min-width-6em", "message-border");

			if (messageInfo.isOutgoing) {
				GridPane.setHalignment(messagePane, HPos.RIGHT);
				messagePane.getStyleClass().addAll("out-bg");
			} else {
				GridPane.setHalignment(messagePane, HPos.LEFT);
				messagePane.getStyleClass().addAll("in-bg");
			}

			GridPane.setFillWidth(messagePane, false);

			messagePane.setPadding(new Insets(GAP));
			messagePane.setEffect(shadow);

			if (messageInfo.attachmentType != null) {
				messagePane.add(getAttachmentArea(), 0, 2);
			}

			if (messageInfo.content != null) {
				messagePane.add(getContentArea(), 0, 3);
			}

			initStatusPane();

			messagePane.add(statusPane, 0, 4);

		}

		private Node getInfoBtn() {

			Button infoBtn = ViewFactory.newInfoBtn();
			GridPane.setMargin(infoBtn, new Insets(0, 0, 0, GAP));

			infoBtn.visibleProperty().bind(selectionModeProperty.not());
			infoBtn.managedProperty().bind(infoBtn.visibleProperty());
			infoBtn.setOnAction(e -> listeners.forEach(listener -> listener.infoClicked(messageInfo.messageId)));

			return infoBtn;

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

		private void initStatusPane() {

			GridPane.setHgrow(statusPane, Priority.ALWAYS);

			statusPane.setAlignment(Pos.CENTER);

			initStarGraph();
			initTimeLbl();

			if (messageInfo.isOutgoing) {
				statusPane.getChildren().addAll(starGraph, getSpace(), getProgressLbl(), getInfoGrp(), newFwdLbl(),
						timeLbl);
			} else {
				statusPane.getChildren().addAll(timeLbl, newFwdLbl(), getSpace(), starGraph);
			}

		}

		private void initStarGraph() {

			starGraph.setEffect(new DropShadow());
			starGraph.visibleProperty().bind(messageInfo.archivedProperty);

		}

		private Label newFwdLbl() {

			if (messageInfo.fwdCount == null) {
				return new Label();
			}

			Label fwdLbl = ViewFactory.newForwardLbl();

			if (messageInfo.fwdCount > 1) {
				Tooltip.install(fwdLbl,
						new Tooltip(String.format(Commons.translate("FORWARDED_N_TIMES"), messageInfo.fwdCount)));
			}

			return fwdLbl;

		}

		private void initTimeLbl() {

			timeLbl.getStyleClass().addAll("em08");
			timeLbl.setTextFill(Color.DIMGRAY);

		}

		private Node getProgressLbl() {

			Label progressLbl = new Label();
			progressLbl.getStyleClass().addAll("em08");
			progressLbl.setAlignment(Pos.BASELINE_RIGHT);
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.visibleProperty().bind(messageInfo.statusProperty.isEqualTo(MessageStatus.FRESH));
			progressLbl.managedProperty().bind(progressLbl.visibleProperty());
			progressLbl.textProperty().bind(Bindings.createStringBinding(() -> {
				int progress = messageInfo.progressProperty.get();
				return progress > 0 ? String.format("%d%%", progress) : "";
			}, messageInfo.progressProperty));

			return progressLbl;

		}

		private Node getInfoGrp() {

			Circle waitingCircle = new Circle(3.0, messageInfo.statusProperty.get().getWaitingColor());
			Circle transmittedCircle = new Circle(3.0, messageInfo.statusProperty.get().getTransmittedColor());

			transmittedCircle.setLayoutX(-2.0 * transmittedCircle.getRadius());

			Group group = new Group(waitingCircle, transmittedCircle);
			group.setStyle(ViewFactory.getScaleCss(1d, 1d));

			Group infoGrp = new Group(group);

			infoGrp.visibleProperty().bind(messageInfo.statusProperty.isNotEqualTo(MessageStatus.FRESH));
			infoGrp.managedProperty().bind(infoGrp.visibleProperty());
			waitingCircle.fillProperty().bind(Bindings.createObjectBinding(
					() -> messageInfo.statusProperty.get().getWaitingColor(), messageInfo.statusProperty));
			transmittedCircle.fillProperty().bind(Bindings.createObjectBinding(
					() -> messageInfo.statusProperty.get().getTransmittedColor(), messageInfo.statusProperty));

			return infoGrp;

		}

		private Node getSpace() {

			Region space = new Region();
			HBox.setHgrow(space, Priority.ALWAYS);

			return space;

		}

	}

	private class MessageGroup extends VBox {

		private final Long ownerId;

		private final Label nameLbl;

		private final AtomicReference<MessageBalloon> namedBalloonRef = new AtomicReference<MessageBalloon>();

		private DayBox dayBox;

		private MessageGroup(MessageInfo messageInfo) {

			super(SMALL_GAP);

			this.ownerId = messageInfo.ownerId;

			if (entityId.isGroup() && !messageInfo.isOutgoing) {
				nameLbl = new Label(messageInfo.senderName);
				initNameLbl(messageInfo.nameColor);
			} else {
				nameLbl = null;
			}

		}

		private void initNameLbl(Color nameColor) {

			nameLbl.getStyleClass().addAll("bold");
			nameLbl.setPadding(new Insets(0.0, 0.0, GAP, 0.0));
			nameLbl.setTextFill(nameColor);

			getChildren().addListener(new ListChangeListener<Node>() {
				@Override
				public void onChanged(Change<? extends Node> arg0) {
					ObservableList<? extends Node> children = arg0.getList();
					MessageBalloon namedBalloon = namedBalloonRef.get();
					if (children.isEmpty() || children.get(0) == namedBalloon) {
						return;
					}
					if (namedBalloon != null) {
						namedBalloon.removeNameLbl(nameLbl);
					}
					Node firstChild = children.get(0);
					if (!(firstChild instanceof MessageBalloon)) {
						return;
					}
					MessageBalloon firstBalloon = (MessageBalloon) firstChild;
					firstBalloon.addNameLbl(nameLbl);
					namedBalloonRef.set(firstBalloon);
				}
			});

		}

		private void addMessageBalloonToTop(MessageBalloon messageBalloon) {

			getChildren().add(0, messageBalloon);

			messageBalloon.messageGroup = this;

		}

		private void addMessageBalloonToBottom(MessageBalloon messageBalloon) {

			getChildren().add(messageBalloon);

			messageBalloon.messageGroup = this;

		}

		private void removeMessageBalloon(MessageBalloon messageBalloon) {

			if (messageBalloon.messageGroup != this) {
				return;
			}

			getChildren().remove(messageBalloon);

			messageBalloon.messageGroup = null;

		}

		private boolean isEmpty() {

			return getChildren().isEmpty();

		}

	}

	private class DayBox extends BorderPane {

		private final LocalDate day;

		private final Label dateLabel;
		private final VBox messageGroupBox = new VBox(GAP);

		private final List<MessageGroup> messageGroups = Collections.synchronizedList(new ArrayList<MessageGroup>());

		private DayBox(LocalDate day) {
			super();
			this.day = day;
			dateLabel = ViewFactory.newNoteLbl(DAY_MONTH_YEAR.format(day));
			init();
		}

		private void init() {

			BorderPane.setAlignment(dateLabel, Pos.CENTER);
			BorderPane.setMargin(dateLabel, new Insets(0.0, 0.0, GAP, 0.0));

			setTop(dateLabel);
			setCenter(messageGroupBox);

		}

		private void addMessageBalloonToTop(MessageBalloon messageBalloon) {

			if (messageGroups.isEmpty()
					|| !Objects.equals(messageGroups.get(0).ownerId, messageBalloon.messageInfo.ownerId)) {
				MessageGroup messageGroup = new MessageGroup(messageBalloon.messageInfo);
				messageGroup.addMessageBalloonToTop(messageBalloon);
				messageGroups.add(0, messageGroup);
				messageGroupBox.getChildren().add(0, messageGroup);
				messageGroup.dayBox = this;
			} else {
				messageGroups.get(0).addMessageBalloonToTop(messageBalloon);
			}

			messageBalloon.dayBox = this;

		}

		private void addMessageBalloonToBottom(MessageBalloon messageBalloon) {

			if (messageGroups.isEmpty() || !Objects.equals(messageGroups.get(messageGroups.size() - 1).ownerId,
					messageBalloon.messageInfo.ownerId)) {
				MessageGroup messageGroup = new MessageGroup(messageBalloon.messageInfo);
				messageGroup.addMessageBalloonToBottom(messageBalloon);
				messageGroups.add(messageGroup);
				messageGroupBox.getChildren().add(messageGroup);
				messageGroup.dayBox = this;
			} else {
				messageGroups.get(messageGroups.size() - 1).addMessageBalloonToBottom(messageBalloon);
			}

			messageBalloon.dayBox = this;

		}

		private void removeMessageBalloon(MessageBalloon messageBalloon) {

			MessageGroup messageGroup = messageBalloon.messageGroup;

			if (messageGroup == null || messageGroup.dayBox != this) {
				return;
			}

			messageGroup.removeMessageBalloon(messageBalloon);

			if (!messageGroup.isEmpty()) {
				return;
			}

			messageGroups.remove(messageGroup);
			messageGroupBox.getChildren().remove(messageGroup);

		}

		private boolean isEmpty() {

			return messageGroupBox.getChildren().isEmpty();

		}

	}

	private class ReplyGroup extends Group {

		private final double radius;

		private final Circle outer = new Circle();
		private final Circle inner = new Circle();

		private ReplyGroup(double radius) {
			super();
			this.radius = radius;
			init();
		}

		private void init() {
			outer.setStyle(ViewFactory.getScaleCss(1d, 1d));
			inner.setStyle(ViewFactory.getScaleCss(1d, 1d));
			outer.setRadius(radius);
			outer.setFill(Color.LIGHTSKYBLUE);
			inner.setFill(Color.DEEPSKYBLUE);
			getChildren().addAll(outer, inner);
			translateXProperty().bind(outer.radiusProperty().multiply(outer.scaleXProperty()).multiply(-2.8));
		}

		private double getInnerRadius() {
			return inner.getRadius() * inner.getScaleX();
		}

		private void setInnerRadius(double radius) {
			inner.setRadius(radius / inner.getScaleX());
		}

		private double getOuterRadius() {
			return outer.getRadius() * outer.getScaleX();
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

	private final class MessageInfo {

		final Long messageId;
		final String content;
		final String attachmentName;
		final String attachmentPath;
		final Long ownerId;
		final boolean isOutgoing;
		final String senderName;
		final LocalDateTime localDateTime;
		final AttachmentType attachmentType;
		final boolean infoAvailable;
		final Color nameColor;
		final Integer fwdCount;
		final ObjectProperty<MessageStatus> statusProperty = new SimpleObjectProperty<MessageStatus>();
		final IntegerProperty progressProperty = new SimpleIntegerProperty(-1);
		final BooleanProperty archivedProperty = new SimpleBooleanProperty();

		MessageInfo(Message message) {

			this.messageId = message.getId();
			this.content = message.getContent();
			this.attachmentName = message.getAttachmentName();
			this.attachmentPath = message.getAttachmentPath();
			this.ownerId = message.getOwner().getId();
			this.isOutgoing = message.isLocal();
			this.senderName = isOutgoing ? Commons.translate("YOU") : message.getOwner().getName();
			this.localDateTime = LocalDateTime.ofInstant(message.getDate().toInstant(), ZoneId.systemDefault());
			this.attachmentType = message.getAttachmentType();
			this.infoAvailable = entityId.isGroup() && isOutgoing;
			this.nameColor = ViewFactory.getColorForUuid(message.getOwner().getUuid());
			fwdCount = message.getForwardCount();
			this.statusProperty.set(message.getMessageStatus());
			this.archivedProperty.set(message.getViewStatus() == ViewStatus.ARCHIVED);

			this.statusProperty.addListener((e0, e1, e2) -> {
				if (e2 == MessageStatus.FRESH) {
					this.progressProperty.set(-1);
				}
			});

		}

	}

}

interface IMessagePane {

	void hideMessagePaneClicked();

	void showAddUpdateGroupClicked();

	void paneScrolledToTop(Long topMessageId);

	void messagesClaimed(Long lastMessageIdExcl, Long firstMessageIdIncl);

	void sendMessageClicked(String message, FileBuilder fileBuilder, Long refMessageId);

	void showFoldersClicked();

	void reportClicked();

	void attachmentClicked(Long messageId);

	void infoClicked(Long messageId);

	void forwardMessagesRequested(Long[] messageIds);

	void archiveMessagesRequested(Long[] messageIds);

	void deleteMessagesRequested(Long[] messageIds);

	void clearConversationRequested();

	void recordButtonPressed();

	void recordEventTriggered(Long refMessageId);

	void recordButtonReleased();

	void searchRequested(String fulltext);

}

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

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.AttachmentType;
import com.ogya.dms.core.structures.FileBuilder;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.RecordButton.RecordListener;
import com.ogya.dms.core.view.factory.ViewFactory;
import com.sun.javafx.scene.control.skin.ScrollPaneSkin;

import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
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
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

class MessagePane extends BorderPane {

	private static final DateTimeFormatter HOUR_MIN = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

	private final double gap = ViewFactory.getGap();
	private final double smallGap = 2.0 * gap / 5.0;

	private final double viewFactor = ViewFactory.getViewFactor();

	private final EntityId entityId;

	private final Border messagePaneBorder = new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
			new CornerRadii(10.0 * viewFactor), BorderWidths.DEFAULT));
	private final Border dateBorder = new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
			new CornerRadii(gap), BorderWidths.DEFAULT, Insets.EMPTY));
	private final Background incomingBackground = new Background(
			new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0 * viewFactor), Insets.EMPTY));
	private final Background outgoingBackground = new Background(
			new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0 * viewFactor), Insets.EMPTY));
	private final Background dateBackground = new Background(
			new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(gap), Insets.EMPTY));

	private final HBox topPane = new HBox(2 * gap);
	private final VBox centerPane = new VBox(2 * gap);
	private final GridPane bottomPane = new GridPane();

	private final Button backBtn;
	private final Circle statusCircle = new Circle(7.0 * viewFactor);
	private final Label nameLabel = new Label();
	private final Button selectAllBtn = ViewFactory.newSelectionBtn();
	private final Button starBtn = ViewFactory.newStarBtn(1.0);
	private final Button deleteBtn = ViewFactory.newDeleteBtn();

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};

	private final HBox referencePane = new HBox();
	private final Button closeReferenceBtn = ViewFactory.newCancelBtn();
	private final HBox attachmentArea = new HBox(gap);
	private final TextArea messageArea = new TextArea();
	private final StackPane btnPane = new StackPane();
	private final Button deleteSelectedBtn = new Button(CommonMethods.translate("DELETE_SELECTED"));

	private final Label attachmentLbl = new Label();
	private final Button removeAttachmentBtn = ViewFactory.newRemoveBtn(0.65);

	private final Button reportBtn = ViewFactory.newReportBtn();
	private final Button showFoldersBtn = ViewFactory.newAttachBtn();
	private final Button sendBtn = ViewFactory.newSendBtn();
	private final RecordButton recordBtn = new RecordButton();

	private final Transition btnAnimation = new Transition() {

		private final Interpolator interpolator = Interpolator.EASE_BOTH;

		{
			setCycleDuration(Duration.millis(100.0));
		}

		private double showFoldersBtnStart;
		private double showFoldersBtnEnd;
		private double reportBtnStart;
		private double reportBtnEnd;
		private int position = 0;

		@Override
		protected void interpolate(double arg0) {
			showFoldersBtn.setTranslateY(interpolator.interpolate(showFoldersBtnStart, showFoldersBtnEnd, arg0));
			reportBtn.setTranslateY(interpolator.interpolate(reportBtnStart, reportBtnEnd, arg0));
		}

		@Override
		public void play() {
			showFoldersBtnStart = -position * (gap + showFoldersBtn.getHeight());
			reportBtnStart = -position * (2 * gap + reportBtn.getHeight() + showFoldersBtn.getHeight());
			position = (position + 1) % 2;
			showFoldersBtnEnd = -position * (gap + showFoldersBtn.getHeight());
			reportBtnEnd = -position * (2 * gap + reportBtn.getHeight() + showFoldersBtn.getHeight());
			super.play();
		}

	};

	private final BooleanProperty deleteModeProperty = new SimpleBooleanProperty(false);
	private final ObservableSet<MessageBalloon> selectedBalloons = FXCollections.observableSet();

	private final ReplyGroup replyGroup = new ReplyGroup(12.0 * viewFactor);

	private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty editableProperty = new SimpleBooleanProperty(false);
	private final ObjectProperty<Long> referenceMessageProperty = new SimpleObjectProperty<Long>(null);
	private final ObjectProperty<FileBuilder> attachmentProperty = new SimpleObjectProperty<FileBuilder>(null);

	private final List<DayBox> dayBoxes = Collections.synchronizedList(new ArrayList<DayBox>());

	private final ObservableMap<Long, MessageBalloon> messageBalloons = FXCollections.observableHashMap();
	private final Set<Long> referencedMessageIds = Collections.synchronizedSet(new HashSet<Long>());

	private final AtomicBoolean autoScroll = new AtomicBoolean(true);

	private final AtomicReference<SimpleEntry<Node, Double>> savedNodeY = new AtomicReference<SimpleEntry<Node, Double>>(
			null);

	private final DoubleProperty dragPosProperty = new SimpleDoubleProperty(0.0);

	private final List<IMessagePane> listeners = Collections.synchronizedList(new ArrayList<IMessagePane>());

	private final AtomicLong minMessageId = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong maxMessageId = new AtomicLong(Long.MIN_VALUE);
	private final AtomicLong lastEventTime = new AtomicLong();

	private final AtomicLong futureReference = new AtomicLong(-1L);

	private final BooleanProperty selectionModeProperty = new SimpleBooleanProperty(false);

	private final LongPressTimer longPressTimer = new LongPressTimer();

	private final ChangeListener<Number> scrollListener = (e0, e1, e2) -> {
		if (e2.doubleValue() == 0.0 && e1.doubleValue() != 0.0)
			listeners.forEach(listener -> listener.paneScrolledToTop(minMessageId.get()));
	};

	MessagePane(EntityId entityId, BooleanProperty unreadProperty) {

		super();

		this.entityId = entityId;
		this.backBtn = ViewFactory.newBackBtn(unreadProperty);

		lastEventTime.set(System.currentTimeMillis());

		init();

	}

	private void init() {

		initTopPane();
		initCenterPane();
		initBottomPane();

		setTop(topPane);
		setCenter(scrollPane);
		setBottom(bottomPane);

	}

	private void initTopPane() {

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(gap));
		topPane.setAlignment(Pos.CENTER_LEFT);

		initBackBtn();
		initNameLbl();
		initSelectAllBtn();
		initStarBtn();
		initDeleteBtn();

		topPane.getChildren().addAll(backBtn, statusCircle, nameLabel, selectAllBtn, starBtn, deleteBtn);

	}

	private void initCenterPane() {

		centerPane.setPadding(new Insets(gap));
		centerPane.heightProperty().addListener((e0, e1, e2) -> {
			if (autoScroll.get())
				scrollPaneToBottom();
		});

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		scrollPane.setSkin(new ScrollPaneSkin(scrollPane) {
			@Override
			public void onTraverse(Node arg0, Bounds arg1) {

			}
		});
		scrollPane.vvalueProperty()
				.addListener((e0, e1, e2) -> autoScroll.set(e1.doubleValue() == scrollPane.getVmax()));
		scrollPane.vvalueProperty().addListener(scrollListener);

	}

	private void initBottomPane() {

		bottomPane.setPadding(new Insets(gap));
		bottomPane.setHgap(gap);
		bottomPane.setVgap(gap);
		bottomPane.managedProperty().bind(bottomPane.visibleProperty());
		bottomPane.visibleProperty().bind(activeProperty);

		initReferencePane();
		initCloseReferenceBtn();
		initAttachmentArea();
		initMessageArea();
		initBtnPane();
		initDeleteSelectedBtn();

		bottomPane.add(referencePane, 0, 0);
		bottomPane.add(closeReferenceBtn, 0, 0);
		bottomPane.add(attachmentArea, 0, 1);
		bottomPane.add(messageArea, 0, 2);
		bottomPane.add(btnPane, 1, 2);
		bottomPane.add(deleteSelectedBtn, 0, 2, 2, 1);

	}

	private void initBackBtn() {

		backBtn.setOnAction(e -> {
			if (selectionModeProperty.get()) {
				messageBalloons.values().stream().filter(messageBalloon -> messageBalloon.selectedProperty.get())
						.forEach(messageBalloon -> messageBalloon.selectedProperty.set(false));
				deleteModeProperty.set(false);
				selectionModeProperty.set(false);
			} else {
				listeners.forEach(listener -> listener.hideMessagePaneClicked());
			}
		});

	}

	private void initNameLbl() {

		HBox.setHgrow(nameLabel, Priority.ALWAYS);
		nameLabel.getStyleClass().add("black-label");
		nameLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0 * viewFactor));
		nameLabel.underlineProperty().bind(Bindings.and(editableProperty, nameLabel.hoverProperty()));
		nameLabel.setMaxWidth(Double.MAX_VALUE);
		nameLabel.setOnMouseClicked(e -> {
			if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
				return;
			if (editableProperty.get())
				listeners.forEach(listener -> listener.showAddUpdateGroupClicked());
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
		final Effect starShadow = new DropShadow();
		starBtn.effectProperty()
				.bind(Bindings.createObjectBinding(
						() -> !selectedBalloons.isEmpty() && selectedBalloons.stream()
								.allMatch(balloon -> balloon.messageInfo.archivedProperty.get()) ? starShadow : null,
						selectedBalloons));
		starBtn.disableProperty().bind(Bindings.isEmpty(selectedBalloons));

	}

	private void initDeleteBtn() {

		deleteBtn.visibleProperty().bind(selectionModeProperty);
		deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());
		deleteBtn.setOnAction(e -> deleteModeProperty.set(!deleteModeProperty.get()));
		final Effect deleteShadow = new DropShadow();
		deleteBtn.effectProperty().bind(
				Bindings.createObjectBinding(() -> deleteModeProperty.get() ? deleteShadow : null, deleteModeProperty));
		deleteBtn.disableProperty()
				.bind(Bindings.createBooleanBinding(
						() -> selectedBalloons.stream().allMatch(balloon -> balloon.messageInfo.archivedProperty.get()),
						selectedBalloons));

	}

	private void initReferencePane() {

		referencePane.getStyleClass().add("reference-panel");
		referenceMessageProperty.addListener((e0, e1, e2) -> {
			referencePane.getChildren().clear();
			if (e2 == null)
				return;
			MessageInfo messageInfo = messageBalloons.get(e2).messageInfo;
			referencePane.getChildren().add(newReferenceBalloon(messageInfo));
		});

	}

	private void initCloseReferenceBtn() {

		GridPane.setMargin(closeReferenceBtn, new Insets(gap));
		GridPane.setHalignment(closeReferenceBtn, HPos.RIGHT);
		GridPane.setValignment(closeReferenceBtn, VPos.TOP);
		closeReferenceBtn.setOnAction(e -> referenceMessageProperty.set(null));
		closeReferenceBtn.visibleProperty().bind(referenceMessageProperty.isNotNull());
		closeReferenceBtn.managedProperty().bind(closeReferenceBtn.visibleProperty());

	}

	private void initAttachmentArea() {

		attachmentArea.getStyleClass().add("attachment-area");
		attachmentArea.setAlignment(Pos.CENTER);
		attachmentArea.setPadding(new Insets(gap));
		attachmentArea.visibleProperty().bind(Bindings.isNotNull(attachmentProperty));
		attachmentArea.managedProperty().bind(attachmentArea.visibleProperty());

		initAttachmentLbl();
		initRemoveAttachmentBtn();

		attachmentArea.getChildren().addAll(attachmentLbl, removeAttachmentBtn);

	}

	private void initMessageArea() {

		GridPane.setHgrow(messageArea, Priority.ALWAYS);
		messageArea.setPrefRowCount(1);
		messageArea.setWrapText(true);
		messageArea.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 400 ? null : change));
		messageArea.disableProperty().bind(recordBtn.pressedProperty());
		messageArea.setOnKeyPressed(e -> {
			if (Objects.equals(e.getCode(), KeyCode.ENTER)) {
				if (e.isShiftDown()) {
					messageArea.appendText(System.lineSeparator());
				} else {
					sendBtn.fire();
					e.consume();
				}
			}
		});

	}

	private void initBtnPane() {

		initReportBtn();
		initShowFoldersBtn();
		initSendBtn();
		initRecordBtn();

		btnPane.getChildren().addAll(reportBtn, showFoldersBtn, sendBtn, recordBtn);

	}

	private void initDeleteSelectedBtn() {

		deleteSelectedBtn.setStyle("-fx-background-color: red;");
		deleteSelectedBtn.setTextFill(Color.ANTIQUEWHITE);
		deleteSelectedBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));
		deleteSelectedBtn.setMnemonicParsing(false);
		deleteSelectedBtn.setMaxWidth(Double.MAX_VALUE);
		deleteSelectedBtn.setMaxHeight(Double.MAX_VALUE);
		deleteSelectedBtn.visibleProperty().bind(deleteModeProperty);
		deleteSelectedBtn.managedProperty().bind(deleteSelectedBtn.visibleProperty());
		deleteSelectedBtn.setOnAction(e -> {
			Long[] selectedIds = selectedBalloons.stream().map(balloon -> balloon.messageInfo.messageId)
					.toArray(Long[]::new);
			backBtn.fire();
			listeners.forEach(listener -> listener.deleteMessagesRequested(selectedIds));
		});

	}

	private void initAttachmentLbl() {

		HBox.setHgrow(attachmentLbl, Priority.ALWAYS);
		attachmentLbl.getStyleClass().add("dim-label");
		attachmentLbl.setMaxWidth(Double.MAX_VALUE);
		attachmentLbl.setGraphic(ViewFactory.newAttachGraph(1.0));
		attachmentLbl.textProperty()
				.bind(Bindings.createStringBinding(
						() -> attachmentProperty.get() == null ? null : attachmentProperty.get().getFileName(),
						attachmentProperty));

	}

	private void initRemoveAttachmentBtn() {

		removeAttachmentBtn.setOnAction(e -> attachmentProperty.set(null));

	}

	private void initReportBtn() {

		reportBtn.setOnMouseClicked(e -> {
			if (Objects.equals(e.getButton(), MouseButton.PRIMARY))
				btnAnimation.play();
		});
		reportBtn.setOnAction(e -> listeners.forEach(listener -> listener.reportClicked()));

	}

	private void initShowFoldersBtn() {

		showFoldersBtn.setOnMouseClicked(e -> {
			if (Objects.equals(e.getButton(), MouseButton.PRIMARY))
				btnAnimation.play();
		});
		showFoldersBtn.setOnAction(e -> listeners.forEach(listener -> listener.showFoldersClicked()));

	}

	private void initSendBtn() {

		sendBtn.setOnMouseClicked(e -> {
			if (Objects.equals(e.getButton(), MouseButton.SECONDARY))
				btnAnimation.play();
		});
		sendBtn.setOnAction(e -> {
			String messageAreaText = messageArea.getText().trim();
			final String mesajTxt = messageAreaText.isEmpty() ? null : messageAreaText;
			messageArea.setText("");
			final FileBuilder fileBuilder = attachmentProperty.get();
			attachmentProperty.set(null);
			if (mesajTxt == null && fileBuilder == null)
				return;
			final Long referenceMessageId = referenceMessageProperty.get();
			referenceMessageProperty.set(null);
			listeners.forEach(listener -> listener.sendMessageClicked(mesajTxt, fileBuilder, referenceMessageId));
		});

	}

	private void initRecordBtn() {

		recordBtn.visibleProperty().bind(messageArea.textProperty().isEmpty().and(attachmentProperty.isNull()));
		recordBtn.setOnMouseClicked(e -> {
			if (Objects.equals(e.getButton(), MouseButton.SECONDARY))
				btnAnimation.play();
		});
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

	void addListener(IMessagePane listener) {

		listeners.add(listener);

	}

	void setStatusColor(Paint fill) {

		statusCircle.setFill(fill);

	}

	void setName(String name) {

		nameLabel.setText(name);

	}

	void setActive(boolean active) {

		activeProperty.setValue(active);

	}

	void setEditable(boolean editable) {

		editableProperty.set(editable);

	}

	EntityId getEntityId() {

		return entityId;

	}

	Long getLastEventTime() {

		return lastEventTime.get();

	}

	void addUpdateMessage(Message message) {

		Long messageId = message.getId();

		if (messageBalloons.containsKey(messageId)) {
			updateMessage(message);
			return;
		}

		if (Objects.equals(message.getViewStatus(), ViewStatus.DELETED))
			return;

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
				centerPane.getChildren().add(dayBox);

			} else {

				dayBoxes.get(dayBoxes.size() - 1).addMessageBalloonToBottom(messageBalloon);

			}

			if (messageInfo.isOutgoing)
				scrollPaneToBottom();

		} else if (minMessageId.get() == messageId) {

			if (dayBoxes.isEmpty() || !Objects.equals(dayBoxes.get(0).day, messageDay)) {

				DayBox dayBox = new DayBox(messageDay);
				dayBox.addMessageBalloonToTop(messageBalloon);
				dayBoxes.add(0, dayBox);
				centerPane.getChildren().add(0, dayBox);

			} else {

				dayBoxes.get(0).addMessageBalloonToTop(messageBalloon);

			}

			if (Objects.equals(messageId, futureReference.get())) {

				futureReference.set(-1L);

				scrollPaneToMessage(messageId);

			}

		}

		lastEventTime.set(System.currentTimeMillis());

	}

	private void updateMessage(Message message) {

		if (Objects.equals(message.getViewStatus(), ViewStatus.DELETED)) {
			deleteMessage(message);
			return;
		}

		Long messageId = message.getId();

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null)
			return;

		messageBalloon.messageInfo.statusProperty.set(message.getMessageStatus());
		messageBalloon.messageInfo.archivedProperty.set(Objects.equals(message.getViewStatus(), ViewStatus.ARCHIVED));

	}

	private void deleteMessage(Message message) {

		Long messageId = message.getId();

		if (Objects.equals(referenceMessageProperty.get(), messageId))
			referenceMessageProperty.set(null);

		referencedMessageIds.remove(messageId);

		MessageBalloon messageBalloon = messageBalloons.remove(messageId);

		if (messageBalloon == null)
			return;

		DayBox dayBox = messageBalloon.dayBox;

		if (dayBox == null)
			return;

		dayBox.removeMessageBalloon(messageBalloon);

		if (!dayBox.isEmpty())
			return;

		dayBoxes.remove(dayBox);
		centerPane.getChildren().remove(dayBox);

	}

	void addAttachment(FileBuilder fileBuilder) {

		attachmentProperty.set(fileBuilder);

	}

	void updateMessageProgress(Long messageId, int progress) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null)
			return;

		messageBalloon.messageInfo.progressProperty.set(progress);

	}

	void scrollPaneToMessage(Long messageId) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {

			scrollPaneToBottom();

			return;

		}

		scrollPane(messageBalloon, smallGap);

		messageBalloon.blink();

	}

	void savePosition(Long messageId) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null)
			return;

		Double yNode = scrollPane.sceneToLocal(messageBalloon.localToScene(0.0, 0.0)).getY();

		savedNodeY.set(new SimpleEntry<Node, Double>(messageBalloon, yNode));

	}

	void scrollToSavedPosition() {

		SimpleEntry<Node, Double> nodeY = savedNodeY.getAndSet(null);

		if (nodeY == null)
			return;

		scrollPane(nodeY.getKey(), nodeY.getValue());

	}

	void allMessagesLoaded() {

		scrollPane.vvalueProperty().removeListener(scrollListener);

	}

	void recordingStopped() {

		recordBtn.stopAnimation();

	}

	private Node getReferenceBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		Node referenceBalloon = newReferenceBalloon(messageInfo);
		referenceBalloon.getStyleClass().add("reference-balloon");

		final InnerShadow shadow = new InnerShadow(2 * gap, Color.DARKGRAY);
		referenceBalloon.setEffect(shadow);

		final Long messageId = messageInfo.messageId;

		referenceBalloon.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			if (referencedMessageIds.contains(messageId)) {
				goToMessage(messageId);
				return;
			}
			if (Objects.equals(shadow.getColor(), Color.DARKGRAY)) {
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

		if (!Objects.equals(message.getViewStatus(), ViewStatus.DELETED))
			referencedMessageIds.add(messageId);

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
						ViewFactory.newAttachGraph(0.8));
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

	void goToMessage(Long messageId) {

		if (messageBalloons.containsKey(messageId)) {

			scrollPaneToMessage(messageId);

		} else {

			futureReference.set(messageId);

			listeners.forEach(listener -> listener.messagesClaimed(minMessageId.get(), messageId));

		}

	}

	private void scrollPane(Node nodeToScrollTo, double bias) {

		scrollPane.applyCss();
		scrollPane.layout();

		Double centerPaneHeight = centerPane.getHeight();
		Double scrollPaneViewportHeight = scrollPane.getViewportBounds().getHeight();

		if (centerPaneHeight < scrollPaneViewportHeight)
			return;

		Double scrollY = centerPane.sceneToLocal(nodeToScrollTo.localToScene(0.0, 0.0)).getY() - bias;

		Double ratioY = Math.min(1.0, scrollY / (centerPaneHeight - scrollPaneViewportHeight));

		scrollPane.setVvalue(scrollPane.getVmax() * ratioY);

	}

	private void scrollPaneToBottom() {

		scrollPane.setVvalue(scrollPane.getVmax());

	}

	private MessageBalloon newMessageBalloon(Message message) {

		MessageInfo messageInfo = new MessageInfo(message);

		MessageBalloon messageBalloon = new MessageBalloon(messageInfo);

		if (message.getRefMessage() != null)
			messageBalloon.addReferenceBalloon(getReferenceBalloon(message.getRefMessage()));

		final Long messageId = messageInfo.messageId;

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

				dragPosProperty.set(e.getSceneX());

			} else if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_DRAGGED)) {

				longPressTimer.stop();

				if (replyGroup.getParent() != messageBalloon)
					messageBalloon.addReplyGroup(replyGroup);
				double diff = e.getSceneX() - dragPosProperty.get();
				dragPosProperty.set(e.getSceneX());
				if (!activeProperty.get())
					return;
				double radius = Math.max(0.0, Math.min(replyGroup.radius, replyGroup.inner.getRadius() + diff / 4.0));
				double translate = 4.0 * radius;
				messageBalloon.setTranslateX(translate);
				replyGroup.inner.setRadius(radius);

			} else if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_RELEASED)) {

				longPressTimer.stop();

				messageBalloon.setTranslateX(0.0);
				if (replyGroup.inner.getRadius() == replyGroup.radius)
					referenceMessageProperty.set(messageId);
				replyGroup.inner.setRadius(0.0);

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

		private final double radius = 3.0 * viewFactor;

		private final MessageInfo messageInfo;

		private final GridPane messagePane = new GridPane();
		private final HBox statusPane = new HBox(gap);
		private final Label timeLbl;
		private final Button smallStarBtn = ViewFactory.newStarBtn(0.65);
		private final Button selectionBtn = ViewFactory.newSelectionBtn();

		private final InnerShadow shadow = new InnerShadow(3 * gap, Color.TRANSPARENT);

		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private DayBox dayBox;
		private MessageGroup messageGroup;

		private MessageBalloon(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			timeLbl = new Label(HOUR_MIN.format(messageInfo.localDateTime));

			selectedProperty.addListener((e0, e1, e2) -> {
				deleteModeProperty.set(false);
				if (e2)
					selectedBalloons.add(this);
				else
					selectedBalloons.remove(this);
			});

			init();

		}

		private void init() {

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

			if (messageInfo.infoAvailable)
				add(getInfoBtn(), 3, 0);

			if (messageInfo.isOutgoing)
				col1.setHgrow(Priority.ALWAYS);
			else
				col3.setHgrow(Priority.ALWAYS);

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

			GridPane.setMargin(referenceBalloon, new Insets(0, 0, gap, 0));
			GridPane.setHgrow(referenceBalloon, Priority.ALWAYS);

			messagePane.add(referenceBalloon, 0, 1);

		}

		void blink() {

			if (!Objects.equals(shadow.getColor(), Color.TRANSPARENT))
				return;

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

			GridPane.setMargin(selectionBtn, new Insets(0, gap, 0, 0));

			selectionBtn.visibleProperty().bind(selectionModeProperty);
			selectionBtn.managedProperty().bind(selectionBtn.visibleProperty());
			selectionBtn.opacityProperty()
					.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

		}

		private void initMessagePane() {

			if (messageInfo.isOutgoing) {
				GridPane.setHalignment(messagePane, HPos.RIGHT);
				messagePane.setBackground(outgoingBackground);
			} else {
				GridPane.setHalignment(messagePane, HPos.LEFT);
				messagePane.setBackground(incomingBackground);
			}

			GridPane.setFillWidth(messagePane, false);

			messagePane.setStyle("-fx-min-width: 6em;");
			messagePane.setBorder(messagePaneBorder);
			messagePane.setPadding(new Insets(gap));
			messagePane.setEffect(shadow);

			if (messageInfo.attachment != null)
				messagePane.add(getAttachmentArea(), 0, 2);

			if (messageInfo.content != null)
				messagePane.add(getContentArea(), 0, 3);

			initStatusPane();

			messagePane.add(statusPane, 0, 4);

		}

		private Node getInfoBtn() {

			Button infoBtn = ViewFactory.newInfoBtn();
			GridPane.setMargin(infoBtn, new Insets(0, 0, 0, gap));

			infoBtn.visibleProperty().bind(selectionModeProperty.not());
			infoBtn.managedProperty().bind(infoBtn.visibleProperty());
			infoBtn.setOnAction(e -> listeners.forEach(listener -> listener.infoClicked(messageInfo.messageId)));

			return infoBtn;

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
					ViewFactory.newAttachGraph(1.0));
			GridPane.setFillWidth(attachmentLabel, false);

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

		private void initStatusPane() {

			GridPane.setHgrow(statusPane, Priority.ALWAYS);

			statusPane.setAlignment(Pos.CENTER);

			initSmallStarBtn();
			initTimeLbl();

			if (messageInfo.isOutgoing)
				statusPane.getChildren().addAll(smallStarBtn, getSpace(), getProgressLbl(), getInfoGrp(), timeLbl);
			else
				statusPane.getChildren().addAll(timeLbl, getSpace(), smallStarBtn);

		}

		private Node getSpace() {

			Region space = new Region();
			HBox.setHgrow(space, Priority.ALWAYS);

			return space;

		}

		private void initSmallStarBtn() {

			smallStarBtn.setEffect(new DropShadow());
			smallStarBtn.visibleProperty().bind(messageInfo.archivedProperty);

		}

		private void initTimeLbl() {

			timeLbl.setFont(Font.font(11.25 * viewFactor));
			timeLbl.setTextFill(Color.DIMGRAY);

		}

		private Node getProgressLbl() {

			Label progressLbl = new Label();

			progressLbl.setAlignment(Pos.BASELINE_RIGHT);
			progressLbl.setFont(Font.font(11.25 * viewFactor));
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.visibleProperty().bind(messageInfo.statusProperty.isEqualTo(MessageStatus.FRESH));
			progressLbl.managedProperty().bind(progressLbl.visibleProperty());
			progressLbl.textProperty().bind(Bindings.createStringBinding(() -> {
				int progress = messageInfo.progressProperty.get();
				return progress < 0 ? "" : String.format("%d%%", progress);
			}, messageInfo.progressProperty));

			return progressLbl;

		}

		private Node getInfoGrp() {

			Group infoGrp = new Group();
			Circle waitingCircle = new Circle(radius, messageInfo.statusProperty.get().getWaitingColor());
			Circle transmittedCircle = new Circle(radius, messageInfo.statusProperty.get().getTransmittedColor());

			transmittedCircle.setLayoutX(-2.0 * radius);
			infoGrp.getChildren().addAll(waitingCircle, transmittedCircle);

			infoGrp.visibleProperty().bind(messageInfo.statusProperty.isNotEqualTo(MessageStatus.FRESH));
			infoGrp.managedProperty().bind(infoGrp.visibleProperty());
			waitingCircle.fillProperty().bind(Bindings.createObjectBinding(
					() -> messageInfo.statusProperty.get().getWaitingColor(), messageInfo.statusProperty));
			transmittedCircle.fillProperty().bind(Bindings.createObjectBinding(
					() -> messageInfo.statusProperty.get().getTransmittedColor(), messageInfo.statusProperty));

			return infoGrp;

		}

	}

	private class MessageGroup extends VBox {

		private final Long ownerId;

		private final Label nameLbl;

		private final AtomicReference<MessageBalloon> namedBalloonRef = new AtomicReference<MessageBalloon>();

		private DayBox dayBox;

		private MessageGroup(MessageInfo messageInfo) {

			super(smallGap);

			this.ownerId = messageInfo.ownerId;

			if (entityId.isGroup() && !messageInfo.isOutgoing) {
				nameLbl = new Label(messageInfo.senderName);
				initNameLbl(messageInfo.nameColor);
			} else {
				nameLbl = null;
			}

		}

		private void initNameLbl(Color nameColor) {

			nameLbl.setPadding(new Insets(0.0, 0.0, gap, 0.0));
			nameLbl.setFont(Font.font(null, FontWeight.BOLD, nameLbl.getFont().getSize()));
			nameLbl.setTextFill(nameColor);

			getChildren().addListener(new ListChangeListener<Node>() {
				@Override
				public void onChanged(Change<? extends Node> arg0) {
					ObservableList<? extends Node> children = arg0.getList();
					MessageBalloon namedBalloon = namedBalloonRef.get();
					if (children.isEmpty() || Objects.equals(children.get(0), namedBalloon))
						return;
					if (namedBalloon != null)
						namedBalloon.removeNameLbl(nameLbl);
					Node firstChild = children.get(0);
					if (!(firstChild instanceof MessageBalloon))
						return;
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

			if (messageBalloon.messageGroup != this)
				return;

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
		private final VBox messageGroupBox = new VBox(gap);

		private final List<MessageGroup> messageGroups = Collections.synchronizedList(new ArrayList<MessageGroup>());

		private DayBox(LocalDate day) {

			super();

			this.day = day;

			dateLabel = new Label(DAY_MONTH_YEAR.format(day));

			init();

		}

		private void init() {

			// init dateLabel
			dateLabel.setPadding(new Insets(0.0, gap, 0.0, gap));
			dateLabel.setFont(Font.font(null, FontWeight.BOLD, dateLabel.getFont().getSize()));
			dateLabel.setTextFill(Color.GRAY);
			dateLabel.setBorder(dateBorder);
			dateLabel.setBackground(dateBackground);
			BorderPane.setAlignment(dateLabel, Pos.CENTER);
			BorderPane.setMargin(dateLabel, new Insets(0.0, 0.0, gap, 0.0));

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

			if (messageGroup == null || messageGroup.dayBox != this)
				return;

			messageGroup.removeMessageBalloon(messageBalloon);

			if (!messageGroup.isEmpty())
				return;

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
			outer.setRadius(radius);
			outer.setFill(Color.LIGHTSKYBLUE);
			inner.setFill(Color.DEEPSKYBLUE);
			getChildren().addAll(outer, inner);
			setTranslateX(-2.0 * (radius + gap));
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

	private final class MessageInfo {

		final Long messageId;
		final String content;
		final String attachment;
		final Long ownerId;
		final boolean isOutgoing;
		final String senderName;
		final LocalDateTime localDateTime;
		final AttachmentType attachmentType;
		final boolean infoAvailable;
		final Color nameColor;
		final ObjectProperty<MessageStatus> statusProperty = new SimpleObjectProperty<MessageStatus>();
		final IntegerProperty progressProperty = new SimpleIntegerProperty(-1);
		final BooleanProperty archivedProperty = new SimpleBooleanProperty();

		MessageInfo(Message message) {

			this.messageId = message.getId();
			this.content = message.getContent();
			this.attachment = message.getAttachment();
			this.ownerId = message.getOwner().getId();
			this.isOutgoing = message.isLocal();
			this.senderName = isOutgoing ? CommonMethods.translate("YOU") : message.getOwner().getName();
			this.localDateTime = LocalDateTime.ofInstant(message.getDate().toInstant(), ZoneId.systemDefault());
			this.attachmentType = message.getAttachmentType();
			this.infoAvailable = entityId.isGroup() && isOutgoing;
			this.nameColor = ViewFactory.getColorForUuid(message.getOwner().getUuid());
			this.statusProperty.set(message.getMessageStatus());
			this.archivedProperty.set(Objects.equals(message.getViewStatus(), ViewStatus.ARCHIVED));

			this.statusProperty.addListener((e0, e1, e2) -> {
				if (Objects.equals(e2, MessageStatus.FRESH))
					this.progressProperty.set(-1);
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

	void deleteMessagesRequested(Long... messageIds);

	void archiveMessagesRequested(Long... messageIds);

	void recordButtonPressed();

	void recordEventTriggered(Long refMessageId);

	void recordButtonReleased();

}

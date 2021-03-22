package com.ogya.dms.core.view;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.MessageDirection;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.MessageType;
import com.ogya.dms.core.structures.ReceiverType;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.RecordButton.RecordListener;
import com.ogya.dms.core.view.factory.ViewFactory;
import com.sun.javafx.tk.Toolkit;

import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
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
import javafx.scene.effect.BlurType;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

class MessagePane extends BorderPane {

	private static final SimpleDateFormat HOUR_MIN = new SimpleDateFormat("HH:mm");
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

	private final double gap = ViewFactory.getGap();
	private final double smallGap = 2.0 * gap / 5.0;

	private final double viewFactor = ViewFactory.getViewFactor();

	private final HBox topPane = new HBox(2 * gap);
	private final VBox centerPane = new VBox(2 * gap);
	private final GridPane bottomPane = new GridPane();

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button backBtn = ViewFactory.newBackBtn();
	private final Circle statusCircle = new Circle(7.0 * viewFactor);
	private final Label nameLabel = new Label();
	private final TextArea messageArea = new TextArea();
	private final HBox referencePane = new HBox();
	private final Button sendBtn = ViewFactory.newSendBtn();
	private final RecordButton recordBtn = new RecordButton();
	private final Button showFoldersBtn = ViewFactory.newAttachBtn();
	private final Button reportBtn = ViewFactory.newReportBtn();
	private final StackPane btnPane = new StackPane();

	private final Button deleteSelectedBtn = new Button(CommonMethods.translate("DELETE_SELECTED"));
	private final Button selectAllBtn = ViewFactory.newSelectionBtn();
	private final Button starBtn = ViewFactory.newStarBtn();
	private final Button deleteBtn = ViewFactory.newDeleteBtn();
	private final BooleanProperty deleteModeProperty = new SimpleBooleanProperty(false);
	private final ObservableSet<MessageBalloon> selectedBalloons = FXCollections.observableSet();

	private final AtomicReference<Runnable> backActionRef = new AtomicReference<Runnable>();

	private final ReplyGroup replyGroup = new ReplyGroup(12.0 * viewFactor);

	private final Effect highlight = new ColorAdjust(0.8, 0.0, 0.0, 0.0);

	private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty editableProperty = new SimpleBooleanProperty(false);
	private final ObjectProperty<Long> referenceMessageProperty = new SimpleObjectProperty<Long>(null);

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

	private final AtomicLong futureReference = new AtomicLong(-1L);

	private final BooleanProperty selectionModeProperty = new SimpleBooleanProperty(false);

	private final LongPressTimer longPressTimer = new LongPressTimer();

	MessagePane() {

		super();

		init();

	}

	private void init() {

		registerListeners();

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));

		topPane.setPadding(new Insets(gap));
		centerPane.setPadding(new Insets(gap));
		bottomPane.setPadding(new Insets(gap));
		bottomPane.setHgap(gap);
		bottomPane.setVgap(gap);

		topPane.setAlignment(Pos.CENTER_LEFT);

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);

		nameLabel.underlineProperty().bind(Bindings.and(editableProperty, nameLabel.hoverProperty()));

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

		nameLabel.getStyleClass().add("black-label");
		nameLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0 * viewFactor));
		nameLabel.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(nameLabel, Priority.ALWAYS);

		backBtn.setOnAction(e -> {
			if (selectionModeProperty.get()) {
				messageBalloons.values().stream().filter(messageBalloon -> messageBalloon.selectedProperty.get())
						.forEach(messageBalloon -> messageBalloon.selectedProperty.set(false));
				deleteModeProperty.set(false);
				selectionModeProperty.set(false);
			} else {
				Runnable backAction = backActionRef.get();
				if (backAction == null)
					return;
				backAction.run();
				backBtn.setEffect(null);
			}
		});

		topPane.getChildren().addAll(backBtn, statusCircle, nameLabel, selectAllBtn, starBtn, deleteBtn);

		initBtnPane();

		referencePane.getStyleClass().add("reference-panel");

		// Close Button

		Button closeBtn = ViewFactory.newCancelBtn();
		GridPane.setMargin(closeBtn, new Insets(gap));
		GridPane.setHalignment(closeBtn, HPos.RIGHT);
		GridPane.setValignment(closeBtn, VPos.TOP);
		closeBtn.setOnAction(e -> referenceMessageProperty.set(null));
		closeBtn.visibleProperty().bind(referenceMessageProperty.isNotNull());
		closeBtn.managedProperty().bind(closeBtn.visibleProperty());

		//

		GridPane.setHgrow(messageArea, Priority.ALWAYS);
		bottomPane.add(referencePane, 0, 0);
		bottomPane.add(closeBtn, 0, 0);
		bottomPane.add(messageArea, 0, 1);
		bottomPane.add(btnPane, 1, 1);
		bottomPane.add(deleteSelectedBtn, 0, 1, 2, 1);

		bottomPane.managedProperty().bind(bottomPane.visibleProperty());
		bottomPane.visibleProperty().bind(activeProperty);

		// Settings

		selectAllBtn.visibleProperty().bind(selectionModeProperty);
		selectAllBtn.managedProperty().bind(selectAllBtn.visibleProperty());
		selectAllBtn.opacityProperty().bind(Bindings.createDoubleBinding(
				() -> selectedBalloons.size() < messageBalloons.size() ? 0.5 : 1.0, selectedBalloons, messageBalloons));
		selectAllBtn.setOnAction(e -> {
			boolean willSelect = selectedBalloons.size() < messageBalloons.size();
			messageBalloons.values().forEach(messageBalloon -> messageBalloon.selectedProperty.set(willSelect));
		});

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

		//

		setTop(topPane);
		setCenter(scrollPane);
		setBottom(bottomPane);

	}

	void addListener(IMessagePane listener) {

		listeners.add(listener);

	}

	void setOnBackAction(final Runnable runnable) {

		backActionRef.set(runnable);

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

	void addMessage(Message message) {

		if (Objects.equals(message.getViewStatus(), ViewStatus.DELETED))
			return;

		Long messageId = message.getId();

		if (messageBalloons.containsKey(messageId))
			return;

		MessageBalloon messageBalloon = newMessageBalloon(message);

		messageBalloons.put(messageId, messageBalloon);

		MessageInfo messageInfo = messageBalloon.messageInfo;

		LocalDate messageDay = messageInfo.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

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

	}

	void updateMessage(Message message) {

		if (Objects.equals(message.getViewStatus(), ViewStatus.DELETED)) {

			deleteMessage(message);

			return;

		}

		Long messageId = message.getId();

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null)
			return;

		messageBalloon.updateMessageStatus(message.getMessageStatus(),
				Objects.equals(message.getViewStatus(), ViewStatus.ARCHIVED));

	}

	void updateMessageProgress(Long messageId, int progress) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null)
			return;

		messageBalloon.setProgress(progress);

	}

	void scrollPaneToMessage(Long messageId) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {

			scrollPaneToBottom();

			return;

		}

		scrollPane(messageBalloon, smallGap);

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

	void highlightBackButton() {

		backBtn.setEffect(highlight);

	}

	void recordingStarted() {

		recordBtn.startAnimation();

	}

	void recordingStopped() {

		recordBtn.stopAnimation();

	}

	Long getRefMessageId() {

		return referenceMessageProperty.get();

	}

	private void registerListeners() {

		nameLabel.setOnMouseClicked(e -> {
			if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
				return;
			if (editableProperty.get())
				listeners.forEach(listener -> listener.editClicked());
		});

		scrollPane.vvalueProperty().addListener((e0, e1, e2) -> {

			autoScroll.set(e2.doubleValue() == scrollPane.getVmax());

			if (e2.doubleValue() != 0.0 || e1.doubleValue() == 0.0)
				return;

			listeners.forEach(listener -> listener.paneScrolledToTop(minMessageId.get()));

		});

		centerPane.heightProperty().addListener((e0, e1, e2) -> {

			if (autoScroll.get())
				scrollPaneToBottom();

		});

		referenceMessageProperty.addListener((e0, e1, e2) -> {

			referencePane.getChildren().clear();

			if (e2 == null)
				return;

			MessageInfo messageInfo = messageBalloons.get(e2).messageInfo;
			referencePane.getChildren().add(newReferenceBalloon(messageInfo.isOutgoing, messageInfo.senderName,
					messageInfo.nameColor, messageInfo.messageType, messageInfo.content));

		});

		sendBtn.setOnAction(e -> {

			final String mesajTxt = messageArea.getText().trim();

			messageArea.setText("");

			if (mesajTxt.isEmpty())
				return;

			final Long referenceMessageId = referenceMessageProperty.get();
			referenceMessageProperty.set(null);

			listeners.forEach(listener -> listener.sendMessageClicked(mesajTxt, referenceMessageId));

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

		showFoldersBtn.setOnAction(e -> listeners.forEach(listener -> listener.showFoldersClicked()));

		reportBtn.setOnAction(e -> listeners.forEach(listener -> listener.reportClicked()));

	}

	private Node getReferenceBalloon(Message message) {

		Long messageId = message.getId();

		boolean isOutgoing = Objects.equals(message.getMessageDirection(), MessageDirection.OUT);
		String senderName = isOutgoing ? CommonMethods.translate("YOU") : message.getOwner().getName();
		Color nameColor = ViewFactory.getColorForUuid(message.getOwner().getUuid());
		MessageType messageType = message.getMessageType();
		String content = Objects.equals(message.getMessageType(), MessageType.TEXT) ? message.getContent()
				: message.getAttachment();
		if (Objects.equals(message.getMessageType(), MessageType.FILE))
			content = Paths.get(content).getFileName().toString();

		Node referenceBalloon = newReferenceBalloon(isOutgoing, senderName, nameColor, messageType, content);
		referenceBalloon.getStyleClass().add("reference-balloon");

		InnerShadow shadow = new InnerShadow(BlurType.GAUSSIAN, Color.DARKGRAY, 2 * gap, 0, 0, 0);
		referenceBalloon.setEffect(shadow);

		final Transition errorAnimation = new Transition() {

			{
				setCycleDuration(Duration.millis(500.0));
			}

			@Override
			protected void interpolate(double arg0) {
				shadow.setColor(Color.RED.interpolate(Color.DARKGRAY, arg0));
			}

		};

		referenceBalloon.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			if (!referencedMessageIds.contains(messageId)) {
				errorAnimation.play();
				return;
			}
			if (messageBalloons.containsKey(messageId)) {
				scrollPaneToMessage(messageId);
			} else {
				futureReference.set(messageId);
				listeners.forEach(listener -> listener.messagesClaimed(minMessageId.get(), messageId));
			}
			e.consume();
		});

		if (!Objects.equals(message.getViewStatus(), ViewStatus.DELETED))
			referencedMessageIds.add(messageId);

		return referenceBalloon;

	}

	private Node newReferenceBalloon(boolean isOutgoing, String senderName, Color nameColor, MessageType messageType,
			String content) {

		VBox referenceBalloon = new VBox(gap);
		referenceBalloon.setPadding(new Insets(gap));

		Label nameLabel = new Label(senderName);
		nameLabel.setFont(Font.font(null, FontWeight.BOLD, nameLabel.getFont().getSize() * 0.8));
		nameLabel.setTextFill(nameColor);

		referenceBalloon.getChildren().add(nameLabel);

		switch (messageType) {

		case AUDIO:

			DmsDummyPlayer dmsDummyPlayer = new DmsDummyPlayer();

			VBox.setMargin(dmsDummyPlayer, new Insets(0.0, 0.0, 0.0, gap));

			referenceBalloon.getChildren().add(dmsDummyPlayer);

			break;

		default:

			Label messageLbl = new Label(content) {
				@Override
				public Orientation getContentBias() {
					return Orientation.HORIZONTAL;
				}

				@Override
				protected double computePrefHeight(double arg0) {
					return Math.min(super.computePrefHeight(arg0), getFont().getSize() * 5.0);
				}

			};

			messageLbl.getStyleClass().add("black-label");

			messageLbl.setFont(Font.font(messageLbl.getFont().getSize() * 0.8));
			messageLbl.setWrapText(true);
			VBox.setMargin(messageLbl, new Insets(0.0, 0.0, 0.0, gap));

			referenceBalloon.getChildren().add(messageLbl);

			break;

		}

		return referenceBalloon;

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

	private void initBtnPane() {

		recordBtn.visibleProperty().bind(messageArea.textProperty().isEmpty());

		btnPane.getChildren().addAll(reportBtn, showFoldersBtn, sendBtn, recordBtn);

		Interpolator interpolator = Interpolator.EASE_BOTH;

		Transition transition = new Transition() {

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

		sendBtn.setOnMouseClicked(e -> {
			if (Objects.equals(e.getButton(), MouseButton.SECONDARY))
				transition.play();
		});

		recordBtn.setOnMouseClicked(e -> {
			if (Objects.equals(e.getButton(), MouseButton.SECONDARY))
				transition.play();
		});

		showFoldersBtn.setOnMouseClicked(e -> {
			if (Objects.equals(e.getButton(), MouseButton.PRIMARY))
				transition.play();
		});

		reportBtn.setOnMouseClicked(e -> {
			if (Objects.equals(e.getButton(), MouseButton.PRIMARY))
				transition.play();
		});

	}

	private MessageBalloon newMessageBalloon(Message message) {

		final Long messageId = message.getId();

		String content = Objects.equals(message.getMessageType(), MessageType.TEXT) ? message.getContent()
				: message.getAttachment();
		if (Objects.equals(message.getMessageType(), MessageType.FILE))
			content = Paths.get(content).getFileName().toString();

		boolean isOutgoing = Objects.equals(message.getMessageDirection(), MessageDirection.OUT);
		Date messageDate = message.getDate();

		String senderName = isOutgoing ? CommonMethods.translate("YOU") : message.getOwner().getName();
		Long ownerId = message.getOwner().getId();

		boolean isGroup = !Objects.equals(message.getReceiverType(), ReceiverType.CONTACT);

		MessageInfo messageInfo = new MessageInfo(messageId, content, ownerId, senderName, messageDate, isGroup,
				isOutgoing, message.getMessageType(), ViewFactory.getColorForUuid(message.getOwner().getUuid()));

		MessageBalloon messageBalloon = new MessageBalloon(messageInfo);
		messageBalloon.updateMessageStatus(message.getMessageStatus(),
				Objects.equals(message.getViewStatus(), ViewStatus.ARCHIVED));

		if (message.getRefMessage() != null)
			messageBalloon.addReferenceBalloon(getReferenceBalloon(message.getRefMessage()));

		if (Objects.equals(messageInfo.messageType, MessageType.FILE)) {

			messageBalloon.contentArea.cursorProperty().bind(Bindings.createObjectBinding(
					() -> selectionModeProperty.get() ? Cursor.DEFAULT : Cursor.HAND, selectionModeProperty));

			messageBalloon.contentArea
					.setOnMouseClicked(e -> listeners.forEach(listener -> listener.messageClicked(messageId)));

			final Effect dropShadow = new DropShadow();

			messageBalloon.messagePane.effectProperty()
					.bind(Bindings
							.createObjectBinding(
									() -> messageBalloon.contentArea.hoverProperty().and(selectionModeProperty.not())
											.get() ? dropShadow : null,
									messageBalloon.contentArea.hoverProperty(), selectionModeProperty));

		}

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

		if (messageInfo.infoAvailable) {

			messageBalloon.infoBtn.setOnAction(e -> listeners.forEach(listener -> listener.infoClicked(messageId)));

		}

		return messageBalloon;

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

	private class MessageBalloon extends GridPane {

		private final double radius = 3.0 * viewFactor;

		private final MessageInfo messageInfo;

		private final GridPane messagePane = new GridPane();
		private final GridPane contentArea = new GridPane();
		private final Label progressLbl = new Label();
		private final Label timeLbl;
		private final Group infoGrp = new Group();
		private final Circle waitingCircle = new Circle(radius, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(radius, Color.TRANSPARENT);
		private final Button smallStarBtn = ViewFactory.newSmallStarBtn();
		private final Button infoBtn = ViewFactory.newInfoBtn();
		private final Button selectionBtn = ViewFactory.newSelectionBtn();

		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private DayBox dayBox;
		private MessageGroup messageGroup;

		private MessageBalloon(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			timeLbl = new Label(HOUR_MIN.format(messageInfo.date));

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

			HBox messagePaneContainer = new HBox(gap);
			messagePaneContainer.setAlignment(Pos.CENTER_LEFT);
			HBox.setHgrow(messagePane, Priority.ALWAYS);

			add(selectionBtn, 1, 0, 1, 1);
			add(messagePaneContainer, 2, 0, 1, 1);

			messagePaneContainer.getChildren().add(messagePane);

			if (messageInfo.infoAvailable) {
				infoBtn.disableProperty().bind(selectionModeProperty);
				messagePaneContainer.getChildren().add(infoBtn);
			}

			GridPane.setFillWidth(messagePaneContainer, false);

			if (messageInfo.isOutgoing) {

				col1.setHgrow(Priority.ALWAYS);

				initOutgoingMessagePane();

				GridPane.setHalignment(messagePaneContainer, HPos.RIGHT);

			} else {

				col3.setHgrow(Priority.ALWAYS);

				initIncomingMessagePane();

				GridPane.setHalignment(messagePaneContainer, HPos.LEFT);

			}

		}

		void addReplyGroup(ReplyGroup replyGroup) {
			add(replyGroup, 0, 0, 1, 1);
		}

		void updateMessageStatus(MessageStatus messageStatus, boolean archived) {

			if (Objects.equals(messageStatus, MessageStatus.FRESH))
				setProgress(-1);

			messageInfo.archivedProperty.set(archived);
			infoGrp.setVisible(!Objects.equals(messageStatus, MessageStatus.FRESH));

			waitingCircle.setFill(messageStatus.getWaitingColor());
			transmittedCircle.setFill(messageStatus.getTransmittedColor());

		}

		void setProgress(int progress) {

			progressLbl.setText(progress < 0 ? "" : String.format("%d%%", progress));

		}

		void addReferenceBalloon(Node referenceBalloon) {

			GridPane.setMargin(referenceBalloon, new Insets(0, 0, gap, 0));
			GridPane.setHgrow(referenceBalloon, Priority.ALWAYS);

			messagePane.add(referenceBalloon, 0, 0, 3, 1);

		}

		private void initSelectionBtn() {

			GridPane.setMargin(selectionBtn, new Insets(0, gap, 0, 0));

			selectionBtn.visibleProperty().bind(selectionModeProperty);
			selectionBtn.managedProperty().bind(selectionBtn.visibleProperty());
			selectionBtn.opacityProperty()
					.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

		}

		private void initMessagePane() {

			messagePane.setStyle("-fx-min-width: 6em;");

			messagePane.setBorder(new Border(new BorderStroke(
					Objects.equals(messageInfo.messageType, MessageType.FILE) ? Color.BLUE : Color.DARKGRAY,
					BorderStrokeStyle.SOLID, new CornerRadii(10.0 * viewFactor), BorderWidths.DEFAULT)));

			messagePane.setPadding(new Insets(gap));
			messagePane.setHgap(gap);

		}

		private void initOutgoingMessagePane() {

			messagePane.setBackground(new Background(
					new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0 * viewFactor), Insets.EMPTY)));

			initContentArea();
			initSmallStarBtn();
			initInfoGrp();
			initProgressLbl();
			initTimeLbl();

			messagePane.add(contentArea, 0, 1, 3, 1);
			messagePane.add(smallStarBtn, 0, 2, 1, 1);
			messagePane.add(infoGrp, 1, 2, 1, 1);
			messagePane.add(progressLbl, 1, 2, 1, 1);
			messagePane.add(timeLbl, 2, 2, 1, 1);

		}

		private void initIncomingMessagePane() {

			messagePane.setBackground(new Background(
					new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0 * viewFactor), Insets.EMPTY)));

			initContentArea();
			initTimeLbl();
			initSmallStarBtn();

			GridPane.setHgrow(timeLbl, Priority.ALWAYS);

			messagePane.add(contentArea, 0, 1, 3, 1);
			messagePane.add(timeLbl, 0, 2, 1, 1);
			messagePane.add(smallStarBtn, 2, 2, 1, 1);

		}

		private void initContentArea() {

			switch (messageInfo.messageType) {

			case AUDIO:

				try {

					contentArea.add(new DmsMediaPlayer(Paths.get(messageInfo.content)), 0, 0, 1, 1);

					break;

				} catch (Exception e) {

				}

			default:

				Label messageLbl = new Label(messageInfo.content);

				messageLbl.getStyleClass().add("black-label");
				messageLbl.setWrapText(true);

				contentArea.add(messageLbl, 0, 0, 1, 1);

				break;

			}

		}

		private void initSmallStarBtn() {

			smallStarBtn.setEffect(new DropShadow());

			smallStarBtn.visibleProperty().bind(messageInfo.archivedProperty);

		}

		private void initInfoGrp() {

			GridPane.setHgrow(infoGrp, Priority.ALWAYS);
			GridPane.setHalignment(infoGrp, HPos.RIGHT);

			transmittedCircle.setLayoutX(2.0 * radius);
			infoGrp.getChildren().addAll(waitingCircle, transmittedCircle);

		}

		private void initProgressLbl() {

			progressLbl.visibleProperty().bind(infoGrp.visibleProperty().not());

			progressLbl.setFont(Font.font(11.25 * viewFactor));
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.setMinWidth(
					Toolkit.getToolkit().getFontLoader().computeStringWidth("100%", progressLbl.getFont()));

		}

		private void initTimeLbl() {

			timeLbl.setFont(Font.font(11.25 * viewFactor));
			timeLbl.setTextFill(Color.DIMGRAY);

		}

	}

	private class MessageGroup extends BorderPane {

		private final MessageInfo messageInfo;

		private final VBox messageBox = new VBox(smallGap);

		private DayBox dayBox;

		private MessageGroup(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			init();

		}

		private void init() {

			// init nameLabel
			if (messageInfo.isGroup && !messageInfo.isOutgoing) {

				Label nameLabel = new Label(messageInfo.senderName);
				nameLabel.setPadding(new Insets(0.0, gap, 0.0, gap));
				nameLabel.setFont(Font.font(null, FontWeight.BOLD, nameLabel.getFont().getSize()));
				nameLabel.setTextFill(messageInfo.nameColor);
				BorderPane.setAlignment(nameLabel, messageInfo.isOutgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
				BorderPane.setMargin(nameLabel, new Insets(0.0, 0.0, gap, 0.0));

				setTop(nameLabel);

			}

			setCenter(messageBox);

		}

		private void addMessageBalloonToTop(MessageBalloon messageBalloon) {

			messageBox.getChildren().add(0, messageBalloon);

			messageBalloon.messageGroup = this;

		}

		private void addMessageBalloonToBottom(MessageBalloon messageBalloon) {

			messageBox.getChildren().add(messageBalloon);

			messageBalloon.messageGroup = this;

		}

		private void removeMessageBalloon(MessageBalloon messageBalloon) {

			if (messageBalloon.messageGroup != this)
				return;

			messageBox.getChildren().remove(messageBalloon);

			messageBalloon.messageGroup = null;

		}

		private boolean isEmpty() {

			return messageBox.getChildren().isEmpty();

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
			dateLabel.setBackground(
					new Background(new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(gap), Insets.EMPTY)));
			dateLabel.setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
					new CornerRadii(gap), BorderWidths.DEFAULT, Insets.EMPTY)));
			BorderPane.setAlignment(dateLabel, Pos.CENTER);
			BorderPane.setMargin(dateLabel, new Insets(0.0, 0.0, gap, 0.0));

			setTop(dateLabel);
			setCenter(messageGroupBox);

		}

		private void addMessageBalloonToTop(MessageBalloon messageBalloon) {

			if (messageGroups.isEmpty()
					|| !Objects.equals(messageGroups.get(0).messageInfo.ownerId, messageBalloon.messageInfo.ownerId)) {
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

			if (messageGroups.isEmpty()
					|| !Objects.equals(messageGroups.get(messageGroups.size() - 1).messageInfo.ownerId,
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

	private static final class MessageInfo {

		final Long messageId;
		final String content;
		final Long ownerId;
		final String senderName;
		final Date date;
		final boolean isGroup;
		final boolean isOutgoing;
		final MessageType messageType;
		final boolean infoAvailable;
		final Color nameColor;
		final BooleanProperty archivedProperty = new SimpleBooleanProperty(false);

		MessageInfo(Long messageId, String content, Long ownerId, String senderName, Date date, boolean isGroup,
				boolean isOutgoing, MessageType messageType, Color nameColor) {

			this.messageId = messageId;
			this.content = content;
			this.ownerId = ownerId;
			this.senderName = senderName;
			this.date = date;
			this.isGroup = isGroup;
			this.isOutgoing = isOutgoing;
			this.messageType = messageType;
			this.infoAvailable = isGroup && isOutgoing;
			this.nameColor = nameColor;

		}

	}

}

interface IMessagePane {

	void editClicked();

	void paneScrolledToTop(Long topMessageId);

	void messagesClaimed(Long lastMessageIdExcl, Long firstMessageIdIncl);

	void sendMessageClicked(String message, Long refMessageId);

	void showFoldersClicked();

	void reportClicked();

	void messageClicked(Long messageId);

	void infoClicked(Long messageId);

	void deleteMessagesRequested(Long... messageIds);

	void archiveMessagesRequested(Long... messageIds);

	void recordButtonPressed();

	void recordEventTriggered(Long refMessageId);

	void recordButtonReleased();

}

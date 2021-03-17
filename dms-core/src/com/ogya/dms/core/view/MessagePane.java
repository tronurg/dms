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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.MessageDirection;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.MessageType;
import com.ogya.dms.core.structures.ReceiverType;
import com.ogya.dms.core.structures.WaitStatus;
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
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
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

	private final HBox topPane = new HBox(gap);
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

	private final ReplyGroup replyGroup = new ReplyGroup(12.0 * viewFactor);

	private final Effect highlight = new ColorAdjust(0.8, 0.0, 0.0, 0.0);

	private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty editableProperty = new SimpleBooleanProperty(false);
	private final ObjectProperty<Long> referenceMessageProperty = new SimpleObjectProperty<Long>(null);

	private final List<DayBox> dayBoxes = Collections.synchronizedList(new ArrayList<DayBox>());

	private final Map<Long, MessageBalloon> messageBalloons = Collections
			.synchronizedMap(new HashMap<Long, MessageBalloon>());
	private final Map<Long, MessageBalloon> transientBalloons = Collections
			.synchronizedMap(new HashMap<Long, MessageBalloon>());

	private final AtomicBoolean autoScroll = new AtomicBoolean(true);

	private final AtomicReference<SimpleEntry<Node, Double>> savedNodeY = new AtomicReference<SimpleEntry<Node, Double>>(
			null);

	private final DoubleProperty dragPosProperty = new SimpleDoubleProperty(0.0);

	private final List<IMessagePane> listeners = Collections.synchronizedList(new ArrayList<IMessagePane>());

	private final AtomicLong minMessageId = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong maxMessageId = new AtomicLong(Long.MIN_VALUE);

	private final AtomicLong futureReference = new AtomicLong(-1L);

	private final BooleanProperty selectionModeProperty = new SimpleBooleanProperty(false);

	private final AnimationTimer longPressTimer = new AnimationTimer() {

		private long startTime;

		@Override
		public void handle(long arg0) {
			if (arg0 - startTime < 500e6)
				return;
			stop();
			selectionModeProperty.set(true);
		}

		@Override
		public void start() {
			startTime = System.nanoTime();
			super.start();
		};

	};

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

		HBox.setMargin(statusCircle, new Insets(gap, gap, gap, 3 * gap));
		nameLabel.getStyleClass().add("black-label");
		nameLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0 * viewFactor));

		topPane.getChildren().addAll(backBtn, statusCircle, nameLabel);

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

		bottomPane.managedProperty().bind(bottomPane.visibleProperty());
		bottomPane.visibleProperty().bind(activeProperty);

		setTop(topPane);
		setCenter(scrollPane);
		setBottom(bottomPane);

		centerPane.heightProperty().addListener((e0, e1, e2) -> {

			if (autoScroll.get())
				scrollPaneToBottom();

		});

		scrollPane.vvalueProperty().addListener((e0, e1, e2) -> {

			autoScroll.set(e1.doubleValue() == scrollPane.getVmax());

		});

	}

	void addListener(IMessagePane listener) {

		listeners.add(listener);

	}

	void setOnBackAction(final Runnable runnable) {

		backBtn.setOnAction(e -> {
			runnable.run();
			backBtn.setEffect(null);
		});

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

		Long messageId = message.getId();

		if (messageBalloons.containsKey(messageId))
			return;

		boolean isOutgoing = Objects.equals(message.getMessageDirection(), MessageDirection.OUT);
		Date messageDate = message.getDate();

		MessageBalloon messageBalloon = transientBalloons.remove(messageId);

		if (messageBalloon == null) {

			String senderName = isOutgoing ? CommonMethods.translate("YOU") : message.getOwner().getName();
			Long ownerId = message.getOwner().getId();

			boolean isGroup = !Objects.equals(message.getReceiverType(), ReceiverType.CONTACT);

			MessageInfo messageInfo = new MessageInfo(ownerId, senderName, messageDate, isGroup, isOutgoing,
					message.getMessageType(), ViewFactory.getColorForUuid(message.getOwner().getUuid()));

			messageBalloon = newMessageBalloon(message, messageInfo);

		}

		if (message.getRefMessage() != null)
			messageBalloon.addReferenceBalloon(getReferenceBalloon(message.getRefMessage()));

		messageBalloons.put(messageId, messageBalloon);

		LocalDate messageDay = messageDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		minMessageId.set(Math.min(minMessageId.get(), messageId));
		maxMessageId.set(Math.max(maxMessageId.get(), messageId));

		if (maxMessageId.get() == messageId) {

			referenceMessageProperty.set(null);

			if (dayBoxes.isEmpty() || !Objects.equals(dayBoxes.get(dayBoxes.size() - 1).day, messageDay)) {

				DayBox dayBox = new DayBox(messageDay);
				dayBox.addMessageBalloonToBottom(messageBalloon);
				dayBoxes.add(dayBox);
				centerPane.getChildren().add(dayBox);

			} else {

				dayBoxes.get(dayBoxes.size() - 1).addMessageBalloonToBottom(messageBalloon);

			}

			if (isOutgoing)
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

	void updateMessageStatus(Message message) {

		Long messageId = message.getId();

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null)
			return;

		if (Objects.equals(referenceMessageProperty.get(), messageId))
			referenceMessageProperty.set(null);

		messageBalloon.updateMessageStatus(message.getMessageStatus(),
				Objects.equals(message.getWaitStatus(), WaitStatus.CANCELED));

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

			if (e2.doubleValue() != 0.0 || e1.doubleValue() == 0.0)
				return;

			listeners.forEach(listener -> listener.paneScrolledToTop(minMessageId.get()));

		});

		referenceMessageProperty.addListener((e0, e1, e2) -> {

			referencePane.getChildren().clear();

			if (e2 != null)
				referencePane.getChildren().add(messageBalloons.get(e2).newReferenceBalloon());

		});

		sendBtn.setOnAction(e -> {

			final String mesajTxt = messageArea.getText().trim();

			messageArea.setText("");

			if (mesajTxt.isEmpty())
				return;

			listeners.forEach(listener -> listener.sendMessageClicked(mesajTxt, referenceMessageProperty.getValue()));

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

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null)
			messageBalloon = transientBalloons.get(messageId);

		if (messageBalloon == null) {

			boolean isOutgoing = Objects.equals(message.getMessageDirection(), MessageDirection.OUT);
			Date messageDate = message.getDate();

			String senderName = isOutgoing ? CommonMethods.translate("YOU") : message.getOwner().getName();
			Long ownerId = message.getOwner().getId();

			boolean isGroup = !Objects.equals(message.getReceiverType(), ReceiverType.CONTACT);

			MessageInfo messageInfo = new MessageInfo(ownerId, senderName, messageDate, isGroup, isOutgoing,
					message.getMessageType(), ViewFactory.getColorForUuid(message.getOwner().getUuid()));

			messageBalloon = newMessageBalloon(message, messageInfo);

			transientBalloons.put(messageId, messageBalloon);

		}

		Node referenceBalloon = messageBalloon.newReferenceBalloon();

		referenceBalloon.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			if (messageBalloons.containsKey(messageId)) {
				scrollPaneToMessage(messageId);
			} else {
				futureReference.set(messageId);
				listeners.forEach(listener -> listener.messagesClaimed(minMessageId.get(), messageId));
			}
			e.consume();
		});

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

	private MessageBalloon newMessageBalloon(Message message, MessageInfo messageInfo) {

		final Long messageId = message.getId();

		String content = Objects.equals(message.getMessageType(), MessageType.TEXT) ? message.getContent()
				: message.getAttachment();
		if (Objects.equals(message.getMessageType(), MessageType.FILE))
			content = Paths.get(content).getFileName().toString();

		MessageBalloon messageBalloon = new MessageBalloon(content, messageInfo);
		messageBalloon.updateMessageStatus(message.getMessageStatus(),
				Objects.equals(message.getWaitStatus(), WaitStatus.CANCELED));

		if (Objects.equals(messageInfo.messageType, MessageType.FILE)) {

			messageBalloon.contentArea.cursorProperty()
					.bind(Bindings.createObjectBinding(
							() -> messageBalloon.activeProperty.and(selectionModeProperty.not()).get() ? Cursor.HAND
									: Cursor.DEFAULT,
							messageBalloon.activeProperty, selectionModeProperty));

			messageBalloon.contentArea.onMouseClickedProperty().bind(Bindings.createObjectBinding(() -> {
				if (messageBalloon.activeProperty.get())
					return e -> listeners.forEach(listener -> listener.messageClicked(messageId));
				return null;
			}, messageBalloon.activeProperty));

			final DropShadow dropShadow = new DropShadow();

			messageBalloon.messagePane.effectProperty().bind(Bindings.createObjectBinding(
					() -> messageBalloon.activeProperty.and(messageBalloon.contentArea.hoverProperty())
							.and(selectionModeProperty.not()).get() ? dropShadow : null,
					messageBalloon.activeProperty, messageBalloon.contentArea.hoverProperty(), selectionModeProperty));

		}

		messageBalloon.addEventFilter(MouseEvent.ANY, e -> {

			if (!(Objects.equals(e.getButton(), MouseButton.NONE)
					|| Objects.equals(e.getButton(), MouseButton.PRIMARY))) {

				e.consume();

			} else if (selectionModeProperty.get()) {

				if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_CLICKED))
					messageBalloon.selectedProperty.set(!messageBalloon.selectedProperty.get());

				e.consume();

			} else if (Objects.equals(e.getEventType(), MouseEvent.MOUSE_PRESSED)) {

				longPressTimer.start();
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

		if (messageInfo.isOutgoing) {

			messageBalloon.cancelBtn.setOnAction(e -> listeners.forEach(listener -> listener.cancelClicked(messageId)));

		}

		return messageBalloon;

	}

	private class MessageBalloon extends GridPane {

		private final double radius = 3.0 * viewFactor;

		private final String message;
		private final MessageInfo messageInfo;

		private final GridPane messagePane = new GridPane();
		private final GridPane contentArea = new GridPane();
		private final Label progressLbl = new Label();
		private final Label timeLbl;
		private final Group infoGrp = new Group();
		private final Circle waitingCircle = new Circle(radius, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(radius, Color.TRANSPARENT);
		private final Button infoBtn = ViewFactory.newInfoBtn();
		private final Button cancelBtn = ViewFactory.newCancelBtn();
		private final Button selectionBtn = ViewFactory.newSelectionBtn();

		private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
		private final BooleanProperty cancellableProperty = new SimpleBooleanProperty(false);
		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private MessageBalloon(String message, MessageInfo messageInfo) {

			super();

			this.message = message;
			this.messageInfo = messageInfo;

			timeLbl = new Label(HOUR_MIN.format(messageInfo.date));

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

			if (messageInfo.infoAvailable)
				messagePaneContainer.getChildren().add(infoBtn);

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

		void updateMessageStatus(MessageStatus messageStatus, boolean canceled) {

			activeProperty.set(!canceled);
			cancellableProperty.set(Objects.equals(messageStatus, MessageStatus.FRESH) && !canceled);

			if (Objects.equals(messageStatus, MessageStatus.FRESH))
				setProgress(-1);

			infoGrp.setVisible(!Objects.equals(messageStatus, MessageStatus.FRESH));

			if (canceled && messageInfo.isOutgoing)
				setDisable(true);

			waitingCircle.setFill(messageStatus.getWaitingColor());
			transmittedCircle.setFill(messageStatus.getTransmittedColor());

		}

		void setProgress(int progress) {

			progressLbl.setText(progress < 0 ? "" : String.format("%d%%", progress));

		}

		Node newReferenceBalloon() {

			VBox referenceBalloon = new VBox(gap);
			referenceBalloon.setPadding(new Insets(gap));

			Label nameLabel = new Label(messageInfo.name);
			nameLabel.setFont(Font.font(null, FontWeight.BOLD, nameLabel.getFont().getSize() * 0.8));
			nameLabel.setTextFill(messageInfo.nameColor);

			referenceBalloon.getChildren().add(nameLabel);

			switch (messageInfo.messageType) {

			case AUDIO:

				DmsDummyPlayer dmsDummyPlayer = new DmsDummyPlayer();

				VBox.setMargin(dmsDummyPlayer, new Insets(0.0, 0.0, 0.0, gap));

				referenceBalloon.getChildren().add(dmsDummyPlayer);

				break;

			default:

				Label messageLbl = new Label(message) {
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

		void addReferenceBalloon(Node referenceBalloon) {

			referenceBalloon.getStyleClass().add("reference-balloon");
			GridPane.setMargin(referenceBalloon, new Insets(0, 0, gap, 0));
			GridPane.setHgrow(referenceBalloon, Priority.ALWAYS);

			messagePane.add(referenceBalloon, 0, 0, 2, 1);

			referenceBalloon.toBack();

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
			initInfoGrp();
			initProgressLbl();
			initTimeLbl();
			initCancelBtn();

			messagePane.add(contentArea, 0, 1, 2, 1);
			messagePane.add(infoGrp, 0, 2, 1, 1);
			messagePane.add(progressLbl, 0, 2, 1, 1);
			messagePane.add(timeLbl, 1, 2, 1, 1);
			messagePane.add(cancelBtn, 0, 0, 2, 2);

		}

		private void initIncomingMessagePane() {

			messagePane.setBackground(new Background(
					new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0 * viewFactor), Insets.EMPTY)));

			initContentArea();
			initTimeLbl();

			GridPane.setHgrow(timeLbl, Priority.ALWAYS);

			messagePane.add(contentArea, 0, 1, 2, 1);
			messagePane.add(timeLbl, 0, 2, 1, 1);

		}

		private void initContentArea() {

			switch (messageInfo.messageType) {

			case AUDIO:

				try {

					contentArea.add(new DmsMediaPlayer(Paths.get(message)), 0, 0, 1, 1);

					break;

				} catch (Exception e) {

				}

			default:

				Label messageLbl = new Label(message);

				messageLbl.getStyleClass().add("black-label");
				messageLbl.setWrapText(true);

				contentArea.add(messageLbl, 0, 0, 1, 1);

				break;

			}

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

		private void initCancelBtn() {

			GridPane.setHalignment(cancelBtn, HPos.RIGHT);
			GridPane.setValignment(cancelBtn, VPos.TOP);

			cancelBtn.opacityProperty().bind(
					Bindings.createDoubleBinding(() -> cancelBtn.isHover() ? 1.0 : 0.5, cancelBtn.hoverProperty()));
			cancelBtn.visibleProperty()
					.bind(messagePane.hoverProperty().and(cancellableProperty).and(selectionModeProperty.not()));
			cancelBtn.managedProperty().bind(cancelBtn.visibleProperty());

		}

	}

	private class MessageGroup extends BorderPane {

		private final MessageInfo messageInfo;

		private final VBox messageBox = new VBox(smallGap);

		private MessageGroup(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			init();

		}

		private void init() {

			// init nameLabel
			if (messageInfo.isGroup && !messageInfo.isOutgoing) {

				Label nameLabel = new Label(messageInfo.name);
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

		}

		private void addMessageBalloonToBottom(MessageBalloon messageBalloon) {

			messageBox.getChildren().add(messageBalloon);

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
			} else {
				messageGroups.get(0).addMessageBalloonToTop(messageBalloon);
			}

		}

		private void addMessageBalloonToBottom(MessageBalloon messageBalloon) {

			if (messageGroups.isEmpty()
					|| !Objects.equals(messageGroups.get(messageGroups.size() - 1).messageInfo.ownerId,
							messageBalloon.messageInfo.ownerId)) {
				MessageGroup messageGroup = new MessageGroup(messageBalloon.messageInfo);
				messageGroup.addMessageBalloonToBottom(messageBalloon);
				messageGroups.add(messageGroup);
				messageGroupBox.getChildren().add(messageGroup);
			} else {
				messageGroups.get(messageGroups.size() - 1).addMessageBalloonToBottom(messageBalloon);
			}

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

	private static final class MessageInfo {

		final Long ownerId;
		final String name;
		final Date date;
		final boolean isGroup;
		final boolean isOutgoing;
		final MessageType messageType;
		final boolean infoAvailable;
		final Color nameColor;

		MessageInfo(Long ownerId, String name, Date date, boolean isGroup, boolean isOutgoing, MessageType messageType,
				Color nameColor) {

			this.ownerId = ownerId;
			this.name = name;
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

	void cancelClicked(Long messageId);

	void recordButtonPressed();

	void recordEventTriggered(Long refMessageId);

	void recordButtonReleased();

}

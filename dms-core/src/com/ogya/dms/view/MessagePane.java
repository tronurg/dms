package com.ogya.dms.view;

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

import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.ReceiverType;
import com.ogya.dms.structures.WaitStatus;
import com.ogya.dms.view.RecordButton.RecordListener;
import com.ogya.dms.view.factory.ViewFactory;
import com.sun.javafx.tk.Toolkit;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
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

	private static final double GAP = ViewFactory.GAP;
	private static final double SMALL_GAP = 2.0 * GAP / 5.0;
	private static final SimpleDateFormat HOUR_MIN = new SimpleDateFormat("HH:mm");

	private final double viewFactor = ViewFactory.getViewFactor();

	private final HBox topPane = new HBox(GAP);
	private final VBox centerPane = new VBox(2 * GAP);
	private final HBox bottomPane = new HBox(GAP);

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button backBtn = ViewFactory.newBackBtn();
	private final Circle statusCircle = new Circle(7.0 * viewFactor);
	private final Label nameLabel = new Label();
	private final TextArea messageArea = new TextArea();
	private final Button sendBtn = ViewFactory.newSendBtn();
	private final RecordButton recordBtn = new RecordButton();
	private final Button showFoldersBtn = ViewFactory.newAttachBtn();
	private final Button reportBtn = ViewFactory.newReportBtn();
	private final StackPane btnPane = new StackPane();

	private final Effect highlight = new ColorAdjust(0.8, 0.0, 0.0, 0.0);

	private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty editableProperty = new SimpleBooleanProperty(false);

	private final List<DayBox> dayBoxes = Collections.synchronizedList(new ArrayList<DayBox>());

	private final Map<Long, MessageBalloon> messageBalloons = Collections
			.synchronizedMap(new HashMap<Long, MessageBalloon>());

	private final AtomicBoolean autoScroll = new AtomicBoolean(true);

	private final AtomicReference<SimpleEntry<Node, Double>> savedNodeY = new AtomicReference<SimpleEntry<Node, Double>>(
			null);

	private final List<IMessagePane> listeners = Collections.synchronizedList(new ArrayList<IMessagePane>());

	private final AtomicLong minMessageId = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong maxMessageId = new AtomicLong(Long.MIN_VALUE);

	MessagePane() {

		super();

		init();

	}

	private void init() {

		registerListeners();

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));

		topPane.setPadding(new Insets(GAP));
		centerPane.setPadding(new Insets(GAP));
		bottomPane.setPadding(new Insets(GAP));

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

		HBox.setHgrow(messageArea, Priority.ALWAYS);

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

		HBox.setMargin(statusCircle, new Insets(GAP, GAP, GAP, 3 * GAP));
		nameLabel.getStyleClass().add("blackLabel");
		nameLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0 * viewFactor));

		topPane.getChildren().addAll(backBtn, statusCircle, nameLabel);

		initBtnPane();

		bottomPane.getChildren().addAll(messageArea, btnPane);

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

		if (messageBalloons.containsKey(message.getId()))
			return;

		boolean isOutgoing = Objects.equals(message.getMessageDirection(), MessageDirection.OUT);
		String senderName = isOutgoing ? null : message.getOwner().getName();
		Long ownerId = message.getOwner().getId();

		long messageId = message.getId();
		Date messageDate = message.getDate();

		boolean infoAvailable = !Objects.equals(message.getReceiverType(), ReceiverType.CONTACT) && isOutgoing;

		MessageInfo messageInfo = new MessageInfo(ownerId, senderName, messageDate, isOutgoing,
				message.getMessageType(), infoAvailable);

		MessageBalloon messageBalloon = newMessageBalloon(message, messageInfo);

		messageBalloons.put(messageId, messageBalloon);

		LocalDate messageDay = messageDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

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

		} else if (minMessageId.get() == messageId) {

			if (dayBoxes.isEmpty() || !Objects.equals(dayBoxes.get(0).day, messageDay)) {

				DayBox dayBox = new DayBox(messageDay);
				dayBox.addMessageBalloonToTop(messageBalloon);
				dayBoxes.add(0, dayBox);
				centerPane.getChildren().add(0, dayBox);

			} else {

				dayBoxes.get(0).addMessageBalloonToTop(messageBalloon);

			}

		}

		if (isOutgoing)
			scrollPaneToBottom();

	}

	void updateMessageStatus(Message message) {

		MessageBalloon messageBalloon = messageBalloons.get(message.getId());

		if (messageBalloon == null)
			return;

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

		scrollPane(messageBalloon, SMALL_GAP);

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

		sendBtn.setOnAction(e -> {

			final String mesajTxt = messageArea.getText().trim();

			messageArea.setText("");

			if (mesajTxt.isEmpty())
				return;

			listeners.forEach(listener -> listener.sendMessageClicked(mesajTxt));

		});

		recordBtn.addRecordListener(new RecordListener() {

			@Override
			public void recordButtonPressed() {

				listeners.forEach(listener -> listener.recordButtonPressed());

			}

			@Override
			public void recordEventTriggered() {

				listeners.forEach(listener -> listener.recordEventTriggered());

			}

			@Override
			public void recordButtonReleased() {

				listeners.forEach(listener -> listener.recordButtonReleased());

			}

		});

		showFoldersBtn.setOnAction(e -> listeners.forEach(listener -> listener.showFoldersClicked()));

		reportBtn.setOnAction(e -> listeners.forEach(listener -> listener.reportClicked()));

	}

	private void scrollPane(Node kaydirilacakNode, double bias) {

		scrollPane.applyCss();
		scrollPane.layout();

		Double centerPaneHeight = centerPane.getHeight();
		Double scrollPaneViewportHeight = scrollPane.getViewportBounds().getHeight();

		if (centerPaneHeight < scrollPaneViewportHeight)
			return;

		Double scrollY = centerPane.sceneToLocal(kaydirilacakNode.localToScene(0.0, 0.0)).getY() - bias;

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
				showFoldersBtnStart = -position * (GAP + showFoldersBtn.getHeight());
				reportBtnStart = -position * (2 * GAP + reportBtn.getHeight() + showFoldersBtn.getHeight());
				position = (position + 1) % 2;
				showFoldersBtnEnd = -position * (GAP + showFoldersBtn.getHeight());
				reportBtnEnd = -position * (2 * GAP + reportBtn.getHeight() + showFoldersBtn.getHeight());
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

		String content = message.getContent();
		if (Objects.equals(message.getMessageType(), MessageType.FILE))
			content = Paths.get(content).getFileName().toString();

		MessageBalloon messageBalloon = new MessageBalloon(content, messageInfo);
		messageBalloon.updateMessageStatus(message.getMessageStatus(),
				Objects.equals(message.getWaitStatus(), WaitStatus.CANCELED));

		if (Objects.equals(messageInfo.messageType, MessageType.FILE)) {

			messageBalloon.messagePane.cursorProperty()
					.bind(Bindings.createObjectBinding(
							() -> messageBalloon.activeProperty.get() ? Cursor.HAND : Cursor.DEFAULT,
							messageBalloon.activeProperty));

			final DropShadow dropShadow = new DropShadow();

			messageBalloon.messagePane.effectProperty().bind(Bindings.createObjectBinding(
					() -> messageBalloon.activeProperty.get() && messageBalloon.messagePane.isHover() ? dropShadow
							: null,
					messageBalloon.activeProperty, messageBalloon.messagePane.hoverProperty()));

			messageBalloon.messagePane.onMouseClickedProperty().bind(Bindings.createObjectBinding(() -> {
				if (messageBalloon.activeProperty.get())
					return e -> {
						if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
							return;
						listeners.forEach(listener -> listener.messageClicked(message.getId()));
					};
				return null;
			}, messageBalloon.activeProperty));

		}

		if (messageInfo.infoAvailable) {

			messageBalloon.infoBtn
					.setOnAction(e -> listeners.forEach(listener -> listener.infoClicked(message.getId())));

		}

		if (messageInfo.isOutgoing) {

			messageBalloon.cancelBtn
					.setOnAction(e -> listeners.forEach(listener -> listener.cancelClicked(message.getId())));

		}

		return messageBalloon;

	}

	private class MessageBalloon extends GridPane {

		private final double radius = 3.0 * viewFactor;

		private final String message;
		private final MessageInfo messageInfo;

		private final GridPane messagePane = new GridPane();
		private final GridPane messageArea = new GridPane();
		private final Label progressLbl = new Label();
		private final Label timeLbl;
		private final Group infoGrp = new Group();
		private final Circle waitingCircle = new Circle(radius, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(radius, Color.TRANSPARENT);
		private final Button infoBtn = ViewFactory.newInfoBtn();
		private final Button cancelBtn = ViewFactory.newCancelBtn();

		private final Region gap = new Region();

		private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
		private final BooleanProperty cancellableProperty = new SimpleBooleanProperty(false);

		private MessageBalloon(String message, MessageInfo messageInfo) {

			super();

			this.message = message;
			this.messageInfo = messageInfo;

			timeLbl = new Label(HOUR_MIN.format(messageInfo.date));

			init();

		}

		private void init() {

			initMessageArea();
			initMessagePane();
			initTimeLbl();

			ColumnConstraints colNarrow = new ColumnConstraints();
			ColumnConstraints colWide = new ColumnConstraints();
			colNarrow.setHgrow(Priority.ALWAYS);
			colWide.setPercentWidth(80.0);

			if (messageInfo.isOutgoing) {

				initOutgoingMessagePane();

				getColumnConstraints().addAll(colNarrow, colWide);

				messagePane.setBackground(new Background(
						new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0 * viewFactor), Insets.EMPTY)));

				GridPane.setHalignment(messagePane, HPos.RIGHT);
				GridPane.setHalignment(messageArea, HPos.RIGHT);

				add(gap, 0, 0, 1, 1);
				add(messagePane, 1, 0, 1, 1);
				if (messageInfo.infoAvailable) {
					infoBtn.setPadding(new Insets(0.0, 0.0, 0.0, GAP));
					add(infoBtn, 2, 0, 1, 1);
				}

			} else {

				initIncomingMessagePane();

				getColumnConstraints().addAll(colWide, colNarrow);

				messagePane.setBackground(new Background(
						new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0 * viewFactor), Insets.EMPTY)));

				GridPane.setHalignment(messagePane, HPos.LEFT);
				GridPane.setHalignment(messageArea, HPos.LEFT);

				add(messagePane, 0, 0, 1, 1);
				add(gap, 1, 0, 1, 1);

			}

		}

		void updateMessageStatus(MessageStatus messageStatus, boolean canceled) {

			activeProperty.set(!canceled);
			cancellableProperty.set(Objects.equals(messageStatus, MessageStatus.FRESH) && !canceled);

			if (Objects.equals(messageStatus, MessageStatus.FRESH))
				setProgress(-1);

			infoGrp.setVisible(!Objects.equals(messageStatus, MessageStatus.FRESH));

			if (canceled && messageInfo.isOutgoing)
				messageArea.setDisable(true);

			waitingCircle.setFill(messageStatus.getWaitingColor());
			transmittedCircle.setFill(messageStatus.getTransmittedColor());

		}

		void setProgress(int progress) {

			progressLbl.setText(progress < 0 ? "" : String.format("%d%%", progress));

		}

		private void initMessageArea() {

			switch (messageInfo.messageType) {

			case AUDIO:

				try {

					messageArea.add(new DmsMediaPlayer(Paths.get(message)), 0, 0, 1, 1);

					break;

				} catch (Exception e) {

				}

			default:

				Label messageLbl = new Label(message);

				messageLbl.getStyleClass().add("blackLabel");
				messageLbl.setWrapText(true);

				messageArea.add(messageLbl, 0, 0, 1, 1);

				break;

			}

		}

		private void initIncomingMessagePane() {

			messagePane.add(messageArea, 0, 0, 1, 1);
			messagePane.add(timeLbl, 0, 1, 1, 1);

		}

		private void initOutgoingMessagePane() {

			initMessagePane();
			initProgressLbl();
			initInfoGrp();
			initCancelBtn();

			messagePane.add(messageArea, 0, 0, 2, 1);
			messagePane.add(infoGrp, 0, 1, 1, 1);
			messagePane.add(progressLbl, 0, 1, 1, 1);
			messagePane.add(timeLbl, 1, 1, 1, 1);
			messagePane.add(cancelBtn, 0, 0, 2, 1);

		}

		private void initMessagePane() {

			GridPane.setFillWidth(messagePane, false);

			messagePane.setBorder(new Border(new BorderStroke(
					Objects.equals(messageInfo.messageType, MessageType.FILE) ? Color.BLUE : Color.DARKGRAY,
					BorderStrokeStyle.SOLID, new CornerRadii(10.0 * viewFactor), BorderWidths.DEFAULT)));

			messagePane.setPadding(new Insets(GAP));
			messagePane.setHgap(GAP);

		}

		private void initProgressLbl() {

			progressLbl.visibleProperty().bind(infoGrp.visibleProperty().not());

			progressLbl.setFont(Font.font(11.25 * viewFactor));
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.setMinWidth(
					Toolkit.getToolkit().getFontLoader().computeStringWidth("100%", progressLbl.getFont()));

		}

		private void initInfoGrp() {

			GridPane.setHgrow(infoGrp, Priority.ALWAYS);
			GridPane.setHalignment(infoGrp, HPos.RIGHT);

			transmittedCircle.setLayoutX(2.0 * radius);
			infoGrp.getChildren().addAll(waitingCircle, transmittedCircle);

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
			cancelBtn.visibleProperty().bind(messagePane.hoverProperty().and(cancellableProperty));

		}

	}

	private static class MessageGroup extends BorderPane {

		private final MessageInfo messageInfo;

		private final Label nameLabel;
		private final VBox messageBox = new VBox(SMALL_GAP);

		private MessageGroup(MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			nameLabel = new Label(messageInfo.name);

			init();

		}

		private void init() {

			// init nameLabel
			if (!(messageInfo.name == null || messageInfo.name.isEmpty())) {

				nameLabel.setPadding(new Insets(0.0, GAP, 0.0, GAP));
				nameLabel.setFont(Font.font(null, FontWeight.BOLD, nameLabel.getFont().getSize()));
				nameLabel.setTextFill(Color.GRAY);
				BorderPane.setAlignment(nameLabel, messageInfo.isOutgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
				BorderPane.setMargin(nameLabel, new Insets(0.0, 0.0, GAP, 0.0));

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

	private static class DayBox extends BorderPane {

		private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

		private final LocalDate day;

		private final Label dateLabel;
		private final VBox messageGroupBox = new VBox(GAP);

		private final List<MessageGroup> messageGroups = Collections.synchronizedList(new ArrayList<MessageGroup>());

		private DayBox(LocalDate day) {

			super();

			this.day = day;

			dateLabel = new Label(DAY_MONTH_YEAR.format(day));

			init();

		}

		private void init() {

			// init dateLabel
			dateLabel.setPadding(new Insets(0.0, GAP, 0.0, GAP));
			dateLabel.setFont(Font.font(null, FontWeight.BOLD, dateLabel.getFont().getSize()));
			dateLabel.setTextFill(Color.GRAY);
			dateLabel.setBackground(
					new Background(new BackgroundFill(Color.LIGHTGRAY, new CornerRadii(GAP), Insets.EMPTY)));
			dateLabel.setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
					new CornerRadii(GAP), BorderWidths.DEFAULT, Insets.EMPTY)));
			BorderPane.setAlignment(dateLabel, Pos.CENTER);
			BorderPane.setMargin(dateLabel, new Insets(0.0, 0.0, GAP, 0.0));

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

	private static class MessageInfo {

		final Long ownerId;
		final String name;
		final Date date;
		final boolean isOutgoing;
		final MessageType messageType;
		final boolean infoAvailable;

		MessageInfo(Long ownerId, String name, Date date, boolean isOutgoing, MessageType messageType,
				boolean infoAvailable) {

			this.ownerId = ownerId;
			this.name = name;
			this.date = date;
			this.isOutgoing = isOutgoing;
			this.messageType = messageType;
			this.infoAvailable = infoAvailable;

		}

	}

}

interface IMessagePane {

	void editClicked();

	void paneScrolledToTop(Long topMessageId);

	void sendMessageClicked(String message);

	void showFoldersClicked();

	void reportClicked();

	void messageClicked(Long messageId);

	void infoClicked(Long messageId);

	void cancelClicked(Long messageId);

	void recordButtonPressed();

	void recordEventTriggered();

	void recordButtonReleased();

}

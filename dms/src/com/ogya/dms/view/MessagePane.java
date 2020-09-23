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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.MessageType;
import com.ogya.dms.structures.ReceiverType;
import com.ogya.dms.structures.WaitStatus;
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

	private static final double GAP = 5.0;
	private static final double SMALL_GAP = 2.0;

	private final HBox topPane = new HBox(GAP);
	private final VBox centerPane = new VBox(2 * GAP);
	private final HBox bottomPane = new HBox(GAP);

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button backBtn = ViewFactory.newBackBtn();
	private final Circle statusCircle = new Circle(7.0);
	private final Label nameLabel = new Label();
	private final TextArea messageArea = new TextArea();
	private final Button sendBtn = ViewFactory.newSendBtn();
	private final Button showFoldersBtn = ViewFactory.newAttachBtn();
	private final StackPane btnPane = new StackPane();

	private final Effect highlight = new DropShadow(3 * GAP, Color.RED);

	private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
	private final BooleanProperty editableProperty = new SimpleBooleanProperty(false);

	private final List<DayBox> dayBoxes = Collections.synchronizedList(new ArrayList<DayBox>());

	private final Map<Long, MessageBalloon> messageBalloons = Collections
			.synchronizedMap(new HashMap<Long, MessageBalloon>());

	private final AtomicBoolean autoScroll = new AtomicBoolean(true);

	private final AtomicReference<SimpleEntry<Node, Double>> savedNodeY = new AtomicReference<SimpleEntry<Node, Double>>(
			null);

	private final List<IMessagePane> listeners = Collections.synchronizedList(new ArrayList<IMessagePane>());

	MessagePane() {

		super();

		init();

	}

	private void init() {

		registerListeners();

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		bottomPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		topPane.setPadding(new Insets(GAP));
		centerPane.setPadding(new Insets(GAP));
		bottomPane.setPadding(new Insets(GAP));

		topPane.setAlignment(Pos.CENTER_LEFT);

		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);

		nameLabel.underlineProperty().bind(Bindings.and(editableProperty, nameLabel.hoverProperty()));

		messageArea.setPrefRowCount(1);
		messageArea.setWrapText(true);
		messageArea.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 400 ? null : change));

		HBox.setHgrow(messageArea, Priority.ALWAYS);

		messageArea.setOnKeyPressed(e -> {

			if (e.getCode().equals(KeyCode.ENTER)) {

				sendBtn.fire();

				e.consume();

			}

		});

		HBox.setMargin(statusCircle, new Insets(GAP, GAP, GAP, 3 * GAP));
		nameLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0));

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

	void addMessageToTop(Message message, String senderName, MessageDirection messageDirection) {

		if (messageBalloons.containsKey(message.getId()))
			return;

		Date messageDate = message.getDate();

		boolean clickable = message.getMessageType().equals(MessageType.FILE);
		boolean infoAvailable = message.getReceiverType().equals(ReceiverType.GROUP)
				&& messageDirection.equals(MessageDirection.OUTGOING);

		MessageInfo messageInfo = new MessageInfo(message.getOwnerUuid(), senderName, messageDate, messageDirection,
				clickable, infoAvailable);

		MessageBalloon messageBalloon = newMessageBalloon(message, messageInfo);

		messageBalloons.put(message.getId(), messageBalloon);

		LocalDate messageDay = messageDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		if (dayBoxes.isEmpty() || !dayBoxes.get(0).getDay().equals(messageDay)) {

			DayBox dayBox = new DayBox(messageDay);
			dayBox.addMessageBalloonToTop(messageBalloon);
			dayBoxes.add(0, dayBox);
			centerPane.getChildren().add(0, dayBox);

		} else {

			dayBoxes.get(0).addMessageBalloonToTop(messageBalloon);

		}

		if (messageDirection.equals(MessageDirection.OUTGOING))
			scrollPaneToBottom();

	}

	void addMessageToBottom(Message message, String senderName, MessageDirection messageDirection) {

		if (messageBalloons.containsKey(message.getId()))
			return;

		Date messageDate = message.getDate();

		boolean clickable = message.getMessageType().equals(MessageType.FILE);
		boolean infoAvailable = message.getReceiverType().equals(ReceiverType.GROUP)
				&& messageDirection.equals(MessageDirection.OUTGOING);

		MessageInfo messageInfo = new MessageInfo(message.getOwnerUuid(), senderName, messageDate, messageDirection,
				clickable, infoAvailable);

		MessageBalloon messageBalloon = newMessageBalloon(message, messageInfo);

		messageBalloons.put(message.getId(), messageBalloon);

		LocalDate messageDay = messageDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		if (dayBoxes.isEmpty() || !dayBoxes.get(dayBoxes.size() - 1).getDay().equals(messageDay)) {

			DayBox dayBox = new DayBox(messageDay);
			dayBox.addMessageBalloonToBottom(messageBalloon);
			dayBoxes.add(dayBox);
			centerPane.getChildren().add(dayBox);

		} else {

			dayBoxes.get(dayBoxes.size() - 1).addMessageBalloonToBottom(messageBalloon);

		}

		if (messageDirection.equals(MessageDirection.OUTGOING))
			scrollPaneToBottom();

	}

	void updateMessageStatus(Message message) {

		MessageBalloon messageBalloon = messageBalloons.get(message.getId());

		if (messageBalloon == null)
			return;

		messageBalloon.updateMessageStatus(message.getMessageStatus(),
				message.getWaitStatus().equals(WaitStatus.CANCELED));

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

	private void registerListeners() {

		backBtn.setOnAction(e -> {
			listeners.forEach(listener -> listener.backClicked());
			backBtn.setEffect(null);
		});

		nameLabel.setOnMouseClicked(e -> {
			if (!e.getButton().equals(MouseButton.PRIMARY))
				return;
			if (editableProperty.get())
				listeners.forEach(listener -> listener.editClicked());
		});

		scrollPane.vvalueProperty().addListener((e0, e1, e2) -> {

			if (e2.doubleValue() != 0.0 || e1.doubleValue() == 0.0)
				return;

			listeners.forEach(listener -> listener.paneScrolledToTop());

		});

		sendBtn.setOnAction(e -> {

			final String mesajTxt = messageArea.getText().trim();

			messageArea.setText("");

			if (mesajTxt.isEmpty())
				return;

			listeners.forEach(listener -> listener.sendMessageClicked(mesajTxt));

		});

		showFoldersBtn.setOnAction(e -> listeners.forEach(listener -> listener.showFoldersClicked()));

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

		btnPane.getChildren().addAll(showFoldersBtn, sendBtn);

		Interpolator interpolator = Interpolator.EASE_BOTH;

		Transition transition = new Transition() {

			{
				setCycleDuration(Duration.millis(100.0));
			}

			private double start;
			private double end;
			private int direction = -1;

			@Override
			protected void interpolate(double arg0) {

				showFoldersBtn.setTranslateY(interpolator.interpolate(start, end, arg0));

			}

			@Override
			public void play() {
				start = showFoldersBtn.getTranslateY();
				end = start + (GAP + showFoldersBtn.getHeight()) * direction;
				direction = -direction;
				super.play();
			}

		};

		sendBtn.setOnMouseClicked(e -> {
			if (e.getButton().equals(MouseButton.SECONDARY))
				transition.play();
		});

		showFoldersBtn.setOnMouseClicked(e -> {
			if (e.getButton().equals(MouseButton.PRIMARY))
				transition.play();
		});

	}

	private MessageBalloon newMessageBalloon(Message message, MessageInfo messageInfo) {

		String content = message.getContent();
		if (message.getMessageType().equals(MessageType.FILE))
			content = Paths.get(content).getFileName().toString();

		MessageBalloon messageBalloon = new MessageBalloon(content, messageInfo);
		messageBalloon.updateMessageStatus(message.getMessageStatus(),
				message.getWaitStatus().equals(WaitStatus.CANCELED));

		if (messageInfo.clickable) {

			messageBalloon.getMessagePane().setCursor(Cursor.HAND);

			final DropShadow dropShadow = new DropShadow();

			messageBalloon.getMessagePane().effectProperty()
					.bind(Bindings.createObjectBinding(
							() -> messageBalloon.getMessagePane().isHover() ? dropShadow : null,
							messageBalloon.getMessagePane().hoverProperty()));

			messageBalloon.getMessagePane().setOnMouseClicked(e -> {
				if (!e.getButton().equals(MouseButton.PRIMARY))
					return;
				listeners.forEach(listener -> listener.messageClicked(message.getId()));
			});

		}

		if (messageInfo.infoAvailable) {

			messageBalloon.getInfoBtn()
					.setOnAction(e -> listeners.forEach(listener -> listener.infoClicked(message.getId())));

		}

		if (messageInfo.messageDirection.equals(MessageDirection.OUTGOING)) {

			messageBalloon.getCancelBtn()
					.setOnAction(e -> listeners.forEach(listener -> listener.cancelClicked(message.getId())));

		}

		return messageBalloon;

	}

	private static class MessageBalloon extends GridPane {

		private static final double RADIUS = 3.0;
		private static final SimpleDateFormat HOUR_MIN = new SimpleDateFormat("HH:mm");

		private final MessageInfo messageInfo;

		private final GridPane messagePane = new GridPane();
		private final Label messageLbl;
		private final Label progressLbl = new Label();
		private final Label timeLbl;
		private final Group infoGrp = new Group();
		private final Circle waitingCircle = new Circle(RADIUS, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(RADIUS, Color.TRANSPARENT);
		private final Button infoBtn = ViewFactory.newInfoBtn();
		private final Button cancelBtn = ViewFactory.newCancelBtn();

		private final Region gap = new Region();

		final BooleanProperty cancellableProperty = new SimpleBooleanProperty(false);

		MessageBalloon(String message, MessageInfo messageInfo) {

			super();

			this.messageInfo = messageInfo;

			messageLbl = new Label(message);
			timeLbl = new Label(HOUR_MIN.format(messageInfo.date));

			init();

		}

		private void init() {

			initMessagePane();
			initTimeLbl();

			ColumnConstraints colNarrow = new ColumnConstraints();
			ColumnConstraints colWide = new ColumnConstraints();
			colNarrow.setHgrow(Priority.ALWAYS);
			colWide.setPercentWidth(80.0);

			messageLbl.setWrapText(true);

			switch (messageInfo.messageDirection) {

			case INCOMING:

				initIncomingMessagePane();

				getColumnConstraints().addAll(colWide, colNarrow);

				messagePane.setBackground(
						new Background(new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0), Insets.EMPTY)));

				GridPane.setHalignment(messagePane, HPos.LEFT);
				GridPane.setHalignment(messageLbl, HPos.LEFT);

				add(messagePane, 0, 0, 1, 1);
				add(gap, 1, 0, 1, 1);

				break;

			case OUTGOING:

				initOutgoingMessagePane();

				getColumnConstraints().addAll(colNarrow, colWide);

				messagePane.setBackground(
						new Background(new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0), Insets.EMPTY)));

				GridPane.setHalignment(messagePane, HPos.RIGHT);
				GridPane.setHalignment(messageLbl, HPos.RIGHT);

				add(gap, 0, 0, 1, 1);
				add(messagePane, 1, 0, 1, 1);
				if (messageInfo.infoAvailable) {
					infoBtn.setPadding(new Insets(0.0, 0.0, 0.0, GAP));
					add(infoBtn, 2, 0, 1, 1);
				}

				break;

			}

		}

		Region getMessagePane() {

			return messagePane;

		}

		Button getInfoBtn() {

			return infoBtn;

		}

		Button getCancelBtn() {

			return cancelBtn;

		}

		void updateMessageStatus(MessageStatus messageStatus, boolean cancelled) {

			cancellableProperty.set(messageStatus.equals(MessageStatus.FRESH) && !cancelled);

			if (messageStatus.equals(MessageStatus.FRESH))
				setProgress(-1);

			infoGrp.setVisible(!messageStatus.equals(MessageStatus.FRESH));

			if (cancelled && messageInfo.messageDirection.equals(MessageDirection.OUTGOING))
				messageLbl.setTextFill(Color.DARKGRAY);

			waitingCircle.setFill(messageStatus.getWaitingColor());
			transmittedCircle.setFill(messageStatus.getTransmittedColor());

		}

		void setProgress(int progress) {

			progressLbl.setText(progress < 0 ? "" : String.format("%d%%", progress));

		}

		MessageInfo getMessageInfo() {

			return messageInfo;

		}

		private void initIncomingMessagePane() {

			messagePane.add(messageLbl, 0, 0, 1, 1);
			messagePane.add(timeLbl, 0, 1, 1, 1);

		}

		private void initOutgoingMessagePane() {

			initMessagePane();
			initProgressLbl();
			initInfoGrp();
			initCancelBtn();

			messagePane.add(messageLbl, 0, 0, 2, 1);
			messagePane.add(infoGrp, 0, 1, 1, 1);
			messagePane.add(progressLbl, 0, 1, 1, 1);
			messagePane.add(timeLbl, 1, 1, 1, 1);
			messagePane.add(cancelBtn, 0, 0, 2, 1);

		}

		private void initMessagePane() {

			GridPane.setFillWidth(messagePane, false);

			messagePane.setBorder(new Border(new BorderStroke(messageInfo.clickable ? Color.BLUE : Color.DARKGRAY,
					BorderStrokeStyle.SOLID, new CornerRadii(2 * GAP), BorderWidths.DEFAULT)));

			messagePane.setPadding(new Insets(GAP));
			messagePane.setHgap(GAP);

		}

		private void initProgressLbl() {

			progressLbl.visibleProperty().bind(infoGrp.visibleProperty().not());

			progressLbl.setFont(Font.font(messageLbl.getFont().getSize() * 0.75));
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.setMinWidth(
					Toolkit.getToolkit().getFontLoader().computeStringWidth("100%", progressLbl.getFont()));

		}

		private void initInfoGrp() {

			GridPane.setHgrow(infoGrp, Priority.ALWAYS);
			GridPane.setHalignment(infoGrp, HPos.RIGHT);

			transmittedCircle.setLayoutX(2 * RADIUS);
			infoGrp.getChildren().addAll(waitingCircle, transmittedCircle);

		}

		private void initTimeLbl() {

			timeLbl.setFont(Font.font(messageLbl.getFont().getSize() * 0.75));
			timeLbl.setTextFill(Color.DIMGRAY);

		}

		private void initCancelBtn() {

			GridPane.setHalignment(cancelBtn, HPos.RIGHT);
			GridPane.setValignment(cancelBtn, VPos.TOP);

			cancelBtn.visibleProperty().bind(messagePane.hoverProperty().and(cancellableProperty));

		}

	}

	private static class MessageGroup extends BorderPane {

		private final MessageInfo messageInfo;

		private final Label nameLabel;
		private final VBox messageBox = new VBox(SMALL_GAP);

		MessageGroup(MessageInfo messageInfo) {

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
				BorderPane.setAlignment(nameLabel,
						messageInfo.messageDirection.equals(MessageDirection.INCOMING) ? Pos.CENTER_LEFT
								: Pos.CENTER_RIGHT);
				BorderPane.setMargin(nameLabel, new Insets(0.0, 0.0, GAP, 0.0));

				setTop(nameLabel);

			}
			setCenter(messageBox);

		}

		void addMessageBalloonToTop(MessageBalloon messageBalloon) {

			messageBox.getChildren().add(0, messageBalloon);

		}

		void addMessageBalloonToBottom(MessageBalloon messageBalloon) {

			messageBox.getChildren().add(messageBalloon);

		}

		MessageInfo getMessageInfo() {

			return messageInfo;

		}

	}

	private static class DayBox extends BorderPane {

		private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

		private final LocalDate day;

		private final Label dateLabel;
		private final VBox messageGroupBox = new VBox(GAP);

		private final List<MessageGroup> messageGroups = Collections.synchronizedList(new ArrayList<MessageGroup>());

		DayBox(LocalDate day) {

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

		void addMessageBalloonToTop(MessageBalloon messageBalloon) {

			if (messageGroups.isEmpty()
					|| !messageGroups.get(0).getMessageInfo().uuid.equals(messageBalloon.getMessageInfo().uuid)) {
				MessageGroup messageGroup = new MessageGroup(messageBalloon.getMessageInfo());
				messageGroup.addMessageBalloonToTop(messageBalloon);
				messageGroups.add(0, messageGroup);
				messageGroupBox.getChildren().add(0, messageGroup);
			} else {
				messageGroups.get(0).addMessageBalloonToTop(messageBalloon);
			}

		}

		void addMessageBalloonToBottom(MessageBalloon messageBalloon) {

			if (messageGroups.isEmpty() || !messageGroups.get(messageGroups.size() - 1).getMessageInfo().uuid
					.equals(messageBalloon.getMessageInfo().uuid)) {
				MessageGroup messageGroup = new MessageGroup(messageBalloon.getMessageInfo());
				messageGroup.addMessageBalloonToBottom(messageBalloon);
				messageGroups.add(messageGroup);
				messageGroupBox.getChildren().add(messageGroup);
			} else {
				messageGroups.get(messageGroups.size() - 1).addMessageBalloonToBottom(messageBalloon);
			}

		}

		LocalDate getDay() {

			return day;

		}

	}

	private static class MessageInfo {

		final String uuid;
		final String name;
		final Date date;
		final MessageDirection messageDirection;
		final boolean clickable;
		final boolean infoAvailable;

		MessageInfo(String uuid, String name, Date date, MessageDirection messageDirection, boolean clickable,
				boolean infoAvailable) {

			this.uuid = uuid;
			this.name = name;
			this.date = date;
			this.messageDirection = messageDirection;
			this.clickable = clickable;
			this.infoAvailable = infoAvailable;

		}

	}

}

interface IMessagePane {

	void backClicked();

	void editClicked();

	void paneScrolledToTop();

	void sendMessageClicked(String message);

	void showFoldersClicked();

	void messageClicked(Long messageId);

	void infoClicked(Long messageId);

	void cancelClicked(Long messageId);

}

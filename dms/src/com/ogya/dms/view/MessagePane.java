package com.ogya.dms.view;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class MessagePane extends BorderPane {

	private static final double GAP = 5.0;

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

	private final Map<LocalDate, DayBox> dayBoxes = Collections.synchronizedMap(new HashMap<LocalDate, DayBox>());

	private final Map<Long, MessageBalloon> messageBalloons = Collections
			.synchronizedMap(new HashMap<Long, MessageBalloon>());

	private final AtomicBoolean autoScroll = new AtomicBoolean(true);

	private final AtomicReference<SimpleEntry<Node, Double>> savedNodeY = new AtomicReference<SimpleEntry<Node, Double>>(
			null);

	private final Comparator<Node> dayBoxesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof DayBox && arg1 instanceof DayBox))
				return 0;

			DayBox dayBox0 = (DayBox) arg0;
			DayBox dayBox1 = (DayBox) arg1;

			return dayBox0.getDate().compareTo(dayBox1.getDate());

		}

	};

	MessagePane() {

		super();

		init();

	}

	private void init() {

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		bottomPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		topPane.setPadding(new Insets(GAP));
		centerPane.setPadding(new Insets(GAP));
		bottomPane.setPadding(new Insets(GAP));

		topPane.setAlignment(Pos.CENTER_LEFT);

		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);

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
		bottomPane.getChildren().addAll(messageArea, sendBtn);

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

	void setStatusColor(Paint fill) {

		statusCircle.setFill(fill);

	}

	void setName(String name) {

		nameLabel.setText(name);

	}

	void addMessage(Message message, MessageDirection messageDirection) {

		if (messageBalloons.containsKey(message.getId()))
			return;

		Date messageDate = message.getDate();

		MessageBalloon messageBalloon = new MessageBalloon(message.getContent(), messageDate, messageDirection);
		messageBalloon.setMessageColors(message.getMessageStatus().getWaitingColor(),
				message.getMessageStatus().getTransmittedColor());

		messageBalloons.put(message.getId(), messageBalloon);

		LocalDate messageDay = messageDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if (!dayBoxes.containsKey(messageDay)) {
			DayBox gunKutusu = new DayBox(messageDay);
			dayBoxes.put(messageDay, gunKutusu);
			centerPane.getChildren().add(gunKutusu);
			FXCollections.sort(centerPane.getChildren(), dayBoxesSorter);
		}

		dayBoxes.get(messageDay).addMessageBalloon(messageBalloon);

		if (messageDirection.equals(MessageDirection.OUTGOING))
			scrollPaneToBottom();

	}

	void updateMessage(Message message) {

		if (!messageBalloons.containsKey(message.getId()))
			return;

		messageBalloons.get(message.getId()).setMessageColors(message.getMessageStatus().getWaitingColor(),
				message.getMessageStatus().getTransmittedColor());

	}

	void scrollPaneToMessage(Long messageId) {

		MessageBalloon messageBalloon = messageBalloons.get(messageId);

		if (messageBalloon == null) {

			scrollPaneToBottom();

			return;

		}

		scrollPane(messageBalloon, GAP);

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

	void setOnBackAction(final Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void setOnPaneScrolledToTop(final Runnable runnable) {

		scrollPane.vvalueProperty().addListener((e0, e1, e2) -> {

			if (e2.doubleValue() != 0.0 || e1.doubleValue() == 0.0)
				return;

			runnable.run();

		});

	}

	void setOnSendMessageAction(final Consumer<String> consumer) {

		sendBtn.setOnAction(e -> {

			final String mesajTxt = messageArea.getText().trim();

			messageArea.setText("");

			if (mesajTxt.isEmpty())
				return;

			consumer.accept(mesajTxt);

		});

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

	private static class MessageBalloon extends GridPane {

		private static final double RADIUS = 3.0;
		private static final SimpleDateFormat HOUR_MIN = new SimpleDateFormat("HH:mm");

		private final Date date;
		private final MessageDirection messageDirection;

		private final GridPane messagePane = new GridPane();
		private final Label messageLbl;
		private final Label timeLbl;
		private final Group infoGrp = new Group();
		private final Circle waitingCircle = new Circle(RADIUS, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(RADIUS, Color.TRANSPARENT);

		private final Region gap = new Region();

		MessageBalloon(String message, Date date, MessageDirection messageDirection) {

			super();

			this.date = date;
			this.messageDirection = messageDirection;

			messageLbl = new Label(message);
			timeLbl = new Label(HOUR_MIN.format(date));

			init();

		}

		private void init() {

			timeLbl.setFont(Font.font(messageLbl.getFont().getSize() * 0.75));
			timeLbl.setTextFill(Color.DIMGRAY);

			ColumnConstraints colNarrow = new ColumnConstraints();
			colNarrow.setPercentWidth(20.0);
			ColumnConstraints colWide = new ColumnConstraints();
			colWide.setPercentWidth(80.0);

			messageLbl.setWrapText(true);

			HBox.setHgrow(gap, Priority.ALWAYS);

			switch (messageDirection) {

			case INCOMING:

				initIncomingMessagePane();

				getColumnConstraints().addAll(colWide, colNarrow);

				messagePane.setBackground(
						new Background(new BackgroundFill(Color.PALETURQUOISE, new CornerRadii(10.0), Insets.EMPTY)));

				GridPane.setHalignment(messagePane, HPos.LEFT);
				GridPane.setHalignment(timeLbl, HPos.LEFT);

				add(messagePane, 0, 0, 1, 1);
				add(gap, 1, 0, 1, 1);

				break;

			case OUTGOING:

				initOutgoingMessagePane();

				getColumnConstraints().addAll(colNarrow, colWide);

				messagePane.setBackground(
						new Background(new BackgroundFill(Color.PALEGREEN, new CornerRadii(10.0), Insets.EMPTY)));

				GridPane.setHalignment(messagePane, HPos.RIGHT);
				GridPane.setHalignment(timeLbl, HPos.RIGHT);

				add(gap, 0, 0, 1, 1);
				add(messagePane, 1, 0, 1, 1);

				break;

			}

		}

		void setMessageColors(Paint waitingCircleColor, Paint transmittedCircleColor) {

			waitingCircle.setFill(waitingCircleColor);
			transmittedCircle.setFill(transmittedCircleColor);

		}

		Date getDate() {

			return date;

		}

		private void initIncomingMessagePane() {

			initMessagePane();

			messagePane.add(messageLbl, 0, 0, 1, 1);
			messagePane.add(timeLbl, 0, 1, 1, 1);

		}

		private void initOutgoingMessagePane() {

			initMessagePane();
			initInfoGrp();

			messagePane.add(messageLbl, 0, 0, 2, 1);
			messagePane.add(timeLbl, 0, 1, 1, 1);
			messagePane.add(infoGrp, 1, 1, 1, 1);

		}

		private void initMessagePane() {

			GridPane.setFillWidth(messagePane, false);

			GridPane.setHgrow(timeLbl, Priority.ALWAYS);

			messagePane.setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID,
					new CornerRadii(2 * GAP), BorderWidths.DEFAULT)));

			messagePane.setPadding(new Insets(GAP));
			messagePane.setHgap(GAP);

		}

		private void initInfoGrp() {

			transmittedCircle.setLayoutX(2 * RADIUS);
			infoGrp.getChildren().addAll(waitingCircle, transmittedCircle);

		}

	}

	private static class DayBox extends BorderPane {

		private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd.MM.uuuu");

		private final LocalDate date;

		private final Label dateLabel;
		private final VBox messageBox = new VBox(GAP);

		private final Comparator<Node> messageBalloonsSorter = new Comparator<Node>() {

			@Override
			public int compare(Node arg0, Node arg1) {

				if (!(arg0 instanceof MessageBalloon && arg1 instanceof MessageBalloon))
					return 0;

				MessageBalloon messageBalloon0 = (MessageBalloon) arg0;
				MessageBalloon messageBalloon1 = (MessageBalloon) arg1;

				return messageBalloon0.getDate().compareTo(messageBalloon1.getDate());

			}

		};

		DayBox(LocalDate date) {

			super();

			this.date = date;

			dateLabel = new Label(DAY_MONTH_YEAR.format(date));

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
			setCenter(messageBox);

		}

		void addMessageBalloon(MessageBalloon messageBalloon) {

			messageBox.getChildren().add(messageBalloon);

			FXCollections.sort(messageBox.getChildren(), messageBalloonsSorter);

		}

		LocalDate getDate() {

			return date;

		}

	}

}
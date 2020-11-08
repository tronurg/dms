package com.ogya.dms.view;

import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageStatus;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class ContactPane extends HBox {

	private static final double SIZE = 24.0;

	private final Group profilePicture = new Group();
	private final Circle statusCircle = new Circle(SIZE);
	private final Circle profileRound = new Circle(SIZE * 0.8);
	private final Label initialLabel = new Label();

	private final VBox middlePane = new VBox();
	private final Label nameLabel = new Label();
	private final Label commentLabel = new Label();
	private final Label coordinatesLabel = new Label();

	private final GridPane rightPane = new GridPane();
	private final Label unreadMessagesLabel = new Label() {

		@Override
		public Orientation getContentBias() {
			return Orientation.VERTICAL;
		}

		@Override
		protected double computeMinWidth(double height) {
			return height;
		}

	};

	private final MessagePane messagePane = new MessagePane();

	private final ObservableSet<Long> unreadMessages = FXCollections.observableSet(new HashSet<Long>());

	ContactPane() {

		super(5.0);

		init();

	}

	private void init() {

		initProfilePicture();
		initMiddlePane();
		initRightPane();

		getChildren().addAll(profilePicture, new Separator(Orientation.VERTICAL), middlePane, rightPane);

	}

	void updateContact(Contact contact) {

		if (contact == null)
			return;

		statusCircle.setStroke(contact.getStatus().getStatusColor());
		initialLabel.setText(contact.getName().substring(0, 1).toUpperCase());

		nameLabel.setText(contact.getName());
		commentLabel.setText(contact.getComment());
		coordinatesLabel.setText(contact.getLattitude() == null || contact.getLongitude() == null ? ""
				: String.format("(%.2f : %.2f)", contact.getLattitude(), contact.getLongitude()));

		messagePane.setStatusColor(contact.getStatus().getStatusColor());
		messagePane.setName(contact.getName());

	}

	void setOnShowMessagePane(Consumer<MessagePane> consumer) {

		setOnMouseClicked(e -> {

			if (!(Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.getClickCount() == 2
					&& e.isStillSincePress()))
				return;

			consumer.accept(messagePane);

		});

	}

	void setOnHideMessagePane(Consumer<MessagePane> consumer) {

		messagePane.setOnBackAction(() -> consumer.accept(messagePane));

	}

	void addMessagePaneListener(IMessagePane listener) {

		messagePane.addListener(listener);

	}

	MessagePane getMessagePane() {

		return messagePane;

	}

	void addMessage(Message message) {

		if (!(Objects.equals(message.getMessageDirection(), MessageDirection.OUT)
				|| Objects.equals(message.getMessageStatus(), MessageStatus.READ)))
			unreadMessages.add(message.getId());

		messagePane.addMessage(message);

	}

	void updateMessageStatus(Message message) {

		if (Objects.equals(message.getMessageStatus(), MessageStatus.READ))
			unreadMessages.remove(message.getId());

		messagePane.updateMessageStatus(message);

	}

	void updateMessageProgress(Long messageId, int progress) {

		messagePane.updateMessageProgress(messageId, progress);

	}

	void scrollPaneToMessage(Long messageId) {

		messagePane.scrollPaneToMessage(messageId);

	}

	void savePosition(Long messageId) {

		messagePane.savePosition(messageId);

	}

	void scrollToSavedPosition() {

		messagePane.scrollToSavedPosition();

	}

	void recordingStarted() {

		messagePane.recordingStarted();

	}

	void recordingStopped() {

		messagePane.recordingStopped();

	}

	String getName() {

		return nameLabel.getText();

	}

	private void initProfilePicture() {

		initStatusCircle();
		initProfileRound();
		initInitialLabel();

		profilePicture.getChildren().addAll(statusCircle, profileRound, initialLabel);

	}

	private void initStatusCircle() {

		statusCircle.setStrokeWidth(SIZE * 0.2);
		statusCircle.setFill(Color.TRANSPARENT);

	}

	private void initProfileRound() {

		profileRound.setFill(Color.DARKGRAY);

	}

	private void initInitialLabel() {

		initialLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		initialLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE));

		initialLabel.translateXProperty().bind(Bindings
				.createDoubleBinding(() -> -initialLabel.widthProperty().get() / 2, initialLabel.widthProperty()));
		initialLabel.translateYProperty().bind(Bindings
				.createDoubleBinding(() -> -initialLabel.heightProperty().get() / 2, initialLabel.heightProperty()));

	}

	private void initMiddlePane() {

		initNameLabel();
		initCommentLabel();
		initCoordinatesLabel();

		setHgrow(middlePane, Priority.ALWAYS);

		middlePane.getChildren().addAll(nameLabel, commentLabel, new Label());

	}

	private void initNameLabel() {

		nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE * 0.8));

		nameLabel.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
			String name = nameLabel.getText();
			if (name == null || name.isEmpty())
				return null;
			return new Tooltip(name);
		}, nameLabel.textProperty()));

	}

	private void initCommentLabel() {

		commentLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		commentLabel.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
			String comment = commentLabel.getText();
			if (comment == null || comment.isEmpty())
				return null;
			return new Tooltip(comment);
		}, commentLabel.textProperty()));

	}

	private void initCoordinatesLabel() {

		coordinatesLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private void initRightPane() {

		initUnreadMessagesLabel();

		GridPane.setVgrow(unreadMessagesLabel, Priority.ALWAYS);

		rightPane.add(unreadMessagesLabel, 0, 0);
		rightPane.add(new Label(), 0, 1);

	}

	private void initUnreadMessagesLabel() {

		unreadMessagesLabel.backgroundProperty()
				.bind(Bindings.createObjectBinding(
						() -> new Background(new BackgroundFill(Color.RED,
								new CornerRadii(unreadMessagesLabel.getHeight() / 2), Insets.EMPTY)),
						unreadMessagesLabel.heightProperty()));

		unreadMessagesLabel.setAlignment(Pos.CENTER);

		unreadMessagesLabel.setFont(Font.font(null, FontWeight.BOLD, unreadMessagesLabel.getFont().getSize()));
		unreadMessagesLabel.setTextFill(Color.WHITE);

		unreadMessagesLabel.visibleProperty().bind(Bindings.size(unreadMessages).greaterThan(0));
		unreadMessagesLabel.managedProperty().bind(unreadMessagesLabel.visibleProperty());
		unreadMessagesLabel.textProperty().bind(Bindings.size(unreadMessages).asString());

	}

}

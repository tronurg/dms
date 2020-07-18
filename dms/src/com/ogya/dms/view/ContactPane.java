package com.ogya.dms.view;

import java.util.HashSet;
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
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class ContactPane extends GridPane {

	private static final double SIZE = 24.0;

	private final Group profilePicture = new Group();
	private final Circle statusCircle = new Circle(SIZE);
	private final Circle profileRound = new Circle(SIZE * 0.8);
	private final Label initialLabel = new Label();

	private final Label nameLabel = new Label();
	private final Label commentLabel = new Label();
	private final Label coordinatesLabel = new Label();

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

		super();

		init();

	}

	private void init() {

		initProfilePicture();
		initStatusCircle();
		initProfileRound();
		initInitialLabel();
		initNameLabel();
		initCommentLabel();
		initCoordinatesLabel();
		initUnreadMessagesLabel();

		setHgap(5.0);
		setValignment(profilePicture, VPos.TOP);
		setHgrow(commentLabel, Priority.ALWAYS);

		add(profilePicture, 0, 0, 1, 3);
		add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		add(nameLabel, 2, 0, 1, 1);
		add(commentLabel, 2, 1, 1, 1);
		add(coordinatesLabel, 2, 2, 1, 1);
		add(unreadMessagesLabel, 3, 0, 1, 2);

	}

	void updateContact(Contact contact) {

		if (contact == null)
			return;

		statusCircle.setStroke(contact.getStatus().getStatusColor());
		initialLabel.setText(contact.getName().substring(0, 1).toUpperCase());

		nameLabel.setText(contact.getName());
		commentLabel.setText(contact.getComment());
		coordinatesLabel.setText(contact.getLattitude() == null || contact.getLongitude() == null ? ""
				: "(" + String.format("%.2f", contact.getLattitude()) + String.format("%.2f", contact.getLongitude())
						+ ")");

		messagePane.setStatusColor(contact.getStatus().getStatusColor());
		messagePane.setName(contact.getName());

	}

	void setOnShowMessagePane(Consumer<MessagePane> consumer) {

		setOnMouseClicked(e -> {

			if (!(e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2 && e.isStillSincePress()))
				return;

			consumer.accept(messagePane);

		});

	}

	void addMessagePaneListener(IMessagePane listener) {

		messagePane.addListener(listener);

	}

	MessagePane getMessagePane() {

		return messagePane;

	}

	void addMessageToTop(Message message, String senderName, MessageDirection messageDirection) {

		if (messageDirection.equals(MessageDirection.INCOMING)
				&& !message.getMessageStatus().equals(MessageStatus.READ))
			unreadMessages.add(message.getId());

		messagePane.addMessageToTop(message, senderName, messageDirection);

	}

	void addMessageToBottom(Message message, String senderName, MessageDirection messageDirection) {

		if (messageDirection.equals(MessageDirection.INCOMING)
				&& !message.getMessageStatus().equals(MessageStatus.READ))
			unreadMessages.add(message.getId());

		messagePane.addMessageToBottom(message, senderName, messageDirection);

	}

	void updateMessageStatus(Message message) {

		if (message.getMessageStatus().equals(MessageStatus.READ))
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

	private void initProfilePicture() {

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

	private void initNameLabel() {

		nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE * 0.8));

	}

	private void initCommentLabel() {

		commentLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private void initCoordinatesLabel() {

		coordinatesLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

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
		unreadMessagesLabel.textProperty().bind(Bindings.size(unreadMessages).asString());

	}

}

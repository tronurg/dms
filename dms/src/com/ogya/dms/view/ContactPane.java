package com.ogya.dms.view;

import java.util.HashSet;

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
import zmq.util.function.Consumer;

class ContactPane extends GridPane {

	private static final double SIZE = 24.0;

	private final Group profilePicture = new Group();
	private final Circle statusCircle = new Circle(SIZE);
	private final Circle profileRound = new Circle(SIZE * 0.8);
	private final Label profileLabel = new Label();

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
		initProfileLabel();
		initNameLabel();
		initCommentLabel();
		initKonumLabel();
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
		profileLabel.setText(contact.getName().substring(0, 1).toUpperCase());

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

	void setOnHideMessagePane(Consumer<MessagePane> consumer) {

		messagePane.setOnBackAction(() -> consumer.accept(messagePane));

	}

	void setOnSendMessageAction(Consumer<String> consumer) {

		messagePane.setOnSendMessageAction(messageTxt -> consumer.accept(messageTxt));

	}

	void setOnPaneScrolledToTop(Runnable runnable) {

		messagePane.setOnPaneScrolledToTop(() -> runnable.run());

	}

	void addMessage(Message message, MessageDirection messageDirection) {

		if (messageDirection.equals(MessageDirection.INCOMING)
				&& !message.getMessageStatus().equals(MessageStatus.READ))
			unreadMessages.add(message.getId());

		messagePane.addMessage(message, messageDirection);

	}

	void updateMessage(Message message) {

		if (message.getMessageStatus().equals(MessageStatus.READ))
			unreadMessages.remove(message.getId());

		messagePane.updateMessage(message);

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

		profilePicture.getChildren().addAll(statusCircle, profileRound, profileLabel);

	}

	private void initStatusCircle() {

		statusCircle.setStrokeWidth(SIZE * 0.2);
		statusCircle.setFill(Color.TRANSPARENT);

	}

	private void initProfileRound() {

		profileRound.setFill(Color.DARKGRAY);

	}

	private void initProfileLabel() {

		profileLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		profileLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE));

		profileLabel.translateXProperty().bind(Bindings
				.createDoubleBinding(() -> -profileLabel.widthProperty().get() / 2, profileLabel.widthProperty()));
		profileLabel.translateYProperty().bind(Bindings
				.createDoubleBinding(() -> -profileLabel.heightProperty().get() / 2, profileLabel.heightProperty()));

	}

	private void initNameLabel() {

		nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE * 0.8));

	}

	private void initCommentLabel() {

		commentLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private void initKonumLabel() {

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
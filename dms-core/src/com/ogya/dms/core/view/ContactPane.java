package com.ogya.dms.core.view;

import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.MessageDirection;
import com.ogya.dms.core.structures.MessageStatus;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class ContactPane extends ContactPaneBase {

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

	private final MessagePane messagePane;

	private final ObservableSet<Long> unreadMessages = FXCollections.observableSet(new HashSet<Long>());

	ContactPane(Long entityId) {

		super();

		this.messagePane = new MessagePane(entityId);

		init();

	}

	private void init() {

//		super.init();

		initRightPane();

		getChildren().add(rightPane);

	}

	@Override
	void updateContact(Contact contact) {

		super.updateContact(contact);

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

	void addUpdateMessage(Message message) {

		messagePane.addUpdateMessage(message);

		if (Objects.equals(message.getMessageDirection(), MessageDirection.OUT))
			return;

		if (Objects.equals(message.getMessageStatus(), MessageStatus.READ))
			unreadMessages.remove(message.getId());
		else
			unreadMessages.add(message.getId());

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

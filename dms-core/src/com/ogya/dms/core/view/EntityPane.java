package com.ogya.dms.core.view;

import java.util.HashSet;
import java.util.Objects;

import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.MessageStatus;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class EntityPane extends EntityPaneBase {

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

	EntityPane(EntityId entityId, BooleanProperty unreadProperty) {

		super();

		this.messagePane = new MessagePane(entityId, unreadProperty);

		init();

	}

	private void init() {

		initRightPane();

		getChildren().add(rightPane);

	}

	@Override
	void updateEntity(EntityBase entity) {

		super.updateEntity(entity);

		messagePane.setStatusColor(entity.getStatus().getStatusColor());
		messagePane.setName(entity.getName());

		if (!entity.getEntityId().isGroup())
			return;

		messagePane.setActive(!Objects.equals(entity.getStatus(), Availability.OFFLINE));
		messagePane.setEditable(Objects.equals(entity.getStatus(), Availability.AVAILABLE));

	}

	MessagePane getMessagePane() {

		return messagePane;

	}

	void addUpdateMessage(Message message) {

		messagePane.addUpdateMessage(message);

		if (message.isLocal())
			return;

		if (Objects.equals(message.getMessageStatus(), MessageStatus.READ))
			unreadMessages.remove(message.getId());
		else
			unreadMessages.add(message.getId());

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

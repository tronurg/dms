package com.ogya.dms.core.view;

import java.util.HashSet;
import java.util.Objects;

import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseButton;
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
	private final Label invisibleLbl = new Label();

	private final MessagePane messagePane;

	private final ObservableSet<Long> unreadMessages = FXCollections.observableSet(new HashSet<Long>());

	private final BooleanProperty hideableProperty = new SimpleBooleanProperty(false);
	private final BooleanProperty hiddenProperty = new SimpleBooleanProperty(false);

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

		hideableProperty.set(Objects.equals(entity.getStatus(), Availability.OFFLINE));
		hiddenProperty.set(Objects.equals(entity.getStatus(), Availability.HIDDEN));

		messagePane.setStatusColor(entity.getStatus().getStatusColor());
		messagePane.setName(entity.getName());

		if (!entity.getEntityId().isGroup())
			return;

		messagePane.setActive(!Objects.equals(entity.getStatus(), Availability.OFFLINE));
		messagePane.setEditable(Objects.equals(entity.getStatus(), Availability.AVAILABLE));

	}

	void setOnHideEntity(final Runnable runnable) {

		invisibleLbl.setOnMouseClicked(e -> {
			if (!(Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.isStillSincePress()))
				return;
			runnable.run();
			e.consume();
		});

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

	final BooleanProperty hiddenProperty() {

		return hiddenProperty;

	}

	private void initRightPane() {

		initUnreadMessagesLabel();
		initInvisibleLbl();

		rightPane.add(unreadMessagesLabel, 0, 0);
		rightPane.add(invisibleLbl, 0, 0);
		rightPane.add(new Label(), 0, 1);

	}

	private void initUnreadMessagesLabel() {

		GridPane.setVgrow(unreadMessagesLabel, Priority.ALWAYS);

		unreadMessagesLabel.backgroundProperty()
				.bind(Bindings.createObjectBinding(
						() -> new Background(new BackgroundFill(Color.RED,
								new CornerRadii(unreadMessagesLabel.getHeight() / 2), Insets.EMPTY)),
						unreadMessagesLabel.heightProperty()));

		unreadMessagesLabel.setAlignment(Pos.CENTER);

		unreadMessagesLabel.setFont(Font.font(null, FontWeight.BOLD, unreadMessagesLabel.getFont().getSize()));
		unreadMessagesLabel.setTextFill(Color.WHITE);

		unreadMessagesLabel.visibleProperty().bind(Bindings.isNotEmpty(unreadMessages));
		unreadMessagesLabel.managedProperty().bind(unreadMessagesLabel.visibleProperty());
		unreadMessagesLabel.textProperty().bind(Bindings.size(unreadMessages).asString());

	}

	private void initInvisibleLbl() {

		GridPane.setVgrow(invisibleLbl, Priority.ALWAYS);

		invisibleLbl.setGraphic(ViewFactory.newInvisibleGraph(0.65));

		final Effect colorAdjust = new ColorAdjust(0.0, -1.0, -0.5, 0.0);
		invisibleLbl.effectProperty().bind(Bindings
				.createObjectBinding(() -> invisibleLbl.isHover() ? null : colorAdjust, invisibleLbl.hoverProperty()));

		invisibleLbl.visibleProperty()
				.bind(hideableProperty.and(hoverProperty()).and(unreadMessagesLabel.visibleProperty().not()));
		invisibleLbl.managedProperty().bind(invisibleLbl.visibleProperty());

	}

}

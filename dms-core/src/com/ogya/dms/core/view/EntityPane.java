package com.ogya.dms.core.view;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class EntityPane extends EntityPaneBase {

	private final EntityId entityId;

	private final Label unreadMessagesLabel = new Label() {

		@Override
		public Orientation getContentBias() {
			return Orientation.VERTICAL;
		}

		@Override
		protected double computePrefWidth(double height) {
			return Math.max(super.computePrefWidth(height), height);
		}

	};

	private final Label invisibleLbl = new Label();

	private final AtomicLong maxMessageId = new AtomicLong(Long.MIN_VALUE);

	private final ObservableSet<Long> unreadMessages = FXCollections.observableSet(new HashSet<Long>());

	private final BooleanProperty hideableProperty = new SimpleBooleanProperty(false);
	private final BooleanProperty hiddenProperty = new SimpleBooleanProperty(false);

	EntityPane(EntityId entityId) {

		super();

		this.entityId = entityId;

		init();

	}

	private void init() {

		initUnreadMessagesLabel();
		initInvisibleLbl();

		addRightNode(invisibleLbl);
		addRightNode(unreadMessagesLabel);

	}

	@Override
	void updateEntity(EntityBase entity) {

		super.updateEntity(entity);

		hideableProperty.set(Objects.equals(entity.getStatus(), Availability.OFFLINE));
		hiddenProperty.set(Objects.equals(entity.getStatus(), Availability.HIDDEN));

	}

	void setOnHideEntityRequested(final Runnable runnable) {

		invisibleLbl.setOnMouseClicked(e -> {
			if (!(Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.isStillSincePress()))
				return;
			runnable.run();
			e.consume();
		});

	}

	EntityId getEntityId() {

		return entityId;

	}

	void updateMessageStatus(Message message) {

		Long messageId = message.getId();

		maxMessageId.set(Math.max(maxMessageId.get(), messageId));

		if (message.isLocal())
			return;

		if (Objects.equals(message.getMessageStatus(), MessageStatus.READ))
			unreadMessages.remove(messageId);
		else
			unreadMessages.add(messageId);

	}

	Long getMaxMessageId() {

		return maxMessageId.get();

	}

	final BooleanProperty hiddenProperty() {

		return hiddenProperty;

	}

	private void initUnreadMessagesLabel() {

		unreadMessagesLabel.setMinWidth(Region.USE_PREF_SIZE);

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

		invisibleLbl.setGraphic(ViewFactory.newInvisibleGraph(0.65));

		final Effect colorAdjust = new ColorAdjust(0.0, -1.0, -0.5, 0.0);
		invisibleLbl.effectProperty().bind(Bindings
				.createObjectBinding(() -> invisibleLbl.isHover() ? null : colorAdjust, invisibleLbl.hoverProperty()));

		invisibleLbl.visibleProperty()
				.bind(hideableProperty.and(hoverProperty()).and(unreadMessagesLabel.visibleProperty().not()));
		invisibleLbl.managedProperty().bind(invisibleLbl.visibleProperty());

	}

}

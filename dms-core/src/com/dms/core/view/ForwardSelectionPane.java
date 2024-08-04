package com.dms.core.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.dms.core.database.tables.EntityBase;
import com.dms.core.database.tables.EntityId;
import com.dms.core.database.tables.Message;
import com.dms.core.structures.Availability;
import com.dms.core.structures.ViewStatus;
import com.dms.core.view.component.DmsBox;
import com.dms.core.view.component.DmsScrollPane;
import com.dms.core.view.component.SearchField;
import com.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ForwardSelectionPane extends GridPane {

	private final HBox topPane = new HBox();
	private final Button backBtn;
	private final SearchField searchField = new SearchField(true);
	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new DmsScrollPane(entities);
	private final Button sendBtn = ViewFactory.newSendBtn();

	private final Map<EntityId, EntityCard> entityIdCards = Collections
			.synchronizedMap(new HashMap<EntityId, EntityCard>());

	private final Comparator<Node> entitiesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof EntityCard && arg1 instanceof EntityCard)) {
				return 0;
			}

			EntityCard card0 = (EntityCard) arg0;
			EntityCard card1 = (EntityCard) arg1;

			int comparison = Long.compare(card1.maxMessageId.get(), card0.maxMessageId.get());
			if (comparison == 0) {
				return Long.compare(card1.entityId.getId(), card0.entityId.getId());
			}

			return comparison;

		}

	};

	private final ObjectProperty<EntityId> selectedEntityIdProperty = new SimpleObjectProperty<EntityId>();

	ForwardSelectionPane(BooleanProperty unreadProperty) {
		super();
		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);
		init();
	}

	private void init() {

		parentProperty().addListener((e0, e1, e2) -> scrollPane.setVvalue(scrollPane.getVmin()));

		initTopPane();
		initEntities();
		initSendBtn();

		add(topPane, 0, 0);
		add(searchField, 0, 1);
		add(scrollPane, 0, 2);
		add(DmsBox.wrap(sendBtn, Pos.BOTTOM_RIGHT, "padding-2"), 0, 2);

	}

	void setOnBackAction(Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void setOnSendAction(Runnable runnable) {

		sendBtn.setOnAction(e -> runnable.run());

	}

	private void initTopPane() {
		topPane.getStyleClass().addAll("top-pane", "transparent-bg");
		topPane.getChildren().add(backBtn);
	}

	private void initEntities() {

		entities.getStyleClass().addAll("padding-2");

		scrollPane.getStyleClass().addAll("edge-to-edge");
		GridPane.setHgrow(scrollPane, Priority.ALWAYS);
		GridPane.setVgrow(scrollPane, Priority.ALWAYS);
		scrollPane.setFitToWidth(true);

	}

	private void initSendBtn() {

		sendBtn.disableProperty().bind(selectedEntityIdProperty.isNull());

	}

	void updateEntity(EntityBase entity) {

		EntityId entityId = entity.getEntityId();

		if (entity.getViewStatus() == ViewStatus.DELETED) {
			removeEntity(entityId);
			return;
		}

		EntityCard entityCard = getEntityCard(entityId);

		boolean active = entity.getViewStatus() == ViewStatus.DEFAULT
				&& !(entityId.isGroup() && entity.getStatus() == Availability.OFFLINE);
		if (!active && Objects.equals(selectedEntityIdProperty.get(), entityId)) {
			selectedEntityIdProperty.set(null);
		}
		entityCard.activeProperty().set(active);
		entityCard.updateEntity(entity);

	}

	private void removeEntity(EntityId entityId) {

		EntityCard entityCard = entityIdCards.remove(entityId);
		if (entityCard == null) {
			return;
		}

		entities.getChildren().remove(entityCard);

	}

	void sortEntities() {

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	void updateMessageStatus(EntityId entityId, Message message) {

		EntityCard entityCard = entityIdCards.get(entityId);

		if (entityCard != null) {
			entityCard.updateMessageStatus(message);
		}

	}

	EntityId getSelectedEntityId() {

		return selectedEntityIdProperty.get();

	}

	void resetSelection() {

		searchField.reset();
		selectedEntityIdProperty.set(null);

	}

	private EntityCard getEntityCard(final EntityId entityId) {

		EntityCard entityCard = entityIdCards.get(entityId);

		if (entityCard == null) {

			final EntityCard fEntityCard = new EntityCard(entityId);
			entityCard = fEntityCard;

			fEntityCard.visibleProperty().bind(fEntityCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchField.getText().toLowerCase(Locale.getDefault());
				return searchContactStr.isEmpty()
						|| fEntityCard.getName().toLowerCase(Locale.getDefault()).startsWith(searchContactStr);
			}, searchField.textProperty(), fEntityCard.nameProperty()))
					.and(searchField.filterOnlineProperty().not().or(fEntityCard.onlineProperty())));

			fEntityCard.managedProperty().bind(fEntityCard.visibleProperty());

			fEntityCard.setOnMouseClicked(e -> {

				EntityId selectedEntityId = selectedEntityIdProperty.get();
				if (Objects.equals(selectedEntityId, entityId)) {
					selectedEntityIdProperty.set(null);
				} else {
					selectedEntityIdProperty.set(entityId);
				}

			});

			entityIdCards.put(entityId, fEntityCard);
			entities.getChildren().add(fEntityCard);
			FXCollections.sort(entities.getChildren(), entitiesSorter);

		}

		return entityCard;

	}

	private final class EntityCard extends SelectableEntityPane {

		private final EntityId entityId;

		private final AtomicLong maxMessageId = new AtomicLong(Long.MIN_VALUE);

		private EntityCard(EntityId entityId) {
			super();
			this.entityId = entityId;
			init();
		}

		private void init() {

			selectProperty().bind(selectedEntityIdProperty.isEqualTo(entityId).and(activeProperty()));

		}

		private void updateMessageStatus(Message message) {

			maxMessageId.set(Math.max(maxMessageId.get(), message.getId()));

		}

	}

}

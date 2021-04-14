package com.ogya.dms.core.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ForwardSelectionPane extends GridPane {

	private final double gap = ViewFactory.getGap();

	private final Button backBtn;
	private final TextField searchTextField = new TextField();
	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(entities) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button sendBtn = ViewFactory.newSendBtn();

	private final Map<EntityId, EntityCard> entityIdCards = Collections
			.synchronizedMap(new HashMap<EntityId, EntityCard>());

	private final Comparator<Node> entitiesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof EntityCard && arg1 instanceof EntityCard))
				return 0;

			EntityCard card0 = (EntityCard) arg0;
			EntityCard card1 = (EntityCard) arg1;

			int comparison = Long.compare(card1.maxMessageId.get(), card0.maxMessageId.get());
			if (comparison == 0)
				return Long.compare(card1.entityId.getId(), card0.entityId.getId());

			return comparison;

		}

	};

	ForwardSelectionPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty);

		init();

	}

	private void init() {

		initBackBtn();
		initSearchTextField();
		initEntities();
		initSendBtn();

		add(backBtn, 0, 0);
		add(searchTextField, 0, 1);
		add(scrollPane, 0, 2);
		add(sendBtn, 0, 2);

	}

	void setOnBackAction(Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void setOnSendAction(Runnable runnable) {

		sendBtn.setOnAction(e -> runnable.run());

	}

	private void initBackBtn() {

		GridPane.setMargin(backBtn, new Insets(gap));

	}

	private void initSearchTextField() {

		searchTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		searchTextField.setPromptText(CommonMethods.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	private void initEntities() {

		entities.setPadding(new Insets(2 * gap));

		GridPane.setHgrow(scrollPane, Priority.ALWAYS);
		GridPane.setVgrow(scrollPane, Priority.ALWAYS);
		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setFitToWidth(true);

	}

	private void initSendBtn() {

		GridPane.setMargin(sendBtn, new Insets(2 * gap));
		GridPane.setHalignment(sendBtn, HPos.RIGHT);
		GridPane.setValignment(sendBtn, VPos.BOTTOM);

	}

	void updateEntity(EntityBase entity) {

		EntityId entityId = entity.getEntityId();

		if (Objects.equals(entity.getViewStatus(), ViewStatus.DELETED)) {
			removeEntity(entityId);
			return;
		}

		EntityCard entityCard = getEntityCard(entityId);

		entityCard.activeProperty().set(Objects.equals(entity.getViewStatus(), ViewStatus.DEFAULT)
				&& !(entityId.isGroup() && Objects.equals(entity.getStatus(), Availability.OFFLINE)));
		entityCard.updateEntity(entity);

	}

	private void removeEntity(EntityId entityId) {

		EntityCard entityCard = entityIdCards.remove(entityId);
		if (entityCard == null)
			return;

		entities.getChildren().remove(entityCard);

	}

	void sortEntities() {

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	void updateMessageStatus(EntityId entityId, Message message) {

		EntityCard entityCard = entityIdCards.get(entityId);

		if (entityCard != null)
			entityCard.updateMessageStatus(message);

	}

	EntityId getSelectedEntityId() {

		try {

			return entityIdCards.entrySet().stream().filter(entry -> entry.getValue().selectedProperty().get())
					.findAny().get().getKey();

		} catch (Exception e) {

		}

		return null;

	}

	void resetSelection() {

		sendBtn.setDisable(true);
		entityIdCards.forEach((id, card) -> card.selectedProperty().set(false));

	}

	private EntityCard getEntityCard(final EntityId entityId) {

		EntityCard entityCard = entityIdCards.get(entityId);

		if (entityCard == null) {

			final EntityCard fEntityCard = new EntityCard(entityId);
			entityCard = fEntityCard;

			fEntityCard.visibleProperty().bind(fEntityCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || fEntityCard.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty(), fEntityCard.nameProperty())));

			fEntityCard.managedProperty().bind(fEntityCard.visibleProperty());

			fEntityCard.setOnMouseClicked(e -> {

				boolean selected = !fEntityCard.selectedProperty().get();
				entityIdCards.values().forEach(card -> card.selectedProperty().set(false));
				fEntityCard.selectedProperty().set(selected);

				sendBtn.setDisable(!selected);

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

		}

		private void updateMessageStatus(Message message) {

			maxMessageId.set(Math.max(maxMessageId.get(), message.getId()));

		}

	}

}

package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.view.factory.ViewFactory;
import com.sun.javafx.scene.control.skin.ScrollPaneSkin;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class EntitiesPaneBase extends BorderPane {

	private final double gap = ViewFactory.getGap();

	private final TextField searchTextField = new TextField();
	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(entities) {
		@Override
		public void requestFocus() {
		}
	};

	private final Map<EntityId, EntityPane> entityIdPane = Collections
			.synchronizedMap(new HashMap<EntityId, EntityPane>());

	private final List<IEntitiesPane> listeners = Collections.synchronizedList(new ArrayList<IEntitiesPane>());

	private final Comparator<Node> entitiesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof EntityPane && arg1 instanceof EntityPane))
				return 0;

			EntityPane pane0 = (EntityPane) arg0;
			EntityPane pane1 = (EntityPane) arg1;

			int comparison = pane1.getMaxMessageId().compareTo(pane0.getMaxMessageId());
			if (comparison == 0)
				return Long.compare(pane1.getEntityId().getId(), pane0.getEntityId().getId());

			return comparison;

		}

	};

	EntitiesPaneBase() {

		super();

		init();

	}

	private void init() {

		initSearchTextField();

		entities.setPadding(new Insets(2 * gap));

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		scrollPane.setSkin(new ScrollPaneSkin(scrollPane) {
			@Override
			public void onTraverse(Node arg0, Bounds arg1) {

			}
		});

		setTop(searchTextField);
		setCenter(scrollPane);

	}

	void addListener(IEntitiesPane listener) {

		listeners.add(listener);

	}

	private void initSearchTextField() {

		searchTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		searchTextField.setPromptText(CommonMethods.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	void updateEntity(EntityBase entity) {

		getEntityPane(entity.getEntityId()).updateEntity(entity);

	}

	void sortEntities() {

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	public void updateMessageStatus(EntityId entityId, Message message) {

		getEntityPane(entityId).updateMessageStatus(message);

	}

	void moveEntityToTop(EntityId entityId) {

		EntityPane entityPane = getEntityPane(entityId);

		entities.getChildren().remove(entityPane);
		entities.getChildren().add(0, entityPane);

	}

	void scrollToTop() {

		scrollPane.setVvalue(scrollPane.getVmin());

	}

	private EntityPane getEntityPane(final EntityId entityId) {

		if (!entityIdPane.containsKey(entityId)) {

			final EntityPane entityPane = new EntityPane(entityId);

			entityPane.managedProperty().bind(entityPane.visibleProperty());

			entityPane.visibleProperty()
					.bind(entityPane.hiddenProperty().not().and(Bindings.createBooleanBinding(() -> {
						String searchContactStr = searchTextField.getText().toLowerCase();
						return searchContactStr.isEmpty()
								|| entityPane.getName().toLowerCase().startsWith(searchContactStr);
					}, searchTextField.textProperty())));

			entityPane.setOnMouseClicked(e -> {

				if (!(Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.getClickCount() == 2
						&& e.isStillSincePress()))
					return;

				listeners.forEach(listener -> listener.entityDoubleClicked(entityId));

			});

			entityPane.setOnHideEntityRequested(() -> {

				listeners.forEach(listener -> listener.hideEntityRequested(entityId));

			});

			entityIdPane.put(entityId, entityPane);

			entities.getChildren().add(0, entityPane);

		}

		return entityIdPane.get(entityId);

	}

}

interface IEntitiesPane {

	void entityDoubleClicked(EntityId entityId);

	void hideEntityRequested(EntityId entityId);

}

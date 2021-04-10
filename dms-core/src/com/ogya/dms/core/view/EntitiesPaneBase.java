package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.factory.ViewFactory;
import com.sun.javafx.scene.control.skin.ScrollPaneSkin;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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

			int comparison = Long.compare(pane1.maxMessageId.get(), pane0.maxMessageId.get());
			if (comparison == 0)
				return Long.compare(pane1.entityId.getId(), pane0.entityId.getId());

			return comparison;

		}

	};

	private final AtomicReference<EventTarget> lastClickedTarget = new AtomicReference<EventTarget>();

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

	void updateEntity(EntityBase entity, boolean active) {

		EntityPane entityPane = getEntityPane(entity.getEntityId());

		entityPane.activeProperty().set(active);
		entityPane.updateEntity(entity);

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

			entityPane.visibleProperty().bind(entityPane.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || entityPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty(), entityPane.nameProperty())));

			entityPane.setOnMouseClicked(e -> {
				EventTarget clickedTarget = e.getTarget();
				if (!Objects.equals(lastClickedTarget.getAndSet(clickedTarget), clickedTarget))
					return;
				if (Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.getClickCount() == 2
						&& e.isStillSincePress())
					listeners.forEach(listener -> listener.entityDoubleClicked(entityId));
			});

			entityPane.visibleLbl.setOnMouseClicked(e -> {
				if (Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.isStillSincePress())
					listeners.forEach(listener -> listener.showEntityRequested(entityId));
			});

			entityPane.invisibleLbl.setOnMouseClicked(e -> {
				if (Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.isStillSincePress())
					listeners.forEach(listener -> listener.hideEntityRequested(entityId));
			});

			entityIdPane.put(entityId, entityPane);

			entities.getChildren().add(0, entityPane);

		}

		return entityIdPane.get(entityId);

	}

	private class EntityPane extends EntityPaneBase {

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

		private final Label visibleLbl = new Label();
		private final Label invisibleLbl = new Label();

		private final AtomicLong maxMessageId = new AtomicLong(Long.MIN_VALUE);

		private final ObservableSet<Long> unreadMessages = FXCollections.observableSet();

		private final BooleanProperty hideableProperty = new SimpleBooleanProperty(false);
		private final BooleanProperty hiddenProperty = new SimpleBooleanProperty(false);

		private EntityPane(EntityId entityId) {

			super();

			this.entityId = entityId;

			init();

		}

		private void init() {

			initVisibleLbl();
			initInvisibleLbl();
			initUnreadMessagesLabel();

			addRightNode(visibleLbl);
			addRightNode(invisibleLbl);
			addRightNode(unreadMessagesLabel);

		}

		@Override
		void updateEntity(EntityBase entity) {

			super.updateEntity(entity);

			hideableProperty.set(Objects.equals(entity.getStatus(), Availability.OFFLINE)
					&& Objects.equals(entity.getViewStatus(), ViewStatus.DEFAULT));
			hiddenProperty.set(Objects.equals(entity.getViewStatus(), ViewStatus.ARCHIVED));

		}

		private void updateMessageStatus(Message message) {

			Long messageId = message.getId();

			maxMessageId.set(Math.max(maxMessageId.get(), messageId));

			if (message.isLocal())
				return;

			if (Objects.equals(message.getMessageStatus(), MessageStatus.READ))
				unreadMessages.remove(messageId);
			else
				unreadMessages.add(messageId);

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

		private void initVisibleLbl() {

			visibleLbl.setGraphic(ViewFactory.newVisibleGraph(0.65));

			final Effect colorAdjust = new ColorAdjust(0.0, -1.0, -0.5, 0.0);
			visibleLbl.effectProperty().bind(Bindings
					.createObjectBinding(() -> visibleLbl.isHover() ? null : colorAdjust, visibleLbl.hoverProperty()));

			visibleLbl.visibleProperty()
					.bind(hiddenProperty.and(hoverProperty()).and(unreadMessagesLabel.visibleProperty().not()));
			visibleLbl.managedProperty().bind(visibleLbl.visibleProperty());

		}

		private void initInvisibleLbl() {

			invisibleLbl.setGraphic(ViewFactory.newInvisibleGraph(0.65));

			final Effect colorAdjust = new ColorAdjust(0.0, -1.0, -0.5, 0.0);
			invisibleLbl.effectProperty().bind(Bindings.createObjectBinding(
					() -> invisibleLbl.isHover() ? null : colorAdjust, invisibleLbl.hoverProperty()));

			invisibleLbl.visibleProperty()
					.bind(hideableProperty.and(hoverProperty()).and(unreadMessagesLabel.visibleProperty().not()));
			invisibleLbl.managedProperty().bind(invisibleLbl.visibleProperty());

		}

	}

}

interface IEntitiesPane {

	void entityDoubleClicked(EntityId entityId);

	void showEntityRequested(EntityId entityId);

	void hideEntityRequested(EntityId entityId);

}

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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class EntitiesPaneBase extends BorderPane {

	private final double gap = ViewFactory.getGap();

	private final double viewFactor = ViewFactory.getViewFactor();

	private final TextField searchTextField = new TextField();
	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(entities) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button removeEntityBtn = new Button(CommonMethods.translate("REMOVE_COMPLETELY"));

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

	private final ObjectProperty<EntityId> entityToBeRemoved = new SimpleObjectProperty<EntityId>();

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

		initRemoveEntityBtn();

		setTop(searchTextField);
		setCenter(scrollPane);
		setBottom(removeEntityBtn);

	}

	void addListener(IEntitiesPane listener) {

		listeners.add(listener);

	}

	private void initSearchTextField() {

		searchTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		searchTextField.setPromptText(CommonMethods.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	private void initRemoveEntityBtn() {

		removeEntityBtn.setStyle("-fx-background-color: red;");
		removeEntityBtn.setTextFill(Color.ANTIQUEWHITE);
		removeEntityBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));
		removeEntityBtn.setMnemonicParsing(false);
		removeEntityBtn.setMaxWidth(Double.MAX_VALUE);
		removeEntityBtn.setMaxHeight(Double.MAX_VALUE);
		removeEntityBtn.visibleProperty().bind(entityToBeRemoved.isNotNull());
		removeEntityBtn.managedProperty().bind(removeEntityBtn.visibleProperty());

		removeEntityBtn.setOnAction(e -> {
			EntityId entityId = entityToBeRemoved.get();
			entityToBeRemoved.set(null);
			listeners.forEach(listener -> listener.removeEntityRequested(entityId));
		});

	}

	void updateEntity(EntityBase entity, boolean active) {

		EntityId entityId = entity.getEntityId();

		if (Objects.equals(entity.getViewStatus(), ViewStatus.DELETED)) {
			removeEntity(entityId);
			return;
		}

		EntityPane entityPane = getEntityPane(entityId);

		entityPane.activeProperty().set(active);
		entityPane.updateEntity(entity);

	}

	private void removeEntity(EntityId entityId) {

		EntityPane entityPane = entityIdPane.remove(entityId);
		if (entityPane == null)
			return;

		entities.getChildren().remove(entityPane);

	}

	void sortEntities() {

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	public void updateMessageStatus(EntityId entityId, Message message) {

		EntityPane entityPane = entityIdPane.get(entityId);

		if (entityPane != null)
			entityPane.updateMessageStatus(message);

	}

	void moveEntityToTop(EntityId entityId) {

		EntityPane entityPane = getEntityPane(entityId);

		entities.getChildren().remove(entityPane);
		entities.getChildren().add(0, entityPane);

	}

	void scrollToTop() {

		scrollPane.setVvalue(scrollPane.getVmin());

	}

	void resetDeleteMode() {

		entityToBeRemoved.set(null);

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
				if (!(Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.getClickCount() == 2
						&& e.isStillSincePress()))
					return;
				entityToBeRemoved.set(null);
				listeners.forEach(listener -> listener.entityDoubleClicked(entityId));
			});

			entityPane.visibleBtn.setOnAction(e -> {
				entityToBeRemoved.set(null);
				listeners.forEach(listener -> listener.showEntityRequested(entityId));
			});

			entityPane.invisibleBtn.setOnAction(e -> {
				entityToBeRemoved.set(null);
				listeners.forEach(listener -> listener.hideEntityRequested(entityId));
			});

			entityIdPane.put(entityId, entityPane);

			entities.getChildren().add(0, entityPane);

		}

		return entityIdPane.get(entityId);

	}

	private class EntityPane extends EntityPaneBase {

		private final EntityId entityId;

		private final Label unreadMessagesLbl = new Label() {

			@Override
			public Orientation getContentBias() {
				return Orientation.VERTICAL;
			}

			@Override
			protected double computePrefWidth(double height) {
				return Math.max(super.computePrefWidth(height), height);
			}

		};

		private final Button visibleBtn = ViewFactory.newVisibleBtn(0.65);
		private final Button invisibleBtn = ViewFactory.newInvisibleBtn(0.65);
		private final Button deleteBtn = ViewFactory.newDeleteBtn();

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

			initVisibleBtn();
			initInvisibleBtn();
			initDeleteBtn();
			initUnreadMessagesLbl();

			HBox btnBox = new HBox(2 * gap);
			btnBox.setAlignment(Pos.CENTER);
			btnBox.getChildren().addAll(visibleBtn, invisibleBtn, deleteBtn, unreadMessagesLbl);

			addRightNode(btnBox);

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

		private void initVisibleBtn() {

			final Effect colorAdjust = new ColorAdjust(0.0, -1.0, -0.5, 0.0);
			visibleBtn.effectProperty().bind(Bindings
					.createObjectBinding(() -> visibleBtn.isHover() ? null : colorAdjust, visibleBtn.hoverProperty()));

			visibleBtn.visibleProperty()
					.bind(hiddenProperty.and(hoverProperty()).and(unreadMessagesLbl.visibleProperty().not()));
			visibleBtn.managedProperty().bind(visibleBtn.visibleProperty());

		}

		private void initInvisibleBtn() {

			final Effect colorAdjust = new ColorAdjust(0.0, -1.0, -0.5, 0.0);
			invisibleBtn.effectProperty().bind(Bindings.createObjectBinding(
					() -> invisibleBtn.isHover() ? null : colorAdjust, invisibleBtn.hoverProperty()));

			invisibleBtn.visibleProperty()
					.bind(hideableProperty.and(hoverProperty()).and(unreadMessagesLbl.visibleProperty().not()));
			invisibleBtn.managedProperty().bind(invisibleBtn.visibleProperty());

		}

		private void initDeleteBtn() {

			final Effect dropShadow = new DropShadow();
			final Effect colorAdjust = new ColorAdjust(0.0, -1.0, -0.5, 0.0);
			deleteBtn.effectProperty().bind(Bindings.createObjectBinding(() -> {
				if (Objects.equals(entityToBeRemoved.get(), entityId))
					return dropShadow;
				if (deleteBtn.isHover())
					return null;
				return colorAdjust;
			}, entityToBeRemoved, deleteBtn.hoverProperty()));

			deleteBtn.visibleProperty().bind(entityToBeRemoved.isEqualTo(entityId)
					.or(hiddenProperty.and(hoverProperty()).and(unreadMessagesLbl.visibleProperty().not())));
			deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());

			deleteBtn.setOnAction(e -> {
				EntityId entityId = entityToBeRemoved.get();
				if (Objects.equals(entityId, this.entityId))
					entityToBeRemoved.set(null);
				else
					entityToBeRemoved.set(this.entityId);
			});

		}

		private void initUnreadMessagesLbl() {

			unreadMessagesLbl.setMinWidth(Region.USE_PREF_SIZE);

			unreadMessagesLbl.backgroundProperty()
					.bind(Bindings.createObjectBinding(
							() -> new Background(new BackgroundFill(Color.RED,
									new CornerRadii(unreadMessagesLbl.getHeight() / 2), Insets.EMPTY)),
							unreadMessagesLbl.heightProperty()));

			unreadMessagesLbl.setAlignment(Pos.CENTER);

			unreadMessagesLbl.setFont(Font.font(null, FontWeight.BOLD, unreadMessagesLbl.getFont().getSize()));
			unreadMessagesLbl.setTextFill(Color.WHITE);

			unreadMessagesLbl.visibleProperty().bind(Bindings.isNotEmpty(unreadMessages));
			unreadMessagesLbl.managedProperty().bind(unreadMessagesLbl.visibleProperty());
			unreadMessagesLbl.textProperty().bind(Bindings.size(unreadMessages).asString());

		}

	}

}

interface IEntitiesPane {

	void entityDoubleClicked(EntityId entityId);

	void showEntityRequested(EntityId entityId);

	void hideEntityRequested(EntityId entityId);

	void removeEntityRequested(EntityId entityId);

}

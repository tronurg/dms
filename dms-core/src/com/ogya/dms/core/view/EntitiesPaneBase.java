package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.component.DmsScrollPane;
import com.ogya.dms.core.view.component.DmsScrollPaneSkin;
import com.ogya.dms.core.view.component.SearchField;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.event.EventTarget;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.effect.ColorAdjust;
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
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;

class EntitiesPaneBase extends BorderPane {

	private static final double GAP = ViewFactory.GAP;
	private static final double VIEW_FACTOR = ViewFactory.VIEW_FACTOR;

	private final SearchField searchField;
	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new DmsScrollPane(entities);
	private final Popup removeEntityPopup = new Popup();
	private final Button removeEntityBtn = new Button(Commons.translate("REMOVE_COMPLETELY"));

	private final Map<EntityId, EntityPane> entityIdPane = Collections
			.synchronizedMap(new HashMap<EntityId, EntityPane>());

	private final List<IEntitiesPane> listeners = Collections.synchronizedList(new ArrayList<IEntitiesPane>());

	private final Comparator<Node> entitiesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof EntityPane && arg1 instanceof EntityPane)) {
				return 0;
			}

			EntityPane pane0 = (EntityPane) arg0;
			EntityPane pane1 = (EntityPane) arg1;

			int comparison = Long.compare(pane1.maxMessageId.get(), pane0.maxMessageId.get());
			if (comparison == 0) {
				return Long.compare(pane1.entityId.getId(), pane0.entityId.getId());
			}

			return comparison;

		}

	};

	private final AtomicReference<EventTarget> lastClickedTarget = new AtomicReference<EventTarget>();

	private final ObjectProperty<EntityId> entityToBeRemoved = new SimpleObjectProperty<EntityId>();

	EntitiesPaneBase(boolean allowFilter) {

		super();

		searchField = new SearchField(allowFilter);

		init();

	}

	private void init() {

		entities.setPadding(new Insets(2 * GAP));

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setFitToWidth(true);
		scrollPane.setSkin(new DmsScrollPaneSkin(scrollPane));

		setTop(searchField);
		setCenter(scrollPane);

		initRemoveEntityPopup();

	}

	void addListener(IEntitiesPane listener) {

		listeners.add(listener);

	}

	private void initRemoveEntityPopup() {

		removeEntityPopup.setAutoHide(true);
		removeEntityPopup.setAnchorLocation(AnchorLocation.WINDOW_TOP_RIGHT);

		removeEntityPopup.setOnHidden(e -> entityToBeRemoved.set(null));

		initRemoveEntityBtn();

		removeEntityPopup.getContent().add(removeEntityBtn);

	}

	private void initRemoveEntityBtn() {

		removeEntityBtn.setStyle("-fx-background-color: red;");
		removeEntityBtn.setTextFill(Color.ANTIQUEWHITE);
		removeEntityBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0 * VIEW_FACTOR));
		removeEntityBtn.setMnemonicParsing(false);

		removeEntityBtn.setOnAction(e -> {
			EntityId entityId = entityToBeRemoved.get();
			removeEntityPopup.hide();
			listeners.forEach(listener -> listener.removeEntityRequested(entityId));
		});

	}

	void updateEntity(EntityBase entity, boolean active) {

		EntityId entityId = entity.getEntityId();

		if (entity.getViewStatus() == ViewStatus.DELETED) {
			removeEntity(entityId);
			return;
		}

		EntityPane entityPane = getEntityPane(entityId);

		entityPane.activeProperty().set(active);
		entityPane.updateEntity(entity);

	}

	private void removeEntity(EntityId entityId) {

		EntityPane entityPane = entityIdPane.remove(entityId);
		if (entityPane == null) {
			return;
		}

		entities.getChildren().remove(entityPane);

	}

	void sortEntities() {

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	void updateMessageStatus(EntityId entityId, Message message) {

		EntityPane entityPane = entityIdPane.get(entityId);

		if (entityPane != null) {
			entityPane.updateMessageStatus(message);
		}

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
				String searchContactStr = searchField.getText().toLowerCase(Locale.getDefault());
				return searchContactStr.isEmpty()
						|| entityPane.getName().toLowerCase(Locale.getDefault()).startsWith(searchContactStr);
			}, searchField.textProperty(), entityPane.nameProperty()))
					.and(searchField.filterOnlineProperty().not().or(entityPane.onlineProperty())));

			entityPane.setOnMouseClicked(e -> {
				EventTarget clickedTarget = e.getTarget();
				if (lastClickedTarget.getAndSet(clickedTarget) != clickedTarget) {
					return;
				}
				if (!(e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && e.isStillSincePress())) {
					return;
				}
				listeners.forEach(listener -> listener.entityDoubleClicked(entityId));
			});

			entityPane.visibleBtn
					.setOnAction(e -> listeners.forEach(listener -> listener.showEntityRequested(entityId)));

			entityPane.invisibleBtn
					.setOnAction(e -> listeners.forEach(listener -> listener.hideEntityRequested(entityId)));

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

		private final Button visibleBtn = ViewFactory.newVisibleBtn();
		private final Button invisibleBtn = ViewFactory.newInvisibleBtn();
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

			HBox btnBox = new HBox(2 * GAP);
			btnBox.setAlignment(Pos.CENTER);
			btnBox.getChildren().addAll(visibleBtn, invisibleBtn, deleteBtn, unreadMessagesLbl);

			addRightNode(btnBox);

		}

		@Override
		void updateEntity(EntityBase entity) {

			super.updateEntity(entity);

			hideableProperty
					.set(entity.getStatus() == Availability.OFFLINE && entity.getViewStatus() == ViewStatus.DEFAULT);
			hiddenProperty.set(entity.getViewStatus() == ViewStatus.ARCHIVED);

		}

		private void updateMessageStatus(Message message) {

			Long messageId = message.getId();

			maxMessageId.set(Math.max(maxMessageId.get(), messageId));

			if (message.isLocal()) {
				return;
			}

			if (message.getMessageStatus() == MessageStatus.READ) {
				unreadMessages.remove(messageId);
			} else {
				unreadMessages.add(messageId);
			}

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

			final Effect colorAdjust = new ColorAdjust(0.0, -1.0, -0.5, 0.0);
			deleteBtn.effectProperty()
					.bind(Bindings.createObjectBinding(
							() -> deleteBtn.isHover() || removeEntityPopup.isShowing() ? null : colorAdjust,
							deleteBtn.hoverProperty(), removeEntityPopup.showingProperty()));

			deleteBtn.visibleProperty().bind(entityToBeRemoved.isEqualTo(entityId)
					.or(hiddenProperty.and(hoverProperty()).and(unreadMessagesLbl.visibleProperty().not())));
			deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());

			deleteBtn.setOnAction(e -> {
				entityToBeRemoved.set(entityId);
				Point2D point = deleteBtn.localToScreen(deleteBtn.getWidth(), deleteBtn.getHeight() + GAP);
				removeEntityPopup.show(deleteBtn, point.getX(), point.getY());
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

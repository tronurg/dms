package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import javax.swing.UIManager;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.impl.ContactHandleImpl;
import com.ogya.dms.core.intf.handles.impl.GroupHandleImpl;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class SelectableEntitiesPane extends BorderPane {

	private final double gap = ViewFactory.getGap();
	private final double viewFactor = ViewFactory.getViewFactor();

	private final SelectionMode selectionMode;
	private final AtomicInteger selectionLimit = new AtomicInteger(Integer.MAX_VALUE);

	private final TextField searchTextField = new TextField();

	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(entities) {
		@Override
		public void requestFocus() {
		}
	};

	private final Map<EntityId, ContactCard> idContactCards = Collections
			.synchronizedMap(new HashMap<EntityId, ContactCard>());
	private final Map<EntityId, GroupCard> idGroupCards = Collections
			.synchronizedMap(new HashMap<EntityId, GroupCard>());

	private final Map<EntityId, ObjectProperty<Color>> memberIdStatus = Collections
			.synchronizedMap(new HashMap<EntityId, ObjectProperty<Color>>());

	private final Set<EntityId> selectedEntityIds = new HashSet<EntityId>();

	private final Comparator<Node> entitiesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof EntityPaneBase && arg1 instanceof EntityPaneBase))
				return 0;

			EntityPaneBase entity0 = (EntityPaneBase) arg0;
			EntityPaneBase entity1 = (EntityPaneBase) arg1;

			return entity0.getName().toLowerCase().compareTo(entity1.getName().toLowerCase());

		}

	};

	private final Comparator<Node> membersSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof MemberCard && arg1 instanceof MemberCard))
				return 0;

			MemberCard card0 = (MemberCard) arg0;
			MemberCard card1 = (MemberCard) arg1;

			return card0.getText().toLowerCase().compareTo(card1.getText().toLowerCase());

		}

	};

	private final ObjectProperty<Predicate<ContactHandle>> contactFilterProperty = new SimpleObjectProperty<Predicate<ContactHandle>>();
	private final ObjectProperty<Predicate<GroupHandle>> groupFilterProperty = new SimpleObjectProperty<Predicate<GroupHandle>>();

	SelectableEntitiesPane(SelectionMode selectionMode) {

		super();

		this.selectionMode = selectionMode;

		init();

	}

	private void init() {

		initSearchTextField();

		entities.setPadding(new Insets(2 * gap));

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setFitToWidth(true);

		setTop(searchTextField);
		setCenter(scrollPane);

		setPadding(Insets.EMPTY);

	}

	private void initSearchTextField() {

		searchTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		searchTextField.setPromptText(CommonMethods.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	void setSelectionLimit(int limit) {

	}

	void updateContact(Contact contact) {

		getContactCard(contact.getEntityId()).updateContact(contact);

	}

	void updateGroup(Dgroup group) {

		getGroupCard(group.getEntityId()).updateGroup(group);

	}

	void updateMember(Contact member) {

		EntityId entityId = member.getEntityId();

		memberIdStatus.putIfAbsent(entityId, new SimpleObjectProperty<Color>());
		memberIdStatus.get(entityId).set(member.getStatus().getStatusColor());

	}

	public List<EntityId> getSelectedEntityIds() {

		return new ArrayList<EntityId>(selectedEntityIds);

	}

	public void setContactFilter(Predicate<ContactHandle> filter) {

		contactFilterProperty.set(filter);

	}

	public void setGroupFilter(Predicate<GroupHandle> filter) {

		groupFilterProperty.set(filter);

	}

	public void resetSelection() {

		selectedEntityIds.forEach(entityId -> {
			if (entityId.isGroup())
				getGroupCard(entityId).selectedProperty().set(false);
			else
				getContactCard(entityId).selectedProperty().set(false);
		});

	}

	public void updateUI() {

		setStyle("-panel-background: #"
				+ String.format("%6s", Integer.toHexString(
						((java.awt.Color) UIManager.get("Panel.background")).getRGB() & 0xffffff)).replace(' ', '0')
				+ ";" + "-text-fill: #" + String
						.format("%6s",
								Integer.toHexString(
										((java.awt.Color) UIManager.get("Panel.foreground")).getRGB() & 0xffffff))
						.replace(' ', '0')
				+ ";");

	}

	private ContactCard getContactCard(EntityId entityId) {

		if (!idContactCards.containsKey(entityId)) {

			final ContactCard contactCard = new ContactCard(entityId);

			contactCard.visibleProperty().bind(contactCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty()
						|| contactCard.entityPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty())));

			contactCard.managedProperty().bind(contactCard.visibleProperty());

			contactCard.setOnMouseClicked(e -> {

				switch (selectionMode) {

				case SINGLE: {

					boolean wasSelected = contactCard.selectedProperty().get();
					if (!wasSelected)
						selectedEntityIds.forEach(selectedEntityId -> {
							if (selectedEntityId.isGroup())
								getGroupCard(selectedEntityId).selectedProperty().set(false);
							else
								getContactCard(selectedEntityId).selectedProperty().set(false);
						});
					contactCard.selectedProperty().set(!wasSelected);

					break;

				}
				case MULTIPLE: {

					contactCard.selectedProperty().set(!contactCard.selectedProperty().get());

					break;

				}
				case LIMITED: {

					boolean wasSelected = contactCard.selectedProperty().get();
					if (wasSelected)
						contactCard.selectedProperty().set(false);
					else if (selectedEntityIds.size() < selectionLimit.get())
						contactCard.selectedProperty().set(true);

					break;

				}

				}

			});

			idContactCards.put(entityId, contactCard);

			entities.getChildren().add(contactCard);

			FXCollections.sort(entities.getChildren(), entitiesSorter);

		}

		return idContactCards.get(entityId);

	}

	private GroupCard getGroupCard(EntityId entityId) {

		if (!idGroupCards.containsKey(entityId)) {

			final GroupCard groupCard = new GroupCard(entityId);

			groupCard.visibleProperty().bind(groupCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty()
						|| groupCard.entityCard.entityPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty())));

			groupCard.managedProperty().bind(groupCard.visibleProperty());

			groupCard.setOnMouseClicked(e -> {

				switch (selectionMode) {

				case SINGLE: {

					boolean wasSelected = groupCard.selectedProperty().get();
					if (!wasSelected)
						selectedEntityIds.forEach(selectedEntityId -> {
							if (selectedEntityId.isGroup())
								getGroupCard(selectedEntityId).selectedProperty().set(false);
							else
								getContactCard(selectedEntityId).selectedProperty().set(false);
						});
					groupCard.selectedProperty().set(!wasSelected);

					break;

				}
				case MULTIPLE: {

					groupCard.selectedProperty().set(!groupCard.selectedProperty().get());

					break;

				}
				case LIMITED: {

					boolean wasSelected = groupCard.selectedProperty().get();
					if (wasSelected)
						groupCard.selectedProperty().set(false);
					else if (selectedEntityIds.size() < selectionLimit.get())
						groupCard.selectedProperty().set(true);

					break;

				}

				}

			});

			idGroupCards.put(entityId, groupCard);

			entities.getChildren().add(groupCard);

			FXCollections.sort(entities.getChildren(), entitiesSorter);

		}

		return idGroupCards.get(entityId);

	}

	private class EntityCard extends GridPane {

		private final EntityId entityId;

		protected final EntityPaneBase entityPane = new EntityPaneBase();
		private final Button selectionBtn = ViewFactory.newSelectionBtn();

		protected final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
		protected final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private EntityCard(EntityId entityId) {

			super();

			this.entityId = entityId;

			init();

		}

		private void init() {

			activeProperty.addListener((e0, e1, e2) -> {

				if (!e2)
					selectedProperty.set(false);

			});

			selectedProperty.addListener((e0, e1, e2) -> {

				if (e2)
					selectedEntityIds.add(entityId);
				else
					selectedEntityIds.remove(entityId);

			});

			RowConstraints row1 = new RowConstraints();
			row1.setPercentHeight(70.0);
			RowConstraints row2 = new RowConstraints();
			row2.setPercentHeight(30.0);
			getRowConstraints().addAll(row1, row2);

			GridPane.setHgrow(entityPane, Priority.ALWAYS);

			selectionBtn.opacityProperty()
					.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

			add(entityPane, 0, 0, 1, 2);
			add(selectionBtn, 1, 0, 1, 1);

		}

		protected final BooleanProperty activeProperty() {

			return activeProperty;

		}

		protected final BooleanProperty selectedProperty() {

			return selectedProperty;

		}

	}

	private final class ContactCard extends EntityCard {

		private final ObjectProperty<ContactHandle> contactHandleProperty = new SimpleObjectProperty<ContactHandle>();

		private ContactCard(EntityId entityId) {

			super(entityId);

			init();

		}

		private void init() {

			activeProperty.bind(Bindings.createBooleanBinding(() -> {

				ContactHandle contactHandle = contactHandleProperty.get();

				if (contactHandle == null)
					return false;

				boolean active = !Objects.equals(contactHandle.getAvailability(), Availability.OFFLINE);

				Predicate<ContactHandle> filter = contactFilterProperty.get();

				if (filter != null)
					active = active && filter.test(contactHandle);

				return active;

			}, contactHandleProperty, contactFilterProperty));

		}

		private void updateContact(Contact contact) {

			entityPane.updateEntity(contact);

			contactHandleProperty.set(new ContactHandleImpl(contact));

		}

	}

	private final class GroupCard extends BorderPane {

		private final EntityCard entityCard;
		private final VBox memberCards = new VBox();

		private final ObjectProperty<GroupHandle> groupHandleProperty = new SimpleObjectProperty<GroupHandle>();

		private GroupCard(EntityId entityId) {

			super();

			this.entityCard = new EntityCard(entityId);

			init();

		}

		private void init() {

			entityCard.activeProperty().bind(Bindings.createBooleanBinding(() -> {

				GroupHandle groupHandle = groupHandleProperty.get();

				if (groupHandle == null)
					return false;

				boolean active = !Objects.equals(groupHandle.getAvailability(), Availability.OFFLINE);

				Predicate<GroupHandle> filter = groupFilterProperty.get();

				if (filter != null)
					active = active && filter.test(groupHandle);

				return active;

			}, groupHandleProperty, groupFilterProperty));

			initMemberCards();

			setTop(entityCard);
			setCenter(memberCards);

		}

		private void initMemberCards() {

			memberCards.setPadding(new Insets(0.0, 0.0, 15.0 * viewFactor, 66.0 * viewFactor));
			memberCards.visibleProperty().bind(entityCard.selectedProperty());
			memberCards.managedProperty().bind(memberCards.visibleProperty());

		}

		private void updateGroup(Dgroup group) {

			entityCard.entityPane.updateEntity(group);

			groupHandleProperty.set(new GroupHandleImpl(group));

			// Update members
			Contact owner = group.getOwner();

			memberCards.getChildren().clear();

			group.getMembers().forEach(member -> {

				EntityId memberEntityId = member.getEntityId();

				if (memberEntityId.getId() == 1L)
					return;

				if (memberIdStatus.containsKey(memberEntityId))
					memberCards.getChildren().add(new MemberCard(member.getName(), memberIdStatus.get(memberEntityId)));

			});

			FXCollections.sort(memberCards.getChildren(), membersSorter);

			EntityId ownerEntityId = owner.getEntityId();

			if (ownerEntityId.getId() == 1L)
				return;

			if (memberIdStatus.containsKey(ownerEntityId))
				memberCards.getChildren().add(0, new MemberCard(owner.getName(), memberIdStatus.get(ownerEntityId)));

		}

		protected final BooleanProperty activeProperty() {

			return entityCard.activeProperty();

		}

		private final BooleanProperty selectedProperty() {

			return entityCard.selectedProperty();

		}

	}

	private final class MemberCard extends Label {

		private final ObjectProperty<Color> statusColorProperty;

		private final Circle statusCircle = new Circle(7.0 * viewFactor);

		private MemberCard(String name, ObjectProperty<Color> statusColorProperty) {

			super(name);

			this.statusColorProperty = statusColorProperty;

			init();

		}

		private void init() {

			setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));
			setGraphic(statusCircle);

			statusCircle.fillProperty().bind(statusColorProperty);

		}

	}

}

enum SelectionMode {

	SINGLE, MULTIPLE, LIMITED

}

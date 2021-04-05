package com.ogya.dms.core.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.swing.UIManager;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.intf.handles.GroupHandle;
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

public class ActiveGroupsPane extends BorderPane {

	private final double gap = ViewFactory.getGap();
	private final double viewFactor = ViewFactory.getViewFactor();

	private final TextField searchTextField = new TextField();

	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(entities) {
		@Override
		public void requestFocus() {
		}
	};

	private final Map<Long, GroupCard> idGroupCards = Collections.synchronizedMap(new HashMap<Long, GroupCard>());
	private final Map<Long, ObjectProperty<Color>> memberIdStatus = Collections
			.synchronizedMap(new HashMap<Long, ObjectProperty<Color>>());

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

	private final ObjectProperty<Predicate<GroupHandle>> groupFilterProperty = new SimpleObjectProperty<Predicate<GroupHandle>>();

	ActiveGroupsPane() {

		super();

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

	void updateGroup(Dgroup group) {

		getGroupCard(group.getId()).updateGroup(group);

	}

	void updateMember(Contact member) {

		Long id = member.getId();

		memberIdStatus.putIfAbsent(id, new SimpleObjectProperty<Color>());
		memberIdStatus.get(id).set(member.getStatus().getStatusColor());

	}

	public Long getSelectedId() {

		try {

			return idGroupCards.entrySet().stream().filter(entry -> entry.getValue().selectedProperty().get()).findAny()
					.get().getKey();

		} catch (Exception e) {

		}

		return null;

	}

	public void setGroupFilter(Predicate<GroupHandle> filter) {

		groupFilterProperty.set(filter);

	}

	public void resetSelection() {

		idGroupCards.forEach((id, card) -> card.selectedProperty().set(false));

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

	private GroupCard getGroupCard(final Long id) {

		if (!idGroupCards.containsKey(id)) {

			final GroupCard groupCard = new GroupCard();

			groupCard.visibleProperty().bind(groupCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty()
						|| groupCard.entityCard.entityPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty())));

			groupCard.managedProperty().bind(groupCard.visibleProperty());

			groupCard.entityCard.setOnMouseClicked(e -> {

				boolean selected = !groupCard.selectedProperty().get();
				idGroupCards.values().forEach(card -> card.selectedProperty().set(false));
				groupCard.selectedProperty().set(selected);

			});

			idGroupCards.put(id, groupCard);

			entities.getChildren().add(groupCard);

			FXCollections.sort(entities.getChildren(), entitiesSorter);

		}

		return idGroupCards.get(id);

	}

	private class EntityCard extends GridPane {

		protected final EntityPaneBase entityPane = new EntityPaneBase();
		private final Button selectionBtn = ViewFactory.newSelectionBtn();

		protected final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
		protected final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private EntityCard() {

			super();

			init();

		}

		private void init() {

			activeProperty.addListener((e0, e1, e2) -> {

				if (!e2)
					selectedProperty.set(false);

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

	private final class GroupCard extends BorderPane {

		private final EntityCard entityCard;
		private final VBox memberCards = new VBox();

		private final ObjectProperty<Dgroup> groupProperty = new SimpleObjectProperty<Dgroup>();

		private GroupCard() {

			super();

			this.entityCard = new EntityCard();

			init();

		}

		private void init() {

			entityCard.activeProperty().bind(Bindings.createBooleanBinding(() -> {

				Dgroup group = groupProperty.get();

				if (group == null)
					return false;

				boolean active = group.getStatus().compare(Availability.OFFLINE) > 0;

				Predicate<GroupHandle> filter = groupFilterProperty.get();

				if (filter != null)
					active = active && filter.test(new GroupHandleImpl(group));

				return active;

			}, groupProperty, groupFilterProperty));

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

			groupProperty.set(group);

			// Update members
			Contact owner = group.getOwner();

			memberCards.getChildren().clear();

			group.getMembers().forEach(member -> {

				Long memberId = member.getId();

				if (memberId == 1L)
					return;

				if (memberIdStatus.containsKey(memberId))
					memberCards.getChildren().add(new MemberCard(member.getName(), memberIdStatus.get(memberId)));

			});

			FXCollections.sort(memberCards.getChildren(), membersSorter);

			Long ownerId = owner.getId();

			if (ownerId == 1L)
				return;

			if (memberIdStatus.containsKey(ownerId))
				memberCards.getChildren().add(0, new MemberCard(owner.getName(), memberIdStatus.get(ownerId)));

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

package com.ogya.dms.core.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.UIManager;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.impl.GroupHandleImpl;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
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

		Long id = group.getId();

		if (Objects.equals(group.getViewStatus(), ViewStatus.DELETED)) {
			removeGroup(id);
			return;
		}

		getGroupCard(id).updateGroup(group);

	}

	private void removeGroup(Long id) {

		GroupCard groupCard = idGroupCards.remove(id);
		if (groupCard == null)
			return;

		entities.getChildren().remove(groupCard);

	}

	void updateMember(Contact member) {

		Long id = member.getId();

		ObjectProperty<Color> colorProperty = memberIdStatus.get(id);
		if (colorProperty == null) {
			colorProperty = new SimpleObjectProperty<Color>();
			memberIdStatus.put(id, colorProperty);
		}
		colorProperty.set(member.getStatus().getStatusColor());

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

		GroupCard groupCard = idGroupCards.get(id);

		if (groupCard == null) {

			final GroupCard fGroupCard = new GroupCard();
			groupCard = fGroupCard;

			fGroupCard.visibleProperty().bind(fGroupCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || fGroupCard.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty(), fGroupCard.nameProperty())));

			fGroupCard.managedProperty().bind(fGroupCard.visibleProperty());

			fGroupCard.setOnMouseClicked(e -> {

				boolean selected = !fGroupCard.selectedProperty().get();
				idGroupCards.values().forEach(card -> card.selectedProperty().set(false));
				fGroupCard.selectedProperty().set(selected);

			});

			idGroupCards.put(id, fGroupCard);

			entities.getChildren().add(fGroupCard);

			FXCollections.sort(entities.getChildren(), entitiesSorter);

		}

		return groupCard;

	}

	private final class GroupCard extends SelectableEntityPane {

		private final VBox memberCards = new VBox();

		private final ObjectProperty<Dgroup> groupProperty = new SimpleObjectProperty<Dgroup>();

		private GroupCard() {

			super();

			init();

		}

		private void init() {

			activeProperty().bind(Bindings.createBooleanBinding(() -> {

				Dgroup group = groupProperty.get();

				if (group == null)
					return false;

				boolean active = !Objects.equals(group.getStatus(), Availability.OFFLINE);

				Predicate<GroupHandle> filter = groupFilterProperty.get();

				if (filter != null)
					active = active && filter.test(new GroupHandleImpl(group));

				return active;

			}, groupProperty, groupFilterProperty));

			initMemberCards();

			addBottomNode(memberCards);

		}

		private void initMemberCards() {

			memberCards.setPadding(new Insets(0.0, 0.0, 15.0 * viewFactor, 0.0));
			memberCards.visibleProperty().bind(selectedProperty());
			memberCards.managedProperty().bind(memberCards.visibleProperty());

			memberCards.addEventFilter(MouseEvent.ANY, e -> e.consume());

		}

		private void updateGroup(Dgroup group) {

			updateEntity(group);

			groupProperty.set(group);

			// Update members
			Contact owner = group.getOwner();

			memberCards.getChildren().clear();

			group.getMembers().forEach(member -> {

				Long memberId = member.getId();

				if (memberId == 1L)
					return;

				ObjectProperty<Color> colorProperty = memberIdStatus.get(memberId);
				if (colorProperty == null) {
					colorProperty = new SimpleObjectProperty<Color>();
					memberIdStatus.put(memberId, colorProperty);
				}
				memberCards.getChildren().add(new MemberCard(member.getName(), colorProperty));

			});

			FXCollections.sort(memberCards.getChildren(), membersSorter);

			Long ownerId = owner.getId();

			if (ownerId == 1L)
				return;

			ObjectProperty<Color> colorProperty = memberIdStatus.get(ownerId);
			if (colorProperty == null) {
				colorProperty = new SimpleObjectProperty<Color>();
				memberIdStatus.put(ownerId, colorProperty);
			}
			memberCards.getChildren().add(0, new MemberCard(owner.getName(), colorProperty));

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

			setGraphicTextGap(gap);
			setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));
			setGraphic(statusCircle);

			statusCircle.fillProperty().bind(statusColorProperty);

		}

	}

}

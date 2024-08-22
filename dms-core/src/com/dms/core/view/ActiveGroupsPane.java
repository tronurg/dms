package com.dms.core.view;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.UIManager;

import com.dms.core.database.tables.Contact;
import com.dms.core.database.tables.Dgroup;
import com.dms.core.intf.handles.GroupHandle;
import com.dms.core.intf.handles.impl.GroupHandleImpl;
import com.dms.core.intf.tools.GroupId;
import com.dms.core.intf.tools.impl.GroupIdImpl;
import com.dms.core.structures.Availability;
import com.dms.core.structures.ViewStatus;
import com.dms.core.view.component.DmsScrollPane;
import com.dms.core.view.component.SearchField;
import com.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ActiveGroupsPane extends BorderPane {

	private final SearchField searchField = new SearchField(false);

	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new DmsScrollPane(entities);

	private final Map<GroupId, GroupCard> idGroupCards = Collections.synchronizedMap(new HashMap<GroupId, GroupCard>());
	private final Map<Long, ObjectProperty<Color>> memberIdStatus = Collections
			.synchronizedMap(new HashMap<Long, ObjectProperty<Color>>());

	private final Comparator<Node> entitiesSorter = new Comparator<Node>() {

		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof EntityPaneBase && arg1 instanceof EntityPaneBase)) {
				return 0;
			}

			EntityPaneBase entity0 = (EntityPaneBase) arg0;
			EntityPaneBase entity1 = (EntityPaneBase) arg1;

			return collator.compare(entity0.getName(), entity1.getName());

		}

	};

	private final Comparator<Node> membersSorter = new Comparator<Node>() {

		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof MemberCard && arg1 instanceof MemberCard)) {
				return 0;
			}

			MemberCard card0 = (MemberCard) arg0;
			MemberCard card1 = (MemberCard) arg1;

			return collator.compare(card0.getText(), card1.getText());

		}

	};

	private final ObjectProperty<Predicate<GroupHandle>> groupFilterProperty = new SimpleObjectProperty<Predicate<GroupHandle>>();

	private final ObjectProperty<GroupId> selectedIdProperty = new SimpleObjectProperty<GroupId>();

	ActiveGroupsPane() {
		super();
		init();
	}

	private void init() {

		entities.getStyleClass().addAll("padding-2");

		scrollPane.getStyleClass().addAll("edge-to-edge");
		scrollPane.setFitToWidth(true);

		setTop(searchField);
		setCenter(scrollPane);

		setPadding(Insets.EMPTY);

	}

	void updateGroup(Dgroup group) {

		GroupId id = GroupIdImpl.of(group);

		if (group.getViewStatus() == ViewStatus.DELETED) {
			removeGroup(id);
			return;
		}

		getGroupCard(id).updateGroup(group);

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	private void removeGroup(GroupId id) {

		GroupCard groupCard = idGroupCards.remove(id);
		if (groupCard == null) {
			return;
		}

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

	public GroupId getSelectedId() {

		return selectedIdProperty.get();

	}

	public void setGroupFilter(Predicate<GroupHandle> filter) {

		groupFilterProperty.set(filter);

	}

	public void resetSelection() {

		selectedIdProperty.set(null);

	}

	public void updateUI() {

		setStyle("-dms-background: #"
				+ String.format("%6s", Integer.toHexString(
						((java.awt.Color) UIManager.get("Panel.background")).getRGB() & 0xffffff)).replace(' ', '0')
				+ ";" + "-dms-foreground: #" + String
						.format("%6s",
								Integer.toHexString(
										((java.awt.Color) UIManager.get("Panel.foreground")).getRGB() & 0xffffff))
						.replace(' ', '0')
				+ ";");

	}

	private GroupCard getGroupCard(final GroupId id) {

		GroupCard groupCard = idGroupCards.get(id);

		if (groupCard == null) {

			final GroupCard fGroupCard = new GroupCard(id);
			groupCard = fGroupCard;

			fGroupCard.visibleProperty().bind(fGroupCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchField.getText().toLowerCase(Locale.getDefault());
				return searchContactStr.isEmpty()
						|| fGroupCard.getName().toLowerCase(Locale.getDefault()).startsWith(searchContactStr);
			}, searchField.textProperty(), fGroupCard.nameProperty())));

			fGroupCard.managedProperty().bind(fGroupCard.visibleProperty());

			fGroupCard.setOnMouseClicked(e -> {

				GroupId selectedId = selectedIdProperty.get();
				if (Objects.equals(selectedId, id)) {
					selectedIdProperty.set(null);
				} else {
					selectedIdProperty.set(id);
				}

			});

			idGroupCards.put(id, fGroupCard);

			entities.getChildren().add(fGroupCard);

		}

		return groupCard;

	}

	private final class GroupCard extends SelectableEntityPane {

		private final GroupId id;

		private final VBox memberCards = new VBox();

		private final ObjectProperty<Dgroup> groupProperty = new SimpleObjectProperty<Dgroup>();

		private GroupCard(GroupId id) {
			super();
			this.id = id;
			init();
		}

		private void init() {

			activeProperty().bind(Bindings.createBooleanBinding(() -> {

				Dgroup group = groupProperty.get();

				if (group == null) {
					return false;
				}

				boolean active = group.getStatus() != Availability.OFFLINE;

				Predicate<GroupHandle> filter = groupFilterProperty.get();

				if (filter != null) {
					active = active && filter.test(new GroupHandleImpl(group));
				}

				if (!active && Objects.equals(selectedIdProperty.get(), id)) {
					selectedIdProperty.set(null);
				}

				return active;

			}, groupProperty, groupFilterProperty));

			selectProperty().bind(selectedIdProperty.isEqualTo(id));

			initMemberCards();

			addBottomNode(memberCards);

		}

		private void initMemberCards() {

			memberCards.getStyleClass().addAll("padding-0030");
			memberCards.visibleProperty().bind(selectProperty());
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

				if (Objects.equals(memberId, 1L)) {
					return;
				}

				ObjectProperty<Color> colorProperty = memberIdStatus.get(memberId);
				if (colorProperty == null) {
					colorProperty = new SimpleObjectProperty<Color>();
					memberIdStatus.put(memberId, colorProperty);
				}
				memberCards.getChildren().add(new MemberCard(member.getName(), colorProperty));

			});

			FXCollections.sort(memberCards.getChildren(), membersSorter);

			Long ownerId = owner.getId();

			if (Objects.equals(ownerId, 1L)) {
				return;
			}

			ObjectProperty<Color> colorProperty = memberIdStatus.get(ownerId);
			if (colorProperty == null) {
				colorProperty = new SimpleObjectProperty<Color>();
				memberIdStatus.put(ownerId, colorProperty);
			}
			memberCards.getChildren().add(0, new MemberCard(owner.getName(), colorProperty));

		}

	}

	private final class MemberCard extends HBox {

		private final ObjectProperty<Color> statusColorProperty;

		private final Circle statusCircle = new Circle(7.0);
		private final Group statusCircleGraph = new Group(statusCircle);
		private final Label nameLbl = new Label();

		private MemberCard(String name, ObjectProperty<Color> statusColorProperty) {
			super();
			this.nameLbl.setText(name);
			this.statusColorProperty = statusColorProperty;
			init();
		}

		private void init() {
			getStyleClass().addAll("spacing-1");
			setAlignment(Pos.CENTER_LEFT);
			statusCircle.setStyle(ViewFactory.getScaleCss(1d, 1d));
			statusCircle.fillProperty().bind(statusColorProperty);
			nameLbl.getStyleClass().addAll("em12", "bold");
			getChildren().addAll(statusCircleGraph, nameLbl);
		}

		private String getText() {
			return nameLbl.getText();
		}

	}

}

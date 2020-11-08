package com.ogya.dms.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.UIManager;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.structures.Availability;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MyActiveGroupsPanel extends BorderPane {

	private final TextField searchTextField = new TextField();

	private final VBox groups = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(groups) {
		@Override
		public void requestFocus() {
		}
	};

	private final Map<Long, GroupBundle> idGroupBundle = Collections.synchronizedMap(new HashMap<Long, GroupBundle>());

	private final Comparator<Node> groupsSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof GroupPane && arg1 instanceof GroupPane))
				return 0;

			GroupPane group0 = (GroupPane) arg0;
			GroupPane group1 = (GroupPane) arg1;

			return group0.getName().toLowerCase().compareTo(group1.getName().toLowerCase());

		}

	};

	private final Comparator<Node> contactsSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof Card && arg1 instanceof Card))
				return 0;

			Card card0 = (Card) arg0;
			Card card1 = (Card) arg1;

			return card0.getName().toLowerCase().compareTo(card1.getName().toLowerCase());

		}

	};

	private final AtomicReference<Long> selectedId = new AtomicReference<Long>();

	private final Map<String, ObjectProperty<Color>> contactUuidStatus = Collections
			.synchronizedMap(new HashMap<String, ObjectProperty<Color>>());

	MyActiveGroupsPanel() {

		super();

		init();

	}

	private void init() {

		initSearchTextField();

		groups.setPadding(new Insets(10.0));

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

		GroupBundle groupBundle = getGroupBundle(group.getId());

		groupBundle.groupPane.updateGroup(group);

		boolean active = Objects.equals(group.getStatus(), Availability.AVAILABLE);

		groupBundle.activeProperty.set(active);

		if (!active)
			groupBundle.selectedProperty.set(false);

		if (active)
			groupBundle.setContacts(group.getMembers());

	}

	void updateContact(Contact contact) {

		contactUuidStatus.putIfAbsent(contact.getUuid(), new SimpleObjectProperty<Color>());
		contactUuidStatus.get(contact.getUuid()).set(contact.getStatus().getStatusColor());

	}

	public Long getSelectedId() {

		return selectedId.get();

	}

	public void resetSelection() {

		selectedId.set(null);

		idGroupBundle.forEach((uuid, bundle) -> bundle.selectedProperty.set(false));

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

	private GroupBundle getGroupBundle(final Long groupId) {

		if (!idGroupBundle.containsKey(groupId)) {

			final GroupBundle groupBundle = new GroupBundle();

			groupBundle.managedProperty().bind(groupBundle.visibleProperty());

			groupBundle.visibleProperty().bind(groupBundle.activeProperty.and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty()
						|| groupBundle.groupPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty())));

			groupBundle.setOnMouseClicked(e -> {

				boolean wasSelected = groupBundle.selectedProperty.get();

				idGroupBundle.forEach((uuid, bundle) -> bundle.selectedProperty.set(false));

				groupBundle.selectedProperty.set(!wasSelected);

				selectedId.set(groupBundle.selectedProperty.get() ? groupId : null);

			});

			idGroupBundle.put(groupId, groupBundle);

			groups.getChildren().add(groupBundle);

			FXCollections.sort(groups.getChildren(), groupsSorter);

		}

		return idGroupBundle.get(groupId);

	}

	private final class GroupBundle extends BorderPane {

		private final GridPane topPane = new GridPane();
		private final GroupPane groupPane = new GroupPane();
		private final Label selectionLbl = ViewFactory.newSelectionLbl();
		private final VBox contactCards = new VBox();

		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
		private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);

		private GroupBundle() {

			super();

			init();

		}

		private void init() {

			initTopPane();
			initContactCards();

			selectionLbl.visibleProperty().bind(selectedProperty);

			setTop(topPane);
			setCenter(contactCards);

		}

		private void initTopPane() {

			RowConstraints row1 = new RowConstraints();
			row1.setPercentHeight(70.0);
			RowConstraints row2 = new RowConstraints();
			row2.setPercentHeight(30.0);
			topPane.getRowConstraints().addAll(row1, row2);

			GridPane.setHgrow(groupPane, Priority.ALWAYS);

			topPane.add(groupPane, 0, 0, 1, 2);
			topPane.add(selectionLbl, 1, 0, 1, 1);
			topPane.add(contactCards, 0, 2, 2, 1);

		}

		private void initContactCards() {

			contactCards.setPadding(new Insets(0.0, 0.0, 15.0, 66.0));

			contactCards.managedProperty().bind(contactCards.visibleProperty());

			contactCards.visibleProperty().bind(selectedProperty);

		}

		private void setContacts(Set<Contact> contacts) {

			contactCards.getChildren().clear();

			contacts.forEach(contact -> {

				updateContact(contact);

				contactCards.getChildren().add(new Card(contact.getName(), contactUuidStatus.get(contact.getUuid())));

			});

			FXCollections.sort(contactCards.getChildren(), contactsSorter);

		}

	}

	private final class Card extends GridPane {

		private static final double GAP = 5.0;

		private final ObjectProperty<Color> statusColorProperty;

		private final Circle statusCircle = new Circle(7.0);
		private final Label nameLabel;

		private Card(String name, ObjectProperty<Color> statusColorProperty) {

			super();

			this.statusColorProperty = statusColorProperty;

			nameLabel = new Label(name);

			init();

		}

		private void init() {

			setHgap(GAP);

			GridPane.setHgrow(nameLabel, Priority.ALWAYS);

			initStatusCircle();
			initNameLabel();

			add(statusCircle, 0, 0, 1, 1);
			add(nameLabel, 1, 0, 1, 1);

		}

		private void initStatusCircle() {

			statusCircle.fillProperty().bind(statusColorProperty);

		}

		private void initNameLabel() {

			HBox.setHgrow(nameLabel, Priority.ALWAYS);
			nameLabel.setFont(Font.font(null, FontWeight.BOLD, 18.0));

		}

		private String getName() {

			return nameLabel.getText();

		}

	}

}

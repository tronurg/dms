package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.UIManager;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.structures.Availability;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

public class OnlineContactsPanel extends BorderPane {

	private final TextField searchTextField = new TextField();

	private final VBox contacts = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(contacts) {
		@Override
		public void requestFocus() {
		}
	};

	private final Map<String, ContactBundle> uuidContactBundle = Collections
			.synchronizedMap(new HashMap<String, ContactBundle>());

	private final Comparator<Node> contactsSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof ContactPane && arg1 instanceof ContactPane))
				return 0;

			ContactPane contact0 = (ContactPane) arg0;
			ContactPane contact1 = (ContactPane) arg1;

			return contact0.getName().toLowerCase().compareTo(contact1.getName().toLowerCase());

		}

	};

	private final List<String> selectedUuids = Collections.synchronizedList(new ArrayList<String>());

	public OnlineContactsPanel() {

		super();

		init();

	}

	private void init() {

		initSearchTextField();

		contacts.setPadding(new Insets(10.0));

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

	void updateContact(Contact contact) {

		ContactBundle contactBundle = getContactBundle(contact.getUuid());

		contactBundle.contactPane.updateContact(contact);

		boolean active = !Objects.equals(contact.getStatus(), Availability.OFFLINE);

		contactBundle.activeProperty.set(active);

		if (!active)
			contactBundle.selectedProperty.set(false);

	}

	public List<String> getSelectedUuids() {

		return new ArrayList<String>(selectedUuids);

	}

	public void resetSelection() {

		selectedUuids.clear();

		uuidContactBundle.forEach((uuid, bundle) -> bundle.selectedProperty.set(false));

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

	private ContactBundle getContactBundle(final String uuid) {

		if (!uuidContactBundle.containsKey(uuid)) {

			final ContactBundle contactBundle = new ContactBundle();

			contactBundle.managedProperty().bind(contactBundle.visibleProperty());

			contactBundle.visibleProperty().bind(contactBundle.activeProperty.and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty()
						|| contactBundle.contactPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty())));

			contactBundle.setOnMouseClicked(e -> {

				contactBundle.selectedProperty.set(!contactBundle.selectedProperty.get());

				if (contactBundle.selectedProperty.get())
					selectedUuids.add(uuid);
				else
					selectedUuids.remove(uuid);

			});

			uuidContactBundle.put(uuid, contactBundle);

			contacts.getChildren().add(contactBundle);

			FXCollections.sort(contacts.getChildren(), contactsSorter);

		}

		return uuidContactBundle.get(uuid);

	}

	private final class ContactBundle extends GridPane {

		private final ContactPane contactPane = new ContactPane();
		private final Label selectionLbl = ViewFactory.newSelectionLbl();
		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
		private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);

		private ContactBundle() {

			RowConstraints row1 = new RowConstraints();
			row1.setPercentHeight(70.0);
			RowConstraints row2 = new RowConstraints();
			row2.setPercentHeight(30.0);
			getRowConstraints().addAll(row1, row2);

			setHgrow(contactPane, Priority.ALWAYS);

			selectionLbl.visibleProperty().bind(selectedProperty);

			add(contactPane, 0, 0, 1, 2);
			add(selectionLbl, 1, 0, 1, 1);

		}

	}

}

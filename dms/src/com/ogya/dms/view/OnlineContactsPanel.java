package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.UIManager;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.structures.Availability;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class OnlineContactsPanel extends BorderPane {

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

		contacts.setPadding(new Insets(10.0));

		scrollPane.setFitToWidth(true);

		setCenter(scrollPane);

		setPadding(Insets.EMPTY);

	}

	public void updateContact(Contact contact) {

		ContactBundle contactBundle = getContactBundle(contact.getUuid());

		contactBundle.contactPane.updateContact(contact);

		boolean visible = !Objects.equals(contact.getStatus(), Availability.OFFLINE);

		contactBundle.setVisible(visible);

		if (!visible)
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

	private final class ContactBundle extends HBox {

		private final ContactPane contactPane = new ContactPane();
		private final Label selectionLbl = ViewFactory.newSelectionLbl();
		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private ContactBundle() {

			HBox.setHgrow(contactPane, Priority.ALWAYS);

			selectionLbl.visibleProperty().bind(selectedProperty);

			getChildren().addAll(contactPane, selectionLbl);

		}

	}

}

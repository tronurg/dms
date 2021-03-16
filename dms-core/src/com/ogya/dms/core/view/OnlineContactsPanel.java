package com.ogya.dms.core.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.UIManager;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.impl.ContactHandleImpl;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

public class OnlineContactsPanel extends BorderPane {

	private final double gap = ViewFactory.getGap();

	private final TextField searchTextField = new TextField();

	private final VBox contacts = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(contacts) {
		@Override
		public void requestFocus() {
		}
	};

	private final Map<Long, ContactBundle> idContactBundle = Collections
			.synchronizedMap(new HashMap<Long, ContactBundle>());

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

	private final ObjectProperty<Predicate<ContactHandle>> filterProperty = new SimpleObjectProperty<Predicate<ContactHandle>>();

	OnlineContactsPanel() {

		super();

		init();

	}

	private void init() {

		initSearchTextField();

		contacts.setPadding(new Insets(2 * gap));

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

		ContactBundle contactBundle = getContactBundle(contact.getId());

		contactBundle.setContact(contact);

	}

	public List<Long> getSelectedIds() {

		return idContactBundle.entrySet().stream().filter(entry -> entry.getValue().selectedProperty.get())
				.map(entry -> entry.getKey()).collect(Collectors.toList());

	}

	public void setFilter(Predicate<ContactHandle> filter) {

		filterProperty.set(filter);

	}

	public void resetSelection() {

		idContactBundle.forEach((uuid, bundle) -> bundle.selectedProperty.set(false));

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

	private ContactBundle getContactBundle(final Long id) {

		if (!idContactBundle.containsKey(id)) {

			final ContactBundle contactBundle = new ContactBundle();

			contactBundle.managedProperty().bind(contactBundle.visibleProperty());

			contactBundle.visibleProperty().bind(contactBundle.activeProperty.and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty()
						|| contactBundle.contactPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty())));

			contactBundle
					.setOnMouseClicked(e -> contactBundle.selectedProperty.set(!contactBundle.selectedProperty.get()));

			idContactBundle.put(id, contactBundle);

			contacts.getChildren().add(contactBundle);

			FXCollections.sort(contacts.getChildren(), contactsSorter);

		}

		return idContactBundle.get(id);

	}

	private final class ContactBundle extends GridPane {

		private final ContactPane contactPane = new ContactPane();
		private final Button selectionBtn = ViewFactory.newSelectionBtn();

		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);
		private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);

		private final ObjectProperty<Contact> contactProperty = new SimpleObjectProperty<Contact>();

		private ContactBundle() {

			RowConstraints row1 = new RowConstraints();
			row1.setPercentHeight(70.0);
			RowConstraints row2 = new RowConstraints();
			row2.setPercentHeight(30.0);
			getRowConstraints().addAll(row1, row2);

			setHgrow(contactPane, Priority.ALWAYS);

			selectionBtn.opacityProperty()
					.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

			activeProperty.addListener((e0, e1, e2) -> {

				if (!e2)
					selectedProperty.set(false);

			});

			activeProperty.bind(Bindings.createBooleanBinding(() -> {

				Contact contact = contactProperty.get();

				if (contact == null)
					return false;

				boolean active = !Objects.equals(contact.getStatus(), Availability.OFFLINE);

				Predicate<ContactHandle> filter = filterProperty.get();

				if (filter != null)
					active = active && filter.test(new ContactHandleImpl(contact));

				return active;

			}, contactProperty, filterProperty));

			add(contactPane, 0, 0, 1, 2);
			add(selectionBtn, 1, 0, 1, 1);

		}

		private void setContact(Contact contact) {

			contactPane.updateContact(contact);

			contactProperty.set(contact);

		}

	}

}

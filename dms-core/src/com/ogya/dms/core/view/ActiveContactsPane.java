package com.ogya.dms.core.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ActiveContactsPane extends BorderPane {

	private final double gap = ViewFactory.getGap();

	private final TextField searchTextField = new TextField();

	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(entities) {
		@Override
		public void requestFocus() {
		}
	};

	private final Map<Long, ContactCard> idContactCards = Collections.synchronizedMap(new HashMap<Long, ContactCard>());

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

	private final ObjectProperty<Predicate<ContactHandle>> contactFilterProperty = new SimpleObjectProperty<Predicate<ContactHandle>>();

	ActiveContactsPane() {

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

	void setSelectionLimit(int limit) {

	}

	void updateContact(Contact contact) {

		getContactCard(contact.getId()).updateContact(contact);

	}

	public List<Long> getSelectedEntityIds() {

		return idContactCards.entrySet().stream().filter(entry -> entry.getValue().selectedProperty().get())
				.map(entry -> entry.getKey()).collect(Collectors.toList());

	}

	public void setContactFilter(Predicate<ContactHandle> filter) {

		contactFilterProperty.set(filter);

	}

	public void resetSelection() {

		idContactCards.forEach((id, card) -> card.selectedProperty().set(false));

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

	private ContactCard getContactCard(final Long id) {

		if (!idContactCards.containsKey(id)) {

			final ContactCard contactCard = new ContactCard();

			contactCard.visibleProperty().bind(contactCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty()
						|| contactCard.entityPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty())));

			contactCard.managedProperty().bind(contactCard.visibleProperty());

			contactCard
					.setOnMouseClicked(e -> contactCard.selectedProperty().set(!contactCard.selectedProperty().get()));

			idContactCards.put(id, contactCard);

			entities.getChildren().add(contactCard);

			FXCollections.sort(entities.getChildren(), entitiesSorter);

		}

		return idContactCards.get(id);

	}

	private final class ContactCard extends EntityCard {

		private final ObjectProperty<Contact> contactProperty = new SimpleObjectProperty<Contact>();

		private ContactCard() {

			super();

			init();

		}

		private void init() {

			activeProperty.bind(Bindings.createBooleanBinding(() -> {

				Contact contact = contactProperty.get();

				if (contact == null)
					return false;

				boolean active = contact.getStatus().compare(Availability.OFFLINE) > 0;

				Predicate<ContactHandle> filter = contactFilterProperty.get();

				if (filter != null)
					active = active && filter.test(new ContactHandleImpl(contact));

				return active;

			}, contactProperty, contactFilterProperty));

		}

		private void updateContact(Contact contact) {

			entityPane.updateEntity(contact);

			contactProperty.set(contact);

		}

	}

}

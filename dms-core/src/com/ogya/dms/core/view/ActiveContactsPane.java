package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.UIManager;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.intf.handles.ContactHandle;
import com.ogya.dms.core.intf.handles.impl.ContactHandleImpl;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.component.SearchField;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ActiveContactsPane extends BorderPane {

	private final double gap = ViewFactory.getGap();

	private final SearchField searchField = new SearchField(false);

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

	private final ObservableSet<Long> selectedIds = FXCollections.observableSet();

	ActiveContactsPane() {

		super();

		init();

	}

	private void init() {

		entities.setPadding(new Insets(2 * gap));

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setFitToWidth(true);

		setTop(searchField);
		setCenter(scrollPane);

		setPadding(Insets.EMPTY);

	}

	void updateContact(Contact contact) {

		Long id = contact.getId();

		if (Objects.equals(contact.getViewStatus(), ViewStatus.DELETED)) {
			removeContact(id);
			return;
		}

		getContactCard(id).updateContact(contact);

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	private void removeContact(Long id) {

		ContactCard contactCard = idContactCards.remove(id);
		if (contactCard == null)
			return;

		entities.getChildren().remove(contactCard);

	}

	public List<Long> getSelectedEntityIds() {

		return new ArrayList<Long>(selectedIds);

	}

	public void setContactFilter(Predicate<ContactHandle> filter) {

		contactFilterProperty.set(filter);

	}

	public void resetSelection() {

		selectedIds.clear();

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

		ContactCard contactCard = idContactCards.get(id);

		if (contactCard == null) {

			final ContactCard fContactCard = new ContactCard(id);
			contactCard = fContactCard;

			fContactCard.visibleProperty().bind(fContactCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchField.getText().toLowerCase();
				return searchContactStr.isEmpty() || fContactCard.getName().toLowerCase().startsWith(searchContactStr);
			}, searchField.textProperty(), fContactCard.nameProperty())));

			fContactCard.managedProperty().bind(fContactCard.visibleProperty());

			fContactCard.setOnMouseClicked(e -> {
				if (fContactCard.selectProperty().get())
					selectedIds.remove(id);
				else
					selectedIds.add(id);
			});

			idContactCards.put(id, fContactCard);

			entities.getChildren().add(fContactCard);

		}

		return contactCard;

	}

	private final class ContactCard extends SelectableEntityPane {

		private final Long id;

		private final ObjectProperty<Contact> contactProperty = new SimpleObjectProperty<Contact>();

		private ContactCard(Long id) {

			super();

			this.id = id;

			init();

		}

		private void init() {

			activeProperty().bind(Bindings.createBooleanBinding(() -> {

				Contact contact = contactProperty.get();

				if (contact == null)
					return false;

				boolean active = !Objects.equals(contact.getStatus(), Availability.OFFLINE);

				Predicate<ContactHandle> filter = contactFilterProperty.get();

				if (filter != null)
					active = active && filter.test(new ContactHandleImpl(contact));

				if (!active)
					selectedIds.remove(id);

				return active;

			}, contactProperty, contactFilterProperty));

			selectProperty().bind(Bindings.createBooleanBinding(() -> selectedIds.contains(id), selectedIds));

		}

		private void updateContact(Contact contact) {

			updateEntity(contact);

			contactProperty.set(contact);

		}

	}

}

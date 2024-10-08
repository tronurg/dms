package com.dms.core.view;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import javax.swing.UIManager;

import com.dms.core.database.tables.Contact;
import com.dms.core.intf.handles.ContactHandle;
import com.dms.core.intf.handles.impl.ContactHandleImpl;
import com.dms.core.intf.tools.ContactId;
import com.dms.core.intf.tools.impl.ContactIdImpl;
import com.dms.core.structures.Availability;
import com.dms.core.structures.ViewStatus;
import com.dms.core.view.component.DmsScrollPane;
import com.dms.core.view.component.SearchField;

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

	private final SearchField searchField = new SearchField(false);

	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new DmsScrollPane(entities);

	private final Map<ContactId, ContactCard> idContactCards = Collections
			.synchronizedMap(new HashMap<ContactId, ContactCard>());

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

	private final ObjectProperty<Predicate<ContactHandle>> contactFilterProperty = new SimpleObjectProperty<Predicate<ContactHandle>>();

	private final ObservableSet<ContactId> selectedIds = FXCollections.observableSet();

	ActiveContactsPane() {
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

	void updateContact(Contact contact) {

		ContactId id = ContactIdImpl.of(contact);

		if (contact.getViewStatus() == ViewStatus.DELETED) {
			removeContact(id);
			return;
		}

		getContactCard(id).updateContact(contact);

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	private void removeContact(ContactId id) {

		ContactCard contactCard = idContactCards.remove(id);
		if (contactCard == null) {
			return;
		}

		entities.getChildren().remove(contactCard);

	}

	public List<ContactId> getSelectedIds() {

		return new ArrayList<ContactId>(selectedIds);

	}

	public void setContactFilter(Predicate<ContactHandle> filter) {

		contactFilterProperty.set(filter);

	}

	public void resetSelection() {

		selectedIds.clear();

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

	private ContactCard getContactCard(final ContactId id) {

		ContactCard contactCard = idContactCards.get(id);

		if (contactCard == null) {

			final ContactCard fContactCard = new ContactCard(id);
			contactCard = fContactCard;

			fContactCard.visibleProperty().bind(fContactCard.activeProperty().and(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchField.getText().toLowerCase(Locale.getDefault());
				return searchContactStr.isEmpty()
						|| fContactCard.getName().toLowerCase(Locale.getDefault()).startsWith(searchContactStr);
			}, searchField.textProperty(), fContactCard.nameProperty())));

			fContactCard.managedProperty().bind(fContactCard.visibleProperty());

			fContactCard.setOnMouseClicked(e -> {
				if (fContactCard.selectProperty().get()) {
					selectedIds.remove(id);
				} else {
					selectedIds.add(id);
				}
			});

			idContactCards.put(id, fContactCard);

			entities.getChildren().add(fContactCard);

		}

		return contactCard;

	}

	private final class ContactCard extends SelectableEntityPane {

		private final ContactId id;

		private final ObjectProperty<Contact> contactProperty = new SimpleObjectProperty<Contact>();

		private ContactCard(ContactId id) {
			super();
			this.id = id;
			init();
		}

		private void init() {

			activeProperty().bind(Bindings.createBooleanBinding(() -> {

				Contact contact = contactProperty.get();

				if (contact == null) {
					return false;
				}

				boolean active = contact.getStatus() != Availability.OFFLINE;

				Predicate<ContactHandle> filter = contactFilterProperty.get();

				if (filter != null) {
					active = active && filter.test(new ContactHandleImpl(contact));
				}

				if (!active) {
					selectedIds.remove(id);
				}

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

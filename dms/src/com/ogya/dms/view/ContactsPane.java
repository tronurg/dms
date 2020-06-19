package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

class ContactsPane extends TitledPane {

	private final VBox contacts = new VBox();

	private final Map<String, ContactPane> uuidContactPane = Collections
			.synchronizedMap(new HashMap<String, ContactPane>());

	private final List<IContactsPane> listeners = Collections.synchronizedList(new ArrayList<IContactsPane>());

	private final AtomicReference<Date> currentDate = new AtomicReference<Date>(new Date(0));

	ContactsPane() {

		super();

		init();

	}

	private void init() {

		setText(CommonMethods.translate("CONTACTS"));

		contacts.setPadding(new Insets(10.0));

		ScrollPane scrollPane = new ScrollPane(contacts);
		scrollPane.setFitToWidth(true);

		scrollPane.setPadding(Insets.EMPTY);

		setContent(scrollPane);

		disableProperty().bind(Bindings.isEmpty(contacts.getChildren()));

	}

	void addListener(IContactsPane listener) {

		listeners.add(listener);

	}

	void updateContact(Contact contact) {

		getContactPane(contact.getUuid()).updateContact(contact);

	}

	void addMessage(Message message, MessageDirection messageDirection, String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.addMessage(message, messageDirection);

		Date messageDate = message.getDate();

		if (currentDate.get().compareTo(messageDate) < 0) {

			currentDate.set(messageDate);

			contacts.getChildren().remove(contactPane);
			contacts.getChildren().add(0, contactPane);

		}

	}

	void updateMessage(Message message, String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.updateMessage(message);

	}

	void scrollPaneToMessage(String uuid, Long mesajId) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.scrollPaneToMessage(mesajId);

	}

	void savePosition(String uuid, Long mesajId) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.savePosition(mesajId);

	}

	void scrollToSavedPosition(String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.scrollToSavedPosition();

	}

	private ContactPane getContactPane(final String uuid) {

		if (!uuidContactPane.containsKey(uuid)) {

			ContactPane contactPane = new ContactPane();

			contactPane.setOnShowMessagePane(messagePane -> {

				listeners.forEach(listener -> listener.showMessagePane(messagePane, uuid));

			});

			contactPane.setOnHideMessagePane(messagePane -> {

				listeners.forEach(listener -> listener.hideMessagePane(messagePane, uuid));

			});

			contactPane.setOnSendMessageAction(messageTxt -> {

				listeners.forEach(listener -> listener.sendMessageClicked(messageTxt, uuid));

			});

			contactPane.setOnPaneScrolledToTop(() -> {

				listeners.forEach(listener -> listener.paneScrolledToTop(uuid));

			});

			uuidContactPane.put(uuid, contactPane);

			contacts.getChildren().add(0, contactPane);

			setExpanded(true);

		}

		return uuidContactPane.get(uuid);

	}

}

interface IContactsPane {

	void showMessagePane(MessagePane messagePane, String uuid);

	void hideMessagePane(MessagePane messagePane, String uuid);

	void sendMessageClicked(String messageTxt, String uuid);

	void paneScrolledToTop(String uuid);

}

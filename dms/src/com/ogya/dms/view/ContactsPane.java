package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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

	private final AtomicLong currentId = new AtomicLong(0);

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

	void addMessageToTop(Message message, String senderName, MessageDirection messageDirection, String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.addMessageToTop(message, senderName, messageDirection);

		Long messageId = message.getId();

		if (currentId.get() < messageId) {

			currentId.set(messageId);

			contacts.getChildren().remove(contactPane);
			contacts.getChildren().add(0, contactPane);

		}

	}

	void addMessageToBottom(Message message, String senderName, MessageDirection messageDirection, String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.addMessageToBottom(message, senderName, messageDirection);

		Long messageId = message.getId();

		if (currentId.get() < messageId) {

			currentId.set(messageId);

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

				listeners.forEach(listener -> listener.showContactMessagePane(messagePane, uuid));

			});

			contactPane.setOnHideMessagePane(messagePane -> {

				listeners.forEach(listener -> listener.hideContactMessagePane(messagePane, uuid));

			});

			contactPane.setOnPaneScrolledToTop(() -> {

				listeners.forEach(listener -> listener.contactPaneScrolledToTop(uuid));

			});

			contactPane.setOnSendMessageAction(messageTxt -> {

				listeners.forEach(listener -> listener.sendPrivateMessageClicked(messageTxt, uuid));

			});

			contactPane.setOnShowFoldersAction(() -> {

				listeners.forEach(listener -> listener.privateShowFoldersClicked(uuid));

			});

			contactPane.setOnMessageClickedAction((senderUuid, messageId) -> {

				listeners.forEach(listener -> listener.messageClicked(senderUuid, messageId));

			});

			uuidContactPane.put(uuid, contactPane);

			contacts.getChildren().add(0, contactPane);

			setExpanded(true);

		}

		return uuidContactPane.get(uuid);

	}

}

interface IContactsPane {

	void showContactMessagePane(MessagePane messagePane, String uuid);

	void hideContactMessagePane(MessagePane messagePane, String uuid);

	void contactPaneScrolledToTop(String uuid);

	void sendPrivateMessageClicked(String messageTxt, String uuid);

	void privateShowFoldersClicked(String uuid);

	void messageClicked(String senderUuid, Long messageId);

}

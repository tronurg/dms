package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.view.intf.AppListener;

import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DmsPanel extends StackPane implements IIdentityPane, IContactsPane, IGroupsPane {

	private final VBox mainPane = new VBox();
	private final IdentityPane identityPane = new IdentityPane();
	private final ContactsPane contactsPane = new ContactsPane();
	private final GroupsPane groupsPane = new GroupsPane();
	private final VBox contactsGroupsPane = new VBox();

	private final List<AppListener> listeners = Collections.synchronizedList(new ArrayList<AppListener>());

	public DmsPanel() {

		super();

		init();

	}

	private void init() {

		VBox.setMargin(identityPane, new Insets(10.0));

		VBox.setVgrow(contactsPane, Priority.ALWAYS);
		VBox.setVgrow(groupsPane, Priority.ALWAYS);
		VBox.setVgrow(contactsGroupsPane, Priority.ALWAYS);

		identityPane.addListener(this);
		contactsPane.addListener(this);
		groupsPane.addListener(this);

		contactsGroupsPane.getChildren().addAll(contactsPane, groupsPane);

		mainPane.getChildren().addAll(identityPane, contactsGroupsPane);

		getChildren().add(mainPane);

	}

	public void addListener(AppListener listener) {

		listeners.add(listener);

	}

	public void setIdentity(Identity identity) {

		identityPane.setIdentity(identity);

	}

	public void updateContact(Contact contact) {

		contactsPane.updateContact(contact);
		groupsPane.createGroupPaneUpdateContact(contact);

	}

	public void updateDgroup(Dgroup dgroup) {

		// TODO

		dgroup.getContacts().forEach(contact -> System.out.println(contact.getName()));

	}

	public void addMessage(Message message, MessageDirection messageDirection, String uuid) {

		contactsPane.addMessage(message, messageDirection, uuid);

	}

	public void updateMessage(Message message, String uuid) {

		contactsPane.updateMessage(message, uuid);

	}

	public void scrollPaneToMessage(String uuid, Long messageId) {

		contactsPane.scrollPaneToMessage(uuid, messageId);

	}

	public void savePosition(String uuid, Long messageId) {

		contactsPane.savePosition(uuid, messageId);

	}

	public void scrollToSavedPosition(String uuid) {

		contactsPane.scrollToSavedPosition(uuid);

	}

	private void commentUpdatedToListeners(final String comment) {

		listeners.forEach(listener -> listener.commentUpdated(comment));

	}

	private void updateStatusClickedToListeners() {

		listeners.forEach(listener -> listener.updateStatusClicked());

	}

	private void contactMessagePaneOpenedToListeners(final String uuid) {

		listeners.forEach(listener -> listener.contactMessagePaneOpened(uuid));

	}

	private void contactMessagePaneClosedToListeners(final String uuid) {

		listeners.forEach(listener -> listener.contactMessagePaneClosed(uuid));

	}

	private void sendMessageClickedToListeners(final String message, final String uuid) {

		listeners.forEach(listener -> listener.sendMessageClicked(message, uuid));

	}

	private void paneScrolledToTopToListeners(final String uuid) {

		listeners.forEach(listener -> listener.paneScrolledToTop(uuid));

	}

	private void createGroupRequestedToListeners(final String groupName, final List<String> selectedUuids) {

		listeners.forEach(listener -> listener.createGroupRequested(groupName, selectedUuids));

	}

	@Override
	public void commentUpdated(String comment) {

		commentUpdatedToListeners(comment);

	}

	@Override
	public void updateStatusClicked() {

		updateStatusClickedToListeners();

	}

	@Override
	public void showMessagePane(final MessagePane messagePane, final String uuid) {

		getChildren().add(messagePane);

		contactMessagePaneOpenedToListeners(uuid);

	}

	@Override
	public void hideMessagePane(MessagePane messagePane, String uuid) {

		contactMessagePaneClosedToListeners(uuid);

		getChildren().remove(messagePane);

	}

	@Override
	public void sendMessageClicked(String messageTxt, String uuid) {

		sendMessageClickedToListeners(messageTxt, uuid);

	}

	@Override
	public void paneScrolledToTop(String uuid) {

		paneScrolledToTopToListeners(uuid);

	}

	@Override
	public void showCreateGroupPane(CreateGroupPane createGroupPane) {

		getChildren().add(createGroupPane);

	}

	@Override
	public void hideCreateGroupPane(CreateGroupPane createGroupPane) {

		getChildren().remove(createGroupPane);

	}

	@Override
	public void createGroupClicked(CreateGroupPane createGroupPane) {

		getChildren().remove(createGroupPane);
		createGroupRequestedToListeners(createGroupPane.getGroupName(), createGroupPane.getSelectedUuids());
		createGroupPane.reset();

	}

	@Override
	public void showGroupMessagePane(MessagePane messagePane, String uuid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void hideGroupMessagePane(MessagePane messagePane, String uuid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendGroupMessageClicked(String messageTxt, String uuid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void groupPaneScrolledToTop(String uuid) {
		// TODO Auto-generated method stub

	}

}

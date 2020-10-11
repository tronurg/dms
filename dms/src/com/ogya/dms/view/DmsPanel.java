package com.ogya.dms.view;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.common.CommonConstants;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.structures.ReceiverType;
import com.ogya.dms.view.intf.AppListener;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DmsPanel extends StackPane implements IIdentityPane, IContactsPane, IGroupsPane {

	private final VBox mainPane = new VBox();
	private final IdentityPane identityPane = new IdentityPane();
	private final EntitiesPane entitiesPane = new EntitiesPane();

	private final FoldersPane foldersPane = new FoldersPane(
			Paths.get(CommonConstants.FILE_EXPLORER_PATH).normalize().toAbsolutePath());

	private final StatusInfoPane statusInfoPane = new StatusInfoPane();
	private final SettingsPane settingsPane = new SettingsPane();
	private final RemoteIpSettingsPane remoteIpSettingsPane = new RemoteIpSettingsPane();

	private final List<AppListener> listeners = Collections.synchronizedList(new ArrayList<AppListener>());

	private final AtomicReference<Entry<String, MessagePane>> uuidOnScreenRef = new AtomicReference<Entry<String, MessagePane>>();

	public DmsPanel() {

		super();

		init();

	}

	private void init() {

		getChildren().addListener(new ListChangeListener<Node>() {

			@Override
			public void onChanged(Change<? extends Node> arg0) {

				ObservableList<? extends Node> nodeList = arg0.getList();

				for (int i = 0; i < nodeList.size() - 1; ++i) {
					nodeList.get(i).setVisible(false);
				}

				if (nodeList.size() > 0)
					nodeList.get(nodeList.size() - 1).setVisible(true);

			}

		});

		VBox.setMargin(identityPane, new Insets(10.0));

		VBox.setVgrow(entitiesPane, Priority.ALWAYS);

		identityPane.addListener(this);
		entitiesPane.addContactListener(this);
		entitiesPane.addGroupListener(this);
		foldersPane.setOnFileSelected(this::fileSelected);
		foldersPane.setOnBackAction(this::backFromFoldersPane);
		statusInfoPane.setOnBackAction(this::backFromStatusInfoPane);
		settingsPane.setOnBackAction(() -> getChildren().remove(settingsPane));
		settingsPane.setOnSettingClickedAction(this::settingClicked);
		remoteIpSettingsPane.setOnBackAction(() -> getChildren().remove(remoteIpSettingsPane));
		remoteIpSettingsPane.setOnAddIpAction(this::addIpClicked);
		remoteIpSettingsPane.setOnRemoveIpAction(this::removeIpClicked);

		mainPane.getChildren().addAll(identityPane, entitiesPane);

		getChildren().add(mainPane);

	}

	public void addListener(AppListener listener) {

		listeners.add(listener);

	}

	public void setIdentity(Identity identity) {

		identityPane.setIdentity(identity);

	}

	public void setCommentEditable(boolean editable) {

		identityPane.setCommentEditable(editable);

	}

	public void updateContact(Contact contact) {

		entitiesPane.updateContact(contact);
		entitiesPane.addUpdateGroupPaneUpdateContact(contact);
		statusInfoPane.updateContact(contact);

	}

	public void updateGroup(Dgroup group) {

		entitiesPane.updateGroup(group);

	}

	public void addMessageToTop(Message message, String senderName, MessageDirection messageDirection, String uuid) {

		switch (message.getReceiverType()) {

		case PRIVATE:

			entitiesPane.addPrivateMessageToTop(message, senderName, messageDirection, uuid);

			break;

		case GROUP:

			entitiesPane.addGroupMessageToTop(message, senderName, messageDirection, uuid);

			break;

		default:

		}

		Entry<String, MessagePane> uuidOnScreen = uuidOnScreenRef.get();
		if (!(uuidOnScreen == null || Objects.equals(uuid, uuidOnScreen.getKey())))
			uuidOnScreen.getValue().highlightBackButton();

	}

	public void addMessageToBottom(Message message, String senderName, MessageDirection messageDirection, String uuid) {

		switch (message.getReceiverType()) {

		case PRIVATE:

			entitiesPane.addPrivateMessageToBottom(message, senderName, messageDirection, uuid);

			break;

		case GROUP:

			entitiesPane.addGroupMessageToBottom(message, senderName, messageDirection, uuid);

			break;

		default:

		}

		Entry<String, MessagePane> uuidOnScreen = uuidOnScreenRef.get();
		if (!(uuidOnScreen == null || Objects.equals(uuid, uuidOnScreen.getKey())))
			uuidOnScreen.getValue().highlightBackButton();

	}

	public void updateMessageStatus(Message message, String uuid) {

		switch (message.getReceiverType()) {

		case PRIVATE:

			entitiesPane.updatePrivateMessageStatus(message, uuid);

			break;

		case GROUP:

			entitiesPane.updateGroupMessageStatus(message, uuid);

			break;

		default:

		}

	}

	public void updatePrivateMessageProgress(String uuid, Long messageId, int progress) {

		entitiesPane.updatePrivateMessageProgress(uuid, messageId, progress);

	}

	public void updateDetailedMessageStatus(String uuid, MessageStatus messageStatus) {

		statusInfoPane.updateMessageStatus(uuid, messageStatus);

	}

	public void updateDetailedMessageProgress(String uuid, int progress) {

		statusInfoPane.updateMessageProgress(uuid, progress);

	}

	public void scrollPaneToMessage(String uuid, Long messageId, ReceiverType receiverType) {

		switch (receiverType) {

		case PRIVATE:

			entitiesPane.scrollPrivatePaneToMessage(uuid, messageId);

			break;

		case GROUP:

			entitiesPane.scrollGroupPaneToMessage(uuid, messageId);

			break;

		default:

		}

	}

	public void savePosition(String uuid, Long messageId, ReceiverType receiverType) {

		switch (receiverType) {

		case PRIVATE:

			entitiesPane.savePrivatePosition(uuid, messageId);

			break;

		case GROUP:

			entitiesPane.saveGroupPosition(uuid, messageId);

			break;

		default:

		}

	}

	public void scrollToSavedPosition(String uuid, ReceiverType receiverType) {

		switch (receiverType) {

		case PRIVATE:

			entitiesPane.scrollToSavedPrivatePosition(uuid);

			break;

		case GROUP:

			entitiesPane.scrollToSavedGroupPosition(uuid);

			break;

		default:

		}

	}

	public void showAddUpdateGroupPane(String groupName, Set<String> selectedUuids, boolean isNewGroup) {

		getChildren().add(entitiesPane.getAddUpdateGroupPane(groupName, selectedUuids, isNewGroup));

	}

	public void showStatusInfoPane(List<Contact> contacts) {

		statusInfoPane.addCards(contacts);

		getChildren().add(statusInfoPane);

	}

	public void serverConnStatusUpdated(boolean connStatus) {

		remoteIpSettingsPane.setDisableInput(!connStatus);
		if (!connStatus)
			remoteIpSettingsPane.clearAll();

	}

	public void updateRemoteIps(String[] ips) {

		remoteIpSettingsPane.updateIps(ips);

	}

	private void fileSelected(Path file) {

		getChildren().remove(foldersPane);

		fileSelectedToListeners(file);

	}

	private void backFromFoldersPane() {

		getChildren().remove(foldersPane);

		foldersPane.reset();

		showFoldersCanceledToListeners();

	}

	private void backFromStatusInfoPane() {

		getChildren().remove(statusInfoPane);

		statusInfoPane.reset();

		statusInfoClosedToListeners();

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

	private void sendPrivateMessageClickedToListeners(final String message, final String uuid) {

		listeners.forEach(listener -> listener.sendPrivateMessageClicked(message, uuid));

	}

	private void privateShowFoldersClickedToListeners(final String uuid) {

		listeners.forEach(listener -> listener.privateShowFoldersClicked(uuid));

	}

	private void contactPaneScrolledToTopToListeners(final String uuid) {

		listeners.forEach(listener -> listener.contactPaneScrolledToTop(uuid));

	}

	private void showAddUpdateGroupClickedToListeners(final String groupUuid) {

		listeners.forEach(listener -> listener.showAddUpdateGroupClicked(groupUuid));

	}

	private void addUpdateGroupRequestedToListeners(final String groupName, final Set<String> selectedUuids) {

		listeners.forEach(listener -> listener.addUpdateGroupRequested(groupName, selectedUuids));

	}

	private void deleteGroupRequestedToListeners() {

		listeners.forEach(listener -> listener.deleteGroupRequested());

	}

	private void groupMessagePaneOpenedToListeners(final String groupUuid) {

		listeners.forEach(listener -> listener.groupMessagePaneOpened(groupUuid));

	}

	private void groupMessagePaneClosedToListeners(final String groupUuid) {

		listeners.forEach(listener -> listener.groupMessagePaneClosed(groupUuid));

	}

	private void sendGroupMessageClickedToListeners(final String message, final String groupUuid) {

		listeners.forEach(listener -> listener.sendGroupMessageClicked(message, groupUuid));

	}

	private void groupShowFoldersClickedToListeners(final String groupUuid) {

		listeners.forEach(listener -> listener.groupShowFoldersClicked(groupUuid));

	}

	private void groupPaneScrolledToTopToListeners(final String groupUuid) {

		listeners.forEach(listener -> listener.groupPaneScrolledToTop(groupUuid));

	}

	private void showFoldersCanceledToListeners() {

		listeners.forEach(listener -> listener.showFoldersCanceled());

	}

	private void statusInfoClosedToListeners() {

		listeners.forEach(listener -> listener.statusInfoClosed());

	}

	private void fileSelectedToListeners(final Path file) {

		listeners.forEach(listener -> listener.fileSelected(file));

	}

	private void messageClickedToListeners(final Long messageId) {

		listeners.forEach(listener -> listener.messageClicked(messageId));

	}

	private void infoClickedToListeners(final Long messageId) {

		listeners.forEach(listener -> listener.infoClicked(messageId));

	}

	private void cancelClickedToListeners(final Long messageId) {

		listeners.forEach(listener -> listener.cancelClicked(messageId));

	}

	private void addIpClickedToListeners(final String ip) {

		listeners.forEach(listener -> listener.addIpClicked(ip));

	}

	private void removeIpClickedToListeners(final String ip) {

		listeners.forEach(listener -> listener.removeIpClicked(ip));

	}

	private void settingClicked(Settings setting) {

		switch (setting) {

		case EDIT_REMOTE_IPS:

			getChildren().add(remoteIpSettingsPane);

			break;

		default:
			break;

		}

	}

	private void addIpClicked(String ip) {

		addIpClickedToListeners(ip);

		remoteIpSettingsPane.clearIpField();

	}

	private void removeIpClicked(String ip) {

		removeIpClickedToListeners(ip);

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
	public void settingsClicked() {

		getChildren().add(settingsPane);

	}

	@Override
	public void showContactMessagePane(final MessagePane messagePane, final String uuid) {

		getChildren().add(messagePane);

		uuidOnScreenRef.set(new AbstractMap.SimpleEntry<String, MessagePane>(uuid, messagePane));

		contactMessagePaneOpenedToListeners(uuid);

	}

	@Override
	public void hideContactMessagePane(MessagePane messagePane, String uuid) {

		contactMessagePaneClosedToListeners(uuid);

		uuidOnScreenRef.set(null);

		getChildren().remove(messagePane);

	}

	@Override
	public void contactPaneScrolledToTop(String uuid) {

		contactPaneScrolledToTopToListeners(uuid);

	}

	@Override
	public void sendPrivateMessageClicked(String messageTxt, String uuid) {

		sendPrivateMessageClickedToListeners(messageTxt, uuid);

	}

	@Override
	public void privateShowFoldersClicked(String uuid) {

		getChildren().add(foldersPane);

		privateShowFoldersClickedToListeners(uuid);

	}

	@Override
	public void showAddUpdateGroupPaneClicked(String groupUuid) {

		showAddUpdateGroupClickedToListeners(groupUuid);

	}

	@Override
	public void hideAddUpdateGroupPane(AddUpdateGroupPane addUpdateGroupPane) {

		getChildren().remove(addUpdateGroupPane);

	}

	@Override
	public void addUpdateGroupClicked(AddUpdateGroupPane addUpdateGroupPane) {

		getChildren().remove(addUpdateGroupPane);
		addUpdateGroupRequestedToListeners(addUpdateGroupPane.getGroupName(), addUpdateGroupPane.getSelectedUuids());

	}

	@Override
	public void deleteGroupClicked(AddUpdateGroupPane addUpdateGroupPane) {

		getChildren().remove(addUpdateGroupPane);
		deleteGroupRequestedToListeners();

	}

	@Override
	public void showGroupMessagePane(MessagePane messagePane, String groupUuid) {

		getChildren().add(messagePane);

		uuidOnScreenRef.set(new AbstractMap.SimpleEntry<String, MessagePane>(groupUuid, messagePane));

		groupMessagePaneOpenedToListeners(groupUuid);

	}

	@Override
	public void hideGroupMessagePane(MessagePane messagePane, String groupUuid) {

		groupMessagePaneClosedToListeners(groupUuid);

		uuidOnScreenRef.set(null);

		getChildren().remove(messagePane);

	}

	@Override
	public void groupPaneScrolledToTop(String groupUuid) {

		groupPaneScrolledToTopToListeners(groupUuid);

	}

	@Override
	public void sendGroupMessageClicked(String messageTxt, String groupUuid) {

		sendGroupMessageClickedToListeners(messageTxt, groupUuid);

	}

	@Override
	public void groupShowFoldersClicked(String groupUuid) {

		getChildren().add(foldersPane);

		groupShowFoldersClickedToListeners(groupUuid);

	}

	@Override
	public void messageClicked(Long messageId) {

		messageClickedToListeners(messageId);

	}

	@Override
	public void infoClicked(Long messageId) {

		infoClickedToListeners(messageId);

	}

	@Override
	public void cancelClicked(Long messageId) {

		cancelClickedToListeners(messageId);

	}

}

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

import javax.swing.UIManager;

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

public class DmsPanel extends StackPane implements IIdentityPane, IEntitiesPane {

	private final VBox mainPane = new VBox();
	private final IdentityPane identityPane = new IdentityPane();
	private final EntitiesPane entitiesPane = new EntitiesPane();

	private final MyActiveGroupsPanel myActiveGroupsPanel = new MyActiveGroupsPanel();
	private final OnlineContactsPanel onlineContactsPanel = new OnlineContactsPanel();

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
		entitiesPane.addEntityListener(this);
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

	public void addListener(AppListener listener) {

		listeners.add(listener);

	}

	public MyActiveGroupsPanel getMyActiveGroupsPanel() {

		return myActiveGroupsPanel;

	}

	public OnlineContactsPanel getOnlineContactsPanel() {

		return onlineContactsPanel;

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
		onlineContactsPanel.updateContact(contact);
		myActiveGroupsPanel.updateContact(contact);

	}

	public void updateGroup(Dgroup group) {

		entitiesPane.updateGroup(group);
		myActiveGroupsPanel.updateGroup(group);

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

	public void recordingStarted(String uuid, ReceiverType receiverType) {

		switch (receiverType) {

		case PRIVATE:

			entitiesPane.privateRecordingStarted(uuid);

			break;

		case GROUP:

			entitiesPane.groupRecordingStarted(uuid);

			break;

		default:

		}

	}

	public void recordingStopped(String uuid, ReceiverType receiverType) {

		switch (receiverType) {

		case PRIVATE:

			entitiesPane.privateRecordingStopped(uuid);

			break;

		case GROUP:

			entitiesPane.groupRecordingStopped(uuid);

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

	private void fileSelected(final Path path) {

		getChildren().remove(foldersPane);

		listeners.forEach(listener -> listener.fileSelected(path));

	}

	private void backFromFoldersPane() {

		getChildren().remove(foldersPane);

		foldersPane.reset();

		listeners.forEach(listener -> listener.showFoldersCanceled());

	}

	private void backFromStatusInfoPane() {

		getChildren().remove(statusInfoPane);

		statusInfoPane.reset();

		listeners.forEach(listener -> listener.statusInfoClosed());

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

	private void addIpClicked(final String ip) {

		listeners.forEach(listener -> listener.addIpClicked(ip));

		remoteIpSettingsPane.clearIpField();

	}

	private void removeIpClicked(final String ip) {

		listeners.forEach(listener -> listener.removeIpClicked(ip));

	}

	@Override
	public void commentUpdated(final String comment) {

		listeners.forEach(listener -> listener.commentUpdated(comment));

	}

	@Override
	public void updateStatusClicked() {

		listeners.forEach(listener -> listener.updateStatusClicked());

	}

	@Override
	public void settingsClicked() {

		getChildren().add(settingsPane);

	}

	@Override
	public void showMessagePane(MessagePane messagePane, final String uuid, final ReceiverType receiverType) {

		getChildren().add(messagePane);

		uuidOnScreenRef.set(new AbstractMap.SimpleEntry<String, MessagePane>(uuid, messagePane));

		listeners.forEach(listener -> listener.messagePaneOpened(uuid, receiverType));

	}

	@Override
	public void hideMessagePane(MessagePane messagePane, final String uuid) {

		listeners.forEach(listener -> listener.messagePaneClosed(uuid));

		uuidOnScreenRef.set(null);

		getChildren().remove(messagePane);

	}

	@Override
	public void paneScrolledToTop(final String uuid, final ReceiverType receiverType) {

		listeners.forEach(listener -> listener.paneScrolledToTop(uuid, receiverType));

	}

	@Override
	public void sendMessageClicked(final String messageTxt, final String uuid, final ReceiverType receiverType) {

		listeners.forEach(listener -> listener.sendMessageClicked(messageTxt, uuid, receiverType));

	}

	@Override
	public void showFoldersClicked(final String uuid, final ReceiverType receiverType) {

		getChildren().add(foldersPane);

		listeners.forEach(listener -> listener.showFoldersClicked(uuid, receiverType));

	}

	@Override
	public void showAddUpdateGroupPaneClicked(final String groupUuid) {

		listeners.forEach(listener -> listener.showAddUpdateGroupClicked(groupUuid));

	}

	@Override
	public void hideAddUpdateGroupPane(AddUpdateGroupPane addUpdateGroupPane) {

		getChildren().remove(addUpdateGroupPane);

	}

	@Override
	public void addUpdateGroupClicked(final AddUpdateGroupPane addUpdateGroupPane) {

		getChildren().remove(addUpdateGroupPane);

		listeners.forEach(listener -> listener.addUpdateGroupRequested(addUpdateGroupPane.getGroupName(),
				addUpdateGroupPane.getSelectedUuids()));

	}

	@Override
	public void deleteGroupClicked(AddUpdateGroupPane addUpdateGroupPane) {

		getChildren().remove(addUpdateGroupPane);

		listeners.forEach(listener -> listener.deleteGroupRequested());

	}

	@Override
	public void messageClicked(final Long messageId) {

		listeners.forEach(listener -> listener.messageClicked(messageId));

	}

	@Override
	public void infoClicked(final Long messageId) {

		listeners.forEach(listener -> listener.infoClicked(messageId));

	}

	@Override
	public void cancelClicked(final Long messageId) {

		listeners.forEach(listener -> listener.cancelClicked(messageId));

	}

	@Override
	public void recordButtonPressed(final String uuid, final ReceiverType receiverType) {

		listeners.forEach(listener -> listener.recordButtonPressed(uuid, receiverType));

	}

	@Override
	public void recordEventTriggered() {

		listeners.forEach(listener -> listener.recordEventTriggered());

	}

	@Override
	public void recordButtonReleased() {

		listeners.forEach(listener -> listener.recordButtonReleased());

	}

	@Override
	public void reportClicked(final String uuid, final ReceiverType receiverType) {

		listeners.forEach(listener -> listener.reportClicked(uuid, receiverType));

	}

}

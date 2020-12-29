package com.ogya.dms.core.view;

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

import com.ogya.dms.core.common.CommonConstants;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ReceiverType;
import com.ogya.dms.core.view.factory.ViewFactory;
import com.ogya.dms.core.view.intf.AppListener;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DmsPanel extends StackPane implements IIdentityPane, IEntitiesPane {

	private static final double GAP = ViewFactory.GAP;

	private final VBox mainPane = new VBox();
	private final IdentityPane identityPane = new IdentityPane();
	private final EntitiesPane entitiesPane = new EntitiesPane();

	private final ActiveGroupsPanel activeGroupsPanel = new ActiveGroupsPanel();
	private final OnlineContactsPanel onlineContactsPanel = new OnlineContactsPanel();

	private final FoldersPane foldersPane = new FoldersPane(
			Paths.get(CommonConstants.FILE_EXPLORER_PATH).normalize().toAbsolutePath());

	private final StatusInfoPane statusInfoPane = new StatusInfoPane();
	private final SettingsPane settingsPane = new SettingsPane();
	private final RemoteIpSettingsPane remoteIpSettingsPane = new RemoteIpSettingsPane();

	private final List<AppListener> listeners = Collections.synchronizedList(new ArrayList<AppListener>());

	private final AtomicReference<Entry<Long, MessagePane>> idOnScreenRef = new AtomicReference<Entry<Long, MessagePane>>();

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

		VBox.setMargin(identityPane, new Insets(2 * GAP));

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

	public ActiveGroupsPanel getActiveGroupsPanel() {

		return activeGroupsPanel;

	}

	public OnlineContactsPanel getOnlineContactsPanel() {

		return onlineContactsPanel;

	}

	public void setIdentity(Contact identity) {

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
		activeGroupsPanel.updateContact(contact);

	}

	public void updateGroup(Dgroup group) {

		entitiesPane.updateGroup(group);
		activeGroupsPanel.updateGroup(group);

	}

	public void addMessage(Message message) {

		entitiesPane.addMessage(message);

		Long entityId = Objects.equals(message.getReceiverType(), ReceiverType.CONTACT) ? message.getContact().getId()
				: -message.getDgroup().getId();

		Entry<Long, MessagePane> idOnScreen = idOnScreenRef.get();
		if (!(idOnScreen == null || Objects.equals(entityId, idOnScreen.getKey())))
			idOnScreen.getValue().highlightBackButton();

	}

	public void updateMessageStatus(Message message) {

		switch (message.getReceiverType()) {

		case CONTACT:

			entitiesPane.updatePrivateMessageStatus(message);

			break;

		case GROUP_OWNER:
		case GROUP_MEMBER:

			entitiesPane.updateGroupMessageStatus(message);

			break;

		default:

		}

	}

	public void updatePrivateMessageProgress(Long id, Long messageId, int progress) {

		entitiesPane.updatePrivateMessageProgress(id, messageId, progress);

	}

	public void updateDetailedMessageStatus(Long id, MessageStatus messageStatus) {

		statusInfoPane.updateMessageStatus(id, messageStatus);

	}

	public void updateDetailedMessageProgress(Long id, int progress) {

		statusInfoPane.updateMessageProgress(id, progress);

	}

	public void scrollPaneToMessage(Long id, Long messageId) {

		if (id > 0) {

			entitiesPane.scrollPrivatePaneToMessage(id, messageId);

		} else {

			entitiesPane.scrollGroupPaneToMessage(-id, messageId);

		}

	}

	public void savePosition(Long id, Long messageId) {

		if (id > 0) {

			entitiesPane.savePrivatePosition(id, messageId);

		} else {

			entitiesPane.saveGroupPosition(-id, messageId);

		}

	}

	public void scrollToSavedPosition(Long id) {

		if (id > 0) {

			entitiesPane.scrollToSavedPrivatePosition(id);

		} else {

			entitiesPane.scrollToSavedGroupPosition(-id);

		}

	}

	public void recordingStarted(Long id) {

		if (id > 0) {

			entitiesPane.privateRecordingStarted(id);

		} else {

			entitiesPane.groupRecordingStarted(-id);

		}

	}

	public void recordingStopped(Long id) {

		if (id > 0) {

			entitiesPane.privateRecordingStopped(id);

		} else {

			entitiesPane.groupRecordingStopped(-id);

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

	public Long getRefMessageId(Long id) {

		if (id < 0)
			return entitiesPane.getGroupRefMessageId(id);

		return entitiesPane.getContactRefMessageId(id);

	}

	private void fileSelected(final Path path) {

		getChildren().remove(foldersPane);

		listeners.forEach(listener -> listener.fileSelected(getIdOnScreen(), path, getRefMessageIdOnScreen()));

	}

	private void backFromFoldersPane() {

		getChildren().remove(foldersPane);

		foldersPane.reset();

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

		}

	}

	private void addIpClicked(final String ip) {

		listeners.forEach(listener -> listener.addIpClicked(ip));

		remoteIpSettingsPane.clearIpField();

	}

	private void removeIpClicked(final String ip) {

		listeners.forEach(listener -> listener.removeIpClicked(ip));

	}

	private Long getIdOnScreen() {

		if (idOnScreenRef.get() == null)
			return null;

		return idOnScreenRef.get().getKey();

	}

	private Long getRefMessageIdOnScreen() {

		if (idOnScreenRef.get() == null)
			return null;

		MessagePane messagePaneOnScreen = idOnScreenRef.get().getValue();

		if (messagePaneOnScreen == null)
			return null;

		return messagePaneOnScreen.getRefMessageId();

	}

	@Override
	public void commentUpdateRequested(final String comment) {

		listeners.forEach(listener -> listener.commentUpdateRequested(comment));

	}

	@Override
	public void statusUpdateRequested(final Availability availability) {

		listeners.forEach(listener -> listener.statusUpdateRequested(availability));

	}

	@Override
	public void settingsClicked() {

		getChildren().add(settingsPane);

	}

	@Override
	public void showMessagePane(final Long id, MessagePane messagePane) {

		getChildren().add(messagePane);

		idOnScreenRef.set(new AbstractMap.SimpleEntry<Long, MessagePane>(id, messagePane));

		listeners.forEach(listener -> listener.messagePaneOpened(id));

	}

	@Override
	public void hideMessagePane(final Long id, MessagePane messagePane) {

		listeners.forEach(listener -> listener.messagePaneClosed(id));

		idOnScreenRef.set(null);

		getChildren().remove(messagePane);

	}

	@Override
	public void paneScrolledToTop(final Long id, Long topMessageId) {

		listeners.forEach(listener -> listener.paneScrolledToTop(id, topMessageId));

	}

	@Override
	public void sendMessageClicked(final Long id, final String messageTxt, final Long refMessageId) {

		listeners.forEach(listener -> listener.sendMessageClicked(id, messageTxt, refMessageId));

	}

	@Override
	public void showFoldersClicked(final Long id) {

		getChildren().add(foldersPane);

	}

	@Override
	public void showAddUpdateGroupPaneClicked(final Long id) {

		listeners.forEach(listener -> listener.showAddUpdateGroupClicked(id));

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
	public void recordButtonPressed(final Long id) {

		listeners.forEach(listener -> listener.recordButtonPressed(id));

	}

	@Override
	public void recordEventTriggered(Long id, final Long refMessageId) {

		listeners.forEach(listener -> listener.recordEventTriggered(id, refMessageId));

	}

	@Override
	public void recordButtonReleased(final Long id) {

		listeners.forEach(listener -> listener.recordButtonReleased(id));

	}

	@Override
	public void reportClicked(final Long id) {

		listeners.forEach(listener -> listener.reportClicked(id));

	}

}

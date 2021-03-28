package com.ogya.dms.core.view;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.UIManager;

import com.ogya.dms.core.common.CommonConstants;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.FileBuilder;
import com.ogya.dms.core.structures.MessageDirection;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.ReceiverType;
import com.ogya.dms.core.view.factory.ViewFactory;
import com.ogya.dms.core.view.intf.AppListener;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DmsPanel extends StackPane implements IIdentityPane, IEntitiesPane, IStarredMessagesPane {

	private final double gap = ViewFactory.getGap();

	private final BooleanProperty unreadProperty = new SimpleBooleanProperty();
	private final ObservableSet<Long> unreadMessagePaneIds = FXCollections.observableSet();

	private final VBox mainPane = new VBox();
	private final IdentityPane identityPane = new IdentityPane();
	private final EntitiesPane entitiesPane = new EntitiesPane(unreadProperty);

	private final ActiveGroupsPanel activeGroupsPanel = new ActiveGroupsPanel();
	private final OnlineContactsPanel onlineContactsPanel = new OnlineContactsPanel();

	private final FoldersPane foldersPane = new FoldersPane(
			Paths.get(CommonConstants.FILE_EXPLORER_PATH).normalize().toAbsolutePath(), unreadProperty);

	private final StatusInfoPane statusInfoPane = new StatusInfoPane(unreadProperty);
	private final SettingsPane settingsPane = new SettingsPane(unreadProperty);
	private final RemoteIpSettingsPane remoteIpSettingsPane = new RemoteIpSettingsPane(unreadProperty);
	private final StarredMessagesPane starredMessagesPane = new StarredMessagesPane(unreadProperty);

	private final List<AppListener> listeners = Collections.synchronizedList(new ArrayList<AppListener>());

	private final ObjectProperty<MessagePane> messagePaneOnScreenRef = new SimpleObjectProperty<MessagePane>();

	public DmsPanel() {

		super();

		init();

	}

	private void init() {

		setDisable(true);

		getChildren().addListener(new ListChangeListener<Node>() {

			@Override
			public void onChanged(Change<? extends Node> arg0) {

				ObservableList<? extends Node> nodeList = arg0.getList();

				for (int i = 0; i < nodeList.size() - 1; ++i) {
					nodeList.get(i).setVisible(false);
				}

				Node topNode = null;

				if (!nodeList.isEmpty())
					topNode = nodeList.get(nodeList.size() - 1);

				messagePaneOnScreenRef.set(null);

				if (topNode == null)
					return;

				topNode.setVisible(true);

				if (Objects.equals(topNode, mainPane))
					unreadMessagePaneIds.clear();
				else if (topNode instanceof MessagePane)
					messagePaneOnScreenRef.set((MessagePane) topNode);

			}

		});

		messagePaneOnScreenRef.addListener((e0, e1, e2) -> {
			if (e2 == null) {
				listeners.forEach(listener -> listener.messagePaneClosed());
			} else {
				Long messagePaneId = e2.getMessagePaneId();
				unreadMessagePaneIds.remove(messagePaneId);
				listeners.forEach(listener -> listener.messagePaneOpened(messagePaneId));
			}
		});

		unreadProperty.bind(Bindings.isNotEmpty(unreadMessagePaneIds));

		VBox.setMargin(identityPane, new Insets(2 * gap));

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
		starredMessagesPane.setOnBackAction(() -> getChildren().remove(starredMessagesPane));
		starredMessagesPane.addListener(this);

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

		if (Objects.equals(message.getMessageDirection(), MessageDirection.OUT)
				|| Objects.equals(message.getMessageStatus(), MessageStatus.READ))
			return;

		Long entityId = Objects.equals(message.getReceiverType(), ReceiverType.CONTACT) ? message.getContact().getId()
				: -message.getDgroup().getId();

		unreadMessagePaneIds.add(entityId);

	}

	public void updateMessage(Message message) {

		switch (message.getReceiverType()) {

		case CONTACT:

			entitiesPane.updatePrivateMessage(message);

			break;

		case GROUP_OWNER:
		case GROUP_MEMBER:

			entitiesPane.updateGroupMessage(message);

			break;

		default:

		}

	}

	public void addUpdateArchivedMessage(Message message) {

		starredMessagesPane.addUpdateMessage(message);

	}

	public void addAttachment(FileBuilder fileBuilder) {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			messagePaneOnScreen.addAttachment(fileBuilder);

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

	public void allMessagesLoaded() {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			messagePaneOnScreen.allMessagesLoaded();

	}

	public void allArchivedMessagesLoaded() {

		starredMessagesPane.allMessagesLoaded();

	}

	public void recordingStarted() {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			messagePaneOnScreen.recordingStarted();

	}

	public void recordingStopped() {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			messagePaneOnScreen.recordingStopped();

	}

	public void showAddUpdateGroupPane(String groupName, Set<String> selectedUuids, boolean isNewGroup) {

		getChildren().add(entitiesPane.getAddUpdateGroupPane(groupName, selectedUuids, isNewGroup));

	}

	public void showStatusInfoPane(List<Contact> contacts) {

		statusInfoPane.addCards(contacts);

		getChildren().add(statusInfoPane);

	}

	public void serverConnStatusUpdated(boolean connStatus) {

		setDisable(!connStatus);

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

	}

	private void backFromStatusInfoPane() {

		getChildren().remove(statusInfoPane);

		statusInfoPane.reset();

		listeners.forEach(listener -> listener.statusInfoClosed());

	}

	private void settingClicked(Settings setting) {

		switch (setting) {

		case STARRED_MESSAGES:

			starredMessagesPane.scrollToTop();
			getChildren().add(starredMessagesPane);

			break;

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
	public void showMessagePane(MessagePane messagePane) {

		getChildren().add(messagePane);

	}

	@Override
	public void hideMessagePane(MessagePane messagePane) {

		getChildren().remove(messagePane);

	}

	@Override
	public void paneScrolledToTop(final Long id, final Long topMessageId) {

		listeners.forEach(listener -> listener.paneScrolledToTop(id, topMessageId));

	}

	@Override
	public void messagesClaimed(final Long id, final Long lastMessageIdExcl, final Long firstMessageIdIncl) {

		listeners.forEach(listener -> listener.messagesClaimed(id, lastMessageIdExcl, firstMessageIdIncl));

	}

	@Override
	public void sendMessageClicked(final Long id, final String messageTxt, final FileBuilder fileBuilder,
			final Long refMessageId) {

		listeners.forEach(listener -> listener.sendMessageClicked(id, messageTxt, fileBuilder, refMessageId));

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
	public void attachmentClicked(final Long messageId) {

		listeners.forEach(listener -> listener.attachmentClicked(messageId));

	}

	@Override
	public void infoClicked(final Long messageId) {

		listeners.forEach(listener -> listener.infoClicked(messageId));

	}

	@Override
	public void deleteMessagesRequested(Long... messageIds) {

		listeners.forEach(listener -> listener.deleteMessagesRequested(messageIds));

	}

	@Override
	public void archiveMessagesRequested(Long... messageIds) {

		listeners.forEach(listener -> listener.archiveMessagesRequested(messageIds));

	}

	@Override
	public void recordButtonPressed() {

		listeners.forEach(listener -> listener.recordButtonPressed());

	}

	@Override
	public void recordEventTriggered(final Long refMessageId) {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			listeners.forEach(
					listener -> listener.recordEventTriggered(messagePaneOnScreen.getMessagePaneId(), refMessageId));

	}

	@Override
	public void recordButtonReleased() {

		listeners.forEach(listener -> listener.recordButtonReleased());

	}

	@Override
	public void reportClicked() {

		listeners.forEach(listener -> listener.reportClicked());

	}

	@Override
	public void loadMoreRequested(final Long bottomMessageId) {

		listeners.forEach(listener -> listener.moreArchivedMessagesRequested(bottomMessageId));

	}

}

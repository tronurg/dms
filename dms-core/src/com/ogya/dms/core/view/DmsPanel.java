package com.ogya.dms.core.view;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.UIManager;

import com.ogya.dms.core.common.CommonConstants;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.FileBuilder;
import com.ogya.dms.core.structures.MessageStatus;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class DmsPanel extends StackPane
		implements IIdentityPane, IEntitiesPane, ISearchInAllMessagesPane, IStarredMessagesPane, IMessagePane {

	private static final double GAP = ViewFactory.GAP;

	private final BooleanProperty unreadProperty = new SimpleBooleanProperty();
	private final ObservableSet<EntityId> unreadEntityIds = FXCollections.observableSet();

	private final BorderPane mainPane = new BorderPane();
	private final IdentityPane identityPane = new IdentityPane();
	private final EntitiesPane entitiesPane = new EntitiesPane();

	private final FoldersPane foldersPane = new FoldersPane(
			Paths.get(CommonConstants.FILE_EXPLORER_PATH).normalize().toAbsolutePath(), unreadProperty);
	private final AddUpdateGroupPane addUpdateGroupPane = new AddUpdateGroupPane(unreadProperty);
	private final StatusInfoPane statusInfoPane = new StatusInfoPane(unreadProperty);
	private final SettingsPane settingsPane = new SettingsPane(unreadProperty);
	private final SearchInAllMessagesPane searchInAllMessagesPane = new SearchInAllMessagesPane(unreadProperty);
	private final StarredMessagesPane starredMessagesPane = new StarredMessagesPane(unreadProperty);
	private final HiddenEntitiesPane hiddenEntitiesPane = new HiddenEntitiesPane(unreadProperty);
	private final RemoteIpSettingsPane remoteIpSettingsPane = new RemoteIpSettingsPane(unreadProperty);
	private final ForwardSelectionPane fwdSelectionPane = new ForwardSelectionPane(unreadProperty);

	private final ActiveGroupsPane activeGroupsPane = new ActiveGroupsPane();
	private final ActiveContactsPane activeContactsPane = new ActiveContactsPane();

	private final List<AppListener> listeners = Collections.synchronizedList(new ArrayList<AppListener>());

	private final Map<EntityId, MessagePane> messagePanes = Collections
			.synchronizedMap(new HashMap<EntityId, MessagePane>());

	private final ObjectProperty<MessagePane> messagePaneOnScreenRef = new SimpleObjectProperty<MessagePane>();

	private final AtomicReference<Long[]> fwdMessageIds = new AtomicReference<Long[]>();

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
					unreadEntityIds.clear();
				else if (topNode instanceof MessagePane)
					messagePaneOnScreenRef.set((MessagePane) topNode);

			}

		});

		messagePaneOnScreenRef.addListener((e0, e1, e2) -> {
			if (e2 == null) {
				listeners.forEach(listener -> listener.messagePaneClosed());
			} else {
				e2.focusOnMessageArea();
				EntityId entityId = e2.getEntityId();
				unreadEntityIds.remove(entityId);
				listeners.forEach(listener -> listener.messagePaneOpened(entityId));
			}
		});

		unreadProperty.bind(Bindings.isNotEmpty(unreadEntityIds));

		BorderPane.setMargin(identityPane, new Insets(2 * GAP));

		registerListeners();

		mainPane.setTop(identityPane);
		mainPane.setCenter(entitiesPane);

		getChildren().add(mainPane);

	}

	private void registerListeners() {

		identityPane.addListener(this);

		// Entities Pane
		entitiesPane.addEntitiesPaneListener(this);
		entitiesPane.setOnAddUpdateGroupClicked(this::showAddUpdateGroupClicked);

		// Folders Pane
		foldersPane.setOnFileSelected(this::fileSelected);
		foldersPane.setOnBackAction(this::backFromFoldersPane);

		// Add Update Group Pane
		addUpdateGroupPane.setOnBackAction(this::hideAddUpdateGroupPane);
		addUpdateGroupPane.setOnAddUpdateGroupAction(this::addUpdateGroupClicked);
		addUpdateGroupPane.setOnDeleteGroupAction(this::deleteGroupClicked);

		// Status Info Pane
		statusInfoPane.setOnBackAction(this::backFromStatusInfoPane);

		// Settings Pane
		settingsPane.setOnBackAction(() -> getChildren().remove(settingsPane));
		settingsPane.setOnSettingClickedAction(this::settingClicked);

		// Search In All Messages Pane
		searchInAllMessagesPane.addListener(this);
		searchInAllMessagesPane.setOnBackAction(() -> getChildren().remove(searchInAllMessagesPane));

		// Starred Messages Pane
		starredMessagesPane.addListener(this);
		starredMessagesPane.setOnBackAction(() -> getChildren().remove(starredMessagesPane));

		// Hidden Entities Pane
		hiddenEntitiesPane.addEntitiesPaneListener(this);
		hiddenEntitiesPane.setOnBackAction(() -> getChildren().remove(hiddenEntitiesPane));

		// Remote IP Settings Pane
		remoteIpSettingsPane.setOnBackAction(() -> getChildren().remove(remoteIpSettingsPane));
		remoteIpSettingsPane.setOnAddIpAction(this::addIpClicked);
		remoteIpSettingsPane.setOnRemoveIpAction(this::removeIpClicked);

		// Forward Selection Pane
		fwdSelectionPane.setOnBackAction(() -> {
			getChildren().remove(fwdSelectionPane);
			fwdMessageIds.set(null);
		});
		fwdSelectionPane.setOnSendAction(this::forwardMessagesRequested);

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

	public ActiveGroupsPane getActiveGroupsPane() {

		return activeGroupsPane;

	}

	public ActiveContactsPane getActiveContactsPane() {

		return activeContactsPane;

	}

	public void setIdentity(Contact identity) {

		identityPane.setIdentity(identity);

	}

	public void setCommentEditable(boolean editable) {

		identityPane.setCommentEditable(editable);

	}

	public void updateContact(Contact contact) {

		updateEntity(contact);

		addUpdateGroupPane.updateContact(contact);
		statusInfoPane.updateContact(contact);
		activeContactsPane.updateContact(contact);
		activeGroupsPane.updateMember(contact);

	}

	public void updateGroup(Dgroup group) {

		updateEntity(group);

		activeGroupsPane.updateGroup(group);

	}

	private void updateEntity(EntityBase entity) {

		getMessagePane(entity.getEntityId()).updateEntity(entity);
		entitiesPane.updateEntity(entity);
		hiddenEntitiesPane.updateEntity(entity);
		fwdSelectionPane.updateEntity(entity);

	}

	public void sortEntities() {

		entitiesPane.sortEntities();
		hiddenEntitiesPane.sortEntities();

	}

	public void addMessage(Message message, boolean moveToTop) {

		EntityId entityId = message.getEntity().getEntityId();

		addUpdateMessage(entityId, message);

		if (moveToTop) {
			entitiesPane.moveEntityToTop(entityId);
			hiddenEntitiesPane.moveEntityToTop(entityId);
		}

		if (getChildren().size() == 1 || message.isLocal()
				|| Objects.equals(message.getMessageStatus(), MessageStatus.READ))
			return;

		unreadEntityIds.add(entityId);

	}

	public void updateMessage(Message message) {

		EntityId entityId = message.getEntity().getEntityId();

		addUpdateMessage(entityId, message);

	}

	private void addUpdateMessage(EntityId entityId, Message message) {

		getMessagePane(entityId).addUpdateMessage(message);
		searchInAllMessagesPane.updateMessage(message);
		entitiesPane.updateMessageStatus(entityId, message);
		hiddenEntitiesPane.updateMessageStatus(entityId, message);
		fwdSelectionPane.updateMessageStatus(entityId, message);

	}

	public void addUpdateArchivedMessage(Message message) {

		starredMessagesPane.addUpdateMessage(message);

	}

	public void addAttachment(FileBuilder fileBuilder) {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			messagePaneOnScreen.addAttachment(fileBuilder);

	}

	public void updateMessageProgress(EntityId entityId, Long messageId, int progress) {

		getMessagePane(entityId).updateMessageProgress(messageId, progress);

	}

	public void updateDetailedMessageStatus(Long contactId, MessageStatus messageStatus) {

		statusInfoPane.updateMessageStatus(contactId, messageStatus);

	}

	public void updateDetailedMessageProgress(Long contactId, int progress) {

		statusInfoPane.updateMessageProgress(contactId, progress);

	}

	public void scrollPaneToMessage(EntityId entityId, Long messageId) {

		getMessagePane(entityId).scrollPaneToMessage(messageId);

	}

	public void savePosition(EntityId entityId, Long messageId) {

		getMessagePane(entityId).savePosition(messageId);

	}

	public void scrollToSavedPosition(EntityId entityId) {

		getMessagePane(entityId).scrollToSavedPosition();

	}

	public void allMessagesLoaded(EntityId entityId) {

		getMessagePane(entityId).allMessagesLoaded();

	}

	public void allArchivedMessagesLoaded() {

		starredMessagesPane.allMessagesLoaded();

	}

	public void recordingStopped() {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			messagePaneOnScreen.recordingStopped();

	}

	public void showAddUpdateGroupPane(Dgroup group, boolean isNewGroup) {

		addUpdateGroupPane.resetContent(group, isNewGroup);

		getChildren().add(addUpdateGroupPane);

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

	public void scrollArchivePaneToMessage(Long messageId) {

		starredMessagesPane.scrollPaneToMessage(messageId);

	}

	private void fileSelected(final Path path) {

		getChildren().remove(foldersPane);

		listeners.forEach(listener -> listener.fileSelected(path));

	}

	private void backFromFoldersPane() {

		getChildren().remove(foldersPane);

		foldersPane.reset();

	}

	public void hideAddUpdateGroupPane() {

		getChildren().remove(addUpdateGroupPane);

	}

	public void addUpdateGroupClicked() {

		getChildren().remove(addUpdateGroupPane);

		listeners.forEach(listener -> listener.addUpdateGroupRequested(addUpdateGroupPane.getGroupName(),
				addUpdateGroupPane.getSelectedIds()));

	}

	public void deleteGroupClicked() {

		getChildren().remove(addUpdateGroupPane);

		listeners.forEach(listener -> listener.deleteGroupRequested());

	}

	private MessagePane getMessagePane(EntityId entityId) {

		if (!messagePanes.containsKey(entityId)) {
			MessagePane messagePane = new MessagePane(entityId, unreadProperty);
			messagePane.addListener(this);
			messagePanes.put(entityId, messagePane);
		}

		return messagePanes.get(entityId);

	}

	private void backFromStatusInfoPane() {

		getChildren().remove(statusInfoPane);

		statusInfoPane.reset();

		listeners.forEach(listener -> listener.statusInfoClosed());

	}

	private void settingClicked(Settings setting) {

		switch (setting) {

		case SEARCH_IN_ALL_MESSAGES:

			getChildren().add(searchInAllMessagesPane);
			searchInAllMessagesPane.focusOnSearchField();

			break;

		case STARRED_MESSAGES:

			starredMessagesPane.scrollToTop();
			getChildren().add(starredMessagesPane);

			break;

		case HIDDEN_CONVERSATIONS:

			hiddenEntitiesPane.scrollToTop();
			getChildren().add(hiddenEntitiesPane);

			break;

		case EDIT_REMOTE_IPS:

			remoteIpSettingsPane.scrollToTop();
			getChildren().add(remoteIpSettingsPane);
			remoteIpSettingsPane.focusOnIpField();

			break;

		}

	}

	private void addIpClicked(final String ip) {

		listeners.forEach(listener -> listener.addIpClicked(ip));

		remoteIpSettingsPane.clearIpField();
		remoteIpSettingsPane.focusOnIpField();

	}

	private void removeIpClicked(final String ip) {

		listeners.forEach(listener -> listener.removeIpClicked(ip));

	}

	public void forwardMessagesRequested() {

		Long[] messageIds = fwdMessageIds.getAndSet(null);
		EntityId entityId = fwdSelectionPane.getSelectedEntityId();

		ObservableList<Node> children = getChildren();
		while (children.size() > 1)
			children.remove(children.size() - 1);

		listeners.forEach(listener -> listener.forwardMessagesRequested(entityId, messageIds));

	}

	public void showMessagePane(EntityId entityId) {

		getChildren().add(getMessagePane(entityId));

	}

	public void showSearchResults(List<Message> hits) {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			messagePaneOnScreen.showSearchResults(hits);

	}

	public void showArchiveSearchResults(List<Message> hits) {

		starredMessagesPane.showSearchResults(hits);

	}

	public void showSearchInAllMessagesResults(List<Message> hits) {

		searchInAllMessagesPane.showSearchResults(hits);

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
	public void entityDoubleClicked(EntityId entityId) {

		getChildren().add(getMessagePane(entityId));

	}

	@Override
	public void hideMessagePaneClicked() {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			getChildren().remove(messagePaneOnScreen);

	}

	@Override
	public void showAddUpdateGroupClicked() {

		listeners.forEach(listener -> listener.showAddUpdateGroupClicked());

	}

	@Override
	public void paneScrolledToTop(final Long topMessageId) {

		listeners.forEach(listener -> listener.paneScrolledToTop(topMessageId));

	}

	@Override
	public void messagesClaimed(final Long lastMessageIdExcl, final Long firstMessageIdIncl) {

		listeners.forEach(listener -> listener.messagesClaimed(lastMessageIdExcl, firstMessageIdIncl));

	}

	@Override
	public void sendMessageClicked(final String messageTxt, final FileBuilder fileBuilder, final Long refMessageId) {

		listeners.forEach(listener -> listener.sendMessageClicked(messageTxt, fileBuilder, refMessageId));

	}

	@Override
	public void showFoldersClicked() {

		getChildren().add(foldersPane);

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
	public void forwardMessagesRequested(Long[] messageIds) {

		fwdMessageIds.set(messageIds);

		fwdSelectionPane.resetSelection();
		fwdSelectionPane.sortEntities();

		getChildren().add(fwdSelectionPane);

	}

	@Override
	public void archiveMessagesRequested(Long[] messageIds) {

		listeners.forEach(listener -> listener.archiveMessagesRequested(messageIds));

	}

	@Override
	public void deleteMessagesRequested(Long[] messageIds) {

		listeners.forEach(listener -> listener.deleteMessagesRequested(messageIds));

	}

	@Override
	public void clearConversationRequested() {

		listeners.forEach(listener -> listener.clearConversationRequested());

	}

	@Override
	public void recordButtonPressed() {

		listeners.forEach(listener -> listener.recordButtonPressed());

	}

	@Override
	public void recordEventTriggered(final Long refMessageId) {

		MessagePane messagePaneOnScreen = messagePaneOnScreenRef.get();
		if (messagePaneOnScreen != null)
			listeners.forEach(listener -> listener.recordEventTriggered(refMessageId));

	}

	@Override
	public void recordButtonReleased() {

		listeners.forEach(listener -> listener.recordButtonReleased());

	}

	@Override
	public void searchRequested(final String fulltext) {

		listeners.forEach(listener -> listener.searchRequested(fulltext));

	}

	@Override
	public void reportClicked() {

		listeners.forEach(listener -> listener.reportClicked());

	}

	@Override
	public void showEntityRequested(final EntityId entityId) {

		listeners.forEach(listener -> listener.showEntityRequested(entityId));

	}

	@Override
	public void hideEntityRequested(final EntityId entityId) {

		listeners.forEach(listener -> listener.hideEntityRequested(entityId));

	}

	@Override
	public void removeEntityRequested(EntityId entityId) {

		listeners.forEach(listener -> listener.removeEntityRequested(entityId));

	}

	@Override
	public void searchInAllMessagesRequested(final String fulltext) {

		listeners.forEach(listener -> listener.searchInAllMessagesClaimed(fulltext));

	}

	@Override
	public void moreArchivedMessagesRequested(final Long bottomMessageId) {

		listeners.forEach(listener -> listener.moreArchivedMessagesRequested(bottomMessageId));

	}

	@Override
	public void goToMessageClicked(EntityId entityId, Long messageId) {

		MessagePane messagePane = getMessagePane(entityId);

		getChildren().add(messagePane);

		messagePane.goToMessage(messageId);

	}

	@Override
	public void archiveSearchRequested(final String fulltext) {

		listeners.forEach(listener -> listener.archiveSearchRequested(fulltext));

	}

	@Override
	public void archivedMessagesClaimed(final Long lastMessageIdExcl, final Long firstMessageIdIncl) {

		listeners.forEach(listener -> listener.archivedMessagesClaimed(lastMessageIdExcl, firstMessageIdIncl));

	}

}

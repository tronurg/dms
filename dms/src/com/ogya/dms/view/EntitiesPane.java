package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.database.tables.Message;
import com.ogya.dms.structures.MessageDirection;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class EntitiesPane extends BorderPane {

	private final VBox topArea = new VBox();

	private final Button createGroupBtn = ViewFactory.newAddBtn();
	private final TextField searchTextField = new TextField();

	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(entities) {
		@Override
		public void requestFocus() {
		}
	};

	private final AddUpdateGroupPane addUpdateGroupPane = new AddUpdateGroupPane();

	private final Map<String, ContactPane> uuidContactPane = Collections
			.synchronizedMap(new HashMap<String, ContactPane>());

	private final Map<String, GroupPane> uuidGroupPane = Collections.synchronizedMap(new HashMap<String, GroupPane>());

	private final List<IContactsPane> contactListeners = Collections.synchronizedList(new ArrayList<IContactsPane>());

	private final List<IGroupsPane> groupListeners = Collections.synchronizedList(new ArrayList<IGroupsPane>());

	private final AtomicLong currentId = new AtomicLong(0);

	EntitiesPane() {

		super();

		init();

	}

	private void init() {

		addUpdateGroupPane.setOnBackAction(
				() -> groupListeners.forEach(listener -> listener.hideAddUpdateGroupPane(addUpdateGroupPane)));
		addUpdateGroupPane.setOnAddUpdateGroupAction(
				() -> groupListeners.forEach(listener -> listener.addUpdateGroupClicked(addUpdateGroupPane)));
		addUpdateGroupPane.setOnDeleteGroupAction(
				() -> groupListeners.forEach(listener -> listener.deleteGroupClicked(addUpdateGroupPane)));

		initTopArea();

		entities.setPadding(new Insets(10.0));

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setFitToWidth(true);

		setTop(topArea);
		setCenter(scrollPane);

		setPadding(Insets.EMPTY);

	}

	void addContactListener(IContactsPane listener) {

		contactListeners.add(listener);

	}

	void addGroupListener(IGroupsPane listener) {

		groupListeners.add(listener);

	}

	void addUpdateGroupPaneUpdateContact(Contact contact) {

		addUpdateGroupPane.updateContact(contact);

	}

	AddUpdateGroupPane getAddUpdateGroupPane(String groupName, Set<String> selectedUuids, boolean isNewGroup) {

		addUpdateGroupPane.resetContent(groupName, selectedUuids, isNewGroup);

		return addUpdateGroupPane;

	}

	private void initTopArea() {

		initCreateGroupBtn();
		initSearchTextField();

		topArea.getChildren().addAll(createGroupBtn, searchTextField);

	}

	private void initCreateGroupBtn() {

		createGroupBtn.getStyleClass().add("dimButton");
		createGroupBtn.setMnemonicParsing(false);
		createGroupBtn.setText(CommonMethods.translate("CREATE_GROUP"));
		createGroupBtn.setPadding(new Insets(10.0));

		createGroupBtn
				.setOnAction(e -> groupListeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(null)));

	}

	private void initSearchTextField() {

		searchTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		searchTextField.setPromptText(CommonMethods.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	void updateContact(Contact contact) {

		getContactPane(contact.getUuid()).updateContact(contact);

	}

	void updateGroup(Dgroup group) {

		getGroupPane(group.getUuid()).updateGroup(group);

	}

	void addPrivateMessageToTop(Message message, String senderName, MessageDirection messageDirection, String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.addMessageToTop(message, senderName, messageDirection);

		Long messageId = message.getId();

		if (currentId.get() < messageId) {

			currentId.set(messageId);

			entities.getChildren().remove(contactPane);
			entities.getChildren().add(0, contactPane);

			scrollPane.setVvalue(0.0);

		}

	}

	void addGroupMessageToTop(Message message, String senderName, MessageDirection messageDirection, String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.addMessageToTop(message, senderName, messageDirection);

		Long messageId = message.getId();

		if (currentId.get() < messageId) {

			currentId.set(messageId);

			entities.getChildren().remove(groupPane);
			entities.getChildren().add(0, groupPane);

			scrollPane.setVvalue(0.0);

		}

	}

	void addPrivateMessageToBottom(Message message, String senderName, MessageDirection messageDirection, String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.addMessageToBottom(message, senderName, messageDirection);

		Long messageId = message.getId();

		if (currentId.get() < messageId) {

			currentId.set(messageId);

			entities.getChildren().remove(contactPane);
			entities.getChildren().add(0, contactPane);

			scrollPane.setVvalue(0.0);

		}

	}

	void addGroupMessageToBottom(Message message, String senderName, MessageDirection messageDirection,
			String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.addMessageToBottom(message, senderName, messageDirection);

		Long messageId = message.getId();

		if (currentId.get() < messageId) {

			currentId.set(messageId);

			entities.getChildren().remove(groupPane);
			entities.getChildren().add(0, groupPane);

			scrollPane.setVvalue(0.0);

		}

	}

	void updatePrivateMessageStatus(Message message, String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.updateMessageStatus(message);

	}

	void updateGroupMessageStatus(Message message, String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.updateMessageStatus(message);

	}

	void updatePrivateMessageProgress(String uuid, Long messageId, int progress) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.updateMessageProgress(messageId, progress);

	}

	void scrollPrivatePaneToMessage(String uuid, Long mesajId) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.scrollPaneToMessage(mesajId);

	}

	void scrollGroupPaneToMessage(String groupUuid, Long mesajId) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.scrollPaneToMessage(mesajId);

	}

	void savePrivatePosition(String uuid, Long mesajId) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.savePosition(mesajId);

	}

	void saveGroupPosition(String groupUuid, Long mesajId) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.savePosition(mesajId);

	}

	void scrollToSavedPrivatePosition(String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.scrollToSavedPosition();

	}

	void scrollToSavedGroupPosition(String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.scrollToSavedPosition();

	}

	void privateRecordingStarted(String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.recordingStarted();

	}

	void privateRecordingStopped(String uuid) {

		ContactPane contactPane = getContactPane(uuid);

		contactPane.recordingStopped();

	}

	void groupRecordingStarted(String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.recordingStarted();

	}

	void groupRecordingStopped(String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.recordingStopped();

	}

	private ContactPane getContactPane(final String uuid) {

		if (!uuidContactPane.containsKey(uuid)) {

			final ContactPane contactPane = new ContactPane();

			contactPane.managedProperty().bind(contactPane.visibleProperty());

			contactPane.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || contactPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty()));

			contactPane.setOnShowMessagePane(messagePane -> {

				contactListeners.forEach(listener -> listener.showContactMessagePane(messagePane, uuid));

			});

			contactPane.addMessagePaneListener(new IMessagePane() {

				@Override
				public void showFoldersClicked() {

					contactListeners.forEach(listener -> listener.privateShowFoldersClicked(uuid));

				}

				@Override
				public void sendMessageClicked(final String message) {

					contactListeners.forEach(listener -> listener.sendPrivateMessageClicked(message, uuid));

				}

				@Override
				public void paneScrolledToTop() {

					contactListeners.forEach(listener -> listener.contactPaneScrolledToTop(uuid));

				}

				@Override
				public void messageClicked(Long messageId) {

					contactListeners.forEach(listener -> listener.messageClicked(messageId));

				}

				@Override
				public void infoClicked(Long messageId) {

					// NO ACTION HERE

				}

				@Override
				public void editClicked() {

					// NO ACTION HERE

				}

				@Override
				public void backClicked() {

					contactListeners
							.forEach(listener -> listener.hideContactMessagePane(contactPane.getMessagePane(), uuid));

				}

				@Override
				public void cancelClicked(Long messageId) {

					contactListeners.forEach(listener -> listener.cancelClicked(messageId));

				}

				public void recordButtonPressed() {

					contactListeners.forEach(listener -> listener.privateRecordButtonPressed(uuid));

				};

				public void recordEventTriggered() {

					contactListeners.forEach(listener -> listener.recordEventTriggered());

				};

				public void recordButtonReleased() {

					contactListeners.forEach(listener -> listener.recordButtonReleased());

				};

			});

			uuidContactPane.put(uuid, contactPane);

			entities.getChildren().add(0, contactPane);

		}

		return uuidContactPane.get(uuid);

	}

	private GroupPane getGroupPane(final String groupUuid) {

		if (!uuidGroupPane.containsKey(groupUuid)) {

			final GroupPane groupPane = new GroupPane();

			groupPane.managedProperty().bind(groupPane.visibleProperty());

			groupPane.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || groupPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty()));

			groupPane.setOnShowMessagePane(messagePane -> {

				groupListeners.forEach(listener -> listener.showGroupMessagePane(messagePane, groupUuid));

			});

			groupPane.addMessagePaneListener(new IMessagePane() {

				@Override
				public void showFoldersClicked() {

					groupListeners.forEach(listener -> listener.groupShowFoldersClicked(groupUuid));

				}

				@Override
				public void sendMessageClicked(final String message) {

					groupListeners.forEach(listener -> listener.sendGroupMessageClicked(message, groupUuid));

				}

				@Override
				public void paneScrolledToTop() {

					groupListeners.forEach(listener -> listener.groupPaneScrolledToTop(groupUuid));

				}

				@Override
				public void messageClicked(Long messageId) {

					groupListeners.forEach(listener -> listener.messageClicked(messageId));

				}

				@Override
				public void infoClicked(Long messageId) {

					groupListeners.forEach(listener -> listener.infoClicked(messageId));

				}

				@Override
				public void editClicked() {

					groupListeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(groupUuid));

				}

				@Override
				public void backClicked() {

					groupListeners
							.forEach(listener -> listener.hideGroupMessagePane(groupPane.getMessagePane(), groupUuid));

				}

				@Override
				public void cancelClicked(Long messageId) {

					groupListeners.forEach(listener -> listener.cancelClicked(messageId));

				}

				public void recordButtonPressed() {

					groupListeners.forEach(listener -> listener.groupRecordButtonPressed(groupUuid));

				};

				public void recordEventTriggered() {

					groupListeners.forEach(listener -> listener.recordEventTriggered());

				};

				public void recordButtonReleased() {

					groupListeners.forEach(listener -> listener.recordButtonReleased());

				};

			});

			uuidGroupPane.put(groupUuid, groupPane);

			entities.getChildren().add(0, groupPane);

		}

		return uuidGroupPane.get(groupUuid);

	}

}

interface IContactsPane {

	void showContactMessagePane(MessagePane messagePane, String uuid);

	void hideContactMessagePane(MessagePane messagePane, String uuid);

	void contactPaneScrolledToTop(String uuid);

	void sendPrivateMessageClicked(String messageTxt, String uuid);

	void privateShowFoldersClicked(String uuid);

	void messageClicked(Long messageId);

	void cancelClicked(Long messageId);

	void privateRecordButtonPressed(String uuid);

	void recordEventTriggered();

	void recordButtonReleased();

}

interface IGroupsPane {

	void showAddUpdateGroupPaneClicked(String groupUuid);

	void hideAddUpdateGroupPane(AddUpdateGroupPane addUpdateGroupPane);

	void addUpdateGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void deleteGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void showGroupMessagePane(MessagePane messagePane, String groupUuid);

	void hideGroupMessagePane(MessagePane messagePane, String groupUuid);

	void groupPaneScrolledToTop(String groupUuid);

	void sendGroupMessageClicked(String messageTxt, String groupUuid);

	void groupShowFoldersClicked(String groupUuid);

	void messageClicked(Long messageId);

	void infoClicked(Long messageId);

	void cancelClicked(Long messageId);

	void groupRecordButtonPressed(String groupUuid);

	void recordEventTriggered();

	void recordButtonReleased();

}

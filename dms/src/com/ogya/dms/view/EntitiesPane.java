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
import com.ogya.dms.structures.ReceiverType;
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

	private final List<IEntitiesPane> entityListeners = Collections.synchronizedList(new ArrayList<IEntitiesPane>());

	private final AtomicLong currentId = new AtomicLong(0);

	EntitiesPane() {

		super();

		init();

	}

	private void init() {

		addUpdateGroupPane.setOnBackAction(
				() -> entityListeners.forEach(listener -> listener.hideAddUpdateGroupPane(addUpdateGroupPane)));
		addUpdateGroupPane.setOnAddUpdateGroupAction(
				() -> entityListeners.forEach(listener -> listener.addUpdateGroupClicked(addUpdateGroupPane)));
		addUpdateGroupPane.setOnDeleteGroupAction(
				() -> entityListeners.forEach(listener -> listener.deleteGroupClicked(addUpdateGroupPane)));

		initTopArea();

		entities.setPadding(new Insets(10.0));

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setFitToWidth(true);

		setTop(topArea);
		setCenter(scrollPane);

		setPadding(Insets.EMPTY);

	}

	void addEntityListener(IEntitiesPane listener) {

		entityListeners.add(listener);

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
				.setOnAction(e -> entityListeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(null)));

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

	void addMessage(Message message, String senderName, boolean isOutgoing, String entityUuid) {

		switch (message.getReceiverType()) {

		case PRIVATE: {

			ContactPane contactPane = getContactPane(entityUuid);

			contactPane.addMessage(message, senderName, isOutgoing);

			Long messageId = message.getId();

			if (currentId.get() < messageId) {

				currentId.set(messageId);

				entities.getChildren().remove(contactPane);
				entities.getChildren().add(0, contactPane);

				scrollPane.setVvalue(0.0);

			}

			break;

		}

		case GROUP: {

			GroupPane groupPane = getGroupPane(entityUuid);

			groupPane.addMessage(message, senderName, isOutgoing);

			Long messageId = message.getId();

			if (currentId.get() < messageId) {

				currentId.set(messageId);

				entities.getChildren().remove(groupPane);
				entities.getChildren().add(0, groupPane);

				scrollPane.setVvalue(0.0);

			}

			break;

		}

		default:

			break;

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

				entityListeners.forEach(listener -> listener.showMessagePane(messagePane, uuid, ReceiverType.PRIVATE));

			});

			contactPane.setOnHideMessagePane(messagePane -> {

				entityListeners.forEach(listener -> listener.hideMessagePane(messagePane, uuid));

			});

			contactPane.addMessagePaneListener(newMessagePaneListener(uuid, ReceiverType.PRIVATE));

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

				entityListeners
						.forEach(listener -> listener.showMessagePane(messagePane, groupUuid, ReceiverType.GROUP));

			});

			groupPane.setOnHideMessagePane(messagePane -> {

				entityListeners.forEach(listener -> listener.hideMessagePane(messagePane, groupUuid));

			});

			groupPane.addMessagePaneListener(newMessagePaneListener(groupUuid, ReceiverType.GROUP));

			uuidGroupPane.put(groupUuid, groupPane);

			entities.getChildren().add(0, groupPane);

		}

		return uuidGroupPane.get(groupUuid);

	}

	private IMessagePane newMessagePaneListener(final String uuid, final ReceiverType receiverType) {

		return new IMessagePane() {

			@Override
			public void showFoldersClicked() {

				entityListeners.forEach(listener -> listener.showFoldersClicked(uuid, receiverType));

			}

			public void reportClicked() {

				entityListeners.forEach(listener -> listener.reportClicked(uuid, receiverType));

			};

			@Override
			public void sendMessageClicked(final String message) {

				entityListeners.forEach(listener -> listener.sendMessageClicked(message, uuid, receiverType));

			}

			@Override
			public void paneScrolledToTop() {

				entityListeners.forEach(listener -> listener.paneScrolledToTop(uuid, receiverType));

			}

			@Override
			public void messageClicked(Long messageId) {

				entityListeners.forEach(listener -> listener.messageClicked(messageId));

			}

			@Override
			public void infoClicked(Long messageId) {

				entityListeners.forEach(listener -> listener.infoClicked(messageId));

			}

			@Override
			public void editClicked() {

				entityListeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(uuid));

			}

			@Override
			public void cancelClicked(Long messageId) {

				entityListeners.forEach(listener -> listener.cancelClicked(messageId));

			}

			public void recordButtonPressed() {

				entityListeners.forEach(listener -> listener.recordButtonPressed(uuid, receiverType));

			};

			public void recordEventTriggered() {

				entityListeners.forEach(listener -> listener.recordEventTriggered());

			};

			public void recordButtonReleased() {

				entityListeners.forEach(listener -> listener.recordButtonReleased());

			};

		};

	}

}

interface IEntitiesPane {

	void showAddUpdateGroupPaneClicked(String groupUuid);

	void hideAddUpdateGroupPane(AddUpdateGroupPane addUpdateGroupPane);

	void addUpdateGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void deleteGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void showMessagePane(MessagePane messagePane, String uuid, ReceiverType receiverType);

	void hideMessagePane(MessagePane messagePane, String uuid);

	void paneScrolledToTop(String uuid, ReceiverType receiverType);

	void sendMessageClicked(String messageTxt, String uuid, ReceiverType receiverType);

	void showFoldersClicked(String uuid, ReceiverType receiverType);

	void reportClicked(String uuid, ReceiverType receiverType);

	void messageClicked(Long messageId);

	void infoClicked(Long messageId);

	void cancelClicked(Long messageId);

	void recordButtonPressed(String uuid, ReceiverType receiverType);

	void recordEventTriggered();

	void recordButtonReleased();

}

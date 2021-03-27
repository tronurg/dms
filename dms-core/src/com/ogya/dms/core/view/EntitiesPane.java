package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.FileBuilder;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class EntitiesPane extends BorderPane {

	private final double gap = ViewFactory.getGap();

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

	private final Map<Long, ContactPane> idContactPane = Collections.synchronizedMap(new HashMap<Long, ContactPane>());

	private final Map<Long, GroupPane> idGroupPane = Collections.synchronizedMap(new HashMap<Long, GroupPane>());

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

		entities.setPadding(new Insets(2 * gap));

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

		createGroupBtn.getStyleClass().add("dim-label");
		createGroupBtn.setMnemonicParsing(false);
		createGroupBtn.setText(CommonMethods.translate("CREATE_GROUP"));
		createGroupBtn.setPadding(new Insets(2 * gap));

		createGroupBtn
				.setOnAction(e -> entityListeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(null)));

	}

	private void initSearchTextField() {

		searchTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		searchTextField.setPromptText(CommonMethods.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	void updateContact(Contact contact) {

		getContactPane(contact.getId()).updateContact(contact);

	}

	void updateGroup(Dgroup group) {

		getGroupPane(group.getId()).updateGroup(group);

	}

	void addMessage(Message message) {

		switch (message.getReceiverType()) {

		case CONTACT: {

			ContactPane contactPane = getContactPane(message.getContact().getId());

			contactPane.addUpdateMessage(message);

			Long messageId = message.getId();

			if (currentId.get() < messageId) {

				currentId.set(messageId);

				entities.getChildren().remove(contactPane);
				entities.getChildren().add(0, contactPane);

				scrollPane.setVvalue(0.0);

			}

			break;

		}

		case GROUP_OWNER:
		case GROUP_MEMBER: {

			GroupPane groupPane = getGroupPane(message.getDgroup().getId());

			groupPane.addUpdateMessage(message);

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

	void updatePrivateMessage(Message message) {

		ContactPane contactPane = getContactPane(message.getContact().getId());

		contactPane.addUpdateMessage(message);

	}

	void updateGroupMessage(Message message) {

		GroupPane groupPane = getGroupPane(message.getDgroup().getId());

		groupPane.addUpdateMessage(message);

	}

	void updatePrivateMessageProgress(Long id, Long messageId, int progress) {

		ContactPane contactPane = getContactPane(id);

		contactPane.updateMessageProgress(messageId, progress);

	}

	void scrollPrivatePaneToMessage(Long id, Long messageId) {

		ContactPane contactPane = getContactPane(id);

		contactPane.scrollPaneToMessage(messageId);

	}

	void scrollGroupPaneToMessage(Long id, Long messageId) {

		GroupPane groupPane = getGroupPane(id);

		groupPane.scrollPaneToMessage(messageId);

	}

	void savePrivatePosition(Long id, Long messageId) {

		ContactPane contactPane = getContactPane(id);

		contactPane.savePosition(messageId);

	}

	void saveGroupPosition(Long id, Long messageId) {

		GroupPane groupPane = getGroupPane(id);

		groupPane.savePosition(messageId);

	}

	void scrollToSavedPrivatePosition(Long id) {

		ContactPane contactPane = getContactPane(id);

		contactPane.scrollToSavedPosition();

	}

	void scrollToSavedGroupPosition(Long id) {

		GroupPane groupPane = getGroupPane(id);

		groupPane.scrollToSavedPosition();

	}

	private ContactPane getContactPane(final Long id) {

		if (!idContactPane.containsKey(id)) {

			final ContactPane contactPane = new ContactPane();

			contactPane.managedProperty().bind(contactPane.visibleProperty());

			contactPane.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || contactPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty()));

			contactPane.setOnShowMessagePane(messagePane -> {

				entityListeners.forEach(listener -> listener.showMessagePane(id, messagePane));

			});

			contactPane.setOnHideMessagePane(messagePane -> {

				entityListeners.forEach(listener -> listener.hideMessagePane(id, messagePane));

			});

			contactPane.addMessagePaneListener(newMessagePaneListener(id));

			idContactPane.put(id, contactPane);

			entities.getChildren().add(0, contactPane);

		}

		return idContactPane.get(id);

	}

	private GroupPane getGroupPane(final Long id) {

		if (!idGroupPane.containsKey(id)) {

			final GroupPane groupPane = new GroupPane();

			groupPane.managedProperty().bind(groupPane.visibleProperty());

			groupPane.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || groupPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty()));

			groupPane.setOnShowMessagePane(messagePane -> {

				entityListeners.forEach(listener -> listener.showMessagePane(-id, messagePane));

			});

			groupPane.setOnHideMessagePane(messagePane -> {

				entityListeners.forEach(listener -> listener.hideMessagePane(-id, messagePane));

			});

			groupPane.addMessagePaneListener(newMessagePaneListener(-id));

			idGroupPane.put(id, groupPane);

			entities.getChildren().add(0, groupPane);

		}

		return idGroupPane.get(id);

	}

	private IMessagePane newMessagePaneListener(final Long id) {

		return new IMessagePane() {

			@Override
			public void showFoldersClicked() {

				entityListeners.forEach(listener -> listener.showFoldersClicked(id));

			}

			@Override
			public void reportClicked() {

				entityListeners.forEach(listener -> listener.reportClicked());

			}

			@Override
			public void sendMessageClicked(final String message, final FileBuilder fileBuilder,
					final Long refMessageId) {

				entityListeners
						.forEach(listener -> listener.sendMessageClicked(id, message, fileBuilder, refMessageId));

			}

			@Override
			public void paneScrolledToTop(Long topMessageId) {

				entityListeners.forEach(listener -> listener.paneScrolledToTop(id, topMessageId));

			}

			@Override
			public void messagesClaimed(Long lastMessageIdExcl, Long firstMessageIdIncl) {

				entityListeners
						.forEach(listener -> listener.messagesClaimed(id, lastMessageIdExcl, firstMessageIdIncl));

			}

			@Override
			public void attachmentClicked(Long messageId) {

				entityListeners.forEach(listener -> listener.attachmentClicked(messageId));

			}

			@Override
			public void infoClicked(Long messageId) {

				entityListeners.forEach(listener -> listener.infoClicked(messageId));

			}

			@Override
			public void editClicked() {

				entityListeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(id));

			}

			@Override
			public void deleteMessagesRequested(Long... messageIds) {

				entityListeners.forEach(listener -> listener.deleteMessagesRequested(messageIds));

			}

			@Override
			public void archiveMessagesRequested(Long... messageIds) {

				entityListeners.forEach(listener -> listener.archiveMessagesRequested(messageIds));

			}

			@Override
			public void recordButtonPressed() {

				entityListeners.forEach(listener -> listener.recordButtonPressed());

			}

			@Override
			public void recordEventTriggered(final Long refMessageId) {

				entityListeners.forEach(listener -> listener.recordEventTriggered(refMessageId));

			}

			@Override
			public void recordButtonReleased() {

				entityListeners.forEach(listener -> listener.recordButtonReleased());

			}

		};

	}

}

interface IEntitiesPane {

	void showAddUpdateGroupPaneClicked(Long id);

	void hideAddUpdateGroupPane(AddUpdateGroupPane addUpdateGroupPane);

	void addUpdateGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void deleteGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void showMessagePane(Long id, MessagePane messagePane);

	void hideMessagePane(Long id, MessagePane messagePane);

	void paneScrolledToTop(Long id, Long topMessageId);

	void messagesClaimed(Long id, Long lastMessageIdExcl, Long firstMessageIdIncl);

	void sendMessageClicked(Long id, String messageTxt, FileBuilder fileBuilder, Long refMessageId);

	void showFoldersClicked(Long id);

	void reportClicked();

	void attachmentClicked(Long messageId);

	void infoClicked(Long messageId);

	void deleteMessagesRequested(Long... messageIds);

	void archiveMessagesRequested(Long... messageIds);

	void recordButtonPressed();

	void recordEventTriggered(Long refMessageId);

	void recordButtonReleased();

}

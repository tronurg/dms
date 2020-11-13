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
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

class EntitiesPane extends BorderPane {

	private static final double GAP = ViewFactory.GAP;

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

		entities.setPadding(new Insets(2 * GAP));

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
		createGroupBtn.setStyle(String.format("-fx-font-size: %f;", ViewFactory.getFontSize()));
		createGroupBtn.setMnemonicParsing(false);
		createGroupBtn.setText(CommonMethods.translate("CREATE_GROUP"));
		createGroupBtn.setPadding(new Insets(2 * GAP));

		createGroupBtn
				.setOnAction(e -> entityListeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(null)));

	}

	private void initSearchTextField() {

		searchTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		searchTextField.setPromptText(CommonMethods.translate("FIND"));
		searchTextField.setFocusTraversable(false);
		searchTextField.setFont(Font.font(ViewFactory.getFontSize()));

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

			contactPane.addMessage(message);

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

			groupPane.addMessage(message);

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

	void updatePrivateMessageStatus(Message message) {

		ContactPane contactPane = getContactPane(message.getContact().getId());

		contactPane.updateMessageStatus(message);

	}

	void updateGroupMessageStatus(Message message) {

		GroupPane groupPane = getGroupPane(message.getDgroup().getId());

		groupPane.updateMessageStatus(message);

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

	void privateRecordingStarted(Long id) {

		ContactPane contactPane = getContactPane(id);

		contactPane.recordingStarted();

	}

	void privateRecordingStopped(Long id) {

		ContactPane contactPane = getContactPane(id);

		contactPane.recordingStopped();

	}

	void groupRecordingStarted(Long id) {

		GroupPane groupPane = getGroupPane(id);

		groupPane.recordingStarted();

	}

	void groupRecordingStopped(Long id) {

		GroupPane groupPane = getGroupPane(id);

		groupPane.recordingStopped();

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

				entityListeners.forEach(listener -> listener.showMessagePane(messagePane, id));

			});

			contactPane.setOnHideMessagePane(messagePane -> {

				entityListeners.forEach(listener -> listener.hideMessagePane(messagePane, id));

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

				entityListeners.forEach(listener -> listener.showMessagePane(messagePane, -id));

			});

			groupPane.setOnHideMessagePane(messagePane -> {

				entityListeners.forEach(listener -> listener.hideMessagePane(messagePane, -id));

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

			public void reportClicked() {

				entityListeners.forEach(listener -> listener.reportClicked(id));

			};

			@Override
			public void sendMessageClicked(final String message) {

				entityListeners.forEach(listener -> listener.sendMessageClicked(message, id));

			}

			@Override
			public void paneScrolledToTop(Long topMessageId) {

				entityListeners.forEach(listener -> listener.paneScrolledToTop(id, topMessageId));

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

				entityListeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(id));

			}

			@Override
			public void cancelClicked(Long messageId) {

				entityListeners.forEach(listener -> listener.cancelClicked(messageId));

			}

			public void recordButtonPressed() {

				entityListeners.forEach(listener -> listener.recordButtonPressed(id));

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

	void showAddUpdateGroupPaneClicked(Long id);

	void hideAddUpdateGroupPane(AddUpdateGroupPane addUpdateGroupPane);

	void addUpdateGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void deleteGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void showMessagePane(MessagePane messagePane, Long id);

	void hideMessagePane(MessagePane messagePane, Long id);

	void paneScrolledToTop(Long id, Long topMessageId);

	void sendMessageClicked(String messageTxt, Long id);

	void showFoldersClicked(Long id);

	void reportClicked(Long id);

	void messageClicked(Long messageId);

	void infoClicked(Long messageId);

	void cancelClicked(Long messageId);

	void recordButtonPressed(Long id);

	void recordEventTriggered();

	void recordButtonReleased();

}

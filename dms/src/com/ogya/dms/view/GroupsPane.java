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

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

class GroupsPane extends TitledPane {

	private final BorderPane borderPane = new BorderPane();
	private final Button createGroupBtn = ViewFactory.newAddBtn();
	private final VBox groups = new VBox();

	private final AddUpdateGroupPane addUpdateGroupPane = new AddUpdateGroupPane();

	private final Map<String, GroupPane> uuidGroupPane = Collections.synchronizedMap(new HashMap<String, GroupPane>());

	private final List<IGroupsPane> listeners = Collections.synchronizedList(new ArrayList<IGroupsPane>());

	private final AtomicLong currentId = new AtomicLong(0);

	GroupsPane() {

		super();

		init();

	}

	private void init() {

		addUpdateGroupPane.setOnBackAction(
				() -> listeners.forEach(listener -> listener.hideAddUpdateGroupPane(addUpdateGroupPane)));
		addUpdateGroupPane.setOnAddUpdateGroupAction(
				() -> listeners.forEach(listener -> listener.addUpdateGroupClicked(addUpdateGroupPane)));
		addUpdateGroupPane.setOnDeleteGroupAction(
				() -> listeners.forEach(listener -> listener.deleteGroupClicked(addUpdateGroupPane)));

		initCreateGroupBtn();

		setText(CommonMethods.translate("GROUPS"));

		groups.setPadding(new Insets(10.0));

		ScrollPane scrollPane = new ScrollPane(groups);
		scrollPane.setFitToWidth(true);

		borderPane.setTop(createGroupBtn);
		borderPane.setCenter(scrollPane);

		borderPane.setPadding(Insets.EMPTY);

		setContent(borderPane);

	}

	void addListener(IGroupsPane listener) {

		listeners.add(listener);

	}

	void addUpdateGroupPaneUpdateContact(Contact contact) {

		addUpdateGroupPane.updateContact(contact);

	}

	AddUpdateGroupPane getAddUpdateGroupPane(String groupName, Set<String> selectedUuids, boolean isNewGroup) {

		addUpdateGroupPane.resetContent(groupName, selectedUuids, isNewGroup);

		return addUpdateGroupPane;

	}

	private void initCreateGroupBtn() {

		createGroupBtn.setMnemonicParsing(false);
		createGroupBtn.setText(CommonMethods.translate("CREATE_GROUP"));
		createGroupBtn.setTextFill(Color.GRAY);
		createGroupBtn.setPadding(new Insets(10.0));

		createGroupBtn.setOnAction(e -> listeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(null)));

	}

	void updateGroup(Dgroup group) {

		getGroupPane(group.getUuid()).updateGroup(group);

	}

	void addMessageToTop(Message message, String senderName, MessageDirection messageDirection, String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.addMessageToTop(message, senderName, messageDirection);

		Long messageId = message.getId();

		if (currentId.get() < messageId) {

			currentId.set(messageId);

			groups.getChildren().remove(groupPane);
			groups.getChildren().add(0, groupPane);

		}

	}

	void addMessageToBottom(Message message, String senderName, MessageDirection messageDirection, String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.addMessageToBottom(message, senderName, messageDirection);

		Long messageId = message.getId();

		if (currentId.get() < messageId) {

			currentId.set(messageId);

			groups.getChildren().remove(groupPane);
			groups.getChildren().add(0, groupPane);

		}

	}

	void updateMessage(Message message, String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.updateMessage(message);

	}

	void scrollPaneToMessage(String groupUuid, Long mesajId) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.scrollPaneToMessage(mesajId);

	}

	void savePosition(String groupUuid, Long mesajId) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.savePosition(mesajId);

	}

	void scrollToSavedPosition(String groupUuid) {

		GroupPane groupPane = getGroupPane(groupUuid);

		groupPane.scrollToSavedPosition();

	}

	private GroupPane getGroupPane(final String groupUuid) {

		if (!uuidGroupPane.containsKey(groupUuid)) {

			GroupPane groupPane = new GroupPane();

			groupPane.setOnShowMessagePane(messagePane -> {

				listeners.forEach(listener -> listener.showGroupMessagePane(messagePane, groupUuid));

			});

			groupPane.setOnHideMessagePane(messagePane -> {

				listeners.forEach(listener -> listener.hideGroupMessagePane(messagePane, groupUuid));

			});

			groupPane.setOnEditGroupAction(() -> {

				listeners.forEach(listener -> listener.showAddUpdateGroupPaneClicked(groupUuid));

			});

			groupPane.setOnSendMessageAction(messageTxt -> {

				listeners.forEach(listener -> listener.sendGroupMessageClicked(messageTxt, groupUuid));

			});

			groupPane.setOnShowFoldersAction(() -> {

				listeners.forEach(listener -> listener.groupShowFoldersClicked(groupUuid));

			});

			groupPane.setOnPaneScrolledToTop(() -> {

				listeners.forEach(listener -> listener.groupPaneScrolledToTop(groupUuid));

			});

			uuidGroupPane.put(groupUuid, groupPane);

			groups.getChildren().add(0, groupPane);

			setExpanded(true);

		}

		return uuidGroupPane.get(groupUuid);

	}

}

interface IGroupsPane {

	void showAddUpdateGroupPaneClicked(String groupUuid);

	void hideAddUpdateGroupPane(AddUpdateGroupPane addUpdateGroupPane);

	void addUpdateGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void deleteGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void showGroupMessagePane(MessagePane messagePane, String groupUuid);

	void hideGroupMessagePane(MessagePane messagePane, String groupUuid);

	void sendGroupMessageClicked(String messageTxt, String groupUuid);

	void groupShowFoldersClicked(String groupUuid);

	void groupPaneScrolledToTop(String groupUuid);

}

package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.FileBuilder;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class EntitiesPane extends BorderPane {

	private final double gap = ViewFactory.getGap();

	private final BooleanProperty unreadProperty;

	private final VBox topArea = new VBox();

	private final Button createGroupBtn = ViewFactory.newAddBtn();
	private final TextField searchTextField = new TextField();

	private final VBox entities = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(entities) {
		@Override
		public void requestFocus() {
		}
	};

	private final AddUpdateGroupPane addUpdateGroupPane;

	private final Map<EntityId, EntityPane> entityIdPane = Collections
			.synchronizedMap(new HashMap<EntityId, EntityPane>());

	private final List<IEntitiesPane> listeners = Collections.synchronizedList(new ArrayList<IEntitiesPane>());

	private final Comparator<Node> entitiesSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof EntityPane && arg1 instanceof EntityPane))
				return 0;

			EntityPane group0 = (EntityPane) arg0;
			EntityPane group1 = (EntityPane) arg1;

			return group1.getMessagePane().getLastEventTime().compareTo(group0.getMessagePane().getLastEventTime());

		}

	};

	EntitiesPane(BooleanProperty unreadProperty) {

		super();

		this.unreadProperty = unreadProperty;
		this.addUpdateGroupPane = new AddUpdateGroupPane(unreadProperty);

		init();

	}

	private void init() {

		addUpdateGroupPane.setOnBackAction(
				() -> listeners.forEach(listener -> listener.hideAddUpdateGroupPane(addUpdateGroupPane)));
		addUpdateGroupPane.setOnAddUpdateGroupAction(
				() -> listeners.forEach(listener -> listener.addUpdateGroupClicked(addUpdateGroupPane)));
		addUpdateGroupPane.setOnDeleteGroupAction(
				() -> listeners.forEach(listener -> listener.deleteGroupClicked(addUpdateGroupPane)));

		initTopArea();

		entities.setPadding(new Insets(2 * gap));

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setFitToWidth(true);

		setTop(topArea);
		setCenter(scrollPane);

		setPadding(Insets.EMPTY);

	}

	void addListener(IEntitiesPane listener) {

		listeners.add(listener);

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

		createGroupBtn.setOnAction(e -> listeners.forEach(listener -> listener.showAddUpdateGroupClicked()));

	}

	private void initSearchTextField() {

		searchTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		searchTextField.setPromptText(CommonMethods.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	void updateEntity(EntityBase entity) {

		getEntityPane(entity.getEntityId()).updateEntity(entity);

	}

	void addMessage(Message message) {

		getEntityPane(message.getEntity().getEntityId()).addUpdateMessage(message);

		FXCollections.sort(entities.getChildren(), entitiesSorter);

	}

	void updateMessage(Message message) {

		getEntityPane(message.getEntity().getEntityId()).addUpdateMessage(message);

	}

	void updateMessageProgress(EntityId entityId, Long messageId, int progress) {

		getEntityPane(entityId).getMessagePane().updateMessageProgress(messageId, progress);

	}

	void scrollPaneToMessage(EntityId entityId, Long messageId) {

		getEntityPane(entityId).getMessagePane().scrollPaneToMessage(messageId);

	}

	void savePosition(EntityId entityId, Long messageId) {

		getEntityPane(entityId).getMessagePane().savePosition(messageId);

	}

	void scrollToSavedPosition(EntityId entityId) {

		getEntityPane(entityId).getMessagePane().scrollToSavedPosition();

	}

	void allMessagesLoaded(EntityId entityId) {

		getEntityPane(entityId).getMessagePane().allMessagesLoaded();

	}

	MessagePane getMessagePane(EntityId entityId) {

		return getEntityPane(entityId).getMessagePane();

	}

	void goToMessage(EntityId entityId, Long messageId) {

		getEntityPane(entityId).getMessagePane().goToMessage(messageId);

	}

	private EntityPane getEntityPane(EntityId entityId) {

		if (!entityIdPane.containsKey(entityId)) {

			final EntityPane entityPane = new EntityPane(entityId, unreadProperty);

			entityPane.managedProperty().bind(entityPane.visibleProperty());

			entityPane.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || entityPane.getName().toLowerCase().startsWith(searchContactStr);
			}, searchTextField.textProperty()));

			entityPane.setOnMouseClicked(e -> {

				if (!(Objects.equals(e.getButton(), MouseButton.PRIMARY) && e.getClickCount() == 2
						&& e.isStillSincePress()))
					return;

				listeners.forEach(listener -> listener.showMessagePane(entityPane.getMessagePane()));

			});

			entityPane.getMessagePane().addListener(newMessagePaneListener());

			entityIdPane.put(entityId, entityPane);

			entities.getChildren().add(0, entityPane);

		}

		return entityIdPane.get(entityId);

	}

	private IMessagePane newMessagePaneListener() {

		return new IMessagePane() {

			@Override
			public void hideMessagePaneClicked() {
				listeners.forEach(listener -> listener.hideMessagePaneClicked());
			}

			@Override
			public void showAddUpdateGroupClicked() {
				listeners.forEach(listener -> listener.showAddUpdateGroupClicked());
			}

			@Override
			public void showFoldersClicked() {
				listeners.forEach(listener -> listener.showFoldersClicked());
			}

			@Override
			public void reportClicked() {
				listeners.forEach(listener -> listener.reportClicked());
			}

			@Override
			public void sendMessageClicked(final String message, final FileBuilder fileBuilder,
					final Long refMessageId) {
				listeners.forEach(listener -> listener.sendMessageClicked(message, fileBuilder, refMessageId));
			}

			@Override
			public void paneScrolledToTop(Long topMessageId) {
				listeners.forEach(listener -> listener.paneScrolledToTop(topMessageId));
			}

			@Override
			public void messagesClaimed(Long lastMessageIdExcl, Long firstMessageIdIncl) {
				listeners.forEach(listener -> listener.messagesClaimed(lastMessageIdExcl, firstMessageIdIncl));
			}

			@Override
			public void attachmentClicked(Long messageId) {
				listeners.forEach(listener -> listener.attachmentClicked(messageId));
			}

			@Override
			public void infoClicked(Long messageId) {
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
				listeners.forEach(listener -> listener.recordEventTriggered(refMessageId));
			}

			@Override
			public void recordButtonReleased() {
				listeners.forEach(listener -> listener.recordButtonReleased());
			}

		};

	}

}

interface IEntitiesPane extends IMessagePane {

	void hideAddUpdateGroupPane(AddUpdateGroupPane addUpdateGroupPane);

	void addUpdateGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void deleteGroupClicked(AddUpdateGroupPane addUpdateGroupPane);

	void showMessagePane(MessagePane messagePane);

}

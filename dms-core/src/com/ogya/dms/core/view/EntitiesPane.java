package com.ogya.dms.core.view;

import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

class EntitiesPane extends BorderPane {

	private static final double GAP = ViewFactory.GAP;

	private final Button createGroupBtn = ViewFactory.newAddBtn();
	private final EntitiesPaneBase entitiesPane = new EntitiesPaneBase(true);

	EntitiesPane() {

		super();

		init();

	}

	private void init() {

		initCreateGroupBtn();

		setTop(createGroupBtn);
		setCenter(entitiesPane);

		setPadding(Insets.EMPTY);

	}

	private void initCreateGroupBtn() {

		createGroupBtn.getStyleClass().addAll("dim-label");
		createGroupBtn.setMnemonicParsing(false);
		createGroupBtn.setText(Commons.translate("CREATE_GROUP"));
		createGroupBtn.setPadding(new Insets(2 * GAP));

	}

	void addEntitiesPaneListener(IEntitiesPane listener) {

		entitiesPane.addListener(listener);

	}

	void setOnAddUpdateGroupClicked(Runnable runnable) {

		createGroupBtn.setOnAction(e -> runnable.run());

	}

	void updateEntity(EntityBase entity) {

		entitiesPane.updateEntity(entity, entity.getViewStatus() == ViewStatus.DEFAULT);

	}

	void sortEntities() {

		entitiesPane.sortEntities();

	}

	void updateMessageStatus(EntityId entityId, Message message) {

		entitiesPane.updateMessageStatus(entityId, message);

	}

	void moveEntityToTop(EntityId entityId) {

		entitiesPane.moveEntityToTop(entityId);

	}

}

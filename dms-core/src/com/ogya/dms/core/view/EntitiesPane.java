package com.ogya.dms.core.view;

import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

class EntitiesPane extends BorderPane {

	private final Label createGroupLbl = new Label(Commons.translate("CREATE_GROUP"));
	private final Button createGroupBtn = ViewFactory.newAddBtnWithLbl(createGroupLbl);
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

		createGroupBtn.getStyleClass().addAll("padding-2");
		createGroupLbl.getStyleClass().addAll("dim-label");
		createGroupLbl.setMnemonicParsing(false);

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

package com.dms.core.view;

import com.dms.core.database.tables.DmsEntity;
import com.dms.core.database.tables.EntityId;
import com.dms.core.database.tables.Message;
import com.dms.core.structures.ViewStatus;
import com.dms.core.util.Commons;
import com.dms.core.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class HiddenEntitiesPane extends BorderPane {

	private final HBox topPane = new HBox();
	private final Button backBtn;
	private final Label headingLabel = new Label(Commons.translate("HIDDEN_CONVERSATIONS"));

	private final EntitiesPaneBase entitiesPane = new EntitiesPaneBase(false);

	HiddenEntitiesPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);

		init();

	}

	private void init() {

		initTopPane();

		setTop(topPane);
		setCenter(entitiesPane);

		setPadding(Insets.EMPTY);

	}

	private void initTopPane() {

		initHeadingLabel();

		topPane.getStyleClass().addAll("top-pane");

		topPane.getChildren().addAll(backBtn, headingLabel);

	}

	private void initHeadingLabel() {

		headingLabel.getStyleClass().addAll("black-label", "em15", "bold");

	}

	void addEntitiesPaneListener(IEntitiesPane listener) {

		entitiesPane.addListener(listener);

	}

	void setOnBackAction(final Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void updateEntity(DmsEntity entity) {

		entitiesPane.updateEntity(entity, entity.getViewStatus() == ViewStatus.ARCHIVED);

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

	void scrollToTop() {

		entitiesPane.scrollToTop();

	}

}

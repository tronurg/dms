package com.ogya.dms.core.view;

import java.util.Objects;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HiddenEntitiesPane extends BorderPane {

	private final double gap = ViewFactory.getGap();

	private final double viewFactor = ViewFactory.getViewFactor();

	private final HBox topPane = new HBox(2 * gap);
	private final Button backBtn;
	private final Label headingLabel = new Label(CommonMethods.translate("HIDDEN_CONVERSATIONS"));

	private final EntitiesPaneBase entitiesPane = new EntitiesPaneBase();

	HiddenEntitiesPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty);

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

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(gap));
		topPane.setAlignment(Pos.CENTER_LEFT);

		topPane.getChildren().addAll(backBtn, headingLabel);

	}

	private void initHeadingLabel() {

		headingLabel.getStyleClass().add("black-label");
		headingLabel.setFont(Font.font(null, FontWeight.BOLD, 22.0 * viewFactor));

	}

	void addEntitiesPaneListener(IEntitiesPane listener) {

		entitiesPane.addListener(listener);

	}

	void setOnBackAction(final Runnable runnable) {

		backBtn.setOnAction(e -> {
			entitiesPane.resetDeleteMode();
			runnable.run();
		});

	}

	void updateEntity(EntityBase entity) {

		entitiesPane.updateEntity(entity, Objects.equals(entity.getViewStatus(), ViewStatus.ARCHIVED));

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

package com.ogya.dms.core.view;

import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class GroupPaneBase extends HBox {

	private final double unitSize = 24.0 * ViewFactory.getViewFactor();

	private final Group profilePicture = new Group();
	private final Circle statusCircle = new Circle(unitSize);
	private final Circle profileRound = new Circle(unitSize * 0.8);
	private final Label initialLabel = new Label();
	private final Label groupSign = new Label("G");

	private final VBox middlePane = new VBox();
	private final Label nameLabel = new Label();
	private final Label commentLabel = new Label();

	GroupPaneBase() {

		super(ViewFactory.getGap());

		init();

	}

	private final void init() {

		initProfilePicture();
		initMiddlePane();

		getChildren().addAll(profilePicture, new Separator(Orientation.VERTICAL), middlePane);

	}

	void updateGroup(Dgroup group) {

		if (group == null)
			return;

		statusCircle.setStroke(group.getStatus().getStatusColor());
		initialLabel.setText(group.getName().substring(0, 1).toUpperCase());

		nameLabel.setText(group.getName());
		commentLabel.setText(group.getComment());

	}

	String getName() {

		return nameLabel.getText();

	}

	private void initProfilePicture() {

		initStatusCircle();
		initProfileRound();
		initInitialLabel();
		initGroupSign();

		profilePicture.getChildren().addAll(statusCircle, profileRound, initialLabel, groupSign);

	}

	private void initStatusCircle() {

		statusCircle.setStrokeWidth(unitSize * 0.2);
		statusCircle.setFill(Color.TRANSPARENT);

	}

	private void initProfileRound() {

		profileRound.setFill(Color.DARKGRAY);

	}

	private void initInitialLabel() {

		initialLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		initialLabel.setStyle("-fx-text-fill: #404040;");
		initialLabel.setFont(Font.font(null, FontWeight.BOLD, unitSize));

		initialLabel.translateXProperty().bind(Bindings
				.createDoubleBinding(() -> -initialLabel.widthProperty().get() / 2, initialLabel.widthProperty()));
		initialLabel.translateYProperty().bind(Bindings
				.createDoubleBinding(() -> -initialLabel.heightProperty().get() / 2, initialLabel.heightProperty()));

	}

	private void initGroupSign() {

		groupSign.setTextFill(Color.WHITE);
		groupSign.setContentDisplay(ContentDisplay.CENTER);
		groupSign.setFont(Font.font(null, FontWeight.EXTRA_BOLD, unitSize * 0.5));

		Circle circle = new Circle(unitSize * 0.3);
		circle.setFill(Color.TOMATO);

		groupSign.setGraphic(circle);

		groupSign.setTranslateX(unitSize * 0.5);
		groupSign.setTranslateY(unitSize * 0.5);

	}

	private void initMiddlePane() {

		initNameLabel();
		initCommentLabel();

		setHgrow(middlePane, Priority.ALWAYS);

		middlePane.getChildren().addAll(nameLabel, commentLabel, new Label());

	}

	private void initNameLabel() {

		nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLabel.setFont(Font.font(null, FontWeight.BOLD, unitSize * 0.8));

		nameLabel.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
			String name = nameLabel.getText();
			if (name == null || name.isEmpty())
				return null;
			return new Tooltip(name);
		}, nameLabel.textProperty()));

	}

	private void initCommentLabel() {

		commentLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		commentLabel.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
			String comment = commentLabel.getText();
			if (comment == null || comment.isEmpty())
				return null;
			return new Tooltip(comment);
		}, commentLabel.textProperty()));

	}

}

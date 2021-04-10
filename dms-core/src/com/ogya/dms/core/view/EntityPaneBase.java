package com.ogya.dms.core.view;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class EntityPaneBase extends GridPane {

	private final double unitSize = 24.0 * ViewFactory.getViewFactor();

	private final Group profilePicture = new Group();
	private final Circle statusCircle = new Circle(unitSize);
	private final Circle profileRound = new Circle(unitSize * 0.8);
	private final Label initialLabel = new Label();
	private final Label groupSign = new Label("G");

	private final Label nameLabel = new Label();
	private final Label commentLabel = new Label();
	private final Label coordinatesLabel = new Label();

	private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);

	EntityPaneBase() {

		super();

		init();

	}

	private final void init() {

		setHgap(ViewFactory.getGap());

		initProfilePicture();
		initNameLabel();
		initCommentLabel();
		initCoordinatesLabel();

		add(profilePicture, 0, 0, 1, 3);
		add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		add(nameLabel, 2, 0, 1, 1);
		add(commentLabel, 2, 1, 1, 1);
		add(coordinatesLabel, 2, 2, 1, 1);

		Region unclickableRegion = new Region();
		unclickableRegion.addEventFilter(MouseEvent.ANY, e -> e.consume());
		add(unclickableRegion, 0, 3, 4, 1);

	}

	private void initProfilePicture() {

		GridPane.setValignment(profilePicture, VPos.TOP);

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

		groupSign.setVisible(false);
		groupSign.setTextFill(Color.WHITE);
		groupSign.setContentDisplay(ContentDisplay.CENTER);
		groupSign.setFont(Font.font(null, FontWeight.EXTRA_BOLD, unitSize * 0.5));

		Circle circle = new Circle(unitSize * 0.3);
		circle.setFill(Color.TOMATO);

		groupSign.setGraphic(circle);

		groupSign.setTranslateX(unitSize * 0.5);
		groupSign.setTranslateY(unitSize * 0.5);

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

	private void initCoordinatesLabel() {

		GridPane.setHgrow(coordinatesLabel, Priority.ALWAYS);

		coordinatesLabel.setOpacity(0.5);
		coordinatesLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	void updateEntity(EntityBase entity) {

		String name = entity.getName();

		statusCircle.setStroke(entity.getStatus().getStatusColor());
		if (!(name == null || name.isEmpty()))
			initialLabel.setText(name.substring(0, 1).toUpperCase());
		nameLabel.setText(name);
		commentLabel.setText(entity.getComment());
		coordinatesLabel.setText(entity.getLattitude() == null || entity.getLongitude() == null ? ""
				: CommonMethods.convertDoubleToCoordinates(entity.getLattitude(), entity.getLongitude()));
		groupSign.setVisible(entity.getEntityId().isGroup());

	}

	final String getName() {

		return nameLabel.getText();

	}

	final StringProperty nameProperty() {

		return nameLabel.textProperty();

	}

	final BooleanProperty activeProperty() {

		return activeProperty;

	}

	protected void addRightNode(Node node) {

		add(node, 3, 0, 1, 2);

	}

	protected void addBottomNode(Node node) {

		add(node, 2, 3, 1, 1);

	}

}

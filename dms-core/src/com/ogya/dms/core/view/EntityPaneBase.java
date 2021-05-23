package com.ogya.dms.core.view;

import java.util.Objects;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.structures.Availability;
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

	private static final double UNIT_SIZE = 24.0 * ViewFactory.VIEW_FACTOR;

	private final Group profilePicture = new Group();
	private final Circle statusCircle = new Circle(UNIT_SIZE);
	private final Circle profileRound = new Circle(UNIT_SIZE * 0.8);
	private final Label initialLbl = new Label();
	private final Label groupSign = new Label("G");

	private final Label nameLbl = new Label();
	private final Label commentLbl = new Label();
	private final Label coordinatesLbl = new Label();

	private final BooleanProperty onlineProperty = new SimpleBooleanProperty(false);
	private final BooleanProperty activeProperty = new SimpleBooleanProperty(true);

	EntityPaneBase() {

		super();

		init();

	}

	private final void init() {

		setHgap(ViewFactory.GAP);

		initProfilePicture();
		initNameLbl();
		initCommentLbl();
		initCoordinatesLbl();

		add(profilePicture, 0, 0, 1, 3);
		add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		add(nameLbl, 2, 0, 1, 1);
		add(commentLbl, 2, 1, 1, 1);
		add(coordinatesLbl, 2, 2, 1, 1);

		Region unclickableRegion = new Region();
		unclickableRegion.addEventFilter(MouseEvent.ANY, e -> e.consume());
		add(unclickableRegion, 0, 3, 4, 1);

	}

	private void initProfilePicture() {

		GridPane.setValignment(profilePicture, VPos.TOP);

		initStatusCircle();
		initProfileRound();
		initInitialLbl();
		initGroupSign();

		profilePicture.getChildren().addAll(statusCircle, profileRound, initialLbl, groupSign);

	}

	private void initStatusCircle() {

		statusCircle.setStrokeWidth(UNIT_SIZE * 0.2);
		statusCircle.setFill(Color.TRANSPARENT);

	}

	private void initProfileRound() {

		profileRound.setFill(Color.DARKGRAY);

	}

	private void initInitialLbl() {

		initialLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

		initialLbl.setStyle("-fx-text-fill: #404040;");
		initialLbl.setFont(Font.font(null, FontWeight.BOLD, UNIT_SIZE));

		initialLbl.translateXProperty().bind(
				Bindings.createDoubleBinding(() -> -initialLbl.widthProperty().get() / 2, initialLbl.widthProperty()));
		initialLbl.translateYProperty().bind(Bindings.createDoubleBinding(() -> -initialLbl.heightProperty().get() / 2,
				initialLbl.heightProperty()));

	}

	private void initGroupSign() {

		groupSign.setVisible(false);
		groupSign.setTextFill(Color.WHITE);
		groupSign.setContentDisplay(ContentDisplay.CENTER);
		groupSign.setFont(Font.font(null, FontWeight.EXTRA_BOLD, UNIT_SIZE * 0.5));

		Circle circle = new Circle(UNIT_SIZE * 0.3);
		circle.setFill(Color.TOMATO);

		groupSign.setGraphic(circle);

		groupSign.setTranslateX(UNIT_SIZE * 0.5);
		groupSign.setTranslateY(UNIT_SIZE * 0.5);

	}

	private void initNameLbl() {

		nameLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLbl.setFont(Font.font(null, FontWeight.BOLD, UNIT_SIZE * 0.8));

		nameLbl.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
			String name = nameLbl.getText();
			if (name == null || name.isEmpty())
				return null;
			return new Tooltip(name);
		}, nameLbl.textProperty()));

	}

	private void initCommentLbl() {

		commentLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

		commentLbl.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
			String comment = commentLbl.getText();
			if (comment == null || comment.isEmpty())
				return null;
			return new Tooltip(comment);
		}, commentLbl.textProperty()));

	}

	private void initCoordinatesLbl() {

		GridPane.setHgrow(coordinatesLbl, Priority.ALWAYS);

		coordinatesLbl.setOpacity(0.5);
		coordinatesLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	void updateEntity(EntityBase entity) {

		getChildren().remove(commentLbl);
		getChildren().remove(coordinatesLbl);

		onlineProperty.set(!Objects.equals(entity.getStatus(), Availability.OFFLINE));

		String name = entity.getName();

		statusCircle.setStroke(entity.getStatus().getStatusColor());
		if (!(name == null || name.isEmpty()))
			initialLbl.setText(name.substring(0, 1).toUpperCase());
		nameLbl.setText(name);
		commentLbl.setText(entity.getComment());
		coordinatesLbl.setText(entity.getLattitude() == null || entity.getLongitude() == null ? ""
				: CommonMethods.convertDoubleToCoordinates(entity.getLattitude(), entity.getLongitude()));
		groupSign.setVisible(entity.getEntityId().isGroup());

		if (commentLbl.textProperty().getValueSafe().isEmpty()) {
			add(coordinatesLbl, 2, 1, 1, 1);
			add(commentLbl, 2, 2, 1, 1);
		} else {
			add(commentLbl, 2, 1, 1, 1);
			add(coordinatesLbl, 2, 2, 1, 1);
		}

	}

	final String getName() {

		return nameLbl.getText();

	}

	final StringProperty nameProperty() {

		return nameLbl.textProperty();

	}

	final BooleanProperty onlineProperty() {

		return onlineProperty;

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

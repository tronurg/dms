package com.ogya.dms.core.view;

import java.util.Locale;

import com.ogya.dms.core.database.tables.EntityBase;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

class EntityPaneBase extends GridPane {

	private static final double RADIUS = 24.0;

	private final Circle statusCircle = new Circle(RADIUS);
	private final Circle profileRound = new Circle(0.8 * RADIUS);
	private final Label initialLbl = new Label();
	private final StackPane groupSign = new StackPane();
	private final StackPane profilePicture = new StackPane(
			new Group(statusCircle, profileRound, initialLbl, groupSign));

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

		getStyleClass().addAll("hgap-1");

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
		GridPane.setFillHeight(profilePicture, false);

		initStatusCircle();
		initProfileRound();
		initInitialLbl();
		initGroupSign();

	}

	private void initStatusCircle() {

		statusCircle.setStyle(ViewFactory.getScaleCss(1d, 1d));
		statusCircle.setStrokeWidth(0.2 * RADIUS);
		statusCircle.setFill(Color.TRANSPARENT);

	}

	private void initProfileRound() {

		profileRound.setStyle(ViewFactory.getScaleCss(1d, 1d));
		profileRound.setFill(Color.DARKGRAY);

	}

	private void initInitialLbl() {

		initialLbl.getStyleClass().addAll("gray40-text", "em16", "bold");
		initialLbl.translateXProperty().bind(initialLbl.widthProperty().multiply(-0.5));
		initialLbl.translateYProperty().bind(initialLbl.heightProperty().multiply(-0.5));
		initialLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private void initGroupSign() {

		groupSign.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		groupSign.translateXProperty().bind(groupSign.widthProperty().multiply(0.5));
		groupSign.translateYProperty().bind(groupSign.heightProperty().multiply(0.5));

		Circle groupCircle = new Circle(0.3 * RADIUS);
		groupCircle.setStyle(ViewFactory.getScaleCss(1d, 1d));
		groupCircle.setFill(Color.TOMATO);

		Label groupLbl = new Label("G");
		groupLbl.getStyleClass().addAll("em08", "extra-bold");
		groupLbl.setTextFill(Color.WHITE);

		groupSign.getChildren().addAll(groupCircle, groupLbl);

	}

	private void initNameLbl() {

		nameLbl.getStyleClass().addAll("em13", "bold");
		nameLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLbl.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
			String name = nameLbl.getText();
			if (name == null || name.isEmpty()) {
				return null;
			}
			return new Tooltip(name);
		}, nameLbl.textProperty()));

	}

	private void initCommentLbl() {

		commentLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

		commentLbl.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
			String comment = commentLbl.getText();
			if (comment == null || comment.isEmpty()) {
				return null;
			}
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

		onlineProperty.set(entity.getStatus() != Availability.OFFLINE);

		String name = entity.getName();

		statusCircle.setStroke(entity.getStatus().getStatusColor());
		if (!(name == null || name.isEmpty())) {
			initialLbl.setText(name.substring(0, 1).toUpperCase(Locale.getDefault()));
		}
		nameLbl.setText(name);
		commentLbl.setText(entity.getComment());
		coordinatesLbl.setText(entity.getLatitude() == null || entity.getLongitude() == null ? ""
				: Commons.convertDoubleToCoordinates(entity.getLatitude(), entity.getLongitude()));
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

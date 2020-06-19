package com.ogya.dms.view.factory;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;

public class ViewFactory {

	public static Button newBackBtn() {

		Button btn = new Button();

		Circle circle = new Circle(16.0);
		circle.setFill(Color.DODGERBLUE);
		Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -13.0, 0.0, 7.0, 10.5, 7.0, -10.5 });
		triangle.setFill(Color.ANTIQUEWHITE);
		Group group = new Group(circle, triangle);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newSendBtn() {

		Button btn = new Button();

		Circle circle = new Circle(16.0);
		circle.setFill(Color.GREEN);
		Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -7.0, -10.5, 13.0, 0.0, -7.0, 10.5 });
		triangle.setFill(Color.ANTIQUEWHITE);
		Group group = new Group(circle, triangle);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newAddBtn() {

		Button btn = new Button();

		Circle circle = new Circle(10.0);
		circle.setStrokeWidth(3.0);
		circle.setFill(Color.TRANSPARENT);
		Line line1 = new Line(0.0, 5.0, 0.0, -5.0);
		line1.setStrokeLineCap(StrokeLineCap.ROUND);
		line1.setStrokeWidth(3.0);
		Line line2 = new Line(-5.0, 0.0, 5.0, 0.0);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(3.0);
		Group group = new Group(circle, line1, line2);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		circle.strokeProperty().bind(
				Bindings.createObjectBinding(() -> btn.isHover() ? Color.GREEN : Color.GRAY, btn.hoverProperty()));
		line1.strokeProperty().bind(
				Bindings.createObjectBinding(() -> btn.isHover() ? Color.GREEN : Color.GRAY, btn.hoverProperty()));
		line2.strokeProperty().bind(
				Bindings.createObjectBinding(() -> btn.isHover() ? Color.GREEN : Color.GRAY, btn.hoverProperty()));

		return btn;

	}

	public static Button newRemoveBtn() {

		Button btn = new Button();

		Circle circle = new Circle(10.0);
		circle.setStrokeWidth(3.0);
		circle.setFill(Color.TRANSPARENT);
		Line line = new Line(-5.0, 0.0, 5.0, 0.0);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		line.setStrokeWidth(3.0);
		Group group = new Group(circle, line);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		circle.strokeProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.RED : Color.GRAY, btn.hoverProperty()));
		line.strokeProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.RED : Color.GRAY, btn.hoverProperty()));

		return btn;

	}

}

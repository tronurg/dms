package com.aselsan.rehis.reform.mcsy.sunum.fabrika;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

public class SunumFabrika {

	public static Button newGeriBtn() {

		Button btn = new Button();

		Circle circle = new Circle(16.0);
		circle.setFill(Color.DODGERBLUE);
		Polygon ucgen = new Polygon();
		ucgen.getPoints().addAll(new Double[] { -13.0, 0.0, 7.0, 10.5, 7.0, -10.5 });
		ucgen.setFill(Color.ANTIQUEWHITE);
		Group group = new Group(circle, ucgen);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newGonderBtn() {

		Button btn = new Button();

		Circle circle = new Circle(16.0);
		circle.setFill(Color.GREEN);
		Polygon ucgen = new Polygon();
		ucgen.getPoints().addAll(new Double[] { -7.0, -10.5, 13.0, 0.0, -7.0, 10.5 });
		ucgen.setFill(Color.ANTIQUEWHITE);
		Group group = new Group(circle, ucgen);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

}

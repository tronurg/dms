package com.ogya.dms.view.factory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

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

	public static Button newDeleteBtn() {

		Button btn = new Button();

		Rectangle rectangle = new Rectangle(12.0, 20.0);
		rectangle.setArcWidth(4.0);
		rectangle.setArcHeight(4.0);
		rectangle.setStrokeWidth(2.0);
		rectangle.setStroke(Color.RED);
		rectangle.setFill(Color.TRANSPARENT);
		Line line1 = new Line(0.0, 4.0, 12.0, 4.0);
		line1.setStrokeWidth(2.0);
		line1.setStroke(Color.RED);
		Line line2 = new Line(4.0, 7.0, 4.0, 17.0);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(2.0);
		line2.setStroke(Color.RED);
		Line line3 = new Line(8.0, 7.0, 8.0, 17.0);
		line3.setStrokeLineCap(StrokeLineCap.ROUND);
		line3.setStrokeWidth(2.0);
		line3.setStroke(Color.RED);
		Group group = new Group(rectangle, line1, line2, line3);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newAttachBtn() {

		Button btn = new Button();

		Circle circle = new Circle(16.0);
		circle.setFill(Color.GREEN);
		Rectangle rectangle = new Rectangle(-12.0, -5.0, 24.0, 10.0);
		rectangle.setArcWidth(6.0);
		rectangle.setArcHeight(6.0);
		rectangle.setStrokeWidth(2.0);
		rectangle.setStroke(Color.ANTIQUEWHITE);
		rectangle.setRotate(-45.0);
		rectangle.setFill(Color.TRANSPARENT);
		Line line = new Line(-6.0, 6.0, 3.0, -3.0);
		line.setStroke(Color.ANTIQUEWHITE);
		line.setStrokeWidth(2.0);
		Group group = new Group(circle, rectangle, line);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newInfoBtn() {

		Button btn = new Button("i");
		btn.setTextFill(Color.WHITE);
		btn.setContentDisplay(ContentDisplay.CENTER);
		Font defaultFont = btn.getFont();
		btn.setFont(Font.font(defaultFont.getFamily(), FontWeight.EXTRA_BOLD, FontPosture.ITALIC, 20.0));

		Circle circle = new Circle(12.0);
		btn.setGraphic(circle);
		circle.fillProperty().bind(Bindings
				.createObjectBinding(() -> btn.isHover() ? Color.LIGHTSKYBLUE : Color.LIGHTGRAY, btn.hoverProperty()));
		btn.setGraphic(circle);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newCancelBtn() {

		Button btn = new Button();

		Circle circle = new Circle(10.0);
		circle.setFill(Color.RED);
		Line line1 = new Line(-4.0, -4.0, 4.0, 4.0);
		line1.setStrokeWidth(3.0);
		line1.setStroke(Color.WHITE);
		Line line2 = new Line(-4.0, 4.0, 4.0, -4.0);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(3.0);
		line2.setStroke(Color.WHITE);
		Group group = new Group(circle, line1, line2);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		btn.opacityProperty().bind(Bindings.createDoubleBinding(() -> btn.isHover() ? 1.0 : 0.5, btn.hoverProperty()));

		return btn;

	}

	public static Button newSettingsBtn() {

		Button btn = new Button();

		ObjectProperty<Color> colorProperty = new SimpleObjectProperty<Color>();

		Circle circle = new Circle(12.0, Color.TRANSPARENT);
		circle.strokeProperty().bind(colorProperty);
		circle.setStrokeWidth(2.0);
		Circle point0 = new Circle(-6.0, 0.0, 2.0);
		point0.fillProperty().bind(colorProperty);
		Circle point1 = new Circle(2.0);
		point1.fillProperty().bind(colorProperty);
		Circle point2 = new Circle(6.0, 0.0, 2.0);
		point2.fillProperty().bind(colorProperty);
		Group group = new Group(circle, point0, point1, point2);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		final Effect dropShadow = new DropShadow(2.0, Color.DODGERBLUE);
		colorProperty.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.DODGERBLUE : Color.DARKGRAY,
				btn.hoverProperty()));
		btn.effectProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? dropShadow : null, btn.hoverProperty()));

		return btn;

	}

	public static Label newSelectionLbl() {

		Label lbl = new Label();

		Circle circle = new Circle(12.0);
		circle.setStrokeWidth(3.0);
		circle.setFill(Color.GREEN);
		Line line1 = new Line(-6.0, 0.0, -2.0, 6.0);
		line1.setStrokeLineCap(StrokeLineCap.ROUND);
		line1.setStrokeWidth(3.0);
		line1.setStroke(Color.WHITE);
		Line line2 = new Line(-2.0, 6.0, 6.0, -6.0);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(3.0);
		line2.setStroke(Color.WHITE);
		Group group = new Group(circle, line1, line2);
		lbl.setGraphic(group);
		lbl.setPadding(Insets.EMPTY);
		lbl.setPickOnBounds(false);

		return lbl;

	}

}

package com.ogya.dms.core.view.factory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

	private static double gap = 0.0;

	private static double viewFactor = 0.0;

	private static final Map<String, Color> colorMap = Collections.synchronizedMap(new HashMap<String, Color>());

	public static double getGap() {

		if (gap == 0.0) {

			gap = Font.getDefault().getSize() / 3.0;

		}

		return gap;

	}

	public static double getViewFactor() {

		if (viewFactor == 0.0) {

			viewFactor = Font.getDefault().getSize() / 15.0;

		}

		return viewFactor;

	}

	public static Color getColorForUuid(String uuid) {

		String hex = uuid.substring(0, 6);

		if (!colorMap.containsKey(hex)) {

			try {

				int red = Integer.valueOf(hex.substring(0, 2), 16);
				int green = Integer.valueOf(hex.substring(2, 4), 16);
				int blue = Integer.valueOf(hex.substring(4, 6), 16);

				int minRange = Math.min(Math.min(red, green), blue);
				int maxRange = Math.max(Math.max(red, green), blue);

				if (red == minRange)
					red = 0;
				else if (green == minRange)
					green = 0;
				else
					blue = 0;

				if (red == maxRange)
					red = 255;
				else if (green == maxRange)
					green = 255;
				else
					blue = 255;

				Color color = Color.rgb(red, green, blue);
				color = Color.hsb(color.getHue(), color.getSaturation(), 0.8);

				colorMap.put(hex, color);

			} catch (Exception e) {

			}

		}

		return colorMap.get(hex);

	}

	public static Button newBackBtn() {

		double viewFactor = getViewFactor();

		Button btn = new Button();

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(Color.DODGERBLUE);
		Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -13.0 * viewFactor, 0.0, 7.0 * viewFactor, 10.5 * viewFactor,
				7.0 * viewFactor, -10.5 * viewFactor });
		triangle.setFill(Color.ANTIQUEWHITE);
		Group group = new Group(circle, triangle);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newSendBtn() {

		double viewFactor = getViewFactor();

		Button btn = new Button();

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(Color.GREEN);
		Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -7.0 * viewFactor, -10.5 * viewFactor, 13.0 * viewFactor, 0.0,
				-7.0 * viewFactor, 10.5 * viewFactor });
		triangle.setFill(Color.ANTIQUEWHITE);
		Group group = new Group(circle, triangle);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newAddBtn() {

		double viewFactor = getViewFactor();

		Button btn = new Button();

		Circle circle = new Circle(10.0 * viewFactor);
		circle.setStrokeWidth(3.0 * viewFactor);
		circle.setFill(Color.TRANSPARENT);
		Line line1 = new Line(0.0, 5.0 * viewFactor, 0.0, -5.0 * viewFactor);
		line1.setStrokeLineCap(StrokeLineCap.ROUND);
		line1.setStrokeWidth(3.0);
		Line line2 = new Line(-5.0 * viewFactor, 0.0, 5.0 * viewFactor, 0.0);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(3.0 * viewFactor);
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

		double viewFactor = getViewFactor();

		Button btn = new Button();

		Circle circle = new Circle(10.0 * viewFactor);
		circle.setStrokeWidth(3.0 * viewFactor);
		circle.setFill(Color.TRANSPARENT);
		Line line = new Line(-5.0 * viewFactor, 0.0, 5.0 * viewFactor, 0.0);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		line.setStrokeWidth(3.0 * viewFactor);
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

		double viewFactor = getViewFactor();

		Button btn = new Button();

		Rectangle rectangle = new Rectangle(12.0 * viewFactor, 20.0 * viewFactor);
		rectangle.setArcWidth(4.0 * viewFactor);
		rectangle.setArcHeight(4.0 * viewFactor);
		rectangle.setStrokeWidth(2.0 * viewFactor);
		rectangle.setStroke(Color.RED);
		rectangle.setFill(Color.TRANSPARENT);
		Line line1 = new Line(0.0, 4.0 * viewFactor, 12.0 * viewFactor, 4.0 * viewFactor);
		line1.setStrokeWidth(2.0 * viewFactor);
		line1.setStroke(Color.RED);
		Line line2 = new Line(4.0 * viewFactor, 7.0 * viewFactor, 4.0 * viewFactor, 17.0 * viewFactor);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(2.0 * viewFactor);
		line2.setStroke(Color.RED);
		Line line3 = new Line(8.0 * viewFactor, 7.0 * viewFactor, 8.0 * viewFactor, 17.0 * viewFactor);
		line3.setStrokeLineCap(StrokeLineCap.ROUND);
		line3.setStrokeWidth(2.0 * viewFactor);
		line3.setStroke(Color.RED);
		Group group = new Group(rectangle, line1, line2, line3);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newAttachBtn() {

		double viewFactor = getViewFactor();

		Button btn = new Button();

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(Color.GREEN);
		Rectangle rectangle = new Rectangle(-12.0 * viewFactor, -5.0 * viewFactor, 24.0 * viewFactor,
				10.0 * viewFactor);
		rectangle.setArcWidth(6.0 * viewFactor);
		rectangle.setArcHeight(6.0 * viewFactor);
		rectangle.setStrokeWidth(2.0 * viewFactor);
		rectangle.setStroke(Color.ANTIQUEWHITE);
		rectangle.setRotate(-45.0);
		rectangle.setFill(Color.TRANSPARENT);
		Line line = new Line(-6.0 * viewFactor, 6.0 * viewFactor, 3.0 * viewFactor, -3.0 * viewFactor);
		line.setStroke(Color.ANTIQUEWHITE);
		line.setStrokeWidth(2.0 * viewFactor);
		Group group = new Group(circle, rectangle, line);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newReportBtn() {

		double viewFactor = getViewFactor();

		Button btn = new Button();

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(Color.GREEN);
		Rectangle rectangle = new Rectangle(-6.0 * viewFactor, -8.0 * viewFactor, 12.0 * viewFactor, 16.0 * viewFactor);
		rectangle.setStrokeWidth(2.0 * viewFactor);
		rectangle.setStroke(Color.ANTIQUEWHITE);
		rectangle.setFill(Color.TRANSPARENT);
		Line line1 = new Line(-2.0 * viewFactor, -4.0 * viewFactor, 2.0 * viewFactor, -4.0 * viewFactor);
		line1.setStroke(Color.ANTIQUEWHITE);
		line1.setStrokeWidth(2.0 * viewFactor);
		Line line2 = new Line(-2.0 * viewFactor, 0.0, 2.0 * viewFactor, 0.0);
		line2.setStroke(Color.ANTIQUEWHITE);
		line2.setStrokeWidth(2.0 * viewFactor);
		Line line3 = new Line(-2.0 * viewFactor, 4.0 * viewFactor, 2.0 * viewFactor, 4.0 * viewFactor);
		line3.setStroke(Color.ANTIQUEWHITE);
		line3.setStrokeWidth(2.0 * viewFactor);
		Group group = new Group(circle, rectangle, line1, line2, line3);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newInfoBtn() {

		double viewFactor = getViewFactor();

		Button btn = new Button("i");
		btn.setTextFill(Color.WHITE);
		btn.setContentDisplay(ContentDisplay.CENTER);
		btn.setFont(Font.font(null, FontWeight.EXTRA_BOLD, FontPosture.ITALIC, 20.0 * viewFactor));

		Circle circle = new Circle(12.0 * viewFactor);
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

		double viewFactor = getViewFactor();

		Button btn = new Button();

		Circle circle = new Circle(10.0 * viewFactor);
		circle.setFill(Color.RED);
		Line line1 = new Line(-4.0 * viewFactor, -4.0 * viewFactor, 4.0 * viewFactor, 4.0 * viewFactor);
		line1.setStrokeWidth(3.0 * viewFactor);
		line1.setStroke(Color.WHITE);
		Line line2 = new Line(-4.0 * viewFactor, 4.0 * viewFactor, 4.0 * viewFactor, -4.0 * viewFactor);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(3.0 * viewFactor);
		line2.setStroke(Color.WHITE);
		Group group = new Group(circle, line1, line2);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newSettingsBtn() {

		double viewFactor = getViewFactor();

		Button btn = new Button();

		ObjectProperty<Color> colorProperty = new SimpleObjectProperty<Color>();

		Circle circle = new Circle(12.0 * viewFactor, Color.TRANSPARENT);
		circle.strokeProperty().bind(colorProperty);
		circle.setStrokeWidth(2.0 * viewFactor);
		Circle point0 = new Circle(-6.0 * viewFactor, 0.0, 2.0 * viewFactor);
		point0.fillProperty().bind(colorProperty);
		Circle point1 = new Circle(2.0 * viewFactor);
		point1.fillProperty().bind(colorProperty);
		Circle point2 = new Circle(6.0 * viewFactor, 0.0, 2.0 * viewFactor);
		point2.fillProperty().bind(colorProperty);
		Group group = new Group(circle, point0, point1, point2);
		btn.setGraphic(group);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		final Effect dropShadow = new DropShadow(2.0 * viewFactor, Color.DODGERBLUE);
		colorProperty.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.DODGERBLUE : Color.DARKGRAY,
				btn.hoverProperty()));
		btn.effectProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? dropShadow : null, btn.hoverProperty()));

		return btn;

	}

	public static Label newSelectionLbl() {

		double viewFactor = getViewFactor();

		Label lbl = new Label();

		Circle circle = new Circle(12.0 * viewFactor);
		circle.setStrokeWidth(3.0 * viewFactor);
		circle.setFill(Color.GREEN);
		Line line1 = new Line(-6.0 * viewFactor, 0.0, -2.0 * viewFactor, 6.0 * viewFactor);
		line1.setStrokeLineCap(StrokeLineCap.ROUND);
		line1.setStrokeWidth(3.0 * viewFactor);
		line1.setStroke(Color.WHITE);
		Line line2 = new Line(-2.0 * viewFactor, 6.0 * viewFactor, 6.0 * viewFactor, -6.0 * viewFactor);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(3.0 * viewFactor);
		line2.setStroke(Color.WHITE);
		Group group = new Group(circle, line1, line2);
		lbl.setGraphic(group);
		lbl.setPadding(Insets.EMPTY);
		lbl.setPickOnBounds(false);

		return lbl;

	}

}

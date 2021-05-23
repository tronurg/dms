package com.ogya.dms.core.view.factory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class ViewFactory {

	private static final double EM = Font.getDefault().getSize();
	public static final double GAP = EM / 3.0;
	public static final double VIEW_FACTOR = EM / 15.0;

	private static final Map<String, Color> colorMap = Collections.synchronizedMap(new HashMap<String, Color>());

	public static Color getColorForUuid(String uuid) {

		String hex = uuid.substring(0, 6);

		Color color = colorMap.get(hex);

		if (color == null) {

			try {

				int red = Integer.valueOf(hex.substring(0, 2), 16);
				int green = Integer.valueOf(hex.substring(2, 4), 16);
				int blue = Integer.valueOf(hex.substring(4, 6), 16);

				color = Color.rgb(red, green, blue);

				double hue = color.getHue();
				double brightness = 0.0 < hue && hue < 200.0 ? 0.5 : 1.0;

				color = Color.hsb(hue, 1.0, brightness);

				colorMap.put(hex, color);

			} catch (Exception e) {

			}

		}

		return color;

	}

	public static Button newBackBtn(BooleanProperty highlightProperty) {

		double viewFactor = VIEW_FACTOR;

		Button btn = new Button();

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(Color.DODGERBLUE);
		Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -13.0 * viewFactor, 0.0, 7.0 * viewFactor, 10.5 * viewFactor,
				7.0 * viewFactor, -10.5 * viewFactor });
		triangle.setFill(Color.ANTIQUEWHITE);
		Group group = new Group(circle, triangle);
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		if (highlightProperty != null) {
			final Effect highlight = new ColorAdjust(0.8, 0.0, 0.0, 0.0);
			btn.effectProperty().bind(
					Bindings.createObjectBinding(() -> highlightProperty.get() ? highlight : null, highlightProperty));
		}

		return btn;

	}

	public static Button newSendBtn() {

		double viewFactor = VIEW_FACTOR;

		Button btn = new Button();

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(Color.GREEN);
		Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -7.0 * viewFactor, -10.5 * viewFactor, 13.0 * viewFactor, 0.0,
				-7.0 * viewFactor, 10.5 * viewFactor });
		triangle.setFill(Color.ANTIQUEWHITE);
		Group group = new Group(circle, triangle);
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newAddBtn() {

		double viewFactor = VIEW_FACTOR;

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

	public static Button newRemoveBtn(double scaleFactor) {

		double viewFactor = scaleFactor * VIEW_FACTOR;

		Button btn = new Button();

		Circle circle = new Circle(10.0 * viewFactor);
		circle.setStrokeWidth(3.0 * viewFactor);
		circle.setFill(Color.TRANSPARENT);
		Line line = new Line(-5.0 * viewFactor, 0.0, 5.0 * viewFactor, 0.0);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		line.setStrokeWidth(3.0 * viewFactor);
		Group group = new Group(circle, line);
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		circle.strokeProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.RED : Color.GRAY, btn.hoverProperty()));
		line.strokeProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.RED : Color.GRAY, btn.hoverProperty()));

		return btn;

	}

	public static Button newDeleteBtn() {

		double viewFactor = VIEW_FACTOR;

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
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newAttachBtnTransparent() {

		return newAttachBtn(0.85, Color.TRANSPARENT, Color.GRAY);

	}

	public static Button newAttachBtnGreen() {

		return newAttachBtn(1.0, Color.GREEN, Color.ANTIQUEWHITE);

	}

	private static Button newAttachBtn(double scaleFactor, Color background, Color foreground) {

		double viewFactor = VIEW_FACTOR;

		Button btn = new Button();

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(background);
		Group group = new Group(circle, newAttachGraph(scaleFactor, foreground));
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Node newAttachGraph(double scaleFactor) {

		return newAttachGraph(scaleFactor, Color.GRAY);

	}

	private static Node newAttachGraph(double scaleFactor, Color color) {

		double viewFactor = scaleFactor * VIEW_FACTOR;

		Rectangle rectangle = new Rectangle(-12.0 * viewFactor, -5.0 * viewFactor, 24.0 * viewFactor,
				10.0 * viewFactor);
		rectangle.setArcWidth(6.0 * viewFactor);
		rectangle.setArcHeight(6.0 * viewFactor);
		rectangle.setStrokeWidth(2.0 * viewFactor);
		rectangle.setStroke(color);
		rectangle.setRotate(-45.0);
		rectangle.setFill(Color.TRANSPARENT);
		Line line = new Line(-6.0 * viewFactor, 6.0 * viewFactor, 3.0 * viewFactor, -3.0 * viewFactor);
		line.setStrokeWidth(2.0 * viewFactor);
		line.setStroke(color);

		return new Group(rectangle, line);

	}

	public static Button newReportBtn() {

		double viewFactor = VIEW_FACTOR;

		Button btn = new Button();

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(Color.TRANSPARENT);
		Rectangle rectangle = new Rectangle(-6.0 * viewFactor, -8.0 * viewFactor, 12.0 * viewFactor, 16.0 * viewFactor);
		rectangle.setStrokeWidth(2.0 * viewFactor);
		rectangle.setStroke(Color.GRAY);
		rectangle.setFill(Color.TRANSPARENT);
		Line line1 = new Line(-2.0 * viewFactor, -4.0 * viewFactor, 2.0 * viewFactor, -4.0 * viewFactor);
		line1.setStrokeWidth(2.0 * viewFactor);
		line1.setStroke(Color.GRAY);
		Line line2 = new Line(-2.0 * viewFactor, 0.0, 2.0 * viewFactor, 0.0);
		line2.setStrokeWidth(2.0 * viewFactor);
		line2.setStroke(Color.GRAY);
		Line line3 = new Line(-2.0 * viewFactor, 4.0 * viewFactor, 2.0 * viewFactor, 4.0 * viewFactor);
		line3.setStrokeWidth(2.0 * viewFactor);
		line3.setStroke(Color.GRAY);
		Group group = new Group(circle, rectangle, line1, line2, line3);
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newInfoBtn() {

		double viewFactor = VIEW_FACTOR;

		Button btn = new Button("i");
		btn.setTextFill(Color.WHITE);
		btn.setContentDisplay(ContentDisplay.CENTER);
		btn.setFont(Font.font(null, FontWeight.EXTRA_BOLD, FontPosture.ITALIC, 20.0 * viewFactor));

		Circle circle = new Circle(12.0 * viewFactor);
		btn.setGraphic(circle);
		circle.fillProperty()
				.bind(Bindings.createObjectBinding(
						() -> btn.isHover() && !btn.isDisabled() ? Color.LIGHTSKYBLUE : Color.LIGHTGRAY,
						btn.hoverProperty(), btn.disabledProperty()));
		btn.setGraphic(circle);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newCancelBtn() {

		double viewFactor = VIEW_FACTOR;

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
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newSettingsBtn() {

		double viewFactor = VIEW_FACTOR;

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
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		final Effect dropShadow = new DropShadow(GAP, Color.DODGERBLUE);
		colorProperty.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.DODGERBLUE : Color.DARKGRAY,
				btn.hoverProperty()));
		btn.effectProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? dropShadow : null, btn.hoverProperty()));

		return btn;

	}

	public static Button newSelectionBtn() {

		double viewFactor = VIEW_FACTOR;

		Button btn = new Button();

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
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newStarBtn(double scaleFactor) {

		Button btn = new Button();

		btn.setGraphic(newStarGraph(scaleFactor));
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Node newStarGraph(double scaleFactor) {

		double viewFactor = scaleFactor * VIEW_FACTOR;

		Polygon star = new Polygon();
		star.setFill(Color.YELLOW);
		Double[] points = new Double[10];
		for (int i = 0; i < 5; ++i) {
			points[2 * i] = 12.0 * viewFactor * Math.cos(2 * Math.PI * (0.25 + 2.0 * i / 5));
			points[2 * i + 1] = -12.0 * viewFactor * Math.sin(2 * Math.PI * (0.25 + 2.0 * i / 5));
		}
		star.getPoints().addAll(points);

		return star;

	}

	public static Button newForwardBtn() {

		double viewFactor = 0.75 * VIEW_FACTOR;

		Button btn = new Button();

		Circle circle = new Circle(-3.75 * viewFactor, 1.25 * viewFactor, 16.0 * viewFactor);
		circle.setFill(Color.DODGERBLUE);
		Group group = new Group(circle, newForwardGraph(0.75, Color.ANTIQUEWHITE));
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newGoToRefBtn() {

		double viewFactor = 0.75 * VIEW_FACTOR;

		Button btn = new Button();

		Circle circle = new Circle(-3.75 * viewFactor, 1.25 * viewFactor, 16.0 * viewFactor);
		circle.fillProperty()
				.bind(Bindings.createObjectBinding(
						() -> btn.isHover() && !btn.isDisabled() ? Color.LIGHTSKYBLUE : Color.LIGHTGRAY,
						btn.hoverProperty(), btn.disabledProperty()));
		Group group = new Group(circle, newForwardGraph(0.75, Color.ANTIQUEWHITE));
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Node newForwardGraph(double scaleFactor, Color color) {

		double viewFactor = scaleFactor * VIEW_FACTOR;

		Polygon arrowHead = new Polygon();
		arrowHead.getPoints()
				.addAll(new Double[] { 0.0, -10.0 * viewFactor, 10.0 * viewFactor, 0.0, 0.0, 10.0 * viewFactor });
		arrowHead.setFill(color);
		Arc arc1 = new Arc(0.0, 10.0 * viewFactor, 15.0 * viewFactor, 14.0 * viewFactor, 90.0, 90.0);
		arc1.setType(ArcType.ROUND);
		Arc arc2 = new Arc(0.0, 10.0 * viewFactor, 15.0 * viewFactor, 6.0 * viewFactor, 90.0, 90.0);
		arc2.setType(ArcType.ROUND);
		Shape arrowTail = Shape.subtract(arc1, arc2);
		arrowTail.setFill(color);

		return new Group(arrowHead, arrowTail);

	}

	public static Button newVisibleBtn(double scaleFactor) {

		double viewFactor = scaleFactor * VIEW_FACTOR;

		Button btn = new Button();

		btn.setGraphic(newEyeGraph(viewFactor, Color.DEEPSKYBLUE));
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	public static Button newInvisibleBtn(double scaleFactor) {

		double viewFactor = scaleFactor * VIEW_FACTOR;

		Button btn = new Button();

		btn.setGraphic(newNoEyeGraph(viewFactor, Color.RED));
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		return btn;

	}

	private static Node newEyeGraph(double viewFactor, Color color) {

		Shape outerCircle = Shape.intersect(new Circle(0.0, -6.0 * viewFactor, 16.0 * viewFactor),
				new Circle(0.0, 6.0 * viewFactor, 16.0 * viewFactor));
		outerCircle.setStrokeLineJoin(StrokeLineJoin.ROUND);
		outerCircle.setStrokeWidth(3.0 * viewFactor);
		outerCircle.setFill(Color.TRANSPARENT);
		outerCircle.setStroke(color);
		Circle innerCircle = new Circle(0.0, 0.0, 6.0 * viewFactor);
		innerCircle.setStrokeWidth(3.0 * viewFactor);
		innerCircle.setFill(Color.TRANSPARENT);
		innerCircle.setStroke(color);

		return new Group(outerCircle, innerCircle);

	}

	private static Node newNoEyeGraph(double viewFactor, Color color) {

		Line line = new Line(-8.0 * viewFactor, 12.0 * viewFactor, 8.0 * viewFactor, -12.0 * viewFactor);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		line.setStrokeWidth(3.0 * viewFactor);
		line.setStroke(color);

		return new Group(newEyeGraph(viewFactor, color), line);

	}

}

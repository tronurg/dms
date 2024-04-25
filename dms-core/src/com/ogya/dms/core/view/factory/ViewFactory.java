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
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

	private static Button newButton(Node... nodes) {
		Button btn = new Button();
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		btn.setGraphic(newGroup(nodes));
		return btn;
	}

	private static Label newLabel(Node... nodes) {
		Label lbl = new Label();
		lbl.setPadding(Insets.EMPTY);
		lbl.setPickOnBounds(false);
		lbl.setGraphic(newGroup(nodes));
		return lbl;
	}

	private static Group newGroup(Node... nodes) {
		Group group = new Group(nodes);
		group.setScaleX(VIEW_FACTOR);
		group.setScaleY(VIEW_FACTOR);
		Group groupContainer = new Group(group);
		return groupContainer;
	}

	public static Button newBackBtn(BooleanProperty highlightProperty, Parent parent) {

		Circle circle = new Circle(16.0);
		circle.setFill(Color.DODGERBLUE);
		Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -13.0, 0.0, 7.0, 10.5, 7.0, -10.5 });
		triangle.setFill(Color.ANTIQUEWHITE);
		Button btn = newButton(circle, triangle);

		if (highlightProperty != null) {
			final Effect highlight = new ColorAdjust(0.8, 0.0, 0.0, 0.0);
			btn.effectProperty().bind(
					Bindings.createObjectBinding(() -> highlightProperty.get() ? highlight : null, highlightProperty));
		}

		if (parent != null) {
			parent.setFocusTraversable(true);
			parent.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
				if (e.getCode() != KeyCode.ESCAPE) {
					return;
				}
				btn.fire();
				e.consume();
			});
		}

		return btn;

	}

	public static Button newSendBtn() {

		Circle circle = new Circle(16.0);
		circle.setFill(Color.GREEN);
		circle.setStroke(Color.GREEN);
		circle.setStrokeWidth(2.0);
		Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -7.0, -10.5, 13.0, 0.0, -7.0, 10.5 });
		triangle.setFill(Color.ANTIQUEWHITE);
		Button btn = newButton(circle, triangle);

		return btn;

	}

	public static Button newAddBtn() {

		Circle circle = new Circle(10.0);
		circle.setStrokeWidth(3.0);
		circle.setFill(Color.TRANSPARENT);
		Line line1 = new Line(0.0, 5.0, 0.0, -5.0);
		line1.setStrokeLineCap(StrokeLineCap.ROUND);
		line1.setStrokeWidth(3.0);
		Line line2 = new Line(-5.0, 0.0, 5.0, 0.0);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(3.0);
		Button btn = newButton(circle, line1, line2);

		circle.strokeProperty().bind(
				Bindings.createObjectBinding(() -> btn.isHover() ? Color.GREEN : Color.GRAY, btn.hoverProperty()));
		line1.strokeProperty().bind(
				Bindings.createObjectBinding(() -> btn.isHover() ? Color.GREEN : Color.GRAY, btn.hoverProperty()));
		line2.strokeProperty().bind(
				Bindings.createObjectBinding(() -> btn.isHover() ? Color.GREEN : Color.GRAY, btn.hoverProperty()));

		return btn;

	}

	public static Button newRemoveBtn(double scaleFactor) {

		Circle circle = new Circle(10.0 * scaleFactor);
		circle.setStrokeWidth(3.0 * scaleFactor);
		circle.setFill(Color.TRANSPARENT);
		Line line = new Line(-5.0 * scaleFactor, 0.0, 5.0 * scaleFactor, 0.0);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		line.setStrokeWidth(3.0 * scaleFactor);
		Button btn = newButton(circle, line);

		circle.strokeProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.RED : Color.GRAY, btn.hoverProperty()));
		line.strokeProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.RED : Color.GRAY, btn.hoverProperty()));

		return btn;

	}

	public static Button newDeleteBtn() {

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
		Button btn = newButton(rectangle, line1, line2, line3);

		return btn;

	}

	public static Button newAttachBtnTransparent() {

		double scaleFactor = 0.85;

		Circle circle = new Circle(16.0);
		circle.setFill(Color.TRANSPARENT);
		Rectangle rectangle = new Rectangle(-12.0 * scaleFactor, -5.0 * scaleFactor, 24.0 * scaleFactor,
				10.0 * scaleFactor);
		rectangle.getStyleClass().add("ghost-shape");
		rectangle.setArcWidth(6.0 * scaleFactor);
		rectangle.setArcHeight(6.0 * scaleFactor);
		rectangle.setStrokeWidth(2.0);
		rectangle.setRotate(-45.0);
		rectangle.setFill(Color.TRANSPARENT);
		Line line = new Line(-6.0 * scaleFactor, 6.0 * scaleFactor, 3.0 * scaleFactor, -3.0 * scaleFactor);
		line.getStyleClass().add("ghost-shape");
		line.setStrokeWidth(2.0);
		Button btn = newButton(circle, rectangle, line);

		return btn;

	}

	public static Button newAttachBtn() {

		double scaleFactor = 1.0;

		Circle circle = new Circle(16.0);
		circle.setFill(Color.GREEN);
		Button btn = newButton(circle, newAttachGraph(scaleFactor, Color.ANTIQUEWHITE));

		return btn;

	}

	public static Label newAttachLbl(double scaleFactor) {

		Label lbl = newLabel(newAttachGraph(scaleFactor, Color.GRAY));
		lbl.getStyleClass().add("dim-label");

		return lbl;

	}

	private static Node newAttachGraph(double scaleFactor, Color color) {

		Rectangle rectangle = new Rectangle(-12.0 * scaleFactor, -5.0 * scaleFactor, 24.0 * scaleFactor,
				10.0 * scaleFactor);
		rectangle.setArcWidth(6.0 * scaleFactor);
		rectangle.setArcHeight(6.0 * scaleFactor);
		rectangle.setStrokeWidth(2.0 * scaleFactor);
		rectangle.setStroke(color);
		rectangle.setRotate(-45.0);
		rectangle.setFill(Color.TRANSPARENT);
		Line line = new Line(-6.0 * scaleFactor, 6.0 * scaleFactor, 3.0 * scaleFactor, -3.0 * scaleFactor);
		line.setStrokeWidth(2.0 * scaleFactor);
		line.setStroke(color);

		return new Group(rectangle, line);

	}

	public static Button newReportBtnTransparent() {

		Circle circle = new Circle(16.0);
		circle.setFill(Color.TRANSPARENT);
		Rectangle rectangle = new Rectangle(-6.0, -8.0, 12.0, 16.0);
		rectangle.getStyleClass().add("ghost-shape");
		rectangle.setStrokeWidth(2.0);
		rectangle.setFill(Color.TRANSPARENT);
		Line line1 = new Line(-2.0, -4.0, 2.0, -4.0);
		line1.getStyleClass().add("ghost-shape");
		line1.setStrokeWidth(2.0);
		Line line2 = new Line(-2.0, 0.0, 2.0, 0.0);
		line2.getStyleClass().add("ghost-shape");
		line2.setStrokeWidth(2.0);
		Line line3 = new Line(-2.0, 4.0, 2.0, 4.0);
		line3.getStyleClass().add("ghost-shape");
		line3.setStrokeWidth(2.0);
		Button btn = newButton(circle, rectangle, line1, line2, line3);

		return btn;

	}

	public static Button newInfoBtn() {

		Circle circle = new Circle(12.0);
		Button btn = newButton(circle);
		btn.setText("i");
		btn.setTextFill(Color.WHITE);
		btn.setContentDisplay(ContentDisplay.CENTER);
		btn.setStyle(CssFactory.getFontStyle(FontWeight.EXTRA_BOLD, FontPosture.ITALIC, 20.0 / 15));

		circle.fillProperty()
				.bind(Bindings.createObjectBinding(
						() -> btn.isHover() && !btn.isDisabled() ? Color.LIGHTSKYBLUE : Color.LIGHTGRAY,
						btn.hoverProperty(), btn.disabledProperty()));

		return btn;

	}

	public static Button newCancelBtn() {

		Circle circle = new Circle(10.0);
		circle.setFill(Color.RED);
		Line line1 = new Line(-4.0, -4.0, 4.0, 4.0);
		line1.setStrokeWidth(3.0);
		line1.setStroke(Color.WHITE);
		Line line2 = new Line(-4.0, 4.0, 4.0, -4.0);
		line2.setStrokeLineCap(StrokeLineCap.ROUND);
		line2.setStrokeWidth(3.0);
		line2.setStroke(Color.WHITE);
		Button btn = newButton(circle, line1, line2);

		return btn;

	}

	public static Button newSettingsBtn() {

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
		Button btn = newButton(circle, point0, point1, point2);

		colorProperty.bind(Bindings.createObjectBinding(() -> btn.isHover() ? Color.DODGERBLUE : Color.DARKGRAY,
				btn.hoverProperty()));

		final Effect dropShadow = new DropShadow(GAP, Color.DODGERBLUE);
		btn.effectProperty()
				.bind(Bindings.createObjectBinding(() -> btn.isHover() ? dropShadow : null, btn.hoverProperty()));

		return btn;

	}

	public static Button newSettingsMenuBtn() {

		Circle circle = new Circle(12.0, Color.TRANSPARENT);
		circle.setStroke(Color.WHITE);
		circle.setStrokeWidth(2.0);
		Circle point0 = new Circle(-6.0, 0.0, 2.0);
		point0.setFill(Color.WHITE);
		Circle point1 = new Circle(2.0);
		point1.setFill(Color.WHITE);
		Circle point2 = new Circle(6.0, 0.0, 2.0);
		point2.setFill(Color.WHITE);
		Button btn = newButton(circle, point0, point1, point2);

		return btn;

	}

	public static Button newSelectionBtn() {

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
		Button btn = newButton(circle, line1, line2);

		return btn;

	}

	public static Button newStarBtn() {

		double scaleFactor = 1.0;

		return newButton(newStarGraph(scaleFactor));

	}

	public static Label newStarLbl() {

		double scaleFactor = 0.65;

		return newLabel(newStarGraph(scaleFactor));

	}

	private static Node newStarGraph(double scaleFactor) {

		Polygon star = new Polygon();
		star.setFill(Color.YELLOW);
		Double[] points = new Double[10];
		for (int i = 0; i < 5; ++i) {
			points[2 * i] = 12.0 * scaleFactor * Math.cos(2 * Math.PI * (0.25 + 2.0 * i / 5));
			points[2 * i + 1] = -12.0 * scaleFactor * Math.sin(2 * Math.PI * (0.25 + 2.0 * i / 5));
		}
		star.getPoints().addAll(points);

		return star;

	}

	public static Button newForwardBtn() {

		double scaleFactor = 0.75;

		Circle circle = new Circle(-3.75 * scaleFactor, 1.25 * scaleFactor, 16.0 * scaleFactor);
		circle.setFill(Color.DODGERBLUE);
		Button btn = newButton(circle, newForwardGraph(scaleFactor, Color.ANTIQUEWHITE));

		return btn;

	}

	public static Button newGoToRefBtn() {

		double scaleFactor = 0.75;

		Circle circle = new Circle(-3.75 * scaleFactor, 1.25 * scaleFactor, 16.0 * scaleFactor);
		Button btn = newButton(circle, newForwardGraph(scaleFactor, Color.ANTIQUEWHITE));

		circle.fillProperty()
				.bind(Bindings.createObjectBinding(
						() -> btn.isHover() && !btn.isDisabled() ? Color.LIGHTSKYBLUE : Color.LIGHTGRAY,
						btn.hoverProperty(), btn.disabledProperty()));

		return btn;

	}

	public static Label newForwardLbl() {

		double scaleFactor = 0.5;

		return newLabel(newForwardGraph(scaleFactor, Color.DARKGRAY));

	}

	private static Node newForwardGraph(double scaleFactor, Color color) {

		Polygon arrowHead = new Polygon();
		arrowHead.getPoints()
				.addAll(new Double[] { 0.0, -10.0 * scaleFactor, 10.0 * scaleFactor, 0.0, 0.0, 10.0 * scaleFactor });
		arrowHead.setFill(color);
		Arc arc1 = new Arc(0.0, 10.0 * scaleFactor, 15.0 * scaleFactor, 14.0 * scaleFactor, 90.0, 90.0);
		arc1.setType(ArcType.ROUND);
		Arc arc2 = new Arc(0.0, 10.0 * scaleFactor, 15.0 * scaleFactor, 6.0 * scaleFactor, 90.0, 90.0);
		arc2.setType(ArcType.ROUND);
		Shape arrowTail = Shape.subtract(arc1, arc2);
		arrowTail.setFill(color);

		return new Group(arrowHead, arrowTail);

	}

	public static Button newVisibleBtn() {

		double scaleFactor = 0.65;

		return newButton(newEyeGraph(scaleFactor, Color.DEEPSKYBLUE));

	}

	public static Button newInvisibleBtn() {

		double scaleFactor = 0.65;

		return newButton(newNoEyeGraph(scaleFactor, Color.RED));

	}

	private static Node newEyeGraph(double scaleFactor, Color color) {

		Shape outerCircle = Shape.intersect(new Circle(0.0, -6.0 * scaleFactor, 16.0 * scaleFactor),
				new Circle(0.0, 6.0 * scaleFactor, 16.0 * scaleFactor));
		outerCircle.setStrokeLineJoin(StrokeLineJoin.ROUND);
		outerCircle.setStrokeWidth(3.0 * scaleFactor);
		outerCircle.setFill(Color.TRANSPARENT);
		outerCircle.setStroke(color);
		Circle innerCircle = new Circle(0.0, 0.0, 6.0 * scaleFactor);
		innerCircle.setStrokeWidth(3.0 * scaleFactor);
		innerCircle.setFill(Color.TRANSPARENT);
		innerCircle.setStroke(color);

		return new Group(outerCircle, innerCircle);

	}

	private static Node newNoEyeGraph(double scaleFactor, Color color) {

		Line line = new Line(-8.0 * scaleFactor, 12.0 * scaleFactor, 8.0 * scaleFactor, -12.0 * scaleFactor);
		line.setStrokeLineCap(StrokeLineCap.ROUND);
		line.setStrokeWidth(3.0 * scaleFactor);
		line.setStroke(color);

		return new Group(newEyeGraph(scaleFactor, color), line);

	}

	public static Button newSearchBtn() {

		Circle circle = new Circle(7.0);
		circle.setFill(Color.TRANSPARENT);
		circle.setStroke(Color.WHITE);
		circle.setStrokeWidth(3.0);
		Line line = new Line(5.0, 5.0, 11.5, 11.5);
		line.setStroke(Color.WHITE);
		line.setStrokeWidth(3.0);
		Button btn = newButton(circle, line);

		return btn;

	}

	public static Button newUpBtn() {

		Line line1 = new Line(-5.0, 0.0, 0.0, -5.0);
		line1.setStroke(Color.WHITE);
		line1.setStrokeWidth(3.0);
		Line line2 = new Line(0.0, -5.0, 5.0, 0.0);
		line2.setStroke(Color.WHITE);
		line2.setStrokeWidth(3.0);
		Button btn = newButton(line1, line2);

		return btn;

	}

	public static Button newDownBtn() {

		Line line1 = new Line(-5.0, -5.0, 0.0, 0.0);
		line1.setStroke(Color.WHITE);
		line1.setStrokeWidth(3.0);
		Line line2 = new Line(0.0, 0.0, 5.0, -5.0);
		line2.setStroke(Color.WHITE);
		line2.setStrokeWidth(3.0);
		Button btn = newButton(line1, line2);

		return btn;

	}

	public static Button newScrollToUnreadBtn() {

		Circle circle = new Circle(12.0);
		circle.setFill(Color.RED);
		Line line1 = new Line(-5.0, -2.0, 0.0, 3.0);
		line1.setStroke(Color.WHITE);
		line1.setStrokeWidth(3.0);
		Line line2 = new Line(0.0, 3.0, 5.0, -2.0);
		line2.setStroke(Color.WHITE);
		line2.setStrokeWidth(3.0);
		Button btn = newButton(circle, line1, line2);
		btn.setStyle("-fx-effect: dropshadow(gaussian, -dms-foreground, 1em, 0, 0, 0);");

		return btn;

	}

	public static Label newNoteLabel(String text) {
		Label noteLabel = new Label(text);
		noteLabel.getStyleClass().add("note-label");
		return noteLabel;
	}

}

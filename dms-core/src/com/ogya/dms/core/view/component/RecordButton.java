package com.ogya.dms.core.view.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

public class RecordButton extends Button {

	private final Timeline timeline = new Timeline();

	private final List<RecordListener> listeners = Collections.synchronizedList(new ArrayList<RecordListener>());

	public RecordButton() {

		super();

		init();

	}

	public void addRecordListener(RecordListener listener) {

		listeners.add(listener);

	}

	private void init() {

		double viewFactor = ViewFactory.getViewFactor();

		setPadding(Insets.EMPTY);
		setPickOnBounds(false);

		Circle circle = new Circle(16.0 * viewFactor);
		circle.setFill(Color.GREEN);
		final Arc recordArc = new Arc();
		recordArc.setCenterX(0.0);
		recordArc.setCenterY(0.0);
		recordArc.setStartAngle(90.0);
		recordArc.setType(ArcType.ROUND);
		recordArc.setFill(Color.RED);
		recordArc.visibleProperty().bind(timeline.statusProperty().isNotEqualTo(Animation.Status.STOPPED));
		Rectangle rectangle = new Rectangle(-3.0 * viewFactor, -10.0 * viewFactor, 6.0 * viewFactor, 16.0 * viewFactor);
		rectangle.setArcWidth(6.0 * viewFactor);
		rectangle.setArcHeight(6.0 * viewFactor);
		rectangle.setFill(Color.ANTIQUEWHITE);
		rectangle.setStroke(Color.ANTIQUEWHITE);
		Arc arc = new Arc(0.0, 3.0 * viewFactor, 7.0 * viewFactor, 7.0 * viewFactor, 180.0, 180.0);
		arc.setStrokeWidth(3.0 * viewFactor);
		arc.setStrokeLineCap(StrokeLineCap.ROUND);
		arc.setFill(Color.TRANSPARENT);
		arc.setStroke(Color.ANTIQUEWHITE);

		Group group = new Group(circle, recordArc, rectangle, arc);

		setGraphic(group);

		KeyFrame key0 = new KeyFrame(Duration.millis(0.0), new KeyValue(recordArc.radiusXProperty(), 0.0),
				new KeyValue(recordArc.radiusYProperty(), 0.0), new KeyValue(recordArc.lengthProperty(), 360.0));

		KeyFrame key1 = new KeyFrame(Duration.millis(1000.0), e -> {
			if (isPressed())
				listeners.forEach(listener -> listener.recordEventTriggered());
		}, new KeyValue(recordArc.radiusXProperty(), 15.9 * viewFactor),
				new KeyValue(recordArc.radiusYProperty(), 15.9 * viewFactor),
				new KeyValue(recordArc.lengthProperty(), 360.0));

		KeyFrame key2 = new KeyFrame(Duration.millis(11000.0), new KeyValue(recordArc.lengthProperty(), 0.0));

		timeline.setOnFinished(e -> {
			if (isPressed())
				listeners.forEach(listener -> listener.recordButtonReleased());
		});

		timeline.getKeyFrames().addAll(key0, key1, key2);

		setOnMousePressed(e -> {

			if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
				return;

			timeline.play();
			listeners.forEach(listener -> listener.recordButtonPressed());

		});

		setOnMouseReleased(e -> {

			if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
				return;

			timeline.stop();
			listeners.forEach(listener -> listener.recordButtonReleased());

		});

	}

	public void stopAnimation() {

		timeline.stop();

	}

	public static interface RecordListener {

		void recordButtonPressed();

		void recordEventTriggered();

		void recordButtonReleased();

	}

}
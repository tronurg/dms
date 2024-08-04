package com.dms.core.view.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dms.core.view.factory.ViewFactory;

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

		setPadding(Insets.EMPTY);
		setPickOnBounds(false);

		Circle circle = new Circle(17.0);
		circle.setFill(Color.GREEN);
		final Arc recordArc = new Arc(0.0, 0.0, 17.0, 17.0, 90.0, 360.0);
		recordArc.setType(ArcType.ROUND);
		recordArc.setFill(Color.RED);
		recordArc.visibleProperty().bind(timeline.statusProperty().isNotEqualTo(Animation.Status.STOPPED));
		Rectangle rectangle = new Rectangle(-3.0, -10.0, 6.0, 16.0);
		rectangle.setArcWidth(6.0);
		rectangle.setArcHeight(6.0);
		rectangle.setFill(Color.ANTIQUEWHITE);
		rectangle.setStroke(Color.ANTIQUEWHITE);
		Arc arc = new Arc(0.0, 3.0, 7.0, 7.0, 180.0, 180.0);
		arc.setStrokeWidth(3.0);
		arc.setStrokeLineCap(StrokeLineCap.ROUND);
		arc.setFill(Color.TRANSPARENT);
		arc.setStroke(Color.ANTIQUEWHITE);

		Group group = new Group(circle, recordArc, rectangle, arc);
		group.setStyle(ViewFactory.getScaleCss(1d, 1d));

		setGraphic(new Group(group));

		KeyFrame key0 = new KeyFrame(Duration.millis(0.0), new KeyValue(recordArc.scaleXProperty(), 0.0),
				new KeyValue(recordArc.scaleYProperty(), 0.0), new KeyValue(recordArc.lengthProperty(), 360.0));

		KeyFrame key1 = new KeyFrame(Duration.millis(1000.0), e -> {
			if (isPressed()) {
				listeners.forEach(listener -> listener.recordEventTriggered());
			}
		}, new KeyValue(recordArc.scaleXProperty(), 1.0), new KeyValue(recordArc.scaleYProperty(), 1.0),
				new KeyValue(recordArc.lengthProperty(), 360.0));

		KeyFrame key2 = new KeyFrame(Duration.millis(11000.0), new KeyValue(recordArc.lengthProperty(), 0.0));

		timeline.setOnFinished(e -> {
			if (isPressed()) {
				listeners.forEach(listener -> listener.recordButtonReleased());
			}
		});

		timeline.getKeyFrames().addAll(key0, key1, key2);

		setOnMousePressed(e -> {

			if (e.getButton() != MouseButton.PRIMARY) {
				return;
			}

			timeline.play();
			listeners.forEach(listener -> listener.recordButtonPressed());

		});

		setOnMouseReleased(e -> {

			if (e.getButton() != MouseButton.PRIMARY) {
				return;
			}

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

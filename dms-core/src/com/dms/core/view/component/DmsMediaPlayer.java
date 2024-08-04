package com.dms.core.view.component;

import java.nio.file.Files;
import java.nio.file.Path;

import com.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class DmsMediaPlayer extends GridPane {

	private MediaPlayer mediaPlayer;

	private final Button btn = new Button();
	private final ProgressBar progressBar = new ProgressBar(0.0);
	private final Label durationLbl = new Label();

	public DmsMediaPlayer(Path path) {

		super();

		if (path == null) {
			init();
			return;
		}

		try {

			if (Files.notExists(path)) {
				throw new Exception();
			}

			mediaPlayer = new MediaPlayer(new Media(path.toUri().toString()));
			init();

		} catch (Exception e) {

			initEmpty(path.getFileName().toString());

		}

	}

	private void init() {

		getStyleClass().addAll("hgap-2");

		initBtn();
		initProgressBar();

		add(btn, 0, 0, 1, 1);
		add(progressBar, 1, 0, 1, 1);

		if (mediaPlayer == null) {
			return;
		}

		initMediaPlayer();
		initDurationLbl();

		add(durationLbl, 2, 0, 1, 1);

	}

	private void initEmpty(String path) {

		getStyleClass().addAll("hgap-1");

		Button btn = new Button();
		Polygon triangle = new Polygon();
		triangle.setFill(Color.GRAY);
		triangle.getPoints().addAll(new Double[] { 0.0, 0.0, 8.0, 8.0, 8.0, -8.0 });
		Rectangle rectangle = new Rectangle(0.0, -3.0, 8.0, 6.0);
		rectangle.setFill(Color.GRAY);
		Group group = new Group(triangle, rectangle);
		group.setStyle(ViewFactory.getScaleCss(1d, 1d));
		btn.setGraphic(new Group(group));
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		Label lbl = new Label(path);
		lbl.getStyleClass().addAll("dim-label");
		lbl.setTooltip(new Tooltip(lbl.getText()));

		add(btn, 0, 0, 1, 1);
		add(lbl, 1, 0, 1, 1);

	}

	private void initMediaPlayer() {

		mediaPlayer.setOnReady(
				() -> durationLbl.setText(String.format("%02d:%02d", (int) mediaPlayer.getTotalDuration().toMinutes(),
						(int) (mediaPlayer.getTotalDuration().toSeconds() + 0.5))));

		mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {

			@Override
			public void changed(ObservableValue<? extends Duration> arg0, Duration arg1, Duration arg2) {

				progressBar.setProgress(arg2.toMillis() / mediaPlayer.getTotalDuration().toMillis());

			}

		});

		mediaPlayer.setOnEndOfMedia(() -> resetMediaPlayer());
		mediaPlayer.setOnError(() -> resetMediaPlayer());

	}

	private void initBtn() {

		double scaleFactor = 1.0;
		if (mediaPlayer == null) {
			scaleFactor *= 0.8;
		}

		btn.setStyle(ViewFactory.getPrefSizeCss(16 * scaleFactor, 16 * scaleFactor));
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		final Polygon triangle = new Polygon();
		triangle.setStyle(ViewFactory.getScaleCss(scaleFactor, scaleFactor));
		triangle.getPoints().addAll(new Double[] { -8.0, -8.0, -8.0, 8.0, 6.0, 0.0 });
		triangle.setFill(Color.GREEN);
		Group triangleGraph = new Group(triangle);

		if (mediaPlayer == null) {
			btn.setGraphic(triangle);
			return;
		}

		final Rectangle rectangle = new Rectangle(16.0, 16.0);
		rectangle.setStyle(ViewFactory.getScaleCss(scaleFactor, scaleFactor));
		rectangle.setFill(Color.RED);
		Group rectangleGraph = new Group(rectangle);

		btn.graphicProperty()
				.bind(Bindings.createObjectBinding(
						() -> mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING ? rectangleGraph : triangleGraph,
						mediaPlayer.statusProperty()));

		btn.setOnAction(e -> {
			if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
				resetMediaPlayer();
			} else {
				mediaPlayer.play();
			}
		});

	}

	private void initProgressBar() {

		if (mediaPlayer == null) {
			progressBar.getStyleClass().addAll("dummy-player");
			return;
		}

		progressBar.setOnMouseClicked(e -> {

			if (e.getButton() != MouseButton.PRIMARY) {
				return;
			}

			if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
				mediaPlayer.seek(
						Duration.millis(mediaPlayer.getTotalDuration().toMillis() * e.getX() / progressBar.getWidth()));
			}

		});

	}

	private void initDurationLbl() {

		durationLbl.getStyleClass().addAll("black-label");

	}

	private void resetMediaPlayer() {

		mediaPlayer.stop();
		mediaPlayer.seek(Duration.ZERO);

	}

}

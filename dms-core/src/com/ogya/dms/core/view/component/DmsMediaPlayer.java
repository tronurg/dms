package com.ogya.dms.core.view.component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.ogya.dms.core.view.factory.ViewFactory;

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
import javafx.scene.layout.Priority;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class DmsMediaPlayer extends GridPane {

	private final double gap = ViewFactory.getGap();

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

			if (Files.notExists(path))
				throw new Exception();

			mediaPlayer = new MediaPlayer(new Media(path.toUri().toString()));
			init();

		} catch (Exception e) {

			initEmpty(path.getFileName().toString());

		}

	}

	private void init() {

		setHgap(2 * gap);

		initBtn();
		initProgressBar();

		add(btn, 0, 0, 1, 1);
		add(progressBar, 1, 0, 1, 1);

		if (mediaPlayer == null)
			return;

		initMediaPlayer();
		initDurationLbl();

		add(durationLbl, 2, 0, 1, 1);

	}

	private void initEmpty(String path) {

		setHgap(gap);

		double viewFactor = ViewFactory.getViewFactor();

		Button btn = new Button();
		Polygon triangle = new Polygon();
		triangle.setFill(Color.GRAY);
		triangle.getPoints().addAll(new Double[] { 0.0 * viewFactor, 0.0, 8.0 * viewFactor, 8.0 * viewFactor,
				8.0 * viewFactor, -8.0 * viewFactor });
		Rectangle rectangle = new Rectangle(0.0 * viewFactor, -3.0 * viewFactor, 8.0 * viewFactor, 6.0 * viewFactor);
		rectangle.setFill(Color.GRAY);
		Group group = new Group(triangle, rectangle);
		btn.setGraphic(group);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);

		Label lbl = new Label(path);
		lbl.getStyleClass().add("dim-label");
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

		double viewFactor = ViewFactory.getViewFactor();
		if (mediaPlayer == null)
			viewFactor *= 0.8;

		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		btn.setPrefSize(16.0 * viewFactor, 16.0 * viewFactor);

		final Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -8.0 * viewFactor, -8.0 * viewFactor, -8.0 * viewFactor,
				8.0 * viewFactor, 6.0 * viewFactor, 0.0 });
		triangle.setFill(Color.GREEN);

		if (mediaPlayer == null) {
			btn.setGraphic(triangle);
			return;
		}

		final Rectangle rectangle = new Rectangle(16.0 * viewFactor, 16.0 * viewFactor);
		rectangle.setFill(Color.RED);
		btn.graphicProperty().bind(Bindings.createObjectBinding(
				() -> Objects.equals(mediaPlayer.getStatus(), MediaPlayer.Status.PLAYING) ? rectangle : triangle,
				mediaPlayer.statusProperty()));

		btn.setOnAction(e -> {
			if (Objects.equals(mediaPlayer.getStatus(), MediaPlayer.Status.PLAYING)) {
				resetMediaPlayer();
			} else {
				mediaPlayer.play();
			}
		});

	}

	private void initProgressBar() {

		GridPane.setHgrow(progressBar, Priority.ALWAYS);

		if (mediaPlayer == null) {
			progressBar.getStyleClass().add("dummy-player");
			return;
		}

		progressBar.setOnMouseClicked(e -> {

			if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
				return;

			if (Objects.equals(mediaPlayer.getStatus(), MediaPlayer.Status.PLAYING))
				mediaPlayer.seek(
						Duration.millis(mediaPlayer.getTotalDuration().toMillis() * e.getX() / progressBar.getWidth()));

		});

	}

	private void initDurationLbl() {

		durationLbl.getStyleClass().add("black-label");

	}

	private void resetMediaPlayer() {

		mediaPlayer.stop();
		mediaPlayer.seek(Duration.ZERO);

	}

}

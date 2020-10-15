package com.ogya.dms.view;

import java.nio.file.Path;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class DmsMediaPlayer extends GridPane {

	private final MediaPlayer mediaPlayer;

	private final Button btn = new Button();
	private final ProgressBar progressBar = new ProgressBar(0.0);
	private final Label durationLbl = new Label();

	public DmsMediaPlayer(Path path) {

		super();

		mediaPlayer = new MediaPlayer(new Media(path.toUri().toString()));

		init();

	}

	private void init() {

		initMediaPlayer();
		initBtn();
		initProgressBar();
		initDurationLbl();

		setHgap(10.0);

		add(btn, 0, 0, 1, 1);
		add(progressBar, 1, 0, 1, 1);
		add(durationLbl, 2, 0, 1, 1);

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

		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		btn.setPrefSize(16.0, 16.0);

		final Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -8.0, -8.0, -8.0, 8.0, 6.0, 0.0 });
		triangle.setFill(Color.GREEN);
		final Rectangle rectangle = new Rectangle(16.0, 16.0);
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

	}

	private void initDurationLbl() {

		durationLbl.getStyleClass().add("blackLabel");

	}

	private void resetMediaPlayer() {

		mediaPlayer.stop();
		mediaPlayer.seek(Duration.ZERO);

	}

}

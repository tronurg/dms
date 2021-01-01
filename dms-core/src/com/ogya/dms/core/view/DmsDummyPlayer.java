package com.ogya.dms.core.view;

import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class DmsDummyPlayer extends GridPane {

	private static final double GAP = ViewFactory.GAP;

	private final Button btn = new Button();
	private final ProgressBar progressBar = new ProgressBar(0.0);

	public DmsDummyPlayer() {

		super();

		init();

	}

	private void init() {

		initBtn();
		initProgressBar();

		setHgap(2 * GAP);

		add(btn, 0, 0, 1, 1);
		add(progressBar, 1, 0, 1, 1);

	}

	private void initBtn() {

		double viewFactor = 0.8 * ViewFactory.getViewFactor();

		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		btn.setPrefSize(16.0 * viewFactor, 16.0 * viewFactor);

		final Polygon triangle = new Polygon();
		triangle.getPoints().addAll(new Double[] { -8.0 * viewFactor, -8.0 * viewFactor, -8.0 * viewFactor,
				8.0 * viewFactor, 6.0 * viewFactor, 0.0 });
		triangle.setFill(Color.GREEN);

		btn.setGraphic(triangle);

	}

	private void initProgressBar() {

		progressBar.getStyleClass().add("dummy");

		GridPane.setHgrow(progressBar, Priority.ALWAYS);

	}

}

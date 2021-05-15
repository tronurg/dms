package com.ogya.dms.core.view.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ImPane extends HBox {

	private final TextArea messageArea = new TextArea();
	private final Button showFoldersBtn = ViewFactory.newAttachBtn();
	private final Button reportBtn = ViewFactory.newReportBtn();

	private final List<ImListener> listeners = Collections.synchronizedList(new ArrayList<ImListener>());

	public ImPane() {

		super();

		init();

	}

	private void init() {

		getStyleClass().add("im-box");

		setAlignment(Pos.CENTER);

		initMessageArea();
		initShowFoldersBtn();
		initReportBtn();

		getChildren().addAll(messageArea, showFoldersBtn, reportBtn);

	}

	private void initMessageArea() {

		messageArea.getStyleClass().add("message-area");

		HBox.setHgrow(messageArea, Priority.ALWAYS);
		messageArea.setPrefRowCount(1);
		messageArea.setWrapText(true);
		messageArea.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 400 ? null : change));

		messageArea.setOnKeyPressed(e -> {
			if (Objects.equals(e.getCode(), KeyCode.ENTER)) {
				if (e.isShiftDown()) {
					messageArea.appendText(System.lineSeparator());
				} else {
					listeners.forEach(listener -> listener.sendFired());
					e.consume();
				}
			}
		});

	}

	private void initShowFoldersBtn() {

		final Effect colorAdjust = new ColorAdjust(-0.9, 1.0, 0.25, 0.0);

		showFoldersBtn.effectProperty().bind(Bindings.createObjectBinding(
				() -> showFoldersBtn.isHover() ? colorAdjust : null, showFoldersBtn.hoverProperty()));

		showFoldersBtn.setOnAction(e -> listeners.forEach(listener -> listener.showFoldersClicked()));

	}

	private void initReportBtn() {

		final Effect colorAdjust = new ColorAdjust(-0.9, 1.0, 0.25, 0.0);

		reportBtn.effectProperty().bind(Bindings.createObjectBinding(() -> reportBtn.isHover() ? colorAdjust : null,
				reportBtn.hoverProperty()));

		reportBtn.setOnAction(e -> listeners.forEach(listener -> listener.reportClicked()));

	}

	public void addImListener(ImListener listener) {

		listeners.add(listener);

	}

	public final StringProperty messageProperty() {

		return messageArea.textProperty();

	}

	public final String getMessage() {

		return messageArea.getText();

	}

	public final void setMessage(String message) {

		messageArea.setText(message);

	}

	public static interface ImListener {

		void sendFired();

		void showFoldersClicked();

		void reportClicked();

	}

}

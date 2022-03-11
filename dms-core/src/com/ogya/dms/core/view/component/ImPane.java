package com.ogya.dms.core.view.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ogya.dms.core.common.CommonConstants;
import com.ogya.dms.core.view.factory.ViewFactory;
import com.sun.javafx.scene.control.skin.TextAreaSkin;

import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

public class ImPane extends HBox {

	private final TextArea messageArea = new TextArea() {
		@Override
		public Orientation getContentBias() {
			return Orientation.HORIZONTAL;
		}
	};
	private final Button showFoldersBtn = ViewFactory.newAttachBtnTransparent();
	private final Button reportBtn = ViewFactory.newReportBtnTransparent();

	private final List<ImListener> listeners = Collections.synchronizedList(new ArrayList<ImListener>());

	public ImPane() {

		super();

		init();

	}

	private void init() {

		getStyleClass().add("im-box");

		setAlignment(Pos.BOTTOM_CENTER);

		initMessageArea();
		initShowFoldersBtn();
		initReportBtn();

		getChildren().addAll(messageArea, showFoldersBtn, reportBtn);

	}

	private void initMessageArea() {

		messageArea.getStyleClass().add("message-area");

		messageArea.setSkin(new TextAreaSkin(messageArea));

		ScrollPane messageAreaScrollPane = (ScrollPane) messageArea.lookup(".scroll-pane");
		messageAreaScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);

		Text messageAreaText = (Text) messageAreaScrollPane.getContent().lookup(".text");
		messageAreaText.boundsInLocalProperty().addListener(new ChangeListener<Bounds>() {

			@Override
			public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {

				double height = newValue.getHeight();
				double singleLineHeight = messageAreaText.getFont().getSize();

				if (height < 2 * singleLineHeight) {
					messageArea.setPrefRowCount(1);
				} else if (height < 3 * singleLineHeight) {
					messageArea.setPrefRowCount(2);
				} else {
					messageArea.setPrefRowCount(3);
				}

			}

		});

		HBox.setHgrow(messageArea, Priority.ALWAYS);
		messageArea.setWrapText(true);
		messageArea.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 400 ? null : change));

		messageArea.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
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

		showFoldersBtn.opacityProperty().bind(Bindings.createObjectBinding(() -> showFoldersBtn.isHover() ? 1.0 : 0.5,
				showFoldersBtn.hoverProperty()));

		showFoldersBtn.setOnAction(e -> listeners.forEach(listener -> listener.showFoldersClicked()));

	}

	private void initReportBtn() {

		reportBtn.managedProperty().bind(reportBtn.visibleProperty());
		reportBtn.setVisible(!CommonConstants.REPORT_TEMPLATES.isEmpty());
		reportBtn.opacityProperty()
				.bind(Bindings.createObjectBinding(() -> reportBtn.isHover() ? 1.0 : 0.5, reportBtn.hoverProperty()));

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

	public final void focusOnMessageArea() {

		messageArea.requestFocus();

	}

	public static interface ImListener {

		void sendFired();

		void showFoldersClicked();

		void reportClicked();

	}

}

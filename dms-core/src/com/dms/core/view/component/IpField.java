package com.dms.core.view.component;

import java.util.function.UnaryOperator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

public class IpField extends HBox {

	private final TextField[] ipFields = new TextField[4];
	private final Label[] dots = new Label[3];

	private final BooleanProperty validProperty = new SimpleBooleanProperty(false);

	public IpField() {
		super();
		init();
	}

	private void init() {

		getStyleClass().addAll("gray-border");
		setFillHeight(true);

		for (int i = 0; i < ipFields.length; i++) {

			final int index = i;

			TextField ipField = new TextField();
			ipField.getStyleClass().addAll("bold", "transparent-tf");
			ipField.setPrefColumnCount(3);
			ipField.setAlignment(Pos.CENTER);

			ipField.textProperty().addListener((observable, oldValue, newValue) -> {

				validProperty.set(getIP() != null);

			});

			StringConverter<String> formatter = new StringConverter<String>() {

				@Override
				public String fromString(String str) {

					return str;

				}

				@Override
				public String toString(String str) {

					return str;

				}

			};

			UnaryOperator<TextFormatter.Change> filter = change -> {

				String str = change.getControlNewText();

				if (str.isEmpty()) {
					return change;
				}

				if (str.length() > 3) {
					return null;
				}

				try {

					int val = Integer.parseInt(str);

					if (val > 255) {
						ipField.setText("255");
					} else if (val < 0) {
						ipField.setText("0");
					}

					return (val > 255 || val < 0) ? null : change;

				} catch (NumberFormatException e) {

					return null;

				}

			};

			ipField.setTextFormatter(new TextFormatter<String>(formatter, "", filter));

			ipField.textProperty().addListener((observable, oldValue, newValue) -> {

				if (ipField.getAnchor() != ipField.getCaretPosition()) {
					return;
				}

				if (index < ipFields.length - 1 && newValue.length() == 3 && ipField.getCaretPosition() == 2) {

					ipFields[index + 1].requestFocus();
					ipFields[index + 1].selectAll();

				}

			});

			ipField.setOnKeyTyped(e -> {

				if (ipField.getAnchor() != ipField.getCaretPosition()) {
					return;
				}

				if (index < ipFields.length - 1 && ipField.getCaretPosition() == ipField.getText().length()
						&& e.getCharacter().charAt(0) == '.') {
					ipFields[index + 1].requestFocus();
					ipFields[index + 1].selectAll();
				}

			});

			ipField.setOnKeyPressed(e -> {

				if (ipField.getAnchor() != ipField.getCaretPosition()) {
					return;
				}

				switch (e.getCode()) {

				case LEFT:
					if (index > 0 && ipField.getCaretPosition() == 0) {
						ipFields[index - 1].requestFocus();
						ipFields[index - 1].positionCaret(ipFields[index - 1].getText().length());
					}
					break;

				case BACK_SPACE:
					if (index > 0 && ipField.getCaretPosition() == 0) {
						if (ipFields[index - 1].getText().length() > 0) {
							String ip = ipFields[index - 1].getText();
							ipFields[index - 1].setText(ip.substring(0, ip.length() - 1));
						}
						ipFields[index - 1].requestFocus();
						ipFields[index - 1].positionCaret(ipFields[index - 1].getText().length());
					}

					break;

				case RIGHT:
					if (index < ipFields.length - 1 && ipField.getCaretPosition() == ipField.getText().length()) {
						ipFields[index + 1].requestFocus();
						ipFields[index + 1].positionCaret(0);
					}
					break;

				default:
					break;

				}

			});

			ipFields[i] = ipField;

			getChildren().add(ipField);

			if (i < dots.length) {

				Label dot = new Label(".");
				dot.getStyleClass().addAll("bold");
				dot.setMaxHeight(Double.MAX_VALUE);

				dots[i] = dot;

				this.getChildren().add(dot);

			}

		}

	}

	public String getIP() {

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < ipFields.length; i++) {

			String txt = ipFields[i].getText();

			if (txt.isEmpty()) {
				return null;
			}

			sb.append(txt);

			if (i == ipFields.length - 1) {
				break;
			}

			sb.append('.');

		}

		return sb.toString();

	}

	public void setIP(String ip) {

		String[] fields = ip.split("\\.");

		for (int i = 0; i < fields.length && i < ipFields.length; i++) {

			if (fields[i].isEmpty()) {

				ipFields[i].setText("");

				continue;

			}

			try {

				int val = Integer.parseInt(fields[i]);

				if (val > 255) {
					val = 255;
				}

				ipFields[i].setText(String.valueOf(val));

			} catch (NumberFormatException e) {

			}

		}

	}

	public void clearIP() {

		for (TextField ipField : ipFields) {

			ipField.setText("");

		}

	}

	public void focusOnIpField() {

		ipFields[0].requestFocus();

	}

	public BooleanProperty validProperty() {

		return validProperty;

	}

	public boolean isValid() {

		return validProperty.get();

	}

}

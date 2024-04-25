package com.ogya.dms.core.view.factory;

import java.util.Locale;

import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class CssFactory {

	public static String getFontStyle(FontWeight fontWeight, FontPosture fontStyle, double fontSize) {
		StringBuilder strBuilder = new StringBuilder();
		if (fontSize > 0) {
			strBuilder.append(String.format(Locale.ROOT, "-fx-font-size: %.2fem;", fontSize));
		}
		if (fontStyle != null) {
			strBuilder.append(
					String.format(Locale.ROOT, "-fx-font-style: %s;", fontStyle.name().toLowerCase(Locale.ROOT)));
		}
		if (fontWeight != null) {
			strBuilder.append(String.format(Locale.ROOT, "-fx-font-weight: %d;", fontWeight.getWeight()));
		}
		return strBuilder.toString();
	}

}

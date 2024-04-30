package com.ogya.dms.core.view.component;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class DmsBox extends VBox {

	public DmsBox(Node node, String styleClass) {
		super();
		getStyleClass().add(styleClass);
		getChildren().add(node);
		setPickOnBounds(false);
//		setMaxHeight(USE_PREF_SIZE);
//		setMaxWidth(USE_PREF_SIZE);
		setStyle("-fx-border-color: blue;");
	}

}

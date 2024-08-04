package com.dms.core.view.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class DmsBox {

	public static Node wrap(Node node, Pos alignment, String... styleClasses) {
		StackPane dmsBox = new StackPane(node);
		dmsBox.getStyleClass().addAll(styleClasses);
		dmsBox.setAlignment(alignment);
		dmsBox.setPickOnBounds(false);
		dmsBox.visibleProperty().bind(node.visibleProperty());
		dmsBox.managedProperty().bind(dmsBox.visibleProperty());
		return dmsBox;
	}

	public static Node wrap(Node node, String... styleClasses) {
		Label dmsBox = new Label();
		dmsBox.getStyleClass().addAll(styleClasses);
		dmsBox.setGraphic(node);
		dmsBox.setWrapText(true);
		dmsBox.setPickOnBounds(false);
		dmsBox.visibleProperty().bind(node.visibleProperty());
		dmsBox.managedProperty().bind(dmsBox.visibleProperty());
		return dmsBox;
	}

}

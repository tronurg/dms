package com.ogya.dms.core.view.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class DmsBox extends StackPane {

	private DmsBox(Node node, Pos alignment, String... styleClasses) {
		super(node);
		getStyleClass().addAll(styleClasses);
		setAlignment(alignment);
		setPickOnBounds(false);
		visibleProperty().bind(node.visibleProperty());
		managedProperty().bind(visibleProperty());
	}

	public static Node wrap(Node node, Pos alignment, String... styleClasses) {
		return new DmsBox(node, alignment, styleClasses);
	}

}

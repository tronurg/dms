package com.ogya.dms.core.view;

import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;

class SelectableEntityPane extends EntityPaneBase {

	private final Button selectionBtn = ViewFactory.newSelectionBtn();

	private final BooleanProperty selectProperty = new SimpleBooleanProperty(false);

	SelectableEntityPane() {

		super();

		init();

	}

	private void init() {

		selectionBtn.setMouseTransparent(true);
		selectionBtn.opacityProperty().bind(Bindings.createDoubleBinding(
				() -> selectProperty.and(activeProperty()).get() ? 1.0 : 0.2, selectProperty, activeProperty()));

		addRightNode(selectionBtn);

	}

	final BooleanProperty selectProperty() {

		return selectProperty;

	}

}
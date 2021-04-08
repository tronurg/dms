package com.ogya.dms.core.view;

import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;

class EntityCard extends EntityPaneBase {

	private final Button selectionBtn = ViewFactory.newSelectionBtn();

	private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

	EntityCard() {

		super();

		init();

	}

	private void init() {

		activeProperty().addListener((e0, e1, e2) -> {

			if (!e2)
				selectedProperty.set(false);

		});

		selectionBtn.opacityProperty()
				.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

		addRightNode(selectionBtn);

	}

	final BooleanProperty selectedProperty() {

		return selectedProperty;

	}

}
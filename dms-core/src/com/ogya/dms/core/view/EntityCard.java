package com.ogya.dms.core.view;

import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

class EntityCard extends GridPane {

	protected final EntityPaneBase entityPane = new EntityPaneBase();
	private final Button selectionBtn = ViewFactory.newSelectionBtn();

	protected final BooleanProperty activeProperty = new SimpleBooleanProperty(true);
	protected final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

	EntityCard() {

		super();

		init();

	}

	private void init() {

		activeProperty.addListener((e0, e1, e2) -> {

			if (!e2)
				selectedProperty.set(false);

		});

		RowConstraints row1 = new RowConstraints();
		row1.setPercentHeight(70.0);
		RowConstraints row2 = new RowConstraints();
		row2.setPercentHeight(30.0);
		getRowConstraints().addAll(row1, row2);

		GridPane.setHgrow(entityPane, Priority.ALWAYS);

		selectionBtn.opacityProperty()
				.bind(Bindings.createDoubleBinding(() -> selectedProperty.get() ? 1.0 : 0.2, selectedProperty));

		add(entityPane, 0, 0, 1, 2);
		add(selectionBtn, 1, 0, 1, 1);

	}

	protected final BooleanProperty activeProperty() {

		return activeProperty;

	}

	protected final BooleanProperty selectedProperty() {

		return selectedProperty;

	}

}
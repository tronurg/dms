package com.dms.core.view.component;

import com.dms.core.util.Commons;
import com.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class SearchField extends HBox {

	private final boolean allowFilter;

	private final TextField searchTextField = new TextField();

	private final BooleanProperty filterOnlineProperty = new SimpleBooleanProperty(false);

	public SearchField(boolean allowFilter) {
		super();
		this.allowFilter = allowFilter;
		init();
	}

	private void init() {

		getStyleClass().addAll("gray-underline");
		setAlignment(Pos.CENTER_LEFT);

		initSearchTextField();

		getChildren().add(searchTextField);

		if (allowFilter) {
			getChildren().add(DmsBox.wrap(newVisibleBtn(), Pos.CENTER_RIGHT, "padding-1"));
		}

	}

	private void initSearchTextField() {

		HBox.setHgrow(searchTextField, Priority.ALWAYS);

		searchTextField.setPromptText(Commons.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	private Node newVisibleBtn() {

		Button visibleBtn = ViewFactory.newVisibleBtn();

		final Effect glow = new Glow();
		visibleBtn.effectProperty().bind(
				Bindings.createObjectBinding(() -> filterOnlineProperty.get() ? glow : null, filterOnlineProperty));
		visibleBtn.opacityProperty()
				.bind(Bindings.createDoubleBinding(() -> visibleBtn.isHover() || filterOnlineProperty.get() ? 1.0 : 0.5,
						visibleBtn.hoverProperty(), filterOnlineProperty));
		visibleBtn.visibleProperty().bind(hoverProperty().or(filterOnlineProperty));

		visibleBtn.setOnAction(e -> filterOnlineProperty.set(!filterOnlineProperty.get()));

		return visibleBtn;

	}

	public final StringProperty textProperty() {

		return searchTextField.textProperty();

	}

	public final String getText() {

		return searchTextField.getText();

	}

	public final BooleanProperty filterOnlineProperty() {

		return filterOnlineProperty;

	}

	public void reset() {

		searchTextField.setText("");
		filterOnlineProperty.set(false);

	}

}

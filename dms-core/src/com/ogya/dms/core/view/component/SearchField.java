package com.ogya.dms.core.view.component;

import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class SearchField extends HBox {

	private static final double GAP = ViewFactory.GAP;

	private final boolean allowFilter;

	private final TextField searchTextField = new TextField();

	private final BooleanProperty filterOnlineProperty = new SimpleBooleanProperty(false);

	public SearchField(boolean allowFilter) {

		super();

		this.allowFilter = allowFilter;

		init();

	}

	private void init() {

		setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
		setAlignment(Pos.CENTER);

		initSearchTextField();

		getChildren().add(searchTextField);

		if (allowFilter) {
			getChildren().add(getVisibleBtn());
		}

	}

	private void initSearchTextField() {

		HBox.setHgrow(searchTextField, Priority.ALWAYS);

		searchTextField.setPromptText(Commons.translate("FIND"));
		searchTextField.setFocusTraversable(false);

	}

	private Button getVisibleBtn() {

		final Button visibleBtn = ViewFactory.newVisibleBtn(0.65);
		HBox.setMargin(visibleBtn, new Insets(GAP));

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

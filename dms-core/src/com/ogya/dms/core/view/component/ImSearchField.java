package com.ogya.dms.core.view.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ImSearchField extends HBox {

	private final TextField searchTextField = new TextField();
	private final Button upBtn = ViewFactory.newUpBtn();
	private final Button downBtn = ViewFactory.newDownBtn();
	private final Button searchBtn = ViewFactory.newSearchBtn();

	private final List<ImSearchListener> listeners = Collections.synchronizedList(new ArrayList<ImSearchListener>());

	public ImSearchField() {

		super(ViewFactory.GAP);

		init();

	}

	private void init() {

		setAlignment(Pos.CENTER);

		initSearchTextField();
		initUpBtn();
		initDownBtn();
		initSearchBtn();

		getChildren().addAll(searchTextField, upBtn, downBtn, searchBtn);

	}

	private void initSearchTextField() {

		searchTextField.getStyleClass().add("search-field");
		HBox.setHgrow(searchTextField, Priority.ALWAYS);
		searchTextField.setMaxWidth(Double.MAX_VALUE);
		searchTextField.setOnKeyPressed(e -> {
			if (Objects.equals(e.getCode(), KeyCode.ENTER))
				searchBtn.fire();
		});

	}

	private void initUpBtn() {

		upBtn.setOnAction(e -> listeners.forEach(listener -> listener.upRequested()));

	}

	private void initDownBtn() {

		downBtn.setOnAction(e -> listeners.forEach(listener -> listener.downRequested()));

	}

	private void initSearchBtn() {

		searchBtn.setOnAction(e -> {
			String fulltext = searchTextField.textProperty().getValueSafe().trim();
			if (!fulltext.isEmpty())
				listeners.forEach(listener -> listener.searchRequested(fulltext));
		});

	}

	public void addImSearchListener(ImSearchListener listener) {

		listeners.add(listener);

	}

	public void clear() {
		searchTextField.clear();
	}

	public void setTextFieldStyle(String style) {
		searchTextField.setStyle(style);
	}

	public final BooleanProperty upDisableProperty() {
		return upBtn.disableProperty();
	}

	public final BooleanProperty downDisableProperty() {
		return downBtn.disableProperty();
	}

	public final StringProperty textProperty() {
		return searchTextField.textProperty();
	}

	@Override
	public void requestFocus() {
		searchTextField.requestFocus();
	}

	public static interface ImSearchListener {

		void searchRequested(String fulltext);

		void upRequested();

		void downRequested();

	}

}

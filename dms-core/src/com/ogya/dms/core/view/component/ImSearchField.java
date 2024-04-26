package com.ogya.dms.core.view.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	private final String errorCssClass = "red-text";

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

		searchTextField.getStyleClass().addAll("search-field");
		HBox.setHgrow(searchTextField, Priority.ALWAYS);
		searchTextField.setMaxWidth(Double.MAX_VALUE);
		searchTextField.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				searchBtn.fire();
			}
		});

	}

	private void initUpBtn() {

		upBtn.managedProperty().bind(upBtn.visibleProperty());
		upBtn.setOnAction(e -> listeners.forEach(listener -> listener.upRequested()));

	}

	private void initDownBtn() {

		downBtn.managedProperty().bind(downBtn.visibleProperty());
		downBtn.setOnAction(e -> listeners.forEach(listener -> listener.downRequested()));

	}

	private void initSearchBtn() {

		searchBtn.setOnAction(e -> {
			String fulltext = searchTextField.textProperty().getValueSafe().trim();
			if (!fulltext.isEmpty()) {
				listeners.forEach(listener -> listener.searchRequested(fulltext));
			}
		});

	}

	public void addImSearchListener(ImSearchListener listener) {

		listeners.add(listener);

	}

	public void fireSearchUp() {
		upBtn.fire();
	}

	public void fireSearchDown() {
		downBtn.fire();
	}

	public void clear() {
		searchTextField.clear();
	}

	public void setError(boolean arg0) {
		if (arg0) {
			searchTextField.getStyleClass().addAll(errorCssClass);
		} else {
			searchTextField.getStyleClass().remove(errorCssClass);
		}
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

	public void setNavigationDisabled(boolean disabled) {
		upBtn.setVisible(!disabled);
		downBtn.setVisible(!disabled);
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

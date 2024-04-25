package com.ogya.dms.core.view;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.component.DmsScrollPane;
import com.ogya.dms.core.view.component.IpField;
import com.ogya.dms.core.view.factory.CssFactory;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;

public class RemoteIpSettingsPane extends BorderPane {

	private static final double GAP = ViewFactory.GAP;

	private final HBox topPane = new HBox();
	private final VBox centerPane = new VBox();

	private final Button backBtn;
	private final Label headingLabel = new Label(Commons.translate("EDIT_REMOTE_IPS"));

	private final HBox addIpPane = new HBox(2 * GAP);

	private final IpField ipField = new IpField();
	private final Button addIpButton = ViewFactory.newAddBtn();

	private final VBox scrollableContent = new VBox(GAP);
	private final ScrollPane scrollPane = new DmsScrollPane(scrollableContent);

	private final AtomicReference<Consumer<String>> removeIpActionRef = new AtomicReference<Consumer<String>>();

	RemoteIpSettingsPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);

		init();

	}

	private void init() {

		addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() != KeyCode.ENTER) {
				return;
			}
			addIpButton.fire();
		});

		initTopPane();
		initCenterPane();

		scrollPane.setFitToWidth(true);

		setTop(topPane);
		setCenter(centerPane);

	}

	void setOnBackAction(Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void setOnAddIpAction(Consumer<String> consumer) {

		addIpButton.setOnAction(e -> consumer.accept(ipField.getIP()));

	}

	void setOnRemoveIpAction(Consumer<String> consumer) {

		removeIpActionRef.set(consumer);

	}

	void clearIpField() {

		ipField.clearIP();

	}

	void focusOnIpField() {

		ipField.focusOnIpField();

	}

	void clearAll() {

		clearIpField();

		scrollableContent.getChildren().clear();

	}

	void updateIps(final String[] ips) {

		scrollableContent.getChildren().clear();

		for (String ip : ips) {

			if (ip.isEmpty()) {
				continue;
			}

			HBox ipField = new HBox(GAP);

			Label ipLabel = new Label(ip);
			ipLabel.setStyle(CssFactory.getFontStyle(FontWeight.BOLD, null, 18.0 / 15));

			Button removeIpButton = ViewFactory.newRemoveBtn(1.0);

			removeIpButton.setOnAction(e -> {

				Consumer<String> removeIpAction = removeIpActionRef.get();

				if (removeIpAction != null) {
					removeIpAction.accept(ip);
				}

			});

			ipField.getChildren().addAll(removeIpButton, ipLabel);

			scrollableContent.getChildren().add(ipField);

		}

	}

	void scrollToTop() {

		scrollPane.setVvalue(scrollPane.getVmin());

	}

	private void initTopPane() {

		topPane.getStyleClass().add("top-pane");

		initHeadingLabel();

		topPane.getChildren().addAll(backBtn, headingLabel);

	}

	private void initCenterPane() {

		initAddIpPane();
		initScrollPane();

		centerPane.getChildren().addAll(addIpPane, new Separator(), scrollPane);

	}

	private void initHeadingLabel() {

		headingLabel.getStyleClass().add("black-label");
		headingLabel.setStyle(CssFactory.getFontStyle(FontWeight.BOLD, null, 22.0 / 15));

	}

	private void initAddIpPane() {

		initIpField();
		initAddIpButton();

		addIpPane.setPadding(new Insets(2 * GAP));

		addIpPane.getChildren().addAll(ipField, addIpButton);

	}

	private void initScrollPane() {

		initScrollableContent();

		scrollPane.getStyleClass().add("edge-to-edge");
		VBox.setVgrow(scrollPane, Priority.ALWAYS);

	}

	private void initScrollableContent() {

		scrollableContent.setPadding(new Insets(2 * GAP));

	}

	private void initIpField() {

		ipField.setStyle(CssFactory.getFontStyle(FontWeight.BOLD, null, 16.0 / 15));

	}

	private void initAddIpButton() {

		addIpButton.setMaxHeight(Double.MAX_VALUE);
		addIpButton.disableProperty().bind(ipField.validProperty().not());

	}

}

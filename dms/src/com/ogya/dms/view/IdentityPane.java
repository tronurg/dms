package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Identity;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

class IdentityPane extends GridPane {

	private static final double SIZE = 30.0;

	private final Group profilePicture = new Group();
	private final Circle statusCircle = new Circle(SIZE);
	private final Circle profileRound = new Circle(SIZE * 0.8);
	private final Label profileLabel = new Label();

	private final Label nameLabel = new Label();
	private final Button settingsButton = ViewFactory.newSettingsBtn();
	private final TextField commentTextField = new TextField();
	private final Label coordinatesLabel = new Label();

	private final List<IIdentityPane> listeners = Collections.synchronizedList(new ArrayList<IIdentityPane>());

	private final AtomicBoolean commentEditable = new AtomicBoolean(true);

	IdentityPane() {

		super();

		init();

	}

	private void init() {

		initProfilePicture();
		initStatusCircle();
		initProfileRound();
		initProfileLabel();
		initNameLabel();
		initSettingsButton();
		initCommentTextField();
		initCoordinatesLabel();

		setHgap(5.0);
		setValignment(profilePicture, VPos.TOP);
		setHgrow(commentTextField, Priority.ALWAYS);

		add(profilePicture, 0, 0, 1, 3);
		add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		add(nameLabel, 2, 0, 1, 1);
		add(settingsButton, 3, 0, 1, 1);
		add(commentTextField, 2, 1, 1, 1);
		add(coordinatesLabel, 2, 2, 1, 1);

	}

	void addListener(IIdentityPane listener) {

		listeners.add(listener);

	}

	void setIdentity(Identity identity) {

		statusCircle.setStroke(identity.getStatus().getStatusColor());
		profileLabel.setText(identity.getName().substring(0, 1).toUpperCase());

		nameLabel.setText(identity.getName());
		if (!commentTextField.isEditable())
			commentTextField.setText(identity.getComment());
		coordinatesLabel.setText(identity.getLattitude() == null || identity.getLongitude() == null ? ""
				: "(" + String.format("%.2f", identity.getLattitude()) + String.format("%.2f", identity.getLongitude())
						+ ")");

	}

	void setCommentEditable(boolean editable) {

		commentEditable.set(editable);

		if (!editable)
			commentTextField.setEditable(false);

	}

	private void initProfilePicture() {

		profilePicture.getChildren().addAll(statusCircle, profileRound, profileLabel);

		profilePicture.setOnMouseClicked(e -> {

			if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
				return;

			listeners.forEach(listener -> listener.updateStatusClicked());

		});

	}

	private void initStatusCircle() {

		statusCircle.setStrokeWidth(SIZE * 0.2);
		statusCircle.setFill(Color.TRANSPARENT);

	}

	private void initProfileRound() {

		profileRound.setFill(Color.DARKGRAY);

	}

	private void initProfileLabel() {

		profileLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		profileLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE));

		profileLabel.translateXProperty().bind(Bindings
				.createDoubleBinding(() -> -profileLabel.widthProperty().get() / 2, profileLabel.widthProperty()));
		profileLabel.translateYProperty().bind(Bindings
				.createDoubleBinding(() -> -profileLabel.heightProperty().get() / 2, profileLabel.heightProperty()));

	}

	private void initNameLabel() {

		nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLabel.setFont(Font.font(null, FontWeight.BOLD, SIZE * 0.8));

	}

	private void initSettingsButton() {

		settingsButton.setOnAction(e -> listeners.forEach(listener -> listener.settingsClicked()));

	}

	private void initCommentTextField() {

		final AtomicReference<String> lastComment = new AtomicReference<String>();

		commentTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

		commentTextField.setPromptText(CommonMethods.translate("TYPE_COMMENT"));
		commentTextField.setFocusTraversable(false);
		commentTextField.setEditable(false);

		commentTextField.setOnMouseClicked(e -> {

			if (!commentEditable.get() || commentTextField.isEditable())
				return;

			if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
				return;

			lastComment.set(commentTextField.getText());
			commentTextField.setEditable(true);

		});

		commentTextField.setOnKeyPressed(e -> {

			if (!commentEditable.get())
				return;

			KeyCode code = e.getCode();
			if (!(Objects.equals(code, KeyCode.ENTER) || Objects.equals(code, KeyCode.ESCAPE)))
				return;

			if (Objects.equals(code, KeyCode.ESCAPE))
				commentTextField.setText(lastComment.get());

			e.consume();
			commentTextField.setEditable(false);
			requestFocus();

			final String comment = commentTextField.getText();

			listeners.forEach(listener -> listener.commentUpdated(comment));

		});

		commentTextField.setBorder(new Border(new BorderStroke[] { new BorderStroke(Color.LIGHTGRAY,
				BorderStrokeStyle.SOLID, new CornerRadii(15.0), BorderWidths.DEFAULT) }));
		commentTextField.setBackground(Background.EMPTY);

	}

	private void initCoordinatesLabel() {

		coordinatesLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

}

interface IIdentityPane {

	void commentUpdated(String comment);

	void updateStatusClicked();

	void settingsClicked();

}

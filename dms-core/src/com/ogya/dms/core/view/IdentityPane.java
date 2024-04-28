package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

class IdentityPane extends GridPane {

	private static final double RADIUS = 30.0;

	private final Circle statusCircle = new Circle(RADIUS);
	private final Circle profileRound = new Circle(0.8 * RADIUS);
	private final Label profileLbl = new Label();
	private final Button availableBtn = newStatusBtn(Availability.AVAILABLE.getStatusColor());
	private final Button awayBtn = newStatusBtn(Availability.AWAY.getStatusColor());
	private final Button busyBtn = newStatusBtn(Availability.BUSY.getStatusColor());
	private final StackPane profilePicture = new StackPane(new Group(statusCircle, profileRound, profileLbl), busyBtn,
			awayBtn, availableBtn);

	private final Label nameLbl = new Label();
	private final Button settingsButton = ViewFactory.newSettingsBtn();
	private final TextField commentTextField = new TextField();
	private final Label coordinatesLbl = new Label();

	private final List<IIdentityPane> listeners = Collections.synchronizedList(new ArrayList<IIdentityPane>());

	private final AtomicBoolean commentEditable = new AtomicBoolean(true);
	private final ObjectProperty<Availability> availabilityProperty = new SimpleObjectProperty<Availability>();

	IdentityPane() {
		super();
		init();
	}

	private void init() {

		getStyleClass().addAll("hgap-1");

		initProfilePicture();
		initNameLbl();
		initSettingsButton();
		initCommentTextField();
		initCoordinatesLbl();

		add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		add(nameLbl, 2, 0, 1, 1);
		add(settingsButton, 3, 0, 1, 1);
		add(commentTextField, 2, 1, 1, 1);
		add(coordinatesLbl, 2, 2, 1, 1);
		add(profilePicture, 0, 0, 1, 3);

	}

	void addListener(IIdentityPane listener) {

		listeners.add(listener);

	}

	void setIdentity(Contact identity) {

		availabilityProperty.set(identity.getStatus());
		profileLbl.setText(identity.getName().substring(0, 1).toUpperCase(Locale.getDefault()));

		nameLbl.setText(identity.getName());
		if (!commentTextField.isEditable()) {
			commentTextField.setText(identity.getComment());
		}
		coordinatesLbl.setText(identity.getLatitude() == null || identity.getLongitude() == null ? ""
				: Commons.convertDoubleToCoordinates(identity.getLatitude(), identity.getLongitude()));

	}

	void setCommentEditable(boolean editable) {

		commentEditable.set(editable);

		if (!editable) {
			commentTextField.setEditable(false);
		}

	}

	private void initProfilePicture() {

		GridPane.setValignment(profilePicture, VPos.TOP);
		GridPane.setFillHeight(profilePicture, false);

		initStatusCircle();
		initProfileRound();
		initProfileLbl();

		final Rotate awayBtnRotate = new Rotate();
		final Rotate busyBtnRotate = new Rotate();

		awayBtnRotate.pivotXProperty().bind(awayBtn.widthProperty().divide(2).subtract(awayBtn.translateXProperty()));
		awayBtnRotate.pivotYProperty().bind(awayBtn.heightProperty().divide(2).subtract(awayBtn.translateYProperty()));
		busyBtnRotate.pivotXProperty().bind(busyBtn.widthProperty().divide(2).subtract(busyBtn.translateXProperty()));
		busyBtnRotate.pivotYProperty().bind(busyBtn.heightProperty().divide(2).subtract(busyBtn.translateYProperty()));

		awayBtn.getTransforms().add(awayBtnRotate);
		busyBtn.getTransforms().add(busyBtnRotate);

		final Interpolator interpolator = Interpolator.EASE_BOTH;

		final Transition mainTransition = new Transition() {

			private double awayBtnStart;
			private double awayBtnEnd;
			private double busyBtnStart;
			private double busyBtnEnd;
			private int position = 0;

			private EventHandler<ActionEvent> onFinished = e -> {
				if (position == 0) {
					availableBtn.setVisible(false);
					awayBtn.setVisible(false);
					busyBtn.setVisible(false);
				}
			};

			{
				setCycleDuration(Duration.millis(100.0));
				setOnFinished(onFinished);
			}

			@Override
			protected void interpolate(double arg0) {

				awayBtnRotate.setAngle(interpolator.interpolate(awayBtnStart, awayBtnEnd, arg0));
				busyBtnRotate.setAngle(interpolator.interpolate(busyBtnStart, busyBtnEnd, arg0));

			}

			@Override
			public void play() {
				if (position == 0) {
					availableBtn.setVisible(true);
					awayBtn.setVisible(true);
					busyBtn.setVisible(true);
				}
				awayBtnStart = position * 45.0;
				busyBtnStart = position * 90.0;
				position = (position + 1) % 2;
				awayBtnEnd = position * 45.0;
				busyBtnEnd = position * 90.0;
				super.play();
			}

		};

		availableBtn.setOnAction(e -> {

			mainTransition.play();

			listeners.forEach(listener -> listener.statusUpdateRequested(Availability.AVAILABLE));

		});

		awayBtn.setOnAction(e -> {

			mainTransition.play();

			listeners.forEach(listener -> listener.statusUpdateRequested(Availability.AWAY));

		});

		busyBtn.setOnAction(e -> {

			mainTransition.play();

			listeners.forEach(listener -> listener.statusUpdateRequested(Availability.BUSY));

		});

		profilePicture.setOnMouseClicked(e -> {

			if (e.getButton() != MouseButton.PRIMARY) {
				return;
			}

			mainTransition.play();

		});

	}

	private void initStatusCircle() {

		statusCircle.setStyle(ViewFactory.getScaleCss(1d, 1d));
		statusCircle.setStrokeWidth(0.2 * RADIUS);
		statusCircle.setFill(Color.TRANSPARENT);
		statusCircle.strokeProperty()
				.bind(Bindings.createObjectBinding(
						() -> availabilityProperty.get() == null ? Availability.AVAILABLE.getStatusColor()
								: availabilityProperty.get().getStatusColor(),
						availabilityProperty));

	}

	private void initProfileRound() {

		profileRound.setStyle(ViewFactory.getScaleCss(1d, 1d));
		profileRound.setFill(Color.DARKGRAY);

	}

	private void initProfileLbl() {

		profileLbl.getStyleClass().addAll("gray40-text", "em20", "bold");
		profileLbl.translateXProperty().bind(profileLbl.widthProperty().multiply(-0.5));
		profileLbl.translateYProperty().bind(profileLbl.heightProperty().multiply(-0.5));
		profileLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private void initNameLbl() {

		nameLbl.getStyleClass().addAll("em16", "bold");
		nameLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private void initSettingsButton() {

		settingsButton.setOnAction(e -> listeners.forEach(listener -> listener.settingsClicked()));

	}

	private void initCommentTextField() {

		GridPane.setHgrow(commentTextField, Priority.ALWAYS);

		final AtomicReference<String> lastComment = new AtomicReference<String>();

		commentTextField.getStyleClass().addAll("comment-border");
		commentTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));
		commentTextField.setPromptText(Commons.translate("TYPE_COMMENT"));
		commentTextField.setFocusTraversable(false);
		commentTextField.setEditable(false);

		commentTextField.setOnMouseClicked(e -> {

			if (!commentEditable.get() || commentTextField.isEditable()) {
				return;
			}

			if (e.getButton() != MouseButton.PRIMARY) {
				return;
			}

			lastComment.set(commentTextField.getText());
			commentTextField.setEditable(true);

		});

		commentTextField.setOnKeyPressed(e -> {

			if (!commentEditable.get()) {
				return;
			}

			KeyCode code = e.getCode();
			if (!(code == KeyCode.ENTER || code == KeyCode.ESCAPE)) {
				return;
			}

			if (code == KeyCode.ESCAPE) {
				commentTextField.setText(lastComment.get());
			}

			e.consume();
			commentTextField.setEditable(false);
			requestFocus();

			final String comment = commentTextField.textProperty().getValueSafe().trim();

			commentTextField.setText(comment);

			listeners.forEach(listener -> listener.commentUpdateRequested(comment));

		});

	}

	private void initCoordinatesLbl() {

		coordinatesLbl.setOpacity(0.5);
		coordinatesLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private Button newStatusBtn(Color color) {

		Button btn = new Button();
		Circle circle = new Circle(0.2 * RADIUS);
		circle.setStyle(ViewFactory.getScaleCss(1d, 1d));
		circle.setFill(color);
		final Group circleGraph = new Group(circle);
		btn.setGraphic(circleGraph);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		btn.setVisible(false);

		btn.translateXProperty().bind(circle.radiusProperty().multiply(circle.scaleXProperty()).multiply(7.5));

		final Interpolator interpolator = Interpolator.EASE_BOTH;

		final Transition circleTransition = new Transition() {

			private double circleGraphStart;
			private double circleGraphEnd;
			private int position = 0;

			{
				setCycleDuration(Duration.millis(100.0));
			}

			@Override
			protected void interpolate(double arg0) {

				circleGraph.setScaleX(interpolator.interpolate(circleGraphStart, circleGraphEnd, arg0));
				circleGraph.setScaleY(interpolator.interpolate(circleGraphStart, circleGraphEnd, arg0));

			}

			@Override
			public void play() {
				circleGraphStart = 1.0 + position * 1.0;
				position = (position + 1) % 2;
				circleGraphEnd = 1.0 + position * 1.0;
				super.play();
			}

		};

		btn.setOnMouseEntered(e -> circleTransition.play());
		btn.setOnMouseExited(e -> circleTransition.play());

		return btn;

	}

}

interface IIdentityPane {

	void commentUpdateRequested(String comment);

	void statusUpdateRequested(Availability availability);

	void settingsClicked();

}

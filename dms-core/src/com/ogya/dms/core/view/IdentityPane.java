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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

class IdentityPane extends GridPane {

	private static final double GAP = ViewFactory.GAP;
	private static final double UNIT_SIZE = 30.0 * ViewFactory.VIEW_FACTOR;

	private final StackPane profilePicture = new StackPane();
	private final Circle statusCircle = new Circle(UNIT_SIZE);
	private final Circle profileRound = new Circle(UNIT_SIZE * 0.8);
	private final Label profileLbl = new Label();
	private final Button availableBtn = newStatusBtn(Availability.AVAILABLE.getStatusColor());
	private final Button awayBtn = newStatusBtn(Availability.AWAY.getStatusColor());
	private final Button busyBtn = newStatusBtn(Availability.BUSY.getStatusColor());

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

		initProfilePicture();
		initNameLbl();
		initSettingsButton();
		initCommentTextField();
		initCoordinatesLbl();

		setHgap(GAP);
		setValignment(profilePicture, VPos.TOP);
		setFillHeight(profilePicture, false);
		setHgrow(commentTextField, Priority.ALWAYS);

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
		if (!commentTextField.isEditable())
			commentTextField.setText(identity.getComment());
		coordinatesLbl.setText(identity.getLatitude() == null || identity.getLongitude() == null ? ""
				: Commons.convertDoubleToCoordinates(identity.getLatitude(), identity.getLongitude()));

	}

	void setCommentEditable(boolean editable) {

		commentEditable.set(editable);

		if (!editable)
			commentTextField.setEditable(false);

	}

	private void initProfilePicture() {

		initStatusCircle();
		initProfileRound();
		initProfileLbl();

		profilePicture.getChildren().addAll(statusCircle, profileRound, profileLbl, busyBtn, awayBtn, availableBtn);

		final Rotate awayBtnRotate = new Rotate();
		final Rotate busyBtnRotate = new Rotate();

		awayBtnRotate.pivotXProperty().bind(awayBtn.widthProperty().divide(2).subtract(awayBtn.getTranslateX()));
		awayBtnRotate.pivotYProperty().bind(awayBtn.heightProperty().divide(2).subtract(awayBtn.getTranslateY()));
		busyBtnRotate.pivotXProperty().bind(busyBtn.widthProperty().divide(2).subtract(busyBtn.getTranslateX()));
		busyBtnRotate.pivotYProperty().bind(busyBtn.heightProperty().divide(2).subtract(busyBtn.getTranslateY()));

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

			if (e.getButton() != MouseButton.PRIMARY)
				return;

			mainTransition.play();

		});

	}

	private void initStatusCircle() {

		statusCircle.setStrokeWidth(UNIT_SIZE * 0.2);
		statusCircle.setFill(Color.TRANSPARENT);
		statusCircle.strokeProperty()
				.bind(Bindings.createObjectBinding(
						() -> availabilityProperty.get() == null ? Availability.AVAILABLE.getStatusColor()
								: availabilityProperty.get().getStatusColor(),
						availabilityProperty));

	}

	private void initProfileRound() {

		profileRound.setFill(Color.DARKGRAY);

	}

	private void initProfileLbl() {

		profileLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

		profileLbl.setStyle("-fx-text-fill: #404040;");
		profileLbl.setFont(Font.font(null, FontWeight.BOLD, UNIT_SIZE));

	}

	private void initNameLbl() {

		nameLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLbl.setFont(Font.font(null, FontWeight.BOLD, UNIT_SIZE * 0.8));

	}

	private void initSettingsButton() {

		settingsButton.setOnAction(e -> listeners.forEach(listener -> listener.settingsClicked()));

	}

	private void initCommentTextField() {

		final AtomicReference<String> lastComment = new AtomicReference<String>();

		commentTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

		commentTextField.setPromptText(Commons.translate("TYPE_COMMENT"));
		commentTextField.setFocusTraversable(false);
		commentTextField.setEditable(false);

		commentTextField.setOnMouseClicked(e -> {

			if (!commentEditable.get() || commentTextField.isEditable())
				return;

			if (e.getButton() != MouseButton.PRIMARY)
				return;

			lastComment.set(commentTextField.getText());
			commentTextField.setEditable(true);

		});

		commentTextField.setOnKeyPressed(e -> {

			if (!commentEditable.get())
				return;

			KeyCode code = e.getCode();
			if (!(code == KeyCode.ENTER || code == KeyCode.ESCAPE))
				return;

			if (code == KeyCode.ESCAPE)
				commentTextField.setText(lastComment.get());

			e.consume();
			commentTextField.setEditable(false);
			requestFocus();

			final String comment = commentTextField.textProperty().getValueSafe().trim();

			commentTextField.setText(comment);

			listeners.forEach(listener -> listener.commentUpdateRequested(comment));

		});

		commentTextField.setBorder(new Border(new BorderStroke[] { new BorderStroke(Color.LIGHTGRAY,
				BorderStrokeStyle.SOLID, new CornerRadii(UNIT_SIZE * 15.0), BorderWidths.DEFAULT) }));

	}

	private void initCoordinatesLbl() {

		coordinatesLbl.setOpacity(0.5);
		coordinatesLbl.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private Button newStatusBtn(Color color) {

		final Button btn = new Button();
		final Circle circle = new Circle(UNIT_SIZE * 0.2);
		circle.setFill(color);
		btn.setGraphic(circle);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		btn.setVisible(false);

		btn.setTranslateX(UNIT_SIZE * 1.5);

		final Interpolator interpolator = Interpolator.EASE_BOTH;

		final Transition circleTransition = new Transition() {

			private double circleStart;
			private double circleEnd;
			private int position = 0;

			{
				setCycleDuration(Duration.millis(100.0));
			}

			@Override
			protected void interpolate(double arg0) {

				circle.setScaleX(interpolator.interpolate(circleStart, circleEnd, arg0));
				circle.setScaleY(interpolator.interpolate(circleStart, circleEnd, arg0));

			}

			@Override
			public void play() {
				circleStart = 1.0 + position * 1.0;
				position = (position + 1) % 2;
				circleEnd = 1.0 + position * 1.0;
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

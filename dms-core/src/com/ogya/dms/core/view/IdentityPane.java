package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.structures.Availability;
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
import javafx.scene.layout.Background;
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

	private final double unitSize = 30.0 * ViewFactory.getViewFactor();

	private final StackPane profilePicture = new StackPane();
	private final Circle statusCircle = new Circle(unitSize);
	private final Circle profileRound = new Circle(unitSize * 0.8);
	private final Label profileLabel = new Label();
	private final Button availableBtn = newStatusBtn(Availability.AVAILABLE.getStatusColor());
	private final Button awayBtn = newStatusBtn(Availability.AWAY.getStatusColor());
	private final Button busyBtn = newStatusBtn(Availability.BUSY.getStatusColor());

	private final Label nameLabel = new Label();
	private final Button settingsButton = ViewFactory.newSettingsBtn();
	private final TextField commentTextField = new TextField();
	private final Label coordinatesLabel = new Label();

	private final List<IIdentityPane> listeners = Collections.synchronizedList(new ArrayList<IIdentityPane>());

	private final AtomicBoolean commentEditable = new AtomicBoolean(true);
	private final ObjectProperty<Availability> availabilityProperty = new SimpleObjectProperty<Availability>();

	IdentityPane() {

		super();

		init();

	}

	private void init() {

		initProfilePicture();
		initNameLabel();
		initSettingsButton();
		initCommentTextField();
		initCoordinatesLabel();

		setHgap(GAP);
		setValignment(profilePicture, VPos.TOP);
		setFillHeight(profilePicture, false);
		setHgrow(commentTextField, Priority.ALWAYS);

		add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		add(nameLabel, 2, 0, 1, 1);
		add(settingsButton, 3, 0, 1, 1);
		add(commentTextField, 2, 1, 1, 1);
		add(coordinatesLabel, 2, 2, 1, 1);
		add(profilePicture, 0, 0, 1, 3);

	}

	void addListener(IIdentityPane listener) {

		listeners.add(listener);

	}

	void setIdentity(Contact identity) {

		availabilityProperty.set(identity.getStatus());
		profileLabel.setText(identity.getName().substring(0, 1).toUpperCase());

		nameLabel.setText(identity.getName());
		if (!commentTextField.isEditable())
			commentTextField.setText(identity.getComment());
		coordinatesLabel.setText(identity.getLattitude() == null || identity.getLongitude() == null ? ""
				: String.format("(%.2f : %.2f)", identity.getLattitude(), identity.getLongitude()));

	}

	void setCommentEditable(boolean editable) {

		commentEditable.set(editable);

		if (!editable)
			commentTextField.setEditable(false);

	}

	private void initProfilePicture() {

		initStatusCircle();
		initProfileRound();
		initProfileLabel();

		profilePicture.getChildren().addAll(statusCircle, profileRound, profileLabel, busyBtn, awayBtn, availableBtn);

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

			if (!Objects.equals(e.getButton(), MouseButton.PRIMARY))
				return;

			mainTransition.play();

		});

	}

	private void initStatusCircle() {

		statusCircle.setStrokeWidth(unitSize * 0.2);
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

	private void initProfileLabel() {

		profileLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		profileLabel.setFont(Font.font(null, FontWeight.BOLD, unitSize));

	}

	private void initNameLabel() {

		nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

		nameLabel.setFont(Font.font(null, FontWeight.BOLD, unitSize * 0.8));

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

			listeners.forEach(listener -> listener.commentUpdateRequested(comment));

		});

		commentTextField.setBorder(new Border(new BorderStroke[] { new BorderStroke(Color.LIGHTGRAY,
				BorderStrokeStyle.SOLID, new CornerRadii(unitSize * 15.0), BorderWidths.DEFAULT) }));

	}

	private void initCoordinatesLabel() {

		coordinatesLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

	}

	private Button newStatusBtn(Color color) {

		final Button btn = new Button();
		final Circle circle = new Circle(unitSize * 0.2);
		circle.setFill(color);
		btn.setGraphic(circle);
		btn.setBackground(Background.EMPTY);
		btn.setPadding(Insets.EMPTY);
		btn.setPickOnBounds(false);
		btn.setVisible(false);

		btn.setTranslateX(unitSize * 1.5);

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

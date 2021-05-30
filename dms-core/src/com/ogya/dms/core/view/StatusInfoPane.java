package com.ogya.dms.core.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class StatusInfoPane extends BorderPane {

	private static final double GAP = ViewFactory.GAP;
	private static final double VIEW_FACTOR = ViewFactory.VIEW_FACTOR;

	private final HBox topPane = new HBox(2 * GAP);
	private final VBox centerPane = new VBox(2 * GAP);

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button backBtn;

	private final Map<Long, Card> cards = Collections.synchronizedMap(new HashMap<Long, Card>());

	StatusInfoPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty);

		init();

	}

	private void init() {

		topPane.setPadding(new Insets(GAP));
		centerPane.setPadding(new Insets(2 * GAP, 4 * GAP, 2 * GAP, 2 * GAP));

		topPane.setAlignment(Pos.CENTER_LEFT);

		scrollPane.getStyleClass().add("edge-to-edge");
		scrollPane.setFitToWidth(true);

		topPane.getChildren().add(backBtn);

		setTop(topPane);
		setCenter(scrollPane);

	}

	void setOnBackAction(Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void addCards(List<Contact> contacts) {

		contacts.forEach(contact -> {

			Card card = new Card();

			cards.put(contact.getId(), card);

			updateContact(contact);

			centerPane.getChildren().add(card);

		});

	}

	void reset() {

		cards.forEach((uuid, card) -> centerPane.getChildren().remove(card));

		cards.clear();

	}

	void updateContact(Contact contact) {

		Card card = cards.get(contact.getId());

		if (card == null)
			return;

		card.setStatusColor(contact.getStatus().getStatusColor());
		card.setName(contact.getName());

	}

	void updateMessageStatus(Long contactId, MessageStatus messageStatus) {

		Card card = cards.get(contactId);

		if (card == null)
			return;

		card.updateMessageStatus(messageStatus);

	}

	void updateMessageProgress(Long contactId, int progress) {

		Card card = cards.get(contactId);

		if (card == null)
			return;

		card.setProgress(progress);

	}

	private final class Card extends HBox {

		private final double radius = 3.0 * VIEW_FACTOR;

		private final Circle statusCircle = new Circle(7.0 * VIEW_FACTOR);
		private final Label nameLbl = new Label();
		private final Label progressLbl = new Label();
		private final Group infoGrp = new Group();
		private final Circle waitingCircle = new Circle(radius, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(radius, Color.TRANSPARENT);

		private Card() {

			super(GAP);

			init();

		}

		private void init() {

			setAlignment(Pos.CENTER);

			initNameLbl();
			initProgressLbl();
			initInfoGrp();

			getChildren().addAll(nameLbl, progressLbl, infoGrp);

		}

		void setStatusColor(Paint fill) {

			statusCircle.setFill(fill);

		}

		void setName(String name) {

			nameLbl.setText(name);

		}

		void updateMessageStatus(MessageStatus messageStatus) {

			if (Objects.equals(messageStatus, MessageStatus.FRESH))
				setProgress(-1);

			infoGrp.setVisible(!Objects.equals(messageStatus, MessageStatus.FRESH));

			waitingCircle.setFill(messageStatus.getWaitingColor());
			transmittedCircle.setFill(messageStatus.getTransmittedColor());

		}

		void setProgress(int progress) {

			progressLbl.setText(progress < 0 ? "" : String.format("%d%%", progress));

		}

		private void initNameLbl() {

			HBox.setHgrow(nameLbl, Priority.ALWAYS);
			nameLbl.setMaxWidth(Double.MAX_VALUE);
			nameLbl.setGraphic(statusCircle);
			nameLbl.setGraphicTextGap(2 * GAP);
			nameLbl.setFont(Font.font(null, FontWeight.BOLD, 18.0 * VIEW_FACTOR));

		}

		private void initProgressLbl() {

			progressLbl.setAlignment(Pos.BASELINE_RIGHT);
			progressLbl.setFont(Font.font(11.25 * VIEW_FACTOR));
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.visibleProperty().bind(infoGrp.visibleProperty().not());
			progressLbl.managedProperty().bind(progressLbl.visibleProperty());

		}

		private void initInfoGrp() {

			infoGrp.managedProperty().bind(infoGrp.visibleProperty());

			transmittedCircle.setLayoutX(-2.0 * radius);
			infoGrp.getChildren().addAll(waitingCircle, transmittedCircle);

		}

	}

}

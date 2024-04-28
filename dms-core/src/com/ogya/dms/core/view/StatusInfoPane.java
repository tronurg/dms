package com.ogya.dms.core.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.view.component.DmsScrollPane;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
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

public class StatusInfoPane extends BorderPane {

	private final HBox topPane = new HBox();
	private final VBox centerPane = new VBox();

	private final ScrollPane scrollPane = new DmsScrollPane(centerPane);
	private final Button backBtn;

	private final Map<Long, Card> cards = Collections.synchronizedMap(new HashMap<Long, Card>());

	StatusInfoPane(BooleanProperty unreadProperty) {
		super();
		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);
		init();
	}

	private void init() {

		initTopPane();
		initScrollPane();

		setTop(topPane);
		setCenter(scrollPane);

	}

	private void initTopPane() {
		topPane.getStyleClass().addAll("top-pane", "transparent-bg");
		topPane.getChildren().add(backBtn);
	}

	private void initScrollPane() {
		scrollPane.getStyleClass().addAll("edge-to-edge");
		scrollPane.setFitToWidth(true);
		centerPane.getStyleClass().addAll("spacing-2", "padding-2422");
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

		if (card == null) {
			return;
		}

		card.setStatusColor(contact.getStatus().getStatusColor());
		card.setName(contact.getName());

	}

	void updateMessageStatus(Long contactId, MessageStatus messageStatus) {

		Card card = cards.get(contactId);

		if (card == null) {
			return;
		}

		card.updateMessageStatus(messageStatus);

	}

	void updateMessageProgress(Long contactId, int progress) {

		Card card = cards.get(contactId);

		if (card == null) {
			return;
		}

		card.setProgress(progress);

	}

	private final class Card extends HBox {

		private final Circle statusCircle = new Circle(7.0);
		private final Group statusCircleGraph = new Group(statusCircle);
		private final Label nameLbl = new Label();
		private final Label progressLbl = new Label();
		private final Circle waitingCircle = new Circle(3.0, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(3.0, Color.TRANSPARENT);
		private final Group infoGrp = new Group();

		private Card() {
			super();
			init();
		}

		private void init() {

			getStyleClass().addAll("spacing-2");
			setAlignment(Pos.CENTER);

			initStatusCircle();
			initNameLbl();
			initProgressLbl();
			initInfoGrp();

			getChildren().addAll(statusCircleGraph, nameLbl, progressLbl, infoGrp);

		}

		void setStatusColor(Paint fill) {

			statusCircle.setFill(fill);

		}

		void setName(String name) {

			nameLbl.setText(name);

		}

		void updateMessageStatus(MessageStatus messageStatus) {

			if (messageStatus == MessageStatus.FRESH) {
				setProgress(-1);
			}

			infoGrp.setVisible(messageStatus != MessageStatus.FRESH);

			waitingCircle.setFill(messageStatus.getWaitingColor());
			transmittedCircle.setFill(messageStatus.getTransmittedColor());

		}

		void setProgress(int progress) {

			progressLbl.setText(progress > 0 ? String.format("%d%%", progress) : "");

		}

		private void initStatusCircle() {

			statusCircle.setStyle(ViewFactory.getScaleCss(1d, 1d));

		}

		private void initNameLbl() {

			nameLbl.getStyleClass().addAll("em12", "bold");
			HBox.setHgrow(nameLbl, Priority.ALWAYS);
			nameLbl.setMaxWidth(Double.MAX_VALUE);

		}

		private void initProgressLbl() {

			progressLbl.getStyleClass().addAll("em08");
			progressLbl.setAlignment(Pos.BASELINE_RIGHT);
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.visibleProperty().bind(infoGrp.visibleProperty().not());
			progressLbl.managedProperty().bind(progressLbl.visibleProperty());

		}

		private void initInfoGrp() {

			infoGrp.managedProperty().bind(infoGrp.visibleProperty());
			transmittedCircle.setLayoutX(-2.0 * transmittedCircle.getRadius());

			Group group = new Group(waitingCircle, transmittedCircle);
			group.setStyle(ViewFactory.getScaleCss(1d, 1d));

			infoGrp.getChildren().add(group);

		}

	}

}

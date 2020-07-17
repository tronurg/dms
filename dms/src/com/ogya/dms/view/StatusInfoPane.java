package com.ogya.dms.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.structures.MessageStatus;
import com.ogya.dms.view.factory.ViewFactory;
import com.sun.javafx.tk.Toolkit;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

public class StatusInfoPane extends BorderPane {

	private static final double GAP = 5.0;

	private final HBox topPane = new HBox(GAP);
	private final VBox centerPane = new VBox(2 * GAP);

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button backBtn = ViewFactory.newBackBtn();

	private final Map<String, Card> cards = Collections.synchronizedMap(new HashMap<String, Card>());

	StatusInfoPane() {

		super();

		init();

	}

	private void init() {

		topPane.setPadding(new Insets(GAP));
		centerPane.setPadding(new Insets(GAP));

		topPane.setAlignment(Pos.CENTER_LEFT);

		scrollPane.setFitToWidth(true);

		topPane.getChildren().addAll(backBtn);

		setTop(topPane);
		setCenter(scrollPane);

	}

	void setOnBackAction(Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void addCards(List<Contact> contacts) {

		contacts.forEach(contact -> {

			Card card = new Card();

			cards.put(contact.getUuid(), card);

			updateContact(contact);

		});

	}

	void reset() {

		cards.forEach((uuid, card) -> centerPane.getChildren().remove(card));

		cards.clear();

	}

	void updateContact(Contact contact) {

		Card card = cards.get(contact.getUuid());

		if (card == null)
			return;

		card.setStatusColor(contact.getStatus().getStatusColor());
		card.setName(contact.getName());

	}

	void updateMessageStatus(String uuid, MessageStatus messageStatus) {

		Card card = cards.get(uuid);

		if (card == null)
			return;

		card.updateMessageStatus(messageStatus);

	}

	void updateMessageProgress(String uuid, int progress) {

		Card card = cards.get(uuid);

		if (card == null)
			return;

		card.setProgress(progress);

	}

	private static class Card extends GridPane {

		private static final double RADIUS = 3.0;

		private final Circle statusCircle = new Circle(7.0);
		private final Label nameLabel = new Label();
		private final Label progressLbl = new Label();
		private final Group infoGrp = new Group();
		private final Circle waitingCircle = new Circle(RADIUS, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(RADIUS, Color.TRANSPARENT);

		private final AtomicReference<MessageStatus> messageStatusRef = new AtomicReference<MessageStatus>();

		Card() {

			super();

			init();

		}

		private void init() {

			GridPane.setHgrow(nameLabel, Priority.ALWAYS);

			initProgressLbl();
			initInfoGrp();

			add(statusCircle, 0, 0, 1, 1);
			add(nameLabel, 1, 0, 1, 1);
			add(progressLbl, 2, 0, 1, 1);
			add(infoGrp, 2, 0, 1, 1);

		}

		void setStatusColor(Paint fill) {

			statusCircle.setFill(fill);

		}

		void setName(String name) {

			nameLabel.setText(name);

		}

		void updateMessageStatus(MessageStatus messageStatus) {

			if (messageStatus.equals(messageStatusRef.getAndSet(messageStatus)))
				return;

			progressLbl.setVisible(false);

			waitingCircle.setFill(messageStatus.getWaitingColor());
			transmittedCircle.setFill(messageStatus.getTransmittedColor());

		}

		void setProgress(int progress) {

			progressLbl.setText(String.format("%d%%", progress));

			progressLbl.setVisible(!(progress < 0));

		}

		private void initProgressLbl() {

			progressLbl.setFont(Font.font(nameLabel.getFont().getSize() * 0.75));
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.setMinWidth(
					Toolkit.getToolkit().getFontLoader().computeStringWidth("100%", progressLbl.getFont()));

		}

		private void initInfoGrp() {

			infoGrp.visibleProperty().bind(progressLbl.visibleProperty().not());

			transmittedCircle.setLayoutX(2 * RADIUS);
			infoGrp.getChildren().addAll(waitingCircle, transmittedCircle);

		}

	}

}

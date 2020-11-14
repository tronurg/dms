package com.ogya.dms.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import javafx.scene.text.FontWeight;

public class StatusInfoPane extends BorderPane {

	private static final double GAP = ViewFactory.GAP;

	private final double viewFactor = ViewFactory.getViewFactor();

	private final HBox topPane = new HBox(GAP);
	private final VBox centerPane = new VBox(2 * GAP);

	private final ScrollPane scrollPane = new ScrollPane(centerPane) {
		@Override
		public void requestFocus() {
		}
	};
	private final Button backBtn = ViewFactory.newBackBtn();

	private final Map<Long, Card> cards = Collections.synchronizedMap(new HashMap<Long, Card>());

	StatusInfoPane() {

		super();

		init();

	}

	private void init() {

		topPane.setPadding(new Insets(GAP));
		centerPane.setPadding(new Insets(2 * GAP));

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

	void updateMessageStatus(Long id, MessageStatus messageStatus) {

		Card card = cards.get(id);

		if (card == null)
			return;

		card.updateMessageStatus(messageStatus);

	}

	void updateMessageProgress(Long id, int progress) {

		Card card = cards.get(id);

		if (card == null)
			return;

		card.setProgress(progress);

	}

	private final class Card extends GridPane {

		private final double radius = 3.0 * viewFactor;

		private final Circle statusCircle = new Circle(7.0 * viewFactor);
		private final Label nameLabel = new Label();
		private final Label progressLbl = new Label();
		private final Group infoGrp = new Group();
		private final Circle waitingCircle = new Circle(radius, Color.TRANSPARENT);
		private final Circle transmittedCircle = new Circle(radius, Color.TRANSPARENT);

		private Card() {

			super();

			init();

		}

		private void init() {

			setHgap(GAP);

			GridPane.setHgrow(nameLabel, Priority.ALWAYS);

			initNameLabel();
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

			if (Objects.equals(messageStatus, MessageStatus.FRESH))
				setProgress(-1);

			infoGrp.setVisible(!Objects.equals(messageStatus, MessageStatus.FRESH));

			waitingCircle.setFill(messageStatus.getWaitingColor());
			transmittedCircle.setFill(messageStatus.getTransmittedColor());

		}

		void setProgress(int progress) {

			progressLbl.setText(progress < 0 ? "" : String.format("%d%%", progress));

		}

		private void initNameLabel() {

			nameLabel.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));

		}

		private void initProgressLbl() {

			progressLbl.visibleProperty().bind(infoGrp.visibleProperty().not());

			progressLbl.setFont(Font.font(11.25 * viewFactor));
			progressLbl.setTextFill(Color.DIMGRAY);

			progressLbl.setMinWidth(
					Toolkit.getToolkit().getFontLoader().computeStringWidth("100%", progressLbl.getFont()));

		}

		private void initInfoGrp() {

			transmittedCircle.setLayoutX(2 * radius);
			infoGrp.getChildren().addAll(waitingCircle, transmittedCircle);

		}

	}

}

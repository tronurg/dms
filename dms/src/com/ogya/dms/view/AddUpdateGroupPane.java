package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AddUpdateGroupPane extends BorderPane {

	private static final double GAP = 5.0;

	private final HBox topPane = new HBox(GAP);

	private final Button backBtn = ViewFactory.newBackBtn();
	private final TextField groupNameTextField = new TextField();

	private final VBox scrollableContent = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(scrollableContent) {
		@Override
		public void requestFocus() {
		}
	};
	private final VBox addedContactsPane = new VBox();
	private final VBox notAddedContactsPane = new VBox();
	private final TextField searchContactTextField = new TextField();

	private final Button addUpdateGroupBtn = new Button();

	private final List<String> uuids = Collections.synchronizedList(new ArrayList<String>());
	private final ObservableSet<String> selectedUuids = FXCollections.observableSet(new HashSet<String>());

	private final Comparator<Node> contactsSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof Labeled && arg1 instanceof Labeled))
				return 0;

			Labeled labeled0 = (Labeled) arg0;
			Labeled labeled1 = (Labeled) arg1;

			return labeled0.getText().compareTo(labeled1.getText());

		}

	};

	AddUpdateGroupPane() {

		super();

		init();

	}

	void setOnBackAction(final Runnable runnable) {

		backBtn.setOnAction(e -> runnable.run());

	}

	void setOnAddUpdateGroupAction(final Runnable runnable) {

		addUpdateGroupBtn.setOnAction(e -> runnable.run());

	}

	void updateContact(Contact contact) {

		final String uuid = contact.getUuid();
		final String name = contact.getName();

		if (!uuids.contains(uuid)) {

			uuids.add(uuid);

			final Button addContactBtn = ViewFactory.newAddBtn();
			initButton(addContactBtn);
			addContactBtn.setText(name);

			final Button removeContactBtn = ViewFactory.newRemoveBtn();
			initButton(removeContactBtn);
			removeContactBtn.setText(name);

			BooleanBinding addContactBinding = Bindings.createBooleanBinding(() -> selectedUuids.contains(uuid),
					selectedUuids);
			BooleanBinding searchContactBinding = Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchContactTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || name.toLowerCase().startsWith(searchContactStr);
			}, searchContactTextField.textProperty());

			addContactBtn.visibleProperty().bind(searchContactBinding.and(addContactBinding.not()));
			removeContactBtn.visibleProperty().bind(addContactBinding);

			addContactBtn.setOnAction(e -> {

				addedContactsPane.getChildren().remove(removeContactBtn);
				addedContactsPane.getChildren().add(0, removeContactBtn);

				selectedUuids.add(uuid);

			});

			removeContactBtn.setOnAction(e -> selectedUuids.remove(uuid));

			addedContactsPane.getChildren().add(removeContactBtn);
			notAddedContactsPane.getChildren().add(addContactBtn);
			FXCollections.sort(notAddedContactsPane.getChildren(), contactsSorter);

		}

	}

	String getGroupName() {

		return groupNameTextField.getText().trim();

	}

	Set<String> getSelectedUuids() {

		return new LinkedHashSet<String>(selectedUuids);

	}

	void resetContent(String groupName, Set<String> newSelectedUuids, boolean isNewGroup) {

		searchContactTextField.setText("");
		groupNameTextField.setText(groupName == null ? "" : groupName);
		selectedUuids.clear();
		if (newSelectedUuids != null) {
			newSelectedUuids.forEach(uuid -> {
				if (uuids.contains(uuid))
					selectedUuids.add(uuid);
			});
		}
		addUpdateGroupBtn.setText(
				isNewGroup ? CommonMethods.translate("CREATE_GROUP") : CommonMethods.translate("UPDATE_GROUP"));

	}

	private void initButton(Button btn) {

		btn.setMnemonicParsing(false);
		btn.setFont(Font.font(null, FontWeight.BOLD, 18.0));
		btn.setPadding(new Insets(5.0));
		btn.managedProperty().bind(btn.visibleProperty());

	}

	private void init() {

		initTopPane();
		initScrollableContent();
		initAddUpdateGroupBtn();

		scrollPane.setFitToWidth(true);

		StackPane bottomPane = new StackPane();
		Pane emptyPane = new Pane();
		emptyPane.setOpacity(1.0);
		emptyPane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
		bottomPane.getChildren().addAll(emptyPane, addUpdateGroupBtn);

		setTop(topPane);
		setCenter(scrollPane);
		setBottom(bottomPane);

	}

	private void initTopPane() {

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(GAP));
		topPane.setAlignment(Pos.CENTER_LEFT);

		initGroupNameTextField();

		topPane.getChildren().addAll(backBtn, groupNameTextField);

	}

	private void initGroupNameTextField() {

		groupNameTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

		groupNameTextField.setPromptText(CommonMethods.translate("TYPE_GROUP_NAME"));
		groupNameTextField.setFocusTraversable(false);
		groupNameTextField.setBackground(Background.EMPTY);
		groupNameTextField.setFont(Font.font(null, FontWeight.BOLD, 18.0));

	}

	private void initScrollableContent() {

		initAddedContactsPane();
		initNotAddedContactsPane();
		initSearchContactTextField();

		BorderPane notAddedContactsBorderPane = new BorderPane();
		notAddedContactsBorderPane.setPadding(Insets.EMPTY);
		notAddedContactsBorderPane.setTop(searchContactTextField);
		notAddedContactsBorderPane.setCenter(notAddedContactsPane);

		TitledPane addedContactsTitledPane = new TitledPane(CommonMethods.translate("ADDED_CONTACTS"),
				addedContactsPane);
		TitledPane notAddedContactsTitledPane = new TitledPane(CommonMethods.translate("ALL_CONTACTS"),
				notAddedContactsBorderPane);

		addedContactsTitledPane.setCollapsible(false);
		notAddedContactsTitledPane.setCollapsible(false);

		scrollableContent.getChildren().addAll(addedContactsTitledPane, notAddedContactsTitledPane);

	}

	private void initAddedContactsPane() {

		addedContactsPane.setPadding(Insets.EMPTY);

	}

	private void initNotAddedContactsPane() {

		notAddedContactsPane.setPadding(Insets.EMPTY);

	}

	private void initSearchContactTextField() {

		searchContactTextField.setPromptText(CommonMethods.translate("FIND"));
		searchContactTextField.setFocusTraversable(false);

	}

	private void initAddUpdateGroupBtn() {

		addUpdateGroupBtn
				.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
		addUpdateGroupBtn.setTextFill(Color.ANTIQUEWHITE);
		addUpdateGroupBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0));

		addUpdateGroupBtn.setMnemonicParsing(false);

		addUpdateGroupBtn.setMaxWidth(Double.MAX_VALUE);

		addUpdateGroupBtn.disableProperty()
				.bind(Bindings.size(selectedUuids).isEqualTo(0).or(groupNameTextField.textProperty().isEmpty()));

	}

}

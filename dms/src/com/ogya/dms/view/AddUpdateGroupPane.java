package com.ogya.dms.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AddUpdateGroupPane extends BorderPane {

	private static final double GAP = 5.0;

	private final HBox topPane = new HBox(GAP);

	private final Button backBtn = ViewFactory.newBackBtn();
	private final TextField groupNameTextField = new TextField();
	private final Button deleteBtn = ViewFactory.newDeleteBtn();

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

	private final Map<String, ObjectProperty<Color>> uuidStatus = Collections
			.synchronizedMap(new HashMap<String, ObjectProperty<Color>>());
	private final ObservableSet<String> selectedUuids = FXCollections.observableSet(new HashSet<String>());

	private final BooleanProperty updateMode = new SimpleBooleanProperty();
	private final BooleanProperty deleteMode = new SimpleBooleanProperty();

	private final AtomicReference<Runnable> addUpdateGroupActionRef = new AtomicReference<Runnable>();
	private final AtomicReference<Runnable> deleteGroupActionRef = new AtomicReference<Runnable>();

	private final Comparator<Node> contactsSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof HBox && arg1 instanceof HBox))
				return 0;

			HBox hBox0 = (HBox) arg0;
			HBox hBox1 = (HBox) arg1;

			if (!(hBox0.getChildren().size() > 0 && hBox0.getChildren().get(0) instanceof Labeled
					&& hBox1.getChildren().size() > 0 && hBox1.getChildren().get(0) instanceof Labeled))
				return 0;

			Labeled labeled0 = (Labeled) hBox0.getChildren().get(0);
			Labeled labeled1 = (Labeled) hBox1.getChildren().get(0);

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

		addUpdateGroupActionRef.set(runnable);

	}

	void setOnDeleteGroupAction(final Runnable runnable) {

		deleteGroupActionRef.set(runnable);

	}

	void updateContact(Contact contact) {

		final String uuid = contact.getUuid();
		final String name = contact.getName();
		final Color statusColor = contact.getStatus().getStatusColor();

		if (!uuidStatus.containsKey(uuid)) {

			uuidStatus.put(uuid, new SimpleObjectProperty<Color>(statusColor));

			final HBox addContactBox = new HBox();
			addContactBox.setAlignment(Pos.CENTER_LEFT);
			addContactBox.managedProperty().bind(addContactBox.visibleProperty());
			final Button addContactBtn = ViewFactory.newAddBtn();
			initButton(addContactBtn);
			addContactBtn.setText(name);
			final Circle addStatusCircle = new Circle(7.0);
			HBox.setMargin(addStatusCircle, new Insets(GAP, 3 * GAP, GAP, GAP));
			addStatusCircle.fillProperty().bind(uuidStatus.get(uuid));
			addContactBox.getChildren().addAll(addContactBtn, addStatusCircle);

			final HBox removeContactBox = new HBox();
			removeContactBox.setAlignment(Pos.CENTER_LEFT);
			removeContactBox.managedProperty().bind(removeContactBox.visibleProperty());
			final Button removeContactBtn = ViewFactory.newRemoveBtn();
			initButton(removeContactBtn);
			removeContactBtn.setText(name);
			final Circle removeStatusCircle = new Circle(7.0);
			HBox.setMargin(removeStatusCircle, new Insets(GAP, 3 * GAP, GAP, GAP));
			removeStatusCircle.fillProperty().bind(uuidStatus.get(uuid));
			removeContactBox.getChildren().addAll(removeContactBtn, removeStatusCircle);

			BooleanBinding addContactBinding = Bindings.createBooleanBinding(() -> selectedUuids.contains(uuid),
					selectedUuids);
			BooleanBinding searchContactBinding = Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchContactTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || name.toLowerCase().startsWith(searchContactStr);
			}, searchContactTextField.textProperty());

			addContactBox.visibleProperty().bind(searchContactBinding.and(addContactBinding.not()));
			removeContactBox.visibleProperty().bind(addContactBinding);

			addContactBtn.setOnAction(e -> {

				addedContactsPane.getChildren().remove(removeContactBox);
				addedContactsPane.getChildren().add(0, removeContactBox);

				selectedUuids.add(uuid);

			});

			removeContactBtn.setOnAction(e -> selectedUuids.remove(uuid));

			addedContactsPane.getChildren().add(removeContactBox);
			notAddedContactsPane.getChildren().add(addContactBox);
			FXCollections.sort(notAddedContactsPane.getChildren(), contactsSorter);

		}

		uuidStatus.get(uuid).setValue(statusColor);

	}

	String getGroupName() {

		return groupNameTextField.getText().trim();

	}

	Set<String> getSelectedUuids() {

		return new HashSet<String>(selectedUuids);

	}

	void resetContent(String groupName, Set<String> newSelectedUuids, boolean isNewGroup) {

		searchContactTextField.setText("");
		groupNameTextField.setText(groupName == null ? "" : groupName);
		selectedUuids.clear();
		if (newSelectedUuids != null) {
			newSelectedUuids.forEach(uuid -> {
				if (uuidStatus.containsKey(uuid))
					selectedUuids.add(uuid);
			});
		}

		deleteMode.set(false);
		updateMode.set(!isNewGroup);

	}

	private void initButton(Button btn) {

		HBox.setHgrow(btn, Priority.ALWAYS);
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setAlignment(Pos.CENTER_LEFT);
		btn.setMnemonicParsing(false);
		btn.setFont(Font.font(null, FontWeight.BOLD, 18.0));
		btn.setPadding(new Insets(5.0));

	}

	private void init() {

		initTopPane();
		initScrollableContent();
		initAddUpdateGroupBtn();
		initDeleteBtn();

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

		topPane.getChildren().addAll(backBtn, groupNameTextField, deleteBtn);

	}

	private void initGroupNameTextField() {

		HBox.setHgrow(groupNameTextField, Priority.ALWAYS);

		groupNameTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

		groupNameTextField.setPromptText(CommonMethods.translate("TYPE_GROUP_NAME"));
		groupNameTextField.setFocusTraversable(false);
		groupNameTextField.setBackground(Background.EMPTY);
		groupNameTextField.setFont(Font.font(null, FontWeight.BOLD, 18.0));

	}

	private void initDeleteBtn() {

		DropShadow shadow = new DropShadow();

		deleteBtn.effectProperty()
				.bind(Bindings.createObjectBinding(() -> deleteMode.get() ? shadow : null, deleteMode));

		deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());
		deleteBtn.visibleProperty().bind(updateMode);

		deleteBtn.setOnAction(e -> deleteMode.set(!deleteMode.get()));

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

		final Background deleteBackgroud = new Background(
				new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY));
		final Background nonDeleteBackgroud = new Background(
				new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY));

		addUpdateGroupBtn.backgroundProperty().bind(Bindings
				.createObjectBinding(() -> deleteMode.get() ? deleteBackgroud : nonDeleteBackgroud, deleteMode));
		addUpdateGroupBtn.setTextFill(Color.ANTIQUEWHITE);

		addUpdateGroupBtn.textProperty().bind(Bindings.createStringBinding(() -> {

			if (updateMode.get())
				return deleteMode.get() ? CommonMethods.translate("DELETE_GROUP")
						: CommonMethods.translate("UPDATE_GROUP");
			return CommonMethods.translate("CREATE_GROUP");

		}, updateMode, deleteMode));

		addUpdateGroupBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0));

		addUpdateGroupBtn.setMnemonicParsing(false);

		addUpdateGroupBtn.setMaxWidth(Double.MAX_VALUE);

		addUpdateGroupBtn.disableProperty().bind(deleteMode.not()
				.and(Bindings.size(selectedUuids).isEqualTo(0).or(groupNameTextField.textProperty().isEmpty())));

		addUpdateGroupBtn.setOnAction(e -> {

			if (deleteMode.get()) {

				Runnable deleteGroupAction = deleteGroupActionRef.get();

				if (deleteGroupAction != null)
					deleteGroupAction.run();

			} else {

				Runnable addUpdateGroupAction = addUpdateGroupActionRef.get();

				if (addUpdateGroupAction != null)
					addUpdateGroupAction.run();

			}

		});

	}

}

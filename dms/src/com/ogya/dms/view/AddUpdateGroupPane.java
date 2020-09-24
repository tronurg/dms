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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import javafx.scene.paint.Paint;
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

			if (!(arg0 instanceof AddRemoveContactBox && arg1 instanceof AddRemoveContactBox))
				return 0;

			AddRemoveContactBox box0 = (AddRemoveContactBox) arg0;
			AddRemoveContactBox box1 = (AddRemoveContactBox) arg1;

			return box0.getName().toLowerCase().compareTo(box1.getName().toLowerCase());

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

			final AddContactBox addContactBox = new AddContactBox(name);
			final RemoveContactBox removeContactBox = new RemoveContactBox(name);

			addContactBox.statusColorProperty().bind(uuidStatus.get(uuid));
			removeContactBox.statusColorProperty().bind(uuidStatus.get(uuid));

			BooleanBinding addContactBinding = Bindings.createBooleanBinding(() -> selectedUuids.contains(uuid),
					selectedUuids);
			BooleanBinding searchContactBinding = Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchContactTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || name.toLowerCase().startsWith(searchContactStr);
			}, searchContactTextField.textProperty());

			addContactBox.visibleProperty().bind(searchContactBinding.and(addContactBinding.not()));
			removeContactBox.visibleProperty().bind(addContactBinding);

			addContactBox.setOnAction(e -> {

				addedContactsPane.getChildren().remove(removeContactBox);
				addedContactsPane.getChildren().add(0, removeContactBox);

				selectedUuids.add(uuid);

			});

			removeContactBox.setOnAction(e -> selectedUuids.remove(uuid));

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

	private static abstract class AddRemoveContactBox extends HBox {

		protected final String name;

		protected Button addRemoveBtn;
		private final Circle addRemoveStatusCircle = new Circle(7.0);

		AddRemoveContactBox(String name) {

			this.name = name;

			init();

		}

		private void init() {

			initButton();
			initCircle();

			setAlignment(Pos.CENTER_LEFT);
			managedProperty().bind(visibleProperty());

			getChildren().addAll(addRemoveBtn, addRemoveStatusCircle);

		}

		protected abstract void initButton();

		private void initCircle() {

			HBox.setMargin(addRemoveStatusCircle, new Insets(GAP, 3 * GAP, GAP, GAP));

		}

		ObjectProperty<Paint> statusColorProperty() {

			return addRemoveStatusCircle.fillProperty();

		}

		void setOnAction(EventHandler<ActionEvent> arg0) {

			addRemoveBtn.setOnAction(arg0);

		}

		String getName() {

			return name;

		}

	}

	private static class AddContactBox extends AddRemoveContactBox {

		AddContactBox(String name) {

			super(name);

		}

		protected final void initButton() {

			addRemoveBtn = ViewFactory.newAddBtn();

			addRemoveBtn.setText(name);
			HBox.setHgrow(addRemoveBtn, Priority.ALWAYS);
			addRemoveBtn.setMaxWidth(Double.MAX_VALUE);
			addRemoveBtn.setAlignment(Pos.CENTER_LEFT);
			addRemoveBtn.setMnemonicParsing(false);
			addRemoveBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0));
			addRemoveBtn.setPadding(new Insets(5.0));

		}

	}

	private static class RemoveContactBox extends AddRemoveContactBox {

		RemoveContactBox(String name) {

			super(name);

		}

		protected final void initButton() {

			addRemoveBtn = ViewFactory.newRemoveBtn();

			addRemoveBtn.setText(name);
			HBox.setHgrow(addRemoveBtn, Priority.ALWAYS);
			addRemoveBtn.setMaxWidth(Double.MAX_VALUE);
			addRemoveBtn.setAlignment(Pos.CENTER_LEFT);
			addRemoveBtn.setMnemonicParsing(false);
			addRemoveBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0));
			addRemoveBtn.setPadding(new Insets(5.0));

		}

	}

}

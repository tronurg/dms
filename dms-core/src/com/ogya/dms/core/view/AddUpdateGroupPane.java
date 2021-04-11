package com.ogya.dms.core.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.common.CommonMethods;
import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
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
import javafx.scene.effect.Effect;
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

	private final double gap = ViewFactory.getGap();

	private final double viewFactor = ViewFactory.getViewFactor();

	private final HBox topPane = new HBox(gap);

	private final Button backBtn;
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

	private final Map<Long, ContactGroup> idContactGroups = Collections
			.synchronizedMap(new HashMap<Long, ContactGroup>());
	private final ObservableSet<Long> selectedIds = FXCollections.observableSet();

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

	AddUpdateGroupPane(BooleanProperty unreadProperty) {

		super();

		this.backBtn = ViewFactory.newBackBtn(unreadProperty);

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

		getContactGroup(contact.getId()).updateContact(contact.getName(), contact.getStatus().getStatusColor(),
				!Objects.equals(contact.getViewStatus(), ViewStatus.DELETED));

	}

	String getGroupName() {

		return groupNameTextField.getText().trim();

	}

	Set<Long> getSelectedIds() {

		return new HashSet<Long>(selectedIds);

	}

	void resetContent(Dgroup group, boolean isNewGroup) {

		searchContactTextField.setText("");
		groupNameTextField.setText(group == null ? "" : group.getName());
		selectedIds.clear();
		if (group != null) {
			group.getMembers().forEach(contact -> {
				Long id = contact.getId();
				// initialize if not exists
				if (!idContactGroups.containsKey(id))
					getContactGroup(id).updateContact(contact.getName(), Color.BLACK, false);
				selectedIds.add(id);
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
		bottomPane.getChildren().addAll(emptyPane, addUpdateGroupBtn);

		setTop(topPane);
		setCenter(scrollPane);
		setBottom(bottomPane);

	}

	private void initTopPane() {

		topPane.setBackground(new Background(new BackgroundFill(Color.LIGHTSKYBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
		topPane.setPadding(new Insets(gap));
		topPane.setAlignment(Pos.CENTER_LEFT);

		initGroupNameTextField();

		topPane.getChildren().addAll(backBtn, groupNameTextField, deleteBtn);

	}

	private void initGroupNameTextField() {

		groupNameTextField.getStyleClass().add("black-label");

		HBox.setHgrow(groupNameTextField, Priority.ALWAYS);

		groupNameTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

		groupNameTextField.setPadding(Insets.EMPTY);
		groupNameTextField.setPromptText(CommonMethods.translate("TYPE_GROUP_NAME"));
		groupNameTextField.setFocusTraversable(false);
		groupNameTextField.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));

	}

	private void initDeleteBtn() {

		final Effect shadow = new DropShadow();

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

		addedContactsTitledPane.setStyle("-fx-box-border: transparent;");
		notAddedContactsTitledPane.setStyle("-fx-box-border: transparent;");

		scrollableContent.getChildren().addAll(addedContactsTitledPane, notAddedContactsTitledPane);

	}

	private void initAddedContactsPane() {

		addedContactsPane.setPadding(Insets.EMPTY);

	}

	private void initNotAddedContactsPane() {

		notAddedContactsPane.setPadding(Insets.EMPTY);

	}

	private void initSearchContactTextField() {

		searchContactTextField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
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

		addUpdateGroupBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));

		addUpdateGroupBtn.setMnemonicParsing(false);

		addUpdateGroupBtn.setMaxWidth(Double.MAX_VALUE);

		addUpdateGroupBtn.disableProperty().bind(
				deleteMode.not().and(Bindings.isEmpty(selectedIds).or(groupNameTextField.textProperty().isEmpty())));

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

	private ContactGroup getContactGroup(Long id) {

		ContactGroup contactGroup = idContactGroups.get(id);

		if (contactGroup == null) {
			contactGroup = new ContactGroup(id);
			idContactGroups.put(id, contactGroup);
		}

		return contactGroup;

	}

	private abstract class AddRemoveContactBox extends HBox {

		protected Button addRemoveBtn;
		private final Circle addRemoveStatusCircle = new Circle(7.0 * viewFactor);

		private AddRemoveContactBox() {

			super();

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

			HBox.setMargin(addRemoveStatusCircle, new Insets(gap, 3 * gap, gap, gap));

		}

		protected void setOnAction(EventHandler<ActionEvent> arg0) {

			addRemoveBtn.setOnAction(arg0);

		}

		protected void updateContact(String name, Color statusColor) {

			addRemoveBtn.setText(name);
			addRemoveStatusCircle.setFill(statusColor);

		}

		protected String getName() {

			return addRemoveBtn.getText();

		}

		protected final StringProperty nameProperty() {

			return addRemoveBtn.textProperty();

		}

	}

	private class AddContactBox extends AddRemoveContactBox {

		private AddContactBox() {

			super();

		}

		protected final void initButton() {

			addRemoveBtn = ViewFactory.newAddBtn();

			HBox.setHgrow(addRemoveBtn, Priority.ALWAYS);
			addRemoveBtn.setMaxWidth(Double.MAX_VALUE);
			addRemoveBtn.setAlignment(Pos.CENTER_LEFT);
			addRemoveBtn.setMnemonicParsing(false);
			addRemoveBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));
			addRemoveBtn.setPadding(new Insets(gap));

		}

	}

	private class RemoveContactBox extends AddRemoveContactBox {

		private RemoveContactBox() {

			super();

		}

		protected final void initButton() {

			addRemoveBtn = ViewFactory.newRemoveBtn(1.0);

			HBox.setHgrow(addRemoveBtn, Priority.ALWAYS);
			addRemoveBtn.setMaxWidth(Double.MAX_VALUE);
			addRemoveBtn.setAlignment(Pos.CENTER_LEFT);
			addRemoveBtn.setMnemonicParsing(false);
			addRemoveBtn.setFont(Font.font(null, FontWeight.BOLD, 18.0 * viewFactor));
			addRemoveBtn.setPadding(new Insets(gap));

		}

	}

	private class ContactGroup {

		private final Long id;

		private final AddContactBox addContactBox;
		private final RemoveContactBox removeContactBox;

		private final BooleanProperty activeProperty = new SimpleBooleanProperty(false);

		private ContactGroup(Long id) {

			super();

			this.id = id;

			addContactBox = new AddContactBox();
			removeContactBox = new RemoveContactBox();

			init();

		}

		private void init() {

			BooleanBinding addContactBinding = Bindings.createBooleanBinding(() -> selectedIds.contains(id),
					selectedIds);
			BooleanBinding searchContactBinding = Bindings.createBooleanBinding(() -> {
				String searchContactStr = searchContactTextField.getText().toLowerCase();
				return searchContactStr.isEmpty() || addContactBox.getName().toLowerCase().startsWith(searchContactStr);
			}, searchContactTextField.textProperty(), addContactBox.nameProperty());

			addContactBox.visibleProperty().bind(activeProperty.and(searchContactBinding).and(addContactBinding.not()));
			removeContactBox.visibleProperty().bind(addContactBinding);

			addContactBox.setOnAction(e -> {

				addedContactsPane.getChildren().remove(removeContactBox);
				addedContactsPane.getChildren().add(0, removeContactBox);

				selectedIds.add(id);

			});

			removeContactBox.setOnAction(e -> selectedIds.remove(id));

			addedContactsPane.getChildren().add(removeContactBox);
			notAddedContactsPane.getChildren().add(addContactBox);

		}

		private void updateContact(String name, Color statusColor, boolean active) {

			activeProperty.set(active);

			boolean toBeSorted = !Objects.equals(addContactBox.getName(), name);

			addContactBox.updateContact(name, statusColor);
			removeContactBox.updateContact(name, statusColor);

			if (toBeSorted)
				FXCollections.sort(notAddedContactsPane.getChildren(), contactsSorter);

		}

	}

}

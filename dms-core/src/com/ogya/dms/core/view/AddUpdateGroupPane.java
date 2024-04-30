package com.ogya.dms.core.view;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.ogya.dms.core.database.tables.Contact;
import com.ogya.dms.core.database.tables.Dgroup;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.ViewStatus;
import com.ogya.dms.core.util.Commons;
import com.ogya.dms.core.view.component.DmsScrollPane;
import com.ogya.dms.core.view.component.SearchField;
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
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;

public class AddUpdateGroupPane extends BorderPane {

	private final HBox topPane = new HBox();

	private final Button backBtn;
	private final TextField groupNameTextField = new TextField();
	private final Button deleteBtn = ViewFactory.newDeleteBtn();

	private final VBox scrollableContent = new VBox();
	private final ScrollPane scrollPane = new DmsScrollPane(scrollableContent);
	private final Popup deleteGroupPopup = new Popup();
	private final VBox addedContactsPane = new VBox();
	private final VBox notAddedContactsPane = new VBox();
	private final SearchField searchField = new SearchField(true);

	private final Button addUpdateGroupBtn = new Button();
	private final Button deleteGroupBtn = new Button(Commons.translate("DELETE_GROUP"));

	private final Map<Long, ContactGroup> idContactGroups = Collections
			.synchronizedMap(new HashMap<Long, ContactGroup>());
	private final ObservableSet<Long> selectedIds = FXCollections.observableSet();

	private final BooleanProperty updateMode = new SimpleBooleanProperty();

	private final AtomicReference<Runnable> addUpdateGroupActionRef = new AtomicReference<Runnable>();
	private final AtomicReference<Runnable> deleteGroupActionRef = new AtomicReference<Runnable>();

	private final Comparator<Node> contactsSorter = new Comparator<Node>() {

		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof AddRemoveContactBox && arg1 instanceof AddRemoveContactBox)) {
				return 0;
			}

			AddRemoveContactBox box0 = (AddRemoveContactBox) arg0;
			AddRemoveContactBox box1 = (AddRemoveContactBox) arg1;

			return collator.compare(box0.getName(), box1.getName());

		}

	};

	AddUpdateGroupPane(BooleanProperty unreadProperty) {
		super();
		this.backBtn = ViewFactory.newBackBtn(unreadProperty, this);
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

		getContactGroup(contact.getId()).updateContact(contact.getName(), contact.getStatus(),
				contact.getViewStatus() != ViewStatus.DELETED);

	}

	String getGroupName() {

		return groupNameTextField.textProperty().getValueSafe().trim();

	}

	Set<Long> getSelectedIds() {

		return new HashSet<Long>(selectedIds);

	}

	void resetContent(Dgroup group, boolean isNewGroup) {

		searchField.reset();
		groupNameTextField.setText(group == null ? "" : group.getName());
		selectedIds.clear();
		if (group != null) {
			group.getMembers().forEach(contact -> {
				Long id = contact.getId();
				// initialize if not exists
				if (!idContactGroups.containsKey(id)) {
					getContactGroup(id).updateContact(contact.getName(), Availability.OFFLINE, false);
				}
				selectedIds.add(id);
			});
		}

		updateMode.set(!isNewGroup);

	}

	private void init() {

		initTopPane();
		initScrollableContent();
		initAddUpdateGroupBtn();

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

		topPane.getStyleClass().addAll("top-pane");

		initGroupNameTextField();
		initDeleteBtn();

		topPane.getChildren().addAll(backBtn, groupNameTextField, deleteBtn);

	}

	private void initScrollableContent() {

		initAddedContactsPane();
		initNotAddedContactsPane();

		BorderPane notAddedContactsBorderPane = new BorderPane();
		notAddedContactsBorderPane.setPadding(Insets.EMPTY);
		notAddedContactsBorderPane.setTop(searchField);
		notAddedContactsBorderPane.setCenter(notAddedContactsPane);

		TitledPane addedContactsTitledPane = new TitledPane(Commons.translate("ADDED_CONTACTS"), addedContactsPane);
		TitledPane notAddedContactsTitledPane = new TitledPane(Commons.translate("ALL_CONTACTS"),
				notAddedContactsBorderPane);
		addedContactsTitledPane.getStyleClass().addAll("transparent-box-border");
		notAddedContactsTitledPane.getStyleClass().addAll("transparent-box-border");
		addedContactsTitledPane.setCollapsible(false);
		notAddedContactsTitledPane.setCollapsible(false);

		scrollableContent.getChildren().addAll(addedContactsTitledPane, notAddedContactsTitledPane);

	}

	private void initAddUpdateGroupBtn() {

		addUpdateGroupBtn.getStyleClass().addAll("green-bg", "em12", "bold");
		addUpdateGroupBtn.setTextFill(Color.ANTIQUEWHITE);
		addUpdateGroupBtn.setMnemonicParsing(false);
		addUpdateGroupBtn.setMaxWidth(Double.MAX_VALUE);

		addUpdateGroupBtn.textProperty()
				.bind(Bindings.createStringBinding(
						() -> updateMode.get() ? Commons.translate("UPDATE_GROUP") : Commons.translate("CREATE_GROUP"),
						updateMode));

		addUpdateGroupBtn.disableProperty().bind(Bindings.isEmpty(selectedIds)
				.or(Bindings.createBooleanBinding(() -> getGroupName().isEmpty(), groupNameTextField.textProperty())));

		addUpdateGroupBtn.setOnAction(e -> {
			Runnable addUpdateGroupAction = addUpdateGroupActionRef.get();
			if (addUpdateGroupAction != null) {
				addUpdateGroupAction.run();
			}
		});

	}

	private void initGroupNameTextField() {

		groupNameTextField.getStyleClass().addAll("black-label", "em12", "bold");
		HBox.setHgrow(groupNameTextField, Priority.ALWAYS);

		groupNameTextField.setTextFormatter(
				new TextFormatter<String>(change -> change.getControlNewText().length() > 40 ? null : change));

		groupNameTextField.setPadding(Insets.EMPTY);
		groupNameTextField.setPromptText(Commons.translate("TYPE_GROUP_NAME"));
		groupNameTextField.setFocusTraversable(false);

	}

	private void initDeleteBtn() {

		initDeleteGroupPopup();

		deleteBtn.visibleProperty().bind(updateMode);
		deleteBtn.managedProperty().bind(deleteBtn.visibleProperty());

		deleteBtn.setOnAction(e -> {
			Point2D point = deleteBtn.localToScreen(deleteBtn.getWidth(), 1.25 * deleteBtn.getHeight());
			deleteGroupPopup.show(deleteBtn, point.getX(), point.getY());
		});

	}

	private void initAddedContactsPane() {

		addedContactsPane.setPadding(Insets.EMPTY);

	}

	private void initNotAddedContactsPane() {

		notAddedContactsPane.setPadding(Insets.EMPTY);

	}

	private void initDeleteGroupPopup() {

		deleteGroupPopup.setAutoHide(true);
		deleteGroupPopup.setAnchorLocation(AnchorLocation.WINDOW_TOP_RIGHT);

		initDeleteGroupBtn();

		deleteGroupPopup.getContent().add(deleteGroupBtn);

	}

	private void initDeleteGroupBtn() {

		deleteGroupBtn.getStyleClass().addAll("red-bg", "em12", "bold");
		deleteGroupBtn.setTextFill(Color.ANTIQUEWHITE);
		deleteGroupBtn.setMnemonicParsing(false);

		deleteGroupBtn.setOnAction(e -> {
			deleteGroupPopup.hide();
			Runnable deleteGroupAction = deleteGroupActionRef.get();
			if (deleteGroupAction != null) {
				deleteGroupAction.run();
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
		protected Label addRemoveLbl = new Label();
		private final Circle addRemoveStatusCircle = new Circle(7.0);
		private final HBox statusCircleGraph = new HBox(new Group(addRemoveStatusCircle));

		private AddRemoveContactBox() {
			super();
			init();
		}

		private void init() {

			initLabel();
			initCircle();
			initButton();

			setAlignment(Pos.CENTER_LEFT);
			managedProperty().bind(visibleProperty());

			getChildren().addAll(addRemoveBtn, statusCircleGraph);

		}

		private void initLabel() {

			addRemoveLbl.getStyleClass().addAll("em12", "bold");
			addRemoveLbl.setMnemonicParsing(false);

		}

		private void initCircle() {

			statusCircleGraph.getStyleClass().addAll("padding-1311");
			statusCircleGraph.setAlignment(Pos.CENTER_LEFT);
			addRemoveStatusCircle.setStyle(ViewFactory.getScaleCss(1d, 1d));

		}

		protected abstract void initButton();

		protected void setOnAction(EventHandler<ActionEvent> arg0) {

			addRemoveBtn.setOnAction(arg0);

		}

		protected void updateContact(String name, Color statusColor) {

			addRemoveLbl.setText(name);
			addRemoveStatusCircle.setFill(statusColor);

		}

		protected String getName() {

			return addRemoveLbl.getText();

		}

		protected final StringProperty nameProperty() {

			return addRemoveLbl.textProperty();

		}

	}

	private class AddContactBox extends AddRemoveContactBox {

		private AddContactBox() {

			super();

		}

		protected final void initButton() {

			addRemoveBtn = ViewFactory.newAddBtnWithLbl(addRemoveLbl);
			addRemoveBtn.getStyleClass().addAll("padding-1");
			HBox.setHgrow(addRemoveBtn, Priority.ALWAYS);
			addRemoveBtn.setMaxWidth(Double.MAX_VALUE);
			addRemoveBtn.setAlignment(Pos.CENTER_LEFT);

		}

	}

	private class RemoveContactBox extends AddRemoveContactBox {

		private RemoveContactBox() {

			super();

		}

		protected final void initButton() {

			addRemoveBtn = ViewFactory.newRemoveBtnWithLbl(addRemoveLbl);
			addRemoveBtn.getStyleClass().addAll("padding-1");
			HBox.setHgrow(addRemoveBtn, Priority.ALWAYS);
			addRemoveBtn.setMaxWidth(Double.MAX_VALUE);
			addRemoveBtn.setAlignment(Pos.CENTER_LEFT);

		}

	}

	private class ContactGroup {

		private final Long id;

		private final AddContactBox addContactBox;
		private final RemoveContactBox removeContactBox;

		private final BooleanProperty onlineProperty = new SimpleBooleanProperty(false);
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
				String searchContactStr = searchField.getText().toLowerCase(Locale.getDefault());
				return searchContactStr.isEmpty()
						|| addContactBox.getName().toLowerCase(Locale.getDefault()).startsWith(searchContactStr);
			}, searchField.textProperty(), addContactBox.nameProperty());
			BooleanBinding filterOnlineProperty = searchField.filterOnlineProperty().not().or(onlineProperty);

			addContactBox.visibleProperty().bind(
					activeProperty.and(searchContactBinding).and(addContactBinding.not()).and(filterOnlineProperty));
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

		private void updateContact(String name, Availability status, boolean active) {

			onlineProperty.set(status != Availability.OFFLINE);
			activeProperty.set(active);

			boolean toBeSorted = !Objects.equals(addContactBox.getName(), name);

			addContactBox.updateContact(name, status.getStatusColor());
			removeContactBox.updateContact(name, status.getStatusColor());

			if (toBeSorted) {
				FXCollections.sort(notAddedContactsPane.getChildren(), contactsSorter);
			}

		}

	}

}

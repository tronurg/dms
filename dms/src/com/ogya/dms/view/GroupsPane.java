package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ogya.dms.common.CommonMethods;
import com.ogya.dms.database.tables.Contact;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

class GroupsPane extends TitledPane {

	private final BorderPane borderPane = new BorderPane();
	private final Button createGroupBtn = ViewFactory.newAddBtn();
	private final VBox groups = new VBox();

	private final CreateGroupPane createGroupPane = new CreateGroupPane();

	private final List<IGroupsPane> listeners = Collections.synchronizedList(new ArrayList<IGroupsPane>());

	GroupsPane() {

		super();

		init();

	}

	private void init() {

		createGroupPane
				.setOnBackAction(() -> listeners.forEach(listener -> listener.hideCreateGroupPane(createGroupPane)));
		createGroupPane.setOnCreateGroupAction(
				() -> listeners.forEach(listener -> listener.createGroupClicked(createGroupPane)));

		initGrupOlusturBtn();

		setText(CommonMethods.translate("GROUPS"));

		groups.setPadding(new Insets(10.0));

		ScrollPane scrollPane = new ScrollPane(groups);
		scrollPane.setFitToWidth(true);

		borderPane.setTop(createGroupBtn);
		borderPane.setCenter(scrollPane);

		borderPane.setPadding(Insets.EMPTY);

		setContent(borderPane);

	}

	void addListener(IGroupsPane listener) {

		listeners.add(listener);

	}

	void createGroupPaneUpdateContact(Contact contact) {

		createGroupPane.updateContact(contact);

	}

	private void initGrupOlusturBtn() {

		createGroupBtn.setMnemonicParsing(false);
		createGroupBtn.setText(CommonMethods.translate("CREATE_GROUP"));
		createGroupBtn.setTextFill(Color.GRAY);
		createGroupBtn.setPadding(new Insets(10.0));

		createGroupBtn.setOnAction(e -> listeners.forEach(listener -> listener.showCreateGroupPane(createGroupPane)));

	}

}

interface IGroupsPane {

	void showCreateGroupPane(CreateGroupPane createGroupPane);

	void hideCreateGroupPane(CreateGroupPane createGroupPane);

	void createGroupClicked(CreateGroupPane createGroupPane);

	void showGroupMessagePane(MessagePane messagePane, String uuid);

	void hideGroupMessagePane(MessagePane messagePane, String uuid);

	void sendGroupMessageClicked(String messageTxt, String uuid);

	void groupPaneScrolledToTop(String uuid);

}

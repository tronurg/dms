package com.ogya.dms.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.UIManager;

import com.ogya.dms.database.tables.Dgroup;
import com.ogya.dms.structures.Availability;
import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MyActiveGroupsPanel extends BorderPane {

	private final VBox groups = new VBox();
	private final ScrollPane scrollPane = new ScrollPane(groups) {
		@Override
		public void requestFocus() {
		}
	};

	private final Map<String, GroupBundle> uuidGroupBundle = Collections
			.synchronizedMap(new HashMap<String, GroupBundle>());

	private final Comparator<Node> groupsSorter = new Comparator<Node>() {

		@Override
		public int compare(Node arg0, Node arg1) {

			if (!(arg0 instanceof GroupPane && arg1 instanceof GroupPane))
				return 0;

			GroupPane group0 = (GroupPane) arg0;
			GroupPane group1 = (GroupPane) arg1;

			return group0.getName().toLowerCase().compareTo(group1.getName().toLowerCase());

		}

	};

	private final AtomicReference<String> selectedUuid = new AtomicReference<String>();

	public MyActiveGroupsPanel() {

		super();

		init();

	}

	private void init() {

		groups.setPadding(new Insets(10.0));

		scrollPane.setFitToWidth(true);

		setCenter(scrollPane);

		setPadding(Insets.EMPTY);

	}

	public void updateGroup(Dgroup group) {

		GroupBundle groupBundle = getGroupBundle(group.getUuid());

		groupBundle.groupPane.updateGroup(group);

		boolean visible = Objects.equals(group.getStatus(), Availability.AVAILABLE);

		groupBundle.setVisible(visible);

		if (!visible)
			groupBundle.selectedProperty.set(false);

	}

	public String getSelectedUuid() {

		return selectedUuid.get();

	}

	public void resetSelection() {

		selectedUuid.set(null);

		uuidGroupBundle.forEach((uuid, bundle) -> bundle.selectedProperty.set(false));

	}

	public void updateUI() {

		setStyle("-panel-background: #"
				+ String.format("%6s", Integer.toHexString(
						((java.awt.Color) UIManager.get("Panel.background")).getRGB() & 0xffffff)).replace(' ', '0')
				+ ";" + "-text-fill: #" + String
						.format("%6s",
								Integer.toHexString(
										((java.awt.Color) UIManager.get("Panel.foreground")).getRGB() & 0xffffff))
						.replace(' ', '0')
				+ ";");

	}

	private GroupBundle getGroupBundle(final String groupUuid) {

		if (!uuidGroupBundle.containsKey(groupUuid)) {

			final GroupBundle groupBundle = new GroupBundle();

			groupBundle.managedProperty().bind(groupBundle.visibleProperty());

			groupBundle.setOnMouseClicked(e -> {

				boolean wasSelected = groupBundle.selectedProperty.get();

				uuidGroupBundle.forEach((uuid, bundle) -> bundle.selectedProperty.set(false));

				groupBundle.selectedProperty.set(!wasSelected);

				selectedUuid.set(groupBundle.selectedProperty.get() ? groupUuid : null);

			});

			uuidGroupBundle.put(groupUuid, groupBundle);

			groups.getChildren().add(groupBundle);

			FXCollections.sort(groups.getChildren(), groupsSorter);

		}

		return uuidGroupBundle.get(groupUuid);

	}

	private final class GroupBundle extends HBox {

		private final GroupPane groupPane = new GroupPane();
		private final Label selectionLbl = ViewFactory.newSelectionLbl();
		private final BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

		private GroupBundle() {

			HBox.setHgrow(groupPane, Priority.ALWAYS);

			selectionLbl.visibleProperty().bind(selectedProperty);

			getChildren().addAll(groupPane, selectionLbl);

		}

	}

}

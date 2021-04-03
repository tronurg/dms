package com.ogya.dms.core.intf.handles.impl;

import java.util.List;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.ogya.dms.core.database.tables.EntityId;
import com.ogya.dms.core.intf.handles.GroupHandle;
import com.ogya.dms.core.intf.handles.GroupSelectionHandle;
import com.ogya.dms.core.view.SelectableEntitiesPane;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class ActiveGroupsHandleImpl implements GroupSelectionHandle {

	private final SelectableEntitiesPane activeGroupsPanel;
	private final JFXPanel activeGroupsPanelSwing;

	public ActiveGroupsHandleImpl(SelectableEntitiesPane activeGroupsPanel) {

		this.activeGroupsPanel = activeGroupsPanel;

		activeGroupsPanelSwing = new JFXPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				super.updateUI();
				Platform.runLater(() -> activeGroupsPanel.updateUI());
			}

		};

		activeGroupsPanelSwing.addAncestorListener(new AncestorListener() {

			@Override
			public void ancestorRemoved(AncestorEvent arg0) {

			}

			@Override
			public void ancestorMoved(AncestorEvent arg0) {

			}

			@Override
			public void ancestorAdded(AncestorEvent arg0) {

				activeGroupsPanelSwing.updateUI();

			}

		});

		Platform.runLater(() -> {

			Scene myActiveGroupsScene = new Scene(activeGroupsPanel);
			myActiveGroupsScene.getStylesheets().add("/resources/css/style.css");
			activeGroupsPanelSwing.setScene(myActiveGroupsScene);

		});

	}

	@Override
	public JComponent getGroupSelectionPanel() {

		return getGroupSelectionPanel(null);

	}

	@Override
	public JComponent getGroupSelectionPanel(Predicate<GroupHandle> filter) {

		resetSelection();

		activeGroupsPanel.setGroupFilter(filter);

		return activeGroupsPanelSwing;

	}

	@Override
	public Long getSelectedGroupId() {

		List<EntityId> selectedEntityIds = activeGroupsPanel.getSelectedEntityIds();
		if (selectedEntityIds.isEmpty())
			return null;

		return selectedEntityIds.get(0).getId();

	}

	@Override
	public void resetSelection() {

		activeGroupsPanel.resetSelection();

	}

}

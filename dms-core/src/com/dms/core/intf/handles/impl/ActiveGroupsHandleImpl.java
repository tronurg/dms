package com.dms.core.intf.handles.impl;

import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.dms.core.intf.handles.GroupHandle;
import com.dms.core.intf.handles.GroupSelectionHandle;
import com.dms.core.intf.tools.GroupId;
import com.dms.core.intf.tools.impl.GroupIdImpl;
import com.dms.core.view.ActiveGroupsPane;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class ActiveGroupsHandleImpl implements GroupSelectionHandle {

	private final ActiveGroupsPane activeGroupsPane;
	private final JFXPanel activeGroupsPanelSwing;

	public ActiveGroupsHandleImpl(ActiveGroupsPane activeGroupsPane) {

		this.activeGroupsPane = activeGroupsPane;

		activeGroupsPanelSwing = new JFXPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				super.updateUI();
				Platform.runLater(() -> activeGroupsPane.updateUI());
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

			Scene myActiveGroupsScene = new Scene(activeGroupsPane);
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

		activeGroupsPane.setGroupFilter(filter);

		return activeGroupsPanelSwing;

	}

	@Override
	public GroupId getSelectedGroupId() {

		return GroupIdImpl.of(activeGroupsPane.getSelectedId());

	}

	@Override
	public void resetSelection() {

		activeGroupsPane.resetSelection();

	}

}

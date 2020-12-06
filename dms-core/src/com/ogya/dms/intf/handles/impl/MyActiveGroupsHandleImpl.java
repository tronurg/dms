package com.ogya.dms.intf.handles.impl;

import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.ogya.dms.intf.handles.GroupHandle;
import com.ogya.dms.intf.handles.GroupSelectionHandle;
import com.ogya.dms.view.MyActiveGroupsPanel;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class MyActiveGroupsHandleImpl implements GroupSelectionHandle {

	private final MyActiveGroupsPanel myActiveGroupsPanel;
	private final JFXPanel myActiveGroupsPanelSwing;

	public MyActiveGroupsHandleImpl(MyActiveGroupsPanel myActiveGroupsPanel) {

		this.myActiveGroupsPanel = myActiveGroupsPanel;

		myActiveGroupsPanelSwing = new JFXPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				super.updateUI();
				Platform.runLater(() -> myActiveGroupsPanel.updateUI());
			}

		};

		myActiveGroupsPanelSwing.addAncestorListener(new AncestorListener() {

			@Override
			public void ancestorRemoved(AncestorEvent arg0) {

			}

			@Override
			public void ancestorMoved(AncestorEvent arg0) {

			}

			@Override
			public void ancestorAdded(AncestorEvent arg0) {

				myActiveGroupsPanelSwing.updateUI();

			}

		});

		Platform.runLater(() -> {

			Scene myActiveGroupsScene = new Scene(myActiveGroupsPanel);
			myActiveGroupsScene.getStylesheets().add("/resources/css/style.css");
			myActiveGroupsPanelSwing.setScene(myActiveGroupsScene);

		});

	}

	@Override
	public JComponent getGroupSelectionPanel() {

		return getGroupSelectionPanel(null);

	}

	@Override
	public JComponent getGroupSelectionPanel(Predicate<GroupHandle> filter) {

		resetSelection();

		myActiveGroupsPanel.setFilter(filter);

		return myActiveGroupsPanelSwing;

	}

	@Override
	public Long getSelectedGroupId() {

		return myActiveGroupsPanel.getSelectedId();

	}

	@Override
	public void resetSelection() {

		myActiveGroupsPanel.resetSelection();

	}

}

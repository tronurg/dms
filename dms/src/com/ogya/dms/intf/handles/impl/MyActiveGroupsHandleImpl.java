package com.ogya.dms.intf.handles.impl;

import javax.swing.JComponent;

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
				Platform.runLater(() -> myActiveGroupsPanel.updateUI());
				super.updateUI();
			}

		};

		Platform.runLater(() -> {

			Scene myActiveGroupsScene = new Scene(myActiveGroupsPanel);
			myActiveGroupsScene.getStylesheets().add("/resources/css/style.css");
			myActiveGroupsPanel.updateUI();
			myActiveGroupsPanelSwing.setScene(myActiveGroupsScene);

		});

	}

	@Override
	public JComponent getGroupSelectionPanel() {

		Platform.runLater(() -> myActiveGroupsPanel.updateUI());

		return myActiveGroupsPanelSwing;

	}

	@Override
	public String getSelectedGroupUuid() {

		return myActiveGroupsPanel.getSelectedUuid();

	}

	@Override
	public void resetSelection() {

		myActiveGroupsPanel.resetSelection();

	}

}

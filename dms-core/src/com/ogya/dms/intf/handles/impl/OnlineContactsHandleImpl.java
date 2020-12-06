package com.ogya.dms.intf.handles.impl;

import java.util.List;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.ogya.dms.intf.handles.ContactHandle;
import com.ogya.dms.intf.handles.ContactSelectionHandle;
import com.ogya.dms.view.OnlineContactsPanel;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class OnlineContactsHandleImpl implements ContactSelectionHandle {

	private final OnlineContactsPanel onlineContactsPanel;
	private final JFXPanel onlineContactsPanelSwing;

	public OnlineContactsHandleImpl(OnlineContactsPanel onlineContactsPanel) {

		this.onlineContactsPanel = onlineContactsPanel;

		onlineContactsPanelSwing = new JFXPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				super.updateUI();
				Platform.runLater(() -> onlineContactsPanel.updateUI());
			}

		};

		onlineContactsPanelSwing.addAncestorListener(new AncestorListener() {

			@Override
			public void ancestorRemoved(AncestorEvent arg0) {

			}

			@Override
			public void ancestorMoved(AncestorEvent arg0) {

			}

			@Override
			public void ancestorAdded(AncestorEvent arg0) {

				onlineContactsPanelSwing.updateUI();

			}

		});

		Platform.runLater(() -> {

			Scene onlineContactsScene = new Scene(onlineContactsPanel);
			onlineContactsScene.getStylesheets().add("/resources/css/style.css");
			onlineContactsPanelSwing.setScene(onlineContactsScene);

		});

	}

	@Override
	public JComponent getContactSelectionPanel() {

		return getContactSelectionPanel(null);

	}

	@Override
	public JComponent getContactSelectionPanel(Predicate<ContactHandle> filter) {

		resetSelection();

		onlineContactsPanel.setFilter(filter);

		return onlineContactsPanelSwing;

	}

	@Override
	public List<Long> getSelectedContactIds() {

		return onlineContactsPanel.getSelectedIds();

	}

	@Override
	public void resetSelection() {

		onlineContactsPanel.resetSelection();

	}

}

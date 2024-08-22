package com.dms.core.intf.handles.impl;

import java.util.List;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.dms.core.intf.handles.ContactHandle;
import com.dms.core.intf.handles.ContactSelectionHandle;
import com.dms.core.intf.tools.ContactId;
import com.dms.core.view.ActiveContactsPane;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class ActiveContactsHandleImpl implements ContactSelectionHandle {

	private final ActiveContactsPane activeContactsPane;
	private final JFXPanel onlineContactsPanelSwing;

	public ActiveContactsHandleImpl(ActiveContactsPane activeContactsPane) {

		this.activeContactsPane = activeContactsPane;

		onlineContactsPanelSwing = new JFXPanel() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				super.updateUI();
				Platform.runLater(() -> activeContactsPane.updateUI());
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

			Scene onlineContactsScene = new Scene(activeContactsPane);
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

		activeContactsPane.setContactFilter(filter);

		return onlineContactsPanelSwing;

	}

	@Override
	public List<ContactId> getSelectedContactIds() {

		return activeContactsPane.getSelectedIds();

	}

	@Override
	public void resetSelection() {

		activeContactsPane.resetSelection();

	}

}

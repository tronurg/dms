package com.ogya.dms.core.view;

import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ogya.dms.core.view.ReportsPane.ReportTemplate;
import com.ogya.dms.core.view.ReportsPane.ReportsListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class ReportsDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final ReportsPane reportsPane;
	private final JFXPanel reportsPaneSwing = new JFXPanel();

	public ReportsDialog(List<ReportTemplate> templates) {

		super();

		reportsPane = new ReportsPane(templates);

		init();

	}

	private void init() {

		reportsPane.setOnCancelAction(this::hideAndReset);

		initMobility();

		Platform.runLater(() -> {

			Scene scene = new Scene(reportsPane);
			scene.getStylesheets().add("/resources/css/style.css");
			reportsPaneSwing.setScene(scene);

		});

		setUndecorated(true);
		setModal(true);
		setAlwaysOnTop(true);
		setContentPane(reportsPaneSwing);

	}

	public void addReportsListener(ReportsListener listener) {

		reportsPane.addReportsListener(listener);

	}

	public void display(Long id) {

		reportsPane.setId(id);

		Rectangle screenBounds = MouseInfo.getPointerInfo().getDevice().getDefaultConfiguration().getBounds();

		int x = screenBounds.x + screenBounds.width / 4;
		int y = screenBounds.y + screenBounds.height / 4;
		int width = screenBounds.width / 2;
		int height = screenBounds.height / 2;

		setBounds(x, y, width, height);

		SwingUtilities.invokeLater(() -> setVisible(true));

	}

	public void hideAndReset() {

		SwingUtilities.invokeLater(() -> dispose());

		reportsPane.reset();

	}

	public void updateUI() {

		reportsPane.setStyle("-panel-background: #"
				+ String.format("%6s", Integer.toHexString(
						((java.awt.Color) UIManager.get("Panel.background")).getRGB() & 0xffffff)).replace(' ', '0')
				+ ";" + "-text-fill: #" + String
						.format("%6s",
								Integer.toHexString(
										((java.awt.Color) UIManager.get("Panel.foreground")).getRGB() & 0xffffff))
						.replace(' ', '0')
				+ ";");

	}

	private void initMobility() {

		final AtomicInteger xRef = new AtomicInteger();
		final AtomicInteger yRef = new AtomicInteger();

		reportsPane.setOnMousePressed(e -> {
			xRef.set((int) e.getSceneX());
			yRef.set((int) e.getSceneY());
		});

		reportsPane.setOnMouseDragged(e -> {
			setLocation((int) e.getScreenX() - xRef.get(), (int) e.getScreenY() - yRef.get());
		});

	}

}

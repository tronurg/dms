package com.ogya.dms.test;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.DmsService;
import com.ogya.dms.intf.exceptions.DbException;
import com.ogya.dms.intf.listeners.DmsListener;

@Component(immediate = true)
public class DmsTest implements DmsListener {

	private DmsService dmsService;

	@Activate
	protected void activate() {

		try {

			DmsHandle handle = dmsService.login("elma", "elma");

			handle.addListener(this);

			JComponent mcPanel = handle.getDmsPanel();

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(mcPanel, BorderLayout.CENTER);

//			mcPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));

			frame.setContentPane(panel);
			frame.setSize(400, 600);
			frame.setLocationRelativeTo(null);
			frame.setLocation(frame.getLocation().x - 450, frame.getLocation().y);

			SwingUtilities.invokeLater(() -> frame.setVisible(true));

		} catch (DbException e) {

			e.printStackTrace();

		}

		try {

			DmsHandle handle = dmsService.login("armut", "armut");

			handle.addListener(this);

			JComponent mcPanel = handle.getDmsPanel();

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(mcPanel, BorderLayout.CENTER);

//			mcPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));

			frame.setContentPane(panel);
			frame.setSize(400, 600);
			frame.setLocationRelativeTo(null);

			SwingUtilities.invokeLater(() -> frame.setVisible(true));

		} catch (DbException e) {

			e.printStackTrace();

		}

		try {

			DmsHandle handle = dmsService.login("kiraz", "kiraz");

			handle.addListener(this);

			JComponent mcPanel = handle.getDmsPanel();

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(mcPanel, BorderLayout.CENTER);

//			mcPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));

			frame.setContentPane(panel);
			frame.setSize(400, 600);
			frame.setLocationRelativeTo(null);
			frame.setLocation(frame.getLocation().x + 450, frame.getLocation().y);

			SwingUtilities.invokeLater(() -> frame.setVisible(true));

		} catch (DbException e) {

			e.printStackTrace();

		}

	}

	@Reference
	protected void addDmsService(DmsService dmsService) {

		this.dmsService = dmsService;

	}

	protected void removeDmsService(DmsService dmsService) {

		this.dmsService = null;

	}

	@Override
	public void fileClicked(Path file) {

		try {

			new ProcessBuilder().directory(file.getParent().toFile())
					.command("cmd", "/C", file.getFileName().toString()).start();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

}

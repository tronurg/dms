package com.ogya.dms.test;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
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

		testOne();

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

	private void testOne() {

		try {

			DmsHandle handle = dmsService.login("elma", "elma");

			handle.addListener(this);

			JComponent mcPanel = handle.getDmsPanel();

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(mcPanel, BorderLayout.CENTER);

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

			frame.setContentPane(panel);
			frame.setSize(400, 600);
			frame.setLocationRelativeTo(null);
			frame.setLocation(frame.getLocation().x + 450, frame.getLocation().y);

			SwingUtilities.invokeLater(() -> frame.setVisible(true));

		} catch (DbException e) {

			e.printStackTrace();

		}

	}

	private void testTwo() {

		JFrame frame = new JFrame("LOGIN");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JTextField textField = new JTextField(30);

		textField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent arg0) {

				if (arg0.getKeyChar() != KeyEvent.VK_ENTER)
					return;

				frame.setVisible(false);

				new Thread(() -> {

					try {

						DmsHandle handle = dmsService.login(textField.getText(), textField.getText());

						handle.addListener(DmsTest.this);

						JComponent mcPanel = handle.getDmsPanel();

						JPanel panel = new JPanel(new BorderLayout());
						panel.add(mcPanel, BorderLayout.CENTER);

						frame.setContentPane(panel);
						frame.setSize(400, 600);
						frame.setLocationRelativeTo(null);

						SwingUtilities.invokeLater(() -> frame.setVisible(true));

					} catch (DbException e) {

						e.printStackTrace();

						System.exit(-1);

					}

				}).start();

			}

		});

		frame.add(textField);
		frame.pack();
		frame.setLocationRelativeTo(null);
		SwingUtilities.invokeLater(() -> frame.setVisible(true));

	}

}

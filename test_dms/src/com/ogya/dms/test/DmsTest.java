package com.ogya.dms.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ogya.dms.intf.DmsHandle;
import com.ogya.dms.intf.DmsService;
import com.ogya.dms.intf.exceptions.DbException;
import com.ogya.dms.intf.handles.ContactSelectionHandle;
import com.ogya.dms.intf.handles.GroupSelectionHandle;

@Component(immediate = true)
public class DmsTest {

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

	private void testOne() {

		try {

			DmsHandle handle = dmsService.login("elma", "elma");

			handle.addListener(new DmsListenerImpl(handle));

			JComponent mcPanel = handle.getDmsPanel();

			handle.setComment("merhaba");
			handle.setCoordinates(30.0, 30.0);

			new Thread(() -> {

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				UIManager.put("Panel.background", Color.DARK_GRAY);
				UIManager.put("Panel.foreground", Color.LIGHT_GRAY);

				SwingUtilities.invokeLater(() -> SwingUtilities.updateComponentTreeUI(mcPanel));

			}).start();

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

			handle.addListener(new DmsListenerImpl(handle));

			GroupSelectionHandle gsh = handle.getMyActiveGroupsHandle();

			JComponent mcPanel = handle.getDmsPanel();
//			JComponent mcPanel = gsh.getGroupSelectionPanel();
			JButton btn = new JButton("test");
			btn.addActionListener(e -> {

				TestPojo testPojo = new TestPojo();
				List<TestPojo> testList = new ArrayList<TestPojo>();
				testList.add(testPojo);

//				handle.sendMessageToGroup("hello group!", 1, gsh.getSelectedGroupUuid());
//				handle.sendObjectToGroup(testPojo, 1, gsh.getSelectedGroupUuid());
//				handle.sendListToGroup(testList, TestPojo.class, 1, gsh.getSelectedGroupUuid());
				handle.sendFileToGroup(Paths.get("D:/test.txt"), 1, gsh.getSelectedGroupUuid());

				gsh.resetSelection();

			});

			new Thread(() -> {

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				UIManager.put("Panel.background", Color.DARK_GRAY);
				UIManager.put("Panel.foreground", Color.LIGHT_GRAY);

				SwingUtilities.invokeLater(() -> SwingUtilities.updateComponentTreeUI(mcPanel));

			}).start();

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(mcPanel, BorderLayout.CENTER);
			panel.add(btn, BorderLayout.SOUTH);

			frame.setContentPane(panel);
			frame.setSize(400, 600);
			frame.setLocationRelativeTo(null);

			SwingUtilities.invokeLater(() -> frame.setVisible(true));

		} catch (DbException e) {

			e.printStackTrace();

		}

		try {

			DmsHandle handle = dmsService.login("kiraz", "kiraz");

			handle.addListener(new DmsListenerImpl(handle));

			ContactSelectionHandle csh = handle.getOnlineContactsHandle();

			JComponent mcPanel = handle.getDmsPanel();
//			JComponent mcPanel = csh.getContactSelectionPanel();
			JButton btn = new JButton("test");
			btn.addActionListener(e -> {

				TestPojo testPojo = new TestPojo();
				List<TestPojo> testList = new ArrayList<TestPojo>();
				testList.add(testPojo);

//				handle.sendMessageToContacts("hello contact!", 1, csh.getSelectedContactUuids());
//				handle.sendObjectToContacts(testPojo, 1, csh.getSelectedContactUuids());
//				handle.sendListToContacts(testList, TestPojo.class, 1, csh.getSelectedContactUuids());
				handle.sendFileToContacts(Paths.get("D:/test.txt"), 1, csh.getSelectedContactUuids());

				csh.resetSelection();

			});

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(mcPanel, BorderLayout.CENTER);
			panel.add(btn, BorderLayout.SOUTH);

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

						handle.addListener(new DmsListenerImpl(handle));

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

package com.ogya.dms.test.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ogya.dms.core.intf.DmsHandle;
import com.ogya.dms.core.intf.exceptions.DbException;
import com.ogya.dms.core.intf.handles.ContactSelectionHandle;
import com.ogya.dms.core.intf.handles.GroupSelectionHandle;
import com.ogya.dms.core.main.DmsCore;

public class DmsTest {

	public static void main(String[] args) {

		testOne();

	}

	private static void testOne() {

		try {

			DmsHandle dmsHandle = DmsCore.login("elma", "elma");

			dmsHandle.addListener(new DmsListenerImpl(dmsHandle));

			JComponent mcPanel = dmsHandle.getDmsPanel();

			dmsHandle.setComment("merhaba");
			dmsHandle.setCoordinates(30.0, 30.0);

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

			DmsHandle dmsHandle = DmsCore.login("armut", "armut");

			dmsHandle.addListener(new DmsListenerImpl(dmsHandle));

			GroupSelectionHandle gsh = dmsHandle.getActiveGroupsHandle();

			JComponent mcPanel = dmsHandle.getDmsPanel();
//			JComponent mcPanel = gsh.getGrupSecimPanel(dmsGrup -> dmsGrup.getIsim().startsWith("g"));
			JButton btn = new JButton("test");
			btn.addActionListener(e -> {

				TestPojo testPojo = new TestPojo();
				List<TestPojo> testList = new ArrayList<TestPojo>();
				testList.add(testPojo);

				dmsHandle.sendMessageToGroup("hello group!", 1, gsh.getSelectedGroupId());
//				dmsHandle.sendObjectToGroup(testPojo, 1, gsh.getSelectedGroupUuid());
//				dmsHandle.sendListToGroup(testList, TestPojo.class, 1, gsh.getSelectedGroupUuid());
//				dmsHandle.grubaDosyaGonder(Paths.get("D:/test.txt"), 1, gsh.getSeciliGrupId());

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

			DmsHandle dmsHandle = DmsCore.login("kiraz", "kiraz");

			dmsHandle.addListener(new DmsListenerImpl(dmsHandle));

			ContactSelectionHandle csh = dmsHandle.getOnlineContactsHandle();

//			JComponent mcPanel = dmsHandle.getDmsPanel();
			InetAddress localAddress = InetAddress.getByName("192.168.1.87");
			JComponent mcPanel = csh.getContactSelectionPanel(contact -> contact.getAddresses().contains(localAddress));
			JButton btn = new JButton("test");
			btn.addActionListener(e -> {

				TestPojo testPojo = new TestPojo();
				List<TestPojo> testList = new ArrayList<TestPojo>();
				testList.add(testPojo);

				dmsHandle.sendMessageToContacts("hello contact!", 1, csh.getSelectedContactIds());
//				dmsHandle.sendObjectToContacts(testPojo, 1, csh.getSelectedContactUuids());
//				dmsHandle.sendListToContacts(testList, TestPojo.class, 1, csh.getSelectedContactUuids());
//				dmsHandle.kisilereDosyaGonder(Paths.get("D:/test.txt"), 1, csh.getSeciliKisiIdler());

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

		} catch (DbException | UnknownHostException e) {

			e.printStackTrace();

		}

	}

	private void testTwo() {

		JFrame frame = new JFrame("DMS");
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

						DmsHandle dmsHandle = DmsCore.login(textField.getText(), textField.getText());

						dmsHandle.addListener(new DmsListenerImpl(dmsHandle));

						JComponent mcPanel = dmsHandle.getDmsPanel();

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

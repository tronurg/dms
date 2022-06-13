package com.ogya.dms.test.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ogya.dms.core.intf.DmsHandle;
import com.ogya.dms.core.intf.handles.ContactSelectionHandle;
import com.ogya.dms.core.intf.handles.GroupSelectionHandle;
import com.ogya.dms.core.main.DmsCore;
import com.ogya.dms.core.structures.Availability;

public class DmsTest {

	public static void main(String[] args) {

		testOne();

	}

	private static void testOne() {

		try {

			DmsHandle dmsHandle = DmsCore.login("elma", "elma");
			dmsHandle.addRemoteIps(InetAddress.getByName("192.168.1.88"), InetAddress.getByName("192.168.1.89"),
					InetAddress.getByName("192.168.1.90"));
			dmsHandle.clearRemoteIps();

			dmsHandle.addListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addGuiListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addDownloadListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.registerFileServer(new DmsListenerImpl(dmsHandle));

			JComponent mcPanel = dmsHandle.getDmsPanel();

//			dmsHandle.setComment("merhaba");
			dmsHandle.setCoordinates(-30.0, 30.0);

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

		} catch (Exception e) {

			e.printStackTrace();

		}

		try {

			DmsHandle dmsHandle = DmsCore.login("armut", "armut");

			dmsHandle.addListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addGuiListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addDownloadListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.registerFileServer(new DmsListenerImpl(dmsHandle));

			GroupSelectionHandle gsh = dmsHandle.getActiveGroupsHandle();

			JComponent mcPanel = dmsHandle.getDmsPanel();
//			JComponent mcPanel = gsh.getGroupSelectionPanel();
			JButton btn = new JButton("test");
			btn.addActionListener(e -> {

				dmsHandle.setAvailability(dmsHandle.getMyContactHandle().getAvailability() == Availability.OFFLINE
						? Availability.AVAILABLE
						: Availability.OFFLINE);

				final Long selectedGroupId = gsh.getSelectedGroupId();

				gsh.resetSelection();

				if (selectedGroupId == null)
					return;

				System.out.println("Group " + dmsHandle.getGroupHandle(selectedGroupId).getName() + " selected.");

//				TestPojo testPojo = new TestPojo();
//				List<TestPojo> testList = new ArrayList<TestPojo>();
//				testList.add(testPojo);
//
//				MessageHandle messageHandle = dmsHandle.createMessageHandle("hello contact!", 1);
////				messageHandle.setFileHandle(dmsHandle.createFileHandle(Paths.get("D:/test.txt"), 2));
//				messageHandle.setObjectHandle(dmsHandle.createObjectHandle(testPojo, 3));
//				messageHandle.setListHandle(dmsHandle.createListHandle(testList, TestPojo.class, 4));
//				try {
//					dmsHandle.sendMessageToGroup(messageHandle, selectedGroupId,
//							dmsHandle.createMessageRules().useTrackingId(124L));
//				} catch (Exception e1) {
//					e1.printStackTrace();
//				}
//
//				try {
//					System.out.println(String.format("armut: Message #%d sent to group %s\n",
//							dmsHandle.sendGuiMessageToGroup("api grup deneme", selectedGroupId).get(),
//							dmsHandle.getGroupHandle(selectedGroupId).getName()));
//				} catch (InterruptedException | ExecutionException e1) {
//					e1.printStackTrace();
//				}

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

		} catch (Exception e) {

			e.printStackTrace();

		}

		try {

			DmsHandle dmsHandle = DmsCore.login("kiraz", "kiraz");

			dmsHandle.addListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addGuiListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addDownloadListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.registerFileServer(new DmsListenerImpl(dmsHandle));

			ContactSelectionHandle csh = dmsHandle.getActiveContactsHandle();

//			JComponent mcPanel = dmsHandle.getDmsPanel();
			JComponent mcPanel = csh.getContactSelectionPanel();
			JButton btn = new JButton("test");
			final AtomicReference<Long> downloadId = new AtomicReference<Long>();
			final AtomicBoolean downloading = new AtomicBoolean();
			btn.addActionListener(e -> {
				if (downloadId.get() == null) {
					if (csh.getSelectedContactIds().isEmpty()) {
						return;
					}
					downloadId.set(dmsHandle.downloadFile(0L, csh.getSelectedContactIds().get(0)));
					downloading.set(true);
				} else if (downloading.get()) {
					dmsHandle.pauseDownload(downloadId.get());
					downloading.set(false);
				} else {
//					dmsHandle.resumeDownload(downloadId.get());
//					downloading.set(true);
					dmsHandle.cancelDownload(downloadId.getAndSet(null));
				}
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

		} catch (Exception e) {

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

					} catch (Exception e) {

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

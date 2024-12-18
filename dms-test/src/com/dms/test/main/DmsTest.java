package com.dms.test.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.dms.core.intf.DmsException;
import com.dms.core.intf.DmsHandle;
import com.dms.core.intf.handles.ContactSelectionHandle;
import com.dms.core.intf.handles.GroupSelectionHandle;
import com.dms.core.main.DmsCore;
import com.dms.passgen.DmsPassGen;
import com.dms.server.main.DmsServer;

public class DmsTest {

	public static void main(String[] args) {

		DmsPassGen.generate("./sec");

		try {
			DmsServer.setIntercomPort(5446);
			DmsServer.setMulticastGroup("234.1.1.3");
			DmsServer.setBeaconPort(5123);
			DmsServer.setServerPort(9011);
			DmsServer.setClientPortFrom(9012);
			DmsServer.setClientPortTo(9111);
			DmsServer.setBeaconIntervalMs(5000);
			DmsServer.setCertificateFolder("./sec");
			DmsServer.setIpDatFolder("./ipdat");
			DmsServer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		DmsCore.setServerIp("localhost");
		DmsCore.setServerPort(5446);
		DmsCore.setDbPath("./dms_db");
		DmsCore.setFileExplorerPath("D:/");
		DmsCore.setMaxFileLength(1000000000L);
		DmsCore.setSmallFileLimit(1000000L);
		DmsCore.setSendFolder("./sent");
		DmsCore.setReceiveFolder("./received");
		DmsCore.setAutoOpenFile(true);

		try {
			Files.list(Paths.get("./templates")).forEach(e -> DmsCore.addReportTemplate(e.toString()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		testOne();

	}

	private static void testOne() {

		try {

			DmsHandle dmsHandle = DmsCore.login("elma", "elma");
			dmsHandle.switchAudio(false);
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

			AtomicReference<DmsHandle> handleRef = new AtomicReference<DmsHandle>();
			DmsHandle dmsHandle = DmsCore.login("armut", "armut");
			dmsHandle.switchAudio(false);
			handleRef.set(dmsHandle);
			JPanel panel = new JPanel(new BorderLayout());

			dmsHandle.addListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addGuiListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addDownloadListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.registerFileServer(new DmsListenerImpl(dmsHandle));

			GroupSelectionHandle gsh = dmsHandle.getActiveGroupsHandle();

			JComponent mcPanel = dmsHandle.getDmsPanel();
//			JComponent mcPanel = gsh.getGroupSelectionPanel();
			JButton btn = new JButton("test");
			final AtomicBoolean loggedIn = new AtomicBoolean(true);
			btn.addActionListener(e -> {
				if (loggedIn.get()) {
					loggedIn.set(false);
					handleRef.get().logout();
				} else {
					try {
						DmsHandle dmsHandleNew = DmsCore.login("armut", "armut");
						handleRef.set(dmsHandleNew);
						dmsHandleNew.addListener(new DmsListenerImpl(dmsHandleNew));
						dmsHandleNew.addGuiListener(new DmsListenerImpl(dmsHandleNew));
						dmsHandleNew.addDownloadListener(new DmsListenerImpl(dmsHandleNew));
						dmsHandleNew.registerFileServer(new DmsListenerImpl(dmsHandleNew));
						SwingUtilities.invokeLater(() -> {
							panel.removeAll();
							panel.add(dmsHandleNew.getDmsPanel(), BorderLayout.CENTER);
							panel.add(btn, BorderLayout.SOUTH);
							panel.revalidate();
							panel.repaint();
						});
					} catch (DmsException e1) {
						e1.printStackTrace();
					}
					loggedIn.set(true);
				}
			});

//			new Thread(() -> {
//
//				try {
//					Thread.sleep(5000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//
//				UIManager.put("Panel.background", Color.DARK_GRAY);
//				UIManager.put("Panel.foreground", Color.LIGHT_GRAY);
//
//				SwingUtilities.invokeLater(() -> SwingUtilities.updateComponentTreeUI(mcPanel));
//
//			}).start();

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
			dmsHandle.switchAudio(false);

			dmsHandle.addListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addGuiListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.addDownloadListener(new DmsListenerImpl(dmsHandle));
			dmsHandle.registerFileServer(new DmsListenerImpl(dmsHandle));

			ContactSelectionHandle csh = dmsHandle.getActiveContactsHandle();

			JComponent mcPanel = dmsHandle.getDmsPanel();
//			JComponent mcPanel = csh.getContactSelectionPanel();
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
//					new Thread(() -> {
//						while (true) {
//							dmsHandle.pauseDownload(downloadId.get());
//							try {
//								Thread.sleep(100);
//							} catch (InterruptedException e1) {
//
//							}
//							dmsHandle.resumeDownload(downloadId.get());
//							try {
//								Thread.sleep(100);
//							} catch (InterruptedException e1) {
//
//							}
//						}
//					}).start();
				} else if (downloading.get()) {
					dmsHandle.pauseDownload(downloadId.get());
					downloading.set(false);
				} else {
					dmsHandle.resumeDownload(downloadId.get());
					downloading.set(true);
//					dmsHandle.cancelDownload(downloadId.getAndSet(null));
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

	private static void testTwo() {

		JFrame frame = new JFrame("DMS");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JTextField textField = new JTextField(30);

		textField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyTyped(KeyEvent arg0) {

				if (arg0.getKeyChar() != KeyEvent.VK_ENTER) {
					return;
				}

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

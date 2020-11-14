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

import com.aselsan.rehis.reform.dms.arayuz.DmsKontrol;
import com.aselsan.rehis.reform.dms.arayuz.DmsServis;
import com.aselsan.rehis.reform.dms.arayuz.hata.VeritabaniHatasi;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsGrupSecim;
import com.aselsan.rehis.reform.dms.arayuz.kontrol.DmsKisiSecim;

@Component(immediate = true)
public class DmsTest {

	private DmsServis dmsServis;

	@Activate
	protected void activate() {

		testOne();

	}

	@Reference
	protected void addDmsService(DmsServis dmsServis) {

		this.dmsServis = dmsServis;

	}

	protected void removeDmsService(DmsServis dmsServis) {

		this.dmsServis = null;

	}

	private void testOne() {

		try {

			DmsKontrol dmsKontrol = dmsServis.girisYap("elma", "elma");

			dmsKontrol.dinleyiciEkle(new DmsDinleyiciGercekleme(dmsKontrol));

			JComponent mcPanel = dmsKontrol.getDmsPanel();

			dmsKontrol.setAciklama("merhaba");
			dmsKontrol.setKoordinatlar(30.0, 30.0);

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

		} catch (VeritabaniHatasi e) {

			e.printStackTrace();

		}

		try {

			DmsKontrol dmsKontrol = dmsServis.girisYap("armut", "armut");

			dmsKontrol.dinleyiciEkle(new DmsDinleyiciGercekleme(dmsKontrol));

			DmsGrupSecim gsh = dmsKontrol.getDmsAktifGruplarim();

			JComponent mcPanel = dmsKontrol.getDmsPanel();
//			JComponent mcPanel = gsh.getGroupSelectionPanel();
			JButton btn = new JButton("test");
			btn.addActionListener(e -> {

				TestPojo testPojo = new TestPojo();
				List<TestPojo> testList = new ArrayList<TestPojo>();
				testList.add(testPojo);

//				dmsKontrol.sendMessageToGroup("hello group!", 1, gsh.getSelectedGroupUuid());
//				dmsKontrol.sendObjectToGroup(testPojo, 1, gsh.getSelectedGroupUuid());
//				dmsKontrol.sendListToGroup(testList, TestPojo.class, 1, gsh.getSelectedGroupUuid());
				dmsKontrol.grubaDosyaGonder(Paths.get("D:/test.txt"), 1, gsh.getSeciliGrupId());

				gsh.secimiSifirla();

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

		} catch (VeritabaniHatasi e) {

			e.printStackTrace();

		}

		try {

			DmsKontrol dmsKontrol = dmsServis.girisYap("kiraz", "kiraz");

			dmsKontrol.dinleyiciEkle(new DmsDinleyiciGercekleme(dmsKontrol));

			DmsKisiSecim csh = dmsKontrol.getDmsCevrimiciKisiler();

			JComponent mcPanel = dmsKontrol.getDmsPanel();
//			JComponent mcPanel = csh.getContactSelectionPanel();
			JButton btn = new JButton("test");
			btn.addActionListener(e -> {

				TestPojo testPojo = new TestPojo();
				List<TestPojo> testList = new ArrayList<TestPojo>();
				testList.add(testPojo);

//				dmsKontrol.sendMessageToContacts("hello contact!", 1, csh.getSelectedContactUuids());
//				dmsKontrol.sendObjectToContacts(testPojo, 1, csh.getSelectedContactUuids());
//				dmsKontrol.sendListToContacts(testList, TestPojo.class, 1, csh.getSelectedContactUuids());
				dmsKontrol.kisilereDosyaGonder(Paths.get("D:/test.txt"), 1, csh.getSeciliKisiIdler());

				csh.secimiSifirla();

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

		} catch (VeritabaniHatasi e) {

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

						DmsKontrol dmsKontrol = dmsServis.girisYap(textField.getText(), textField.getText());

						dmsKontrol.dinleyiciEkle(new DmsDinleyiciGercekleme(dmsKontrol));

						JComponent mcPanel = dmsKontrol.getDmsPanel();

						JPanel panel = new JPanel(new BorderLayout());
						panel.add(mcPanel, BorderLayout.CENTER);

						frame.setContentPane(panel);
						frame.setSize(400, 600);
						frame.setLocationRelativeTo(null);

						SwingUtilities.invokeLater(() -> frame.setVisible(true));

					} catch (VeritabaniHatasi e) {

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

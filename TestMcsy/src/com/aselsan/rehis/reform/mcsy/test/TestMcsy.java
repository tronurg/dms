package com.aselsan.rehis.reform.mcsy.test;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.aselsan.rehis.reform.mcsy.arayuz.McServisi;
import com.aselsan.rehis.reform.mcsy.arayuz.exceptions.VeritabaniHatasi;

@Component(immediate = true)
public class TestMcsy {

	private McServisi mcServisi;

	@Activate
	protected void activate() {

		try {
			JComponent mcPanel = mcServisi.login("elma", "elma").getMcPanel();

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

		} catch (VeritabaniHatasi e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			JComponent mcPanel = mcServisi.login("armut", "armut").getMcPanel();

			JFrame frame = new JFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(mcPanel, BorderLayout.CENTER);

//			mcPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));

			frame.setContentPane(panel);
			frame.setSize(400, 600);
			frame.setLocationRelativeTo(null);

			SwingUtilities.invokeLater(() -> frame.setVisible(true));

		} catch (VeritabaniHatasi e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			JComponent mcPanel = mcServisi.login("kiraz", "kiraz").getMcPanel();

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

		} catch (VeritabaniHatasi e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Reference
	protected void addMcServisi(McServisi mcServisi) {

		this.mcServisi = mcServisi;

	}

	protected void removeMcServisi(McServisi mcServisi) {

		this.mcServisi = null;

	}

}

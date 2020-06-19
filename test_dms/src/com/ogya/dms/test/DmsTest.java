package com.ogya.dms.test;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ogya.dms.intf.DmsService;
import com.ogya.dms.intf.exceptions.DbException;

@Component(immediate = true)
public class DmsTest {

	private DmsService dmsService;

	@Activate
	protected void activate() {

		try {
			JComponent mcPanel = dmsService.login("elma", "elma").getDmsPanel();

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			JComponent mcPanel = dmsService.login("armut", "armut").getDmsPanel();

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			JComponent mcPanel = dmsService.login("kiraz", "kiraz").getDmsPanel();

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
			// TODO Auto-generated catch block
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

}

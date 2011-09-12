// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.aladin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import cds.allsky.MainPanel;
import cds.tools.Util;

public class FrameAllskyTool extends JFrame {

	public Aladin aladin;
	public MainPanel mainPanel;

	private String title;

	private FrameAllskyTool(Aladin aladin) {
		super();
		Aladin.setIcon(this);
		this.aladin = aladin;
		createChaine(Aladin.getChaine());
		setTitle(title);

		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		getContentPane().setLayout(new BorderLayout(1, 1));
		getContentPane().add(createPanel(), "Center");

		setLocation(500, 100);
		pack();
	}

	private void createChaine(Chaine chaine) {
		title = chaine.getString("TITLEALLSKY");
	}

	private JPanel createPanel() {
		JPanel p = new JPanel(new BorderLayout(1, 1));
		mainPanel = new MainPanel(aladin);
		p.add(mainPanel, BorderLayout.CENTER);
		return p;
	}

	public static void display(Aladin aladin) {
		if (aladin.frameAllsky == null) aladin.frameAllsky = new FrameAllskyTool(aladin);
		aladin.frameAllsky.setVisible(true);
	}

	/** Fermeture de la fenêtre */
	public void close() {
		setVisible(false);
	}

	public void export(PlanBG plan, String exportpath) {
		String path = mainPanel.getOutputPath();
//		Plan plan;
//		int n = aladin.calque.newPlanBG(path, allskyPanel.getLabel(), null,
//				null);
//		plan = aladin.calque.getPlan(n);
		((PlanBG)plan).setBitpix(-32);
		while (!plan.isSync()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		aladin.save.saveImage(exportpath, plan, 1);

	}

	public void showPublish() {
		mainPanel.showPubTab();
	}

	public void showDisplay() {
		mainPanel.showJpgTab();
	}

//    public void setCut(double[] cut, int transfertFct)  {
//       allskyPanel.setCut(cut,transfertFct);
//   }
    public void updateCurrentCM()  {
       mainPanel.updateCurrentCM();
   }
//	public void setRestart() {
////		allskyPanel.displayReStart();
//		allskyPanel.setRestart();
//	}

//	public void setResume() {
////		allskyPanel.displayResume();
//		allskyPanel.setResume();
//	}

//	public void setDone() {
////		allskyPanel.displayDone();
//		allskyPanel.setDone();
//	}
//
//	public void initStart() {
////		allskyPanel.displayStart();
//		allskyPanel.setStart();
//	}

}

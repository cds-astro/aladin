// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import cds.tools.Util;


/**
 * This class is to show the upload frame for Tap servers
 *
 */
public class FrameSimple extends JFrame implements ActionListener, GrabItFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3541428440636805284L;
	
	Aladin aladin;
	Server server;
	JPanel buttonsPanel;
	
	protected FrameSimple() {
		super();
	}
	
	/**
	 * All you have is only one tapserver frame. 
	 * So we will need the argument during frame construction
	 * @param url 
	 * @wbp.parser.constructor
	 */
	protected FrameSimple(Aladin aladin) {
		super();
		this.aladin = aladin;
		Aladin.setIcon(this);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);
		setLocation(Aladin.computeLocation(this));
		setFont(Aladin.PLAIN);
	}
	
	/** Affichage des infos associées à un serveur */
	protected void show(Server s, String title) {
		if (s != this.server) {
			setTitle(title);
			if (this.server == null) {
				aladin.grabUtilInstance.grabItServers.add(s);
			} else {
				aladin.grabUtilInstance.removeAndAdd(this.server, s);
			}
			this.server = s;
			createFrame();
			this.server.updateWidgets(this);// to make sure grab is instantiated right
			pack();
		}
		setVisible(true);
	}
	
	/**
	 * Creates the first form for loading upload data
	 * @param server2 
	 */
	public void createFrame() {
//		this.getContentPane().setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
		this.getContentPane().removeAll();
		this.getContentPane().setBackground(Aladin.COLOR_MAINPANEL_BACKGROUND);
		this.getContentPane().add(this.server, "Center");
		buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton submit = new JButton("Submit");
		submit.addActionListener(this);
		submit.setActionCommand("SUBMIT");
		buttonsPanel.add(submit);
		
		this.getContentPane().add(buttonsPanel, "South");
		this.getContentPane().revalidate();
		this.getContentPane().repaint();
		this.getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray));
		this.getRootPane().getInsets().set(2, 2, 0, 2);
//		setSize(700, 500);
	}
	
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		if (command.equals("SUBMIT")) {
			server.submit();
		}
	}

	@Override
	public void setGrabItCoord(double x, double y) {
		GrabUtil.setGrabItCoord(aladin, server, x, y);
	}

	@Override
	public void stopGrabIt() {
	    GrabUtil.stopGrabIt(aladin, this, server);
	}

	/**
	    * Retourne true si le bouton grabit du formulaire existe et qu'il est
	    * enfoncé
	    */
	@Override
	public boolean isGrabIt() {
	      return (server.modeCoo != Server.NOMODE
	            && server.grab != null && server.grab.getModel().isSelected());
	   }

	@Override
	public void setGrabItRadius(double x1, double y1, double x2, double y2) {
		GrabUtil.setGrabItRadius(aladin, server, x1, y1, x2, y2);
	}


}

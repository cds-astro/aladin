// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

package cds.aladin;

import static cds.aladin.Constants.CLEARACTION;
import static cds.aladin.Constants.SUBMITACTION;
import static cds.aladin.Constants.RESETACTION;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import cds.tools.Util;


/**
 * This class is generic to show servers
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

	protected String CLOSE, TIPSUBMIT, TIPCLOSE;
	
	protected FrameSimple() {
		super();
	}
	
	protected void createChaine() {
	      CLOSE = Aladin.chaine.getString("CLOSE");
	      TIPSUBMIT = Aladin.chaine.getString("TIPSUBMIT");
	      TIPCLOSE = Aladin.chaine.getString("TIPCLOSE");
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
		createChaine();
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);
		setLocation(Aladin.computeLocation(this));
		setFont(Aladin.PLAIN);
		getRootPane().registerKeyboardAction(new ActionListener() {
	         public void actionPerformed(ActionEvent e) { processAnyClosingActions(); }
	      },
	      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
	      JComponent.WHEN_IN_FOCUSED_WINDOW
	            );
	      getRootPane().registerKeyboardAction(new ActionListener() {
	         public void actionPerformed(ActionEvent e) { processAnyClosingActions(); }
	      },
	      KeyStroke.getKeyStroke(KeyEvent.VK_W,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
	      JComponent.WHEN_IN_FOCUSED_WINDOW
	            );
	}
	
	/** Affichage des infos associées à un serveur */
	/*protected void show(Server s, String title) {
		if (s != this.server) {
			setTitle(title);
			if (this.server == null) {
				aladin.grabUtilInstance.grabItServers.add(s);
			} else {
				aladin.grabUtilInstance.removeAndAdd(this.server, s);
			}
			this.server = s;
			createServerFrame();
			this.server.updateWidgets(this);// to make sure grab is instantiated right
			pack();
		}
		setVisible(true);
	}*/
	
	public Dimension getPreferredSize() {
		return new Dimension(520, 450);
	}
	
	protected void show(JComponent panel, String title) {
		if (panel != null) {
			if (panel.getRootPane() == null || !panel.getParent().equals(this.getContentPane())) {
				setTitle(title);
				this.server = null;
				this.getContentPane().removeAll();
				this.getContentPane().add(panel, "Center");
				this.getContentPane().revalidate();
				this.getContentPane().repaint();
				this.getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray));
				this.getRootPane().getInsets().set(2, 2, 0, 2);
				pack();
			}
		}
		setVisible(true);
	}
	
	protected void show(Server server, String title) {
		setTitle(title);
		if (server != null) {
			if (server != this.server) {
				setTitle(title);
				if (this.server == null) {
					aladin.grabUtilInstance.grabItServers.add(server);
				} else {
					aladin.grabUtilInstance.removeAndAdd(this.server, server);
				}
				this.server = server;
				createServerFrame();
				this.server.updateWidgets(this);// to make sure grab is instantiated right
				pack();
			}
		}
		setVisible(true);
	}
	
	/**
	 * Creates the first form for loading upload data
	 * @param server2 
	 */
	public void createServerFrame() {
//		this.getContentPane().setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
		this.getContentPane().removeAll();
		this.getContentPane().setBackground(Aladin.COLOR_MAINPANEL_BACKGROUND);
		this.getContentPane().add(this.server, "Center");
		buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		
		JButton button = new JButton("Reset");
		button.addActionListener(this);
		button.setActionCommand(RESETACTION);
		buttonsPanel.add(button);
		
		button = new JButton("Clear");
		button.addActionListener(this);
		button.setActionCommand(CLEARACTION);
		buttonsPanel.add(button);
		
		button = new JButton("Submit");
		button.addActionListener(this);
		button.setActionCommand(SUBMITACTION);
		button.setToolTipText(TIPSUBMIT);
		buttonsPanel.add(button);
		
		button = new JButton(CLOSE);
		button.addActionListener(this);
		button.setActionCommand(CLOSE);
		button.setToolTipText(TIPCLOSE);
		buttonsPanel.add(button);
		
		this.getContentPane().add(buttonsPanel, "South");
		try { this.getContentPane().revalidate();
        } catch( Throwable e ) { pack(); }  // Not yet implemented in 1.6
		this.getContentPane().repaint();
		this.getRootPane().setBorder(BorderFactory.createLineBorder(Color.gray));
		this.getRootPane().getInsets().set(2, 2, 0, 2);
//		setSize(700, 500);
	}
	
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		if (this.server != null) {
			if (command.equals(SUBMITACTION)) {
				server.submit();
			} else if (command.equals(RESETACTION)) {
				server.reset();
			} else if (command.equals(CLEARACTION)) {
				server.clear();
			}
		}
		
		if (command.equals(CLOSE)) {
			processAnyClosingActions();
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
	
	@Override
	protected void processWindowEvent(WindowEvent e) {
		// TODO Auto-generated method stub
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
			processAnyClosingActions();
		super.processWindowEvent(e);
	}

	protected void processAnyClosingActions() {
		try {
			if (server != null && server instanceof ServerGlu) {
				((ServerGlu) server).cleanUpFOV();
			}
		} catch (Exception e) {
		}
		setVisible(false);
	}

}

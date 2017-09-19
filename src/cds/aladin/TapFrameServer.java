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

import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.LOADCLIENTTAPURL;
import static cds.aladin.Constants.REGISTRYPANEL;
import static cds.aladin.Constants.SUBMITPANEL;
import static cds.tools.CDSConstants.DEFAULT;
import static cds.tools.CDSConstants.WAIT;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import cds.tools.TwoColorJTable;
import cds.tools.Util;

/**
 * Created based on {@link FrameServer}
 * 
 */
public final class TapFrameServer extends JFrame implements ActionListener,KeyListener {
   
	private static final long serialVersionUID = 1L;

	static String FSSELECTTAP, INFO = " ? ", CLOSE, TIPSUBMIT, TIPCLOSE, FILTER, RESET, GO, LOAD, TIPLOAD,
			TIPRELOAD, TAPNOFILELOAD, TAPNOFILERELOAD, SELECTSERVERLABEL, SELECTASYNCPANELLABEL, SELECTSERVERTOOLTIP,
			SELECTASYNCPANELTOOLTIP, NOTAPSERVERSCONFIGUREDMESSAGE, TAPURLCLIENTFIELDLABEL, SELECTDIRTAPSERVERSTOOLTIP,
			SELECTSPLTAPSERVERSLABEL, SELECTSPLTAPSERVERSTOOLTIP, SELECTDIRTAPSERVERSLABEL, CHOOSEFROMTAPSERVERSTEXT;

   Aladin aladin;
   TapManager tapManager;
   JTextField filter=null;
   private JPanel splListPanelScroll;
   private JPanel completeListPanelScroll;
   GridBagLayout g;
   GridBagConstraints c;
   JTabbedPane tabbedTapThings;
   JPanel allRegistryPanel;
   JPanel splRegistryPanel;
   JPanel registryPanel;
   public DataLabel selectedServerLabel;
   JTabbedPane allRegistryPane;
   
   //inputs
   JTextField userProvidedTapUrl;

   protected void createChaine() {
      GO = Aladin.chaine.getString("FSGO");
      RESET = Aladin.chaine.getString("RESET");
      FILTER = Aladin.chaine.getString("FSFILTER");
      CLOSE = Aladin.chaine.getString("CLOSE");
      TIPSUBMIT = Aladin.chaine.getString("TIPSUBMIT");
      TIPCLOSE = Aladin.chaine.getString("TIPCLOSE");
      LOAD = Aladin.chaine.getString("FSLOAD");
      TIPLOAD = Aladin.chaine.getString("TIPLOAD");
      FSSELECTTAP = Aladin.chaine.getString("FSSELECTTAP");
      TIPRELOAD = Aladin.chaine.getString("TIPRELOAD");
      TAPNOFILELOAD = Aladin.chaine.getString("TAPNOFILELOAD");
      TAPNOFILERELOAD = Aladin.chaine.getString("TAPNOFILERELOAD");
      SELECTSERVERLABEL = Aladin.chaine.getString("SELECTSERVERLABEL");
      SELECTASYNCPANELLABEL = Aladin.chaine.getString("SELECTASYNCPANELLABEL");
      SELECTSERVERTOOLTIP = Aladin.chaine.getString("SELECTSERVERTOOLTIP");
      SELECTASYNCPANELTOOLTIP = Aladin.chaine.getString("SELECTASYNCPANELTOOLTIP");
      NOTAPSERVERSCONFIGUREDMESSAGE = Aladin.chaine.getString("NOTAPSERVERSCONFIGUREDMESSAGE");
      TAPURLCLIENTFIELDLABEL = Aladin.chaine.getString("TAPURLCLIENTFIELDLABEL");
      SELECTDIRTAPSERVERSTOOLTIP = Aladin.chaine.getString("SELECTDIRTAPSERVERSTOOLTIP");
      SELECTSPLTAPSERVERSLABEL = Aladin.chaine.getString("SELECTSPLTAPSERVERSLABEL");
      SELECTSPLTAPSERVERSTOOLTIP = Aladin.chaine.getString("SELECTSPLTAPSERVERSTOOLTIP");
      SELECTDIRTAPSERVERSLABEL = Aladin.chaine.getString("SELECTDIRTAPSERVERSLABEL");
      CHOOSEFROMTAPSERVERSTEXT = Aladin.chaine.getString("CHOOSEFROMTAPSERVERSTEXT");
   }

	protected TapFrameServer(Aladin aladin, TapManager tapManager){
		super();
		this.setName(REGISTRYPANEL);
		this.aladin = aladin;
		Aladin.setIcon(this);
		createChaine();
		setTitle(FSSELECTTAP);
		this.tapManager = tapManager;
		this.aladin = aladin;
		setLocation(Aladin.computeLocation(this));

		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);
		getContentPane().add(createCenterPane(), "Center");
		pack();
	}

	public Dimension getPreferredSize() {
		return new Dimension(650, 450);
	}

	private JTabbedPane createCenterPane(){
		splRegistryPanel = new JPanel();
		registryPanel = new JPanel();
		
		allRegistryPane = new JTabbedPane();
		allRegistryPanel = new JPanel();
		allRegistryPanel.setLayout(new BorderLayout(0, 0));
		allRegistryPanel.add(allRegistryPane, "Center");
		allRegistryPanel.setFont(Aladin.PLAIN);
		createRegistryPanel();
		MySplitPane asyncPanel = this.tapManager.uwsFacade.instantiateGui();

		tabbedTapThings = new JTabbedPane();
		tabbedTapThings.setFont(Aladin.PLAIN);
		
		allRegistryPane.addTab(SELECTSPLTAPSERVERSLABEL, null, splRegistryPanel, SELECTSPLTAPSERVERSTOOLTIP);
		allRegistryPane.addTab(SELECTDIRTAPSERVERSLABEL, null, registryPanel, SELECTDIRTAPSERVERSTOOLTIP);
		
		tabbedTapThings.addTab(SELECTSERVERLABEL, null, allRegistryPanel, SELECTSERVERTOOLTIP);
		tabbedTapThings.addTab(SELECTASYNCPANELLABEL, null, asyncPanel, SELECTASYNCPANELTOOLTIP);
		return tabbedTapThings;
	}
  
	public void createRegistryPanel() {
		splRegistryPanel.removeAll();
		splRegistryPanel.setLayout(new BorderLayout(0, 0));
		registryPanel.removeAll();
		registryPanel.setLayout(new BorderLayout(0, 0));

		int scrollWidth = 400;
		int scrollHeight = 350;
		
		JPanel urlClientPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.gridx = 0;
	    c.gridy = 0;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
//		this.getContentPane().add(file, c);
		c.insets = new Insets(1, 10, 1, 1);
		
		c.gridx = 0;
		c.insets = new Insets(1,1,1,1);
		c.weightx = 0.05;
		urlClientPanel.add(new JLabel(TAPURLCLIENTFIELDLABEL), c);
		
		c.gridx = 1;
		c.weightx = 0.90;
		userProvidedTapUrl = new JTextField();
		userProvidedTapUrl.setPreferredSize(new Dimension(240, Server.HAUT));
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		urlClientPanel.add(userProvidedTapUrl, c);

		JButton loadUserUrl = new JButton(LOAD);
		loadUserUrl.setActionCommand(LOADCLIENTTAPURL);
		loadUserUrl.addActionListener(this);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(1, 1, 1, 35);
		c.weightx = 0.05;
		c.gridx = 2;
		urlClientPanel.add(loadUserUrl, c);
		try {
			List<DataLabel> datalabels = tapManager.getTapServerList(-1);
			if (datalabels != null && !datalabels.isEmpty()) {
				JPanel header = new JPanel(new BorderLayout());
				header.add(urlClientPanel, "North");
				header.add(new JLabel(CHOOSEFROMTAPSERVERSTEXT), "West");
				allRegistryPanel.add(header, "North");
				
				JButton b;
				g = new GridBagLayout();
				c = new GridBagConstraints();
				
				splListPanelScroll = new JPanel(g);
				JScrollPane scroll = new JScrollPane(splListPanelScroll);
				scroll.setSize(scrollWidth, scrollHeight);
				scroll.setBackground(Color.white);
				scroll.getVerticalScrollBar().setUnitIncrement(70);

				fillWithSplRegistryServers();

				Aladin.makeAdd(splRegistryPanel, scroll, "Center");
				
				// panelScroll.setLayout(new GridLayout(0,1,0,0));
				g = new GridBagLayout();
				c = new GridBagConstraints();
				completeListPanelScroll = new JPanel(g);
				if (fillWithRegistryServers(null)) {
					JPanel check = new JPanel();
					check.add(new JLabel(FILTER + ": "));
					filter = new JTextField(15);
					check.add(filter);
					filter.addKeyListener(this);
					check.add(b = new JButton(GO));
					b.addActionListener(this);
					check.add(b = new JButton(RESET));
					b.addActionListener(this);
					registryPanel.add(check, "North");
				}
				scroll = new JScrollPane(completeListPanelScroll);
				scroll.setSize(scrollWidth, scrollHeight);
				scroll.setBackground(Color.white);
				scroll.getVerticalScrollBar().setUnitIncrement(70);
				Aladin.makeAdd(registryPanel, scroll, "Center");

				JPanel submit = new JPanel();
				submit.setName(SUBMITPANEL);
				submit.add(b = new JButton(LOAD));
				b.addActionListener(this);
				b.setToolTipText(TIPLOAD);
				submit.add(b = new JButton(CLOSE));
				b.addActionListener(this);
				b.setToolTipText(TIPCLOSE);
				Aladin.makeAdd(allRegistryPanel, submit, "South");
//				allRegistryPanel.add(submit, "South");
			} else {
				registryPanel.add(urlClientPanel, "North");
				completeListPanelScroll = new JPanel();
				completeListPanelScroll.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
				Aladin.makeAdd(registryPanel, completeListPanelScroll, "Center");
				splListPanelScroll = new JPanel();
				splListPanelScroll.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
				Aladin.makeAdd(splRegistryPanel, splListPanelScroll, "Center");
			}
		} catch (Exception e) {
			// TODO: handle exception
			registryPanel.add(urlClientPanel, "North");
			completeListPanelScroll = new JPanel();
			completeListPanelScroll.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
			Aladin.makeAdd(registryPanel, completeListPanelScroll, "Center");
			splListPanelScroll = new JPanel();
			splListPanelScroll.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
			Aladin.makeAdd(splRegistryPanel, splListPanelScroll, "Center");
		}
	}
  
	private boolean fillWithSplRegistryServers() {
		List<DataLabel> datalabels = tapManager.getTapServerList(0);
		return fillWithSplListRegistryServers(datalabels);
	}
	
	private boolean fillWithRegistryServers(String mask) {
		List<DataLabel> datalabels = tapManager.getTapServerList(1);
		return fillWithRegistryServers(datalabels, mask);
	}
	
	/**
	 * Method to create frame for all tap servers
	 * @return 
	 * 
	 * @throws Exception
	 */
	private boolean fillWithSplListRegistryServers(List<DataLabel> datalabels) {
		splListPanelScroll.removeAll();
		boolean result = false;
		if (datalabels != null && !datalabels.isEmpty()) {
			int h = 0;
			ButtonGroup radioGroup = new ButtonGroup();
			c.gridx = 0;
			c.weighty = 0.01;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.PAGE_START;

			for (DataLabel dataLabel : datalabels) {
				h++;
				dataLabel.setUi();
//				height = dataLabel.gui.getPreferredSize().height;
//				dataLabel.gui.setPreferredSize(new Dimension(330, height));
				Color bg = h % 2 == 0 ? TwoColorJTable.DEFAULT_ALTERNATE_COLOR : getBackground();

				c.gridy++;
//				c.gridwidth = 1;
				c.weightx = 1;
				c.insets.left = 5;
				dataLabel.gui.setBackground(bg);
				g.setConstraints(dataLabel.gui, c);
				radioGroup.add(dataLabel.gui);
				splListPanelScroll.add(dataLabel.gui);
			}
			c.gridy++;
			c.weighty = 0.99;
			JLabel l1 = new JLabel(" ");
			g.setConstraints(l1, c);
			splListPanelScroll.add(l1);
			result = true;
		} else {
			splListPanelScroll.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
		}
		splListPanelScroll.revalidate();
		splListPanelScroll.repaint();
		return result;
	}
	
	/**
	 * Method to create frame for all tap servers
	 * @return 
	 * 
	 * @throws Exception
	 */
	private boolean fillWithRegistryServers(List<DataLabel> datalabels, String mask) {
		completeListPanelScroll.removeAll();
		boolean result = false;
		if (datalabels != null && !datalabels.isEmpty()) {
			int h = 0;
			ButtonGroup radioGroup = new ButtonGroup();
			c.gridx = 0;
			c.weighty = 0.01;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.PAGE_START;

			for (DataLabel dataLabel : datalabels) {
				if (mask != null)
					if (!(Util.indexOfIgnoreCase(dataLabel.getLabel(), mask) >= 0
							|| (dataLabel.getValue() != null && Util.indexOfIgnoreCase(dataLabel.getValue(), mask) >= 0)
							|| (dataLabel.getDescription() != null
									&& Util.indexOfIgnoreCase(dataLabel.getDescription(), mask) >= 0))) {
						radioGroup.add(dataLabel.gui);
						continue;
					}
				h++;

				dataLabel.setUi();
//				height = dataLabel.gui.getPreferredSize().height;
//				dataLabel.gui.setPreferredSize(new Dimension(330, height));
				Color bg = h % 2 == 0 ? TwoColorJTable.DEFAULT_ALTERNATE_COLOR : getBackground();

				c.gridy++;
//				c.gridwidth = 1;
				c.weightx = 1;
				c.insets.left = 5;
				dataLabel.gui.setBackground(bg);
				g.setConstraints(dataLabel.gui, c);
				radioGroup.add(dataLabel.gui);
				completeListPanelScroll.add(dataLabel.gui);
			}
			c.gridy++;
			c.weighty = 0.99;
			JLabel l1 = new JLabel(" ");
			g.setConstraints(l1, c);
			completeListPanelScroll.add(l1);
			result = true;
		} else {
			completeListPanelScroll.add(new JLabel(NOTAPSERVERSCONFIGUREDMESSAGE));
		}
		completeListPanelScroll.revalidate();
		completeListPanelScroll.repaint();
		return result;
	}
	   
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (!(o instanceof JButton))
			return;

		String s = ((JButton) o).getActionCommand();

		// Affichage du selecteur de fichiers
		if (s.equals(INFO)) {
			for (int i = 0; i < aladin.dialog.server.length; i++) {
				if (o != aladin.dialog.server[i].statusReport)
					continue;
				aladin.dialog.server[i].showStatusReport();
			}
		} else if (s.equals(CLOSE)) {
			setVisible(false);
		} else if (s.equals(GO)) {
			go();
		} else if (s.equals(RESET)) {
			reset();
		} else if (s.equals(LOAD)) {
			try {
				Aladin.makeCursor(this, WAIT);
				this.tapManager.setSelectedServerLabel();
				if (this.selectedServerLabel != null) {
					if (this.selectedServerLabel == null || this.selectedServerLabel.getLabel() == null
							|| this.selectedServerLabel.getValue() == null) {// not a necessary condition. But adding just in case.
						this.tapManager.showTapRegistryForm();
					} else {
						this.tapManager.loadTapServer(this.selectedServerLabel.getLabel(),
								this.selectedServerLabel.getValue(), null);
						this.aladin.dialog.show(this.aladin.dialog.tapServer);
					}
				} else {
					Aladin.warning(TAPNOFILELOAD);
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				Aladin.warning(e1.getMessage());
			} finally {
				Aladin.makeCursor(this, DEFAULT);
			}
		} else if (s.equals(LOADCLIENTTAPURL)) {
			loadInputUrlServer();
		}
	}

	/**
	 * Loads tap client from the url provided by the user
	 */
	private void loadInputUrlServer() {
		try {
			Aladin.makeCursor(this, WAIT);
			if (userProvidedTapUrl.getText() != null && !userProvidedTapUrl.getText().isEmpty()) {
				URL tapUrl = new URL(userProvidedTapUrl.getText());
				Aladin.trace(3, "Will create tap client for: " + tapUrl);
				tapManager.loadTapServer(tapUrl.toString(), tapUrl.toString(), null);
				this.aladin.dialog.show(this.aladin.dialog.tapServer);
			}
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace >= 3)
				e1.printStackTrace();
			Aladin.warning("Error! please check the url provided");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			Aladin.makeCursor(this, DEFAULT);
		}
	}

	private void go() {
		String mask = filter.getText().trim();
		if (mask.length() == 0) {
			mask = null;
		}
		fillWithRegistryServers(mask);
		pack();
	}

	public void reset() {
		fillWithRegistryServers(EMPTYSTRING);
		reloadRegistryPanel();
	}
	
	public void reloadRegistryPanel() {
		if (filter!=null) {
			filter.setText(EMPTYSTRING);
		}
		splRegistryPanel.revalidate();
		splRegistryPanel.repaint();
		registryPanel.revalidate();
		registryPanel.repaint();
	}

   public void keyPressed(KeyEvent e) { }
   public void keyTyped(KeyEvent e) { }

   public void keyReleased(KeyEvent e) {
      if( e.getKeyCode()==KeyEvent.VK_ENTER ) go();
   }

}

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

import static cds.aladin.Constants.SUBMITPANEL;
import static cds.aladin.Constants.REGISTRYPANEL;
import static cds.tools.CDSConstants.DEFAULT;
import static cds.tools.CDSConstants.WAIT;
import static cds.aladin.Constants.SHOWAYNCJOBS;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cds.tools.TwoColorJTable;
import cds.tools.Util;

/**
 * Created based on {@link FrameServer}
 * 
 */
public final class TapFrameServer extends JFrame implements ActionListener,KeyListener {
   
	private static final long serialVersionUID = 1L;

	static String FSSELECTTAP,INFO = " ? ",CLOSE,
                 TIPSUBMIT,TIPCLOSE,FILTER,RESET,GO,LOAD,TIPLOAD,RELOAD,TIPRELOAD,TAPNOFILELOAD,TAPNOFILERELOAD, SELECTSERVERTOOLTIP,SELECTASYNCPANELTOOLTIP;

   Aladin aladin;
   TapManager tapManager;
   JTextField filter=null;
   private JPanel panelScroll;
   GridBagLayout g;
   GridBagConstraints c;
   protected JButton reloadServerButton;
   JTabbedPane tabbedTapThings;

   protected void createChaine() {
      GO = aladin.chaine.getString("FSGO");
      RESET = aladin.chaine.getString("RESET");
      FILTER = aladin.chaine.getString("FSFILTER");
      CLOSE = aladin.chaine.getString("CLOSE");
      TIPSUBMIT = aladin.chaine.getString("TIPSUBMIT");
      TIPCLOSE = aladin.chaine.getString("TIPCLOSE");
      LOAD = aladin.chaine.getString("FSLOAD");
      TIPLOAD = aladin.chaine.getString("TIPLOAD");
      FSSELECTTAP = aladin.chaine.getString("FSSELECTTAP");
      RELOAD = aladin.chaine.getString("FSRELOAD");
      TIPRELOAD = aladin.chaine.getString("TIPRELOAD");
      TAPNOFILELOAD = aladin.chaine.getString("TAPNOFILELOAD");
      TAPNOFILERELOAD = aladin.chaine.getString("TAPNOFILERELOAD");
      SELECTSERVERTOOLTIP = aladin.chaine.getString("SELECTSERVERTOOLTIP");
      SELECTASYNCPANELTOOLTIP = aladin.chaine.getString("SELECTASYNCPANELTOOLTIP");
   }

	protected TapFrameServer(Aladin aladin, TapManager tapManager) {
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
		return new Dimension(600, 500);
	}

	private JTabbedPane createCenterPane() {
		JPanel registryPanel = createRegistryPanel();
		JPanel asyncPanel = this.tapManager.uwsFacade.instantiateGui();

		tabbedTapThings = new JTabbedPane();
		tabbedTapThings.setFont(Aladin.PLAIN);
		tabbedTapThings.addTab("Select server", null, registryPanel, SELECTSERVERTOOLTIP);
		tabbedTapThings.addTab("Async jobs", null, asyncPanel, SELECTASYNCPANELTOOLTIP);
		return tabbedTapThings;
	}
  
   private JPanel createRegistryPanel() {
      JPanel p = new JPanel();
      p.setLayout( new BorderLayout(0,0) );

      int scrollWidth = 650;
      int scrollHeight = 500;

      JPanel check = new JPanel();
      JButton b;
      check.add(new JLabel("      "+FILTER+": "));
      filter = new JTextField(15);
      check.add(filter); filter.addKeyListener(this);
      check.add(b=new JButton(GO));     b.addActionListener(this);
      check.add(b=new JButton(RESET));  b.addActionListener(this);

      JPanel header = new JPanel( new BorderLayout());
      header.add(check,"West");

      p.add(header,"North");

      g = new GridBagLayout();
      c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;

      panelScroll = new JPanel(g);
//	      panelScroll.setLayout(new GridLayout(0,1,0,0));
      JScrollPane scroll = new JScrollPane(panelScroll);
      scroll.setSize(scrollWidth,scrollHeight);
      scroll.setBackground(Color.white);
      scroll.getVerticalScrollBar().setUnitIncrement(70);

      fillWithRegistryServers();

      Aladin.makeAdd(p,scroll,"Center");

      JPanel submit = new JPanel();
      submit.setName(SUBMITPANEL);
      submit.add(b=new JButton(LOAD));
      b.addActionListener(this);
      b.setToolTipText(TIPLOAD);
      submit.add(reloadServerButton=new JButton(RELOAD));
      reloadServerButton.addActionListener(this);
      reloadServerButton.setToolTipText(TIPRELOAD);
      submit.add(b=new JButton(CLOSE));
      b.addActionListener(this);
      b.setToolTipText(TIPCLOSE);
      p.add(submit,"South");

      return p;
   }

   /**
    * Method to create frame for all tap servers
    */
	private void fillWithRegistryServers() {
		panelScroll.removeAll();
		List<DataLabel> datalabels = tapManager.getTapServerList();

		if (datalabels != null) {
			int h = 0;
			int height;

			String mask = filter.getText().trim();
			if (mask.length() == 0)
				mask = null;
			ButtonGroup radioGroup = new ButtonGroup();

			for (DataLabel dataLabel : datalabels) {
				if (mask != null)
					if (!(Util.indexOfIgnoreCase(dataLabel.getLabel(), mask) >= 0
							|| Util.indexOfIgnoreCase(dataLabel.getValue(), mask) >= 0
							|| Util.indexOfIgnoreCase(dataLabel.getDescription(), mask) >= 0)) {
						continue;
					}
				h++;
				
				dataLabel.setUi();
				dataLabel.setUiActionForTapRegistry();
				height = dataLabel.gui.getPreferredSize().height;
				dataLabel.gui.setPreferredSize(new Dimension(330, height));
				Color bg = h % 2 == 0 ? TwoColorJTable.DEFAULT_ALTERNATE_COLOR : getBackground();

				c.gridx = 0;
				c.gridy++;
				c.gridwidth = 1;
				c.weightx = 0.6;
				c.insets.left = 5;
				dataLabel.gui.setBackground(bg);
				g.setConstraints(dataLabel.gui, c);
				radioGroup.add(dataLabel.gui);
				panelScroll.add(dataLabel.gui);
			}

			c.gridy++;
			c.gridwidth = 4;
			c.weighty = 0.9;
			JLabel l1 = new JLabel(" ");
			g.setConstraints(l1, c);
			panelScroll.add(l1);

			panelScroll.revalidate();
			panelScroll.repaint();
		}
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
			this.tapManager.setSelectedServerLabel();
			if (this.tapManager.getSelectedServerLabel() == null) {
				Aladin.warning(TAPNOFILELOAD);
				return;
			}
			loadServer();
		} else if (s.equals(RELOAD)) {
			this.tapManager.setSelectedServerLabel();
			if (this.tapManager.getSelectedServerLabel() == null) {
				Aladin.warning(TAPNOFILERELOAD);
				return;
			}
			reloadServer();
		}
	}

	/**
	 * Action on click of load button: asks tap manager downstream to load whichever 
	 * server is selected by user on the tap servers list
	 */
	private void loadServer() {
		Aladin.makeCursor(this, WAIT);
		this.tapManager.loadTapServer();
		this.aladin.dialog.show(this.aladin.dialog.tapServer);
		Aladin.makeCursor(this, DEFAULT);
	}
	
	/**
	 * Method sets whether the server can be reloaded or not
	 * Servers loaded for GLU are not 
	 * Servers which have been loaded/if load resulted in error: can be reloaded
	 * @param serverId
	 */
	public void setReload(String serverId) {
		this.reloadServerButton.setEnabled(this.tapManager.canReload(serverId));
	}
	
	/**
	 * Removes userselected server from cache and reloads it
	 */
	private void reloadServer() {
		Aladin.makeCursor(this, WAIT);
		this.tapManager.removeCurrentFromServerCache();
		this.tapManager.loadTapServer();
		this.aladin.dialog.show(this.aladin.dialog.tapServer);
		Aladin.makeCursor(this, DEFAULT);
	}
	
   private void go() {
      fillWithRegistryServers();
      pack();
   }

   private void reset() {
      filter.setText("");
      fillWithRegistryServers();
      revalidate();
	  repaint();
   }
   
   public void keyPressed(KeyEvent e) { }
   public void keyTyped(KeyEvent e) { }

   public void keyReleased(KeyEvent e) {
      if( e.getKeyCode()==KeyEvent.VK_ENTER ) go();
   }

}

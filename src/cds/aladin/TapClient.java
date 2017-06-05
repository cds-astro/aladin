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

/**
 * 
 */
package cds.aladin;
import static cds.aladin.Constants.GENERIC;
import static cds.aladin.Constants.GLU;
import static cds.aladin.Constants.TAPFORM_STATUS_NOTCREATEDGUI;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import cds.aladin.Constants.TapClientMode;
import cds.aladin.Constants.TapServerMode;

/**
 * @author chaitra
 *
 */
public class TapClient{
	
	public static String[] modeIconImages = { "gluIconV2.png", "genericIconV2.png" };//tintin remove this
	public static String modeIconImage = "settings.png";
	public static String modesLabel = "Modes";
	public static String modesToolTip = "Load a different TAP client to construct your queries";
	public static String[] modeIconToolTips = { "Click to load GLU mode - this is a customised form  generated from a GLU record", "Click to load a generic tap client" };
	public static String[] modeChoices = { GLU, GENERIC };
	public DefaultComboBoxModel model = null;
	public static String TAPGLUGENTOGGLEBUTTONTOOLTIP,RELOAD, TIPRELOAD;
	
	public TapManager tapManager;
	public String tapLabel;
	public String tapBaseUrl;
	public ServerGlu serverGlu;
	public ServerTap serverTap;
	public TapClientMode mode;
	
	
	static {
		TAPGLUGENTOGGLEBUTTONTOOLTIP = Aladin.chaine.getString("TAPGLUGENTOGGLEBUTTONTOOLTIP");
		RELOAD = Aladin.chaine.getString("FSRELOAD");
	    TIPRELOAD = Aladin.chaine.getString("TIPRELOAD");
	}
	
	public TapClient(TapClientMode mode, TapManager tapManager, String tapLabel, String tapBaseUrl) {
		// TODO Auto-generated constructor stub
		this.mode = mode;
		this.tapManager = tapManager;
		this.tapLabel = tapLabel;
		this.tapBaseUrl = tapBaseUrl;
		
		model = new DefaultComboBoxModel(modeChoices);
		model.addListDataListener(new ListDataListener() {

			@Override
			public void intervalRemoved(ListDataEvent arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void intervalAdded(ListDataEvent arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void contentsChanged(ListDataEvent arg0) {
				// TODO Auto-generated method stub
//				System.out.println("contentsChanges" + arg0.getIndex0() + "  " + arg0.getIndex1());
				try {
					String selected = (String) model.getSelectedItem();
					Server serverToDisplay = getServerToDisplay(selected);
					if (TapClient.this.mode == TapClientMode.TREEPANEL) {
						TapClient.this.tapManager.showTapPanelFromTree(TapClient.this.tapLabel, serverToDisplay);
					} else {
						TapClient.this.tapManager.showTapServerOnServerSelector(serverToDisplay);
					}
				} catch (Exception ex) {
					if (Aladin.levelTrace >= 3) ex.printStackTrace();
					Aladin.warning("Error! unable load tap server!" + ex.getMessage());
				}
			}

		});
	}
	
	public TapClient(TapClientMode mode, TapManager tapManager, String tapLabel, String tapBaseUrl, ServerGlu serverGlu, ServerTap serverTap) {
		// TODO Auto-generated constructor stub
		this(mode, tapManager, tapLabel, tapBaseUrl);
		this.serverGlu = serverGlu;
		this.serverTap = serverTap;
	}

	public JToggleButton getGluModeButtonOld() {
		JToggleButton changeModeButton = null;
		Image image = Aladin.aladin.getImagette("gluIconV5.png");
		if (image == null) {
			changeModeButton = new JToggleButton(GLU);
		} else {
			changeModeButton = new JToggleButton(new ImageIcon(image));
		}
		changeModeButton.setToolTipText(TAPGLUGENTOGGLEBUTTONTOOLTIP);
		changeModeButton.setMargin(new Insets(0, 0, 0, 0));
		if (this.serverGlu == null) {
			changeModeButton.setVisible(false);
		}
		changeModeButton.setBorderPainted(false);
		changeModeButton.setContentAreaFilled(true);
		return changeModeButton;
	}
	
	public JComboBox getGluModeButton() {
		JComboBox comboBox = new JComboBox(model);
		comboBox.setRenderer(new TapClientModesRenderer());
		comboBox.setOpaque(false);
		comboBox.setBackground(Aladin.BLUE);
		comboBox.setForeground(Aladin.BLUE);
		if (this.serverGlu == null) {
			comboBox.setVisible(false);
		}
		return comboBox;
	}
	
	public JPanel getOptionsPanel(Server server) {
		JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0 , 0));
		if (this.mode == TapClientMode.DIALOG) {
			JButton button = ServerTap.getChangeServerButton();
			button.addActionListener(server);
//			titlePanel.add(button);
			optionsPanel.add(button);
		}

		if (server.mode != TapServerMode.UPLOAD) {
			if (server instanceof ServerTap) {
				JButton reloadButton = getReloadButton();
				reloadButton.addActionListener(server);
//				titlePanel.add(reloadButton);
				optionsPanel.add(reloadButton);
			}
			
//			titlePanel.add(modeChoice);
			server.modeChoice = getGluModeButton();
			optionsPanel.add(server.modeChoice);
			
			//enable mode in servertap if it is already created
			if (this.serverGlu != null && this.serverTap != null && this.serverTap.modeChoice != null) {
				this.serverTap.modeChoice.setVisible(true);
				this.serverTap.modeChoice.revalidate();
				this.serverTap.modeChoice.repaint();
			}
		}
		return optionsPanel;
	}
	
	public static JButton getReloadButton() {
		JButton reloadButton = null;
		Image image = Aladin.aladin.getImagette("reload.png");
		if (image == null) {
			reloadButton = new JButton("Reload");
		} else {
			reloadButton = new JButton(new ImageIcon(image));
		}
		reloadButton.setBorderPainted(false);
		reloadButton.setMargin(new Insets(0, 0, 0, 0));
		reloadButton.setContentAreaFilled(true);
		reloadButton.setActionCommand(RELOAD);
		reloadButton.setToolTipText(TIPRELOAD);
		return reloadButton;
	}
	
	/**
	 * Precedence is for serverGlu. So by default serverGlu this displayed
	 * In case there is not ServerGlu configured for the tap client then 
	 * generic client is displayed
	 * @param serverType - trumps priority. if specified returns the server asked for
	 * @return
	 * @throws Exception 
	 */
	public Server getServerToDisplay(String serverType) throws Exception {
		Server resultServer = null;
		if ((serverType == null || serverType == GLU ) && this.serverGlu != null ) {
			resultServer = this.serverGlu;
		} else if (serverType == null || serverType == GENERIC ) {
			if (this.serverTap == null) {
				this.serverTap = tapManager.createAndLoadServerTap(this);
			} else if (this.serverTap.formLoadStatus == TAPFORM_STATUS_NOTCREATEDGUI) {
				this.serverTap.showloading();
				if (this.mode == TapClientMode.TREEPANEL) {
					this.serverTap.primaryColor = Aladin.COLOR_FOREGROUND;
					this.serverTap.secondColor = Color.white;
				}
				tapManager.createGenericTapFormFromMetaData(this.serverTap);
			}
			resultServer = this.serverTap;
		} else {
			Aladin.warning("Error! unable load glu tap client!");
		}
		return resultServer;
	}

	/**
	 * Reloads the generic tap client
	 * @throws Exception 
	 */
	public void reload() throws Exception {
		this.serverTap = tapManager.createAndLoadServerTap(this);
		if (this.mode == TapClientMode.TREEPANEL) {
			tapManager.showTapPanelFromTree(tapLabel, this.serverTap);
		} else {
			tapManager.showTapServerOnServerSelector(this.serverTap);
		}
	}
	
}

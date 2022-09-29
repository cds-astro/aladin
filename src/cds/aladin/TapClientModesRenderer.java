// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import static cds.aladin.Constants.GENERIC;
import static cds.aladin.Constants.GLU;
import static cds.aladin.Constants.NODE;
import static cds.aladin.Constants.OBSCORE;
import static cds.aladin.Constants.TEMPLATES;

import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class TapClientModesRenderer extends BasicComboBoxRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4372535217338044431L;
	
	TapClient tapClient;
	public static Map<String, String> tooltipMap = null;
	
	static {
		tooltipMap  = new HashMap<String, String>();
		tooltipMap.put(GLU, Aladin.chaine.getString("GLU_TAPMODESTOOLTIP"));
		tooltipMap.put(NODE, Aladin.chaine.getString("NODE_TAPMODESTOOLTIP"));
		tooltipMap.put(GENERIC, Aladin.chaine.getString("GENERIC_TAPMODESTOOLTIP"));
//		tooltipMap.put(ACCESSALL, Aladin.chaine.getString("ACCESSALL_TAPMODESTOOLTIP"));
		tooltipMap.put(TEMPLATES, Aladin.chaine.getString("EXAMPLE_TAPMODESTOOLTIP"));
		tooltipMap.put(OBSCORE, Aladin.chaine.getString("OBSCORE_TAPMODESTOOLTIP"));
	}
	
	public TapClientModesRenderer(TapClient tapClient) {
		// TODO Auto-generated constructor stub
		this.tapClient = tapClient;
	}
	
	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel option = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//		System.err.println("Mode: "+list.getSelectedValue()+" isSelected "+isSelected+" tapclient model: "+tapClient.model.getSelectedItem());
		if (index < 0) {
			setText("Mode");
			option.setEnabled(true);
			 if (tapClient.model != null && tapClient.model.getSelectedItem() != null) {
		            if (tapClient.model.getSelectedItem() != null ) {
		            	setText("Mode: "+tapClient.model.getSelectedItem());
			        }
		        }
		} else if (value == GLU && this.tapClient.serverGlu == null) {//not removing here
			makeDisabled(option, value, TapClient.NOGLURECFOUND);
			option.setSize(0,0);
		} else if (value == OBSCORE && this.tapClient.obscoreTables.isEmpty()) {
			makeDisabled(option, value, "Obscore client");
		}  else {
			String tooltip = null;
			String valueTxt = (String) value;
			setText(valueTxt);
			if (valueTxt.equalsIgnoreCase(this.tapClient.nodeName)) {
				tooltip = tooltipMap.get(NODE);
			} else {
				tooltip = tooltipMap.get(value);
			}
			setToolTipText(tooltip);
			setIcon(null);
			option.setEnabled(true);
		}
//		setPreferredSize(new Dimension(45, Server.HAUT));
		setPreferredSize(new Dimension(105, Server.HAUT));
		return this;
	}
	
	public void makeDisabled(JLabel option, Object value, String tooltip) {
		setText((String) value);
		setIcon(null);
		option.setVisible(false);
		option.setEnabled(false);
		setToolTipText(tooltip);
	}

}

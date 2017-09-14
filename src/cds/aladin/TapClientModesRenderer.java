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

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

class TapClientModesRenderer extends BasicComboBoxRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4372535217338044431L;
	
	TapClient tapClient;

	public TapClientModesRenderer(TapClient tapClient) {
		// TODO Auto-generated constructor stub
		this.tapClient = tapClient;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel option = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (index < 0) {
			setText("Mode");
			option.setEnabled(true);
		} else if (index == 0 && this.tapClient.serverGlu == null) {
			makeDisabled(option, value, TapClient.NOGLURECFOUND);
		} else if (index == 3 && this.tapClient.obscoreTables.isEmpty()) {
			makeDisabled(option, value, "Obscore client");
		}  else {
			setText((String) value);
			setToolTipText(TapClient.modeIconToolTips[index]);
			setIcon(null);
			option.setEnabled(true);
		}
		setPreferredSize(new Dimension(45, Server.HAUT));
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
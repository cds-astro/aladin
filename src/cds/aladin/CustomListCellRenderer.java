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
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class CustomListCellRenderer extends JLabel implements ListCellRenderer {
	
	protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
	
	private Map<String, CustomListCell> model; 
	
	public CustomListCellRenderer() {
		// TODO Auto-generated constructor stub
	}
	
	public CustomListCellRenderer(Map<String, CustomListCell> model) {
		// TODO Auto-generated constructor stub
		this.model = model;
	}
	

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel column = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected,
				cellHasFocus);
		String textToSet = null;
		String tooltip = null;
		
		if (value instanceof TapTableColumn) {
			textToSet = ((TapTableColumn) value).getColumn_name();
			tooltip = ((TapTableColumn) value).getDescription();
		} else if (model != null && model.get(value) != null) {
//			textToSet = model.get(value).label;
			tooltip = model.get(value).tooltip;
		}
		StringBuffer texter = new StringBuffer("<html><p>").append(textToSet).append("</p></html>");
    	column.setPreferredSize(new Dimension(150, Server.HAUT));//25(Server.HAUT) renders a little bit small. Had it at 28, but windows shows lot of height.
		
		if (textToSet != null) {
			column.setText(texter.toString());
		}
		
		if (tooltip == null || tooltip.isEmpty()) {
			column.setToolTipText(null);
		} else {
			texter = new StringBuffer("<html><p width=\"500\">").append(tooltip).append("</p></html>");
			column.setToolTipText(tooltip);
		}
		
		return column;
	}

}

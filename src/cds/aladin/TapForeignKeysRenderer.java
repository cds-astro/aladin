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

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class TapForeignKeysRenderer extends BasicComboBoxRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4372535217338044431L;
	
	public TapForeignKeysRenderer() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if (value instanceof ForeignKeyColumn) {
			ForeignKeyColumn forRel = (ForeignKeyColumn) value;
			StringBuffer textToSet = new StringBuffer(forRel.getTarget_column());
			textToSet.append(" = ").append(forRel.getFrom_column());
			setText(textToSet.toString());
		}
		setPreferredSize(new Dimension(45, Server.HAUT));
		return this;
	}
	

}

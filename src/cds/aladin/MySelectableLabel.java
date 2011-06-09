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

/*
 * Created on 29-Nov-2005
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import cds.tools.Util;

/** Un Label sélectionnable par simple click
 * (avec copie du texte correspondant dans le presse-papiers)
 * @author Thomas Boch[CDS]
 */
public class MySelectableLabel extends Label {

	// dernier label à avoir été sélectionné
	static private MySelectableLabel lastSelected;
	
	// couleur lorsqu'on passe sur un label
	static private Color HOVER_COLOR = new Color(232,242,254);
	// couleur de sélection d'un label
	static private Color SELECTION_COLOR = new Color(60,107,222);
	
	// les couleurs d'origine
	private Color orgBkgdColor, orgFntColor;
	
	/** Constructeur
	 * 
	 * @param s texte du label
	 */
	public MySelectableLabel(String s) {
		super(s);
		
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				if( orgBkgdColor==null ) {
					orgBkgdColor = getBackground();
					orgFntColor = getForeground();
				}
				
				Component selected = (MySelectableLabel)me.getComponent();
				
				if( lastSelected!=null ) lastSelected.restoreOrgColors();
				
				if( lastSelected==null || selected!=lastSelected  ) {
					lastSelected = (MySelectableLabel)me.getComponent();
					
					setBackground(getSelectionColor());
					setForeground(Util.getReverseColor(orgFntColor));
					
					Aladin.copyToClipBoard(getText());
				}
				else lastSelected = null;
			}
			
			public void mouseEntered(MouseEvent me) {
				if( orgBkgdColor==null ) orgBkgdColor = getBackground();
				
				if( lastSelected==null || me.getComponent()!=lastSelected )
					setBackground(HOVER_COLOR);
				
				Aladin.makeCursor(me.getComponent(), Aladin.TEXTCURSOR);
			}
			
			public void mouseExited(MouseEvent me) {
				if( lastSelected==null || me.getComponent()!=lastSelected )
					restoreOrgColors();
				
				Aladin.makeCursor(me.getComponent(), Aladin.DEFAULT);
			}
		});
	}
	
	private Color getSelectionColor() {
		return SELECTION_COLOR;
	}
	
	/**
	 * remet les couleurs d'origine
	 *
	 */
	private void restoreOrgColors() {
		setBackground(orgBkgdColor);
		setForeground(orgFntColor);
	}
	
}

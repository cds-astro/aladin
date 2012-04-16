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

import cds.tools.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.*;

/**
 * Gestion de la fenêtre d'affichage des infos sur les tables d'un plan catalogue
 * @date jan 2009 - création
 * @author P. Fernique
 */
public class FrameInfoTable extends JFrame {

	private Aladin aladin;
	private Plan plan;

	protected FrameInfoTable(Aladin aladin,Plan plan) {
	   super();
	   this.aladin = aladin;
	   this.plan = plan;
       Aladin.setIcon(this);
       setTitle(plan.getLabel());

       enableEvents(AWTEvent.WINDOW_EVENT_MASK);
       Util.setCloseShortcut(this, false, aladin);
       
       // Le panel general
       JPanel pgen = (JPanel)getContentPane();

       // Le panel des tables
	   JPanel p = new JPanel( new GridLayout(0,1,5,5) );
	   JScrollPane sc = new JScrollPane(p);
	   Vector legs = plan.getLegende();
	   boolean multiTable = plan.getNbTable()>1;
	   Enumeration e = legs.elements();
	   for( int i=1; e.hasMoreElements(); i++ ) {
	      Legende leg = (Legende)e.nextElement();
	      JPanel p1 = new JPanel( new BorderLayout(5,5) );
          if( multiTable )  p1.setBorder(BorderFactory.createTitledBorder(leg.name));
          else p1.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	      p1.add( leg.getTablePanel(aladin,plan), BorderLayout.CENTER);
	      p.add(p1);
	   }
	   pgen.add(sc,BorderLayout.CENTER);
	   
	   // Le Panel des boutons de controle
	   JPanel p2 = new JPanel();
	   JButton b;
	   if( plan.pcat!=null && plan.pcat.hasCatalogInfo() ) {
          b = new JButton(aladin.chaine.getString("PARSING"));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { seeCatalogInfo(); }
          });
          p2.add(b);
          
          b = new JButton(aladin.chaine.getString("COORDCOLUMN"));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { seeCoordColumnInfo(); }
          });
          p2.add(b);
	   }
	   b = new JButton(aladin.chaine.getString("CLOSE"));
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { dispose(); }
       });
	   p2.add(b);
	   pgen.add(p2,BorderLayout.SOUTH);
	   
       setLocation(aladin.computeLocation(this));
       pack();
       setVisible(true);
	}
	
	private void seeCoordColumnInfo() {
	   aladin.info(this, Util.fold( aladin.chaine.getString("COORDCOLUMNDETAIL"),50));
	}
	
	private void seeCatalogInfo() { plan.pcat.seeCatalogInfo(); }
}

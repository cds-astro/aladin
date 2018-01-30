// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import cds.tools.Util;
import cds.xml.Field;

/**
 * Gestion de la fenêtre d'affichage des infos sur les tables d'un plan catalogue
 * @date jan 2009 - création
 * @author P. Fernique
 */
public class FrameInfoTable extends JFrame {

	private Aladin aladin;
	private Plan plan;
	private JTextField epochField;

	protected FrameInfoTable(Aladin aladin,final Plan plan) {
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
	   Vector<Legende> legs = plan.getLegende();
	   boolean multiTable = plan.getNbTable()>1;
	   Enumeration<Legende> e = legs.elements();
	   while( e.hasMoreElements() ) {
	      Legende leg = e.nextElement();
	      JPanel p1 = new JPanel( new BorderLayout(5,5) );
          if( multiTable )  p1.setBorder(BorderFactory.createTitledBorder(leg.name));
          else p1.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	      p1.add( leg.getTablePanel(aladin,this,plan), BorderLayout.CENTER);
	      p.add(p1);
	   }
	   pgen.add(sc,BorderLayout.CENTER);
	   
	   // Le Panel des boutons de controle
	   JPanel p2 = new JPanel();
	   JButton b;
	   if( plan.pcat!=null && plan.pcat.hasCatalogInfo() ) {
	      
          b = new JButton(" ^ ");
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { up(); }
          });
          p2.add(b);
          
          b = new JButton(" v ");
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { down(); }
          });
          p2.add(b);
         

	      
          b = new JButton(aladin.chaine.getString("CHECK"));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { check(true); }
          });
          p2.add(b);
          
          b = new JButton(aladin.chaine.getString("UNCHECK"));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { check(false); }
          });
          p2.add(b);
          
          p2.add(new JLabel(" - "));
	      
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
          
          if( !multiTable ) {
             String epoch;
             try {
                epoch = "J"+plan.getOriginalEpoch().getJyr();
             } catch( Exception e1 ) { epoch="J2000"; }
             final String oEpoch = epoch;
             epochField = new JTextField(epoch);
             final JTextField t = epochField;
             p2.add( new JLabel( aladin.chaine.getString("PROPEPOCH")+" "));
             p2.add(t);
             t.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                   String s = t.getText();
                   if( !plan.modifyOriginalEpoch(s) ) t.setText(oEpoch);
                }
             });
          }

          p2.add(new JLabel(" - "));

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
	
	// Affiche/cache tous les champs
	private void check(boolean flag) { 
       Vector<Legende> legs = plan.getLegende();
       Enumeration<Legende> e = legs.elements();
       while( e.hasMoreElements() ) {
          Legende leg = e.nextElement();
          for( Field f : leg.field ) f.visible=flag;
          leg.fireTableDataChanged();
       }
       aladin.mesure.redisplay();
	}
	
	
	protected void epochFieldActivate( boolean flag ) { epochField.setEnabled(flag); }
	
	// Remonte d'un cran l'affichage de la ligne sélectionnée
    private void up()   { upDown(-1); }
    
    // Descend d'un cran l'affichage de la ligne sélectionnée
    private void down() { upDown(+1); }
    
    // Remonte ou descend d'un cran l'affichage de la ligne sélectionnée
    // LE CODE EST DEJA PREVU POUR SUPPORTER DE MULTI-SELECTION (SUPPRIMER LES BREAK)
    private void upDown(int sens) {
	   boolean trouve=false;
       Vector<Legende> legs = plan.getLegende();
       Enumeration<Legende> e = legs.elements();
       while( e.hasMoreElements() ) {
          Legende leg = e.nextElement();
          boolean trouve0 = false;
          JTable table = leg.getTable();
          int pos=-1;
          for( int oos : table.getSelectedRows() ) {
             if( pos==-1 ) pos =oos;
             trouve0 |= leg.upDown( oos,sens );
             trouve |= trouve0;
             break;
          }
          if( trouve0 ) {
             leg.fireTableDataChanged();
             if( pos!=-1 ) table.setRowSelectionInterval(pos+sens, pos+sens);
             break;
          }
       }
       if( trouve ) aladin.mesure.redisplay();
	}
}

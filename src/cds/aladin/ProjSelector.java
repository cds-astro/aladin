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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cds.tools.Util;

/** Gère le sélecteur global de la projection courante. Ce widget apparait dans l'interface
 * principale à droite du champ de localisation, sous le menu.
 * @author Pierre Fernique [DS]
 * @version 1.0 Dec 2016 - création pour la version 10 d'Aladin
 */
public class ProjSelector extends JPanel {
   private Aladin aladin;
   private JComboBox<String> combo;
   private ActionListener actionListener;


   protected ProjSelector(Aladin aladin) {
      this.aladin = aladin;

      // Construction du Panel (label + selector)
      JLabel lab = new JLabel( aladin.chaine.getString("PROPPROJ")+" " );
      String tip = aladin.chaine.getString("PROPPROJTIP");
      Util.toolTip(lab, tip);
      lab.setFont(lab.getFont().deriveFont(Font.BOLD));
      lab.setForeground(Aladin.COLOR_LABEL);

      String [] list = Projection.getAlaProj();
      combo = new JComboBox<>( list );
      combo.setUI( new MyComboBoxUI());
      combo.setMaximumRowCount(list.length);
      combo.setFont(Aladin.PLAIN);
      Util.toolTip(combo, tip);
      
      // Positionnement de la projection par défaut
      String s = aladin.configuration.getProj();
      initProjection(s);
      actionListener=new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            submitProjection( (String)combo.getSelectedItem());
         }
      };
      combo.addActionListener( actionListener);
      combo.setPrototypeDisplayValue("12345678");
      
      GridBagLayout g;
      JPanel pCombo = new JPanel( g=new GridBagLayout() );
      pCombo.setBackground( aladin.getBackground() );
      GridBagConstraints gc = new GridBagConstraints();
      gc.fill = GridBagConstraints.HORIZONTAL;
      pCombo.add(combo,gc);
      
      setLayout(new BorderLayout(7,7));
      setBackground( aladin.getBackground() );
      add( lab, BorderLayout.WEST);
      add( pCombo, BorderLayout.CENTER);
      
      setEnabled(false);
   }
   
   
   
   public void setEnabled( boolean enabled ) {
      combo.setEnabled( enabled );
   }
   
   /** Affiche une projection spécifique sur la combo, sans entrainer une action */
   protected void setProjectionSilently(String s) {
      combo.removeActionListener(actionListener);
      setProjection(s);
      combo.addActionListener(actionListener);
   }
   
   /** Met en place une projection spécifique sur la combo, et l'applique sur tous les
    * plans concernés */
   protected void setProjection(String s) { 
      int index = Projection.getAlaProjIndex(s);
      if( index<0 ) return;
      combo.setSelectedIndex(index);
   }

   /** Change la projection de tous les plans HiPS exceptés ceux qui ont une
    * projection particulière 
    * @param s nom de la projection (Projection.alaProj[])
    */
   protected void submitProjection(String s) {
      aladin.calque.modifyProjection(s);
   }
   
   /** Positionne la projection du selecteur */
   protected void initProjection(String s) {
      combo.setSelectedItem(s);
   }

   /** Retourne le code de la projection courante */
   protected int getProjType() {
      int i= Projection.getAlaProjIndex( (String)combo.getSelectedItem() );
//      String calibProj = Projection.alaProjToType[i];
      String calibProj = Projection.getProjType(i);
      i=Calib.getProjType(calibProj);
      return i;
   }
   
   /** Retourne le nom (code 3 lettres) de la projection courante */
   protected String getProjName() {
      return Calib.getProjName( getProjType() );
   }
   
   /** Retourne le nom de la projection courante */
   protected String getProjItem() { return (String)combo.getSelectedItem(); }
}

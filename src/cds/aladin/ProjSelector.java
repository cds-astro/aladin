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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Gère le sélecteur de la projection courante. Ce widget apparait dans l'interface
 * principale à droite du champ de localisation, sous le menu.
 * @author Pierre Fernique [DS]
 * @version 1.0 Dec 2016 - création pour la version 10 d'Aladin
 */
public class ProjSelector extends JPanel {
   private Aladin aladin;
   private JComboBox<String> combo;

   protected ProjSelector(Aladin aladin) {
      this.aladin = aladin;

      // Construction du Panel (label + selector)
      JLabel lab = new JLabel( aladin.chaine.getString("PROPPROJ")+" " );
      lab.setFont(lab.getFont().deriveFont(Font.BOLD));
      lab.setForeground(Aladin.COLOR_LABEL);

      String [] list = Projection.getAlaProj();
      combo = new JComboBox<String>( list );
      combo.setUI( new MyComboBoxUI());
      combo.setMaximumRowCount(list.length);
      combo.setFont(Aladin.PLAIN);
      
      // Positionnement de la projection par défaut
      String s = aladin.configuration.getProj();
      initProjection(s);
      
      combo.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            submitProjection( (String)combo.getSelectedItem());
         }
      });
      combo.setPrototypeDisplayValue("12345678");
      
//      JPanel pCombo = new JPanel( new FlowLayout(FlowLayout.LEFT,0,0));
//      pCombo.setBackground( aladin.getBackground() );
//      pCombo.add(combo);
      
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
   }
   
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
      String calibProj = Projection.alaProjToType[i];
      i=Calib.getProjType(calibProj);
      return i;
   }
}

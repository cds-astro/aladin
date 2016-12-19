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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** G�re le s�lecteur de la projection courante. Ce widget apparait dans l'interface
 * principale � droite du champ de localisation, sous le menu.
 * @author Pierre Fernique [DS]
 * @version 1.0 Dec 2016 - cr�ation pour la version 10 d'Aladin
 */
public class ProjSelector extends JPanel {
   private Aladin aladin;
   private JComboBox<String> combo;

   protected ProjSelector(Aladin aladin) {
      this.aladin = aladin;

      // Construction du Panel (label + selector)
      setLayout(new BorderLayout(0,0));
      setBackground( aladin.getBackground() );
      JLabel lab = new JLabel( aladin.chaine.getString("PROPPROJ")+" " );
      lab.setFont(Aladin.BOLD);
      lab.setForeground(Aladin.DARKBLUE);

      add( lab, BorderLayout.WEST);
      combo = new JComboBox<String>( Projection.getAlaProj() );
      
      // Positionnement de la projection par d�faut
      String s = aladin.configuration.getProj();
      initProjection(s);
      
      combo.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            submitProjection( (String)combo.getSelectedItem());
         }
      });
      combo.setPrototypeDisplayValue("12345678");
      add( combo, BorderLayout.CENTER);
   }
   
   /** Change la projection de tous les plans HiPS except�s ceux qui ont une
    * projection particuli�re 
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

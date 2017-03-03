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

import java.awt.Graphics;

/**
 * Bouton pour gérer les filtres de collection
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (jan 2017) Creation
 */
public class IconFilter extends MyIcon {
   static final int L = 12;      // Taille d'un logo
   private boolean activated=false;
   private String title;

  /** Creation */
   protected IconFilter(Aladin aladin) {
      super(aladin,32,24);
      title = "filter";
   }
   
   // Barres horizontales du dessin 
   static final private int TX[][] = {
      {0, 9,21},
      {1, 9,10},  {1, 20,21}, 
      {2, 10,20},
      {3, 11,19}, 
      {4, 12,18},
      {5, 13,17},
      {6, 14,16},
      {7, 14,16},
      {8, 14,16},
      {9, 14,16},
      {10,14,16},
      {11,14,15},
      {12,14,14},
   };
   
  
   /** Dessine l'icone  */
   protected void drawIcon(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   /** Retourne true si le scanning des collections sélectionnées peut être lancé */
   protected boolean isAvailable() {
      return aladin.directory.hasCollections() && aladin.directory.hasFilter();
   }
   
   /** Activation du filtrage */
   protected void setActivated(boolean flag ) {
      activated=flag;
      repaint();
   }
   
   /** Retourne true si le scanning de certaines collections est en cours */
   protected boolean isActivated() { return activated; }

   /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x = 3;
      int y = 1;

      g.setColor( getLogoColor() );
      drawIcon(g,x,y);
      
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      
      g.drawString(title,W/2-g.getFontMetrics().stringWidth(title)/2,H-2);
   }
   
   protected void submit() {
      if( !isAvailable() ) return;
      activated = !activated;
      if( activated ) {
         int i = aladin.directory.comboFilter.getSelectedIndex();
         if( i>0 ) {
            aladin.directory.filtre( (String)aladin.directory.comboFilter.getSelectedItem() );
            return;
         }
      }
      aladin.directory.doFiltre();
   }
      
   protected String getHelpTip() { return "Activate/suspend the filter"; } //aladin.chaine.getString("SCANTIP"); }
   protected String Help()       { return aladin.chaine.getString("Scan.HELP");  }

}

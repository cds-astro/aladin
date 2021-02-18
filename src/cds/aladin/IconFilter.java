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

import java.awt.Graphics;

import javax.swing.SwingUtilities;

/**
 * Bouton pour gérer les filtres de collection
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (jan 2017) Creation
 */
public class IconFilter extends Icon {
   static final int L = 12;      // Taille d'un logo
   private String title;

  /** Creation */
   protected IconFilter(Aladin aladin) {
      super(aladin,32,24);
      title = aladin.getChaine().getString("DTFILTER");
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
      return isActivated();
   }
   
   /** Retourne true si le scanning de certaines collections est en cours */
   protected boolean isActivated() {
      return aladin.directory.hasCollections() && aladin.directory.hasFilter();
   }

   /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x = 3+DX;
      int y = 1;

      g.setColor( getLogoColor() );
      drawIcon(g,x,y);
      
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      
      g.drawString(title,W/2-g.getFontMetrics().stringWidth(title)/2,H-2);
   }
   
   protected void submit() {
      if( !isAvailable() ) return;
      
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            aladin.directory.fullReset();
         }
      });

   }
      
   protected String getHelpTip() { return aladin.chaine.getString("DTFILTERTIP"); }
   protected String getHelpKey() { return "Filter.HELP"; }
}

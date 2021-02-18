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

/**
 * Bouton pour "trier" l'arbre des collections
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (jan 2018 ) Creation
 */
public class IconSort extends Icon {
   static final int L = 12;      // Taille d'un logo
   static String SORT;
   
  /** Creation */
   protected IconSort(Aladin aladin) {
      super(aladin,32,24);
      SORT= aladin.chaine.getString("SORT");
   }
   
   // Barres horizontales du dessin 
   static final private int TX[][] = {
                   {0,3,3},
         {1,2,4},                    {1,9,9},
         {2,1,1},  {2,3,3}, {2,5,5}, {2,9,9},
                   {3,3,3},          {3,9,9},
                   {4,3,3},          {4,9,9},
                   {5,3,3},          {5,9,9},
                   {6,3,3},          {6,9,9},
                   {7,3,3},          {7,9,9},
                   {8,3,3}, {8,7,7}, {8,9,9}, {8,11,11},
                   {9,3,3},          {9,8,10},
                                     {10,9,9},
    };
   
   
   /** Dessine l'icone  */
   private void drawLogo(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   protected boolean isAvailable() { return !aladin.directory.isFree(); }

//   protected boolean isAvailable() {  return aladin.directory.isSortable(); }
   
   protected boolean isActivated() { return false; }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x = 10+DX;
      int y = 3;
      
      if( isAvailable() ) {
         g.setColor( getFillInColor() );
         g.fillRect(x,y,9,9);
      }
      
      g.setColor( getLogoColor() );
      drawLogo(g,x,y);
      
      // Label
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      String s = SORT;
      g.drawString(s,W/2-g.getFontMetrics().stringWidth(s)/2,H-2);
   }
   
   protected void submit() {
      if( !isAvailable() ) return;
      aladin.directory.triGlobal( getParent(), 5,5);
      repaint();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("SORTTIP"); }
   protected String getHelpKey() { return "Sort.HELP"; }
}

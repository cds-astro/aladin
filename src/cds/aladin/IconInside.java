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

import java.awt.Graphics;

/**
 * Bouton pour ne conserver que les collections visibles dans le champ de vue courant
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (déc 2016) Creation
 */
public class IconInside extends MyIcon {
   static final int L = 12;      // Taille d'un logo
   static String INSIDE;
   
   private boolean activated=false;

  /** Creation */
   protected IconInside(Aladin aladin) {
      super(aladin,32,24);
      INSIDE=  aladin.chaine.getString("INSIDE");
   }
   
//   // Barres horizontales du dessin  (un sécateur)
//   static final private int TX[][] = {
//         {0,2,7},  {0,11,12},
//         {1,0,8},  {1,10,17},
//         {2,1,1},  {2,9,10},   {2,13,16},
//         {3,8,9},  {3,11,15},
//         {4,0,8},  {4,12,14},
//         {5,1,7},   };
   
   // Barres horizontales du dessin (cadre et une flèche pointant à l'intérieur)
   static final private int TX[][] = {
         {0,0,13},
         {1,0,0},             {1,13,13},
         {2,0,0},             {2,13,13},
         {3,0,0},             {3,13,13},
         {4,0,0},             {4,13,13},
         {5,0,0},  {5,6,6},   {5,13,13},
         {6,0,0},  {6,5,7},   {6,13,13},
         {7,0,0},  {7,4,8},   {7,13,13},
         {8,0,0},  {8,5,7},   {8,13,13},
         {9,0,2},  {9,5,7},   {9,11,13},
                  {10,5,7},  
                  {11,5,7},  
         };
   
  
   /** Dessine l'icone  */
   protected void drawElague(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   protected boolean isAvailable() {
      return !aladin.view.isFree() && !aladin.directory.isFree();
   }
   
   /** Retourne true si l'élagage de l'arbre HiPS est activé */
   protected boolean isActivated() { return activated; }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x = 10;
      int y = 1;
      boolean isAvailable = isAvailable();
      
      if( isAvailable ) {
         g.setColor( getFillInColor() );
         g.fillRect(x, y, 13, 10);
      }

      g.setColor( getLogoColor() );
      drawElague(g,x,y);
      
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      g.drawString(INSIDE,W/2-g.getFontMetrics().stringWidth(INSIDE)/2,H-2);
   }
   
   protected void submit() {
      if( !isAvailable() ) return;
      activated = !activated;
      aladin.directory.resumeTree();
      repaint();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("INSIDETIP"); }
   protected String Help()       { return aladin.chaine.getString("Inside.HELP");  }

}

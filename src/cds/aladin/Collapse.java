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

import java.awt.Color;
import java.awt.Graphics;

/**
 * Bouton pour "développer/refermer" l'arbre HiPS
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (déc 2016) Creation
 */
public class Collapse extends MyIcon {
   static final int L = 12;      // Taille d'un logo
   static String COLLAPSE,EXPAND;
   
  /** Creation */
   protected Collapse(Aladin aladin) {
      super(aladin,32,24);
      COLLAPSE= aladin.chaine.getString("COLLAPSE");
      EXPAND =aladin.chaine.getString("EXPAND");
   }
   
   // Barres horizontales du dessin 
   static final private int TX[][] = {
         {0,0,8},
         {1,0,0},  {1,8,8},
         {2,0,0},  {2,4,4}, {2,8,8},
         {3,0,0},  {3,4,4}, {3,8,8},
         {4,0,0},  {4,2,6}, {4,8,9}, {4,11,11}, {4,13,13},
         {5,0,0},  {5,4,4}, {5,8,8},
         {6,0,0},  {6,4,4}, {6,8,8},
         {7,0,0},  {7,8,8},
         {8,0,8},
    };
   
   
   /** Dessine l'icone  */
   protected void drawLogo(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   private boolean isAvailable() {
      return !aladin.directory.isFree();
   }
   private boolean isMouseIn()   { return in; }
   
   /** Retourne true si l'élagage de l'arbre HiPS est activé */
   protected boolean isActivated() { return true; }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      g.setColor( getBackground());
      g.fillRect(0,0,W,H);
      int x = 10;
      int y = 3;
      
      if( isAvailable() ) {
         g.setColor( isMouseIn() ? Aladin.MYBLUE : Color.white );
         g.fillRect(x,y,9,9);
      }
      
      g.setColor( !isAvailable() ?  ( isMouseIn() ? Aladin.MYBLUE : Aladin.MYGRAY) : Color.black );
      drawLogo(g,x,y);
      
      // Label
      g.setColor(isAvailable() ? Color.black : Aladin.MYGRAY);
      g.setFont(Aladin.SPLAIN);
      String s = aladin.directory.isDefaultExpand() ? EXPAND : COLLAPSE;
      g.drawString(s,W/2-g.getFontMetrics().stringWidth(s)/2,H-2);
   }
   
   protected void submit() {
      if( !isAvailable() ) return;
      aladin.directory.collapseAllExceptCurrent();
      repaint();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("COLLAPSETIP"); }
   protected String Help()       { return aladin.chaine.getString("Collapse.HELP");  }

}

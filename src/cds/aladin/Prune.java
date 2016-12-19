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
 * Bouton pour "tailler" l'arbre HiPS
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (déc 2016) Creation
 */
public class Prune extends MyIcon {
   static final int L = 12;      // Taille d'un logo
   static String ELAGUE;
   
   private boolean activated=false;

  /** Creation */
   protected Prune(Aladin aladin) {
      super(aladin,32,24);
      ELAGUE=  aladin.chaine.getString("PRUNE");
   }
   
   // Barres horizontales du dessin 
   static final private int TX[][] = {
         {0,2,7},  {0,11,12},
         {1,0,8},  {1,10,17},
         {2,1,1},  {2,9,10},   {2,13,16},
         {3,8,9},  {3,11,15},
         {4,0,8},  {4,12,14},
         {5,1,7},   };
   
   
   /** Dessine l'icone  */
   protected void drawElague(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   private boolean isAvailable() {
      return !aladin.view.isFree() && !aladin.hipsStore.isFree();
   }
   private boolean isMouseIn()   { return in; }
   
   
   /** Retourne true si l'élagage de l'arbre HiPS est activé */
   protected boolean isActivated() { return activated; }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      g.setColor( getBackground());
      g.fillRect(0,0,W,H);
      int x = 10;
      int y = 5;
      
      g.setColor( !isAvailable() ?  ( isMouseIn() ? Aladin.MYBLUE : Aladin.MYGRAY) : isActivated() ? Color.red : Color.black );
      drawElague(g,x,y);
      
      // Label
      g.setColor(isAvailable() ? Color.black : Aladin.MYGRAY);
      g.setFont(Aladin.SPLAIN);
      g.drawString(ELAGUE,W/2-g.getFontMetrics().stringWidth(ELAGUE)/2,H-2);
   }
   
   protected void submit() {
      if( !isAvailable() ) return;
      activated = !activated;
      aladin.hipsStore.pruneTree();
      repaint();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("PRUNETIP"); }
   protected String Help()       { return aladin.chaine.getString("Prune.HELP");  }

}

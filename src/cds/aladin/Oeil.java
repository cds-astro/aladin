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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Bouton "Oeil" pour afficher/cacher tous les plans, sauf le courant
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Avril 2012) Creation
 */
public class Oeil extends MyIcon {
   
   // Barres horizontales du dessin 
   static final private int TX[][] = {
      {0, 5,9},
      {1, 2,4},  {1, 6,8},   {1, 10,12},
      {2, 0,1},  {2, 5,6},   {2,9,9},    {2,13,14},
      {3, 5,6},  {3,9,9}, 
      {4, 5,9},
      {5, 0,1},  {5, 6,8},   {5,13,14},
      {6, 2,4},  {6, 10,12},
      {7, 5,9},
   };
   
   protected String LABEL;
   
   protected Oeil(Aladin aladin) {
      super(aladin,29,24);
      LABEL = aladin.chaine.getString("OEIL");
   }
   
   /** Dessine l'icone de la grille */
   static protected void drawOeil(Graphics g, int x,int y,Color c) {
      g.setColor(c);
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   private boolean isAvailable() {
      if( aladin.calque==null || aladin.calque.isFree() ) return false;
      if( isActive() ) return true;
      Plan pb = aladin.calque.getPlanBase();
      if( pb==null || !pb.isPixel() ) return false;
      ViewSimple v = aladin.view.getCurrentView();
      int n=0;
      for( int i=0; i<aladin.calque.plan.length; i++ ) {
         Plan p = aladin.calque.plan[i];
         if( p.hasError() || !p.active || p.getOpacityLevel()<0.1f || p==pb ) continue;
         if( p.hasNoReduction() || pb.hasNoReduction() || pb.projd.agree(p.projd, v) ) n++;
      }
      return n>0;
   }
   private boolean isActive()    { return aladin.calque.hasClinDoeil(); }
   private boolean isMouseIn()   { return false;  }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      
      int x=5;
      int y=2;
      
      g.setColor( getBackground() );
      g.fillRect(0,0,W,H);
      
      // Dessin
      drawOeil(g,x,y, !isAvailable() ? Aladin.MYGRAY :
                          isActive() ? Color.red :
                         isMouseIn() ? Color.blue : Color.black);
      
      // Label
      g.setColor(isAvailable() ? Color.black : Aladin.MYGRAY);
      g.setFont(Aladin.SPLAIN);
      g.drawString(LABEL,W/2-g.getFontMetrics().stringWidth(LABEL)/2,H-2);
   }

   protected void submit() {
      aladin.calque.clinDoeil();
      aladin.calque.repaintAll();
   }
   
   protected String getHelpTip() { return aladin.chaine.getString("OEILH"); }
   protected String Help()       { return aladin.chaine.getString("OEILH"); }

}

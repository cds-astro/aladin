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

import java.awt.Color;
import java.awt.Graphics;

/**
 * Bouton "Oeil" pour afficher/cacher tous les plans, sauf le courant
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Avril 2012) Creation
 */
public class Oeil extends Icon {
   
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
   
   protected boolean isAvailable() {
      if( aladin.calque==null || aladin.calque.isFree() ) return false;
      if( isActivated() ) return true;
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
   protected boolean isActivated()    { return aladin.calque.hasClinDoeil(); }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x=5+DX;
      int y=2;
      
      drawOeil(g,x,y, getLogoColor() );
      
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      g.drawString(LABEL,W/2-g.getFontMetrics().stringWidth(LABEL)/2,H-2);
   }

   protected void submit() {
      if( !isAvailable() ) return;
      aladin.calque.clinDoeil();
      aladin.calque.repaintAll();
   }
   
   protected String getHelpTip() { return aladin.chaine.getString("OEILH"); }
   protected String getHelpKey() { return "OEILH"; }
}

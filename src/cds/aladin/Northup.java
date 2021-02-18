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
 * Bouton pour forcer le nord en haut
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Décembre 2009) Creation
 */
public class Northup extends Icon {
   static final int L = 13;      // Taille d'un logo
   static String NORTHUP;

  /** Creation */
   protected Northup(Aladin aladin) {
      super(aladin,30,24);
      NORTHUP= aladin.chaine.getString("NORTHUP");
   }
   
   protected boolean isAvailable() {
      try {
         return !aladin.calque.isFree() && aladin.view.getCurrentView().canBeNorthUp();
      } catch( Exception e ) { return false; }
   }
   protected boolean isActivated() {
      try {
         return !aladin.calque.isFree()&& aladin.view.getCurrentView().isNorthUp();
      } catch( Exception e ) { return false; }
   }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      
      g.setColor( getLogoColor() );
      int x=7+DX,y=0;
      g.drawLine(x,y+10,x+9,y+10);
      g.drawLine(x+1,y+9,x+1,y+11);
      g.drawLine(x+2,y+8,x+2,y+12);
      g.drawLine(x+10,y,x+10,y+10);
      g.drawLine(x+9,y+1,x+11,y+1);
      g.drawLine(x+8,y+2,x+12,y+2);
      
      // Label
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      g.drawString(NORTHUP,W/2-g.getFontMetrics().stringWidth(NORTHUP)/2,H-2);

   }
   
   protected void submit() {
      aladin.view.getCurrentView().switchNorthUp();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("NORTHUPHELP"); }
   protected String getHelpKey() { return "Northup.HELP"; }
}

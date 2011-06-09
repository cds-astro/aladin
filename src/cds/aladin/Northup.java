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
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Bouton pour forcer le nord en haut
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Décembre 2009) Creation
 */
public class Northup extends MyIcon {
   static final int L = 13;      // Taille d'un logo
   static String NORTHUP;

  /** Creation */
   protected Northup(Aladin aladin) {
      super(aladin,30,24);
      NORTHUP= aladin.chaine.getString("NORTHUP");
   }
   
   private boolean isAvailable() {
      try {
         return !aladin.calque.isFree() && aladin.view.getCurrentView().canBeNorthUp();
      } catch( Exception e ) { return false; }
   }
   private boolean isActivated() {
      try {
         return !aladin.calque.isFree()&& aladin.view.getCurrentView().isNorthUp();
      } catch( Exception e ) { return false; }
   }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      g.setColor(!isAvailable() ? Aladin.MYGRAY : isActivated() ? Color.red : in ? Color.blue : Color.black);
      int x=7,y=0;
      g.drawLine(x,y+10,x+9,y+10);
      g.drawLine(x+1,y+9,x+1,y+11);
      g.drawLine(x+2,y+8,x+2,y+12);
      g.drawLine(x+10,y,x+10,y+10);
      g.drawLine(x+9,y+1,x+11,y+1);
      g.drawLine(x+8,y+2,x+12,y+2);
      
      // Label
      g.setColor(isAvailable() ? Color.black : Aladin.MYGRAY);
      g.setFont(Aladin.SPLAIN);
      g.drawString(NORTHUP,W/2-g.getFontMetrics().stringWidth(NORTHUP)/2,H-2);

   }
   
   protected void submit() {
      aladin.view.getCurrentView().switchNorthUp();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("NORTHUPHELP"); }
   protected String Help()       { return aladin.chaine.getString("Northup.HELP"); }
}

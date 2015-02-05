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
 * Bouton pour la synchronisation des vues
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Mars 2007) Creation
 */
public class Match extends MyIcon {
   static final int L = 12;      // Taille d'un logo
   static String MATCH;
   protected boolean megaMatch=false;

  /** Creation */
   protected Match(Aladin aladin) {
      super(aladin,35,24);
      MATCH= aladin.chaine.getString("SYNC");
   }
   
   /** Retourne true si on est en mode sync */
   protected boolean isMatched() {
      return aladin.view.getNbSelectedView()>1 && !aladin.view.hasCompatibleViews();
   }

   private boolean isAvailable() { return aladin.view.isMultiView() && getMode()>=1; }
   private boolean isMouseIn()   { return in; }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      g.setColor( getBackground());
      g.fillRect(0,0,W,H);
      int x = 20;
      int y = 7;
      int r = 3;
      
      int mode=getMode();
      
      g.setColor( mode==0 ? getBackground() :
                  mode==1 ? Color.green :
                  mode==2 ? new Color(80,80,255) /* ViewSimple.ORANGE*/ :
                     Color.red );
      g.fillRect(x-r,y-r,2*r,2*r);
      
      // Les bords du carré
      g.setColor( !isAvailable() ?  Aladin.MYGRAY : isMouseIn() ? Color.blue : Color.black);
      g.drawRect(x-r,y-r,2*r,2*r);
      
      // La croix
      g.drawLine(x-L/2,y,x-1,y);
      g.drawLine(x+1,y,x+L/2,y);
      g.drawLine(x,y-L/2,x,y-1);
      g.drawLine(x,y+1,x,y+L/2);
      
      // Label
      g.setColor(isAvailable() ? Color.black : Aladin.MYGRAY);
      g.setFont(Aladin.SPLAIN);
//      g.drawString(SYNC,6,H-2);
      g.drawString(MATCH,W/2-g.getFontMetrics().stringWidth(MATCH)/2,H-2);

   }
   
   
   /** Retourne un code pour le mode de synchronisation actuelle
    * 0 - pas possible (gris)
    * 1 - possible (vert)
    * 2 - activé (orange)
    * 3 - activé + megaSync (rouge)
    */
   protected int getMode() {
      boolean active = aladin.view.hasCompatibleViews();
      boolean sync = aladin.view.getNbSelectedView()>1  && !active;
      return  sync && megaMatch ? 3 :
              sync ? 2 :
              active ? 1 : 0; 
   }
   
   protected boolean isProjSync() { return getMode()==3; }

   protected void submit() {
      aladin.cycleMatch();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("MVIEWSYNC"); }
   protected String Help()       { return aladin.chaine.getString("Sync.HELP"); }
}

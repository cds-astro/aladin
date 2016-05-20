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
 * Bouton pour passer en mode High Distribution Range
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Jan 2016) Creation
 */
public class Hdr extends MyIcon {
   static final int L = 13;      // Taille d'un logo
   static String LABEL;

  /** Creation */
   protected Hdr(Aladin aladin) {
      super(aladin,25,24);
      LABEL= "hdr";
   }
   
   private boolean isAvailable() {
      try {
         Plan p = aladin.calque.getPlanBase();
         return p instanceof PlanBG ?  ((PlanBG)p).inFits : p.hasAvailablePixels() ;
      } catch( Exception e ) { return false; }
   }
   private boolean isActivated() {
      try {
         Plan p = aladin.calque.getPlanBase();
//       return  p instanceof PlanBG &&  ((PlanBG)p).truePixels;
         return  p instanceof PlanBG ? ((PlanBG)p).truePixels : false;
      } catch( Exception e ) { return false; }
   }
   
   // True si on vient d'appuyer sur le bouton
   private boolean flagSwitch = false;
   
   public int getMyCursor() { 
      if( flagSwitch ) {
         Plan p = aladin.calque.getPlanBase();
         if( p instanceof PlanBG && 
               ((PlanBG)p).hasDrawnSomething && System.currentTimeMillis()-tClick>500 ) {
            flagSwitch=false;
         }
         else return Aladin.WAITCURSOR;
      }
      return Aladin.HANDCURSOR ; 
   }

   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      g.setColor(!isAvailable() ? Aladin.MYGRAY : isActivated() ? Color.red /* Aladin.GREEN */ : in ? Color.blue : Color.black);
      int x=4,y=1;
//      int x=9,y=6;
//      Util.drawCirclePix(g, x, y);
      
      g.fillRect(x+1, y+8, 3, 4);
      g.fillRect(x+5, y+6, 3, 6);
      g.fillRect(x+9, y+1, 3, 11);
      g.drawLine(x-2,y+11,x+14,y+11);
      
      // Label
      g.setColor(isAvailable() ? Color.black : Aladin.MYGRAY);
      g.setFont(Aladin.SPLAIN);
      g.drawString(LABEL,W/2-g.getFontMetrics().stringWidth(LABEL)/2-1,H-2);

   }
   
   private long tClick=0;
   
   protected void submit() {
      if( !isAvailable() ) return;
      Plan p = aladin.calque.getPlanBase();
      tClick = System.currentTimeMillis();
      if( p instanceof PlanBG ) {
         ((PlanBG)p).switchFormat();
         flagSwitch=true;
      }
      if( aladin.frameCM==null ) aladin.frameCM = new FrameColorMap(aladin);
      aladin.frameCM.localcut((PlanImage)p);
      Aladin.makeCursor(this, getMyCursor() );
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("PIXHELP"); }
   protected String Help()       { return aladin.chaine.getString("Pix.HELP"); }
}

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
import java.awt.event.MouseEvent;

import cds.tools.Util;

/**
 * Bouton pour l'activation du Simbad Pointer + VizieR SED
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (déc 2016) Creation
 */
public class Look extends MyIcon {
   static final int L = 12;      // Taille d'un logo
   static String LOOK;

  /** Creation */
   protected Look(Aladin aladin) {
      super(aladin,32,24);
      LOOK= aladin.chaine.getString("LOOK");
   }
   
   // Barres horizontales du dessin 
   static final private int TX[][] = {
      {0, 3,7},
      {1, 2,2},  {1, 8,8}, 
      {2, 1,1},  {2, 9,9},
      {3, 0,0},  {3,4,4},   {3,9,10}, 
      {4, 0,0},  {4,3,3},   {4,10,10},
      {5, 0,0},             {5,10,10},
      {6, 0,0},             {6,10,10},
      {7, 0,0},             {7,10,10},
      {8, 1,1},             {8,9,12},
      {9, 2,2},  {9,8,8},   {9,10,14},
      {10,3,7},            {10,11,15},
      {11,12,15},
      {12,14,14},
   };
   
   
   /** Dessine l'icone  */
   protected void drawLook(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
   }
   
   private boolean isAvailable() {
      if( aladin.view.isFree() ) return false;
      return true;
//      ViewSimple v = aladin.view.getMouseView();
//      return v!=null && !v.isFree() && !v.isAllSky();
   }
   private boolean isMouseIn()   { return in; }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      g.setColor( getBackground());
      g.fillRect(0,0,W,H);
      int x = 10;
      int y = 0;
      int r = 10;
      
      int mode=getMode();
      
      g.setColor( mode==0 ? getBackground() :
                  mode==1 ? Color.green :
                  mode==3 ? ViewSimple.ORANGE :
                     Color.red );
      
      if( isAvailable() && isMouseIn() ) {
         g.setColor( Aladin.MYBLUE );
         g.fillOval(x,y,r,r);
      }
      
      g.setColor( !isAvailable() ?  Aladin.MYGRAY : 
           mode==0 ? Color.black :
           mode==1 ? Aladin.GREEN :
           mode==2 ?  Color.red : Color.orange ) ;
      drawLook(g,10,0);
      
      // Label
      g.setColor(isAvailable() ? Color.black : Aladin.MYGRAY);
      g.setFont(Aladin.SPLAIN);
      g.drawString(LOOK,W/2-g.getFontMetrics().stringWidth(LOOK)/2,H-2);
   }
   
   /** Retourne un code pour le mode actuel
    * 0 - Rien 
    * 1 - Simbad
    * 2 - Simbad+VizieR
    * 3 - VizieR
    */
   protected int getMode() {
      boolean simbad = aladin.calque.flagSimbad;
      boolean vizier = aladin.calque.flagVizierSED;
      return  simbad && vizier ? 2 : simbad ? 1 : vizier ? 3 : 0;
   }
   
   static final private String [] MODE = { "No","Simbad","Simbad+VizieR","VizieR"};
   
   protected void submit() {
      aladin.cycleLook();
   }
   
   /** On se deplace sur l'icone */
   public void mouseMoved(MouseEvent e) {
      if( aladin.inHelp ) return;
      Util.toolTip(this,getHelpTip());
   }
      
   protected String getHelpTip() {
      if( !isAvailable() ) return aladin.chaine.getString("LOOKTIP");
      return MODE[ getMode() ]+ " "+aladin.chaine.getString("LOOKTIP1");
   }
   protected String Help()       { return aladin.chaine.getString("Look.HELP");  }

}

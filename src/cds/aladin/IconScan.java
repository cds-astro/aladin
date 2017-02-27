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

import java.awt.Graphics;

/**
 * Bouton pour ne conserver que les collections visibles dans le champ de vue courant
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (déc 2016) Creation
 */
public class IconScan extends MyIcon {
   static final int L = 12;      // Taille d'un logo
   private boolean abort=false;
   private String STOP,SCAN;

  /** Creation */
   protected IconScan(Aladin aladin) {
      super(aladin,32,24);
      SCAN = aladin.chaine.getString("SCAN");
      STOP = aladin.chaine.getString("STOP");
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
   protected void drawIcon(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(13-TX[i][1]+x,TX[i][0]+y,13-TX[i][2]+x,TX[i][0]+y);
   }
   
   /** Retourne true si le scanning des collections sélectionnées peut être lancé */
   protected boolean isAvailable() {
      return !aladin.view.isFree() && aladin.directory.isScannable();
   }
   
   /** Retourne true si le scanning de certaines collections est en cours */
   protected boolean isActivated() { 
//      if( abort ) {
//         if( !aladin.directory.isScanning() ) abort=false;  // pour réinitialiser automatiquement le flag
//         return false;
//      }
      return aladin.directory.isScanning(); }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      super.drawLogo(g);
      int x = 10;
      int y = 1;
//      boolean isAvailable = isAvailable();
      boolean isActivated = isActivated();
      

      if( isActivated ) g.setColor( aladin.directory.blinkState ? (abort ? Aladin.COLOR_RED : Aladin.ORANGE) 
            : Aladin.COLOR_CONTROL_FOREGROUND);
      else g.setColor( getLogoColor() );
      drawIcon(g,x,y);
      
      g.setColor( getLabelColor() );
      g.setFont(Aladin.SPLAIN);
      
      String label = !isActivated ? SCAN : abort ? "abort!" : STOP;
      g.drawString(label,W/2-g.getFontMetrics().stringWidth(label)/2,H-2);
   }
   
   protected void submit() {
      if( !isAvailable() ) return;
      if( !isActivated() ) { 
         abort=false; 
         aladin.directory.scan();
      } else {
         abort=true;
         aladin.directory.abortScan();
      }
      repaint();
   }
      
   protected String getHelpTip() { return aladin.chaine.getString("SCANTIP"); }
   protected String Help()       { return aladin.chaine.getString("Scan.HELP");  }

}

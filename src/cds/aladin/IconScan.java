// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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
public class IconScan extends Icon {
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
//   static final private int TX[][] = {
//      {0, 3,7},
//      {1, 2,2},  {1, 8,8}, 
//      {2, 1,1},  {2, 9,9},
//      {3, 0,0},  {3,4,4},   {3,9,10}, 
//      {4, 0,0},  {4,3,3},   {4,10,10},
//      {5, 0,0},             {5,10,10},
//      {6, 0,0},             {6,10,10},
//      {7, 0,0},             {7,10,10},
//      {8, 1,1},             {8,9,12},
//      {9, 2,2},  {9,8,8},   {9,10,14},
//      {10,3,7},            {10,11,15},
//      {11,12,15},
//      {12,14,14},
//   };
   
   static final private int TX[][] = {
         {0, 4,10},
         {1, 2,4},  {1, 10,12}, 
         {2, 1,1},  {2,3,3}, {2,7,7}, {2,11,13},
         {3, 0,0},  {3,3,3}, {3,7,7}, {3,11,11}, {3,14,14}, 
         {4, 0,4},           {4,7,7},            {4,13,4},
         {5, 0,0},  {5,3,7}, {5,10,11},          {5,14,14},
         {6, 0,0},  {6,3,3}, {6,10,11},
         {7, 1,1},  {7,3,3}, {7,7,9},            {7,12,14},
         {8, 2,4},           {8,7,8},            {8,13,14},
         {9, 4,5},           {9,8,8},            {9,13,13},
                             {10,7,8},           {10,13,14},
                             {11,7,9},           {11,12,14},
                             {12,10,11},
                             {13,10,11},
      };

   
  
   /** Dessine l'icone  */
   protected void drawIcon(Graphics g, int x,int y) {
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][1]+x,TX[i][0]+y,TX[i][2]+x,TX[i][0]+y);
//      for( int i=0; i<TX.length; i++ ) g.drawLine(13-TX[i][1]+x,TX[i][0]+y,13-TX[i][2]+x,TX[i][0]+y);
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
      

      if( isActivated ) g.setColor( aladin.directory.blinkState ? (abort ? Aladin.COLOR_ICON_ACTIVATED : Aladin.ORANGE) 
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
   protected String getHelpKey() { return "Scan.HELP"; }
}

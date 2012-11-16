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
 * Bouton "Grille" pour afficher/cacher la grille de coordonnées
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (Mars 2007) Creation
 */
public class Grid extends MyIcon {
   
   // Barres verticales du dessin
   static final private int TX[][] = {
       {4,10,0},  {2,3,1},  {11,12,1},  {1,13,5},
       {2,3,8},  {11,12,8},  {4,10,9}
   };
   
   // Barres horizontales du dessin
   static final private int TY[][] = {
      {3,6,0},  {3,6,14},  {2,2,1},  {7,7,1},  
      {2,2,13}, {7,7,13},  {3,6,3} , {3,6,11},
      {2,2,4},  {7,7,4},  {2,2,10},  {7,7,10},
      {1,8,7},  {1,1,5},  {1,1,9},   {8,8,5},  {8,8,9},
   };
   
   // Barres horizontales du dessin de la grille de polarisation
   // (pas de barres verticales)
   static final private int TY1[][] = {
      {0, 4,11},
      {1, 2,4},  {1, 11,12},
      {2, 1,1},  {2, 9,9},   {2,13,13},
      {3, 0,0},  {3,4,4},    {3, 9,9},   {3,14,14},
      {4, 0,0},  {4, 9,9},   {4,14,14},
      {5, 0,0},  {5, 2,5},   {5,14,14},
      {6, 0,0},  {6, 14,14},
      {7, 1,1},  {7, 7,9},  {7,13,13},
      {8, 2,4},  {8, 11,12},
      {9, 4,10},
   };
   
   // Barres horizontales du dessin de la grille dun MOC
   // (pas de barres verticales)
   static final private int TY2[][] = {
      {3,3,0},   {7,7,0},    {11,11,0},
      {2,2,1},   {4,4,1},    {6,6,1},   {8,8,1},     {10,10,1},    {12,12,1},
      {1,1,2},   {5,5,2},    {9,9,2},   {13,13,2},
      {2,2,3},   {4,4,3},    {10,10,3}, {12,12,3},   {14,14,3},
      {3,3,4},   {11,11,4},  {15,15,4},
      {2,2,5},   {4,4,5},    {10,10,5}, {12,12,5},   {14,14,5},
      {1,1,6},   {5,5,6},    {9,9,6},   {13,13,6},
      {2,2,7},   {4,4,7},    {6,6,7},   {8,8,7},     {10,10,7},    {12,12,7},  {14,14,7},
      {3,3,8},   {7,7,8},    {11,11,8}, {13,13,8},
   };
   
   // Barres horizontales du dessin de la grille dun MOC
   // (pas de barres verticales)
   static final private int TY2BG[][] = {
      {3,3,1},   {7,7,1},    {11,11,1},
      {2,12,2},
      {3,13,3},
      {4,14,4},
      {3,13,5},
      {2,12,6},
      {3,3,7},   {7,7,7},    {11,13,7},
  
   };
   

   // Rectangle de fond
   static final private int FD[][] = { {2,1,11,8}, {1,3,13,4} };
   
   protected String LABEL;
   
   protected Grid(Aladin aladin) {
      super(aladin,29,24);
      LABEL = aladin.chaine.getString("GRID");
   }
   
    /** Dessine l'icone de la grille */
   static protected void drawGrid(Graphics g, int x,int y,Color c) {
      g.setColor(c);
      for( int i=0; i<TX.length; i++ ) g.drawLine(TX[i][0]+x,TX[i][2]+y,TX[i][1]+x,TX[i][2]+y);
      for( int i=0; i<TY.length; i++ ) g.drawLine(TY[i][2]+x,TY[i][0]+y,TY[i][2]+x,TY[i][1]+y);
   }
   
   /** Dessine l'icone de la grille de polarisation */
   static protected void drawPolar(Graphics g, int x,int y,Color c) {
      g.setColor(c);
      for( int i=0; i<TY1.length; i++ ) g.drawLine(TY1[i][1]+x,TY1[i][0]+y,TY1[i][2]+x,TY1[i][0]+y);
   }
   
   /** Dessine l'icone de la grille d'un MOC */
   static protected void drawMOC(Graphics g, int x,int y,Color c) {
      g.setColor(c);
      for( int i=0; i<TY2.length; i++ ) g.drawLine(TY2[i][0]+x,TY2[i][2]+y,TY2[i][1]+x,TY2[i][2]+y);
   }
   
   /** Dessine lle fond de l'icone de la grille d'un MOC */
   static protected void fillMOC(Graphics g, int x,int y,Color c) {
      g.setColor(c);
      for( int i=0; i<TY2BG.length; i++ ) g.drawLine(TY2BG[i][0]+x,TY2BG[i][2]+y,TY2BG[i][1]+x,TY2BG[i][2]+y);
   }
   
   static protected void fillBG(Graphics g, int x,int y,Color c) {
      g.setColor(c);
      for( int i=0; i<2; i++ ) g.fillRect(FD[i][0]+x,FD[i][1]+y,FD[i][2],FD[i][3]);
      
   }
   
   private boolean isAvailable() {
      if( aladin.calque==null ) return false;
      Plan p = aladin.calque.getPlanRef();
      return p!=null && Projection.isOk(p.projd);
   }
   private boolean isActive()    { return aladin.calque.hasGrid(); }
   private boolean isMouseIn()   { return false; /* return in; */ }
   
  /** Affichage du logo */
   protected void drawLogo(Graphics g) {
      
      int x=5;
      int y=2;
      
      g.setColor( getBackground() );
      g.fillRect(0,0,W,H);
      
      // Remplissage
      fillBG(g,x,y,Color.white);
      
      // Dessin
      drawGrid(g,x,y, !isAvailable() ? Aladin.MYGRAY :
                          isActive() ? Aladin.GREEN :
                         isMouseIn() ? Color.blue : Color.black);
      
      // Label
      g.setColor(isAvailable() ? Color.black : Aladin.MYGRAY);
      g.setFont(Aladin.SPLAIN);
      g.drawString(LABEL,W/2-g.getFontMetrics().stringWidth(LABEL)/2,H-2);
   }

   protected void submit() {
      aladin.calque.switchGrid(true);
   }
   
   protected String getHelpTip() { return aladin.chaine.getString("GRIDH"); }
   protected String Help()       { return aladin.chaine.getString("Grid.HELP"); }

}

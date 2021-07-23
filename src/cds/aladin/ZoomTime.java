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

/**
 * Gestion d'une case de l'histogramme de répartition des valeurs d'une colonne
 * pour les mesures affichées dans MCanvas (voir Zoomview.setHist())
 */

package cds.aladin;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;

import cds.moc.TMoc;
import cds.tools.Astrodate;
import cds.tools.Util;

class ZoomTime {
   
   static final int HEIGHT = 15;    // Hauteur du slider temporel
   static final int DELTAY = 48;   // Distance au bord en bas.
   static final int DELTA_G = 7;   // Marge de Gauche
   static final int DELTA_D = 7;   // Marge de droite
   
   Aladin aladin;
   ZoomView zoomView;
   
   private double globalJdmin=Double.NaN;   // Temps min global de toutes les données chargées dans Aladin
   private double globalJdmax=Double.NaN;   // Temps max global de toutes les données chargées dans Aladin
   
   int width,height;
   int xDrag;
   int state,stateMove;
   Rectangle inRect = null;
   
   ZoomTime(ZoomView zoomView) {
      this.zoomView = zoomView;
      this.aladin=zoomView.aladin;
      xDrag=-1;
      stateMove=state=OUT;
   }
   
   protected boolean mouseIn(int x,int y) {
      return inRect==null ? false : inRect.contains(x, y);
   }
   
   static final int OUT     = 0;
   static final int BEFORE  = 1;
   static final int ONSTART = 2;
   static final int INTIME  = 3;
   static final int ONEND   = 4;
   static final int AFTER   = 5;
   static final int INCROSS = 6;
   
   static final int M = 3;
   
   protected int getMouseState(int x,int y,ViewSimple v) {
      if( rectCross!=null && rectCross.contains(x,y) ) return INCROSS;
      double [] t = v.getTimeRange();
      int start = Double.isNaN(t[0]) ? DELTA_G : getX(t[0]);
      int end = Double.isNaN(t[1]) ? width-DELTA_D : getX(t[1]);
      return x<DELTA_G-M ? OUT : x<start-M ? BEFORE : x<=start+M ? ONSTART
            : x<end-M ? INTIME : x<=end+M ? ONEND : x<=width-DELTA_D+M ? AFTER : OUT;
   }
   
   private double getDeltaTime( int deltaX ) {
      double total = globalJdmax - globalJdmin;
      double w = width - (DELTA_G+DELTA_D);
      double onePix = total/w;
      double deltaJD = deltaX*onePix;
      return deltaJD;
   }
   
   private double getTime( int x ) {
      return getDeltaTime( x+DELTA_G ) + globalJdmin;
   }
   
   protected double [] deltaXTime(int deltaX, ViewSimple v) {
      double deltaJD = getDeltaTime( deltaX );
      double t[] = v.getTimeRange();
      if( Double.isNaN(t[0]) ) t[0] = globalJdmin;
      if( Double.isNaN(t[1]) ) t[1] = globalJdmax;
      if( state==ONSTART ) t[0]+=deltaJD;
      else if( state==ONEND ) t[1]+=deltaJD;
      else if( state==INTIME ) {
         t[0]+=deltaJD; 
         t[1]+=deltaJD;
      }
      if( t[0]>t[1] ) t[0]=t[1];
      aladin.view.setTimeRange(t);
      
      return t;
   }
   
   protected double setXTime(int x, ViewSimple v) {
      double jd = getTime(x);
      double t[] = v.getTimeRange();
      if( Double.isNaN( t[0] ) && Double.isNaN( t[1] ) ) return Double.NaN;
      if( Double.isNaN( t[0] ) ) t[0] = globalJdmin;
      if( Double.isNaN( t[1] ) ) t[1] = globalJdmax;
      double midRange = (t[1]-t[0])/2;
      t[0] = jd-midRange;
      t[1] = jd+midRange;
      if( t[0]<=globalJdmin ) t[0]=Double.NaN;
      if( t[1]>=globalJdmax ) t[1]=Double.NaN;
      aladin.view.setTimeRange(t);
      
      return midRange;
   }
   
   private int oldStatMove=-1;
   protected void mouseMove(int x, int y, ViewSimple v) {
      if( !mouseIn(x,y) ) stateMove=OUT; 
      else  stateMove = getMouseState(x, y, v);
      if( oldStatMove!=stateMove && (stateMove==INCROSS || oldStatMove==INCROSS) ) zoomView.repaint();
      oldStatMove=stateMove;
   }

   protected boolean mousePress(int x, int y, ViewSimple v, boolean doubleClick) {
      if( !mouseIn(x,y) ) { state=OUT; return false; }
      
      // Suppression de la restriction temporelle
      if( doubleClick || stateMove==INCROSS ) {
         double [] t = new double [] { Double.NaN, Double.NaN };
         aladin.view.setTimeRange(t);
         return true;
      }
      
      state = getMouseState(x,y,v);
      return true;
   }
   
   protected boolean mouseDrag(int x, int y, ViewSimple v) {
      if( state==OUT ) return false;
      if( xDrag!=-1 ) {
         int deltaX = x-xDrag;
         deltaXTime(deltaX,v);
      }
      xDrag=x;
      return true;
   }
   
   protected boolean mouseRelease(int x, int y, ViewSimple v) {
      if( xDrag!=-1 ) {
         int deltaX = x-xDrag;
         double t [] = deltaXTime(deltaX,v);
         xDrag=-1;
      }
      return true;
   }
   
   private void dilate(double [] t, double pourcent) {
      double delta = t[1]-t[0];
      double add = delta*(pourcent/100);
      t[0]-=add;
      t[1]+=add;
   }
   
   /** Met à jour les valeurs du slider temporel en fonction de l'état de la pile
    * et de la vue courante 
    * @return false si aucun time range n'est à afficher
    */
   protected boolean resume() {
      double t [] = aladin.view.calque.getGlobalTimeRange();
      if( Double.isNaN(t[0]) ) t = aladin.view.getCurrentView().getTimeRange();
      if( Double.isNaN(t[0]) ) globalJdmax = globalJdmin = Double.NaN;
      else {
         dilate(t,2);
         globalJdmin = t[0];
         globalJdmax = t[1];
      }

      if( Double.isNaN(globalJdmin) )  return false;
      return globalJdmax!=globalJdmin;
   }
   
   private int getX( double jd ) {
      double total = globalJdmax - globalJdmin;
      double w = width - (DELTA_G+DELTA_D);
      double fct = w/total;
      return (int)( (jd-globalJdmin)*fct ) + DELTA_G;
   }
   
   final static double FCTZOOM = 0.12;
   
   /** Modifie le time Range de la vue en fonction d'un clic de la molette de la souris */
   protected boolean mouseWheelMoved( MouseWheelEvent e, ViewSimple v ) {
      if( !mouseIn( e.getX(), e.getY() ) ) return false;
      double [] t = v.getTimeRange();
      if( Double.isNaN(t[0]) ) t = new double [] { globalJdmin, globalJdmax }; 
      
      int sens = e.getWheelRotation()>0 ? 1 : -1;
      double range = t[1]-t[0];
      double nRange = range* (sens==1 ? 1+FCTZOOM : 1-FCTZOOM);
      double centre = (t[0]+t[1])/2;
      t[0] = centre-nRange/2;
      t[1] = centre+nRange/2;
      boolean rep = aladin.view.setTimeRange(t);
      if( rep ) zoomView.repaint();
      return true;
   }
   
   static final private int W = 4;
   private Rectangle rectCross = null;
   
   private void drawCross(Graphics g, int x, int y) {
      g.setColor(stateMove==INCROSS ? Color.red : Aladin.COLOR_CONTROL_FOREGROUND );
      g.drawLine(x, y, x + W, y + W);
      g.drawLine(x + 1, y, x + W + 1, y + W);
      g.drawLine(x + 2, y, x + W + 2, y + W);
      g.drawLine(x + W, y, x, y + W);
      g.drawLine(x + W + 1, y, x + 1, y + W);
      g.drawLine(x + W + 2, y, x + 2, y + W);
      rectCross = new Rectangle(x-2, y - 2, W + 3, W + 2);
   }


   protected void draw(Graphics g, ViewSimple v) { 
      
      // Pas de données temporelles => rien à afficher
      if( !resume() ) { inRect=null; return; }

      width = zoomView.getWidth();
      height = zoomView.getHeight();

      int x = DELTA_G;
      int y = height-DELTAY;
      int w = width-(DELTA_G+DELTA_D);
      int MARGE_D = width-DELTA_D;

      
      // Mémorisation de la position du controleur
      if( inRect==null || inRect.width!=width || inRect.height!=height ) {
         inRect = new Rectangle(x-2,y-5,w+4,HEIGHT+6);
      }
      
      g.setFont( Aladin.SPLAIN );

      // Dessin de la barre temporelle complète
      g.setColor( Aladin.COLOR_GREEN );
      g.drawRect(x,y,w,HEIGHT);
      g.setColor( Aladin.COLOR_BLUETIME );
      g.fillRect(x+2, y+2, w-4, HEIGHT-4);
      
      // Au cas où on ne réafficherait pas le croix
      rectCross=null;
      
      // Dessin de la zone temporelle courante
      double [] t = v.getTimeRange();
      if( !Double.isNaN(t[0]) || !Double.isNaN(t[1])) {
         int x1 = Double.isNaN(t[0]) ? DELTA_G : getX( t[0] );
         int x2 = Double.isNaN(t[1]) ? width-DELTA_D : getX( t[1] );

         // zone temporelle courante (colorée)
         g.setColor( Aladin.COLOR_BLUE );
         int xa = x1<DELTA_G ? DELTA_G : x1;
         int xb = x2>width-DELTA_D ? width-DELTA_D : x2;
         g.fillRect(xa, y+2, xb-xa+1, HEIGHT-4);

         // Crochet début
         if( !Double.isNaN(t[0]) ) {
            g.setColor ( Aladin.COLOR_CONTROL_FOREGROUND_HIGHLIGHT );
            g.fillRect( x1, y-3, 2, HEIGHT+6);
            g.drawLine( x1, y-3, x1+3, y-3);
            g.drawLine( x1, y+HEIGHT+3, x1+3, y+HEIGHT+3);
         }
         
         // Crochet fin
         if( !Double.isNaN(t[1]) ) {
            g.fillRect( x2-1, y-3, 2, HEIGHT+6);
            g.drawLine( x2, y-3, x2-3, y-3);
            g.drawLine( x2, y+HEIGHT+3, x2-3, y+HEIGHT+3);
         }
         
         // Les dates min et max de la zone temporelle courante
         g.setColor( Aladin.COLOR_VERTDEAU );
         String startDate = Astrodate.JDToDate( t[0],false,false );
         String endDate = Astrodate.JDToDate( t[1], false,false );
         if( startDate.equals(endDate) ) {
            startDate = Astrodate.JDToDate( t[0] );
            endDate = Astrodate.JDToDate( t[1] );
            int i= endDate.indexOf('T');
            endDate = endDate.substring(i+1);
         }
         FontMetrics fm = g.getFontMetrics();
         int la = fm.stringWidth(startDate);
         xa = x1-la/2;
         if( xa<DELTA_G ) xa = DELTA_G;
         int lb = fm.stringWidth(endDate);
         if( xb+lb/2 > MARGE_D ) xb = MARGE_D-lb;
         else xb=x2-lb/2;
         // Pour ne pas se marcher sur les pieds
         if( xa+la>xb ) {
            if( xb+lb>=MARGE_D ) xa=xb-la-8;
            else xb = xa+la+8;
         }
         g.drawString( startDate,xa,y-5);
         g.drawString( endDate,xb,y-5);
         
         // La petite croix pour pouvoir supprimer les crochets
         if( !v.isPlotTime() ) drawCross(g, x2-10, y+5);

         // La durée
         g.setColor ( Aladin.COLOR_CONTROL_FOREGROUND_HIGHLIGHT );
         if( Double.isNaN(t[0]) ) t[0] = globalJdmin;
         if( Double.isNaN(t[1]) ) t[1] = globalJdmax;
         String s = Util.getTemps( (long)( (t[1]-t[0])*(TMoc.DAYMICROSEC)) );
         int len = g.getFontMetrics().stringWidth(s);
         int x3 = (x2+x1)/2 - len/2;
         if( x3>x1 && x3>DELTA_G && x3+len<x2 && x2+len<MARGE_D )  g.drawString( s,x3,y+HEIGHT-2);
         

         // Dessin de 2 petits triangles pour indiquer qu'on peut rétrécir l'intervalle temporel
      } else {
         g.setColor( stateMove==ONSTART ? Aladin.COLOR_CONTROL_FOREGROUND : Aladin.COLOR_GREEN );
         Util.drawTriangle(g, DELTA_G, y-2, 7, false);
         g.setColor( stateMove==ONEND ? Aladin.COLOR_CONTROL_FOREGROUND : Aladin.COLOR_GREEN );
         Util.drawTriangle(g, MARGE_D, y-2, 7, false);
      }

   }
   

}

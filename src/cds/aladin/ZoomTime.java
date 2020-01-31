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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;

import cds.moc.TimeMoc;
import cds.tools.Astrodate;
import cds.tools.Util;

class ZoomTime {
   
   static final int DELTAY = 40;
   static final int DELTA_G = 7;
   static final int DELTA_D = 7;
   
   Aladin aladin;
   ZoomView zoomView;
   
   private double globalJdmin=-1;   // Temps min global de toutes les données chargées dans Aladin
   private double globalJdmax=-1;   // Temps max global de toutes les données chargées dans Aladin
   
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
   
   void setGlobalTimeRange(double [] timeRange ) {
      globalJdmin = timeRange[0];
      globalJdmax = timeRange[1];
   }
   
   /** Donne l'intervalle de temps max (en fonction du dernier scan de la pile */
   protected double [] getGlobalTimeRange() { return new double [] { globalJdmin, globalJdmax }; }
   
   protected boolean mouseIn(int x,int y) {
      return inRect==null ? false : inRect.contains(x, y);
   }
   
   static final int OUT     = 0;
   static final int BEFORE  = 1;
   static final int ONSTART = 2;
   static final int INTIME  = 3;
   static final int ONEND   = 4;
   static final int AFTER   = 5;
   
   static final int M = 3;
   
   protected int getMouseState(int x,ViewSimple v) {
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
      if( t[0]<=globalJdmin ) t[0]=Double.NaN;
      if( t[1]>=globalJdmax ) t[1]=Double.NaN;
      v.setTimeRange( t );
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
      v.setTimeRange( t );
      return midRange;
   }
   
   protected void mouseMove(int x, int y, ViewSimple v) {
      if( !mouseIn(x,y) ) { stateMove=OUT; return; }
      stateMove = getMouseState(x, v);
//      System.out.println("Je me déplace en "+x+","+y);
   }
   
   protected boolean mousePress(int x, int y, ViewSimple v, boolean doubleClick) {
      if( !mouseIn(x,y) ) { state=OUT; return false; }
      
      // Suppression de la restriction temporelle
      if( doubleClick ) {
         v.setTimeRange( new double [] { Double.NaN, Double.NaN } );
         return true;
      }
      
      state = getMouseState(x,v);
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
      if( xDrag==-1 ) {
         if( !mouseIn(x,y) ) return false;
         double t = setXTime(x, v);
//         memoCommand(t);
         return true;
      }
      int deltaX = x-xDrag;
      double t [] = deltaXTime(deltaX,v);
//      memoCommand(t);
      xDrag=-1;
      return true;
   }
   
   /** Retourne true s'il existe des données temporelles dans la pile et que l'on 
    * a pu initialiser un range temporel. */
   protected boolean isActivated() {
      if( Double.isNaN(globalJdmin) ) return false;
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
      if( t[0]<=globalJdmin ) t[0] = Double.NaN;
      if( t[1]>=globalJdmax ) t[1] = Double.NaN;
//      memoCommand( t );
      if( v.setTimeRange( t ) ) zoomView.repaint();
      return true;
   }
   
//   // Mémorisatino de la commmande script associée au temps => intervalle
//   private void memoCommand( double [] t ) {
//      String cmd = Astrodate.JDToDate(t[0])+" "+Astrodate.JDToDate(t[1]);
//      aladin.console.printCommand(cmd);
//   }
//
//   // Mémorisatino de la commmande script associée au temps => date centrale
//   private void memoCommand( double t ) {
//      String cmd = Astrodate.JDToDate(t);
//      aladin.console.printCommand(cmd);
//   }

   protected void draw(Graphics g, ViewSimple v) { 
      
      // Pas de données temporelles => rien à afficher
      if( !isActivated() ) { inRect=null; return; }

      width = zoomView.getWidth();
      height = zoomView.getHeight();

      int x = DELTA_G;
      int y = height-DELTAY;
      int w = width-(DELTA_G+DELTA_D);
      int h = 13;
      
      // Mémorisation de la position du controleur
      if( inRect==null || inRect.width!=width || inRect.height!=height ) {
         inRect = new Rectangle(x-2,y-5,w+4,h+6);
      }
      
      g.setFont( Aladin.SPLAIN );

      // Dessin de la barre temporelle complète
      g.setColor( Aladin.COLOR_GREEN );
      g.drawRect(x,y,w,h);
      g.setColor( Aladin.COLOR_BLUE.darker().darker() );
      g.fillRect(x+2, y+2, w-4, h-4);
      
      // Les dates min et max
      g.setColor( Aladin.COLOR_VERTDEAU );
      String startDate = Astrodate.JDToDate( globalJdmin,false );
      String endDate = Astrodate.JDToDate( globalJdmax, false );
      if( startDate.equals(endDate) ) {
         startDate = Astrodate.JDToDate( globalJdmin );
         endDate = Astrodate.JDToDate( globalJdmax );
         int i= endDate.indexOf('T');
         endDate = endDate.substring(i+1);
      }
      g.drawString( startDate,x,y+h+13);
      g.drawString( endDate,width-g.getFontMetrics().stringWidth(endDate)-5,y+h+13);

      // Dessin de la zone temporelle courante
      double [] t = v.getTimeRange();
      if( !Double.isNaN(t[0]) || !Double.isNaN(t[1])) {
         int x1 = Double.isNaN(t[0]) ? DELTA_G : getX( t[0] );
         int x2 = Double.isNaN(t[1]) ? width-DELTA_D : getX( t[1] );

         g.setColor( Aladin.COLOR_BLUE );
         g.fillRect(x1, y+2, x2-x1+1, h-4);

         // Crochet début
         g.setColor ( Aladin.COLOR_CONTROL_FOREGROUND_HIGHLIGHT );
         g.fillRect( x1, y-3, 2, h+6);
         g.drawLine( x1, y-3, x1+3, y-3);
         g.drawLine( x1, y+h+3, x1+3, y+h+3);
         
         // Crochet fin
         g.fillRect( x2-1, y-3, 2, h+6);
         g.drawLine( x2, y-3, x2-3, y-3);
         g.drawLine( x2, y+h+3, x2-3, y+h+3);
         
         // La date centrale
         int xm = (x1+x2)/2;
         g.drawLine( xm,y-2, xm, y+h+2);
         if( Double.isNaN(t[0]) ) t[0] = globalJdmin;
         if( Double.isNaN(t[1]) ) t[1] = globalJdmax;
         String s = Astrodate.JDToDate( (t[0]+t[1])/2 );
         int len = g.getFontMetrics().stringWidth(s);
         int x3 = xm - len/2;
         if( x3<2 ) x3=2;
         if( x3+len>width ) x3=width-len-2;
         g.drawString( s,x3,y-5-12);
         
         // La durée
         s = TimeMoc.getTemps( (long)( (t[1]-t[0])*TimeMoc.DAYMICROSEC),true );
         len = g.getFontMetrics().stringWidth(s);
         x3 = xm - len/2;
         if( x3<2 ) x3=2;
         if( x3+len>width ) x3=width-len-2;
         g.drawString( s,x3,y-5);

         // Dessin de 2 petits triangles pour indiquer qu'on peut rétrécir l'intervalle temporel
      } else {
         g.setColor( stateMove==ONSTART ? Aladin.COLOR_CONTROL_FOREGROUND : Aladin.COLOR_GREEN );
         Util.drawTriangle(g, DELTA_G, y-2, 7, false);
         g.setColor( stateMove==ONEND ? Aladin.COLOR_CONTROL_FOREGROUND : Aladin.COLOR_GREEN );
         Util.drawTriangle(g, width-DELTA_D, y-2, 7, false);
      }

   }
   
   static final private int SIZE_CLOCK = 5;

}

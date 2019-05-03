// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

/**
 * Gestion d'une case de l'histogramme de répartition des valeurs d'une colonne
 * pour les mesures affichées dans MCanvas (voir Zoomview.setHist())
 */

package cds.aladin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import cds.tools.Astrodate;

class ZoomTimeControl {
   
   static final int DELTAY = 80;
   
   Aladin aladin;
   ZoomView zoomView;
   
   double globalJdmin=-1;   // Temps min global de toutes les données chargées dans Aladin
   double globalJdmax=-1;   // Temps max global de toutes les données chargées dans Aladin
   
   int width,height;
   Rectangle inRect = null;
   
   ZoomTimeControl(ZoomView zoomView) {
      this.zoomView = zoomView;
      this.aladin=zoomView.aladin;
   }
   
   void setGlobalTimeRange(double [] timeRange ) {
      globalJdmin = timeRange[0];
      globalJdmax = timeRange[1];
      
      System.out.println("jdmin="+globalJdmin+" jdmax="+globalJdmax);
   }
   
   protected boolean mouseIn(int x,int y) { return inRect==null ? false : inRect.contains(x, y); }
   
   protected void mouseMove(int x, int y) {
      if( !mouseIn(x,y) ) return;
      System.out.println("Je me déplace en "+x+","+y);
   }
   
   protected boolean isActivated() {
      return !Double.isNaN(globalJdmin);
   }

   protected void draw(Graphics g) { 
      
      // Pas de données temporelles => rien à afficher
      if( !isActivated() ) { inRect=null; return; }

      width = zoomView.getWidth();
      height = zoomView.getHeight();

      int x = 5;
      int y = width-DELTAY;
      int w = width-2*x;
      int h = 13;
      
      // Mémorisation de la position du controleur
      if( inRect==null ) inRect = new Rectangle(x,y,w,h);

      int x1 = 50;
      int x2 = 100;

      // Dessin de la barre temporelle complète
      g.setColor( Aladin.COLOR_GREEN );
      g.drawRect(x,y,w,h);
      g.setColor( Aladin.COLOR_BLUE );
      g.fillRect(x+2, y+2, w-4, h-4);

      // Dessin de la zone temporelle courante
      g.setColor( Aladin.COLOR_RED );
      g.fillRect(x1, y+2, x2-x1+1, h-4);

      // Crochet début
      g.setColor (Color.magenta );
      g.fillRect( x1, y-3, 2, h+6);
      g.drawLine( x1, y-3, x1+3, y-3);
      g.drawLine( x1, y+h+3, x1+3, y+h+3);

      // Crochet fin
      g.fillRect( x2-1, y-3, 2, h+6);
      g.drawLine( x2, y-3, x2-3, y-3);
      g.drawLine( x2, y+h+3, x2-3, y+h+3);
      
      // Les dates min et max
      g.setColor( Aladin.COLOR_GREEN );
      String s = Astrodate.JDToDate( globalJdmin,false );
      g.drawString( s,x,y+h+12);
      s = Astrodate.JDToDate( globalJdmax, false );
      g.drawString( s,width-g.getFontMetrics().stringWidth(s)-5,y+h+12);
   }

}

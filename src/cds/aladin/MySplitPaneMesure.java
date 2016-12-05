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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

// Surcharges de classes pour supprimer le trait séparateur du JSplitPane
public class MySplitPaneMesure extends MySplitPane {
   Rectangle expandRect=null,splitRect=null;
   Aladin aladin;
   private int split=-1;
   
   public MySplitPaneMesure(Aladin aladin, int newOrientation, boolean newContinuousLayout,
         Component newLeftComponent, Component newRightComponent ) {
      super(newOrientation,newContinuousLayout,newLeftComponent,newRightComponent);
      setUI(new MyBasicSplitPaneUIMesure());
      this.aladin = aladin;
      setDividerSize(7);
   }

   // Repositionne le diviseur à la position mémorisée
   public void restoreSlit() {
      setDividerLocation(getHeight()-(split<=0 ? 150 : split)); }

   // Positionne le diviseur en fonction de la taille de la fenêtre des mesures,
   // et mémorise cette valeur pour pouvoir y revenir
   public void setSplit(int h) { split=h; }

   // Retourne la taille de la fenêtre des mesures.
   public int getSplit() { return split; }

   // On bride à 55 pixels minimum pour la taille de la fenêtre des mesures
   public void setDividerLocation(int n) {
      int h = getHeight();
      if( h-n<53 ) return;
      split = h-n;
      super.setDividerLocation(n);
   }

   class MyBasicSplitPaneUIMesure extends BasicSplitPaneUI {
      public BasicSplitPaneDivider createDefaultDivider() {
         return new MySplitPaneDividerMesure(this);
      }
   }
   class MySplitPaneDividerMesure extends BasicSplitPaneDivider implements MouseListener  {
      public MySplitPaneDividerMesure(BasicSplitPaneUI ui) {
         super(ui);
         addMouseListener(this);
      }
      public void paint(Graphics g) { drawLanguettes(g, aladin.mesure.flagReduced ); }
      
      private void drawLanguettes(Graphics g,boolean closed) {
         int h = getHeight();
         int w = h*4;
         int x = getWidth()-h*7;
         int y = 0;
         
         int x1 = getWidth()-h-1;
         int w1 = h;
         
         // Si fenêtre des mesures fermées => tracé d'un trait sous les languettes de controle
         if( closed ) {
            g.setColor(Color.lightGray);
            g.drawLine(2*getWidth()/3,h-1,x,h-1);
            g.drawLine(x+w-1,h-1,getWidth(),h-1);
         }
         
         g.setColor(Color.gray);
         g.drawLine(x,y,x,y+h);
         g.drawLine(x,y,x+w,y);
         g.drawLine(x+w-1,y,x+w-1,y+h);
         if( expandRect==null) expandRect = new Rectangle(x,y,w,h);
         else { expandRect.x=x; expandRect.y=y; } 
         for( int i=0; i<3; i++ ) drawTriangle(g, closed ,h-1, x+h/2+1+(w/4)*i, y+2);
         
         g.drawLine(x1,y,x1,y+h);
         g.drawLine(x1,y,x1+w1,y);
         g.drawLine(x1,y+1,x1+w1-2,y+1);
         g.drawLine(x1,y+2,x1+w1,y+2);
         g.drawLine(x1+w1,y,x1+w1,y+h);
         if( splitRect==null ) splitRect = new Rectangle(x1,y,w1,h);
         else  { splitRect.x=x1; splitRect.y=y; } 
      }
      
      // Trois petits triangles pour ouvrir ou fermée la fenêtre
      private void drawTriangle(Graphics g, boolean closed, int h, int x, int y) {
         Polygon p = new Polygon();
         if( !closed ) {
            p.addPoint(x, y);
            p.addPoint(x+h,y);
            p.addPoint(x+h/2,y+h);
         } else {
            p.addPoint(x, y+h-1);
            p.addPoint(x+h,y+h-1);
            p.addPoint(x+h/2,y-1);
         }
         g.fillPolygon(p);
      }
      
      public void mouseClicked(MouseEvent e) { }
      public void mousePressed(MouseEvent e) { }
      public void mouseReleased(MouseEvent e) {
         System.out.println("mouseReleased => "+e.getPoint());
         if( expandRect!=null && expandRect.contains( e.getPoint() ) ) aladin.mesure.switchReduced();
         else if( splitRect!=null && splitRect.contains( e.getPoint() ) ) aladin.mesure.split();
      }
      public void mouseEntered(MouseEvent e) { }
      public void mouseExited(MouseEvent e) { }
   }
}


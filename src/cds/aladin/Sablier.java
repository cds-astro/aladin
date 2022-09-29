// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

package cds.aladin;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;

import cds.tools.FastMath;
import cds.tools.Util;

/**
 * Dessin d'un sablier tournant d'attente
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (juin 2008) Creation
 */
public final class Sablier extends JComponent {
   
   static final double DEG = Math.PI/6;
   static final int DELAY = 300;
   
   private int xCenter,yCenter;
   private int rho=16;
   private double startAngle=0;
   private long timer=0L;
   
   public Sablier() {
      xCenter=yCenter=rho+4;
   }
   
   public void setCenter(int x,int y) {
      xCenter=x;
      yCenter=y;
   }
   
   private void drawLogo(Graphics g) {
//System.out.println("DrawLogo");      
      int c=170;
      startAngle = ((System.currentTimeMillis() - timer)/DELAY ) * DEG;
      for( double theta=0; theta<2*Math.PI; theta+=DEG  ) {
         int x = (int)Math.round( xCenter + rho*FastMath.cos( theta+startAngle ) );
         int y = (int)Math.round( yCenter + rho*FastMath.sin( theta+startAngle ) );
         g.setColor(new Color(c,c,c));
         c-=10;
         Util.fillCircle7(g,x,y);
      }
   }
   
   public void stop() {
      timer=0L;
//      System.out.println("Stop sablier");
   }
   
   public void start() {
      timer = System.currentTimeMillis();
//      System.out.println("Start sablier");
   }
   
   public boolean isRunning() { return timer!=0L;  }
   
   public void paintComponents(Graphics g) {
      if( !isRunning() ) return;
      drawLogo(g);
   }
   
}

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
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

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
         int x = (int)Math.round( xCenter + rho*Math.cos( theta+startAngle ) );
         int y = (int)Math.round( yCenter + rho*Math.sin( theta+startAngle ) );
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
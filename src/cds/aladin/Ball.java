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
import javax.swing.*;

public class Ball extends JComponent implements ActionListener {
   
   static final int UNKNOWN = 0;        // Voyant blanc => Non encore utilisé
   static final int OK = 1;             // Voyant vert  => Résultat disponible
   static final int HS = 2;             // Croix rouge => Le serveur est HS
   static final int NOK =3;             // Voyant rouge => Pas de données disponibles
   static final int WAIT=4;             // Clignotant vert => En attente de résultat
   static final int PARTIAL=5;          // Orange => Résultat partiel

   private int W = 11;
   private Dimension dim = new Dimension(W,W);
   private int mode = UNKNOWN;
   private boolean blinkState;
   private Timer timer=null;
   private int x=0,y=0;
   
   public Ball() { }
   public Ball(int x,int y) { this.x=x; this.y=y; }
   
   public Dimension getPreferredSize() { return dim; }
   
   protected void setMode(int mode) {
      this.mode = mode;
      if( !Aladin.NOGUI ) repaint();
   }
   
   protected boolean isBlinking() { return mode==WAIT; }
   
   protected boolean isRed() { return mode==NOK; }
   
   public void actionPerformed(ActionEvent e) {
      blinkState=!blinkState;
      if( !Aladin.NOGUI ) repaint();
   }
   
   public void paintComponent(Graphics g) {
      if( Aladin.NOGUI ) return;
      super.paintComponent(g);
      switch( mode ) {
         case UNKNOWN: Slide.drawBall(g,x,y,Color.white);  break;
         case OK:      Slide.drawBall(g,x,y,Color.green);  break;
         case NOK:     Slide.drawBall(g,x,y,Color.red);    break;
         case PARTIAL: Slide.drawBall(g,x,y,Color.orange); break;
         case HS:      Slide.drawCross(g, x,y);            break;
         case WAIT:    Slide.drawBall(g,x,y,!blinkState?Color.green:Color.white); break;
      }
      if( mode==WAIT ) {
         if( timer==null ) timer = new Timer(500,this);
         if( !timer.isRunning() ) timer.start();
      } else if( timer!=null && timer.isRunning() ) timer.stop();
   }
}

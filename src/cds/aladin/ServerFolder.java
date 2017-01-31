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
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;

/**
 * Popup menu maison
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (12 mars 2001) creation
 */
public final class ServerFolder extends Server implements
                   MouseMotionListener, MouseListener
                   {

   // Les composants du bouton
   Image img;			// L'image associee au bouton
   Graphics g;			// Le contexte graphique
   FontMetrics m=null;
   Vector menu;
   boolean flagOk=false;

   static final int LEFT=0;
   static final int RIGHT=1;
   static final int TOP=2;
   
   static final int MARGE = 3;
   static final int GAP = 5;
   int W,H;			    // taille du sous-menu
   int X,Y;			    // Position du bouton correspondant
   int x=0,y=0;			// position calculee du haut/gauche
   int pos;			    // Position TOP, LEFT ou RIGHT
   int numButton;		// indice du bouton associe
   int choix=-1;			// indice de l'item courant
   Color backGround;
   static final String NOTHING = " -- nothing -- ";

   ServerFolder(Aladin aladin,String nom,int numButton,int pos) {
      super();
      aladinLogo = "FolderLogo.gif";
      this.aladinLabel=nom;
      this.aladin=aladin;
      this.numButton = numButton;
      this.pos=pos;
      setBackground(Aladin.BLUE);
      type=(pos==LEFT)?IMAGE: pos==RIGHT ? CATALOG : APPLI;
      menu = new Vector();
      menu.addElement(NOTHING);
      m = Toolkit.getDefaultToolkit().getFontMetrics(Aladin.PLAIN);
      backGround =Aladin.COLOR_CONTROL_BACKGROUND;
      
      addMouseMotionListener(this);
      addMouseListener(this);
   }

   void addItem(String mi) {
      if( menu.size()==1 && menu.elementAt(0)==NOTHING ) menu.removeElementAt(0);
      menu.addElement(mi);
      flagOk=false;
   }

   void adjust() {
      MyButton c = aladin.dialog.buttons[numButton];
      Y = c.getLocation().y;
      X = c.getLocation().x;

      Enumeration e = menu.elements();
      int max=0;
      while( e.hasMoreElements() ) {
         String s = (String)e.nextElement();
         int l = m.stringWidth(s);
         if( max<l ) max=l;
      }
      
      W = max+30;
      if( pos==TOP ) {
         x=X;
         if( x+W>getSize().width-2 ) x=getSize().width-W-1;
         if( x<1 ) x=1;
      } else x=(pos==LEFT)?2:getSize().width-W-1;
      
      H = menu.size()*(Aladin.SIZE+GAP)+2;
      if( pos==TOP ) y=2;
      else {
         y=(Y+H>=getSize().height)?getSize().height-H-1:Y;
      }
      
      flagOk=true;
   }

   private int getItem(int y) {
      int i= (y-this.y)/(Aladin.SIZE+GAP);
//      if( i<0 ) i=0;
//      if( i>=menu.size() ) i=menu.size()-1;
      return i;
   }

  /** Gestion de la souris */
   public void mouseMoved(MouseEvent e) {
      int ochoix=choix;
      int i = getItem(e.getY());
      if( ochoix!=i ) {
         choix=i;
         repaint();
      }
   }

  /** Gestion de la souris */
   public void mouseReleased(MouseEvent e) {
      int i = getItem(e.getY());
      if( i<0 ) return;
      String text = (String)menu.elementAt(i);
      postEvent(new Event(new Button(text),Event.ACTION_EVENT,text) );
   }

   public void update(Graphics g) {
      paint(g);
   }

   // Il faut que j'évènement se propage
   public boolean action(Event evt, Object what) { return false; }

   public void paintComponent(Graphics g) {
      if( !flagOk ) adjust();

      Enumeration e = menu.elements();
      int yc=y+MARGE;
      g.setColor(Aladin.BLUE);
      g.fillRect(0,0,getWidth(),getHeight());
      g.setColor(Aladin.COLOR_CONTROL_BACKGROUND);
      g.fillRect(x,y,W,H);
      for( int i=0; e.hasMoreElements(); i++ ) {
         if( i==choix ) {
            g.setColor( Color.blue );
            g.fillRect(x+1,yc-2, W-2,Aladin.SIZE+GAP);
         }
         
         String s = (String)e.nextElement();
         g.setColor(i==choix ? Color.white : Color.black);
         g.drawString(s,x+MARGE,yc+Aladin.SIZE);
         
         
//         g.setColor(i==choix?Color.white:backGround);
//         g.drawLine(x+1,yc+Aladin.SIZE+2,x+1,yc-2);
//         g.drawLine(x+1,yc-2,x+W-1,yc-2);
//         g.setColor(i==choix?Color.black:backGround);
//         g.drawLine(x+W-1,yc-2,x+W-1,yc+Aladin.SIZE+2);
//         g.drawLine(x+W-1,yc+Aladin.SIZE+2,x+1,yc+Aladin.SIZE+2);

         yc+=Aladin.SIZE+GAP;
      }
      g.setColor(Color.white);
      g.drawLine(x,y+H,x,y); g.drawLine(x,y,x+W,y);
      g.setColor(Color.black);
      g.drawLine(x+W,y,x+W,y+H); g.drawLine(x+W,y+H,x,y+H);
   }

   public void mouseDragged(MouseEvent e) { }
   public void mouseClicked(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
   public void mousePressed(MouseEvent e) { }

}

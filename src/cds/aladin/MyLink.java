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

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.JComponent;

/**
 * Bouton Aladin java qui gere les entrees et les sorties
 * de la souris
 *
 * @author Pierre Fernique [CDS]
 * @version 2.0 : (janv 03) contourne bug JVM 1.4.0 + suppression double buffer
 * @version 1.1 : (16 dec 99) corr bug hauteur des boutons
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class MyLink extends JComponent {

   // Les constantes
   static final int UP      =  1;
   static final int DOWN    = -1;
   static final int UNAVAIL =  0;

   static final int NORMAL  = 0;
   static final int LEFT    = 1;
   static final int RIGHT   = 2;

   static final int TRI     = 7;

   /** La phrase d'explication du bouton */
   protected String baratin;
   /** Le nom du bouton */
   protected String text;
   String help=null;        // Le help associe au bouton

   // Les references
   Aladin aladin;
   Label status=null;

   // Les variables d'etats
   boolean alwaysUp=true;   // Indique s'il s'agit d'un bouton toujours UP
   int mode=UP;         // Etat UP, UNAVAIL, DOWN, du bouton
   int type = NORMAL;       // si !=NORMAL, => fortement lie a Aladin
                //   (baratin sera affiche dans aladin.status(),
                                //    type de bord LEFT ou RIGHT...)

   // Les composants du bouton
   int W = 75;          // Largeur minimale
   static final int MARGE=10;   // Marge
   int H = Aladin.LSIZE+MARGE;  // Hauteur du bouton pour une ligne
   private Color color= Color.black;  // Couleur du texte
   Font f=null;         // Font du bouton
   
   int strWidth;

  /** Creation d'un bouton.
   * @param aladin  Reference
   * @param s       Le label du bouton
   */
   protected MyLink(Aladin aladin,String s) { suite(aladin,s,null,null,true); }

  /** Creation d'un bouton.
   * @param aladin  Reference
   * @param status  Label dans lequel il faudra afficher le baratin d'explication
   *                si le type est ``NORMAL''
   * @param type    Type de bouton (si != NORMAL, affichage du status dans
   *                aladin.status
   * @param s       Le label du bouton
   * @param baratin L'explication associee au bouton
   */
   protected MyLink(Aladin aladin,Label status,int type,
                      String s, String baratin) {
      this.status = status;
      this.type = type;
      suite(aladin,s,baratin,null,true);
   }

  /** Creation d'un bouton.
   * @param aladin  Reference
   * @param s       Le label du bouton
   * @param baratin L'explication (courte) associee au bouton
   * @param help    Le help associee au bouton
   */
   protected MyLink(Aladin aladin,String s, String baratin, String help) {
      suite(aladin,s,baratin,help,true);
   }
   protected MyLink(Aladin aladin,String s, String baratin,
                      String help,boolean enable) {
      suite(aladin,s,baratin,help,enable);
   }

  /** Generation d'un bouton (procedure interne) */
   void suite(Aladin aladin,String s, String baratin,
              String help,boolean enable) {
      this.aladin = aladin;
      this.baratin = baratin;
      text=s;
      mode=enable?UP:UNAVAIL;
      if( help!=null ) this.help=help;
//      setBackground( Aladin.BKGD );
      f=Aladin.LBOLD;
      H=getH();
      resize(W,H);
   }

  /** Changement de la font */
   public void setFont(Font f) {
      this.f = f;
      FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
      W = fm.stringWidth(text)+10;
      strWidth = fm.stringWidth(text);
      resize(W,H);
   }

   private int getH() {
      char [] a = text.toCharArray();
      int n=1;
      for( int i=0; i<a.length; i++ ) if( a[i]=='\n' ) n++;
      return Aladin.LSIZE*n+MARGE;
   }

  /** Modification du nom label du bouton.
   * @param s le nouveau label
   */
   protected void setLabel(String s) { text=s; repaint(); }

  /** Modification du mode de fonctionnement.
   * Positionne ou non l'etat toujours UP du bouton.
   * @param a <I>true</I>, le bouton est toujours UP, <I>flase</I> sinon
   */
   protected void setAlwaysUp(boolean a) { alwaysUp=a; }

  /** Rend ou non le bouton accessible
   * @param a <I>true</I>, le bouton est active, <I>flase</I> sinon
   */
   public void enable(boolean a)  {
      int omode=mode;
      if( a ) mode=UP;
      else mode=UNAVAIL;
      if( omode!=mode ) repaint();
   }

  /** Enfonce le bouton */
   protected void push() { /*if( mode!=UNAVAIL) { mode=DOWN; repaint();}*/  }

  /** Relache le bouton */
   protected void pop()  {  /*if( mode!=UNAVAIL) { mode=UP;   repaint();}*/ }

   public boolean mouseDown(Event e, int x, int y) {
       /*
      if( aladin.inHelp ) return true;
      if( mode==UNAVAIL ) return true;
      push();
      if( pm!=null ) {
         pm.show(this,W-10,H/2+10);
      }
      */
      return true;
   }

   public boolean mouseUp(Event e, int x, int y) {
      if( mode==UNAVAIL ) return true;
      if( alwaysUp ) pop();
      postEvent(new Event(new Button(baratin),Event.ACTION_EVENT,baratin) );
      return true;
   }


   PopupMenu pm=null;

   public synchronized void add(PopupMenu pm) {
      this.pm=pm;
      super.add(pm);
   }

   /** Reactions aux differents boutons du menu */
    public boolean action(Event e, Object o) {
      String s;
      try { s = (String) o; } catch( Exception eact ) { return true; }

//      System.out.println("action o=["+s+"]");
      return false;
   }

   public boolean mouseMove(Event e, int x, int y ) { 
       if( x>strWidth+5 ) {
           if( inCanvas ) {
            inCanvas = false;
            Aladin.makeCursor(this, Aladin.DEFAULTCURSOR);
            repaint();
           }
       }
       else if( !inCanvas ) {
           inCanvas = true;
           Aladin.makeCursor(this, Aladin.HANDCURSOR);
           repaint();
       }
       return true;
       
   }
   public boolean mouseEnter(Event e, int x, int y) { if( mode==UP ) { color=Aladin.COLOR_GREEN; repaint(); } return true;}
   public boolean mouseExit(Event e, int x, int y) { color=Color.black; repaint(); return true;}

   static boolean flagShift=false;
   synchronized private void setFlagShift(boolean flag) { flagShift=flag; }

  /** Retourne true si la touche Key est pressee */
   synchronized static boolean shiftDown() { return flagShift; }

   public boolean handleEvent(Event e) {
/*
      if( aladin!=null && aladin.inHelp ) {
          
         if( e.id==Event.MOUSE_ENTER ) aladin.help.setText(help);
         else if( e.id==Event.MOUSE_UP ) aladin.helpOff();
         return true;
      }
      */
      
      if( e.id==Event.MOUSE_ENTER ) {
          if( e.x<strWidth+5 ) {
              inCanvas = true; 
              Aladin.makeCursor(this, Aladin.HANDCURSOR);
              repaint();
          }
      } 
      else if( e.id==Event.MOUSE_EXIT ) {
          inCanvas = false;
          Aladin.makeCursor(this, Aladin.DEFAULTCURSOR);
          repaint();
      }

      // Memorisation de la touche Shift
      if( e.target instanceof MyButton ) setFlagShift( e.shiftDown());
      if( e.id==Event.MOUSE_EXIT && alwaysUp && mode!=UNAVAIL ) pop();
      if( baratin==null ) return super.handleEvent(e);

/*
      // Pour le Help
      if( type==NORMAL && Aladin.inHelp ) {
         if( e.id==Event.MOUSE_ENTER ) {
            if( text.equals(Aladin.MHLP2) ) aladin.help.setDefault();
            else aladin.help.setText(help);
         }
         return super.handleEvent(e);
      }
*/
/*
      if( status!=null && type!=NORMAL ) {
         // Baratin sur les boutons dans les selecteur de serveurs
         if( e.id==Event.MOUSE_ENTER ) status.setText(baratin);
         else if( e.id==Event.MOUSE_EXIT ) status.setText("");

      } else {
         // Baratin sur les boutons
         if( e.id==Event.MOUSE_ENTER ) aladin.status.setText(baratin);
         else if( e.id==Event.MOUSE_EXIT ) aladin.status.setText("");
      }
*/
      return super.handleEvent(e);
   }

   // Dessin du bouton
   void draw(Graphics g) {
      Color c = (mode!=UNAVAIL)?Color.black:Color.gray;

      // Dessin du fond
      g.setColor(getBackground());
      g.fillRect(0,0,W,H);

      // Couleur du fond du bouton
      if( mode==DOWN ) {
         g.setColor(type!=NORMAL?Aladin.COLOR_CONTROL_BACKGROUND:Color.gray);
         g.fillRect(2,2,W-4,H-4);
      }

      // Dessin des bords
      /*
      g.setColor( Color.black );
      if( type!=NORMAL && mode==DOWN ) {
         g.drawRect(1,1,W-3,H-2);

         int x=(type==RIGHT)?0:W-2;
         int y=H/3 - TRI;
         g.setColor( Aladin.LGRAY );
         g.fillRect(x,y,2,TRI*2);
         g.setColor( Color.black );
         g.drawLine(x,y,x+2,y);
         g.drawLine(x,y+TRI*2,x+2,y+TRI*2);

      } else {
         g.setColor( (mode!=DOWN)?Color.white:Color.darkGray);
         g.drawLine(1,1,W-2,1);
         g.drawLine(1,1,1,  H-2);
         g.setColor( mode!=DOWN?Color.darkGray:Color.white );
         g.drawLine(W-2,1,  W-2,H-2);
         g.drawLine(W-2,H-2,1,  H-2);
      }
      */
   }

   boolean inCanvas = false;

   public void update(Graphics g) {
      paint(g);
   }
   public void paint(Graphics g) {
      W = getSize().width;
      H = getSize().height;
      draw(g);
//      if( w!=W || h!=H ) { W=w; H=h; update(g); return; }

      // La fonte et la couleur
      g.setFont(f);
      g.setColor(Color.blue);

      // Le texte
      
      if( text!=null && !text.equals("") ) {
         //StringTokenizer st = new StringTokenizer(text,"\n");
         //int nligne = st.countTokens();
         int y = H/2 - (1*(Aladin.SIZE+4))/2 + Aladin.SIZE+2;
         String s = text;
         int l = strWidth;
         //int x=W/2-l/2;
         int x = 0;
         g.drawString(s,x,y);
         if( inCanvas ) g.drawLine(0,y+1,l,y+1);
         
      }
   }

}

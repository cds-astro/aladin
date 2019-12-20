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

package cds.aladin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.JComponent;
import javax.swing.JLabel;

import cds.tools.Util;

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
public class MyButton extends JComponent implements MouseListener {

   // Les constantes
   static final int UP      =  1;
   static final int DOWN    = -1;
   static final int UNAVAIL =  0;

   static final int NORMAL     = 0;
   static final int NORMALRD   = 1;
   static final int LEFT       = 2;
   static final int RIGHT      = 3;
   static final int TOP        = 4;

   static final int TRI     = 7;

   static final int NOLABEL = 0;
   static final int WITHLABEL = 1;

   // couleurs pour dessiner le triangle
   static final Color GREY = new Color(106,106,106);
   static final Color LIGHT_GREY = new Color(228,228,228);
   
   /** La phrase d'explication du bouton */
   protected String baratin;
   /** Le nom du bouton */
   protected String text;
   protected String label;
   String help=null;		// Le help associe au bouton
   Image image=null; // Image de fond, sinon null
   private int imageMode=NOLABEL; // Affichage ou non du label dans le cas d'une image

   // Les references
   Aladin aladin;
   JLabel status=null;

   // Les variables d'etats
   boolean modeMenu; // true s'il s'agit d'un bouton type menu (sans cadre par défaut)
   boolean alwaysUp=true;	// Indique s'il s'agit d'un bouton toujours UP
   int mode=UP;			// Etat UP, UNAVAIL, DOWN, du bouton
   int type = NORMAL;		// si !=NORMAL, => fortement lie a Aladin
   				//   (baratin sera affiche dans aladin.status(),
                                //    type de bord LEFT ou RIGHT ou TOP...)

   boolean mouseOverChange = true; // si true, changement de couleur quand on survole le bouton
   boolean withTriangle = false; // si true, on dessine un petit triangle à la droite du label
   
   // Les composants du bouton
   int W = 75;			// Largeur minimale
   static final int MARGE=10;	// Marge
   int H = Aladin.LSIZE+MARGE;	// Hauteur du bouton pour une ligne
   private Color color= Color.black;  // Couleur du texte
   Font f=null;			// Font du bouton
   private boolean last=false; // true si le bouton est le dernier de sa colonne

  /** Creation d'un bouton.
   * @param aladin  Reference
   * @param s       Le label du bouton
   */
   protected MyButton(Aladin aladin,String s) {
      label=s;
      suite(aladin,s,null,null,true);
   }

  /** Creation d'un bouton.
   * @param aladin  Reference
    * @param s       Le label du bouton
    */
    protected MyButton(Aladin aladin,String s, boolean mouseOverChange, boolean withTriangle) {
    	this.withTriangle = withTriangle;
    	W += 17;
    	this.mouseOverChange = mouseOverChange;
        label=s;
    	suite(aladin,s,null,null,true);
    }

  /** Creation d'un bouton.
   * @param aladin  Reference
   * @param status  Label dans lequel il faudra afficher le baratin d'explication
   *                si le type est ``NORMAL''
   * @param type    Type de bouton (si != NORMAL, affichage du status dans
   *                aladin.status
   * @param s       Le label du bouton
   * @param baratin L'explication associee au bouton
   */
   protected MyButton(Aladin aladin,JLabel status,int type,
                      String s, String baratin) {
      this.status = status;
      this.type = type;
      label=s;
      if( type==LEFT || type==RIGHT || type==TOP ) {
         if( s.length()>10 && s.indexOf('\n')<0 ) label=(new StringTokenizer(s)).nextToken();
      }
      suite(aladin,s,baratin,null,true);
   }

  /** Creation d'un bouton.
   * @param aladin  Reference
   * @param s       Le label du bouton
   * @param baratin L'explication (courte) associee au bouton
   * @param help    Le help associee au bouton
   */
   protected MyButton(Aladin aladin,String s, String baratin, String help) {
      label=s;
      suite(aladin,s,baratin,help,true);
   }
   protected MyButton(Aladin aladin,String s, String baratin,
                      String help,boolean enable) {
      label=s;
      suite(aladin,s,baratin,help,enable);
   }

  /** Generation d'un bouton (procedure interne) */
   void suite(Aladin aladin,String s, String baratin,
              String help,boolean enable) {
      this.aladin = aladin;
      addMouseListener(this);
      this.baratin = baratin;
      if( baratin!=null ) Util.toolTip(this,Util.fold(baratin,60,true));
      text=s;
      mode=enable?UP:UNAVAIL;
      if( help!=null ) this.help=help;
      f=Aladin.LBOLD;
      H=getH();
   }
   
   public Dimension getPreferredSize() { return new Dimension(W,H); }
   public void setPreferredSize( Dimension d ) { W=d.width; H=d.height; }

  /** Changement de la font */
   public void setFont(Font f) {
      this.f = f;
      FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
      W = fm.stringWidth(text)+10;
      if( withTriangle ) W+=17;
   }

   /** Positionne le mode du bouton (true => pas de bord ) */
   protected void setModeMenu(boolean flag) { modeMenu=flag; }
   
   /** Positionne une imagette de fond. Eventuellement, remplace
    * le label du bouton
    * @param img L'image de fond
    * @param mode NOLABEL ou WITHLABEL
    */
   protected void setBackGroundLogo(Image img,int mode) {
      if( img==null ) return;
   	  imageMode = mode;
      image = img;
   }

   private int getH() {
   	  if( type==TOP ) return 35;
   	  if( type==LEFT || type==RIGHT ) return 39;
      int n=1;
      if( text!=null ) {
         char [] a = text.toCharArray();
         for( int i=0; i<a.length; i++ ) if( a[i]=='\n' ) n++;
      }
      return Aladin.LSIZE*n+MARGE;
   }
   
  /** Modification du nom label du bouton.
   * @param s le nouveau label
   */
   protected void setLabel(String s) { text=s; repaint(); }

   /** Spécifie que le bouton sera arrondi */
   protected void setRond() { this.type=NORMALRD; }

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
   protected void push() { if( mode!=UNAVAIL) { mode=DOWN; repaint();}  }

  /** Relache le bouton */
   protected void pop()  {  if( mode!=UNAVAIL) { mode=UP;   repaint();} }

   private boolean flagShowPopup=false;
   
   public void mousePressed(MouseEvent e) {
      if( aladin.inHelp ) return;
      if( mode==UNAVAIL ) return;
      push();
   }

   public void mouseReleased(MouseEvent e) {
      if( mode==UNAVAIL ) return;
      if( pm==null ) postEvent(new Event(new Button(text),Event.ACTION_EVENT,text) );
      else { flagShowPopup=true; color=Color.black; }
      if( alwaysUp ) pop();
   }


   PopupMenu pm=null;

   public synchronized void add(PopupMenu pm) {
      this.pm=pm;
      super.add(pm);
   }

   public void mouseEntered(MouseEvent e) { if( mode==UP && mouseOverChange ) { color=Aladin.COLOR_GREEN; repaint(); } }
   public void mouseExited(MouseEvent e) { color=Color.black; repaint(); }

   /** Remet en mode normal le bouton (la souris n'est plus dessus) */
   protected void normal() { color=Color.black; }

   static boolean flagShift=false;
   synchronized private void setFlagShift(boolean flag) { flagShift=flag; }

  /** Retourne true si la touche Key est pressee */
   synchronized static boolean shiftDown() { return flagShift; }

   public boolean handleEvent(Event e) {
      
      if( aladin!=null && aladin.inHelp ) {
         if( e.id==Event.MOUSE_ENTER ) aladin.help.setText(help);
         else if( e.id==Event.MOUSE_UP ) aladin.helpOff();
         return true;
      }

      // Memorisation de la touche Shift
      if( e.target instanceof MyButton ) setFlagShift( e.shiftDown());
      if( baratin==null ) return super.handleEvent(e);

     if( status!=null && type!=NORMAL ) {
         // Baratin sur les boutons dans les selecteur de serveurs
         if( e.id==Event.MOUSE_ENTER ) status.setText(baratin);
         else if( e.id==Event.MOUSE_EXIT ) status.setText("");

      } else {
         // Baratin sur les boutons
         if( e.id==Event.MOUSE_ENTER ) aladin.status.setText(baratin);
         else if( e.id==Event.MOUSE_EXIT ) aladin.status.setText("");
      }

      return super.handleEvent(e);
   }
   
   /** Dessin d'un pourtour d'onglet arrondi gauche */
   private void drawCircLeft(Graphics g,int x,int y,int width, int height,
         int b,Color fgd,Color fgl) {
      g.setColor(fgd);
      g.drawLine(x+b/2,y,x+width,y);
      g.drawArc(x,y,b,b,90,90);
      g.drawLine(x,y+b/2,x,y+height-b/2);
      g.drawArc(x,y+height-b,b,b,180,45);
      g.setColor(fgl);
      g.drawArc(x,y+height-b,b,b,235,45);
      g.drawLine(x+b/2,y+height,x+width,y+height);
   }
   
   /** Dessin d'un fond d'onglet arrondi gauche */
   private void fillCircLeft(Graphics g,int x,int y,int width, int height,
         int b,Color bg) {
      g.setColor(bg);
      g.fillRect(x+b/2,y,width-b/2,height);
      g.fillRect(x,y+b/2,b/2,height-b);
      g.fillArc(x,y,b,b,89,92);
      g.fillArc(x,y+height-b,b,b,189,92);
   }

   /** Dessin d'un pourtour d'onglet arrondi droite */
   private void drawCircRight(Graphics g,int x,int y,int width, int height,
         int b,Color fgd,Color fgl) {
      g.setColor(fgd);
      g.drawLine(x,y,x+width-b/2,y);
      g.drawArc(x+width-b,y,b,b,0,90);
      g.drawLine(x+width,y+b/2,x+width,y+height-b/2);
      g.drawArc(x+width-b,y+height-b,b,b,270,90);
      g.setColor(fgl);
      g.drawLine(x,y+height,x+width-b/2,y+height);
   }
   
   /** Dessin d'un fond d'onglet arrondi droite */
  private void fillCircRight(Graphics g,int x,int y,int width, int height,
         int b,Color bg) {
      g.setColor(bg);
      g.fillRect(x,y,width-b/2,height);
      g.fillRect(x+width-b/2,b/2,b/2,height-b);
      g.fillArc(x+width-b,0,b,b,-1,92);
      g.fillArc(x+width-b,height-b,b,b,279,92);
   }

  /** Dessin d'un pourtour d'onglet arrondi haut */
  private void drawCircTop(Graphics g,int x,int y,int width, int height,
         int b,Color fgd,Color fgl) {
      g.setColor(fgd);
      g.drawLine(x,y+height,x,y+b/2);
      g.drawArc(x,y,b,b,90,90);
      g.drawLine(x+b/2,y,x+width-b/2,y);
      g.drawArc(x+width-b,y,b,b,0,90);
      g.drawLine(x+width,y+b/2,x+width,y+height);
   }
   
  /** Dessin d'un fond d'onglet arrondi haut */
   private void fillCircTop(Graphics g,int x,int y,int width, int height,
         int b,Color bg) {
      g.setColor(bg);
      g.fillRect(x,y+b/2,width,height-b/2);
      g.fillRect(x+b/2,y,width-b,b/2);
      g.fillArc(x,y,b,b,89,92);
      g.fillArc(x+width-b,y,b,b,-1,92);
   }
   
   /** Dessin d'un pourtour de bouton arrondi */
  private void drawCirc(Graphics g,int x,int y,int width, int height,
         int b,Color fgd,Color fgl) {
      g.setColor(fgd);
      g.drawArc(x+width-b,y,b,b,45,45);
      g.drawLine(x+b/2,y,x+width-b/2,y);
      g.drawArc(x,y,b,b,90,90);
      g.drawLine(x,y+b/2,x,y+height-b/2);
      g.drawArc(x,y+height-b,b,b,180,45);
      
      g.setColor(fgl);
      g.drawArc(x,y+height-b,b,b,225,45);
      g.drawLine(x+b/2,y+height,x+width-b/2,y+height);
      g.drawArc(x+width-b,y+height-b,b,b,280,90);
      g.drawLine(x+width,y+height-b/2,x+width,y+b/2);
      g.drawArc(x+width-b,y,b,b,0,45);
      
      g.setColor(Color.gray);
      g.drawArc(x,y+height-b,b,b-1,225,45);
      g.drawLine(x+b/2,y+height-1,x+width-b/2,y+height-1);
      g.drawArc(x+width-b,y+height-b,b-1,b-1,280,90);
//      g.drawArc(x+width-b,y+height-b,b-1,b,280,90);
//      g.drawArc(x+width-b,y+height-b,b,b-1,280,90);
      g.drawLine(x+width-1,y+height-b/2-1,x+width-1,y+b/2);
      g.drawArc(x+width-b,y-1+1,b-1,b,0,45);

   }
   
  /** Dessin d'un fond de bouton arrondi */
   private void fillCirc(Graphics g,int x,int y,int width, int height,
         int b,Color bg) {
      g.setColor(bg);
      g.fillRect(x,y+b/2,width,height-b);
      g.fillRect(x+b/2,y,width-b,b/2);
      g.fillRect(x+b/2,y+height-b/2,width-b,b/2);
      g.fillArc(x,y,b,b,90,90);
      g.fillArc(x+width-b,y,b,b,-1,92);
      g.fillArc(x,y+height-b,b,b,179,92);
      g.fillArc(x+width-b,y+height-b,b,b,279,92);
   }
   
   /** Méthode générique de dessin d'un bouton ou d'un onglet
    * (bordure et fond) suivant la variable mode */
   private void drawFond(Graphics g,Color bg,Color fgd,Color fgl) {
      int b=10;
      int d = mode==DOWN ? 0 : 2;
      switch(type) {
         case TOP:   fillCircTop(g,0,d,last?W-1:W,H-d,b,bg); 
                     drawCircTop(g,0,d,last?W-1:W,H-d,b,fgd,fgl);
                     break;
         case LEFT:  fillCircLeft(g,d,0,W-d,last?H-1:H,b,bg); 
                     drawCircLeft(g,d,0,W-d,last?H-1:H,b,fgd,fgl);
                     break;
         case RIGHT: fillCircRight(g,0,0,W-1-d,last?H-1:H,b,bg); 
                     drawCircRight(g,0,0,W-1-d,last?H-1:H,b,fgd,fgl);
                     break;
         case NORMALRD: fillCirc(g,0,0,W-1,H-1,b+6,bg);
                     drawCirc(g,0,0,W-1,H-1,b+6,fgd,fgl);
                     break;
         default:    g.setColor(bg);  g.fillRect(0,0,W,H);
                     g.setColor(fgd);
                     g.drawLine(0,0,W,0); g.drawLine(0,0,0,H);
                     g.drawLine(1,1,W,1); g.drawLine(1,1,1,H);
                     g.setColor(fgl);
                     g.drawLine(0,H-1,W,H-1); g.drawLine(W-1,0,W-1,H);
                     g.drawLine(1,H-2,W-2,H-2); g.drawLine(W-2,1,W-2,H-2);
      }
   }
   
   // Dessin du bouton
   void draw(Graphics g) {
      
      if( mode==DOWN ) {
         if( isNormal() ) drawFond(g, Aladin.MAXBLUE,Color.black,Color.white);
         else drawFond(g, Aladin.BLUE,Color.black,Color.black);
         
      } else if( color==Aladin.COLOR_GREEN ) {
         if( isNormal() ) drawFond(g,Aladin.MYBLUE,Color.white,Color.black);
         else drawFond(g,Aladin.MYBLUE,Color.gray,Color.gray);
         
      } else {
         if( !isNormal()) drawFond(g, new Color(250,249,245), Color.gray,Color.gray);
         else if( type==NORMAL ) {
            drawFond(g, bkgdColor!=null ? bkgdColor : Aladin.BLUE,Color.white,Color.gray );
         } else {
            drawFond(g, bkgdColor!=null ? bkgdColor : Aladin.BLUE,Color.gray, Color.black);
            
         }
      }
      
      int dh=0, dw=0;
      if( mode!=DOWN ) {
         dw = type==TOP ? 0 : type==LEFT ? 2 : -2;
         dh = type==TOP ? 2 : 0;
      }
      
      if( image!=null ) {      
         int h = image.getHeight(this);
     	 if( h>H ) g.drawImage(image,2+dw,2+dh,W-4,H-4,this);
     	 else g.drawImage(image,2+dw,2+dh+(H-h)/2,this);
      }
   }
   
   /** Retourne true s'il s'agit d'un bouton normal (pas un onglet) */
   private boolean isNormal() { return type==NORMAL || type==NORMALRD; }
   
   
   static Dimension DIM = null;
   
   public Dimension getSize() { 
      if( DIM==null ) DIM = new Dimension(W,H);
      return DIM;
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
//      W = getSize().width;
//      H = getSize().height;     
      
      if( aladin!=null ) aladin.setAliasing(g);
      // Le fond et le bord
      draw(g);

      if( image!=null && imageMode==NOLABEL ) return;

      // La fonte et la couleur
      if( mode==UNAVAIL ) {
         g.setFont( f );
         g.setColor(Color.gray);
      } else {
         g.setFont( f );
         g.setColor( color==Aladin.COLOR_GREEN && image==null && mode==UP ? Color.white : getForeground());
      }

      // Le texte
      int dw=0; 
      if( mode!=DOWN && type!=TOP && !isNormal() )  dw = type==LEFT ? 2 : -2;
      
      FontMetrics m = g.getFontMetrics();
      if( label!=null && !label.equals("") ) {
         StringTokenizer st = new StringTokenizer(label,"\n");
         int nligne = st.countTokens();
         int y = H/2 - (nligne*(Aladin.SIZE+4))/2 + Aladin.SIZE;
         for( int i=0; i<nligne; i++ ) {
            String s = st.nextToken();
            int l = m.stringWidth(s);
            int x=W/2-l/2+dw;
            if( withTriangle ) x -= 9;
            g.drawString(s,x,y);
            y+=Aladin.SIZE+4;
         }
      }
      
      // Pour le sous-menu
      if( flagShowPopup ) {
         flagShowPopup=false;
         pm.show(this,10,H/2+10);
      }

   }
   
   // couleur de fond du bouton
   private Color bkgdColor=null;
   public void setBackground(Color c) {
   	  if( c==null ) super.setBackground(aladin.getBackground());
	  else super.setBackground(c);
   	  
      bkgdColor = c;
      repaint();
   }
   
   /**
    * @param color The color to set.
    */
   protected void setColor(Color color) {
      this.color = color;
   }
   
   /** Indique que ce bouton est le dernier dans sa colonne */
   protected void setLastInColumn() {
      last=true;
   }
   
   /**
    * @return Returns the color.
    */
   protected Color getColor() {
      return color;
   }
   
   public void mouseClicked(MouseEvent e) { }
}

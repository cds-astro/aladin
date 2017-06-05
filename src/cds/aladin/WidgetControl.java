// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;

import cds.tools.Util;

/**
 * Objet permettant de g�rer sous la forme d'un "Widget" un JComponent
 * en superposition d'un � JPanel classique. Par exemple, permet l'utilisation
 * de la boite de boutons d'Aladin (ToolBox) en superposition d'une vue (ViewSimple).
 *
 * Le JComponent doit implanter l'interface Widget, et le JPanel "parent"
 * doit utiliser un WidgetController pour l'affichage et la transmission des
 * �v�nements des widgets en superposition.
 *
 * @see aladin.view.ViewSimple
 * @see aladin.view.ToolBox
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : jan 2015 - cr�ation
 */
public class WidgetControl {

   // Poign�es pour �tirer le rectangle
   static final private int IN=0,HG=1,H=2,HD=3,D=4,BD=5,B=6,BG=7,G=8,INHEAD=9,INCOLLAPSED=10;
   static final private int NW=00, NE=0x10 , SE=0x11, SW=0x01;

   static final public int NOTHING=0, DISPOSE=1, UP=2;

   // Curseur en fonction de la position de la souris
   static final int [] CURSOR = { Cursor.HAND_CURSOR, Cursor.NW_RESIZE_CURSOR, Cursor.N_RESIZE_CURSOR,
      Cursor.NE_RESIZE_CURSOR, Cursor.E_RESIZE_CURSOR,Cursor.SE_RESIZE_CURSOR,
      Cursor.S_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR, Cursor.W_RESIZE_CURSOR };

   static final private Color BLUE = new Color(150,150,255);    // Couleur de l'ent�te
   static final private int HB=12;                              // Hauteur de l'ent�te
   static final private int WCOLLAPSED = 25;                    // Taille du bouton (mode collapsed)
   static final private int DS=5;                               // Taille des poign�es de redimensionnement
   static final private int W=16;                               // Taille de la sensibilit� des poign�es d'�tirement

   private int position=NW;             // Originine de la position NW,NE,SE,SW;
   private int x,y;                     // Position relative � NW, NE, SE ou SW
   private int width,height;            // Taille du widget
   private float opacity=1.0f;          // Niveau d'opacit� (0..1)
   private JComponent c;                // Le widget (doit implanter l'interface ViewOverlay)
   private Rectangle closeButton;       // Emplacement du bouton close (corrdonn�es locales)
   private boolean movable=true;        // true si le widget est d�pla�able
   private boolean resizable=false;     // true si le widget peut �tre redimensionn�
   private boolean collapsable=true;    // true si le widget peut �tre collaps�
   private JComponent parent=null;      // Parent du widget (celui qui g�re un ViewOverlayController)

   // les diff�rents flags d'�tat
   private boolean isCollapsed=false;   // true si le widget est collaps� (sous forme de bouton)
   private boolean in=false;            // true si la souris se trouve dans le widget
   private boolean inHead=false;        // true si la souris se trouve dans l'ent�te au-dessus du widget
   private boolean inCollapsed=false;   // true si la souris se trouve dans le bouton du collapse
   private boolean inShowHead=false;    // true si l'ent�te est actuellement visible
   private boolean flagControl=false;   // true si les poign�es de controle de dimensionnement sont visibles
   private int poignee=-1;              // Code de la poign�e sous la souris

   // Flags li�s � un drag en cours
   private int dragPoignee=-1;          // Index de la poign�e en cours d'utilisation
   private int xDrag=0,yDrag=0;         // Position initiale d'un drag (coordonn�es locales)
   private boolean flagMove=false; // true si on est en train d'effectuer un d�placement
   private boolean draggedDone=false;   // true si on est pass� par la m�thode mouseDragged depuis le dernier mousePressed

   /**
    * Cr�ation d'un objet de contr�le de l'overlay d'un component qui va s'afficher sous forme d'un
    * widget en superposition d'un component parent. Le widget sera par d�faut "collaps�" sous forme d'un
    * bouton. Pour l'afficher imm�diatement sous forme normal, il est n�cessaire d'appeler la
    * m�thode setCollapsed(false). De m�me, le widget est par d�faut d�pla�able et non redimensionnable.
    * Pour modifier ce comportement, il est n�cessaire d'utiliser les m�thodes setResizable(boolean)
    * et setMovable(boolean).
    *
    * @param c Le widget (doit implanter l'interface ViewOverlay)
    * @param x Abscisse du widget (rep�re du parent � partir du coin HG)
    * @param y Ordonn�e du widget (rep�re du parent � partir du coin HG)
    * @param width Largeur du widget ou -1 si on garde la largeur courante du component c
    * @param height Hauteur du widget ou -1 si on garde la hauteur courante du component c
    * @param opacity Niveau d'opacit� ([0 .. 1], -1 pour totalement opaque
    * @param parent Parent sur lequel va s'afficher en superposition le widget
    */
   public WidgetControl(JComponent c,int x, int y, int width, int height, float opacity,JComponent parent ) {
      this.c=c;
      this.parent=parent;
      setX(x);
      setY(y);
      if( opacity<=0 ) opacity=1.0f;
      setOpacity(opacity);
      if( width<=0) width=c.getWidth();
      if( height<=0 ) height=c.getHeight();
      setSize(width, height);
      setCollapsed(true);
   }

   /** Modifie la taille du widget */
   public void setSize(int width,int height) {
      c.setSize(width, height);
      this.width = c.getWidth();
      this.height = c.getHeight();
   }

   /** Modifie le niveau d'opacit� [0 .. 1] */
   public void setOpacity(float opacity)   { this.opacity=opacity; }

   /** Modifie le caract�re d�pla�able du widget */
   public void setMovable(boolean flag)    { movable=flag; }

   /** Modifie le caract�re redimensionnable du widget */
   public void setResizable(boolean flag)  { resizable=flag; }

   /** Modifie le caract�re collapsable du widget */
   public void setCollapsable(boolean flag)   { collapsable=flag; }

   /** Collapse ou d�-collapse le widget. Attention, il est n�cessaire
    * d'ajuster les coordonn�es car le bouton doit rester le long de la marge
    * la plus proche (voir la variable "position") */
   public void setCollapsed(boolean flag) {
      if( flag==isCollapsed ) return;
      isCollapsed=flag;
      int x=getX();
      int y=getY();
      int dx=0, dy=0;
      if( aDroite() ) dx = width-WCOLLAPSED;
      if( enBas() ) dy = height-WCOLLAPSED;
      if( isCollapsed ) { x+=dx; y+=dy; }
      else { x-=dx; y-=dy; }
      setX(x);
      setY(y);
      flagMove=draggedDone=false;
   }

   // Retourne l'absisse courante du widget en coordonn�e absolue du parent (origine HG)
   public int getX() {
      if( parent==null ) return x;
      if( aDroite()  ) return parent.getWidth()-x;
      return x;
   }

   // Retourne l'ordonn�e courante du widget en coordonn�e absolue du parent (origine HG)
   public int getY() {
      if( parent==null ) return y;
      if( enBas() ) return parent.getHeight()-y;
      return y;
   }

   // Positionne l'absisse du widget (coordonn�e absolue du parent origine HG
   private void setX(int x) {
      if( parent==null ) this.x=x;
      else {
         if( x>parent.getWidth()/2 ) position |= 0x10;
         else position &= 0x01;
      }
      if( aDroite()  ) this.x = parent.getWidth()-x;
      else this.x=x;
   }

   // Positionne l'ordonn�e du widget (coordonn�e absolue du parent origine HG
   private void setY(int y) {
      if( parent==null ) this.y=y;
      else {
         if( y>parent.getHeight()/2 ) position |= 0x01;
         else position &= 0x10;
      }
      if( enBas() ) this.y = parent.getHeight()-y;
      else this.y=y;
   }

   // True si le widget se trouve sur la droite du parent
   private boolean aDroite() { return (position & 0x10) !=0; }

   // True si le widget se trouve en bas du parent
   private boolean enBas()   { return (position & 0x01) !=0; }

   /** Force le parent � afficher le m�me curseur courant que celui du widget.
    * @return Le curseur courant du widget
    */
   public Cursor getCursor() {
      if( parent==null ) return null;
      Cursor cursor = c.getCursor();
      parent.setCursor( cursor );
      return cursor;
   }

   /** True si le widget est en cours d'activation (poign�ees de redimensionnement
    * visibles, d�placement en cours  */
   public boolean isActivated(MouseEvent e) {
      isMouseIn(e);   // Pour mettre � jour les flags d'�tats (in, inHead...)
      return flagMove || flagControl /* || isCollapsed && draggedDone */;
   }

   /** True si le widget est affich� sous forme d'une ic�ne */
   public boolean isCollapsed() { return collapsable; }
   
   /** True si la souris se trouve sur le widget (collaps� ou non).
    * Dans le cas o� une entr�e ou une sortie du widget est d�tect�e
    * et que d'autre part le widget implante l'interface MouseListener, l'�v�nement
    * est transmis au component widget.
    * En profite pour mettre � jour les flags in,inHead et inShowHead */
   public boolean isMouseIn(MouseEvent e) {
      int xm=e.getX();
      int ym=e.getY();
      int x = getX();
      int y = getY();

      // Si le widget est collaps�
      if( isCollapsed ) {
         in = x<=xm && xm<=x+WCOLLAPSED && y<=ym && ym<y+WCOLLAPSED;
         return in;
      }

      int m = flagControl ? W/2 : 0;
      boolean inNow = x-m<=xm && xm<=x+width+m && y-HB-m<=ym && ym<y+height+m;

      // Si on est entr�, ou sorti, l'�v�nement est transmis au component
      if( c instanceof MouseListener && in!=inNow) {
         e.translatePoint(-x, -y);
         if( !in && inNow ) ((MouseListener)c).mouseEntered(e);
         else if( in && !inNow ) ((MouseListener)c).mouseExited(e);
         e.translatePoint(x, y);
      }

      // Mise � jour des flags d'�tats
      in=inNow;
      inHead = (movable || resizable || collapsable)
            && x<=xm && xm<=x+width && y-HB<=ym && ym<=y;
      inShowHead = (movable || resizable || collapsable )
            && x<=xm && xm<=x+width && y-HB<=ym && ym<=y+HB;

      return inNow || inHead;
   }

   /** Tranmission au component de l'�v�nement mouseMoved. Dans le cas o� les poign�es
    * de contr�le du dimensionnement sont visibles, en profite positionner le curseur sur le component et son parent */
   public void mouseMoved(MouseEvent e) {
      int x = getX();
      int y = getY();

      inCollapsed=(closeButton!=null && closeButton.contains(e.getX()-x, e.getY()-y));

      if( flagControl ) {
         poignee = getPoignee(e.getX()-x, e.getY()-y);
         if( poignee>=0 && poignee<CURSOR.length) {
            Cursor cursor = Cursor.getPredefinedCursor(CURSOR[poignee]);
            c.setCursor(cursor);
         }
      }

      if( !(c instanceof MouseMotionListener )) return;
      e.translatePoint(-x, -y);
      ((MouseMotionListener)c).mouseMoved(e);
      e.translatePoint(x, y);
   }

   /** Tranmission au widget de l'�v�nement mousePressed. Dans le cas o� l'on se trouve dans l'ent�te
    * initie un d�but de d�placement ou un affichage des poign�es de controle du redimensionnement
    * (qui sera confirm� dans le mouseReleased(...) si on n'a pas fait de mouseDragged(...) entre temps */
   public void mousePressed(MouseEvent e) {
      if( inHead && !flagControl && movable ) startMove(e);
      else if( flagControl && resizable )     startResize(e);
      else if( !(c instanceof MouseListener )) return;
      else {
         int x = getX();
         int y = getY();
         e.translatePoint(-x, -y);
         ((MouseListener)c).mousePressed(e);
         e.translatePoint(x, y);
      }
      draggedDone=false;
   }

   // Initialisation d'un d�placement du widget
   private void startMove(MouseEvent e) {
      xDrag=e.getX();
      yDrag=e.getY();
      dragPoignee=INHEAD;
      flagMove=true;
   }

   // Initialisation d'un d�placement des poign�es
   private void startResize(MouseEvent e) {
      xDrag=e.getX();
      yDrag=e.getY();
      int x = getX();
      int y = getY();
      dragPoignee = getPoignee(xDrag-x, yDrag-y);
   }

   /** Transmission au widget de l'�v�nement mouseReleased. Dans le cas o� un d�placement
    * ou un redimensionnement du widget a �t� effectu�, l'action est achev�e.
    * @return Le code UP, DISPOSE ou NOTHING pour informer le ViewOverlayController de ce qu'il doit faire
    */
   public int mouseReleased(MouseEvent e) {


      int ret = inHead && !draggedDone ? UP : NOTHING;
      int x = getX();
      int y = getY();

      int act = getAction(e.getX()-x, e.getY()-y);

      // Dans le bouton de collapsed
      // RQ: Le CLOSE (suppression pur et simple du widget) N'EST PAS ENCORE IMPLANTE
      if( act==INCOLLAPSED && !draggedDone ) {
         /* if( closable ) return DISPOSE;
         else */ setCollapsed(!isCollapsed);
      }

      // Fin d'un redimensionnement
      else if( flagControl ) {
         flagControl = !draggedDone && !(poignee>IN && poignee<INHEAD) ? false : true;
         draggedDone=false;
         setSize(width, height);
      }

      // Passage d'un d�placement � un controle de taille (car il n'y a pas eu de d�placement)
      else if( flagMove && !draggedDone && resizable ) {
         flagControl=true;
         flagMove=false;
      }

      // Fin d'un d�placement
      else if( flagMove ) flagMove=false;

      else if( !(c instanceof MouseListener )) return ret;

      else {
         e.translatePoint(-x, -y);
         ((MouseListener)c).mouseReleased(e);
         e.translatePoint(x, y);
      }

      draggedDone=false;
      return ret;
   }

   /** Transmission au widget de l'�v�nement mouseDragged. */
   public void mouseDragged(MouseEvent e) {
      // Le d�placement du bouton en mode dragged n'est pas autoris�
      if( isCollapsed ) return;

      // Gestion d'un d�placement ou d'un redimensionnement
      draggedDone=true;
      if( flagMove || flagControl ) { mouseDrag(e); return; }

      // Transmission au widget
      if( c instanceof MouseMotionListener ){
         int x = getX();
         int y = getY();
         e.translatePoint(-x, -y);
         ((MouseMotionListener)c).mouseDragged(e);
         e.translatePoint(x, y);
      }
   }

   /** Transmission au widget de l'�v�nement mouseWheel. Si la souris se trouve
    * sur l'ent�te, l'opacit� du widget sera modifi� par la molette
    * de la souris.*/
   public void mouseWheel(MouseWheelEvent e) {

      // Changement de l'opacit� du widget via la molette
      if( inHead ) {
         opacity += e.getWheelRotation()/5.;
         if( opacity<0.2 ) opacity=0.2f;
         else if( opacity>1 ) opacity=1f;
      }

      // Transmission au widget
      else if( c instanceof MouseWheelListener) {
         int x = getX();
         int y = getY();
         e.translatePoint(-x, -y);
         ((MouseWheelListener)c).mouseWheelMoved(e);
         e.translatePoint(x, y);
      }
   }

   // Ev�nement lors d'un clic & drag => d�placement ou redimensionnement
   private void mouseDrag(MouseEvent e) {
      int dx = e.getX()-xDrag;
      int dy = e.getY()-yDrag;
      if( dx==0 && dy==0 ) return;

      int x = getX();
      int y = getY();
      xDrag=e.getX();
      yDrag=e.getY();

      switch(dragPoignee) {
         case INHEAD:
         case IN: x+=dx; y+=dy; break;
         case HG: x+=dx; y+=dy; width-=dx; height-=dy; break;
         case HD: y+=dy; width+=dx; height-=dy; break;
         case BD: width+=dx; height+=dy; break;
         case BG: x+=dx; width-=dx; height+=dy; break;
         case H:  y+=dy; height-=dy; break;
         case D:  width+=dx; break;
         case B:  height+=dy; break;
         case G:  x+=dx; width-=dx; break;
      }
      setX(x);
      setY(y);
   }

   // Retourne la poign�e sous la souris (coordonn�es images)
   private int getPoignee(int xm, int ym) {
      if( width==1 || height==1 ) return BD;
      if( closeButton!=null && closeButton.contains(xm,ym)) return INCOLLAPSED;
      for( int i=1; i<=8; i++ ) {
         Rectangle rc = getRectPoignee(i);
         if( rc.contains(xm,ym) ) return i;
      }
      return inHead ? INHEAD : in ? IN : -1;
   }

   // Retourne l'action qu'il faudra effectuer
   private int getAction(int xm, int ym) {
      if( isCollapsed && in ) return INCOLLAPSED;
      if( closeButton!=null && closeButton.contains(xm,ym)) return INCOLLAPSED;
      return -1;
   }

   // Retourne le rectangle de sensibilit� correspondant � la poign�e indiqu�e
   private Rectangle getRectPoignee(int poignee) {
      int w = W;
      int w2 = w/2;
      switch(poignee) {
         case HG:  return new Rectangle(-w2,-w2-HB, w,w);
         case HD:  return new Rectangle(width-w2,-w2-HB, w,w);
         case BD:  return new Rectangle(width-w2,height-w2, w,w);
         case BG:  return new Rectangle(-w2,height-w2, w,w);

         case H:   return new Rectangle(width/2-w2,-w2-HB, w,w);
         case D:   return new Rectangle(width-w2,height/2-w2, w,w);
         case G:   return new Rectangle(-w2,height/2-w2, w,w);
         case B:   return new Rectangle(width/2-w2,height-w2, w,w);
      }
      return null;
   }

   // Dessin des poign�es de controles
   private void drawControl(Graphics g) {
      if( !movable && !resizable ) return;
      for( int i=0; i<8; i++ ) {
         int xp = (i==0 || i>=6 ? 0 : i==1 || i ==5 ?width/2 : width)-DS/2;
         int yp = (i<=2 ? -HB : i==3 || i==7 ? height/2 : height)-DS/2;
         g.setColor( (i+1)==poignee ? Color.red : Color.green );
         drawPoignee(g,xp,yp);
      }
   }

   // Dessin d'une poign�e (coordonn�e locale)
   protected void drawPoignee(Graphics g, int xp,int yp) {
      g.fillRect( xp+1,yp+1 , DS,DS );
      g.setColor( Color.black );
      g.drawRect( xp,yp , DS,DS );
   }


   // Dessin du bouton close (utilis� de fait pour le collapse)
   private void drawClose(Graphics g) {
      if( !collapsable ) return;
      int w=HB/2 +1;
      int yc=-(HB+w)/2 -1;
      int xc=width-w-3;
      g.setColor( inCollapsed ? Color.red : new Color(220,220,220) );
      g.fillRect(xc,yc,w+1,w+1);
      g.setColor( inCollapsed ? Color.white : Color.red);
      g.drawLine(xc+w/2-3,yc+w/2-3,xc+w/2+3,yc+w/2+3);
      g.drawLine(xc+w/2-2,yc+w/2-3,xc+w/2+4,yc+w/2+3);
      g.drawLine(xc+w/2-3,yc+w/2+3,xc+w/2+3,yc+w/2-3);
      g.drawLine(xc+w/2-2,yc+w/2+3,xc+w/2+4,yc+w/2-3);

      closeButton = new Rectangle(xc,yc,w,w);
   }

   // Dessin de l'ent�te
   private void drawHead(Graphics g) {
      if( !movable && !resizable && !collapsable ) return;
      g.setColor( BLUE );
      g.fillRect(0,-HB,width,HB);
      drawClose(g);
   }

   // Dessin des bords (en mode redimensionnement)
   private void drawBorder(Graphics g) {
      g.setColor(Color.black);
      g.drawRect(0,-HB,width,height+HB);
   }

   // Dessin des �l�ments de contr�le (redimensionnement, ent�te)
   private void paintControl(Graphics g) {
      if( !in && !inHead && !flagControl ) return;
      g.setClip(-DS/2,-HB-DS/2,width+DS+1,height+HB+DS+1);
      if( inShowHead || flagControl ) drawHead(g);
      if( flagControl ) {
         drawBorder(g);
         drawControl(g);
      }
   }

   // Dessin du widget sous forme collaps� (bouton)
   private void paintCollapsed(Graphics g) {

      Graphics2D g2d = (Graphics2D)g;
      AffineTransform saveTransform = g2d.getTransform();
      Shape saveClip = g2d.getClip();

      int x = getX();
      int y = getY();
      g2d.translate(x,y);
      g2d.setClip(0,0,width,height);

      // Int�rieur
      g.setColor(Color.lightGray);
      g.fillRoundRect(0, 0, WCOLLAPSED, WCOLLAPSED, 3, 3);

      // Contenu
      ((Widget)c).paintCollapsed(g);

      // Bords ext�rieurs
      g.setColor(Color.gray);
      g.drawRoundRect(0, 0, WCOLLAPSED, WCOLLAPSED, 3, 3);
      g.drawRoundRect(1, 1, WCOLLAPSED-2, WCOLLAPSED-2, 3, 3);

      g2d.setTransform(saveTransform);
      g2d.setClip(saveClip);
   }

   // Dessin du widget (collaps� ou non)
   public void paint(Graphics g) {

      if( isCollapsed ) { paintCollapsed(g); return; }

      // redimensionneme syst�matiquement le widget
      // afin d'annuler d'�ventuels redimensionnements du component sous-jacent
      setSize(width, height);

      Graphics2D g2d = (Graphics2D)g;
      AffineTransform saveTransform = g2d.getTransform();
      Composite saveComposite = g2d.getComposite();
      Shape saveClip = g2d.getClip();

      if( opacity!=1.0f ) {
         Composite myComposite = Util.getImageComposite(opacity);
         g2d.setComposite(myComposite);
      }

      int x = getX();
      int y = getY();
      g2d.translate(x,y);
      g2d.setClip(0,0,width,height);

      // Appel au paint su component sous-jacent
      c.paint(g2d);

      // retour � l'�tat ant�rieur de la transparence
      if( opacity!=1.0f ) g2d.setComposite(saveComposite);

      // Trac� des controles du widget si n�cessaire
      paintControl(g);

      g2d.setTransform(saveTransform);
      g2d.setClip(saveClip);
   }

   // Pour du debugging
   private String getPosCode() {
      return position==NW ? "NW" : position==NE ? "NE" : position==SW ? "SW" : "SE";
   }

   // Pour du debugging
   public String toString() { return c.getClass().getCanonicalName()+" ("+x+","+y+" "+width+"x"+height+") pos="+getPosCode(); }

}

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cds.tools.Util;

/**
 * Slider avec bouton "plus", "moins" et titre
 * @author Pierre Fernique [CDS]
 * @version 1.0 Jan 2012 - cr�ation
 */
public abstract class SliderPlusMoins extends JPanel implements MouseWheelListener,
MouseMotionListener,MouseListener,Widget {
   Aladin aladin;

   JLabel label;
   Slider slider;
   JButton plus,moins;
   int wheelIncr;

   public String toString() { return (slider == null) ? "null" : slider.toString(); }

   /**
    * Cr�ation d'un slider
    * @param aladin r�f�rence
    * @param title - titre du slider (apparait sur la gauche)
    * @param min,max - valeurs du slider
    * @param incr - valeur de l'incr�ment lors de l'usage du bouton + ou -
    */
   public SliderPlusMoins(Aladin aladin,String title, int min, int max, final int incr) {
      this(aladin,title,min,max,incr,incr);
   }


   public SliderPlusMoins(Aladin aladin,String title, int min, int max, final int incr,int wheelIncr) {
      this.aladin = aladin;
      slider = new Slider(min,max,incr);

      label = new Lab(title);
      label.setOpaque(true);
      label.setFont(Aladin.SBOLD);
      label.setBackground( aladin.getBackground() );

      JButton b;
      moins=b = new Bouton("-");
      b.setBorderPainted(false);
      b.setBackground( aladin.getBackground());
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(-incr); }
      });

      plus=b = new Bouton("+");
      b.setFont(b.getFont().deriveFont((float)b.getFont().getSize()-1));
      b.setBorderPainted(false);
      b.setBackground( aladin.getBackground());
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(incr); }
      });

      JPanel p = new JPanel(new BorderLayout(5,0));
      p.setBackground( aladin.getBackground());
      p.add(moins,BorderLayout.WEST);
      p.add(slider,BorderLayout.CENTER);
      p.add(plus,BorderLayout.EAST);

      setLayout( new BorderLayout(0,0));
      setBackground( aladin.getBackground());
      setOpaque(true);
      add(label,BorderLayout.WEST);
      add(p,BorderLayout.CENTER);

      setEnabled(false);

//      setBorder( BorderFactory.createEmptyBorder(0, 10, 0, 5));
      addMouseWheelListener(this);
      this.wheelIncr=wheelIncr;
   }

   public void mouseWheelMoved(MouseWheelEvent e) {
      if( !enable ) return;
      if( e.getClickCount()==2 ) return;    // SOUS LINUX, J'ai un double �v�nement � chaque fois !!!
      submit( -wheelIncr*e.getWheelRotation() );
      slider.repaint();
   }

   /** R�cup�re la valeur courant du slider */
   public double getValue() { return slider.getValue(); }

   /** Positionne la valeur courante du slider */
   public void setValue(int v) { slider.setValue(v); }

   /** Action appel�e lors de la modification du slider par l'utilisateur */
   abstract void submit(int inc);

   boolean enable=true;

   /** Active ou d�sactive le slider */
   public void setEnabled(boolean m) {
      if( m==enable ) return;       // d�j� fait
      enable=m;
      super.setEnabled(m);
      slider.setEnabled(m);
      label.setForeground( m ? Aladin.COLOR_CONTROL_FOREGROUND 
            : Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE );
      plus.setEnabled(m);
      moins.setEnabled(m);
   }

   /** Positionne le tip */
   void setTooltip(String tip) {
      Util.toolTip(label, tip);
      Util.toolTip(moins, tip);
      Util.toolTip(plus, tip);
      Util.toolTip(slider, tip);
   }

   boolean setMinMax(int min,int max) { return slider.setMinMax(min,max); }

   private void shiftE(MouseEvent e) { e.translatePoint(-(label.getWidth()+moins.getWidth()), 0); }
   public void mouseDragged(MouseEvent e) { shiftE(e); slider.mouseDragged(e); }
   public void mouseMoved(MouseEvent e)   { shiftE(e); slider.mouseMoved(e); }
   public void mouseClicked(MouseEvent e) { shiftE(e); slider.mouseClicked(e); }
   public void mousePressed(MouseEvent e) { shiftE(e); slider.mousePressed(e); }
   public void mouseReleased(MouseEvent e){ shiftE(e); slider.mouseReleased(e); }
   public void mouseEntered(MouseEvent e) { shiftE(e); slider.mouseEntered(e); }
   public void mouseExited(MouseEvent e)  { shiftE(e); slider.mouseExited(e); }

   class Slider extends JPanel implements MouseMotionListener,MouseListener {
      int min,max,incr;
      double value;

      Slider(int min, int max,int incr) {
         this.value=this.min=min;
         this.max=max;
         this.incr=incr;
         addMouseListener(this);
         addMouseMotionListener(this);
         setBackground( Aladin.COLOR_MAINPANEL_BACKGROUND );
      }

      public String toString() { return "slider["+min+" .. "+max+"] => "+value; }

      double getValue() { return value; }
      void setValue(double v) { value=v; repaint(); }

      boolean setMinMax(int min, int max) {
         if( this.min==min && this.max==max ) return false;
         this.min=min;
         this.max=max;
         if( value<min ) value=min;
         else if( value>max ) value=max;
         return true;
      }

      private Rectangle r;
      private boolean in=false;
      private int memoX,memoWhere;

      // X du slider (absisse) en fonction de la valeur courante
      private int getPos() { return (int)( getWidth() * ( (value-min)/(max-min) )); }

      // Positionnement de la valeur du slider en fonction de sa position X
      private void setPos(int x) {
         value = (int)( ((double)x/getWidth())*(max-min)+min );
         if( value>max ) value=max;
         else if( value<min ) value=min;
      }

      private int where(int x) {
         return x<r.x ? -1 : r.x<=x && x<=r.x+r.width ? 0 : 1;
      }

      public void mouseClicked(MouseEvent e) { }
      public void mousePressed(MouseEvent e) {
         memoX=e.getX();
         memoWhere=where(memoX);
      }
      public void mouseReleased(MouseEvent e) {
         if( !isEnabled() ) return;
         /*if( memoWhere==-1 ) value-=incr;
         else if( memoWhere==1 ) value+=incr;
         else */{
            int x=e.getX();
            //           if( x==memoX ) return;
            memoX=x;
            setPos(x);
         }
         if( value>max ) value=max;
         else if( value<min ) value=min;

         submit(0);
         repaint();
      }
      public void mouseEntered(MouseEvent e) { mouseMoved(e); }
      public void mouseExited(MouseEvent e) { in=false; repaint(); }
      public void mouseDragged(MouseEvent e) { in=true; mouseReleased(e); }
      public void mouseMoved(MouseEvent e) {
         if( !enable ) return;
         boolean newIn = where(e.getX())==0;
         if( newIn==in ) return;
         in=newIn;
         repaint();
      }

      public void paintComponent(Graphics g) {
         int H = getHeight();
         int W = getWidth();
         g.setClip(null);
         g.setColor( slider.getBackground());
         g.fillRect(0, 0, W, H);
         
         Color bg = Aladin.COLOR_BACKGROUND;
         Color fg = Aladin.COLOR_FOREGROUND;
         
         Util.drawCartouche(g, 0, H/2-2, W, 5, 1f, 
               enable ? Aladin.COLOR_CONTROL_FOREGROUND : Aladin.MYGRAY, bg);

         int x = getPos();
         if( x-7<0 ) x=7;
         if( x+5>W ) x=W-5;

         r = new Rectangle(x-7, H/2-6, 14, 13);
         g.setColor( enable ? Aladin.COLOR_CONTROL_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE );
         g.fillRect(r.x,r.y,r.width,r.height);
         if( enable ) Util.drawEdge(g,r.x,r.y,r.width,r.height);

         x=x-4;
         for( int i=0; i<3; i++ ) {
            g.setColor(!enable ? Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE : fg );
            g.drawLine(x+i*3,H/2-4,x+i*3,H/2+3);
            g.setColor(bg);
            g.drawLine(x+i*3+1,H/2-4,x+i*3+1,H/2+3);
         }
      }
   }

   class Bouton extends JButton implements MouseListener {
      static final int SIZE=10;
      boolean in=false;
      Bouton(String s) {
         super.setText(s);
         setFont(Aladin.LBOLD);
         addMouseListener(this);
      }
      public Dimension getPreferredSize() { return new Dimension(SIZE,SIZE); }
      public Dimension getSize() { return new Dimension(SIZE,SIZE); }

      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         int H = getHeight();
         int W = getWidth();
         g.setColor( slider.getBackground() );
         g.fillRect(0, 0, W, H);
         g.setColor( !enable ? Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE :
           in ? Aladin.COLOR_CONTROL_FOREGROUND.brighter() : Aladin.COLOR_CONTROL_FOREGROUND);
         String s = getText();
         g.drawString(s,W/2-g.getFontMetrics().stringWidth(s)/2,H/2+5);
      }
      public void mouseDragged(MouseEvent e) { }
      public void mouseMoved(MouseEvent e) {
         if( !enable ) return;
         setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
      public void mouseClicked(MouseEvent e) { }
      public void mousePressed(MouseEvent e) { }
      public void mouseReleased(MouseEvent e) { }
      public void mouseEntered(MouseEvent e) {
         in=true;
         setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         repaint();
      }
      public void mouseExited(MouseEvent e) {
         in=false;
         setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
         repaint();
      }
   }

   class Lab extends JLabel {
      private int width=40;
      public Lab(String s) { super(s==null?"":s); if( s==null ) width=0; }
      public Dimension getPreferredSize() {  return new Dimension(width,14); }
   }

   private WidgetControl voc=null;

   @Override
   public WidgetControl getWidgetControl() { return voc; }

   @Override
   public void createWidgetControl(int x, int y, int width, int height, float opacity,JComponent parent) {
      voc = new WidgetControl(this,x,y,width,height,opacity,parent);
      voc.setResizable(true);
   }

   @Override
   public void paintCollapsed(Graphics g) {}


}

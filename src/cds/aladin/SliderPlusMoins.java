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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.Aladin;
import cds.tools.Util;

/**
 * Slider avec bouton "plus", "moins" et titre
 * @author Pierre Fernique [CDS]
 * @version 1.0 Jan 2012 - création
 */
public abstract class SliderPlusMoins extends JPanel {
   Aladin aladin;
   
   JLabel label;
   Slider slider;
   JButton plus,moins;
   
   /**
    * Création d'un slider
    * @param aladin référence
    * @param title - titre du slider (apparait sur la gauche)
    * @param min,max - valeurs du slider
    * @param incr - valeur de l'incrément lors de l'usage du bouton + ou -
    */
   public SliderPlusMoins(Aladin aladin,String title, int min, int max, final int incr) {
      this.aladin = aladin;
      
      slider = new Slider(min,max,incr);
      
      label = new JLabel(title);
      label.setFont(Aladin.SBOLD);
      label.setBackground( slider.getBackground() );

      JButton b;
      moins=b = new Bouton("-");
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(-incr); }
      });

      plus=b = new Bouton("+");
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(incr); }
      });
      
      setLayout( new BorderLayout(0,0));
      JPanel p = new JPanel(new BorderLayout(5,0));
      p.add(moins,BorderLayout.WEST);
      p.add(slider,BorderLayout.CENTER);
      p.add(plus,BorderLayout.EAST);
      
      add(label,BorderLayout.WEST);
      add(p,BorderLayout.CENTER);
      
      setEnabled(false);
      
   }
   
   /** Récupère la valeur courant du slider */
   public int getValue() { return slider.getValue(); }
   
   /** Positionne la valeur courante du slider */
   public void setValue(int v) { slider.setValue(v); }
   
   /** Action appelée lors de la modification du slider par l'utilisateur */
   abstract void submit(int inc);
   
   boolean enable=true;
   
   /** Active ou désactive le slider */
   public void setEnabled(boolean m) {
      if( m==enable ) return;       // déjà fait
      enable=m;
      super.setEnabled(m);
      slider.setEnabled(m);
      label.setForeground( m ? Color.black : Aladin.MYGRAY );
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
   
   class Slider extends JPanel implements MouseMotionListener,MouseListener {
      int min,max,value,incr;
      Slider(int min, int max,int incr) {
         this.value=this.min=min;
         this.max=max;
         this.incr=incr;
         addMouseListener(this);
         addMouseMotionListener(this);
      }
      
      int getValue() { return value; }
      void setValue(int v) { value=v; repaint(); }
      
      public void paintComponent(Graphics g) {
         int H = getHeight();
         int W = getWidth();
         g.setClip(null);
         g.setColor( slider.getBackground());
         g.fillRect(0, 0, W, H);
         
         Util.drawCartouche(g, 0, H/2-2, W, 5, 1f, enable ? Color.gray : Aladin.MYGRAY, Color.white);
         
         int x = value;
         x = (int)( W* ( (double)(x-min)/(max-min) ));
         if( x-7<0 ) x=7;
         if( x+5>W ) x=W-5;
         
         r = new Rectangle(x-7, H/2-6, 14, 13);
         g.setColor( enable ? Color.lightGray : Aladin.MYGRAY );
         g.fillRect(r.x,r.y,r.width,r.height);
         if( enable ) Util.drawEdge(g,r.x,r.y,r.width,r.height);
         
         x=x-4;
         for( int i=0; i<3; i++ ) {
            g.setColor(enable ? Color.black : Color.lightGray);
            g.drawLine(x+i*3,H/2-4,x+i*3,H/2+3);
            g.setColor(Color.white);
            g.drawLine(x+i*3+1,H/2-4,x+i*3+1,H/2+3);
         }
      }
      
      private Rectangle r;
      private int memoX,memoY,memoWhere;
      
      private int where(int x, int y) { 
         return x<r.x ? -1 : r.contains(x, y) ? 0 : 1;
      }

      public void mouseClicked(MouseEvent e) { }
      public void mousePressed(MouseEvent e) {
         memoX=e.getX(); memoY=e.getY();
         memoWhere=where(memoX,memoY);
      }
      public void mouseReleased(MouseEvent e) { 
         if( !isEnabled() ) return;
         if( memoWhere==-1 ) value-=incr;
         else if( memoWhere==1 ) value+=incr;
         else {
           int x=e.getX(),y=e.getY();
           if( x==memoX && y==memoY ) return; 
           memoX=x; memoY=y;
           value = (int)( ((double)x/getWidth())*(max-min)+min );
         }
         if( value>max ) value=max;
         else if( value<min ) value=min;
         
         submit(0);
         repaint();
      }
      public void mouseEntered(MouseEvent e) { }
      public void mouseExited(MouseEvent e) { }
      public void mouseDragged(MouseEvent e) { mouseReleased(e); }
      public void mouseMoved(MouseEvent e) {  }
   }

   class Bouton extends JButton implements MouseMotionListener {
      static final int SIZE=10;
      Bouton(String s) {
         super.setText(s);
         setFont(Aladin.LBOLD);
         addMouseMotionListener(this);
      }
      public Dimension getPreferredSize() { return new Dimension(SIZE,SIZE); }
      public Dimension getSize() { return new Dimension(SIZE,SIZE); }
   
      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         int H = getHeight();
         int W = getWidth();
         g.setColor( slider.getBackground());
         g.fillRect(0, 0, W, H);
         g.setColor( enable ? Color.black : Aladin.MYGRAY );
         String s = getText();
         g.drawString(s,W/2-g.getFontMetrics().stringWidth(s)/2,H/2+5);
      }
      public void mouseDragged(MouseEvent e) { }
      public void mouseMoved(MouseEvent e) {
         if( !enable ) return;
         setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
      
   }
   
}

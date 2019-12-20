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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Sélecteur de couleur 1 case.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : sept 2017 création
 */
public final class CouleurBox extends JPanel implements MouseListener,ActionListener {

   private Color defaultCouleur;            // Couleur par défaut
   private Color couleur;                   // Couleur sélectionnée
   private int size;                        // Taille du carré affichant la couleur
   private FrameCouleur frameCouleur=null;  // Fenêtre de sélection d'une autre couleur
   private ActionListener actionListener;   // Listener à appeler au changement de couleur, null si aucun
   
   protected CouleurBox(Color defaultCouleur, Color couleur) { this(defaultCouleur,couleur,18); }
   protected CouleurBox(Color defaultCouleur, Color couleur, int size) { 
      super();
      this.defaultCouleur=defaultCouleur;
      this.couleur=couleur;
      this.size = size;
      addMouseListener(this);
   }
   
   protected Color getColor() { return couleur; }
   protected void setColor(Color c) { couleur=c; }
   
   /** Retourne la couleur courante sous la forme d'une chaine RGB(r,g,b) */
   protected String getCouleur() { 
      if( couleur==null ) return null;
      return "RGB("+couleur.getRed()+","+couleur.getGreen()+","+couleur.getBlue()+")"; 
   }
   
   /** Retourne la couleur sous la forme d'une chaine RGB(r,g,b) */
   static protected Color getCouleur(String rgb) throws Exception { 
      Tok tok = new Tok(rgb,"(, )");
      tok.nextToken();
      int r = Integer.parseInt( tok.nextToken() );
      int g = Integer.parseInt( tok.nextToken() );
      int b = Integer.parseInt( tok.nextToken() );
      return new Color( r,g,b );
   }
   
   public void addActionListener( ActionListener l ) { actionListener=l; }
   
   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setColor(couleur==null?defaultCouleur:couleur);
      g.fillRect(0, 0, size, size);
      g.setColor( Color.gray );
      g.drawRect(0, 0, size, size);
   }

   public void mouseClicked(MouseEvent e) {
      if( frameCouleur==null ) frameCouleur = new FrameCouleur(this);
      else frameCouleur.setCouleur( couleur );
      Point p = e.getLocationOnScreen();
      frameCouleur.setLocation(p.x+e.getX(),p.y+e.getY()-20);
      frameCouleur.setVisible(true);
   }
   public void mousePressed(MouseEvent e) { }
   public void mouseReleased(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
   
   private void hideFrame() { if( frameCouleur!=null ) frameCouleur.setVisible(false); }
   

   @Override
   public void actionPerformed(ActionEvent e) {
      frameCouleur.setVisible(false);
      couleur = frameCouleur.getCouleur();
      repaint();
      if( actionListener!=null ) actionListener.actionPerformed( new ActionEvent(this, ActionEvent.ACTION_PERFORMED, couleur+""));
   }
   
   // Le Panel de la fenêtre qui gère localement une croix de fermeture
   private class MyPanel extends JPanel implements MouseListener,MouseMotionListener {
      static private final int W = 6;
      Rectangle cross=null;
      boolean in;
      
      MyPanel() {
         super();
         setLayout( new BorderLayout() );
         setBorder( BorderFactory.createEmptyBorder(7,7,3,7) );
         addMouseListener(this);
         addMouseMotionListener(this);;
      }
      
      public void paint( Graphics g ) { super.paint(g); drawCross(g, getWidth()-W-3, 2); }

      private void drawCross(Graphics g, int x, int y) {
         g.setColor( in ? Color.red : Color.gray );
         g.drawLine(x,y,x+W,y+W);
         g.drawLine(x+1,y,x+W+1,y+W);
         g.drawLine(x+2,y,x+W+2,y+W);
         g.drawLine(x+W,y,x,y+W);
         g.drawLine(x+W+1,y,x+1,y+W);
         g.drawLine(x+W+2,y,x+2,y+W);
         cross = new Rectangle(x,y-2,W+2,W+2);
      }

      public void mouseClicked(MouseEvent e) { }
      public void mouseReleased(MouseEvent e) { }
      public void mouseEntered(MouseEvent e) { }
      public void mouseExited(MouseEvent e) { }
      public void mouseDragged(MouseEvent e) { }
      
      public void mousePressed(MouseEvent e) { if( in ) hideFrame(); }
      public void mouseMoved(MouseEvent e) {
         boolean in1 = cross.contains( e.getPoint() );
         if( in1!=in ) repaint();
         in=in1;
      }
   }
   
   // Le Frame de sélection d'une nouvelle couleur
   private class FrameCouleur extends JFrame {
      Couleur selecteurCouleur;
      
      FrameCouleur(CouleurBox cb) {
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         JPanel contentPane = (JPanel)getContentPane();
         contentPane.setLayout( new BorderLayout(5,5)) ;
         contentPane.setBackground( new Color(240,240,250));
         contentPane.setBorder( BorderFactory.createLineBorder(Color.black));
         setUndecorated(true);
         
         JPanel panel = new MyPanel();
         selecteurCouleur = new Couleur(cb.couleur);
         selecteurCouleur.setNoColorFlag(true);
         selecteurCouleur.addActionListener(cb);
         panel.add(selecteurCouleur);
         contentPane.add( panel, BorderLayout.CENTER );
         pack();
      }
      
      void setCouleur( Color c ) { selecteurCouleur.setCouleur(c); }
      Color getCouleur() { return selecteurCouleur.getCouleur(); }
   }

}


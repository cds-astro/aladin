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
import java.awt.event.*;

import javax.swing.JComponent;

/** 
 * @author Thomas Boch
 */


/* classe destinee a l'affichage des curseurs pour definir les niveaux des contours */
public class Curseur extends JComponent implements MouseListener, MouseMotionListener {
   
     static final int NO =  -1;     // constante signifiant qu'aucun triangle
      	    	    	    	    // n'est le triangle courant
     
     int[] niveaux;                 // tableau des niveaux
     int currentTriangle;           // indice dans niveaux du triangle en cours de selection
     boolean flagDrag = false;      // true si on drag
     int nbNiveaux = 0;             // nb de niveaux couramment utilises
     Color[] couleurTriangle;       // tableau des couleurs pour les triangles
     
     private MyListener listener;   // pour declencher une action associee aux valeurs du curseur
     
     // Les constantes d'affichage
     final int mX = 10;      	// Marge en abscisse
     final int mY = 0;      	// Marge en ordonnee
     final int Hp = 0;      	// Hauteur du graphique
     final int W = 256+2*mX; 	// Largeur totale du graphique
     final int H = Hp+mY+24; 	// Hauteur totale du graphique
     
     /* constructeur */
     protected Curseur() {
       initNiveaux();
       initCouleurs();
//       setBackground(Aladin.BKGD);
       enableEvents(AWTEvent.WINDOW_EVENT_MASK);
       addMouseListener(this);
       addMouseMotionListener(this);
       resize(W,H);
     }
     
     protected Curseur(MyListener listener) {
         this();
         this.listener = listener;
     }
     
     /** initCouleurs
      *  initialise le tableau des couleurs
      *  chaque element est initialise a Color.black
      */
     private void initCouleurs() {
       couleurTriangle = new Color[PlanContour.MAXLEVELS];
       for (int i=0;i<PlanContour.MAXLEVELS;i++) {
         
         couleurTriangle[i] = Color.black;
       }
     }
     
     
     /** initNiveaux
      * fixe arbitrairement les niveaux initiaux
      */
     private void initNiveaux() {
       niveaux = new int[PlanContour.MAXLEVELS];
       niveaux[0] = 100;
       nbNiveaux = 1;
     }
     
     public Dimension preferredSize() { return new Dimension(W,H); }
     public Dimension getPreferredSize() { return preferredSize(); }
     
     /** Dessin d'un triangle.
      * @param g le contexte graphique
      * @param x l'abcisse du triangle a dessinner
      * @param i indice dans niveaux du triangle que l'on dessine
      */
     protected void drawTriangle(Graphics g,int x, int i) {
      int [] tx = new int[4];
      int [] ty = new int[4];

      tx[0] = tx[3] = x+mX;
      tx[1] = tx[0]-7;
      tx[2] = tx[0]+7;

      ty[0] = ty[3] = Hp+4+mY;
      ty[1] = ty[2] = ty[0]+10;

      g.setColor( couleurTriangle[i] );
      g.fillPolygon(tx,ty,tx.length);
      g.setColor(Color.black);
      g.drawPolygon(tx,ty,tx.length);
      g.setFont( Aladin.SPLAIN );
      g.drawString(""+x,mX+x-7,mY+Hp+24);
      }
      
      /** Reperage du triangle le plus proche de la position de la souris */
      public boolean mouseMove(Event e,int x,int y) {
        
        x-=mX;
        if( listener!=null ) listener.fireStateChange(-1);
        // on demande a avoir le focus pour les evts clavier
        requestFocus();

        // Reperage du triangle le plus proche
        for( int i=0; i<nbNiveaux; i++ ) {
          if( x>niveaux[i]-5 && x<niveaux[i]+5 ) {
            
            currentTriangle=i;
            return true;
          }
        }
        currentTriangle = NO;
        return true;
      }
      
      // implementation de MouseListener
      /** Reperage du triangle le plus proche du clic souris */
      public void mousePressed(MouseEvent e) {
        int x = e.getX();
        x-=mX;

        // Reperage du triangle le plus proche
        for( int i=0; i<nbNiveaux; i++ ) {
          if( x>niveaux[i]-5 && x<niveaux[i]+5 ) {
            
            currentTriangle=i;
            if( listener!=null ) listener.fireStateChange(niveaux[currentTriangle]);
            return;
          }
        }
        currentTriangle = NO;
        if( listener!=null ) listener.fireStateChange(null);
      }
      /** Fin de deplacement du triangle precedemment selectionne */
      public void mouseReleased(MouseEvent e) {
         if( currentTriangle==NO ) return;

         int x = e.getX();
         x-=mX;
         if( x<0 ) x=0;
         else if( x>255 ) x=255;

         niveaux[currentTriangle] = x;
      
         flagDrag=false;
 
         repaint();
      }
      public void mouseClicked(MouseEvent e) {}
      public void mouseEntered(MouseEvent e) {}
      public void mouseExited(MouseEvent e) {}
      
      // implementation de MouseMotionListener
      /** Deplacement du triangle precedemment selectionne avec mise a jour
       *  d'une portion de l'image
       */
      public void mouseDragged(MouseEvent e) {
        if( currentTriangle==NO ) return;
        int x = e.getX();
        x-=mX;
        if( x<0 ) x=0;
        else if( x>255 ) x=255;

        niveaux[currentTriangle] = x;
        if( listener!=null ) listener.fireStateChange(x);
        flagDrag=true;
        
        repaint();
      }
      public void mouseMoved(MouseEvent e) {}
      
      
    public void paint(Graphics g) {
        int i;

        g.setColor(Color.black);
        g.drawLine(mX, Hp+mY+3, mX+255, Hp+mY+3);
        g.drawLine(mX, Hp+mY, mX, Hp+mY+6);
        g.drawLine(mX+255, Hp+mY, mX+255, Hp+mY+6);

        for( i=0; i<nbNiveaux; i++ ) {
            drawTriangle(g,niveaux[i],i);
        }
    }
      
      // ajoute un curseur au nivau par defaut 0
      protected boolean addCurseur() {
          if (nbNiveaux>=PlanContour.MAXLEVELS) return false;
          
          niveaux[nbNiveaux] = 0; // niveau a la creation du nouveau curseur
          nbNiveaux++;
          repaint();
          return true;

      }
      
      // ajoute un curseur au niveau specifie
      protected boolean addCurseur(int level) {
          if (nbNiveaux>=PlanContour.MAXLEVELS) return false;
          
          niveaux[nbNiveaux] = level; // niveau a la creation du nouveau curseur
          nbNiveaux++;
          repaint();
          return true;

      }
      
      // remet le nb de niveaux a zero
      protected void reset() {
          nbNiveaux = 0;
          repaint();
      }
   
   // pour Francois Ochsenbein
   // methode permettant le deplacement des niveaux au clavier
   public boolean keyDown(Event e,int key) {
      if(currentTriangle == NO) return true;
      
      boolean move = false;
      if (key == Event.RIGHT) {
          move = true;
          if(niveaux[currentTriangle]<255) niveaux[currentTriangle]++;
        }
        else if (key == Event.LEFT) {
            move = true;
          if(niveaux[currentTriangle]>0) niveaux[currentTriangle]--;
        }
      if( move ) {
          if( listener!=null ) listener.fireStateChange(niveaux[currentTriangle]);
          repaint();
      } 
      return true;
   }
      
      
      
} 

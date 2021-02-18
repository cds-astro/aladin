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
/*
 * Created on 16 janv. 2004
 *
 * To change this generated comment go to
 * Window>Preferences>Java>Code Generation>Code Template
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;

import javax.swing.JComponent;

/** A very basic slider in AWT
 * @author Thomas Boch [CDS]
 */
public class Slider extends JComponent {
   private double begin; // start value of the slider
   private int nbSteps; // number of steps, ie nb of pixels
   private double stepSize = 1.0; // value between 2 consecutive steps

   private int curPos = 0; // current position (in term of number of steps from pos 0

   // Les constantes d'affichage
   private int mX;          // Marge en abscisse
   private int margeDroite;
   private int mY;       // Marge en ordonnee
   private int Hp;       // Hauteur du graphique
   private int W;        // Largeur totale du graphique
   private int H;        // Hauteur totale du graphique

   boolean selected = false;
   boolean flagDrag = false;

   // listener qui écoute les changements de valeur !
   MyListener listener;

   /** Creates a new slider with a step size of 1.0
    *
    * @param begin
    * @param end
    * @param nbSteps
    */
   public Slider(double begin, int nbSteps) {
      setOpaque(true);
      this.begin = begin;
      this.nbSteps = nbSteps;
      
      double sc = Aladin.getUIScale();
      
      mX = (int)(10*sc);          // Marge en abscisse
      margeDroite = (int)(20*sc);
      mY = 0;       // Marge en ordonnee
      Hp = 0;       // Hauteur du graphique
      H = Hp+mY+(int)(34*sc);     // Hauteur totale du graphique
      W = nbSteps + mX + margeDroite;
   }

   /** Creates a new slider with a user-defined step size
    *
    * @param begin
    * @param end
    * @param nbSteps
    */
   public Slider(double begin,int nbSteps, double stepSize) {
      this(begin, nbSteps);
      this.stepSize = stepSize;
   }

   public Slider() {
      W = nbSteps + mX + margeDroite;
   }

   public void setParams(double begin,int nbSteps, double stepSize) {
      this.begin = begin;
      this.nbSteps = nbSteps;
      this.stepSize = stepSize;
      W = nbSteps + mX + margeDroite;
   }

   public void setListener(MyListener listener) {
      this.listener = listener;
   }



   public void setPosition(int nbStepsFromStart, boolean callListener) {
      this.curPos = nbStepsFromStart;
      // callback pour le listener
      if( listener!=null && callListener ) {
         listener.fireStateChange((this.curPos+1)+"");
         //node.curImgNumber = (this.curPos+1)+"";
         //System.out.println(node.curImgNumber);
      }
   }

   public void setPosition(int nbStepsFromStart) {
      setPosition(nbStepsFromStart, true);
   }

   public int getPosition() {
      return curPos;
   }

   public double getValue() {
      return begin+curPos*stepSize;
   }

   /** Dessin d'un triangle.
    * @param g le contexte graphique
    * @param x l'abcisse du triangle a dessiner
    */
   protected void drawTriangle(Graphics g,int x) {
      int [] tx = new int[4];
      int [] ty = new int[4];

      tx[0] = tx[3] = x+mX;
      tx[1] = tx[0]-7;
      tx[2] = tx[0]+7;

      ty[0] = ty[3] = Hp+14+mY;
      ty[1] = ty[2] = ty[0]+10;

      //g.setColor( couleurTriangle[i] );
      g.setColor(Color.blue);
      g.fillPolygon(tx,ty,tx.length);
      g.setColor(Color.black);
      g.drawPolygon(tx,ty,tx.length);
      g.setFont( Aladin.SPLAIN );
      double val = Slider.round(getValue(),2);

      String valStr = Double.toString(val);
      int fontSize = g.getFontMetrics().stringWidth(valStr);
      int xStr = mX+x-fontSize/2;
      if( (xStr+fontSize)>W ) xStr -= xStr+fontSize-W;
      if( xStr<0 ) xStr = 0;
      g.drawString(valStr,xStr,mY+Hp+34);

      // on met les valeurs extremes
      double leftVal = Slider.round(begin, 2);
      double rightVal = Slider.round(begin+nbSteps*stepSize, 2);
      g.drawString(Double.toString(leftVal),0,mY+Hp+10);

      String rightValStr = Double.toString(rightVal);
      fontSize = g.getFontMetrics().stringWidth(rightValStr);
      xStr = nbSteps+mX+-fontSize/2;
      if( (xStr+fontSize)>W ) xStr -= xStr+fontSize-W;
      g.drawString(rightValStr,xStr,mY+Hp+10);
   }

   public Dimension preferredSize() { return new Dimension(W,H); }
   public Dimension getPreferredSize() { return preferredSize(); }

   /** Reperage du triangle le plus proche de la position de la souris */
   public boolean mouseMove(Event e,int x,int y) {

      x-=mX;
      int pos = getPosition();
      // on demande a avoir le focus pour les evts clavier
      requestFocus();

      // Reperage du triangle le plus proche
      if( x>pos-5 && x<pos+5 ) {
         selected = true;
      }
      else selected = false;

      return true;
   }


   /** Reperage du triangle le plus proche du clic souris */
   public boolean mouseDown(Event e,int x,int y) {

      x-=mX;
      int pos = getPosition();

      // Reperage du triangle le plus proche
      if( x>pos-5 && x<pos+5 ) {
         selected=true;
      }
      else selected = false;
      return true;
   }



   /** Deplacement du triangle precedemment selectionne avec mise a jour
    *  d'une portion de l'image
    */
   public boolean mouseDrag(Event e, int x, int y) {
      if( !selected ) return true;
      x-=mX;
      if( x<0 ) x=0;
      else if( x>nbSteps ) x=nbSteps;

      setPosition(x);
      flagDrag=true;

      repaint();
      return true;
   }



   /** Fin de deplacement du triangle precedemment selectionne */
   public boolean mouseUp(Event e,int x, int y) {
      if( !selected ) return true;

      x-=mX;
      if( x<0 ) x=0;
      else if( x>nbSteps ) x=nbSteps;

      setPosition(x);

      flagDrag=false;

      repaint();
      return true;

   }

   public void paint(Graphics g) {
      g.setColor(Color.black);
      g.drawLine(mX, Hp+mY+13, mX+nbSteps, Hp+mY+13);
      g.drawLine(mX, Hp+mY+16, mX, Hp+mY+10);
      g.drawLine(mX+nbSteps, Hp+mY+16, mX+nbSteps, Hp+mY+10);

      drawTriangle(g, getPosition());
   }

   // methode permettant le deplacement des niveaux au clavier
   public boolean keyDown(Event e,int key) {
      //if(currentTriangle == NO) return true;
      int pos = getPosition();

      if (key == Event.RIGHT) {
         if(pos<nbSteps) setPosition(pos+1);
      }
      else if (key == Event.LEFT) {
         if(pos>0) setPosition(pos-1);
      }
      repaint();
      return true;
   }

   /** Arrondit et limite le nombre de décimales
    * @param d nombre à arrondir
    * @param nbDec nb de décimales à conserver
    * @return le nombre arrondi en conservant nbDec décimales
    */
   public static double round(double d, int nbDec) {
      if( d==Double.NEGATIVE_INFINITY || d==Double.POSITIVE_INFINITY ) return d;
      double fact = Math.pow(10,nbDec);
      return Math.round(d*fact)/fact;
   }

}

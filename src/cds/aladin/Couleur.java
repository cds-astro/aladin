// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;

/**
 * Gestion des couleurs (choix et valeur par defaut)
 * Affichage d'un selecteur de couleurs (8 petits carres cliquables)
 *
 * @author Pierre Fernique [CDS]
 * @versino 1.2 (février 2006) palette plus tranchée
 * @version 1.1? : (mars 02) Ajout de constructeur permettant le choix de la taille des carres
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Couleur extends JComponent implements MouseListener {
   static final int W   =  20; // Taille d'un carre de selection de couleur (marge comprise)
   static final int GAP = 4;   // Marge entre deux carres de selection de couleur
   int w   = W-GAP;             // Taille reelle d'un carre
   static int iDC = 0;         // indice de la prochaine couleur si aucune vide
   
   int ww = W;
   int gap = GAP;
   private boolean noColor=false;   // true s'il est possible de choisir une "non couleur"
   
   // Les couleurs gerees (doivent necessairement etre en nombre pair)
   static final int NBDEFAULTCOLORS = 22;
   static Color [] DC;
   
   // Les premières couleurs par défaut sont en "dur", les autres random
   static Color [] DC1 = {
         
         new Color(255,20,20),    // Rouge
         new Color(0,142,212),    // Bleu azur
         new Color(255,174,71),   // Orange clair
         new Color(180,196,45),   // Vert kaki
         new Color(255,154,204),  // Rose-violet
         new Color(223,225,36),   // Jaune-vert
         new Color(97,177,204),   // Bleu clair
         new Color(187,136,0),    // Brun
         new Color(204,0,153),    // Violet
         new Color(0,254,153),    // Vert d'eau
         new Color(255,204,154),  // Rose-peau
         new Color(102,117,197),  // Bleu satin
         new Color(154,255,51),   // Vert clair
         new Color(211,79,2),     // Brun teck
         new Color(255,255,20),   // Jaune
         new Color(255,20,255),   // Magenta
         new Color(20,255,255),   // cyan
         new Color(85,109,117),   // Gris foncé
         new Color(146,161,161),  // Gris clair
         new Color(20,20,20),     // Noir
         new Color(235,235,235),  // Blanc

//      Aladin.COLOR_RED, //Color.red,
//      Aladin.COLOR_BLUE, //Color.blue,
//      new Color(153,204,0),
//      new Color(255,255,0),
//      new Color(0,0,102),
//      new Color(0,255,255),
//      new Color(153,0,204),
//      new Color(0,153,204),
//      new Color(204,153,0),
//      new Color(204,0,153),
//      new Color(0,254,153),
//      new Color(102,51,51),
//      new Color(255,204,154),      
//      new Color(255,154,204),      
//      new Color(154,255,51),
//      new Color(102,0,0),
//      new Color(255,154,51),
//      new Color(255,0,255),
//      new Color(0,255,0),
//      new Color(0,0,0),
//      new Color(255,255,255),
   };
   
   Color[] dc;
   Rectangle [] dcRect;
   int row;
   int current=-1;             // Couleur courante du selecteur
   boolean first;              // true si on affiche pour la premiere fois le selecteur
   
   static { initDefaultColors(); }


  /** Creation d'un selecteur de couleur.
   * (pas de couleur selectionne par defaut)
   */
   protected Couleur() { this(null); }

  /** Creation d'un selecteur de couleur.
   * <P><B>Rq :</B> les couleurs possibles sont : Color.red,  Color.blue,
   * Color.yellow, Color.cyan, Color.pink, Color.orange, Color.magenta,
   * Color.green,Color.black,Color.white
   *
   * @param c la couleur courante (ex: Color.red...)
   * @param flagNoColor Utilise le dernier carré de couleur comme "no color"
   */
   protected Couleur(Color c) { this(c,false); }
   protected Couleur(Color c,boolean flagNoColor) {
      dc = DC;
      noColor=flagNoColor;
      setCouleur(c);
      DIM = new Dimension( (dc.length/2)*W,2*W);
      first=true;
      row=dc.length/2;
      addMouseListener(this);
   }
   
   private Dimension DIM;
   public Dimension getMinimumSize() { return DIM; }
   public Dimension getPreferredSize() { return DIM; }
   
   /** Juste pour supporter JVM1.1.8 qui ne dispose pas de Random.nextInt() */
   static private int nextRand() {
      return (int)(Math.random()*1000) %256;
   }
   
   
   /**
    * Construction d'un tableau de couleurs suffisamment distinctes.
    * Si on appelle 2x de suite cette méthode avec le même nombre de couleurs,
    * les deux tableaux retournés auront les mêmes couleurs
    * @param val Le nombre de couleurs demandées
    */
   static private void initDefaultColors() {
      int n=NBDEFAULTCOLORS;
      DC = new Color[n];
      for( int i=0; i<n; i++) {
         if( i<DC1.length ) { DC[i] = DC1[i]; continue; }         
         DC[i] = new Color(nextRand(),nextRand(),nextRand());
      }
   }
   
   //thomas
   /** Creation d'un selecteur de couleur avec choix de la taille des carres.
   * @param c la couleur courante
   * @param width taille d'un carre, gap compris
   * @param gap l'espace entre deux carres
   */
   protected Couleur(Color c, int width, int gap) {
       dc=DC;
       setCouleur(c);
       if (width>gap) {
         this.ww = width;
         this.gap = gap;
         this.w = ww-this.gap;
       }
       DIM = new Dimension( (DC.length/2)*ww,2*ww);
       first=true;
       
       row=dc.length/2;
       addMouseListener(this);
    }
   
   //thomas
   /** Creation d'un selecteur de couleur avec choix de la taille des carres
    *  et choix des couleurs
    * @param c la couleur courante
    * @param width taille d'un carre, gap compris
    * @param gap l'espace entre deux carres
    * @param tabcoul tableau des couleurs que l'on veut en plus des couleurs initiales
    */
   protected Couleur(Color c, int width, int gap, Color[] tabcoul) {
       int i;
       
       dc = new Color[DC1.length+tabcoul.length];
       // initialisation de dc
       for(i=0;i<DC1.length;i++) dc[i] = DC1[i];
       for(i=0;i<tabcoul.length;i++) dc[i+DC1.length] = tabcoul[i];
       
       
       row=dc.length/2;
       
       setCouleur(c);
       if (width>gap) {
         this.ww = width;
         this.gap = gap;
         this.w = ww-this.gap;
       }
       
       
       DIM = new Dimension( (dc.length/2)*ww,2*ww);
       first=true;
       addMouseListener(this);
    }
   
    //thomas
   /** Creation d'un selecteur de couleur avec choix de la taille des carres
    *  et choix des couleurs
    * @param tabcoul tableau des couleurs que l'on veut
    * @param c la couleur courante
    * @param width taille d'un carre, gap compris
    * @param gap l'espace entre deux carres
    */
   protected Couleur(Color[] tabcoul,Color c, int width, int gap) {
       int i;
       
       dc = new Color[tabcoul.length];
       // initialisation de dc
       for(i=0;i<tabcoul.length;i++) dc[i] = tabcoul[i];
       
       
       row=dc.length/2;
       
       setCouleur(c);
       if (width>gap) {
         this.ww = width;
         this.gap = gap;
         this.w = ww-this.gap;
       }
       
       
       DIM = new Dimension( (dc.length/2)*ww,2*ww);
       first=true;
       addMouseListener(this);
    }
   
   /** Positionne le flag "nocolor". Si celui-ci est à true, 
    * le widget ajoute une case "Nocolor" qui permettra à l'utilisateur
    * de ne pas choisir de couleur, par exemple pour prendre celle par défaut
    * @param flag
    */
   public void setNoColorFlag(boolean flag) { noColor=flag; }
    

  /** Retourne la prochaine couleur par defaut.
   * Prend en compte les couleurs deja utilisees par les autres plans
   * @param calque reference
   * @return la prochaine couleur par defaut
   */
   protected static Color getNextDefault(Calque calque) {
      int i,j;

      // Y a-t-il une couleur de libre ?
      Plan [] allPlan = calque.getPlans();
      for( j=0; j<DC.length; j++) {
         for( i=0; i<allPlan.length && (allPlan[i].type==Plan.NO
              || DC[j]!=allPlan[i].c); i++);
         if( i==allPlan.length ) break;
      }

      // Tout est pris, on prend une simple suivante
      if( j==DC.length ) {
         iDC++;
         if( iDC==DC.length ) iDC=0;
         j=iDC;
      }

      return DC[j];
   }
   
   /** Cette couleur est-elle libre ? */
   protected static boolean isFree(Calque calque, Color c ) {
      Plan [] allPlan = calque.getPlans();
      for( int i=0; i<allPlan.length; i++) {
         if( allPlan[i].type!=Plan.NO && c==allPlan[i].c ) return false;
      }
      return true;
   }
   
  /** Modification de la couleur courante dans le selecteur de couleur.
   * @param c la nouvelle couleur courante
   * @return <I>false</I> si la couleur n'est pas valide, <I>true</I> sinon
   * see aladin.Couleur(java.awt.color)
   */
   protected boolean setCouleur( Color c ) {
      if( noColor && c==null ) {
         current = dc.length-1;
         return true;
      }
      
      for( int i=0; i<dc.length; i++ ) {
         if( c.equals(dc[i]) ) {
            current=i;
            repaint();
            return true;
         }
      }
      return false;
   }

  /** Retourne la couleur courante ou null si aucune sélectionnée
   * @return la couleur courante du selecteur de couleur
   */
   protected Color getCouleur() {
      if( current== -1 || noColor && current==dc.length-1 ) return null;
      return dc[current];
   }
   
   /** retourne la couleur d'indice i */
   static protected Color getCouleur(int i) {
      return DC[i%DC.length];
   }
   
   private int getIndice(int x,int y) {
      if( dcRect==null ) return -1;
      Point p = new Point(x,y);
      for( int i=0; i<dcRect.length; i++ ) if( dcRect[i].contains(p) ) return i;
      return -1;
   }

  /** Gestion de la souris liee au selecteur de couleur */
   public void mousePressed(MouseEvent e) {
      int current = getIndice(e.getX(),e.getY());
      if( this.current!=current) {
         this.current=current;
         if( listener!=null ) listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
         repaint();
      }
   }

  /** Gestion de l'affichage du selecteur de couleur */
   public void update(Graphics g) { paint(g); }

  /** Gestion de l'affichage du selecteur de couleur */
   public void paint(Graphics g) {
      int i,j,k;
      Color c1,c2;

      // Remplissage du fond : uniquement la premiere fois
      if( first ) {
         g.setColor( getBackground() );
         g.fillRect(0,0,(dc.length/2)*ww,2*ww);
         first=false;
      }

      // On remplit sur deux rangees des petits carres avec les couleurs
      // selectionnables. La couleur courante est repere par une ombre
      // inverse laissant croire que le carre est enfonce

      for( j=0; j<2; j++ ) {
         for( i=0; i<row; i++ ) {
            k = j*row+i;                // Indice de la couleur
            if( dcRect==null ) dcRect = new Rectangle[ dc.length ];
            if( dcRect[k]==null ) dcRect[k] = new Rectangle(i*ww,j*ww,w,w);
            
            boolean onNoColor = noColor && k==dc.length-1;
            g.setColor( onNoColor ? Color.white : dc[k] );
            g.fillRect( dcRect[k].x,dcRect[k].y,dcRect[k].width,dcRect[k].height );

            // Trace des bordures
            if( k!=current ) { c1=Color.white; c2=Color.black; }
            else { c2=Color.white; c1=Color.black; }

            g.setColor(c1);
            g.drawLine(dcRect[k].x,dcRect[k].y+dcRect[k].height,dcRect[k].x,dcRect[k].y);
            g.drawLine(dcRect[k].x,dcRect[k].y,dcRect[k].x+dcRect[k].width,dcRect[k].y);
            g.setColor(c2);
            g.drawLine(dcRect[k].x+w,dcRect[k].y,dcRect[k].x+dcRect[k].width,dcRect[k].y+dcRect[k].height); 
            g.drawLine(dcRect[k].x+w,dcRect[k].y+dcRect[k].height,dcRect[k].x,dcRect[k].y+dcRect[k].height);
            
            // Tracé du dernier carré en tant que "no color"
            if( onNoColor ) {
               g.setColor(Color.black);
               g.drawLine(dcRect[k].x,dcRect[k].y,dcRect[k].x+dcRect[k].width,dcRect[k].y+dcRect[k].height);
               g.drawLine(dcRect[k].x,dcRect[k].y+dcRect[k].height,dcRect[k].x+dcRect[k].width,dcRect[k].y);
            }
         }
      }
   }
   
   
   
   /** getBrighterColors
    *  @param c  - la couleur de base
    *  @param nb  - le nb desire de couleurs
    *  @return   un tableau de nb couleurs, allant de la teinte de base vers les teintes plus claires
    *  (le principe est de faire varier la couche B du modele HSB)
    */
   protected static Color[] getBrighterColors(Color c, int nb) {
      Color[] colors = new Color[nb];
      float hsb[];
      hsb = Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
      float step = (hsb[1]-0.15f)/(Math.max(1,nb-1));
      
      
      for(int i=0;i<nb;i++) {
         colors[i] = Color.getHSBColor(hsb[0],hsb[1]-i*step,hsb[2]);
      }
      return colors;   
   }

   public void mouseClicked(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
   public void mouseReleased(MouseEvent e) { }

   private ActionListener listener=null;
   public void addActionListener(ActionListener l) { listener=l; }
   public void removeActionListener(ActionListener l) { listener=null; }
}


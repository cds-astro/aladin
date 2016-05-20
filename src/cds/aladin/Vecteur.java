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

import healpix.essentials.FastMath;

import java.awt.Graphics;
import java.awt.Point;

import cds.tools.Util;

/**
* Objet graphique representant un vecteur
* On utilise Forme avec 2 objets :
* - o[0] : le debut
* - o[1] : la fin
* 
* @author Pierre Fernique [CDS]
* @version 1.0 : (déc 11) Création (pour support régions DS9)
*/
public class Vecteur extends Forme {
   
   /**
    * Création d'un vecteur en coordonnées cartésiennes
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param v la vue utilisée pour le système de coord cartésienne, (ou null si aucun)
    * @param xv l'absisse du début
    * @param yv l'ordonnée du début
    * @param w la taille
    * @param angle (en degrés - trigo)
    */
   protected Vecteur(Plan plan, ViewSimple v, double xv, double yv,double w, double angle) {
      super(plan,new Position[2]);
      double x,y,d;
      o[0] = new Position(plan,v,xv,yv,0,0,XY|RADE_COMPUTE,null);
      double a = Math.toRadians(angle);
      double b = Math.toRadians(90);
      d= b+a;
      x = xv + w*FastMath.sin(d);
      y = yv + w*FastMath.cos(d);
      o[1] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      setObjet(o);
   }
   
   /**
    * Création d'un vecteur avec des coordonnées sphériques
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param c Coordonnées du début
    * @param w longueur (en degrés)
    * @param angle (en degrés - trigo)
    */
   protected Vecteur(Plan plan,Coord c,double w, double angle) {
      super(plan,new Position[2]);
      o[0] = new Position(plan,null,0,0,c.al,c.del,RADE,null);
      Coord c1 = applySphereRot(c,w,270+angle);
      o[1] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      setObjet(o);
   }
   
   /** Pour faire plaisir aux objets dérivées (Pickle par exemple) */
   protected Vecteur(Plan plan,Position o[]) { super(plan,o); }   
   
   /** Retourne le type d'objet */
   public String getObjType() { return "vector"; }
   
   /** Test d'appartenance. */
   protected boolean inside(ViewSimple v,double x, double y) { return in(v,x,y); }
   
   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve sur le périmètre
    * @param v la vue concernée
    * @param x,y le point a tester dans le plan de projection de la vue
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   protected boolean in(ViewSimple v,double x, double y) {  
      if( !isVisible() ) return false;
      if( o[0].in(v,x,y) || o[1].in(v,x,y) ) return true;
      if( inLigne(1,v,x,y) ) return true;
      return false;
   }
   
   // Retourne true si on se trouve sur la ligne i (de 1 à 4)
   private boolean inLigne(int i,ViewSimple v,double x,double y) {
      
      PointD p1 = v.getViewCoordDble(o[i-1].xv[v.n],o[i-1].yv[v.n]);
      PointD p2 = v.getViewCoordDble(o[i].xv[v.n],o[i].yv[v.n]);
      PointD p = v.getViewCoordDble(x,y);
      return Ligne.inLigne(p1.x,p1.y,p2.x,p2.y,p.x,p.y,mouseDist(v));

//      return Ligne.inLigne(o[i-1].xv[v.n],o[i-1].yv[v.n],o[i].xv[v.n],o[i].yv[v.n],x,y,mouseDist(v));
   }
   
   /** Test d'appartenance sur un des coins
    * Retourne vrai si le point (x,y) de l'image se trouve sur un des bouts de l'objet
    * @param v la vue courante
    * @param x,y le point a tester
    */
    protected boolean inBout(ViewSimple v, double x, double y) {
       if( !isVisible() ) return false;
       for( int i=0; i<o.length; i++ ) if( o[i].in(v,x,y) ) return true;
       return false;
    }
          
    /** Il suffit qu'un des coins soit dans le rectangle pour retourner vrai */
    protected boolean inRectangle(ViewSimple v,RectangleD r) {
       for( int i=0; i<o.length; i++ ) if( o[i].inRectangle(v,r) ) return true;
       return false;
    }
    
    /** Dessine l'objet dans le contexte graphique en fonction:
     * @param g : le contexte graphique
     * @param v : la vue concernée
     * @param dx : un éventuel offset en absisse (impression sur papier par exemple)
     * @param dy : un éventuel offset en ordonnée (idem)
     */
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( !isVisible() ) return false;
      
      Point op=null;
      for( int i=0; i<o.length; i++ ) {
         if( o[i].xv==null ) return false;
         Point p = v.getViewCoord(o[i].xv[v.n],o[i].yv[v.n]);
         if( p==null ) return false;
         p.x+=dx; p.y+=dy;
         if( op!=null ) {
            g.setColor( getColor() );
            Util.drawFleche(g, op.x, op.y, p.x, p.y, 5, null);
         }
         if( isSelected() ) drawSelect(g,v,i);
         op=p;
      }
      return true;
   }
}

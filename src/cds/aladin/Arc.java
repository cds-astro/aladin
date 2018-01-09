// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import cds.astro.Proj3;

/**
* Objet graphique representant un arc de cercle
* On utilise Forme avec 3 objets :
* - o[0] : le centre
* - o[1] : le début de l'arc
* - o[2] : la fin de l'arc
* 
* @author Pierre Fernique [CDS]
* @version 2.0 : (15 déc 05) Modification complète en utilisant Forme.
* @version 1.0 : (11 sept 05) Création en vue de l'implantation des FoV HST
*/
public class Arc extends Cercle {
   
   protected double angle;
      
   /**
    * Création d'un arc de cercle en coordonnées cartésiennes
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param v la vue utilisée pour le système de coord cartésienne, (ou null si aucun)
    * @param xv l'absisse du centre
    * @param yv l'ordonnée du centre
    * @param rv2 le rayon
    * @param startAngleNorth l'angle de départ (à partir du Nord dans le sens trigo)
    * @param angle l'angle du Pickle (à partir du startAngleNorth dans le sens trigo)
    */
   protected Arc(Plan plan, ViewSimple v, double xv, double yv,double rv,double startAngleNorth,double angle) {
      super(plan,new Position[3]);
      this.angle=angle;
      double x,y,a;
      o[0] = new Position(plan,v,xv,yv,0,0,XY|RADE_COMPUTE,null);
      a =startAngleNorth;
      a = Math.PI*a/180.;
      x = xv+ rv*Math.sin(a);
      y = yv+ rv*Math.cos(a);
      o[1] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      a =startAngleNorth+angle;
      a = Math.PI*a/180.;
      x = xv+ rv*Math.sin(a);
      y = yv+ rv*Math.cos(a);
      o[2] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      setObjet(o);
   }
   
   /**
    * Création d'un Arc avec des coordonnées sphériques
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param c Coordonnées du centre
    * @param radius1 rayon en degrés
    * @param startAngleNorth l'angle de départ (à partir du Nord dans le sens trigo)
    * @param angle l'angle du Pickle (à partir du startAngleNorth dans le sens trigo)
    */
   protected Arc(Plan plan,Coord c,double radius,double startAngleNorth,double angle) {
      super(plan,new Position[3]);
      this.angle=angle;
      o[0] = new Position(plan,null,0,0,c.al,c.del,RADE,null);
      Coord c1 = applySphereRot(c,radius,startAngleNorth);
      o[1] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      Coord c2 = applySphereRot(c,radius,startAngleNorth+angle);
      o[2] = new Position(plan,null,0,0,c2.al,c2.del,RADE,null);
      setObjet(o);
   }
   
   /** Pour faire plaisir aux objets dérivées (Pickle par exemple) */
   protected Arc(Plan plan,Position o[]) { super(plan,o); }   
   
   /** Retourne le type d'objet */
   public String getObjType() { return "Arc"; }
   
   /** Retourne l'angle initiale à partir de 3H dans le sens trigo */
   protected double getStartAngle3H(ViewSimple v) {
      return 180.*Math.atan2( o[0].yv[v.n]-o[1].yv[v.n] , o[1].xv[v.n]-o[0].xv[v.n])/Math.PI;
   }

   /** Retourne l'angle finale à partir de 3H dans le sens trigo */
   protected double getEndAngle3H(ViewSimple v) {
      return 180.*Math.atan2( o[0].yv[v.n]-o[2].yv[v.n] , o[2].xv[v.n]-o[0].xv[v.n])/Math.PI;
   }
   
   /** Retourne l'angle mémorisé pour éviter de retourner un éventuel complément */
   protected double getAngle() { return angle; }

     
   /** Test d'appartenance. */
   protected boolean inside(ViewSimple v,double x, double y) { return in(v,x,y); }
   
   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve SUR l'arc de cercle
    * @param v la vue concernée
    * @param x,y le point a tester dans le plan de projection de la vue
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   protected boolean in(ViewSimple v,double x, double y) {     
      if( !super.in(v,x,y) ) return false;
      double sa = getStartAngle3H(v);      
      double a = 180.*Math.atan2(o[0].yv[v.n]-y,x-o[0].xv[v.n])/Math.PI;
      double a1 = a-sa;
      return getAngle()>0 ? -1<a1 && a1<getAngle()+1
                : getAngle()-1<a1 && a1<1;
      
   }
   
   /** Test d'appartenance sur un bout
    * Retourne vrai si le point (x,y) de l'image se trouve sur un des bouts de l'objet
    * @param v la vue courante
    * @param x,y le point a tester
    */
    protected boolean inBout(ViewSimple v, double x, double y) {
       if( !isVisible() ) return false;
       return o[1].in(v,x,y) || o[2].in(v,x,y);
    }
          
    /** Il suffit qu'un des bouts de l'arc soit dans le rectangle pour retourner vrai */
    protected boolean inRectangle(ViewSimple v,RectangleD r) {
       for( int i=1; i<o.length; i++ ) if( o[i].inRectangle(v,r) ) return true;
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
      int L = (int)Math.round( getRayon(v)*v.getZoom() );
      Point p = o[0].getViewCoord(v,L*2,L*2);
      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      g.setColor( getColor() );
      double sa = getStartAngle3H(v);
      double a = getAngle();
      g.drawArc(p.x-L,p.y-L,L*2,L*2,(int)Math.round(sa),(int)Math.round(a));
      
      if( isSelected() ) {
         if( plan!=null && plan.type==Plan.APERTURE ) return true;
         drawSelect(g,v,1);
         drawSelect(g,v,2);
      }
      return true;
   }
}

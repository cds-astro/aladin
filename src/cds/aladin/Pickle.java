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

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;

import cds.tools.Util;

/**
* Objet graphique representant un secteur de couronne (Pickle)
* On utilise Forme avec 5 objets :
* - o[0] : le centre
* - o[1] : le début de l'arc le plus éloigné du centre
* - o[2] : la fin de l'arc le plus éloigné du centre
* - o[3] : le début de l'arc le plus proche du centre
* - o[4] : la fin de l'arc le plus proche du centre
* 
* @author Pierre Fernique [CDS]
* @version 2.0 : (15 déc 05) Modification complète en utilisant Forme.
* @version 1.0 : (11 sept 05) Création en vue de l'implantation des FoV HST
*/
public class Pickle extends Arc {
   
   /**
    * Création d'un Pickle avec des coordonnées cartésiennes
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param v la vue utilisée pour le système de coord cartésienne, (ou null si aucun)
    * @param xv l'absisse du centre
    * @param yv l'ordonnée du centre
    * @param rv1 le rayon intérieur
    * @param rv2 le rayon extérieur
    * @param startAngleNorth l'angle de départ (à partir du Nord dans le sens trigo)
    * @param angle l'angle du Pickle (à partir du startAngleNorth dans le sens trigo)
    */
   protected Pickle(Plan plan, ViewSimple v, double xv, double yv,double rv1,double rv2,double startAngleNorth,double angle) {
      super(plan,new Position[5]);
      this.angle=angle;
      double x,y,a;
      o[0] = new Position(plan,v,xv,yv,0,0,XY|RADE_COMPUTE,null);
      a = startAngleNorth;
      a = Math.PI*a/180.;
      x = xv+ rv2*Math.sin(a);
      y = yv+ rv2*Math.cos(a);
      o[1] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      a = startAngleNorth+angle;
      a = Math.PI*a/180.;
      x = xv+ rv2*Math.sin(a);
      y = yv+ rv2*Math.cos(a);
      o[2] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      a = startAngleNorth;
      a = Math.PI*a/180.;
      x = xv+ rv1*Math.sin(a);
      y = yv+ rv1*Math.cos(a);
      o[3] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      a = startAngleNorth+angle;
      a = Math.PI*a/180.;
      x = xv+ rv1*Math.sin(a);
      y = yv+ rv1*Math.cos(a);
      o[4] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      setObjet(o);
   }
      
   /**
    * Création d'un Pickle avec des coordonnées cartésiennes
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param c Coordonnées du centre
    * @param radius1 rayon intérieur en degrés
    * @param radius2 rayon extérieur en degrés
    * @param startAngleNorth l'angle de départ (à partir du Nord dans le sens trigo)
    * @param angle l'angle du Pickle (à partir du startAngleNorth dans le sens trigo)
    */
   protected Pickle(Plan plan,Coord c,double radius1,double radius2,double startAngleNorth,double angle) {
      super(plan,new Position[5]);
      this.angle=angle;
      Coord c1,c2;
      o[0] = new Position(plan,null,0,0,c.al,c.del,RADE,null);
      c1 = applySphereRot(c,radius2,startAngleNorth);
      o[1] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      c2 = applySphereRot(c,radius2,startAngleNorth+angle);
      o[2] = new Position(plan,null,0,0,c2.al,c2.del,RADE,null);
      c1 = applySphereRot(c,radius1,startAngleNorth);
      o[3] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      c2 = applySphereRot(c,radius1,startAngleNorth+angle);
      o[4] = new Position(plan,null,0,0,c2.al,c2.del,RADE,null);
      setObjet(o);
   }

   /** Retourne le type d'objet */
   public String getObjType() { return "Pickle"; }
   
  /** Calcul le rayon extérieur en fonction de la vue courante */
   protected double getRayonExt(ViewSimple v) { return super.getRayon(v); }

   /** Calcul le rayon intérieur en fonction de la vue courante */
   protected double getRayonInt(ViewSimple v) {
      double dy=o[3].yv[v.n]-o[0].yv[v.n];
      double dx=o[3].xv[v.n]-o[0].xv[v.n];
      return Math.sqrt(dx*dx + dy*dy);
   }
   
   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve SUR l'un des bords de l'objet
    * @param v la vue concernée
    * @param x,y le point a tester dans le plan de projection de la vue
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   protected boolean in(ViewSimple v,double x, double y) {     
      if( !isVisible() ) return false;
      
      double l = mouseDist(v);
      
      // Sur un des deux arcs ?
      if( Cercle.inPerimetre(o[0].xv[v.n],o[0].yv[v.n],getRayonInt(v), x,y,l) ||
          Cercle.inPerimetre(o[0].xv[v.n],o[0].yv[v.n],getRayonExt(v), x,y,l)  ) {
         double sa = getStartAngle3H(v);      
         double a = 180.*Math.atan2(o[0].yv[v.n]-y,x-o[0].xv[v.n])/Math.PI;
         double a1 = a-sa;
         return getAngle()>0 ? -1<a1 && a1<getAngle()+1
                   : getAngle()-1<a1 && a1<1;
      }
      
      // Sur le premier coté ?
      PointD p1 = v.getViewCoordDble(o[1].xv[v.n],o[1].yv[v.n]);
      PointD p2 = v.getViewCoordDble(o[2].xv[v.n],o[2].yv[v.n]);
      PointD p = v.getViewCoordDble(x,y);
      if( Ligne.inLigne(p1.x,p1.y,p2.x,p2.y,p.x,p.y,mouseDist(v)) ) return true;

      // Sur le deuxième coté ?
      p1 = v.getViewCoordDble(o[2].xv[v.n],o[2].yv[v.n]);
      p2 = v.getViewCoordDble(o[4].xv[v.n],o[4].yv[v.n]);
      return Ligne.inLigne(p1.x,p1.y,p2.x,p2.y,p.x,p.y,mouseDist(v));
      
      // Sur un des cotés ?
//      return Ligne.inLigne(o[1].xv[v.n],o[1].yv[v.n], o[3].xv[v.n],o[3].yv[v.n], x,y,l)
//          || Ligne.inLigne(o[2].xv[v.n],o[2].yv[v.n], o[4].xv[v.n],o[4].yv[v.n], x,y,l);
   }
   

   /** Test d'appartenance sur un bout
    * Retourne vrai si le point (x,y) de l'image se trouve sur un des bouts de l'objet
    * @param v la vue courante
    * @param x,y le point a tester
    */
    protected boolean inBout(ViewSimple v, double x, double y) {
       if( !isVisible() ) return false;
       return o[1].inBout(v,x,y) || o[2].inBout(v,x,y) || o[3].inBout(v,x,y) || o[4].inBout(v,x,y);
    }
       
   /** Dessine l'objet dans le contexte graphique en fonction:
    * @param g : le contexte graphique
    * @param v : la vue concernée
    * @param dx : un éventuel offset en abscisse (impression sur papier par exemple)
    * @param dy : un éventuel offset en ordonnée (idem)
    */
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( !isVisible() ) return false;
      
      g.setColor( getColor() );

      double sa = getStartAngle3H(v);
      double ea = getEndAngle3H(v);
      double angle = getAngle();
      
      int L1 = (int)Math.round( getRayonExt(v)*v.getZoom() );
      int D = L1*2;
      int L2 = (int)Math.round( getRayonInt(v)*v.getZoom() );
      Point p = o[0].getViewCoord(v,D,D);
      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      
      // gestion de la transparence
      if( plan!=null && Aladin.isFootprintPlane(plan) && plan.getOpacityLevel()>0.02 &&
    		  Aladin.ENABLE_FOOTPRINT_OPACITY && g instanceof Graphics2D ) {
    	 Composite saveComposite=null;
         Graphics2D g2d = (Graphics2D)g;
         saveComposite = g2d.getComposite();
         Composite myComposite = Util.getFootprintComposite(plan.getOpacityLevel());
         g2d.setComposite(myComposite);
         // TODO : remarque : beaucoup de new ici, voir si on peut optimiser cela
         Area pickle = new Area(new Arc2D.Double( p.x-L1, p.y-L1, L1*2,L1*2, sa,angle, Arc2D.PIE ));
         pickle.subtract(new Area(new Arc2D.Double( p.x-L2, p.y-L2, L2*2,L2*2, 0.0,360.0, Arc2D.PIE )  ));
         g2d.fill(pickle);
         
         
         g2d.setComposite(saveComposite);
      }
      
      g.drawArc(p.x-L1,p.y-L1,L1*2,L1*2,(int)Math.round(sa),(int)Math.round(angle));            
      g.drawArc(p.x-L2,p.y-L2,L2*2,L2*2,(int)Math.round(sa),(int)Math.round(angle));

      // On dessine encore les 2 segments de droite
      Point a,b;
      a = o[1].getViewCoord(v,D,D);
      b = o[3].getViewCoord(v,D,D);
      if( a!=null && b!=null ) g.drawLine(dx+a.x,dy+a.y,dx+b.x,dy+b.y);
      a = o[2].getViewCoord(v,D,D);
      b = o[4].getViewCoord(v,D,D);
      if( a!=null && b!=null ) g.drawLine(dx+a.x,dy+a.y,dx+b.x,dy+b.y);

      if( isSelected() ) {
         if( plan!=null && plan.type==Plan.APERTURE ) return true;
         for( int i=1; i<5; i++ ) drawSelect(g,v,i);
      }
      return true;
   }
}

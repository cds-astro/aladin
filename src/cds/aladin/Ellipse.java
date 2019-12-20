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

import java.awt.Graphics;

import cds.tools.Util;

/**
* Objet graphique representant une ellipse
* On utilise Forme avec 3 objets :
* - o[0] : le centre
* - o[1] : le point au grand axe
* - o[2] : le poitn au petit axe
* - o[3] : le point au grand axe
* - o[4] : le poitn au petit axe

* 
* @author Pierre Fernique [CDS]
* @version 1.0 : (déc 11) Création (pour support régions DS9)
*/
public class Ellipse extends Forme {
   
   protected double angle;
      
   /**
    * Création d'une boite en coordonnées cartésiennes
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param v la vue utilisée pour le système de coord cartésienne, (ou null si aucun)
    * @param xv l'absisse du centre
    * @param yv l'ordonnée du centre
    * @param w la largeur
    * @param h la hauteur
    * @param angle l'angle de la boite (dans le sens trigo)
    */
   protected Ellipse(Plan plan, ViewSimple v, double xv, double yv,double w, double h,double angle) {
      super(plan,new Position[5]);
      this.angle=angle;
      double x,y,d;
      o[0] = new Position(plan,v,xv,yv,0,0,XY|RADE_COMPUTE,null);
      double demiDia = Math.sqrt(w*w/4+h*h/4);
      double a = Math.atan2(h/2,w/2);
      double b = Math.toRadians(90+angle);
      d= b+a;
      x = xv + demiDia*Math.sin(d);
      y = yv + demiDia*Math.cos(d);
      o[1] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      d= b-a;
      x = xv + demiDia*Math.sin(d);
      y = yv + demiDia*Math.cos(d);
      o[2] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      d= b+Math.PI-a;
      x = xv + demiDia*Math.sin(d);
      y = yv + demiDia*Math.cos(d);
      o[4] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      d= b+Math.PI+a;
      x = xv + demiDia*Math.sin(d);
      y = yv + demiDia*Math.cos(d);
      o[3] = new Position(plan,v,x,y,0,0,XY|RADE_COMPUTE,null);
      setObjet(o);
   }
   
   /**
    * Création d'une boite avec des coordonnées sphériques
    * @param plan le plan d'appartenance de l'objet (ou null si aucun)
    * @param c Coordonnées du centre
    * @param w largeur (en degrés)
    * @param h hauteur (en degrés)
    * @param angle angle (dans le sens trigo)
    */
   protected Ellipse(Plan plan,Coord c,double semiMA, double semiMI,double angle) {
      super(plan,new Position[5]);
      this.angle=angle;
      double b=90;
      o[0] = new Position(plan,null,0,0,c.al,c.del,RADE,null);
      Coord c1 = applySphereRot(c,semiMA,b+angle);
      o[1] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      c1 = applySphereRot(c,semiMI,b+angle+90);
      o[2] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      c1 = applySphereRot(c,semiMA,b+angle+180);
      o[3] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      c1 = applySphereRot(c,semiMI,b+angle+270);
      o[4] = new Position(plan,null,0,0,c1.al,c1.del,RADE,null);
      setObjet(o);
   }
   
   /** Pour faire plaisir aux objets dérivées (Pickle par exemple) */
   protected Ellipse(Plan plan,Position o[]) { super(plan,o); }   
   
   /** Retourne le type d'objet */
   public String getObjType() { return "ellipse"; }
   
   /** Retourne l'angle mémorisé pour éviter de retourner un éventuel complément */
   protected double getAngle() { return angle; }
   
   /** Retourne le demi grand axe */
   protected double getSemiMA(ViewSimple v) {
      double dy=o[1].yv[v.n]-o[0].yv[v.n];
      double dx=o[1].xv[v.n]-o[0].xv[v.n];
      return Math.sqrt(dx*dx + dy*dy);
   }

   /** Retourne le demi petit axe */
   protected double getSemiMI(ViewSimple v) {
      double dy=o[2].yv[v.n]-o[0].yv[v.n];
      double dx=o[2].xv[v.n]-o[0].xv[v.n];
      return Math.sqrt(dx*dx + dy*dy);
   }
     
   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve  dans l'ellipse
    * @param x,y le point a tester
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   protected boolean inside(ViewSimple v,double x, double y) { return in1(v,x,y,false); }
   
   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve sur SUR le l'ellipse
    * @param x,y le point a tester
    * @param z valeur courante du zoom
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   protected boolean in(ViewSimple v,double x, double y) { return in1(v,x,y,true); }
   
   private boolean in1(ViewSimple v,double x, double y,boolean flagPerimetre) {
      if( !isVisible() ) return false;
      
      
//      PointD p1 = v.getViewCoordDble(x, y);
//      PointD p0 =v.getViewCoordDble(o[0].xv[v.n],o[0].yv[v.n]);
//      double xc = Math.abs(p1.x-p0.x);
//      double yc = Math.abs(p1.y-p0.y);
//      double u  = Math.toRadians(angle)-Math.atan2(yc,xc);
//      double len = Math.sqrt(xc*xc+yc*yc);
//      
//      double semiMA = getSemiMA(v)*v.zoom;
//      double semiMI = getSemiMI(v)*v.zoom;
//      double xp = semiMA*Math.cos(u);
//      double yp = semiMI*Math.sin(u);
//      double p = Math.sqrt(xp*xp+yp*yp);
//      return flagPerimetre ? Math.abs(len-p)<mouseDist(v) : len<p;
     
      double xc = Math.abs(x-o[0].xv[v.n]);
      double yc = Math.abs(y-o[0].yv[v.n]);
      double u  = Math.toRadians(angle)-Math.atan2(yc,xc);
      double len = Math.sqrt(xc*xc+yc*yc);
      
      double semiMA = getSemiMA(v);
      double semiMI = getSemiMI(v);
      double xp = semiMA*Math.cos(u);
      double yp = semiMI*Math.sin(u);
      double p = Math.sqrt(xp*xp+yp*yp);
      return flagPerimetre ? Math.abs(len-p)<3+9/v.getZoom() : len<p;
   }

   /** Il faut que le centre de l'ellipse soit dans le rectangle pour retourner vrai */
   protected boolean inRectangle(ViewSimple v,RectangleD r) {
      return o[0].inRectangle(v,r);
   }
   
   /** Test d'appartenance sur un des points de controle
    * Retourne vrai si le point (x,y) de l'image se trouve sur un des bouts de l'objet
    * @param v la vue courante
    * @param x,y le point a tester
    */
    protected boolean inBout(ViewSimple v, double x, double y) {
       if( !isVisible() ) return false;
       for( int i=1; i<o.length; i++ ) if( o[i].in(v,x,y) ) return true;
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
      
      PointD [] p = new PointD[3];
      for( int i=0; i<p.length; i++ ) {
         p[i] = v.getViewCoordDble(o[i].xv[v.n],o[i].yv[v.n]);
         if( p[i]==null ) return false;
         p[i].x+=dx; p[i].y+=dy;
      }

      double dxMA = p[1].x - p[0].x;
      double dyMA = p[1].y - p[0].y;
      double dxMI = p[2].x - p[0].x;
      double dyMI = p[2].y - p[0].y;
      double demiMA = Math.sqrt(dxMA*dxMA+dyMA*dyMA);
      double demiMI = Math.sqrt(dxMI*dxMI+dyMI*dyMI);
      double a = Math.toDegrees( Math.atan2(dyMA, dxMA) );
      
      g.setColor( getColor() );
      Util.drawEllipse(g, p[0].x, p[0].y, demiMA, demiMI, a);
      if( isSelected ()  ) for( int i=1; i<o.length; i++ ) drawSelect(g,v,i);
      return true;
   }
}

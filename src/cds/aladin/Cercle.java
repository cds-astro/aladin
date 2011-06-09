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

import java.awt.*;
import java.awt.geom.Ellipse2D;

import cds.tools.Util;

/**
* Objet graphique representant un cercle
*
* On utilise Forme avec 2 objets :
* - o[0] : le centre du cercle
* - o[1] : un point sur le rayon y = yc+r;
*
* @author Pierre Fernique [CDS]
* @version 2.0 : (15 déc 05) Modification complète en utilisant Forme.
* @version 1.0 : (11 sept 05) Création en vue de l'implantation des FoV HST
*/
public class Cercle extends Forme {

   protected Color couleur=null; // Couleur alternative


   /** Creation d'un repere graphique sans connaitre RA/DE.
    * @param plan plan d'appartenance de la ligne
    * @param x,y  position
    */
   protected Cercle(Plan plan, ViewSimple v, double xv, double yv,double rv) {
      super(plan,new Position[2]);
      o[0] = new Position(plan,v,xv,yv,0,0,XY|RADE_COMPUTE,null);
      o[1] = new Position(plan,v,xv,yv+rv,0,0,XY|RADE_COMPUTE,null);
      setObjet(o);
   }

   /** Creation d'un repere speciale de positionnement dans l'ecran */
   protected Cercle(Plan plan,Coord c,double radius) {
      super(plan,new Position[2]);
      o[0] = new Position(plan,null,0,0,c.al,c.del,RADE,null);
      double del = c.del+radius;
      if( del>90 ) del = c.del-radius;
      o[1] = new Position(plan,null,0,0,c.al,del,RADE,null);
      setObjet(o);
   }

   protected Cercle(Plan plan,Position o[]) { super(plan,o); }

   /** Retourne le type d'objet */
   @Override
public String getObjType() { return "Circle"; }

   /** Retourne le rayon */
   protected double getRayon(ViewSimple v) {
      double dy=o[1].yv[v.n]-o[0].yv[v.n];
      double dx=o[1].xv[v.n]-o[0].xv[v.n];
      return Math.sqrt(dx*dx + dy*dy);
   }

   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve  dans le cercle
    * @param x,y le point a tester
    * @param z valeur courante du zoom
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   @Override
protected boolean inside(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      double xc = Math.abs(x-o[0].xv[v.n]);
      double yc = Math.abs(y-o[0].yv[v.n]);
      return( Math.sqrt(xc*xc + yc*yc) < getRayon(v) );
   }

   /** Test d'appartenance.
    * Retourne vrai si le point (x,y) de l'image se trouve sur SUR le cercle
    * @param x,y le point a tester
    * @param z valeur courante du zoom
    * @return <I>true</I> c'est bon, <I>false</I> sinon
    */
   @Override
protected boolean in(ViewSimple v,double x, double y) {
      if( !isVisible() ) return false;
      return inPerimetre(o[0].xv[v.n],o[0].yv[v.n],getRayon(v), x,y,3+9/v.getZoom());

//      double l=getRayon(v);
//      double xc = Math.abs(x-o[0].xv[v.n]);
//      double yc = Math.abs(y-o[0].yv[v.n]);
//      double d = Math.sqrt(xc*xc + yc*yc);
//      return d>l-5 && d<l+5;
   }

   /** Test d'appartenance d'un point sur un cercle (à l pixels prêts ) */
   static protected boolean inPerimetre(double xc,double yc, double r, double x,double y,double l) {
      x -= xc;
      y -= yc;
      return Math.abs(x*x + y*y - r*r) < l*l;
   }

   /** Il faut que le centre du cercle soit dans le rectangle pour retourner vrai */
   @Override
protected boolean inRectangle(ViewSimple v,RectangleD r) {
      return o[0].inRectangle(v,r);
   }


   @Override
protected boolean inClip(ViewSimple v, Rectangle clip) {
      boolean rep = super.inClip(v,clip);
      return rep;
   }

   /** Generation d'un clip englobant.
    * Retourne un rectangle qui englobe l'objet
    * @param zoomview reference au zoom courant
    * @return         le rectangle enblobant
    */
//   protected Rectangle getClip(ViewSimple v) {
//      if( !visible ) return null;
//      Rectangle r;
//      int L = (int)Math.round( getRayon(v)*v.getZoom() )+1;
//      Point p = o[0].getViewCoord(v,L,L);
//      if( p==null ) return null;
//      if( select ) r = new Rectangle(p.x-L-DS/2,p.y-L-DS/2,L*2+DS,L*2+DS);
//      else r = new Rectangle(p.x-L,p.y-L,L*2,L*2);
//      return r;
//   }
   @Override
protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      int L = (int)Math.round( getRayon(v)*v.getZoom() )+1;
      Point p = o[0].getViewCoord(v,L,L);
      if( p==null ) return clip;
      if( isSelected() ) return unionRect(clip, p.x-L-DS/2,p.y-L-DS/2,L*2+DS,L*2+DS);
      return unionRect(clip, p.x-L,p.y-L,L*2,L*2);
   }

   /** Dessine les poignees de selection de l'objet */
   @Override
protected void drawSelect(Graphics g,ViewSimple v) {
      Rectangle r = getClip(v);
      int xc=0;
      int yc=0;

      // Trace des poignees de selection
      for( int i=0; i<4; i++ ) {
         switch(i ) {
            case 0: xc=r.x+r.width/2-DS; yc=r.y; break;                // Bas
            case 1: xc=r.x+r.width/2-DS; yc=r.y+r.height-DS; break;       // Haut
            case 2: xc=r.x+r.width-DS; yc=r.y+r.height/2-DS;  break;      // Droite
            case 3: xc=r.x; yc=r.y+r.height/2-DS;  break;              // Gauche
         }
         g.setColor( Color.green );
         g.fillRect( xc+1,yc+1 , DS,DS );
         g.setColor( Color.black );
         g.drawRect( xc,yc , DS,DS );
      }
   }

   /** Détermination de la couleur de l'objet */
   protected Color getColor() {
   	  if( couleur!=null ) return couleur;
   	  if( plan!=null && plan.type==Plan.APERTURE ) {
   	  	couleur = ((PlanField)plan).getColor(this);
   	  	if( couleur==null ) return plan.c;
   	  	return couleur;
   	  }
   	  if( plan!=null ) return plan.c;
   	  return Color.black;
   }

   /** Affiche le repere
    * @param g        le contexte graphique
    * @param zoomview reference au zoom courant
    */
   @Override
protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( !isVisible() ) return false;
      int L = (int)Math.round( getRayon(v)*v.getZoom() );
      Point p = o[0].getViewCoord(v,L,L);
      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      g.setColor( getColor());

      // gestion de la transparence
      if( plan!=null && plan.getOpacityLevel()>0.02 && Aladin.isFootprintPlane(plan) &&
    		  Aladin.ENABLE_FOOTPRINT_OPACITY && g instanceof Graphics2D ) {
         Composite saveComposite=null;
         Graphics2D g2d = (Graphics2D)g;
         saveComposite = g2d.getComposite();
         Composite myComposite = Util.getFootprintComposite(plan.getOpacityLevel());
         g2d.setComposite(myComposite);
         g2d.fill(new Ellipse2D.Double(p.x-L,p.y-L,L*2,L*2));

         g2d.setComposite(saveComposite);

      }


      g.drawOval(p.x-L,p.y-L,L*2,L*2);

      if( isSelected() ) {
         if( plan!=null && plan.type==Plan.APERTURE ) return true;
         g.setColor( Color.green );
         drawSelect(g,v);
      }
      return true;
   }
}

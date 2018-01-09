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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Vector;

import cds.aladin.Pcat.PlanObjetIterator;
import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.astro.Proj3;

/**
 * Forme composée de plusieurs objets
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (déc 2005) création
 */
public class Forme extends Position {

   protected Color couleur=null; // Couleur alternative
   public Position o[];		 // Liste des objets qui compose la forme

   protected void createCacheXYVP() {
      if( o==null ) return;
      for( int i=0; i<o.length; i++ ) o[i].createCacheXYVP();
   }
   protected void createCacheXYVP(int dim) {
      if( o==null ) return;
      for( int i=0; i<o.length; i++ ) o[i].createCacheXYVP(dim);
   }
//   protected void resetVP(){
//      if( !Aladin.VP ) return;
//      for( int i=0; i<Aladin.aladin.view.modeView; i++ ) oiz[i]=0;
//   }
   protected Forme(Plan plan,Position o[]) {
      super(plan);
      this.o=o;
   }

   public Vector getProp() {
      Vector propList = super.getProp();

      final Couleur col = new Couleur(couleur,true);
      PropAction changeCouleur = new PropAction() {
         public int action() {
            Color c= col.getCouleur();
            if( c==couleur ) return PropAction.NOTHING;
            setColor(c);
            return PropAction.SUCCESS;
         }
      };
      propList.add( Prop.propFactory("color","Color","Alternative color",col,null,changeCouleur) );
      return propList;
  }

   /** Provide RA J2000 position */
   public double getRa() { return o[0].getRa(); }

   /** Provide DEC J2000 position */
   public double getDec() { return o[0].getDec(); }

   public void setColor(Color c) { couleur=c; }

   public void setRaDec(double ra, double de) {
      double dra = o[0].getRa()-ra;
      double dde = o[0].getDec()-de;
      for( int i=0; i<o.length; i++ ) o[i].deltaRaDec(dra, dde);
   }

   /** Retourne le type d'objet */
   public String getObjType() { return "ComposedObject"; }

   protected void setObjet(Position o[]) { this.o = o; }

   protected void setCoord(ViewSimple v) {
      for( int i=0; i<o.length; i++ ) o[i].setCoord(v);
   }
   protected void setCoord(ViewSimple v,Projection proj) {
      for( int i=0; i<o.length; i++ ) o[i].setCoord(v,proj);
   }
   protected void setXY(Projection proj) {
      for( int i=0; i<o.length; i++ ) o[i].setXY(proj);
   }
   protected void setXYTan(double x, double y) {
      for( int i=0; i<o.length; i++ ) o[i].setXYTan(x,y);
   }
   protected void setXYTan(Coord center) {
      for( int i=0; i<o.length; i++ ) o[i].setXYTan(center);
   }
   protected void projection(ViewSimple v) {
      for( int i=0; i<o.length; i++ ) o[i].projection(v);
   }
   protected void setPosition(ViewSimple v,double x, double y) {};
   protected void deltaPosition(ViewSimple v,double x, double y) {
      for( int i=0; i<o.length; i++ ) o[i].deltaPosition(v,x,y);
   }
   protected void rotatePosition(ViewSimple v,double theta,double x0,double y0) {
      for( int i=0; i<o.length; i++ ) o[i].rotatePosition(v,theta,x0,y0);
   }
   protected void deltaRaDec(double dra, double dde) {
      for( int i=0; i<o.length; i++ ) o[i].deltaRaDec(dra,dde);
   }
//   protected Rectangle getClip(ViewSimple v) { return null; }
   protected Rectangle extendClip(ViewSimple v,Rectangle clip) {
      if( !isVisible() ) return clip;
      if( o.length==0 ) return clip;
      for( int i=0; i<o.length; i++ ) clip = o[i].extendClip(v,clip);
      return clip;
   }
   protected Point getViewCoord(ViewSimple v,int dw, int dh) { return null; }
   protected boolean inside(ViewSimple v,double x, double y) { return false; }
   protected boolean in(ViewSimple v,double x, double y) { return false; }
   protected boolean inBout(ViewSimple v,double x,double y) { return false; }
   protected boolean inRectangle(ViewSimple v,RectangleD r) { return false; }
   protected boolean draw(Graphics g,ViewSimple v,int dx,int dy) { return false; }
   protected void drawSelect(Graphics g,ViewSimple v) {}
   protected void drawSelect(Graphics g,ViewSimple v,int i) {
      int ds=DS/2;
      Point p = o[i].getViewCoord(v,0,0);
      if( p==null ) return;
      g.setColor( Color.green );
      g.fillRect( p.x-ds+1, p.y-ds+1, DS-1,DS-1 );
      g.setColor( Color.black );
      g.drawRect( p.x-ds, p.y-ds, DS,DS );
   }
   protected void setSelect(boolean flag) {
      super.setSelect(flag);
      for( int i=0; i<o.length; i++ ) o[i].setSelect(flag);
   }
   protected void setVisibleGenerique(boolean flag) {
      super.setVisibleGenerique(flag);
      for( int i=0; i<o.length; i++ ) o[i].setVisibleGenerique(flag);
   }
   protected void switchSelect(){
      super.switchSelect();
      for( int i=0; i<o.length; i++ ) o[i].switchSelect();
   }

   /** Détermination de la couleur de l'objet */
   public Color getColor() {
      if( couleur!=null ) return couleur;
      if( plan!=null && plan.type==Plan.APERTURE ) {
         couleur = ((PlanField)plan).getColor(this);
         if( couleur==null ) return plan.c;
         return couleur;
      }
      if( plan!=null ) return plan.c;
      return Color.black;
   }

   /** Rotation en coordonnées sphériques (via le plan tangentiel)
    * @param c Le centre de rotation
    * @param radius le rayon
    * @param angle l'angle en degrés par rapport au Nord dans le sens trigo
    * @return le point au bout du vecteur en coordonnées sphériques
    */
   protected Coord applySphereRot(Coord c, double radius, double angle) {
      if( angle/360.==Math.round(angle/360.) ) return c;
      Proj3 a = new Proj3(Proj3.TAN,c.al,c.del);
      double tanr = Math.tan(Math.PI*radius/180.);
      double cost = Math.cos( Math.PI*angle/180.);
      double sint = Math.sin( Math.PI*angle/180.);
      double x =  tanr*sint;
      double y =  tanr*cost;
//      a.computeAngles(x,y);
      a.set(x,y);
      return new Coord(a.getLon(),a.getLat());
   }
   
   // Recupération d'un itérator sur les objets qui compose la forme
   public Iterator<Obj> iterator() { return new ObjetIterator(); }

   class ObjetIterator implements Iterator<Obj> {
      private int index=0;
      public boolean hasNext() { return index<o.length; }
      public Obj next() { return o[index++]; }
      public void remove() { }
   }



//   void debug();
}

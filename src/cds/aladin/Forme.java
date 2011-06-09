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
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Forme compos�e de plusieurs objets
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (d�c 2005) cr�ation
 */
public class Forme extends Position {
   
   protected Position o[];		// Liste des objets qui compose la forme
   
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
//   void debug();
}

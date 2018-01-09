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

import java.awt.Graphics;
import java.awt.Point;

import cds.tools.Util;

/**
 * Objet graphique representant un repere pour l'extraction d'un Spectre
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : 22 nov 2016) Creation (derive desormais de Repere)
 */
public class RepereSpectrum extends Repere {

   /** Creation d'un repere graphique sans connaitre RA/DE.
    * @param plan plan d'appartenance
    * @param x,y  position
    */
   protected RepereSpectrum(Plan plan, ViewSimple v, double x, double y) {
      super(plan,v,x,y);
   }

   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( !isVisible() ) return false;
      Point p = getViewCoord(v,L,L);
      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      g.setColor( getColor() );
      Util.drawCircle7(g, p.x, p.y);

      if( isSelected() && plan.aladin.view.nbSelectedObjet()<=2 ) cutOn();
      else cutOff();

      return super.draw(g,v,dx,dy);
   }

   protected void remove() { cutOff(); }

   /** Suppression de la coupe memorise dans le zoomView
    * => arret de son affichage
    */
   protected void cutOff() {
      plan.aladin.calque.zoom.zoomView.stopHist();
      plan.aladin.calque.zoom.zoomView.cutOff(this);
   }

   /** Passage du spectre (sous la forme d'un histogramme) au zoomView
    * => affichage d'un histogramme dans le zoomView 
    * @return true si le CutGraph a pu être fait
    */
   protected boolean cutOn() {
      ViewSimple v=plan.aladin.view.getCurrentView();
      if( v==null || plan.aladin.toolBox.getTool()==ToolBox.PAN ) return false;
      Plan pc=v.pref;
      if( !pc.isCube() ) return false;

      double x= xv[v.n];
      double y= yv[v.n];
      int n=pc.getDepth();
      int res[] = new int[n];
      try {
         for( int z=0; z<n; z++ ) res[z] = (pc.getPixel8bit(z,x,y)) & 0xFF;
      } catch( Exception e ) {}

      plan.aladin.calque.zoom.zoomView.setCut(this,res,ZoomView.CUTNORMAL);

      return true;
   }

}

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
import java.awt.Graphics;
import java.awt.Point;
import java.util.Vector;

import javax.swing.JTextField;

import cds.aladin.prop.Prop;
import cds.aladin.prop.PropAction;
import cds.tools.Util;

/**
 * Objet graphique representant une mesure photométrique manuelle sur un anneau
 * PAS TERMINE - PF SEPTEMBRE 2022 => Voir Toolbox.newTool(...) pour l'utiliser
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 (sept 2022): Creation
 */
public class SourceStatRing extends SourceStat {
   
   protected double internalRadius=0;
   
   /** Creation pour les backups */
   protected SourceStatRing(Plan plan) { super(plan); }

   /** Creation à partir d'une position x,y dans l'image
    * @param plan plan d'appartenance
    * @param v vue de référence qui déterminera le PlanBase
    * @param x,y  position
    * @param id identificateur spécifique, ou null pour attribution automatique
    */
   protected SourceStatRing(Plan plan, ViewSimple v, double x, double y,String id) {
      super(plan,v,x,y,id);
   }

   /** Creation à partir d'une position céleste
    * @param plan plan d'appartenance
    * @param v vue de référence qui déterminera le PlanBase
    * @param c coordonnées
    * @param id identificateur spécifique, ou null pour attribution automatique
    */
   protected SourceStatRing(Plan plan,ViewSimple v, Coord c,String id) {
      super(plan,v,c,id);
   }
   
   /** Retourne la rayon interne en degrés */
   public double getInternalRadius() { return internalRadius; }
   
   /** Positionne un rayon interne (avec possibilité d'une unité) + maj des mesures  */
   protected void setInternalRadius(String r) throws Exception {
      internalRadius = Server.getAngleInArcmin(r,Server.RADIUS)/60.;
      if( internalRadius>getRadius() ) throw new Exception();
      resume();
   }


   
   protected void otherProp( Vector propList) {
      final Obj myself = this;
      final JTextField testRadius = new JTextField( 10 );
      final PropAction updateRadius = new PropAction() {
         public int action() { testRadius.setText( Coord.getUnit(getInternalRadius()) ); return PropAction.SUCCESS; }
      };
      PropAction changRadius = new PropAction() {
         public int action() {
            testRadius.setForeground(Color.black);
            String oval = Coord.getUnit(getRadius());
            try {
               String nval = testRadius.getText();
               if( nval.equals(oval) ) return PropAction.NOTHING;
               ((SourceStatRing)myself).setInternalRadius(nval);
               return PropAction.SUCCESS;
            } catch( Exception e1 ) {
               updateRadius.action();
               testRadius.setForeground(Color.red);
            }
            return PropAction.FAILED;
         }
      };
      propList.add( Prop.propFactory("internalradius","Internal radius","",testRadius,updateRadius,changRadius) );
   }

   /** Tracé effectif */
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {      
      if( !super.draw(g,v,dx,dy) ) return false;
      double r = getInternalRadius();
      if( r==0.0 ) return false;
      int l = (int)(r*v.getZoom());
      Point p = getViewCoord(v,l,l);

      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      g.setColor( getColor() );
      if( hasPhot(v.pref) && v.pref==planBase ) {
         Util.drawFillOval(g, p.x-l, p.y-l, l*2, l*2, 0.2f * plan.getOpacityLevel(), null);
      } else g.drawOval(p.x-l, p.y-l, l*2, l*2);
      
            
      return true;
   }

   
}

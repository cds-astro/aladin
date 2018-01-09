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

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

/**
 * Objet graphique pour tracé soit la hauteur soit la base d'une Cote (les deux traits du triangle)
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : jan 2018 - creation
 */
public final class CoteBase extends Ligne {
   
   protected CoteBase( Ligne a, ViewSimple v) {
      super(a.raj, a.dej, a.plan, v);
   }
   
   protected CoteBase(double ra, double dec, Plan plan, ViewSimple v, Ligne debligne,String id) {
      super(ra,dec,plan,v,null,debligne);
      setWithLabel(true);
      this.id=id;
   }
   
   protected boolean draw(Graphics g, ViewSimple v, int dx, int dy) {
      projection(v);
      if( debligne!=null ) debligne.projection(v);
      else finligne.projection(v);
      
      return super.draw(g,v,dx,dy);
   }
   
   /** Dessin du segment à proprement parlé */
   protected void drawLine(Graphics g, ViewSimple v, Point p1, Point p2) {
      if( g instanceof Graphics2D ) {
         Graphics2D g2D = (Graphics2D) g;
         Stroke stroke = g2D.getStroke();
         try { 
            g2D.setStroke(new BasicStroke(0.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] {5f, 5f}, .0f));
            g2D.drawLine(p1.x,p1.y, p2.x,p2.y);
         } finally { g2D.setStroke( stroke ); }
         
      // sinon une ligne normale
      } else  g.drawLine(p1.x,p1.y, p2.x,p2.y);
      
      // Le label est toujours affiché pour ce type de segment
      drawLabel(g,v,p1,p2,id, Aladin.SPLAIN);
   }


}

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

package cds.tools.pixtools;

import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;

import cds.aladin.PointD;
import cds.aladin.ViewSimple;
import cds.moc.TMoc;

/** Gestion d'un Pixel Moc Temporel avec mémorisation des coins,
 * et des coordonnées XY projetés dans une vue particulière
 * @author Pierre Fernique [CDS]
 * @version 1.0 Feb 2018
 */
public final class HpixT extends Hpix {
   
   private long ovIZ;              // Signature de la vue/projection/zoom utilisée pour le calcul de coins[]
   private PointD[] viewCorners;   // les 4 coins en X,Y dans la vue repérée par ovIZ;
   private int offset=0;
   
   /** Création à partir des deux valeurs order et npix */
   public HpixT(int n, int order, long npix) {
      super();
      this.order=order;
      this.npix=npix;
      this.offset=n*13;
   }
   
   /** Retourne true si la case temporelle est hors de la vue courante */
   public boolean isOutView(ViewSimple v) {
      double x1 = TMoc.getTime( order, npix );
      double x2 = TMoc.getTime( order, npix+1 );
      
      double min = v.getTpsMin();
      double max = v.getTpsMax();
      
      return x2<min || x1>max;
//      return (x1<min || x1>max) && (x2<min || x2>max);
   }
   
   /** Trace le losange en aplat */
   public void fill(Graphics g,ViewSimple v) {
      PointD [] b = getProjViewCorners(v);
      if( b==null ) return;
      
      Polygon pol = new Polygon();
      for( int i=0; i<b.length; i++ ) {
         if( b[i]==null ) continue;
         pol.addPoint((int)b[i].x,(int)b[i].y);
      }
      g.fillPolygon(pol);
   }
   
   /** Trace les bords du losange, de sommet à sommet */
   public void draw(Graphics g,ViewSimple v) {
      PointD [] b = getProjViewCorners(v);
      if( b==null ) return;
      
      boolean drawnOk=true;
      
      if( drawnOk ) {
         for( int i=0; i<4; i++ ) {
            int d = i;
            int f = i==3 ? 0 : i+1;
            if( b[d]==null || b[f]==null ) { drawnOk=false; continue; }
            g.drawLine((int)b[d].x,(int)b[d].y, (int)b[f].x,(int)b[f].y);
         }
      }
   }
   
   public Rectangle getClip(ViewSimple v ) {
      PointD [] b = getProjViewCorners(v);
      if( b==null ) return null;
      return new Rectangle( (int)b[3].x, (int)b[3].y, (int)( b[1].x-b[0].x+0.5), (int)(b[0].y - b[3].y+0.5));
   }
   
   private static final int H = 20;  // Hauteur d'une cellule temporaire (pixels de la vue)
   
   /** Retourne les coordonnées X,Y des 4 coins du losange dans la projection de
    * la vue ou null si problème */
   public PointD[] getProjViewCorners(ViewSimple v) {
      
      // déjà fait ?
      long vIZ = v.getIZ();
      if( ovIZ==vIZ ) return viewCorners;
      
      // positionnement en Y dans la vue
      int origY = v.getTpsYviewOrig();
      if( origY<0 ) origY = v.getHeight()+origY;
      
      double tmin = TMoc.getTime( order, npix );
      double tmax = TMoc.getTime( order, npix+1 );
      
      double z = v.getTpsZoom();
      double x = (tmin - v.getTpsMin()) * z;
      double w  = (tmax-tmin) * z;
      
      if( viewCorners==null ) viewCorners = new PointD[4];
      for( int i=0; i<4; i++ ) viewCorners[i] = new PointD(0,0);
      viewCorners[0].x=x;   viewCorners[0].y=origY+H+offset;
      viewCorners[1].x=x+w; viewCorners[1].y=viewCorners[0].y;
      viewCorners[2].x=x+w; viewCorners[2].y=origY+offset;
      viewCorners[3].x=x;   viewCorners[3].y=viewCorners[2].y;
      
      ovIZ=vIZ;
      return viewCorners;
   }
   
}

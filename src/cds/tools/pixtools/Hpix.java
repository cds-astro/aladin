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

package cds.tools.pixtools;

import java.awt.Graphics;
import java.awt.Polygon;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.aladin.HealpixKey;
import cds.aladin.HealpixKeyCat;
import cds.aladin.Localisation;
import cds.aladin.PointD;
import cds.aladin.Projection;
import cds.aladin.ViewSimple;

/** Gestion d'un Pixel Healpix avec mémorisation des coins,
 * et des coordonnées XY projetés dans une vue particulière
 * @author Pierre Fernique [CDS]
 * @version 1.0 March 2011
 */
public final class Hpix {
   // Ordre des indices des coins, Bas -> Gauche -> Haut -> Droite
   static final private int [] ORDRE = { 0,1,3,2 };
   
   private int order;       // Order HEALPIX (=log2(NSIDE))
   private long npix;       // Numéro du pixel (toujours en NESTED)
   private int frame;       // Le système de coordonnées (Localisation.GAL, Localisation.ECLIPTIC, Localisation.ICRS)
   
   private long ovIZ;              // Signature de la vue/projection/zoom utilisée pour le calcul de coins[]
   private PointD[] viewCorners;   // les 4 coins en X,Y dans la vue repérée par ovIZ;
   private Coord[] corners;        // Coordonnées Ra,Dec ICRS des 4 coins du losange
   private int nNull;              // Nombre de coins ou corners indéfinis (derrière le ciel)
   
   private boolean computeCorners; // True si corners[] a déjà été calculé
   
   /** Création à partir d'une chaine selon la syntaxe "order/npix" */
   public Hpix(String s) throws Exception {
      int i = s.indexOf('/');
      init( Integer.parseInt(s.substring(0,i)), Long.parseLong(s.substring(i+1)), Localisation.GAL );
   }
   
   /** Création à partir des deux valeurs order et npix */
   public Hpix(int order, long npix) { init(order,npix,Localisation.GAL); }
   
   /** Création à partir des deux valeurs order et npix */
   public Hpix(int order, long npix, int frame ) { init(order,npix,frame); }
   
   /** Retourne l'ordre du pixel (=log2(NSIDE)) */
   public int getOrder() { return order; }
   
   /** Retourne le numéro du pixel (en mode NESTED) */
   public long getNpix() { return npix; }
   
   /** Retourne le système de référence des coordonnées  (Localisation.GAL, Localisation.ECLIPTIC, Localisation.ICRS) */
   public int getFrame() { return frame; }
   
   /** Retourne les coordonnées ICRS des 4 coins dans l'ordre Bas -> Gauche -> Droite -> Haut */
   public Coord [] getCorners() { 
      if( !computeCorners ) computeCorners();
      return corners;
   }
   
   /** Trace le losange en aplat */
   public void fill(Graphics g,ViewSimple v) {
      PointD [] b = getProjViewCorners(v);
      if( b==null ) return;
      
      // Taille max d'un segment => sinon sans doute passe derrière le ciel
      double maxSize=getMaxSize(v);
      
      Polygon pol = new Polygon();
      int d=-1;
      for( int i=0; i<b.length; i++ ) {
         int f = ORDRE[i];
         if( b[f]==null ) continue;
         if( d>=0 && HealpixKey.dist(b,d,f)>maxSize*maxSize ) continue;
         pol.addPoint((int)b[f].x,(int)b[f].y);
         d=f;
      }
      g.fillPolygon(pol);
   }
   
   /** Trace les bords du losange, de sommet à sommet */
   public void draw(Graphics g,ViewSimple v) {
      PointD [] b = getProjViewCorners(v);
      if( b==null ) return;
      
      // Taille max d'un segment => sinon sans doute passe derrière le ciel
      double maxSize=getMaxSize(v);
      
      for( int i=0; i<4; i++ ) {
         int d = ORDRE[ i==0 ? 3 : i-1 ];
         int f = ORDRE[i];
         if( b[d]==null || b[f]==null ) continue;
         if( HealpixKey.dist(b,d,f)>maxSize*maxSize ) continue;
         g.drawLine((int)b[d].x,(int)b[d].y, (int)b[f].x,(int)b[f].y);
      }
   }
   
   /** Retourne les coordonnées X,Y des 4 coins du losange dans la projection de
    * la vue ou null si problème */
   public PointD[] getProjViewCorners(ViewSimple v) {
      
      // déjà fait ?
      long vIZ = v.getIZ();
      if( ovIZ==vIZ ) {
         if( nNull>1 ) return null;
         return viewCorners;
      }
      
      Projection proj=v.getProj();
      Coord [] corners = getCorners();
      if( proj==null || corners==null ) return null;
      nNull=0;
      if( viewCorners==null ) viewCorners = new PointD[corners.length];

      // On calcul les xy projection des 4 coins
      for (int i = 0; i<corners.length; i++) {
         Coord c = corners[i];
         proj.getXY(c);
         if( Double.isNaN(c.x)) {
            nNull++;
            if( nNull>1 ) return null;
            else { viewCorners[i]=null; continue; }
         }
         if( viewCorners[i]==null ) viewCorners[i]=new PointD(c.x,c.y);
         else { viewCorners[i].x=c.x; viewCorners[i].y=c.y; }
      }
      
      // On calcule les xy de la vue
      for( int i=0; i<corners.length; i++ ) {
         if( viewCorners[i]!=null ) v.getViewCoordDble(viewCorners[i],viewCorners[i].x, viewCorners[i].y);
      }

      ovIZ=vIZ;
      return viewCorners;
   }
   
   
   /** Retourne true si à coup sûr le losange est hors de la vue courante */
   public boolean isOutView(ViewSimple v) { return isOutView(v,null); }

   /** Retourne true si à coup sûr le losange est hors de la vue courante
    * (les coordonnées XY des coins sont passées en paramètre) */
   public boolean isOutView(ViewSimple v,PointD []b) {
      if( v.isAllSky() ) return false;
      int w = v.getWidth();
      int h = v.getHeight();
      if( b==null ) b = getProjViewCorners(v);
      if( b==null ) return true;
      if( b[0]==null || b[1]==null || b[2]==null || b[3]==null ) return false;

      double minX,maxX,minY,maxY;
      minX=maxX=b[0].x;
      minY=maxY=b[0].y;
      for( int i=1; i<4; i++ ) {
         if( b[i].x<minX ) minX = b[i].x;
         else if( b[i].x>maxX ) maxX = b[i].x;
         if( b[i].y<minY ) minY = b[i].y;
         else if( b[i].y>maxY ) maxY = b[i].y;
      }

      // Tout à droite ou tout à gauche
      if( minX<0 && maxX<0 || minX>=w && maxX>=w ) return true;

      // Au-dessus ou en dessous
      if( minY<0 && maxY<0 || minY>=h && maxY>=h ) return true;

      return false;  // Mais attention, ce n'est pas certain !!
   }
   
   public String toString() { return order+"/"+npix ; }
   
   
   // Initialisation des valeurs
   private void init(int order, long npix,int frame) {
      this.order=order;
      this.npix=npix;
      this.frame=frame;
      computeCorners=false;
   }
   
   // Initialise les coordonnées ICRS des 4 coins
   private void computeCorners() {
      computeCorners=true;
      try {
         long nside = CDSHealpix.pow2(order);
         double [][] x = CDSHealpix.corners(nside,npix);
         corners = new Coord[4];
         for( int i=0; i<x.length; i++ ) corners[i] = new Coord(x[i][0],x[i][1]);
         corners = computeCornersToICRS(corners);
       } catch( Exception e ) {  }
   }
   
   // Convertie les coordonnées des 4 coins données selon le frame en ICRS
   private Coord [] computeCornersToICRS(Coord [] corners) {
      if( frame==Localisation.ICRS ) return corners;
      for( int i=0; i<4; i++ ) corners[i]=Localisation.frameToFrame(corners[i],frame,Localisation.ICRS);
      return corners;
   }
   
   // retourne la taille supposée max pour un segment à tracer
   private double getMaxSize(ViewSimple v) {
      double maxSize=200;
      if( !v.isAllSky() ) {
         double pixRes = CDSHealpix.pixRes(CDSHealpix.pow2(order))/3600;
         double pixelViewSize = v.getPixelSize();
         maxSize = (pixRes/pixelViewSize)*4;
      }
      return maxSize;
   }


}

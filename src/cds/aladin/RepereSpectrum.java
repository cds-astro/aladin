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
import java.awt.Point;

import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

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

   protected void remove() {
      cutOff();
   }

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
   
   public boolean hasPhot() { return true; }
   
   public boolean hasPhot(Plan p) { return p.isCube(); }
   
   private StatPixels statPixels = new StatPixels();
   
   
   /** Retourne une clé unique associé aux statistiques courantes */
//  protected int getStatsHashcode(Plan p, int z) {
//      int k= p.hashCode();
//      k = k*13 + (raj+"").hashCode();
//      k = k*17 + (dej+"").hashCode();
//      if( p.isSync() ) k = k*23;
//      if( z==-1 && p.isCube() ) z = (int)p.getZ();
//      k = k*29 + z;
//      if( p instanceof PlanBG ) k = k*31 + ((PlanBG)p).getOrder();
//      return k;
//   }

   protected int getStatsHashcode(Plan p, int z) { return getPixelStatsCle(p,z).hashCode(); }

   /** Retourne une clé unique associé aux statistiques courantes */
   protected String getPixelStatsCle(Plan p, int z) { 
      if( z==-1 && p.isCube() ) z=(int)p.getZ();
      String sync = p.isSync() ? "sync":"";
      return raj+","+dej+","+p.hashCode()
            + (p.isCube() ? ","+z : "")
            + ","+sync
           + (p instanceof PlanBG ? ((PlanBG)p).getOrder()+"" : "");
   }

   
   /** Retourne la liste des triplets associées aux pixels des statistiques (raj,dej,val)
    * @param p Le plan de base concernée
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @return le tableau des triplets
    * @throws Exception
    */
   public double [] getStatisticsRaDecPix(Plan p, int z) throws Exception {
      if( p.isCube() && z==-1 ) z=(int)p.getZ();
      resumeStatistics(p,z);
      return statPixels.getStatisticsRaDecPix();
   }
   
   /** Retourne les statistiques en fonction du plan passé en paramètre
    * @param p Le plan de base concernée
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @return Nombre, total, sigma, surface, min, max, [median]
    */
   public double [] getStatistics(Plan p, int z) throws Exception {
      if( p.isCube() && z==-1 ) z=(int)p.getZ();
      resumeStatistics(p,z);
      return statPixels.getStatistics();
   }
   
   /** Regénère si nécessaire les statistiques associées à l'objet
    * Dans le cas d'un RepereSpectrum, ne concerne qu'un seul pixel, mais
    * on met à jour tout de même pixelsStat pour rester homogène
    * @param p Le plan de base concernée
    * @param z l'index de la tranche du cube s'il s'agit d'un cube
    * @return true si les stats ont été regénérées, false si inutile
    * @throws Exception
    */
   private boolean resumeStatistics(Plan p, int z) throws Exception {
      
      Projection proj = p.projd;
      if( !p.hasAvailablePixels() ) throw new Exception("getStats error: image without pixel values");
      if( !hasPhot(p) )  throw new Exception("getStats error: not compatible image");
      if( !Projection.isOk(proj) ) throw new Exception("getStats error: image without astrometrical calibration");
      
      // Faut-il re-extraire le pixel concerné par la stat ?
      String cle = getPixelStatsCle(p,z);
      if( !statPixels.reinit( cle ) ) return false;
      
      double pixelSurf;

      // Cas HiPS
      if( p.type==Plan.ALLSKYIMG || p.type==Plan.ALLSKYCUBE ) {

         PlanBG pbg = (PlanBG) p;
         int orderFile = pbg.getOrder();
         long nsideLosange = CDSHealpix.pow2(pbg.getTileOrder());
         int orderPix = pbg.getOrder() + pbg.getTileOrder();
         pixelSurf = CDSHealpix.pixRes(orderPix)/3600;
         pixelSurf *= pixelSurf;
         Coord coo = new Coord(raj,dej);
         coo = Localisation.frameToFrame(coo,Localisation.ICRS,pbg.frameOrigin);
         double[] polar = CDSHealpix.radecToPolar(new double[] {coo.al, coo.del});
         long npix = CDSHealpix.ang2pix_nest( orderPix, polar[0], polar[1]);
         long npixFile = npix/(nsideLosange*nsideLosange);
         double pix = pbg.getHealpixPixel(orderFile,npixFile,npix,z,HealpixKey.SYNC);
         if( Double.isNaN(pix) ) return true;
         pix = pix*pbg.bScale+pbg.bZero;
         polar = CDSHealpix.pix2ang_nest(orderPix, npix);
         polar = CDSHealpix.polarToRadec(polar);
         coo.al = polar[0]; coo.del = polar[1];
         coo = Localisation.frameToFrame(coo,pbg.frameOrigin,Localisation.ICRS);
         statPixels.addPix(coo.al,coo.del, pix);

         // Cas d'une image ou d'un cube "classique"
      } else {
         boolean isCube = p instanceof PlanImageBlink;
         PlanImage pi = (PlanImage)p;

         pixelSurf = proj.getPixResAlpha()*proj.getPixResDelta();
         Coord c = new Coord(raj,dej);
         proj.getXY(c);
         int  xc= (int)(c.x-0.5);
         int  yc= (int)(c.y-0.5);

         try {
            // Cas d'une image "classique"
            if( !isCube ) {
               pi.setLockCacheFree(true);
               pi.pixelsOriginFromCache();

               // Pour un cube
            } else {
               if( z<0 || z>((PlanImageBlink)pi).getDepth() ) throw new Exception("Cube index out of frame range");
            }

            if( !pi.isIn(xc,yc) ) return true;
            double pix= isCube ? ((PlanImageBlink)pi).getPixel(xc, pi.height-yc-1, z) : pi.getPixelInDouble(xc,yc);
            if( Double.isNaN(pix) ) return true;

            c.x=xc+0.5; 
            c.y=yc+0.5;
            proj.getCoord(c);
            statPixels.addPix(c.al,c.del, pix);
         } finally {
            if( !isCube ) pi.setLockCacheFree(false);
         }
      }

      statPixels.setSurface( pixelSurf );
      return true;
   }


   
//   /** Calcule des statistiques en fonction du plan passé en paramètre
//    * @return Nombre, total, sigma, surface, min, max
//    */
//   public double [] getStatistics(Plan p, int z) throws Exception {
//
//      Projection proj = p.projd;
//      if( !p.hasAvailablePixels() ) throw new Exception("getStats error: image without pixel values");
//      if( !hasPhot(p) )  throw new Exception("getStats error: not compatible image");
//      if( !Projection.isOk(proj) ) throw new Exception("getStats error: image without astrometrical calibration");
//
//      double pix = Double.NaN;
//      double pixelSurf;
//
//      // Cas d'une map HEALPix
//      if( p.type==Plan.ALLSKYIMG || p.type==Plan.ALLSKYCUBE ) {
//         throw new Exception("Not yet supported for HiPS cubes");
//
//         // Cas d'un cube "classique"
//      } else {
//         PlanImageBlink pi = (PlanImageBlink)p;
//         pixelSurf = proj.getPixResAlpha()*proj.getPixResDelta();
//         Coord c = new Coord(raj,dej);
//         proj.getXY(c);
//         int  xc= (int)c.x;
//         int  yc= (int)(pi.height-c.y);
//
//         if( z<0 || z>pi.getDepth() ) throw new Exception("Cube index out of frame range");
//         if( pi.isIn(xc,yc) ) pix= pi.getPixel(xc, yc, z);
//      }
//
//      return new double[]{ 1, pix, 0, pixelSurf, pix, pix };
//   }



}

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

import java.awt.image.IndexColorModel;

import cds.allsky.BuilderRgb;
import cds.allsky.TabRgb;
import cds.fits.Fits;

/**
 * Gère un losange Healpix pour un PlanBGRgb (HiPS RGB dynamique
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Janvier 2020
 */
public class HealpixKeyRgb extends HealpixKey {

   protected HealpixKey cmp[]; // Les 3 composantes couleurs (R,G,B)
   private int missing;
   
   protected HealpixKeyRgb(PlanBGRgb planBG,int order, long npix) {
      super(planBG,order,npix,NOLOAD);
      fromNet = false;
      missing = planBG.tabRgb.getMissing();
      
      cmp = new HealpixKey[3];
      for( int c=0; c<3; c++ ) {
         if( c==missing ) continue;
         PlanBG p = c==0 ? planBG.red : c==1 ? planBG.green : planBG.blue;
         cmp[c] = p.getHealpix(order, npix,true);
      }
   }
   
   protected HealpixKeyRgb(HealpixKeyRgb father,int child) {
      super(father,child);
   }

   // Toutes les composantes doivent être READY pour être READY
   protected int getStatus() { return READY; }

   protected boolean shouldBeCached() { return false; }
   
   /** Chargement synchrone */
   protected void loadNow() throws Exception {
      Aladin.makeCursor(Aladin.aladin, Aladin.WAITCURSOR);
      if( anc!=null ) { anc.loadNow(); return; }
      for( int c=0; c<3; c++ ) {
         if( c==missing ) continue;
         cmp[c].loadNow();
         
         // On en profite pour mettre à jour la taille de la tuile si pas encore connue
         if( width==0 ) { height = width = cmp[c].width; }
      }
      Aladin.makeCursor(Aladin.aladin, Aladin.DEFAULTCURSOR);
   }
   
   protected int [] getPixelFromAncetreRGB() throws Exception {
      if( anc.rgb==null ) anc.rgb = anc.getPixelRgb();
      return super.getPixelFromAncetreRGB();
   }
   
   /** Génération des pixels RGB si nécessaire */
   protected int [] getPixelRgb() {
      if( rgb==null ) createRGB();
      return rgb; 
   }
   
   /** Génération des pixels RGB à partir des pixels d'origine des 3 composantes */
   protected void createRGB() {
      Fits [] fits       = new Fits[3];   
      double [] pixelMin = new double[3];
      double [] pixelMax = new double[3];
      byte [][] tcm      = new byte[3][];
      double luptonQ=20;
      double [] luptonM  = new double[3];
      double [] luptonS  = new double[3];
      
      TabRgb t = ((PlanBGRgb)planBG).tabRgb;
      int mode = t.getRGBMethod();
      int format = t.getFormat();
//      boolean flagGauss = t.getGauss() && order==planBG.maxOrder;
      boolean flagGauss = false;

      try {
         for( int c=0; c<3; c++ ) {
            if( c==missing ) continue;
            
            // Regénération des tuiles fits des composantes
            HealpixKey a = cmp[c];
            if( a==null ) throw new Exception("Composante "+c+" pour "+order+"/"+npix+" est nulle !!"); 
            
            fits[c] = new Fits(a.width,a.width,a.planBG.bitpix);
            fits[c].bscale = a.planBG.bScale;
            fits[c].bzero = a.planBG.bZero;
            fits[c].blank = a.planBG.blank;
            if( a.pixelsOrigin==null ) a.loadPixelsOrigin(HealpixKey.ONLYIFDISKAVAIL); //NOW);
            if( a.pixelsOrigin==null ) throw new Exception("Composante "+c+" pour "+order+"/"+npix+" n'a pas (encore) les pixelsOriginaux !!");
            fits[c].pixels = a.pixelsOrigin;

            // Récupération des paramètres pour RGB classique
            if( mode==0 ) {
               pixelMin[c] = t.getCutMin(c);
               pixelMax[c] = t.getCutMax(c);
               IndexColorModel cm=null;
               
               // Si un des paramètres est manuel, il faut générer manuellement la colormap associée
               if( t.hasManual() ) {
                  double pixelMiddle = t.getCutMiddle(c);
                  int indexMiddle = (int)( 256 * (pixelMiddle - pixelMin[c]) / (pixelMax[c] - pixelMin[c]) );
                  int transfertFct = PlanImage.getTransfertFct( t.getTransfertFct(c));
                  cm = CanvasColorMap.getCM(0, indexMiddle, 255, false, PlanImage.CMGRAY, transfertFct, true );
                  
               // Sinon on peut directement récupérer celle du plan
               } else cm = (IndexColorModel)a.planBG.getCM();
               tcm[c] = cds.tools.Util.getTableCM(cm, 2);

            // Récupération des paramètres pour méthode Lupton
            } else {
               luptonQ    = t.getLuptonQ();
               luptonM[c] = t.getLuptonM(c);
               luptonS[c] = t.getLuptonS(c);
            }
         }

         // Calcul de la tuile RGB
         Fits out=null;
         if( mode==0 ) out = BuilderRgb.createLeaveRGBClassic(fits,width,format,flagGauss,missing,pixelMin,pixelMax,tcm);
         else out = BuilderRgb.createLeaveRGBLupton(fits, width, format, flagGauss,missing, luptonQ, luptonM, luptonS);
         
         // On mémorise le résultat
         rgb = out.rgb;

         // Inversion du sens des lignes (FITS -> java)
         int [] line = new int[width];
         for( int y=0; y<height/2; y++ ) {
            System.arraycopy(rgb, y*width, line, 0, width);
            System.arraycopy(rgb, (height-y-1)*width, rgb, y*width, width);
            System.arraycopy(line,0, rgb, (height-y-1)*width, width);
         }
         
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

}

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

import java.io.File;
import cds.tools.Util;
import cds.tools.pixtools.Hpix;


/**
  * G�re Tous les losanges Healpix de niveau le plus haut
  * @author Pierre Fernique [CDS]
  */
class HealpixAllsky extends HealpixKey {
   
   protected int nbPix;       // Nombre de losanges
   protected HealpixKey [] pixList;  // Liste des losanges
   private int mem=0;
   private int order;

   protected HealpixAllsky(PlanBG planBG,int order) {
      this.planBG = planBG;
      this.order=order;
      this.npix=-1;
      allSky=true;
      resetTimer();
      String nameNet = "Norder"+order+"/Allsky";
      String nameCache = planBG.survey+planBG.version+"/"+"Norder"+order+"/Allsky";
      if( planBG.truePixels ) extCache=extNet=FITS;
      else /* if( planBG.color ) */ extCache=extNet=JPEG;
      fileCache = nameCache+ EXT[extCache];
      fileNet = nameNet+ EXT[extNet];
      alreadyCached=false;
      priority=-1;
      
      nbPix = 12*(int)Math.pow(4,order);
      pixList= null;
//System.out.println("Cr�ation d'un Allsky pour "+nbPix+" losanges");      
      setStatus(ASKING);
   }
   
   HealpixKey createOneKey(int npix,int width,byte [] pix) {
      HealpixKey h = new HealpixKey();
      h.planBG=planBG;
      h.order=order;
      h.npix=npix;
      h.hpix = new Hpix(order,npix,planBG.frameOrigin);
//      h.corners = h.computeCorners();
      h.resetTimer();
      h.width=h.height=width;
      h.pixels=pix;
      h.alreadyCached=true;
      h.allSky=true;
      h.setStatus(READY);
      return h;
   }
   
   HealpixKey createOneKeyRGB(int npix,int width,int []rgb) {
      HealpixKey h = new HealpixKey();
      h.planBG=planBG;
      h.order=order;
      h.npix=npix;
//      h.corners = h.computeCorners();
      h.hpix = new Hpix(order,npix,planBG.frameOrigin);
      h.resetTimer();
      h.width=h.height=width;
      h.rgb=rgb;
      h.alreadyCached=true;
      h.allSky=true;
      h.setStatus(READY);
      return h;
   }
   
   /** M�thode statique pour v�rifier qu'un Allsky est pr�sent en cache */
   static boolean isCached(PlanBG planBG,int order) {
      String pathName = planBG.getCacheDir();
      if( pathName==null ) return false;
      String name = planBG.survey+planBG.version+"/"+"Norder"+order+"/Allsky"+ (planBG.truePixels ? ".fits" : planBG.color ? ".jpg" : ".fits");
      pathName = pathName+Util.FS+name;
      File f= new File(pathName);
      return f.exists() && f.canRead();
   }
   
   /** Cr�ation de tous les losanges individuels � partir d'un HealpixAllSky */
   protected void createPixList() {
      if( getStatus()!=READY ) return;
      
      try {
         if( planBG.color ) { createPixListRGB1(); return; }
      } catch( Exception e ) { e.printStackTrace(); return; }
      
      int nbLosangeWidth = (int)Math.sqrt(nbPix);
      int nbLosangeHeight = nbPix/nbLosangeWidth;
      if( nbPix/nbLosangeWidth!=(double)nbPix/nbLosangeWidth ) nbLosangeHeight++;
      int w = width/nbLosangeWidth;
//System.out.println("G�n�ration de la liste des "+nbLosangeWidth+"x"+nbLosangeHeight+" losanges "+w+"x"+w+" pour AllSky "+"...");      
      
      pixList = new HealpixKey[nbPix];
      for( int i=0; i<nbPix; i++ ) {
         byte [] pix = new byte[w*w];
         int yLosange=(i/nbLosangeWidth)*w;
         int xLosange=(i%nbLosangeWidth)*w;
         if( i%100==0 ) Util.pause(10);
         for( int y=0; y<w; y++ ) {
            for( int x=0; x<w; x++) {
               int offset = (yLosange+y)*width + (xLosange+x);
               if( offset>=pixels.length ) {
                  System.err.println("offset="+offset+" pixels.length="+pixels.length+" x,y="+x+","+y);
                  offset=pixels.length-1;
               }
               int pos = y*w +x;
               pix[pos] = pixels[offset];
            }
         }
         pixList[i] = createOneKey(i,w,pix);
         mem+=pixList[i].getMem();
      }
      
      // Plus besoin des pixels du AllSky
      pixels=null;
   }
   
   /** Retourne la taille approximative du losange en bytes */
   protected int getMem() { return mem; }


   
   /** Cr�ation de tous les losanges individuels � partir d'un HealpixAllSky
    * dans le cas de la couleur RGB. Dans ce cas, il est n�cessaire de g�n�rer
    * directement "img" de chaque HealpixKey plut�t que de passer par pixels[] en bytes
    * RQ : ne pas appeler directement, utiliser la m�thode g�n�rique createPixList()
    */
   private void createPixListRGB1() throws Exception {
      
      int nbLosangeWidth = (int)Math.sqrt(nbPix);
      int nbLosangeHeight = nbPix/nbLosangeWidth;
      if( nbPix/nbLosangeWidth!=(double)nbPix/nbLosangeWidth ) nbLosangeHeight++;
      int w = width/nbLosangeWidth;
//System.out.println("G�n�ration de la liste des "+nbLosangeWidth+"x"+nbLosangeHeight+" losanges "+w+"x"+w+" pour AllSky EN COULEUR...");      
      pixList = new HealpixKey[nbPix];
      for( int npix=0; npix<nbPix; npix++ ) {
         int [] pix = new int[w*w];
         int yLosange=(npix/nbLosangeWidth)*w;
         int xLosange=(npix%nbLosangeWidth)*w;
         if( npix%100==0 ) Util.pause(10);
         for( int y=0; y<w; y++ ) {
            for( int x=0; x<w; x++) {
               pix[y*w +x] = rgb[ (yLosange+y)*width + (xLosange+x) ];
            }
         }
         pixList[npix] = createOneKeyRGB(npix,w,pix);
         mem+=pixList[npix].getMem();
      }
      
      rgb=null;
   }
   
   protected int free() {
      int n=0;
      if( pixList!=null ) {
         n=pixList.length;
         for( int j=0; j<pixList.length; j++ ) if( pixList[j]!=null ) pixList[j].free();
      }
      pixList=null;
      mem=0;
      return n;
   }
   
   protected void clearBuf() {
      if( pixList==null ) return;
      for( int j=0; j<pixList.length; j++ ) if( pixList[j]!=null ) pixList[j].clearBuf();
   }
   
   HealpixKey [] getPixList() {
      if( pixList==null ) createPixList();
      return pixList;
   }
}
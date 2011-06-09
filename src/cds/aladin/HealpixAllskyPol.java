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

import cds.tools.Util;
import cds.tools.pixtools.Hpix;

class HealpixAllskyPol extends HealpixKeyPol {
   
   protected int nbPix;       // Nombre de losanges
   protected HealpixKey [] pixList;  // Liste des losanges
   protected int order;

   protected HealpixAllskyPol(PlanBG planBG,int order) {
      this.planBG = planBG;
      this.order=order;
      this.npix=-1;
      allSky=true;
      resetTimer();
      String nameNet = "Norder"+order+"/Allsky";
      String nameCache = planBG.survey+planBG.version+"/"+"Norder"+order+"/Allsky";
      if( planBG.color ) extCache=extNet=JPEG;
      if( planBG.truePixels ) extCache=extNet=FITS;
      fileCache = nameCache+ EXT[extCache];
      fileNet = nameNet+ EXT[extNet];
      alreadyCached=false;
      priority=-1;
      
      nbPix = 12*(int)Math.pow(4,order);
      pixList= null;
      setStatus(ASKING);
   }
   
   HealpixKeyPol createOneKey(int npix,int width,byte [] pix1, byte [] pix2) {
      HealpixKeyPol h = new HealpixKeyPol();
      h.planBG=planBG;
      h.order=order;
      h.npix=npix;
      h.hpix = new Hpix(order,npix,planBG.frameOrigin);
//      h.corners = h.computeCorners();
      h.resetTimer();
      h.width=h.height=width;
      h.pixels=null;
      h.bitpix=bitpix;
      h.pixelsOrigin=pix1;
      h.bitpix2=bitpix2;
      h.pixelsOrigin2=pix2;
      h.alreadyCached=true;
      h.allSky=true;
      h.setStatus(READY);
      return h;
   }
   
   public int getOrder1() { return order; }
   public long getNpix() { return -1; }
   
   /** Création de tous les losanges individuels à partir d'un HealpixAllSky */
   protected void createPixList() {
      if( getStatus()!=READY ) return;
      
      int nbLosangeWidth = (int)Math.sqrt(nbPix);
      int nbLosangeHeight = nbPix/nbLosangeWidth;
      if( nbPix/nbLosangeWidth!=(double)nbPix/nbLosangeWidth ) nbLosangeHeight++;
      int w = width/nbLosangeWidth;
System.out.println("Génération de la liste des "+nbLosangeWidth+"x"+nbLosangeHeight+" losanges "+w+"x"+w+" pour AllSky "+(pixelsOrigin2!=null?"polarisation":"sans polarisation")+"...");      
      
      pixList = new HealpixKey[nbPix];
      for( int i=0; i<nbPix; i++ ) {
         byte [] pix1 = null;
         byte [] pix2 = null;
         int npix1=0,npix2=0;
         if( bitpix!=0 )  { npix1=Math.abs(bitpix)/8;  pix1 = new byte[w*w*npix1];  }
         if( bitpix2!=0 ) { npix2=Math.abs(bitpix2)/8; pix2 = new byte[w*w*npix2]; }
         int yLosange=(i/nbLosangeWidth)*w;
         int xLosange=(i%nbLosangeWidth)*w;
         if( i%100==0 ) Util.pause(10);
         for( int y=0; y<w; y++ ) {
            for( int x=0; x<w; x++) {
               int offset = (yLosange+y)*width + (xLosange+x);
               int pos = y*w +x;
               if( bitpix!=0 )  System.arraycopy(pixelsOrigin,  offset*npix1, pix1, pos*npix1, npix1);
               if( bitpix2!=0 ) System.arraycopy(pixelsOrigin2, offset*npix2, pix2, pos*npix2, npix2);
            }
         }
         pixList[i] = createOneKey(i,w,pix1,pix2);
      }
      
      // Plus besoin des pixels du AllSky
      pixels=null;
      pixelsOrigin = pixelsOrigin2 = null;
   }
   
   protected int free() {
      int n=0;
      if( pixList!=null ) {
         n=pixList.length;
         for( int j=0; j<pixList.length; j++ ) pixList[j].free();
      }
      pixList=null;
      return n;
   }
   
   protected void clearBuf() {
      if( pixList==null ) return;
      for( int j=0; j<pixList.length; j++ ) pixList[j].clearBuf();
   }
   
   HealpixKey [] getPixList() {
      if( pixList==null ) createPixList();
      return pixList;
   }

}
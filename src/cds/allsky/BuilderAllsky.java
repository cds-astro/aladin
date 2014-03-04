// Copyright 2012 - UDS/CNRS
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

package cds.allsky;

import java.io.File;

import cds.aladin.Aladin;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;


/**
 * Création d'un fichier Allsky.fits ou Allsky.jpg à partir des losanges individuels
 * @author Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 */
final public class BuilderAllsky  extends Builder {
   
   public static final String FS = System.getProperty("file.separator");
   
   public BuilderAllsky(Context context) { super(context); }
   
   public Action getAction() { return Action.ALLSKY; }
   
   public void validateContext() throws Exception {
      validateOutput();
      context.setProgressMax(100);
   }
   
   public void run() throws Exception { 
      if( !context.isColor() ) createAllSky(context.getOutputPath(),3, 64);
      if( !context.isColor() ) validateCut();
      createAllSkyColor(context.getOutputPath(),3,"png",64);
      createAllSkyColor(context.getOutputPath(),3,"jpeg",64);
      context.writePropertiesFile();
   }
   
   /** Création des fichiers Allsky.fits (true bitpix) et Allsky.jpg (8 bits) pour tout un niveau Healpix
    * Rq : seule la méthode FIRST est supportée
    * @param order order Healpix
    * @param outLosangeWidth largeur des losanges pour le Allsky (typiquement 64 ou 128 pixels)
    */
   public void createAllSky(String path, int order,int outLosangeWidth) throws Exception {
      long t=System.currentTimeMillis();
      int nside = (int)CDSHealpix.pow2(order);
      int n = 12*nside*nside;
      int nbOutLosangeWidth = (int)Math.sqrt(n);
      int nbOutLosangeHeight = (int)((double)n/nbOutLosangeWidth);
      if( (double)n/nbOutLosangeWidth!=nbOutLosangeHeight ) nbOutLosangeHeight++;
      int outFileWidth = outLosangeWidth * nbOutLosangeWidth;
      
      boolean findParam=false;
      double bzero=0,bscale=1,blank=Double.NaN;
      
//      Aladin.trace(3,"Création Allsky order="+order+" mode=FIRST "
//      +": "+n+" losanges ("+nbOutLosangeWidth+"x"+nbOutLosangeHeight
//      +" de "+outLosangeWidth+"x"+outLosangeWidth+" soit "+outFileWidth+"x"+nbOutLosangeHeight*outLosangeWidth+" pixels)...");
      Fits out = null;
      
//      double blank = context.getBlank();
      
      for( int npix=0; npix<n; npix++ ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         if( context.getAction()==getAction() ) context.setProgress(npix*100./n);
         String name = Util.getFilePath("", order, npix);
         Fits in = new Fits();
         String filename = path+FS+name;
         try {
            if( !(new File(filename+".fits")).exists() ) continue;
            in.loadFITS(filename+".fits");
            if( !findParam ) { bzero=in.getBzero(); bscale=in.getBscale(); blank=in.getBlank(); findParam=true; }
            if( out==null ) {
               if( in.width!=0 && in.width<outLosangeWidth ) {
                  context.info("createAllsky: reducing width=>"+in.width+" ...");
                  createAllSky(path,order,in.width);
                  return;
               }
               out = new Fits(outFileWidth,nbOutLosangeHeight*outLosangeWidth,in.bitpix);
               
               // initilialise toutes les valeurs à Blank
               if( blank!=0 ) {
                  for( int y=0; y<out.height; y++ ) {
                     for( int x=0; x<out.width; x++ ) {
                        out.setPixelDouble(x, out.height-1-y, blank);
                     }
                  }
               }
            }
                    
            int yLosange=npix/nbOutLosangeWidth;
            int xLosange=npix%nbOutLosangeWidth;
            int gap = in.width/outLosangeWidth;
            for( int y=0; y<in.width/gap; y++ ) {
               for( int x=0; x<in.width/gap; x++ ) {
                  double p = in.getPixelDouble(x*gap,in.height-1-y*gap);

                  int xOut= xLosange*outLosangeWidth + x;
                  int yOut = yLosange*outLosangeWidth +y;
                  out.setPixelDouble(xOut, out.height-1-yOut, p);
               }
            }
         } catch( Exception e ) { }
      }
      
      // Détermination des pixCutmin..pixCutmax et min..max directement dans le fichier AllSky
      if( out==null ) {
//         context.warning("createAllsky: no FITS tiles found !");
         return;
//         throw new Exception("createAllSky error: null output file !");
      }
//      double cut [] = context.getCut();
//      double bzero  = context.getBZero();
//      double bscale = context.getBScale();
      
//      int bitpix = out.bitpix;
      
      out.setBlank(blank);
      out.setBzero(bzero);
      out.setBscale(bscale);
      
//      if( cut==null ) {
//         cut = out.findAutocutRange(0, 0, true);
//      }
//      
//      if( cut[0]<cut[1] ) {
//         out.headerFits.setKeyValue("PIXELMIN", (bitpix>0 ? (int)cut[0] : cut[0])+"");
//         out.headerFits.setKeyValue("PIXELMAX", (bitpix>0 ? (int)cut[1] : cut[1])+"");
//         
//         if( !(cut[2]<cut[3] && cut[2]<=cut[0] && cut[3]>=cut[1]) ) {
//            cut[2] = bitpix==-64?-Double.MAX_VALUE : bitpix==-32? -Float.MAX_VALUE
//                  : bitpix==64?Long.MIN_VALUE+1 : bitpix==32?Integer.MIN_VALUE+1 : bitpix==16?Short.MIN_VALUE+1:1;
//            cut[3] = bitpix==-64?Double.MAX_VALUE : bitpix==-32? Float.MAX_VALUE
//                  : bitpix==64?Long.MAX_VALUE : bitpix==32?Integer.MAX_VALUE : bitpix==16?Short.MAX_VALUE:255;
//            Aladin.trace(1,"BuilderAllsky.createAllSky() data range [DATAMMIN..DATAMAX] not consistante => max possible range");
//         }
//         out.headerFits.setKeyValue("DATAMIN",  (bitpix>0 ? (int)cut[2] : cut[2])+"");
//         out.headerFits.setKeyValue("DATAMAX",  (bitpix>0 ? (int)cut[3] : cut[3])+"");
//
//      } else {
//         Aladin.trace(1,"BuilderAllsky.createAllSky() pixel cut range [PIXELMIN..PIXELMAX] not consistante => ignored");
//      }


   // Ecriture du FITS (true bits)
      String filename = getFileName(path, order);
      out.writeFITS(filename+".fits");
      
      Aladin.trace(2,"BuilderAllsky.createAllSky()... bitpix="+out.bitpix+" bzero="+out.bzero+" bscale="+out.bscale
            /*+" pixelRange=["+cut[0]+".."+cut[1]+"] dataRange=["+cut[2]+".."+cut[3]*/+"] created in "+ (int)((System.currentTimeMillis()-t)/1000)+"s");
   }

   static public String getFileName(String path, int order) {
	   return path+FS+"Norder"+order+FS+"Allsky";
   }
   
   /** Création d'un AllSky JPEG couleur à partir des images JPEG ou PNG à l'ordre indiqué
    * Rq : seule la méthode FIRST est supportée
    * @param order order Healpix
    * @param mode jpeg ou png
    * @param outLosangeWidth largeur des losanges pour le Allsky (typiquement 64 ou 128 pixels)
    */
   public void createAllSkyColor(String path, int order,String mode,int outLosangeWidth) throws Exception {
      long t=System.currentTimeMillis();
      int nside = (int)CDSHealpix.pow2(order);
      int n = 12*nside*nside;
      int nbOutLosangeWidth = (int)Math.sqrt(n);
      int nbOutLosangeHeight = (int)((double)n/nbOutLosangeWidth);
      if( (double)n/nbOutLosangeWidth!=nbOutLosangeHeight ) nbOutLosangeHeight++;
      int outFileWidth = outLosangeWidth * nbOutLosangeWidth;
      int outFileHeight = nbOutLosangeHeight*outLosangeWidth;
      boolean first = true;
      String ext = mode=="png" ? ".png" : ".jpg";
     
      Aladin.trace(3,"Creation Allsky order="+order+" mode=FIRST color"
      +": "+n+" losanges ("+nbOutLosangeWidth+"x"+nbOutLosangeHeight
      +" de "+outLosangeWidth+"x"+outLosangeWidth+" soit "+outFileWidth+"x"+nbOutLosangeHeight*outLosangeWidth+" pixels)...");

      Fits out = new Fits(outFileWidth,outFileHeight, 0);
      
      for( int npix=0; npix<n; npix++ ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         if( context.getAction()==getAction() ) context.setProgress(npix*100./n);
         String name = Util.getFilePath(order, npix);
         Fits in = new Fits();
         String filename = path+FS+name;
         try {
            if( !(new File(filename+ext)).exists() ) continue;
            in.loadJpeg(filename+ext,true,false);
            if( first ) {
               if( in.width!=0 && in.width<outLosangeWidth ) {
                  context.info("createAllsky: reducing width=>"+in.width+" ...");
                  createAllSkyColor(path,order,ext,in.width);
                  return;
               }
            }
            first = false;
            int yLosange=npix/nbOutLosangeWidth;
            int xLosange=npix%nbOutLosangeWidth;
            int gap = in.width/outLosangeWidth;
            for( int y=0; y<in.width/gap; y++ ) {
               for( int x=0; x<in.width/gap; x++ ) {
                  int p=in.getPixelRGB(x*gap,in.width-y*gap-1);
                  int xOut = xLosange*outLosangeWidth + x;
                  int yOut = yLosange*outLosangeWidth + y;
                  out.setPixelRGB(xOut, outFileHeight-yOut-1, p);
               }
            }
         }
         catch( Exception e ) { }
      }
      
      if( first ) {
//         context.warning("createAllSkyColor : no "+ext+" tiles found!");
         return;
      }

      String filename = getFileName(path, order);
      out.writeCompressed(filename+ext,0,0,null,mode);
      
      context.trace(4,"SkyGenerator.createAllSkyColor()... "+ (int)((System.currentTimeMillis()-t)/1000)+"s");
   }
   
}

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
   
   private boolean abort=false;
   
   public static final String FS = System.getProperty("file.separator");
   
   public BuilderAllsky(Context context) { super(context); }
   
   public Action getAction() { return Action.ALLSKY; }
   
   public void validateContext() throws Exception {
      validateOutput();
      context.setProgressMax(100);
   }
   
   protected void abort() { abort=true; }
   
   public void run() throws Exception { 
//      if( !context.isColor() ) validateCut();
      abort=false;
      validateDepth();
      for( int z=0; !abort && z<context.depth; z++ ) {
         if( !context.isColor() ) createAllSky(context.getOutputPath(),3, 64, z);
         try {
            if( z==0 && !context.isColor() ) validateCut();
            createAllSkyColor(context.getOutputPath(),3,"png",64, z);
            createAllSkyColor(context.getOutputPath(),3,"jpeg",64, z);
         } catch( Exception e ) { }
      }

      postJob();
   }
   
   public void runJpegOrPngOnly(String format) throws Exception { 
    validateDepth();
    validateCut();
    for( int z=0; z<context.depth; z++ ) {
       createAllSkyColor(context.getOutputPath(),3,format,64, z);
    }

    postJob();
 }
   
   private void postJob() throws Exception {
//      validateFrame();
      validateLabel();
      validateBitpix();
      context.writePropertiesFile();
   }

   /** Création des fichiers Allsky.fits (true bitpix) et Allsky.jpg (8 bits) pour tout un niveau Healpix
    * Rq : seule la méthode FIRST est supportée
    * @param order order Healpix
    * @param outLosangeWidth largeur des losanges pour le Allsky (typiquement 64 ou 128 pixels)
    */
   public void createAllSky(String path, int order,int outLosangeWidth, int z) throws Exception {
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
         if( abort || context.isTaskAborting() ) throw new Exception("Task abort !");
         if( context.getAction()==getAction() ) context.setProgress(npix*100./n);
         String name = Util.getFilePath("", order, npix,z);
         Fits in = new Fits();
         String filename = path+FS+name;
         try {
            if( !(new File(filename+".fits")).exists() ) continue;
            in.loadFITS(filename+".fits");
            if( !findParam ) { bzero=in.getBzero(); bscale=in.getBscale(); blank=in.getBlank(); findParam=true; }
            if( out==null ) {
               if( in.width!=0 && in.width<outLosangeWidth ) {
                  context.info("createAllsky: reducing width=>"+in.width+" ...");
                  createAllSky(path,order,in.width,z);
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
      }
      
      out.setBlank(blank);
      out.setBzero(bzero);
      out.setBscale(bscale);
      

   // Ecriture du FITS (true bits)
      String filename = getFileName(path, order,z);
      out.writeFITS(filename+".fits",true);
      
      // Dans le cas d'un cube, il est possible que le Allsky.fits n'ait pas été créé (vide),
      // on va alors dupliquer le premier Allsky_nnn.fits en Allsky.fits pour s'en sortir
      if( z>1 ) {
         String f = getFileName(path,order,0);
         if( !(new File(f+".fits")).isFile() ) out.writeFITS(f+".fits",true);
      }
      
      Aladin.trace(2,"BuilderAllsky.createAllSky()... bitpix="+out.bitpix+" bzero="+out.bzero+" bscale="+out.bscale
            +"] created in "+ (int)((System.currentTimeMillis()-t)/1000)+"s");
   }

   static public String getFileName(String path, int order, int z) {
	   return path+FS+"Norder"+order+FS+"Allsky"+(z>0?"_"+z : "");
   }
   
   /** Création d'un AllSky JPEG couleur à partir des images JPEG ou PNG à l'ordre indiqué
    * Rq : seule la méthode FIRST est supportée
    * @param order order Healpix
    * @param mode jpeg ou png
    * @param outLosangeWidth largeur des losanges pour le Allsky (typiquement 64 ou 128 pixels)
    */
   public void createAllSkyColor(String path, int order,String mode,int outLosangeWidth,int z) throws Exception {
      long t=System.currentTimeMillis();
      int nside = (int)CDSHealpix.pow2(order);
      int n = 12*nside*nside;
      int nbOutLosangeWidth = (int)Math.sqrt(n);
      int nbOutLosangeHeight = (int)((double)n/nbOutLosangeWidth);
      if( (double)n/nbOutLosangeWidth!=nbOutLosangeHeight ) nbOutLosangeHeight++;
      int outFileWidth = outLosangeWidth * nbOutLosangeWidth;
      int outFileHeight = nbOutLosangeHeight*outLosangeWidth;
      boolean first = true;
      String ext = mode.equals("png") ? ".png" : ".jpg";
      int format =  mode.equals("png") ? Fits.PREVIEW_PNG : Fits.PREVIEW_JPEG;
     
      Aladin.trace(3,"Creation Allsky"+ext+" order="+order+(z>0?"_"+z:"")+" mode=FIRST color"
      +": "+n+" losanges ("+nbOutLosangeWidth+"x"+nbOutLosangeHeight
      +" de "+outLosangeWidth+"x"+outLosangeWidth+" soit "+outFileWidth+"x"+nbOutLosangeHeight*outLosangeWidth+" pixels)...");

      Fits out = new Fits(outFileWidth,outFileHeight, 0);
      
      for( int npix=0; npix<n; npix++ ) {
         if( abort || context.isTaskAborting() ) throw new Exception("Task abort !");
         if( context.getAction()==getAction() ) context.setProgress(npix*100./n);
         String name = Util.getFilePath(order, npix, z);
         Fits in = new Fits();
         String filename = path+FS+name;
         try {
            if( !(new File(filename+ext)).exists() ) continue;
            in.loadPreview(filename+ext,true,false,format);
            if( first ) {
               if( in.width!=0 && in.width<outLosangeWidth ) {
                  Aladin.trace(3,"restart createAllsky: reducing width=>"+in.width+" ...");
                  createAllSkyColor(path,order,mode,in.width, z);
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

      String filename = getFileName(path, order, z);
      out.writeCompressed(filename+ext,0,0,null,mode);
      
      // Dans le cas d'un cube, il est possible que le Allsky.ext n'ait pas été créé (vide),
      // on va alors dupliquer le premier Allsky_nnn.ext en Allsky.ext pour s'en sortir
      if( z>1 ) {
         String f = getFileName(path,order,0);
         if( !(new File(f+ext)).isFile() ) out.writeCompressed(f+ext,0,0,null,mode);
      }
      

      
      context.trace(4,"SkyGenerator.createAllSkyColor()... "+ (int)((System.currentTimeMillis()-t)/1000)+"s");
   }
   
}

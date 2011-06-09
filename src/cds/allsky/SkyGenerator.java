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

package cds.allsky;

import cds.aladin.Aladin;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.HpixTree;
import cds.tools.pixtools.Util;


/**
 * Cr�ation d'un fichier Allsky.fits ou Allsky.jpg � partir des losanges individuels
 */
final public class SkyGenerator {
   
   private static final String FS = System.getProperty("file.separator");
   private double progress = 0;
   
   public SkyGenerator() {}
   
    /** Cr�ation des fichiers Allsky.fits (true bitpix) et Allsky.jpg (8 bits) pour tout un niveau Healpix
    * Rq : seule la m�thode FIRST est support�e
    * @param path Emplacement du survey
    * @param order order Healpix
    * @param outLosangeWidth largeur des losanges pour le Allsky (typiquement 64 ou 128 pixels)
    */
   public void createAllSky(String path,int order,int outLosangeWidth,double pixelMin,double pixelMax, boolean keepBB) throws Exception {
      long t=System.currentTimeMillis();
      int nside = (int)CDSHealpix.pow2(order);
      int n = 12*nside*nside;
      int nbOutLosangeWidth = (int)Math.sqrt(n);
      int nbOutLosangeHeight = (int)((double)n/nbOutLosangeWidth);
      if( (double)n/nbOutLosangeWidth!=nbOutLosangeHeight ) nbOutLosangeHeight++;
      int outFileWidth = outLosangeWidth * nbOutLosangeWidth;
      
//      Aladin.trace(3,"Cr�ation Allsky order="+order+" mode=FIRST "
//      +": "+n+" losanges ("+nbOutLosangeWidth+"x"+nbOutLosangeHeight
//      +" de "+outLosangeWidth+"x"+outLosangeWidth+" soit "+outFileWidth+"x"+nbOutLosangeHeight*outLosangeWidth+" pixels)...");
      Fits out = null;
      
      for( int npix=0; npix<n; npix++ ) {
         progress = npix*100./n;
         String name = Util.getFilePath("", order, npix);
         Fits in = new Fits();
         String filename = path+FS+name;
         try {
            in.loadFITS(filename+".fits");
            if( out==null ) {
               out = new Fits(outFileWidth,nbOutLosangeHeight*outLosangeWidth,in.bitpix);
               if( in.hasBlank() ) out.setBlank( in.getBlank() );
               out.setBscale( in.getBscale() );
               out.setBzero( in.getBzero() );
               // initilialise toutes les valeurs � Blank
               for( int y=0; y<out.height; y++ ) {
            	   for( int x=0; x<out.width; x++ ) {
            		   out.setPixelDouble(x, out.height-1-y, out.getBlank());
            	   }
               }
            }
                    
            int yLosange=npix/nbOutLosangeWidth;
            int xLosange=npix%nbOutLosangeWidth;
            int gap = in.width/outLosangeWidth;
            for( int y=0; y<in.width/gap; y++ ) {
               for( int x=0; x<in.width/gap; x++ ) {
                  double p;
                  if (keepBB) p = in.getPixelFull(x*gap,in.height-1-y*gap);
                  else p=in.getPixelDouble(x*gap,in.height-1-y*gap);

                  int xOut= xLosange*outLosangeWidth + x;
                  int yOut = yLosange*outLosangeWidth +y;
                  out.setPixelDouble(xOut, out.height-1-yOut, p);
               }
            }
         } catch( Exception e ) { }
      }
      
      // D�termination des pixCutmin..pixCutmax et min..max directement dans le fichier AllSky
      if( out==null ) throw new Exception("createAllSky error: null output file !");
      double range [] = out.findAutocutRange();
      
      // Indication du pixelmin et pixelmax par l'utilisateur ?
      if( pixelMin!=0 || pixelMax!=0 ) { range[0]=pixelMin; range[1]=pixelMax; }
      
      out.headerFits.setKeyValue("PIXELMIN", range[0]+"");
      out.headerFits.setKeyValue("PIXELMAX", range[1]+"");
      out.headerFits.setKeyValue("DATAMIN",  range[2]+"");
      out.headerFits.setKeyValue("DATAMAX",  range[3]+"");
      
      // Ecriture du FITS (true bits)
      String filename = path+FS+"Norder"+order+FS+"Allsky";
      cds.tools.Util.createPath(filename);
      out.writeFITS(filename+".fits");
      
      Aladin.trace(4,"SkyGenerator.createAllSky()... "+ (int)((System.currentTimeMillis()-t)/1000)+"s");
      progress=100;
   }
   
   /** Cr�ation d'un AllSky JPEG couleur � partir des images JPEG � l'ordre indiqu�
    * Rq : seule la m�thode FIRST est support�e
    * @param path Emplacement du survey
    * @param order order Healpix
    * @param outLosangeWidth largeur des losanges pour le Allsky (typiquement 64 ou 128 pixels)
    */
   public void createAllSkyJpgColor(String path,int order,int outLosangeWidth) throws Exception {
      long t=System.currentTimeMillis();
      int nside = (int)CDSHealpix.pow2(order);
      int n = 12*nside*nside;
      int nbOutLosangeWidth = (int)Math.sqrt(n);
      int nbOutLosangeHeight = (int)((double)n/nbOutLosangeWidth);
      if( (double)n/nbOutLosangeWidth!=nbOutLosangeHeight ) nbOutLosangeHeight++;
      int outFileWidth = outLosangeWidth * nbOutLosangeWidth;
      
//      Aladin.trace(3,"Cr�ation Allsky order="+order+" mode=FIRST color"
//      +": "+n+" losanges ("+nbOutLosangeWidth+"x"+nbOutLosangeHeight
//      +" de "+outLosangeWidth+"x"+outLosangeWidth+" soit "+outFileWidth+"x"+nbOutLosangeHeight*outLosangeWidth+" pixels)...");

      Fits out = new Fits(outFileWidth,nbOutLosangeHeight*outLosangeWidth, 0);
      
      for( int npix=0; npix<n; npix++ ) {
         progress = npix*100./n;
         String name = Util.getFilePath(order, npix);
         Fits in = new Fits();
         String filename = path+FS+name;
         try {
            in.loadJpeg(filename+".jpg",true);
            int yLosange=npix/nbOutLosangeWidth;
            int xLosange=npix%nbOutLosangeWidth;
            int gap = in.width/outLosangeWidth;
            for( int y=0; y<in.width/gap; y++ ) {
               for( int x=0; x<in.width/gap; x++ ) {
                  int p=in.getPixelRGB(x*gap,y*gap);
                  int xOut = xLosange*outLosangeWidth + x;
                  int yOut = yLosange*outLosangeWidth + y;
                  out.setPixelRGB(xOut, yOut, p);
               }
            }
         }
         catch( Exception e ) { }
      }
      
//      if( out==null ) throw new Exception("createAllSkyJpgColor error: null output file !");

      String filename = path+FS+"Norder"+order+FS+"Allsky";
      cds.tools.Util.createPath(filename);
      out.writeJPEG(filename+".jpg");
      
      Aladin.trace(4,"SkyGenerator.createAllSkyJpgColor()... "+ (int)((System.currentTimeMillis()-t)/1000)+"s");
      progress=100;
   }
   
   private void createMOC(HpixTree moc,String path,int order, long npix) {
      String cfile = Util.getFilePath(order, npix);
      
   }

//   public void createMOC(String path) throws Exception {
//      long t=System.currentTimeMillis();
//      
//      // Parcours des r�pertoires de niveau le plus haut Norder3
//      // Pour chaque Npixnnn de niveau N, s'il manque un fils, je recommence r�cursivement
//     
//
//      String filename = path+FS+"Norder"+order+FS+"Allsky";
//      
//      Aladin.trace(4,"SkyGenerator.createMOC()... "+ (int)((System.currentTimeMillis()-t)/1000)+"s");
//      progress=100;
//   }

   /** Simulation d'un traitement de g�n�ration d'une image � partir d'une autre
    * comme le fera SkyBrowser pour la g�n�ration de la base Healpix
    */
   public static void main(String[] args) {
      
      try {
         SkyGenerator sg = new SkyGenerator();
         boolean color = false;
         String path="";
         int order=3;
         int size=64;
         double pixelMin=0,pixelMax=0;

         for( int i=0; i<args.length; i++ ) {
            if( args[i].equals("-color") ) color=true;
            else if( args[i].startsWith("-order=") ) order = Integer.parseInt(args[i].substring(7));
            else if( args[i].startsWith("-size=") )  size = Integer.parseInt(args[i].substring(6));
            else if( args[i].startsWith("-cut=") )   {
               String s = args[i].substring(5);
               int j = s.indexOf(',');
               pixelMin = Double.parseDouble(s.substring(0,j));
               pixelMax = Double.parseDouble(s.substring(j+1));
            }
            else path = args[i];
         }
         
         if( color ) sg.createAllSkyJpgColor(path, order, size);
         else sg.createAllSky(path,order,size,pixelMin,pixelMax,true);

      } catch( Exception e) {
         e.printStackTrace();
         System.out.println("Usage: [-color] [-order=nn] [-size=xx] [-cut=pixelMin,pixelMax] /Path/Survey");
      }
   }


   public double getProgress() {
	   return progress;
   }
}

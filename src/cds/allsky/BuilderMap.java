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

package cds.allsky;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Vector;

import cds.aladin.Localisation;
import cds.aladin.PlanImage;
import cds.aladin.Save;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/**
 * Création d'une Map HEALPix Fits (Map.fits) à partir des losanges individuels
 * @author Pierre Fernique [CDS]
 */
final public class BuilderMap  extends Builder {


   public BuilderMap(Context context) { super(context); }

   public Action getAction() { return Action.MAP; }

   public void validateContext() throws Exception {
      validateOutput();
      output = context.getOutputPath();
      nside = context.getMapNside();
      bitpix = context.getBitpix();
      blank = context.getBlank();
      if( bitpix==-1 ) {
         context.loadProperties();
         bitpix = Integer.parseInt( context.prop.getProperty("hips_pixel_bitpix") );
         frame = context.prop.getProperty("hips_frame").startsWith("G") ? Localisation.GAL : Localisation.ICRS;
         losangeWidth = Integer.parseInt( context.prop.getProperty("hips_tile_width") );
      } else {
         frame = context.getFrame();
         losangeWidth = context.getTileSide();
      }
      nside=4096;
      if( bitpix>0 && (context.getBZero()!=0 || context.getBScale()!=1) ) {
         bitpix=-32;
         context.warning("Coding in real values due to BZERO/BSCALE factors");
      }
   }
   
   private int bitpix;
   private double blank;
   private String output;
   private int frame;
   private int losangeWidth;
   private long nside;

   public void run() throws Exception {
      exportHpx();
      String tForm = bitpix==8 ? "I" : bitpix==16 ? "I" : bitpix==32 ? "J" : bitpix==-32 ? "E" : "D";
      context.info("HEALPix map generation in progress: NSIDE="+nside+" frame="+context.getFrameCode()
          +" TFORM=1"+tForm+" in "+ output+Util.FS+"Map.fits");
   }
   
   protected void exportHpx() throws Exception {
      
      OutputStream f = null;
      try {
         f=new FileOutputStream( output+Util.FS+"Map.fits" );
         int size=0;

         int orderLosange = (int)CDSHealpix.log2(losangeWidth);
         int orderMap = (int)CDSHealpix.log2(nside);
         int nbits=Math.abs(bitpix)/8;

         long nbPix3 = 12*8*8;// 12 x 2^3 x 2^3 // à l'ordre 3
         boolean ring = false;
         int lenLine=1024;

         // Generation de la première HDU FITS
         Vector v = Save.generateHealpixHDU0(false);
         size=Save.writeFitsLines(f,v,size);
         byte [] end = Save.getEndBourrage(size);
         f.write(end);
         size += end.length;

         // Generation de la deuxième HDU FITS
         double badData = Double.NaN;
         if( bitpix>0 && !Double.isNaN(blank) ) badData=blank;
         v = Save.generateHealpixHDU1(orderMap,bitpix,ring,lenLine,frame,badData);
         size=Save.writeFitsLines(f,v,size);
         end = Save.getEndBourrage(size);
         f.write(end);
         size += end.length;

         // Sauvegarde des pixels (on parcourt les pixels Healpix dans l'ordre)
         // et on écrit ligne par ligne (lenLigne valeurs à chaque fois)
         byte [] buf = new byte[lenLine*nbits];
         int pos=0;
         // nb pixels par losange
         int nbPix = losangeWidth*losangeWidth;
         byte [] nan = new byte[nbPix*nbits];
         for (int i = 0 ; i < nbPix ; i++) PlanImage.setPixVal(nan, bitpix, i, Double.NaN);
         
         int[] hpx2xy = cds.tools.pixtools.Util.createHpx2xy(orderLosange);
         
         for (int i = 0 ; i < nbPix3 ; i++) {
            boolean found = true;
            double val;
            // récupère le losange de niveau 3
            String filename = cds.tools.pixtools.Util.getFilePath(output,3, i);
            Fits los = new Fits();
            try {
               los.loadFITS(filename+".fits");
            } catch (FileNotFoundException e) {
               // ne rien dire, il va y en avoir plein si c'est partiel !
               found=false;
            }
            if (!found) {
               // on finit d'écrire ce qu'il restait dans le buffer
               f.write(buf,0,pos); size+=pos; pos=0;
               // on ajoute tout le losange en nan
               f.write(nan); pos=0; size+=nan.length; 
            }
            else {
               for( int ipix = 0 ; ipix < nbPix ; ipix++) {
                  //                  int[] xy = cds.tools.pixtools.Util.hpx2XY(ipix+1,N);
                  //                  val = los.getPixelDouble(xy[0],losangeWidth-1-xy[1]);
                  int idx = hpx2xy[ipix];
                  int yy = idx/losangeWidth;
                  int xx = idx-yy*losangeWidth;
                  val = los.getPixelFull(xx,yy);
                  if( bitpix<0 && los.isBlankPixel(val) ) val=Double.NaN;
//                  val = los.getPixelDouble(xx,yy);
                  PlanImage.setPixVal(buf, bitpix, pos++, val);

                  if( pos==lenLine ) {
                     f.write(buf); pos=0; size+=buf.length;
                  }
               }
            }
         }
         if( pos>0 ) { f.write(buf,0,pos); size+=pos; }

         end = Save.getEndBourrage(size);
         f.write(end);
         size += end.length;

      } finally { f.close(); }
   }


}

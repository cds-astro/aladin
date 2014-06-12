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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;

import cds.aladin.Aladin;
import cds.aladin.HealpixProgen;
import cds.allsky.Context.JpegMethod;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

/** Fusion de 2 HiPS, puis reconstruction de l'arborescence, du allsky et du MOC
 * @author Pierre Fernique
 */
public class BuilderConcat extends BuilderTiles {
//   private int statNbFile;
//   private long statSize;
//   private long startTime,totalTime;
   private HealpixMoc inputMoc,outputMoc;
   private String outputPath;
   private String inputPath;
   private String outputPathIndex;
   private String inputPathIndex;
   private Mode mode;
   private boolean doHpxFinder;
   private int tileMode;

   public BuilderConcat(Context context) {
      super(context);
   }

   public Action getAction() { return Action.CONCAT; }
   
   public void run() throws Exception {
      build();
      
      // Regeneration de l'arborescence pour la zone concernée
//      (new BuilderTree(context)).run();
//      context.info("tree updated");
      
      boolean inJpg=false,inPng=false;
      
      if( !context.isColor() ) {
         // Regeneration des tuiles jpeg et de l'arborescence pour la zone concernée si nécessaire
         inJpg = (new File(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.jpg")).exists();
         if( inJpg ) { (new BuilderJpg(context)).run(); context.info("JPEG tiles updated"); }

         // Regeneration des tuiles png et de l'arborescence pour la zone concernée si nécessaire
         inPng = (new File(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.png")).exists();
         if( inPng ) { (new BuilderPng(context)).run(); context.info("PNG tiles updated"); }
      }
      
      // Regeneration des allsky si non encore fait
      if( !inJpg && !inPng ) (new BuilderAllsky(context)).run();
      context.info("Allsky updated");
      
      // Mise à jour ou generation du MOC final
      String outputPath = context.getOutputPath();
      outputMoc = new HealpixMoc();
      File f = new File(outputPath+Util.FS+BuilderMoc.MOCNAME);
      if( f.exists() ) {
         outputMoc.read( f.getCanonicalPath() );
         outputMoc = outputMoc.union(inputMoc);
         outputMoc.write( context.getOutputPath()+Util.FS+"Moc.fits");
         context.info("MOC updated");
      } else {
         (new BuilderMoc(context)).run();
         context.info("MOC done");
      }
      
      // Post traitement sur le HpxFinder si nécessaire
      if( !doHpxFinder ) {
         f = new File(outputPathIndex);
         if( f.isDirectory() ) {
            f.renameTo( new File(outputPathIndex+"-partial"));
            context.warning("Previous HpxFinder has been removed as "+Constante.HPX_FINDER+"-partial");
         }
      } else {
         // Il faut refaire le MOC de l'index cumulé
         (new BuilderMocIndex(context)).run();
         context.info("Index MOC updated");
        
         // Faut-il lancer également une commande PROGEN
         f = new File(outputPathIndex+Util.FS+"Norder"+(context.order-1));
         if( f.isDirectory() ) {
            (new BuilderDetails(context)).run();
            context.info("PROGEN tiles updated");
         }
      }
   }
   
   // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      outputPath = context.getOutputPath();
      inputPath = context.getInputPath();
      outputPathIndex = cds.tools.Util.concatDir( outputPath,Constante.HPX_FINDER);
      inputPathIndex = cds.tools.Util.concatDir( inputPath,Constante.HPX_FINDER);
      mode = context.getMode();
      tileMode=Context.FITS;

      if( inputPath==null ) throw new Exception("\"in\" parameter required !");
      File f = new File(inputPath);
      if( f.exists() && (!f.isDirectory() || !f.canRead() )) throw new Exception("\"inputPath\" directory not available ["+inputPath+"]");

      int order = Util.getMaxOrderByPath( outputPath );
      if( order==-1 )  throw new Exception("No HiPS found in ouput dir");
      context.setOrder(order);
      int inputOrder = Util.getMaxOrderByPath( inputPath );
      if( inputOrder==-1 )  throw new Exception("No HiPS found in input dir");
      if( order!=inputOrder ) throw new Exception("Uncompatible HiPS: out.order="+order+" input.order="+inputOrder);
      context.info("Order retrieved from ["+inputPath+"] => "+order);
      
      String allsky = context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits";
      if( (new File(allsky)).exists() ) {
         setContextParamFromPreviousAllskyFile(allsky);
         validateParams(inputPath+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
      } else {
         allsky = context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.png";
         if( (new File(allsky)).exists() ) {
            context.setColor("png");
            tileMode=Context.PNG;
            context.info("Processing HiPS colored in "+context.getTileExt()+" tiles");
         } else {
            allsky = context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.jpg";
            if( (new File(allsky)).exists() ) {
               context.setColor("jpg");
               tileMode=Context.JPEG;
               context.info("Processing HiPS colored in "+context.getTileExt()+" tiles");
            }
         }
      }
      
      // faudra-t-il traiter les index
      doHpxFinder = (new File(inputPathIndex)).isDirectory() && (new File(outputPathIndex)).isDirectory();
      if( doHpxFinder ) context.info("HpxFinder will be also concatenated (mode="+mode+")");

      inputMoc = new HealpixMoc();
      f = new File(inputPath+Util.FS+BuilderMoc.MOCNAME);
      if( f.exists() ) inputMoc.read( f.getCanonicalPath() );
      else {
         context.info("No input MOC found => generate it...");
         context.setOutputPath(inputPath);
         (new BuilderMoc(context)).run();
         context.setOutputPath(outputPath);
         f = new File(inputPath+Util.FS+BuilderMoc.MOCNAME);
         inputMoc.read( f.getCanonicalPath() );
      }
      
      if( context.mocArea!=null ) inputMoc = inputMoc.intersection(context.mocArea);
      context.moc = inputMoc;

   }
   
   protected void setContextParamFromPreviousAllskyFile(String allskyFile) throws Exception {
      Fits f = new Fits();
      f.loadHeaderFITS(allskyFile);
      double cut[] = new double[4];
      try {
         cut[0] = f.headerFits.getDoubleFromHeader("PIXELMIN");
         cut[1] = f.headerFits.getDoubleFromHeader("PIXELMAX");
      } catch( Exception e ) { cut[0]=cut[1]=0; }
      try {
         cut[2] = f.headerFits.getDoubleFromHeader("DATAMIN");
         cut[3] = f.headerFits.getDoubleFromHeader("DATAMAX");
      } catch( Exception e ) { cut[2]=cut[3]=0; }
      context.setCut(cut);
      
      try {
         double blank = f.headerFits.getDoubleFromHeader("BLANK");
         context.blank = blank;
      } catch( Exception e ) { }
    
      try {
         double bscale = f.headerFits.getDoubleFromHeader("BSCALE");
         context.bscale = bscale;
      } catch( Exception e ) { }
    
      try {
         double bzero = f.headerFits.getDoubleFromHeader("BZERO");
         context.bzero = bzero;
      } catch( Exception e ) { }
    
      try {
         int bitpix = f.headerFits.getIntFromHeader("BITPIX");
         context.bitpix = bitpix;
      } catch( Exception e ) { }
    
   }

   
   /** Vérifie l'adéquation des deux HiPS en fonction du fichier allsky.fits input */
   public void validateParams(String allskyFile) throws Exception {
      Fits f = new Fits();
      f.loadHeaderFITS(allskyFile);
      
      int bitpix = f.headerFits.getIntFromHeader("BITPIX");
      if( bitpix!=context.bitpix ) throw new Exception("Uncompatible HiPS => input.BITPIX="+bitpix+" output.BITPIX="+context.bitpix);
    
      double bscale=1;
      try {
         bscale = f.headerFits.getDoubleFromHeader("BSCALE");
      } catch( Exception e ) { }
      if( bscale!=context.bscale ) context.warning("BSCALE modification => ignored (input.BSCALE="+bscale+" output.BSCALE="+context.bscale+")");
    
      double bzero=0;
      try {
         bzero = f.headerFits.getDoubleFromHeader("BZERO");
      } catch( Exception e ) { }
      if( bzero!=context.bzero ) context.warning("BZERO modification =>ignored (input.BZERO="+bzero+" output.BZERO="+context.bzero+")");
      
      double blank=Double.NaN;
      try {
         blank = f.headerFits.getDoubleFromHeader("BLANK");
      } catch( Exception e ) { }
      if( !Double.isNaN(blank) && blank!=context.blank ) context.warning("BLANK modification => ignored (input.BLANK="+blank+" output.BLANK="+context.blank+")");
   }

   
   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,int order,long npix,int z) throws Exception {
      long t = System.currentTimeMillis();
      Fits out=null;
      
      String inputFile = Util.getFilePath(inputPath,order,npix,z);
      Fits input = loadTile(inputFile);
      if( input==null ) {
         long duree = System.currentTimeMillis()-t;
         updateStat(0,0,1,duree,0,0);
         return null;
      }

      // traitement de la tuile
      String outFile = Util.getFilePath(outputPath,order,npix,z);
      out = loadTile(outFile);
      
      switch(mode) {
         case REPLACETILE:
            out=input;
            break;
         case KEEPTILE :
            if( out==null ) out=input;
            break;
         case AVERAGE:
            if( out!=null ) input.coadd(out);
            out=input;
            break;
         case OVERWRITE:
            if( out!=null ) out.mergeOnNaN(input);
            else out=input; 
            break;
         case KEEP:
            if( out!=null ) input.mergeOnNaN(out); 
            out=input;
            break;
      }

      if( out==null ) throw new Exception("Y a un blème ! out==null");
      else write(outFile,out);
      
      if( context.isTaskAborting() )  throw new Exception("Task abort !");

      // Traitement de la tuile index
      if( doHpxFinder ) {
         String inputIndexFile = Util.getFilePath(inputPathIndex,order,npix,z);
         HealpixProgen inputIndex = loadIndex(inputIndexFile);
         String outIndexFile = Util.getFilePath(outputPathIndex,order,npix,z);
         HealpixProgen outIndex = loadIndex(outIndexFile);

         switch(mode) {
            case REPLACETILE:
               outIndex=inputIndex;
               break;
            case KEEPTILE :
               if( outIndex==null ) outIndex=inputIndex;
               break;
            case AVERAGE:
            case OVERWRITE:
            case KEEP:
               if( outIndex!=null ) inputIndex.merge(outIndex);
               outIndex=inputIndex;
               break;
         }
         writeIndex(outIndexFile,outIndex);
      }

      long duree = System.currentTimeMillis()-t;
      updateStat(0,1,0,duree,0,0);
      return out;
   }
   
   private Fits loadTile(String file) throws Exception {
      Fits f = new Fits();
      try {
         if( tileMode==Context.FITS ) f.loadFITS(file+".fits");
         else if( tileMode==Context.PNG ) f.loadJpeg(file+".png",true,false);
         else if( tileMode==Context.JPEG ) f.loadJpeg(file+".jpg",true,false);
      } catch( Exception e ) {
         f=null;
      }
      return f;
   }
   
//   private void writeTile(Fits out,String file) throws Exception {
//      String s=null;
//      if( tileMode==Context.FITS )  out.writeFITS(s=file+".fits");
//      else if( tileMode==Context.PNG ) out.writeRGBcompressed(s=file+".png","png");
//      else if( tileMode==Context.JPEG ) out.writeRGBcompressed(s=file+".jpg","jpg");
//      
//      File f = new File(s);
//      updateStat(f);
//   }
   
   // Ecriture du fichier d'index HEALPix correspondant à la map passée en paramètre
   private void writeIndex(String file,HealpixProgen map) throws Exception {
      cds.tools.Util.createPath(file);
      map.writeStream(new FileOutputStream(file) );
   }
   
   /** Construction d'une tuile terminale. Lit le fichier est map les entrées de l'index
    * dans une TreeMap */
   private HealpixProgen loadIndex(String file) throws Exception {
      File f = new File(file);
      if( !f.exists() ) return null;
      HealpixProgen out = new HealpixProgen();
      out.loadStream( new FileInputStream(f));
      return out;
   }
   

   
//   private void initStat() { statNbFile=0; statSize=0; startTime = System.currentTimeMillis(); }
//
//   // Mise à jour des stats
//   private void updateStat(File f) {
//      statNbFile++;
//      statSize += f.length();
//      totalTime = System.currentTimeMillis()-startTime;
//   }

}
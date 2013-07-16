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
import cds.aladin.HealpixIndex;
import cds.allsky.Context.JpegMethod;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

/** Fusion de 2 HiPS, puis reconstruction de l'arborescence, du allsky et du MOC
 * @author Pierre Fernique
 */
public class BuilderTreeMerge extends Builder {
   private int statNbFile;
   private long statSize;
   private long startTime,totalTime;
   private HealpixMoc inputMoc,outputMoc;
   private String outputPath;
   private String inputPath;
   private String outputPathIndex;
   private String inputPathIndex;
   private CoAddMode mode;
   private boolean doHpxFinder;

   /**
    * Création du générateur de l'arbre FITS.
    * @param context
    */
   public BuilderTreeMerge(Context context) {
      super(context);
   }

   public Action getAction() { return Action.CONCAT; }
   
   public void run() throws Exception {
      build();
      try {
         setContextParamFromPreviousAllskyFile(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
      } catch( Exception e ) { }
      
      // Regeneration de l'arborescence pour la zone concernée
      (new BuilderTree(context)).run();
      context.info("tree updated");
      
      // Regeneration des tuiles jpeg et de l'arborescence pour la zone concernée si nécessaire
      boolean inJpg = (new File(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.jpg")).exists();
      if( inJpg ) { (new BuilderJpg(context)).run(); context.info("JPEG tiles updated"); }
      
      // Regeneration des tuiles png et de l'arborescence pour la zone concernée si nécessaire
      boolean inPng = (new File(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.png")).exists();
      if( inPng ) { (new BuilderPng(context)).run(); context.info("PNG tiles updated"); }
      
      // Regeneration des allsky si non encore fait
      if( !inJpg && !inPng ) (new BuilderAllsky(context)).run();
      context.info("Allsky updated");
      
      // Mise à jour ou generation du MOC final
      String outputPath = context.getOutputPath();
      outputMoc = new HealpixMoc();
      File f = new File(outputPath+Util.FS+BuilderMoc.MOCNAME);
      if( f.exists() ) {
         outputMoc.read( f.getCanonicalPath() );
         outputMoc.union(inputMoc);
         outputMoc.write( context.getOutputPath()+Util.FS+"Moc.fits", HealpixMoc.FITS);
         context.info("MOC updated");
      } else {
         (new BuilderMoc(context)).run();
         context.info("MOC done");
       }
      
      if( !doHpxFinder ) {
         f = new File(outputPathIndex);
         if( f.isDirectory() ) {
            f.renameTo( new File(outputPathIndex+"-partial"));
            context.warning("Previous HpxFinder has been removed as "+Constante.HPX_FINDER+"-partial");
         }
      }
   }
   
   // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      outputPath = context.getOutputPath();
      inputPath = context.getInputPath();
      outputPathIndex = cds.tools.Util.concatDir( outputPath,Constante.HPX_FINDER);
      inputPathIndex = cds.tools.Util.concatDir( inputPath,Constante.HPX_FINDER);
      mode = context.getCoAddMode();

      if( inputPath==null ) throw new Exception("\"input\" parameter required !");
      File f = new File(inputPath);
      if( f.exists() && (!f.isDirectory() || !f.canRead() )) throw new Exception("\"inputPath\" directory not available ["+inputPath+"]");

      if( !context.isExistingAllskyDir() ) throw new Exception("No HiPS found in ouput dir");
      if( !context.isExistingAllskyDir( inputPath ) ) throw new Exception("No HiPS found in inputPath dir");

      int order = Util.getMaxOrderByPath( outputPath );
      context.setOrder(order);
      int inputOrder = Util.getMaxOrderByPath( inputPath );
      if( order!=inputOrder ) throw new Exception("Uncompatible HiPS: out.order="+order+" input.order="+inputOrder);
      context.info("Order retrieved from ["+inputPath+"] => "+order);
      
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
      if( context.mocArea!=null ) inputMoc.intersection(context.mocArea);
      context.moc = inputMoc;

   }
   
   /** retourne les pixelscut et pixelsrange en fonction d'un fichier allsky.fits */
   public void setContextParamFromPreviousAllskyFile(String allskyFile) throws Exception {
      Fits f = new Fits();
      f.loadHeaderFITS(allskyFile);
      double cut[] = new double[4];
      try {
         cut[0] = f.headerFits.getDoubleFromHeader("PIXELMIN");
         cut[1] = f.headerFits.getDoubleFromHeader("PIXELMAX");
      } catch( Exception e ) { cut[0]=cut[1]=0; }
      try {
         cut[2] = f.headerFits.getDoubleFromHeader("RANGEMIN");
         cut[3] = f.headerFits.getDoubleFromHeader("RANGEMAX");
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
    
   }


   
   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, statSize, totalTime);
   }

   public void build() throws Exception {
      initStat();
      
      int order = context.getOrder();
      
      Iterator<Long> it = context.moc.pixelIterator();
      while( it.hasNext() ) {
         long npix = it.next().longValue();
         
         
         Fits out=null;
         
         String inputFile = Util.getFilePath(inputPath,order,npix);
         Fits input = loadFits(inputFile);
         if( input==null ) continue;
         
         // traitement de la tuile
         String outFile = Util.getFilePath(outputPath,order,npix);
         out = loadFits(outFile);
         
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
         
         out.writeFITS(outFile+".fits");
         File f = new File(outFile+".fits");
         updateStat(f);
         if( context.isTaskAborting() )  throw new Exception("Task abort !");

         if( !doHpxFinder ) continue;
         
         // Traitement de la tuile index
         String inputIndexFile = Util.getFilePath(inputPathIndex,order,npix);
         HealpixIndex inputIndex = loadIndex(inputIndexFile);
         String outIndexFile = Util.getFilePath(outputPathIndex,order,npix);
         HealpixIndex outIndex = loadIndex(outIndexFile);
         
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
      
      // Union des deux mocs pour préparer l'arbre
//      context.moc.union(contextMoc);
   }
   
   private Fits loadFits(String file) throws Exception {
      Fits f = new Fits();
      try {
         f.loadFITS(file+".fits");
      } catch( Exception e ) {
         f=null;
      }
      return f;
   }
   
   // Ecriture du fichier d'index HEALPix correspondant à la map passée en paramètre
   private void writeIndex(String file,HealpixIndex map) throws Exception {
      cds.tools.Util.createPath(file);
      map.writeStream(new FileOutputStream(file) );
   }
   
   /** Construction d'une tuile terminale. Lit le fichier est map les entrées de l'index
    * dans une TreeMap */
   private HealpixIndex loadIndex(String file) throws Exception {
      File f = new File(file);
      if( !f.exists() ) return null;
      HealpixIndex out = new HealpixIndex();
      out.loadStream( new FileInputStream(f));
      return out;
   }
   

   
   private void initStat() { statNbFile=0; statSize=0; startTime = System.currentTimeMillis(); }

   // Mise à jour des stats
   private void updateStat(File f) {
      statNbFile++;
      statSize += f.length();
      totalTime = System.currentTimeMillis()-startTime;
   }

}
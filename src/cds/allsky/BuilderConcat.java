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

package cds.allsky;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import cds.aladin.HealpixProgen;
import cds.aladin.MyProperties;
import cds.aladin.PlanBG;
import cds.fits.Fits;
import cds.moc.SMoc;
import cds.tools.pixtools.Util;

/** Fusion de 2 HiPS, puis reconstruction de l'arborescence, du allsky et du MOC
 * @author Pierre Fernique
 */
public class BuilderConcat extends BuilderRunner {
   //   private int statNbFile;
   //   private long statSize;
   //   private long startTime,totalTime;
   private SMoc inputMoc,outputMoc;
   private String outputPath;
   private String inputPath;
   private String outputPathIndex;
   private String inputPathIndex;
   private ModeMerge mode;
   private boolean doHpxFinder;
   private int tileMode;
   private int tileSide;
   private boolean live=false;            // true si on doit utiliser les cartes de poids
   private boolean liveIn,liveOut;

   public BuilderConcat(Context context) {
      super(context);
   }

   public Action getAction() { return Action.CONCAT; }

   public void run() throws Exception {
      build();
      
      context.resetCheckCode();

      // Regeneration de l'arborescence pour la zone concernée
      (new BuilderTree(context)).run();
      context.info("Hips tree updated");

      boolean inJpg=false,inPng=false;

      if( !context.isColor() ) {
         // Regeneration des tuiles jpeg et de l'arborescence pour la zone concernée si nécessaire
//         inJpg = (new File(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.jpg")).exists();
         inJpg = context.findOneNpixFile(context.getOutputPath(),"jpg")!=null;
         if( inJpg ) { (new BuilderJpg(context)).run(); context.info("JPEG tiles updated"); }

         // Regeneration des tuiles png et de l'arborescence pour la zone concernée si nécessaire
//         inPng = (new File(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.png")).exists();
         inPng = context.findOneNpixFile(context.getOutputPath(),"png")!=null;
         if( inPng ) {
            context.info("Updating PNG tiles...");
            context.setModeMerge(ModeMerge.mergeOverwriteTile);
            (new BuilderPng(context)).run(); 
         }
      }

      String outputPath = context.getOutputPath();
      
      // Dans le cas d'un traitement algorythmique, on ne fait pas de mise à jour
      // ni du MOC ni du HpxFinder
      if( mode!=ModeMerge.mergeMul && mode!=ModeMerge.mergeDiv && mode!=ModeMerge.mergeSub) {
         
         // Mise à jour ou generation du MOC final
         outputMoc = new SMoc();
         File f = new File(outputPath+Util.FS+Constante.FILE_MOC);
         if( f.exists() ) {
            outputMoc.read( f.getCanonicalPath() );
            outputMoc = outputMoc.union(inputMoc);
            outputMoc.write( context.getOutputPath()+Util.FS+Constante.FILE_MOC);
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
               context.warning("Previous HpxFinder has been removed as "+Constante.FILE_HPXFINDER+"-partial");
            }
         } else {
            // Il faut refaire le MOC de l'index cumulé
            (new BuilderMocIndex(context)).run();
            context.info("Index MOC updated");

            // Faut-il lancer également une commande PROGEN
            f = new File(outputPathIndex+Util.FS+"metadata.xml");
            if( f.exists() ) {
               BuilderDetails  b = new BuilderDetails(context);
               b.validateContext();
               b.run();
               context.info("PROGEN tiles updated");
            }
         }
      }
   }

   // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      outputPath = context.getOutputPath();
      inputPath = context.getInputPath();
      outputPathIndex = cds.tools.Util.concatDir( outputPath,Constante.FILE_HPXFINDER);
      inputPathIndex = cds.tools.Util.concatDir( inputPath,Constante.FILE_HPXFINDER);
      mode = context.getModeMerge();
      tileMode=Constante.TILE_FITS;
      tileSide=context.getTileSide();

      if( inputPath==null ) throw new Exception("\"in\" parameter required !");
      File f = new File(inputPath);
      if( f.exists() && (!f.isDirectory() || !f.canRead() )) throw new Exception("\"inputPath\" directory not available ["+inputPath+"]");

      // Mise à jour des propriétés pour le suivi des addendum
      addAddendum( context.getInputPath(), context.getOutputPath() );

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
            tileMode=Constante.TILE_PNG;
            context.info("Processing HiPS colored in "+context.getTileExt()+" tiles");
         } else {
            allsky = context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.jpg";
            if( (new File(allsky)).exists() ) {
               context.setColor("jpg");
               tileMode=Constante.TILE_JPEG;
               context.info("Processing HiPS colored in "+context.getTileExt()+" tiles");
            }
         }
      }

      inputMoc = new SMoc();
      f = new File(inputPath+Util.FS+Constante.FILE_MOC);
      if( f.exists() ) inputMoc.read( f.getCanonicalPath() );
      else {
         context.info("No input MOC found => generate it...");
         context.setOutputPath(inputPath);
         (new BuilderMoc(context)).run();
         context.setOutputPath(outputPath);
         f = new File(inputPath+Util.FS+Constante.FILE_MOC);
         inputMoc.read( f.getCanonicalPath() );
      }

      if( context.mocArea!=null ) inputMoc = inputMoc.intersection(context.mocArea);
      context.moc = inputMoc;

      // Dans le cas de rénégération des allsky.png il faut connaître les cuts
      double [] cut = context.getCut();
      updateCutByProperties(cut);
      context.setCut(cut);
      context.setValidateCut(true);
      
      // Il faut voir si on peut utiliser les tuiles de poids
      liveOut = checkLiveByProperties(context.getOutputPath());
      liveIn = checkLiveByProperties(context.getInputPath());
      live = liveIn && liveOut;
      
      // faudra-t-il traiter les index
      doHpxFinder = (new File(inputPathIndex)).isDirectory() && (new File(outputPathIndex)).isDirectory();
      if( mode==ModeMerge.mergeMul || mode==ModeMerge.mergeDiv || mode==ModeMerge.mergeSub) {
         doHpxFinder=false;
         if( liveIn ) context.warning("Source HiPS does provide weight tiles => ignored");
      }
      if( doHpxFinder ) context.info("HpxFinder will be also concatenated");

      if( mode==ModeMerge.mergeMean ) {
         if( !live ) context.warning("Both HiPS to merge do not provide weight tiles => assuming basic average");
         else if( !liveOut ) context.warning("Target HiPS do not provide weight tiles => assuming weigth 1 for each output pixel");
         else if( !liveIn ) context.warning("Source HiPS do not provide weight tiles => assuming weigth 1 for each input pixel");
      }
      
      // Info sur la méthode
      context.info("Merge mode (tiles): "+ModeMerge.getExplanation(context.getModeMerge()));
      context.info("Hierarchy mode (tree): "+ModeTree.getExplanation(context.getModeTree()));
      
      
   }
   
   /** Vérifie que les 2 hips disposent des cartes de poids */
   protected boolean checkLiveByProperties(String path) {
      try {
         String s = loadProperty(path,Constante.KEY_DATAPRODUCT_SUBTYPE);
         return s!=null && s.indexOf("live")>=0;
      } catch( Exception e ) {}
      return false;
   }

   /** Ajoute le Addendum_id aux propriétés, et génère une exception si déjà existant */
   protected void addAddendum(String sourcePath, String targetPath) throws Exception {
      
      String targetHipsId = getHipsIdFromProperty(targetPath);
      context.setHipsId(targetHipsId);
      String addendumId = loadProperty(targetPath,Constante.KEY_ADDENDUM_ID);
      context.setAddendum(addendumId);

      String sourceHipsId = getHipsIdFromProperty(sourcePath);
      context.addAddendum(sourceHipsId);
      context.info("Concat "+sourceHipsId+" into "+targetHipsId+"...");
   }
   
   
   /** Charge une valeur d'un mot clé d'un fichier de properties pour un répertoire particulier, null sinon */
   protected String loadProperty(String path,String key) throws Exception {
      MyProperties prop = loadProperties(path);
      if( prop==null ) return null;
      return prop.getProperty(key);
   }
   
   /** Détermine l'HiPS ID depuis un fichier properties */
   protected String getHipsIdFromProperty(String path) throws Exception {
      MyProperties prop = loadProperties(path);
      return PlanBG.getHiPSID(prop);
   }
   
   /** Charge les propriétés d'un fichier properties, retourne null si problème */
   protected MyProperties loadProperties(String path) {
      InputStreamReader in = null;
      try {
         String propFile = path+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            in = new InputStreamReader( new BufferedInputStream( new FileInputStream(propFile) ), "UTF-8");
            prop.load(in);
            in.close();
            in=null;
            return prop;
         }
      } 
      catch( Exception e ) {}
      finally { if( in!=null ) try { in.close(); } catch( Exception e ) {} }
      return null;
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
//      boolean flagOp = Mode.isOperation( context.getMode());

      int bitpix = f.headerFits.getIntFromHeader("BITPIX");
      if( bitpix!=context.bitpix ) throw new Exception("Uncompatible HiPS => input.BITPIX="+bitpix+" output.BITPIX="+context.bitpix);

      double bscale=1;
      try {
         bscale = f.headerFits.getDoubleFromHeader("BSCALE");
      } catch( Exception e ) { }
      double bzero=0;
      try {
         bzero = f.headerFits.getDoubleFromHeader("BZERO");
      } catch( Exception e ) { }
      double blank=Double.NaN;
      if( bscale!=context.bscale || bzero!=context.bzero ) {
         /* if( flagOp ) throw new Exception("Uncompatible BSCALE/BZERO => input:"+bscale+"/"+bzero+" output:"+context.bscale+"/"+context.bzero);
         else */ context.info("BSCALE/BZERO modification => assuming input:"+bscale+"/"+bzero+" output:"+context.bscale+"/"+context.bzero);
      }
      try
      {
         blank = f.headerFits.getDoubleFromHeader("BLANK");
      } catch( Exception e ) { }
      if( !Double.isNaN(blank) && blank!=context.blank ) {
         /* if( flagOp ) throw new Exception("Uncompatible BLANK => input:"+blank+" output:"+context.blank);
         else */ context.info("BLANK modification => assuming input:"+blank+" output:"+context.blank);
      }
   }


   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,String path,int order,long npix,int z) throws Exception {
      long t = System.currentTimeMillis();
      Fits out=null;

      String inputFile = Util.getFilePath(inputPath,order,npix,z);
      Fits input = loadTile(inputFile);
      if( input==null ) {
         long duree = System.currentTimeMillis()-t;
         updateStat(0,0,1,duree,0,0);
         return null;
      }
      
      double [] weightOut=null;
      double [] weightIn=null;
      
      // traitement de la tuile
      String outFile = Util.getFilePath(outputPath,order,npix,z);
      if( mode!=ModeMerge.mergeOverwriteTile ) {
         out = loadTile(outFile);
         if( out!=null && live ) weightOut  = loadWeight(outFile,tileSide,1);
      }
      if( live ) weightIn = loadWeight(inputFile,tileSide,1);
      
      switch(mode) {
         case mergeOverwriteTile:
            input.adjustParams(bzero, bscale, blank);
            out=input;
            weightOut=weightIn;
            break;
         case mergeKeepTile :
            if( out==null ) {
               input.adjustParams(bzero, bscale, blank);
               out=input;
               weightOut=weightIn;
            }
            break;
         case mergeMean:
            if( out!=null ) {
               input.adjustParams(bzero, bscale, blank);
               if( !live ) input.coadd(out,Fits.AVG);
               else {
                  input.coadd(out, weightIn, weightOut);
               }
            } 
            input.adjustParams(bzero, bscale, blank);
            out=input;
            weightOut=weightIn;
            break;
         case mergeAdd:
            if( out!=null ) {
               out.coadd(input,Fits.ADD);
               if( live ) for( int i=0; i<weightOut.length; i++ ) weightOut[i] += weightIn[i];
            } else {
               input.adjustParams(bzero, bscale, blank);
               out=input; 
               weightOut=weightIn;
            }
            break;
         case mergeKeep:
            if( out!=null ) {
               if( !live ) out.mergeOnNaN(input);
               else out.mergeOnNaN(input,weightOut, weightIn);
            }  else {
               input.adjustParams(bzero, bscale, blank);
               out=input;
               weightOut=weightIn;
            }
            break;
         case mergeOverwrite:
            if( out!=null ) {
               input.adjustParams(bzero, bscale, blank);
               if( !live ) input.mergeOnNaN(out);
               else input.mergeOnNaN(out,weightIn, weightOut);
            } 
            input.adjustParams(bzero, bscale, blank);
            out=input;
            weightOut=weightIn;
            break;
         case mergeSub:
            if( out!=null ) out.coadd(input,Fits.SUB);
            break;
         case mergeMul:
            if( out!=null ) out.coadd(input,Fits.MUL);
            break;
         case mergeDiv:
            if( out!=null ) out.coadd(input,Fits.DIV);
            break;
         default:
            throw new Exception("Coadd mode ["+mode+"] not supported for CONCAT action");
      }

      if( out!=null ) {
         write(outFile,out);
         if( liveOut ) writeWeight(outFile,weightOut,tileSide,order,npix);
      }

      if( context.isTaskAborting() )  throw new Exception("Task abort !");

      // Traitement de la tuile index
      if( doHpxFinder ) {
         String inputIndexFile = Util.getFilePath(inputPathIndex,order,npix,z);
         HealpixProgen inputIndex = loadIndex(inputIndexFile);
         String outIndexFile = Util.getFilePath(outputPathIndex,order,npix,z);
         HealpixProgen outIndex = loadIndex(outIndexFile);

         switch(mode) {
            case mergeOverwriteTile:
               outIndex=inputIndex;
               break;
            case mergeKeepTile :
               if( outIndex==null ) outIndex=inputIndex;
               break;
            case mergeAdd:
            case mergeMean:
            case mergeOverwrite:
            case mergeKeep:
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
         if( tileMode==Constante.TILE_FITS ) f.loadFITS(file+".fits");
         else if( tileMode==Constante.TILE_PNG ) f.loadPreview(file+".png",true,false,Fits.PREVIEW_PNG);
         else if( tileMode==Constante.TILE_JPEG ) f.loadPreview(file+".jpg",true,false,Fits.PREVIEW_JPEG);
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

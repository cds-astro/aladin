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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.moc.SMoc;
import cds.tools.pixtools.Util;

/** Permet la génération du survey HEALPix à partir d'un index préalablement généré
 * Il s'agit d'une classe à étendre. Elle implante tout ce qu'il faut pour
 * travailler en multithreading.
 * Elle est étendue par BuilderTiles, BuilderJpg, BuilderPng, BuilderMirror, etc...
 * 
 * @authors Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public abstract class BuilderRunner extends Builder {
   
   protected int nbThread;  // Nombre de threads utilisés
   
   private boolean isColor;
   protected int bitpix;
   protected double bzero;
   protected double bscale;
   protected double blank;

   // Liste des Threads de calcul
   protected ArrayList<ThreadBuilder> threadList = new ArrayList<>();
   private ModeMerge modeMerge;
   private ModeTree modeTree;

   protected int ordermin = 3;
   protected int ordermax;
   protected long nummin = 0;
   protected long nummax = 0;
   protected LinkedList<Item> fifo;
   protected double automin = 0;
   protected double automax = 0;
   
   public static boolean DEBUG = true;

   public static String FS;
//   private boolean stopped = false;

   static { FS = System.getProperty("file.separator"); }

   // Pour les stat
   protected int statNbTile;                 // Nombre de tuiles terminales déjà calculés
   protected long statMinTime,statMaxTime,statTotalTime,statAvgTime;
   protected int statEmptyTile;               // Nombre de tuiles terminales de fait vides
   protected int statNodeTile;                 // Nombre de tuiles "intermédiaires" déjà calculés
   protected long statNodeTotalTime,statNodeAvgTime;
   protected long startTime;                 // Date de lancement du calcul
   protected long totalTime;                 // Temps depuis le début du calcul

   public BuilderRunner(Context context) { super(context); }

   public Action getAction() { return null; }

   public void run() throws Exception {
      long t0 = System.currentTimeMillis();
      String ext = getTileExt().replace('.',' ');
      int tileWidth = context.getTileSide();
      context.info("Creating "+context.getNbLowCells()+ext+" "+tileWidth+"x"+tileWidth+" tiles (order="+context.getOrder()+")...");
      if( context.live ) context.info("Will store pixel weights in "+context.getNbLowCells()+" fits"+" "+tileWidth+"x"+tileWidth+" weighted tiles...");
      
      context.resetCheckCode( context.getTileExt());

      // Un peu de baratin
      if( !context.isColor() ) {
         int b0=context.getBitpixOrig(), b1=context.getBitpix();
         if( b0!=b1 ) {
            context.info("BITPIX conversion from "+context.getBitpixOrig()+" to "+context.getBitpix());
            double cutOrig[] = context.getCutOrig();
            double cut[] = context.getCut();
            if( context.cutByImage ) context.info("Map original cut pixel range to ["+cut[2]+" .. "+cut[3]+"]");
            else context.info("Map original raw pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"] to ["+cut[2]+" .. "+cut[3]+"]");
         }
         else context.info("BITPIX = "+b1+" (no conversion)");
//         if( context.getDiskMem()!=-1 ) {
//            context.info("Disk requirement (upper approximation) : "+cds.tools.Util.getUnitDisk((long)(context.getDiskMem()*1.25)));
//         }
         double bs=context.getBScale(), bz=context.getBZero();
         if( bs!=1 || bz!=0 ) { context.info("BSCALE="+bs+" BZERO="+bz); }
         double bl0 = context.getBlankOrig();
         double bl1 = context.getBlank();
         String bkey = context.getBlankKey();
         if( context.hasAlternateBlank() ) context.info("BLANK conversion from "+(Double.isNaN(bl0)?"NaN":bl0)+" to "+(Double.isNaN(bl1)?"NaN":bl1));
         else context.info("BLANK="+ (bkey!=null? bkey : Double.isNaN(bl1)?"NaN":bl1));
         if( context.good!=null ) context.info("Good pixel values ["+ip(context.good[0],bz,bs)+" .. "+ip(context.good[1],bz,bs)+"] => other values are ignored");
         if( context.live ) context.info("Live HiPS => Weight tiles saved for potential future additions"); 
      }

      build();
      
      execTime = System.currentTimeMillis() - t0;
   }
   
   // Retourne true si le HiPS a une couverture tres petite sur le ciel, ne justifiant pas la creation d'un allsky
   // qui serait plein de vide.
   private boolean isTooSmallForAllsky() {
//      if( true ) return true;
      
      // Pour le moment on ne le fait que pour les cubes
      if( !context.isCube() ) return false;
      
      // Chargement du MOC réel à la place de celui de l'index (moins précis)
      try {
         context.loadMoc();
         return context.mocIndex.getCoverage()<0.04;   
      } catch( Exception e ) {
         return false;
      }

   }
   

   //   public boolean isAlreadyDone() {
   //      if( !context.actionPrecedeAction(Action.INDEX, Action.TILES)) return false;
   //      context.info("Pre-existing HEALPix FITS survey seems to be ready");
   //      return true;
   //   }

   public void validateContext() throws Exception {
      if( context instanceof ContextGui ) {
         context.setProgressBar( ((ContextGui)context).mainPanel.getProgressBarTile() );
      }

      validateInput();
      validateOutput();
      try {
         validateOrder(context.getHpxFinderPath());
      } catch(Exception e) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         context.warning(e.getMessage());
         // retry order validation with tiles directory
         validateOrder(context.getOutputPath());
      }
      
      //      if(  !context.isColor() ) {

      String img = context.getImgEtalon();
      if( img==null ) img = context.justFindImgEtalon( context.getInputPath() );

      // mémorisation des cuts et blank positionnés manuellement
      double [] memoCutOrig = context.getCutOrig();
      boolean hasAlternateBlank = context.hasAlternateBlank();
      double blankOrig = context.getBlankOrig();
      int bitpixOrig = context.getBitpixOrig();

      // Image étalon à charger obligatoirement pour BSCALE, BZERO, BITPIX et BLANK ainsi que pour le setTarget
      if( img==null ) throw new Exception("No source image found in "+context.getInputPath());
      context.info("Reference image: "+img);
      try { context.setImgEtalon(img); }
      catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }

      // Image de référence en couleur => pas besoin de plus
      if(  !context.isColor() ) {

         if( bitpixOrig==-1 ) {
            context.info("BITPIX found in the reference image => "+context.getBitpixOrig());
         } else if( bitpixOrig!=context.getBitpixOrig() ) {
            context.warning("The provided BITPIX (" +bitpixOrig+ ") is different than the original one (" + context.getBitpixOrig() + ") => bitpix conversion will be applied");
            context.setBitpixOrig(bitpixOrig);
         }

         if( context.depth>1 ) context.info("Original images are cubes (depth="+context.depth+")");

         double [] cutOrigBefore = context.getPixelRangeCut();
         if( cutOrigBefore!=null ) {
            memoCutOrig = new double[7];
            for( int i=0; i<4; i++ ) {
               if( Double.isNaN(cutOrigBefore[i]) ) continue;
               memoCutOrig[i] = (cutOrigBefore[i] - context.bZeroOrig)/context.bScaleOrig;
            }
         }

         // repositionnement des cuts et blank passés par paramètre
         // Attention, si un pourcentage a été indiquée, le cutmin/cutmax ne sera
         // pas reprit de l'origine, mais remplacé par celui calculé
         double [] cutOrig = context.getCutOrig();
         double bs = context.bScaleOrig;
         double bz = context.bZeroOrig;
         if( memoCutOrig!=null ) {
            if( Context.hasCut(memoCutOrig) ) { 
               cutOrig[Context.CUTMIN]=memoCutOrig[Context.CUTMIN]; 
               cutOrig[Context.CUTMAX]=memoCutOrig[Context.CUTMAX]; }
            if( Context.hasRange(memoCutOrig) ) {
               cutOrig[Context.RANGEMIN]=memoCutOrig[Context.RANGEMIN]; 
               cutOrig[Context.RANGEMAX]=memoCutOrig[Context.RANGEMAX]; 
            }
            context.setCutOrig(cutOrig);
         }
         
         if( cutOrig[0]==cutOrig[1] ) {
            context.warning("Suspicious pixel cut: ["+ip(cutOrig[Context.CUTMIN],bz,bs)
            +" .. "+ip(cutOrig[Context.CUTMAX],bz,bs)+"] => YOU WILL PROBABLY HAVE TO CHANGE/EDIT THE properties FILE VALUES");
         }

         context.setValidateCut(true);

         if( hasAlternateBlank ) context.setBlankOrig(blankOrig);

         context.initParameters();
         
         if( this instanceof BuilderJpg ) {
            context.info("Generating 8-bit tiles (PNG or JPEG) directly from the original images...");
         } else {
            context.info("Data range ["+ip(cutOrig[Context.RANGEMIN],bz,bs)
            +" .. "+ip(cutOrig[Context.RANGEMAX],bz,bs)+"], pixel cut ["
            +ip(cutOrig[Context.CUTMIN],bz,bs)+" .. "
            +ip(cutOrig[Context.CUTMAX],bz,bs)+"]");
         }

      } else context.initParameters();


      if( !context.verifCoherence() ) throw new Exception("Uncompatible pre-existing HiPS survey");
      if( !context.isColor() && context.getBScale()==0 ) throw new Exception("Big bug => BSCALE=0 !! please contact CDS");

      
      // Info sur les méthodes
      context.info("Overlay mode (progenitors): "+ModeOverlay.getExplanation(context.getModeOverlay()));
      
      // Pour accélérer un peu (pas de merge à faire)
      if( !context.isExistingAllskyDir() ) context.setModeMerge( ModeMerge.mergeOverwriteTile );
      else context.info("Merge mode (tiles): "+ModeMerge.getExplanation(context.getModeMerge()));
      
      context.info("Hierarchy mode (tree): "+ModeTree.getExplanation(context.getModeTree()));
      
      // Info sur le coordinate frame
      context.info("Frame (HiPS coordinate reference frame) => "+context.getFrameName());
      
      validateSplit();
   }
   
   
   private void validateSplit() throws Exception {
      String splitCmd = context.getSplit();
      if( splitCmd==null ) return;
      
      int bitpix, tileWidth, depth, order;
      String format, outputPath;
      SMoc moc;
      
      outputPath   = context.getOutputPath();
      bitpix       = context.getBitpix();
      tileWidth    = context.getTileSide();
      order        = context.getOrder();
      depth        = context.getDepth();
      format       = context.isColor() ? "jpeg" : "fits png";  // on suppose qu'on va créer des tuiles fits et preview (png)
      moc          = ( context.mocIndex.clone() );
      if( moc==null ) throw new Exception("No MOC available => splitting action not possible");
      if( !outputIsFree( outputPath, order ) ) {
         context.warning("HiPS output dir not empty => split function ignored");
         return;
      }

      validateSplit( outputPath, splitCmd, moc, order, bitpix, tileWidth, depth, format );
   }
   
   private boolean outputIsFree(String outputPath, int order) {
      String path = cds.tools.Util.concatDir(outputPath, "Norder" + order);
      File dir = new File(path);
      File [] fs = dir.listFiles();
      if( fs==null ) return true;
      for( File f : fs ) {
         if( f.isDirectory() && f.getName().startsWith("Dir") ) {
            try { Integer.parseInt( f.getName().substring(3) ); return false; }
            catch( Exception e ) {}
         }
      }
      return false;
   }
   

   long lastTime = 0L;
   long lastNbTile = 0L;

   /** Demande d'affichage des statistiques (via Task()) */
   public void showStatistics() {
      if( b!=null ) { b.showStatistics(); return; }
      int statNbThreadRunning = getNbThreadRunning();
      int statNbThread = getNbThreads();
      if( statNbThreadRunning==0 || statNbTile==0 ) return;
      long now = System.currentTimeMillis();
      totalTime = now-startTime;
      long deltaTime = now-lastTime;
      long deltaNbTile = statNbTile-lastNbTile;
      lastTime=now;
      lastNbTile=statNbTile;
      context.showTilesStat(statNbThreadRunning,statNbThread,totalTime,statNbTile,statEmptyTile,statNodeTile,
            statMinTime,statMaxTime,statAvgTime,statNodeAvgTime,0L/*getUsedMem()*/,deltaTime,deltaNbTile);
      if( Aladin.levelTrace>=3 ) {
         String s = showMem();
         if( s.length()>0 ) context.stat(s);
         showDebugInfo();
      }
     
      //      if( context.cacheFits!=null && context.cacheFits.getStatNbOpen()>0 ) context.stat(context.cacheFits+"");
   }

   Hashtable<Thread,ArrayList<Fits>> memPerThread;

   // Initialisation des statistiques
   private void initStat(int nbThread) {
//      statNbThread=nbThread; statNbThreadRunning=0;
      statNbTile=statNodeTile=0;
      statTotalTime=statNodeTotalTime=0L;
      startTime = System.currentTimeMillis();
      totalTime=0L;
      memPerThread = new Hashtable<>();
   }

   // Suivi de mémoire d'un Thread particulier : suppression du Thread
   private void rmThread(Thread t) {
      Long key = new Long(t.hashCode());
      synchronized( memPerThread ) {
         ArrayList<Fits> m = memPerThread.get(key);
         if( m!=null ) for( Fits f : m ) f.free();
         memPerThread.remove(key);
//         ThreadBuilderTile.nbThreads--;
      }
   }

   // Suivi de mémoire d'un Thread particulier : ajout d'un Fits
   protected void addFits(Thread t,Fits f) {
      if( f==null ) return;
      if( f.width==0 ) {
         try {
            throw new Exception();
         } catch( Exception e) { e.printStackTrace(); }
      }
      
      synchronized( memPerThread ) {
         ArrayList<Fits> m = memPerThread.get(t);
         if( m==null ) {
            m=new  ArrayList<>();
            memPerThread.put(t,m);
         }
         m.add(f);
      }
   }

   // Suivi de mémoire d'un Thread particulier : retrait d'un Fits
   protected void rmFits(Thread t,Fits f) {
      
      synchronized( memPerThread ) {
         ArrayList<Fits> m = memPerThread.get(t);
         if( m==null ) return;
         m.remove(f);
      }
   }
   
   // Libère les bitmaps des Fits en cours de construction pour faire de la place
   protected long releaseBitmap() {
      long size=0L;
      
      synchronized( memPerThread ) {
         for( Thread t : memPerThread.keySet() ) {
            ArrayList<Fits> m = memPerThread.get(t);
            for( Fits f : m ) {
               try {
                  if( f.isReleasable() ) size += f.releaseBitmap();
               } catch( Exception e ) {  }
            }
         }
      }
      return size;
   }

   private String showMem() {
      try {
         StringBuilder s = new StringBuilder();
         synchronized( memPerThread ) {
            for( Thread t : memPerThread.keySet() ) {
               ArrayList<Fits> m = memPerThread.get(t);
               if( s.length()>0 ) s.append(", ");
               s.append(t.getName()+":"+m.size()+"tiles"+"/"+cds.tools.Util.getUnitDisk( getUsedMem(m) ));
            }
         }
         return s.toString();
      } catch( Exception e ) {
          return null;
      }
   }

   // Suivi de mémoire d'un Thread particulier : retourne la mémoire utilisé (en bytes)
   private long getUsedMem(ArrayList<Fits> m) {
      if( m==null ) return 0L;
      long mem=0L;
      try {
         for( Fits f : m ) mem += f.getMem();
      } catch( Exception e ) { }
      return mem;
   }

   // Mise à jour des stats
   protected void updateStat(int deltaNbThread,int deltaTile,int deltaEmptyTile,long timeTile,int deltaNodeTile,long timeNodeTile) {
      statNbTile+=deltaTile;
      statNodeTile+=deltaNodeTile;
      statEmptyTile+=deltaEmptyTile;
      if( timeTile>0 ) {
         if( statNbTile==1 || timeTile<statMinTime ) statMinTime=timeTile;
         if( statNbTile==1 || timeTile>statMaxTime ) statMaxTime=timeTile;
         if( deltaTile==1 ) {
            statTotalTime+=timeTile;
            statAvgTime = statTotalTime/statNbTile;
         }
      }
      if( timeNodeTile>0 ) {
         if( deltaNodeTile==1 ) {
            statNodeTotalTime+=timeNodeTile;
            statNodeAvgTime = statNodeTotalTime/statNodeTile;
         }
      }
   }
   
   class Item {
      int order;
      long npix;
      int z;
      Fits fits;
      boolean ready;
      Thread th;
      boolean suspendable;
      
      Item() { ready=true; order=-1; }
      
      Item(int order, long npix, int z,Thread th, boolean suspendable ) {
         this.order=order;
         this.npix=npix;
         this.z=z;
         this.th=th;
         ready=false;
         this.suspendable=suspendable;
      }
      
      protected boolean hasBeenUsed() { 
         synchronized( fifo ) { return order!=-1; }
      }
      
      private boolean isReady() { 
         synchronized( fifo ) { return ready; }
      }
      
      private Fits getFits() { 
         synchronized( fifo ) { return fits; }
      }
      
      private void setFits(Fits fits) throws Exception {
         synchronized( fifo ) { 
            ready=true;
            this.fits=fits;
         }
         if( th==null ) return;
         th.interrupt();
      }
      
      public String toString() { return order+"/"+npix+(z>0?"-"+z:"")+(isReady()?"R":"")+(suspendable?"s":""); }
      
   }
   
   protected int getBitpix0() { return context.getBitpix(); }
   
   protected void build() throws Exception {
      this.ordermax = context.getOrder();
      context.resetCounter();
      long t = System.currentTimeMillis();

      SMoc moc = new SMoc();
      SMoc m = context.getRegion();
      if( m==null ) m = new SMoc("0/0-11");
      moc.add( m );
      int minorder = context.getMinOrder();
      
      moc.setMocOrder(minorder);
      int depth = Builder.NEWCUBE ? 1: context.getDepth();
      fifo = new LinkedList<>();
      
      for( int z=0; z<depth; z++ ) {
         Iterator<Long> it = moc.valIterator();
         while( it.hasNext() ) {
            long npix=it.next();
            fifo.add( new Item(minorder, npix,z,null,true ) );
         }
      }
      
      // Initialisation des variables
      isColor = context.isColor();
      bitpix = getBitpix0();
      modeMerge = context.getModeMerge();
      modeTree = context.getModeTree();

      if( !isColor ) {
         bzero = context.getBZero();
         bscale = context.getBScale();
         blank = context.getBlank();
      }

      int nbProc = Runtime.getRuntime().availableProcessors();

      // On utilisera pratiquement toute la mémoire pour le cache
      long size = context.getMem();

      //      long maxMemPerThread = 4L * Constante.MAXOVERLAY * Constante.FITSCELLSIZE * Constante.FITSCELLSIZE * context.getNpix();
      int npixOrig = context.getNpixOrig();
      if( npixOrig<=0 ) npixOrig=4;
      
      // si partitionné => si statmax inconnue getPartitioning, sinon min(getPartitioning,statMax)
      // non partitionné => statMax, et si inconnu OrigineCELLW
      int maxWidth  = context.statMaxWidth;
      int maxHeight = context.statMaxHeight;
      int bloc = context.getPartitioning();
      if( context.isPartitioning() ) {
         maxWidth  = maxWidth==-1  ? bloc : Math.min(bloc,maxWidth);
         maxHeight = maxHeight==-1 ? bloc : Math.min(bloc,maxHeight);
      } else {
         if( maxWidth==-1 )  maxWidth = Constante.ORIGCELLWIDTH;
         if( maxHeight==-1 ) maxHeight = Constante.ORIGCELLWIDTH;
      }
      
      long bufMem =  4L * maxWidth * maxHeight * npixOrig;
      long oneRhomb = context.getTileSide()*context.getTileSide()*context.getNpix();
      long maxMemPerThread = 4*oneRhomb + bufMem;
      if( isColor )  maxMemPerThread += oneRhomb*(ordermax-ordermin);
      //      context.info("Minimal RAM required per thread (upper estimation): "+cds.tools.Util.getUnitDisk(maxMemPerThread));
      nbThread = this instanceof BuilderMirror ? 16 : (int) (size / maxMemPerThread);

      int maxNbThread = context.getMaxNbThread();
      if( maxNbThread>0 && nbThread>maxNbThread ) nbThread=maxNbThread;
      if (nbThread==0) nbThread=1;
      if( nbThread>nbProc && !(this instanceof BuilderMirror) ) nbThread=nbProc;

      Aladin.trace(4,"BuildController.build(): Found "+nbProc+" processor(s) for "+size/(1024*1024)+"MB RAM => Launch "+nbThread+" thread(s)");
      context.info("Building tiles thanks to "+nbThread+" thread"+(nbThread>1?"s":""));
      
      context.createPropForVisu();
      
      // Initialisation spécifique du Builder
      buildPre();

      // Lancement des threads de calcul
      launchThreadBuilderHpx(nbThread);

      // Attente de la fin du travail
      while( /* !stopped && */ !fifo.isEmpty() || stillWorking() ) {
         cds.tools.Util.pause(1000);
         infoInCaseOfProblem();
      }
      
      destroyThreadBuilderHpx();
//      if( stopped ) return;
      
      long duree = System.currentTimeMillis()-t;
      if( !context.isTaskAborting() ) {
         Aladin.trace(3,"Hips survey build in "+cds.tools.Util.getTemps(duree*1000L));
      }
      buildPost(duree);
    }
   
   protected void buildPre() throws Exception {
      long size = context.getMem();
      int npixOrig = context.getNpixOrig();

      // Initialisation du cache en lecture
      long limitMem = 2*size/3L;
      int limitFile = 5000;

      int maxFile;
      long maxMem;

      // Dans le cas d'un relevé JPEG ou PNG, on va utiliser un simple cache
      if( context.isColor() || !context.isPartitioning() ) {
         maxFile=limitFile;
         maxMem=limitMem;

      // Sinon, on va paramètrer le cache pour être dynamique et relativement petit au démarrage
      } else {
         maxFile = nbThread * Constante.MAXOVERLAY;
         if( maxFile<300 ) maxFile=300;
//         int bloc = context.isPartitioning() ? context.getPartitioning() : Constante.ORIGCELLWIDTH;
//         maxMem = (long)maxFile * bloc * bloc * npixOrig;
         
         // si partitionné => si statmax inconnue getPartitioning, sinon min(getPartitioning,statMax)
         int maxWidth  = context.statMaxWidth;
         int maxHeight = context.statMaxHeight;
         int bloc = context.getPartitioning();
         maxWidth  = maxWidth==-1  ? bloc : Math.min(bloc,maxWidth);
         maxHeight = maxHeight==-1 ? bloc : Math.min(bloc,maxHeight);
         maxMem = (long)maxFile * maxWidth * maxHeight * npixOrig;
      }

      CacheFits cache = new CacheFits(maxMem,maxFile,limitMem, limitFile);
      context.setCache( cache );
      context.info("Available RAM: "+cds.tools.Util.getUnitDisk(size)+" => RAM cache size: "+cache.getMaxFile()
          +" items / "+ cds.tools.Util.getUnitDisk( cache.getMaxMem()));
   }
   
   protected void buildPost(long duree) throws Exception {
      
      // Mise à jour des propriétés liées au traitement
      if( !context.isColor() ) {
         if( context.bitpix!=-1 ) context.setPropriete(Constante.KEY_HIPS_PIXEL_BITPIX,context.bitpix+"");
         if( context.bitpixOrig!=-1 ) context.setPropriete(Constante.KEY_DATA_PIXEL_BITPIX,context.bitpixOrig+"");
         context.setPropriete(Constante.KEY_HIPS_PROCESS_SAMPLING, context.isMap() ? "none" : "bilinear");
         if( context.skyvalName!=null ) {
            context.setPropriete(Constante.KEY_HIPS_SKYVAL,context.skyvalName);
            StringBuilder s1 = null;
            double cutOrig[] = context.getCutOrig();
            for( int i=0; i<4; i++ ) {
               double x = cutOrig[i];
               if( s1==null ) s1 = new StringBuilder(""+x);
               else s1.append(" "+x);
            }
            context.setPropriete(Constante.KEY_HIPS_SKYVAL_VALUE,s1.toString());
         }
      }
      context.setPropriete(Constante.KEY_HIPS_COADD,
               context.isMap() ? "none" : 
                  context.getModeOverlay().toString()+" "+
                  context.getModeMerge().toString()+" "+
                  context.getModeTree().toString() );
      
      if( !context.isTaskAborting() ) { (b=new BuilderMoc(context)).run(); b=null; }
      if( !context.isTaskAborting() ) {
         if( isTooSmallForAllsky() ) {
            context.warning("ALLSKY ignored (too small coverage). Use ALLSKY action if required"); 
            (new BuilderAllsky(context)).postJob();
         } else { (new BuilderAllsky(context)).run(); context.done("ALLSKY file done"); }
      }

      context.removeListReport();
      
      if( ThreadBuilderTile.statMaxOverlays>0 )
         context.stat("Tile overlay stats : max overlays="+ThreadBuilderTile.statMaxOverlays+", " +
               ThreadBuilderTile.statOnePass+" in one step, "+
               ThreadBuilderTile.statMultiPass+" in multi steps");
      if( context.cacheFits!=null ) {
         Aladin.trace(4,"Cache FITS status: "+ context.cacheFits);
         context.cacheFits.reset();
         context.setCache(null);
      }
      if( context.trimMem>0 ) context.stat("Tiles trim method saves "+cds.tools.Util.getUnitDisk(context.trimMem,1,2));

      infoCounter(duree);
   }

   /** Affichage des compteurs (s'ils ont été utilisés) */
   private void infoCounter(long duree) {
      
      if( duree>1000L && (context.statPixelIn>0 || context.statPixelOut>0) ) {
         long d = duree/1000L;
         context.stat("Pixel times: "+
               (context.statPixelIn==0?"":"Original images="+cds.tools.Util.getUnitDisk(context.statPixelIn).replace("B","pix") 
                  + " => "+cds.tools.Util.getUnitDisk(context.statPixelIn/d).replace("B","pix")+"/s")
               + (context.statPixelOut==0?"":
                 ("  Low tiles="+cds.tools.Util.getUnitDisk(context.statPixelOut).replace("B","pix") 
               + (context.statPixelIn==0?"":" (x"+cds.tools.Util.myRound((double)context.statPixelOut/context.statPixelIn)+")")
               + " => "+cds.tools.Util.getUnitDisk(context.statPixelOut/d).replace("B","pix")+"/s") )
               );
     
      }
   }
   
   private static final long MAXCHECKTIME = 3*60*1000L;   // 3mn
   private long lastCheckTime=-1;
   private double lastProgress=-1;
   
   // Affiche des infos de debug si rien n'a été calculé depuis un bail
   private void infoInCaseOfProblem() {
      long t = System.currentTimeMillis();
      if( t==-1 ) { lastCheckTime=t; return; }
      
      // On attend un certain temps avant de tester la progression
      if( t-lastCheckTime<MAXCHECKTIME || context.getVerbose()>=3 && t-lastCheckTime<20*1000L ) return;
      lastCheckTime=t;
//      System.out.println("Check 10s progress="+context.progress);
      
      // Premier test
      if( lastProgress==-1 ) { lastProgress=context.progress; return; }
      
      // Il y a eu du progres => c'est bon
      if( context.progress!=lastProgress ) { lastProgress=context.progress; return; }
      
      context.warning("Nothing done since a while. Here a short report to understand the problem:");
      showDebugInfo();
   }
   
   protected void showDebugInfo() {
      context.warning("DEBUG REPORT !!! (fifosize="+fifo.size()+")");
      int nbDied=0;
      Iterator<ThreadBuilder> it = threadList.iterator();
      while( it.hasNext() ) {
         ThreadBuilder tb = it.next();
         String info = tb.getInfo();
         if( tb.isDied() ) nbDied++;
         context.warning(".thread "+tb.getName()+": "+tb.getMode()+(info!=null?" => "+info:""));
      }
      if( nbDied>0 ) context.warning(".and "+nbDied+" threads DIED");
      CacheFits cache = context.getCache();
      if( cache!=null ) context.warning(cache.toString());
      else context.warning("No cache. FreeRAM="+cds.tools.Util.getUnitDisk(CacheFits.getFreeMem()));
   }
   
   /** Par défaut, il s'agit d'un simple accès à la tuile déjà calculée,
    * sauf pour BuilderMirror */
   protected Fits findLeaf(ThreadBuilderTile hpx, String file, String path,int order,long npix, int z) throws Exception { 
      return findLeaf(file);
   }
   
   /** Création d'un losange et de toute sa descendance si nécessaire.
    * Méthode récursive qui
    * 1) Vérifie si le travail n'a pas déjà été fait en se basant sur
    *    l'existance d'un fichier fits (si option keepall à vrai)
    * 2) Si order==maxOrder, calcul le losange terminal => createLeaveHpx(...)
    * 3) sinon concatène les 4 fils (appel récursif) en 1 losange => createNodeHpx(...)
    *
    * @param path  répertoire où stocker la base
    * @param sky   Nom de la base (premier niveau de répertoire dans path)
    * @param order Ordre healpix du losange de départ
    * @param maxOrder Ordre healpix max de la descendance
    * @param npix Numéro healpix du losange
    * @param z Dans le cas d'un cube, indice de la tranche en cours
    * @return Le losange
    */
   private Fits createHpx(ThreadBuilderTile hpx, String path,int order,long npix, int z) throws Exception {
      String file = Util.getFilePath(path,order,npix,z);

      // si le process a été arrêté on essaie de ressortir au plus vite
//      if (stopped) return null;
      
      // si on n'est pas dans le Moc, il faut retourner le fichier
      // pour la construction de l'arborescence...
      if( !context.isInMocTree(order,npix) ) return findLeaf(file);
      
      // si le losange a déjà été calculé on le renvoie directement
      // ou que l'on n'a pas besoin de descendre plus loin dans cette branche
      if( modeMerge==ModeMerge.mergeKeepTile ) {
         Fits oldOut = findLeaf(hpx,file,path,order,npix, z);
         if( oldOut!=null ) {
            SMoc moc = context.getRegion();
            SMoc a = new SMoc(order+"/"+npix);
            a.setSys( context.getFrameCode() );
            moc = moc.intersection(a);
            moc.setMocOrder(ordermax);
            int nbTiles = (int)moc.getNbValues();
            updateStat(0,0,nbTiles,0,nbTiles/4,0);
            return oldOut;
         }
      }

      Fits f = null;

      // Création d'un losange terminal
      if( order==ordermax )  {
         hpx.threadBuilder.setInfo("createLeavveHpx "+file+"...");
         try { f = createLeafHpx(hpx,file,path,order,npix,z); }
         catch( Exception e ) {
            hpx.threadBuilder.setInfo("createLeavveHpx error "+file+"...");
            System.err.println("BuilderTiles.createLeave error: "+file);
            e.printStackTrace();
            return null;
         }

         // Création des branches filles, et cumul des résultats
      } else {

         Fits fils[] = new Fits[4];
         Item item[] = new Item[4];
         for( int i=0; i<4; i++ ) item[i]=new Item();

         int nbDelegate=0;  // Nombre de fils délégués à d'autres threads
         for( int i =0; i<4; i++ ) {
            if( context.isTaskAborting() ) throw new Exception("Task abort !");

            // Si je peux déléguer des calculs à d'autres threads libres, je le fais
            // sauf pour au-moins une tuile fille
            if( nbDelegate<3 && fifo.isEmpty() && oneWaiting() ) {
               item[i] = addNpix(order+1,npix*4+i, z,Thread.currentThread());
               nbDelegate++;
               wakeUp();

            } else {
               hpx.threadBuilder.setInfo("CreateHpx go to next order => "+(order+1)+"/"+(npix*4+i)+"...");
               fils[i] = createHpx(hpx, path,order+1,npix*4+i, z);
            }
         }

         // Attente et récupération des fils si besoin
         while( !(item[0].isReady() && item[1].isReady() && item[2].isReady() && item[3].isReady()) ) {
            hpx.threadBuilder.setWaitingChildren(true);
            StringBuilder t = new StringBuilder();
            for( int i=0; i<item.length; i++ ) t.append( item[i].isReady()?".": (item[i].hasBeenUsed()?"x":"o"));
            hpx.threadBuilder.setInfo("CreateHpx still waiting children of "+order+"/"+npix+" ["+t+"]...");
            try { Thread.currentThread().sleep(300); } catch( Exception e ) { }
            if( context.isTaskAborting() ) throw new Exception("Task abort !");
         }
         hpx.threadBuilder.setWaitingChildren(false);
         for( int i=0; i<4; i++ ) {
            if( item[i].hasBeenUsed() ) fils[i] = item[i].getFits();
         }

         hpx.threadBuilder.setInfo("createNodeHpx "+file+"...");
         try { f = createNodeHpx(file,path,order,npix,fils,z); }
         catch( Exception e ) {
            System.err.println("BuilderTiles.createNodeHpx error: "+file);
            e.printStackTrace();
            return null;
         }
      }

      return f;
   }
   
   static final String [] MODE =  { "START","WAIT","EXEC","DIED","SUSPEND" };
   
   protected Object lockStop = new Object();
   
   // Classe des threads de calcul
   public class ThreadBuilder extends Thread {
      static final int START =0;
      static final int WAIT  =1;
      static final int EXEC  =2;
      static final int DIED  =3;
      static final int SUSPEND = 4;
      
      ThreadBuilderTile threadBuilderTile;
      private int mode=START;
      private String info;      // Message expliquant l'état courant
      private boolean encore=true;
      protected Item item=null;   // Item en cours de calcul

      public ThreadBuilder(String name,ThreadBuilderTile threadBuilderTile) {
         super(name);
         this.threadBuilderTile = threadBuilderTile;
         threadBuilderTile.threadBuilder = this;   // pour faire des remontées de debug
         Aladin.trace(3,"Creating "+getName());
      }
      
      // Pour du débogage, possiblité d'indiquer pourquoi le thread
      // se trouve dans tel ou tel état
      public String getInfo() { return info; }
      
      public String getMode() { return MODE[mode]; }

      /** Le thread est mort */
      public boolean isDied() { return mode==DIED; }

      /** Le thread travaille */
      public boolean isExec() { return mode==EXEC; }

      /** Le thread est suspendu */
      public boolean isSuspend() { return mode==SUSPEND; }
      
      
      /** Les thread est en attente du résultat d'un ou plusieurs fils */
      private boolean waitingChildren=false;
      public void setWaitingChildren( boolean flag ) { waitingChildren = flag; }
      public boolean isWaitingChildren() { return waitingChildren; }

      /** Le thread est en attente pour un nouveau boulot */
      public boolean isWait() { return mode==WAIT; }

      /** Le thread est en attente et éventuellement
       * s'il y a assez de RAM pour le réutiliser */
      public boolean isWaitingAndUsable(boolean checkMem) {
         if( mode!=WAIT ) return false;
         if( checkMem ) {
            try {
               return !threadBuilderTile.requiredMem(Constante.MAXOVERLAY,1 ); 
            } catch( Exception e ) {}
         }
         return true;
      }
      
      public void setInfo(String info) { this.info = info; }
      
      public boolean arret(String info) {
         if( !suspendable ) return false;
         mode=SUSPEND;
         this.info=info;
         return true;
      }
      public boolean reprise() { 
         if( mode!=SUSPEND ) return false;
         mode=EXEC;
         info=null;
         wakeUp();
         return true;
      }
      
      // true si le thread peut être suspendu temporairement (jamais un thread fils)
      private boolean suspendable=true;
      
      /** Demande la mort du thread */
      public void tue() { encore=false; }
      
      public void run() {

         while( encore ) {
            item=null;
            
            // arrêt du thread de calcul
            if( ThreadBuilderTile.nbThreadsToStop>0 ) {
               synchronized( lockStop ) {
                  if( ThreadBuilderTile.nbThreadsToStop>0 ) {
                     ThreadBuilderTile.nbThreadsToStop--;
                     break;
                  }
               }
            }
            
            while( encore && mode==SUSPEND ) {
               try { Thread.currentThread().sleep(100); } catch( Exception e) { };
            }
            while( encore && (item = getNextNpix())==null ) {
               mode=WAIT;
               info="No more HEALPix cell branch to compute => thread by waiting another task";
               try { Thread.currentThread().sleep(100); } catch( Exception e) { };
               if( context.isTaskAborting() ) { encore=false; break; }
            }
            if( encore ) {
//               suspendable= item.order==0; //  3;
               suspendable= item.suspendable;
               mode=EXEC;
               info=null;
               try {
                  Aladin.trace(4,Thread.currentThread().getName()+" process HEALPix cell branch "+item.order+"/"+item.npix+"...");

                  // si le process a été arrêté on essaie de ressortir au plus vite
//                  if (stopped) break;
                  if( context.isTaskAborting() ) break;

                  Fits fits = createHpx(threadBuilderTile, context.getOutputPath(), item.order, item.npix, item.z);
                  setInfo("Tile ready");
                  item.setFits(fits);
                  
                  rmFits(Thread.currentThread(), fits);
                  
                  if( item.order==3 && item.z==0 ) setProgressBar((int)item.npix);

               } catch( Throwable e ) {
                  try { item.setFits(null); } catch( Exception e1 ) {}
                  Aladin.trace(1,"*** "+Thread.currentThread().getName()+" exception !!! ("+e.getMessage()+")");
                  e.printStackTrace();
                  context.taskAbort();
               }
            }
         }

         mode=DIED;
         info="Thread died";
         rmThread(Thread.currentThread());
         //         Aladin.trace(3,Thread.currentThread().getName()+" died !");
      }
   }
   
   
   /** Mise à jour de la barre de progression en mode GUI */
   protected void setProgressBar(int npix) { context.setProgressLastNorder3(npix); }


   private Item getNextNpix() {
      Item pix=null;
      synchronized( lockObj ) {
         if( fifo.isEmpty() ) return null;
         pix = fifo.removeLast();
      }
      return pix;
   }
   
   private Item addNpix(int order, long npix,int z,Thread th) {
      Item item=new Item(order,npix,z,th,false);
      synchronized( lockObj ) { fifo.add(item); }
      return item;
   }
   
   private Object lockObj = new Object();
   
   private int threadId = 0;

   // Crée une série de threads de calcul
   private void launchThreadBuilderHpx(int nbThreads) throws Exception {

      initStat(nbThreads);
      context.createHealpixOrder(context.getTileOrder());
      ThreadBuilderTile.hasShape = new HashMap<>();

      for( int i=0; i<nbThreads; i++ ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         ThreadBuilderTile threadBuilderTile = new ThreadBuilderTile(context,this);
         ThreadBuilder t = new ThreadBuilder("Builder"+threadId++,threadBuilderTile);
         threadList.add( t );
         t.start();
         if( i==0 ) cds.tools.Util.pause(100);   // Juste pour que la fifo se remplisse plus efficacement (profondeur d'abord)
      }
   }
   
   /** Augmente le nombre de threads de calcul (notamment pour BuilderMirror) */
   protected boolean addThreadBuilderHpx(int nbThreads) throws Exception {
      
      // On ne peut à la fois ralentir et accélerer
      if( ThreadBuilderTile.nbThreadsToStop>0 ) return false;
      
      context.info("Probably not enough threads => ask to launch "+nbThreads+" thread(s) asap...");
      
      synchronized( lockObj ) {
         
         // On récupère d'éventuelles places libres ?
         for( int i=0; nbThreads>0 && i<threadList.size(); i++ ) {
            ThreadBuilder tb = threadList.get(i);
            if( tb.isDied() ) {
               ThreadBuilderTile threadBuilderTile = new ThreadBuilderTile(context,this);
               ThreadBuilder t = new ThreadBuilder(tb.getName(),threadBuilderTile);
               threadList.set(i, t);
               t.start();
               nbThreads--;
            }
         }
         if( nbThreads==0 ) return true;
         
         // On augmente la liste
         for( int i=0; i<nbThreads; i++ ) {
            if( context.isTaskAborting() ) throw new Exception("Task abort !");
            ThreadBuilderTile threadBuilderTile = new ThreadBuilderTile(context,this);
            ThreadBuilder t = new ThreadBuilder("Builder"+threadId++,threadBuilderTile);
            threadList.add( t );
            t.start();
         }
      }
      return true;
   }
   
   /** Retourne le nombre de threads actifs */
   protected int getNbThreads() {
      int nb=0;
      try {
         for( ThreadBuilder tb : threadList ) {
            if( !tb.isDied() ) nb++;
         }
      } catch( Exception e ) { return -1; }
      return nb;
   }
   
   /** Demande d'arrêt de threads de calcul (notamment pour BuilderMirror) */
   protected boolean removeThreadBuilderHpx(int nbThreads) {
      if( ThreadBuilderTile.nbThreadsToStop>0 ) return false;
      synchronized( lockObj ) {
         ThreadBuilderTile.nbThreadsToStop=nbThreads;
         context.info("Probably too many threads => ask to stop "+nbThreads+" thread(s) asap...");
      }
      return true;
   }
   
   // Demande l'arrêt de tous les threads de calcul
   void destroyThreadBuilderHpx() {
      Iterator<ThreadBuilder> it = threadList.iterator();
      while( it.hasNext() ) {
         ThreadBuilder tb = it.next();
         tb.tue();
      }
   }
   
   boolean arret(ThreadBuilderTile tbt, String info ) {
      Iterator<ThreadBuilder> it = threadList.iterator();
      while( it.hasNext() ) {
         ThreadBuilder tb = it.next();
         if( tb.threadBuilderTile==tbt ) return tb.arret(info);
      }
      return false;
   }
   
   boolean reprise(ThreadBuilderTile tbt ) {
      Iterator<ThreadBuilder> it = threadList.iterator();
      while( it.hasNext() ) {
         ThreadBuilder tb = it.next();
         if( tb.threadBuilderTile==tbt ) return tb.reprise();
      }
      return false;
   }

   int getNbThreadRunning() {
      int n=0;
      Iterator<ThreadBuilder> it = threadList.iterator();
      while( it.hasNext() ) {
         ThreadBuilder tb = it.next();
         if( tb.isExec() ) n++;
//         System.out.println(tb.getName()+" mode="+tb.getMode());
      }
      return n;
   }

   // Retourne true si au-moins un thread est encore en train de travailler
   boolean stillWorking() {
      Iterator<ThreadBuilder> it = threadList.iterator();
      while( it.hasNext() ) {
         ThreadBuilder tb = it.next();
         if( tb.isExec() || tb.isSuspend() ) return true;
      }
      return false;
   }
   
   boolean oneWaiting() {
      try {
         Iterator<ThreadBuilder> it = threadList.iterator();
         while( it.hasNext() ) if( it.next().isWaitingAndUsable(true) ) return true;
      } catch( Exception e ) { }
      return false;
   }
   
   void wakeUp() {
      try {
         Iterator<ThreadBuilder> it = threadList.iterator();
         while( it.hasNext() ) {
            ThreadBuilder tb = it.next();
            if( tb.isWaitingAndUsable(false) ) tb.interrupt();
         }
      } catch( Exception e ) {
      }
   }
   
//   static protected long time() { return 0; } //return System.currentTimeMillis(); }

   /** Création d'un losange par concaténation de ses 4 fils
    * @param file Nom du fichier complet, mais sans l'extension
    * @param path Path de la base
    * @param order Ordre Healpix du losange
    * @param npix Numéro Healpix du losange
    * @param fils les 4 fils du losange
    */
   protected Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[],int z1) throws Exception {
      long t1,t2;
      int width=context.getTileSide();
      
      boolean inTree = context.isInMocTree(order,npix);
      if( !inTree ||
            fils[0]==null && fils[1]==null && fils[2]==null && fils[3]==null) {
         if( isColor ) return null;
         Fits f = findLeaf(file);
         addFits(Thread.currentThread(),f);
         return f;
      }
      
      long t = System.currentTimeMillis();

      for( Fits f : fils ) if( f!=null ) f.reloadBitmap();

      int depth = 0;
      for( Fits f : fils ) if( f!=null ) { depth =f.depth; break; }
      
      int targetDepth=depth;
//      int targetDepth=depth/2;
//      if( targetDepth<16 ) targetDepth=depth;
      int gapZ = depth==targetDepth ? 1 : 2;
      
      // Tableaux de travail pour éviter des allocations à répétition
      double pixd[] = new double[4];
      int pixi[] = new int[4];
      int buf[] = new int[4]; 
      
      Fits out = new Fits(width,width,targetDepth,bitpix);
      if( !isColor ) {
         out.setBlank(blank);
         out.setBzero(bzero);
         out.setBscale(bscale);
      }
      Fits in;
      for( int z=0; z<targetDepth; z++ ) {
         for( int dg=0; dg<2; dg++ ) {
            for( int hb=0; hb<2; hb++ ) {
               int quad = dg<<1 | hb;
               int offX = (dg*width)>>>1;
               int offY = ((1-hb)*width)>>>1;
               in = fils[quad];

               for( int y=0; y<width; y+=2 ) {
                  for( int x=0; x<width; x+=2 ) {

                     // Couleur
                     if( isColor ) {
                        int pix=0;
                        int nbPix=0;
                        if( in!=null ) {
                           for( int i=0;i<4; i++ ) {
                              int gx = i==1 || i==3 ? 1 : 0;
                              int gy = i>1 ? 1 : 0;
                              pixi[nbPix] = in.getPixelRGBJPG(x+gx,y+gy);
                              if( (pixi[nbPix]&0xFF000000)!=0 ) {
                                 nbPix++;
                                 if( modeTree==ModeTree.treeFirst ) break;
                              }
                           }
                           if( nbPix==0 ) pix=0;  // aucune valeur => transparent
                           else {
                              if( modeTree==ModeTree.treeMean ) {
                                 pix = 0xFF000000 | getMean(pixi, nbPix,16) | getMean(pixi,nbPix,8) | getMean(pixi, nbPix,0);
                              } else if( modeTree==ModeTree.treeMedian ) {
                                 pix = 0xFF000000 | getMedian(pixi,buf,nbPix,16) | getMedian(pixi,buf,nbPix,8) | getMedian(pixi,buf,nbPix,0);
                              } else if( modeTree==ModeTree.treeMiddle ) {
                                 pix = 0xFF000000 | getMiddle(pixi,buf,nbPix,16) | getMiddle(pixi,buf,nbPix,8) | getMiddle(pixi,buf,nbPix,0);
                              } else pix = pixi[0];
                           }
                        }
                        out.setPixelRGBJPG(offX+(x>>>1), offY+(y>>>1), pix);
                        
                        // Normal
                     } else {
                        double pix=blank;
                        int nbPix=0;
                        if( in!=null ) {
                           for( int i=0;i<4; i++ ) {
                              int gx = i==1 || i==3 ? 1 : 0;
                              int gy = i>1 ? 1 : 0;
                              pixd[nbPix] = in.getPixelDouble(x+gx,y+gy,z*gapZ);
                              if( !in.isBlankPixel(pixd[nbPix]) ) {
                                 nbPix++;
                                 if( modeTree==ModeTree.treeFirst ) break;
                              }
                           }
                           if( nbPix==0 ) pix=blank;  // aucune valeur => BLANK
                           else {
                              if( modeTree==ModeTree.treeMean ) pix = getMean(pixd, nbPix);
                              else if( modeTree==ModeTree.treeMedian )  pix = getMedian(pixd, nbPix);
                              else pix = pixd[0];
                           }
                        }
                        out.setPixelDouble(offX+(x>>>1), offY+(y>>>1), z, pix);
                     }
                  }
               }
            }
         }
      }
      
      if( !isColor && modeMerge!=ModeMerge.mergeOverwriteTile && modeMerge!=ModeMerge.mergeKeepTile ) {
         Fits oldOut = findLeaf(file);
         if( oldOut!=null ) {
            out.mergeOnNaN(oldOut);
        }
      }

      context.updateHeader(out,order,npix);
      write(file,out);

      long duree = System.currentTimeMillis() -t;
      if( npix%10 == 0 || DEBUG ) Aladin.trace(4,Thread.currentThread().getName()+".createNodeHpx("+order+"/"+npix+") "+modeMerge+" in "+duree+"ms");

      updateStat(0,0,0,0,1,duree);

      for( int i=0; i<4; i++ ) {
         if( fils[i]!=null ) {
            rmFits(Thread.currentThread(), fils[i]);
            fils[i].free();
            fils[i]=null;
         }
      }

      addFits(Thread.currentThread(),out);
      return out;
   }
   
   final protected double getMiddle(double px[], int nbpix ) {
      Arrays.sort(px,0,nbpix);
      return nbpix>=3 ? px[1] : px[0];
   }
   
   final protected double getMedian(double px[], int nbpix ) {
      Arrays.sort(px,0,nbpix);
      return nbpix==4 ? (px[1]/2+px[2]/2) : nbpix==3 ? px[1] : nbpix==2 ? (px[0]/2+px[1]/2) : px[0];
   }

   final protected double getMean(double px[], int nbpix ) {
      double pix=0;
      for( int i=0; i<nbpix; i++ ) pix += px[i]/nbpix;
      return pix;
   }

   final private int getMedian(int p[], int buf[], int nbpix, int shiftRgb ) {
      for( int i=0; i<nbpix; i++ ) buf[i]= (p[i]>>shiftRgb)&0xFF;
      Arrays.sort(buf,0,nbpix);
      return (nbpix==4 ? (buf[1]+buf[2])/2 : nbpix==3 ? buf[1] : nbpix==2 ? (buf[0]+buf[1])/2 : buf[0] ) <<shiftRgb;
   }

   final private int getMiddle(int p[], int buf[], int nbpix, int shiftRgb ) {
      for( int i=0; i<nbpix; i++ ) buf[i]= (p[i]>>shiftRgb)&0xFF;
      Arrays.sort(buf,0,nbpix);
      return (nbpix>=4 ? buf[1]: buf[0] ) <<shiftRgb;
   }

   final private int getMean(int px[], int nbpix, int shiftRgb  ) {
      int pix=0;
      for( int i=0; i<nbpix; i++ ) pix += (px[i]>>shiftRgb)&0xFF;
      return (pix/nbpix)<<shiftRgb;
   }

   protected void write(String file, Fits out) throws Exception {
      String filename = file+context.getTileExt();
      if( isColor ) out.writeCompressed(filename,0,0,null, Constante.TILE_MODE[ context.targetColorMode ]);
      else {
         if( context.trim ) {
            long mem = out.getMem();
            Fits nout = out.trimFactory();
            if( nout!=null ) {
               out.setReleasable(false);
               long mem1 = nout.getMem();
               context.trimMem+=(mem-mem1)/1024L;
               out=nout;
            }
         } 
         out.addDataSum();
         out.writeFITS(filename,context.gzip);
      }
   }

   /** Création d'un losange terminal
    * @param file Nom du fichier de destination (complet mais sans l'extension)
    * @param order Ordre healpix du losange
    * @param npix Numéro Healpix du losange
    * @param z numéro de la frame (pour un cube)
    * @return null si rien trouvé pour construire ce fichier
    */
   protected Fits createLeafHpx(ThreadBuilderTile hpx, String file,String path, int order,long npix,int z) throws Exception {
      long t = System.currentTimeMillis();

      Fits oldOut=null;

      boolean isInList = context.isInMoc(order,npix);

      if( !isInList && modeMerge!=ModeMerge.mergeOverwriteTile ) {
         oldOut = findLeaf(file);
         if( !(oldOut==null && context.isMocDescendant(order,npix) ) ) {
            addFits(Thread.currentThread(), oldOut);
            return oldOut;
         }
      }

      Fits out= hpx.buildHealpix(this,path, order, npix,z);
      
      if( out !=null  ) {

         if( modeMerge!=ModeMerge.mergeOverwriteTile ) {
            if( oldOut==null ) oldOut = findLeaf(file);
            if( oldOut!=null && modeMerge==ModeMerge.mergeKeepTile ) {
               out=null;
               addFits(Thread.currentThread(), oldOut);
               return oldOut;
            }
            if( oldOut!=null ) {
               // Dans le cas integer, si le losange déjà calculé n'a pas de BLANK indiqué, on utilisera
               // celui renseigné par l'utilisateur, et sinon celui par défaut
               if( oldOut.bitpix>0 && Double.isNaN(oldOut.blank)) oldOut.setBlank(blank);

               if( modeMerge==ModeMerge.mergeMean  ) out.coadd(oldOut,Fits.AVG);
               else if( modeMerge==ModeMerge.mergeAdd ) out.coadd(oldOut,Fits.ADD);
               else if( modeMerge==ModeMerge.mergeOverwrite ) out.mergeOnNaN(oldOut);
               else if( modeMerge==ModeMerge.mergeKeep ) {
                  oldOut.mergeOnNaN(out);
                  out=oldOut;
                  oldOut=null;
               }
            }
         }
      }

      long duree;
      if (out!=null) {
         writeLeaf(out,file,order,npix);
         hpx.threadBuilder.setInfo("createLeavveHpx write done "+file+"...");
         duree = System.currentTimeMillis()-t;
         updateStat(0,1,0,duree,0,0);

      } else {
         duree = System.currentTimeMillis()-t;
         updateStat(0,0,1,duree,0,0);
      }

      addFits(Thread.currentThread(), out);
      return out;
   }
   
   protected void writeLeaf(Fits out,String file,int order,long npix) throws Exception {
      context.updateHeader(out,order,npix);
      write(file,out);
      context.addPixelOut( out.getNbPix() );
   }
   
   protected String getTileExt() { return context.getTileExt(); }
   
   /** Recherche et chargement d'un losange déjà calculé
    *  Retourne null si non trouvé
    * @param file Nom du fichier ( sans extension)
    */
   public Fits findLeaf(String file) throws Exception {
      String filename = file + getTileExt();
      File f = new File(filename);
      if( !f.exists() ) return null;
      Fits out = new Fits();
      MyInputStream is = null;
      try {
         is = new MyInputStream( new FileInputStream(f)); //,MyInputStream.UNKNOWN,false);
         loadLeaf(out,is);
         out.setFilename(filename);
      }
      catch( Exception e ) { return null; }
      finally { if( is!=null ) is.close(); }
      return out;
   }
   
   protected void loadLeaf(Fits out,MyInputStream is) throws Exception {
      if( isColor ) out.loadPreview(is, true);
      else out.loadFITS(is);
   }

}

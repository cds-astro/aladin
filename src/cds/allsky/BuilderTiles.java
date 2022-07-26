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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
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
 * @author Standard Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderTiles extends Builder {

   private boolean isColor;
   protected int bitpix;
   protected double bzero;
   protected double bscale;
   protected double blank;
   private Context.JpegMethod method;

   // Liste des Threads de calcul
   protected ArrayList<ThreadBuilder> threadList = new ArrayList<>();
   private Mode coaddMode=Mode.REPLACETILE;

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
//   protected int statNbThreadRunning=-1;     // Nombre de Thread qui sont en train de calculer
//   protected int statNbThread;               // Nombre max de Thread de calcul
   protected int statNbTile;                 // Nombre de tuiles terminales déjà calculés
   protected long statMinTime,statMaxTime,statTotalTime,statAvgTime;
   protected int statEmptyTile;               // Nombre de tuiles terminales de fait vides
   protected int statNodeTile;                 // Nombre de tuiles "intermédiaires" déjà calculés
   protected long statNodeTotalTime,statNodeAvgTime;
   protected long startTime;                 // Date de lancement du calcul
   protected long totalTime;                 // Temps depuis le début du calcul

   public BuilderTiles(Context context) { super(context); }

   public Action getAction() { return Action.TILES; }


   public void run() throws Exception {
      context.info("Creating "+context.getTileExt()+" tiles and allsky (max depth="+context.getOrder()+")...");
      context.info("sky area to process: "+context.getNbLowCells()+" low level HEALPix cells");
      
      context.resetCheckCode( context.getTileExt());

      // Un peu de baratin
      if( !context.isColor() ) {
         int b0=context.getBitpixOrig(), b1=context.getBitpix();
         if( b0!=b1 ) {
            context.info("BITPIX conversion from "+context.getBitpixOrig()+" to "+context.getBitpix());
            double cutOrig[] = context.getCutOrig();
            double cut[] = context.getCut();
            context.info("Map original raw pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"] to ["+cut[2]+" .. "+cut[3]+"]");
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
         context.info("Tile aggregation method="+Context.JpegMethod.MEAN);
         if( context.live ) context.info("Live HiPS => Weight tiles saved for potential future additions"); 
      }

      build();
      
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
      context.setPropriete(Constante.KEY_HIPS_PROCESS_OVERLAY,
               context.isMap() ? "none" : 
               context.mode==Mode.ADD ? "add" :
               context.mode==Mode.SUM ? "cumul" :
               context.fading ? "border_fading" : 
               context.mixing ? "mean" : "first");
      context.setPropriete(Constante.KEY_HIPS_PROCESS_HIERARCHY, context.getJpegMethod().toString().toLowerCase());
      
      if( !context.isTaskAborting() ) { (new BuilderAllsky(context)).run(); context.done("ALLSKY file done"); }
      if( !context.isTaskAborting() ) { (b=new BuilderMoc(context)).run(); b=null; }
      
      context.removeListReport();
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
            memoCutOrig = new double[4];
            for( int i=0; i<4; i++ ) {
               if( Double.isNaN(cutOrigBefore[i]) ) continue;
               memoCutOrig[i] = (cutOrigBefore[i] - context.bZeroOrig)/context.bScaleOrig;
            }
         }

         // repositionnement des cuts et blank passés par paramètre
         double [] cutOrig = context.getCutOrig();
         double bs = context.bScaleOrig;
         double bz = context.bZeroOrig;
         if( memoCutOrig!=null ) {
            if( memoCutOrig[0]!=0 || memoCutOrig[1]!=0 ) { cutOrig[0]=memoCutOrig[0]; cutOrig[1]=memoCutOrig[1]; }
            if( memoCutOrig[2]!=0 || memoCutOrig[3]!=0 ) { cutOrig[2]=memoCutOrig[2]; cutOrig[3]=memoCutOrig[3]; }
            context.setCutOrig(cutOrig);
         }

         if( cutOrig[0]==cutOrig[1] ) context.warning("BAD PIXEL CUT: ["+ip(cutOrig[0],bz,bs)+" .. "+ip(cutOrig[1],bz,bs)+"] => YOU WILL HAVE TO CHANGE/EDIT THE properties FILE VALUES");

         context.setValidateCut(true);

         if( hasAlternateBlank ) context.setBlankOrig(blankOrig);

         context.initParameters();
         context.info("Data range ["+ip(cutOrig[2],bz,bs)+" .. "+ip(cutOrig[3],bz,bs)+"], pixel cut ["+ip(cutOrig[0],bz,bs)+" .. "+ip(cutOrig[1],bz,bs)+"]");

      } else context.initParameters();


      if( !context.verifCoherence() ) throw new Exception("Uncompatible pre-existing HiPS survey");
      if( !context.isColor() && context.getBScale()==0 ) throw new Exception("Big bug => BSCALE=0 !! please contact CDS");


      // Info sur la méthode
      Mode mode = context.getMode();
      if( mode==Mode.MUL || mode==Mode.DIV || mode==Mode.COPY || mode==Mode.LINK ) {
         throw new Exception("Coadd mode ["+mode+"] not supported for TILES action");
      }
      context.info("Coadd mode: "+Mode.getExplanation(mode));
      
      // Info sur le coordinate frame
      context.info("HiPS coordinate frame => "+context.getFrameName());
      
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

   //   private long maxMem=0;

   // Libère de la mémoire non indispensable si besoin
   //   private void adjustMem() {
   //      if( maxMem==0 ) return;
   //      long diff=0L;
   //      long totMem=0L;
   //      long maxMemPerThread = maxMem/memPerThread.size();
   //      for( Long key : memPerThread.keySet() ) {
   //         ArrayList<Fits> m = memPerThread.get(key);
   //         if( m==null ) continue;
   //         long mem=0L;
   //         for( int i=m.size()-1; i>=0; i--) {
   //            Fits f = m.get(i);
   //            long m1 = f.getMem();
   //            if( mem>maxMemPerThread ) {
   //               try { f.releaseBitmap(); } catch( Exception e ) { }
   //               long m2= f.getMem();
   //               diff += m1-m2;
   //               m1=m2;
   //            }
   //            mem += m1;
   //         }
   //         totMem+=mem;
   //      }
   //      System.out.println("adjustMem: maxMem="+cds.tools.Util.getUnitDisk(maxMem)+" mem="+cds.tools.Util.getUnitDisk(totMem)+" diff="+cds.tools.Util.getUnitDisk(diff));
   //   }

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

   private long getUsedMem() {
      long mem=0L;
      try {
         synchronized( memPerThread ) {
            for( Thread t : memPerThread.keySet() ) {
               ArrayList<Fits> m = memPerThread.get(t);
               mem += getUsedMem(m);
            }
         }
      } catch( Exception e ) { }
      return mem;
   }

   private String showMem() {
      try {
         StringBuffer s = new StringBuffer();
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
//      statNbThreadRunning+=deltaNbThread;
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
      long t = System.currentTimeMillis();

      SMoc moc = new SMoc();
      SMoc m = context.getRegion();
      if( m==null ) m = new SMoc("0/0-11");
      moc.add( m );
//      int minorder=0; //3;
//      context.setMinOrder( minorder );
      int minorder = context.getMinOrder();
      
      moc.setMocOrder(minorder);
      int depth = context.getDepth();
      fifo = new LinkedList<>();
      
//      Iterator<Long> it = moc.pixelIterator();
//      while( it.hasNext() ) {
//         long npix=it.next();
//         for( int z=0; z<depth; z++ ) {
//            fifo.add( new Item(3, npix,z,null ) );
//         }
//      }
      
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
      coaddMode = context.getMode();
      method = context.getJpegMethod();

      if( !isColor ) {
         bzero = context.getBZero();
         bscale = context.getBScale();
         blank = context.getBlank();
      }

      int nbProc = Runtime.getRuntime().availableProcessors();

      // On utilisera pratiquement toute la mémoire pour le cache
      long size = context.getMem();

      //      long maxMemPerThread = 4L * Constante.MAXOVERLAY * Constante.FITSCELLSIZE * Constante.FITSCELLSIZE * context.getNpix();
      long bufMem =  4L * Constante.ORIGCELLWIDTH * Constante.ORIGCELLWIDTH * context.getNpixOrig();
      long oneRhomb = context.getTileSide()*context.getTileSide()*context.getNpix();
      long maxMemPerThread = 4*oneRhomb + bufMem;
      if( isColor )  maxMemPerThread += oneRhomb*(ordermax-ordermin);
      //      context.info("Minimal RAM required per thread (upper estimation): "+cds.tools.Util.getUnitDisk(maxMemPerThread));
      int nbThread = this instanceof BuilderMirror ? 16 : (int) (size / maxMemPerThread);


      int maxNbThread = context.getMaxNbThread();
      if( maxNbThread>0 && nbThread>maxNbThread ) nbThread=maxNbThread;
      if (nbThread==0) nbThread=1;
      if( nbThread>nbProc && !(this instanceof BuilderMirror) ) nbThread=nbProc;

      Aladin.trace(4,"BuildController.build(): Found "+nbProc+" processor(s) for "+size/(1024*1024)+"MB RAM => Launch "+nbThread+" thread(s)");
      context.info("Starts with "+nbThread+" thread"+(nbThread>1?"s":""));

      activateCache(size,2L*size/3L);

      // Lancement des threads de calcul
      launchThreadBuilderHpx(nbThread);

      // Attente de la fin du travail
      while( /* !stopped && */ !fifo.isEmpty() || stillWorking() ) {
         cds.tools.Util.pause(1000);
         infoInCaseOfProblem();
      }
      
      destroyThreadBuilderHpx();
//      if( stopped ) return;
      
      if( !context.isTaskAborting() ) {

         if( ThreadBuilderTile.statMaxOverlays>0 )
            context.stat("Tile overlay stats : max overlays="+ThreadBuilderTile.statMaxOverlays+", " +
                  ThreadBuilderTile.statOnePass+" in one step, "+
                  ThreadBuilderTile.statMultiPass+" in multi steps");
         if( context.cacheFits!=null ) Aladin.trace(3,"Cache FITS status: "+ context.cacheFits);
         Aladin.trace(3,"Healpix survey build in "+cds.tools.Util.getTemps((System.currentTimeMillis()-t)*1000L));
      }

      if( context.cacheFits!=null ) context.cacheFits.reset();
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
   
   protected void activateCache(long size,long sizeCache) {
      context.setCache(new CacheFits(sizeCache));
      context.info("Available RAM: "+cds.tools.Util.getUnitDisk(size)+" => Cache size: "+cds.tools.Util.getUnitDisk(sizeCache));
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
      // ou que l'on n'a pas besoin de descendre plus loin dans c"ette branche
      if( coaddMode==Mode.KEEPTILE ) {
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
         try { f = createLeaveHpx(hpx,file,path,order,npix,z); }
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

      // On soulage la mémoire RAM des losanges qui ne vont pas servir tout de suite
      // on les relira lorsqu'on en aura besoin dans createNodeHpx(...)
//      if( f!=null && f.isReleasable() ) f.releaseBitmap();
      return f;
   }
   
//   // Génération des 4 fils en parallèle
//   private void multiThreadCreateHpx(ThreadBuilderTile hpx, Fits [] fils, String path,int order,long npix, int z) throws Exception {
//      Item [] item = new Item[3];
//      
//      // Ajout des travaux dans la file d'attente des threads de travail
//      for( int i=0; i<3; i++ ) {
//         item[i] = addNpix(order+1,npix*4+i, z,Thread.currentThread());
//      }
//      
//      // Il faudrait ici réveiller les threads de calculs en attente
//      wakeUp();
//            
//      // Un des fils est calculé par le thread courant lui-même
//      fils[3] = createHpx(hpx, path,order+1,npix*4+3, z);
//      
//      // Attente et récupération des fils
//      while( !(item[0].isReady() && item[1].isReady() && item[2].isReady()) ) {
//         hpx.threadBuilder.setWaitingChildren(true);
//         StringBuilder t = new StringBuilder();
//         for( int i=0; i<item.length; i++ ) t.append( item[i].isReady()?".":"x");
//         hpx.threadBuilder.setInfo("multiThreadCreateHpx still waiting children of "+order+"/"+npix+" ["+t+"]...");
//         try { Thread.currentThread().sleep(100); } catch( Exception e ) { }
//         if( context.isTaskAborting() ) throw new Exception("Task abort !");
//      }
//      hpx.threadBuilder.setWaitingChildren(false);
//      for( int i=0; i<3; i++ ) fils[i] = item[i].getFits();
//   }
   
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
//      ThreadBuilderTile.nbThreads=nbThreads;
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
//         int lastnbThreads = ThreadBuilderTile.nbThreads;
//         ThreadBuilderTile.nbThreads+=nbThreads;

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

   /** Création d'un losange par concaténation de ses 4 fils
    * @param file Nom du fichier complet, mais sans l'extension
    * @param path Path de la base
    * @param order Ordre Healpix du losange
    * @param npix Numéro Healpix du losange
    * @param fils les 4 fils du losange
    */
   protected Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[],int z) throws Exception {
      long t = System.currentTimeMillis();
      int w=context.getTileSide();
      double px[] = new double[4];


      boolean inTree = context.isInMocTree(order,npix);
      if( !inTree ||
            fils[0]==null && fils[1]==null && fils[2]==null && fils[3]==null) {
         if( isColor ) return null;
         Fits f = findLeaf(file);
         addFits(Thread.currentThread(),f);
         return f;
      }

      for( Fits f : fils ) if( f!=null ) f.reloadBitmap();

      Fits out = new Fits(w,w,bitpix);
      if( !isColor ) {
         out.setBlank(blank);
         out.setBzero(bzero);
         out.setBscale(bscale);
      }
      Fits in;
      for( int dg=0; dg<2; dg++ ) {
         for( int hb=0; hb<2; hb++ ) {
            int quad = dg<<1 | hb;
            int offX = (dg*w)>>>1;
            int offY = ((1-hb)*w)>>>1;
      in = fils[quad];

      for( int y=0; y<w; y+=2 ) {
         for( int x=0; x<w; x+=2 ) {

            // Couleur
            if( isColor ) {

               int pix=0;
               if( in!=null ) {
                  if( method==Context.JpegMethod.MEAN ) {
                     int pixR=0,pixG=0,pixB=0;
                     int nbPix=0;
                     for( int i=0;i<4; i++ ) {
                        int gx = i==1 || i==3 ? 1 : 0;
                        int gy = i>1 ? 1 : 0;
                        int p = in.getPixelRGBJPG(x+gx,y+gy);
                        int alpha = (p>>24)&0xFF;
                        if( alpha!=0 ) {
                           nbPix++;
                           pixR += (p>>16)&0xFF;
                           pixG += (p>>8)&0xFF;
                           pixB += p&0xFF;
                        }
                     }
                     if( nbPix!=0 ) pix= 0xFF000000 | ((pixR/nbPix)<<16) | ((pixG/nbPix)<<8) | (pixB/nbPix);

                  } else if( method==Context.JpegMethod.MEDIAN ) {
                     int pixR[]=new int[4], pixG[]=new int[4], pixB[]=new int[4];
                     int nbPix=0;
                     for( int i=0;i<4; i++ ) {
                        int gx = i==1 || i==3 ? 1 : 0;
                        int gy = i>1 ? 1 : 0;
                        int p = in.getPixelRGBJPG(x+gx,y+gy);
                        int alpha = (p>>24)&0xFF;
                        if( alpha!=0 ) {
                           nbPix++;
                           pixR[i] = (p>>16)&0xFF;
                           pixG[i] = (p>>8)&0xFF;
                           pixB[i] = p&0xFF;
                        }
                     }
                     if( nbPix!=0 ) {
                        int pR,pB,pG;

                        // Mediane en Red
                        if( pixR[0]>pixR[1] && (pixR[0]<pixR[2] || pixR[0]<pixR[3]) || pixR[0]<pixR[1] && (pixR[0]>pixR[2] || pixR[0]>pixR[3]) ) pR=pixR[0];
                        else if( pixR[1]>pixR[0] && (pixR[1]<pixR[2] || pixR[1]<pixR[3]) || pixR[1]<pixR[0] && (pixR[1]>pixR[2] || pixR[1]>pixR[3]) ) pR=pixR[1];
                        else if( pixR[2]>pixR[0] && (pixR[2]<pixR[1] || pixR[2]<pixR[3]) || pixR[2]<pixR[0] && (pixR[2]>pixR[1] || pixR[2]>pixR[3]) ) pR=pixR[2];
                        else pR=pixR[3];

                        // Mediane en Green
                        if( pixG[0]>pixG[1] && (pixG[0]<pixG[2] || pixG[0]<pixG[3]) || pixG[0]<pixG[1] && (pixG[0]>pixG[2] || pixG[0]>pixG[3]) ) pG=pixG[0];
                        else if( pixG[1]>pixG[0] && (pixG[1]<pixG[2] || pixG[1]<pixG[3]) || pixG[1]<pixG[0] && (pixG[1]>pixG[2] || pixG[1]>pixG[3]) ) pG=pixG[1];
                        else if( pixG[2]>pixG[0] && (pixG[2]<pixG[1] || pixG[2]<pixG[3]) || pixG[2]<pixG[0] && (pixG[2]>pixG[1] || pixG[2]>pixG[3]) ) pG=pixG[2];
                        else pG=pixG[3];

                        // Médiane en Blue
                        if( pixB[0]>pixB[1] && (pixB[0]<pixB[2] || pixB[0]<pixB[3]) || pixB[0]<pixB[1] && (pixB[0]>pixB[2] || pixB[0]>pixB[3]) ) pB=pixB[0];
                        else if( pixB[1]>pixB[0] && (pixB[1]<pixB[2] || pixB[1]<pixB[3]) || pixB[1]<pixB[0] && (pixB[1]>pixB[2] || pixB[1]>pixB[3]) ) pB=pixB[1];
                        else if( pixB[2]>pixB[0] && (pixB[2]<pixB[1] || pixB[2]<pixB[3]) || pixB[2]<pixB[0] && (pixB[2]>pixB[1] || pixB[2]>pixB[3]) ) pB=pixB[2];
                        else pB=pixB[3];

                        pix = 0xFF000000 | (pR<<16) | (pG<<8) | pB;
                     }


                  } else {
                     pix = in.getPixelRGBJPG(x,y);

                  }
               }

               out.setPixelRGBJPG(offX+(x>>>1), offY+(y>>>1), pix);

               // Normal
            } else {

               // On prend la moyenne des 4
               double pix=blank;
               int nbPix=0;
               if( in!=null ) {
                  for( int i=0;i<4; i++ ) {
                     int gx = i==1 || i==3 ? 1 : 0;
                     int gy = i>1 ? 1 : 0;
                     px[i] = in.getPixelDouble(x+gx,y+gy);
                     if( !in.isBlankPixel(px[i]) ) nbPix++;
                     //                     if( !Double.isNaN(px[i]) && px[i]!=blank ) nbPix++;
                  }
                  if( nbPix==0 ) pix=blank;  // aucune valeur => BLANK
                  else {
                     pix=0;
                     for( int i=0; i<4; i++ ) {
                        if( !in.isBlankPixel(px[i]) ) pix += (px[i]/nbPix);
                        //                        if( !Double.isNaN(px[i]) && px[i]!=blank ) pix+=px[i]/nbPix;
                     }
                  }
               }
               out.setPixelDouble(offX+(x>>>1), offY+(y>>>1), pix);
            }
         }
      }
         }
      }

      if( !isColor && coaddMode!=Mode.REPLACETILE && coaddMode!=Mode.KEEPTILE ) {
         Fits oldOut = findLeaf(file);
         if( oldOut!=null ) out.mergeOnNaN(oldOut);
      }

      context.updateHeader(out,order,npix);
      write(file,out);

      long duree = System.currentTimeMillis() -t;
      if( npix%10 == 0 || DEBUG ) Aladin.trace(4,Thread.currentThread().getName()+".createNodeHpx("+order+"/"+npix+") "+coaddMode+" in "+duree+"ms");

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

   protected void write(String file, Fits out) throws Exception {
      String filename = file+context.getTileExt();
      if( isColor ) out.writeCompressed(filename,0,0,null, Constante.TILE_MODE[ context.targetColorMode ]);
      else {
         out.addDataSum();
         out.writeFITS(filename);
      }
   }

   /** Création d'un losange terminal
    * @param file Nom du fichier de destination (complet mais sans l'extension)
    * @param order Ordre healpix du losange
    * @param npix Numéro Healpix du losange
    * @param z numéro de la frame (pour un cube)
    * @return null si rien trouvé pour construire ce fichier
    */
   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,String path, int order,long npix,int z) throws Exception {
      long t = System.currentTimeMillis();

      Fits oldOut=null;

      boolean isInList = context.isInMoc(order,npix);

      if( !isInList && coaddMode!=Mode.REPLACETILE ) {
         oldOut = findLeaf(file);
         if( !(oldOut==null && context.isMocDescendant(order,npix) ) ) {
            addFits(Thread.currentThread(), oldOut);
            return oldOut;
         }
      }

      Fits out= hpx.buildHealpix(this,path, order, npix,z);

      if( out !=null  ) {

         if( coaddMode!=Mode.REPLACETILE ) {
            if( oldOut==null ) oldOut = findLeaf(file);
            if( oldOut!=null && coaddMode==Mode.KEEPTILE ) {
               out=null;
               addFits(Thread.currentThread(), oldOut);
               return oldOut;
            }
            if( oldOut!=null ) {
               // Dans le cas integer, si le losange déjà calculé n'a pas de BLANK indiqué, on utilisera
               // celui renseigné par l'utilisateur, et sinon celui par défaut
               if( oldOut.bitpix>0 && Double.isNaN(oldOut.blank)) oldOut.setBlank(blank);

               if( coaddMode==Mode.AVERAGE ) out.coadd(oldOut,Fits.AVG);
               else if( coaddMode==Mode.ADD 
                     || coaddMode==Mode.SUM ) out.coadd(oldOut,Fits.ADD);
               else if( coaddMode==Mode.OVERWRITE ) out.mergeOnNaN(oldOut);
               else if( coaddMode==Mode.KEEP ) {
                  oldOut.mergeOnNaN(out);
                  out=oldOut;
                  oldOut=null;
               }
            }
         }
      }

      long duree;
      if (out!=null) {
         context.updateHeader(out,order,npix);
         write(file,out);
         hpx.threadBuilder.setInfo("createLeavveHpx write done "+file+"...");
         duree = System.currentTimeMillis()-t;
         //         if( npix%10 == 0 || DEBUG ) Aladin.trace(4,Thread.currentThread().getName()+".createLeaveHpx("+order+"/"+npix+") "+coaddMode+" in "+duree+"ms");
         updateStat(0,1,0,duree,0,0);

      } else {
         duree = System.currentTimeMillis()-t;
         updateStat(0,0,1,duree,0,0);
      }

      addFits(Thread.currentThread(), out);
      return out;
   }
   
   /** Recherche et chargement d'un losange déjà calculé
    *  Retourne null si non trouvé
    * @param file Nom du fichier ( sans extension)
    */
   public Fits findLeaf(String file) throws Exception {
      String filename = file + context.getTileExt();
      File f = new File(filename);
      if( !f.exists() ) return null;
      Fits out = new Fits();
      MyInputStream is = null;
      try {
         is = new MyInputStream( new FileInputStream(f));
         if( isColor ) out.loadPreview(is, true);
         else out.loadFITS(is);
         out.setFilename(filename);
      }
      catch( Exception e ) { return null; }
      finally { if( is!=null ) is.close(); }
      return out;
   }

}

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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/** Permet la génération du survey HEALPix à partir d'un index préalablement généré
 * @author Standard Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderTiles extends Builder {

   private boolean isColor;
   private int bitpix;
   private double bZero;
   private double bScale;
   private double blank;

   // Liste des Threads de calcul
   private ArrayList<ThreadBuilder> threadList = new ArrayList<ThreadBuilder>();
   private CoAddMode coaddMode=CoAddMode.REPLACETILE;
   
   protected int ordermin = 3;
   protected long nummin = 0;
   protected long nummax = 0;
   protected ArrayList<Long> npix_list;
   protected double automin = 0;
   protected double automax = 0;

   public static boolean DEBUG = true;

   public static String FS;
   private int NCURRENT = 0;
   private int ordermax;
   private boolean stopped = false;

   static { FS = System.getProperty("file.separator"); }

   // Pour les stat
   private int statNbThreadRunning=-1;     // Nombre de Thread qui sont en train de calculer
   private int statNbThread;               // Nombre max de Thread de calcul
   private int statNbTile;                 // Nombre de tuiles terminales déjà calculés
   private long statMinTime,statMaxTime,statTotalTime,statAvgTime;
   private int statEmptyTile;               // Nombre de tuiles terminales de fait vides
   private int statNodeTile;                 // Nombre de tuiles "intermédiaires" déjà calculés
   private long statNodeTotalTime,statNodeAvgTime;
   private long startTime;                 // Date de lancement du calcul
   private long totalTime;                 // Temps depuis le début du calcul

   public BuilderTiles(Context context) { super(context); }

   public Action getAction() { return Action.TILES; }


   public void run() throws Exception {
      String mode = context.isColor() ? "JPEG":"FITS";
      context.running("Creating "+mode+" tiles and allsky (max depth="+context.getOrder()+")...");
      context.info("sky area to process: "+context.getNbLowCells()+" low level HEALPix cells");

      // Un peu de baratin
      if( !context.isColor() ) {
         int b0=context.getBitpixOrig(), b1=context.getBitpix();
         if( b0!=b1 ) {
            context.info("BITPIX conversion from "+context.getBitpixOrig()+" to "+context.getBitpix());
            double cutOrig[] = context.getCutOrig();
            double cut[] = context.getCut();
            context.info("Map original pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"] to ["+cut[2]+" .. "+cut[3]+"]");
         }
         else context.info("BITPIX = "+b1+" (no conversion)");
         if( context.getDiskMem()!=-1 ) {
            context.info("Disk requirement (upper approximation) : "+cds.tools.Util.getUnitDisk(context.getDiskMem()*1.25));
         }
         double bs=context.getBScale(), bz=context.getBZero();
         if( bs!=1 || bz!=0 ) { context.info("BSCALE="+bs+" BZERO="+bz); }
         double bl0 = context.getBlankOrig();
         double bl1 = context.getBlank();
         if( context.hasAlternateBlank() ) context.info("BLANK conversion from "+(Double.isNaN(bl0)?"NaN":bl0)+" to "+(Double.isNaN(bl1)?"NaN":bl1));
         else context.info("BLANK="+ (Double.isNaN(bl1)?"NaN":bl1));
      }
      build();
      if( !context.isTaskAborting() ) (new BuilderAllsky(context)).run();
      if( !context.isTaskAborting() ) (new BuilderMoc(context)).run();
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
      }
      catch(Exception e) {
          context.warning(e.getMessage());
          // retry order validation with tiles directory
          validateOrder(context.getOutputPath());
      }
      
      String img = context.getImgEtalon();
      if( img==null ) img = context.justFindImgEtalon( context.getInputPath() );
      
      // Image de référence en couleur => pas besoin de plus
//      if(  context.isColor() ) { context.initRegion(); return; }
      if(  !context.isColor() ) { 

         // mémorisation des cuts et blank positionnés manuellement
         double [] memoCutOrig = context.getCutOrig();
         boolean hasAlternateBlank = context.hasAlternateBlank();
         double blankOrig = context.getBlankOrig();
         int bitpixOrig = context.getBitpixOrig();

         // Image étalon à charger obligatoirement pour BSCALE, BZERO, BITPIX et BLANK
         //      String img = context.getImgEtalon();
         //      if( img==null ) img = context.justFindImgEtalon( context.getInputPath() );
         if( img==null ) throw new Exception("No source image found in "+context.getInputPath());
         context.info("Reference image: "+img);
         try { context.setImgEtalon(img); }
         catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }

         //      // Image de référence en couleur
         //      if(  context.getBitpixOrig()==0 ) {
         //         context.initRegion();
         //         return;
         //      }

         if( bitpixOrig==-1 ) {
            context.info("BITPIX found in the reference image => "+context.getBitpixOrig());
         } else if( bitpixOrig!=context.getBitpixOrig() ) {
            context.warning("The provided BITPIX (" +bitpixOrig+ ") is different than the original one (" + context.getBitpixOrig() + ") => bitpix conversion will be applied");
            context.setBitpixOrig(bitpixOrig);
         }

         // repositionnement des cuts et blank passés par paramètre
         double [] cutOrig = context.getCutOrig();
         if( memoCutOrig!=null ) {
            if( memoCutOrig[0]!=0 && memoCutOrig[1]!=0 ) { cutOrig[0]=memoCutOrig[0]; cutOrig[1]=memoCutOrig[1]; }
            if( memoCutOrig[2]!=0 && memoCutOrig[3]!=0 ) { cutOrig[2]=memoCutOrig[2]; cutOrig[3]=memoCutOrig[3]; }
            context.setCutOrig(cutOrig);
         }
         context.info("Data range ["+cutOrig[2]+" .. "+cutOrig[3]+"], pixel cut ["+cutOrig[0]+" .. "+cutOrig[1]+"]");
         context.setValidateCut(true);
         if( hasAlternateBlank ) context.setBlankOrig(blankOrig);
      }
      
      // Mise à jour des paramètres de sortie (en cas de conversion du BITPIX notamment)
      context.initParameters();
      if( !context.verifCoherence() ) throw new Exception("Uncompatible pre-existing HEALPix survey");
      if( !context.isColor() && context.getBScale()==0 ) throw new Exception("Big bug => BSCALE=0 !! please contact CDS");
      

      // Info sur la méthode
      CoAddMode m = context.getCoAddMode();
      context.info("mode="+CoAddMode.getExplanation(m));
   }

   /** Demande d'affichage des statistiques (via Task()) */
   public void showStatistics() {
      if( statNbTile+statEmptyTile==0 ) return;
      context.showTilesStat(statNbThreadRunning,statNbThread,totalTime,statNbTile,statEmptyTile,statNodeTile,
            statMinTime,statMaxTime,statAvgTime,statNodeAvgTime,getUsedMem(),getMem());
      String s = showMem();
      if( s.length()>0 ) {
         //         adjustMem();
         context.stat(s);
      }
      context.stat(context.cacheFits+"");
      System.gc();
   }
   
   Hashtable<Thread,ArrayList<Fits>> memPerThread;

   // Initialisation des statistiques
   private void initStat(int nbThread) {
      statNbThread=nbThread; statNbThreadRunning=0;
      statNbTile=statNodeTile=0;
      statTotalTime=statNodeTotalTime=0L;
      startTime = System.currentTimeMillis();
      totalTime=0L;
      memPerThread = new Hashtable<Thread,ArrayList<Fits>>();
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
      ArrayList<Fits> m = memPerThread.get(key);
      if( m!=null ) for( Fits f : m ) f.free();
      memPerThread.remove(key);
   }
   
   // Suivi de mémoire d'un Thread particulier : ajout d'un Fits
   protected void addFits(Thread t,Fits f) {
      if( f==null ) return;
      ArrayList<Fits> m = memPerThread.get(t);
      if( m==null ) {
         m=new  ArrayList<Fits>();
         memPerThread.put(t,m);
      }
      m.add(f);
   }
   
   // Suivi de mémoire d'un Thread particulier : retrait d'un Fits
   protected void rmFits(Thread t,Fits f) {
      ArrayList<Fits> m = memPerThread.get(t);
      if( m==null ) return;
      m.remove(f);
   }
   
   private long getUsedMem() {
      long mem=0L;
      for( Thread t : memPerThread.keySet() ) {
         ArrayList<Fits> m = memPerThread.get(t);
         mem += getUsedMem(m);
      }
      return mem;
   }
   
   private String showMem() {
      StringBuffer s = new StringBuffer();
      int i=0;
      for( Thread t : memPerThread.keySet() ) {
         ArrayList<Fits> m = memPerThread.get(t);
         if( s.length()>0 ) s.append(", ");
         s.append(t.getName()+": "+m.size()+" tiles"+" ("+cds.tools.Util.getUnitDisk( getUsedMem(m) )+")");
      }
      return s.toString();
   }
   
   // Suivi de mémoire d'un Thread particulier : retourne la mémoire utilisé (en bytes)
   private long getUsedMem(ArrayList<Fits> m) {
      if( m==null ) return 0L;
      long mem=0L;
      for( Fits f : m ) mem += f.getMem();
      return mem;
   }
   
   // Mise à jour des stats
   private void updateStat(int deltaNbThread,int deltaTile,int deltaEmptyTile,long timeTile,int deltaNodeTile,long timeNodeTile) {
      statNbThreadRunning+=deltaNbThread;
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
      totalTime = System.currentTimeMillis()-startTime;
   }

   private void build() throws Exception {
      this.ordermax = context.getOrder();
      long t = System.currentTimeMillis();

      npix_list = new ArrayList<Long>(1024);
      for( long npix=0; npix<768L; npix++) {
         if( context.isInMoc(3, npix) ) npix_list.add(npix);
      }

      // Initialisation des variables
      isColor = context.isColor();
      bitpix = context.getBitpix();
      coaddMode = context.getCoAddMode();

      if( !isColor ) {
         bZero = context.getBZero();
         bScale = context.getBScale();
         blank = context.getBlank();
      }

      // Numéro courant dans la liste npix_list
      NCURRENT = 0;

      int nbProc = Runtime.getRuntime().availableProcessors();

      // On utilisera pratiquement toute la mémoire pour le cache
      long size = getMem();
      long sizeCache= size<=50*1024*1024 ? 0 : size>400*1024*1024L ? -200*1024*1024L : -50*1024*1024;   // On laissera 200Mo de libre 
//      sizeCache=150*1024*1024L;
      context.setCache(new CacheFits(sizeCache));

      long maxMemPerThread = 4L * Constante.MAXOVERLAY * Constante.FITSCELLSIZE * Constante.FITSCELLSIZE * context.getNpix();
      context.info("Minimal RAM required per thread (upper estimation): "+cds.tools.Util.getUnitDisk(maxMemPerThread));
      context.info("Processing original image files in blocs of "+Constante.FITSCELLSIZE+"x"+Constante.FITSCELLSIZE+" pixels");
      int nbThread = (int) (size / maxMemPerThread);
      
//      int nbThread=nbProc;
      
      int maxNbThread = context.getMaxNbThread();
      if( maxNbThread>0 && nbThread>maxNbThread ) nbThread=maxNbThread;
      if (nbThread==0) nbThread=1;
      if( nbThread>nbProc ) nbThread=nbProc;
      
      Aladin.trace(4,"BuildController.build(): Found "+nbProc+" processor(s) for "+size/(1024*1024)+"MB RAM => Launch "+nbThread+" thread(s)");
      context.info("processing by "+nbThread+" thread"+(nbThread>1?"s":"")+" with "+cds.tools.Util.getUnitDisk(size)+" available RAM");

      // Lancement des threads de calcul
      launchThreadBuilderHpx(nbThread,ordermin,ordermax);

      // Attente de la fin du travail
      while( stillAlive() ) {
         if( stopped ) { destroyThreadBuilderHpx(); return; }
         cds.tools.Util.pause(1000);
      }

      context.cacheFits.reset();
      Aladin.trace(3,"Cache FITS status: "+ context.cacheFits);
      Aladin.trace(3,"Healpix survey build in "+cds.tools.Util.getTemps(System.currentTimeMillis()-t));
   }

//   private void searchMinMax() {
//
//      long max = Util.nbrPix(Util.nside(ordermin));
//
//      // nummin nummax
//      String path = Util.getFilePath( context.getHpxFinderPath(), ordermax, 0);
//      path = path.substring(0, path.lastIndexOf(Util.FS)); // enleve le nom de fichier
//      path = path.substring(0, path.lastIndexOf(Util.FS)); // enleve le nom de repertoire
//
//      File f = new File(path);
//      String[] dirs = f.list();
//      npix_list = new ArrayList<Long>();
//      for (String dir : dirs) {
//         long i = Util.getNDirFromPath(dir+Util.FS);
//         // ajoute tous les losanges touchés par ce répertoire
//         for (long n = Util.idx(i, ordermax, ordermin) ; n <= Util.idx(i+10000-1, ordermax, ordermin) ; n++) {
//            if( n>max ) break;
//            if (!npix_list.contains(n)) npix_list.add(n);
//         }
//      }
//   }


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
    * @param fast true si on utilise la méthode la plus rapide (non bilinéaire, non moyennée)
    * @param fading true si on utilise un fading sur les bords des images
    * @return Le losange
    */
   private Fits createHpx(ThreadBuilderTile hpx, String path,int order, int maxOrder, long npix) throws Exception {
      String file = Util.getFilePath(path,order,npix);

      // si le process a été arrêté on essaie de ressortir au plus vite
      if (stopped) return null;

      // si on n'est pas dans le Moc, on sort
      boolean inTree = context.isInMocTree(order,npix);
      if (!inTree) return null;
      
      // si le losange a déjà été calculé on le renvoie
      if( coaddMode==CoAddMode.KEEPTILE ) {
         Fits oldOut = findLeaf(file);
         if( oldOut!=null ) {
            HealpixMoc moc = context.getRegion();
            moc = moc.intersection(new HealpixMoc(order+"/"+npix));
            int nbTiles = (int)moc.getUsedArea();
            updateStat(0,0,nbTiles,0,nbTiles/4,0);
            oldOut.releaseBitmap();
            return oldOut;
         }
      }

      Fits f = null;

      // Création d'un losange terminal
      if( order==maxOrder )  {
         try { f = createLeaveHpx(hpx,file,order,npix); }
         catch( Exception e ) {
            System.err.println("BuilderTiles.createLeave error: "+file);
            e.printStackTrace();
            return null;
         }

      // Création des branches filles, et cumul des résultats
      } else {

         Fits fils[] = new Fits[4];
         for( int i =0; !stopped && i<4; i++ ) {
            if( context.isTaskAborting() ) throw new Exception("Task abort !");
            fils[i] = createHpx(hpx, path,order+1,maxOrder,npix*4+i);
         }
         try { f = createNodeHpx(file,path,order,npix,fils); }
         catch( Exception e ) {
            System.err.println("BuilderTiles.createNodeHpx error: "+file);
            e.printStackTrace();
            return null;
         }
      }

      // On soulage la mémoire RAM des losanges qui ne vont pas servir tout de suite
      // on les relira lorsqu'on en aura besoin dans createNodeHpx(...)
//      if( order<maxOrder-(Constante.MAXDEPTHINRAM-1) && f!=null ) f.releaseBitmap();
      if( f!=null ) f.releaseBitmap();
      return f;
   }


   // Classe des threads de calcul
   private class ThreadBuilder extends Thread {
      int ordermin;
      int ordermax;
      ThreadBuilderTile hpx;
      static final int WAIT=0;
      static final int EXEC=1;
      static final int DIED=2;

      private int mode=WAIT;
      private boolean encore=true;

      public ThreadBuilder(String name,ThreadBuilderTile hpx, int ordermin,int ordermax) {
         super(name);
         this.ordermin = ordermin;
         this.ordermax = ordermax;
         this.hpx = hpx;
         Aladin.trace(3,"Creating "+getName());
      }

      /** Le thread est mort */
      public boolean isDied() { return mode==DIED; }

      /** Demande la mort du thread */
      public void tue() { encore=false; }

      public void run() {
         mode=EXEC;
         updateStat(+1,0,0,0,0,0);
         while( encore ) {
            long npix = getNextNpix();
            if( npix==-1 ) break;
            try {
//               Aladin.trace(4,Thread.currentThread().getName()+" process tree "+npix+"...");

               // si le process a été arrêté on essaie de ressortir au plus vite
               if (stopped) break;
               if( context.isTaskAborting() ) break;

               createHpx(hpx, context.getOutputPath(), ordermin, ordermax, npix);
               context.setProgressLastNorder3((int)npix);

            } catch( Throwable e ) { e.printStackTrace(); }
         }
         updateStat(-1,0,0,0,0,0);
         mode=DIED;
         rmThread(Thread.currentThread());
         Aladin.trace(3,Thread.currentThread().getName()+" died !");
      }
   }

   // Retourne le prochain numéro de pixel à traiter par les threads de calcul, -1 si terminer
   private long getNextNpix() {
      getlock();
      long pix = NCURRENT==npix_list.size()  ? -1 : npix_list.get(NCURRENT++);
      unlock();
      return pix;
   }

   // Gère l'accès exclusif par les threads de calcul à la liste des losanges à traiter (npix_list)
   private void getlock() {
      while( true ) {
         synchronized(lockObj) { if( !lock ) { lock=true; return; } }
         cds.tools.Util.pause(10);
      }
   }
   private void unlock() { synchronized(lockObj) { lock=false; } }

   private Object lockObj = new Object();
   private boolean lock=false;


   // Crée une série de threads de calcul
   private void launchThreadBuilderHpx(int nbThreads,int ordermin,int ordermax) throws Exception {

      initStat(nbThreads);
      context.createHealpixOrder(Constante.ORDER);

      for( int i=0; i<nbThreads; i++ ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         ThreadBuilderTile hpx = new ThreadBuilderTile(context);
         ThreadBuilder t = new ThreadBuilder("Builder"+i,hpx,ordermin,ordermax);
         threadList.add( t );
         t.start();
      }
   }

   // Demande l'arrêt de tous les threads de calcul
   void destroyThreadBuilderHpx() {
      Iterator<ThreadBuilder> it = threadList.iterator();
      while( it.hasNext() ) it.next().tue();
   }

   // Retourne true s'il reste encore au-moins un thread de calcul vivant
   boolean stillAlive() {
      Iterator<ThreadBuilder> it = threadList.iterator();
      while( it.hasNext() ) if( !it.next().isDied() ) return true;
      return false;
   }

   /** Création d'un losange par concaténation de ses 4 fils
    * @param file Nom du fichier complet, mais sans l'extension
    * @param path Path de la base
    * @param order Ordre Healpix du losange
    * @param npix Numéro Healpix du losange
    * @param fils les 4 fils du losange
    */
   private Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[]) throws Exception {
      long t = System.currentTimeMillis();
      int w=Constante.SIDE;
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
         out.setBscale(bScale);
         out.setBzero(bZero);
      }
      Fits in;
      for( int dg=0; dg<2; dg++ ) {
         for( int hb=0; hb<2; hb++ ) {
            int quad = dg<<1 | hb;
            in = fils[quad];
            int offX = (dg*w)>>>1;
            int offY = ((1-hb)*w)>>>1;

            for( int y=0; y<w; y+=2 ) {
               for( int x=0; x<w; x+=2 ) {

                  // Couleur
                  if( isColor ) {
                     int pix=0;
                     if( in!=null ) {
                        for( int i=0;i<4; i++ ) {
                           int gx = i==1 || i==3 ? 1 : 0;
                           int gy = i>1 ? 1 : 0;
                           int p = in.getPixelRGBJPG(x+gx,y+gy);
                           pix=p;
                           break;
                           //	                        pix+=p/4;
                        }
                     }
                     out.setPixelRGBJPG(offX+(x>>>1), offY+(y>>>1), pix);

                     // Normal
                  } else {

                     // On prend la moyenne des 4
                     double pix=0;
                     int nbPix=0;
                     if( in!=null ) {
                        for( int i=0;i<4; i++ ) {
                           int gx = i==1 || i==3 ? 1 : 0;
                           int gy = i>1 ? 1 : 0;
                           px[i] = in.getPixelDouble(x+gx,y+gy);
                           if( !Double.isNaN(px[i]) && px[i]!=blank ) nbPix++;
                        }
                     }
                     for( int i=0; i<4; i++ ) {
                        if( !Double.isNaN(px[i]) && px[i]!=blank ) pix+=px[i]/nbPix;
                     }
                     if( nbPix==0 ) pix=blank;  // aucune valeur => BLANK
                     out.setPixelDouble(offX+(x>>>1), offY+(y>>>1), pix);
                  }
               }
            }
         }
      }

      if( coaddMode!=CoAddMode.REPLACETILE && coaddMode!=CoAddMode.KEEPTILE ) {
         Fits oldOut = findLeaf(file);
         if( oldOut!=null ) {
            if( coaddMode==CoAddMode.AVERAGE ) out.coadd(oldOut);
            else if( coaddMode==CoAddMode.OVERWRITE ) out.mergeOnNaN(oldOut);
            else if( coaddMode==CoAddMode.KEEP ) {
               // Dans le cas integer, si le losange déjà calculé n'a pas de BLANK indiqué, on utilisera
               // celui renseigné par l'utilisateur, et sinon celui par défaut
               if( oldOut.bitpix>0 && Double.isNaN(oldOut.blank)) oldOut.setBlank(blank);
               oldOut.mergeOnNaN(out);
               out=oldOut;
               oldOut=null;
            }
         }
      }

      String filename = file+context.getTileExt();
      if( isColor ) out.writeCompressed(filename,0,0,null,"jpeg");
      else out.writeFITS(filename);

      long duree = System.currentTimeMillis() -t;
      if( npix%10 == 0 || DEBUG ) Aladin.trace(4,Thread.currentThread().getName()+".createNodeHpx("+order+"/"+npix+") "+coaddMode+" in "+duree+"ms");

      updateStat(0,0,0,0,1,duree);

      for( int i=0; i<4; i++ ) {
         if( fils[i]!=null ) {
            fils[i].free();
            rmFits(Thread.currentThread(), fils[i]);
            fils[i]=null;
         }
      }
      
      addFits(Thread.currentThread(),out);
      return out;
   }

   /** Création d'un losange terminal
    * @param file Nom du fichier de destination (complet mais sans l'extension)
    * @param order Ordre healpix du losange
    * @param npix Numéro Healpix du losange
    * @param fading utilisation d'un fading pour les bords/recouvrements d'images
    * @return null si rien trouvé pour construire ce fichier
    */
   private Fits createLeaveHpx(ThreadBuilderTile hpx, String file,int order,long npix) throws Exception {
      long t = System.currentTimeMillis();
      
      Fits oldOut=null;
      
      boolean isInList = context.isInMoc(order,npix);
      if( !isInList && coaddMode!=CoAddMode.REPLACETILE ) {
         if( oldOut==null ) oldOut = findLeaf(file);
         if( !(oldOut==null && context.isMocDescendant(order,npix) ) ) {
            addFits(Thread.currentThread(), oldOut);
            return oldOut;
         }
      }

      int nside_file = Util.nside(order);
      int nside = Util.nside(order+Constante.ORDER);

      Fits out = hpx.buildHealpix(this,nside_file, npix, nside);

      if( out !=null  ) {

         if( coaddMode!=CoAddMode.REPLACETILE ) {
            if( oldOut==null ) oldOut = findLeaf(file);
            if( oldOut!=null && coaddMode==CoAddMode.KEEPTILE ) { out=null; addFits(Thread.currentThread(), oldOut); return oldOut; }
            if( oldOut!=null ) {
               if( coaddMode==CoAddMode.AVERAGE ) out.coadd(oldOut);
               else if( coaddMode==CoAddMode.OVERWRITE ) out.mergeOnNaN(oldOut);
               else if( coaddMode==CoAddMode.KEEP ) {
                  // Dans le cas integer, si le losange déjà calculé n'a pas de BLANK indiqué, on utilisera
                  // celui renseigné par l'utilisateur, et sinon celui par défaut
                  if( oldOut.bitpix>0 && Double.isNaN(oldOut.blank)) oldOut.setBlank(blank);
                  oldOut.mergeOnNaN(out);
                  out=oldOut;
                  oldOut=null;
               }
            }
         }
      }

      long duree = System.currentTimeMillis()-t;
      if (out!=null) {
         String filename = file + context.getTileExt();
         if( isColor ) out.writeCompressed(filename,0,0,null,"jpeg");
         else out.writeFITS(filename);
         if( npix%10 == 0 || DEBUG ) Aladin.trace(4,Thread.currentThread().getName()+".createLeaveHpx("+order+"/"+npix+") "+coaddMode+" in "+duree+"ms");
         updateStat(0,1,0,duree,0,0);
         
      } else updateStat(0,0,1,duree,0,0);
      
      addFits(Thread.currentThread(), out);
      return out;
   }
   

   /** Recherche et chargement d'un losange déjà calculé (présence du fichier Fits 8 bits).
    *  Retourne null si non trouvé
    * @param file Nom du fichier fits (complet avec extension)
    */
   public Fits findLeaf(String file) throws Exception {
      String filename = file + context.getTileExt();
      File f = new File(filename);
      if( !f.exists() ) return null;
      Fits out = new Fits();
      MyInputStream is = null;
      try {
         is = new MyInputStream( new FileInputStream(f));
         if( isColor ) out.loadJpeg(is, true);
         else out.loadFITS(is);
         out.setFilename(filename);
      } 
      catch( Exception e ) { return null; } 
      finally { if( is!=null ) is.close(); }
      return out;
   }
   
}

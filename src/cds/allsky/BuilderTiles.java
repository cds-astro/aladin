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
import java.util.Iterator;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.tools.pixtools.Util;

/** Permet la g�n�ration du survey HEALPix � partir d'un index pr�alablement g�n�r�
 * @author Standard Ana�s Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderTiles extends Builder {

   private boolean flagColor;
   private int bitpix;
   private double bZero;
   private double bScale;
   private double blank;

   // Liste des Threads de calcul
   private ArrayList<ThreadBuilder> threadList = new ArrayList<ThreadBuilder>();
   private CoAddMode coaddMode=CoAddMode.REPLACETILE;

//   protected String hpxFinderPath;
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
   private int statNbTile;                 // Nombre de tuiles terminales d�j� calcul�s
   private long statMinTime,statMaxTime,statTotalTime,statAvgTime;
   private int statEmptyTile;               // Nombre de tuiles terminales de fait vides
   private int statNodeTile;                 // Nombre de tuiles "interm�diaires" d�j� calcul�s
   private long statNodeTotalTime,statNodeAvgTime;
   private long startTime;                 // Date de lancement du calcul
   private long totalTime;                 // Temps depuis le d�but du calcul

   public BuilderTiles(Context context) { super(context); }

   public Action getAction() { return Action.TILES; }


   public void run() throws Exception {
      context.running("Creating FITS tiles and allsky (max depth="+context.getOrder()+")...");
      context.info("sky area to process: "+context.getNbLowCells()+" low level HEALPix cells");

      // Un peu de baratin
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
      
//      context.setBitpixOrig(0);
//      if( true ) return;
      
      // m�morisation des cuts et blank positionn�s manuellement
      double [] memoCutOrig = context.getCutOrig();
      boolean hasAlternateBlank = context.hasAlternateBlank();
      double blankOrig = context.getBlankOrig();
      int bitpixOrig = context.getBitpixOrig();
      
      // Image �talon � charger obligatoirement pour BSCALE, BZERO, BITPIX et BLANK
      String img = context.getImgEtalon();
      if( img==null ) img = context.justFindImgEtalon( context.getInputPath() );
      if( img==null ) throw new Exception("No source image found in "+context.getInputPath());
      context.info("Reference image: "+img);
      try { context.setImgEtalon(img); }
      catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
      
      // Image de r�f�rence en couleur
      if(  context.getBitpixOrig()==0 ) {
         context.initRegion();
         return;
      }

      if( bitpixOrig==-1 ) {
         context.info("BITPIX found in the reference image => "+context.getBitpixOrig());
      } else if( bitpixOrig!=context.getBitpixOrig() ) {
         context.warning("The provided BITPIX (" +bitpixOrig+ ") is different than the original one (" + context.getBitpixOrig() + ") => bitpix conversion will be applied");
         context.setBitpixOrig(bitpixOrig);
      }

      // repositionnement des cuts et blank pass�s par param�tre
      double [] cutOrig = context.getCutOrig();
      if( memoCutOrig!=null ) {
         if( memoCutOrig[0]!=0 && memoCutOrig[1]!=0 ) { cutOrig[0]=memoCutOrig[0]; cutOrig[1]=memoCutOrig[1]; }
         if( memoCutOrig[2]!=0 && memoCutOrig[3]!=0 ) { cutOrig[2]=memoCutOrig[2]; cutOrig[3]=memoCutOrig[3]; }
         context.setCutOrig(cutOrig);
      }
      context.info("Data range ["+cutOrig[2]+" .. "+cutOrig[3]+"], pixel cut ["+cutOrig[0]+" .. "+cutOrig[1]+"]");
      context.setValidateCut(true);
      if( hasAlternateBlank ) context.setBlankOrig(blankOrig);

      // Mise � jour des param�tres de sortie (en cas de conversion du BITPIX notamment)
      context.initParameters();
      if( !context.verifCoherence() ) throw new Exception("Uncompatible pre-existing HEALPix survey");
      if( context.getBScale()==0 ) throw new Exception("Big bug => BSCALE=0 !! please contact CDS");

      // Info sur la m�thode
      CoAddMode m = context.getCoAddMode();
      context.info("mode="+CoAddMode.getExplanation(m));
   }

   /** Demande d'affichage des statistiques (via Task()) */
   public void showStatistics() {
      context.showTilesStat(statNbThreadRunning,statNbThread,totalTime,statNbTile,statEmptyTile,statNodeTile,
            statMinTime,statMaxTime,statAvgTime,statNodeAvgTime);
   }

   // Initialisation des statistiques
   private void initStat(int nbThread) {
      statNbThread=nbThread; statNbThreadRunning=0;
      statNbTile=statNodeTile=0;
      statTotalTime=statNodeTotalTime=0L;
      startTime = System.currentTimeMillis();
      totalTime=0L;
   }

   // Mise � jour des stats
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
      flagColor = context.isColor();
      bitpix = context.getBitpix();
      coaddMode = context.getCoAddMode();

      if( !flagColor ) {
         bZero = context.getBZero();
         bScale = context.getBScale();
         blank = context.getBlank();
      }

      // Num�ro courant dans la liste npix_list
      NCURRENT = 0;

      int nbProc = Runtime.getRuntime().availableProcessors();

      // On utilisera 2/3 de la m�moire pour les threads et le reste pour le cacheFits
      long size = Runtime.getRuntime().maxMemory();
      long sizeCache = (size/3L)/(1024L*1024L);
      size -=sizeCache;
      Aladin.trace(4,"BuildController.build() cacheFits.size="+sizeCache+"Mo");
      context.setCache(new CacheFits(sizeCache, 100000));

      long maxMemPerThread = Constante.NBTILESINRAM * (long)( Constante.SIDE*Constante.SIDE*context.getNpix() );
      maxMemPerThread += Constante.MAXOVERLAY * Constante.FITSCELLSIZE * Constante.FITSCELLSIZE * context.getNpix();
      Aladin.trace(4,"BuildController.build(): RAM required per thread estimated at "+cds.tools.Util.getUnitDisk(maxMemPerThread));
      int nbThread = (int) (size / maxMemPerThread);
      if (nbThread==0) nbThread=1;
      if( nbThread>nbProc ) nbThread=nbProc;

      Aladin.trace(4,"BuildController.build(): Found "+nbProc+" processor(s) for "+size/(1024*1024)+"MB RAM => Launch "+nbThread+" thread(s)");
      context.info("processing by "+nbThread+" thread"+(nbThread>1?"s":"")+" with "+cds.tools.Util.getUnitDisk(size));

      // Lancement des threads de calcul
      launchThreadBuilderHpx(nbThread,ordermin,ordermax);

      // Attente de la fin du travail
      while( stillAlive() ) {
         if( stopped ) { destroyThreadBuilderHpx(); return; }
         cds.tools.Util.pause(1000);
      }

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
//         // ajoute tous les losanges touch�s par ce r�pertoire
//         for (long n = Util.idx(i, ordermax, ordermin) ; n <= Util.idx(i+10000-1, ordermax, ordermin) ; n++) {
//            if( n>max ) break;
//            if (!npix_list.contains(n)) npix_list.add(n);
//         }
//      }
//   }


   /** Cr�ation d'un losange et de toute sa descendance si n�cessaire.
    * M�thode r�cursive qui
    * 1) V�rifie si le travail n'a pas d�j� �t� fait en se basant sur
    *    l'existance d'un fichier fits (si option keepall � vrai)
    * 2) Si order==maxOrder, calcul le losange terminal => createLeaveHpx(...)
    * 3) sinon concat�ne les 4 fils (appel r�cursif) en 1 losange => createNodeHpx(...)
    *
    * @param path  r�pertoire o� stocker la base
    * @param sky   Nom de la base (premier niveau de r�pertoire dans path)
    * @param order Ordre healpix du losange de d�part
    * @param maxOrder Ordre healpix max de la descendance
    * @param npix Num�ro healpix du losange
    * @param fast true si on utilise la m�thode la plus rapide (non bilin�aire, non moyenn�e)
    * @param fading true si on utilise un fading sur les bords des images
    * @return Le losange
    */
   private Fits createHpx(ThreadBuilderTile hpx, String path,int order, int maxOrder, long npix) throws Exception {
      String file = Util.getFilePath(path,order,npix);

      // si le process a �t� arr�t� on essaie de ressortir au plus vite
      if (stopped) return null;

      // si le losange a d�j� �t� calcul� on le renvoie
      if( coaddMode==CoAddMode.KEEPTILE ) {
         Fits oldOut = findFits(file+".fits");
         if( oldOut!=null ) return oldOut;
      }

      // si on n'est pas dans le Moc, on sort
      boolean inTree = context.isInMocTree(order,npix);
      if (!inTree) return null;


      Fits f = null;

      // Cr�ation d'un losange terminal
      if( order==maxOrder )  {
         f = createLeaveHpx(hpx,file,order,npix);

      // Cr�ation des branches filles, et cumul des r�sultats
      } else {

         Fits fils[] = new Fits[4];
         for( int i =0; !stopped && i<4; i++ ) {
            if( context.isTaskAborting() ) throw new Exception("Task abort !");
            fils[i] = createHpx(hpx, path,order+1,maxOrder,npix*4+i);
         }
         f = createNodeHpx(file,path,order,npix,fils);
      }

      // On soulage la m�moire RAM des losanges qui ne vont pas servir tout de suite
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

               // si le process a �t� arr�t� on essaie de ressortir au plus vite
               if (stopped) break;
               if( context.isTaskAborting() ) break;

               createHpx(hpx, context.getOutputPath(), ordermin, ordermax, npix);
               context.setProgressLastNorder3((int)npix);

            } catch( Throwable e ) { e.printStackTrace(); }
         }
         updateStat(-1,0,0,0,0,0);
         mode=DIED;
         Aladin.trace(3,Thread.currentThread().getName()+" died !");
      }
   }

   // Retourne le prochain num�ro de pixel � traiter par les threads de calcul, -1 si terminer
   private long getNextNpix() {
      getlock();
      long pix = NCURRENT==npix_list.size()  ? -1 : npix_list.get(NCURRENT++);
      unlock();
      return pix;
   }

   // G�re l'acc�s exclusif par les threads de calcul � la liste des losanges � traiter (npix_list)
   private void getlock() {
      while( true ) {
         synchronized(lockObj) { if( !lock ) { lock=true; return; } }
         cds.tools.Util.pause(10);
      }
   }
   private void unlock() { synchronized(lockObj) { lock=false; } }

   private Object lockObj = new Object();
   private boolean lock=false;


   // Cr�e une s�rie de threads de calcul
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

   // Demande l'arr�t de tous les threads de calcul
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

   /** Cr�ation d'un losange par concat�nation de ses 4 fils
    * @param file Nom du fichier complet, mais sans l'extension
    * @param path Path de la base
    * @param order Ordre Healpix du losange
    * @param npix Num�ro Healpix du losange
    * @param fils les 4 fils du losange
    */
   private Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[]) throws Exception {
      long t = System.currentTimeMillis();
      int w=Constante.SIDE;
      double px[] = new double[4];

      boolean inTree = context.isInMocTree(order,npix);
      if( !inTree ||
            fils[0]==null && fils[1]==null && fils[2]==null && fils[3]==null) return flagColor ? null : findFits(file+".fits");

      for( Fits f : fils ) if( f!=null ) f.reloadBitmap();

      Fits out = new Fits(w,w,bitpix);
      if( !flagColor ) {
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
                  if( flagColor ) {
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
         Fits oldOut = findFits(file+".fits");
         if( oldOut!=null ) {
            if( coaddMode==CoAddMode.AVERAGE ) out.coadd(oldOut);
            else if( coaddMode==CoAddMode.OVERWRITE ) out.mergeOnNaN(oldOut);
            else if( coaddMode==CoAddMode.KEEP ) {
               // Dans le cas integer, si le losange d�j� calcul� n'a pas de BLANK indiqu�, on utilisera
               // celui renseign� par l'utilisateur, et sinon celui par d�faut
               if( oldOut.bitpix>0 && Double.isNaN(oldOut.blank)) oldOut.setBlank(blank);
               oldOut.mergeOnNaN(out);
               out=oldOut;
            }
         }
      }

      if( flagColor ) out.writeJPEG(file+".jpg");
      else out.writeFITS(file+".fits");

      long duree = System.currentTimeMillis() -t;
      if( npix%10 == 0 || DEBUG ) Aladin.trace(4,Thread.currentThread().getName()+".createNodeHpx("+order+"/"+npix+") "+coaddMode+" in "+duree+"ms");

      updateStat(0,0,0,0,1,duree);

      for( int i=0; i<4; i++ ) {
         if( fils[i]!=null ) fils[i].free();
      }
      return out;
   }

   /** Cr�ation d'un losange terminal
    * @param file Nom du fichier de destination (complet mais sans l'extension)
    * @param order Ordre healpix du losange
    * @param npix Num�ro Healpix du losange
    * @param fading utilisation d'un fading pour les bords/recouvrements d'images
    * @return null si rien trouv� pour construire ce fichier
    */
   private Fits createLeaveHpx(ThreadBuilderTile hpx, String file,int order,long npix) throws Exception {
      long t = System.currentTimeMillis();

      Fits oldOut=null;
      boolean isInList = context.isInMoc(order,npix);
      if( !isInList && coaddMode!=CoAddMode.REPLACETILE ) {
         oldOut = findFits(file+".fits");
         if( !(oldOut==null && context.isMocDescendant(order,npix) ) ) return oldOut;
      }

      int nside_file = Util.nside(order);
      int nside = Util.nside(order+Constante.ORDER);

      Fits out = hpx.buildHealpix(nside_file, npix, nside);

      if( out !=null ) {

         if( coaddMode!=CoAddMode.REPLACETILE ) {
            if( oldOut==null ) oldOut = findFits(file+".fits");
            if( oldOut!=null && coaddMode==CoAddMode.KEEPTILE ) return oldOut;
            if( oldOut!=null && out!=null) {
               if( coaddMode==CoAddMode.AVERAGE ) out.coadd(oldOut);
               else if( coaddMode==CoAddMode.OVERWRITE ) out.mergeOnNaN(oldOut);
               else if( coaddMode==CoAddMode.KEEP ) {
                  // Dans le cas integer, si le losange d�j� calcul� n'a pas de BLANK indiqu�, on utilisera
                  // celui renseign� par l'utilisateur, et sinon celui par d�faut
                  if( oldOut.bitpix>0 && Double.isNaN(oldOut.blank)) oldOut.setBlank(blank);
                  oldOut.mergeOnNaN(out);
                  out=oldOut;
               }
            }
         }
      }

      long duree = System.currentTimeMillis()-t;
      if (out!=null) {
         if( flagColor ) out.writeJPEG(file+".jpg");
         else out.writeFITS(file+".fits");
         if( npix%10 == 0 || DEBUG ) Aladin.trace(4,Thread.currentThread().getName()+".createLeaveHpx("+order+"/"+npix+") "+coaddMode+" in "+duree+"ms");
         updateStat(0,1,0,duree,0,0);
      } else updateStat(0,0,1,duree,0,0);

      return out;
   }

   /** Recherche et chargement d'un losange d�j� calcul� (pr�sence du fichier Fits 8 bits).
    *  Retourne null si non trouv�
    * @param filefits Nom du fichier fits (complet avec extension)
    */
   static public Fits findFits(String filefits) throws Exception {
      File f = new File(filefits);
      if( !f.exists() ) return null;
      Fits out = new Fits();
      MyInputStream is = new MyInputStream( new FileInputStream(f));
      out.loadFITS(is);
      out.setFilename(filefits);
      is.close();
      return out;
   }
}

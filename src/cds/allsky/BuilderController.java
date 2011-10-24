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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Iterator;

import cds.aladin.Aladin;
import cds.aladin.Localisation;
import cds.aladin.MyInputStream;
import cds.aladin.PlanImage;
import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.tools.pixtools.Hpix;
import cds.tools.pixtools.HpixTree;
import cds.tools.pixtools.Util;


public class BuilderController  {

   private boolean flagColor;
   private int bitpix;
   private double bZero;
   private double bScale;
   private double blank;
//   private HpixTree moc=null;
   
   // Liste des Threads de calcul
   private ArrayList<ThreadBuilder> threadList = new ArrayList<ThreadBuilder>();
   //   private int fct = PlanImage.LINEAR;
   private CoAddMode coaddMode=CoAddMode.REPLACE;

   public Context context;

   
//   protected String hpxFinderPath;
   protected int ordermin = 3;
   protected long nummin = 0;
   protected long nummax = 0;
   ArrayList<Long> npix_list;
   protected double automin = 0;
   protected double automax = 0;
   private int progress = 0;

   private int lastN3 = -1;
   public static boolean DEBUG = true;

   public static String FS;
   private long NMAX = 0;
   private int NCURRENT = 0;
   private int ordermax;
   private boolean stopped = false;

   static { FS = System.getProperty("file.separator"); }

   // Pour les stat
   private int statNbThreadRunning=-1;     // Nombre de Thread qui sont en train de calculer
   private int statNbThread;               // Nombre max de Thread de calcul
   private int statNbTile;                 // Nombre de tuiles terminales d�j� calcul�s
   private long statMinTime,statMaxTime,statTotalTime,statAvgTime;
   private int statNodeTile;                 // Nombre de tuiles "interm�diaires" d�j� calcul�s
   private long statNodeTotalTime,statNodeAvgTime;
   private long startTime;                 // Date de lancement du calcul
   private long totalTime;                 // Temps depuis le d�but du calcul
   private long statLastShowTime = 0L;     // Date de la derni�re mise � jour du panneau d'affichage

   public BuilderController(Context context) {
      this.context=context;
      
   }
   
   // Suppression des statistiques
   private void resetStat() { statNbThread=-1; totalTime=-1; }

   // Initialisation des statistiques
   private void initStat(int nbThread) {
      statNbThread=nbThread; statNbThreadRunning=0; 
      statNbTile=statNodeTile=0;
      statTotalTime=statNodeTotalTime=0L;
      startTime = System.currentTimeMillis();
      totalTime=0L;
   }

   // Mise � jour des stats
   private void updateStat(int deltaNbThread,int deltaTile,long timeTile,int deltaNodeTile,long timeNodeTile) {
      statNbThreadRunning+=deltaNbThread;
      statNbTile+=deltaTile;
      statNodeTile+=deltaNodeTile;
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
      long t = System.currentTimeMillis();
      if( t-statLastShowTime < 1000 ) return;
      totalTime=System.currentTimeMillis()-startTime;
      statLastShowTime=t;
      context.showBuildStat(statNbThreadRunning,statNbThread,totalTime,statNbTile,statNodeTile,
            statMinTime,statMaxTime,statAvgTime,statNodeAvgTime);
   }

   public void build() throws Exception {
	   build(context.getOrder());
   }
   private void build(int ordermax) throws Exception {
      progress = 0;
      this.ordermax = ordermax;
      long t = System.currentTimeMillis();

      // r�cup�re les num�ros des losanges du niveau haut (ordermin)
      if (nummin == nummax && nummin == 0) searchMinMax();
      else {
         npix_list = new ArrayList<Long>((int) (nummax-nummin));
         for (int i = 0; i < nummax-nummin ; i++) {
            npix_list.add(i+nummin);
         }
      }

      // TODO cherche � d�finir l'ordre si ce n'est pas d�j� impos�
      if (ordermax==-1) ordermax=4;

      // pour chaque losange s�lectionn�
      NMAX = npix_list.size();     
      
      // Initialisation du Moc
      context.initParamFromGui();

      // Y a-t-il un changement de bitpix ?
      if( context.getBitpix() != context.getBitpixOrig() ) {
         context.initChangeBitpix();
         Aladin.trace(3,"Change BITPIX from "+context.getBitpixOrig()+" to "+context.getBitpix());
         Aladin.trace(3,"Map original pixel range ["+context.getCutOrig()[2]+" .. "+context.getCutOrig()[3]+"] " +
         		        "to ["+context.getCut()[2]+" .. "+context.getCut()[3]+"]");
         Aladin.trace(3,"Change BZERO,BSCALE="+context.getBZeroOrig()+","+context.getBScaleOrig()+" to "+context.getBZero()+","+context.getBScale());
      }
      
      // Initialisation des variables 
      flagColor = context.isColor();
      bitpix = context.getBitpix();
      context.getSkyval();
//      moc = context.getMoc();
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

      long maxMemPerThread = Constante.MAXMBPERTHREAD*1024*1024L;
      int nbThread = (int) (size / maxMemPerThread);
      if (nbThread==0) nbThread=1;
      if( nbThread>nbProc ) nbThread=nbProc;

      Aladin.trace(4,"BuildController.build(): Found "+nbProc+" processor(s) for "+size/(1024*1024)+"MB RAM => Launch "+nbThread+" thread(s)");

      // Lancement des threads de calcul
      launchThreadBuilderHpx(nbThread,ordermin,ordermax);

      // Attente de la fin du travail
      while( stillAlive() ) {
         if( stopped ) { destroyThreadBuilderHpx(); return; }
         cds.tools.Util.pause(1000);
      }
      
      context.showBuildStat(statNbThreadRunning,statNbThread,totalTime,statNbTile,statNodeTile,
            statMinTime,statMaxTime,statAvgTime,statNodeAvgTime);
      Aladin.trace(3,"Cache stated: "+ context.cacheFits);
      Aladin.trace(3,"Healpix survey build in "+cds.tools.Util.getTemps(System.currentTimeMillis()-t));
   }

   private void searchMinMax() {

      long max = Util.nbrPix(Util.nside(ordermin));

      // nummin nummax
      String path = Util.getFilePath( context.getHpxFinderPath(), ordermax, 0);
      path = path.substring(0, path.lastIndexOf(Util.FS)); // enleve le nom de fichier
      path = path.substring(0, path.lastIndexOf(Util.FS)); // enleve le nom de repertoire

      File f = new File(path);
      String[] dirs = f.list();
      npix_list = new ArrayList<Long>();
      for (String dir : dirs) {
         int i = Util.getNDirFromPath(dir+Util.FS);
         // ajoute tous les losanges touch�s par ce r�pertoire
         for (long n = Util.idx(i, ordermax, ordermin) ; n <= Util.idx(i+10000-1, ordermax, ordermin) ; n++) {
            if( n>max ) break;
            if (!npix_list.contains(n)) npix_list.add(n);
         }
      }
   }


   /** Cr�ation d'un losange et de toute sa descendance si n�cessaire.
    * M�thode r�cursive qui 
    * 1) V�rifie si le travail n'a pas d�j� �t� fait en se basant sur
    *    l'existance d'un fichier fits (si overwrite � false)
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
   Fits createHpx(BuilderHpx hpx, String path,int order, int maxOrder, long npix) throws Exception {
      String file = Util.getFilePath(path,order,npix);

      // si le process a �t� arr�t� on essaie de ressortir au plus vite
      if (stopped) return null;

      if( order==maxOrder ) {
         return createLeaveHpx(hpx,file,order,npix);      
      }
      Fits fils[] = new Fits[4];
      boolean found = false;
      for( int i =0; !stopped && i<4; i++ ) {
         fils[i] = createHpx(hpx, path,order+1,maxOrder,npix*4+i);
         if (fils[i] != null && !found) found = true;
      }
      if (!found) return null;
      return createNodeHpx(file,path,order,npix,fils);
   }


   // Classe des threads de calcul
   class ThreadBuilder extends Thread {
      int ordermin;
      int ordermax;
      BuilderHpx hpx;
      static final int WAIT=0;
      static final int EXEC=1;
      static final int DIED=2;

      private int mode=WAIT;
      private boolean encore=true;

      public ThreadBuilder(String name,BuilderHpx hpx, int ordermin,int ordermax) {
         super(name);
         this.ordermin = ordermin;
         this.ordermax = ordermax;
         this.hpx = hpx;
         Aladin.trace(3,"Creating "+getName());
      }

      /** Le thread est mort */
      public boolean isDied() { return mode==DIED; }

      /** Retourne le mode courant du thread (WAIT,EXEC ou DIED) */
      public int getMode() { return mode; }

      /** Demande la mort du thread */
      public void tue() { encore=false; }

      public void run() {
         mode=EXEC;
         updateStat(+1,0,0,0,0);
         while( encore ) {
            long npix = getNextNpix();
            if( npix==-1 ) break;
            try { 
               Aladin.trace(4,Thread.currentThread().getName()+" process tree "+npix+"/"+NMAX+"...");

               // si le process a �t� arr�t� on essaie de ressortir au plus vite
               if (stopped) break;

               Fits f = createHpx(hpx, context.getOutputPath(), ordermin, ordermax, npix);
               if (f!=null) lastN3 = (int)npix;
               progress++;
            } catch( Throwable e ) { e.printStackTrace(); }
         }
         updateStat(-1,0,0,0,0);
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
   private void launchThreadBuilderHpx(int nbThreads,int ordermin,int ordermax) {

      initStat(nbThreads);
      context.createHealpixOrder(Constante.ORDER);

      for( int i=0; i<nbThreads; i++ ) {
         BuilderHpx hpx = new BuilderHpx(context);
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

//   private boolean isAscendant(int order,long npix) {
//      if( moc==null ) return true;
//      return moc.isAscendant(order,npix);
//   }
//
//   private boolean isDescendant(int order,long npix) {
//      if( moc==null ) return true;
//      return moc.isDescendant(order,npix);
//   }
//
//   private boolean isInList(int order,long npix) {
//      if( moc==null ) return true;
//      return moc.isIn(order,npix);
//   }
   
   /** Cr�ation d'un losange par concat�nation de ses 4 fils
    * @param file Nom du fichier complet, mais sans l'extension
    * @param path Path de la base
    * @param order Ordre Healpix du losange
    * @param npix Num�ro Healpix du losange
    * @param fils les 4 fils du losange
    */
   Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[]) throws Exception {
      long t = System.currentTimeMillis();
      int w=Constante.SIDE;
      double px[] = new double[4];

//      boolean inTree = isInList(order,npix) || isAscendant(order,npix) || isDescendant(order,npix);
      boolean inTree = context.isInMocTree(order,npix);
      if( !inTree ) return flagColor ? null : findFits(file+".fits");

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
                     boolean ok=false;
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

      if( flagColor ) out.writeJPEG(file+".jpg");
      else out.writeFITS(file+".fits");

      long duree = System.currentTimeMillis() -t;
      if (npix%1000 == 0 || DEBUG) Aladin.trace(4,Thread.currentThread().getName()+".createNodeHpx("+order+"/"+npix+") in "+duree+"ms "+file+"... ");

      updateStat(0,0,0,1,duree);

      for( int i=0; i<4; i++ ) {
         if( fils[i]!=null ) fils[i].free();
      }
      return out;
   }
   
   /** Cr�ation d'un losange par concat�nation de ses 4 fils
    * et suppression des fichiers 8bits FITS des fils en question
    * puisque d�sormais inutiles.
    * @param file Nom du fichier complet, mais sans l'extension
    * @param path Path de la base
    * @param sky  Nom de la base
    * @param order Ordre Healpix du losange
    * @param npix Num�ro Healpix du losange
    * @param fils les 4 fils du losange
    * @deprecated
    */
   /*
	Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[], boolean overwrite) throws Exception {
		switch (bitpix) {
			case 8:
			case 16:
			case 32:
				return createNodeHpxInt(file, order, npix, fils,overwrite);
			case -32:
			case -64:
				return createNodeHpxDouble(file, order, npix, fils,overwrite);
			case 0:
				return createNodeHpxColor(file, order, npix, fils,overwrite);
		}
		return null;
	}*/

   /**
    * 
    * @param file
    * @param order
    * @param npix
    * @param fils
    * @param overwrite
    * @return
    * @deprecated
    * @throws Exception
    */
   //	Fits createNodeHpxInt(String file,int order,long npix,Fits fils[], boolean overwrite) throws Exception {
   ////		long t= System.currentTimeMillis();
   //		if (npix%1000 == 0 || DEBUG) System.out.println("Cumul de "+file+"... ");
   //		int w=SIDE;
   //		// essaye de r�cup�rer l'ancien fichier s'il existe
   //		Fits out;
   //		out = findFits(file+".fits");
   //		// si le fichier fits existe d�j� et qu'on ne doit pas l'�craser
   //		if (out != null && !overwrite) {
   //			// on re-cr�� juste le jpg
   //			toJPG(out,file+".jpg");
   //			return out;
   //		}
   //		out = new Fits(w,w,bitpix);
   //		out.setBlank(getBlank());
   //		if (getBscale() != Double.NaN && getBzero() != Double.NaN) { 
   //			out.setBscale(getBscale());
   //			out.setBzero(getBzero());
   //		}
   //		Fits in;
   //		int pix,p1,p2=0,p3=0,p4=0;
   //		for( int dg=0; dg<2; dg++ ) {
   //			for( int hb=0; hb<2; hb++ ) {
   //				int quad = dg<<1 | hb;
   //				in = fils[quad];
   //				int offX = (dg*w)>>>1;
   //				int offY = ((1-hb)*w)>>>1;
   //				for( int y=0; y<w; y+=2 ) {
   //					for( int x=0; x<w; x+=2 ) {
   //						p1=in.getPixelInt(x,y);
   //						p2=in.getPixelInt(x+1,y);
   //						p3=in.getPixelInt(x,y+1);
   //						p4=in.getPixelInt(x+1,y+1);
   //					
   //                        if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix=p1;
   //                        else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix=p2;
   //                        else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix=p3;
   //                        else pix=p4;
   //		                  
   //						out.setPixelInt(offX+(x>>>1), offY+(y>>>1), pix);
   //					}
   //				}
   //			}
   //		}
   //		// �crit les vrais pixels en FITS
   //		out.writeFITS(file+".fits");
   //		// l'�crit en JPG
   ////		toJPG(out,file+".jpg");
   //		
   //		for( int i=0; i<4; i++ ) {
   //			fils[i].free();
   //		}
   ////		System.out.println("=> "+(System.currentTimeMillis()-t)+"ms");
   //		return out;
   //	}


   /**
    * 
    * @param file
    * @param order
    * @param npix
    * @param fils
    * @param overwrite
    * @return
    * @deprecated
    * @throws Exception
    */
   //	Fits createNodeHpxDouble(String file,int order,long npix,Fits fils[], boolean overwrite) throws Exception {
   ////		long t= System.currentTimeMillis();
   //		if (npix%1000 == 0 || DEBUG)
   //			System.out.print("Cumul de "+file+"... ");
   //		int w=SIDE;
   //		// essaye de r�cup�rer l'ancien fichier s'il existe
   //		Fits out;
   //		out = findFits(file+".fits");
   //		// si le fichier fits existe d�j� et qu'on ne doit pas l'�craser
   //		if (out != null && !overwrite) {
   //			// on re-cr�� juste le jpg
   //			toJPG(out,file+".jpg");
   //			return out;
   //		}
   //		out = new Fits(w,w,bitpix);
   //		out.setBlank(getBlank());
   //		if (getBscale() != Double.NaN && getBzero() != Double.NaN) { 
   //			out.setBscale(getBscale());
   //			out.setBzero(getBzero());
   //		}
   //		Fits in;
   //		for( int dg=0; dg<2; dg++ ) {
   //			for( int hb=0; hb<2; hb++ ) {
   //				int quad = dg<<1 | hb;
   //				in = fils[quad];
   //				int offX = (dg*w)>>>1;
   //				int offY = ((1-hb)*w)>>>1;
   //				double pix,p1,p2=0,p3=0,p4=0;
   //				for( int y=0; y<w; y+=2 ) {
   //					for( int x=0; x<w; x+=2 ) {
   //						p1=in.getPixelDouble(x,y);
   //						p2=in.getPixelDouble(x+1,y);
   //						p3=in.getPixelDouble(x,y+1);
   //						p4=in.getPixelDouble(x+1,y+1);
   //					
   //                        if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix=p1;
   //                        else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix=p2;
   //                        else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix=p3;
   //                        else pix=p4;
   //		                  
   //                        out.setPixelDouble(offX+(x>>>1), offY+(y>>>1), pix);
   //					}
   //				}
   //			}
   //		}
   //		// �crit les vrais pixels en FITS
   //		out.writeFITS(file+".fits");
   //		// l'�crit en JPG
   ////		toJPG(out,file+".jpg");
   //		
   //		for( int i=0; i<4; i++ ) {
   //			fils[i].free();
   //		}
   //
   //		return out;
   //	}


   /**
    * 
    * @param file
    * @param order
    * @param npix
    * @param fils
    * @param overwrite
    * @return
    * @throws Exception
    * @deprecated
    */
   //	Fits createNodeHpxColor(String file,int order,long npix,Fits fils[], boolean overwrite) throws Exception {
   //		if (DEBUG)
   //			System.out.print("Cumul de "+file+"... ");
   //		int w=SIDE;
   //		// essaye de r�cup�rer l'ancien fichier s'il existe
   //		Fits out;
   //		out = findFits(file+".fits");
   //		// si le fichier fits existe d�j� et qu'on ne doit pas l'�craser
   //		if (out != null && !overwrite) {
   //			// on re-cr�� juste le jpg
   //			toJPG(out,file+".jpg");
   //			return out;
   //		}
   //		out = new Fits(w,w,bitpix);
   //		Fits in;
   //		for( int dg=0; dg<2; dg++ ) {
   //			for( int hb=0; hb<2; hb++ ) {
   //				int quad = dg<<1 | hb; // multiplie par 2
   //				in = fils[quad];
   //				in.inverseYColor();
   //				int offX = (dg*w)>>>1; // divise par 2 
   //				int offY = ((1-hb)*w)>>>1;// divise par 2
   //				int pix=0;//,p1,p2=0,p3=0,p4=0;
   //				for( int y=0; y<w; y+=2 ) {
   //					for( int x=0; x<w; x+=2 ) {
   //						pix=in.getPixelRGB(x,y);
   //                        out.setPixelRGB(offX+(x>>>1), offY+(y>>>1), pix);
   //					}
   //				}
   //			}
   //		}
   //		out.inverseYColor();
   //		out.writeJPEG(file+".jpg");
   //		
   //		for( int i=0; i<4; i++ ) {
   //			fils[i].free();
   //		}
   ////		System.out.println("=> "+(System.currentTimeMillis()-t)+"ms");
   //		return out;
   //	}

   /** Cr�ation d'un losange terminal
    * @param file Nom du fichier de destination (complet mais sans l'extension)
    * @param order Ordre healpix du losange
    * @param npix Num�ro Healpix du losange
    * @param fading utilisation d'un fading pour les bords/recouvrements d'images
    * @return null si rien trouv� pour construire ce fichier
    */
   Fits createLeaveHpx(BuilderHpx hpx, String file,int order,long npix) throws Exception {
      long t = System.currentTimeMillis();

      Fits oldOut=null;
      boolean isInList = context.isInMocLevel(order,npix);
//      boolean isInList = isInList(order,npix);
      if( !isInList ) {
         oldOut = findFits(file+".fits");
         if( !(oldOut==null && context.isMocDescendant(order,npix) ) ) return oldOut;
//         if( !(oldOut==null && isDescendant(order,npix) ) ) return oldOut;
      }

      int nside_file = Util.nside(order);
      int nside = Util.nside(order+Constante.ORDER);

      Fits out = hpx.buildHealpix(nside_file, npix, nside);

      if( out !=null ) {

         if( coaddMode!=CoAddMode.REPLACE ) {
            if( oldOut==null ) oldOut = findFits(file+".fits");
            if( oldOut!=null ) {
               if( coaddMode==CoAddMode.AVERAGE ) out.coadd(oldOut);
               else if( coaddMode==CoAddMode.KEEP ) out.mergeOnNaN(oldOut);
               else if( coaddMode==CoAddMode.OVERWRITE ) { oldOut.mergeOnNaN(out); out=oldOut; }
            }
         }

         if( flagColor ) out.writeJPEG(file+".jpg");
         else out.writeFITS(file+".fits");


         long duree = System.currentTimeMillis()-t;
         if( npix%10 == 0 || DEBUG ) Aladin.trace(4,Thread.currentThread().getName()+".createLeaveHpx("+order+"/"+npix+") "+coaddMode+" in "+duree+"ms");

         updateStat(0,1,duree,0,0);
      }

      return out;
   }

   /** Cr�ation si n�cessaire des r�pertoires et sous-r�pertoire du fichier 
    * pass� en param�tre */
   //	public void createPath(String filename) {
   //	   File f;
   //	   // Pour acc�lerer, on teste d'abord l'existence �ventuelle du dernier r�pertoire
   //	   int i = filename.lastIndexOf('/');
   //	   if( i<0 ) return;
   //	   f = new File( filename.substring(0,i) ) ;
   //	   if( f.exists() ) return;
   //
   //	   // Test et cr�ation si n�cecessaire des r�pertoires
   //	   for( int pos=filename.indexOf('/',3); pos>=0; pos=filename.indexOf('/',pos+1)) {
   //	      f = new File( filename.substring(0,pos) );
   //	      if( !f.exists() ) f.mkdir();
   //	   }
   //	}

   /** Recherche et chargement d'un losange d�j� calcul� (pr�sence du fichier Fits 8 bits).
    *  Retourne null si non trouv�
    * @param filefits Nom du fichier fits (complet avec extension)
    */
   Fits findJpg(String file) throws Exception {
      //		long t=System.currentTimeMillis();
      File f = new File(file+".jpg");
      if( !f.exists() ) return null;
      Fits out = new Fits();
      out.loadJpeg(file+".jpg", true);
      //		System.out.println("Relecture de "+file+".jpg ... => "+(System.currentTimeMillis()-t)+"ms");
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

   protected void toJPG(Fits out, String filename) throws Exception {
      double [] range;
      if (automin == automax && automin == 0) {
         range = out.findAutocutRange();
         automin = range[0];
         automax = range[1];
      }

      // l'�crit en JPG
      //		switch (fct) {
      //		case PlanImage.ASINH: out.toPix8ASinH(automin, automax);
      //		case PlanImage.LOG:out.toPix8Log(automin, automax);
      //		case PlanImage.SQRT:out.toPix8Sqrt(automin, automax);
      //		case PlanImage.SQR:out.toPix8Pow(automin, automax);
      //		case PlanImage.LINEAR:out.toPix8(automin, automax);
      //		}
      out.toPix8(automin, automax);

      out.writeJPEG(filename);
   }


   public int getProgress() {
      if (NMAX == 0) return 0;
      return (int) (progress*100./NMAX);
   }

   public void setCoadd(CoAddMode coaddMode) {
      this.coaddMode = coaddMode;
   }

//   public void setHpixTree(HpixTree hpixTree) {
//      this.hpixTree = hpixTree;
//   }

//   public void setBScaleBZero(double bscale, double bzero) {
//      this.bscale = bscale;
//      this.bzero = bzero;
//   }

//   public void setBlank(double blank) {
//      this.blank = blank;
//   }


   /**
    * @return the bscale
    */
//   public double getBscale() {
//      return bscale;
//   }

   /**
    * @return the bzero
    */
//   public double getBzero() {
//      return bzero;
//   }

//   public void setContext(Context context) {
//      this.context = context;
//   }

   /**
    * @param automin the automin to set
    */
   //	public void setAutoCut(double[] autocut, int fct) {
   //		automin = autocut[0];
   //		automin = automin/getBscale() + getBzero();
   //		automin = Fits.toBitpixRange(automin, bitpix, new double[] {autocut[2], autocut[3]});
   //		automax = autocut[1];
   //		automax = automax/getBscale() + getBzero();
   //		automax = Fits.toBitpixRange(automax, bitpix, new double[] {autocut[2], autocut[3]});
   //		
   //		this.fct = fct; 
   //		datacut = new double[] {autocut[2], autocut[3]}; 
   //		
   //	}


   /**
    * @return the blank
    */
//   public double getBlank() {
//      return blank;
//   }


   public void stop() {
      stopped = true;
      destroyThreadBuilderHpx();
      context.stop();
   }

   public void reset(String path) {
      cds.tools.Util.deleteDir(new File(path));
   }

   public int getLastN3() {
      return lastN3;
   }

   /*
	public void testVitesse() throws Exception {
		String outpath = "/tmp/";
		localServer = outpath + AllskyConst.HPX_FINDER;
		hpx = new HpxBuilder(16, localServer); 

		outpath += FS; 
		hpx.createHealpixOrder(ORDER);

		long t= System.currentTimeMillis();
		Thread thisThread = Thread.currentThread();
		thread = thisThread;
		long max = Util.getMax(11);
		for (int n = 0 ; thread == thisThread && n < 1 ; n++) {
			hpx.downFiles = new ArrayList<DownFile>();
			createHpx(outpath, AllskyConst.SURVEY, 3, 11, 1, false);
		}
		System.out.println("=> "+(System.currentTimeMillis()-t)+"ms");
	}*/
}

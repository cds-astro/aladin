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
import java.util.ArrayList;
import java.util.Iterator;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.aladin.PlanImage;
import cds.fits.Fits;
import cds.tools.pixtools.Hpix;
import cds.tools.pixtools.HpixTree;
import cds.tools.pixtools.Util;


public class DBBuilder  {
   
    static public boolean DSS = false;
    final static public int ORDER = 9; // 2^9 = 512 = SIDE
    final static public int SIDE = 512;

	protected int ordermin = 3;
	protected long nummin = 0;
	protected long nummax = 0;
	ArrayList<Long> npix_list;
	protected double automin = 0;
	protected double automax = 0;
	protected int bitpix = 16;
	protected String localServer = null;
	private int progress = 0;
	
	private int lastN3 = -1;
	public static boolean DEBUG = true;

	public static String FS;
	private long NMAX = 0;
	private int NCURRENT = 0;
	private int ordermax;
	private boolean stopped = false;

	static { FS = System.getProperty("file.separator"); }

	
	public void build(int ordermax, String outpath, int mybitpix,boolean fast, boolean fading, boolean keepBB) throws Exception {
		localServer = outpath + AllskyConst.HPX_FINDER;
		build(ordermax, outpath, mybitpix, fast, fading, localServer, keepBB);
	}
//	public void build(int ordermax, String outpath, int mybitpix, boolean overwrite, boolean fast,String hpxfinder) throws Exception {
//		progress = 0;
//		this.ordermax = ordermax;
//		long t = System.currentTimeMillis();
//		localServer = hpxfinder;
//		bitpix = mybitpix; 
////		hpx = new HpxBuilder(mybitpix, localServer);
//		hpx.setBitpix(mybitpix);
//		hpx.setLocalServer(localServer);
//
//		outpath += FS; 
//		hpx.createHealpixOrder(ORDER);
////		boolean rebuild = false;
//		
//		
//		//rebuild=Boolean.parseBoolean(args[9]);
//		
//		
//		// récupère les numéros des losanges du niveau haut (ordermin)
//		searchMinMax();
////		npix_list = new ArrayList<Long>((int) (nummax-nummin));
////		for (int i = 0; i < nummax-nummin ; i++) {
////			npix_list.add(i+nummin);
////		}
//
//		// TODO cherche à définir l'ordre si ce n'est pas déjà imposé
//		if (ordermax==-1) {
//			ordermax=4;
//		}
//
//		// pour chaque losange sélectionné
//		NMAX = npix_list.size();
//
//		Thread thisThread = Thread.currentThread();
//		for (int n = 0 ; thread == thisThread && n < npix_list.size() ; n++) {
//			progress++;
//			createHpx(outpath, ordermin, ordermax, npix_list.get(n), overwrite,fast);
//		}
//		System.out.println(">> Build in "+(System.currentTimeMillis()-t)/1000+"s");
//	}
	
//	public void build(int ordermax, String outpath, int mybitpix, boolean overwrite, boolean fast,String hpxfinder) throws Exception {
//		progress = 0;
//		this.ordermax = ordermax;
//		long t = System.currentTimeMillis();
//		localServer = hpxfinder;
//		bitpix = mybitpix; 
////		hpx = new HpxBuilder(mybitpix, localServer);
//		hpx.setBitpix(mybitpix);
//		hpx.setLocalServer(localServer);
//
//		outpath += FS; 
//		hpx.createHealpixOrder(ORDER);
////		boolean rebuild = false;
//		
//		
//		//rebuild=Boolean.parseBoolean(args[9]);
//		
//		
//		// récupère les numéros des losanges du niveau haut (ordermin)
//		searchMinMax();
////		npix_list = new ArrayList<Long>((int) (nummax-nummin));
////		for (int i = 0; i < nummax-nummin ; i++) {
////			npix_list.add(i+nummin);
////		}
//
//		// TODO cherche à définir l'ordre si ce n'est pas déjà imposé
//		if (ordermax==-1) {
//			ordermax=4;
//		}
//
//		// pour chaque losange sélectionné
//		NMAX = npix_list.size();
//
//		Thread thisThread = Thread.currentThread();
//		for (int n = 0 ; thread == thisThread && n < npix_list.size() ; n++) {
//			progress++;
//			createHpx(outpath, ordermin, ordermax, npix_list.get(n), overwrite,fast);
//		}
//		System.out.println(">> Build in "+(System.currentTimeMillis()-t)/1000+"s");
//	}
	
	public void build(int ordermax, String outpath, int mybitpix, boolean fast, boolean fading,
			String hpxfinder, boolean keepBB) throws Exception {
		progress = 0;
		this.ordermax = ordermax;
		long t = System.currentTimeMillis();
		localServer = hpxfinder;
		bitpix = mybitpix;
		outpath += FS; 
		
		// récupère les numéros des losanges du niveau haut (ordermin)
		if (nummin == nummax && nummin == 0) searchMinMax();
		else {
			npix_list = new ArrayList<Long>((int) (nummax-nummin));
			for (int i = 0; i < nummax-nummin ; i++) {
				npix_list.add(i+nummin);
			}
		}
		
		// TODO cherche à définir l'ordre si ce n'est pas déjà imposé
		if (ordermax==-1) ordermax=4;
		
		// pour chaque losange sélectionné
		NMAX = npix_list.size();

	   // Numéro courant dans la liste npix_list
	   NCURRENT = 0;

	   int nbProc = Runtime.getRuntime().availableProcessors();
	   long heapsize = Runtime.getRuntime().maxMemory();
	   int nbThread = (int) ( (heapsize / 512000 < nbProc)?heapsize / 512000:nbProc);
	   if (nbThread==0) nbThread=1;
	   Aladin.trace(3,"Found "+nbProc+" processor(s) for "+heapsize/(1024*1024)+"MB RAM => Launch "+nbThread+" thread(s)");

	   // Lancement des threads de calcul
	   createThread(nbThread,outpath,ordermin,ordermax,fast, fading, keepBB);

	   // Attente de la fin du travail
	   while( stillAlive() ) {
	      if( stopped ) { destroyThread(); return; }
	      cds.tools.Util.pause(1000);
	   }
	   //	   createPropertiesFile(outpath);
	   Aladin.trace(3,"Healpix survey build in "+cds.tools.Util.getTemps(System.currentTimeMillis()-t));
	}
/*
	private void createPropertiesFile(String dir) {
		 // the properties file will be used to check the file modification date
	    // and to store other parameters
	        Properties prop = new Properties();
	        prop.setProperty(KEY_ORIGINAL_PATH, originalPath);
	        if (isLocal) {
	            try {
	                String modifDate = new File(originalPath).lastModified() + "";
	                prop.setProperty(KEY_LAST_MODIFICATON_DATE, modifDate);
	            } catch (Exception e) {}
	        }
	        prop.setProperty(KEY_PROCESSING_DATE, DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
	        prop.setProperty(KEY_NSIDE_FILE, newNSideImage+""); // TODO : oui, je sais, j'ai merdé sur les noms !! newNSideImage devrait s'appeler newNSideFile
	        prop.setProperty(KEY_NSIDE_PIXEL, newNSideFile+""); // et newNSideFile devrait s'appeler newNSidePixel

	        prop.setProperty(KEY_ORDER_GENERATED_IMGS, hpxOrderGeneratedImgs+"");

	        prop.setProperty(KEY_ORDERING, ordering);

	        prop.setProperty(KEY_TFIELDS, nField+"");

	        prop.setProperty(KEY_TTYPES, Util.join(tfieldNames, ','));

	        prop.setProperty(KEY_LOCAL_DATA, isLocal+"");

	        prop.setProperty(KEY_GZ, isGZ+"");

	        prop.setProperty(KEY_OFFSET, initialOffsetHpx+"");

	        prop.setProperty(KEY_SIZERECORD, sizeRecord+"");

	        prop.setProperty(KEY_ISPARTIAL, isPartial+"");

	        prop.setProperty(KEY_ARGB, isARGB+"");

	        prop.setProperty(KEY_NBPIXGENERATEDIMAGE, nbPixGeneratedImage+"");

	        prop.setProperty(KEY_CURTFORMBITPIX, curTFormBitpix+"");

	        prop.setProperty(KEY_COORDSYS, coordsys+"");

	        prop.setProperty(KEY_LENHPX, Util.join(lenHpx, ','));

	        prop.setProperty(KEY_TYPEHPX, Util.join(typeHpx, ','));

	        prop.setProperty(KEY_ALADINVERSION, aladin.VERSION);


	        try {
	            prop.store(new FileOutputStream(propertiesFile(dir)), null);
	        } catch (FileNotFoundException e) {
	            return false;
	        } catch (IOException e) {
	            return false;
	        }

	        return true;
	    }
	}
	*/
	private void searchMinMax() {
	   
	   long max = Util.nbrPix(Util.nside(ordermin));
	   
		// nummin nummax
		String path = Util.getFilePath(localServer, ordermax, 0);
		path = path.substring(0, path.lastIndexOf(Util.FS)); // enleve le nom de fichier
		path = path.substring(0, path.lastIndexOf(Util.FS)); // enleve le nom de repertoire
		
		File f = new File(path);
		String[] dirs = f.list();
		npix_list = new ArrayList<Long>();
		for (String dir : dirs) {
			int i = Util.getNDirFromPath(dir+Util.FS);
			// ajoute tous les losanges touchés par ce répertoire
			for (long n = Util.idx(i, ordermax, ordermin) ; n <= Util.idx(i+10000-1, ordermax, ordermin) ; n++) {
			   if( n>max ) break;
				if (!npix_list.contains(n)) npix_list.add(n);
			}
		}
	}


	/** Création d'un losange et de toute sa descendance si nécessaire.
	 * Méthode récursive qui 
	 * 1) Vérifie si le travail n'a pas déjà été fait en se basant sur
	 *    l'existance d'un fichier fits (si overwrite à false)
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
	Fits createHpx(HpxBuilder hpx, String path,int order, int maxOrder, long npix,boolean fast,boolean fading) throws Exception {
		String file = Util.getFilePath(path,order,npix);
		
		// si le process a été arrêté on essaie de ressortir au plus vite
		if (stopped) return null;
		
		if( order==maxOrder ) {
		   return createLeaveHpx(hpx,file,order,npix, fast,fading);      
		}
		Fits fils[] = new Fits[4];
		boolean found = false;
		for( int i =0; !stopped && i<4; i++ ) {
			fils[i] = createHpx(hpx, path,order+1,maxOrder,npix*4+i,fast,fading);
			if (fils[i] != null && !found) found = true;
		}
		if (!found) return null;
//		for( int i =0; i<4; i++ ) {
//			// s'il y a au moins un fils, on met les autres à vides et pas null
//			// pour pouvoir quand meme construire le parent (tronqué)
//			if (fils[i] == null) fils[i] = new Fits(SIDE,SIDE,bitpix);
//		}
		return createNodeHpx(file,path,order,npix,fils);
	}
	
	
	// Classe des threads de calcul
    class ThreadBuilder extends Thread {
       String outpath;
       int ordermin;
       int ordermax;
       boolean fast;
       boolean fading;
       HpxBuilder hpx;
       static final int WAIT=0;
       static final int EXEC=1;
       static final int DIED=2;
       
       private int mode=WAIT;
       private boolean encore=true;
       
       public ThreadBuilder(String name,String outpath, HpxBuilder hpx, int ordermin,int ordermax,boolean fast,boolean fading) {
          super(name);
          this.outpath = outpath;
          this.ordermin = ordermin;
          this.ordermax = ordermax;
          this.fast = fast;
          this.fading = fading;
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
          while( encore ) {
             long npix = getNextNpix();
             if( npix==-1 ) break;
             try { 
//                System.out.println(Thread.currentThread().getName()+" process tree "+npix+"/"+NMAX+"...");

         		// si le process a été arrêté on essaie de ressortir au plus vite
         		if (stopped) {
         			break;
         		}
         		
                Fits f = createHpx(hpx, outpath, ordermin, ordermax, npix, fast,fading);
                if (f!=null) {
                	lastN3 = (int)npix;
                }
                progress++;
             } catch( Throwable e ) { e.printStackTrace(); }
          }
          mode=DIED;
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
	
    // Liste des Threads de calcul
    private ArrayList<ThreadBuilder> threadList = new ArrayList<ThreadBuilder>();
//	private int fct = PlanImage.LINEAR;
	private double bscale;
	private double bzero;
	private double blank;
	private double[] datacut;
	private HpixTree hpixTree=null;
	private int coaddMode=DescPanel.REPLACETILE;
    
	// Crée une série de threads de calcul
	private void createThread(int nbThreads,String outpath,int ordermin,int ordermax,boolean fast, boolean fading, boolean keepBB) {
	   for( int i=0; i<nbThreads; i++ ) {
	      HpxBuilder hpx = new HpxBuilder();
	      hpx.setBitpix(bitpix, keepBB);
	      hpx.setLocalServer(localServer);
	      hpx.createHealpixOrder(ORDER);
	      hpx.setBscale(bscale);
	      hpx.setBzero(bzero);
	      hpx.setBlank(blank);
	      hpx.setDataCut(datacut);
	      hpx.setCoadd(coaddMode);
	      ThreadBuilder t = new ThreadBuilder("Builder"+i,outpath, hpx,ordermin,ordermax,fast,fading);
	      threadList.add( t );
	      t.start();
	   }
	}

	// Demande l'arrêt de tous les threads de calcul
	void destroyThread() {
	   Iterator<ThreadBuilder> it = threadList.iterator();
	   while( it.hasNext() ) it.next().tue();
	}

	// Retourne true s'il reste encore au-moins un thread de calcul vivant
	boolean stillAlive() {
	   Iterator<ThreadBuilder> it = threadList.iterator();
	   while( it.hasNext() ) if( !it.next().isDied() ) return true;
	   return false;
	}
	
	
	private boolean isInTree(int order,long npix) {
       if( hpixTree==null ) return true;
       int diffOrder = order-3;
       long perePix = npix / (long)Math.pow(4,diffOrder);
       Hpix hpix = new Hpix(3,perePix);
       return hpixTree.isAscendant(hpix) || hpixTree.isIn(hpix);
    }
	private boolean isAscendant(int order,long npix) {
	   if( hpixTree==null ) return true;
       Hpix hpix = new Hpix(order,npix);
       return hpixTree.isAscendant(hpix);
	}

    private boolean isDescendant(int order,long npix) {
       if( hpixTree==null ) return true;
       Hpix hpix = new Hpix(order,npix);
       return hpixTree.isDescendant(hpix);
    }
    
    private boolean isBrother(int order,long npix) {
       if( hpixTree==null ) return true;
       Hpix hpix = new Hpix(order,npix);
       return hpixTree.isBrother(hpix);
    }

    private boolean isInList(int order,long npix) {
       if( hpixTree==null ) return true;
       Hpix hpix = new Hpix(order,npix);
       return hpixTree.isIn(hpix);
    }

	/** Création d'un losange par concaténation de ses 4 fils
	 * @param file Nom du fichier complet, mais sans l'extension
	 * @param path Path de la base
	 * @param order Ordre Healpix du losange
	 * @param npix Numéro Healpix du losange
	 * @param fils les 4 fils du losange
	 */
	Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[]) throws Exception {
	   
	   int w=SIDE;
	   double blank = getBlank();
	   
       boolean inTree = isInList(order,npix) || isAscendant(order,npix) || isDescendant(order,npix);
	   if( !inTree ) return findFits(file+".fits");
	   
	   Fits out = new Fits(w,w,bitpix);
	   out.setBlank(blank);
	   if (getBscale() != Double.NaN && getBzero() != Double.NaN) { 
	      out.setBscale(getBscale());
	      out.setBzero(getBzero());
	   }
	   Fits in;
	   for( int dg=0; dg<2; dg++ ) {
	      for( int hb=0; hb<2; hb++ ) {
	         int quad = dg<<1 | hb;
	         in = fils[quad];
	         int offX = (dg*w)>>>1;
	         int offY = ((1-hb)*w)>>>1;
//	         double pix=0;
//	         double p1,p2,p3,p4;
	         
	         for( int y=0; y<w; y+=2 ) {
	            for( int x=0; x<w; x+=2 ) {
	               
	               // On prend la moyenne des 4
	               double pix=0;
	               boolean ok=false;
	               if( in!=null ) {
	                  for( int i=0;i<4; i++ ) {
	                     int gx = i==1 || i==3 ? 1 : 0;
	                     int gy = i>1 ? 1 : 0;
	                     double p = in.getPixelDouble(x+gx,y+gy);
	                     if( !in.isBlankPixel(p) ) { pix+=p/4; ok=true; }
	                  }
	               }
	               if( !ok ) pix=blank;  // aucune valeur => BLANK

                   // On garde la valeur médiane
//                 if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix=p1;
//                 else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix=p2;
//                 else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix=p3;
//                 else pix=p4;
                   
//	               p1=in.getPixelDouble(x,y);
//	               p2=in.getPixelDouble(x+1,y);
//	               p3=in.getPixelDouble(x,y+1);
//	               p4=in.getPixelDouble(x+1,y+1);
//	               
//	               // On prend la moyenne
//	               pix = p1/4 + p2/4 + p3/4 + p4/4;
	               
	               out.setPixelDouble(offX+(x>>>1), offY+(y>>>1), pix);
	            }
	         }
	      }
	   }
	   
	   // écrit les vrais pixels en FITS
	   out.writeFITS(file+".fits");
       if (npix%1000 == 0 || DEBUG) Aladin.trace(4,"DBDuilber.createNodeHpx("+order+"/"+npix+") "+file+"... ");

	   for( int i=0; i<4; i++ ) {
	      if( fils[i]!=null ) fils[i].free();
	   }
	   return out;
	}

	/** Création d'un losange par concaténation de ses 4 fils
	 * et suppression des fichiers 8bits FITS des fils en question
	 * puisque désormais inutiles.
	 * @param file Nom du fichier complet, mais sans l'extension
	 * @param path Path de la base
	 * @param sky  Nom de la base
	 * @param order Ordre Healpix du losange
	 * @param npix Numéro Healpix du losange
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
//		// essaye de récupérer l'ancien fichier s'il existe
//		Fits out;
//		out = findFits(file+".fits");
//		// si le fichier fits existe déjà et qu'on ne doit pas l'écraser
//		if (out != null && !overwrite) {
//			// on re-créé juste le jpg
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
//		// écrit les vrais pixels en FITS
//		out.writeFITS(file+".fits");
//		// l'écrit en JPG
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
//		// essaye de récupérer l'ancien fichier s'il existe
//		Fits out;
//		out = findFits(file+".fits");
//		// si le fichier fits existe déjà et qu'on ne doit pas l'écraser
//		if (out != null && !overwrite) {
//			// on re-créé juste le jpg
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
//		// écrit les vrais pixels en FITS
//		out.writeFITS(file+".fits");
//		// l'écrit en JPG
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
//		// essaye de récupérer l'ancien fichier s'il existe
//		Fits out;
//		out = findFits(file+".fits");
//		// si le fichier fits existe déjà et qu'on ne doit pas l'écraser
//		if (out != null && !overwrite) {
//			// on re-créé juste le jpg
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
	
	/** Création d'un losange terminal
	 * @param file Nom du fichier de destination (complet mais sans l'extension)
	 * @param order Ordre healpix du losange
	 * @param npix Numéro Healpix du losange
	 * @param fast méthode rapide (non biblinéaire, non moyennée)
	 * @param fading utilisation d'un fading pour les bords/recouvrements d'images
	 * @return null si rien trouvé pour construire ce fichier
	 */
	Fits createLeaveHpx(HpxBuilder hpx, String file,int order,long npix,boolean fast,boolean fading) throws Exception {
		long t = System.currentTimeMillis();
		
		Fits oldOut=null;
		boolean isInList = isInList(order,npix);
		if( !isInList /* || coaddMode==DescPanel.KEEP */ ) {
		   oldOut = findFits(file+".fits");
		   if( !(oldOut==null && isDescendant(order,npix) ) ) return oldOut;
		}
				
		int nside_file = Util.nside(order);
		int nside = Util.nside(order+ORDER);
		
		Fits out;
//		out = hpx.buildTestHealpix(nside_file, npix, nside); else
		if( DSS ) out = hpx.buildDSSHealpix(nside_file, npix, nside);
        else out = hpx.buildHealpix(nside_file, npix, nside, fast,fading);
        
		if( out !=null ) {
		   
		   if( coaddMode!=DescPanel.REPLACETILE ) {
		      if( oldOut==null ) oldOut = findFits(file+".fits");
		      if( oldOut!=null ) {
		         if( coaddMode==DescPanel.AVERAGE ) out.coadd(oldOut);
		         else if( coaddMode==DescPanel.KEEP ) out.mergeOnNaN(oldOut);
		         else if( coaddMode==DescPanel.OVERWRITE ) { oldOut.mergeOnNaN(out); out=oldOut; }
		      }
		   }
		   cds.tools.Util.createPath(file);

		   // écrit les vrais pixels
		   if (bitpix!=0) out.writeFITS(file+".fits");

		   // écrit les pixels couleurs
		   else {
		      out.inverseYColor();
		      out.writeJPEG(file+".jpg");
		   }
		   
		   if( npix%10 == 0 || DEBUG ) Aladin.trace(4,"DBBuilder.createLeaveHpx("+order+"/"+npix+") "+DescPanel.COADDMODE[coaddMode]+" in "+(System.currentTimeMillis()-t)+"ms");
		}

		return out;
	}
	
	/** Création si nécessaire des répertoires et sous-répertoire du fichier 
	 * passé en paramètre */
//	public void createPath(String filename) {
//	   File f;
//	   // Pour accélerer, on teste d'abord l'existence éventuelle du dernier répertoire
//	   int i = filename.lastIndexOf('/');
//	   if( i<0 ) return;
//	   f = new File( filename.substring(0,i) ) ;
//	   if( f.exists() ) return;
//
//	   // Test et création si nécecessaire des répertoires
//	   for( int pos=filename.indexOf('/',3); pos>=0; pos=filename.indexOf('/',pos+1)) {
//	      f = new File( filename.substring(0,pos) );
//	      if( !f.exists() ) f.mkdir();
//	   }
//	}

	/** Recherche et chargement d'un losange déjà calculé (présence du fichier Fits 8 bits).
	 *  Retourne null si non trouvé
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

	/** Recherche et chargement d'un losange déjà calculé (présence du fichier Fits 8 bits).
	 *  Retourne null si non trouvé
	 * @param filefits Nom du fichier fits (complet avec extension)
	 */
	Fits findFits(String filefits) throws Exception {
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
		
		// l'écrit en JPG
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
	
	public void setCoadd(int coaddMode) {
	   this.coaddMode = coaddMode;
	}
	
	public void setHpixTree(HpixTree hpixTree) {
	   this.hpixTree = hpixTree;
	}

	public void setBScaleBZero(double bscale, double bzero) {
		this.bscale = bscale;
		this.bzero = bzero;
	}
	
	public void setBlank(double blank) {
		this.blank = blank;
	}
	
	/**
	 * @return the bscale
	 */
	public double getBscale() {
		return bscale;
	}
	
	/**
	 * @return the bzero
	 */
	public double getBzero() {
		return bzero;
	}

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
	public double getBlank() {
		return blank;
	}

	
	public void stop() {
		stopped = true;
		destroyThread();
		
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

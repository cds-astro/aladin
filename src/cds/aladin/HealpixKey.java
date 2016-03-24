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


package cds.aladin;

import healpix.essentials.FastMath;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import cds.fits.Fits;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Hpix;

/**
 * G�re un losange Healpix pour un PlanBG
 * @author Ana�s Oberto + Pierre Fernique [CDS]
 */
public class HealpixKey implements Comparable<HealpixKey> {

   static final int UNKNOWN           = 0;
   static final int ASKING            = 1;
   static final int TOBELOADFROMCACHE = 2;
   static final int TOBELOADFROMNET   = 3;
   static final int LOADINGFROMCACHE  = 4;
   static final int LOADINGFROMNET    = 5;
   static final int READY             = 6;
   static final int ERROR             = 7;
   static final int ABORTING          = 8;
   static final int PURGING           = 9;


   static public int NBSTATUS = PURGING+1;

   static public final String [] STATUS = { "UNKOWN","ASKING","TOBELOADFROMCACHE","TOBELOADFROMNET",
      "LOADINGFROMCACHE","LOADINGFROMNET",
      "READY","ERROR","ABORTING","PURGING" };

   protected int status=UNKNOWN;  // status courant du losange
   protected long timer;        // Date de la derni�re utilisation du losange, -1 si jamais encore utilis�
   protected long timerLoad;    // Date du chargement
   protected int priority=-1;   // Priorit� pour le chargement

   protected PlanBG planBG;     // Plan d'appartenance de ce losange
   protected int order;         // Num�ro de r�solution HEALPIX
   protected long npix;         // NNum�ro du pixel HEALPIX dans la r�solution nside
   protected int z;             // Dans le cas d'un HiPS cube, l'indice de la frame
   protected Hpix hpix;
   protected String fileCache;  // path partiel du fichier  SURVEY/NorderXX/DirYY/NpixYYyyyy
   protected String fileNet;    // nom du fichier "� l'ancienne" (� virer d�s que la base est Ok)

   protected int width;           // Taille du losange en pixels (un cot�)
   protected int height;          // tjrs = � width sauf pour HealpixAllsky
   protected byte pixels[];       // Tableau des pixels 8 bits avant cr�ation de img. =null si img!=null (ordre des lignes � la java)
   protected byte pixelsOrigin[]; // Tableau des pixels true bits. (il s'agit d'un cache susceptible d'�tre lib�r� et recharg�)
   protected int rgb[];           // Tableau des pixels ARGB
   protected Image imgBuf;        // Derni�re image cr��e (si assez de place en m�moire sinon null)
   protected int imgID=-2;        // Num�ro de l'image du plan de r�f�rence
   protected byte stream[];       // stream repr�sentant le fichier JPEG ou FITS (planBG.color=true)
   protected boolean truePixels;  // true si les pixels 8 bits ont �t� calcul�s depuis du FITS true pixels, et non du JPEG ou du PNG
   protected boolean alreadyCached; // true si le losange est en cache
   protected boolean allSky;      // true si ne doit jamais �tre effac�
   protected boolean fromNet=true;// true si le losange provient du r�seau et non du cache
   protected int timeStream;      // stat de lecture du stream,
   protected int timeNet;         // stat de lecture du stream via le net
   protected long sizeStream;     // Nombre d'octets du stream
   private int timeJPEG, timePixel;  // stat de la cr�ation de l'image, de l'extraction des pixels
   private int typeColor;         // Mode couleur JPEG ou PNG

   protected HealpixKey fils[] = null; // Si pr�sence de fils en sous-�chantillonnage
   protected HealpixKey anc=null;      // Anc�tre qui dispose des pixels, null sinon
   protected int parente=0;            // Si losange issu d'un anc�tre (sous-�chantillonnage), longueur de la filiation
   protected Point p = new Point(0,0);   // Position du pixel en haut � gauche dans le tableau pixels[] de l'anc�tre

   int oiz=-1;
   int vHashCode=-1;

   protected HealpixKey() { }

   // Mode de r�cup�ration d'un true pixel (voir getTruePixel(...)
   static final public int NOW = 0;                // Retourne la valeur du pixel quitte � attendre le chargement des donn�es
   static final public int ONLYIFRAMAVAIL = 1;     // Retourne la valeur du pixel si les donn�es sont disponibles en m�moire
   static final public int ONLYIFDISKAVAIL = 2;    // Retourne la valeur du pixel quitte � attendre le chargement des donn�es si elles sont locales (cache, ou locales)
   static final public int PIX8 = 3;               // Retourne la valeur du pixel 8 bits si pr�sent en m�moire

   // Type de tuiles
   static protected final int JPEG=0;
   static protected final int FITS=1;
   static protected final int TSV =2;
   static protected final int XML =3;
   static protected final int PNG =4;
   static protected final int IDX =5;

   static final public String[] EXT = { ".jpg",".fits",".tsv",".xml",".png","" };

   protected int extCache=JPEG;         // Format d'image pour le cache
   protected int extNet=JPEG;           // Format d'image pour le net

   protected HealpixKey(PlanBG planBG) { this.planBG = planBG; }


   protected HealpixKey(PlanBG planBG,int order, long npix) {
      this(planBG,order,npix,ASYNC);
   }

   // Diff�rents mode de construction possible
   static public final int NOLOAD = 0;
   static public final int ASYNC  = 1;
   static public final int SYNC   = 2;
   static public final int SYNCONLYIFLOCAL   = 3;
   static public final int TESTNET   = 4;

   /**
    * Cr�ation d'un losange Healpix
    * @param planBG Plan d'appartenance
    * @param order  order heapix (nside = 2^order)
    * @param npix   num�ro healpix du pixel � l'ordre indiqu�
    * @param z      profondeur (dans le cas d'un cube, 0 sinon)
    * @param mode : NOLOAD - pas de chargement, ASYNC - chargement asynchrone, SYNC - chargement synchrone,
    *               SYNCONLYIFLOCAL - chargement synchrone si acc�s local, sinon asynchrone
    *               NETTEST - Uniquement pour tester le temps de chargement r�seau
    */
   protected HealpixKey(PlanBG planBG,int order, long npix,int mode) {
      this(planBG,order,npix,(int)planBG.getZ(),mode);
   }
   protected HealpixKey(PlanBG planBG,int order, long npix,int z,int mode) {
      this.planBG = planBG;
      this.order=order;
      this.npix=npix;
      this.z=z;
      hpix = new Hpix(order,npix,planBG.frameOrigin);
      //      corners = computeCorners();

      // Pas de chargement demand�e
      if( mode==NOLOAD ) return;

      alreadyCached=allSky=false;
      setStatus(ASKING);
      if( !allSky ) planBG.nbCreated++;
      resetTimer();
      extCache=extNet=planBG.getTileMode();
      fileNet = getFileNet();
      fileCache = getFileCache();

      // Chargement imm�diat des donn�es
      try {

         // Juste pour un test - temps r�cup�r� dans "timeStream"
         if( mode==TESTNET ) {
            stream = loadStream(planBG.url+"/"+fileNet);
            if( stream!=null ) sizeStream = stream.length;
            stream=null;
         }

         // Normal
         else if(  mode==SYNC ||
               (mode==SYNCONLYIFLOCAL && (planBG.useCache && isCached() || planBG.isLocalAllSky())) ) loadNow();
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }

   /** Juste pour dessiner la grille */
   protected HealpixKey(int order, long npix, int frame) {
      this.order=order;
      this.npix=npix;
      hpix = new Hpix(order,npix,frame);
   }


   protected String getFileNet() {
      return getFilePath(null,order,npix,z)+ EXT[extNet];
   }

   protected String getFileCache() {
      return getFilePath(planBG.getCacheName(),order,npix,z)+ EXT[extCache];
   }

   /** Cr�ation d'un losange Healpix en fonction de son p�re (sous-�chantillonnage)
    * @param father le losange p�re
    * @param child le num�ro du fils (0,1,2 ou 3) dans l'ordre Healpix NESTED
    */
   protected HealpixKey(HealpixKey father,int child) {
      planBG = father.planBG;
      order=father.order+1;
      npix=father.npix*4+child;
      hpix = new Hpix(order,npix,planBG.frameOrigin);
      anc=father.anc;
      if( anc==null ) anc=father;
      width=height=father.width/2;
      int offsetX = child==2 || child==3 ? width : 0;
      int offsetY = child==1 || child==3 ? width : 0;
      p = new Point(father.p.x + offsetX ,father.p.y + offsetY);
      parente=father.parente+1;
      resetTimer();
      pixels=null;
      rgb = null;
      alreadyCached=true;
      //      allSky=false;
      allSky=father.allSky;
      setStatus(UNKNOWN);
   }

   /** Cr�ation dynamique des 4 losanges fils � partir du losange courant */
   protected HealpixKey [] getChild() {
      if( getStatus()!=READY ) return null;
      try {
         if( fils!=null && fils[0].getStatus()==READY && fils[1].getStatus()==READY
               && fils[2].getStatus()==READY && fils[3].getStatus()==READY) return fils;
      } catch( Exception e ) { }

      HealpixKey f1 [] = new HealpixKey[4];
      for( int child=0; child<4; child++ ) {
         HealpixKey f;
         if( this instanceof HealpixKeyPol ) f = new HealpixKeyPol((HealpixKeyPol)this,child);
         else f = new HealpixKey(this,child);
         f.setStatus(READY);
         f1[child] = f;
      }
      fils = f1;
      return fils;
   }

   /** G�n�ration du tableau des pixels d'un losange issu d'une filiation en fonction
    * des pixels de son anc�tre
    */
   protected byte [] getPixelFromAncetre() throws Exception {
      byte [] pixAnc = anc.pixels==null ? anc.getPixelFromAncetre() : anc.pixels;
      byte [] pixels = new byte[width*height];

      for( int y=0; y<width; y++) {
         for( int x=0; x<width; x++) {
            pixels[ y*width+x ] = pixAnc[ (y+p.y)*anc.width + (x+p.x) ];
         }
      }

      return pixels;

   }

   /** G�n�ration du tableau des pixels rgb d'un losange issu d'une filiation en fonction
    * des pixels de son anc�tre */
   private int [] getPixelFromAncetreRGB() throws Exception {
      int [] rgb = new int[width*width];
      int [] pixAnc = anc.rgb!=null ? anc.rgb : anc.getPixelFromAncetreRGB();
      for( int y=0; y<width; y++) {
         for( int x=0; x<width; x++) {
            rgb[ y*width+x ] = pixAnc[ (y+p.y)*anc.width + (x+p.x) ];
         }
      }
      return rgb;
   }

   /** Retourne le nom de fichier complet en fonction du survey, de l'ordre et du num�ro healpix */
   static protected String getFilePath(String survey,int order, long npix,int z) {
      return
            (survey!=null ? survey + "/" :"") + cds.tools.pixtools.Util.getFilePath(order, npix, z);
      //      "Norder" + order + "/" +
      //      "Dir" + ((npix / 10000)*10000) + "/" +
      //      "Npix" + npix;
   }

   /** Retourne le num�ro du losange sous la forme : Norder+parente/npix[-] (pour d�bogage) */
   protected String getStringNumber() {
      return order+"/"+npix+(z<=0?"":"_"+z);
   }

   /** Pour du debuging */
   @Override
   public String toString() {
      String code = status==HealpixKey.LOADINGFROMNET || status==HealpixKey.LOADINGFROMCACHE ? ">>" :
         status==HealpixKey.TOBELOADFROMNET || status==HealpixKey.TOBELOADFROMCACHE ? " >" : " .";

      //      long t = (int)(getAskRepaintTime()/1000L);
      return code+"["+Util.align(priority+"",5)+"] "+
      Util.align(getStringNumber()+(fils!=null?">":" "),8)+
      Util.align(getLongFullMem(),8)+
      (truePixels ? " truePix ":"         ")+
      Util.align(getStatusString(),16)+
      ( timer==-1 ? -1 : getCurrentLiveTime()/1000 ) +
      //             "/"+t + "s => "+VIE[-getLive()]+
      "s => "+VIE[-getLive()]+
      (getStatus()==READY?(fromNet?" Net":" Cache")+":"+timeStream+"+"+
            timeJPEG+"+"+timePixel+"ms" : "");
   }

   //   private String toStringCorners() {
   //      if( corners==null ) return "    ";
   //      StringBuffer res = new StringBuffer();
   //      for( int i=0; i<corners.length; i++ ) {
   //         res.append( corners[i]==null ? '-' :
   //            Double.isNaN(corners[i].al) || Double.isNaN(corners[i].del) ? 'N' :
   //            corners[i].al==0 && corners[i].del==0? '0' : '1' );
   //      }
   //      return res.toString();
   //   }
   //
   //   private String toStringCoins() {
   //      if( coins==null ) return "    ";
   //      StringBuffer res = new StringBuffer();
   //      for( int i=0; i<coins.length; i++ ) {
   //         res.append( coins[i]==null ? '-' :
   //            coins[i].x==0 && coins[i].y==0? '0' : '1' );
   //      }
   //      return res.toString();
   //   }

   /** Lib�ration des ressources d'un losange et de ses fils (pour aider GC)
    * retourne le nombre de losanges pour qui vraisemblablement il y a eu lib�ration de m�moire
    * pour forcer un �ventuel gc() */
   protected int free() {
      //      if( allSky ) return 0;
      int rep = 0;
      try {
         rep+=free1();
         rep+=filsFree();
      } catch( Exception e ) { }
      return rep;
   }

   /** Lib�ration des ressources (sans la r�cursivit� - voir HealpixKeyPol) */
   protected int free1() {
      int rep=0;
      int status = getStatus();

      if( parente==0 ) {
         planBG.nbFree++;
         if( status==LOADINGFROMCACHE || status==LOADINGFROMNET ) abort();  // Arr�t de lecture
         else if( status==READY && planBG.useCache) write();                // Sauvegarde en cache
      }

      setStatus(UNKNOWN);
      if( pixels!=null )  { pixels=null;  rep=1; }
      if( rgb!=null ) {  rgb=null; rep=1; }
      if( imgBuf!=null ) {  imgBuf.flush(); imgBuf=null;  rep=1; }
      if( pixelsOrigin!=null ) { pixelsOrigin=null; rep=1; }

      return rep;
   }

   /** Lib�re les 4 fils
    * retourne le nombre de losanges pour qui vraisemblablement il y a eu lib�ration de m�moire
    * pour forcer un �ventuel gc() */
   protected int filsFree() {
      int rep=0;
      if( fils!=null ) {
         for( int i=0; i<4; i++ ) { rep += fils[i].free(); }
         fils=null;
      }
      return rep;
   }

   /** purge des fils r�cursivement : suppression de l'image imgBuf ou suppression total */
   protected boolean purgeFils() {

      // Est-ce que les 4 fils peuvent �tre purg�s ?
      boolean rep=true;
      if( fils!=null ) {
         for( int i=0; i<4; i++ ) {
            if( fils[i]!=null ) rep = rep && fils[i].purgeFils();
         }
      }

      // Tous les fils on �t� purg�
      if( rep ) {

         // Il s'agit de l'anc�tre
         if( parente==0 ) {  fils=null; return rep; }

         // On purge le losange ?
         if( getLive()==DEATH ) { free(); return true; }
      }
      return false;
   }

   /** Nettoyage de la m�moire temporaire, ainsi que pour les fils */
   protected void clearBuf() {
      if( pixelsOrigin!=null ) {
         //         Aladin.trace(4,"clearBuf.pixelsOrigin pour "+this);
         pixelsOrigin=null;
      }
      if( imgBuf==null ) return;
      if( fils!=null ) {
         for( int i=0; i<4; i++ ) {
            if( fils[i]!=null ) fils[i].clearBuf();
         }
      }
      imgBuf=null;
   }

   boolean retry=false;

   /** Chargement depuis le r�seau */
   protected void loadFromNet() {
      setStatus(LOADINGFROMNET);
      try {

         long t = System.currentTimeMillis();
         String fileName = planBG.url+"/"+fileNet;

         char c = planBG.url.charAt(planBG.url.length()-1);
         if( c=='\\' || c=='/' ) fileName = planBG.url+fileNet;  // Directement sur root

         //System.out.println("Start load from NET "+fileName+"...");
         planBG.nByteReadNet+=loadNet(fileName);
         alreadyCached=false;
         resetTimer();
         setTimerLoad();
         setStatus(READY);
         planBG.nbLoadNet++;
         parente=0;
         fromNet=true;
         planBG.cumulTimeLoadNet+= timeNet;
         planBG.cumulTimeStream += timeStream;
         planBG.cumulTimeJPEG += timeJPEG;
         planBG.cumulTimePixel += timePixel;
         planBG.askForRepaint();
         Aladin.trace(5,"HealpixKey.LoadFromNet() in "+(System.currentTimeMillis()-t)+"ms : "+this);

      } catch( Throwable e ) {
         pixels=null;
         rgb=null;
         if( getStatus()!=ABORTING ) {

            //            System.err.println("Throwable: "+e.getMessage());
            //            e.printStackTrace();

            // Le test sur FileNotFoundException ne peut suffire car en keepAlive il n'est pas g�n�r�
            boolean notFoundError = e instanceof FileNotFoundException ||
                  e.getMessage()!=null && e.getMessage().indexOf("HTTP response code: 40")>=0;

                  // Peut-on retenter sur un autre site mirroir
                  if( !notFoundError && !retry && planBG.checkSite(true) ) {
                     retry=true;
                     loadFromNet();
                     return;
                  }

                  setStatus(ERROR);
                  if( this instanceof HealpixAllsky ) planBG.askForRepaint();
                  //            if( Aladin.levelTrace>=3 ) System.err.println("HealpixKey.loadFromNet error: "+e.getMessage());
         }
      }
   }

   protected long loadNet(String fileName) throws Exception {
      long n=0;
      if( extNet==JPEG || extNet==PNG ) n=loadJpeg(fileName);
      else n=loadFits(fileName);
      if( !planBG.useCache ) stream=null; // Inutile de le conserver puisqu'on ne le cachera pas
      return n;
   }

   /** Chargement du losange depuis le cache */
   protected void loadFromCache() {

      setStatus(LOADINGFROMCACHE);
      try {
         long t = System.currentTimeMillis();
         String pathName = planBG.getCacheDir();
         if( pathName==null ) throw new Exception("Cache not ready");
         pathName = pathName+Util.FS+fileCache;
         try { updateCacheIfRequired(1000); } catch( Exception e ) {}
         try {
            planBG.nByteReadCache+=loadCache(pathName);
            alreadyCached=true;
            Aladin.trace(5,"HealpixKey.LoadFromCache() in "+(System.currentTimeMillis()-t)+"ms "+this);
            resetTimer();
            setTimerLoad();
            setStatus(READY);
            parente=0;
            planBG.nbLoadCache++;
            fromNet=false;
            planBG.cumulTimeLoadCache+=(System.currentTimeMillis()-t);
            planBG.askForRepaint();
            planBG.touchCache();
         } catch( Exception e) { e.printStackTrace(); (new File(pathName)).delete(); throw e;}   // Sans doute fichier erron�

      } catch( Exception e ) {
         pixels=null;
         alreadyCached=false;
         if( getStatus()!=ABORTING ) {
            setStatus(TOBELOADFROMNET);
            if( Aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
   }

   protected void updateCacheIfRequired(int time) throws Exception { }


   /** Chargement synchrone */
   protected void loadNow() throws Exception {

      if( getStatus()!=READY ) {
         // Dans le cache ?
         if( planBG.useCache && isCached() ) {
            setStatus(TOBELOADFROMCACHE);
            loadFromCache();
         }

         // Pas dans le cache, ou innaccessible par le cache
         if( !(planBG.useCache && isCached()) ) {
            setStatus(TOBELOADFROMNET);
            loadFromNet();
         }
      } else resetTimer();

      if( getStatus()==READY ) {
         if( allSky ) planBG.setTileOrder( getLosangeOrder() );
         if( planBG.isTruePixels() ) loadPixelsOrigin(ONLYIFDISKAVAIL);  // En SYNC, on pr�f�re tout de suite garder les pixels d'origine en RAM
         if( planBG.useCache ) write();
      }
   }

   protected long loadCache(String pathName) throws Exception {
      long n;
      if( extCache==JPEG || extCache==PNG ) n=loadJpeg(pathName);
      else n=loadFits(pathName);
      stream=null;     // Inutile de conserver le stream puisqu'on le prend du cache

      File f = new File(pathName);
      f.setLastModified(System.currentTimeMillis());
      return n;
   }

   /** Retourne true si le losange est dans le cache */
   protected boolean isCached() {
      if( alreadyCached ) return true;

      String pathName = planBG.getCacheDir();
      if( pathName==null ) return false;

      pathName = pathName+Util.FS+fileCache;
      File f= new File(pathName);
      if( f.exists() && f.canRead() ) {
         alreadyCached=true;
         return true;
      }

      return false;
   }

   /** Retourne true si le losange peut/doit �tre cach� */
   protected boolean shouldBeCached() {
      if( alreadyCached ) return false;
      if( getStatus()!=READY ) return false;
      return true;
   }

   /** Ecriture du losange en cache
    * @return le nombre de bytes effectivement �crits */
   protected void write() {
      if( alreadyCached ) return;           // Inutile
      try {
         long t = System.currentTimeMillis();
         int n=writeCache();
         planBG.nByteWriteCache+=n;
         planBG.nbWriteCache++;
         planBG.cumulTimeWriteCache+=(System.currentTimeMillis()-t);
         planBG.addInCache(n/1024);
      } catch( Exception e ) {}
      alreadyCached = true;
   }

   protected int writeCache() throws Exception {
      int n=0;
      if( stream!=null /* extCache==JPEG */ ) {
         n=writeStream();
         stream=null;        // Inutile de conserver le stream plus longtemps
      } else n=writeFits();
      return n;
   }

   /** Interrompt les chargements */
   protected void abort() {
      //      System.out.println("*** ABORTING "+this);
      if( npix==-1 ) {
         //         if( Aladin.levelTrace>=3 ) {
         //            Aladin.trace(4,"Tentative d'ABORT sur un allsky");
         //            try { throw new Exception(); } catch( Exception e ) { e.printStackTrace(); }
         //         }
         return;        // on n'aborte pas les allsky
      }
      planBG.nbAborted++;
      setStatus(ABORTING,true);
   }

   /** Ouverture du stream pour l'�criture (dans le cache) */
   private FileOutputStream openOutputStream() throws Exception {
      String pathName = planBG.getCacheDir()+Util.FS+fileCache;

      // On supprime une ancienne �ventuelle version
      File f = new File(pathName);
      if( f.exists() )  f.delete();

      Util.createPath(pathName);
      FileOutputStream ois = new FileOutputStream(pathName);
      return ois;
   }

   static final int SIZESLOW = 512;
   static final int SIZEFAST = 8*1024;

   // Force le ralentissement du chargement du losange dans le cas d'un clic and drag
   private boolean slowDown() {
      return planBG.aladin.view!=null && planBG.aladin.view.mustDrawFast();
   }

   /**
    * Lecture de la totalite du flux dans un tableau de bytes
    * sans savoir a priori la taille du flux
    * La lecture se fait dans un vecteur de blocs que l'on concatene
    * a la fin de la lecture
    * @return le tableau de byte
    */
   public byte [] readFully(MyInputStream in,boolean fastLoad) throws Exception {

      Vector<byte[]> v = new Vector<byte[]>(10);
      Vector<Integer> vSize = new Vector<Integer>(10);
      int n=0,m=0,i=0,j=0;
      byte [] tmp;
      long t = System.currentTimeMillis();
      //       long t0=t;
      boolean letTimeForDragging = slowDown();
      //       boolean oletTime = letTimeForDragging;
      //       boolean slowdown=false;
      int size=SIZESLOW;

      tmp = new byte[size];
      while( (n=in.read(tmp,0,size))!=-1 ) {
         i++;
         // System.out.println("Je lis "+n+" octets (tranche "+i+")");
         if( getStatus()==ABORTING ) {
            //             System.out.println("readFully aborted for "+this);
            throw new Exception("readFullly aborted !");
         }
         v.addElement(tmp);

         long t1 = System.currentTimeMillis();
         if( t1-t>10 ) {
            letTimeForDragging = slowDown();
            //             if( oletTime!=letTimeForDragging ) slowdown=true;
            t=t1;
         }

         // Faut laisser un peu souffler la b�te => sinon il y a des �-coups dans la souris
         if( !fastLoad && letTimeForDragging ) {
            Util.pause(10);
            size=SIZESLOW;
         } else size=SIZEFAST;

         vSize.addElement( new Integer(n) );
         m+=n;
         tmp = new byte[size];
      }

      // System.out.println("La taille totale est de m="+m);
      byte [] tab = new byte[m];
      j=v.size();
      for( n=i=0; i<j; i++ ) {
         tmp = v.elementAt(i);
         m = vSize.elementAt(i).intValue();
         // System.out.println("Je copie la tranche "+(i+1)+" => "+m+" octets");
         System.arraycopy(tmp,0,tab,n,m);
         n+=m;

      }
      //       Aladin.trace(4,"Load ("+(fastLoad?"faster":"fast")+(slowdown?" => slowdown":"")+" mode) "+getStringNumber()+" in "+(System.currentTimeMillis()-t0)+"ms");
      return tab;
   }



   /** Chargement total du fichier en m�moire soit en un coup si fichier local, soit morceau
    * par morceau via le r�seau
    * @param filename le nom du fichier, �ventuellement une URL. Le fichier peut �tre gzipp�
    * @param skip le nombre d'octets � sauter en d�but de fichier
    */
   protected byte [] loadStream(String filename) throws Exception { return loadStream(filename,0); }
   protected byte [] loadStream(String filename,int skip) throws Exception {
      byte [] buf;
      boolean local=true;
      long t1 = Util.getTime();
      //       planBG.aladin.trace(4,"loadStream("+filename+")...");
      MyInputStream dis=null;
      boolean fastLoad = this instanceof HealpixAllsky;

      // Fichier distant
      if( filename.startsWith("http://") || filename.startsWith("https://") ) {
         local=false;
         try {
            dis = Util.openStream(filename,false,10000);
            if( skip>0 ) dis.skip(skip);
            buf = readFully(dis, fastLoad );
         } finally { if( dis!=null ) dis.close(); }

         // Fichier local (zipp� ou non)
      } else {
         RandomAccessFile f = null;

         // est-ce que le fichier est gzipp� ?
         try {
            f = new RandomAccessFile(filename,"r");
            byte [] c = new byte[8];
            f.readFully(c);

            // Detection de JPEG
            if( (c[0] & 0xFF)==255 && (c[1] & 0xFF)==216 ) typeColor = JPEG;
            else if( (c[0] & 0xFF)==137 && c[1]==80 && c[2]==78 && c[3]==71
                  && c[4]==13 && c[5]==10 && c[6]==26 && c[7]==10)  typeColor = PNG;

            if( (c[0] & 0xFF)==31 && (c[1] & 0xFF)==139 ) {
               //                Aladin.trace(4,"HealpixKey.loadStream: "+filename+" gzipped => reading by MyInputStream rather than RandomAccessFile");
               FileInputStream fgz = new FileInputStream(new File(filename));
               dis = new MyInputStream( new GZIPInputStream(fgz) );
               if( skip>0 ) dis.skip(skip);
               buf = readFully(dis, fastLoad);

               // Le fichier est normal
            } else {
               f.seek(skip);
               buf = new byte[(int)(f.length()-skip)];
               f.readFully(buf);
            }
         } finally {
            if( f!=null ) f.close();
            if( dis!=null ) dis.close();
         }
      }

      // Stats de lecture
      int t = (int)(Util.getTime()-t1);
      if( local ) timeStream = t;
      else timeNet = t;

      return buf;
   }

   /** Chargement du losange sous forme de JPEG
    * @return le nombre de bytes du flux JPEG
    */
   private int loadJpeg(String filename) throws Exception {
      stream = loadStream(filename);
      truePixels=false;
      int n=stream.length;
      long t1=Util.getTime();
      Image img = Toolkit.getDefaultToolkit().createImage(stream);
      if( extCache!=JPEG && extCache!=PNG || !planBG.useCache ) stream=null;
      boolean encore=true;
      while( encore ) {
         try {
            if( getStatus()==ABORTING ) {
               throw new Exception("Aborting");
            }
            MediaTracker mt = new MediaTracker(Aladin.aladin);
            mt.addImage(img,0);
            mt.waitForID(0);
            encore=false;
         } catch( InterruptedException e ) { }
      }
      width =img.getWidth(Aladin.aladin);
      height=img.getHeight(Aladin.aladin);
      if( width==-1 ) { throw new Exception("width = -1"); }
      timeJPEG = (int)(Util.getTime()-t1);

      // D�termination a posteriori de la couleur ou non du survey
      if( planBG.colorUnknown && this instanceof HealpixAllsky ) {
         planBG.color = Util.isColoredImage(stream);
         planBG.colorUnknown = false;
         planBG.aladin.trace(4,"HealpixKey.loadJpeg("+filename+") => "+(typeColor==PNG?"PNG":"JPEG")+" "+(planBG.color?"color":" grey levels"));
      }

      if( !planBG.color ) {
         if( planBG.pixMode==-1 ) planBG.pixMode = extCache==JPEG ? PlanBG.PIX_256 : PlanBG.PIX_255;
         pixels = getPixels(img);
         planBG.setBufPixels8(pixels);
         planBG.pixelMin   = planBG.pixMode == PlanBG.PIX_255 ? 1 : 0;
         planBG.pixelMax   = 255;
         planBG.dataMin    = planBG.pixMode == PlanBG.PIX_255 ? 1 : 0;
         planBG.dataMax    = 255;
      } else {
         if( planBG.pixMode==-1 ) planBG.pixMode = typeColor==PNG ? PlanBG.PIX_ARGB : PlanBG.PIX_RGB;
         planBG.video = PlanImage.VIDEO_NORMAL;
         rgb = getPixelsRGB(img);
      }

      if( this instanceof HealpixAllsky ) {
         if( planBG.cm==null ) planBG.creatDefaultCM();
         planBG.colorPNG = typeColor==PNG;
      }

      return n;
   }

   /** Retourne la valeur du mot cl� FITS key = nnnn */
   protected double getValue(byte[] head,String key) throws Exception {
      int pos,len;
      int n=head.length/80;
      for( int i=0; i<n; i++) {
         String k = new String(head,i*80,8).trim();
         if( !(k.equals(key)) ) continue;
         for( pos=i*80+9; head[pos]==' '; pos++);
         for( len=0; Character.isDigit( (char)head[pos+len]) || (char)head[pos+len]=='-'
               || (char)head[pos+len]=='.' || Character.toUpperCase((char)head[pos+len])=='E'; len++ );
         return Double.parseDouble(new String(head,pos,len));
      }
      throw new Exception();
   }

   /** Retourne le pixel (true bits) d'indice healpixIdxPixel (l'order de r�f�rence est celui du pixel final)
    * @param mode NOW, ONLYIFAVAIL, ONLYIFLOCALAVAIL - pr�t � attendre le chargement des donn�es ou pas
    */
   protected double getPixelValue(long healpixIdxPixel,int mode) {
      long startIdx =  npix * width * width;
      int order = (int)CDSHealpix.log2(width);
      if( planBG.hpx2xy == null || planBG.hpx2xy.length!=width*width ) {
         try { planBG.createHealpixOrder(order); } catch( Exception e ) { return Double.NaN; }
      }
      int idx = planBG.hpx2xy((int)(healpixIdxPixel-startIdx));
      return getPixel(idx,mode);
   }

   /** Retourne la valeur du pixel � l'emplacement idx (idx = y*width + x) */
   protected double getPixel(int idx,int mode) {
      if( mode==PIX8 ) {
         int x = idx%width;
         int y = width - idx/width -1;
         return pixels[ y*width+x ];
      }

      if( !loadPixelsOrigin(mode) ) return Double.NaN;
      resetTimer();
      double pix = planBG.bitpix>0 ? (double)getPixValInt(pixelsOrigin,planBG.bitpix,idx)
            : getPixValDouble(pixelsOrigin,planBG.bitpix,idx);
      if( planBG.isBlank(pix) ) pix = Double.NaN;
      return pix;
   }


   /** Retourne un tableau w*w pixels d'origine (sous forme de byte[]) centr� sur le pixel
    * rep�r� par  healpixIdxPixel, null sinon */
   protected byte [] getSample(Coord coo, int w, byte [] pixelsOrigin, int width ) throws Exception {

      coo = Localisation.frameToFrame(coo,Localisation.ICRS,planBG.frameOrigin);
      if( Double.isNaN(coo.al) || Double.isNaN(coo.del) ) throw new Exception();

      double[] polar = CDSHealpix.radecToPolar(new double[] {coo.al, coo.del});
      long nside = CDSHealpix.pow2(order+ CDSHealpix.log2(width) );
      long healpixIdxPixel = CDSHealpix.ang2pix_nest( nside, polar[0], polar[1]);

      long startIdx =  npix * width * width;

      if( planBG.hpx2xy == null || planBG.hpx2xy.length!=width*width ) {
         planBG.createHealpixOrder( (int)CDSHealpix.log2(width) );
      }
      int idx = planBG.hpx2xy((int)(healpixIdxPixel-startIdx));
      int xc = idx%width;
      int yc = idx/width;
      int n = Math.abs(planBG.bitpix)/8;

      int w2=w/2;
      byte sample[] = new byte[ w*w*n ];
      for( int i=0; i<w*w; i++ ) PlanImage.setPixVal(sample, planBG.bitpix, i, planBG.blank);

      int posSample = 0;
      for( int y=yc-w2; y<yc+(w-w2); y++ ) {
         for( int x=xc-w2; x<xc+(w-w2); x++ ) {
            if( x<0 || x>=width || y<0 || y>=width ) continue;
            int pos = (y*width+x)*n;
            for( int i=0; i<n; i++ ) sample[posSample++] = pixelsOrigin[pos+i];
         }
      }
      return sample;
   }

   /** Retourne true si dans l'ent�te on trouve "COLORMOD  ARGB" */
   protected boolean isARGB(byte[] head){
      int n=head.length/80;
      for( int i=0; i<n; i++) {
         if( new String(head,i*80,8).equals("COLORMOD") ) return true;
      }
      return false;
   }

   // Lecture d'un pixel full bits en position i (num�ro de pixel) dans le tableau t[], codage bitpix
   final private int getPixValInt(byte[]t ,int bitpix,int i) {
      PlanImage.getPixVal1(t, bitpix, i);
      switch(bitpix) {
         case   8: return ((t[i])&0xFF);
         case  16: i*=2;
         return ( ((t[i])<<8) | (t[i+1])&0xFF );
         case  32: i*=4;
         return  ((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF ;
      }
      return 0;
   }

   // Lecture d'un pixel full bits en position i (num�ro de pixel) dans le tableau t[], codage bitpix
   final private double getPixValDouble(byte[]t ,int bitpix,int i) {
      switch(bitpix) {
         case -32: i*=4;
         return Float.intBitsToFloat(( ((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF ));
         case -64: i*=8;
         return Double.longBitsToDouble((((long)( ((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF ))<<32)
               | (( (((t[i+4])<<24) | (((t[i+5])&0xFF)<<16) | (((t[i+6])&0xFF)<<8) | (t[i+7])&0xFF )) & 0xFFFFFFFFL));
      }
      return 0.;
   }

   /** Passage en 8 bits dans le cas d'une lecture d'un fichier FITS avec BITPIX!=8
    * AVEC RETOURNEMENT DES LIGNES
    * @param pixels Les pixels de d�part (full bits)
    * @param bitpix Le code FITS de codage des pixels
    * @param min La valeur min du pixel: tout ce qui est inf�rieur ou �gal sera 0 apr�s conversion
    * @param max La valeur max du pixel: tout ce qui est sup�rieur ou �gal sera 255 apr�s conversion
    * @param pixMOde : PIX_256: 256 valeurs
    *                  PIX_255: 255 valeurs de 1 � 254 - on r�serve le 0 pour la transparence
    * @return la tableau de pixels 8 bits
    */

   final private byte [] to8bits(byte [] pixels, int bitpix, double min, double max, int pixMode) {
      byte [] out = new byte[pixels.length/(Math.abs(bitpix)/8)];

      int range  = pixMode==PlanBG.PIX_255 ? 255 : 256;
      int gapTransp = pixMode==PlanBG.PIX_255 ?   1 :   0;

      double r = range/(max - min);
      range--;
      for( int y=0; y<height; y++ ) {
         for( int x=0; x<width; x++ ) {
            double pixIn = PlanImage.getPixVal1(pixels, bitpix, y*width + x);
            if( planBG.isBlank(pixIn) ) out[ (height-1-y)*width +x ]=0;
            else  out[ (height-1-y)*width +x ] = (byte)( ( (int)( gapTransp+ (pixIn<=min ? 0 : pixIn>=max ? range : (pixIn-min)*r))  ) & 0xFF );
         }
      }

      return out;
   }

   //   final private byte [] to8bits(byte [] pixels, int bitpix, double min, double max) {
   //      byte [] out = new byte[pixels.length/(Math.abs(bitpix)/8)];
   //
   //      double r = 256./(max - min);
   //      if( bitpix>0 ) {
   //         for( int y=0; y<height; y++ ) {
   //            for( int x=0; x<width; x++ ) {
   //               int pixIn = getPixValInt(pixels,bitpix,(y*width)+x);
   //               out[(height-1-y)*width +x] = (byte)( pixIn<=min || planBG.isBlank(pixIn) ?0x00:pixIn>=max ? 0xff : (int)( ((pixIn-min)*r) ) & 0xff);
   //            }
   //         }
   //      } else {
   //         for( int y=0; y<height; y++ ) {
   //            for( int x=0; x<width; x++ ) {
   //               double pixIn = getPixValDouble(pixels,bitpix,(y*width)+x);
   //               out[(height-1-y)*width +x] = (byte)( pixIn<=min || planBG.isBlank(pixIn) ?0x00:pixIn>=max ? 0xff : (int)( ((pixIn-min)*r) ) & 0xff);
   //            }
   //         }
   //      }
   //      return out;
   //   }

   /** Inversion des lignes */
   protected void invLine(byte src[],byte dst[],int bitpix) {
      int nbytes = Math.abs(bitpix)/8;
      for( int h=0; h<height; h++ ){
         System.arraycopy(src,h*width * nbytes, dst,(height-h-1)*width * nbytes, width * nbytes);
      }
   }

   /** Chargement du losange sous forme de FITS. Si le BITPIX n'est pas � 8, il y a conversions des pixels
    * en 8 bits en fonction des valeurs PIXELMIN et PIXELMAX indiqu�es dans l'ent�te. Si ces valeurs
    * sont absentes ou �gales (par exemple 0,0), Aladin effectue un autocut
    * @return le nombre de bytes du flux FITS */
   protected int loadFits(String filename) throws Exception {

      stream = loadStream(filename);

      boolean initPixMode = planBG.pixMode==-1;
      if( initPixMode ) planBG.pixMode = PlanBG.PIX_TRUE;

      // Lecture de l'entete Fits (� la brute - elle ne doit pas d�passer 2880 catact�res)
      byte [] head = new byte[2880];
      System.arraycopy(stream, 0, head, 0, 2880);
      int bitpix=8;
      boolean flagARGB=false;
      double pixelMin=planBG.pixelMin;
      double pixelMax=planBG.pixelMax;
      try {
         width  = (int)getValue(head,"NAXIS1");
         height = (int)getValue(head,"NAXIS2");
         bitpix = (int)getValue(head,"BITPIX");
         if( flagARGB =isARGB(head) ) {
            bitpix=0;
            if( initPixMode ) planBG.pixMode = PlanBG.PIX_ARGB;
            //            System.out.println("HealpixKey FITS in ARGB");
         }
         if( bitpix!=8 && !flagARGB ) truePixels=true;

      } catch( Exception e ) { width=height=512; bitpix=8; }

      int taille=width*height*(Math.abs(bitpix)/8);

      // Lecture FITS couleur ARGB
      if( flagARGB ) {
         planBG.color=true;     // PEUT ETRE PAS VRAIMENT NECESSAIRE
         rgb = new int[width*height];
         for( int i=0; i<width*height; i++ ) {
            rgb[i]  =  (stream[2880 + i*4]   & 0xFF)  << 24
                  | (stream[2880 + i*4+1] & 0xFF)  << 16
                  | (stream[2880 + i*4+2] & 0xFF)  << 8
                  | (stream[2880 + i*4+3] & 0xFF) ;
         }

         // Lecture FITS classique
      } else {

         // Allocation puis lecture
         byte [] in = new byte[taille];
         System.arraycopy(stream, 2880, in, 0, taille);

         boolean flagInit = planBG.bitpix==0 || planBG.flagRecut ;

         if( flagInit ) {
            boolean init = planBG.bitpix==0;
            planBG.bitpix=bitpix;
            planBG.flagRecut=false;
            try { planBG.bScale = getValue(head,"BSCALE"); } catch( Exception e ) { planBG.bScale=1; }
            try { planBG.bZero  = getValue(head,"BZERO");  } catch( Exception e ) { planBG.bZero=0;  }
            try { planBG.blank  = getValue(head,"BLANK"); planBG.isBlank = true; } catch( Exception e ) { planBG.isBlank = false; }

            // Est-ce que j'ai des informations de range et de cut via le fichier de properties ?
            boolean flagPixelRange=false;
            boolean flagPixelCut=false;

            if( init ) {
               if( planBG.pixelRange!=null ) {
                  double [] a = split(planBG.pixelRange);
                  if( a!=null ) {
                     planBG.dataMin =(a[0]-planBG.bZero)/planBG.bScale;
                     planBG.dataMax =(a[1]-planBG.bZero)/planBG.bScale;
                     flagPixelRange=true;
                  }
               }
               if( planBG.pixelCut!=null ) {
                  double [] a = split(planBG.pixelCut);
                  if( a!=null ) {
                     planBG.pixelMin = pixelMin =(a[0]-planBG.bZero)/planBG.bScale;
                     planBG.pixelMax = pixelMax =(a[1]-planBG.bZero)/planBG.bScale;
                     //                     planBG.transfertFct = (int)a[2];
                     flagPixelCut=true;
                  }
               }
            }

            if( !flagPixelCut && !flagPixelRange ) {

               // Je fais un autocut, soit sur la totalit� du losange, soit sur une portion de celui-ci
               // centr�e sur un pixel particulier
               int w = width;
               if( planBG.flagRecutRadius>0 ) {
                  double angRes = CDSHealpix.pixRes( CDSHealpix.pow2(order + CDSHealpix.log2(width) ) ) / 3600;
                  w = (int)( planBG.flagRecutRadius/ angRes );
               }
               if( w>width ) w=width;

               Fits tmp = new Fits(w,w,bitpix);
               if( planBG.isBlank ) tmp.setBlank(planBG.blank);
               tmp.pixels = in;
               if( planBG.flagRecutRadius>0 && w!=width) {
                  try { tmp.pixels = getSample(planBG.flagRecutCoo, w, in, width); }
                  catch( Exception e ) {
                     if( planBG.aladin.levelTrace>=3 ) e.printStackTrace();
                  }
               }
               double [] range = tmp.findAutocutRange(0,0,true);

               if( !flagPixelCut ) {
                  planBG.pixelMin = pixelMin = range[0];
                  planBG.pixelMax = pixelMax = range[1];
               }
               if( !flagPixelRange ) {
                  planBG.dataMin  = range[2];
                  planBG.dataMax  = range[3];
               }

               planBG.aladin.trace(3,"Pixel range detection on "+getStringNumber()+"["+w+"x"+w+"] NaN="+Util.round(range[4]*100,1)
                     +"% => PixelMinMax=["+Util.myRound(planBG.pixelMin)+","+Util.myRound(planBG.pixelMax)+"], " +
                     "DataMinMax=["+Util.myRound(planBG.dataMin)+","+Util.myRound(planBG.dataMax)+"] "+(planBG.isBlank?" Blank="+planBG.blank:"")
                     +" bzero="+planBG.bZero+" bscale="+planBG.bScale);
            } else {
               planBG.aladin.trace(3,"Pixel properties PixelMinMax=["+Util.myRound(planBG.pixelMin)+","+Util.myRound(planBG.pixelMax)+"], " +
                     "DataMinMax=["+Util.myRound(planBG.dataMin)+","+Util.myRound(planBG.dataMax)+"] "+(planBG.isBlank?" Blank="+planBG.blank:"")
                     +" bzero="+planBG.bZero+" bscale="+planBG.bScale);

            }
            planBG.restoreCM();
            if( planBG.aladin.frameCM!=null && planBG.aladin.frameCM.isVisible() ) {
               planBG.aladin.frameCM.showCM();
            }
         }

         pixels = to8bits(in,bitpix,pixelMin,pixelMax, PlanBG.PIX_255);

         if( flagInit && !planBG.color) {
            planBG.pixelsOrigin=in;
            planBG.setBufPixels8(pixels);
            planBG.bitpix=bitpix;
            planBG.npix=Math.abs(bitpix)/8;
            planBG.naxis1=planBG.width=width;
            planBG.naxis2=planBG.height=height;
         }
         in=null;
      }

      return stream.length; //2880+taille;
   }

   private double [] split(String s) {
      double [] a = new double[3];
      StringTokenizer st = new StringTokenizer(s);
      a[0] = Double.parseDouble(st.nextToken());
      a[1] = Double.parseDouble(st.nextToken());
      if( st.hasMoreTokens() ) {
         a[2]=PlanImage.getTransfertFct(st.nextToken());
         if( a[2]==-1 ) a[2]=PlanImage.LINEAR;
      }
      return a;
   }

   /** Construit le pathName du fichier, o� qu'il soit (cache, local, ou r�seau) */
   private String getFileNameForPixelsOrigin() {
      String fileName;
      if( isCached() ) {
         fileName = planBG.getCacheDir();
         if( fileName!=null ) return fileName+Util.FS+fileCache;
      }
      fileName = planBG.url+"/"+fileNet;
      char c = planBG.url.charAt(planBG.url.length()-1);
      if( c=='\\' || c=='/' ) fileName = planBG.url+fileNet;  // Directement sur root
      return fileName;
   }

   /** Chargement (si n�cessaire) des pixels d'origine dans pixelsOrigin[]
    * Attention, il doit n�cessairement s'agir d'un flux FITS */
   protected boolean loadPixelsOrigin(int mode) {

      // D�j� en m�moire
      if( pixelsOrigin!=null ) return true;

      // Ce n'est pas du FITS
      if( !planBG.hasOriginalPixels() ) return false;

      // Coup de bol, le flux original est encore en m�moire
      if( stream!=null ) {
         Aladin.trace(4,"HealpixKey.loadPixelsOrigin: from stream for "+this);
         int taille = width*width* Math.abs(planBG.bitpix)/8;
         pixelsOrigin = new byte[ taille ];
         System.arraycopy(stream, 2880, pixelsOrigin, 0, taille);
         return true;
      }

      // Les donn�es ne sont pas en m�moire, et on n'est pas pr�t � attendre
      if( mode==ONLYIFRAMAVAIL ) return false;

      if( mode==ONLYIFDISKAVAIL ) {
         // Les donn�es ne sont disponible qu'� distance et on n'est pas pr�t � attendre
         if( !planBG.isLocalAllSky() && (!planBG.useCache || !isCached()) ) return false;
      }

      // On lit (ou relit) le fichier, en sautant l'ent�te
      String fileName = getFileNameForPixelsOrigin();
      //      Aladin.trace(4,"HealpixKey.loadPixelsOrigin(): "+getStringNumber()+" from ["+fileName+"]");
      try {
         pixelsOrigin = loadStream(fileName,2880);
      } catch( Exception e) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         return false;
      }
      return true;

   }

   //   private void setSize(int w) { factor(width/w); }
   //   private void factor(int z) {
   //      imgBuf=null;
   //      imgID=-2;
   //      int w = width/z;
   //      int h = height/z;
   //      if( pixels!=null ) {
   //         byte [] pixels1 = new byte[pixels.length/z];
   //         for( int y=0; y<h; y++) {
   //            for( int x=0; x<w; x++ ) {
   //               pixels1[y*w + x] = pixels[ (y*z)*width + x*z ];
   //            }
   //         }
   //         pixels = pixels1;
   //      }
   //      if( rgb!=null ) {
   //         int [] rgb1 = new int[rgb.length/z];
   //         for( int y=0; y<h; y++) {
   //            for( int x=0; x<w; x++ ) {
   //               rgb1[y*w + x] = rgb[ (y*z)*width + x*z ];
   //            }
   //         }
   //         rgb = rgb1;
   //      }
   //      width=w;;
   //      height=h;
   //   }

   /** Ecriture du losange dans le cache sous forme de JPEG couleur
    *  � partir du stream de lecture qui a �t� conserv�
    * @return le nombre d'octets �crits
    */
   protected int writeStream() throws Exception {

      FileOutputStream ois = openOutputStream();
      if( ois==null ) return 0;
      ois.write(stream);
      ois.close();

      //System.out.println("*** Ecriture dans le cache de "+this);
      return stream.length;
   }

   /** Ecriture du losange dans le cache sous forme de FITS 8bits
    * @return le nombre d'octets �crits
    */
   private int writeFits() throws Exception {

      FileOutputStream ois = openOutputStream();
      if( ois==null ) return 0;

      // Ecriture de l'ent�te Fits
      int n=0;
      ois.write(Save.getFitsLine("SIMPLE","T",null) );        n+=80;
      ois.write(Save.getFitsLine("BITPIX","8",null) );        n+=80;
      ois.write(Save.getFitsLine("NAXIS","2",null) );         n+=80;
      ois.write(Save.getFitsLine("NAXIS1",width+"",null) );   n+=80;
      ois.write(Save.getFitsLine("NAXIS2",height+"",null) );  n+=80;
      ois.write(Save.getFitsLine("NORDER",order+"",null) );   n+=80;
      ois.write(Save.getFitsLine("NPIX",npix+"",null) );      n+=80;
      ois.write(Save.getEndBourrage(n));

      // retournement des lignes
      byte out[] = new byte[pixels.length];
      invLine(pixels, out, 8);

      // Ecriture des pixels
      ois.write(out);
      out=null;
      ois.close();

      //System.out.println("*** Ecriture dans le cache de "+this);
      return pixels.length+2880;
   }



   /** Ecriture du losange dans le cache sous forme de FITS 8bits
    * @return le nombre d'octets �crits
    */
   //   private int writeFitsError() throws Exception {
   //      FileOutputStream ois = openOutputStream();
   //      if( ois==null ) return 0;
   //
   //      // Ecriture de l'ent�te Fits
   //      int n=0;
   //      ois.write(Save.getFitsLine("SIMPLE","T",null) );        n+=80;
   //      ois.write(Save.getFitsLine("BITPIX","8",null) );        n+=80;
   //      ois.write(Save.getFitsLine("NAXIS","0",null) );         n+=80;
   //      ois.write(Save.getFitsLine("NAXIS1",0+"",null) );       n+=80;
   //      ois.write(Save.getFitsLine("NAXIS2",0+"",null) );       n+=80;
   //      ois.write(Save.getFitsLine("NORDER",order+"",null) );   n+=80;
   //      ois.write(Save.getFitsLine("NPIX",npix+"",null) );      n+=80;
   //      ois.write(Save.getEndBourrage(n));
   //
   //      ois.close();
   //
   //System.out.println("*** Ecriture dans le cache de "+this);
   //      return 2880;
   //   }

   static private final Component observer = new Label();

   // Reg�n�ration des pixels 8 bits
   protected byte [] getPixels(Image img) throws Exception {
      long t1=Util.getTime();
      byte [] pixels=null;
      BufferedImage imgBuf;

      // Mode PNG B&W => Je ne trouve pas comment faire autrement
      if( planBG.pixMode==PlanImage.PIX_255 ) {
         int [] rgb = getPixelsRGB1(img);
         pixels = new byte[rgb.length];
         for( int i=0; i<pixels.length; i++ ) {
            if( (rgb[i] & 0xFF000000)==0 ) pixels[i]=0;
            else {
               byte p = (byte) (0xFF & rgb[i]);
               if( p==0 ) p=1;
               pixels[i] = p;
            }
         }
         rgb = null;

         // Mode JPEG B&W
      } else {
         imgBuf = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);
         Graphics g = imgBuf.getGraphics();
         g.drawImage(img,0,0,observer);
         g.finalize(); g=null;

         pixels = ((DataBufferByte)imgBuf.getRaster().getDataBuffer()).getData();

         imgBuf.flush(); imgBuf=null;
      }

      timePixel = (int)(Util.getTime()-t1);

      return pixels;
   }

   // Reg�n�ration des pixels RGB
   protected int [] getPixelsRGB(Image img) throws Exception {
      try {
         return getPixelsRGB1(img);

         // On ressaye en faisant de la place avant
      } catch( Throwable e ) {
         planBG.clearBuf();
         return getPixelsRGB1(img);
      }
   }

   // Reg�n�ration des pixels RGB
   protected int [] getPixelsRGB1(Image img) throws Exception {
      long t1=Util.getTime();
      BufferedImage imgBuf = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
      Graphics g = imgBuf.getGraphics();
      g.drawImage(img,0,0,observer);
      g.finalize(); g=null;

      int [] rgb = ((DataBufferInt)imgBuf.getRaster().getDataBuffer()).getData();

      imgBuf.flush(); imgBuf=null;
      timePixel = (int)(Util.getTime()-t1);

      return rgb;
   }


   // Reg�n�ration des pixels RGB
   //   protected int [] getPixelsRGB2(Image img) throws Exception {
   //      long t1=Util.getTime();
   ////      BufferedImage imgBuf = planBG.aladin.getGraphicsConfiguration().createCompatibleImage(width,height);
   //      BufferedImage imgBuf = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
   //      Graphics g = imgBuf.getGraphics();
   //      g.drawImage(img,0,0,Aladin.aladin);
   //      g.finalize(); g=null;
   //
   //      int taille=width*height;
   //      int rgb[] = new int[taille];
   //      imgBuf.getRGB(0, 0, width, height, rgb, 0, width);
   //      imgBuf.flush(); imgBuf=null;
   //      timePixel = (int)(Util.getTime()-t1);
   //
   //      return rgb;
   //   }

   private Object lockStatus = new Object();

   /** Positionne le status du losange NOTREADY, READY... */
   protected void setStatus(int status) { setStatus(status,false); }
   protected void setStatus(int status,boolean flagForce) {
      synchronized( lockStatus ) {
         if( !flagForce && getStatus()==ABORTING ) return;
         this.status=status;
      }
   }

   /** Retourne le status du losange NOTREADY, READY... */
   protected int getStatus() {
      synchronized( lockStatus ) { return status; }
   }

   /** Retourne le status sous forme d'une string */
   protected String getStatusString() { return STATUS[getStatus()]; }

   static final int DEATH      = -1;
   static final int MAYBEDEATH = -2;
   static final int INLIFE     = -3;

   static protected final String [] VIE = { "","DEATH","MAYBEDEATH","INLIFE" };

   /** retourne un code correspondant � l'�tat de vie du losange */
   protected int getLive() {

      int status = getStatus();

      long time = getLiveTime();

      // Ne meurt jamais pour une dur�e de vie n�gative
      if( time==-1 ) return INLIFE;

      // reste en vie pour �viter de redemander le chargement
      if( (status==ERROR || status==LOADINGFROMNET) && parente==0  /* || npix==-1 */ ) return INLIFE;

      if( getCurrentLiveTime()<=time ) return INLIFE;     // En vie
      if( getCurrentLiveTime()>time+2000 ) return DEATH;
      return MAYBEDEATH;
   }

   protected long getLiveTime() { return PlanBG.LIVETIME; }

   /** Retourne le temps de vie courant du losange (en ms) */
   protected long getCurrentLiveTime() {
      return System.currentTimeMillis()-timer;
   }

   //   /** Retourne le temps depuis la derni�re demande de r�affichage (en ms) */
   //   synchronized protected long getAskRepaintTime() {
   //      if( timeAskRepaint==0 ) return 0;
   //      return System.currentTimeMillis()-timeAskRepaint;
   //   }


   //   long timeAskRepaint=0;

   //   /** Positionne le temps de demande de r�affichage forc� */
   //   synchronized protected void setTimeAskRepaint(long t) {
   //      if( fils!=null ) {
   //         for( int i=0; i<4; i++ ) fils[i].setTimeAskRepaint(t);
   //      }
   //      if( timeAskRepaint!=0 ) return;
   //      timeAskRepaint = t;
   //   }

   // /** Reset le timer d'une demande de r�affichage suite � un affichage r�ussi */
   // synchronized protected void resetTimeAskRepaint() {
   //    timeAskRepaint=0;
   // }


   /** Force le losange a d�passer son temps de vie afin d'�tre rapidement test� */
   protected void setOld() {
      timer-=PlanBG.LIVETIME;
   }

   /** reset le timer pour "rajeunir" le losange */
   protected void resetTimer() {
      timer = System.currentTimeMillis();
   }

   // Positionne la date du chargement
   private void setTimerLoad() {
      timerLoad = System.currentTimeMillis();
   }

   /** Agrandissement du losange de val pixels dans toutes les directions */
   private PointD [] grow(PointD [] b,double val) throws Exception {
      int j=0;
      for( int i=0; i<4; i++ ) {
         if( b[i]==null ) j++;
      }
      if( j>1 ) return b;

      PointD [] b1 = new PointD[ b.length ];
      for( int i=0; i<4; i++ ) b1[i] = new PointD(b[i].x,b[i].y);

      for( int i=0; i<2; i++ ) {
         int a= i==1 ? 1 : 0;
         int c= i==1 ? 2 : 3;

         if( b1[a]==null ) {
            int d,g;
            if( a==0 || a==3 ) { d=1; g=2; }
            else { d=0; g=3; }
            b1[a] = new PointD((b1[d].x+b1[g].x)/2,(b1[d].y+b1[g].y)/2);
         }
         if( b1[c]==null ) {
            int d,g;
            if( c==0 || c==3 ) { d=1; g=2; }
            else { d=0; g=3; }
            b1[c] = new PointD((b1[d].x+b1[g].x)/2,(b1[d].y+b1[g].y)/2);
         }
         if( b1[a]==null || b1[c]==null ) continue;

         double angle = FastMath.atan2(b1[c].y-b1[a].y, b1[c].x-b1[a].x);
         double chouilla = val*FastMath.cos(angle);
         b1[a].x-=chouilla;
         b1[c].x+=chouilla;
         chouilla = val*FastMath.sin(angle);
         b1[a].y-=chouilla;
         b1[c].y+=chouilla;
      }
      return b1;
   }

   // Limite du rapport entre la largeur et la hauteur d'un losange healpix pour laquelle
   // on ne tracera pas le losange
   static final private double RAPPORT = 5;

   private int drawFils(Graphics g, ViewSimple v/*,Vector<HealpixKey> redraw*/) { return drawFils(g,v,1/*,redraw*/); }
   private int drawFils(Graphics g, ViewSimple v,int maxParente/*,Vector<HealpixKey> redraw*/) {
      int n=0;
      int limitOrder = CDSHealpix.MAXORDER-10;
      if( width>1 && order<limitOrder && parente<maxParente ) {
         fils = getChild();
         if( fils!=null ) {
            for( int i=0; i<4; i++ ) if( fils[i]!=null ) n+=fils[i].draw(g,v,maxParente/*,redraw*/);
         }
      }
      return n;
   }

   //   /** Cr�ation de l'image du losange � tracer en fonction du tableau des pixels
   //    * Cette image est conserv� s'il y a assez de place en m�moire pour un usage ult�rieur */
   //   private Image createImage() throws Exception {
   //      if( imgVolatileOk(imgBuf) && imgID==planBG.imgID ) { if( !allSky ) nbImgInBuf++; return imgBuf; }
   //
   //      VolatileImage imgv = planBG.aladin.createVolatileImage(width, height);
   //      do {
   //         int code = imgv.validate(planBG.aladin.getGraphicsConfiguration());
   //         switch( code ) {
   //            case VolatileImage.IMAGE_INCOMPATIBLE:
   //               imgv = planBG.aladin.createVolatileImage(width, height);
   //            case VolatileImage.IMAGE_RESTORED:
   //               MemoryImageSource imgSrc;
   //               if( planBG.color ) {
   //                  int pix[] = parente==0 ? rgb : getPixelFromAncetreRGB();
   //                  imgSrc=new MemoryImageSource(width,height,ColorModel.getRGBdefault(), pix, 0, width);
   //               } else {
   //                  byte pix[] = parente==0 ? pixels : getPixelFromAncetre();
   //                  imgSrc=new MemoryImageSource(width,height,planBG.cm, pix, 0, width);
   //               }
   //               Graphics2D g = imgv.createGraphics();
   //               g.drawImage(planBG.aladin.createImage(imgSrc), 0, 0, null);
   //               if( !allSky ) nbImgCreated++;
   //               imgID=planBG.imgID;
   //        }
   //      } while( imgv.contentsLost());
   //
   //      // On a de place en m�moire, on conserve l'image
   //      if( planBG.aladin.enoughMemory() ) imgBuf=imgv;
   //      return imgv;
   //   }
   //
   //
   //   private boolean imgVolatileOk(Image imgBuf) {
   //      VolatileImage imgv = (VolatileImage)imgBuf;
   //      if( imgv==null ) return false;
   //      int code = imgv.validate(planBG.aladin.getGraphicsConfiguration());
   //      return code!=VolatileImage.IMAGE_INCOMPATIBLE
   //      && code!=VolatileImage.IMAGE_RESTORED && !imgv.contentsLost();
   //   }

   /** Cr�ation de l'image du losange � tracer en fonction du tableau des pixels
    * Cette image est conserv� s'il y a assez de place en m�moire pour un usage ult�rieur */
   private Image createImage() throws Exception {
      if( imgBuf!=null && (planBG.color || imgID==planBG.imgID) ) {
         if( !allSky ) planBG.nbImgInBuf++;
         return imgBuf;
      }

      //      BufferedImage img = null;
      Image img = null;

      if( planBG.color ) {
         int pix[] = parente==0 ? rgb : getPixelFromAncetreRGB();
         DataBuffer dbuf = new DataBufferInt(pix, width*height);
         int bitMasks[] = new int[]{0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000   };
         SampleModel sampleModel = new SinglePixelPackedSampleModel(
               DataBuffer.TYPE_INT, width, height, bitMasks);
         ColorModel colorModel = ColorModel.getRGBdefault();
         WritableRaster raster = Raster.createWritableRaster(sampleModel, dbuf, null);
         img = new BufferedImage(colorModel, raster, true, null);

         //         MemoryImageSource x = new MemoryImageSource(width, height, ColorModel.getRGBdefault(), pix, 0, width);
         //         img = Toolkit.getDefaultToolkit().createImage(x);


      } else {
         byte pix[] = parente==0 ? pixels : getPixelFromAncetre();

         //         MemoryImageSource x = new MemoryImageSource(width, height, planBG.getCM(), pix, 0, width);
         //         img = Toolkit.getDefaultToolkit().createImage(x);

         img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED,(IndexColorModel)planBG.getCM());
         WritableRaster wr = ((BufferedImage)img).getRaster();
         wr.setDataElements (0, 0, width, height, pix);

         // N'ARRIVE PAS A ETRE ACCELERE PAR LA CARTE GRAPHIQUE, PAS DE CHANCE !
         //         DataBuffer dbuf = (DataBuffer) new DataBufferByte(pix, width*height);
         //         int[] offsets = new int[] {0};
         //         SampleModel sampleModel = new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, width, offsets);
         //         ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
         //         int[] nBits = {8};
         //         ColorModel colorModel = new ComponentColorModel(cs, nBits, false, true,
         //               Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
         //         int[] offsets = new int[] {0};
         //         img = new BufferedImage(colorModel, raster, true, null);
      }
      //      img.setAccelerationPriority(1f);
      if( !allSky ) planBG.nbImgCreated++;
      imgID=planBG.imgID;

      // On a de place en m�moire, on conserve l'image
      if( planBG.aladin.enoughMemory() ) imgBuf=img;

      return img;
   }

   /** Cr�ation de l'image du losange � tracer en fonction du tableau des pixels
    * Cette image est conserv� s'il y a assez de place en m�moire pour un usage ult�rieur */
   //   private Image createImage() throws Exception {
   //      if( imgBuf!=null && imgID==planBG.imgID ) { if( !allSky ) planBG.nbImgInBuf++; return imgBuf; }
   //      MemoryImageSource imgSrc=null;
   //      if( planBG.color ) {
   //         int pix[] = parente==0 ? rgb : getPixelFromAncetreRGB();
   //         imgSrc=new MemoryImageSource(width,height,ColorModel.getRGBdefault(), pix, 0, width);
   //      } else {
   //         byte pix[] = parente==0 ? pixels : getPixelFromAncetre();
   //         imgSrc=new MemoryImageSource(width,height,planBG.cm, pix, 0, width);
   //      }
   //      if( !allSky ) planBG.nbImgCreated++;
   //      imgID=planBG.imgID;
   //      Image img=Toolkit.getDefaultToolkit().createImage(imgSrc);
   //
   //      // On a de place en m�moire, on conserve l'image
   //      if( planBG.aladin.enoughMemory() ) imgBuf=img;
   //
   //      return img;
   //   }

   /** Retourne la taille approximative du losange en bytes */
   protected int getMem() {
      int mem =0;
      if( pixels!=null ) mem+=pixels.length*2;
      else if( rgb!=null ) mem+=rgb.length*8;
      //      if( imgBuf!=null ) mem*=2;
      if( stream!=null ) mem+=stream.length;
      if( pixelsOrigin!=null ) mem+=pixelsOrigin.length;
      return mem;
   }

   /** Retourne la taille approximative du losange et de tous ses fils en bytes */
   protected int getFullMem() {
      int mem = getMem();
      if( fils!=null ) {
         for( int i=0; i<4; i++ ) mem+=fils[i].getFullMem();
      }
      return mem;
   }

   /** Retourne la taille du losange et de tous ses fytes dans une unit� compr�hensible */
   protected String getLongFullMem() {
      int n = getFullMem()/1024;
      if( n>1024 ) return n/1024+"MB";
      return n+"KB";
   }

   /** Retourne true si le losange d�crit par ses quatres coins se trouve
    * "derri�re" le ciel */
   protected boolean isBehindSky(PointD b[],ViewSimple v) {
      double sgn = v.getProj().sym?-1:1;
      return ((b[1].x-b[0].x)*(b[2].y-b[0].y) - (b[2].x-b[0].x)*(b[1].y-b[0].y))*sgn <= 0;
   }

   /** Retourne true si le losange d�crit par ses quatres coins est trop
    * grand pour �tre trac� en une seule fois => subdivision */
   static final double M = 280*280;
   static final double N = 150*150;
   static final double RAP=0.7;

   protected boolean isTooLarge(PointD b[] ) throws Exception {

      if( planBG.DEBUGMODE ) return false;
      double d1,d2;
      if( (d1=dist(b,0,2))>M || (d2=dist(b,2,1))>M ) return true;
      if( d1==0 || d2==0 ) throw new Exception("Rhomb error");
      double diag1 = dist(b,0,3);
      double diag2 = dist(b,1,2);
      if( diag2==0 || diag2==0 ) throw new Exception("Rhomb error");
      double rap = diag2>diag1 ? diag1/diag2 : diag2/diag1;
      return rap<RAP && (diag1>N || diag2>N);
   }

   static protected int nDraw = 0;
   static protected int nOut = 0;

   /** Trac� du losange Healpix.
    * Si le bord d'un losange fait plus de 100 pixels, on trace les 4 fils r�cursivement (prof maximum 8);
    * Si le losange est trop d�form� (cot�s oppos�s non parall�les), on trace 2 triangles t�te-b�ches.
    * Si le losange ne peut �tre trac� compl�tement (bord de ciel), on trace les 4 fils r�cursivement (prof maximum 2)
    * Si le losange ne peut tout de m�me pas �tre trac� compl�tement (bord de ciel), on tente de trac� un des 2 triangles en choississant la meilleure diagonale
    * @param g
    * @param v
    * @param maxParente parente maximale autoris�e pour le trac� des fils (longueur de la descendance, -1 si aucune)
    * @return le nombre d'images (java) trac�s
    */

   private boolean flagSym;

   //   protected int draw(Graphics g, ViewSimple v) { return draw(g,v,-1,planBG.redraw); }
   protected int draw(Graphics g, ViewSimple v/*,Vector<HealpixKey> redraw*/) { return draw(g,v,-1/*,redraw*/); }
   protected int draw(Graphics g, ViewSimple v,int maxParente/*,Vector<HealpixKey> redraw*/) {
      long t1 = Util.getTime(0);
      int n=0;  // nombre d'images java que l'on va tracer (valeur du return)
      PointD[] b = getProjViewCorners(v);

      nDraw++;
      
      boolean out=false;
      if( b==null  || (out=isOutView(v,b)) ) {
         if( out ) nOut++;
         return 0;
      }

      try { b = grow(b, 1); } catch( Exception e ) {  }

      // D�termination des triangles � tracer
      double p1,p2;
      boolean flagLosange=false;

      flagSym = false;
      
      boolean drawFast = planBG.mustDrawFast();
      
      if( b[0]!=null && b[1]!=null && b[2]!=null && b[3]!=null ) {

         // Test losange sur le bord du ciel
         if( aDroite(b[0],b[1],b[2])*aDroite(b[3],b[1],b[2])>=0
               || aDroite(b[1],b[0],b[3])*aDroite(b[2],b[0],b[3])>=0 ) {
            double d12=dist(b,1,2);
            double d03=dist(b,0,3);
            // CA NE FONCTIONNE PAS SI BIEN, MEME SI C'EST PLUS RAPIDE
            //            boolean flagPatate = planBG.projd.t==Calib.AIT || planBG.projd.t==Calib.MOL;
            boolean flagPatate=false;
            flagSym=flagPatate;
            if( d12<d03 ) {
               p1 = distCentre(b[0],b[1],b[2]);
               p2 = distCentre(b[3],b[1],b[2]);

               if( flagPatate ) {
                  if( p1<p2 ) symetric(b[3],b[0],b[1],b[2]);
                  else symetric(b[0],b[3],b[1],b[2]);
               } else {
                  if( p1<p2 ) b[3]=null;
                  else b[0]=null;
               }

            } else {
               p1 = distCentre(b[1],b[0],b[3]);
               p2 = distCentre(b[2],b[0],b[3]);

               if( flagPatate ) {
                  if( p1<p2 ) symetric(b[2],b[1],b[0],b[3]);
                  else symetric(b[1],b[2],b[0],b[3]);
               } else {
                  if( p1<p2 ) b[2]=null;
                  else b[1]=null;
               }
            }

         } else {


            // Test losange trop grand,  A retracer
            //            if( !drawFast && isTooLarge(b) ) {
            //               if( parente==0 && maxParente==-1 ) redraw.add(this);
            //               else if( (n=drawFils(g,v,maxParente,redraw))>0 ) {
            //                  resetTimer();
            //                  resetTimeAskRepaint();
            //                  return n;
            //               }
            //            }
            try {
               if( !drawFast && isTooLarge(b) ) {
                  resetTimer();
                  int m = drawFils(g,v,8/*,redraw*/);

                  // Si aucun sous-losange n'a pu �tre dessin�, je trace tout de m�me le p�re
                  if( m>0 ) return m;
               }
            } catch( Exception e ) { return 0; }

            // Test losange derri�re le ciel
            if( isBehindSky(b,v) ) {
               if( drawFast ) return 0;
               resetTimer();
               return drawFils(g,v/*,redraw*/);
            }

            // Test dessin 1 losange plut�t que 2 triangles
            if( (int)b[0].x==(int)(b[1].x+b[2].x-b[3].x)
                  && (int)b[0].y == (int)(b[2].y+b[1].y-b[3].y) ) flagLosange=true;
         }
      }

      int th=-1;     // sommet du premier triangle, -1 si non trac�
      int tb=-1;     // sommet du deuxi�me triangle, -1 si non trac�
      if( b[0]==null ) th=3;
      else if( b[3]==null ) tb=0;
      else if( b[1]==null ) th=2;
      else if( b[2]==null ) tb=1;
      else { th=0; tb=3; }
      
      // Tratement sp�cifique pour les coins des poles
      if( !drawFast && (maxParente==-1 || parente<maxParente) && hpix.isPoleCorner() ) {
         if( (n=drawFils(g,v,parente+1))>0 ) return n;
      }

      // Dessin des fils
      if( !drawFast && (b[0]==null || b[1]==null || b[2]==null || b[3]==null) ) {
         if( (n=drawFils(g,v/*,redraw*/))>0 ) return n;
      }
      
      if( th==-1 && tb==-1 ) return 0;

      Image img=null;
      try { img=createImage(); }
      catch( Exception e ) {/* e.printStackTrace(); */ return 0; }

      Graphics2D g2d = (Graphics2D)g;
      float opacity = getFadingOpacity();
      Composite saveComposite = g2d.getComposite();
      g2d.setComposite( Util.getImageComposite( opacity ) );
      Shape clip = g2d.getClip();

      try {
         if( th!=-1 ) n+=drawTriangle(g2d, img, b, th, !flagLosange);
         if( tb!=-1 && !flagLosange ) n+=drawTriangle(g2d, img, b, tb, true);
      }
      catch( Throwable e ) { 
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         planBG.clearBuf();
      }
      finally {
         g2d.setComposite(saveComposite);
         g2d.setClip(clip);
      }
      if( parente>0 ) { pixels=null; rgb=null; }

      drawLosangeBorder(g,b);

      long t2 = Util.getTime(0);
      if( !allSky ) planBG.nbImgDraw++;
      planBG.cumulTimeDraw += (t2-t1);

      resetTimer();
      //      resetTimeAskRepaint();

      if( opacity<1f ) planBG.updateFading(true);

      return n;
   }

   private long loadDate = System.currentTimeMillis();

   static private final long TIMEFADER = 1500;
   static private final float MINFADER = 0.6f;

   // Pour le moment pas d'animation de fondu-enchain�
   // EN FAIT IL FAUDRAIT MELANGER LES PIXELS AVEC LES VALEURS DU LOSANGE PERE
   // PLUTOT QUE DE JOUER SUR L'OPACITY
   private float getFadingOpacity() {
      return 1f;
      //      if( allSky ) return 1f;
      //      long t = System.currentTimeMillis() - loadDate;
      //      float op = t>=TIMEFADER ? 1f : MINFADER +( (float)t/TIMEFADER)*(1f-MINFADER);
      //      return op;
   }

   /** Retourne >0 si le point a est "� droite" de la droite passant par g et d */
   public static double aDroite(PointD a, PointD g, PointD d) {
      double dx = d.x-g.x;
      double dy = d.y-g.y;
      return dx*a.y - dy*a.x - d.y*dx + d.x*dy;
   }

   /** Retourne le carr� de la distance de a au centre du segment g,d */
   public static  double distCentre(PointD a, PointD g, PointD d) {
      double mx=(g.x+d.x)/2;
      double my=(g.y+d.y)/2;
      return (a.x-mx)*(a.x-mx) + (a.y-my)*(a.y-my) ;
   }

   /** calcul dans b la sym�trie orthogonale de a par rapport � la droite gd */
   public static void symetric(PointD b, PointD a, PointD g, PointD d) {

      // Calcul de l'angle theta entre la droite gd et ga
      double alpha = Math.atan2(d.y-g.y,d.x-g.x);
      double beta  = Math.atan2(a.y-g.y,a.x-g.x);
      double theta = alpha - beta;

      // on d�cale l'origine sur "g"
      double ax = a.x - g.x;
      double ay = a.y - g.y;

      // Rotation de -2*theta
      double cost = FastMath.cos( -2*theta );
      double sint = FastMath.sin( -2*theta );
      double x = ax*cost + ay*sint;
      double y = - ax*sint + ay*cost;

      // on recale
      b.x = x + g.x;
      b.y = y + g.y;
   }

   /** Retourne le carr� de la distance des coins d'indice g et d */
   public static  double dist(PointD [] b, int g, int d) {
      double dx=b[g].x-b[d].x;
      double dy=b[g].y-b[d].y;
      return  dx*dx + dy*dy;
   }

   /** Trac� du triangle dont l'indice de l'angle dans le losange est "h"
    * flagClip=true s'il y a positionnement d'un clip */
   protected int drawTriangle(Graphics2D g2d, Image img,PointD []b, int h,boolean flagClip) {

      int d,g;
      switch(h) {
         case 0:   d=2; g=1; break;
         case 3:   d=1; g=2; break;
         case 1:   d=3; g=0; break;
         default : d=0; g=3; break;
      }

      if( b[d]==null || b[g]==null ) return 0;

      if( flagClip ) {
         PointD[] b1=null;
         try {
            b1 = grow(b, 2);
            Polygon p = new Polygon(new int[]{ (int)(b1[h].x+0.5),(int)(b1[d].x+0.5),(int)(b1[g].x+0.5)},
                  new int[]{ (int)(b1[h].y+0.5),(int)(b1[d].y+0.5),(int)(b1[g].y+0.5)}, 3);
            g2d.setClip(p);
         } catch( Exception e ) {  }
      }

      // On tourne l'image pour l'aligner sur h-d
      double hdx = b[h].x - b[d].x;    if( h==0 || h==2 ) hdx= -hdx;
      double hdy = b[h].y - b[d].y;    if( h==0 || h==2 ) hdy= -hdy;
      double angle = FastMath.atan2(hdy,hdx);

      // On �crase la longueur
      double hd = Math.sqrt( hdx*hdx + hdy*hdy );
      double mx= hd/width;

      // On �crase la hauteur
      double hgx = b[h].x - b[g].x;   if( h==0 || h==2 ) hgx= -hgx;
      double hgy = b[h].y - b[g].y;   if( h==0 || h==2 ) hgy= -hgy;
      double dhg = Math.sqrt( hgx*hgx + hgy*hgy );
      double anglehg = FastMath.atan2(hgy,hgx) - angle;
      double my= dhg*FastMath.sin(anglehg)/width;

      // On fait glisser selon les x pour longer l'axe d-h
      double sx = ( dhg*FastMath.cos(anglehg) )/ hd;
      AffineTransform tr = new AffineTransform();
      if( h==3 || h==1 )  tr.translate(b[d].x+b[g].x-b[h].x,b[d].y+b[g].y-b[h].y);
      else tr.translate(b[h].x,b[h].y);
      tr.rotate(angle);
      tr.scale(mx,my);
      tr.shear(sx,0);

      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            planBG.mustDrawFast() &&  order-parente<planBG.maxOrder || /*(!planBG.color || planBG.truePixels) &&*/ order-parente>=planBG.maxOrder
            ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR :
               RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      if( img==null ) return 0;
      g2d.drawImage(img,tr,planBG.aladin);

      return 1;
   }

   static final Color BLUE1 = new Color(0,0,255);
   static final Color BLUE2 = new Color(10,52,128);
   static final Color BLUE3 = new Color(51,144,198);

   /** Trac� du contour du losange et indication de son num�ro et de son ordre Helapix
    * => command� par le menu Aladin.aladin.hpxCtrl */
   //   final protected void drawLosangeDiagonale(Graphics g,PointD b1[]) {
   //      if( !planBG.ref ) return;
   //      int debugIn = planBG.isDebugIn(npix);
   //      if( debugIn==0 && !planBG.aladin.calque.hasHpxGrid() ) return;
   //      PointD b [] = new PointD[4];
   //   }

   /** Trac� du contour du losange et indication de son num�ro et de son ordre Helapix
    * => command� par le menu Aladin.aladin.hpxCtrl */
   final protected void drawLosangeBorder(Graphics g,PointD b1[]) {
      if( !planBG.ref ) return;
      int debugIn = planBG.isDebugIn(npix);
//      if( debugIn==0 && !planBG.aladin.calque.hasHpxGrid() ) return;
      if( debugIn==0 && planBG.aladin.levelTrace<6 ) return;
      PointD b [] = new PointD[4];
      int j=0;
      for( int i=0; i<4; i++ ) if( b1[i]!=null ) b[j++]=b1[i];
      Polygon p=null;
      if( j==4 ) p = new Polygon(new int[]{ (int)b[0].x,(int)b[1].x,(int)b[3].x,(int)b[2].x},
            new int[]{ (int)b[0].y,(int)b[1].y,(int)b[3].y,(int)b[2].y}, 4);
      else if( j==3 ) p = new Polygon(new int[]{ (int)b[0].x,(int)b[1].x,(int)b[2].x},
            new int[]{ (int)b[0].y,(int)b[1].y,(int)b[2].y}, 3);
      else return;

      if( debugIn>0 ) {
         g.setColor(debugIn==1 ? BLUE1 :debugIn==2 ? BLUE2 : BLUE3 );
         g.fillPolygon(p);
      }

      Color c= parente>0 ? new Color(100,100,0) : Color.green;
      //      if( parente>0 ) {
      //         int n=200-(parente*50);
      //         if( n<0 ) c = Color.blue;
      //         else c=new Color(n,n,0);
      //      }
      g.setColor(flagSym ? Color.magenta : j==3?Color.red:c);
      g.drawPolygon(p);

      //      if( j==4 ) drawNumber(g,b);

      //      for( int i=0; i<4; i++ ) {
      //         j = i==0 ? 3 : i==1 ? 2 : i==2 ? 1 : 0;
      //         int x = (int)( b[i].x+ (b[j].x - b[i].x)/10. );
      //         int y = (int)( b[i].y+ (b[j].y - b[i].y)/10. );
      //         g.drawString(i+"", x,y+10);
      //      }

      //      if( st!=null ) {
      //         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      //            RenderingHints.VALUE_ANTIALIAS_OFF);
      //         ((Graphics2D)g).setStroke(st);
      //      }
   }

   /** Trac� du contour du losange et indication de son num�ro et de son ordre Helapix */
   final protected void drawLosangeBorder(Graphics g,ViewSimple v) {
      PointD b [] = getProjViewCorners(v);
      if( b==null || b[0]==null || b[1]==null || b[2]==null || b[3]==null ) return;
      double c0 = dist(b,0,1);
      double c1 = dist(b,0,2);
      double c2 = dist(b,1,3);
      double c3 = dist(b,2,3);
      double min = Math.min(Math.min(c0,c1),Math.min(c2,c3));
      double min2 = 20*min;
      if( c0<min2 ) g.drawLine((int)b[0].x,(int)b[0].y,(int)b[1].x,(int)b[1].y);
      if( c1<min2 ) g.drawLine((int)b[0].x,(int)b[0].y,(int)b[2].x,(int)b[2].y);
      if( c2<min2 ) g.drawLine((int)b[1].x,(int)b[1].y,(int)b[3].x,(int)b[3].y);
      if( c3<min2 ) g.drawLine((int)b[2].x,(int)b[2].y,(int)b[3].x,(int)b[3].y);

      drawNumber(g,v,b);
   }

   // Affichage du label (order/npix)
   final private void drawNumber(Graphics g,ViewSimple v,PointD [] b) {
      if( v.isAllSky() ) return;
      String s=getStringNumber();
      FontMetrics m = g.getFontMetrics();
      int size=m.stringWidth(s);
      int h = m.getHeight();
      //      if( dist(b,0,1)< (v.rv.width/6)*(v.rv.width/6) ) return;
      double xMin = Math.min(Math.min(b[0].x,b[1].x),Math.min(b[2].x,b[3].x));
      double xMax = Math.max(Math.max(b[0].x,b[1].x),Math.max(b[2].x,b[3].x));
      double dist= xMax-xMin;
      if( size<dist ) {
         int x = (int)(b[0].x+b[1].x+b[2].x+b[3].x)/4 - size/2;
         int y = (int)(b[0].y+b[1].y+b[2].y+b[3].y)/4 + h/2;

         boolean a=false,c=false;
         if( a=(x<3) ) x=3;
         else if( a=(x+size+3>v.rv.width) ) x=v.rv.width-size-3;
         if( c=(y<25) ) y=25;
         else if( c=(y+25>v.rv.height) ) y = v.rv.height-25;
         if( a || c ) {
            if( dist<150  ) return;
            else {
               Polygon pol = new Polygon();
               pol.addPoint((int)b[0].x,(int)b[0].y);
               pol.addPoint((int)b[1].x,(int)b[1].y);
               pol.addPoint((int)b[3].x,(int)b[3].y);
               pol.addPoint((int)b[2].x,(int)b[2].y);
               if( !pol.contains(x, y) || !pol.contains(x+size,y) ) return;
            }
         }

//         Util.drawStringOutline(g, s, x,y, Color.green, Color.black);
         
         int w = m.stringWidth(order+"");
         Util.drawStringOutline(g, order+"", x,y-4, Color.green, Color.black);
         Util.drawStringOutline(g, npix+"", x+w+4,y+4, Color.green, Color.black);
//         g.drawString(order+"", x,y-4);
//         g.drawString(npix+"", x+w+4,y+4);
         g.drawString("/",x+w,y);
      }
   }

   /** Trac� des bords du losange Healpix (au plus juste) */
   protected void drawRealBorders(Graphics g, ViewSimple v) {
      try {
         Projection proj = v.getProj();
         double x [][] = CDSHealpix.borders(CDSHealpix.pow2(order), npix, 50);
         for( int i=0; i<x.length; i++ ) {
            Coord c = new Coord(x[i][0],x[i][1]);
            int frame = hpix.getFrame();
            if( frame!=Localisation.ICRS ) {
               c=Localisation.frameToFrame(c,frame,Localisation.ICRS);
            }
            proj.getXY(c);
            if( Double.isNaN(c.x) ) continue;
            PointD p=v.getViewCoordDble(c.x,c.y);
            g.drawLine((int)p.x,(int)p.y,(int)p.x,(int)p.y);
         }
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }


   /** Affichage de controle de la position du losange */
   protected void drawCtrl(Graphics g1, ViewSimple v) {
      Graphics2D g = (Graphics2D)g1;

      Color c = g.getColor();
      g.setColor(Color.red);
      Stroke st = g.getStroke();
      g.setStroke(new BasicStroke(2f));
      drawRealBorders(g, v);
      g.setStroke(st);

      PointD[] b = getProjViewCorners(v);
      if( b==null || b[0]==null || b[1]==null || b[2]==null || b[3]==null) return;
      String s=getStringNumber();
      g.setFont(Aladin.PLAIN);
      try {
         s = s+" ("+CDSHealpix.nest2ring(CDSHealpix.pow2(order-parente), npix/(long)Math.pow(4,parente))+")";
      } catch( Exception e ) { }
      int size=g.getFontMetrics().stringWidth(s);
      int x = (int)(b[0].x+b[1].x+b[2].x+b[3].x)/4;
      int y = (int)(b[0].y+b[1].y+b[2].y+b[3].y)/4+7;
      if( Math.abs(b[2].x-b[1].x)<size+18 ) {
         y=(int)Math.min(Math.min(b[0].y, b[1].y),Math.min(b[2].y, b[3].y))-10;
      }
      g.drawString(s, x-size/2,y+3);

      //      // Les indices des coins
      //      if( Math.abs(b[2].x-b[1].x)>30 ) {
      //         for( int i=0; i<4; i++ ) {
      //            int j = i==0 ? 3 : i==1 ? 2 : i==2 ? 1 : 0;
      //            int x = (int)( b[i].x+ (b[j].x - b[i].x)/10. );
      //            int y = (int)( b[i].y+ (b[j].y - b[i].y)/10. );
      //            g.drawString(i+"", x,y+10);
      //         }
      //      }

      g.setColor(c);
   }

   /** Calcule le num�ro du pixel p�re (pour l'order pr�c�dent) */
   protected long getFather() { return npix/4; }

   /** Calcule les num�ros des 4 pixels fils (pour l'order suivant) */
   protected long [] getChildren() { return getChildren(null); }
   protected long [] getChildren(long [] pixChild) {
      if( pixChild==null ) pixChild = new long[4];
      pixChild[0]= npix*4;
      pixChild[1]= npix*4 +1;
      pixChild[2]= npix*4 +2;
      pixChild[3]= npix*4 +3;
      return pixChild;
   }

   /** Retourne les coordonn�es des pixels d'angle du losange */
   protected Coord [] getCorners() {
      return hpix.getCorners();
   }

   //   protected Coord [] getCorners() { hpix.getCorners(); }
   //      if( corners==null ) corners = computeCorners();
   //      return corners;
   //   }

   protected boolean isOutView(ViewSimple v) { return hpix.isOutView(v,null); }
   protected boolean isOutView(ViewSimple v,PointD []b) { return hpix.isOutView(v,b); }

   //   /** Retourne true si le losange est � coup s�r hors de la vue - je teste
   //    * uniquement les cas en-dessous, au-dessous, � droite ou � gauche */
   //   protected boolean isOutView(ViewSimple v) { return isOutView(v,null); }
   //
   //   protected boolean isOutView(ViewSimple v,PointD []b) {
   //      int w = v.getWidth();
   //      int h = v.getHeight();
   //      if( b==null ) b = getProjViewCorners(v);
   //      if( b==null ) return true;
   //      if( b[0]==null || b[1]==null || b[2]==null || b[3]==null ) return false;
   //
   //      double minX,maxX,minY,maxY;
   //      minX=maxX=b[0].x;
   //      minY=maxY=b[0].y;
   //      for( int i=1; i<4; i++ ) {
   //         if( b[i].x<minX ) minX = b[i].x;
   //         else if( b[i].x>maxX ) maxX = b[i].x;
   //         if( b[i].y<minY ) minY = b[i].y;
   //         else if( b[i].y>maxY ) maxY = b[i].y;
   //      }
   //
   //      // Tout � droite ou tout � gauche
   //      if( minX<0 && maxX<0 || minX>=w && maxX>=w ) return true;
   //
   //      // Au-dessus ou en dessous
   //      if( minY<0 && maxY<0 || minY>=h && maxY>=h ) return true;
   //
   //      return false;  // Mais attention, ce n'est pas certain !!
   //   }

   //   private int oframe = -1;


   //   protected Coord [] computeCorners() {
   //      Coord [] corners;
   //      try {
   //         long nside = (long)Math.pow(2,order);
   //         double [][] x = CDSHealpix.corners_nest(nside,npix);
   //         corners = new Coord[4];
   //         for( int i=0; i<x.length; i++ ) corners[i] = new Coord(x[i][0],x[i][1]);
   //         corners = computeCornersToICRS(corners);
   //      } catch( Exception e ) { return null; }
   //      return corners;
   //   }

   //   protected Coord [] computeCornersToICRS(Coord [] corners) {
   //      int frameOrigin = planBG!=null ? planBG.frameOrigin : Localisation.GAL;
   //      if( frameOrigin!=Localisation.ICRS ) {
   //         for( int i=0; i<4; i++ ) corners[i]=Localisation.frameToFrame(corners[i],frameOrigin,Localisation.ICRS);
   //      }
   //      return corners;
   //   }

   final protected PointD[] getProjViewCorners(ViewSimple v) {
      return hpix.getProjViewCorners(v);
   }

   //   private int nNull;
   //
   //   /**
   //    * Retourne les coordonn�es ra,de des 4 coins du losange dans la projection de
   //    * la vue ou null si probl�me
   //    */
   //   final protected PointD[] getProjViewCorners(ViewSimple v) {
   //      Projection proj;
   //      if( (proj=v.getProj())==null || corners==null ) return null;
   //      if( oiz==v.iz && vHashCode==v.hashCode() ) {
   //         if( nNull>1 ) return null;
   //         return coins;
   //      }
   //      nNull=0;
   //
   //      // On calcul les xy projection des 4 coins
   //      for (int i = 0; i<4; i++) {
   //         Coord c = corners[i];
   //         proj.getXY(c);
   //         if( Double.isNaN(c.x)) {
   //            nNull++;
   //            if( nNull>1 ) return null;
   //            else { coins[i]=null; continue; }
   //         }
   //         if( coins[i]==null ) coins[i]=new PointD(c.x,c.y);
   //         else { coins[i].x=c.x; coins[i].y=c.y; }
   //      }
   //
   //      // On augmente d'un pixel �cran pour couvrir les coutures
   //      if( Aladin.macPlateform && Aladin.ISJVM15 ) {
   //         double val = 2/v.zoom;
   //         if( val>0.5 ) {
   //            boolean aNull=false,cNull=false;
   //            for( int i=0; i<2; i++ ) {
   //               int a= i==1 ? 1 : 0;
   //               int c= i==1 ? 2 : 3;
   //               if( coins[a]==null ) {
   //                  int d,g;
   //                  if( a==0 || a==3 ) { d=1; g=2; }
   //                  else { d=0; g=3; }
   //                  coins[a] = new PointD((coins[d].x+coins[g].x)/2,(coins[d].y+coins[g].y)/2);
   //                  val*=2;  // Les triangles doivent �tre encore un peu agrandis
   //                  aNull=true;
   //               } else if( coins[c]==null ) {
   //                  int d,g;
   //                  if( c==0 || c==3 ) { d=1; g=2; }
   //                  else { d=0; g=3; }
   //                  coins[c] = new PointD((coins[d].x+coins[g].x)/2,(coins[d].y+coins[g].y)/2);
   //                  val*=2;
   //                  cNull=true;
   //               } else aNull=cNull=false;
   //
   //               double angle = Math.atan2(coins[c].y-coins[a].y, coins[c].x-coins[a].x);
   //               double chouilla =val*Math.cos(angle);
   //               coins[a].x-=chouilla;
   //               coins[c].x+=chouilla;
   //               chouilla = val*Math.sin(angle);
   //               coins[a].y-=chouilla;
   //               coins[c].y+=chouilla;
   //               if( aNull ) coins[a]=null;
   //               else if( cNull ) coins[c]=null;
   //            }
   //         }
   //      }
   //
   //      // On calcule les xy de la vue
   //      for( int i=0; i<4; i++ ) {
   //         if( coins[i]!=null )  v.getViewCoordDble(coins[i],coins[i].x, coins[i].y);
   //      }
   //
   //      oiz=v.iz;
   //      vHashCode=v.hashCode();
   //      return coins;
   //   }

   HealpixKey [] getPixList() { return null; }

   /** Retourne le Norder du losange */
   protected int getLosangeOrder() { return (int)CDSHealpix.log2(width); }

   @Override
   public int compareTo(HealpixKey o) {
      return priority - o.priority;
   }

}
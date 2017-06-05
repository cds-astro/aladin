// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
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

package cds.fits;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import cds.aladin.Aladin;
import cds.allsky.CacheFitsWriter;
import cds.allsky.Constante;
import cds.allsky.Context;
import cds.allsky.MyInputStreamCachedException;
import cds.tools.Util;


/**
 * Classe de manipulation d'un cache de fichiers FITS/JPEG ouverts.
 * Permet d'�viter les ouvertures et r�ouvertures inutiles lors d'un traitement n�cessitant plusieurs fois
 * l'acc�s aux m�mes images.
 * Il faut mentionner � la cr�ation la taille max allou�e au cache, en Mo (par d�faut 512Mo) et en Nb de fichiers (par d�faut 10000)
 * L'acc�s � un fichier Fits se fait par Fits getFits(filename)( ou getFits(filename,true) pour du JPEG)
 *
 * Rq: Le rajouti d'Ana�s sur la soustraction du skyvalName ne devrait pas �tre dans cette classe (selon moi)
 * => A voir si on le d�place
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 - novembre 2014
 * @version 1.1 - juillet 2012
 * @version 1.0 - sept 2011
 */
public class CacheFits {

   static private final long DEFAULT_MAXMEM=512*1024*1024L;
   static protected final int MAXFILE = 10000;

   private long maxMem;             // Taille max (en octets)
   private int nextId;              // prochain identificateur unique de fichier
   volatile private boolean cacheOutOfMem;   // En cas de d�bordement m�moire, on vire totalement le cache
   protected HashMap<String, FitsFile> map;             // Table des fichiers
   //   private TreeMap<String,FitsFile> sortedMap;        // Table tri� par ordre de dernier acc�s
   Context context;
   private Hashtable<String, double[]> cutCache = new Hashtable<String, double[]>();
   private Hashtable<String, double[]> shapeCache = new Hashtable<String, double[]>();


   //private boolean skyvalSub = false;// condition d'application d'une soustraction du skyval au moment
   //de la mise dans le cache

   protected int statNbOpen,statNbFind,statNbFree;

   /**
    * Cr�ation d'un cache de fichiers Fits
    */
   public CacheFits() { this(DEFAULT_MAXMEM); }

   /**
    * Cr�ation d'un cache de fichiers Fits
    * @param maxMem limite en bytes occup�s, ou si n�gatif,
    *          nombre de bytes � garder libre par rapport � la RAM dispo
    * @param rewriteMode true si les fichiers du cache seront r��crits syst�matiquement
    */
   public CacheFits(long maxMem) {
      this.maxMem = maxMem;
      cacheOutOfMem = maxMem==0;
      nextId = 0;
      statNbFree = statNbOpen = statNbFind = 0;
      map = new HashMap<String, FitsFile>(MAXFILE+MAXFILE/100);
      //      sortedMap = new TreeMap<String, FitsFile>( new ValueComparator(map) );
   }

   /** V�rification qu'il reste assez de m�moire RAM, et sinon
    * on va lib�rer temporairement les blocs pixels[] non utilis�s afin de r�cup�rer
    * ce qui manque
    * @param rqMem taille de la m�moire requise
    * @return true c'est bon, false, faut attendre
    */
   //   public boolean needMem(long rqMem) {
   //      if( rqMem<=0L ) return false;
   //      long freeMem=getFreeMem();
   //      if( getFreeMem()>rqMem ) return false;
   //
   //      rqMem = rqMem-freeMem;
   //      long mem = 0L;
   //      try {
   //         waitLock();
   //         for( String key : map.keySet() ) {
   //            Fits f = map.get(key).fits;
   //            long m = f.getMem();
   //            try { f.releaseBitmap();
   //            } catch( Exception e ) { } mem += m-f.getMem();
   //            if( mem>rqMem ) break;
   //         }
   //      } finally { unlock(); }
   //      gc();
   //      return mem>rqMem;
   //   }

   // Gestion d'un lock
   static protected final Object lockObj= new Object();
   //   transient private boolean lock;
   //   private void waitLock() {
   //      while( !getLock() ) sleep(5);
   //   }
   //   private void unlock() { lock=false; }
   //   private boolean getLock() {
   //      synchronized( lockObj ) {
   //         if( lock ) return false;
   //         lock=true;
   //         return true;
   //      }
   //   }
   //   // Mise en pause
   //   private void sleep(int delay) {
   //      try { Thread.currentThread().sleep(delay); }
   //      catch( Exception e) { }
   //   }

   static public final int FITS = 0; // FITS classique
   static public final int JPEG = 1; // JPEG
   static public final int PNG  = 2; // PNG
   static public final int HHH  = 4; // HHH (combin� avec JPEG ou PNG)
   

   /**
    * R�cup�ration d'un Fits sp�cifi� par son nom de fichier.
    * @param fileName Nom du fichier Fits � acc�der (supporte le mode mosaic nomfichier[x,y-wxh])
    * @return l'objet Fits
    * @throws Exception
    */
   public Fits getFits(String fileName) throws Exception,MyInputStreamCachedException { return getFits(fileName,FITS,true,true); }
   public Fits getFits(String fileName,int mode,boolean flagLoad,boolean keepHeader) throws Exception,MyInputStreamCachedException {
      if( cacheOutOfMem )  return open(fileName,mode,flagLoad,keepHeader).fits;

//      synchronized( lockObj  ) {
         FitsFile f = find(fileName);

         // Trouv�, je le mets � jour
         if( f!=null ) {
            f.update();
            statNbFind++;
         }

         // Pas trouv�, je l'ajoute
         else {
            if( isOver() ) clean();
            try {
               f=add(fileName,mode,flagLoad,keepHeader);
            } catch( OutOfMemoryError e ) {
               System.err.println("CacheFits.getFits("+fileName+") out of memory... clean and try again...");
               if( maxMem<0 ) maxMem*=2;
               else maxMem /= 2;
               try {
                  clean();
                  f=add(fileName,mode,flagLoad,keepHeader);
               } catch( OutOfMemoryError e1 ) {
                  System.err.println("CacheFits.getFits("+fileName+") out of memory... double error... removing the cache...");
                  e1.printStackTrace();
                  reset();
                  cacheOutOfMem=true;
                  return open(fileName,mode,flagLoad,keepHeader).fits;
               }
            }
            statNbOpen++;
         }

         return f.fits;
//      }
   }

   // Retrouve l'objet Fits dans le cache, null si inconnu
   protected FitsFile find(String name) {
      return map.get(name);
   }

   // Ajoute un fichier Fits au cache. Celui-ci est totalement charg� en m�moire
   private FitsFile add(String name,int mode,boolean flagLoad,boolean keepHeader) throws Exception,MyInputStreamCachedException {
      FitsFile f = open(name,mode,flagLoad,keepHeader);
      map.put(name, f);
      return f;
   }

   // Suppression d'un fichier Fits de cache.
   protected void remove(String name) throws Exception {
      map.remove(name);
   }

   private boolean firstChangeOrig=true;

   // Ouvre un fichier
   private FitsFile open(String fileName,int mode,boolean flagLoad,boolean keepHeader) throws Exception,MyInputStreamCachedException {
      boolean flagChangeOrig=false;

      // Le fichier existe-t-il ? (suppression d'une �ventuelle extension [x,y-wxh]
      String file = fileName;
      int fin =  file.length() - 1;
      if( file.charAt( fin ) == ']' ) {
         int deb = file.lastIndexOf( '[', fin );
         if( deb>0 ) file = file.substring(0,deb);
      }
      if( !(new File(file)).exists() ) throw new FileNotFoundException();


      FitsFile f = new FitsFile();
      f.fits = new Fits();
      if( context!=null && context.skyvalName!=null ) { flagLoad=true; f.fits.setReleasable(false); }

      // Il faut lire deux fichiers, le HHH, puis le JPEG ou PNG suivant le cas
      if( (mode&HHH)!=0 ) {
         f.fits.loadHeaderFITS(fileName);
         String pngFile = fileName.replaceAll("\\.hhh",".png");
         String jpgFile = fileName.replaceAll("\\.hhh",".jpg");

         // On ne connait pas le type de fichier pour les pixels,
         // => on va voir ce qui existe
         if( (mode&(JPEG|PNG))==0 ) {
            String racJpgFile = jpgFile;
            String racPngFile = pngFile;
            int i;
            if( (i=jpgFile.indexOf(".jpg["))>0 ) racJpgFile = jpgFile.substring(0,i+4);
            if( (i=pngFile.indexOf(".png["))>0 ) racPngFile = pngFile.substring(0,i+4);

            if( (new File(racJpgFile)).exists() ) mode |= JPEG;
            else if( (new File(racPngFile)).exists() ) mode |= PNG;
         }
         if( (mode&PNG)!=0  ) fileName=pngFile;
         else if( (mode&JPEG)!=0 ) fileName=jpgFile;
         else throw new Exception(".hhh file without associated .jpg or .png file");
      }

      if( (mode&(PNG|JPEG))!=0 ) {
         int format = (mode&PNG)!=0 ? Fits.PREVIEW_PNG
               : (mode&JPEG)!=0 ? Fits.PREVIEW_JPEG
                     : Fits.PREVIEW_UNKOWN;
         f.fits.loadPreview(fileName,true, (mode&HHH)==0, format);
      }
      else {
         f.fits.loadFITS(fileName, false, flagLoad);
         if( context!=null ) {
            flagChangeOrig = f.fits.bzero!=context.bZeroOrig || f.fits.bscale!=context.bScaleOrig;
            if( flagChangeOrig && firstChangeOrig ) {
               context.warning("All original data sets do no used the same BZERO & BSCALE factors => rescaling will be applied => "+fileName);
               firstChangeOrig=false;
            }
            if( context.isCube() ) {
               int d=1;
               try {
                  d = f.fits.headerFits.getIntFromHeader("NAXIS3");
                  if( d!=context.depth )  throw new Exception();
               } catch( Exception e ) {
                  throw new Exception("Uncompatible cube depth ("+d+") => file ignored ["+f.fits.getFilename()+"]");
               }
               if( context.isCubeCanal() ) {
                  try {
                     double crpix3 = f.fits.headerFits.getDoubleFromHeader("CRPIX3");
                     double crval3 = f.fits.headerFits.getDoubleFromHeader("CRVAL3");
                     double cdelt3 = f.fits.headerFits.getDoubleFromHeader("CDELT3");
                     if( context.crpix3!=crpix3 || context.crval3!=crval3 || context.cdelt3!=cdelt3 ) {
                        throw new Exception();
                     }
                  } catch( Exception e) {
                     context.warning("All original data sets do no used the same CRPIX3,CRVAL3 & CDELT3 factors => factors ignored");
                     context.crpix3=context.crval3=context.cdelt3=0;
                     context.bunit3=null;
                  }
               }
            }
         }
      }

      // applique un filtre sp�cial
      if ( context!=null && (context.skyvalName!=null || context.expTimeName!=null
            || context.pixelGood!=null || flagChangeOrig || context.dataArea!=Constante.SHAPE_UNKNOWN)) delSkyval(f.fits,flagChangeOrig);

      // On ne conserve pas le HeaderFits
      if( !keepHeader ) f.fits.freeHeader();
      return f;
   }

   // Retourne true si le cache est en surcapacit�
   protected boolean isOver() {
      if( map.size()>MAXFILE ) return true;
      if( maxMem<0 ) {
         //         System.out.println("Cachemem="+Util.getUnitDisk(mem)+" freeMem="+Util.getUnitDisk(getFreeMem())
         //               +" maxMem="+Util.getUnitDisk(-maxMem)
         //               +" isOver="+(mem>getFreeMem()+maxMem));
         //         return mem>getFreeMem()+maxMem;
         return getFreeMem()<-maxMem;
      }
      return getMem()>maxMem;
   }

   /** Retourne la taille occup�e par le cache */
   public long getMem() {
      long mem=0L;
      if( map==null ) return mem;
      
//      Enumeration<String> e = map.keys();
//      while( e.hasMoreElements() ) {
//         String key = e.nextElement();
//         FitsFile f = map.get(key);
//         mem += f.fits.getMem();
//      }
      
      try { for( FitsFile f: map.values() ) mem += f.fits.getMem(); } catch( Exception e ) { };

      return mem;
   }

   // Force le nettoyage du clean
   public void forceClean() {
      synchronized( lockObj  ) {  clean(); }
   }


   volatile boolean deja=false;
   
   // Supprime les plus vieux �l�ments du cache pour
   // qu'il y ait un peu de place libre
   protected void clean() {
      
      synchronized( lockObj ) {
         long mem = getMem();
         long freeMem = getFreeMem();
         
         boolean tooManyFile = map.size()>MAXFILE;
         boolean tooManyMem = maxMem<0 && freeMem<-maxMem || maxMem>=0 && mem>maxMem;
         if( !tooManyFile && !tooManyMem ) return;
         
//         if( !isOver() ) return;
         
         long totMem=0L;
         long m=0;
         int nb=0;
         boolean encore=true;
         long now = System.currentTimeMillis();
         long delay=5000;

         int i;
         try {
            cacheOutOfMem=true;

            HashMap<String,String> libere = new  HashMap<String,String>(map.size());
            HashMap<String,FitsFile> map1 = new  HashMap<String,FitsFile>(map.size());

            // en premier tour, on supprime les fits non utilis�s
            // depuis plus de 5s, et si pas assez de m�moire, on ne regarde plus la date
            for( i=0; i<2 && encore; i++) {
               int mapsize=map.size();

               for( String key: map.keySet() ) {
                  FitsFile f = map.get(key);
                  if( f.fits.hasUsers() ) continue;
                  if( i==0 && now-f.timeAccess<delay ) continue;
                  m = f.getMem();
                  totMem+=m;
                  nb++;
                  statNbFree++;

                  libere.put(key,"");
                  if( totMem>mem/2L && mapsize-nb < 2*MAXFILE/3 ) { encore=false; break; }
               }

               for( String key: map.keySet() ) {
                  if( libere.get(key)!=null ) continue;
                  FitsFile f = map.get(key);
                  map1.put(key,f);
               }
               
               // Obligatoire d'appeler le remove dans le cas d'un cacheFitsWriter
               if( this instanceof CacheFitsWriter ) {
                  for( String key: libere.keySet() ) try { remove(key); } catch( Exception e ) {}
               }
               
               map=map1;

               //         Enumeration<String> e = map.keys();
               //         while( e.hasMoreElements() ) {
               //            String key = e.nextElement();
               //            FitsFile f = map.get(key);
               //            if( f.fits.hasUsers() ) continue;
               //            if( i==0 && now-f.timeAccess<delay ) continue;
               //            m = f.getMem();
               //            totMem+=m;
               //            nb++;
               //            statNbFree++;
               //            //            map.remove(key);
               //            try { remove(key); } catch( Exception e1 ) { }
               //            if( totMem> mem/2L && map.size()<2*MAXFILE/3 ) { encore=false; break; }
               //         }
            }

            //      sortedMap.clear();
            gc();
         } finally {
            cacheOutOfMem=false;
         }

         long duree = System.currentTimeMillis() - now;
         String s1 = i>1 ? "s":"";
         long freeRam = getFreeMem();
         if( context!=null ) {
            context.stat("Cache: freeRAM="+Util.getUnitDisk(freeMem)+" => "+nb+" files released ("+Util.getUnitDisk(totMem)+") in "+i+" step"+s1+" in "+Util.getTemps(duree)
            +" => freeRAM="+Util.getUnitDisk(freeRam));
         }
      }
   }

   // Reset totalement le cache
   public void reset() {
      statNbFree+=map.size();
      
      if( this instanceof CacheFitsWriter ) {
         ArrayList<String> a = new ArrayList<String>(map.size());
         for( String key: map.keySet() ) a.add(key);
         for( String key: a ) {
            try { remove(key); } catch( Exception e1 ) { }
         }
         
      } else map.clear();

      gc();
   }


   public void close() { reset(); }

   public void setContext(Context c) { context = c; }


   private static final int WIDTHAUTOCUT=1024;

   // D�termination des cuts d'une image,
   // @param pourcentMin % de l'info ignor� en d�but d'histogramme (ex: 0.003)
   // @param pourcentMax % de l'info ignor� en fin d'histogramme (ex: 0.9995)
   // et conservation dans un cache pour �viter de refaire plusieurs fois
   // le calcul notamment dans le cas d'une image ouverte en mode "blocs"
   private double [] findAutocutRange(Fits f,double pourcentMin, double pourcentMax) throws Exception {
      String filename = f.getFilename()+f.getMefSuffix();
      double [] cut = cutCache.get(filename);
      if( cut!=null ) return cut;
      Cut cut1[] = new Cut[5];
      Fits f1 = new Fits();
      f1.loadHeaderFITS(filename);

      int width = f1.width;
      int height = f1.height;

      // pour debug
      int ox,oy,oz,ow,oh,od;
      ox=oy=oz=ow=oh=od=-1;

      // Estimation au centre
      try {
         int w=f1.width>WIDTHAUTOCUT ? WIDTHAUTOCUT : f1.width;
         int h=f1.height>WIDTHAUTOCUT ? WIDTHAUTOCUT : f1.height;
         int d=f1.depth>10 ? 10 : f1.depth;
         int x = f.width/2-w/2, y=f.height/2-h/2, z=f.depth/3-d/3;

         // Pour debug
         ox=x; oy=y; oz=z; ow=w; oh=h; od=d;

         f1.loadFITS(f.getFilename(),f.ext,x,y,z,w,h,d);
         if( context.hasAlternateBlank() ) f1.setBlank( context.getBlankOrig() );
         cut = f1.findAutocutRange(pourcentMin,pourcentMax);

         //      System.out.println("cut central: "+cut[0]+" pour "+filename);

         // Si l'image est assez grande on va faire 4 autres mesures
         // et prendre la m�diane des 5
         if( width>3*WIDTHAUTOCUT &&height>3*WIDTHAUTOCUT ) {
            int gapx = (width-2*WIDTHAUTOCUT)/3;
            int gapy = (height-2*WIDTHAUTOCUT)/3;
            for( int i=0; i<4; i++ ) {
               x = i==0 || i==2 ? gapx : width-WIDTHAUTOCUT-gapx;
               y = i<2 ? gapy : height-WIDTHAUTOCUT-gapy;
               f1.loadFITS(f.getFilename(),f.ext,x,y,z,w,h,d);
               cut1[i] = new Cut();
               cut1[i].cut = f1.findAutocutRange(pourcentMin,pourcentMax);
               //            System.out.println("cut coin "+i+" en ("+x+","+y+"): "+cut1[i].cut[0]);
            }
            cut1[4] = new Cut(); cut1[4].cut = new double[ cut.length ];
            for( int j=0; j<cut.length; j++ ) cut1[4].cut[j]=cut[j];
            Arrays.sort(cut1);
            for( int j=0; j<cut.length; j++ ) cut[j] = cut1[2].cut[j];
         }
      } catch( Exception e ) {
         System.err.println("findAutocutRange exception: on "+filename+" width="+width+" height="+height+" box="+ox+","+oy+","+oz+" "+ow+"x"+oh+"x"+od);
         e.printStackTrace();
         throw e;
      }
      cutCache.put(filename,cut);
      //      context.info("Skyval estimation => "+ip(cut[0],f1.bzero,f1.bscale)+" for "+filename);

      return cut;
   }

   class Cut implements Comparable<Cut>{
      double [] cut;

      @Override
      public int compareTo(Cut o) {
         return  o.cut[0]<cut[0] ? -1 : o.cut[0]>cut[0] ? 1 : 0;
      }
   }

   private String ip(double raw,double bzero,double bscale) {
      return cds.tools.Util.myRound(raw) + (bzero!=0 || bscale!=1 ? "/"+cds.tools.Util.myRound(raw*bscale+bzero) : "");
   }

   /** D�termination de la zone des donn�es observ�es (cercle, ellipse ou rectangle). Retourne
    * Les coordonn�es de la zone qui contient des pixels observ�es afin de supprimer le bord
    * La connaissance de la forme (ellipse ou rectangle) doit �tre connue par ailleurs
    * Conservation dans un cache, notamment dans le cas d'une image ouverte en mode blocs
    * @param f
    * @return x,y,xradius,yradius (sens FITS)
    * @throws Exception
    */
   private double [] findDataArea(Fits f) throws Exception {
      String filename = f.getFilename()+f.getMefSuffix();
      double [] shape = shapeCache.get(filename);
      if( shape!=null ) return shape;

      Fits f1 = new Fits();
      f1.loadFITS(filename);
      if( context.hasAlternateBlank() ) f1.setBlank( context.getBlankOrig() );
      shape = f1.findData();
      shapeCache.put(filename,shape);
//      context.info("Data area range => ["+shape[0]+","+shape[1]+" "+shape[2]+"x"+shape[3]+"] for "+filename);

      return shape;
   }


   private boolean first=true;

   /**
    * Applique un filtre (soustraction du skyval, division par le expTime)
    * sur les pixels avant de les mettre dans le cache
    * @param f fitsfile
    */
   private void delSkyval(Fits f,boolean flagChangeOrig) {
      double skyval = 0;
      double expTime = 1;
      //      double newval = f.blank;
      boolean skyValTag=false;
      boolean expTimeTag=false;
      double [] shape=null;
      double marge=0;
      boolean flagAuto=true;
      double pourcentMin = context.pourcentMin;
      double pourcentMax = context.pourcentMax;

      // Faut-il retrancher le fond du ciel, et par quelle m�thode ?
      if( context.skyvalName!=null ) {

         try {
            if( context.skyvalName.equalsIgnoreCase("true") ) {
               double cut [] = findAutocutRange(f,pourcentMin,pourcentMax);
               double cutOrig [] = context.getCutOrig();
               skyval = cut[0] - cutOrig[0];
               //               skyval = cut[0];
            } else {
               try {
                  skyval = f.headerFits.getDoubleFromHeader(context.skyvalName);
                  skyval = (skyval - context.bZeroOrig)/context.bScaleOrig;
                  double cutOrig [] = context.getCutOrig();
                  skyval = skyval - cutOrig[0];
                  flagAuto=false;
               } catch( Exception e ) {
                  double cut [] = findAutocutRange(f,pourcentMin,pourcentMax);
                  double cutOrig [] = context.getCutOrig();
                  skyval = cut[0] - cutOrig[0];
                  //                skyval = cut[0];
                  if( first ) {
                     context.warning("\nSKYVAL="+context.skyvalName+" not found is some images => use an estimation for these images");
                     first=false;
                  }
               }
            }

            skyValTag= skyval!=0;
         } catch (Exception e) { e.printStackTrace(); }
      }

      // Faut-il supprimer les bords dans le cas d'une observation ellipso�de ou rectangulaire
      if( context.dataArea!=Constante.SHAPE_UNKNOWN ) {
         try { shape = findDataArea(f); } catch (Exception e) { }
      }

      if( context.expTimeName!=null ) {
         try {
            expTime = f.headerFits.getDoubleFromHeader(context.expTimeName);
            expTimeTag= expTime!=1;
         } catch (NullPointerException e) { }
      }

      if( !skyValTag && !expTimeTag && !flagChangeOrig && context.pixelGood==null && shape==null ) return;

      Aladin.trace(4,"SkyVal="+skyval+(flagAuto?"( estimation)":"( from header)")+" => "+f.getFileNameExtended());


      double blank = context.hasAlternateBlank() ? context.getBlankOrig() : f.blank;
      double a2=0,b2=0;
      if( shape!=null ) {
         if( context.dataArea==Constante.SHAPE_ELLIPSE ) {
            a2=(shape[2]-context.borderSize[0]-context.borderSize[2]); a2 *= a2;
            b2=(shape[3]-context.borderSize[1]-context.borderSize[3]); b2 *= b2;
         } else {
            a2 = (shape[2]-context.borderSize[0]-context.borderSize[2])/2;
            b2 = (shape[3]-context.borderSize[1]-context.borderSize[3])/2;
         }
      }

      for( int z=0; z<f.depthCell; z++ ) {
         for( int y=0; y<f.heightCell; y++ ) {
            for( int x=0; x<f.widthCell; x++ ) {
               double pixelFull = f.getPixelDouble(x+f.xCell, y+f.yCell, z+f.zCell);

               if( Double.isNaN(pixelFull) ) continue;

               if( context.good!=null && (pixelFull<context.good[0] || context.good[1]<pixelFull) ) {
                  if( f.bitpix<0 ) f.setPixelDouble(x+f.xCell, y+f.yCell, z+f.zCell, blank);
                  else f.setPixelInt(x+f.xCell, y+f.yCell, z+f.zCell, (int)blank);
                  continue;
               }

               if( shape!=null ) {
                  double x1 = x+f.xCell - shape[0];
                  double y1 = y+f.yCell - shape[1];
                  boolean out;
                  if( context.dataArea==Constante.SHAPE_ELLIPSE ) out = (x1*x1)/a2 + (y1*y1)/b2 >= 1;
                  else out = Math.abs(x1)>=a2 || Math.abs(y1)>=b2;

                  if( out ) {
                     if( f.bitpix<0 ) f.setPixelDouble(x+f.xCell, y+f.yCell, z+f.zCell, blank);
                     else f.setPixelInt(x+f.xCell, y+f.yCell, z+f.zCell, (int)blank);
                     continue;
                  }
               }


               if( context.hasAlternateBlank() ) {
                  if( pixelFull==context.getBlankOrig() ) continue;
               } else if( f.isBlankPixel(pixelFull) ) continue;


               if( skyValTag  ) pixelFull -= skyval;
               if( expTimeTag ) pixelFull /= expTime;

               if( flagChangeOrig ) {
                  pixelFull = pixelFull*f.bscale + f.bzero;
                  pixelFull = (pixelFull - context.bZeroOrig) / context.bScaleOrig;
               }

               if( f.bitpix<0 ) f.setPixelDouble(x+f.xCell, y+f.yCell, z+f.zCell, pixelFull);
               else f.setPixelInt(x+f.xCell, y+f.yCell, z+f.zCell, (int)(pixelFull+0.5));
            }
         }
      }
   }

   static double obscale=-1;

   /** Retourne le nombre de fichiers ayant �t� ouverts */
   public int getStatNbOpen() { return statNbOpen; }

   /** Retourne le nombre de fichiers ayant �t� trouv�s directement dans le cache */
   public int getStatNbFind() { return statNbFind; }

   /** Retourne le nombre de fichiers ayant �t� supprim�s du cache */
   public int getStatNbFree() { return statNbFree; }

   public String toString() {
      //      int nbReleased = getNbReleased();
      int n = map.size();
      String s = n>1 ? "s":"";
      return "Cache: "+n+" cell"+s
            //      +(nbReleased>0 ? "("+nbReleased+" released)" : "")
            +" using "+Util.getUnitDisk(getMem())
            +(maxMem>0 ? "/"+Util.getUnitDisk(maxMem):"["+Util.getUnitDisk(maxMem)+"]")
            +" freeRAM="+Util.getUnitDisk(getFreeMem())
            +" (opened="+statNbOpen+" found="+statNbFind+" released="+statNbFree+")";
   }

   // retourne le nombre de fichier dans le cache dont le bloc m�moire pixel[]
   // est actuellement vide
   //   private int getNbReleased() {
   //      try {
   //         int n=0;
   //         Enumeration<String> e = map.keys();
   //         while( e.hasMoreElements() ) {
   //            String key = e.nextElement();
   //            if( map.get(key).fits.isReleased() ) n++;
   //         }
   //         return n;
   //      } catch( Exception e ) {
   //         return -1;
   //      }
   //   }

   // G�re une entr�e dans le cache
   protected class FitsFile {
      public Fits fits;
      long timeAccess;

      private int id;

      public FitsFile() {
         timeAccess = System.currentTimeMillis();
         id=nextId++;
      }

      //      public void free() {
      //         if( fits!=null ) fits.free();
      //      }

      public long getMem() {
         if( fits==null ) return 0L;
         return fits.getMem();
      }

      void update() { timeAccess = System.currentTimeMillis(); }

      public String toString() {
         long now = System.currentTimeMillis();
         return "["+id+"] age="+(now-timeAccess)+" => "+fits.getFileNameExtended();
      }
   }

   class ValueComparator implements Comparator {
      Map base;
      public ValueComparator(Map base) {
         this.base = base;
      }

      // dans le cas d'�galit� sur l'age du fichier, distingue le r�sultat en fonction
      // du num�ro unique (id). Sinon on perd un des �l�ments lors du balayage
      public int compare(Object a, Object b) {
         FitsFile a1 = (FitsFile)base.get(a);
         FitsFile b1 = (FitsFile)base.get(b);
         if( a1==null ) return 1;
         if( b1==null ) return -1;
         int val = (int)( b1.timeAccess+b1.id - a1.timeAccess );
         if( val==0 ) return b1.id-a1.id;
         return val;
      }
   }

   static long lastTimeMem=0L;
   static long lastMem=0;

   /** Retourne le nombre d'octets disponibles en RAM */
  static public long getFreeMem() {
      //      long t1 = System.nanoTime();
      //      if( t1-lastTimeMem<100000 ) return lastMem;
      lastMem = Runtime.getRuntime().maxMemory()-
            (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
      return lastMem;
   }

   /** Lib�re toute la m�moire inutile */
   public void gc() {
      //      System.runFinalization();
      System.gc();
      Util.pause(100);
   }



   //   static final String NAME1 = "C:/2MASS.fits";
   //   static final String NAME2 = "C:/Documents and Settings/Standard/Bureau/CFHTLS_W_g_222054+011900_T0007.fits[100,100-1000x1000]";
   //   static final String NAME3 = "C:/Test.fits";
   //   static final String NAME4 = "C:/Documents and Settings/Standard/Bureau/CFHTLS_W_g_222054+011900_T0007.fits[200,100-500x500]";
   //
   //   static public void main(String argv[]) {
   //      try {
   //         CacheFits cf = new CacheFits(70*1024*1024,2);
   //         Fits f1 = cf.getFits(NAME1);
   //         System.out.println("f1 = "+f1);
   //         System.out.println(cf);
   //         Fits f2 = cf.getFits(NAME2);
   //         System.out.println("f2 = "+f2);
   //         System.out.println(cf);
   //         Fits f3 = cf.getFits(NAME3);
   //         System.out.println("f3 = "+f3);
   //         System.out.println(cf);
   //         Fits f4 = cf.getFits(NAME4);
   //         System.out.println("f4 = "+f4);
   //         System.out.println(cf);
   //         f3 = cf.getFits(NAME3);
   //         System.out.println("f3 = "+f3);
   //         System.out.println(cf);
   //         f4 = cf.getFits(NAME4);
   //         System.out.println("f4 = "+f4);
   //         System.out.println(cf);
   //         f2 = cf.getFits(NAME2);
   //         System.out.println("f2 = "+f2);
   //         System.out.println(cf);
   //
   //
   //      } catch( Exception e ) { e.printStackTrace(); }
   //   }

}

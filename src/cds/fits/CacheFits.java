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
 * Permet d'éviter les ouvertures et réouvertures inutiles lors d'un traitement nécessitant plusieurs fois
 * l'accès aux mêmes images.
 * 
 * Il faut mentionner à la création la taille max allouée au cache, en Mo et en Nb de fichiers, ainsi que la borne max en volume, resp en nombre max en cas de modification
 * dynamique de la capacité du cache. Ainsi lorsque le cache sature, et qu'il n'arrive pas à être purger sans virer des fichiers encore très récemment utilisés,
 * la capacité du cache va automatiquement augmenté d'un tiers (sans dépasser les bornes indiquées). Cette augmentation ne sera que temporairie dans la mesure du possible
 * et cette capacité sera peu à peu diminuer pour reprendre les valeurs déclarées au départ.
 * 
 * L'accès à un fichier Fits se fait par Fits getFits(filename)( ou getFits(filename,true) pour du JPEG)
 *
 * Rq: Le rajouti d'Anaïs sur la soustraction du skyvalName ne devrait pas être dans cette classe (selon moi)
 * => A voir si on le déplace
 *
 * @author Pierre Fernique [CDS]
 * @version 1.3 - novembre 2022 - ajout de la variation dynamique de la capacité du cache
 * @version 1.2 - novembre 2014
 * @version 1.1 - juillet 2012
 * @version 1.0 - sept 2011
 */
public class CacheFits {
   
   static final private long DELAYINCREASE = 5 * 60000L;  // Délai avant d'éventuellement diminuer les capacités du cache (5mn)
   private long timeLastIncrease=0L;                      // Date de la dernière augmentation des capacités du cache
   
   static private final long DEFAULT_MAXMEM=512*1024*1024L;   // Taille max courante du cache par défaut
   static protected final int DEFAULT_MAXFILE = 500;          // Nbre max d'items du cache par défaut
   
   private long LIMITMEM;           // Taille max autorisée
   private int LIMITFILE;           // Nombre max d'items autorisés
   
   private long maxMem;             // Taille max courante (en octets)
   private int maxFile;             // Nombre max courant d'items
   
   private long initMem;             // Taille max courante souhaitée au départ (en octets)
   private int initFile;             // Nombre max courant d'items souhaité au départ 

   private int nextId;              // prochain identificateur unique de fichier
   volatile private boolean cacheOutOfMem;   // En cas de débordement mémoire, on vire totalement le cache
   protected HashMap<String, FitsFile> map;             // Table des fichiers
   private int nbClean;  // Nombre de purges du cache

   private Context context;
   private Hashtable<String, double[]> cutCache = new Hashtable<>();
   private Hashtable<String, double[]> shapeCache = new Hashtable<>();

   protected int statNbOpen,statNbFind,statNbFree;

   /**
    * Création d'un cache de fichiers Fits
    */
   public CacheFits() { this(DEFAULT_MAXMEM,DEFAULT_MAXFILE,    DEFAULT_MAXMEM*3,DEFAULT_MAXFILE*3); }
   public CacheFits(long maxMem) { this(maxMem,DEFAULT_MAXFILE, DEFAULT_MAXMEM*3,DEFAULT_MAXFILE*3); }

   /**
    * Création d'un cache de fichiers Fits
    * @param maxMem taille max du cache (en bytes)
    * @param maxFile nombre max de fichier du cache
    * @param limitMem borne max du cache en cas d'augmentation dynamique de sa taille (voir description de la classe)
    * @param limitFile borne max en nombre de fichiers en cas d'augmentation dynamique de ses capacités
    * @param rewriteMode true si les fichiers du cache seront réécrits systématiquement
    */
   public CacheFits(long maxMem,int maxFile,long limitMem, int limitFile) {
      if( maxMem>limitMem ) maxMem=limitMem;
      if( maxFile>limitFile ) maxFile=limitFile;
      
      this.maxMem = this.initMem = maxMem;
      this.maxFile = this.initFile = maxFile;
      
      this.LIMITMEM = limitMem;
      this.LIMITFILE = limitFile;
      
      cacheOutOfMem = maxMem==0;
      nextId = 0;
      nbClean=0;
      statNbFree = statNbOpen = statNbFind = 0;
      map = new HashMap<>(maxFile+maxFile/2);
   }
   
   /** Retourne la limite courante en mémoire du cache (en bytes) */
   public long getMaxMem() { return maxMem; }
   
   /** Retourne la limite courante en nombre de fichiers du cache */
   public int getMaxFile() { return maxFile; }

   // Gestion d'un lock
   static protected final Object lockObj= new Object();

   static public final int FITS = 0; // FITS classique
   static public final int JPEG = 1; // JPEG
   static public final int PNG  = 2; // PNG
   static public final int HHH  = 4; // HHH (combiné avec JPEG ou PNG)

   /**
    * Récupération d'un Fits spécifié par son nom de fichier.
    * @param fileName Nom du fichier Fits à accéder (supporte le mode mosaic nomfichier[x,y-wxh])
    * @return l'objet Fits
    * @throws Exception
    */
   public Fits getFits(String fileName) throws Exception,MyInputStreamCachedException { return getFits(fileName,FITS,true,true); }
   public Fits getFits(String fileName,int mode,boolean flagLoad,boolean keepHeader) throws Exception,MyInputStreamCachedException {
      if( cacheOutOfMem )  return open(fileName,mode,flagLoad,keepHeader).fits;

      synchronized( lockObj  ) {
         FitsFile f = find(fileName);

         // Trouvé, je le mets à jour
         if( f!=null ) {
            f.update();
            statNbFind++;
         }

         // Pas trouvé, je l'ajoute
         else {
            if( isOver() ) clean();
            try {
               f=add(fileName,mode,flagLoad,keepHeader);
            } catch( OutOfMemoryError e ) {
               System.out.println("CacheFits.getFits("+fileName+") out of memory... clean and try again...");
               if( maxMem<0 ) maxMem*=2;
               else maxMem /= 2;
               try {
                  clean();
                  f=add(fileName,mode,flagLoad,keepHeader);
               } catch( OutOfMemoryError e1 ) {
                  System.out.println("CacheFits.getFits("+fileName+") out of memory... double error... removing the cache...");
                  e1.printStackTrace();
                  reset();
                  cacheOutOfMem=true;
                  return open(fileName,mode,flagLoad,keepHeader).fits;
               }
            }
            statNbOpen++;
         }

         return f.fits;
      }
   }

   // Retrouve l'objet Fits dans le cache, null si inconnu
   protected FitsFile find(String name) {
      return map.get(name);
   }

   // Ajoute un fichier Fits au cache. Celui-ci est totalement chargé en mémoire
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
   
   /** Remplace une extension par une autre dans un nom de ficheir
    * ex: toto.hhh[xxx] => toto.fits[xxx]
    * @param filename Le nom du fichier
    * @param orig l'extension d'origine (sans le point), ou null pour n'importe quelle extension
    * @param ext la nouvelle extension (sans le point)
    * @return le nom de fichier avec la nouvelle extension
    */
   private String replaceExt(String filename,String orig, String ext) {
      if( orig==null ) orig="";
      int pos = filename.lastIndexOf("."+orig);
      if( pos==-1 ) return filename+"."+ext;
      return filename.substring(0,pos)+"."+ext+filename.substring(pos+("."+orig).length() );
      
   }
   
   // Ouvre un fichier
   private FitsFile open(String fileName,int mode,boolean flagLoad,boolean keepHeader) throws Exception,MyInputStreamCachedException {
      boolean flagChangeOrig=false;

      // Le fichier existe-t-il ? (suppression d'une éventuelle extension [x,y-wxh]
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
      if( context!=null && context.getBlankKey()!=null ) f.fits.setBlank(context.getBlankKey());

      // Il faut lire deux fichiers, le HHH, puis le JPEG ou PNG, voire FITS suivant le cas
      if( (mode&HHH)!=0 ) {
         f.fits.loadHeaderFITS(fileName);
//         String pngFile  = fileName.replaceAll("\\.hhh",".png");
//         String jpgFile  = fileName.replaceAll("\\.hhh",".jpg");
         String pngFile   = replaceExt(fileName,"hhh","png");
         String jpgFile   = replaceExt(fileName,"hhh","jpg");
         String fitsFile  = replaceExt(fileName,"hhh","fits");
         
         // On ne connait pas le type de fichier pour les pixels,
         // => on va voir ce qui existe
         boolean modeSpecialFitsHHH=false;
         if( (mode&(JPEG|PNG))==0 ) {
            String racJpgFile  = jpgFile;
            String racPngFile  = pngFile;
            String racFitsFile = fitsFile;
            int i;
            if( (i=  jpgFile.indexOf(".jpg["))>0 )   racJpgFile  = jpgFile.substring(0,i+4);
            if( (i=  pngFile.indexOf(".png["))>0 )   racPngFile  = pngFile.substring(0,i+4);
            if( (i= fitsFile.indexOf(".fits["))>0 ) racFitsFile  = fitsFile.substring(0,i+5);

            if( (new File(racJpgFile)).exists() ) mode |= JPEG;
            else if( (new File(racPngFile)).exists() ) mode |= PNG;
            else if( (new File(racFitsFile)).exists() ) modeSpecialFitsHHH=true;
         }
         if( (mode&PNG)!=0  ) fileName=pngFile;
         else if( (mode&JPEG)!=0 ) fileName=jpgFile;
         else if( modeSpecialFitsHHH ) fileName=fitsFile;
         else throw new Exception(".hhh file without associated .jpg, .png or .fits file");
      }

      if( (mode&(PNG|JPEG))!=0 ) {
         int format = (mode&PNG)!=0 ? Fits.PREVIEW_PNG
               : (mode&JPEG)!=0 ? Fits.PREVIEW_JPEG
                     : Fits.PREVIEW_UNKOWN;
         f.fits.loadPreview(fileName,true, (mode&HHH)==0, format);
      }
      else {
         f.fits.loadFITS(fileName, false, flagLoad,true);
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
                     context.cunit3=null;
                  }
               }
            }
         }
      }
      
      // applique un filtre spécial
      if ( context!=null && (context.skyvalName!=null 
            || context.expTimeName!=null
            || context.pixelGood!=null || flagChangeOrig 
            || context.dataArea!=Constante.SHAPE_UNKNOWN )) {
         applyPostFilter(f.fits,flagChangeOrig);
      }

      // On ne conserve pas le HeaderFits
      if( !keepHeader ) f.fits.freeHeader();
      return f;
   }

   // Retourne true si le cache est en surcapacité
   protected boolean isOver() {
      if( map.size()>maxFile ) return true;
      if( maxMem<0 ) {
         //         System.out.println("Cachemem="+Util.getUnitDisk(mem)+" freeMem="+Util.getUnitDisk(getFreeMem())
         //               +" maxMem="+Util.getUnitDisk(-maxMem)
         //               +" isOver="+(mem>getFreeMem()+maxMem));
         //         return mem>getFreeMem()+maxMem;
         return getFreeMem()<-maxMem;
      }
      return getMem()>maxMem;
   }

   /** Retourne la taille occupée par le cache */
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
   
   // Supprime les plus vieux éléments du cache pour
   // qu'il y ait un peu de place libre
   protected void clean() {
      
      synchronized( lockObj ) {
         long mem = getMem();
         long freeMem = getFreeMem();
         
         boolean tooManyFile = map.size()>maxFile;
         boolean tooManyMem = maxMem<0 && freeMem<-maxMem || maxMem>=0 && mem>maxMem;
         if( !tooManyFile && !tooManyMem ) return;
         
//         if( !isOver() ) return;
         
         long totMem=0L;
         long m=0;
         int nb=0;
         boolean encore=true;
         long now = Util.getTime(0);
         long delay=5000;
         boolean flagGC=false;

         int pass;
         try {
            cacheOutOfMem=true;

            HashMap<String,String> libere = new  HashMap<>(map.size());
            HashMap<String,FitsFile> map1 = new  HashMap<>(maxFile+maxFile/2);

            // en premier tour, on supprime les fits non utilisés
            // depuis plus de 5s, et si pas assez de mémoire, on ne regarde plus la date
            for( pass=0; pass<2 && encore; pass++) {
               int mapsize=map.size();

               for( String key: map.keySet() ) {
                  FitsFile f = map.get(key);
                  if( f.fits.hasUsers() ) continue;
                  if( pass==0 && now-f.timeAccess<delay ) continue;
                  m = f.getMem();
                  totMem+=m;
                  nb++;
                  statNbFree++;

                  libere.put(key,"");
                  if( totMem>mem/3L && mapsize-nb < 2*maxFile/3 ) { encore=false; break; }
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
            }

            // Je fais explicitement un gc() si la taille RAM restante est plus petite que la moitié de la taille du cache
            // sinon on laisse la JVM gérer tout seul
            if( freeMem < mem/2 ) { gc(); flagGC=true; }
            
         } finally {
            cacheOutOfMem=false;
         }

         long duree = Util.getTime(0) - now;
         String s1 = pass>1 ? "s":"";
         long freeRam = getFreeMem();
         nbClean++;
         if( context!=null && context.getVerbose()>=3) {
            String tps = duree>1000 ? " in "+Util.getTemps(duree/1000L):""; 
            String p = pass>1 ? " in "+pass+" steps":"";
            context.stat("Cache: freeRAM="+Util.getUnitDisk(freeMem)+" => "+nb+" files released ("+Util.getUnitDisk(totMem)+")"+p+s1+tps
            +(flagGC?" => freeRAM="+Util.getUnitDisk(freeRam):""));
         }
         
         // Modification de la capacité du cache
         // (augmentation si dernier nettoyage en 2 passes, diminution si ça fait longtemps qu'on n'a pas du augmenter)
         now = System.currentTimeMillis();
         if( pass>1 ) { 
            if( increaseCache() ) timeLastIncrease=now;
         } else {
            if( System.currentTimeMillis()-timeLastIncrease>DELAYINCREASE ) decreaseCache();
         }
      }
   }
   
   // Augmente la capacité du cache si possible
   private boolean increaseCache() {
      if( maxFile>=LIMITFILE || maxMem>=LIMITMEM) return false;
      long memPerItem = maxMem / maxFile;

      // J'ajoute 50% d'items en plus
      int maxFile2 = maxFile+ maxFile/2;
      if( maxFile2>LIMITFILE ) maxFile2=LIMITFILE;
      long maxMem2 = maxFile2 * memPerItem;
      if( maxMem2 > LIMITMEM ) maxMem2=maxMem2;
      maxFile=maxFile2;
      maxMem=maxMem2;
      if( context!=null && context.getVerbose()>=3) {
         context.stat("Cache: increaseCache: items="+maxFile+" mem="+Util.getUnitDisk(maxMem));
      }
      return true;
   }
   
   // Diminue la capacité du cache si possible
   private boolean decreaseCache() {
      if( maxFile<=initFile || maxMem<=initMem ) return false;
      
      int delay=5000;
      long now = System.currentTimeMillis();
      
      // Je vérifie qu'il y a suffisamment d'items que l'on peut effectivement libérer
      int nb=0;
      for( String key: map.keySet() ) {
         FitsFile f = map.get(key);
         if( f.fits.hasUsers() ) continue;
         if( now-f.timeAccess<delay ) continue;
         nb++;
         if( nb< maxFile/3 ) return false;
      }
      
      long memPerItem = maxMem / maxFile;

      // Je diminue d'un tier d'items
      int maxFile2 = maxFile- maxFile/3;
      if( maxFile2  < initFile ) maxFile2=initFile;
      long maxMem2 = maxFile2 * memPerItem;
      if( maxMem2 < initMem ) maxMem2=initMem;
      maxFile=maxFile2;
      maxMem=maxMem2;
      if( context!=null && context.getVerbose()>=3) {
         context.stat("Cache: decreaseCache: items="+maxFile+" mem="+Util.getUnitDisk(maxMem));
      }
      return true;
   }
   
   /** Retourne le nombre de fois où le cache a été purgé */
   public int getNbClean() { return nbClean; }

   // Reset totalement le cache
   public void reset() {
      statNbFree+=map.size();
      
      if( this instanceof CacheFitsWriter ) {
         ArrayList<String> a = new ArrayList<>(map.size());
         for( String key: map.keySet() ) a.add(key);
         for( String key: a ) {
            try { remove(key); } catch( Exception e1 ) { }
         }
         
      } else map.clear();

      gc();
   }


   public void close() { reset(); }

   public void setContext(Context c) { context = c; }


   protected static int WIDTHAUTOCUT =-1;
   protected static int HEIGHTAUTOCUT=-1;

   // Détermination des cuts d'une image,
   // @param pourcentMin % de l'info ignoré en début d'histogramme (ex: 0.003)
   // @param pourcentMax % de l'info ignoré en fin d'histogramme (ex: 0.9995)
   // et conservation dans un cache pour éviter de refaire plusieurs fois
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
         
         // Initialisation de la taille
         if( WIDTHAUTOCUT==-1 ) {
            if( f1.width>512 && f1.height>512 ) {
               WIDTHAUTOCUT = (int)(f1.width/3.5);
               if( WIDTHAUTOCUT>1024 ) WIDTHAUTOCUT=1024;
               HEIGHTAUTOCUT = (int)(f1.height/3.5);
               if( HEIGHTAUTOCUT>1024 ) HEIGHTAUTOCUT=1024;
               context.info("skyval estimation based on median on 5 regions ("+WIDTHAUTOCUT+"x"+HEIGHTAUTOCUT+" pixels)");
            } else {
               WIDTHAUTOCUT = (int)(f1.width*0.8);
               WIDTHAUTOCUT = (int)(f1.width*0.8);
               context.info("skyval estimation based central region ("+WIDTHAUTOCUT+"x"+HEIGHTAUTOCUT+" pixels)");
           }
         }
         
         int w=f1.width>WIDTHAUTOCUT ? WIDTHAUTOCUT : f1.width;
         int h=f1.height>HEIGHTAUTOCUT ? HEIGHTAUTOCUT : f1.height;
         int d=f1.depth>10 ? 10 : f1.depth;
         int x = f.width/2-w/2, y=f.height/2-h/2, z=f.depth/3-d/3;

         // Pour debug
         ox=x; oy=y; oz=z; ow=w; oh=h; od=d;

         f1.loadFITS(f.getFilename(),f.ext,x,y,z,w,h,d);
         if( context.hasAlternateBlank() ) f1.setBlank( context.getBlankOrig() );
         cut = f1.findAutocutRange(pourcentMin,pourcentMax);

         //      System.out.println("cut central: "+cut[0]+" pour "+filename);

         // Si l'image est assez grande on va faire 4 autres mesures
         // et prendre la médiane des 5
         if( width>3*WIDTHAUTOCUT &&height>3*WIDTHAUTOCUT ) {
            int gapx = (width-2*WIDTHAUTOCUT)/3;
            int gapy = (height-2*HEIGHTAUTOCUT)/3;
            for( int i=0; i<4; i++ ) {
               x = i==0 || i==2 ? gapx : width-WIDTHAUTOCUT-gapx;
               y = i<2 ? gapy : height-HEIGHTAUTOCUT-gapy;
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

   /** Détermination de la zone des données observées (cercle, ellipse ou rectangle). Retourne
    * Les coordonnées de la zone qui contient des pixels observées afin de supprimer le bord
    * La connaissance de la forme (ellipse ou rectangle) doit être connue par ailleurs
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
    * Applique un filtre (soustraction du skyval, division par le expTime, masque, Lupton, ...)
    * sur les pixels avant de les mettre dans le cache
    * @param f fitsfile
    */
   private void applyPostFilter(Fits f,boolean flagChangeOrig) {
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
      
      // Faut-il retrancher le fond du ciel, et par quelle méthode ?
      if( context.skyvalName!=null ) {

         try {
            if( context.skyvalName.equalsIgnoreCase("auto") ) {
               double cut [] = findAutocutRange(f,pourcentMin,pourcentMax);
               double cutOrig [] = context.getCutOrig();
               skyval = cut[0] - cutOrig[0];
               //               skyval = cut[0];
            } else {
               try {
                  skyval = f.headerFits.getDoubleFromHeader(context.skyvalName);
                  double cutOrig [] = context.getCutOrig();
                  
                  double refSkyVal = cutOrig[0]*context.bScaleOrig+context.bZeroOrig;
                  skyval -= refSkyVal;
                  skyval = (skyval - f.bzero)/f.bscale;
                  flagAuto=false;
               } catch( Exception e ) {
                  double cut [] = findAutocutRange(f,pourcentMin,pourcentMax);
                  double cutOrig [] = context.getCutOrig();
                  skyval = cut[0] - cutOrig[0];
                  if( first ) {
                     context.warning("\nSKYVAL="+context.skyvalName+" not found is some images => use an estimation for these images");
                     first=false;
                  }
               }
            }

            skyValTag= skyval!=0;
         } catch (Exception e) { e.printStackTrace(); }
      }

      // Faut-il supprimer les bords dans le cas d'une observation ellipsoïde ou rectangulaire
      if( context.dataArea!=Constante.SHAPE_UNKNOWN ) {
         try { shape = findDataArea(f); } catch (Exception e) { }
      }

      if( context.expTimeName!=null ) {
         try {
            expTime = f.headerFits.getDoubleFromHeader(context.expTimeName);
            expTimeTag= expTime!=1;
         } catch (NullPointerException e) { }
      }

      // Pas besoin d'aller plus loin
      if( !skyValTag && !expTimeTag && !flagChangeOrig 
            && context.pixelGood==null && shape==null ) return;

      if( skyValTag ) Aladin.trace(4,"SkyVal="+skyval+(flagAuto?"( estimation)":"( from header)")+" => "+f.getFileNameExtended());

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

   /** Retourne le nombre de fichiers ayant été ouverts */
   public int getStatNbOpen() { return statNbOpen; }

   /** Retourne le nombre de fichiers ayant été trouvés directement dans le cache */
   public int getStatNbFind() { return statNbFind; }

   /** Retourne le nombre de fichiers ayant été supprimés du cache */
   public int getStatNbFree() { return statNbFree; }

   public String toString() {
      //      int nbReleased = getNbReleased();
      int n = map.size();
      String s = n>1 ? "s":"";
      return "RAM cache: "+n+" item"+s+"/"+maxFile
            //      +(nbReleased>0 ? "("+nbReleased+" released)" : "")
            +" using "+Util.getUnitDisk(getMem())
            +(maxMem>0 ? "/"+Util.getUnitDisk(maxMem):"["+Util.getUnitDisk(maxMem)+"]")
            +" freeRAM="+Util.getUnitDisk(getFreeMem())
            +" (opened="+statNbOpen+" reused="+statNbFind+" released="+statNbFree+(nbClean>0?" [flush:"+nbClean+"x]":"")+")";
   }

   // retourne le nombre de fichier dans le cache dont le bloc mémoire pixel[]
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

   // Gère une entrée dans le cache
   protected class FitsFile {
      public Fits fits;
      long timeAccess;

      private int id;
      
      public int hashCode() { return Integer.hashCode(id); } 

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

      // dans le cas d'égalité sur l'age du fichier, distingue le résultat en fonction
      // du numéro unique (id). Sinon on perd un des éléments lors du balayage
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

   /** Libère toute la mémoire inutile */
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

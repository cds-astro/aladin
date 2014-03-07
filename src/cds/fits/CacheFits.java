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

package cds.fits;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import cds.allsky.Constante;
import cds.allsky.Context;
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
 * @version 1.1 - juillet 2012
 * @version 1.0 - sept 2011
 */
public class CacheFits {

   static private final long DEFAULT_MAXMEM=512*1024*1024L;

   private long maxMem;     // Taille max (en octets)
   private int nextId;      // prochain identificateur unique de fichier
   private boolean cacheOutOfMem;   // En cas de d�bordement m�moire, on vire totalement le cache
   private Hashtable<String, FitsFile> map;             // Table des fichiers
//   private TreeMap<String,FitsFile> sortedMap;        // Table tri� par ordre de dernier acc�s
   Context context;
   private Hashtable<String, double[]> cutCache = new Hashtable<String, double[]>();
   

   //private boolean skyvalSub = false;// condition d'application d'une soustraction du skyval au moment 
   //de la mise dans le cache

   private int statNbOpen,statNbFind,statNbFree;

   /**
    * Cr�ation d'un cache de fichiers Fits
    */
   public CacheFits() { this(DEFAULT_MAXMEM); }

   /**
    * Cr�ation d'un cache de fichiers Fits
    * @param maxMem limite en bytes occup�s, ou si n�gatif, 
    *          nombre de bytes � garder libre par rapport � la RAM dispo
    */
   public CacheFits(long maxMem) {
      this.maxMem=maxMem;
      cacheOutOfMem=maxMem==0;
      nextId=0;
      statNbFree=statNbOpen=statNbFind=0;
      map = new Hashtable<String, FitsFile>(20000);
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
   static private final Object lockObj= new Object();
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
  public Fits getFits(String fileName) throws Exception { return getFits(fileName,FITS,true); }
  public Fits getFits(String fileName,int mode,boolean flagLoad) throws Exception {
     if( cacheOutOfMem )  return open(fileName,mode,flagLoad).fits;

     synchronized( lockObj  ) {
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
              f=add(fileName,mode,flagLoad);
           } catch( OutOfMemoryError e ) {
              System.err.println("CacheFits.getFits("+fileName+") out of memory... clean and try again...");
              if( maxMem<0 ) maxMem*=2;
              else maxMem /= 2;
              try {
                 clean();
                 f=add(fileName,mode,flagLoad);
              } catch( OutOfMemoryError e1 ) {
                 System.err.println("CacheFits.getFits("+fileName+") out of memory... double error... removing the cache...");
                 e1.printStackTrace();
                 reset();
                 cacheOutOfMem=true;
                 return open(fileName,mode,flagLoad).fits;
              }
           }
           statNbOpen++;
        }

        return f.fits;
     }

  }
  
   // Retrouve l'objet Fits dans le cache, null si inconnu
   private FitsFile find(String name) { 
      return map.get(name);
   }

   // Ajoute un fichier Fits au cache. Celui-ci est totalement charg� en m�moire
   private FitsFile add(String name,int mode,boolean flagLoad) throws Exception {
      FitsFile f = open(name,mode,flagLoad);
      map.put(name, f);
      return f;
   }
   
   // Ouvre un fichier
   private FitsFile open(String fileName,int mode,boolean flagLoad) throws Exception {
      FitsFile f = new FitsFile();
      f.fits = new Fits();
      if( context.skyvalName!=null ) { flagLoad=true; f.fits.setReleasable(false); }
      
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
         f.fits.loadJpeg(fileName,true, (mode&HHH)==0);
      }
      else {
         f.fits.loadFITS(fileName, false, flagLoad);
//         f.fits.initPixDoubleFast();
      }


      // applique un filtre sp�cial
      if (context.skyvalName!=null || context.expTimeName!=null || context.pixelGood!=null ) delSkyval(f.fits);

      return f;
   }

   // Retourne true si le cache est en surcapacit�
   private boolean isOver() {
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
      Enumeration<String> e = map.keys();
      while( e.hasMoreElements() ) {
         String key = e.nextElement();
         FitsFile f = map.get(key);
         mem += f.fits.getMem();
      }
      return mem;
   }
   
   // Force le nettoyage du clean
   public void forceClean() {
      synchronized( lockObj  ) {  clean(); }
   }


   // Supprime les plus vieux �l�ments du cache pour 
   // qu'il y ait un peu de place libre
   private void clean() {
      long mem = getMem();
      long totMem=0L;
      long m=0;
      int nb=0;
      long freeMem = getFreeMem();
      boolean encore=true;
      long now = System.currentTimeMillis();
      long delay=5000;
      
      // en premier tour, on supprime les fits non utilis�s 
      // depuis plus de 5s, et si pas assez de m�moire, on ne regarde plus la date
      int i;
      for( i=0; i<2 && encore; i++) {
         Enumeration<String> e = map.keys();
         while( e.hasMoreElements() ) {
            String key = e.nextElement();
            FitsFile f = map.get(key);
            if( f.fits.hasUsers() ) continue;
            if( i==0 && now-f.timeAccess<delay ) continue;
            m = f.getMem();
            totMem+=m;
            nb++;
            statNbFree++;
            map.remove(key);
            if( totMem> mem/2L  ) { encore=false; break; }
         }
      }
      
//      sortedMap.clear();
      gc();
      
      long duree = System.currentTimeMillis() - now;
      String s1 = i>1 ? "s":"";
      long freeRam = getFreeMem();
      context.stat("Cache: freeRAM="+Util.getUnitDisk(freeMem)+" => "+nb+" files removed ("+Util.getUnitDisk(totMem)+") in "+i+" step"+s1+" in "+Util.getTemps(duree)
            +" => freeRAM="+Util.getUnitDisk(freeRam));
   }
   
   // Reset totalement le cache
   public void reset() {
      statNbFree+=map.size();
      map.clear();
      gc();
   }
   
   public void setContext(Context c) { context = c; }

   
   // D�termination des cuts d'une image,
   // et conservation dans un cache pour �viter de refaire plusieurs fois
   // le calcul notamment dans le cas d'une image ouverte en mode "blocks"
   private double [] findAutocutRange(Fits f) throws Exception {
      String filename = f.getFilename()+f.getMefSuffix();
      double [] cut = cutCache.get(filename);
      if( cut!=null ) return cut;
      Fits f1 = new Fits();
      int w=1024;
      int x = f.width/2-w/2, y=f.height/2-w/2;
      f1.loadFITS(f.getFilename(),f.ext,x,y,w,w);
      if( context.hasAlternateBlank() ) f1.setBlank( context.getBlankOrig() );
      cut = f1.findAutocutRange();
      cutCache.put(filename,cut);
//      context.info("Skyval estimation => "+ip(cut[0],f1.bzero,f1.bscale)+" for "+filename);

      return cut;
   }
   
//   private String ip(double raw,double bzero,double bscale) {
//      return cds.tools.Util.myRound(raw) + (bzero!=0 || bscale!=1 ? "/"+cds.tools.Util.myRound(raw*bscale+bzero) : "");
//   }

   
   private boolean first=true;

   /**
    * Applique un filtre (soustraction du skyval, division par le expTime)
    * sur les pixels avant de les mettre dans le cache
    * @param f fitsfile
    */
   private void delSkyval(Fits f) {
      double skyval = 0;
      double expTime = 1;
//      double newval = f.blank;
      boolean skyValTag=false;
      boolean expTimeTag=false;
      
      if( context.skyvalName!=null ) {

         try {
            if( context.skyvalName.equalsIgnoreCase("true") ) {
               double cut [] = findAutocutRange(f);
               double cutOrig [] = context.getCutOrig();
               skyval = cut[0] - cutOrig[0];
               //               skyval = cut[0];
            } else {
               try {
                  skyval = f.headerFits.getDoubleFromHeader(context.skyvalName);
                  double cutOrig [] = context.getCutOrig();
                  skyval = skyval - cutOrig[0];
               } catch( Exception e ) {
                  double cut [] = findAutocutRange(f);
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
         } catch (Exception e) { }
      }
      
      if( context.expTimeName!=null ) {
         try {
            expTime = f.headerFits.getDoubleFromHeader(context.expTimeName);
            expTimeTag= expTime!=1;
         } catch (NullPointerException e) { }
      }
      
      if( !skyValTag && !expTimeTag && f.bzero==0 && f.bscale==1 && context.pixelGood==null ) return;
      
//      System.out.println("SkyVal="+skyval+" => "+f.getFilename()+f.getFileNameExtended());
      
      
      double blank = context.hasAlternateBlank() ? context.getBlankOrig() : f.blank;
      
      for( int y=0; y<f.heightCell; y++ ) {
         for( int x=0; x<f.widthCell; x++ ) {
            double pixelFull = f.getPixelDouble(x+f.xCell, y+f.yCell);
            
            if( Double.isNaN(pixelFull) ) continue; 
            
            if( context.pixelGood!=null && (pixelFull<context.pixelGood[0] || context.pixelGood[1]<pixelFull) ) {
               if( f.bitpix<0 ) f.setPixelDouble(x+f.xCell, y+f.yCell, blank);
               else f.setPixelInt(x+f.xCell, y+f.yCell, (int)blank); 
               continue;
            }
             
            if( context.hasAlternateBlank() ) {
               if( pixelFull==context.getBlankOrig() ) continue;
            } else if( f.isBlankPixel(pixelFull) ) continue;

            pixelFull = pixelFull*f.bscale + f.bzero;

            if( skyValTag  ) pixelFull -= skyval;
            if( expTimeTag ) pixelFull /= expTime;
            
            pixelFull = (pixelFull - f.bzero) / f.bscale;

            if( f.bitpix<0 ) f.setPixelDouble(x+f.xCell, y+f.yCell, pixelFull);
            else f.setPixelInt(x+f.xCell, y+f.yCell, (int)(pixelFull+0.5)); 
         }
      }
   }

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
      return "Cache: "+n+" file"+s
//      +(nbReleased>0 ? "("+nbReleased+" released)" : "")
            +" using "+Util.getUnitDisk(getMem())
            +(maxMem>0 ? "/"+Util.getUnitDisk(maxMem):"["+Util.getUnitDisk(maxMem)+"]")
            +" freeRAM="+Util.getUnitDisk(getFreeMem())
            +" (open="+statNbOpen+" find="+statNbFind+" remove="+statNbFree+")";
     }

   // retourne le nombre de fichier dans le cache dont le bloc m�moire pixel[]
   // est actuellement vide
   private int getNbReleased() {
      try {
         int n=0;
         Enumeration<String> e = map.keys();
         while( e.hasMoreElements() ) {
            String key = e.nextElement();
            if( map.get(key).fits.isReleased() ) n++;
         }
         return n;
      } catch( Exception e ) {
         return -1;
      }
   }
   
   // G�re une entr�e dans le cache
   class FitsFile {
      Fits fits;
      long timeAccess;

      private int id;

      FitsFile() {
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
   public long getFreeMem() {
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

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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import cds.allsky.Constante;
import cds.tools.Util;


/**
 * Classe de manipulation d'un cache de fichiers FITS/JPEG ouverts.
 * Permet d'éviter les ouvertures et réouvertures inutiles lors d'un traitement nécessitant plusieurs fois
 * l'accès aux mêmes images.
 * Il faut mentionner à la création la taille max allouée au cache, en Mo (par défaut 512Mo) et en Nb de fichiers (par défaut 10000)
 * L'accès à un fichier Fits se fait par Fits getFits(filename)( ou getFits(filename,true) pour du JPEG)
 * 
 * Rq: Le rajouti d'Anaïs sur la soustraction du skyvalName ne devrait pas être dans cette classe (selon moi)
 * => A voir si on le déplace
 * 
 * @author Pierre Fernique [CDS]
 * @version 1.1 - juillet 2012
 * @version 1.0 - sept 2011
 */
public class CacheFits {

   static private final long DEFAULT_MAXMEM=512*1024*1024L;

   private long maxMem;     // Taille max (en octets)
   private int  maxFile;    // Nombre de fichiers max
   private int nextId;      // prochain identificateur unique de fichier
   private boolean cacheOutOfMem;   // En cas de débordement mémoire, on vire totalement le cache
   private HashMap<String, FitsFile> map;             // Table des fichiers
   private TreeMap<String,FitsFile> sortedMap;        // Table trié par ordre de dernier accès
   String skyvalName;          // Nom du champ pour soustraire le fond au moment 
   //de la mise dans le cache


   //private boolean skyvalSub = false;// condition d'application d'une soustraction du skyval au moment 
   //de la mise dans le cache

   private int statNbOpen,statNbFind,statNbFree;

   /**
    * Création d'un cache de fichiers Fits
    */
   public CacheFits() { this(DEFAULT_MAXMEM); }

   /**
    * Création d'un cache de fichiers Fits
    * @param maxMem limite en bytes occupés, ou si négatif, 
    *          nombre de bytes à garder libre par rapport à la RAM dispo
    */
   public CacheFits(long maxMem) {
      this.maxMem=maxMem;
      cacheOutOfMem=maxMem==0;
      nextId=0;
      statNbFree=statNbOpen=statNbFind=0;
      map = new HashMap<String, FitsFile>(maxFile);
      sortedMap = new TreeMap<String, FitsFile>( new ValueComparator(map) );
   }
   
   /** Vérification qu'il reste assez de mémoire RAM, et sinon
    * on va libérer temporairement les blocs pixels[] non utilisés afin de récupérer
    * ce qui manque
    * @param rqMem taille de la mémoire requise
    * @return true c'est bon, false, faut attendre
    */
   public boolean needMem(long rqMem) {
      if( getFreeMem()>rqMem ) return true;
      
      long mem = 0L;
      synchronized( this ) {
         for( String key : map.keySet() ) {
            Fits f = map.get(key).fits;
            long m = f.getMem();
            try { f.releaseBitmap();
            } catch( Exception e ) { } mem += m-f.getMem();
            if( mem>rqMem ) break;
         }
      }
      gc();
      return mem>rqMem;
   }

   /**
    * Récupération d'un Fits spécifié par son nom de fichier. 
    * @param fileName Nom du fichier Fits à accéder (supporte le mode mosaic nomfichier[x,y-wxh])
    * @return l'objet Fits
    * @throws Exception
    */
  public Fits getFits(String fileName) throws Exception { return getFits(fileName,false,true); }
  synchronized public Fits getFits(String fileName,boolean jpeg,boolean flagLoad) throws Exception {
      if( cacheOutOfMem ) return open(fileName,jpeg,flagLoad).fits;
      
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
            f=add(fileName,jpeg,flagLoad);
         } catch( Throwable e ) {
            System.err.println("CacheFits.getFits("+fileName+") out of memory... clean and try again...");
            maxMem /= 2;
            try {
               clean();
               f=add(fileName,jpeg,flagLoad);
            } catch( Throwable e1 ) {
               System.err.println("CacheFits.getFits("+fileName+") out of memory... double error... removing the cache...");
               reset();
               cacheOutOfMem=true;
               return open(fileName,jpeg,flagLoad).fits;
            }
         }
         statNbOpen++;
      }

      return f.fits;

   }

   // Retrouve l'objet Fits dans le cache, null si inconnu
   private FitsFile find(String name) { return map.get(name); }

   // Ajoute un fichier Fits au cache. Celui-ci est totalement chargé en mémoire
   private FitsFile add(String name,boolean jpeg,boolean flagLoad) throws Exception {
      FitsFile f = open(name,jpeg,flagLoad);
      map.put(name, f);
      return f;
   }
   
   // Ouvre un fichier
   private FitsFile open(String fileName,boolean jpeg,boolean flagLoad) throws Exception {
      FitsFile f = new FitsFile();
      f.fits = new Fits();
      if( skyvalName!=null ) { flagLoad=true; f.fits.setReleasable(false); }
      if( jpeg ) f.fits.loadJpeg(fileName,true);
      else f.fits.loadFITS(fileName, false, flagLoad);

      // applique un filtre spécial
      if (skyvalName!=null) delSkyval(f.fits);

      return f;
   }

   // Retourne true si le cache est en surcapacité
   private boolean isOver() {
      long mem = getMem();
      if( maxMem<0 ) return mem>getFreeMem()+maxMem;
      return mem>maxMem;
   }

   /** Retourne la taille occupée par le cache */
   public long getMem() {
      long mem=0L;
      if( map==null ) return mem;
      for( String key : map.keySet() ) {
         FitsFile f = map.get(key);
         mem += f.fits.getMem();
      }
      return mem;
   }

   // Supprime les plus vieux éléments du cache pour 
   // qu'il y ait un peu de place libre
   private void clean() {
      long mem = getMem();
      sortedMap.clear();
      sortedMap.putAll(map);
      long totMem=0L;
      long m=0;
      int nb=0;
      long freeMem = getFreeMem();
      
      for( String key : sortedMap.keySet() ) {
         FitsFile f = map.get(key);
         if( f.fits.hasUsers() ) continue;
         m = f.getMem();
         totMem+=mem;
         mem -= m;
         nb++;
         statNbFree++;
         map.remove(key);
//         System.out.println("clean.remove "+key );
         
         if( maxMem<0 && mem<freeMem+4*(maxMem/3L) || maxMem>=0 && mem<3*(maxMem/4L) ) break;
      }
      
      sortedMap.clear();
      gc();
      System.out.println("\nCache: "+nb+" files removed ("+Util.getUnitDisk(totMem)+"): "+this);
   }
   
   // Reset totalement le cache
   synchronized public void reset() {
      statNbFree+=map.size();
      map.clear();
      gc();
   }
   
   public void setSkySub(String key) {
      skyvalName = key;
   }

   /**
    * Applique un filtre (soustraction du skyval)
    * sur les pixels avant de les mettre dans le cache
    * @param f fitsfile
    */
   private void delSkyval(Fits f) {
      // enlève le fond de ciel
      double skyval = 0;
      double newval = f.blank;
      try {
    	  skyval = f.headerFits.getDoubleFromHeader(skyvalName);
      } catch (NullPointerException e) {
    	  // On n'a pas trouve le mot cle, tampis on sort proprement
    	  return;
      }
      if (skyval != 0) {
    	  skyval -= 1;
    	  for( int y=0; y<f.heightCell; y++ ) {
    		  for( int x=0; x<f.widthCell; x++ ) {
    			  // applique un nettoyage pour enlever les valeurs aberrantes
    			  // et garder les anciens blank à blank
    			  double pixelFull = f.getPixelFull(x+f.xCell, y+f.yCell);
    			  if (pixelFull==f.blank || pixelFull<f.blank+skyval)
    				  newval = f.blank;
    			  else
    				  newval = pixelFull-skyval;

    			  switch (f.bitpix) {
    			  case 8 :
    			  case 16:
    			  case 32 :
    				  f.setPixelInt(x+f.xCell, y+f.yCell, (int)newval); break;
    			  case -32:
    			  case -64 :
    				  f.setPixelDouble(x+f.xCell, y+f.yCell, newval); break;
    			  }
    		  }
    	  }
      }

   }

   /** Retourne le nombre de fichiers ayant été ouverts */
   public int getStatNbOpen() { return statNbOpen; }

   /** Retourne le nombre de fichiers ayant été trouvés directement dans le cache */
   public int getStatNbFind() { return statNbFind; }

   /** Retourne le nombre de fichiers ayant été supprimés du cache */
   public int getStatNbFree() { return statNbFree; }

   public String toString() { 
      long upLimit = maxMem<0 ? getFreeMem()+maxMem : maxMem;
      if( upLimit<0 ) upLimit=0L;
      return "Cache: "+map.size()+" file bloc(s) ("+getNbReleased()+" released) for "+Util.getUnitDisk(getMem())
      +"/"+Util.getUnitDisk(upLimit)+" (open="+statNbOpen+" find="+statNbFind+" free="+statNbFree+") RAMfree="+Util.getUnitDisk(getFreeMem());
     }

   // retourne le nombre de fichier dans le cache dont le bloc mémoire pixel[]
   // est actuellement vide
   private int getNbReleased() {
      int n=0;
      for( String key : map.keySet() ) {
         if( map.get(key).fits.isReleased() ) n++;
      }
      return n;
   }
   
   // Gère une entrée dans le cache
   class FitsFile {
      Fits fits;
      long timeAccess;

      private int id;

      FitsFile() {
         timeAccess = System.currentTimeMillis();
         id=nextId++;
      }
      
      public void free() {
         if( fits!=null ) fits.free();
      }

      public long getMem() {
         if( fits==null ) return 0L;
         return fits.getMem(); 
      }

      void update() { timeAccess = System.currentTimeMillis(); }

      public String toString() {
         long now = System.currentTimeMillis();
         return "["+id+"] age="+(now-timeAccess)+" => "+fits.getFilename();
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
   
   /** Retourne le nombre d'octets disponibles en RAM */
   public long getFreeMem() {
      return Runtime.getRuntime().maxMemory()-
            (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
   }
   
   /** Libère toute la mémoire inutile */
   protected void gc() {
      System.runFinalization();
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

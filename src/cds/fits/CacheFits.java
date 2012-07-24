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

   static private final long DEFAULT_MAXMEM=512L;
   static private final int  DEFAULT_MAXFILE=10000;

   private long maxMem;     // Taille max (en octets)
   private int  maxFile;    // Nombre de fichiers max
   private long mem;        // m�moire courante (en octets)
   private int file;        // Nombre de fichiers g�r�s
   private int nextId;      // prochain identificateur unique de fichier
   private HashMap<String, FitsFile> map;             // Table des fichiers
   private TreeMap<String,FitsFile> sortedMap;        // Table tri� par ordre de dernier acc�s
   String skyvalName;          // Nom du champ pour soustraire le fond au moment 
   //de la mise dans le cache


   //private boolean skyvalSub = false;// condition d'application d'une soustraction du skyval au moment 
   //de la mise dans le cache

   private int statNbOpen,statNbFind,statNbFree;

   /**
    * Cr�ation d'un cache de fichiers Fits
    */
   public CacheFits() { this(DEFAULT_MAXMEM,DEFAULT_MAXFILE); }

   /**
    * Cr�ation d'un cache de fichiers Fits
    * @param maxMem limite en Mo occup�s
    * @param maxFile limite en nombre de fichiers ouverts simultan�ment
    */
   public CacheFits(long maxMem,int maxFile) {
      this.maxMem=maxMem*1024L*1024L;
      this.maxFile=maxFile;
      mem=0L;
      file=0;
      nextId=0;
      statNbFree=statNbOpen=statNbFind=0;
      map = new HashMap<String, FitsFile>(maxFile);
      sortedMap = new TreeMap<String, FitsFile>( new ValueComparator(map) );
   }

   /**
    * R�cup�ration d'un Fits sp�cifi� par son nom de fichier. 
    * @param fileName Nom du fichier Fits � acc�der (supporte le mode mosaic nomfichier[x,y-wxh])
    * @return l'objet Fits
    * @throws Exception
    */
  public Fits getFits(String fileName) throws Exception { return getFits(fileName,false); }
  synchronized public Fits getFits(String fileName,boolean jpeg) throws Exception {
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
            f=add(fileName,jpeg);
         } catch( OutOfMemoryError e ) {
            System.err.println("CacheFits.getFits("+fileName+") out of memory... clean and try again...");
            maxMem /= 2;
            clean();
            f=add(fileName,jpeg);
         }
         statNbOpen++;
      }

      return f.fits;

   }

   // Retrouve l'objet Fits dans le cache, null si inconnu
   private FitsFile find(String name) { return map.get(name); }

   // Ajoute un fichier Fits au cache. Celui-ci est totalement charg� en m�moire
   private FitsFile add(String name,boolean jpeg) throws Exception {
      FitsFile f = new FitsFile();
      f.fits = new Fits();
      if( jpeg ) f.fits.loadJpeg(name,true);
      else f.fits.loadFITS(name);
      
      // applique un filtre sp�cial
      if (skyvalName!=null) delSkyval(f.fits);

      map.put(name, f);
      mem+=f.getMem(); ;
      file++;
      return f;
   }

   // Supprime un objet Fits du cache
   private void remove(String name) {
      FitsFile f=find(name);
      if( f==null ) return;
      statNbFree++;
      mem-=f.getMem();
      file--;
      map.remove(name);
   }

   // Retourne true si le cache est en surcapacit�
   private boolean isOver() {
      return mem>maxMem || file>maxFile;
   }

   // Retourne true si le cache est rempli � moins des 3/4
   private boolean isMemOk() {
      return mem<3*(maxMem/4) && file<3*(maxFile/4);
   }

   // Supprime les plus vieux �l�ments du cache pour qu'il ne soit rempli qu'au 3/4
   private void clean() {
      sortedMap.clear();
      sortedMap.putAll(map);
      for( String key : sortedMap.keySet() ) {
         if( isMemOk() ) return;
         remove(key);
         //         System.out.println("clean.remove "+key );
      }
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
      // enl�ve le fond de ciel
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
    			  // et garder les anciens blank � blank
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

   /** Retourne le nombre de fichiers ayant �t� ouverts */
   public int getStatNbOpen() { return statNbOpen; }

   /** Retourne le nombre de fichiers ayant �t� trouv�s directement dans le cache */
   public int getStatNbFind() { return statNbFind; }

   /** Retourne le nombre de fichiers ayant �t� supprim�s du cache */
   public int getStatNbFree() { return statNbFree; }

   /** Retourne le volume courant du cache en octets */
   public long getStatMem() { return mem; }


   public String toString() { return "CacheFits: "+file+" file(s) for "+Util.getUnitDisk(mem)
      +" open="+statNbOpen+" find="+statNbFind+" free="+statNbFree; }

   // G�re une entr�e dans le cache
   class FitsFile {
      Fits fits;
      long timeAccess;

      private int id;

      FitsFile() {
         timeAccess = System.currentTimeMillis();
         id=nextId++;
      }

      public long getMem() {
         if( fits==null ) return 0L;
         return fits.widthCell*fits.heightCell*Math.abs(fits.bitpix==0 ? 32 : fits.bitpix)/8; 
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

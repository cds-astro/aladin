// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.allsky;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import cds.aladin.Cache;
import cds.aladin.MyInputStream;
import cds.aladin.PlanImageRice;
import cds.fits.HeaderFits;
import cds.image.Bzip2;
import cds.tools.Util;

public class MyInputStreamCached extends MyInputStream {
   
   // Taille max du cache disque par défaut (cf setCacheLimit() )
   static private long DEFAULTLIMIT = 500*1024; // 500Go
   
   // Nom du fichier dans le cache (clé unique)
   private String nameInCache=null;
   
   // Juste pour pouvoir faire des affichages cohérents
   static public Context context=null;
   
   /** Seul le constructeur avec un vrai fichier local est disponible */
   public MyInputStreamCached(String filename) throws Exception {
      super( new FileInputStream(filename) );
      this.filename = filename;
   }
   
   private MyInputStreamCached(InputStream in,long type,String filename) {
      super(in,type,true);
      this.filename = filename;
   }

   /**
    * Si le flux est compressé (gzip ou Rice), on va décompresser dans un cache disque temporaire
    * @return le flux lui-meme, ou un nouveau flux s'il s'agissait d'un flux compressé
    */
   public MyInputStream startRead() throws IOException,Exception,MyInputStreamCachedException {
      
      long type = getType(10000);
      
      if( (type & ( GZ|BZIP2|RICE) )==0 ) return this;
      
      synchronized( lock ) {
         if( (type & (GZ|BZIP2))!=0 )    return convertGZorBzip2(type);
         if( (type & RICE)!=0 )          return convertRice();
      }
      throw new MyInputStreamCachedException("Compression mode not implemented yet!");
   }
   
   /** Récupération du nom interne qu'aura le fichier dans le cache */
   private String getTargetName() { return getTargetName(filename); }
   static private String getTargetName(String filename) { return Cache.codage(filename); }
   
   // Fonction inverse
   static private String getFilenameOrig(String nameInCache) { return Cache.decodage(nameInCache); }
   
   static private String getFilenameOrig(File f) throws Exception {
      String filename = f.getCanonicalPath();
      int i = filename.lastIndexOf(Util.FS);
      return filename.substring(i+1);
   }
   
   private MyInputStream convertGZorBzip2(long type) throws Exception,MyInputStreamCachedException {
      
      // Détermination du nom de fichier dans le cache disque
      File dir = getCacheDir();
      nameInCache = getTargetName();
      File file = new File( dir.getCanonicalPath()+Util.FS+nameInCache );
      
      // N'existe pas encore ? => il faut décompresser et stocker dans le cache
      if( !file.exists() ) {
         
         // Ajustement éventuelle de la taille du cache en supposant que le fichier
         // décompresser prendra au mieux 3x la taille du fichier compressé
         checkCache( (new File(filename)).length()*(2/(1024*1024.)) );
         
         // Décompression
         InputStream in = null;
         OutputStream out = null;
         try {
            in = (type & GZ)!=0 ? new GZIPInputStream(this) : new Bzip2(this);
            out = new FileOutputStream(file);
            byte [] buf = new byte[512];
            int n;
            long size=0;
            while( (n=in.read(buf))>=0 ) { size+=n; out.write( buf, 0 , n); }
            
            // Positionnement de la nouvelle taille du cache disque
            cacheSize+=size/(1024*1024.);
            
         } finally {
            in.close();
            out.close();
         }
      }
//         System.out.println("Add in cache "+nameInCache);
//         
//      } else System.out.println("Reuse from cache "+nameInCache);
      
      return new MyInputStreamCached( new FileInputStream(file), type, filename);
   }
   
   private MyInputStream convertRice() throws Exception,MyInputStreamCachedException {
      
      // Détermination du nom de fichier dans le cache disque
      File dir = getCacheDir();
      nameInCache = getTargetName();
      String targetFile = dir.getCanonicalPath()+Util.FS+nameInCache;
      File file = new File( targetFile );
      
      // N'existe pas encore ? => il faut décompresser et stocker dans le cache
      if( !file.exists() ) {
         
         // Ajustement éventuelle de la taille du cache en supposant que le fichier
         // décompresser prendra au mieux 3x la taille du fichier compressé
         checkCache( (new File(filename)).length()*(3/(1024*1024.)) );
         
         HeaderFits header = new HeaderFits(this);
         
         // Décompression
         OutputStream out = null;
         try {
            out = new FileOutputStream(file);
            
            // La première entête
            long size = header.writeHeader(out);

            // Chaque HDU RICE, jusqu'à la fin du fichier
            long n;
            while( (n=writeRice( out, this))!=-1 ) {
               size+=n;
            }
            
            // Positionnement de la nouvelle taille du cache disque
            cacheSize+=size/(1024*1024.);
            
         // Y a un problème, l'image en cours de construction est supprimée du cache
         } catch( Exception e ) {
            out.close();
            out=null;
            file.delete();
            throw e;
            
         } finally {
            close();
            if( out!=null ) out.close();
         }
      }
      
      return new MyInputStreamCached( new FileInputStream(file), RICE, filename);
   }
   
   protected long writeRice(OutputStream os, MyInputStream dis) throws Exception {

      HeaderFits headerFits;
      try {
         headerFits = new HeaderFits(dis);
      } catch( EOFException e0 ) { return -1; }

      int bitpix = headerFits.getIntFromHeader("ZBITPIX");
      int naxis1 = headerFits.getIntFromHeader("ZNAXIS1");
      int naxis2 = headerFits.getIntFromHeader("ZNAXIS2");
      int n = Math.abs(bitpix)/8;    // Nombre d'octets par valeur

      int nnaxis1 = headerFits.getIntFromHeader("NAXIS1");
      int nnaxis2 = headerFits.getIntFromHeader("NAXIS2");
      int theap=nnaxis1*nnaxis2;
      try  { theap = headerFits.getIntFromHeader("THEAP"); } catch( Exception e ) {}

      int pcount=headerFits.getIntFromHeader("PCOUNT");    // nombres d'octets a lire en tout
      int tile = headerFits.getIntFromHeader("ZTILE1");

      int nblock=32;
      try { nblock = headerFits.getIntFromHeader("ZVAL1"); } catch( Exception e ) {}

      int bsize=4;
      try { bsize = headerFits.getIntFromHeader("ZVAL2"); } catch( Exception e ) {}

      int posCompress=0;
      int posZscale=-1;
      int posZzero=-1;
      int posUncompress=-1;

      int tfields = headerFits.getIntFromHeader("TFIELDS");
      for( int i=1,pos=0; i<=tfields; i++ ) {
         String type = headerFits.getStringFromHeader("TTYPE"+i);
         if( type.equals("COMPRESSED_DATA") ) posCompress = pos;
         if( type.equals("ZSCALE") ) posZscale = pos;
         if( type.equals("ZZERO") ) posZzero = pos;
         if( type.equals("UNCOMPRESSED_DATA") ) posUncompress = pos;
         String form = headerFits.getStringFromHeader("TFORM"+i);
         pos+=Util.binSizeOf(form);
      }
//      System.out.println("Converting RICE FITS image (TFIELDS="+tfields+" NBLOCK="+nblock+" BSIZE="+bsize+")");
      
      byte [] pixelsOrigin = new byte[naxis1*naxis2*n];
      byte [] table = new byte[nnaxis1*nnaxis2];
      byte [] heap = new byte[pcount];

      dis.readFully(table);
      dis.skip(theap - nnaxis1*nnaxis2);
      dis.readFully(heap);

      int offset=0;
      for( int row=0; row<nnaxis2; row++ ) {
         int offsetRec = row*nnaxis1;
         int size = PlanImageRice.getInt(table,offsetRec+posCompress);
         int pos = PlanImageRice.getInt(table,offsetRec+posCompress+4);
         double bzero = posZscale<0 ? 0 : PlanImageRice.getDouble(table,offsetRec+posZzero);
         double bscale = posZscale<0 ? 1 : PlanImageRice.getDouble(table,offsetRec+posZscale);

         // Non compressé
         if( size==0 && posUncompress>=0 ) {
            size = PlanImageRice.getInt(table,offsetRec+posUncompress);
            pos  = PlanImageRice.getInt(table,offsetRec+posUncompress);
            PlanImageRice.direct(heap,pos,pixelsOrigin,offset,tile,bitpix,bzero,bscale);

            // Compressé
         } else PlanImageRice.decomp(heap,pos,pixelsOrigin,offset,tile,bsize,nblock,bitpix,bzero,bscale);

         offset+=tile;
      }
      
      // Alignement sur 2880
      // On se cale sur le prochain segment de 2880
      long pos = getPos();
      if( pos%2880!=0 ) {
         long off = ((pos/2880)+1) *2880  -pos;
         skip(off);
      }
      
      // Génération de l'entête de sortie
      HeaderFits outHeader = new HeaderFits();
      Hashtable<String,String> map = headerFits.getHashHeader();
      Enumeration<String> e = headerFits.getKeys();
      while( e.hasMoreElements() ) {
         String key = e.nextElement();
         if( Util.indexInArrayOf(key, KEYIGNORE)>=0 ) continue;
         
         String  val;
              if( key.equals("XTENSION") ) val="IMAGE";
         else if( key.equals("BITPIX") )   val=bitpix+"";
         else if( key.equals("NAXIS1") )   val=naxis1+"";
         else if( key.equals("NAXIS2") )   val=naxis2+"";
         else if( key.equals("NAXIS") )    val="2";
         else if( key.equals("PCOUNT") )   val="0";
         else if( key.equals("GCOUNT") )   val="1";
         else val = map.get(key);
         outHeader.setKeyValue(key, val);
      }
      
      // Ecriture de l'image FITS décompressée
      long size = outHeader.writeHeader(os);
      os.write(pixelsOrigin);
      size += pixelsOrigin.length;
      os.write( getBourrage(size) ); 
      
      return size;
   }
   
   static private String [] KEYIGNORE = { "TFIELDS","TFIELDS","TTYPE1","TFORM1",
         "ZIMAGE","ZTILE1","ZTILE2","ZCMPTYPE","ZNAME1","ZVAL1","ZNAME2","ZVAL2","ZSIMPLE","ZBITPIX",
         "ZNAXIS","ZNAXIS1","ZNAXIS2","ZEXTEND","ZPCOUNT","ZGCOUNT","ZTENSION" };
   
   static public byte[] getBourrage(long currentPos) {
      int n = (int)(currentPos % 2880L);
      int size = n == 0 ? 0 : 2880 - n;
      byte[] b = new byte[size];
      return b;
   }
   
   /** Vérification de la taille du cache disque, et nettoyage si nécessaire
    * @param size nombre d'octets qu'il faudrait ajouter au cache
    */
   static private void checkCache(double size) throws Exception,MyInputStreamCachedException {
      if( cacheSize+size < cacheLimit ) return;
      
      // Nettoyage des fichiers qui ne sont plus utilisés
      File dir = getCacheDir();
      File [] files = dir.listFiles();
      double rmSize=0.;
      int n=0;
      for( File f : files ) {
         if( stillActive(f) ) continue;
         rmSize += f.length()/(1024*1024.);
         f.delete();
         n++;
         if( cacheSize-rmSize < (cacheLimit/3)*2 ) {
            cacheSize -= rmSize;
            if( context!=null ) context.info("Disk cache "+n+" file"+(n>1?"s":"")+"/"+Util.getUnitDisk(rmSize+"MB")
                +" released => "+Util.getUnitDisk((long)cacheSize+"MB")+"/"+Util.getUnitDisk(cacheLimit+"MB"));
            return;
         }
      }
      
      // Impossible de libérer assez de place - le cache est trop petit
      if( context!=null ) context.abort("Cache disk overflow ! "+cachedir+" => "+Util.getUnitDisk((long)cacheSize+"MB")+"/"+Util.getUnitDisk(cacheLimit+"MB"));

      throw new MyInputStreamCachedException();
   }
   
   /** Vrai s'il y a encore au-moins un utilisateur du fichier cache indiqué */
   static private boolean stillActive(File f) throws Exception {
      String filename = f.getCanonicalPath();
      int i = filename.lastIndexOf(Util.FS);
      String nameInCache = filename.substring(i+1);
      Integer n = activeFile.get(nameInCache);
      return n!=null && n>0;
    }
   
   /** Mémorise le nombre d'utilisateur de chaque fichier dans le cache disque */
   static private HashMap<String, Integer> activeFile = new HashMap<>();
   
   static Object lock = new Object();
   
   /** Incrémente le nombre d'utilisateur du ficheir f */
   static public void incActiveFile(String filenameOrig) { 
      String f = getTargetName(filenameOrig);
      synchronized( lock ) {
         Integer nbActive = activeFile.get(f);
         int n;
         if( nbActive==null ) n=1;
         else n= nbActive+1;
//         System.out.println("DiskCacheInc "+n+" for "+filenameOrig);
         activeFile.put(f, n);
      }
   }
   
   /** Décrémente le nombre d'utilisateur du ficheir f. Supprime l'entrée si <= 0 */
   static public void decActiveFile(String filenameOrig) {
      String f = getTargetName(filenameOrig);
      synchronized( lock ) {
         Integer nbActive = activeFile.get(f);
         if( nbActive==null ) return;
         int n = nbActive-1;
//         System.out.println("DiskCacheDec "+n+" for "+filenameOrig);
         if( n<=0 ) activeFile.remove(f);
         else activeFile.put(f,n);
      }
   }
   
   
   static private File cachedir = null;
   static private double cacheSize = 0;
   static private long cacheLimit = DEFAULTLIMIT;  
   
   /** Indication explicite d'un emplacement pour le cache disque, ainsi que sa taille limite (en MB) */
   static public void setCache(File dir) throws Exception { setCache(dir,DEFAULTLIMIT); }
   static public void setCache(File dir,long sizeLimit) throws Exception {
      synchronized( lock ) {
         if( cachedir!=null ) throw new Exception("Cache dir already in use ["+cachedir.getCanonicalPath()+"]");
         cachedir=dir;
         cacheLimit = sizeLimit==-1 ? DEFAULTLIMIT : sizeLimit;
         cacheSize=0L;
         if( cachedir==null || !cachedir.exists() ) return;
         
         // Détermination de la taille initiale
         File [] files = cachedir.listFiles();
         
         if( files!=null ) {
            for( File f : files ) { cacheSize += f.length()/(1024*1024.); }
            setCacheSize(cacheLimit);
         }
         
         if( context!=null ) context.info("Cache disk reused: "+cachedir.getAbsolutePath()+" (max size: "+Util.getUnitDisk(cacheLimit+"MB")+")");

      }
   }
   
   /** Supprime le cache disque */
   static public void removeCache() {
      synchronized( lock ) {
         if( cachedir==null ) return;
         
         double rmSize=0;
         int n=0;
         File [] files = cachedir.listFiles();
         if( files!=null ) for( File f : files ) {
            rmSize += f.length()/(1024*1024.);
            n++;
            f.delete();
         }
         
         cachedir.delete();
         try {
            if( context!=null ) context.info("Cache disk removed: "+cachedir.getAbsolutePath()+" ("+n+"file"+(n>1?"s":"")
                  +"/"+Util.getUnitDisk((long)rmSize+"MB")+")!");
         } catch( Exception e ) {
            e.printStackTrace();
         }
         
         // Réinitialisation au cas d'une nouvelle utilisation ultérieure
         cachedir = null;
         cacheSize = 0.;
         cacheLimit = DEFAULTLIMIT;
         activeFile = new HashMap<>();
      }
   }
   
   /** Retourne true si au-moins un fichier est encore en cours d'utilisation dans le cache disque */
   static public boolean stillActive() {
      if( cachedir==null ) return false;

      boolean rep=false;
      File [] files = cachedir.listFiles();
      for( File f : files ) {
         try {
            if( stillActive(f) ) {
               rep = true;
//               System.out.println("Still in use : "+getFilenameOrig(f));
            }
         } catch( Exception e ) { }
      }
      return rep;
   }

   /** Modification de la taille limite du cache disque, et réajustement si nécesssaire (en MB)*/
   static public void setCacheSize(long sizeLimit) throws Exception {
      synchronized( lock ) {
         cacheLimit = sizeLimit;
         checkCache(0);
      }
   }
   
   /** Retourne le répertoire du cache Disque en cours d'utilisation, le crée automatiquement si nécessaire */
   static public File getCacheDir() throws Exception {
      boolean flagCreate=false;
      if( cachedir==null ) { cachedir = createTempDir(); flagCreate=true; }
      else if( !cachedir.exists() ) {
         flagCreate=cachedir.mkdirs();
         if( !flagCreate ) throw new MyInputStreamCachedException("Cache disk creation error ("+cachedir.getAbsolutePath()+")");
      }
      if( flagCreate ) {
         long freeSpace = (long)( cachedir.getFreeSpace()/(1024*1024.) );
         if( freeSpace<cacheLimit ) {
            long newsize = (3L*freeSpace)/5L;
            String s = "Cache disk: not enoug space on partition for "+Util.getUnitDisk(cacheLimit+"MB")
            +" => assume "+Util.getUnitDisk(newsize+"MB");
            if( context!=null ) context.warning(s);
            else System.err.println(s);
            cacheLimit = newsize;
//            throw new MyInputStreamCachedException("Cache disk: not enough space on partition ("
//                  +Util.getUnitDisk(cacheLimit+"MB")+"/"+Util.getUnitDisk(freeSpace+"MB")+") "
//                  +cachedir.getAbsolutePath());
         }
         
         if( context!=null ) context.info("Cache disk created: "+cachedir.getAbsolutePath()+" (max size: "+Util.getUnitDisk(cacheLimit+"MB")+")");
      }
      return cachedir;
   }
   
   static private int TEMP_DIR_ATTEMPTS = 10000;
   public static File createTempDir() {
      File baseDir = new File(System.getProperty("java.io.tmpdir"));
      String baseName = System.currentTimeMillis() + "-";

      for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
        File tempDir = new File(baseDir, baseName + counter);
        if (tempDir.mkdir()) return tempDir;
      }
      
      throw new IllegalStateException("Failed to create directory within "
          + TEMP_DIR_ATTEMPTS + " attempts (tried "
          + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
    }



}

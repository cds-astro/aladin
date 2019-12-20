// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

package cds.allsky;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cds.tools.pixtools.Util;


/**
 * Permet la compression de certaines tuiles Fits (les 3 premiers niveaux et le Allsky.fits)
 * Rq : Ne compresse jamais le niveau le plus profond
 * @author P. Fernique [CDS]
 * @version 1.0 - mai 2012 - création
 */
public class BuilderZip extends Builder {
   
   public BuilderZip(Context context) {
      super(context);
   }
   
   public Action getAction() { return Action.GZIP; }

   public void validateContext() throws Exception {
      validateOutput();
   }
   
   /** Gzippe toutes les tuiles FITS ainsi que le fichier Allsky.fits qui se trouve
    * dans le répertoire Allsky repéré par root.
    * Attention: ne change pas pour autant les extensions des fichiers (toujours.fits)
    */
   public void run() throws Exception { 
      try {
         zip();
      } catch( Exception e ) {
         e.printStackTrace();
         throw e;
      }
   }
   
   // lance le gzip (resp gunzip) récursivement sur tous les répertoire Norder??
   // Dans le cas où un fichier est déjà gzippé (resp. gunzippé), le fichier est simplement ignoré
   protected void zip() throws Exception {
      String path = context.getOutputPath();
      int maxOrder = Util.getMaxOrderByPath(path);

      for( int order=0; order<=maxOrder; order++ ) {
         File nOrder = new File(path+"/Norder"+order);
         if( nOrder.isDirectory() ) {
            File [] dirs = nOrder.listFiles( new FileFilter() {
               public boolean accept(File pathname) {
                  String name = pathname.getName();
                  if( !name.startsWith("Dir") ) return false;
                  try { Integer.parseInt( name.substring(3) ); } catch( Exception e ) { return false; }
                  return true;
               }
            });
            for( File dir : dirs ) { {
               zip( dir.getCanonicalPath() );
               // IL FAUDRAIT ICI SUPPRIMER L'ARBORESCENCE CORRESPONDANTE
            }
            }
         }
      }
   }
   
   static final int ENTRYSIZE = 50;
   
   class IndexEntry {
      String name;
      BigInteger  offset;
      
      IndexEntry(String name, BigInteger offset) {
         this.name = name;
         this.offset = offset;
      }
      
      int write(byte [] buf, int pos) {
         String s = String.format("%-18s%30d\r\n", name, offset);
         System.arraycopy( s.getBytes(),0,buf,pos,ENTRYSIZE);
         return pos+ENTRYSIZE;
      }
   }

   // gzip (resp. gunzip) du fichier indiqué. 
   // Dans le cas où un fichier est déjà gzippé (resp. gunzippé), le fichier est simplement ignoré
   private void zip(String dir) throws Exception {
      File [] files = (new File(dir)).listFiles();
      if( files==null ) return;
      
      File zip =  new File(dir+".zip");
      if( zip.exists() ) zip.delete();
      
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream( zip ));
      out.setMethod(ZipEntry.STORED);
      
      ArrayList<IndexEntry> index = new ArrayList<>();
      BigInteger offset= new BigInteger("0");
      for( File f : files ) {
         offset = offset.add( new BigInteger(""+f.length()));
         index.add( new IndexEntry(f.getName(),offset) );
      }
      int indexSize = index.size() * ENTRYSIZE;
      byte [] indexBuf = new byte[ indexSize ];
      int pos=0;
      for( IndexEntry e : index ) { 
         e.offset = e.offset.add( new BigInteger(""+indexSize)); 
         pos = e.write( indexBuf, pos);
      };
      
      ZipEntry e = new ZipEntry("00INDEX.LIST");
      CRC32 crc = new CRC32();
      crc.update( indexBuf );
      e.setSize( indexSize );
      e.setCompressedSize( indexSize );
      e.setCrc( crc.getValue() );
      out.putNextEntry(e);
      out.write( indexBuf );
      out.closeEntry();
      
      for( File f : files ) {
         e = new ZipEntry(f.getName());

         RandomAccessFile rf = new RandomAccessFile(f, "r");
         long size = rf.length();
         byte [] buf = new byte[(int)size];
         int taille= (int)size;
         pos=0;
         while( taille>0 ) {
            int n=rf.read(buf,pos,taille);
            taille-=n;
            pos+=n;
         }
         
         crc.reset();
         crc.update(buf);
         
         e.setSize(size);
         e.setCompressedSize(size);
         e.setCrc(crc.getValue());
         out.putNextEntry(e);

         out.write(buf);
         rf.close();
         
         out.closeEntry();

      }
      out.close();
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
   }
   
}

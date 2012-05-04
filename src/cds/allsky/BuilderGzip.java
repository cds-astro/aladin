// Copyright 2012 - UDS/CNRS
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

import static cds.tools.Util.FS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import cds.aladin.MyInputStream;
import cds.moc.Healpix;
import cds.tools.pixtools.Util;


/**
 * Classe de compression (resp. décompression) de toutes les tuiles Fits
 * @author P. Fernique [CDS]
 * @version 1.0 - mai 2012 - création
 */
final public class BuilderGzip {
   
   private String root;     // Répertoire racine à partir duquel il faut (un)zipper les tuiles FITS
   private int verbose;     // Niveau de verbosité 0-rien, 1-étoiles, 2-fichiers
   private int nbFile;      // Nombre de fichires traités
   
   public BuilderGzip(String root) { this( root,0); }
   public BuilderGzip(String root,int verbose) {
      this.root = root;
      this.verbose=verbose;
      nbFile=0;
   }
   
   /** Gzippe toutes les tuiles FITS ainsi que le fichier Allsky.fits qui se trouve
    * dans le répertoire Allsky repéré par root.
    * Attention: ne change pas pour autant les extensions des fichiers (toujours.fits)
    */
   public void gzip() { gzipRec(true); }

   /** Gunzippe toutes les tuiles FITS ainsi que le fichier Allsky.fits qui se trouve
    * dans le répertoire Allsky repéré par root
    * Attention: ne change pas pour autant les extensions des fichiers (toujours.fits)
    */
   public void gunzip() { gzipRec(false); }
   
   /** Permutation du mode de compression des tuiles.
    * Se base sur l'état du fichier Allsky.fits pour déterminer l'état courant */
   public void switchGzip() throws Exception {
      MyInputStream mis = new MyInputStream( new FileInputStream( getAllskyPath() ) );
      boolean doCompress = !mis.isGZ();
      mis.close();
      if( verbose>0 ) System.out.println("Starting "+(doCompress?"gzip":"gunzip")+"...");
      gzipRec(doCompress);
   }
   
   // retourne le path du fichier Allsky.fits
   private String getAllskyPath() { return root+FS+"Norder3"+FS+"Allsky.fits"; }
   
   // lance le gzip (resp gunzip) récursivement sur tous les répertoire Norder??
   // Dans le cas où un fichier est déjà gzippé (resp. gunzippé), le fichier est simplement ignoré
   private void gzipRec(boolean compress) {
      int order;
      String path = root;
      
      // Parcours de tous les répertoire Norder?? trouvés
      for( File nOrder : (new File(path)).listFiles() ) {
         String name = nOrder.getName();
         if( !name.startsWith("Norder") ) continue;
         if( !nOrder.isDirectory() ) continue;
         try { order = Integer.parseInt(name.substring(6)); }
         catch( Exception e ) { continue; }
         
         
         // traitement particulier pour le fichier Allsky.fits qui se trouve dans le Norder3
         if( order==3 ) {
            String allsky = getAllskyPath();
            if( (new File(allsky)).isFile() ) gzip(allsky,compress);
         }
         
         // Traitement de toutes les tuiles du niveau
         long maxNpix = Healpix.pow2(order);
         maxNpix = 12*maxNpix*maxNpix;
         for( long npix=0; npix<maxNpix; npix++ ) {
            String filename = Util.getFilePath(path, order, npix)+".fits";
            if( (new File(filename)).isFile() ) gzip(filename,compress);
         }
      }
      if( verbose>0 ) System.out.println();
   }
   
   // gzip (resp. gunzip) du fichier indiqué. 
   // Dans le cas où un fichier est déjà gzippé (resp. gunzippé), le fichier est simplement ignoré
   private void gzip(String file,boolean compress) {
      try {
        File in = new File(file);
        if( !in.isFile() ) throw new Exception(file+" does not exist !");
        MyInputStream mis = new MyInputStream(new FileInputStream(in));
        if( compress ) {
           if( mis.isGZ() ) throw new Exception(file+" already gzipped");
        } else {
           if( !mis.isGZ() ) throw new Exception(file+" not gzipped");
           mis = mis.startRead();
        }
        String outFile = file+".tmp";
        File out = new File(outFile);
        if( out.isFile() ) out.delete();
        OutputStream fos = new FileOutputStream(outFile);
        OutputStream mos = compress ? new GZIPOutputStream( fos ) : fos;
        
        byte [] buf = new byte[8192];
        int n;
        while( (n=mis.read(buf))>=0 ) mos.write(buf,0,n);
        
        mos.close();
        mis.close();
        
        in.delete();
        out.renameTo(in);
        
        if( verbose>1 ) System.out.println((compress?"gzip ":"gunzip ")+file);
        else if( verbose>0 && nbFile%100==0 ) System.out.print("*");
        
        nbFile++;
        
     } catch( Exception e ) {
        if( verbose>0 ) System.err.println(e.getMessage());
     }
   }
   
   // Test
   static public void main(String [] argv) {
      try {
         long t = System.currentTimeMillis();
         BuilderGzip bdMoc = new BuilderGzip("C:/HalphaNorthALLSKY",1);
         bdMoc.switchGzip();
         System.out.println("Done in "+((System.currentTimeMillis()-t)/1000)+"s !");
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
}

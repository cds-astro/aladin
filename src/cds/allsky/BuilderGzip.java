// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
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
 * Permet la compression de certaines tuiles Fits (les 3 premiers niveaux et le Allsky.fits)
 * Rq : Ne compresse jamais le niveau le plus profond
 * @author P. Fernique [CDS]
 * @version 1.0 - mai 2012 - création
 */
public class BuilderGzip extends Builder {
   
   private int nbFile;      // Nombre de fichires traités
   
   public BuilderGzip(Context context) {
      super(context);
      nbFile=0;
   }
   
   public Action getAction() { return Action.GZIP; }

   public void validateContext() throws Exception {
      validateOutput();
      validateDepth();
   }
   
   /** Gzippe toutes les tuiles FITS ainsi que le fichier Allsky.fits qui se trouve
    * dans le répertoire Allsky repéré par root.
    * Attention: ne change pas pour autant les extensions des fichiers (toujours.fits)
    */
   public void run() throws Exception { 
      gzipRec(true);
   }
   
   public boolean isAlreadyDone() {
      if( !context.actionPrecedeAction(Action.INDEX, Action.TILES)) return false;
      if( !context.actionPrecedeAction(Action.TILES, Action.GZIP)) return false;
      if( context.actionAlreadyDone(Action.GUNZIP) && !context.actionPrecedeAction(Action.GZIP, Action.GUNZIP)) return false;
      context.info("GZIP seems to be already done");
      return true;
   }
   
   // lance le gzip (resp gunzip) récursivement sur tous les répertoire Norder??
   // Dans le cas où un fichier est déjà gzippé (resp. gunzippé), le fichier est simplement ignoré
   protected void gzipRec(boolean compress) throws Exception {
      String path = context.getOutputPath();
      int maxOrder = Util.getMaxOrderByPath(path);
      int order;
      
      // Parcours de tous les répertoire Norder?? trouvés
      for( File nOrder : (new File(path)).listFiles() ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         String name = nOrder.getName();
         if( !name.startsWith("Norder") ) continue;
         if( !nOrder.isDirectory() ) continue;
         try { order = Integer.parseInt(name.substring(6)); }
         catch( Exception e ) { continue; }
         
         for( int z=0; z<context.depth; z++ ) {
            // traitement particulier pour le fichier Allsky.fits qui se trouve dans le Norder3
            if( order==3 ) {
               String allsky = path+FS+"Norder3"+FS+"Allsky"+(z==0?"":"_"+z)+".fits";
               if( (new File(allsky)).isFile() ) { gzip(allsky,compress); nbFile++; }
            }

            // On ne compresse pas les tuiles au-delà de l'ordre 5
            // Ni celles du dernier niveau
            if( compress && (order>Constante.GZIPMAXORDER || order==maxOrder) ) continue;

            // Traitement de toutes les tuiles du niveau
            long maxNpix = Healpix.pow2(order);
            maxNpix = 12*maxNpix*maxNpix;
            for( long npix=0; npix<maxNpix; npix++ ) {
               String filename = Util.getFilePath(path, order, npix,z)+".fits";
               if( (new File(filename)).isFile() ) {
                  gzip(filename,compress);
               }
            }
         }
      }
      
   }
   
   // gzip (resp. gunzip) du fichier indiqué. 
   // Dans le cas où un fichier est déjà gzippé (resp. gunzippé), le fichier est simplement ignoré
   private void gzip(String file,boolean compress) throws Exception {
      MyInputStream mis = null;
      OutputStream mos = null;
      OutputStream fos = null;
      try {
        File in = new File(file);
        if( !in.isFile() ) throw new Exception(file+" does not exist !");
        mis = new MyInputStream(new FileInputStream(in));
        if( compress ) {
           if( mis.isGZ() ) throw new Exception(file+" already gzipped");
        } else {
           if( !mis.isGZ() ) throw new Exception(file+" not gzipped");
           mis = mis.startRead();
        }
        String outFile = file+".tmp";
        File out = new File(outFile);
        if( out.isFile() ) out.delete();
        fos = new FileOutputStream(outFile);
        mos = compress ? new GZIPOutputStream( fos ) : fos;
        
        byte [] buf = new byte[8192];
        int n;
        while( (n=mis.read(buf))>=0 ) {
           mos.write(buf,0,n);
           if( context.isTaskAborting() ) break;
        }
        
        if( context.isTaskAborting() ) out.delete();
        else {

           in.delete();
           out.renameTo(in);

           nbFile++;
           if( context!=null ) context.setProgress(nbFile);
        }
        
     } catch( Exception e ) { }
     finally { 
        if( mis!=null ) mis.close();
        if( mos!=null ) mos.close();
        if( fos!=null ) fos.close();
     }
     
     if( context.isTaskAborting() ) throw new Exception("Task abort !");
   }
   
}

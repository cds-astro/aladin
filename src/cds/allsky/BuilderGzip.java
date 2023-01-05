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

package cds.allsky;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import cds.aladin.MyInputStream;
import cds.fits.Fits;
import cds.tools.pixtools.Util;


/**
 * Permet la compression des tuiles Fits (+ le Allsky.fits)
 * @author P. Fernique [CDS]
 * @version 1.0 - mai 2012 - création
 */
public class BuilderGzip extends BuilderRunner {
   
   public int nbFile;      // Nombre de fichires traités
   private Fits bidon;                  // Ne sert qu'à renvoyer quelque chose pour faire plaisir à BuilderTiles
   protected boolean compress;          // Compression ou decompression ?
   
   public BuilderGzip(Context context) {
      super(context);
      nbFile=0;
   }

   public Action getAction() { return Action.GZIP; }

   public void validateContext() throws Exception {
      validateOutput();
      if( !context.isExistingAllskyDir() ) throw new Exception("No Fits tile found");
      validateOrder( context.getOutputPath() );
      validateDepth();

      // Chargement du MOC réel à la place de celui de l'index (moins précis)
      try { context.loadMoc(); } catch( Exception e ) {
         context.warning("Tile MOC not found => use index MOC");
      }

      // reprise du frame si nécessaire depuis le fichier de propriété
      if( !context.hasFrame() ) context.setFrameName( getFrame() );

      context.initRegion();
   }
   
   public boolean isAlreadyDone() {
      if( !context.actionPrecedeAction(Action.INDEX, Action.TILES)) return false;
      if( !context.actionPrecedeAction(Action.TILES, Action.GZIP)) return false;
      if( context.actionAlreadyDone(Action.GUNZIP) && !context.actionPrecedeAction(Action.GZIP, Action.GUNZIP)) return false;
      context.info("GZIP seems to be already done");
      return true;
   }
   
   public void run() throws Exception { build(); }
   
   public void build() throws Exception {
      ordermax = context.getOrder();
      context.resetCheckCode("fits");
      super.build();
      context.info(getAction()+" allsky...");
      allsky( context.getOutputPath() );
   }

   public void buildPre() {
      bidon = new Fits();
      compress=true;
   }

   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,String path,int order,long npix, int z) throws Exception {
//      if( order>Constante.GZIPMAXORDER ) return bidon;
      
      String filename = Util.getFilePath(path, order, npix,z)+".fits";
      long duree = gzip(filename);
      updateStat(0,1,0,duree,0,0);
      nbFile++;
      return bidon;
   }

   protected Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[], int z) throws Exception {
//      if( order>Constante.GZIPMAXORDER ) return bidon;
      
      String filename = Util.getFilePath(path, order, npix,z)+".fits";
      long duree = gzip(filename);
      updateStat(0,0,0,0,1,duree);
      nbFile++;
      return bidon;
   }
   
   private void allsky(String path) throws Exception {
      for( int z=0; z<context.depth; z++ ) {
         String allsky = path+FS+"Norder3"+FS+"Allsky"+(z==0?"":"_"+z)+".fits";
         if( (new File(allsky)).isFile() ) {
            gzip(allsky,compress);
            nbFile++;
         }
      }
   }
   
   protected long gzip( String filename ) throws Exception {
      if( !(new File(filename)).isFile() ) return 0L;
      long t = System.currentTimeMillis();
      gzip(filename,compress);
      return System.currentTimeMillis()-t;
   }


   
//   /** Gzippe toutes les tuiles FITS ainsi que le fichier Allsky.fits qui se trouve
//    * dans le répertoire Allsky repéré par root.
//    * Attention: ne change pas pour autant les extensions des fichiers (toujours.fits)
//    */
//   public void run() throws Exception { 
////      context.warning("GZIP action is deprecated => ignored");
//      gzipRec(true);
//   }
//   
//   
//   // lance le gzip (resp gunzip) récursivement sur tous les répertoire Norder??
//   // Dans le cas où un fichier est déjà gzippé (resp. gunzippé), le fichier est simplement ignoré
//   protected void gzipRec(boolean compress) throws Exception {
//      String path = context.getOutputPath();
//      int maxOrder = Util.getMaxOrderByPath(path);
//      int order;
//      
//      context.resetCheckCode("fits");
//      
//      // Parcours de tous les répertoire Norder?? trouvés
//      for( File nOrder : (new File(path)).listFiles() ) {
//         if( context.isTaskAborting() ) throw new Exception("Task abort !");
//         String name = nOrder.getName();
//         if( !name.startsWith("Norder") ) continue;
//         if( !nOrder.isDirectory() ) continue;
//         try { order = Integer.parseInt(name.substring(6)); }
//         catch( Exception e ) { e.printStackTrace(); continue; }
//         
//         for( int z=0; z<context.depth; z++ ) {
//            // traitement particulier pour le fichier Allsky.fits qui se trouve dans le Norder3
//            if( order==3 ) {
//               String allsky = path+FS+"Norder3"+FS+"Allsky"+(z==0?"":"_"+z)+".fits";
//               if( (new File(allsky)).isFile() ) { gzip(allsky,compress); nbFile++; }
//            }
//
//            // On ne compresse pas les tuiles au-delà de l'ordre 5
//            // Ni celles du dernier niveau
//              if( order>Constante.GZIPMAXORDER || order==maxOrder) continue;
////            if( compress && (order>Constante.GZIPMAXORDER || order==maxOrder) ) continue;
//
//            // Traitement de toutes les tuiles du niveau
//            long maxNpix = Healpix.pow2(order);
//            maxNpix = 12*maxNpix*maxNpix;
//            for( long npix=0; npix<maxNpix; npix++ ) {
//               String filename = Util.getFilePath(path, order, npix,z)+".fits";
//               if( (new File(filename)).isFile() ) {
//                  long t0 = System.currentTimeMillis();
//                  gzip(filename,compress);
//                  context.stat("gzip "+filename+" in "+cds.tools.Util.getTemps(System.currentTimeMillis()-t0));
//               }
//            }
//         }
//      }
//      
//   }
   
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
           if( mis.isGZ() ) return; // throw new Exception(file+" already gzipped");
        } else {
           if( !mis.isGZ() ) return; // throw new Exception(file+" not gzipped");
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
        
        mis.close(); mis=null;
        mos.close(); mos=null; fos=null;
        
        if( context.isTaskAborting() ) out.delete();
        else {

           in.delete();
           in = new File(file);
           if( !out.renameTo(in) ) throw new Exception("Cannot rename "+outFile+" in "+file);

           nbFile++;
           if( context!=null ) context.setProgress(nbFile);
        }
        
     } 
     finally { 
        if( mis!=null ) mis.close();
        if( mos!=null ) mos.close();
        if( fos!=null ) fos.close();
     }
     
     if( context.isTaskAborting() ) throw new Exception("Task abort !");
   }
   
}

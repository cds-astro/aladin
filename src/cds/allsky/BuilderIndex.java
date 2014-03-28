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


import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/** Permet la génération de l'index HEALPix
 * Rq : le MOC de l'index sera également généré à l'issue de la génération de l'index
 * @author Standard Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderIndex extends Builder {

   private int [] borderSize= {0,0,0,0};
   private int radius = 0;
   private String currentfile = null;
   private boolean partitioning;
   private int [] hdu = null;

   // Pour les stat
   private int statNbFile;                 // Nombre de fichiers sources
   private int statNbZipFile;              // Nombre de fichiers sources gzippés
   private int statBlocFile;              // Nombre de fichiers qu'il aura fallu découper en blocs
   private long statMemFile;               // Taille totale des fichiers sources (en octets)
   private long statPixSize;               // Nombre total de pixels
   private long statMaxSize;               // taille du plus gros fichier trouvé
   private long statTime;                  // Date de début
   private int statMaxWidth, statMaxHeight, statMaxDepth, statMaxNbyte; // info sur le plus gros fichier trouvé

   boolean stopped = false;

   public BuilderIndex(Context context) { super(context); }
   
   public Action getAction() { return Action.INDEX; }

   public void run() throws Exception {
      build();
      BuilderMocIndex builderMocIndex = new BuilderMocIndex(context);
      builderMocIndex.run();
      context.setMocIndex( builderMocIndex.getMoc() ) ;
   }
   
   
   public boolean isAlreadyDone() {
      if( !context.isExistingIndexDir() ) return false;
      if( !context.actionAlreadyDone(Action.INDEX)) return false;
      if( context.getMocIndex()==null ) {
         try { context.loadMocIndex(); } catch( Exception e ) { return false; }
      }
      context.info("Pre-existing HEALPix index seems to be ready");
      return true;
   }
   
   public void validateContext() throws Exception { 
      if( context instanceof ContextGui ) {
         context.setProgressBar( ((ContextGui)context).mainPanel.getProgressBarIndex() );
      }
      
      partitioning = context.partitioning;
      if( partitioning ) context.info("Partitioning large original image files in blocks of "+Constante.FITSCELLSIZE+"x"+Constante.FITSCELLSIZE+" pixels");

      validateInput();
      validateOutput();
      
      // Tests order
      int order = context.getOrder();
      if( order==-1 ) {
         String img = context.getImgEtalon();
         if( img==null ) {
            img = context.justFindImgEtalon( context.getInputPath() );
            context.info("Use this reference image => "+img);
         }
         if( img==null ) throw new Exception("No source image found in "+context.getInputPath());
         try {
            Fits file = new Fits();
            file.loadHeaderFITS(img);
            long nside = calculateNSide(file.getCalib().GetResol()[0] * 3600.);
            order = ((int) Util.order((int) nside) - Constante.ORDER);
            context.setOrder(order);
         } catch (Exception e) {
            context.warning("The reference image has no astrometrical calibration ["+img+"] => order can not be computed");
         }
      }
      if( order==-1 ) throw new Exception("Argument \"order\" is required");
      else if( order<context.getOrder() ) {
         context.warning("The provided order ["+order+"] is less than the optimal order ["+context.getOrder()+"] => OVER-sample will be applied");
      } else if( order>context.getOrder() ) {
         context.warning("The provided order ["+order+"] is greater than the optimal order ["+context.getOrder()+"] => SUB-sample will be applied");
      } else context.info("Order="+context.getOrder()+" => PixelAngularRes="
         +Coord.getUnit( CDSHealpix.pixRes( CDSHealpix.pow2(context.getOrder()+Constante.ORDER))/3600. ) );
      
      
      // Récupération de la liste des HDU
      hdu = context.getHDU();
      if( hdu==null ) context.info("MEF stategy => extension 0, otherwise 1");
      else if( hdu.length>0 && hdu[0]==-1 ) context.info("MEF stategy => all images found in the MEF");
      else {
         StringBuilder s = new StringBuilder("MEF stategy: extensions ");
         for( int i=0; i<hdu.length; i++ ) { if( i>0 )  s.append(','); s.append(hdu[i]+""); }
         context.info(s+"");
      }

      // Pour indiquer les listes des mots clés fits dont les valeurs vont être retenues
      // dans les fichiers d'index JSON afin d'être utiliser dans l'accès à la "Table des détails" (progéniteurs)
      if( context.fitsKeys!=null ) {
         StringBuilder res = null;
         for( String key : context.fitsKeys ) {
            if( res==null ) res = new StringBuilder();
            else res.append(", ");
            res.append(key);
         }
         context.info("Extended metadata extraction based on FITS keys: "+res);
      }
   }
   
   static public int calculateNSide(double pixsize) {
      double arcsec2rad=Math.PI/(180.*60.*60.);
      double nsd = Math.sqrt(4*Math.PI/12.)/(arcsec2rad*pixsize);
      int order_req=Math.max(0,Math.min(29,1+(int)CDSHealpix.log2((long)(nsd))));
      return 1<<order_req;
  }
   
   /** Demande d'affichage des stats (dans le TabBuild) */
   public void showStatistics() {
      long statDuree = System.currentTimeMillis()-statTime;
      context.showIndexStat(statNbFile, statBlocFile, statNbZipFile, statMemFile, statPixSize, statMaxSize,
            statMaxWidth, statMaxHeight, statMaxDepth, statMaxNbyte,statDuree);
   }


   // Génération de l'index
   private boolean build() throws Exception {
      initStat();
      String input = context.getInputPath();
      String output = context.getOutputPath();
      int order = context.getOrder();
      borderSize = context.getBorderSize();
      radius = context.circle;

      File f = new File(output);
      if (!f.exists()) f.mkdir();
      String pathDest = context.getHpxFinderPath();
      
      create(input, pathDest, order);
      
      if( statNbFile==0 ) throw new Exception("No available image found ! Notice that Multi-Extension Fits is not supported (yet) by HiPS generator"); 
      return true;
   }

   // Initialisation des statistiques
   private void initStat() {
      statNbFile = statNbZipFile = statBlocFile = 0;
      statPixSize=statMemFile = 0;
      statMaxSize = -1;
      statTime = System.currentTimeMillis();
   }

   // Mise à jour des stats
   private void updateStat(File f,int code, int width,int height,int depth,int nbyte,int deltaBlocFile) {
      statNbFile++;
      statBlocFile += deltaBlocFile;
      if( (code & Fits.GZIP) !=0 ) statNbZipFile++;
      long size = f.length();
      statPixSize += width*height;
      statMemFile += size;
      if( statMaxSize<size ) {
         statMaxSize=size;
         statMaxWidth = width;
         statMaxHeight = height;
         statMaxDepth = depth;
         statMaxNbyte = nbyte;
      }
   }

   // Création si nécessaire du fichier passé en paramètre et ouverture en écriture
   private FileOutputStream openFile(String filename) throws Exception {
      File f = new File( filename/*.replaceAll(FS+FS, FS)*/ );
      if( !f.exists() ) {
         cds.tools.Util.createPath(filename);
         return new FileOutputStream(f);
      }
      return new FileOutputStream(f, true);
   }

   // Insertion d'un nouveau fichier d'origine dans la tuile d'index repérée par out
   private void createAFile(FileOutputStream out, String filename, Coord center, String stc, String fitsVal)
   throws IOException {
      int o1 = filename.lastIndexOf('/');
      int o1b = filename.lastIndexOf('\\');
      if( o1b>o1 ) o1=o1b;
      int o2 = filename.indexOf('.',o1);
      if( o2==-1 ) o2 = filename.length();
      String name = filename.substring(o1+1,o2);
      if( fitsVal==null ) fitsVal="";
      
      DataOutputStream dataoutputstream = null;
      try {
         dataoutputstream = new DataOutputStream(out);
         dataoutputstream.writeBytes(
               "{ \"name\": \""+name+"\", \"path\": \""+filename+"\", " +
                 "\"ra\": \""+center.al+"\", \"dec\": \""+center.del+"\", " +
                 "\"stc\": \""+stc+"\""+fitsVal+" }\n");
         dataoutputstream.flush();
      } finally { if( dataoutputstream!=null ) dataoutputstream.close(); }
   }

   // Pour chaque fichiers FITS, cherche la liste des losanges couvrant la
   // zone. Créé (ou complète) un fichier texte "d'index" contenant le chemin vers
   // les fichiers FITS
   private void create(String pathSource, String pathDest, int order) throws Exception {
      
      // pour chaque fichier dans le sous répertoire
      File main = new File(pathSource);

      ArrayList<File> dir = new ArrayList<File>();
      File[] list = main.listFiles();
      if (list == null) return;
      
      int i=0;
      context.setProgress(0,list.length-1);
      for( File file : list ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         context.setProgress(i++);
         if( file.isDirectory() ) { dir.add(file); continue; }
         currentfile = file.getPath();

         Fits fitsfile = new Fits();
         boolean flagDefaultHDU = hdu==null;
         boolean flagAllHDU = hdu!=null && hdu.length>0 && hdu[0]==-1;
         int cellSize = Constante.FITSCELLSIZE;
         int firstDepth=0;

         // Multi Extension ou non ?
         for( int j=0; flagAllHDU || flagDefaultHDU ||  j<hdu.length; j++ ) {
            int ext = flagDefaultHDU ? 0 : flagAllHDU ? j : hdu[j];
            
            // L'image sera mosaiquée en cellSize x cellSize pour éviter de
            // saturer la mémoire par la suite
            try {
               int code = fitsfile.loadHeaderFITS(currentfile+ (ext==0?"":"["+ext+"]"));
               if( flagAllHDU && (code & Fits.HDU0SKIP) != 0 ) continue;
               
              // S'agit-il d'une image calibrée ?
               if( fitsfile.calib==null ) continue;
               
               if( firstDepth==0 ) firstDepth=fitsfile.depth;
               else if( fitsfile.depth!=firstDepth ) continue;
               
               Aladin.trace(4,"HiPS indexing "+currentfile+ (ext==0?"":"["+ext+"]..."));

               try {

                  // Test sur l'image entière
                  if( !partitioning /* || fitsfile.width*fitsfile.height<=4*Constante.FITSCELLSIZE*Constante.FITSCELLSIZE */ ) {
                     updateStat(file, code, fitsfile.width, fitsfile.height, fitsfile.depth, fitsfile.bitpix==0 ? 4 : Math.abs(fitsfile.bitpix) / 8, 0);
                     testAndInsert(fitsfile, pathDest, currentfile, null, order);

                     // Découpage en blocs
                  } else {   
                     //                     context.info("Scanning by cells "+cellSize+"x"+cellSize+"...");
                     int width = fitsfile.width - borderSize[3];
                     int height = fitsfile.height - borderSize[2];

                     updateStat(file, code, width, height, fitsfile.depth, fitsfile.bitpix==0 ? 4 : Math.abs(fitsfile.bitpix) / 8, 1);

                     for( int x=borderSize[1]; x<width; x+=cellSize ) {

                        for( int y=borderSize[0]; y<height; y+=cellSize ) {
                           fitsfile.widthCell = x + cellSize > width ? width - x : cellSize;
                           fitsfile.heightCell = y + cellSize > height ? height - y : cellSize;
                           fitsfile.depthCell = fitsfile.depth = 1;
                           fitsfile.xCell=x;
                           fitsfile.yCell=y;
                           fitsfile.zCell=0;
                           fitsfile.ext = ext;
                           String currentCell = fitsfile.getCellSuffix();
                           testAndInsert(fitsfile, pathDest, currentfile, currentCell, order);
                        }
                     }
                  }
               } catch (Exception e) {
                  if( Aladin.levelTrace>=3 ) e.printStackTrace();
                  break;
               }
            }  catch (Exception e) {
               Aladin.trace(3,e.getMessage() + " " + currentfile);
               break;
            }
            if( flagDefaultHDU ) break;
         }
      }

      list=null;
      if( dir.size()>0 ) {
         for( File f1 : dir ) {
            if( !f1.isDirectory() ) continue;
            currentfile = f1.getPath();
//            System.out.println("Look into dir " + currentfile);
            try {
               create(currentfile, pathDest, order);
            } catch( Exception e ) {
               Aladin.trace(3,e.getMessage() + " " + currentfile);
               continue;
            }
         }
      }
   }

   private void testAndInsert(Fits fitsfile, String pathDest, String currentFile, 
         String suffix, int order) throws Exception {
      String hpxname;
      FileOutputStream out;
      Coord center = new Coord();
      String fitsVal=null;
      
      try {
         // Recherche les 4 coins de l'image (cellule)
         Calib c = fitsfile.getCalib();
         ArrayList<double[]> cooList = new ArrayList<double[]>(4);
         Coord coo = new Coord();
         StringBuffer stc = new StringBuffer("POLYGON J2000");
         boolean hasCell = fitsfile.hasCell();
         for( int i=0; i<4; i++ ) {
            coo.x = (i==0 || i==3 ? fitsfile.xCell : fitsfile.xCell +fitsfile.widthCell);
            coo.y = (i<2 ? fitsfile.yCell : fitsfile.yCell+fitsfile.heightCell);
            if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 ) 
               coo.y = fitsfile.height - coo.y -1;
            c.GetCoord(coo);
            cooList.add( context.ICRS2galIfRequired(coo.al, coo.del) );
            
            // S'il s'agit d'une cellule, il faut également calculé le STC pour l'observation complète
            if( hasCell ) {
               coo.x = (i==0 || i==3 ? 0 : fitsfile.width);
               coo.y = (i<2 ? 0 : fitsfile.height);
               if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 ) 
                  coo.y = fitsfile.height - coo.y -1;
               c.GetCoord(coo);
            }
            stc.append(" "+coo.al+" "+coo.del);
            
            // On calcul également les coordonnées du centre de l'image
            center.x = fitsfile.width/2;
            center.y = fitsfile.height/2;
            if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 ) 
               center.y = fitsfile.height - center.y -1;
            c.GetCoord(center);
            
            // Faut-il récupérer des infos dans l'entête fits, ou dans la première HDU
            if( context.fitsKeys!=null ) {
               StringBuilder res=null; 
               for( String key : context.fitsKeys ) {
                  String val;
                  if( (val=fitsfile.headerFits.getStringFromHeader(key))==null ) {
                     if( fitsfile.headerFits0==null || fitsfile.headerFits0!=null 
                           && (val=fitsfile.headerFits0.getStringFromHeader(key))==null ) continue;
                  }
                  if( res==null ) res = new StringBuilder();
                  res.append(", \""+key+"\": \""+val.replace("\"","\\\"")+"\"");
               }
               if( res!=null ) fitsVal=res.toString();
            }
            
         }
         
         long [] npixs = CDSHealpix.query_polygon(CDSHealpix.pow2(order), cooList);

         // pour chacun des losanges concernés
         for (int i = 0; i < npixs.length; i++) {
            long npix = npixs[i];

            // vérifie la validité du losange trouvé
            if (!isInImage(fitsfile, Util.getCorners(order, npix)))  continue;

            hpxname = cds.tools.Util.concatDir(pathDest,Util.getFilePath("", order,npix));
            out = openFile(hpxname);

            // ajoute le chemin du fichier Source FITS, 
            // suivi éventuellement de la définition de la cellule en question
            // (mode mosaic), void du HDU particulier
            String filename = currentFile + (suffix == null ? "" : suffix);
            
            createAFile(out, filename, center, stc.toString(), fitsVal);
            out.close();
         }
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }

   }

   private boolean isInImage(Fits f, Coord[] corners) {
      int signeX = 0;
      int signeY = 0;
      try {
         int marge = 2;
         for (int i = 0; i < corners.length; i++) {
            Coord coo = corners[i];
            if (context.getFrame() != Localisation.ICRS) {
               double[] radec = context.gal2ICRSIfRequired(coo.al, coo.del);
               coo.al = radec[0];
               coo.del = radec[1];
            }
            f.getCalib().GetXY(coo);
            if (Double.isNaN(coo.x)) continue;
            coo.y = f.height - coo.y -1;
            int width = f.widthCell+marge;
            int height = f.heightCell+marge;
            if (coo.x >= f.xCell - marge && coo.x < f.xCell + width
                  && coo.y >= f.yCell - marge && coo.y < f.yCell + height) {
               return true;
            }
            // tous d'un coté => x/y tous du meme signe
            signeX += (coo.x >= f.xCell + width) ? 1 : (coo.x < f.xCell - marge) ? -1 : 0;
            signeY += (coo.y >= f.yCell + height) ? 1 : (coo.y < f.yCell - marge) ? -1 : 0;

         }
      } catch (Exception e) {
         return false;
      }

      if (Math.abs(signeX) == Math.abs(corners.length) || Math.abs(signeY) == Math.abs(corners.length)) {
         return false;
      }
      return true;
   }
}

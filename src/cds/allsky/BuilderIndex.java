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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/** Permet la génération de l'index HEALPix
 * Rq : le MOC de l'index sera également généré à l'issue de la génération de l'index
 * @author Standard Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderIndex extends Builder {

   private int [] borderSize= {0,0,0,0};
   private String initpath = null;
   private String currentfile = null;

   // Pour les stat
   private int statNbFile;                 // Nombre de fichiers sources
   private int statNbZipFile;              // Nombre de fichiers sources gzippés
   private long statMemFile;               // Taille totale des fichiers sources (en octets)
   private long statMaxSize;               // taille du plus gros fichier trouvé
   private int statMaxWidth, statMaxHeight, statMaxNbyte; // info sur le plus gros fichier trouvé

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
            long nside = healpix.core.HealpixIndex.calculateNSide(file.getCalib().GetResol()[0] * 3600.);
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
      }
   }
   
   /** Demande d'affichage des stats (dans le TabBuild) */
   public void showStatistics() {
      context.showIndexStat(statNbFile, statNbZipFile, statMemFile, statMaxSize,
            statMaxWidth, statMaxHeight, statMaxNbyte);
   }


   // Génération de l'index
   private boolean build() throws Exception {
      initStat();
      String input = context.getInputPath();
      String output = context.getOutputPath();
      int order = context.getOrder();
      borderSize = context.getBorderSize();

      File f = new File(output);
      if (!f.exists()) f.mkdir();
      String pathDest = context.getHpxFinderPath();
      
      create(input, pathDest, order);
      return true;
   }

   // Initialisation des statistiques
   private void initStat() {
      statNbFile = statNbZipFile = 0;
      statMemFile = 0;
      statMaxSize = -1;
   }

   // Mise à jour des stats
   private void updateStat(File f,int code, int width,int height,int nbyte) {
      statNbFile++;
      if( (code & Fits.GZIP) !=0 ) statNbZipFile++;
      long size = f.length();
      statMemFile += size;
      if( statMaxSize<size ) {
         statMaxSize=size;
         statMaxWidth = width;
         statMaxHeight = height;
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
   private void createAFile(FileOutputStream out, String filename, String stc)
   throws IOException {
      int o1 = filename.lastIndexOf('/');
      int o1b = filename.lastIndexOf('\\');
      if( o1b>o1 ) o1=o1b;
      int o2 = filename.indexOf('.',o1);
      if( o2==-1 ) o2 = filename.length();
      String name = filename.substring(o1+1,o2);
      
      DataOutputStream dataoutputstream = new DataOutputStream(out);
      dataoutputstream.writeBytes("{ \"name\": \""+name+"\", \"path\": \""+filename+"\", \"stc\": \""+stc+"\" }\n");
      dataoutputstream.flush();
      dataoutputstream.close();
   }

   // Pour chaque fichiers FITS, cherche la liste des losanges couvrant la
   // zone. Créé (ou complète) un fichier texte "d'index" contenant le chemin vers
   // les fichiers FITS
   private void create(String pathSource, String pathDest, int order) throws Exception {

      // pour chaque fichier dans le sous répertoire
      File main = new File(pathSource);

      String[] list = main.list();
      // trie la liste pour garantir la reprise au bon endroit 
      Arrays.sort(list);
      if (list == null) return;

      context.setProgress(0,list.length-1);
      for (int f = 0 ; f < list.length ; f++) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");

         context.setProgress(f);
         currentfile = pathSource+FS+list[f];
         File file = new File(currentfile); 

         if (file.isDirectory() && !list[f].equals(Constante.SURVEY)) {
//            System.out.println("Look into dir " + currentfile);
            create(currentfile, pathDest, order);
         } else {
            // en cas de reprise, saute jusqu'au dernier fichier utilisé
            if (initpath != null) { 
               if (initpath.equals(currentfile))  initpath=null;
               else continue;
            }

            Fits fitsfile = new Fits();
            int cellSize = Constante.FITSCELLSIZE; // permet à un Thread de travailler au max avec 500Mo pour 6
                                                   // recouvrements en 32 bits

            // L'image sera mosaiquée en cellSize x cellSize pour éviter de
            // saturer la mémoire par la suite
            try {
               int code = fitsfile.loadHeaderFITS(currentfile);
               // TODO MEF
               if ((code | Fits.XFITS)!=0) {
            	   
               }

               try {
                  
                  // Test sur l'image entière (pas possible autrement)
                  if( !Fits.INCELLS && currentfile.endsWith(".hhh") ) {
                     updateStat(file, code, fitsfile.width, fitsfile.height, fitsfile.bitpix==0 ? 4 : Math.abs(fitsfile.bitpix) / 8);
                     testAndInsert(fitsfile, pathDest, currentfile, null, order);
                     
                  // Découpage en petits carrés
                  } else {   
//                     context.info("Scanning by cells "+cellSize+"x"+cellSize+"...");
                        int width = fitsfile.width - borderSize[3];
                        int height = fitsfile.height - borderSize[2];

                        updateStat(file, code, width, height, fitsfile.bitpix==0 ? 4 : Math.abs(fitsfile.bitpix) / 8);
                        
                        for( int x=borderSize[1]; x<width; x+=cellSize ) {
                          
                           for( int y=borderSize[0]; y<height; y+=cellSize ) {
                              fitsfile.widthCell = x + cellSize > width ? width - x : cellSize;
                              fitsfile.heightCell = y + cellSize > height ? height - y : cellSize;
                              fitsfile.xCell=x;
                              fitsfile.yCell=y;
                              String currentCell = fitsfile.getCellSuffix();
                              testAndInsert(fitsfile, pathDest, currentfile, currentCell, order);
                           }
                        }
                     }
               } catch (Exception e) {
                  if( Aladin.levelTrace>=3 ) e.printStackTrace();
                  return;
               }
            }  catch (Exception e) {
               Aladin.trace(3,e.getMessage() + " " + currentfile);
               continue;
            }
         }
      }
   }

   private void testAndInsert(Fits fitsfile, String pathDest, String currentFile, String currentCell, int order) throws Exception {
      String hpxname;
      FileOutputStream out;
      
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
         }
         
         long [] npixs = CDSHealpix.query_polygon(CDSHealpix.pow2(order), cooList);

         // pour chacun des losanges concernés
         for (int i = 0; i < npixs.length; i++) {
            long npix = npixs[i];

            // vérifie la validité du losange trouvé
            if (!isInImage(fitsfile, Util.getCorners(order, npix)))  continue;

            hpxname = cds.tools.Util.concatDir(pathDest,Util.getFilePath("", order,npix));
            cds.tools.Util.createPath(hpxname);
            out = openFile(hpxname);

            // ajoute le chemin du fichier Source FITS, 
            // suivi éventuellement de la définition de la cellule en question
            // (mode mosaic)
            String filename = currentFile + (currentCell == null ? "" : currentCell);
            
            createAFile(out, filename, stc.toString());
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

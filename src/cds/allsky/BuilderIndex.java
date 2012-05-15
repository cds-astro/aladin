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

package cds.allsky;

import static cds.tools.Util.FS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

public class BuilderIndex implements Progressive {

   private double progress = 0;
   private int [] borderSize= {0,0,0,0};
   private String initpath = null;
   private String currentfile = null;
   private String pausepath = null;
   private String currentpath = "";

   // Pour les stat
   private int statNbFile;                 // Nombre de fichiers sources
   private int statNbZipFile;              // Nombre de fichiers sources gzippés
   private long statMemFile;               // Taille totale des fichiers sources (en octets)
   private long statMaxSize;               // taille du plus gros fichier trouvé
   private int statMaxWidth, statMaxHeight, statMaxNbyte; // info sur le plus gros fichier trouvé
   private long statLastShowTime = 0L; // Date de la dernière mise à jour du panneau d'affichage

   boolean stopped = false;

   private Context context;

   public BuilderIndex() { }

   public BuilderIndex(Context context) {
      this.context = context;
   }

   // Suppression des statistiques
   private void resetStat() {
      statNbFile = -1;
   }

   // Initialisation des statistiques
   private void initStat() {
      statNbFile = statNbZipFile = 0;
      statMemFile = 0;
      statMaxSize = -1;
      borderSize = context.getBorderSize();
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
      long t = System.currentTimeMillis();
      if (t - statLastShowTime < 1000) return;
      statLastShowTime=t;
      showStat();
   }

   // Demande d'affichage des stats (dans le TabBuild)
   private void showStat() {
      context.showIndexStat(statNbFile, statNbZipFile, statMemFile, statMaxSize,
            statMaxWidth, statMaxHeight, statMaxNbyte);
   }

   public boolean build() {
	   return build(context.getInputPath(),context.getOutputPath(),context.getOrder());
   }
   protected boolean build(String input, String output, int order) {
      initStat();

      File f = new File(output);
      if (!f.exists()) f.mkdir();
      String pathDest = context.getHpxFinderPath();
      stopped = false;

      progress = 0;
      f = new File(pathDest+FS+"Norder"+order);
      pausepath = pathDest+FS+"Norder"+order+FS+"pause";
      if (f.exists()) {
         File fpause = new File(pausepath);
         if (fpause.exists()) {
            BufferedReader r;
            try {
               r = new BufferedReader(new InputStreamReader( (new FileInputStream(fpause))));
               initpath = r.readLine();

            } catch (FileNotFoundException e) {
               e.printStackTrace();
            } catch (IOException e) {
               e.printStackTrace();
            }
         } else {
            progress=100;
            return false;
         }
      }
      create(input, pathDest, order);

      // s'il ya eu une interruption -> sortie rapide
      if (stopped) {
         resetStat();
         showStat();
         return false;
      } 

      // On en profite pour créer le Moc associé à l'index
      buildMoc();
      
      progress=100;
      File fpause = new File(pausepath);
      fpause.delete();
      showStat();
      return true;
   }
   
   protected void buildMoc() {
      BuilderMoc builderMoc = new BuilderMoc();
      builderMoc.createMoc(context.getHpxFinderPath());
      context.setNbCells( builderMoc.getUsedArea()) ;
   }
   
   protected void loadMoc() {
      try {
         HealpixMoc moc = new HealpixMoc();
         moc.read(context.getHpxFinderPath()+Util.FS+BuilderMoc.MOCNAME);
         context.setNbCells( moc.getUsedArea()) ;
      } catch( Exception e) { e.printStackTrace(); }
   }

   /**
    * Création si nécessaire du fichier passé en paramètre et ouverture en
    * écriture
    * 
    * @throws FileNotFoundException 
    */
   public static FileOutputStream openFile(String filename) throws Exception {
      File f = new File( filename/*.replaceAll(FS+FS, FS)*/ );
      if( !f.exists() ) {
         cds.tools.Util.createPath(filename);
         return new FileOutputStream(f);
      }
      return new FileOutputStream(f, true);
   }

   /**
    * Writes a String to a local file
    * 
    * @param outfile
    *            the file to write to
    * @param content
    *            the contents of the file
    * @exception IOException 
    */
//   private static void createAFile(FileOutputStream out, String content)
//   throws IOException {
//      DataOutputStream dataoutputstream = new DataOutputStream(out);
//      dataoutputstream.writeBytes(content);
//      dataoutputstream.flush();
//      dataoutputstream.close();
//   }

   private static void createAFile(FileOutputStream out, String filename, String stc)
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


   /**
    * Pour chaque fichiers FITS, cherche la liste des losanges couvrant la
    * zone. Créé (ou complète) un fichier HPX texte contenant le chemin vers
    * les fichiers FITS
    */
   public void create(String pathSource, String pathDest, int order) {

      // pour chaque fichier dans le sous répertoire
      File main = new File(pathSource);

      String[] list = main.list();
      currentpath = pathSource;
      // trie la liste pour garantir la reprise au bon endroit 
      Arrays.sort(list);
      if (list == null) return;

      progress=0;
      for (int f = 0 ; f < list.length && !stopped; f++) {
         progress = f*100./(list.length-1);

         currentfile = pathSource+FS+list[f];

         File file = new File(currentfile); 

         if (file.isDirectory() && !list[f].equals(Constante.SURVEY)) {
//            System.out.println("Look into dir " + currentfile);
            create(currentfile, pathDest, order);
            currentpath = pathSource;
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

               try {
                  
                  // Test sur l'image entière (pas possible autrement)
                  if( currentfile.endsWith(".hhh") ) {
                     updateStat(file, code, fitsfile.width, fitsfile.height, Math.abs(fitsfile.bitpix) / 8);
                     testAndInsert(fitsfile, pathDest, currentfile, null, order);
                     
                  // Découpage en petits carrés
                  } else {   
                        int width = fitsfile.width - borderSize[3];
                        int height = fitsfile.height - borderSize[2];

                        updateStat(file, code, width, height, Math.abs(fitsfile.bitpix) / 8);
                        
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

      if (!stopped) progress = 100;
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
            coo.y = fitsfile.height - coo.y -1;
            c.GetCoord(coo);
            cooList.add( context.ICRS2galIfRequired(coo.al, coo.del) );
            
            // S'il s'agit d'une cellule, il faut également calculé le STC pour l'observation complète
            if( hasCell ) {
               coo.x = (i==0 || i==3 ? 0 : fitsfile.width);
               coo.y = (i<2 ? 0 : fitsfile.height);
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
            if (!isInImage(fitsfile, Util.getCorners(order, npix))) continue;

            hpxname = cds.tools.Util.concatDir(pathDest,Util.getFilePath("", order,npix));
            cds.tools.Util.createPath(hpxname);
            out = openFile(hpxname);

            // ajoute le chemin du fichier Source FITS, 
            // suivi éventuellement de la définition de la cellule en question
            // (mode mosaic)
            String filename = currentFile + (currentCell == null ? "" : currentCell);
//            createAFile(out, filename + "\n");
            
            createAFile(out, filename, stc.toString());
         }
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }

   }


//   private void testAndInsert(Fits fitsfile, String pathDest,
//         String currentFile, String currentCell, int order) throws Exception {
//      String hpxname;
//      FileOutputStream out;
//      long npix;
//      long[] npixs = null;
//
//      // transforme les coordonnées du point de ref dans le bon frame
//      Coord centerGAL;
//      double[] aldel = context.ICRS2galIfRequired(fitsfile.center.al, fitsfile.center.del);
//      centerGAL = new Coord(aldel[0], aldel[1]);
//
//      double[] inc = fitsfile.getCalib().GetResol();
//      double radius = Math.max(Math.abs(inc[0]) * fitsfile.widthCell / 2.,
//                               Math.abs(inc[1]) * fitsfile.heightCell / 2.);
//      // rayon jusqu'à l'angle, au pire * sqrt(2)
//      radius *= Math.sqrt(2.);
//
//      npixs = getNpixList(order, centerGAL, radius);
//
//      // pour chacun des losanges concernés
//      for (int i = 0; i < npixs.length; i++) {
//         npix = npixs[i];
//
//         // vérifie la validité du losange trouvé
//         if (!isInImage(fitsfile, Util.getCorners(order, npix))) continue;
//
//         // initialise les chemins
//         if (!pathDest.endsWith(FS)) {
//            pathDest = pathDest + FS;
//         }
//         hpxname = pathDest+ Util.getFilePath("", order,npix);
//         cds.tools.Util.createPath(hpxname);
//         out = openFile(hpxname);
//
//         // ajoute le chemin du fichier Source FITS, 
//         // suivi éventuellement de la définition de la cellule en question
//         // (mode mosaic)
//         createAFile(out, currentFile + (currentCell == null ? "" : currentCell) + "\n");
//      }
//   }

   public String getCurrentpath() {
      return currentpath;
   }
   
   /**
    * Récupère la liste des numéros de pixels healpix dans un cercle
    * 
    * @param order
    * @param center
    *            centre de la recherche en gal
    * @param radius
    *            rayon de la recherche (sera agrandi) en degrés
    * @return
    */
//   static long [] getNpixList(int order, Coord center, double radius) {
//      long nside = CDSHealpix.pow2(order);
//      try {
//         // augmente le rayon de la taille d'un demi pixel en plus de
//         // l'option inclusive => +1 pixel
//         //			long[] npix = CDSHealpix.query_disc(nside,center.al,center.del,
//         //					Math.toRadians(radius)+Math.PI/(4.*nside),true);
//
//         // --- calcule la taille réel de la plus grande diagonale du pixel
//         // récupère le pixel central
//         // double[] thetaphi = Util.RaDecToPolar(new
//         // double[]{center.al,center.del});
//         double[] thetaphi = CDSHealpix.radecToPolar(new double[] {
//               center.al, center.del });
//         long n = CDSHealpix.ang2pix_nest(nside, thetaphi[0], thetaphi[1]);
//         // calcule ses 4 coins et son centre
//         Coord[] corners = Util.getCorners(order,n);
//         double[] c = CDSHealpix.pix2ang_nest(nside, n);
//         //			c = Util.PolarToRaDec(c);
//         c = CDSHealpix.polarToRadec(c);
//         Coord c1 = new Coord(c[0],c[1]);
//         // cherche la plus grande distance entre le centre et chaque coin
//         double dist = 0;
//         for (int i = 0 ; i < 4 ; i++)
//            dist = Math.max(dist, Coord.getDist(c1, corners[i]));
//
//         long[] npix = CDSHealpix.query_disc(nside, center.al, center.del,
//               Math.toRadians(radius + dist), true);
//
//         return npix;
//      } catch (Exception e1) {
//         // TODO Auto-generated catch block
//         e1.printStackTrace();
//         return null;
//      } 
//   }

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

      if (Math.abs(signeX) == Math.abs(corners.length)
            || Math.abs(signeY) == Math.abs(corners.length))
         return false;

      return true;
   }


   public int getProgress() {
      return (int) progress;
   }

   public static int getNbNpix(String output, int order) {
      return Util.computeNFiles(new File(output + FS + Constante.HPX_FINDER
            +FS +"Norder"+order ));
   }

   public void stop() {
      stopped = true;
      if (pausepath == null)
         return;

      // on enregistre l'état
      File fpause = new File(pausepath);
      BufferedWriter r;
      try {
         r = new BufferedWriter(new OutputStreamWriter(
               (new FileOutputStream(fpause))));
         r.write(currentfile);
         r.flush();
         r.close();

      } catch (FileNotFoundException e) {
         e.printStackTrace();
         stopped = false;
      } catch (IOException e) {
         e.printStackTrace();
         stopped = false;
      }
   }


   /*
    * public static void main(String[] args) { Calib c = new Calib();
    * InitLocalAccess init = new InitLocalAccess();
    * 
    * long t = System.currentTimeMillis(); for (int i = 0 ; i < 10000000 ; i++)
    * { c.GalacticToRaDec(i%360,(i%360)-100, init.cooeq, init.framegal); }
    * System.out.println((System.currentTimeMillis()-t)+"ms"); // 4700 ms avec
    * la déclaration en externe de la méthode / 8400 ms en interne }
    */
}

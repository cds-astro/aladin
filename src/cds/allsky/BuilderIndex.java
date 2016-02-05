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


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/** Permet la g�n�ration de l'index HEALPix
 * Rq : le MOC de l'index sera �galement g�n�r� � l'issue de la g�n�ration de l'index
 * @author Standard Ana�s Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderIndex extends Builder {

   private int [] borderSize= {0,0,0,0};
   private String currentfile = null;
   private boolean partitioning;
   private double maxRatio;
   private int [] hdu = null;
   private HealpixMoc area;                // r�gion de travail
   private boolean flagAppend;             // true => inutile de v�rifier les doublons

   // Pour les stat
   private int statNbFile;                 // Nombre de fichiers sources
   private int statNbZipFile;              // Nombre de fichiers sources gzipp�s
   private int statBlocFile;               // Nombre de fichiers qu'il aura fallu d�couper en blocs
   private long statMemFile;               // Taille totale des fichiers sources (en octets)
   private long statPixSize;               // Nombre total de pixels
   private long statMaxSize;               // taille du plus gros fichier trouv�
   private long statTime;                  // Date de d�but
   private int statMaxWidth, statMaxHeight, statMaxDepth, statMaxNbyte; // info sur le plus gros fichier trouv�

   boolean stopped = false;

   public BuilderIndex(Context context) { super(context); }

   public Action getAction() { return Action.INDEX; }

   public void run() throws Exception {
      build();
      report();
      context.writeHpxFinderProperties();

      BuilderMocIndex builderMocIndex = new BuilderMocIndex(context);
      builderMocIndex.run();
      context.setMocIndex( builderMocIndex.getMoc() ) ;
   }

   private ArrayList<String> badFiles=null;   // Liste des fichiers �cart�s

   /** Ajout d'un fichier � la liste des fichiers images sources �cart�s lors de l'indexation
    * suivi de la raison de son �viction. */
   private void addBadFile(String file, String error) {
      if( badFiles==null ) badFiles = new ArrayList<String>();
      badFiles.add( file+(error!=null && error.length()>0 ? " => "+error:"") );
   }

   // fournit le rapport d'indexation notamment la liste des fichiers
   // �cart�s
   private void report() throws Exception {
      if( badFiles!=null ) {
         int n = badFiles.size();
         context.warning("Index report: "+n+" input file"+(n>1?"s":"")+" not incorporated:");
         for( String s : badFiles ) context.warning("   "+s);
      }

      if( statNbFile==0 ) throw new Exception("No available image found ! => Aborted");
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
      if( partitioning ) context.info("Partitioning large original image files in blocks of "+Constante.ORIGCELLWIDTH+"x"+Constante.ORIGCELLWIDTH+" pixels");

      
      validateInput();
      validateOutput();
      validateLabel();

      // S'il existe d�j� un hpxFinder, on va v�rifier qu'on ins��re pas de doublons
      flagAppend = !context.isExistingIndexDir();
      if( !flagAppend ) context.info("Pre-existing HpxFinder index => will add new images only...");
      
      // Tests order
      int order = context.getOrder();
      if( order==-1 ) {
         String img = context.getImgEtalon();
         if( img==null ) {
            img = context.justFindImgEtalon( context.getInputPath() );
            if( img!=null ) context.info("Use this reference image => "+img);
         }
         if( img==null ) throw new Exception("No source image found in "+context.getInputPath());
         try {
            Fits file = new Fits();
            file.loadHeaderFITS(img);
            long nside = calculateNSide(file.getCalib().GetResol()[0] * 3600.);
            order = (Util.order((int) nside) - context.getTileOrder() );
            if( order<3 ) order=3;
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
            +Coord.getUnit( CDSHealpix.pixRes( CDSHealpix.pow2(context.getOrder()+context.getTileOrder()))/3600. ) );

      int w = context.getTileSide();
      context.info("TileOrder="+context.getTileOrder()+" => tileSize="+w+"x"+w+" pixels");

      // R�cup�ration de la liste des HDU
      hdu = context.getHDU();
      if( hdu==null ) context.info("MEF stategy => extension 0, otherwise 1");
      else if( hdu.length>0 && hdu[0]==-1 ) context.info("MEF stategy => all images found in the MEF");
      else {
         StringBuilder s = new StringBuilder("MEF stategy: extensions ");
         for( int i=0; i<hdu.length; i++ ) { if( i>0 )  s.append(','); s.append(hdu[i]+""); }
         context.info(s+"");
      }

      // Pour indiquer les listes des mots cl�s fits dont les valeurs vont �tre retenues
      // dans les fichiers d'index JSON afin d'�tre utiliser dans l'acc�s � la "Table des d�tails" (prog�niteurs)
      if( context.fitsKeys!=null ) {
         StringBuilder res = null;
         for( String key : context.fitsKeys ) {
            if( res==null ) res = new StringBuilder();
            else res.append(", ");
            res.append(key);
         }
         context.info("Extended metadata extraction based on FITS keys: "+res);
      }
      
      // r�cup�ration du MOC de travail
      area = context.getArea();
   }

   static public int calculateNSide(double pixsize) {
      double arcsec2rad=Math.PI/(180.*60.*60.);
      double nsd = Math.sqrt(4*Math.PI/12.)/(arcsec2rad*pixsize);
      int order_req=Math.max(0,Math.min(29,1+(int)CDSHealpix.log2((long)(nsd))));
      return 1<<order_req;
   }

   /** Demande d'affichage des stats (dans le TabBuild) */
   public void showStatistics() {
      if( statNbFile<=0 ) return;
      long statDuree = System.currentTimeMillis()-statTime;
      context.showIndexStat(statNbFile, statBlocFile, statNbZipFile, statMemFile, statPixSize, statMaxSize,
            statMaxWidth, statMaxHeight, statMaxDepth, statMaxNbyte,statDuree);
   }


   // G�n�ration de l'index
   private boolean build() throws Exception {
      initStat();
      String input = context.getInputPath();
      String output = context.getOutputPath();
      int order = context.getOrder();
      borderSize = context.getBorderSize();
      maxRatio = context.getMaxRatio();

      File f = new File(output);
      if (!f.exists()) f.mkdir();
      String pathDest = context.getHpxFinderPath();

      create(input, pathDest, order);

      return true;
   }

   // Initialisation des statistiques
   private void initStat() {
      statNbFile = statNbZipFile = statBlocFile = 0;
      statPixSize=statMemFile = 0;
      statMaxSize = -1;
      statTime = System.currentTimeMillis();
   }

   // Mise � jour des stats
   private void updateStat(File f,int code, int width,int height,int depth,int nbyte,int deltaBlocFile) {
      statNbFile++;
      statBlocFile += deltaBlocFile;
      if( (code & Fits.GZIP) !=0 ) statNbZipFile++;
      long size = f.length();
      statPixSize += width*height*depth;
      statMemFile += size;
      if( statMaxSize<size ) {
         statMaxSize=size;
         statMaxWidth = width;
         statMaxHeight = height;
         statMaxDepth = depth;
         statMaxNbyte = nbyte;
      }
   }
   
   // Cr�ation si n�cessaire du fichier pass� en param�tre et ouverture en �criture
   private RandomAccessFile openFile(String filename) throws Exception {
      File f = new File( filename );
      if( !f.exists() ) cds.tools.Util.createPath(filename);
      return new RandomAccessFile(f,"rw");
   }

   // Insertion d'un nouveau fichier d'origine dans la tuile d'index rep�r�e par out
   private void createAFile(RandomAccessFile out, String filename, Coord center, long cellMem, String stc, String fitsVal)
         throws IOException {

      try {
         // D�termination d'un nom de produit � partir du filename
         // 1.Suppression du path
         int o1 = filename.lastIndexOf('/');
         int o1b = filename.lastIndexOf('\\');
         if( o1b>o1 ) o1=o1b;

         // 2.Suppression d'une extension ?
         int o2 = filename.lastIndexOf('.');

         // 3.Suppression du suffixe [x,y-wxh] si n�cessaire
         int o3 = filename.charAt(filename.length()-1)==']' ? filename.lastIndexOf('['):-1;
         if( o3>o2 ) o2=o3;

         if( o2==-1 || o2<=o1 ) o2 = filename.length();
         String name = filename.substring(o1+1,o2);

         if( fitsVal==null ) fitsVal="";

         String line = "{ \"name\": \""+name+"\", \"path\": \""+filename+"\", " +
               "\"ra\": \""+center.al+"\", \"dec\": \""+center.del+"\", " +
               "\"cellmem\": \""+cellMem+"\", " +
               "\"stc\": \""+stc+"\""+fitsVal+" }\n";

         if( flagAppend ) out.seek( out.length() );
         else {
            String s;
            while( (s=out.readLine())!=null ) {
               if( (s+"\n").equals(line) ) {
//                  System.out.println("Bloc d�j� pr�sent dans la tuile => "+line);
                  return;       // d�j� pr�sent dans la tuile d'index
               }
            }
         }
         out.write( line.getBytes() );
      } finally { if( out!=null ) out.close(); }
   }


//   // Cr�ation si n�cessaire du fichier pass� en param�tre et ouverture en �criture
//   private FileOutputStream openFile(String filename) throws Exception {
//      File f = new File( filename/*.replaceAll(FS+FS, FS)*/ );
//      if( !f.exists() ) {
//         cds.tools.Util.createPath(filename);
//         return new FileOutputStream(f);
//      }
//      return new FileOutputStream(f, true);
//   }
//
//   // Insertion d'un nouveau fichier d'origine dans la tuile d'index rep�r�e par out
//   private void createAFile(FileOutputStream out, String filename, Coord center, String stc, String fitsVal)
//         throws IOException {
//
//      // D�termination d'un nom de produit � partir du filename
//      // 1.Suppression du path
//      int o1 = filename.lastIndexOf('/');
//      int o1b = filename.lastIndexOf('\\');
//      if( o1b>o1 ) o1=o1b;
//
//      // 2.Suppression d'une extension ?
//      int o2 = filename.lastIndexOf('.');
//
//      // 3.Suppression du suffixe [x,y-wxh] si n�cessaire
//      int o3 = filename.charAt(filename.length()-1)==']' ? filename.lastIndexOf('['):-1;
//      if( o3>o2 ) o2=o3;
//
//      if( o2==-1 || o2<=o1 ) o2 = filename.length();
//      String name = filename.substring(o1+1,o2);
//
//      if( fitsVal==null ) fitsVal="";
//
//      DataOutputStream dataoutputstream = null;
//      try {
//         dataoutputstream = new DataOutputStream(out);
//         dataoutputstream.writeBytes(
//               "{ \"name\": \""+name+"\", \"path\": \""+filename+"\", " +
//                     "\"ra\": \""+center.al+"\", \"dec\": \""+center.del+"\", " +
//                     "\"stc\": \""+stc+"\""+fitsVal+" }\n");
//         dataoutputstream.flush();
//      } finally { if( dataoutputstream!=null ) dataoutputstream.close(); }
//   }

   // Pour chaque fichiers FITS, cherche la liste des losanges couvrant la
   // zone. Cr�� (ou compl�te) un fichier texte "d'index" contenant le chemin vers
   // les fichiers FITS
   private void create(String pathSource, String pathDest, int order) throws Exception {

      // pour chaque fichier dans le sous r�pertoire
      File main = new File(pathSource);

      ArrayList<File> dir = new ArrayList<File>();
      File[] list = context.isInputFile ? new File[]{ main } : main.listFiles();
      if (list == null) return;

      int i=0;
      context.setProgress(0,list.length-1);
      for( File file : list ) {
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
         context.setProgress(i++);
         if( !context.isInputFile && file.isDirectory() ) { dir.add(file); continue; }
         currentfile = file.getPath();

         Fits fitsfile = new Fits();
         boolean flagDefaultHDU = hdu==null;
         boolean flagAllHDU = hdu!=null && hdu.length>0 && hdu[0]==-1;
         int cellSize = Constante.ORIGCELLWIDTH;
         int firstDepth=0;

         // Multi Extension ou non ?
         for( int j=0; flagAllHDU || flagDefaultHDU ||  j<hdu.length; j++ ) {
            int ext = flagDefaultHDU ? 0 : flagAllHDU ? j : hdu[j];

            // L'image sera mosaiqu�e en cellSize x cellSize pour �viter de
            // saturer la m�moire par la suite
            try {
               int code = fitsfile.loadHeaderFITS(currentfile+ (ext==0?"":"["+ext+"]"));
               if( flagAllHDU && (code & Fits.HDU0SKIP) != 0 ) continue;

               // S'agit-il d'une image calibr�e ?
               if( fitsfile.calib==null ) {
                  if( flagDefaultHDU ) break;
                  else continue;
               }

               if( firstDepth==0 ) firstDepth=fitsfile.depth;
               else if( fitsfile.depth!=firstDepth ) continue;

               Aladin.trace(4,"HiPS indexing "+currentfile+ (ext==0?"":"["+ext+"]..."));

               try {

                  // Test sur l'image enti�re
                  if( !partitioning /* || fitsfile.width*fitsfile.height<=4*Constante.FITSCELLSIZE*Constante.FITSCELLSIZE */ ) {
                     testAndInsert(fitsfile, pathDest, currentfile, null, order);
                     updateStat(file, code, fitsfile.width, fitsfile.height, fitsfile.depth, fitsfile.bitpix==0 ? 4 : Math.abs(fitsfile.bitpix) / 8, 0);

                     // D�coupage en blocs
                  } else {
                     //                     context.info("Scanning by cells "+cellSize+"x"+cellSize+"...");
                     int width = fitsfile.width - borderSize[3];
                     int height = fitsfile.height - borderSize[2];


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


                     updateStat(file, code, width, height, fitsfile.depth, fitsfile.bitpix==0 ? 4 : Math.abs(fitsfile.bitpix) / 8, 1);
                  }
               } catch( Exception e1 ) {
                  addBadFile(currentfile,e1.getMessage());
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
      RandomAccessFile out;
      Coord center = new Coord();
      String fitsVal=null;

      // Recherche les 4 coins de l'image (cellule)
      Calib c = fitsfile.getCalib();
      ArrayList<double[]> cooList = new ArrayList<double[]>(4);
      Coord coo = new Coord();
      Coord corner[] = new Coord[4];
      StringBuffer stc = new StringBuffer("POLYGON J2000");
      boolean hasCell = fitsfile.hasCell();
//      System.out.print("draw polygon");
      for( int i=0; i<4; i++ ) {
         coo.x = (i==0 || i==3 ? fitsfile.xCell : fitsfile.xCell +fitsfile.widthCell);
         coo.y = (i<2 ? fitsfile.yCell : fitsfile.yCell+fitsfile.heightCell);
         if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 )
            coo.y = fitsfile.height - coo.y -1;
         c.GetCoord(coo);
         
//         System.out.print(" "+coo.al+" "+coo.del);

         cooList.add( context.ICRS2galIfRequired(coo.al, coo.del) );
         

         // S'il s'agit d'une cellule, il faut �galement calcul� le STC pour l'observation compl�te
         if( hasCell ) {
            coo.x = (i==0 || i==3 ? 0 : fitsfile.width);
            coo.y = (i<2 ? 0 : fitsfile.height);
            if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 )
               coo.y = fitsfile.height - coo.y -1;
            c.GetCoord(coo);
         }
         stc.append(" "+coo.al+" "+coo.del);
         corner[i] = new Coord(coo.al,coo.del);
      }
//      System.out.println();
      
      // On teste le rapport largeur/longeur du pixel si n�cessaire
      if( maxRatio>0 ) {
         double w = Coord.getDist(corner[0], corner[1])/fitsfile.width;
         double h = Coord.getDist(corner[1], corner[2])/fitsfile.height;
         //         System.out.println("w="+Coord.getUnit(w)+" h="+Coord.getUnit(h));
         if( h>w*maxRatio || w>h*maxRatio ) throw 
         new Exception("Suspicious image calibration (pixel size=" +Coord.getUnit(w)+"x"+Coord.getUnit(h)+") => see -maxRatio=xx parameter");
      }

      // On calcul �galement les coordonn�es du centre de l'image
      center.x = fitsfile.width/2;
      center.y = fitsfile.height/2;
      if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 )
         center.y = fitsfile.height - center.y -1;
      c.GetCoord(center);

      // Faut-il r�cup�rer des infos dans l'ent�te fits, ou dans la premi�re HDU
      if( context.fitsKeys!=null ) {
         StringBuilder res=null;
         for( String key : context.fitsKeys ) {
            String val;
            if( (val=fitsfile.headerFits.getStringFromHeader(key))==null ) {
               if( fitsfile.headerFits0==null
                     || fitsfile.headerFits0!=null && (val=fitsfile.headerFits0.getStringFromHeader(key))==null ) continue;
            }
            if( res==null ) res = new StringBuilder();
            res.append(", \""+key+"\": \""+val.replace("\"","\\\"")+"\"");
         }
         if( res!=null ) fitsVal=res.toString();
      }
      
      // On estime la RAM n�cessaire au chargement d'une cellule du fichier
      long cellMem = fitsfile.widthCell * fitsfile.heightCell * (fitsfile.bitpix==0 ?32 : Math.abs( fitsfile.bitpix ) /8);

      long[] npixs;
      long nside = CDSHealpix.pow2(order);
      double radius = Coord.getDist(center, new Coord(cooList.get(0)[0],cooList.get(0)[1]));
      
      // Si le rayon est trop grand on pr�f�rera une requ�te pour cone pour
      // �viter le risque d'un polygone sph�rique concave
      if( radius<30 ) {
         npixs = CDSHealpix.query_polygon(nside, cooList);
      } else {
         try {
            npixs = CDSHealpix.query_disc(nside, center.al, center.del, radius);
         } catch( Exception e ) {
          throw new Exception("BuilderIndex error in CDSHealpix.query_disc() order="+order+" radius="+radius+"deg file="+fitsfile.getFilename()+" => ignored");
         }
      }
      // pour chacun des losanges concern�s
      for (int i = 0; i < npixs.length; i++) {
         long npix = npixs[i];
         
         // Suis-je dans la r�gion de travail ?
         if( area!=null && !area.isIntersecting(order, npix) ) continue;

         // v�rifie la validit� du losange trouv�
         if (!isInImage(fitsfile, Util.getCorners(order, npix)))  continue;

         hpxname = cds.tools.Util.concatDir(pathDest,Util.getFilePath("", order,npix));
         out = openFile(hpxname);

         // ajoute le chemin du fichier Source FITS,
         // suivi �ventuellement de la d�finition de la cellule en question
         // (mode mosaic), void du HDU particulier
         String filename = currentFile + (suffix == null ? "" : suffix);

         createAFile(out, filename, center, cellMem, stc.toString(), fitsVal);
         out.close();
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
            // tous d'un cot� => x/y tous du meme signe
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

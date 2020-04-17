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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.fits.Fits;
import cds.moc.SMoc;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

/** Permet la génération de l'index HEALPix
 * Rq : le MOC de l'index sera également généré à l'issue de la génération de l'index
 * @author Standard Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderIndex extends Builder {

   private int [] borderSize= {0,0,0,0};
   private String currentfile = null;
   private boolean partitioning;
   private double maxRatio;
   private int [] hdu = null;
   private SMoc area;                  // région de travail
   private boolean flagAppend;             // true => inutile de vérifier les doublons
   private int maxOverlays;                // Estimation du nombre max d'overlays 
                                           // (en décomptant le nombre d'entrées dans chaque tuile de HpxFinder=

   // Pour les stat
   private int statNbFile;                 // Nombre de fichiers sources
   private int statNbZipFile;              // Nombre de fichiers sources gzippés
   private int statBlocFile;               // Nombre de fichiers qu'il aura fallu découper en blocs
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
      report();
      context.writeHpxFinderProperties();

      BuilderMocIndex builderMocIndex = new BuilderMocIndex(context);
      builderMocIndex.run();
      context.setMocIndex( builderMocIndex.getMoc() ) ;
   }

   private ArrayList<String> badFiles=null;   // Liste des fichiers écartés

   /** Ajout d'un fichier à la liste des fichiers images sources écartés lors de l'indexation
    * suivi de la raison de son éviction. */
   private void addBadFile(String file, String error) {
      if( badFiles==null ) badFiles = new ArrayList<>();
      badFiles.add( file+(error!=null && error.length()>0 ? " => "+error:"") );
   }

   // fournit le rapport d'indexation notamment la liste des fichiers
   // écartés
   private void report() throws Exception {
      if( badFiles!=null ) {
         int n = badFiles.size();
         context.warning("Index report: "+n+" input file"+(n>1?"s":"")+" not incorporated:");
         for( String s : badFiles ) context.warning("   "+s);
      }

      if( statNbFile==0 ) throw new Exception("No available image found ! => Aborted");
      
      long val = partitioning ? Constante.ORIGCELLWIDTH * Constante.ORIGCELLWIDTH : statMaxWidth*statMaxHeight;
      val *= statMaxNbyte;
      val *= maxOverlays;
      context.info("Max original image overlay estimation"
         +(partitioning?" ("+Constante.ORIGCELLWIDTH+"x"+Constante.ORIGCELLWIDTH+" pixel blocks from original images)":"")
         +": "+maxOverlays+" => may required "+cds.tools.Util.getUnitDisk(val)+" per thread");
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
      
      // S'il existe déjà un hpxFinder, on va vérifier qu'on inséère pas de doublons
      flagAppend = !context.isExistingIndexDir();
      if( !flagAppend ) context.info("Pre-existing HpxFinder index => will add new images only...");
      
      // Tests order
      int order = context.getOrder();
      if( order==-1 || context.getFitsKeys()==null ) {
         String img = context.getImgEtalon();
         if( img==null ) {
            img = context.justFindImgEtalon( context.getInputPath() );
            if( img!=null ) context.info("Use this reference image => "+img);
         }
         if( img==null ) throw new Exception("No source image found in "+context.getInputPath());
         try {
            Fits file = new Fits( context.getHDU() );
            file.loadHeaderFITS(img);
            if( file.getCalib()==null ) throw new Exception("null calib");

            // Recherche des fitsKey à garder par défaut
            context.defaultFitsKey = context.scanDefaultFitsKey( file.headerFits );

            if( order==-1 ) {
               long nside = calculateNSide(file.getCalib().GetResol()[0] * 3600.);
               order = (Util.order((int) nside) - context.getTileOrder() );
               if( order<3 ) order=3;
            }
            context.setOrder(order);
         } catch (Exception e) {
            e.printStackTrace();
            context.warning("The reference image has no astrometrical calibration ["+img+"] => order can not be computed");
         }
      }
      if( order==-1 ) throw new Exception("Argument \"order\" is required");
      else if( order<context.getOrder() ) {
         context.warning("The provided order ["+order+"] is less than the optimal order ["+context.getOrder()+"] => OVER-sample will be applied");
      } else if( order>context.getOrder() ) {
         context.warning("The provided order ["+order+"] is greater than the optimal order ["+context.getOrder()+"] => SUB-sample will be applied");
      } else context.info("Order="+context.getOrder()+" => PixelAngularRes="
            +Coord.getUnit( CDSHealpix.pixRes( context.getOrder()+context.getTileOrder())/3600. ) );

      int w = context.getTileSide();
      context.info("TileOrder="+context.getTileOrder()+" => tileSize="+w+"x"+w+" pixels");

      // Récupération de la liste des HDU
      hdu = context.getHDU();
      if( hdu==null ) {
         if( !context.isColor() ) context.info("MEF strategy => extension 0, otherwise 1");
      } else if( hdu.length>0 && hdu[0]==-1 ) context.info("MEF strategy => all images found in the MEF");
      else {
         StringBuilder s = new StringBuilder("MEF stategy: extensions ");
         for( int i=0; i<hdu.length; i++ ) { if( i>0 )  s.append(','); s.append(hdu[i]+""); }
         context.info(s+"");
      }

      // Pour indiquer les listes des mots clés fits dont les valeurs vont être retenues
      // dans les fichiers d'index JSON afin d'être utiliser dans l'accès à la "Table des détails" (progéniteurs)
      if( context.getFitsKeys()!=null ) {
         StringBuilder res = null;
         for( String key : context.getFitsKeys() ) {
            if( res==null ) res = new StringBuilder();
            else res.append(", ");
            res.append(key);
         }
         context.info("Extended metadata extraction based on FITS keys: "+res);
      }
      
      // récupération du MOC de travail
      area = context.getArea();
      
      // info sur le coorsys frame
      context.info("HiPS coordinate frame => "+context.getFrameName());
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


   // Génération de l'index
   protected void build() throws Exception {
      initStat();
      String input = context.getInputPath();
      String output = context.getOutputPath();
      int order = context.getOrder();
      borderSize = context.getBorderSize();
      maxRatio = context.getMaxRatio();
      maxOverlays = 0;

      File f = new File(output);
      if (!f.exists()) f.mkdir();
      String pathDest = context.getHpxFinderPath();

      create(input, pathDest, order);
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
   
   // Création si nécessaire du fichier passé en paramètre et ouverture en écriture
   private RandomAccessFile openFile(String filename) throws Exception {
      File f = new File( filename );
      if( !f.exists() ) cds.tools.Util.createPath(filename);
      return new RandomAccessFile(f,"rw");
   }

   // Insertion d'un nouveau fichier d'origine dans la tuile d'index repérée par out
   private void createAFile(RandomAccessFile out, String filename, Coord center, long cellMem, 
         String stc, String fitsVal)
         throws IOException {

      try {
         // Détermination d'un nom de produit à partir du filename
         // 1.Suppression du path
         int o1 = filename.lastIndexOf('/');
         int o1b = filename.lastIndexOf('\\');
         if( o1b>o1 ) o1=o1b;

         // 2.Suppression d'une extension ?
         int o2 = filename.lastIndexOf('.');

         // 3.Suppression du suffixe [x,y-wxh] si nécessaire (dans le cas où il n'y a pas d'extension
         // car sinon ce suffixe aura déjà été supprimé)
         if( o2==-1 || o2<=o1 ) {
            int o3 = filename.charAt(filename.length()-1)==']' ? filename.lastIndexOf('['):-1;
            if( o3>o2 ) o2=o3;
         }

         if( o2==-1 || o2<=o1 ) o2 = filename.length();
         String name = filename.substring(o1+1,o2);

         if( fitsVal==null ) fitsVal="";

         String line = "{ \"name\": \""+name+"\", \"path\": \""+filename+"\", " +
               "\"ra\": \""+center.al+"\", \"dec\": \""+center.del+"\", " +
               "\"cellmem\": \""+cellMem+"\", " +
               "\"stc\": \""+stc+"\""+fitsVal+" }\n";
         
         // Estimation du nombre d'entrées
         int nbEntries = (int)( out.length()/line.length()) +1;
         if( maxOverlays<nbEntries ) maxOverlays=nbEntries;

         if( flagAppend ) out.seek( out.length() );
         else {
            if( checkIn(out,line) ) {
//             System.out.print("Ligne déjà présente dans la tuile => "+line);
             return;
               
            }
//            String s;
//            while( (s=out.readLine())!=null ) {
//               if( (s+"\n").equals(line) ) {
////                  System.out.print("Ligne déjà présente dans la tuile => "+line);
//                  return;       // déjà présent dans la tuile d'index
//               }
//            }
         }
         out.write( line.getBytes() );
         out.close();
         out=null;
      } finally { if( out!=null ) out.close(); }
   }
   
   // Recherche rapide d'une chaine dans le fichier out
   // Charge tout d'abord, puis compare ensuite.
   private boolean checkIn(RandomAccessFile out, String line) throws IOException {
      long size = out.length();
      byte [] buf = new byte[(int)size];
      out.readFully(buf);
      int start=0;
      for( int i=0; i<buf.length; i++ ) {
         if( (char)buf[i]=='\n' ) {
            if( (new String(buf,start,i-start+1)).equals(line) ) return true;
            start=i+1;
         }
      }
      return false;
   }
         
   // Pour chaque fichiers FITS, cherche la liste des losanges couvrant la
   // zone. Créé (ou complète) un fichier texte "d'index" contenant le chemin vers
   // les fichiers FITS
   private boolean create(String pathSource, String pathDest, int order) throws Exception {

      // pour chaque fichier dans le sous répertoire
      File main = new File(pathSource);

      ArrayList<File> dir = new ArrayList<>();
      File[] list = context.isInputFile ? new File[]{ main } : main.listFiles();
      if (list == null) return true;
      
      // Limitation du nombre de fichiers dans le cas d'un Pilot
      int nbPilot = context.nbPilot;

      int i=0;
      int nbFiles=0;
      
      context.setProgress(0,list.length-1);
      for( File file : list ) {
         
         // S'agit-il d'un Pilot ?
         if( nbPilot>=0 && nbFiles>nbPilot ) {
            context.warning("Test Pilot limited to "+nbPilot+" images => partial HiPS");
            return false;
         }
         
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

            // L'image sera mosaiquée en cellSize x cellSize pour éviter de
            // saturer la mémoire par la suite
            try {
               int code = fitsfile.loadHeaderFITS(currentfile+ (ext==0?"":"["+ext+"]"));
               if( flagAllHDU && (code & Fits.HDU0SKIP) != 0 ) continue;

               // S'agit-il d'une image calibrée ?
               if( fitsfile.calib==null ) {
                  if( flagDefaultHDU ) break;
                  else continue;
               }
               
              nbFiles++;

               if( firstDepth==0 ) firstDepth=fitsfile.depth;
               else if( fitsfile.depth!=firstDepth ) continue;

               Aladin.trace(4,"HiPS indexing "+currentfile+ (ext==0?"":"["+ext+"]..."));

               try {

                  // Test sur l'image entière
                  if( !partitioning ) {
                     testAndInsert(fitsfile, pathDest, currentfile, null, order);
                     updateStat(file, code, fitsfile.width, fitsfile.height, fitsfile.depth, fitsfile.bitpix==0 ? 4 : Math.abs(fitsfile.bitpix) / 8, 0);

                  // Découpage en blocs de tailles fixes sauf les derniers des lignes et des colonnes
                  // pour qu'ils ne soient pas trop petits
                  } else {
                     //                     context.info("Scanning by cells "+cellSize+"x"+cellSize+"...");
                     int width = fitsfile.width - borderSize[3];
                     int height = fitsfile.height - borderSize[2];
                     
                     for( int x=borderSize[1]; x<width; x+=fitsfile.widthCell ) {

                        for( int y=borderSize[0]; y<height; y+=fitsfile.heightCell ) {
                           fitsfile.widthCell = x + cellSize > width || width-x<2*cellSize ? 
                                 width - x : cellSize;
                           fitsfile.heightCell = y + cellSize > height || height-y<2*cellSize ? 
                                 height - y : cellSize;
                           
//                           if( fitsfile.widthCell!=cellSize || fitsfile.heightCell!=cellSize ) {
//                              System.out.println(currentfile +" "+x+","+y+" "+fitsfile.widthCell+"x"+fitsfile.heightCell);
//                           }
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
            } catch( MyInputStreamCachedException e ) {
               context.taskAbort();
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
               if( !create(currentfile, pathDest, order) ) return false;
            } catch( Exception e ) {
               Aladin.trace(3,e.getMessage() + " " + currentfile);
               continue;
            }
         }
      }
      return true;
   }
   
   private void testAndInsert(Fits fitsfile, String pathDest, String currentFile,
         String suffix, int order) throws Exception {
      String hpxname;
      RandomAccessFile out;
      Coord center = new Coord();
      String fitsVal=null;

      // Recherche les 4 coins de l'image (cellule)
      Calib c = fitsfile.getCalib();
      boolean isCAR = c.getProj()==Calib.CAR;
      ArrayList<double[]> cooList = new ArrayList<>(4);
      Coord coo = new Coord();
      Coord corner[] = new Coord[4];
      Coord cornerCell[] = new Coord[4];
      StringBuffer stc = new StringBuffer("POLYGON J2000");
      boolean hasCell = fitsfile.hasCell();
//      System.out.print("draw blue polygon");
      for( int i=0; i<4; i++ ) {
         coo.x = (i==0 || i==3 ? fitsfile.xCell : fitsfile.xCell +fitsfile.widthCell);
         coo.y = (i<2 ? fitsfile.yCell : fitsfile.yCell+fitsfile.heightCell);
         if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 )
            coo.y = fitsfile.height - coo.y; 
         c.GetCoord(coo);
         
//         System.out.print(" "+coo.al+" "+coo.del);

         cooList.add( context.ICRS2galIfRequired(coo.al, coo.del) );
//         double [] cx = CDSHealpix.normalizeRaDec(coo.al, coo.del);
//         cooList.add( context.ICRS2galIfRequired(cx[0], cx[1]) );
         
         cornerCell[i] = new Coord(coo.al,coo.del);

         // S'il s'agit d'une cellule, il faut également calculé le STC pour l'observation complète
         if( hasCell ) {
            coo.x = (i==0 || i==3 ? 0 : fitsfile.width);
            coo.y = (i<2 ? 0 : fitsfile.height);
            if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 )
               coo.y = fitsfile.height - coo.y;
            c.GetCoord(coo);
         }
         stc.append(" "+coo.al+" "+coo.del);
         corner[i] = new Coord(coo.al,coo.del);
      }
//      System.out.println();
      
      // On teste le rapport largeur/longeur du pixel si nécessaire
      // sauf s'il n'y a qu'une image ou que la projection est CAR
      if( !isCAR && maxRatio>0 && statNbFile>0 ) {
         double w = Coord.getDist(corner[0], corner[1])/fitsfile.width;
         double h = Coord.getDist(corner[1], corner[2])/fitsfile.height;
         //         System.out.println("w="+Coord.getUnit(w)+" h="+Coord.getUnit(h));
         if( h>w*maxRatio || w>h*maxRatio ) throw 
         new Exception("Suspicious image calibration (pixel size=" +Coord.getUnit(w)+"x"+Coord.getUnit(h)+") => see -maxRatio=xx parameter");
      }

      // On calcul également les coordonnées du centre de l'image
      center.x = fitsfile.width/2.;
      center.y = fitsfile.height/2.;
      if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 )
         center.y = fitsfile.height - center.y;
      c.GetCoord(center);

      // Faut-il récupérer des infos dans l'entête fits, ou dans la première HDU
      if( context.getFitsKeys()!=null ) {
         StringBuilder res=null;
         for( String key : context.getFitsKeys() ) {
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
      
      // On estime la RAM nécessaire au chargement d'une cellule du fichier
      long cellMem = fitsfile.widthCell * fitsfile.heightCell * fitsfile.depth
            * (fitsfile.bitpix==0 ?32 : Math.abs( fitsfile.bitpix ) /8);

      long[] npixs=null;
//      long nside = CDSHealpix.pow2(order);
      
      double maxRadius=0;
      for( Coord c2 : corner ) {
         double dist = Coord.getDist(center,c2 );
         if( dist>maxRadius ) maxRadius=dist;
      }
      double radius=maxRadius;
      
      // on évite les rayons trop grands pour ne pas tomber sur le cas d'un polygone concave
      if( radius<30 && !isCAR ) {
         try {
//            System.out.print("draw stc polygon");
//            for( double [] c1 : cooList ) System.out.print(" "+c1[0]+" "+c1[1]);
//            System.out.println();
//            System.out.print("draw moc "+order+"/");
            
            npixs = CDSHealpix.query_polygon(order, cooList, true);
            
//            for( long a : npixs ) System.out.print(" "+a);
//            System.out.println();
            
         } catch( Exception e ) { }
      }
         
      // Deuxième essai via un disque plutôt qu'un polygone
      if( npixs==null ) {
         try {
            
            // On calcule le centre et le rayon de la cellule
            if( hasCell || isCAR ) {
               center.x = fitsfile.xCell+fitsfile.widthCell/2.;
               center.y = fitsfile.yCell+fitsfile.heightCell/2.;
               if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && fitsfile.bitpix!=0 )
                  center.y = fitsfile.height - center.y;
               c.GetCoord(center);

               maxRadius=0;
               for( Coord c2 : cornerCell ) {
                  double dist = Coord.getDist(center,c2 );
                  if( dist>maxRadius ) maxRadius=dist;
               }
               
               if( isCAR ) {
                  Coord c2 = new Coord();
                  c2.x = fitsfile.xCell;
                  c2.y = center.y;
                  c.GetCoord(c2);
                  double dist = Coord.getDist(center,c2 );
                  if( dist>maxRadius ) maxRadius=dist;
               }
               
               radius=maxRadius;
            }

            double cent[] = context.ICRS2galIfRequired(center.al, center.del);
            npixs = CDSHealpix.query_disc(order, cent[0], cent[1], Math.toRadians(radius),true);
         } catch( Exception e ) {
          throw new Exception("BuilderIndex error in CDSHealpix.query_disc() order="+order+" center="+center+" radius="+radius+"deg file="+fitsfile.getFilename()+" => ignored");
         }
      }
      
      // pour chacun des losanges concernés
      for (int i = 0; i < npixs.length; i++) {
         long npix = npixs[i];
         
         // Suis-je dans la région de travail ?
         if( area!=null && !area.isIntersecting(order, npix) ) continue;
         
         // vérifie la validité du losange trouvé
         if( !isInImage(fitsfile, Util.getCorners(order, npix), isCAR)) continue;

         hpxname = cds.tools.Util.concatDir(pathDest,Util.getFilePath("", order,npix));
         out = openFile(hpxname);

         // ajoute le chemin du fichier Source FITS,
         // suivi éventuellement de la définition de la cellule en question
         // (mode mosaic), void du HDU particulier
         String filename = currentFile + (suffix == null ? "" : suffix);

         createAFile(out, filename, center, cellMem, stc.toString(), fitsVal);
         out.close();
      }
   }

   private boolean isInImage(Fits f, Coord[] corners,boolean isCAR) {
      int signeX = 0;
      int signeY = 0;
      try {
         int marge = 2;
         for (int i = 0; i < corners.length; i++) {
            Coord coo = corners[i];
            
            // Pour éviter de récupérer la coordonnée X de l'autre coté du ciel
            if( isCAR && coo.al==180 ) return true;
                        
            if (context.getFrame() != Localisation.ICRS) {
               double[] radec = context.gal2ICRSIfRequired(coo.al, coo.del);
               coo.al = radec[0];
               coo.del = radec[1];
            }
            
            f.getCalib().GetXY(coo);

            if (Double.isNaN(coo.x)) continue;
            coo.y = f.height - coo.y;// -1;
            int width = f.widthCell+marge;
            int height = f.heightCell+marge;
            
            if( coo.x >= f.xCell - marge/2 && coo.x <= f.xCell + width
                  && coo.y >= f.yCell - marge/2 && coo.y <= f.yCell + height ) {
               return true;
            }
            // tous d'un coté => x/y tous du meme signe
            signeX += (coo.x > f.xCell + width) ? 1 : (coo.x < f.xCell - marge/2) ? -1 : 0;
            signeY += (coo.y > f.yCell + height) ? 1 : (coo.y < f.yCell - marge/2) ? -1 : 0;

         }
      } catch (Exception e) {
         return false;
      }

      if( Math.abs(signeX) == 4 || Math.abs(signeY) == 4 ) {
         return false;
      }
      return true;
   }
}

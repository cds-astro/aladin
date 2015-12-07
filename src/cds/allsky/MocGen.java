// Copyright 2013 - UDS/CNRS
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

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.fits.Fits;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * 
 * @author Pierre Fernique [CDS]
 * @version 1.0 - may 2013
 *
 */
public class MocGen {
   Healpix hpx = new Healpix();     // Healpix stuff
   private String in=null;          // Input file | dir
   private String out=null;         // Target MOC file
   private String previous=null;    // Previous MOC file if need
   private int order=10;            // Default order
   private double blank=Double.NaN; // Alternative blank value
   private int fmt=HealpixMoc.FITS;
   public  boolean verbose=false;
   public boolean debug=false;
   private boolean strict=false;
   private boolean recursive=false;
   private boolean multWrite=false; // true for updating output MOC continously
   private int hdu[] = null;
   
   private HealpixMoc moc;      // Le MOC en cour de calcul
   private boolean ready;       // true si le MOC est pr�t
   private boolean error;       // true si le MOC n'a pas pu �tre g�n�r�
   private String serror;       // Message d'erreur
   private boolean abort;       // Demande d'interruption de construction
   private String scanningDir=null; // Le r�pertoire en cours de scanning
   private int nbImg;           // Le nombre d'images sources
   private long tStart=0;       // Date de lancement
   
   /** G�n�ration d'un MOC � partir d'une ligne de commande */
   public MocGen(String [] args) {
      super();
      execute(args);
   }
   
   /** G�n�ration d'un MOC � partir d'une liste de param�tres */
   public MocGen(String in,int order,boolean recursive,boolean strict,double blank,int [] hdu) throws Exception {
      this.strict=strict;
      this.blank=blank;
      this.in=in;
      this.recursive=recursive;
      this.order=order;
      this.hdu=hdu;
   }
   
   /** Retourne true si le MOC est pr�t - il faut tester isError() pour v�rifier
    * que le MOC est non seulement pr�t mais �galement Ok */
   public boolean isReady() { return ready; }
   
   /** Retourne true si le MOC n'a pas pu �tre g�n�r� - valable qu'apr�s un test isReady() */
   public boolean isError() { return error; }
   
   /** Retourne l'erreur suite � un �chec de g�n�ration */
   public String getError() { return serror; }
   
   /** Retourne le MOC g�n�r� */
   public HealpixMoc getMoc() throws Exception {
      if( error ) throw new Exception("MOC error => "+serror);
      if( !ready ) throw new Exception("MOC not yet ready");
      return moc;
   }
   
   /** Demande d'interruption de construction du MOC */
   public void abort() { abort=true;}
   
   /** Retourne le nom du r�pertoire en cours de Scanning */
   public String getScanningDir() { return scanningDir; }
   
   /** Retourne le nombre d'images sources */
   public int getNbImages() { return nbImg; }

   /** Lancement de la g�n�ration du MOC */
   public void start() {
      (new Thread() {
         public void run() {
            try {
               serror=null;
               error=abort=ready=false;
               nbImg=0;
               moc = new HealpixMoc();
               moc.setMocOrder(order);
               moc.setCheckConsistencyFlag(false);
               scanAndDo(moc,new File(in),order);
               moc.checkAndFix();
               ready=true;
            } catch( Exception e ) {
//               System.out.println("Moc Exception => "+e.getMessage());
               moc=null;
               serror=e.getMessage();
               error=true;
               ready=true;
            }
         }
      }).start();
   }
   
   // Ajout dans le MOC du fichier pass� en param�tre avec scan des pixels
   private boolean addInMocPixel(HealpixMoc moc,File file,int order) throws Exception {
      boolean rep=false;
      
      String currentfile = file.getPath();

      Fits f = new Fits();
      boolean flagDefaultHDU = hdu==null;
      boolean flagAllHDU = hdu!=null && hdu.length>0 && hdu[0]==-1;
      int cellSize = 2048;
      int firstDepth=0;
      
      for( int i=0; flagAllHDU || flagDefaultHDU || i<hdu.length; i++ ) {
         int ext = flagDefaultHDU ? 0 : flagAllHDU ? i : hdu[i];
         
         
         // L'image sera mosaiqu�e en cellSize x cellSize pour �viter de
         // saturer la m�moire par la suite
         try {
            int code = f.loadHeaderFITS(currentfile+ (ext==0?"":"["+ext+"]"));
            if( flagAllHDU && (code & Fits.HDU0SKIP) != 0 ) continue;
            
           // S'agit-il d'une image calibr�e ?
            if( f.calib==null ) continue;
            
            if( firstDepth==0 ) firstDepth=f.depth;
            else if( f.depth!=firstDepth ) continue;
            
            try {

               // Test sur
               //                     context.info("Scanning by cells "+cellSize+"x"+cellSize+"...");
               int width = f.width;
               int height = f.height;
               int heightSize= 1024*1024*1024/ (f.height*Math.abs(f.bitpix));
               if( heightSize<1 ) heightSize=1;
               else if( heightSize>height) heightSize=height;
//               System.out.println("Scanning by cells "+width+"x"+heightSize+"...");

//               for( int x=0; x<width; x+=cellSize ) {
                  for( int y=0; y<height; y+=heightSize) { //cellSize ) {
                     f.widthCell = width; //x + cellSize > width ? width - x : cellSize;
                     f.heightCell = y + heightSize > height ? height - y : heightSize;
                     f.depthCell = f.depth = 1;
                     f.xCell=0; //x;
                     f.yCell=y;
                     f.zCell=0;
                     f.ext = ext;
                     String currentCell = f.getCellSuffix();
                     rep |= addInMocPixel1(f, moc, currentfile, currentCell, order);
                     moc.checkAndFix();
                  }
//               }
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
      return rep;
   }
   
   private boolean addInMocPixel1(Fits f, HealpixMoc moc, String currentfile, String currentCell, int order) throws Exception {
      boolean rep=false;
      
      int localOrder=0;
      Calib c = f.getCalib();
      if( c==null ) {
         if( debug ) System.out.println(currentfile+currentCell+" ignored (no calibration) => "+currentfile);
         return rep;
      }
      
      f.loadFITS(currentfile+currentCell);
      Coord coo = new Coord();

      double pix;
      double gap=1;
      try { 
         double resImage = Math.min(c.GetResol()[0],c.GetResol()[1]);
         for( localOrder=order; CDSHealpix.pixRes( CDSHealpix.pow2(localOrder) )/3600. <= resImage; localOrder--);
         if( gap<1 ) gap=1;
      } catch( Exception e1 ) {
         e1.printStackTrace();
      }
      if( verbose ) System.out.println("Adding pixel coverage of "+currentfile+currentCell+"...");

      if( !Double.isNaN(blank) ) f.setBlank(blank);

      int n=0,n1=0;
      long oNpix=-1;  

      for( int y=f.yCell; y<f.yCell+f.heightCell; y++ ) {
         for( int x=f.xCell; x<f.xCell+f.widthCell; x++ ) {
            
            try {

               pix = f.getPixelDouble(x,y);
               if( f.isBlankPixel(pix) ) continue;
               
               coo.x = x;
               coo.y = (f.height-y-1);

               c.GetCoord(coo);
               long npix=0;
               npix = hpx.ang2pix(localOrder, coo.al, coo.del);

               // Juste pour �viter d'ins�rer 2x de suite le m�me npix
               if( npix==oNpix ) { n1++; continue; }
               moc.add(localOrder,npix);
               oNpix=npix;
               n++;
               if( n>100000 ) {
                  n=0;
                  moc.checkAndFix();
                  updateMoc(moc,out,fmt);
               }
               rep=true;
            } catch( Exception e ) {
               e.printStackTrace();
            }
         }
      }
//      System.out.println("Insert "+n+" pix, "+n1+" ignored");
      return rep;
   }

   
   // Ajout dans le MOC du losange indiqu� � la condition que le pixel central correspondant soit dans l'image
   // et ne soit pas BLANK, sinon appel r�cursif sur les 4 fils jusqu'� atteindre la r�solution de l'image
//   private boolean addInMocNpix(Fits f,HealpixMoc moc,int order, long npix) throws Exception {
//      
//      boolean rep=true;
//      Calib c = f.getCalib();
//      double resImg = Math.min(c.GetResol()[0],c.GetResol()[1]);
//      long nside = CDSHealpix.pow2(order);
//      double resOrder = CDSHealpix.pixRes( nside )/3600.;
//      if( resOrder<resImg ) {
//         return false;
//      }
//      
//      double polar[] = CDSHealpix.pix2ang_nest(nside, npix);
//      polar = CDSHealpix.polarToRadec(polar);
//      Coord coo = new Coord();
//      coo.al = polar[0]; coo.del = polar[1];
//      
//      c.GetXY(coo,false);
//      coo.y = f.height-coo.y-1;
//      if( coo.x<0 || coo.x>= f.width || coo.y<0 || coo.y>= f.height 
//            || f.isBlankPixel( f.getPixelDirectAccess((int)coo.x, (int)coo.y)) ) {
//         for( int i=0; i<4; i++ ) {
//            rep |= addInMocNpix(f,moc,order+1,npix*4+i);
//         }
//      } else {
//         System.out.println("AddInMocNpix("+order+"/"+npix+")");
//         rep=moc.add(order,npix);
//      }
//      
//      return rep;
//   }
   
   // Ajout dans le MOC de la Calib pass� en param�tre
   private boolean addInMocBox(Fits f,HealpixMoc moc,int order) throws Exception {
      boolean res=true;
      Coord coo = new Coord();
      ArrayList<double[]> cooList = new ArrayList<double[]>(10);
      Calib c = f.getCalib();
      Dimension dim = c.getImgSize();
      for( int i=0; i<4; i++ ) {
         coo.x = (i==0 || i==3 ? 0 :dim.width);
         coo.y = (i<2 ? 0 : dim.height);
         c.GetCoord(coo);
         cooList.add(new double[]{coo.al,coo.del});
      }
      long [] npixs = CDSHealpix.query_polygon(CDSHealpix.pow2(order), cooList);
      for( long npix : npixs ) moc.add(order,npix) ;
      return res;
   }
   
   // Ajout dans le MOC du fichier indiqu�
   // 1) Recherche d'une calibration astrom�trique, 2) calcul des 4 coins en ra,dec
   // 3) extraction des pixels HEALPix correspondants, 4) ajout dans le MOC
   // Rq : les fichiers qui n'ont pas de calibration sont simplement ignor�s
   private boolean addInMocBox(HealpixMoc moc, File file,int order) throws Exception {
      boolean rep=false;
      boolean flagFirstHdu = hdu==null;
      boolean flagAllHdu = hdu!=null && hdu.length>0 && hdu[0]==-1;
      
      for( int i=0; flagAllHdu || flagFirstHdu || i<hdu.length; i++ ) {
         int ext = flagFirstHdu ? 0 : flagAllHdu ? i : hdu[i];
         Fits f = new Fits();
         f.setSkipHDU0( flagFirstHdu );
         try {
            f.loadHeaderFITS(file.getAbsolutePath()+"["+ext+"]");
         }  catch( Exception e ) { return rep; }
         Calib c = f.getCalib();
         if( c==null ) continue;

         if( verbose ) System.out.println("Adding footprint of "+file.getName()+f.getCellSuffix()+"...");
         if( !Double.isNaN(blank) ) f.setBlank(blank);
         rep |= addInMocBox(f,moc,order);
         if( flagFirstHdu ) break;
     }
     return rep;
   }
   
   private boolean addInMoc(HealpixMoc moc, File file, int order,boolean strict) throws Exception {
      if( !strict ) return addInMocBox(moc,file,order);
      return addInMocPixel(moc,file,order);
   }
   
   
   // Ajout dans le MOC de tous les fichiers FITS
   // trouv�s dans le r�pertoire
   // Rq: m�thode r�cursive en parcours en largeur d'abord.
   // @return : le nombre de fichiers trait�
   private void scanAndDo(HealpixMoc moc,File rep,int order) throws Exception {
      File [] list;
      
      scanningDir=rep.getCanonicalPath();
      
      // Pour supporter le cas o� ce serait directement un fichier 
      // et non un r�pertoire
      if( rep.isFile() ) list = new File[]{rep};
      else list = rep.listFiles();
      
      for( File f : list ) {
         if( debug ) System.out.println("Scanning "+f);
         if( abort ) throw new Exception("MOC aborted");
         if( f.isFile() ) {
            if( addInMoc(moc,f,order,strict) ) nbImg++;
            if( nbImg>0 && nbImg%100==0 ) {
               moc.checkAndFix();
               updateMoc(moc,out,fmt);
            }
         }
      }
      if( recursive ) {
         for( File f : list ) {
            if( f.isDirectory() ) scanAndDo(moc,f,order);
         } 
      }
   }
   
   
   private long t1=0;
   private long t2=0;
   private int dot=0;
   private boolean needNL=false;
   
   // Met � jour si n�cessaire le Moc sur le disque (mode maj continue)
   private boolean updateMoc(HealpixMoc moc, String out, int fmt) throws Exception {
      long t = System.currentTimeMillis();
      if( (t-t1)<6000 ) return false;
      t1=t;
      if( !verbose ) {
         System.out.print(".");
         needNL=true;
         dot++;
         if( dot>10 ) { dot=0; System.out.println(); needNL=false; }
      }
      if( t2>0 && t-t2>60000 ) {
         if( needNL ) System.out.println();
         String s = nbImg+1>1 ? "s":"";
         System.out.println((nbImg+1)+" image"+s+" in progress (MOC size="+Util.getUnitDisk(moc.getMem())+" in "+Util.getTemps(t-tStart)+")...");
         needNL=false;
         t2=t;
      }

      if( !multWrite || moc.getSize()==0 ) return false;
      if( verbose ) System.out.println("Updating output MOC ["+out+"]...");
      moc.write(out, fmt);
      return true;
   }
         
   
//   private int scanStdin(HealpixMoc moc,int order) throws Exception {
//      int n=0;
//      String s;
//      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//      try {
//         StringBuffer buf = new StringBuffer();
//         while( (s=in.readLine())!=null ) {
//            if( abort ) throw new Exception("MOC aborted");
//            if( s.trim().length()==0 ) {
//               HeaderFits header = new HeaderFits(buf.toString());
//               if( addInMocBox(moc,new Calib(header),order) ) n++;
//               buf = new StringBuffer();
//            }
//            buf.append(s+"\n");
//         }
//      } finally { in.close(); }
//
//      return n;
//   }
   
   
   // parameter scanning and interpretation
   private boolean scanArgs(String args[] )  {
      for( String s : args ) {
         int x = s.indexOf("=")+1;
         if( s.startsWith("in=") ){
            in=s.substring(x);
            if( !(new File(in)).exists() ) {
               System.out.println("Unavailable directory or file ["+in+"]");
               return false;
            }
            
         } else if( s.startsWith("out=") ) {
            out=s.substring(x);
            if( (new File(out)).exists() && !(new File(out)).canWrite() ) {
               System.out.println("Unavailable output file ["+out+"]");
               return false;
            }
           
         } else if( s.startsWith("previous=") ) {
            previous=s.substring(x);
            if( !(new File(previous)).exists() ) {
               System.out.println("Unavailable previous MOC file ["+previous+"]");
               return false;
            }
           
         } else if( s.startsWith("hdu=") ) {
            try {
               hdu = Context.parseHDU(s.substring(x));
            } catch( Exception e ) {
               System.out.println("Unavailable HDU numbers ["+s.substring(x)+"]");
               return false;
            }
            
         } else if( s.equals("-v") || s.equals("-verbose") ) {
            verbose=true;
            
         } else if( s.equals("-d") ) {
            debug=verbose=true;
            
         } else if( s.equals("-r") ) {
            recursive=true;
            
         } else if( s.equals("-o") ) {
            multWrite=true;
            
            
         } else if( s.equals("-strict") ) {
            strict=true;
            
         } else if( s.equalsIgnoreCase("-pixfoot") || s.equalsIgnoreCase("-mocgen")) {
            continue;       // Juste pour pouvoir utiliser le main() de la classe en debug
            
         } else if( s.startsWith("mocfmt=") ) {
            String a=s.substring(x);
            if( a.equalsIgnoreCase("fits") ) fmt=HealpixMoc.FITS;
            else if( a.equalsIgnoreCase("json") 
                  || a.equalsIgnoreCase("ascii") ) fmt=HealpixMoc.JSON;
            else {
               System.out.println("Unkown MOC format ["+a+"]");
               return false;
            }
            
         } else if( s.startsWith("blank=") ) {
            try { blank = Double.parseDouble(s.substring(x)); }
            catch(Exception e) {
               System.out.println("Wrong blank value ["+s.substring(x)+"]");
               return false;
            }

         } else if( s.startsWith("order=") ) {
            try { order = Integer.parseInt(s.substring(x)); }
            catch(Exception e) {
               System.out.println("Wrong order value ["+s.substring(x)+"]");
               return false;
            }
         } else {
            System.out.println("Unkown parameter ["+s+"]");
            return false;
         }
      }
      if( out==null ) {
         System.out.println("Missing parameters !"); 
         return false;
      }
      
      if( strict ) System.out.println("MOC generation based on *pixel* coverage:");
      else System.out.println("MOC generation based on *image* coverage:");
      System.out.println(".in="+(in==null?"null => assuming stdin WCS headers stream (blank line separator)":in));
      if( recursive && in!=null && (new File(in)).isDirectory() ) System.out.println(".recursive directory scanning");
      System.out.println(".out="+out);
      if( previous!=null ) System.out.println(".previous="+previous);
      System.out.println(".order="+order);
      if( hdu!=null ) {
         System.out.print(".hdu=");
         if( hdu.length>0 && hdu[0]==-1 ) System.out.println("all");
         else {
            for( int i=0; i<hdu.length; i++ ) System.out.print((i==0?"":",")+hdu[i]);
            System.out.println();
         }
      }
      System.out.println(".mocfmt="+(fmt==HealpixMoc.FITS?"fits":"ascii"));
      if( strict ) System.out.println(".blank=NaN"+(Double.isNaN(blank)?"":"|"+blank));

      return true;
   }
   
   // Affichage d'un HELP
   private void usage() {
      System.out.println("Usage: java -jar Aladin.jar -mocgen ... in=inputDirOrFile out=MocFile\n" +
            "-h                 : This help\n" +
            "[-v]               : Verbose\n" +
      		"[-strict]          : Scan pixel values instead of using WCS image coverage\n" +
      		"[blank=value]      : Alternate BLANK value (-strict only)\n" +
            "[order=nn]         : MOC resolution (default 10 => 3.345')\n" +
            "[mocfmt=fits|json] : MOC output format (default Fits)\n" +
      		"[previous=moc.fits]: Previous MOC (if additions)\n" +
      		"[hdu=n1,n2-n3|all] : List of concerned Fits extensions\n" +
            "[-r]               : Recursive directory scanning\n" +
            "[-o]               : Output MOC updated continuously rather than generated at the end\n" +
            "in=fileOrDir       : Directory of images/headers collection\n" +
      		"[out=outMoc.fits]  : Output MOC file\n" +
            "[-d]               : Debug trace\n" +
     		"\n" +
      		"Generate the MOC corresponding to a collection of images or WCS headers.\n" +
      		"A MOC is a a coverage map based on HEALPix sky tesselation.\n" +
      		"\n" +
      		"The supported formats are : FITS files, MEF files, jpeg or png files\n" +
      		"(WCS header in the comment segment), .hhh file (FITS header files without pixels)\n" +
      		"and .txt simple ASCII file (FITS header as keyword = value basic ASCII lines).\n" +
      		"\n" +
      		"Version: 1.5 - based on Aladin "+Aladin.VERSION+" - Oct 2015 - P.Fernique [CDS]");
   }
   
   // Generation d'un MOC pour toute une hi�rarchie de fichiers FITS (ou JPEG/PNG avec calibration)
   // en basant le calcul du MOC sur les 4 coins des images trouv�es
   private void execute(String [] args) {
      if( (args.length>0 && (args[0].equals("-h") || args[0].equals("-help"))) 
            || !scanArgs(args)) {
         usage();
         return;
      }

      int n=0;
      try {
         serror=null;
         error=abort=ready=false;
         nbImg=0;
         moc = new HealpixMoc();
         if( previous!=null ) moc.read(previous);
         moc.setMocOrder(order);
         moc.setCheckConsistencyFlag(false);
         tStart = System.currentTimeMillis();
//         if( in!=null ) scanAndDo(moc,new File(in),order);
//         else scanStdin(moc,order);
         scanAndDo(moc,new File(in),order);
         moc.checkAndFix();
         long ms = System.currentTimeMillis()-tStart;
         if( needNL ) System.out.println();
         if( verbose ) {
            String s = nbImg>1?"s":"";
            System.out.println(nbImg+" image"+s+" added in the MOC in "+cds.tools.Util.getTemps(ms));
         }
         moc.write(out,fmt);
         System.out.println("MOC achieved in "+Util.getTemps(ms)+" => "+out);
         ready=true;
      } catch( Exception e ) {
         moc=null;
         serror=e.getMessage();
         error=true;
         e.printStackTrace();
      }
   }
   
   /** Debuging launcher */
   public static void main(String[] args) {
      new MocGen(args);
   }
}

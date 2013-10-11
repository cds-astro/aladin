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
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.fits.Fits;
import cds.fits.HeaderFits;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
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
   private boolean verbose=false;
   private boolean strict=false;
   private boolean recursive=false;
   private boolean multWrite=false; // true for updating output MOC continously
   
   // Ajout dans le MOC du fichier passé en paramètre avec scan des pixels
   protected boolean addInMocPixel(HealpixMoc moc,File file,int res) throws Exception {
      Fits f = new Fits();
      try { f.loadFITS(file.getAbsolutePath()); } 
      catch( Exception e ) { return false; }
     
      Calib c = f.getCalib();
      Coord coo = new Coord();
      
      double pix;
      double gap=1;
      double gapA=0;
      try { 
         gapA = Math.min(c.GetResol()[0],c.GetResol()[1]);
         for( order=res; CDSHealpix.pixRes( CDSHealpix.pow2(order) )/3600. <= gapA*2; order--);
      } catch( Exception e1 ) {
         e1.printStackTrace();
      }
      if( verbose ) System.out.println("Adding pixel coverage of ["+file.getName()+"]...");
      
      if( !Double.isNaN(blank) ) f.setBlank(blank);
      
      long oNpix=-1;  
      for( double y=0; y<f.height; y+=gap ) {
         for( double x=0; x<f.width; x+=gap ) {
            try {
               coo.x = x;
               coo.y = (f.height-y-1);
               
               // dans du vide - on test d'abord le buffers 8bits, et on vérifie si on tombe sur 0
               pix = f.getPixelDouble((int)x,(int)y);
               if( f.isBlankPixel(pix) ) continue;
               
               c.GetCoord(coo);
               long npix=0;
               npix = hpx.ang2pix(order, coo.al, coo.del);

               // Juste pour éviter d'insérer 2x de suite le même npix
               if( npix==oNpix ) continue;
               
               moc.add(order,npix);
               oNpix=npix;
            } catch( Exception e ) {
               e.printStackTrace();
            }
         }
      }
      return true;
   }
   
   // Ajout dans le MOC de la Calib passé en paramètre
   private boolean addInMocBox(HealpixMoc moc, Calib c,int order) throws Exception {
      Coord coo = new Coord();
      ArrayList<double[]> cooList = new ArrayList<double[]>(10);
      Dimension dim = c.getImgSize();
      
      for( int i=0; i<4; i++ ) {
         coo.x = (i==0 || i==3 ? 0 :dim.width);
         coo.y = (i<2 ? 0 : dim.height);
         c.GetCoord(coo);
         cooList.add(new double[]{coo.al,coo.del});
      }
      long [] npixs = CDSHealpix.query_polygon(CDSHealpix.pow2(order), cooList);
      for (long npix : npixs ) moc.add(order,npix);
      return true;
   }
   
   // Ajout dans le MOC du fichier indiqué
   // 1) Recherche d'une calibration astrométrique, 2) calcul des 4 coins en ra,dec
   // 3) extraction des pixels HEALPix correspondants, 4) ajout dans le MOC
   // Rq : les fichiers qui n'ont pas de calibration sont simplement ignorés
   private boolean addInMocBox(HealpixMoc moc, File file,int order) throws Exception {
      Fits f = new Fits();
      try { f.loadHeaderFITS(file.getAbsolutePath()); } 
      catch( Exception e ) { return false; }
      Calib c = f.getCalib();
      
      if( verbose ) System.out.println("Adding footprint of ["+file.getName()+"]...");
      return addInMocBox(moc,c,order);
   }
   
   private boolean addInMoc(HealpixMoc moc, File file, int order,boolean strict) throws Exception {
      if( !strict ) return addInMocBox(moc,file,order);
      return addInMocPixel(moc,file,order);
   }
   
   // Ajout dans le MOC de tous les fichiers FITS
   // trouvés dans le répertoire
   // Rq: méthode récursive en parcours en largeur d'abord.
   // @return : le nombre de fichiers traité
   private int scanAndDo(HealpixMoc moc,File rep,int order) throws Exception {
      int n=0;
      File [] list;
      
      // Pour supporter le cas où ce serait directement un fichier 
      // etnon un répertoire
      if( rep.isFile() ) list = new File[]{rep};
      else list = rep.listFiles();
      
      for( File f : list ) {
         if( f.isFile() ) {
            if( addInMoc(moc,f,order,strict) ) n++;
            if( n>0 && n%100==0 ) {
               moc.checkAndFix();
               if( multWrite ) {
                  if( verbose ) System.out.println("Updating output MOC ["+out+"]...");
                  moc.write(out, fmt);
               }
            }
         }
      }
      if( recursive ) {
         for( File f : list ) {
            if( f.isDirectory() ) n+=scanAndDo(moc,f,order);
         } 
      }
      return n;
   }
   
   // Ajout dans le MOC de tous les fichiers FITS
   // trouvés dans le répertoire
   // Rq: méthode récursive en parcours en largeur d'abord.
   // @return : le nombre de fichiers traité
   private int scanStdin(HealpixMoc moc,int order) throws Exception {
      int n=0;
      String s;
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      try {
         StringBuffer buf = new StringBuffer();
         while( (s=in.readLine())!=null ) {
            if( s.trim().length()==0 ) {
               HeaderFits header = new HeaderFits(buf.toString());
               if( addInMocBox(moc,new Calib(header),order) ) n++;
               buf = new StringBuffer();
            }
            buf.append(s+"\n");
         }
      } finally { in.close(); }

      return n;
   }
   
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
           
         } else if( s.equals("-v") || s.equals("-verbose") ) {
            verbose=true;
            
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
            else if( a.equalsIgnoreCase("obsolete") ) fmt=HealpixMoc.OBSOLETE;
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
      if( recursive && in!=null && (new File(in)).isDirectory() ) 
         System.out.println(".recursive directory scanning");
      System.out.println(".out="+out);
      if( previous!=null ) System.out.println(".previous="+previous);
      System.out.println(".order="+order);
      System.out.println(".mocfmt="+(fmt==HealpixMoc.FITS?"fits":"ascii"));
      if( strict ) System.out.println(".blank=NaN"+(Double.isNaN(blank)?"":"|"+blank));
         
      return true;
   }
   
   // Affichage d'un HELP
   private void usage() {
      System.out.println("Usage: java -jar Aladin.jar -mocgen ... [in=inputDirOrFile] out=MocFile\n" +
            "-h                 : This help\n" +
            "[-v]               : Verbose\n" +
      		"[-strict]          : Scan pixel values instead of using WCS image coverage\n" +
      		"[blank=value]      : Alternate BLANK value (-strict only)\n" +
            "[order=nn]         : MOC resolution (default 10)\n" +
            "[mocfmt=fits|json] : MOC output format (default Fits)\n" +
      		"[previous=moc.fits]: Previous MOC (if additions)\n" +
      		"[in=fileOrDir]     : Directory of images/headers collection\n" +
            "[-r]               : Recursive directory scanning\n" +
            "[-o]               : Output MOC updated continuously rather than generated at the end\n" +
      		"[out=outMoc.fits]  : Output MOC file\n" +
      		"\n" +
      		"Generate the MOC corresponding to a collection of images or WCS headers.\n" +
      		"A MOC is a a coverage map based on HEALPix sky tesselation.\n" +
      		"\n" +
      		"The supported formats are : FITS files, MEF files (only first HDU), jpeg or png files\n" +
      		"(WCS header in the comment segment), .hhh file (FITS header files without pixels)\n" +
      		"and .txt simple ASCII file (FITS header as keyword = value basic ASCII lines).\n" +
      		"\n" +
      		"Version: 1.1 - based on Aladin "+Aladin.VERSION+" - may 2013 - P.Fernique [CDS]");
   }
   
   // Generation d'un MOC pour toute une hiérarchie de fichiers FITS (ou JPEG/PNG avec calibration)
   // en basant le calcul du MOC sur les 4 coins des images trouvées
   private void excute(String [] args) {
      if( (args.length>0 && (args[0].equals("-h") || args[0].equals("-help"))) 
            || !scanArgs(args)) {
         usage();
         return;
      }

      int n=0;
      try {
         HealpixMoc moc = new HealpixMoc();
         if( previous!=null ) moc.read(previous);
         moc.setMaxLimitOrder(order);
         moc.setCheckConsistencyFlag(false);
         long t = System.currentTimeMillis();
         if( in!=null ) n = scanAndDo(moc,new File(in),order);
         else n = scanStdin(moc,order);
         moc.checkAndFix();
         long ms = System.currentTimeMillis()-t;
         if( verbose ) System.out.println(n+" files added in the MOC in "+cds.tools.Util.getTemps(ms));
         moc.write(out,fmt);
         if( verbose ) System.out.println("MOC achieved => "+out);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   /** Debuging launcher */
   public static void main(String[] args) {
      MocGen generator = new MocGen();
      generator.excute(args);
   }
}

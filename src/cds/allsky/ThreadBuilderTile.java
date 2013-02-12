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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

final public class ThreadBuilderTile {

   private Context context;

   private int bitpix;
   private boolean hasAlternateBlank;
   private double blankOrig;
   private double blank;
   private boolean flagColor;
   private double bScale;
   private double bZero;
   private String hpxFinderPath = null;
   private double[] cutOrig;
   private double[] cut;
   private int[] borderSize;
   private ArrayList<SrcFile> downFiles;

   public ThreadBuilderTile(Context context) {
      this.context = context;

      bitpix=context.getBitpix();
      flagColor = context.isColor();
      if( !flagColor ) {
         bZero = context.getBZero();
         bScale = context.getBScale();
         cutOrig=context.getCutOrig();
         cut=context.getCut();
         blankOrig=context.getBlankOrig();
         hasAlternateBlank = context.hasAlternateBlank();
         blank = context.getBlank();
      } else {
         blank=0;
      }
      borderSize = context.getBorderSize();
      hpxFinderPath = context.getHpxFinderPath();

      downFiles = new ArrayList<SrcFile>(Constante.MAXOVERLAY);

   }

   public long getMem() {
      long mem=2+5*4+4*8+2*8*4;
      for( SrcFile f : downFiles) {
         mem += f.fitsfile.getMem();
      }
      return mem;
   }


   private void rmFits(BuilderTiles bt,ArrayList<SrcFile> downFiles) {
      if( downFiles==null ) return;
      for( SrcFile f : downFiles ) {
         bt.rmFits(Thread.currentThread(),f.fitsfile);
      }
   }

   private void checkMem(long nbProgen) throws Exception {
      long rqMem = 4 * nbProgen * Constante.FITSCELLSIZE*Constante.FITSCELLSIZE*context.getNpixOrig();
      rqMem += 2*Constante.SIDE*Constante.SIDE*context.getNpix();
      if( nbProgen>Constante.MAXOVERLAY ) {
         rqMem += 2*Constante.SIDE*Constante.SIDE*8;
      }
      while( !context.cacheFits.needMem(rqMem) ) {
         try {
            context.nlwarning(Thread.currentThread().getName()+" is waiting more memory (need "+
               cds.tools.Util.getUnitDisk(rqMem)+")...");
            cds.tools.Util.pause(1000);
         } catch( Exception e ) { }
         if( context.isTaskAborting() ) throw new Exception("Task abort !");
      }
   }

   Fits buildHealpix(BuilderTiles bt, int nside_file, long npix_file, int nside) throws Exception {
      ArrayList<SrcFile> downFiles = null;
      Fits out=null;
      try {
         // initialisation de la liste des fichiers originaux pour ce losange
         downFiles = new ArrayList<SrcFile>(Constante.MAXOVERLAY);
         if (!askLocalFinder(bt,downFiles,hpxFinderPath, npix_file, Util.order(nside), blank)) {
            rmFits(bt,downFiles);
            return null;
         }

         Fits f;
         int n=downFiles.size();

         // Pas trop de progéniteurs => on peut tout faire d'un coup
         if( n<Constante.MAXOVERLAY ) {

            checkMem(n);
            for( int i=0; i<n; i++ ) {
               f = downFiles.get(i).fitsfile;
               f.addUser();
               f.reloadBitmap();
            }
            out = buildHealpix1(bt,nside_file,npix_file,nside,downFiles,0,n,null);

            for( int i=0; i<n; i++ ) {
               f = downFiles.get(i).fitsfile;
               f.rmUser();
            }


         // Trop de progéniteurs, on va travailler en plusieurs couches de peinture
         // en mémorisant le poids de chaque pixel à chaque couche
         } else {

            checkMem(Constante.MAXOVERLAY);

            // poids déjà calculés
            double [] weight = null;
            double [] fWeight = new double[Constante.SIDE*Constante.SIDE];

            for( int deb=0; deb<n; deb+=Constante.MAXOVERLAY ) {
               if( context.isTaskAborting() ) throw new Exception("Task abort !");
               int fin = deb+Constante.MAXOVERLAY;
               if( fin>=n ) fin=n;
               for( int i=deb; i<fin; i++ ) {
                  f = downFiles.get(i).fitsfile;
                  f.addUser();
                  f.reloadBitmap();
               }
               f = buildHealpix1(bt,nside_file,npix_file,nside,downFiles,deb,fin,fWeight);
               if( f!=null ) {
                  if( out==null ) {
                     out=f;
                     weight=fWeight;
                     fWeight = new double[Constante.SIDE*Constante.SIDE];
                  }
                  else out.coadd(f,weight,fWeight);
               }
               for( int i=deb; i<fin; i++ ) {
                  f = downFiles.get(i).fitsfile;
                  f.rmUser();
               }
            }

            // Juste pour vérifier les poids
//            if( out!=null ) {
//               for( int y=0; y<Constante.SIDE; y++ ) {
//                  for( int x=0; x<Constante.SIDE; x++ ) {
//                     out.setPixelDouble(x, y, weight[y*Constante.SIDE+x]);
//                  }
//               }
//            }
         }

      } catch( Exception e ) { e.printStackTrace(); }

      rmFits(bt,downFiles);
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      return out;
   }

   /**
    * Rempli le tableau de pixels correspondant au fichier (losange) Healpix
    * donné
    *
    * @param nside_file
    * @param npix_file
    * @param nside
    * @param pixels
    * @return
    */
   Fits buildHealpix1(BuilderTiles bt, int nside_file, long npix_file, int nside,
                      ArrayList<SrcFile> downFiles,int deb,int fin,double [] weight) throws Exception {
      boolean empty = true;
      long min;
      long index;
      double point[] = new double[2];
      double radec[] = new double[2];
      Coord coo = new Coord();
      SrcFile file = null;
      Fits out=null;

      try {
         // cherche les numéros de pixels Healpix dans ce losange
         min = Util.getHealpixMin(nside_file, npix_file, nside, true);

         boolean flagModifBitpix = bitpix!=context.getBitpixOrig();

         out = new Fits(Constante.SIDE, Constante.SIDE, bitpix);
         if( !flagColor ) {
            out.setBlank(blank);
            out.setBzero(bZero);
            out.setBscale(bScale);
         }

         // cherche la valeur à affecter dans chacun des pixels healpix
         double pixval[] = new double[Constante.MAXOVERLAY];   // on va éviter de passer par le total afin d'éviter un débordement
         double pixcoef[] = new double[Constante.MAXOVERLAY];
         double [] pixvalG=null,pixvalB=null;
         if( flagColor ) { pixvalG = new double[Constante.MAXOVERLAY]; pixvalB = new double[Constante.MAXOVERLAY]; }

         for (int y = 0; y < out.height; y++) {
            if( context.isTaskAborting() ) break;
            for (int x = 0; x < out.width; x++) {
               index = min + context.xy2hpx(y * out.width + x);
               // recherche les coordonnées du pixels HPX
               point = CDSHealpix.pix2ang_nest(nside, index);
               CDSHealpix.polarToRadec(point, radec);

               radec = context.gal2ICRSIfRequired(radec);
               coo.al = radec[0]; coo.del = radec[1];

               int nbPix=0;
               double totalCoef=0;
               String lastFitsFile=null;
               double lastX=-1,lastY=-1;
//               for( int i=downFiles.size()-1; i>=0 && nbPix<Constante.MAXOVERLAY; i-- ) {
               for( int i=deb; i<fin; i++ ) {
                  file = downFiles.get(i);
                  double currentBlankOrig = !hasAlternateBlank ? file.fitsfile.getBlank() : blankOrig;

                  // Même fichier qu'avant => même calibration, on s'évite un calcul ra,dec=>x,y
                  if( lastFitsFile!=null && lastFitsFile.equals(file.fitsfile.getFilename()) ) { coo.y=lastY; coo.x=lastX; }

                  // Détermination du pixel dans l'image à traiter
                  else {
                     file.calib.GetXY(coo);
                     if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && file.fitsfile.bitpix!=0 )
                        coo.y = file.fitsfile.height-coo.y -1;
                     lastY=coo.y;
                     lastX=coo.x -= 1;                             // Correction manuelle de 1 en comparaison avec les originaux
                     lastFitsFile=file.fitsfile.getFilename();
                  }

                  // Chargement du buffer si nécessaire
//                  file.fitsfile.reloadBitmap();
                  // IL FAUDRAIT ICI FAIRE DE LA PLACE SI NECESSAIRE

                  // Cas RGB
                  if( flagColor ) {
                     int pix = getBilinearPixelRGB(file.fitsfile,coo);
                     if( pix==0 ) continue;
                     pixval[nbPix] = 0xFF & (pix>>16);
                     pixvalG[nbPix] = 0xFF & (pix>>8);
                     pixvalB[nbPix] = 0xFF & pix;

                     // Cas normal
                  } else {
                     double pix = getBilinearPixel(file.fitsfile,coo,currentBlankOrig);
                     if( Double.isNaN(pix) ) continue;
                     pixval[nbPix]=pix;
                  }
                  // fading
                  totalCoef+= pixcoef[nbPix] = getCoef(file.fitsfile,coo);
                  nbPix++;
               }

               // cas RGB
               if( flagColor ) {
                  int pixelFinal=0;

                  if( nbPix!=0 ) {
                     if( totalCoef==0 )  pixelFinal = (((int)pixval[0])<<16) | (((int)pixvalG[0])<<8) | ((int)pixvalB[0]);
                     else {
                        double r=0,g=0,b=0;
                        for( int i=0; i<nbPix; i++ ) {
                           r += (pixval[i]*pixcoef[i])/totalCoef;
                           g += (pixvalG[i]*pixcoef[i])/totalCoef;
                           b += (pixvalB[i]*pixcoef[i])/totalCoef;
                        }
                        if( r>255 ) r=255;
                        if( g>255 ) g=255;
                        if( b>255 ) b=255;
                        pixelFinal = (((int)r)<<16) | (((int)g)<<8) | ((int)b);
                     }
                  }
                  if( pixelFinal!=0 ) empty=false;

                  out.setPixelRGBJPG(x, y, 0xFF000000 | pixelFinal);

                  // Cas normal
               }  else {
                  double pixelFinal=0;
                  if( nbPix==0 ) pixelFinal = Double.NaN;
                  else if( totalCoef==0 )  { empty=false; pixelFinal = pixval[0]; }
                  else {
                     empty=false;
                     for( int i=0; i<nbPix; i++ ) pixelFinal += (pixval[i]*pixcoef[i])/totalCoef;
                  }

                  // Changement de bitpix ?
                  if( flagModifBitpix ) {
                     pixelFinal = Double.isNaN(pixelFinal) ? blank
                                : pixelFinal<=cutOrig[2] ? cut[2]
                                : pixelFinal>=cutOrig[3] ? cut[3]
                                : (pixelFinal-cutOrig[2])*context.coef + cut[2];
                  } else if( Double.isNaN(pixelFinal) ) pixelFinal = blank;

                  out.setPixelDouble(x,y,pixelFinal);
               }

               // Mémorisation du poids du pixel (si nécessaire)
               if( weight!=null ) weight[y*Constante.SIDE+x]=totalCoef;
            }
         }
      } catch( Exception e ) { e.printStackTrace(); }

      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      return (!empty) ? out : null;
   }


   static private final double OVERLAY_PROPORTION = 1/6.;

   // Détermination d'un coefficent d'atténuation de la valeur du pixel en fonction de sa distance au bord
   	private double getCoef(Fits f,Coord coo) {
   	   double width  = f.width -(borderSize[1]+borderSize[3]);
   	   double height = f.height-(borderSize[0]+borderSize[2]);
   	   double mx = width *OVERLAY_PROPORTION;
   	   double my = height*OVERLAY_PROPORTION;
   	   double x = coo.x-borderSize[1];
   	   double y = coo.y-borderSize[0];
   	   double coefx=1, coefy=1;
   	   if( x<mx ) coefx =  x/mx;
   	   else if( x>width-mx ) coefx = (width-x)/mx;
          if( y<my ) coefy =  y/my;
          else if( y>height-my ) coefy = (height-y)/my;
          return Math.min(coefx,coefy);
   	}

   // Détermination d'un coefficent d'atténuation de la valeur du pixel en fonction de sa distance au centre
//   private double getCoef(Fits f,Coord coo) {
//      double cx = f.width/2;
//      double cy = f.height/2;
//      double dx = coo.x-cx;
//      double dy = coo.y-cy;
//      double d = Math.sqrt(dx*dx + dy*dy);
//      double maxd = Math.sqrt(cx*cx + cy*cy);
//      return (maxd - d)/maxd;
//   }

   private double getBilinearPixel(Fits f,Coord coo,double myBlank) {
      double x = coo.x;
      double y = coo.y;

      int x1 = (int)Math.floor(x);
      int y1 = (int)Math.floor(y);
      int x2=x1+1;
      int y2=y1+1;

      int ox1= x1;
      int oy1= y1;
      int ox2= x2;
      int oy2= y2;

    if( x2<f.xCell || y2<f.yCell ||
        x1>=f.xCell+f.widthCell || y1>=f.yCell+f.heightCell ) return Double.NaN;

      // Sur le bord, on dédouble le dernier pixel
      if( ox1==f.xCell-1 ) ox1++;
      if( oy1==f.yCell-1 ) oy1++;
      if( ox2==f.xCell+f.widthCell ) ox2--;
      if( oy2==f.yCell+f.heightCell ) oy2--;

      double a0 = f.getPixelDouble(ox1,oy1);
      double a1 = f.getPixelDouble(ox2,oy1);
      double a2 = f.getPixelDouble(ox1,oy2);
      double a3 = f.getPixelDouble(ox2,oy2);

      boolean b0 = Double.isNaN(a0) || a0==myBlank;
      boolean b1 = Double.isNaN(a1) || a1==myBlank;
      boolean b2 = Double.isNaN(a2) || a2==myBlank;
      boolean b3 = Double.isNaN(a3) || a3==myBlank;

      if( b0 && b1 && b2 && b3 ) return Double.NaN;
      if( b0 || b1 || b2 || b3 ) {
         double a = !b0 ? a0 : !b1 ? a1 : !b2 ? a2 : a3;
         if( b0 ) a0=a;
         if( b1 ) a1=a;
         if( b2 ) a2=a;
         if( b3 ) a3=a;
      }
//
//      if( isBlankPixel(a0,blank) ) return Double.NaN;
//      if( isBlankPixel(a1,blank) ) a1=a0;
//      if( isBlankPixel(a2,blank) ) a2=a0;
//      if( isBlankPixel(a3,blank) ) a3=a0;

      return bilineaire(x1,y1,x2,y2,x,y,a0,a1,a2,a3);
   }

   private int getBilinearPixelRGB(Fits f,Coord coo) {
      double x = coo.x;
      double y = coo.y;

      int x1 = (int)Math.floor(x);
      int y1 = (int)Math.floor(y);
      int x2=x1+1;
      int y2=y1+1;

      int ox1= x1;
      int oy1= y1;
      int ox2= x2;
      int oy2= y2;

    if( x2<f.xCell || y2<f.yCell ||
        x1>=f.xCell+f.widthCell || y1>=f.yCell+f.heightCell ) return 0;

      // Sur le bord, on dédouble le dernier pixel
      if( ox1==f.xCell-1 ) ox1++;
      if( oy1==f.yCell-1 ) oy1++;
      if( ox2==f.xCell+f.widthCell ) ox2--;
      if( oy2==f.yCell+f.heightCell ) oy2--;

      int b0 = f.getPixelRGBJPG(ox1,oy1);
      if( b0==0 ) return 0;     // pixel transparent (canal alpha à 0)
      int b1 = f.getPixelRGBJPG(ox2,oy1);
      int b2 = f.getPixelRGBJPG(ox1,oy2);
      int b3 = f.getPixelRGBJPG(ox2,oy2);
//      int b0 = f.getPixelRGB(ox1,oy1);
//      int b1 = f.getPixelRGB(ox2,oy1);
//      int b2 = f.getPixelRGB(ox1,oy2);
//      int b3 = f.getPixelRGB(ox2,oy2);

      int pix=0xFF;
      for( int i=16; i>=0; i-=8 ) {
         double a0 = 0xFF & (b0>>i);
         double a1 = 0xFF & (b1>>i);
         double a2 = 0xFF & (b2>>i);
         double a3 = 0xFF & (b3>>i);
         pix = (pix<<8) | (int)bilineaire(x1,y1,x2,y2,x,y,a0,a1,a2,a3);
      }
      return pix;
   }

   private double bilineaire(int x1,int y1,int x2,int y2, double x, double y, double a0, double a1, double a2, double a3 ) {
      double d0,d1,d2,d3,pA,pB;
      if( x==x1 ) { d0=1; d1=0; }
      else if( x==x2 ) { d0=0; d1=1; }
      else { d0 = 1./(x-x1); d1 = 1./(x2-x); }
      if( y==y1 ) { d2=1; d3=0; }
      else if( y==y2 ) { d2=0; d3=1; }
      else { d2 = 1./(y-y1); d3 = 1./(y2-y); }
      pA = (a0*d0+a1*d1)/(d0+d1);
      pB = (a2*d0+a3*d1)/(d0+d1);
      return (pA*d2+pB*d3)/(d2+d3);
   }

   /**
    * This method does the actual GET
    *
    * @param theUrl The URL to retrieve
    * @param filename the local file to save to
    * @exception IOException
    */
   public void get(String theUrl, String filename) throws IOException {
      try {
         URL gotoUrl = new URL(theUrl);
         InputStreamReader isr = new InputStreamReader(gotoUrl.openStream());
         BufferedReader in = new BufferedReader(isr);

         StringBuffer sb = new StringBuffer();
         String inputLine;

         // grab the contents at the URL
         while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine + "\r\n");
         }
         // write it locally
         createAFile(filename, sb.toString());
      } catch (MalformedURLException mue) {
         mue.printStackTrace();
      } catch (IOException ioe) {
         throw ioe;
      }
   }

   // creates a local file
   /**
    * Writes a String to a local file
    *
    * @param outfile the file to write to
    * @param content the contents of the file
    * @exception IOException
    */
   private static void createAFile(String outfile, String content)
   throws IOException {
      FileOutputStream fileoutputstream = new FileOutputStream(outfile);
      DataOutputStream dataoutputstream = new DataOutputStream(
            fileoutputstream);
      dataoutputstream.writeBytes(content);
      dataoutputstream.flush();
      dataoutputstream.close();
   }


   // Je fais au plus simple pour un simple test => il faudra utiliser une librairie JSON
   private String nextPath(BufferedReader r) throws Exception {
      String s = r.readLine();
      if( s==null ) return null;
      if( s.charAt(0)!='{' ) return s;   // Ancien format : un path par ligne
      int o = s.indexOf("path");
      int o1 = s.indexOf(':',o);
      int o2 = s.indexOf('"',o1+1);
      int o3 = s.indexOf('"',o2+1);
      return s.substring(o2+1,o3);
   }

   /**
    * Interroge les répertoires locaux HpxFinder pour obtenir une liste de
    * fichiers pour le losange donné Rempli le tableau downFiles
    *
    * @param path
    * @param npix
    * @param order
    * @return
    */
   boolean askLocalFinder(BuilderTiles bt, ArrayList<SrcFile> downFiles, String path, long npix, int order,double blank) {
      String hpxfilename = path + cds.tools.Util.FS + Util.getFilePath("", order - Constante.ORDER, npix);
      File f = new File(hpxfilename);
      String fitsfilename = null;
      if (f.exists()) {
         BufferedReader reader;
         try {
            reader = new BufferedReader(new FileReader(f));
            for( int i=0; (fitsfilename = nextPath(reader)) != null ; i++) {

//               if( maxFile>0 && i>=maxFile ) break; // Pas plus de N images

               try {
                  //					récupère l'image
                  Fits fitsfile = new Fits();

                  // Mode JPEG ou PNG avec .hhh
                  if( fitsfilename.endsWith(".hhh") || fitsfilename.indexOf(".hhh[")>0 ) {
                     String hhhFile = fitsfilename;

                     fitsfilename=fitsfilename.replaceAll("\\.hhh", ".jpg");
                     try { fitsfile=context.cacheFits.getFits(fitsfilename,true,true); }

                     // Sinon peut être en PNG ? (C'est pas très beau mais c'est si facile)
                     catch( Exception e ) {
                        fitsfilename=fitsfilename.replaceAll("\\.jpg", ".png");
                        fitsfile=context.cacheFits.getFits(fitsfilename,true,true);
                     }
                     fitsfile.loadHeaderFITS(hhhFile);

                  // Mode JPEG ou PNG avec calib interne
                  }
                  else
                     if( fitsfilename.endsWith(".jpg") || fitsfilename.indexOf(".jpg[")>0
                       || fitsfilename.endsWith(".png") || fitsfilename.indexOf(".png[")>0 ) {
                        fitsfile=context.cacheFits.getFits(fitsfilename,true,true);
                     }

                  // Mode FITS couleur
                  else if (bitpix==0) fitsfile.loadFITS(fitsfilename,true,true);

                  // Mode FITS classique ou JPEG/PNG avec calib
                  else {
                     fitsfile=context.cacheFits.getFits(fitsfilename,false,false);   // Utilisation d'un cache de fichiers Fits déjà ouvert
                     //					   fitsfile.loadFITS(fitsfilename);
                  }

                  fitsfile.setFilename(fitsfilename);
//                  if( !Double.isNaN(blank) ) fitsfile.setBlank(blank);

                  SrcFile file = new SrcFile();
                  file.fitsfile = fitsfile;
                  file.calib = fitsfile.getCalib();

                  downFiles.add(file);
                  bt.addFits(Thread.currentThread(),fitsfile);

               } catch (Exception e) {
                  System.err.println("Erreur de chargement de : " + fitsfilename);
                  e.printStackTrace();
                  continue;
               }
            }
         } catch (Exception e1) { // FileNotFound sur f=File(hpxfilename) et IO sur reader.readLine
            // this should never happens
            e1.printStackTrace();
            return false;
         }
         return true;
      }
      else {
         //			System.err.println("File Not Found : " + hpxfilename);
         return false;
      }
   }

   int n =0;

//   /**
//    * Cherche dans la liste des fichiers récupérés sur Aladin si les
//    * coordonnées y sont, et renvoie sa position dans l'objet de Coordonnées
//    * @param coo_gal coordonnée d'entrée en ra,dec (en GAL), où le x,y est enregistré
//    * @return l'accès au fichier FITS lu
//    * @see Calib#GetXY(Coord)
//    */
//   private Fits searchDownloaded(ArrayList<SrcFile> downFiles, Coord coo_gal, int recouvrement) {
//      if (downFiles == null)
//         return null;
//
//      int nfiles = downFiles.size();
//      for (int i = 0 ; i<nfiles ; i++,gagnant++) {
//         if( gagnant>=nfiles ) gagnant=0;
//         // cherche d'abord dans l'ancien gagnant
//         SrcFile file = downFiles.get(gagnant);
//         Calib calib = file.calib;
//         // transforme les coordonnées en ICRS
//         double[] radec = context.gal2ICRSIfRequired(coo_gal.al,coo_gal.del);
//         Coord c = new Coord(radec[0],radec[1]);
//
//         if (isInFile(c, recouvrement, calib)) {
//            double pix = file.fitsfile.getPixelDouble((int)c.x, file.fitsfile.heightCell-1-(int)c.y);
//            coo_gal.x = c.x;
//            coo_gal.y = c.y;
//            if( !file.fitsfile.isBlankPixel(pix ) ) return file.fitsfile;
//
//         }
//
//         //			else
//         //				System.out.println("\t inutile : " + downFiles.get(gagnant).fitsfile.filename);
//      }
//      return null;
//   }

//   /**
//    * sans recouvrement si elle est un tout petit peu sur le bord on copie la
//    * valeur
//    *
//    * @param c
//    *            coordonnées en ICRS (x,y modifiés)
//    */
//   private boolean isInFile(Coord c, int recouvrement, Calib calib) {
//      try {
//         calib.GetXY(c);
//      } catch (Exception e) {
//         e.printStackTrace();
//         return false;
//      }
//      // si la coordonnée est bien dedans
//      double xnpix = calib.getImgSize().getWidth();
//      double ynpix = calib.getImgSize().getHeight();
//      if (c.x >= recouvrement && c.x <= xnpix - 1 - recouvrement
//            && c.y >= recouvrement && c.y <= ynpix - 1 - recouvrement) {
//
//         return true;
//      }
//      // sans recouvrement si elle est un tout petit peu sur le bord on copie la valeur
//      if (recouvrement==0 &&
//            c.x >= -2 && c.x <= xnpix+1
//            && c.y >= -2 && c.y <= ynpix+1
//      ) {
//         if (c.x >=-2 && c.x < 0) {
//            c.x = 0;
//         }
//         if (c.x > xnpix-1 && c.x <= xnpix+2) {
//            c.x = xnpix-1;
//         }
//         if (c.y >= -2 && c.y < 0) {
//            c.y = 0;
//         }
//         if (c.y > ynpix-1 && c.y <= ynpix+2) {
//            c.y = ynpix-1;
//         }
//
//         return true;
//      }
//      return false;
//   }
//
//   int gagnant=0;

   /**
    * Ecrit les pixels dans un fichier .hpx avec notre format Healpix
    *
    * @param filename_base
    * @param nside_file
    * @param npix
    * @param nside
    * @param out
    * @throws Exception
    */
   void writeHealpix(String filename_base, int nside_file, long npix,
         int nside, Fits out) throws Exception {

      // prépare l'entete
      double incA = -(90./nside_file)/(Constante.SIDE);
      double incD = (90./nside_file)/(Constante.SIDE);
      double[] proj_center = new double[2];
      proj_center = CDSHealpix.pix2ang_nest(nside_file,npix);
      out.setCalib( new Calib(
            proj_center[0],proj_center[1],
            (Constante.SIDE+1)/2,(Constante.SIDE+1)/2,
            Constante.SIDE,Constante.SIDE,
            incA,incD,0,Calib.TAN,false,Calib.FK5));

      out.headerFits.setKeyValue("ORDERING", "CDSHEALPIX");
      if (bitpix > 0)
         out.headerFits.setKeyValue("BLANK", String
               .valueOf(Integer.MAX_VALUE));

      // gestion des niveaux de gris
      // cherche le min max
      double minmax[] = out.findMinMax();
      out.headerFits.setKeyValue("DATAMIN", String.valueOf(minmax[0]));
      out.headerFits.setKeyValue("DATAMAX", String.valueOf(minmax[1]));

      // Ecriture de l'image générée sous forme FITS "fullbits"
      out.writeFITS(filename_base);
      System.out.println("file " + filename_base + " written !!");

   }

   /*
	public static void main(String[] args) {
		long t= System.currentTimeMillis();
		int bitpix = 16;
		long n =0;


		for (long i = 0 ; i < 100000000 ; i++) {
//			switch (bitpix) {
//			case 8:
//			case 16:
//			case 32:
				int j = (int)i;
//			case -32:
//			case -64:
//				double d = (double)i;
//			case 0:
//				int k = (int)i%256;
//			}
		}
		System.out.println((System.currentTimeMillis()-t)+"ms");
	}
    */

}

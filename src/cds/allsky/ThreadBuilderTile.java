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

import healpix.newcore.HealpixBase;
import healpix.newcore.Pointing;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

final public class ThreadBuilderTile {

   private Context context;
   private BuilderTiles builderTiles;
   private int bitpix;
   private boolean hasAlternateBlank;
   private double blankOrig;
   private double blank;
   private boolean flagColor;
   private double bScale;
   private double bZero;
   private boolean fading;
   private String hpxFinderPath = null;
   private double[] cutOrig;
   private double[] cut;
   private int[] borderSize;
   private int radius;
   private ArrayList<SrcFile> downFiles;
   private boolean mixing;

   public ThreadBuilderTile(Context context,BuilderTiles builderTiles) {
      this.context = context;
      this.builderTiles=builderTiles;
      
      bitpix=context.getBitpix();
      flagColor = context.isColor();
      mixing = context.mixing;
      if( !flagColor ) {
         bZero = context.getBZero();
         bScale = context.getBScale();
         try {
            cutOrig=context.getCutOrig();
            cut=context.getCut();
         } catch( Exception e ) {
            e.printStackTrace();
         }
         blankOrig=context.getBlankOrig();
         hasAlternateBlank = context.hasAlternateBlank();
         blank = context.getBlank();
      } else {
         blank=0;
      }
      fading = context.getFading();
      borderSize = context.getBorderSize();
      radius = context.circle;
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
   
//   private void rmFits(BuilderTiles bt,ArrayList<SrcFile> downFiles) {
//      if( downFiles==null ) return;
//      for( SrcFile f : downFiles ) {
//         bt.rmFits(Thread.currentThread(),f.fitsfile);
//      }
//   }
   
   private boolean needMem(long rqMem) {
      return context.cacheFits.getFreeMem()<rqMem;
   }
   
   private boolean requiredMem(long nbProgen ) throws Exception {
      long rqMem = 4 * nbProgen * Constante.FITSCELLSIZE*Constante.FITSCELLSIZE*context.getNpixOrig();
      rqMem += 2*Constante.SIDE*Constante.SIDE*context.getNpix();
      return needMem(nbThreadRunning*rqMem);
   }
   
   private void checkMem(long nbProgen ) throws Exception {
      long rqMem = 4 * nbProgen * Constante.FITSCELLSIZE*Constante.FITSCELLSIZE*context.getNpixOrig();
      rqMem += 2*Constante.SIDE*Constante.SIDE*context.getNpix();
      if( nbProgen>Constante.MAXOVERLAY ) {
         rqMem += 2*Constante.SIDE*Constante.SIDE*8;
      }
      if( !needMem(rqMem) ) return;
      if( isTheLastRunning() ) {
         context.nlwarning(Thread.currentThread().getName()+" needs "+
               cds.tools.Util.getUnitDisk(rqMem)+" but can not stop (last thread running) !");
      }
      try {
         nbThreadRunning--;
         while( needMem(rqMem) ) {
            try { 
               context.nlwarning(Thread.currentThread().getName()+" is waiting more memory (need "+
                     cds.tools.Util.getUnitDisk(rqMem)+")...");

               cds.tools.Util.pause((int)( 1000*(1+Math.random()*5)));
               context.cacheFits.forceClean();
               nbThreadRunning = builderTiles.nbThreadRunning(); // Pour etre vraiment sur
               if( nbThreadRunning<=1 ) {
                  context.nlwarning(Thread.currentThread().getName()+" resumes (last thread runnning)");
//                  context.cacheFits.forceClean();
                  break;
               }
            } catch( Exception e ) { }
            if( context.isTaskAborting() ) throw new Exception("Task abort !");
         }
      } finally { nbThreadRunning++; }
   }
   
   static protected int nbThreadRunning=0;
   
   private boolean isTheLastRunning() {
      return nbThreadRunning<=1;
   }
   
//   static private long totalDelay=0L;
//   static private long nRead=0L;
//   static private long totalDelay1=0L;
//   static private long nRead1=0L;

   Fits buildHealpix(BuilderTiles bt, int order, long npix_file) throws Exception {
      ArrayList<SrcFile> downFiles = null;
      Fits out=null;

      try {
         // initialisation de la liste des fichiers originaux pour ce losange
         downFiles = new ArrayList<SrcFile>(Constante.MAXOVERLAY*2);         
         if( !askLocalFinder(bt,downFiles,hpxFinderPath, order, npix_file, blank)) {
            return null;
         }

         Fits f;
         int n=downFiles.size();
         if( n>statMaxOverlays ) statMaxOverlays=n;

         // Pas trop de progéniteurs => on peut tout faire d'un coup
         if( !mixing || n<Constante.MAXOVERLAY  || !requiredMem(mixing ? n : 1) ) {

            statOnePass++;
            checkMem(mixing ? n : 1);
            out = buildHealpix1(bt,order,npix_file,downFiles,0,n,null);

            // Trop de progéniteurs, on va travailler en plusieurs couches de peinture
            // en mémorisant le poids de chaque pixel à chaque couche
         } else {

            statMultiPass++;
            checkMem(Constante.MAXOVERLAY);

            // poids déjà calculés
            double [] weight = null;  
            double [] fWeight = new double[Constante.SIDE*Constante.SIDE];

            for( int deb=0; deb<n; deb+=Constante.MAXOVERLAY ) {
               int fin = deb+Constante.MAXOVERLAY;
               if( fin>=n ) fin=n;
               f = buildHealpix1(bt,order,npix_file,downFiles,deb,fin,fWeight);
               if( f!=null ) {
                  if( out==null ) {
                     out=f;
                     weight=fWeight;
                     fWeight = new double[Constante.SIDE*Constante.SIDE];
                  } else out.coadd(f,weight,fWeight);
               }
               
               // On libère dès à présent les fichiers Fits déjà utilisés
               // pour qu'ils puissent être supprimés du cache le cas échéant
               for( int i=deb; i<fin; i++ ) {
                  SrcFile f1 = downFiles.get(i);
                  f1.fitsfile.rmUser();
                  downFiles.set(i,null);
               }
            }
            // Changement de bitpix a la fin du calcul pour éviter les erreurs d'arrondi
            // liées aux changements de bitpix
            if( out!=null && bitpix!=context.getBitpixOrig() ) {
               Fits out1 = new Fits(out.width,out.height,bitpix);
               out1.setBlank(blank);
               out1.setBzero(bZero);
               out1.setBscale(bScale);
               for( int y=0; y<out.height; y++ ) {
                  for( int x=0; x<out.width; x++ ) {
                     double pixelFinal = out.getPixelDouble(x, y);
                     pixelFinal = Double.isNaN(pixelFinal) ? blank
                           : pixelFinal<=cutOrig[2] ? cut[2]
                                 : pixelFinal>=cutOrig[3] ? cut[3]
                                       : (pixelFinal-cutOrig[2])*context.coef + cut[2];
                                 out1.setPixelDouble(x, y, pixelFinal);
                  }
               }
               out = out1;
               out1=null;
            }
         }
      } 
      catch( Exception e ) { 
         e.printStackTrace(); 
      }

      for( int i=downFiles.size()-1; i>=0; i-- ) {
         SrcFile f1 = downFiles.get(i);
         if( f1!=null ) {
            Fits f = f1.fitsfile;
            f.rmUser();
         }
      }

      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      
      return out;
   }
   
   static long statOnePass=0L;
   static long statMultiPass=0L;
   static int statMaxOverlays=0;
   
   static private double toRad = 180./Math.PI;
   static private double PI2 = Math.PI/2.;


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
//   Fits buildHealpix1(BuilderTiles bt, int nside_file, long npix_file, int nside,
//         ArrayList<SrcFile> downFiles,int deb,int fin,double [] weight) throws Exception {
   Fits buildHealpix1(BuilderTiles bt, int order, long npix_file,
         ArrayList<SrcFile> downFiles,int deb,int fin,double [] weight) throws Exception {
      boolean empty = true;
      long min;
      long index;
      double radec[] = new double[2];
      Coord coo = new Coord();
      SrcFile file = null;
      Fits out=null;
      int bitpix    = this.bitpix;
      double blank  = this.blank;
      double bScale = this.bScale;
      double bZero  = this.bZero;;

      try {
         // cherche les numéros de pixels Healpix dans ce losange
         min = npix_file * Constante.SIDE * Constante.SIDE;

         boolean flagModifBitpix = bitpix!=context.getBitpixOrig();
         
         // Dans le cas d'un travail itératif par matrice de coefficients, on ne peut pas changer le bitpix
         // sans risque d'altérer la moyenne par des problèmes d'arrondi. 
         // Il faudra donc le faire a posteriori, tout à la fin
         if( flagModifBitpix && weight!=null ) {
            bitpix= context.getBitpixOrig();
            blank = bitpix<0 ? Double.NaN : blankOrig;
            flagModifBitpix=false;
         }

         out = new Fits(Constante.SIDE, Constante.SIDE, bitpix);
         if( !flagColor ) {
            out.setBlank(blank);
            out.setBzero(bZero);
            out.setBscale(bScale);
            
         // remplissage transparent
         } else {
            if( context.targetColorMode==Context.PNG ) {
               for( int i=0; i<out.rgb.length; i++ ) out.rgb[i]=0xFF000000;
            }
         }
         
         // cherche la valeur à affecter dans chacun des pixels healpix
         int overlay = fin-deb;
         double [] pixvalG=null,pixvalB=null;
         double [] pixval = new double[overlay];  
         double [] pixcoef = new double[overlay];
         if( flagColor ) { pixvalG = new double[overlay]; pixvalB = new double[overlay]; }
         
         HealpixBase hpx = CDSHealpix.getHealpixBase(order+Constante.ORDER);
         
         boolean gal2ICRS = context.frame!=Localisation.ICRS;
         
         for (int y = 0; y < out.height; y++) {
//            if( context.isTaskAborting() ) break;
            for (int x = 0; x < out.width; x++) {
               index = min + context.xy2hpx(y * out.width + x);
               // recherche les coordonnées du pixels HPX
               Pointing pt = hpx.pix2ang(index);
               radec[1] = (PI2 - pt.theta)*toRad;
               radec[0] = pt.phi*toRad;

               if( gal2ICRS ) radec = context.gal2ICRSIfRequired(radec);
               coo.al = radec[0]; coo.del = radec[1];
               
               int nbPix=0;
               double totalCoef=0;
//               String lastFitsFile="";
//               double lastX=-1,lastY=-1;
               
               for( int i=deb; i<fin; i++ ) {
                  try {
                     file = downFiles.get(i);
                     file.open();

                     // Même fichier qu'avant => même calibration, on s'évite un calcul ra,dec=>x,y
//                      if( lastFitsFile.equals(file.fitsfile.filename) ) {
//                         nX++; coo.y=lastY; coo.x=lastX;
//                         if( nX%10==0 ) System.out.println("same nX="+nX);
//                      }

                     // Détermination du pixel dans l'image à traiter
//                     else { 
//                        file.fitsfile.calib.GetXY(coo,false);
                        file.fitsfile.calib.GetXY(coo,false);
//                        if( !Fits.JPEGORDERCALIB || Fits.JPEGORDERCALIB && file.fitsfile.bitpix!=0 ) 
                           coo.y = file.fitsfile.height-coo.y -1;
//                        lastY=coo.y;
//                        lastX=
                              coo.x -= 1;                             // Correction manuelle de 1 en comparaison avec les originaux
//                        lastFitsFile=file.fitsfile.filename;
//                     }
                     
                     // Cas RGB
                     if( flagColor ) {
                        int pix = getBilinearPixelRGB(file.fitsfile,coo);
                        if( pix==0 ) continue;
//                        if( !mixing ) {
//                           out.setPixelRGBJPG(x, y, pix);
//                           empty=false;
//                           break;
//                        }
                        pixval[nbPix] = 0xFF & (pix>>16);
                        pixvalG[nbPix] = 0xFF & (pix>>8);
                        pixvalB[nbPix] = 0xFF & pix;

                        // Cas normal
                     } else {
                        double pix = getBilinearPixel(file.fitsfile,coo,file.blank);
                        if( Double.isNaN(pix) ) continue;
//                        if( !mixing ) {
//                           out.setPixelDouble(x,y,pix);
//                           empty=false;
//                           break;
//                        }
                        pixval[nbPix]=pix;
                     }
                     // fading
                     totalCoef+= pixcoef[nbPix] = getCoef(file.fitsfile,coo);
                     nbPix++;
                     
                     // On a un pixel, pas besoin d'aller plus loin
                     if( !mixing ) break;

                  }
                  catch( Exception e ) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                     continue;
                  }
               }

               // cas RGB
               if( flagColor ) {
                  int pixelFinal=0;
                  
                  if( nbPix!=0 ) {
                     if( totalCoef==0 )  pixelFinal = (((int)pixval[0] & 0xFF)<<16) | (((int)pixvalG[0] & 0xFF)<<8) | ((int)pixvalB[0] & 0xFF);
                     else {
                        double r=0,g=0,b=0;
                        for( int i=0; i<nbPix; i++ ) {
                           r += (pixval[i]*pixcoef[i])/totalCoef;
                           g += (pixvalG[i]*pixcoef[i])/totalCoef;
                           b += (pixvalB[i]*pixcoef[i])/totalCoef;
                        }
                        if( r>255 ) r=255; else if( r<0 ) r=0;
                        if( g>255 ) g=255; else if( g<0 ) g=0;
                        if( b>255 ) b=255; else if( b<0 ) b=0;
                        pixelFinal = (((int)r & 0xFF)<<16) | (((int)g & 0xFF)<<8) | ((int)b & 0xFF);
                     }
                  }
                  
                  if( pixelFinal!=0 ) empty=false;
                  
                  int alpha = pixelFinal==0 ?  0 : 0xFF000000;
                  out.setPixelRGBJPG(x, y, alpha | pixelFinal);

                  // Cas normal
               }  else {
                  double pixelFinal=0;
                  if( nbPix==0 ) pixelFinal = Double.NaN;
                  else if( totalCoef==0 )  { empty=false; pixelFinal = pixval[0]; }
                  else {
                     empty=false;
                     for( int i=0; i<nbPix; i++ ) {
                        pixelFinal += (pixval[i]*pixcoef[i])/totalCoef;
//                        pixelFinal += totalCoef;
                     }
                  }
                  
                  // Changement de bitpix ?
                  if( flagModifBitpix ) {
                     pixelFinal = Double.isNaN(pixelFinal) ? blank
                                : pixelFinal<=cutOrig[2] ? cut[2]
                                : pixelFinal>=cutOrig[3] ? cut[3]
                                : (pixelFinal-cutOrig[2])*context.coef + cut[2];
                     if( bitpix>0 && (long)pixelFinal==blank && pixelFinal!=blank ) pixelFinal+=0.5;
                  } else if( Double.isNaN(pixelFinal) ) pixelFinal = blank;
                  
                  out.setPixelDouble(x,y,pixelFinal);
               }
               
               // Mémorisation du poids du pixel (si nécessaire)
               if( weight!=null ) weight[y*Constante.SIDE+x]=totalCoef;
               
            }
         }
      } 
      catch( Exception e ) { 
         e.printStackTrace(); 
         empty=true;
         if( weight!=null ) for( int i=0; i<weight.length; i++ ) weight[i]=0;
      }
      
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      return (!empty) ? out : null;
   }
   
   static private final double OVERLAY_PROPORTION = 1/6.;
   
   
   // détermine si le pixel doit être pris en compte, ou s'il est dans la marge
   private boolean isIn(Fits f, Coord coo) {
      if( radius>0 ) {
         double cx = f.width/2;
         double cy = f.height/2;
         double dx = coo.x-cx;
         double dy = coo.y-cy;
         double d = dx*dx + dy*dy;
         if( d>radius*radius ) return false;
      }
      if( coo.x<borderSize[1] || coo.x>f.width-borderSize[3] ) return false;
      if( coo.y<borderSize[0] || coo.y>f.height-borderSize[2] ) return false;
      return true;
   }
   
   // Détermination d'un coefficent d'atténuation de la valeur du pixel en fonction de sa distance au bord 
   // Même si le fading est désactivé, il faut tout de même divisé par 2 ou 4 les lignes des cellules
   // adjacentes (dû au fait que le pixel des bords de cellules auront sinon un poids
   // double voire quadruple, ce qui va se voir en cas de superpostion avec une autre image)
   private double getCoef(Fits f,Coord coo) {
      int x1 = (int)coo.x;
      int y1 = (int)coo.y;
      
      // Diviseur du coefficient sur les lignes de recouvrements des cellules
      // adjacentes (voir commentaire de la méthode)
      double div=1;
      if( x1>0 && x1<f.width  && (coo.x<=f.xCell || coo.x>=f.xCell+f.widthCell-1) ) div*=2;
      if( y1>0 && y1<f.height && (coo.y<=f.yCell || coo.y>=f.yCell+f.heightCell-1) ) div*=2;
      
      if( !fading ) return 1./div;
      
      double c=0;
      try {
          if( radius>0 ) {
            double cx = f.width/2;
            double cy = f.height/2;
            double dx = coo.x-cx;
            double dy = coo.y-cy;
            double d = Math.sqrt(dx*dx + dy*dy);
            if( d<radius-radius*OVERLAY_PROPORTION ) c=1;
            else c = (radius - d)/radius;
         } else {
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
            //         c = Math.min(coefx,coefy);
            c = coefx*coefy;
         }
      } catch( Exception e ) {
         c=0;
      }
      return c/div;
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
      if( !isIn(f,coo) ) return Double.NaN;
      
      double x = coo.x;
      double y = coo.y;
      
      int x1 = (int)x;
      int y1 = (int)y;
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

      return bilineaire(x1,y1,x2,y2,x,y,a0,a1,a2,a3);
   }

   private int getBilinearPixelRGB(Fits f,Coord coo) {
      if( !isIn(f,coo) ) return 0;
      
      double x = coo.x;
      double y = coo.y;
      
      int x1 = (int)x;
      int y1 = (int)y;
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
            
//      boolean c0 = b0==0;
//      boolean c1 = b1==0;
//      boolean c2 = b2==0;
//      boolean c3 = b3==0;
      boolean c0 = (b0&0xFF000000)==0;
      boolean c1 = (b1&0xFF000000)==0;
      boolean c2 = (b2&0xFF000000)==0;
      boolean c3 = (b3&0xFF000000)==0;
      
      if( c0 && c1 && c2 && c3 ) return 0;
      if( c0 || c1 || c2 || c3 ) {
         int a = !c0 ? b0 : !c1 ? b1 : !c2 ? b2 : b3;
         if( c0 ) b0=a;
         if( c1 ) b1=a;
         if( c2 ) b2=a;
         if( c3 ) b3=a;
      }

//      int pix=0xFF;
      int pix=0x00;
      for( int i=16; i>=0; i-=8 ) {
         double a0 = 0xFF & (b0>>i);
         double a1 = 0xFF & (b1>>i);
         double a2 = 0xFF & (b2>>i);
         double a3 = 0xFF & (b3>>i);
         int p = (int)(bilineaire(x1,y1,x2,y2,x,y,a0,a1,a2,a3)+0.5);
         pix = (pix<<8) | p;
      }
      if( pix!=0 ) pix|=0xFF000000;
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
  boolean askLocalFinder(BuilderTiles bt, ArrayList<SrcFile> downFiles, String path, int order, long npix, double blank) {
      String hpxfilename = path + cds.tools.Util.FS + Util.getFilePath("", order, npix);
      File f = new File(hpxfilename);
      String fitsfilename = null;
      if (f.exists()) {
         BufferedReader reader = null;
         try {
            reader = new BufferedReader(new FileReader(f));
            for( int n=0; (fitsfilename = nextPath(reader)) != null; n++) {
               
               try {
                  Fits fitsfile = new Fits();
                  fitsfile.setFilename(fitsfilename);

                  SrcFile file = new SrcFile(fitsfilename);
                  file.fitsfile = fitsfile;
//                  file.open();

                  downFiles.add(file);
//                  bt.addFits(Thread.currentThread(),fitsfile);
                  
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
         } finally {
            if( reader!=null ) try { reader.close(); } catch( Exception e) {}
         }
         return true;
      }
      else {
         //         System.err.println("File Not Found : " + hpxfilename);
         return false;
      }
   }
   
   class SrcFile {
      Fits fitsfile;
      boolean isOpened=false;
      String name=null;
      double blank;
      
      SrcFile(String name ) {
         this.name=name;
      }
      
      @Override
      public String toString() {
          return fitsfile.getFilename();
      }
      
      protected void open() throws Exception {
         if( isOpened ) return;
         int mode = (name.endsWith(".hhh") || name.indexOf(".hhh[")>0) ? CacheFits.HHH
               : (name.endsWith(".jpg") || name.indexOf(".jpg[")>0) ? CacheFits.JPEG
               : (name.endsWith(".png") || name.indexOf(".png[")>0) ? CacheFits.PNG   
               : CacheFits.FITS;

         // Mode FITS couleur
         if( mode==CacheFits.FITS && bitpix==0 ) fitsfile.loadFITS(name,true,true);
         
         // Mode normal
         else fitsfile=context.cacheFits.getFits(name,mode,true); 
         
         isOpened=true;
         fitsfile.addUser();
         
         blank = !hasAlternateBlank ? fitsfile.blank : blankOrig;
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
//         //           else
//         //               System.out.println("\t inutile : " + downFiles.get(gagnant).fitsfile.filename);
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

//   /**
//    * Ecrit les pixels dans un fichier .hpx avec notre format Healpix
//    * 
//    * @param filename_base
//    * @param nside_file
//    * @param npix
//    * @param nside
//    * @param out
//    * @throws Exception
//    */
//   void writeHealpix(String filename_base, int nside_file, long npix,
//         int nside, Fits out) throws Exception {
//
//      // prépare l'entete
//      double incA = -(90./nside_file)/(Constante.SIDE);
//      double incD = (90./nside_file)/(Constante.SIDE);
//      double[] proj_center = new double[2];
//      proj_center = CDSHealpix.pix2ang_nest(nside_file,npix);
//      out.setCalib( new Calib(
//            proj_center[0],proj_center[1],
//            (Constante.SIDE+1)/2,(Constante.SIDE+1)/2,
//            Constante.SIDE,Constante.SIDE,
//            incA,incD,0,Calib.TAN,false,Calib.FK5));
//
//      out.headerFits.setKeyValue("ORDERING", "CDSHEALPIX");
//      if (bitpix > 0)
//         out.headerFits.setKeyValue("BLANK", String
//               .valueOf(Integer.MAX_VALUE));
//
//      // gestion des niveaux de gris
//      // cherche le min max
//      double minmax[] = out.findMinMax();
//      out.headerFits.setKeyValue("DATAMIN", String.valueOf(minmax[0]));
//      out.headerFits.setKeyValue("DATAMAX", String.valueOf(minmax[1]));
//
//      // Ecriture de l'image générée sous forme FITS "fullbits"
//      out.writeFITS(filename_base);
//      System.out.println("file " + filename_base + " written !!");
//
//   }

   /*
    public static void main(String[] args) {
        long t= System.currentTimeMillis();
        int bitpix = 16;
        long n =0;


        for (long i = 0 ; i < 100000000 ; i++) {
//          switch (bitpix) {
//          case 8:
//          case 16:
//          case 32:
                int j = (int)i;
//          case -32:
//          case -64:
//              double d = (double)i;
//          case 0:
//              int k = (int)i%256;
//          }
        }
        System.out.println((System.currentTimeMillis()-t)+"ms");
    }
    */

}

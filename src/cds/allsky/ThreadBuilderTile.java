// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

import java.awt.Polygon;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;
import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;

final public class ThreadBuilderTile {

   private Context context;
   private BuilderTiles builderTiles;
   private int bitpix;
   private Mode coaddMode;
   private double max;
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
   private int circle;
   private Polygon polygon;
   private ArrayList<SrcFile> downFiles;
   private boolean mixing;
   private int tileSide;

   static protected HashMap<File, Polygon> hashPolygon=null;   // Polygones associés à chaque fichier ou répertoire

   public ThreadBuilderTile(Context context,BuilderTiles builderTiles) {
      this.context = context;
      this.builderTiles=builderTiles;

      bitpix=context.getBitpix();
      coaddMode=context.getMode();
      max = Fits.getMax( context.getBitpixOrig() );
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
      circle = context.circle;
      polygon=context.polygon;
      hpxFinderPath = context.getHpxFinderPath();
      tileSide = context.getTileSide();

      downFiles = new ArrayList<SrcFile>(Constante.MAXOVERLAY);
   }

   public long getMem() {
      long mem=2+5*4+4*8+2*8*4;
      for( SrcFile f : downFiles) {
         mem += f.fitsfile.getMem();
      }
      return mem;
   }

   private boolean needMem(long rqMem) {
      long mem =  CacheFits.getFreeMem();
      return mem-40*1024L*1024L<rqMem;
   }
   
   static public int nbThreads;

   protected boolean requiredMem(long nbProgen,int nbThreads ) throws Exception {
      long rqMem = 4 * nbProgen * Constante.ORIGCELLWIDTH*Constante.ORIGCELLWIDTH*context.getNpixOrig();
      rqMem += 2*tileSide*tileSide*context.getNpix();
      return needMem(nbThreads*rqMem);
   }
   
   private Object objRel = new Object();

   private void checkMem(int nbProgen, long rqMem ) throws Exception {
      rqMem += 2*tileSide*tileSide*context.getNpix();
      if( nbProgen>Constante.MAXOVERLAY ) {
         rqMem += 2*tileSide*tileSide*8;
      }
//      System.out.println("required "+cds.tools.Util.getUnitDisk(rqMem)+"/"+cds.tools.Util.getUnitDisk(CacheFits.getFreeMem()));
      if( !needMem(rqMem) ) return;
      synchronized( objRel ) {
         if( !needMem(rqMem) ) return;

         // On fait d'abord le ménage dans les Fits à générer
         long sizeReleaseBitmap;
         if( (sizeReleaseBitmap=builderTiles.releaseBitmap())>0 ) {
            System.gc();
            cds.tools.Util.pause(100);
            if( context.getVerbose()>3 ) context.info("Need more RAM: output Fits bitmap release => "+cds.tools.Util.getUnitDisk(sizeReleaseBitmap));
            if( !needMem(rqMem) ) return;
         }

         if( builderTiles.nbThreadRunning()<=1 ) {
            context.cacheFits.forceClean();
            if( context.getVerbose()>3 ) context.warning(Thread.currentThread().getName()+" needs "+
                  cds.tools.Util.getUnitDisk(rqMem)+" but can not stop (last thread running) !");
            return;
         }
         try {
            if( !builderTiles.arret(this) ) {
               if( context.getVerbose()>3 ) context.warning(Thread.currentThread().getName()+" needs "+
                     cds.tools.Util.getUnitDisk(rqMem)+" but can not stop (sub thread) !");
               return;
              
            }
            if( context.getVerbose()>3 ) context.info(Thread.currentThread().getName()+" suspended");
            while( needMem(rqMem) ) {
               try {
                  if( context.getVerbose()>3 ) context.info(Thread.currentThread().getName()+" is waiting more memory (need "+
                        cds.tools.Util.getUnitDisk(rqMem)+")...");

                  cds.tools.Util.pause((int)( 1000*(1+Math.random()*5)));
                  context.cacheFits.forceClean();
                  if( builderTiles.nbThreadRunning()<=0 ) {
                     if( context.getVerbose()>3 ) context.warning(Thread.currentThread().getName()+" resumes (last thread)");
                     break;
                  }
               } catch( Exception e ) { }
               if( context.isTaskAborting() ) throw new Exception("Task abort !");
            }
         } finally {
            if( builderTiles.reprise(this) ) {
               if( context.getVerbose()>3 ) context.info(Thread.currentThread().getName()+" restarted");
            }
        }
      }
   }
   

   /** Détermination de la mémoire requise pour ouvrir n fichiers originaux de downFiles à partir de la position deb */
   protected long getReqMem(ArrayList<SrcFile> downFiles,int deb, int n) {
      long mem=0L;
      for( int i=0; i<n && deb<downFiles.size(); i++,deb++ ) {
         SrcFile file = downFiles.get(deb);   
         mem += file.cellMem;
      }
      return mem;
   }

   //   static private long totalDelay=0L;
   //   static private long nRead=0L;
   //   static private long totalDelay1=0L;
   //   static private long nRead1=0L;

   Fits buildHealpix(BuilderTiles bt, String path, int order, long npix_file, int z) throws Exception {
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
         // Pour les cubes, on va pour le moment travailler en 1 seule passe (A VOIR PAR LA SUITE S'IL FAUT AMELIORER)
         if( !context.live && (coaddMode==Mode.ADD || !mixing || n<Constante.MAXOVERLAY  || !requiredMem(mixing ? n : 1, nbThreads)) ) {

            statOnePass++;
            long mem = getReqMem(downFiles, 0, n);
            checkMem(mixing ? n : 1, mem);
            out = buildHealpix1(bt,order,npix_file,z,downFiles,0,n,null);

            // Trop de progéniteurs, on va travailler en plusieurs couches de peinture
            // en mémorisant le poids de chaque pixel à chaque couche
         } else {

            statMultiPass++;

            // poids déjà calculés
            double [] weight = null;
            double [] fWeight = new double[tileSide*tileSide];

            for( int deb=0; deb<n; deb+=Constante.MAXOVERLAY ) {
               int fin = deb+Constante.MAXOVERLAY;
               if( fin>=n ) fin=n;
               
               long mem = getReqMem(downFiles,deb,n);
               checkMem(n,mem);
               
               f = buildHealpix1(bt,order,npix_file,z,downFiles,deb,fin,fWeight);
               if( f!=null ) {
                  if( out==null ) {
                     out=f;
                     weight=fWeight;
                     fWeight = new double[tileSide*tileSide];
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
            
            // Sauvegarde de la tuile de poids
            if( out!=null && context.live  ) {
               String file = Util.getFilePath(path,order,npix_file,z);
               writeWeight(file,weight,tileSide);
            }

         }
          
      } catch( Exception e ) {
         e.printStackTrace();
      }

      for( int i=downFiles.size()-1; i>=0; i-- ) {
         SrcFile f1 = downFiles.get(i);
         try { if( f1!=null ) f1.release(); } catch( Exception e ) { }
      }

      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      
      return out;
   }
   
   /**
    * Sauvegarde des poids des pixels d'une tuile sous la forme d'une FITS
    * La nomenclature suit la même que pour les tuiles classiques, avec le suffixe "_w"
    * @param file    Le nom de fichier (sans le suffixe "_w" ni l'extension ".fits)
    * @param weight  La matrice des poids
    * @param w       La largeur de la tuile
    * @throws Exception
    */
   static public void writeWeight(String file, double [] weight, int w) throws Exception {
      if( weight==null ) return;
      Fits fits = new Fits(w,w,-32);
      int i=0;
      for( int y=0; y<fits.height; y++ ) {
         for( int x=0; x<fits.width; x++ ) {
            fits.setPixelDouble(x,y,weight[i++]);
         }
      }
      fits.writeFITS(file+"_w.fits");
   }

   /**
    *  Chargement des poids des pixels d'une tuile sous la forme d'une FITS
    * La nomenclature suit la même que pour les tuiles classiques, avec le suffixe "_w"
    * @param file    Le nom de fichier (sans le suffixe "_w" ni l'extension ".fits)
    * @return        La matrice des poids - remplie de la valeur par défaut si le fichier n'existe pas
    * @throws Exception
    */
   static public double [] loadWeight(String file,int w,double defaultWeight) throws Exception {
      double [] weight = new double[ w *w ];
      String filename = file+"_w.fits";
      if( !(new File(filename)).exists() ) {
         for( int i=0; i<weight.length; i++ ) weight[i]=defaultWeight;
         return weight;
      }
      
      Fits fits = new Fits();
      fits.loadFITS(filename);
      int i=0;
      for( int y=0; y<fits.height; y++ ) {
         for( int x=0; x<fits.width; x++ ) {
            weight[i++] = fits.getPixelDouble(x,y);
         }
      }
      return weight;
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
   Fits buildHealpix1(BuilderTiles bt, int order, long npix_file, int z,
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
      double bZero  = this.bZero;
      int tileSide = context.getTileSide();

      try {
         // cherche les numéros de pixels Healpix dans ce losange
         min = npix_file * tileSide * tileSide;

         boolean flagModifBitpix = bitpix!=context.getBitpixOrig();

         // Dans le cas d'un travail itératif par matrice de coefficients, on ne peut pas changer le bitpix
         // sans risque d'altérer la moyenne par des problèmes d'arrondi.
         // Il faudra donc le faire a posteriori, tout à la fin
         if( flagModifBitpix && weight!=null ) {
            bitpix= context.getBitpixOrig();
            blank = bitpix<0 ? Double.NaN : blankOrig;
            flagModifBitpix=false;
         }

         out = new Fits(tileSide, tileSide, bitpix);
         if( !flagColor ) {
            out.setBlank(blank);
            out.setBzero(bZero);
            out.setBscale(bScale);

            // remplissage transparent
         } else {
            if( context.targetColorMode==Constante.TILE_PNG ) {
               for( int i=0; i<out.rgb.length; i++ ) out.rgb[i]=0xFF000000;
            }
         }

         // cherche la valeur à affecter dans chacun des pixels healpix
         int overlay = fin-deb;
         double [] pixvalG=null,pixvalB=null;
         double [] pixval = new double[overlay];
         double [] pixcoef = new double[overlay];
         if( flagColor ) { pixvalG = new double[overlay]; pixvalB = new double[overlay]; }

         HealpixBase hpx = CDSHealpix.getHealpixBase(order+context.getTileOrder());

         boolean gal2ICRS = context.getFrame()!=Localisation.ICRS;

         for (int y = 0; y < out.height; y++) {
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

               int removed=0;
               for( int i=deb; i<fin; i++ ) {
                  try {
                     file = downFiles.get(i);
                     if( file.flagRemoved ) continue;
                     try {
                        file.open(z);
                     } catch( Exception e ) {
                        file.flagRemoved=true;
                        removed++;
                        if( removed>=fin-deb ) return null;  // Aucun fichier source disponible
                        continue;
                     }

                     // Détermination du pixel dans l'image à traiter
                     file.fitsfile.calib.GetXY(coo,false);
                     coo.y = file.fitsfile.height-coo.y -1;
                     coo.x -= 1;                             // Correction manuelle de 1 en comparaison avec les originaux

                     // Cas RGB
                     if( flagColor ) {
                        int pix = getBilinearPixelRGB(file,coo);
                        if( pix==0 ) continue;
                        pixval[nbPix] = 0xFF & (pix>>16);
                        pixvalG[nbPix] = 0xFF & (pix>>8);
                        pixvalB[nbPix] = 0xFF & pix;

                        // Cas normal
                     } else {
                        double pix = getBilinearPixel(file,coo,z,file.blank);
                        if( Double.isNaN(pix) ) continue;
                        pixval[nbPix]=pix;
                     }
                     // fading
                     totalCoef+= pixcoef[nbPix] = getCoef(file.fitsfile,coo);
                     nbPix++;

                     // On a un pixel, pas besoin d'aller plus loin
                     if( !mixing ) break;
                  }
                  catch( Exception e ) {
                     e.printStackTrace();
                     continue;
                  }
               }

               // cas RGB
               if( flagColor ) {
                  int pixelFinal=0;

                  if( nbPix!=0 ) {
                     if( totalCoef==0 )  pixelFinal = 0xFF000000 | (((int)pixval[0] & 0xFF)<<16) | (((int)pixvalG[0] & 0xFF)<<8) | ((int)pixvalB[0] & 0xFF);

                     // Addition simple
                     else if( coaddMode==Mode.ADD ) {
                        double r=0,g=0,b=0;
                        for( int i=0; i<nbPix; i++ ) {
                           r += pixval[i];
                           g += pixvalG[i];
                           b += pixvalB[i];
                        }
                        if( r>255 ) r=255;
                        if( g>255 ) g=255;
                        if( b>255 ) b=255;
                        pixelFinal = 0xFF000000 | (((int)r & 0xFF)<<16) | (((int)g & 0xFF)<<8) | ((int)b & 0xFF);

                        // Calcul de moyenne
                     } else {
                        double r=0,g=0,b=0;
                        for( int i=0; i<nbPix; i++ ) {
                           r += (pixval[i]*pixcoef[i])/totalCoef;
                           g += (pixvalG[i]*pixcoef[i])/totalCoef;
                           b += (pixvalB[i]*pixcoef[i])/totalCoef;
                        }
                        if( r>255 ) r=255; else if( r<0 ) r=0;
                        if( g>255 ) g=255; else if( g<0 ) g=0;
                        if( b>255 ) b=255; else if( b<0 ) b=0;
                        pixelFinal = 0xFF000000 | (((int)r & 0xFF)<<16) | (((int)g & 0xFF)<<8) | ((int)b & 0xFF);
                     }
                     empty=false;
                  }

                  //                  if( pixelFinal!=0 ) empty=false;
                  //                  int alpha = pixelFinal==0 ?  0 : 0xFF000000;
                  //                  out.setPixelRGBJPG(x, y, alpha | pixelFinal);
                  out.setPixelRGBJPG(x, y, pixelFinal);

                  // Cas normal
               }  else {
                  double pixelFinal=0;
                  if( nbPix==0 ) pixelFinal = Double.NaN;

                  // Mode ADD simple
                  else if( coaddMode==Mode.ADD ) {
                     empty=false;
                     for( int i=0; i<nbPix; i++ ) {
                        if( pixelFinal/2. + pixval[i]/2 > max/2. ) { pixelFinal=max; break; }
                        pixelFinal += pixval[i];
                     }
                  }

                  else if( totalCoef==0 )  { empty=false; pixelFinal = pixval[0]; }

                  // Prise en compte des coef.
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
               if( weight!=null ) weight[y*tileSide+x]=totalCoef;
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
   private boolean isIn(SrcFile srcFile, Coord coo) {
      Fits f = srcFile.fitsfile;
      if( circle>0 ) {
         double cx = f.width/2;
         double cy = f.height/2;
         double dx = coo.x-cx;
         double dy = coo.y-cy;
         double d = dx*dx + dy*dy;
         if( d>circle*circle ) return false;
      }
      if( coo.x<borderSize[1] || coo.x>f.width-borderSize[3] ) return false;
      if( coo.y<borderSize[0] || coo.y>f.height-borderSize[2] ) return false;
      if( polygon!=null && !polygon.contains(coo.x, coo.y) ) return false;
      if( srcFile.polygon!=null && !srcFile.polygon.contains(coo.x, coo.y) ) return false;
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
         if( circle>0 ) {
            double cx = f.width/2;
            double cy = f.height/2;
            double dx = coo.x-cx;
            double dy = coo.y-cy;
            double d = Math.sqrt(dx*dx + dy*dy);
            if( d<circle-circle*OVERLAY_PROPORTION ) c=1;
            else c = (circle - d)/circle;
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


   private double getBilinearPixel(SrcFile srcFile,Coord coo,int z,double myBlank) {

      Fits f = srcFile.fitsfile;
      if( !isIn(srcFile,coo) ) return Double.NaN;

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
            x1>=f.xCell+f.widthCell || y1>=f.yCell+f.heightCell ||
            z<0 || z>=f.depth ) return Double.NaN;

      // Sur le bord, on dédouble le dernier pixel
      if( ox1==f.xCell-1 ) ox1++;
      if( oy1==f.yCell-1 ) oy1++;
      if( ox2==f.xCell+f.widthCell ) ox2--;
      if( oy2==f.yCell+f.heightCell ) oy2--;

      double a0 = f.getPixelDouble(ox1,oy1,z);
      double a1 = f.getPixelDouble(ox2,oy1,z);
      double a2 = f.getPixelDouble(ox1,oy2,z);
      double a3 = f.getPixelDouble(ox2,oy2,z);

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

   //   private double getBilinearPixel(Fits f,Coord coo,double myBlank) {
   //      if( !isIn(f,coo) ) return Double.NaN;
   //
   //      double x = coo.x;
   //      double y = coo.y;
   //
   //      int x1 = (int)x;
   //      int y1 = (int)y;
   //      int x2=x1+1;
   //      int y2=y1+1;
   //
   //      int ox1= x1;
   //      int oy1= y1;
   //      int ox2= x2;
   //      int oy2= y2;
   //
   //      if( x2<f.xCell || y2<f.yCell ||
   //        x1>=f.xCell+f.widthCell || y1>=f.yCell+f.heightCell ) return Double.NaN;
   //
   //      // Sur le bord, on dédouble le dernier pixel
   //      if( ox1==f.xCell-1 ) ox1++;
   //      if( oy1==f.yCell-1 ) oy1++;
   //      if( ox2==f.xCell+f.widthCell ) ox2--;
   //      if( oy2==f.yCell+f.heightCell ) oy2--;
   //
   //      double a0 = f.getPixelDouble(ox1,oy1);
   //      double a1 = f.getPixelDouble(ox2,oy1);
   //      double a2 = f.getPixelDouble(ox1,oy2);
   //      double a3 = f.getPixelDouble(ox2,oy2);
   //
   //      boolean b0 = Double.isNaN(a0) || a0==myBlank;
   //      boolean b1 = Double.isNaN(a1) || a1==myBlank;
   //      boolean b2 = Double.isNaN(a2) || a2==myBlank;
   //      boolean b3 = Double.isNaN(a3) || a3==myBlank;
   //
   //      if( b0 && b1 && b2 && b3 ) return Double.NaN;
   //      if( b0 || b1 || b2 || b3 ) {
   //         double a = !b0 ? a0 : !b1 ? a1 : !b2 ? a2 : a3;
   //         if( b0 ) a0=a;
   //         if( b1 ) a1=a;
   //         if( b2 ) a2=a;
   //         if( b3 ) a3=a;
   //      }
   //
   //      return bilineaire(x1,y1,x2,y2,x,y,a0,a1,a2,a3);
   //   }

   private int getBilinearPixelRGB(SrcFile srcFile,Coord coo) {
      Fits f = srcFile.fitsfile;
      if( !isIn(srcFile,coo) ) return 0;

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
      int b1 = f.getPixelRGBJPG(ox2,oy1);
      int b2 = f.getPixelRGBJPG(ox1,oy2);
      int b3 = f.getPixelRGBJPG(ox2,oy2);

      boolean c0 = (b0&0xFF000000)==0;
      boolean c1 = (b1&0xFF000000)==0;
      boolean c2 = (b2&0xFF000000)==0;
      boolean c3 = (b3&0xFF000000)==0;

      // Si les 4 pixels sont transparents, on retourne transparent
      if( c0 && c1 && c2 && c3 ) return 0;

      // Si l'un des 4 pixels n'est pas transparent, on remplace les transparents
      // par ce dernier
      if( c0 || c1 || c2 || c3 ) {
         int a = !c0 ? b0 : !c1 ? b1 : !c2 ? b2 : b3;
         if( c0 ) b0=a;
         if( c1 ) b1=a;
         if( c2 ) b2=a;
         if( c3 ) b3=a;
      }

      int pix=0xFF;
      for( int i=16; i>=0; i-=8 ) {
         double a0 = 0xFF & (b0>>i);
         double a1 = 0xFF & (b1>>i);
         double a2 = 0xFF & (b2>>i);
         double a3 = 0xFF & (b3>>i);
         int p = (int)(bilineaire(x1,y1,x2,y2,x,y,a0,a1,a2,a3)+0.5);
         if( p<0 ) p=0; else if( p>255 ) p=255;
         pix = (pix<<8) | p;
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
   private String getPath(String s) throws Exception {
      if( s.charAt(0)!='{' ) return s;   // Ancien format : un path par ligne
      int o = s.indexOf("path");
      int o1 = s.indexOf(':',o);
      int o2 = s.indexOf('"',o1+1);
      int o3 = s.indexOf('"',o2+1);
      return s.substring(o2+1,o3);
   }

   // Je fais au plus simple pour un simple test => il faudra utiliser une librairie JSON
   private long getCellMem(String s) throws Exception {
      int o = s.indexOf("cellmem");
      if( s.charAt(0)=='{' && o>-1 ) {
         int o1 = s.indexOf(':',o);
         int o2 = s.indexOf('"',o1+1);
         int o3 = s.indexOf('"',o2+1);
         try { 
            return Integer.parseInt( s.substring(o2+1,o3) ); 
         } catch( Exception e ) {}
      }
      return Constante.ORIGCELLWIDTH*Constante.ORIGCELLWIDTH* (context.bitpixOrig==0?32:Math.abs(context.bitpixOrig)/8);   // Approximation en l'absence de l'info
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
      String line = null;
      String fitsfilename = null;
      long cellMem;
      if (f.exists()) {
         BufferedReader reader = null;
         try {
            reader = new BufferedReader(new FileReader(f));
            for( int n=0; (line = reader.readLine()) != null; n++) {
               fitsfilename = getPath(line);
               cellMem = getCellMem(line);
               try {
                  Fits fitsfile = new Fits();
                  fitsfile.setFilename(fitsfilename);

                  SrcFile file = new SrcFile(fitsfilename,cellMem);
                  file.fitsfile = fitsfile;

                  downFiles.add(file);

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


   // retourne le polygone associé au fichier ou null si aucun
   // Soit il s'agit du même nom de fichier avec l'extension ".fov"
   // soit c'est le premier répertoire parent qui a un fichier associé avec l'extension ".fov"
   // sinon null
   private Polygon getPolygon(Fits fits) {
      File last = (new File( context.getInputPath() )).getParentFile();
      File dir = (new File( fits.getFilename() ));
      Polygon p;
      for( p=null; p==null && !dir.equals(last); dir = dir.getParentFile() ) {
         p = getPolygon(fits,dir);
      }
      return p;
   }


   // retourne le Polygone associé au nom de fichier ou de répertoire (extension ".fov")
   // (utilise une hashmap comme cache pour éviter les accès disques répétitifs)
   private Polygon getPolygon(Fits fits,File dir) {

      if( hashPolygon.containsKey(dir) ) return hashPolygon.get(dir);
      synchronized( hashPolygon ) {
         if( hashPolygon.containsKey(dir) ) return hashPolygon.get(dir);
         Polygon pol = null;
         String file;
         if( dir.isDirectory() ) file = dir.getAbsoluteFile()+".fov";
         else {
            String name = dir.getName();
            int dot = name.lastIndexOf('.');
            if( dot>0 ) name = name.substring(0,dot);
            file = dir.getParent()+Util.FS+name+".fov";
         }
         File f = new File(file);
         if( f.isFile() ) {
            FileInputStream fr = null;
            try {
               fr = new FileInputStream(f);
               InputStreamReader in = new InputStreamReader(fr);
               BufferedReader dis = new BufferedReader(in);
               String s;
               StringBuilder res = new StringBuilder();
               while( (s=dis.readLine())!=null ) {
                  if( res.length()>0 ) res.append(' ');
                  res.append(s.trim());
               }
               dis.close();
               in.close();
               pol = Context.createPolygon(res.toString());
               if( pol!=null ) context.info("FoV detected: "+f.getName()+" => "+pol2String(pol));
            }
            catch( Exception e) { e.printStackTrace(); }
            finally { if( fr!=null ) { try { fr.close(); } catch( Exception e1) {} } }
         }
         hashPolygon.put(dir,pol);
         return pol;
      }
   }

   private String pol2String(Polygon pol) {
      int i;
      StringBuilder rep= new StringBuilder();
      for( i=0; i<pol.npoints && i<5; i++ ) {
         if( i>0 ) rep.append(' ');
         rep.append(pol.xpoints[i]+","+pol.ypoints[i]);
      }
      if( i<pol.npoints ) rep.append("...");
      return rep.toString();
   }

   class SrcFile {
      Fits fitsfile;
      long cellMem;       // Mémoire requise pour ouvrir une cellule du fichier (en octets)
      int isOpened=-1;     // numero du frame ouvert, -1 si aucun
      String name=null;
      double blank;
      Polygon polygon=null;
      boolean flagRemoved=false;   // true si ce fichier est supprimé a posteriori

      SrcFile(String name,long cellMem) {
         this.name=name;
         this.cellMem=cellMem;
      }

      @Override
      public String toString() {
         return fitsfile.getFilename();
      }

      /** Ouverture effective du fichier FITS */
      protected void open(int frame) throws Exception {
         if( isOpened==frame ) return;
         if( isOpened!=-1 ) fitsfile.rmUser();  // je ne peux de totue façon pas ouvrir simultanément plusieurs frame du même fichier
         int mode = (name.endsWith(".hhh") || name.indexOf(".hhh[")>0) ? CacheFits.HHH
               : (name.endsWith(".jpg") || name.indexOf(".jpg[")>0) ? CacheFits.JPEG
                     : (name.endsWith(".png") || name.indexOf(".png[")>0) ? CacheFits.PNG
                           : CacheFits.FITS;

         // Mode FITS couleur
         if( mode==CacheFits.FITS && bitpix==0 ) fitsfile.loadFITS(name,true,true);

         // Mode normal
         else {
            if( context.depth>1 || frame>0 ) name = addFrameToName(name,frame);
            try {
               fitsfile=context.cacheFits.getFits(name,mode,true,false);
            } catch( MyInputStreamCachedException e ) {
               context.taskAbort();
               throw new Exception();
            }
         }

         // Faut-il associer un Polygon particulier
         if( context.scanFov ) polygon = getPolygon(fitsfile);

         isOpened=frame;
         fitsfile.addUser();
         MyInputStreamCached.incActiveFile(name);

         blank = !hasAlternateBlank ? fitsfile.blank : blankOrig;
      }
      
      /** Libération des ressources associées à la geston de ce fichier Fits */
      protected void release() {
         fitsfile.rmUser();
         MyInputStreamCached.decActiveFile(name);
         isOpened=-1;
      }

      // J'ai [ext;x,y-wxh] et je veux [ext;x,y,z-w*h*d]
      String addFrameToName(String name,int frame) throws Exception {
         Fits tmp = new Fits();
         tmp.loadHeaderFITS(name);
         tmp.zCell=frame;
         tmp.depthCell=1;
         return tmp.getFileNameExtended();
      }

   }


   int n =0;

}

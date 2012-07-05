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

import cds.aladin.Aladin;
import cds.allsky.Context.JpegMethod;
import cds.fits.Fits;
import cds.tools.pixtools.Util;

/** Construction de la hiérarchie des tuiles FITS à partir des tuiles de plus bas
 * niveau. La méthode employée est la moyenne.
 * @author Pierre Fernique
 */
public class BuilderTree extends Builder {

   private int maxOrder;
   private int bitpix;
   private int width;
   private double blank,bscale,bzero;
   boolean flagAllsky=true;

   private int statNbFile;
   private long statSize;
   private long startTime,totalTime;

   /**
    * Création du générateur de l'arbre FITS.
    * @param context
    */
   public BuilderTree(Context context) {
      super(context);
   }

   public Action getAction() { return Action.TREE; }

   public void run() throws Exception {
      build();
      if( flagAllsky && !context.isTaskAborting() ) (new BuilderAllsky(context)).run();
   }
   
   // Valide la cohérence des paramètres pour la création des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      if( !context.isExistingAllskyDir() ) throw new Exception("No Fits tile found");
      validateOrder(context.getOutputPath());      
      try {
         validateCut();
         context.initParameters();
      } catch( Exception e ) {
         context.warning("No pixel cut information => you will have to (re)create the allsky.fits separately !");
         flagAllsky=false;
         context.initRegion();
      }
   }
   
   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, statSize, totalTime);
   }

   public void build() throws Exception {
      initStat();
      context.setProgressMax(768);
      String output = context.getOutputPath();
      maxOrder = context.getOrder();
      
      for( int i=0; i<768; i++ ) {
//         if( context.isInMocTree(3, i) ) createTree(output,3,i);
         createTree(output,3,i);
         context.setProgress(i);
      }
   }
   
   private void initStat() { statNbFile=0; statSize=0; startTime = System.currentTimeMillis(); }

   // Mise à jour des stats
   private void updateStat(File f) {
      statNbFile++;
      statSize += f.length();
      totalTime = System.currentTimeMillis()-startTime;
   }

   /** Construction récursive de la hiérarchie des tuiles FITS à partir des tuiles FITS
    * de plus bas niveau. La méthode employée est la moyenne
    */
   private Fits createTree(String path,int order, long npix ) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      
//      if( !context.isInMocTree(order,npix) ) return null;
      
      // Si ni lui, ni ses frères sont dans le MOC, on passe
      boolean ok=false;
      long brother = npix - npix%4L;
      for( int i=0; i<4; i++ ) {
         ok = context.isInMocTree(order,brother+i);
         if( ok ) break;
      }
      if( !ok ) return null;
      
      String file = Util.getFilePath(path,order,npix);
//      JpegMethod method = context.getJpegMethod();
      JpegMethod method = JpegMethod.MEAN;

      // S'il n'existe pas le fits, c'est une branche morte
//      if( !new File(file+".fits").exists() ) return null;

      Fits out = null;
      if( order==maxOrder ) out = createLeaveFits(file);
      else {
         Fits fils[] = new Fits[4];
         boolean found = false;
         for( int i =0; i<4; i++ ) {
            fils[i] = createTree(path,order+1,npix*4+i);
            if (fils[i] != null && !found) found = true;
         }
         if( found ) out = createNodeFits(fils, method);
      }
      if( out!=null && context.isInMocTree(order,npix) ) {
         out.writeFITS(file+".fits");
         Aladin.trace(4, "Writing " + file+".fits");

         if( order==maxOrder ) {
            File f = new File(file+".fits");
            updateStat(f);
         }
      }
      return out;
   }
   
   /** Construction d'une tuile terminale. De fait, simple chargement
    * du fichier FITS correspondant. */
   private Fits createLeaveFits(String file) throws Exception {
      Fits out = null;
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      try {
         out = new Fits();
         out.loadFITS(file+".fits");
         if( first ) { first=false; setConstantes(out); }
      } catch( Exception e ) { out=null; }
      return out;
   }

   private boolean first=true;
   private void setConstantes(Fits f) {
      bitpix = f.bitpix;
      blank  = f.blank;
      bscale = f.bscale;
      bzero  = f.bzero;
      width  = f.width;
   }

   /** Construction d'une tuile intermédiaire à partir des 4 tuiles filles */
   private Fits createNodeFits(Fits fils[], JpegMethod method) throws Exception {
      
      Fits out = new Fits(width,width,bitpix);
      out.setBlank(blank);
      out.setBscale(bscale);
      out.setBzero(bzero);

      Fits in;
      double p[] = new double[4];
      double coef[] = new double[4];

      for( int dg=0; dg<2; dg++ ) {
         for( int hb=0; hb<2; hb++ ) {
            int quad = dg<<1 | hb;
            in = fils[quad];
            int offX = (dg*width)/2;
            int offY = ((1-hb)*width)/2;

            for( int y=0; y<width; y+=2 ) {
               for( int x=0; x<width; x+=2 ) {

                  double pix=blank;
                  if( in!=null ) {

                     // On prend la moyenne (sans prendre en compte les BLANK)
                     if( method==Context.JpegMethod.MEAN ) {
                        double totalCoef=0;
                        for( int i=0; i<4; i++ ) {
                           int dx = i==1 || i==3 ? 1 : 0;
                           int dy = i>=2 ? 1 : 0;
                           p[i] = in.getPixelDouble(x+dx,y+dy);
                           if( in.isBlankPixel(p[i]) ) coef[i]=0;
                           else coef[i]=1;
                           totalCoef+=coef[i];
                        }
                        if( totalCoef!=0 ) {
                           pix=0;
                           for( int i=0; i<4; i++ ) {
                              if( coef[i]!=0 ) pix += p[i]*(coef[i]/totalCoef);
                           }
                        }

                        // On garde la valeur médiane (les BLANK seront automatiquement non retenus)
                     } else {

                        double p1 = in.getPixelDouble(x,y);
                        if( in.isBlankPixel(p1) ) p1=Double.NaN;
                        double p2 = in.getPixelDouble(x+1,y);
                        if( in.isBlankPixel(p2) ) p1=Double.NaN;
                        double p3 = in.getPixelDouble(x,y+1);
                        if( in.isBlankPixel(p3) ) p1=Double.NaN;
                        double p4 = in.getPixelDouble(x+1,y+1);
                        if( in.isBlankPixel(p4) ) p1=Double.NaN;

                        if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix=p1;
                        else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix=p2;
                        else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix=p3;
                        else pix=p4;
                     }
                  }

                  out.setPixelDouble(offX+(x/2), offY+(y/2), pix);
               }
            }
         }
      }

      for( int i=0; i<4; i++ ) {
         if( fils[i]!=null ) fils[i].free();
      }
      return out;
   }

}
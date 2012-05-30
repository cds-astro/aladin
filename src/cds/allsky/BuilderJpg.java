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

import java.awt.image.ColorModel;
import java.io.File;

import cds.aladin.Aladin;
import cds.aladin.ColorMap;
import cds.allsky.Context.JpegMethod;
import cds.fits.Fits;
import cds.tools.pixtools.Util;

/** Construction de la hiérarchie des tuiles JPEG à partir des tuiles FITS de plus bas
 * niveau. La méthode employée est soit la médiane soit la moyenne pour passer des 4 pixels de niveau
 * inférieur au pixel de niveau supérieur (fait disparaitre peu à peu les étoiles
 * faibles). Le passage en 8 bits se fait, soit par une table de couleur (cm) fournie,
 * soit par une intervalle (cut[]).
 * @author Anaïs Oberto & Pierre Fernique
 */
public class BuilderJpg extends Builder {

   private int maxOrder;
   private double[] cut;
   private byte [] tcm;
   private int bitpix;
   private int width;
   private double blank,bscale,bzero;

   private int statNbFile;
   private long statSize;
   private long startTime,totalTime;

   /**
    * Création du générateur JPEG.
    * @param cut borne de l'intervalle pour le passage en 8 bits (uniquement si cm==null)
    * @param cm table des couleurs pour le passage en 8 bits (prioritaire sur cut), 
    * @param context
    */
   public BuilderJpg(Context context) {
      super(context);
      ColorModel cm = context.fct==null ? null : ColorMap.getCM(0, 128, 255,false, 0/*PlanImage.CMGRAY*/, context.fct.code());
      tcm = cm==null ? null : cds.tools.Util.getTableCM(cm,2);
   }

   public Action getAction() { return Action.JPEG; }

   public void run() throws Exception {
      double cut [] = context.getCut();
      String fct = context.getTransfertFct();
      context.info("Map pixel cut ["+cut[0]+" .. "+cut[1]+"] to [0..255] ("+fct+") jpegMethod="+context.getJpegMethod());
      build();
      if( !context.isTaskAborting() ) (new BuilderAllsky(context)).run();
   }
   
   public boolean isAlreadyDone() {
      if( context.isColor() ) {
         context.info("Jpeg conversion not required for Healpix colored survey");
         return true;
      }
      if( !context.actionPrecedeAction(Action.INDEX, Action.TILES)) return false;
      if( !context.actionPrecedeAction(Action.TILES, Action.JPEG)) return false;
      context.info("Pre-existing HEALPix JPEG survey seems to be ready");
      return true;
   }

   // Valide la cohérence des paramètres pour la création des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      if( !context.isExistingAllskyDir() ) throw new Exception("No Fits tile found");
      validateOrder(context.getOutputPath());      
      validateCut();
      
      context.initParameters();
   }
   
   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, statSize, totalTime);
   }

   // Initialise la valeur du BSCALE BZERO de sortie en fonction du premier fichier Npixxxx.fits trouvé dans Norder3/Dir0
   // Nécessaire dans le cas de relance juste pour le calcul des JPEG car ces valeurs n'ont 
   // jamais été initialisées dans ce cas.
   private void initBscaleBzeroFromNpixFits(String path) {
      if( context.isBScaleBZeroSet() ) return; // inutile
      try {
         File f1 = null;
         for( int i=0; i<768; i++ ) {
            f1 = new File( Util.getFilePath(path, 3, i)+".fits" );
            if( f1.exists() ) break;
         }
         Fits f = new Fits();
         f.loadHeaderFITS(f1.getAbsolutePath());
         double bscale=1;
         double bzero=0;
         try { bscale = f.headerFits.getDoubleFromHeader("BSCALE"); } catch( Exception e ) { }
         try { bzero = f.headerFits.getDoubleFromHeader("BZERO"); } catch( Exception e ) { }
         Aladin.trace(4,"BuilderJpg.initBscaleBzeroFromNpixFits()... reinit target BZERO="+bzero+", BSCALE="+bscale);
         context.setBScale(bscale);
         context.setBZero(bzero);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   public void build() throws Exception {
      initStat();
      context.setProgressMax(768);
      String output = context.getOutputPath();
      maxOrder = context.getOrder();
      cut = context.getCut();
      
      JpegMethod method = context.getJpegMethod();
      
      // par la médiane, il faut repartir des losanges FITS de niveaux le plus bas
      if( method==JpegMethod.MEDIAN ) {
         for( int i=0; i<768; i++ ) {
            if( context.isInMocTree(3, i) ) createJpg(output,3,i);
            context.setProgress(i);
         }
         
      // Par la moyenne, on peut accélérer les choses en se contentant
      // de convertir tous les fichiers Fits trouvés
      } else fits2JpgDir(new File(output));
      
   }
   
   private void initStat() { statNbFile=0; statSize=0; startTime = System.currentTimeMillis(); }

   // Mise à jour des stats
   private void updateStat(File f) {
      statNbFile++;
      statSize += f.length();
      totalTime = System.currentTimeMillis()-startTime;
   }
   
   public boolean mustBeTranslated(File f) {
      String name = f.getName();
      if( name.equals("Allsky.fits") ) return true;
      if( !name.endsWith(".fits") )    return false;
      if( !name.startsWith("Npix") )   return false;
      return true;
   }
   
   // Conversion de toute l'arborescence FITS en JPEG (nécessairement méthode
   // de la moyenne (comme pour le FITS))
   private void fits2JpgDir(File dir) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      
      // répertoire
      if( dir.isDirectory() ) {
         for ( File f : dir.listFiles() ) fits2JpgDir(f);
         
      // Fichier
      } else {
         int order = Util.getOrderFromPath(dir.getCanonicalPath());
         if( order!=-1 && mustBeTranslated(dir) ) {
            String file = dir.getCanonicalPath();
            file = file.substring(0,file.lastIndexOf('.'));
            fits2jpeg(file);
            if( order==maxOrder ) {
               File f = new File(file+".jpg");
               updateStat(f);
            }
         }
      }
   }
   
   // Conversion d'un fichier de FITS en JEPG (file sans l'extension)
   private void fits2jpeg(String file) throws Exception {
      Fits out = createLeaveJpg(file);
      if( tcm==null ) out.toPix8(cut[0],cut[1]);
      else out.toPix8(cut[0],cut[1],tcm);
      out.writeJPEG(file+".jpg");
      Aladin.trace(4, "Writing " + file+".jpg");
   }

   /** Construction récursive de la hiérarchie des tuiles JPEG à partir des tuiles FITS
    * de plus bas niveau. La méthode employée est la moyenne ou la médiane sur les 4 pixels de niveau inférieurs
    */
   private Fits createJpg(String path,int order, long npix ) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      String file = Util.getFilePath(path,order,npix);
      JpegMethod method = context.getJpegMethod();

      // S'il n'existe pas le fits, c'est une branche morte
      if( !new File(file+".fits").exists() ) return null;

      Fits out = null;
      if( order==maxOrder ) out = createLeaveJpg(file);
      else {
         Fits fils[] = new Fits[4];
         boolean found = false;
         for( int i =0; i<4; i++ ) {
            fils[i] = createJpg(path,order+1,npix*4+i);
            if (fils[i] != null && !found) found = true;
         }
         if( found ) out = createNodeJpg(fils, method);
      }
      if( out!=null && context.isInMocTree(order,npix) ) {
         if( debugFlag ) {
            debugFlag=false;
            Aladin.trace(3,"Creating JPEG tiles: method="+(method==Context.JpegMethod.MEAN?"average":"median")
                  +" maxOrder="+maxOrder+" bitpix="+bitpix+" blank="+blank+" bzero="+bzero+" bscale="+bscale
                  +" cut="+(cut==null?"null":cut[0]+".."+cut[1])
                  +" tcm="+(tcm==null?"null":"provided"));
         }
         if( tcm==null ) out.toPix8(cut[0],cut[1]);
         else out.toPix8(cut[0],cut[1],tcm);
         out.writeJPEG(file+".jpg");
         Aladin.trace(4, "Writing " + file+".jpg");

         if( order==maxOrder ) {
            File f = new File(file+".jpg");
            updateStat(f);
         }
      }
      return out;
   }

   private boolean debugFlag=true;

   /** Construction d'une tuile terminale. De fait, simple chargement
    * du fichier FITS correspondant. */
   private Fits createLeaveJpg(String file) throws Exception {
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
   private Fits createNodeJpg(Fits fils[], JpegMethod method) throws Exception {
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
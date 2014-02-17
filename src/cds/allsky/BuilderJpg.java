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
public class BuilderJpg extends BuilderTiles {

   private double[] cut;
   protected byte [] tcm;
   private int bitpix;
   private int width;
   private double blank,bscale,bzero;

   private int statNbFile;
   
   protected String fmt;
   protected String ext;

   /**
    * Création du générateur JPEG.
    * @param cut borne de l'intervalle pour le passage en 8 bits (uniquement si cm==null)
    * @param cm table des couleurs pour le passage en 8 bits (prioritaire sur cut), 
    * @param context
    */
   public BuilderJpg(Context context) {
      super(context);
      init();
   }
   
   protected void init() {
      fmt = "jpeg";
      ext = ".jpg";
   }

   public Action getAction() { return Action.JPEG; }
   
   // Valide la cohérence des paramètres pour la création des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      if( !context.isExistingAllskyDir() ) throw new Exception("No Fits tile found");
      validateOrder(context.getOutputPath());      
      if( !context.isColor() ) {
         
         try { validateCut(); }
         catch( Exception e ) {
            try {
               setFitsParamFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
               context.info("Will use pixelCut ["+context.cutOrig[0]+" .. "+context.cutOrig[1]+"], " +
                    "BLANK="+context.blank+" BZERO="+context.bzero+" BSCALE="+context.bscale+" found in Allsky.fits");
            } catch( Exception e1 ) {
               throw new Exception("Pixel cut unkown => use pixelcut parameter");
            }
         }
      }
      
      // Chargement du MOC réel à la place de celui de l'index (moins précis)
      try { context.loadMoc(); } catch( Exception e ) {
         context.warning("Tile MOC not found => use index MOC");
      }
      
      context.initParameters();
   }
   
   
   protected int getMinCM() { return 0; }

   public void run() throws Exception {
      ColorModel cm = context.fct==null ? null : ColorMap.getCM(0, 128, 255,false, 
            0/*PlanImage.CMGRAY*/, context.fct.code());
      tcm = cm==null ? null : cds.tools.Util.getTableCM(cm,2);
      cut = context.getCut();
      if( context.bzero!=0 || context.bscale!=1 ) {
         double [] val = { cut[0]*context.bscale+context.bzero, cut[1]*context.bscale+context.bzero };
         context.info("Map pixel cut ["+cut[0]+"/"+val[0]+" .. "+cut[1]+"/"+val[1]+"] to ["+getMinCM()+"..255] ("+context.getTransfertFct()+")");
      } else  {
         context.info("Map pixel cut ["+cut[0]+" .. "+cut[1]+"] to ["+getMinCM()+"..255] ("+context.getTransfertFct()+")");

      }
      context.info("Tile aggregation method="+context.getJpegMethod());
      build();
      if( !context.isTaskAborting() ) {
         (new BuilderAllsky(context)).createAllSkyColor(context.getOutputPath(),3,fmt,64);
         context.writePropertiesFile();
         if( context instanceof ContextGui && ((ContextGui) context).mainPanel.planPreview!=null ) {
            if( fmt.equals("jpeg") ) ((ContextGui) context).mainPanel.planPreview.inJPEG = true;
            else ((ContextGui) context).mainPanel.planPreview.inPNG = true;
         }
      }
   }
   
   public boolean isAlreadyDone() {
      if( context.isColor() ) {
         context.info("Jpeg conversion not required for Healpix colored survey");
         return true;
      }
      if( !context.actionPrecedeAction(Action.INDEX, Action.TILES)) return false;
      if( !context.actionPrecedeAction(Action.TILES, getAction())) return false;
      context.info("Pre-existing HEALPix JPEG survey seems to be ready");
      return true;
   }

   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, totalTime,statNbThread,statNbThreadRunning);
      if( !(context instanceof ContextGui ) ) super.showStatistics();
   }

   public void build() throws Exception {
      initStat();
      super.build();
   }
   
   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,int order,long npix) throws Exception {
      Fits out = createLeaveJpg(file);
      if( out==null ) return null;
      
      out.writeCompressed(file+ext,cut[0],cut[1],tcm,fmt);
      Aladin.trace(4, "Writing " + file+ext);
      updateStat();
      return out;
   }
   
   protected Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[]) throws Exception {
      JpegMethod method = context.getJpegMethod();
      Fits out = createNodeJpg(fils, method);
      if( out==null ) return null;
      out.writeCompressed(file+ext,cut[0],cut[1],tcm,fmt);
      Aladin.trace(4, "Writing " + file+ext);
      return out;
   }
   
   /** Mise à jour de la barre de progression en mode GUI */
   protected void setProgressBar(int npix) { context.setProgress(npix); }

   
   private void initStat() {
      context.setProgressMax(768);
      statNbFile=0; 
      startTime = System.currentTimeMillis();
   }

   // Mise à jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }
   
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
// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.image.ColorModel;

import cds.aladin.Aladin;
import cds.aladin.CanvasColorMap;
import cds.aladin.MyInputStream;
import cds.fits.Fits;

/** Construction de la hi�rarchie des tuiles JPEG � partir des tuiles FITS de plus bas
 * niveau. La m�thode employ�e est soit la m�diane soit la moyenne pour passer des 4 pixels de niveau
 * inf�rieur au pixel de niveau sup�rieur (fait disparaitre peu � peu les �toiles
 * faibles). Le passage en 8 bits se fait, soit par une table de couleur (cm) fournie,
 * soit par une intervalle (cut[]).
 * @author Ana�s Oberto & Pierre Fernique
 */
public class BuilderJpg extends BuilderRunner {

   private double[] cut;
   protected byte [] tcm;
   private int bitpix;
   private int width;
   private double blank,bscale,bzero;
   protected ModeTree modeHierarchy=null;

   private int statNbFile;

   protected String fmt;
   protected String ext;

   /**
    * Cr�ation du g�n�rateur JPEG.
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

   // Valide la coh�rence des param�tres pour la cr�ation des tuiles JPEG
   public void validateContext() throws Exception {
      if( context.cutByImage ) super.validateContext();
      else {
         validateOutput();
         if( !context.isExistingAllskyDir() ) throw new Exception("No Fits tile found");
         validateOrder(context.getOutputPath());
         validateDepth();
         if( !context.isColor() ) validateCut();
         if( context.cutByRegion ) doCutIfRequired();

         modeHierarchy = context.getModeTree();
         context.info("Hierarchy mode (tree): "+ModeTree.getExplanation(modeHierarchy));

         // Chargement du MOC r�el � la place de celui de l'index (moins pr�cis)
         try { context.loadMoc(); } catch( Exception e ) {
            context.warning("Tile MOC not found => use index MOC");
         }

         // reprise du frame si n�cessaire depuis le fichier de propri�t�
         if( !context.hasFrame() ) context.setFrameName( getFrame() );

         context.initRegion();
      }
   }

   protected String getTileExt() { return ext; }

   protected int getMinCM() { return 0; }

   public void run() throws Exception {
      context.resetCheckCode(fmt);
      build();
   }
   
   /**
    * G�n�re les cuts par r�gion si n�cessaire
    * @throws Exception
    */
   private void doCutIfRequired() throws Exception {
      try {
         String file = context.findOneNpixFile(context.getOutputPath(),"fits",true);
         Fits fits = new Fits(file);
         fits.headerFits.getDoubleFromHeader("CUTMIN");
         
      // si absence de CUTMIN dans une des tuiles d'ordre MAX, �a veut dire qu'il faut 
      // g�n�rer les cuts par r�gion
      } catch( Exception e ) {
         (b=new BuilderCut(context)).run();
         b=null;
      }
   }

   public boolean isAlreadyDone() {
      if( context.isColor() ) {
         context.info(fmt+" conversion not required for Healpix colored survey");
         return true;
      }
      if( !context.actionPrecedeAction(Action.INDEX, Action.TILES)) return false;
      if( !context.actionPrecedeAction(Action.TILES, getAction())) return false;
      context.info("Pre-existing HEALPix "+fmt+" survey seems to be ready");
      return true;
   }

   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      if( context.cutByImage ) super.showStatistics();
      else {
         if( statNbFile>0 ) context.showJpgStat(statNbFile, totalTime, getNbThreads(), getNbThreadRunning() );
         if( !(context instanceof ContextGui ) ) super.showStatistics();
      }
   }

   public void build() throws Exception {
      initStat();
      super.build();
   }
   
   protected void buildPre() throws Exception { 
      if( context.cutByImage ) super.buildPre();
      else {
         ColorModel cm = context.getFct()==null ? null : CanvasColorMap.getCM(0, 128, 255,false,
               0/*PlanImage.CMGRAY*/, context.getFct().code());
         tcm = cm==null ? null : cds.tools.Util.getTableCM(cm,2);
         cut = context.getCut();
         double bz = context.bzero;
         double bs = context.bscale;
         if( !context.cutByImage ) context.info("Map pixel cut ["+(context.cutByRegion?"by region":ip(cut[0],bz,bs)+" .. "+ip(cut[1],bz,bs))
               +"] to ["+getMinCM()+"..255] ("+context.getTransfertFct()+")");
      }
   }
   
   protected void buildPost(long duree) throws Exception { 
      if( context.cutByImage ) super.buildPost(duree); 
      else {
         (new BuilderAllsky(context)).runJpegOrPngOnly(fmt);
         if( context instanceof ContextGui && ((ContextGui) context).mainPanel.planPreview!=null ) {
            if( fmt.equals("jpeg") ) ((ContextGui) context).mainPanel.planPreview.inJPEG = true;
            else ((ContextGui) context).mainPanel.planPreview.inPNG = true;
         }
      }
   }
   
   protected void activateCache(long size,long sizeCache) { }
   
   protected Fits createLeafHpx(ThreadBuilderTile hpx, String file,String path,int order,long npix, int z) throws Exception {
      Fits out = null;
      if( context.cutByImage ) {
         out = super.createLeafHpx(hpx, file, path, order, npix, z);
         if( out!=null && first ) { first=false; setConstantes(out); }
         
      } else out = createLeafJpg(file);
      
      if( out==null ) return null;

      double cutmin,cutmax;
      if( context.cutByRegion || context.cutByImage ) { 
         cutmin=1;
         cutmax=255;
      } else {
         cutmin=cut[Context.CUTMIN];
         cutmax=cut[Context.CUTMAX];
      }
      out.writeCompressed(file+ext,cutmin,cutmax,tcm,fmt);
      Aladin.trace(4, "Writing " + file+ext);
      updateStat();
      return out;
   }
   
   protected void writeLeaf(Fits out,String file,int order,long npix) throws Exception { }

   protected Fits createNodeHpx(String file,String path,int order,long npix,Fits fils[], int z) throws Exception {
      Fits out = createNodePreview(fils, modeHierarchy );
      if( out==null ) return null;
      double cutmin,cutmax;
      if( context.cutByRegion || context.cutByImage ) {
         cutmin=1;
         cutmax=255;
      } else {
         cutmin=cut[Context.CUTMIN];
         cutmax=cut[Context.CUTMAX];
      }
      out.writeCompressed(file+ext,cutmin,cutmax,tcm,fmt);
      Aladin.trace(4, "Writing " + file+ext);
      return out;
   }

   /** Mise � jour de la barre de progression en mode GUI */
   protected void setProgressBar(int npix) { context.setProgress(npix); }

   private void initStat() {
      context.setProgressMax(768);
      statNbFile=0;
      startTime = System.currentTimeMillis();
   }

   // Mise � jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }
   
   protected void loadLeaf(Fits out,MyInputStream is) throws Exception {
      out.loadPreview(is, true);
   }

   /** Construction d'une tuile terminale. De fait, simple chargement
    * du fichier FITS correspondant. */
   private Fits createLeafJpg(String file) throws Exception {
      Fits out = null;
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      try {
         out = new Fits();
         out.loadFITS(file+".fits");
         if( first ) { first=false; setConstantes(out); }
         if( context.cutByRegion ) {
            Fits f8 = new Fits(out.width,out.height,8);
            f8.setBlank(0);
            try { 
               double cutmin = out.headerFits.getDoubleFromHeader("CUTMIN");
               double cutmax = out.headerFits.getDoubleFromHeader("CUTMAX");
               byte [] buf = out.toPix8(cutmin, cutmax, null, Fits.PIX_TRUE);
               Fits.invImageLine(f8.width, f8.height, buf);
               f8.pixels = buf;
               out=f8;
            } catch( Exception e ) {
               throw new Exception("Cut by region error [missing CUTMIN,CUTMAX FITS header values] => launch CUT action before");
            }
         }
    
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
   
   private void heriteCutValues(Fits out,Fits fils[]) {
      for( Fits f : fils ) {
         if( f==null ) continue;
         try {
            out.headerFits.setKeyValue("CUTMIN", f.headerFits.getStringFromHeader("CUTMIN"));
            out.headerFits.setKeyValue("CUTMAX", f.headerFits.getStringFromHeader("CUTMAX"));
            return;
         } catch( Exception e) { }
      }
   }

   /** Construction d'une tuile interm�diaire � partir des 4 tuiles filles */
   private Fits createNodePreview(Fits fils[], ModeTree modeTree) throws Exception {
      if( width==0 || fils[0]==null && fils[1]==null && fils[2]==null && fils[3]==null ) return null;
      Fits out = new Fits(width,width,bitpix);
      out.setBlank(blank);
      out.setBscale(bscale);
      out.setBzero(bzero);
      if( context.cutByRegion ) heriteCutValues(out,fils);

      Fits in;
      double pixd[] = new double[4];
      
      for( int dg=0; dg<2; dg++ ) {
         for( int hb=0; hb<2; hb++ ) {
            int quad = dg<<1 | hb;
            in = fils[quad];
            int offX = (dg*width)>>>1;
            int offY = ((1-hb)*width)>>>1;

            for( int y=0; y<width; y+=2 ) {
               for( int x=0; x<width; x+=2 ) {
                  
                  double pix=blank;
                  int nbPix=0;
                  if( in!=null ) {
                     for( int i=0;i<4; i++ ) {
                        int gx = i==1 || i==3 ? 1 : 0;
                        int gy = i>1 ? 1 : 0;
                        pixd[nbPix] = in.getPixelDouble(x+gx,y+gy);
                        if( !in.isBlankPixel(pixd[nbPix]) ) {
                           nbPix++;
                           if( modeTree==ModeTree.treeFirst ) break;
                        }
                     }
                     if( nbPix==0 ) pix=blank;  // aucune valeur => BLANK
                     else {
                        if( modeTree==ModeTree.treeMean ) pix = getMean(pixd, nbPix);
                        else if( modeTree==ModeTree.treeMedian )  pix = getMedian(pixd, nbPix);
                        else if( modeTree==ModeTree.treeMiddle )  pix = getMiddle(pixd, nbPix);
                        else pix = pixd[0];
                     }
                  }
                  out.setPixelDouble(offX+(x>>>1), offY+(y>>>1), pix);
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

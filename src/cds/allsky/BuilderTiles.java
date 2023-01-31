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

import cds.aladin.Aladin;
import cds.fits.CacheFits;

/** Permet la génération du survey HEALPix à partir d'un index préalablement généré
 * @author Standard Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderTiles extends BuilderRunner {
   
   public BuilderTiles(Context context) { super(context); }

   public Action getAction() { return Action.TILES; }
   
   protected void buildPre() {
      long size = context.getMem();
      int npixOrig = context.getNpixOrig();

      // Initialisation du cache en lecture
      long limitMem = 2*size/3L;
      int limitFile = 5000;

      int maxFile;
      long maxMem;

      // Dans le cas d'un relevé JPEG ou PNG, on va utiliser un simple cache
      if( context.isColor() || !context.isPartitioning() ) {
         maxFile=limitFile;
         maxMem=limitMem;

      // Sinon, on va paramètrer le cache pour être dynamique et relativement petit au démarrage
      } else {
         maxFile = nbThread * Constante.MAXOVERLAY;
         if( maxFile<300 ) maxFile=300;
//         int bloc = context.isPartitioning() ? context.getPartitioning() : Constante.ORIGCELLWIDTH;
//         maxMem = (long)maxFile * bloc * bloc * npixOrig;
         
         // si partitionné => si statmax inconnue getPartitioning, sinon min(getPartitioning,statMax)
         int maxWidth  = context.statMaxWidth;
         int maxHeight = context.statMaxHeight;
         int bloc = context.getPartitioning();
         maxWidth  = maxWidth==-1  ? bloc : Math.min(bloc,maxWidth);
         maxHeight = maxHeight==-1 ? bloc : Math.min(bloc,maxHeight);
         maxMem = (long)maxFile * maxWidth * maxHeight * npixOrig;
      }

      CacheFits cache = new CacheFits(maxMem,maxFile,limitMem, limitFile);
      context.setCache( cache );
      context.info("Available RAM: "+cds.tools.Util.getUnitDisk(size)+" => RAM cache size: "+cache.getMaxFile()
          +" items / "+ cds.tools.Util.getUnitDisk( cache.getMaxMem()));
   }
   
   protected void buildPost(long duree) throws Exception {
      if( ThreadBuilderTile.statMaxOverlays>0 )
         context.stat("Tile overlay stats : max overlays="+ThreadBuilderTile.statMaxOverlays+", " +
               ThreadBuilderTile.statOnePass+" in one step, "+
               ThreadBuilderTile.statMultiPass+" in multi steps");
      if( context.cacheFits!=null ) {
         Aladin.trace(4,"Cache FITS status: "+ context.cacheFits);
         context.cacheFits.reset();
         context.setCache(null);
      }
      if( context.trimMem>0 ) context.stat("Tiles trim method saves "+cds.tools.Util.getUnitDisk(context.trimMem,1,2));

      infoCounter(duree);
   }


   /** Affichage des compteurs (s'ils ont été utilisés) */
   private void infoCounter(long duree) {
      
      if( duree>1000L && (context.statPixelIn>0 || context.statPixelOut>0) ) {
         long d = duree/1000L;
         context.stat("Pixel times: "+
               (context.statPixelIn==0?"":"Original images="+cds.tools.Util.getUnitDisk(context.statPixelIn).replace("B","pix") 
                  + " => "+cds.tools.Util.getUnitDisk(context.statPixelIn/d).replace("B","pix")+"/s")
               + (context.statPixelOut==0?"":
                 ("  Low tiles="+cds.tools.Util.getUnitDisk(context.statPixelOut).replace("B","pix") 
               + (context.statPixelIn==0?"":" (x"+cds.tools.Util.myRound((double)context.statPixelOut/context.statPixelIn)+")")
               + " => "+cds.tools.Util.getUnitDisk(context.statPixelOut/d).replace("B","pix")+"/s") )
               );
     
      }
   }

}

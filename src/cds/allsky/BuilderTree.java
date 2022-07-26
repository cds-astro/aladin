// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import cds.fits.Fits;

/** Permet la génération du survey HEALPix à partir d'un index préalablement généré
 * @author Standard Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderTree extends BuilderTiles {

   public BuilderTree(Context context) { super(context); }

   public Action getAction() { return Action.TREE; }

   public void run() throws Exception {
      context.info("Creating "+context.getTileExt()+" tree and allsky (max depth="+context.getOrder()+")...");
      context.info("sky area to process: "+context.getNbLowCells()+" low level HEALPix cells");
      build();
      context.resetCheckCode();
      if( !context.isTaskAborting() ) { (new BuilderAllsky(context)).run(); context.done("ALLSKY file done"); }
   }
   

   // Valide la cohérence des paramètres
   public void validateContext() throws Exception {
      validateOutput();
      if( !context.isExistingAllskyDir() ) throw new Exception("No tile found");
      validateOrder(context.getOutputPath());
      validateTileSide(context.getOutputPath());

      try { context.loadMoc(); }
      catch( Exception e ) {
         (new BuilderMoc(context)).run();
         context.info("MOC rebuilt from low rhombs");
         context.loadMoc();
      }
      context.initRegion();
   }

   private boolean first=true;
   protected void setConstantes(Fits f) {
      first=false;
      if( context.isColor() ) return;
      context.bitpix = bitpix = f.bitpix;
      context.blank  = blank  = f.blank;
      context.bzero  = bzero  = f.bzero;
      context.bscale = bscale = f.bscale;
      if( context.bitpix!=0 ) context.info("Found in first low rhomb: BITPIX="+bitpix+" BLANK="+blank+" BZERO="+bzero+" BSCALE="+bscale);
      else context.info("Colored pixels found in first low rhomb");
   }
   
   public Fits findLeaf(String file) throws Exception {
      Fits f = super.findLeaf(file);
      if( first && f!=null ) setConstantes(f);
      return f;
   }

   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,String path,int order,long npix, int z) throws Exception {
      long t = System.currentTimeMillis();
      Fits f = findLeaf(file);

      long duree = System.currentTimeMillis()-t;
      if( f==null ) updateStat(0,0,1,duree,0,0);
      else updateStat(0,1,0,duree,0,0);
      return f;
   }

}

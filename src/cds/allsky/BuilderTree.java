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

import cds.fits.Fits;

/** Permet la g�n�ration du survey HEALPix � partir d'un index pr�alablement g�n�r�
 * @author Standard Ana�s Oberto [CDS] & Pierre Fernique [CDS]
 *
 */
public class BuilderTree extends BuilderTiles {

   public BuilderTree(Context context) { super(context); }

   public Action getAction() { return Action.TREE; }

   public void run() throws Exception {
      context.running("Creating "+context.getTileExt()+" tree and allsky (max depth="+context.getOrder()+")...");
      context.info("sky area to process: "+context.getNbLowCells()+" low level HEALPix cells");
      build();
      if( !context.isTaskAborting() ) { (new BuilderMoc(context)).run();  context.info("MOC done"); }
      if( !context.isTaskAborting() ) { (new BuilderAllsky(context)).run(); context.info("Allsky done"); }
   }

   // Valide la coh�rence des param�tres
   public void validateContext() throws Exception {
      validateOutput();
      if( !context.isExistingAllskyDir() ) throw new Exception("No tile found");
      validateOrder(context.getOutputPath());  
      if( !context.isColor() ) {
         validateCut();
         context.initParameters();
      } else {
         context.info("Building tree for a colored HiPS ("+context.getTileExt()+")");
         context.initRegion();
      }
   }
   
   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,int order,long npix) throws Exception {
      long t = System.currentTimeMillis();
      Fits f = findLeaf(file);
      long duree = System.currentTimeMillis()-t;
      if( f==null ) updateStat(0,0,1,duree,0,0);
      else updateStat(0,1,0,duree,0,0);
      return f;
   }
   
}

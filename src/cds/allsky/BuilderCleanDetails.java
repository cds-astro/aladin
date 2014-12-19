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

import javax.swing.JProgressBar;

import cds.tools.Util;

/** Permet de nettoyer les détails d'un index HEALPix
 * @author Pierre Fernique [CDS]
 */
public class BuilderCleanDetails extends BuilderClean {
   
   private String lastOrder;

   public BuilderCleanDetails(Context context) {
      super(context);
   }
   
   public Action getAction() { return Action.CLEANDETAILS; }
   
   public void validateContext() throws Exception { 
      super.validateContext();
      int orderIndex = cds.tools.pixtools.Util.getMaxOrderByPath( context.getHpxFinderPath() );
      if( orderIndex==-1 ) throw new Exception("HpxFinder order dir not found !");
      lastOrder = "Norder"+orderIndex;
      context.info("Cleaning all Norder dir except "+lastOrder+"...");
   }
   
   public void deleteDirExceptLastOrder(File dir) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");

      for( File f : dir.listFiles() ) {
         if( f.getName().equals(lastOrder) ) continue;
         deleteDir(f);
      }
   }
   
   public void run() throws Exception {
      deleteDirExceptLastOrder( new File(context.getHpxFinderPath()) );
   }
   
   public boolean mustBeDeleted(File f) {
      String name = f.getName();
      if( name.equals(Constante.FILE_METADATAXML) ) return false;
      else if( name.equals(Constante.FILE_MOC) ) return false;
      return true;
   }

}
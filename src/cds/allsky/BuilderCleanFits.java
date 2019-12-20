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

import java.io.File;

/** Permet de nettoyer toutes les tuiles FITS ainsi que le Allsky.fits
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 */
public class BuilderCleanFits extends BuilderClean  {

   public BuilderCleanFits(Context context) { super(context); }
   
   public Action getAction() { return Action.CLEANFITS; }
   
   public boolean mustBeDeleted(File f) {
      String name = f.getName();
      if( name.equals("Allsky.fits") ) return true;
      if( !name.endsWith(".fits") )    return false;
      if( !name.startsWith("Npix") ) return false;
      return true;
   }

   public void run() throws Exception {
      deleteDirExceptIndex(new File(context.getOutputPath()));
      context.writeMetaFile();
   }
}

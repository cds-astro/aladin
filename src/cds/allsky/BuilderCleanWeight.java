// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.allsky;

import java.io.File;

/** Permet de nettoyer toutes les tuiles WEIGHT
 * @author Pierre Fernique [CDS]
 */
public class BuilderCleanWeight extends BuilderCleanFits {

   public BuilderCleanWeight(Context context) { super(context); }

   public Action getAction() { return Action.CLEANWEIGHT; }

   public boolean mustBeDeleted(File f) {
      String name = f.getName();
      if( !name.endsWith("_w.fits") ) return false;
      if( !name.startsWith("Npix") ) return false;
      return true;
   }
   
   public void run() throws Exception {
      context.live=false;
      super.run();
   }


}

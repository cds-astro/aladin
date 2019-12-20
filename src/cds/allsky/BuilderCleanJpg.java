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

/** Permet de nettoyer toutes les tuiles JPEG ainsi que le Allsky.jpg
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 */
public class BuilderCleanJpg extends BuilderCleanFits {

   public BuilderCleanJpg(Context context) { super(context); }
   
   public Action getAction() { return Action.CLEANJPEG; }

   public boolean mustBeDeleted(File f) {
      String name = f.getName();
      if( name.equals("Allsky.jpg") ) return true;
      if( !name.endsWith(".jpg") )    return false;
      if( !name.startsWith("Npix") ) return false;
      return true;
   }

}

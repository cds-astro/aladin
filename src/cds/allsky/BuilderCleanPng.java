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

import java.io.File;

/** Permet de nettoyer toutes les tuiles PNG ainsi que le Allsky.png
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 */
public class BuilderCleanPng extends BuilderCleanFits {

   public BuilderCleanPng(Context context) { super(context); }

   public Action getAction() { return Action.CLEANPNG; }

   public boolean mustBeDeleted(File f) {
      String name = f.getName();
      if( name.equals("Allsky.png") ) return true;
      if( !name.endsWith(".png") )    return false;
      if( !name.startsWith("Npix") ) return false;
      return true;
   }
   
   public void resetCheckCode() { context.resetCheckCode("png"); };

}

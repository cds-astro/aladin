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

/** Permet de nettoyer un index temporel (TimeFinder)
 * @author Pierre Fernique [CDS]
 */
public class BuilderCleanTIndex extends BuilderClean {

   public BuilderCleanTIndex(Context context) {
      super(context);
   }

   public Action getAction() { return Action.CLEANTINDEX; }

   public boolean isAlreadyDone() { return !(new File(context.getTimeFinderPath())).exists(); }

   public void run() throws Exception {
      deleteDir( new File(context.getTimeFinderPath()) );
   }

   public boolean mustBeDeleted(File f) {
      String name = f.getName();
      if( name.equals(Constante.FILE_METADATAXML) ) return false;
      return true;
   }

}

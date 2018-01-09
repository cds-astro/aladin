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

/** Permet de nettoyer toutes les tuiles ainsi que le Allsky.* qui sont plus vieux qu'une
 * certaine date
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 */
public class BuilderCleanDate extends BuilderCleanFits {

   private long date=-1;

   public BuilderCleanDate(Context context) { super(context); }

   public Action getAction() { return Action.CLEANDATE; }

   public void setDate(long date) { this.date=date; }

   public void validateContext() throws Exception {
      super.validateContext();
      if( date<=0 ) throw new Exception("Limit date unknown (see setDate(...))");
   }

   public boolean mustBeDeleted(File f) {
      return f.lastModified()<=date;
   }

}

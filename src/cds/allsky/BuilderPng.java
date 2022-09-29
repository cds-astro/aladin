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

/** Construction de la hiérarchie des tuiles PNG à partir des tuiles FITS de plus bas
 * niveau. Voir commentaire BuilderJpg
 * @author Pierre Fernique
 */
public class BuilderPng extends BuilderJpg {

   public BuilderPng(Context context) {
      super(context);
   }
   
   protected void init() {
      fmt = "png";
      ext = ".png";
   }

   public Action getAction() { return Action.PNG; }
   
   protected int getMinCM() { return 1; }

}

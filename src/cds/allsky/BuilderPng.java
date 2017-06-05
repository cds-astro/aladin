// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
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

/** Construction de la hi�rarchie des tuiles PNG � partir des tuiles FITS de plus bas
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

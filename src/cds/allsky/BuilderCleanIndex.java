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

import javax.swing.JProgressBar;

import cds.tools.Util;

/** Permet de nettoyer un index HEALPix
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 */
public class BuilderCleanIndex extends BuilderClean {

   public BuilderCleanIndex(Context context) {
      super(context);
   }

   public Action getAction() { return Action.CLEANINDEX; }

   public void validateContext() throws Exception {
      super.validateContext();
      if( context instanceof ContextGui ) {
         JProgressBar bar = ((ContextGui)context).mainPanel.getProgressBarIndex();
         bar.setIndeterminate(true);
         context.setProgressBar(bar);
         bar.setString("Cleaning previous index...");
      }
   }

   public boolean isAlreadyDone() { return !(new File(context.getHpxFinderPath())).exists(); }

   public void run() throws Exception {
      if( context instanceof ContextGui ) Util.pause(1000); // Juste pour faire beau
      deleteDir( new File(context.getHpxFinderPath()) );
   }

   public boolean mustBeDeleted(File f) {
      String name = f.getName();
      if( name.equals(Constante.FILE_METADATAXML) ) return false;
      return true;
   }

}

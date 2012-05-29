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

/** Permet de nettoyer la totalit� des tuiles, fichiers de propri�t�s, MOC, ... d'un
 * survey HEALPix sauf l'index
 * @author Ana�s Oberto & Pierre Fernique [CDS]
 */
public class BuilderCleanTiles extends BuilderClean {

   public BuilderCleanTiles(Context context) { super(context); }
   
   public Action getAction() { return Action.CLEANTILES; }
   
   public void validateContext() throws Exception { 
      super.validateContext();
      if( context instanceof ContextGui ) {
         JProgressBar bar = ((ContextGui)context).mainPanel.getProgressBarTile();
         context.setProgressBar(bar);
         bar.setString("Cleaning previous survey...");
      }
   }
   
   public void run() throws Exception {
      if( context instanceof ContextGui ) Util.pause(1000); // Pour faire beau
      deleteDirExceptIndex(new File(context.getOutputPath()));
   }
}
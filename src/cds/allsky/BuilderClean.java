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

/** Permet de nettoyer la totalité d'un survey généré, sauf le fichier Properties
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 */
public class BuilderClean extends Builder {
   private int nbFile;      // Nombre de fichiers supprimés

   public BuilderClean(Context context) {
      super(context);
      nbFile=0;
   }

   public Action getAction() { return Action.CLEAN; }

   public void run() throws Exception {
      deleteDir(new File(context.getOutputPath()));
   }

   public void validateContext() throws Exception { validateOutput(); }

   public boolean isAlreadyDone() { return !(new File(context.getOutputPath())).exists(); }

   public void showStatistics() {
      if( context instanceof ContextGui ) return;
      context.stat(nbFile+" file"+(nbFile>1?"s":"")+" deleted");
   }

   public boolean mustBeDeleted(File f) {
      String name = f.getName();
      if( name.equals(Constante.FILE_PROPERTIES) ) return false;
      return true;
   }

   public void deleteDir(File dir) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");

      // répertoire
      if( dir.isDirectory() ) {
         for ( File f : dir.listFiles() ) deleteDir(f);
         dir.delete();

         // simple fichier
      } else if( mustBeDeleted(dir) ) {
         if( !dir.delete() ) throw new Exception("Cannot delete "+dir.getCanonicalPath());
         //         System.out.println("delete "+dir);
         nbFile++;
         context.setProgress(nbFile);
      }
   }

   public void deleteDirExceptIndex(File dir) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");

      for( File f : dir.listFiles() ) {
         if( f.getName().equals(Constante.FILE_HPXFINDER) ) continue;
         deleteDir(f);
      }
   }

}
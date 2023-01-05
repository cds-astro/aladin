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
import java.nio.file.Files;
import java.nio.file.Path;

import cds.fits.Fits;
import cds.tools.pixtools.Util;

/** Vérification du check code et de certains DATASUM associés aux tuiles FITS
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Décembre 2022
 */
public class BuilderCheckFast extends BuilderCheck {
   static final int MAXCORRUPT = 99;  // Nombre MAX de fichiers corrompus avant d'arrêter
   static final int MAXMISSING = 10;  // Nombre MAX de fichiers tolérés sans DATASUM
   static final int MAXCHECKED = 100; // Nombre de fichiers à tester
   
   int missingDataSum=0;              // Nombre de fichiers sans DATASUM
   int corruptDataSum=0;              // Nombre de fichiers corrompus

   public BuilderCheckFast(Context context) {
      super(context);
   }
   
   public Action getAction() { return Action.CHECKFAST; }
   
   protected void validateContextMore() throws Exception {}
   
   public void run() throws Exception {

      // Vérification des premiers ordres
      String fmt="fits";
      Info info = new Info(fmt);
      for( int i=0; i<=3; i++ ) {
         File dir = new File( context.getOutputPath()+Util.FS+"Norder"+i );
         if( !dir.exists() ) continue;
         scanDir(dir,fmt,info);
      }
      
      // A priori pas de fichiers avec DATASUM
      if( missingDataSum>MAXMISSING  && corruptDataSum==0 ) {
         context.warning("This HiPS has not DATASUM in their FITS tiles => No DATASUM check");
      
      // Rapport des fichiers éventuellement corrompus
      } else if( corruptDataSum==0 ) {
         context.info("Tested"+(missingDataSum==0?"":" tested")+" HiPS Fits tiles DATASUM Ok");
         
      } else {
         if( missingDataSum>0 ) context.warning(missingDataSum+" HiPS Fits tile(s) without DATASUM => not tested");
         context.error("HiPS is corrupted");
      }
   }
   
   public void scanDir(File dir, String fmt, Info info) throws Exception {
      if( nbFile>MAXCHECKED || corruptDataSum>0) return;
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      
      // répertoire
      if( dir.isDirectory() ) {
         File [] list = dir.listFiles();
         for ( File f : list ) scanDir(f,fmt,info);
         
         if( Files.isSymbolicLink( dir.toPath()) ) {
            Path target = Files.readSymbolicLink( dir.toPath() );
            updateInfo( target.toFile(), info);
         }

         // simple fichier
      } else if( mustBeScanned(dir,fmt) ) {
         updateInfo( dir, info );
         nbFile++;
         context.setProgress(nbFile);
      }
   }

   protected void updateInfo(File f, Info info) throws Exception {
      super.updateInfo(f, info);
      Fits fits = new Fits();
      fits.loadFITS( f.getAbsolutePath() );
      int check = fits.checkDataSum();
      if( check==0 ) { corruptDataSum++; info.addCorruptedFile(f); }
      else if( check==-1 ) missingDataSum++;
   }

}

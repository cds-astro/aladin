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

import cds.fits.Fits;


/**
 * Permet la suppression des bords dess tuiles Fits et du Allsky.fits
 * @author P. Fernique [CDS]
 * @version 1.0 - dec 2022- création
 */
public class BuilderTrim extends BuilderGzip {
   
   public BuilderTrim(Context context) { super(context); }

   public Action getAction() { return Action.TRIM; }

   
   public boolean isAlreadyDone() {
      if( !context.actionPrecedeAction(Action.INDEX, Action.TILES)) return false;
      if( !context.actionPrecedeAction(Action.TILES, Action.GZIP)) return false;
      if( context.actionAlreadyDone(Action.UNTRIM) && !context.actionPrecedeAction(Action.TRIM, Action.UNTRIM)) return false;
      context.info("TRIM seems to be already done");
      return true;
   }
   
   protected long gzip( String filename ) throws Exception {
      if( !(new File(filename)).isFile() ) return 0L;
      long t = System.currentTimeMillis();
      gzip(filename,compress);
      return System.currentTimeMillis()-t;
   }
   
   // trim (resp. untrim) du fichier indiqué. 
   // Dans le cas où un fichier est déjà trimmé (resp. untrimmé), le fichier est simplement ignoré
   private void gzip(String file,boolean compress) throws Exception {
      if( context.isTaskAborting() ) throw new Exception("Task abort !");
      Fits f = new Fits();
      Fits out = null;
      f.loadFITS(file);
      out = compress ? f.trimFactory() : f.untrimFactory();
      if( out!=null ) out.writeFITS(file);
   }
   
}

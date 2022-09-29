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

/** Génération ou mise-à-jour de tous les DATASUM associés aux fichiers FITS
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Juillet 2022
 */
public class BuilderUpdateDataSum extends BuilderCheckCode {

   public BuilderUpdateDataSum(Context context) {
      super(context);
   }

   public Action getAction() { return Action.UPDATEDATASUM; }
   
   protected void validateContextMore() throws Exception {
      if( format.indexOf("fits")<0 ) throw new Exception("No Fits tiles for this HiPS!");
   }
   
   public void run() throws Exception {
      context.loadProperties();
      scanDir(new File( context.getOutputPath() ),"fits");
      context.writePropertiesFile(null);
      context.info("All DATASUM generated/updated");
   }
   
   protected void updateInfo(File f, Info info) throws Exception {
      super.updateInfo(f, info);
      String filename = f.getAbsolutePath();
      Fits fits = new Fits();
      fits.loadFITS( filename );
      fits.addDataSum();
      fits.writeFITS( filename );
   }

}

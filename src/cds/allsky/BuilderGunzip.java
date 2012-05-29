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


/** Permet la décompression de toutes les tuiles Fits
 * @author P. Fernique [CDS]
 * @version 1.0 - mai 2012 - création
 */
public class BuilderGunzip extends BuilderGzip {
   
   public BuilderGunzip(Context context) { super(context); }

   public Action getAction() { return Action.GUNZIP; }

   /** Gunzippe toutes les tuiles FITS ainsi que le fichier Allsky.fits qui se trouve
    * dans le répertoire Allsky repéré par root
    * Attention: ne change pas pour autant les extensions des fichiers (toujours.fits)
    */
   public void run() throws Exception {
      gzipRec(false);
   }
}

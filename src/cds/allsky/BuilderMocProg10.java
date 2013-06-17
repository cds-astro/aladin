// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
// This file is part of Aladin.
//
// Aladin is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 3 of the License.
//
// Aladin is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// The GNU General Public License is available in COPYING file
// along with Aladin.
//

package cds.allsky;


import static cds.tools.Util.FS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;

import cds.aladin.MyInputStream;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;

/** Création d'un fichier Moc.fits correspondant au tuile HEALPix, mais à 
 * une résolution supérieure
 */
final public class BuilderMocProg10 extends BuilderMocIndex {
   
   
   public BuilderMocProg10(Context context) {
      super(context);
   }
   
   public Action getAction() { return Action.MOC10; }
   
   /** Création d'un Moc associé à l'arborescence trouvée dans le répertoire path */
   protected void createMoc(String path) throws Exception {
      createMoc(path, path + FS + "Moc10.fits");
   }
   
   
   protected void generateTileMoc(HealpixMoc moc,File f,int order, long npix) throws Exception {
      BufferedReader reader = null;
      try {
         reader = new BufferedReader(new FileReader(f));
         int n=0;
         while( reader.readLine()!=null ) n++;
         if( n>=Constante.MAXOVERLAY ) {
//            System.out.println("J'ajoute : "+order+"/"+npix);
            moc.add(order,npix);
         }
      }  finally {
         if( reader!=null ) reader.close();
      }
   }


}

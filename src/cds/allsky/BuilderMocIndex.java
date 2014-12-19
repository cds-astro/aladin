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
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

/** Création d'un fichier Moc.fits correspondant à l'index HEALpix
 * @author Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 */
public class BuilderMocIndex extends BuilderMoc {

   public BuilderMocIndex(Context context) { super(context); }
   
   public void run() throws Exception {
      long t = System.currentTimeMillis();
      
      String path = context.getHpxFinderPath();
      
      moc = new HealpixMoc();
      mocOrder = Util.getMaxOrderByPath(path);
      moc.setMocOrder(mocOrder);

      String outputFile = path + FS + Constante.FILE_MOC;
      moc.setCoordSys(getFrame());
      moc.setCheckConsistencyFlag(false);
      generateMoc(moc,mocOrder, path);
      moc.setCheckConsistencyFlag(true);
      moc.write(outputFile);
      
      long time = System.currentTimeMillis() - t;
      context.info("MOC Index done in "+cds.tools.Util.getTemps(time,true)
                        +": mocOrder="+moc.getMocOrder()
                        +" size="+cds.tools.Util.getUnitDisk( moc.getSize()));

   }
   
   public Action getAction() { return Action.MOCINDEX; }
}

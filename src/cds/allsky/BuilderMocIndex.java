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

import static cds.tools.Util.FS;

import cds.moc.SMoc;
import cds.tools.pixtools.Util;

/** Création d'un fichier Moc.fits correspondant à l'index HEALpix
 * @author Anaïs Oberto [CDS] & Pierre Fernique [CDS]
 */
public class BuilderMocIndex extends BuilderMoc {

   public BuilderMocIndex(Context context) { super(context); }
   
   public void run() throws Exception {
      long t = System.currentTimeMillis();
      
      String path = context.getHpxFinderPath();
      
      moc = new SMoc();
      mocOrder = Util.getMaxOrderByPath(path);
      moc.setMocOrder(mocOrder);

      String outputFile = path + FS + Constante.FILE_MOC;
      String frame = getFrame();
      moc.setSpaceSys(frame);
      generateMoc(moc,mocOrder, path);
      moc.write(outputFile);
      
// IL NE FAUT PAS CONVERTIR EN ICRS SI ON EST EN GAL CAR SINON LE BuilderTiles NE VA PAS
// FONCTIONNER (cf. BuilderTiles.createHpx -> isInMoc(norder,npix)
//
//      // Faut-il changer le référentiel du MOC ?
//      if( !frame.equals("C") ) {
//         SMoc moc1 = convertTo(moc,"C");
//         context.info("MOC Index convertTo ICRS...");
//         moc = moc1;
//      }
      
      long time = System.currentTimeMillis() - t;
      context.info("MOC Index done in "+cds.tools.Util.getTemps(time*1000L)
                        +": mocOrder="+moc.getMocOrder()
                        +"  frame="+frame
                        +" size="+cds.tools.Util.getUnitDisk( moc.getMem()));

   }
   
   public Action getAction() { return Action.MOCINDEX; }
}

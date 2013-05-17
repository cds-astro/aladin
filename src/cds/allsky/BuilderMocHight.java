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

import java.io.File;
import java.io.FileInputStream;

import cds.aladin.MyInputStream;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/** Création d'un fichier Moc.fits correspondant au tuile HEALPix, mais à 
 * une résolution supérieure
 */
final public class BuilderMocHight extends BuilderMoc {
   
   private int diffOrder;
   
   public BuilderMocHight(Context context) {
      super(context);
      context.createHealpixOrder(Constante.ORDER);
      diffOrder = context.getDiffOrder();
   }
   
   public Action getAction() { return Action.MOCHIGHT; }
   
   
   protected void generateTileMoc(HealpixMoc moc,File f,int order, long npix) throws Exception {
      Fits fits = new Fits();
      MyInputStream dis = new MyInputStream(new FileInputStream(f));
      fits.loadFITS(dis);
      dis.close();
      
      double blank = context.getBlank();
      long nsize = CDSHealpix.pow2(Constante.ORDER);
      long min = nsize * nsize * npix;
      order+=Constante.ORDER;
      moc.setMaxLimitOrder(order-diffOrder);
      
//      System.out.println("generateTileMoc for "+f.getName()+" MOC="+moc.getMaxLimitOrder()+"/"+Util.getUnitDisk(moc.getMem()));
      
      long oNpix=-1;  
      for( int y=0; y<fits.height; y++ ) {
         for( int x=0; x<fits.width; x++ ) {
            try {
               npix = min + context.xy2hpx(y * fits.width + x);
               double pixel = fits.getPixelDouble(x,y);
               
               // Pixel vide
               if( Double.isNaN( pixel ) || pixel==blank ) continue;

               // Juste pour éviter d'insérer 2x de suite le même npix
               if( npix==oNpix ) continue;
               
               moc.add(order,npix);
               oNpix=npix;
            } catch( Exception e ) {
               e.printStackTrace();
            }
         }
      }
      
      moc.checkAndFix();
   }


}

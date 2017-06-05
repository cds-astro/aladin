// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

package cds.aladin;

import cds.tools.Util;

/**
 * Plan gérant une carte de densité au format Healpix
 * @author Pierre Fernique [CDS]
 *
 */
public class PlanHealpixDMap extends PlanHealpix {
   
   public PlanHealpixDMap(Aladin aladin, String urlOfFile, String label) throws Exception {
      super(aladin);

      MyInputStream in = null;
      if( label==null ) label = "DMAP";
      try { in=Util.openAnyStream(urlOfFile); } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); throw e;}
      init( urlOfFile , in, label, 0);
      setDrawMode(DRAWPIXEL);
      threading();
   }
   
   protected void postProd() {
      Projection p =new Projection("test",Projection.WCS,co.al,co.del,60*4,60*4,250,250,500,500,0,false,Calib.AIT,Calib.FK5);
      p.frame = getCurrentFrameDrawing();
      setNewProjD(p);
      initZoom=1./64;

      loadAllSkyNow();          // pour avoir immédiatement un échantillon des pixelOrigin[] via le allsky
      setCmParam("eosb noreverse all nocut");
   }


}

// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

import java.awt.Graphics;

class HealpixAllskyCat extends HealpixKeyCat {


   protected HealpixAllskyCat(PlanBG planBG,int order,int ext) {
      super(planBG);
      this.order=order;
      this.npix=-1;
      allSky=true;
      resetTimer();
      String nameNet = "Norder"+order+"/Allsky";
      String nameCache = planBG.getCacheName()+"/"+"Norder"+order+"/Allsky";
      extCache=extNet=ext;
      fileCache = nameCache+ EXT[extCache];
      fileNet = nameNet+ EXT[extNet];
      alreadyCached=false;
      priority=-1;

      setStatus(ASKING);
   }
   
   protected boolean isOutView(ViewSimple v) { return false; }
   protected boolean isOutView(ViewSimple v,PointD []b) { return false; }

   protected int draw(Graphics g, ViewSimple v) {
      if( pcat==null || !pcat.hasObj() ) return 0;
      pcat.draw(g, v.rv, v, true, 0, 0);
      return pcat.getCount();
   }

   HealpixKey [] getPixList() {
      return new HealpixKey[]{ this };
   }

}

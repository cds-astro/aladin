// Copyright 2010 - UDS/CNRS
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

class HealpixAllskyProgen extends HealpixKeyProgen {

   protected HealpixAllskyProgen(PlanBG planBG,int order) {
      super(planBG);
      this.order=order;
      this.npix=-1;
      allSky=true;
      resetTimer();
      String nameNet = "Norder"+order+"/Allsky";
      String nameCache = planBG.survey+planBG.version+"/"+"Norder"+order+"/Allsky";
      extCache=extNet=IDX;
      fileCache = nameCache+ EXT[extCache];
      fileNet = nameNet+ EXT[extNet];
      alreadyCached=false;
      priority=-1;

      setStatus(ASKING);
   }
}
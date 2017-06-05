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

import java.util.Hashtable;


public class PlanBGStatic extends PlanBG {
   
   PlanBGStatic(Aladin aladin, String label, String url) {
      super(aladin);
      
      this.url = url;
      this.label = label;
      
      dataMin=pixelMin=0;
      dataMax=pixelMax=255;
      isOldPlan=false;

      pixList = new Hashtable<String,HealpixKey>(1000);

      RGBControl = new int[RGBCONTROL.length];
      for( int i=0; i<RGBCONTROL.length; i++) RGBControl[i] = RGBCONTROL[i];
      creatDefaultCM();
      
      type = ALLSKYIMG;
      inFits = false;
      inJPEG = true;
      inPNG = false;
      color = true;
      frameOrigin = Localisation.ICRS;

      scanProperties();
      
//      int defaultProjType = Projection.getProjType(sProj);
      int defaultProjType = Calib.SIN;
      
      projd = new Projection("allsky",Projection.WCS,0,0,60*4,60*4,250,250,500,500,0,false, defaultProjType,Calib.FK5);
      projd.frame = getCurrentFrameDrawing();
      
      typeCM = aladin.configuration.getCMMap();
      transfertFct = aladin.configuration.getCMFct();
      video = aladin.configuration.getCMVideo();
      
   }
   
   protected void askForRepaint() { }
}

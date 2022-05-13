// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

package cds.aladin;

import java.util.Hashtable;


public class PlanBGStatic extends PlanBG {
   
   PlanBGStatic(Aladin aladin, String url, boolean fits) throws Exception {
      super(aladin);
      
      this.url = url;
      this.label = "foo";
      
      dataMin=pixelMin=0;
      dataMax=pixelMax=255;
      isOldPlan=false;

      pixList = new Hashtable<>(1000);

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
      
      if( fits ) {
         truePixels=true;
         color=false;
      }
      
      local = !url.startsWith("http://") && !url.startsWith("https://");
      if( local ) useCache=false;
      
//      int defaultProjType = Projection.getProjType(sProj);
      int defaultProjType = Calib.AIT;
      
      projd = new Projection("allsky",Projection.WCS,0,0,60*4,60*4,250,250,500,500,0,false, defaultProjType,Calib.FK5,this);
      projd.frame = getCurrentFrameDrawing();
      
      typeCM = aladin.configuration.getCMMap();
      transfertFct = aladin.configuration.getCMFct();
      video = aladin.configuration.getCMVideo();
      
      flagOk=true;
   }
   
   protected boolean checkSite() { return true; }
   protected void askForRepaint() { }
   
   @Override
   protected void modifyProj(String projName) {
      int t = Projection.getProjType(projName);
      Projection p = projd;
      if( p==null || t==-1 ) return;
      projd.modify(label,Projection.SIMPLE,p.alphai,p.deltai,p.rm1,p.rm1,p.cx,p.cy,p.r1,p.r1,p.rot,p.sym,t,p.system);
   }
}

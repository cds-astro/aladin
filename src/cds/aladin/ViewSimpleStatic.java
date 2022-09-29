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

package cds.aladin;

import java.awt.Color;

import cds.aladin.stc.STCObj;

public class ViewSimpleStatic extends ViewSimple {

   
   protected ViewSimpleStatic(Aladin aladin) {
      super(aladin);

      setBackground(Color.white);
      setOpaque(true);
      setDoubleBuffered(false);
   }
   
   protected void setViewParam(PlanBG p, int width, int height, Coord c, double radius) {
      pref=p;
      setDimension(width,height);
      p.projd.setProjCenter(c.al, c.del);
      p.setDefaultZoom( c, radius, width);
      setZoomXY(p.initZoom,-1,-1,true);
      aladin.trace(3,"ImageMaker->setViewParam c="+c+" zoom="+zoom+" radius="+Coord.getUnit(radius)+" rzoom="+rzoom);
   }
   
   /** Génération d'un plan à partir des pixels repérés par le rectangle crop pour un plan allsky */
   protected PlanImage cropAreaBG(RectangleD rcrop,String label,double zoom,double resMult,boolean fullRes,boolean inStack)
   throws Exception {
      PlanImage pi=null;
      PlanBG pref = (PlanBG)this.pref;
      pref.projd = this.pref.projd.copy();
      
      if( pref.color ) throw new Exception("Not a HiPS fits");
      if( !pref.hasOriginalPixels() ) throw new Exception("No fits tiles");

      try {
         pi = new PlanImage(aladin,pref);
         pi.type=Plan.IMAGE;

         double zoomFct = zoom*resMult;

         pi.width = pi.naxis1 = (int)Math.round(rcrop.width*zoomFct);
         pi.height = pi.naxis2 = (int)Math.round(rcrop.height*zoomFct);
         pi.initZoom=1;
         
         pref.getCurrentBufPixels(pi,rcrop,zoomFct,resMult,fullRes);

         pi.projd.cropAndZoom(rcrop.x,rcrop.y,rcrop.width,rcrop.height, zoomFct);

         double deltaX= 0.5*zoomFct;
         double deltaY= 0.5*zoomFct;
         pi.projd.deltaProjXYCenter(-deltaX,-deltaY);

         pi.noCacheFromOriginalFile();
         pi.setHasSpecificCalib();
         pi.flagOk=true;

      } catch( Exception e ) { if( pi!=null ) pi.error=e.getMessage(); e.printStackTrace(); }
      return pi;
   }
   
   protected PlanImage cropAreaBG(RectangleD rcrop, STCObj stcObj, String label,double zoom,double resMult,boolean fullRes,boolean inStack)
		   throws Exception {
      PlanImage pi=null;
      PlanBG pref = (PlanBG)this.pref;
      pref.projd = this.pref.projd.copy();
      
      if( pref.color ) throw new Exception("Not a HiPS fits");
      if( !pref.hasOriginalPixels() ) throw new Exception("No fits tiles");

      try {
         pi = new PlanImage(aladin,pref);
         pi.type=Plan.IMAGE;

         double zoomFct = zoom*resMult;

         pi.width = pi.naxis1 = (int)Math.round(rcrop.width*zoomFct);
         pi.height = pi.naxis2 = (int)Math.round(rcrop.height*zoomFct);
         pi.initZoom=1;
         
         if (aladin.bubbleWrapIMProcessing && aladin.imListener != null) {
        	 pref.getCurrentBufPixelsBubbleWrapped(pi,rcrop,stcObj,zoomFct,resMult,fullRes);
         } else {
        	 pref.getCurrentBufPixels(pi,rcrop,stcObj,zoomFct,resMult,fullRes);
		}

         pi.projd.cropAndZoom(rcrop.x,rcrop.y,rcrop.width,rcrop.height, zoomFct);

         double deltaX= 0.5*zoomFct;
         double deltaY= 0.5*zoomFct;
         pi.projd.deltaProjXYCenter(-deltaX,-deltaY);

         pi.noCacheFromOriginalFile();
         pi.setHasSpecificCalib();
         pi.flagOk=true;

      } catch( Exception e ) { if( pi!=null ) pi.error=e.getMessage(); e.printStackTrace(); }
      return pi;
   }

   
}

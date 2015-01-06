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

import java.awt.Graphics;

/**
 * Outil d'affichage d'une colormap dédiée aux pixels de la vue
 * @author Pierre Fernique [CDS]
 * @version 1 (création Avril 2011)
 */
public class RainbowPixel extends Rainbow {

   private ViewSimple v;
   private boolean flagRedCM;

   public RainbowPixel(Aladin aladin,ViewSimple v) {
      super(aladin);
      r = new RectangleD(10,v.getHeight()-330,30,300);
      this.v = v;
      flagRedCM=false;
   }

   private PlanImage getPlan() { return (PlanImage)v.pref; }

   private double oLastPos=-1;
   private long t0=-1;

   public void setRedCM() {
      if( flagRedCM && lastPos==oLastPos ) return;
      if( !flagRedCM ) {
         if( t0==-1 ) t0=System.currentTimeMillis();
         if( System.currentTimeMillis()-t0<200 ) return;
      }
      double greyLevel = 256 * lastPos;
      if( greyLevel>255 ) greyLevel=255;
      PlanImage pimg = getPlan();
      pimg.setCM(CanvasColorMap.getCMBand((int)greyLevel, getPlan().video==PlanImage.VIDEO_INVERSE,
            true,pimg.isTransparent()));
      oLastPos=lastPos;
      flagRedCM=true;
   }

   public void restoreCM() {
      t0=-1;
      if( !flagRedCM ) return;
      getPlan().restoreCM();
      flagRedCM=false;
   }

   public boolean isAvailable() { return v.pref.hasAvailablePixels(); }

   /** Reçoit un évènement de la vue suite à un survol de la souris */
   public boolean mouseMove(double xview, double yview) {
      boolean rep = super.mouseMove(xview,yview);
      if( isIn && !isSelected ) setRedCM();
      else restoreCM();
      return rep;
   }

   public boolean submit(ViewSimple v) {
      restoreCM();
      return super.submit(v);
   }

   public void draw(Graphics gr,ViewSimple v,int dx, int dy) {
      if( !isAvailable() ) return;
      super.draw(gr,v,dx,dy);
   }
}

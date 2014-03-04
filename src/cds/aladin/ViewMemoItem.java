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

import java.awt.*;
import java.util.Hashtable;
import java.util.Vector;

public final class ViewMemoItem {
   protected double zoom;
   protected double xzoomView;
   protected double yzoomView;
   protected double rzoomWidth,rzoomHeight;
   protected int rvWidth,rvHeight;
   protected Plan pref;
   protected int delay;
   protected int lastFrame;
   protected int nbFrame;
   protected int modeBlink;
   protected int ordreTaquin;
   protected boolean locked,selected,northUp;
   protected Plot plot;
   
   
   boolean isPlotView;
   Vector plotTable;
   Projection plotProj;
   
   Projection projLocal;
//   double alphai, deltai;   // Uniquement dans le cas de planBG (v.projLocal)
   
   protected ViewMemoItem() { }

   protected ViewMemoItem(ViewSimple v) {
      set(v);
   }
   
   protected void set(ViewSimple v) {
      zoom=v.zoom;
      xzoomView=v.xzoomView;
      yzoomView=v.yzoomView;
      if( v.rzoom!=null ) {
         rzoomWidth = v.rzoom.width;
         rzoomHeight = v.rzoom.height;
      }
      if( v.rv!=null ) {
         rvWidth = v.rv.width;
         rvHeight = v.rv.height;
      }
      pref=v.pref;
      locked=v.locked;
      northUp=v.northUp;
      selected=v.selected;
      ordreTaquin=v.ordreTaquin;
      
      if( v.isPlotView() ) plot = v.plot.copyIn(v);
      else plot=null;   
      
      // POUR LE MOMENT CE N'EST PAS UTILISE (PF FEV 2009)
      if( v.projLocal!=null ) {
//         alphai = v.projLocal.alphai;
//         deltai = v.projLocal.deltai;
         projLocal = v.projLocal.copy();
      }
      
      if( v.pref instanceof PlanImageBlink && v.blinkControl!=null) {
         delay = v.blinkControl.delay;
         lastFrame = v.blinkControl.lastFrame;
         nbFrame = v.blinkControl.nbFrame;
         modeBlink = v.blinkControl.mode;
      }
   }

   protected ViewSimple get(ViewSimple v) {
      v.zoom=zoom;
      v.xzoomView=xzoomView;
      v.yzoomView=yzoomView;
      v.rzoom = new RectangleD(0,0,rzoomWidth,rzoomHeight);
      v.rv = new Rectangle(0,0,rvWidth,rvHeight);
      v.pref=pref;
      v.locked=locked;
      v.northUp=northUp;
      v.selected=selected;
      v.ordreTaquin=ordreTaquin;
      
      v.plot = plot==null ? null : plot.copyIn(v);
      
      // POUR LE MOMENT CE N'EST PAS UTILISE (PF FEV 2009)
      if( pref instanceof PlanBG ) {
//         v.projLocal = v.pref.projd.copy();
//         v.projLocal.setProjCenter(alphai, deltai);
         v.projLocal = projLocal.copy();
      }
      if( pref instanceof PlanImageBlink ) {
         if( v.blinkControl==null ) v.blinkControl = new BlinkControl(pref.aladin,
                                         (PlanImageBlink)pref,delay,
                                         modeBlink==BlinkControl.PAUSE);
         else v.blinkControl.delay = delay;
         v.blinkControl.lastFrame = lastFrame;
         v.blinkControl.nbFrame = nbFrame;
         v.blinkControl.mode = modeBlink;
         v.blinkControl.resume();
      }
      return v;
   }
}
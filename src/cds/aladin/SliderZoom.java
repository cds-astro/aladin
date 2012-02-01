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

/**
 * Slider de contrôle du zoom des vues sélectionnées
 * @author Pierre Fernique [CDS]
 * @version 1.0 Jan 2012 - création
 */
public class SliderZoom extends SliderPlusMoins {
   private Zoom zoom;
   
   public SliderZoom(Zoom zoom) {
//      super(zoom.aladin,zoom.aladin.getChaine().getString("ZOOM"),0,zoom.cZoom.getItemCount(),1);
      super(zoom.aladin,zoom.aladin.getChaine().getString("ZOOM"),6,18,1);
//      slider.setValue(zoom.MINZOOM);
      setTooltip(aladin.getChaine().getString("ZOOMTIP"));
      this.zoom = zoom;
   }
   
   void submit(int inc) {
      if( zoom==null ) return;
      if( inc==0 ) zoom.submit();
      else zoom.incZoom(inc);
   }
   
//   public void paintComponent(Graphics g) {
//      Plan p = aladin.calque.getPlanRef();
//      if( p instanceof PlanBG ) setMinMax(6,18);
//      else setMinMax(0,aladin.calque.zoom.cZoom.getItemCount()-1);
//      super.paintComponent(g);
//   }

}

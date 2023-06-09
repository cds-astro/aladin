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

import java.awt.Graphics;

/**
 * Slider de contr�le du zoom des vues s�lectionn�es
 * @author Pierre Fernique [CDS]
 * @version 1.0 Jan 2012 - cr�ation
 */
public class SliderZoom extends SliderPlusMoins {
   public SliderZoom(Aladin aladin) {
      super(aladin,aladin.getChaine().getString("ZOOM"),Zoom.MINSLIDER,Zoom.MAXSLIDER,1);
      setTooltip(aladin.getChaine().getString("ZOOMTIP"));
   }

   void submit(int inc) {
      if( aladin.calque.zoom==null ) return;
      if( inc==0 ) aladin.calque.zoom.submit();
      else aladin.calque.zoom.incZoom(inc);
   }

   public void paintComponent(Graphics g) {
      if( aladin.calque.isFree() ) slider.setValue(slider.min);
      else {
         if( aladin.calque.zoom.isBG() ) slider.setMinMax(Zoom.MINSLIDERBG, Zoom.MAXSLIDERBG);
         else slider.setMinMax(Zoom.MINSLIDER, Zoom.MAXSLIDER);
      }
      super.paintComponent(g);
   }

}

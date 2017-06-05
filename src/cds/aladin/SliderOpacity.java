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

import java.awt.Graphics;

import cds.aladin.Aladin;

/**
 * Slider de contrôle de l'opacité du plan sélectionné
 * @author Pierre Fernique [CDS]
 * @version 1.0 Jan 2012 - création
 */
public class SliderOpacity extends SliderPlusMoins {
   
   public SliderOpacity(Aladin aladin) {
      super(aladin,aladin.getChaine().getString("OPACITY"),0,100,20);
      setTooltip(aladin.getChaine().getString("OPACITYTIP"));
   }
   
   void submit(int inc) {
      Plan p = aladin.calque.getFirstSelectedPlan();
      if( p==null || !p.hasCanBeTranspState() ) return;
      float n = (float)( (slider.getValue()+inc)/100.);
      if( n<0f ) n=0f;
      if( n>1f ) n=1;
      if( inc!=0 ) slider.setValue((int)(n*100));
//      if( p.getOpacityLevel()==n ) return;
      aladin.calque.setOpacityLevel(n);
      aladin.calque.repaintAll();
   }
   
   public void paintComponent(Graphics g) {
      Plan p = aladin.calque.getFirstSelectedPlan();
      if( p!=null && p.hasCanBeTranspState() ) {
         setEnabled(true);
         slider.setValue((int)( p.getOpacityLevel()*100 ));
      } else { slider.setValue(slider.min); setEnabled(false); }
      super.paintComponent(g);
   }

}

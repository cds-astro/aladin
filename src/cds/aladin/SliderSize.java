// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.Graphics;
import java.util.Iterator;

/**
 * Slider de contrôle du facteur des tailles des sources (filtres et objets)
 * @author Pierre Fernique [CDS]
 * @version 1.0 Jan 2012 - création
 */
public class SliderSize extends SliderPlusMoins {
   
   public SliderSize(Aladin aladin) {
      super(aladin,aladin.getChaine().getString("SLIDERSIZE"),0,300,10);
      setTooltip(aladin.getChaine().getString("SLIDERSIZETIP"));
   }
   
   void submit(int inc) {
      Plan p = getPlans();
      if( p==null ) return;
      float n = (float)( (slider.getValue()+inc)/100.);
      if( n<0f ) n=0f;
      if( n>4 ) n=3f;
      if( inc!=0 ) slider.setValue((int)(n*100));
//      if( p.getScalingFactor()==n ) return;
      aladin.calque.setScalingFactor(n);
      aladin.calque.repaintAll();
   }
   
   // retourne le premier plan sélectionné 
   Plan getPlans() {
      Plan p = aladin.calque.getFirstSelectedPlan();
      if( p==null || !(p.isCatalog() || p.type==Plan.TOOL) ) return null;
      return p;
   }
   
   public void paintComponent(Graphics g) {
      Plan p = getPlans();
      if( p!=null ) {
         setEnabled(true);
         slider.setValue((int)( p.getScalingFactor()*100 ));
      } else { slider.setValue(slider.min); setEnabled(false); }
      super.paintComponent(g);
   }

}

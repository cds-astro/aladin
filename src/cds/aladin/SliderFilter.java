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
 * Slider de contrôle du facteur pour les filtres
 * @author Pierre Fernique [CDS]
 * @version 1.0 Jan 2012 - création
 */
public class SliderFilter extends SliderPlusMoins {
   
   public SliderFilter(Aladin aladin) {
      super(aladin,"Filter",0,300,10);
      setTooltip(aladin.getChaine().getString("SLIDERFILTERTIP"));
   }
   
   void submit(int inc) {
      Plan p = getPlanCatalog();
      if( p==null ) return;
      float n = (float)( (slider.getValue()+inc)/100.);
      if( n<0f ) n=0f;
      if( n>4 ) n=3f;
      if( inc!=0 ) slider.setValue((int)(n*100));
      Source s = (Source) p.iterator().next();
      if( s.actions!=null ) {
         if( p.getScalingFactor()==n ) return;
         p.setScalingFactor(n);
      } else ((PlanCatalog)p).setSourceType(slider.getValue()/50);
      aladin.calque.repaintAll();
   }
   
   // retourne le premier plan sélectionné s'il est sous influence d'un filtre
   // sinon null
   Plan getPlanCatalog() {
      Plan p = aladin.calque.getFirstSelectedPlan();
      if( p==null || !p.isCatalog() ) return null;
      return p;
   }
   
   public void paintComponent(Graphics g) {
      Plan p = getPlanCatalog();
      if( p!=null ) {
         setEnabled(true);
         Source s = (Source) p.iterator().next();
         if( s.actions==null ) {
            slider.setValue(s.sourceType*200);
         } else slider.setValue((int)( p.getScalingFactor()*100 ));
      } else { slider.setValue(slider.min); setEnabled(false); }
      super.paintComponent(g);
   }

}

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
 * Slider de contrôle de la visualisation d'un cube
 * @author Pierre Fernique [CDS]
 * @version 1.0 Mai 2013 - création
 */
public class SliderCube extends SliderPlusMoins {
   
   public SliderCube(Aladin aladin) {
      super(aladin,aladin.getChaine().getString("SLIDERCUBE"),0,10,1);
      setTooltip(aladin.getChaine().getString("SLIDERCUBETIP"));
   }

   void submit(int inc) {
      PlanImageBlink p = getPlanCube();
      if( p==null ) return;
      double frame = getValue();
      ViewSimple vs = aladin.view.getView(p);
      p.changeImgID();
      if( vs!=null ) aladin.view.setFrame(vs, frame, true);
      else  p.activePixels((int)frame);
      aladin.view.repaintAll();
   }
   
   // retourne le premier plan Cube sélectionné 
   PlanImageBlink  getPlanCube() {
      Plan [] p = aladin.calque.getPlans();
      for( Plan p1 : p ) if( p1.selected && p1 instanceof PlanImageBlink ) return (PlanImageBlink)p1;
      return null;
   }
   
   public void paintComponent(Graphics g) {
      PlanImageBlink p = getPlanCube();
      if( p!=null ) {
         setEnabled(true);
         slider.setMinMax(0, p.getNbFrame()-1 );
         slider.setValue( p.initFrame );
      } else { 
         slider.setValue(slider.min); 
         setEnabled(false); 
      }
      super.paintComponent(g);
   }

}

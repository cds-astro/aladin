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
      Plan p = getPlanCube();
      if( p==null ) return;
      double frame = getValue()+inc;
//      System.out.println("submit inc="+inc+" frame="+frame);
      p.changeImgID();     
      
//      ViewSimple vs = aladin.view.getView(p);
//      if( vs!=null ) aladin.view.setCubeFrame(vs, frame, true);
//      else p.setCubeFrame((int)frame);
      
      int vn[] = aladin.view.getNumView(p);
      for( int i=0; i<vn.length; i++ ) {
         aladin.view.setCubeFrame(aladin.view.viewSimple[ vn[i] ], frame, true);
      }
      aladin.view.repaintAll();
   }
   
   // retourne le premier plan Cube sélectionné 
   Plan getPlanCube() {
      Plan [] p = aladin.calque.getPlans();
      for( Plan p1 : p ) if( p1.selected && p1.isCube() ) return p1;
      return null;
   }
   
   public void paintComponent(Graphics g) {
      Plan p = getPlanCube();
      if( p!=null ) {
//         System.out.println("p="+p.label+" depth="+p.getDepth()+" z="+p.getZ()+" initFrame="+p.getInitFrame());
         setEnabled(true);
         slider.setMinMax(0, p.getDepth()-1 );
         slider.setValue( p.getZ() );
//         slider.setValue( p.getZ() );
      } else { 
         slider.setValue(slider.min); 
         setEnabled(false); 
      }
      super.paintComponent(g);
   }

}

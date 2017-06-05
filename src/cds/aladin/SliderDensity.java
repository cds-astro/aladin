// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

/**
 * Slider de contr�le de la densit� des catalogues progressifs
 * @author Pierre Fernique [CDS]
 * @version 1.0 Mars 2013 - cr�ation
 */
public class SliderDensity extends SliderPlusMoins {
   
   public SliderDensity(Aladin aladin) {
      super(aladin,aladin.getChaine().getString("SLIDERDENSITY"),
            -PlanBGCat.MAXGAPORDER,PlanBGCat.MAXGAPORDER,1);
      setTooltip(aladin.getChaine().getString("SLIDERDENSITYTIP"));
   }

   void submit(int inc) {
      Plan [] p = getPlanCatalog();
      if( p==null  ) return;
      for( Plan p1 : p ) {
         try {
            if( p1 instanceof PlanBGCat )  {
               ((PlanBGCat)p1).setGapOrder((int)slider.getValue()+inc);
            } else {
               ((PlanMoc)p1).setGapOrder((int)slider.getValue()+inc);

            }
         } catch( Exception e ) { }
      }
      //      aladin.calque.setScalingFactor(n);
      aladin.calque.repaintAll();
   }
   
   // retourne le premier plan s�lectionn� 
   Plan [] getPlanCatalog() {
      Plan [] p = aladin.calque.getPlans();
      
      // D�compte des plans concern�s
      int n=0;
      for( Plan p1 : p ) if( isOk(p1) ) n++;
      if( n==0 ) return null;
      
      // G�n�ration du tableau des plans concern�s
      Plan [] p2 = new Plan[n];
      n=0;
      for( Plan p1 : p ) if( isOk(p1) ) p2[n++]=p1;
      
      return p2;
   }
   
   private boolean isOk(Plan p) {
      return p.selected && 
            (p.type==Plan.ALLSKYCAT || p.type==Plan.ALLSKYMOC );
   }
   
   public void paintComponent(Graphics g) {
      Plan [] p = getPlanCatalog();
      if( p!=null ) {
         setEnabled(true);
         slider.setValue( ((PlanBGCat)p[0]).getGapOrder() );
      } else { 
         slider.setValue(slider.min); 
         setEnabled(false); 
      }
      super.paintComponent(g);
   }

}

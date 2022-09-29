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
 * Slider de contrôle de la densité des catalogues progressifs
 * @author Pierre Fernique [CDS]
 * @version 1.0 Mars 2013 - création
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
   
   // retourne le premier plan sélectionné 
   Plan [] getPlanCatalog() {
      Plan [] p = aladin.calque.getPlans();
      
      // Décompte des plans concernés
      int n=0;
      for( Plan p1 : p ) if( isOk(p1) ) n++;
      if( n==0 ) return null;
      
      // Génération du tableau des plans concernés
      Plan [] p2 = new Plan[n];
      n=0;
      for( Plan p1 : p ) if( isOk(p1) ) p2[n++]=p1;
      
      return p2;
   }
   
   private boolean isOk(Plan p) {
      return p.selected && 
            (p.type==Plan.ALLSKYCAT || p instanceof PlanMoc ); //|| p.type==Plan.ALLSKYTMOC);
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

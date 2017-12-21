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

import cds.astro.Astrotime;

/**
 * Slider de contrôle del'époche des sources
 * @author Pierre Fernique [CDS]
 * @version 1.0 Mars 2013 - création
 */
public class SliderEpoch extends SliderPlusMoins {
   
   String defaultLabel;
   
   public SliderEpoch(Aladin aladin) {
      super(aladin,aladin.getChaine().getString("SLIDEREPOCH"),1500,2500,1,100);
      setTooltip(aladin.getChaine().getString("SLIDEREPOCHTIP"));
      defaultLabel=label.getText();
//      label.addMouseListener(new MouseListener() {
//         public void mouseReleased(MouseEvent e) { setDefaultEpoch(); }
//         public void mousePressed(MouseEvent e) { }
//         public void mouseExited(MouseEvent e) { }
//         public void mouseEntered(MouseEvent e) { }
//         public void mouseClicked(MouseEvent e) { }
//      });
      label.setToolTipText(aladin.getChaine().getString("SLIDEREPOCHLABELTIP"));
   }
   
   /** Positionne l'époque en fonction de l'image de fond */
//   protected void setDefaultEpoch() {
   protected void setDefault() {
      double yr=2000;
      try {
         Plan pi = aladin.calque.getPlanBase();
         if( pi instanceof PlanImage) {
            String ep=((PlanImage)pi).getDateObs();
            if( ep!=null ) {
               Astrotime t = new Astrotime();
               t.set( ep );
               yr = t.getJyr();
            }
         }
      } catch( Exception e ) { }
      slider.setValue(yr);
      submit(0);
   }
   

   void submit(int inc) {
      Plan [] p = getPlanCatalog();
      if( p==null  ) return;
      for( Plan p1 : p ) {
         try { p1.setEpoch((slider.getValue()+inc)+"");
         } catch( Exception e ) { }
      }
      if( aladin.view.coteDist!=null ) aladin.view.getCurrentView().createCoteDist();

      aladin.calque.repaintAll();
   }
   
   // retourne le premier plan sélectionné 
   Plan [] getPlanCatalog() {
      Plan [] p = aladin.calque.getPlans();
      
      // Décompte des plans concernés
      int n=0;
      for( Plan p1 : p ) if( p1.selected && p1.hasPM() ) n++;
      if( n==0 ) return null;
      
      // Génération du tableau des plans concernés
      Plan [] p2 = new Plan[n];
      n=0;
      for( Plan p1 : p ) if( p1.selected && p1.hasPM() ) p2[n++]=p1;
      
      return p2;
   }
   
   public void paintComponent(Graphics g) {
      Plan [] p = getPlanCatalog();
      if( p!=null ) {
         setEnabled(true);
         
         int yr = (int)(p[0].getEpoch().getJyr()+0.5);
         label.setText("J"+yr);
         slider.setValue( yr );
      } else { 
         slider.setValue(slider.min); 
         label.setText(defaultLabel);
         setEnabled(false); 
      }
      super.paintComponent(g);
   }

}

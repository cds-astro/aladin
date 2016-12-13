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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JPanel;

/**
 * JPanel de gestion du Zoom et de la loupe
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (24 nov 2016) creation (mise en place d'un panel indépendant)
 */
public class SliderPanel extends JPanel {


   static final int MINZOOM=Zoom.mzn.length; // Nombre de valeurs zoom <1
   static final int MAXZOOM=67;   // en puissance de 2, valeur maximal du zoom

   static public final int MINSLIDERBG=0;
   static public final int MAXSLIDERBG=MAXZOOM-7;

   static public final int MINSLIDER=Zoom.mzn.length-7;
   static public final int MAXSLIDER=Zoom.mzn.length+7;

   protected SliderSize sizeSlider;
   protected SliderOpacity opacitySlider;
   protected SliderZoom zoomSlider;
   protected SliderEpoch epochSlider;
   protected SliderCube  cubeSlider;
   protected SliderDensity densitySlider;
   
   private JPanel sp;

   // Les references aux objets
   Aladin aladin;
   
   protected SliderPanel() { super(); }
   
   /** Creation du JPanel du zoom.
    * @param calque,aladin References
    */
   protected SliderPanel(Aladin aladin) {
      this.aladin = aladin;

      cubeSlider    = new SliderCube(aladin);
      epochSlider   = new SliderEpoch(aladin);
      sizeSlider    = new SliderSize(aladin);
      densitySlider = new SliderDensity(aladin);
      opacitySlider = new SliderOpacity(aladin);
      zoomSlider    = new SliderZoom(aladin);

      setLayout( new BorderLayout() );
      setBackground( aladin.getBackground());
      
      JPanel p0 = new JPanel();
      p0.setBackground( Color.yellow );
      
      sp = new JPanel(new BorderLayout());
      sp.setBackground( aladin.getBackground());
      adjustSliderPanel();
      
      add(p0);
      add(sp);
   }
   
   protected void adjustSliderPanel() {
      JPanel p = new JPanel(new GridLayout(0,1,1,1));
      p.setBackground( aladin.getBackground());
      if( !Aladin.OUTREACH ) {
         if( aladin.configuration.isSliderEpoch() )   p.add(epochSlider);
         if( aladin.configuration.isSliderSize() )    p.add(sizeSlider);
         if( aladin.configuration.isSliderDensity() ) p.add(densitySlider);
         if( aladin.configuration.isSliderCube() )    p.add(cubeSlider);
      }
      if( aladin.configuration.isSliderOpac() ) p.add(opacitySlider);
      if( aladin.configuration.isSliderZoom() ) p.add(zoomSlider);
      boolean flagValidate=false;
      if( sp.getComponentCount()>0 ) {
         sp.removeAll();
         flagValidate=true;
      }
      sp.add(p,BorderLayout.CENTER);
      if( flagValidate ) sp.revalidate();
   }
}

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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.Aladin;
import cds.tools.Util;

/**
 * Slider avec bouton "plus", "moins" et titre
 * @author Pierre Fernique [CDS]
 * @version 1.0 Jan 2012 - création
 */
public abstract class SliderPlusMoins extends JToolBar {
   Aladin aladin;
   
   JLabel label;
   JSlider slider;
   JButton plus,moins;
   
   /**
    * Création d'un slider
    * @param aladin référence
    * @param title - titre du slider (apparait sur la gauche)
    * @param min,max - valeurs du slider
    * @param incr - valeur de l'incrément lors de l'usage du bouton + ou -
    */
   public SliderPlusMoins(Aladin aladin,String title, int min, int max, final int incr) {
      super(HORIZONTAL);
      setFloatable(false);
      setBorder(BorderFactory.createEmptyBorder());
      this.aladin = aladin;
      
      add(label = new JLabel(title));

      slider = new JSlider(JSlider.HORIZONTAL,min,max,min);
      slider.addChangeListener( new ChangeListener() {
         public void stateChanged(ChangeEvent e) { submit(0); }
      });
      
      JButton b;
      moins=b = new JButton("-");
      b.setFont(b.getFont().deriveFont(Font.BOLD));
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(-incr); }
      });
      add(b);

      add(slider);
      
      plus=b = new JButton("+");
      b.setFont(b.getFont().deriveFont(Font.BOLD));
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(incr); }
      });
      add(b);
      
      setEnabled(false);
      
   }
   
   /** Récupère la valeur courant du slider */
   public int getValue() { return slider.getValue(); }
   
   /** Positionne la valeur courante du slider */
   public void setValue(int v) { slider.setValue(v); }
   
   /** Action appelée lors de la modification du slider par l'utilisateur */
   abstract void submit(int inc);
   
   boolean enable=true;
   
   /** Active ou désactive le slider */
   public void setEnabled(boolean m) {
      if( m==enable ) return;       // déjà fait
      enable=m;
      super.setEnabled(m);
      slider.setEnabled(m);
      label.setForeground( m ? Color.black : Aladin.MYGRAY );
      plus.setEnabled(m);
      moins.setEnabled(m);
   }
   
   /** Positionne le tip */
   void setTooltip(String tip) {
      Util.toolTip(label, tip);
      Util.toolTip(moins, tip);
      Util.toolTip(plus, tip);
      Util.toolTip(slider, tip);
   }
   
}

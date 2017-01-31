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

import javax.swing.JButton;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

public class MyComboBoxUI extends BasicComboBoxUI {
   
   protected void installDefaults() {
      super.installDefaults();
      comboBox.setBackground( Aladin.COLOR_TEXT_BACKGROUND );
      comboBox.setForeground( Aladin.COLOR_TEXT_FOREGROUND );
   }
   
   protected JButton createArrowButton() {
      if( !Aladin.DARK_THEME ) return super.createArrowButton();
      
      JButton button = new BasicArrowButton(BasicArrowButton.SOUTH,
            Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE, // UIManager.getColor("ComboBox.buttonBackground"),
            Aladin.COLOR_BACKGROUND,          // UIManager.getColor("ComboBox.buttonBackground"), 
            new Color(20,20,20),              //UIManager.getColor("ComboBox.buttonShadow"),
            Aladin.COLOR_CONTROL_FOREGROUND); //   UIManager.getColor("ComboBox.buttonHighlight"));
      button.setName("ComboBox.arrowButton");
      return button;
  }
}

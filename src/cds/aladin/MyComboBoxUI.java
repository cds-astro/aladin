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


import java.awt.Color;

import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

public class MyComboBoxUI extends BasicComboBoxUI {
   
   protected void installDefaults() {
      super.installDefaults();
      comboBox.setBackground( Aladin.COLOR_TEXT_BACKGROUND );
      comboBox.setForeground( Aladin.COLOR_TEXT_FOREGROUND );
      if( Aladin.DARK_THEME )  {
         UIManager.put("ComboBox.disabledForeground",Aladin.COLOR_CONTROL_BACKGROUND_UNAVAILABLE.darker());
         UIManager.put("ComboBox.disabledBackground",Aladin.COLOR_CONTROL_BACKGROUND_UNAVAILABLE);
      }
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


// UIManager.put("ComboBox.background", ...
//ComboBox.ancestorInputMap        =javax.swing.plaf.InputMapUIResource@21c887
//ComboBox.background              =javax.swing.plaf.ColorUIResource[r=204,g=204,b=204]
//ComboBox.disabledBackground      =javax.swing.plaf.ColorUIResource[r=204,g=204,b=204]
//ComboBox.disabledForeground      =javax.swing.plaf.ColorUIResource[r=153,g=153,b=153]
//ComboBox.font                    =javax.swing.plaf.FontUIResource[family=dialog.bold,name=Dialog,style=bold,size=12]
//ComboBox.foreground              =javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
//ComboBox.listBackground          =javax.swing.plaf.ColorUIResource[r=204,g=204,b=204]
//ComboBox.listForeground          =javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
//ComboBox.selectionBackground     =javax.swing.plaf.ColorUIResource[r=153,g=153,b=204]
//ComboBox.selectionForeground     =javax.swing.plaf.ColorUIResource[r=0,g=0,b=0]
//ComboBoxUI                       =javax.swing.plaf.metal.MetalComboBoxUI


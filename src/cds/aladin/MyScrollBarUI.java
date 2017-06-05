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
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class MyScrollBarUI extends BasicScrollBarUI {
   
   protected void configureScrollBarColors()
   {
       LookAndFeel.installColors(scrollbar, "ScrollBar.background", "ScrollBar.foreground");
       thumbHighlightColor = Aladin.COLOR_CONTROL_FOREGROUND;     // UIManager.getColor("ScrollBar.thumbHighlight");
       thumbLightShadowColor = Aladin.COLOR_BACKGROUND;           // UIManager.getColor("ScrollBar.thumbShadow");
       thumbDarkShadowColor = new Color(20,20,20);                // UIManager.getColor("ScrollBar.thumbDarkShadow");
       thumbColor = Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE;  // UIManager.getColor("ScrollBar.thumb");
       trackColor = Aladin.COLOR_BACKGROUND;                      // UIManager.getColor("ScrollBar.track");
       trackHighlightColor = UIManager.getColor("ScrollBar.trackHighlight");
       
       scrollBarWidth=16;
   }
   
   protected JButton createDecreaseButton(int orientation)  {
      return new BasicArrowButton(orientation,
            thumbColor,             // UIManager.getColor("ScrollBar.thumb"),
            thumbLightShadowColor,  // UIManager.getColor("ScrollBar.thumbShadow"),
            thumbDarkShadowColor,   // UIManager.getColor("ScrollBar.thumbDarkShadow"),
            thumbHighlightColor);   // UIManager.getColor("ScrollBar.thumbHighlight"));
  }

  protected JButton createIncreaseButton(int orientation)  {
      return new BasicArrowButton(orientation,
            thumbColor,               // UIManager.getColor("ScrollBar.thumb"),
            thumbLightShadowColor,    // UIManager.getColor("ScrollBar.thumbShadow"),
            thumbDarkShadowColor,     // UIManager.getColor("ScrollBar.thumbDarkShadow"),
            thumbHighlightColor);     // UIManager.getColor("ScrollBar.thumbHighlight"));
  }
}

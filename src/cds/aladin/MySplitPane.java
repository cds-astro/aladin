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

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

// Surcharges de classes pour supprimer le trait séparateur du JSplitPane
public class MySplitPane extends JSplitPane {
   
   public MySplitPane(int newOrientation, boolean newContinuousLayout,
         Component newLeftComponent, Component newRightComponent ) {
      super(newOrientation,newContinuousLayout,newLeftComponent,newRightComponent);
      setUI(new MyBasicSplitPaneUI());
   }
   
   public int getPos() {
      return (getOrientation()==VERTICAL_SPLIT ? getHeight() : getWidth()) - getDividerLocation();
   }

   class MyBasicSplitPaneUI extends BasicSplitPaneUI {
      public BasicSplitPaneDivider createDefaultDivider() {
         return new MySplitPaneDivider(this);
      }
   }
   class MySplitPaneDivider extends BasicSplitPaneDivider {
      public MySplitPaneDivider(BasicSplitPaneUI ui) { super(ui); }
      public void paint(Graphics g) { }
   }
}


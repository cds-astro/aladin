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

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTreeUI;

public class MyTreeUI extends BasicTreeUI {

   public MyTreeUI() {
      super();
      UIManager.put("Tree.selectionBackground", Aladin.COLOR_STACK_SELECT.brighter() );
   }


   protected void paintHorizontalLine(Graphics g,JComponent c,int y,int left,int right){
//               super.paintHorizontalLine(g,c,y,left,right);
   }
   protected void paintVerticalLine(Graphics g,JComponent c,int x,int top,int bottom){
//               super.paintVerticalLine(g,c,x,top,bottom);
   }



}

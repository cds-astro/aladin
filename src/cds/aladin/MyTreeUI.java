// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import java.awt.Color;
import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTreeUI;

public class MyTreeUI extends BasicTreeUI {

   public MyTreeUI(Color c) {
      super();
      
      UIManager.put("Tree.background", c );
      UIManager.put("Tree.selectionBackground", Aladin.COLOR_STACK_SELECT.brighter() );
      UIManager.put("Tree.paintLines", false );
      UIManager.put("Tree.drawsFocusBorder", false);
      
      Image img;
      img = Aladin.aladin.getImagette("expandedIcon.png");
      if( img!=null ) UIManager.put("Tree.expandedIcon", new ImageIcon( img ) );
      img = Aladin.aladin.getImagette("collapsedIcon.png");
      if( img!=null ) UIManager.put("Tree.collapsedIcon", new ImageIcon( img ) );
      if( img!=null ) UIManager.put("Tree.closeIcon", new ImageIcon( img ) );
   }


//   protected void paintHorizontalLine(Graphics g,JComponent c,int y,int left,int right){
////               super.paintHorizontalLine(g,c,y,left,right);
//   }
//   protected void paintVerticalLine(Graphics g,JComponent c,int x,int top,int bottom){
////               super.paintVerticalLine(g,c,x,top,bottom);
//   }



}

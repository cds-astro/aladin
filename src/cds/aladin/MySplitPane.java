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
      flagMesure = newOrientation==JSplitPane.VERTICAL_SPLIT;
      setUI(new MyBasicSplitPaneUI());
//      setDividerSize(20);
//      setOneTouchExpandable(true);
   }

   private boolean flagMesure;
   private int mesureHeight;

   // Repositionne le diviseur à la position mémorisée
   public void restoreMesureHeight() {
      setDividerLocation(getHeight()-(mesureHeight<=0 ? 150 : mesureHeight)); }

   // Positionne le diviseur en fonction de la taille de la fenêtre des mesures,
   // et mémorise cette valeur pour pouvoir y revenir
   public void setMesureHeight(int h) { mesureHeight=h; }

   // Retourne la taille de la fenêtre des mesures.
   public int getMesureHeight() { return mesureHeight; }

   // On bride à 55 pixels minimum pour la taille de la fenêtre des mesures
   public void setDividerLocation(int n) {
      if( flagMesure ) {
         int h = getHeight();
         if( h-n<53 ) return;
         mesureHeight = h-n;
      }
      super.setDividerLocation(n);
   }

}
class MyBasicSplitPaneUI extends BasicSplitPaneUI {
   public BasicSplitPaneDivider createDefaultDivider() {
      return new MySplitPaneDivider(this);
   }
}
class MySplitPaneDivider extends BasicSplitPaneDivider {
   public MySplitPaneDivider(BasicSplitPaneUI ui) { super(ui); }
   public void paint(Graphics g) {
   }
   
}


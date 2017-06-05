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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;

import javax.swing.JPanel;

/**
 * Emballage pour pouvoir gerer l'encadrement des formulaires
 * multiples de l'objet ServerDialog
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class SelectDialog extends JPanel {
   ServerDialog serverDialog;
   int currentpanel=0;
   static final int dw = 1;
   static boolean flagInsets=false;


  /** Creation.
   * @param serverDialog Reference
   * @param mp Les formulaires multiples
   */
   protected SelectDialog(ServerDialog serverDialog, JPanel mp) {
      this.serverDialog = serverDialog;
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      setLayout(gbl);
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = gbc.weighty = 1;
      gbc.insets = new Insets(dw,dw,dw,dw);
      gbl.setConstraints(mp,gbc);
      add(mp);
   }
   
   public Insets getInsets() { return new Insets(dw,dw,5,5); }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);

      Dimension d = getSize();
      int w = d.width;
      int h = d.height;

      g.setColor( getBackground() );
      g.fillRect(0,0,w,h);

      g.setColor( Aladin.BLUE );
      g.fillRect(0,0,w,h);
      g.setColor( Color.black );
      g.drawLine(0,0,w,0);
      g.drawLine(0,0,0,h);
      g.setColor( Color.gray );
      g.drawLine(0,h-1,w,h-1);
      g.drawLine(w-1,0,w-1,h);

      MyButton c = serverDialog.buttons[serverDialog.bcurrent];

      if( c.type==MyButton.LEFT || c.type==MyButton.RIGHT ) {
      	 int y = c.getLocation().y;
      	 h = c.getSize().height;
      	 int x = c.type==MyButton.LEFT ? 0 : w-dw-3;

      	 g.setColor(Aladin.BLUE);
      	 g.fillRect(x,y+1,dw+3,h-1);
      }

      if( c.type==MyButton.TOP ) {
      	 w = c.getSize().width;
     	 int x = c.getLocation().x-serverDialog.getMargeGauche();
      	 g.setColor(Aladin.BLUE);
      	 g.fillRect(x+1,0,w-1,dw+3);
      }
   }
}

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

import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

/**
 * Gestion d'un Frame avec reception de l'evenement
 * de destruction et aiguillage suivant le cas applet, standalone...
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : nov 07 Simplification due au passage à swing
 * @version 1.1 : 3 juin 99    Gestion du flagNormal pour ``disposer''
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class MyFrame extends JFrame {
   static GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
   Aladin aladin=null;

   /** Creation du Frame. */
   protected MyFrame(Aladin aladin,String title) {
      super(env.getDefaultScreenDevice().getDefaultConfiguration());
      setTitle(title);
      this.aladin=aladin;
      Aladin.setIcon(this);
   }

   public void processWindowEvent(WindowEvent e) {
      if( e.getID() == WindowEvent.WINDOW_CLOSING ) {
         if( !aladin.isApplet() || Aladin.extApplet!=null ) {
            aladin.quit(0);
         } else {
            if( aladin.isApplet() && aladin.SCREEN!=null ) aladin.f.setVisible(false);
            else  {
               try { aladin.unDetach(); }
               catch( Exception e1 ) { aladin.f.dispose(); }
            }
         }
      }
      super.processWindowEvent(e);
   }
}

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


package cds.aladin.prop;

import java.awt.*;

import javax.swing.JComponent;

/**
 * Filet de separation
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Filet extends JComponent {
   int type;            // Type de filet
                        // 0 - du vide
                        // 1 - un trait
   int w=60,h=1;

  /** Creation d'un filet.
   * Mode par defaut : 5 pixels de haut
   */
   public Filet() { this(5,1); }

  /** Creation d'un filet.
   * @param h la hauteur du filet en pixels
   */
   public Filet(int h) { this(h,1); }
   
   
   public Dimension getPreferredSize() { return new Dimension(w,h); }

  /** Creation d'un filet.
   * @param h la hauteur du filet en pixels
   * @param type <I>1</I> pour une ligne, <I>0</I> pour du blanc
   */
   public Filet(int h,int type) {
      this.type = type;
      this.h=h;
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      switch( type ) {
         case 0: return;
         case 1:
            int w = getSize().width-20;
            int y = getSize().height/2;
            if( w<=0 ) return;

            // Je trace un filet bicolore
            g.setColor( Color.gray );
            g.drawLine(5,y,w,y);
            g.setColor( Color.white );
            g.drawLine(5,y+1,w,y+1);
            break;
      }
   }
}

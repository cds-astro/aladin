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

import java.awt.Dimension;

import javax.swing.JScrollBar;

/**
 * Un Scrollbar de largeur raisonnable
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (4 mars 2002) Creation
 */
public class MyScrollbar extends JScrollBar {
   final static int LARGEUR=16;
   private int largeur=LARGEUR;
   
   MyScrollbar(int orientation,int value,int visible,int min,int max) {
      super(orientation,value,visible,min,max);
      if( Aladin.DARK_THEME ) setUI( new MyScrollBarUI() );
   }
   
   MyScrollbar(int orientation) {
      super(orientation);
   }
      
   public Dimension getPreferredSize() { return xsize(); }
   public Dimension getMinimumSize() { return xsize(); }
   
   private Dimension xsize() {
      if( getOrientation()==HORIZONTAL )
         return new Dimension(getSize().width,largeur);
      else return new Dimension(largeur,getSize().height);
   }

}

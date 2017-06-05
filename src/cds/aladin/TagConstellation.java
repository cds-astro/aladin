// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

import java.awt.Graphics;
import java.awt.Point;


/**
 * Objet graphique Tag d�di� au label des constellations
 * @version 1.0 nov 2015 - cr�ation
 */
public final class TagConstellation extends Tag {

   protected TagConstellation(Coord c,String id) {
      super(null,c,id);
   }
   
   protected boolean draw(Graphics g,ViewSimple v,int dx,int dy) {
      if( !isVisible() /* || v.isAllSky() */ ) return false;
      Point p = getViewCoord(v,50,50);
      if( p==null ) return false;
      p.x+=dx; p.y+=dy;
      g.setFont( getFont() );
      g.setColor( getColor() );
      
      drawLabel(g,p.x-20,p.y+5);
      return true;
   }
}

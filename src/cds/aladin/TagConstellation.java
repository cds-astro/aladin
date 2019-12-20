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

import java.awt.Graphics;
import java.awt.Point;


/**
 * Objet graphique Tag dédié au label des constellations
 * @version 1.0 nov 2015 - création
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

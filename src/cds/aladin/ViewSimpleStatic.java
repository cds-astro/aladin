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

public class ViewSimpleStatic extends ViewSimple {

   
   protected ViewSimpleStatic(Aladin aladin) {
      super(aladin);

      setBackground(Color.white);
      setOpaque(true);
      setDoubleBuffered(false);
   }
   
   protected void setViewParam(PlanBG p, int width, int height, Coord c, double radius) {
      pref=p;
      setDimension(width,height);
      p.projd.setProjCenter(c.al, c.del);
      p.setDefaultZoom( c, radius, width);
      setZoomXY(p.initZoom,-1,-1,true);
      System.out.println("c="+c+" zoom="+zoom+" radius="+Coord.getUnit(radius)+" rzoom="+rzoom);
   }
   
}

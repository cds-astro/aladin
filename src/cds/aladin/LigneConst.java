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

import java.awt.Point;

/**
 * Objet graphique pour une Ligne de constellation et des périmètres des MOCs
 */
public class LigneConst extends Ligne {

   protected LigneConst(double ra, double dec, LigneConst p) {
      super(ra, dec,null,null,p);
   }
   
   protected LigneConst(double ra, double dec, Plan plan, ViewSimple v, Ligne debligne) {
      super(ra,dec,plan,v,null,debligne);
   }

   protected boolean tooLarge(ViewSimple v,Point p1, Point p2) {
      double dx = p1.x-p2.x;
      double dy = p1.y-p2.y;
      double dist = Math.sqrt(dx*dx+dy*dy);
      double taille = v.getTaille();
      return taille>=30 && dist>50 || taille<30 && dist>v.rv.width;
   }
   
   public boolean hasPhot() { return false; }

//   protected void drawID(Graphics g, ViewSimple v,Point p1,Point p2) {
//      double dy=p2.y-p1.y;
//      double dx=p2.x-p1.x;
//      if( Math.sqrt(dy*dy + dx*dx)<20 && v.getTaille()>10 ) return; // trop petit
//      int a = (p1.x+p2.x)/2;
//      int b = (p1.y+p2.y)/2;
//      g.setFont( Aladin.BOLD );
//      String s = id;
//      int x = a+3;
//      int y = b+(dy*dx>0?-2:12);
//      Color c = g.getColor();
//      Color c1 = (c==Color.red || c==Color.blue) ? Color.white : Color.black;
//      Util.drawStringOutline(g, s,x,y,c,c1);
//      g.setColor(c);
//   }


}

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
 * Objet graphique pour une Ligne de constellation
 */
public class LigneConst extends Ligne {

   protected LigneConst(double ra, double dec, LigneConst p) {
      super(ra, dec,null,null,p);
   }

   protected boolean tooLarge(ViewSimple v,Point p1, Point p2) {
      double dx = p1.x-p2.x;
      double dy = p1.y-p2.y;
      double dist = Math.sqrt(dx*dx+dy*dy);
      double taille = v.getTaille();
      return taille>=30 && dist>50 || taille<30 && dist>v.rv.width;
   }
   
   public boolean hasPhot() { return false; }


}

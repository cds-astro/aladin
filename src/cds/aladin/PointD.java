// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import cds.tools.Util;

/**
 * Manipulation d'un Point en coordonnees reelles
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (29 juillet 2002) creation
 */
public class PointD {

   public double x,y;
   public PointD(double x, double y) { this.x=x; this.y=y; }

   // Override of equals method
   public boolean equals(Object o) {
	  if (o == null) return false;
	  if (! o.getClass().equals(getClass())) return false;
      PointD p = (PointD) o;
      return (x == p.x) && (y == p.y);
   } 
   
   public String toString() { return "("+Util.myRound(""+x,6)+","+Util.myRound(""+y,6)+")"; }
}

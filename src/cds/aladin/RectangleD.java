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

import cds.tools.Util;

/**
 * Manipulation d'un Rectangle en coordonnees reelles
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (30 juillet 2002) creation
 */
public class RectangleD {

   public double x,y,width,height;
   public RectangleD() { };
   public RectangleD(double x, double y, double width, double height) {
      this.x=x; this.y=y;
      this.width=width; this.height=height;
   }

   /** Copie du rectangle */
   public RectangleD copy() {
      return new RectangleD(x,y,width,height);
   }

   /** Return true if (x,y) is in the rectangle */
   public boolean contains(double xc, double yc) {
      return xc>=x && xc<=x+width && yc>=y && yc<=y+height;
   }

   /** Retourne true si le rectangle passée en paramètre est
    * contenu dans le rectangle */
   public boolean inside(RectangleD r) {
      return r.x>=x && r.x<=x+width && r.y>=y && r.y<=y+height;
   }

   public boolean equals(RectangleD r) {
      if( r==this ) return true;
      if( r==null ) return false;
      return x==r.x && y==r.y && width==r.width && height==r.height;
   }

   public String toString() { return Util.myRound(""+x,2)+","+Util.myRound(""+y,2)+" "
         +Util.myRound(""+width,2)+"x"+Util.myRound(""+height,2); }
}

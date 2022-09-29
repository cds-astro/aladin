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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

/**
 * Objet graphique pour une Cote affichant une distance entre 2 objets
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : juin 2013 - creation
 */
public final class CoteDist extends Cote {
   
   private Obj a,b;
   

   protected CoteDist(Obj a, Obj b, ViewSimple v) {
      super(a.raj,a.dej,a.plan,v,new Ligne(b.raj,b.dej,b.plan,v));
      this.a=a;
      this.b=b;
      setId(v);
   }
   
   public Color getColor() { return Color.cyan; }
   
   protected void projection(ViewSimple v) {
      debligne.projection(v);
      super.projection(v);
   }
   
   protected void drawID(Graphics g, ViewSimple v,Point p1,Point p2) {
      drawID1(g,v,p1,p2);
   }
   
   protected boolean draw(Graphics g,ViewSimple v,int dx, int dy) {
      if( a.plan.isFree() || b.plan.isFree() ) return false;
      if( !a.isSelected() || !b.isSelected() ) return false;
      
      boolean rep = super.draw(g,v,dx,dy);
      if( rep ) drawCoteBase(g,v,dx,dy);
      return rep;
   }
}

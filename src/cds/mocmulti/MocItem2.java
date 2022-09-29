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

package cds.mocmulti;

import cds.aladin.MyProperties;
import cds.moc.Moc;


public class MocItem2 extends MocItem {
   public Moc mocRef;   // Couverture de référence (zone connue)
   
   public MocItem2(String id,Moc m, MyProperties p, long dMoc, long dProp) {
      super(id,m,p,dMoc,dProp);
      mocRef=null;
   }
   
   public MocItem2(String id,MyProperties p) {
      super(id,p);
      mocRef=null;
   }
   
   public Moc getMocRef() { return mocRef; }
}

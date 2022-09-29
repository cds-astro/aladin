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


import cds.moc.Moc;
import cds.moc.SMoc;

/**
 * Génération d'un plan SMOC issu d'un PlanSTMoc
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 Création - avril 2021
 */
public class PlanMocFromST extends PlanMoc {

   protected PlanSTMoc father=null; 
   protected boolean flagSelect;     // true - pour montrer une sélection, false - pour montrer un highlight
   
   public PlanMocFromST(Aladin a) { super(a); type = ALLSKYMOC; }
   
   public PlanMocFromST(PlanSTMoc father, boolean flagSelect) {
      super(father.aladin);
      this.flagSelect = flagSelect;
      type = ALLSKYMOC;
      this.father=father;
      setOpacityLevel(1.0f);
      c= flagSelect ? Aladin.COLOR_CONTROL_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND_HIGHLIGHT;
      active=true;
   }
   
   protected int getTimeStackIndex() { return father.getTimeStackIndex(); }
   
   protected Moc getSpaceMocLow1(ViewSimple v,int order,int gapOrder) {
      try {
         SMoc m = (SMoc)moc.clone();
         if( father.lastOrderDrawn!=-1 ) m.setMocOrder(father.lastOrderDrawn);
         return m;
      } catch( Exception e1 ) { }
      return null;
   }
   
   public boolean isDrawingFillIn() { return flagSelect; }
   public boolean isDrawingBorder() { return true; }
}


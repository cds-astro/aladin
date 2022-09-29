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
import cds.moc.TMoc;

/**
 * Génération d'un plan TMOC issu d'un PlanSTMoc
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 Création - avril 2021
 */
public class PlanTMocFromST extends PlanTMoc {

   protected PlanSTMoc father=null; 
   protected boolean flagSelect;     // true - pour montrer une sélection, false - pour montrer un highlight

   public PlanTMocFromST(Aladin a) { super(a); type = ALLSKYTMOC; }
   
   public PlanTMocFromST(PlanSTMoc father, boolean flagSelect ) {
      super(father.aladin);
      this.flagSelect = flagSelect;
      type = ALLSKYTMOC;
      this.father=father;
      setOpacityLevel(1.0f);
      c= flagSelect ? Aladin.COLOR_CONTROL_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND_HIGHLIGHT;
      active=true;
   }
   
   protected int getTimeStackIndex() { return father.getTimeStackIndex(); }
   
   protected Moc getTimeMocLow(int order,int gapOrder) {
      try {
         TMoc m = (TMoc)moc.clone();
         m.setMocOrder(order);
         return m;
      } catch( Exception e1 ) {
         e1.printStackTrace();
      }
      return null;
   }
   
   public boolean isDrawingFillIn() { return flagSelect; }
   public boolean isDrawingBorder() { return true; }

}


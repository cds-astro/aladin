// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

/**
 * Gestion de la fenetre associee a la creation d'un MOC de plus faible résolution
 * à partir d'un autre MOC
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (déc 2013) Creation
 */
public final class FrameMocGenRes extends FrameMocGenImg {
   
   protected FrameMocGenRes(Aladin aladin) {
      super(aladin);
   }
   
   protected void createChaine() {
      super.createChaine();
      TITLE = a.chaine.getString("MOCGENTITLE");
      INFO  = a.chaine.getString("MOCGENORDINFO");
      PLANE = a.chaine.getString("MOCFILTERINGMOC");
   }

   protected boolean isPlanOk(Plan p) { return p.isMoc(); }
   
   protected void addSpecifPanel(JPanel p,GridBagConstraints c,GridBagLayout g) { }
   
   @Override
   protected void submit() {
      try {
         PlanMoc [] ps = new PlanMoc[]{ (PlanMoc)getPlan(ch[0]) };
         int order=getOrder();
         a.calque.newPlanMoc(ps[0].label,ps,5,order,-1);
         hide();

      } catch ( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         Aladin.error("MOC generation failed !");
      }

   }

   @Override
   protected void adjustWidgets() { };
}

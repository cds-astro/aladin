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

import java.util.Vector;

import javax.swing.JComboBox;

/**
 * Gestion de la fenetre associee a la creation d'un MOC à partir des sources sélectionnées
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (mai 2018) Creation
 */
public final class FrameSTMocGenObj extends FrameSTMocGenCat {
   
   private Plan p;

   protected FrameSTMocGenObj(Aladin aladin) {
      super(aladin);
   }
   
   protected void maj() { 
      Vector v = a.view.getSelectedObjet();
      p = a.calque.newPlanCatalogBySources(v, "Selected sources",true,false);
      show();
   }
   
   protected void createChaine() {
      super.createChaine();
      INFO  = a.chaine.getString("STMOCGENOBJINFO");
   }
   
   protected Plan[] getPlan() { return new Plan[] { p }; }
   
   // Procedure interne utilisee par createImageChoice() et adjustImageChoice()
   synchronized protected void setItems(JComboBox c) {
      for (int i=0; i<choicePlan.length; i++) c.addItem(choicePlan[i].label);
   }
   
   protected Plan getPlan(JComboBox c) {
      int i=c.getSelectedIndex();
      if (i<0) return null;
      return choicePlan[i];
   }


   
   protected void adjustImageChoice(JComboBox c, int defaut) {
      c.removeAllItems();
      c.addItem(p.label);
      c.setSelectedIndex(0);
   }


   protected void adjustWidgets() { ch[0].setEnabled(false); };

}

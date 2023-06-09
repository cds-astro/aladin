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


/**
 * Plan libre (NO)
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class PlanFree extends Plan {

  /** Creation d'un plan vide
   * @param aladin reference
   */
   protected PlanFree(Aladin aladin) {
      this.aladin= aladin;
      init();
   }
   
   /** Creation d'un plan bidon pour g�rer le param�tre "body" pour les Projections
    * @param aladin reference
    * @param body le body associ� � ce plan
    */
    protected PlanFree(Aladin aladin, String body) {
       this(aladin);
       this.body=body;
    }

}


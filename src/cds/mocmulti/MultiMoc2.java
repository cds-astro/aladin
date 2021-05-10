// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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


// Copyright 2011 - UDS/CNRS
// The MOC API project is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of MOC API java project.
//
//    MOC API java project is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    MOC API java project is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with MOC API java project.
//


import cds.aladin.MyProperties;
import cds.moc.SMoc;


/**
 * MultiMoc utilisant MocItem2
 * @author Pierre Fernique [CDS]
 */
public class MultiMoc2 extends MultiMoc {
   
   public MultiMoc2() { super(); }
   
   public MultiMoc2( MultiMoc moc ) {
      for( MocItem mo : moc ) {
         MocItem2 mo2 = new MocItem2(mo.mocId, mo.moc, mo.prop, mo.dateMoc, mo.dateProp);
         add(mo2);
      }
   }
   
   /** Add or replace a MOC to the MultiMoc.
    * @param mocId  MOC identifier (unique)
    * @param moc MOC to memorize
    */
   public void add(String mocId, SMoc moc, MyProperties prop, long dateMoc, long dateProp) throws Exception {
      if( moc!=null ) {
         int o = moc.getMocOrder();
         if( o==SMoc.MAXORD_S ) o = moc.getDeepestOrder();  // A cause du bug
         if( mocOrder<o) mocOrder=o;
      }
      MocItem2 mi = new MocItem2(mocId,moc,prop,dateMoc,dateProp);
      add( mi );
   }
   
   /** Add or replace a MyProperties to the MultiMoc. 
    * Le MOC reste � null, la date du MOC � 0. La date du MyProperties
    * est prise directement de la valeur de la propri�t� TIMESTAMP (si pr�sent)
    * @param mocId Identificateur de l'enregistrement
    * @param prop Propri�t�s
    * @throws Exception
    */
   public void add(MyProperties prop) throws Exception {
      String id = getID(prop);
      MocItem2 mi = new MocItem2(id,prop);
      add( mi );
   }
   
   /** Return directly a MocItem */
   public MocItem2 getItem(String mocId) {
      return (MocItem2) map.get(mocId);
   }
   

   
}

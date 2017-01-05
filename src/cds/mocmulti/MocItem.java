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
package cds.mocmulti;

import cds.aladin.MyProperties;
import cds.moc.HealpixMoc;


public class MocItem {
   public String mocId;
   public HealpixMoc moc;
   public MyProperties prop;
   public long dateMoc,dateProp;
   
   public MocItem(String id,HealpixMoc m, MyProperties p, long dMoc, long dProp) {
      mocId=id; moc=m; prop=p;
      dateMoc=dMoc; dateProp=dProp;
      
      // Ajout du timestamp directement dans les propri�t�s
      if( prop!=null ) prop.replaceValue(MultiMoc.KEY_TIMESTAMP,getPropTimeStamp()+"");
   }
   
   /** Initialisation qu'avec des MyProperties. Le timestand est pris
    * de l'enregistrement lui-m�me.
    * @param id
    * @param p
    */
   public MocItem(String id,MyProperties p) {
      mocId=id;
      prop=p;
      moc=null;
      dateMoc=dateProp=0L;
      
      String s = prop.getProperty(MultiMoc.KEY_TIMESTAMP);
      if( s!=null ) {
         try { dateProp = Long.parseLong(s); }
         catch( Exception e) {}
      }
   }
   
   /** Donne le timestamp associ�e aux propri�t�s (correspond � la derni�re date
    * de maj du fichier des properties (en millisecondes Unix) */
   public long getPropTimeStamp() {
      return dateProp!=0 ? dateProp : dateMoc;
   }
   
   public MocItem copy() { return new MocItem(mocId,moc,prop,dateMoc,dateProp); }
   
   public boolean equals(MocItem m) {
      if( this==m ) return true;
      if( this.prop==null && m.prop!=null 
            || this.prop!=null && m.prop==null ) return false;
      return this.prop!=null && this.prop.equals(m.prop);
   }
}

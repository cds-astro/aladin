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

package cds.aladin;

import java.util.ArrayList;

/**
 * Pour g�rer l'historique des cibles successives et pouvoir y revenir facilement
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (oct 2017) Creation
 */
public class TargetHistory {
   Aladin aladin;
   ArrayList<String> list;
   
   protected TargetHistory(Aladin aladin) {
      this.aladin = aladin;
      list = new ArrayList<>();
   }
   
   /** Ajoute une target, en la replacant "dessus" si elle existe d�j� */
   protected void add(String target ) {
      if( target==null || target.trim().length()==0 ) return;
      
      String t = getValue( target );
      if( !Command.isDateCmd(t) && !Localisation.notCoord(t) && !Localisation.hasFoxSuffix(t) ) {
         target = target+" "+aladin.localisation.getFrameFox();
      }
      remove(target);
      list.add( target );
   }
   
   static final String SEPARATOR = ": ";
   static final String CURRENTPOS = "Last"+SEPARATOR;
   
   protected void setCurrentPos(String s) {
      removeCurrentPos();
      list.add(CURRENTPOS+s);
   }
   
   private void removeCurrentPos() {
      int i;
      for( i=0; i<list.size(); i++ ) {
         if( list.get(i).startsWith(CURRENTPOS) ) break;
      }
      if( i<list.size() ) list.remove(i);

   }
   
   /** Le target peut �tre �ventuellement pr�fix� par un identificateur (texte libre)
    * et s�par� par "->". Cette m�thode retourne le target sans son �ventuel pr�fixe
    * @param s le target �ventuellement pr�c�d� d'une pr�fixe (ex: myTarget : M1)
    * @return le target sans son pr�fixe
    */
   static protected String getValue(String s) {
      int i=s.indexOf(SEPARATOR);
      if( i<0 ) return s;
      return s.substring(i+SEPARATOR.length());
   }
   
   protected boolean remove( String target ) {
      int i = find( target );
      if( i>=0 )  list.remove(i);
      return i>=0;
   }
   
   
   protected int find( String target ) {
      for( int i=0; i<list.size(); i++ ) {
         if( target.equals( list.get(i)) ) return i;
      }
      return -1;
   }
   
   protected int size() { return list.size(); }
   
   /** Retourne la derni�re target m�moris�e */
   protected String getLast() { return list.size()==0 ? "" : getValue( list.get( list.size()-1 ) ); }
   
   /** Retourne une liste de nb targets � partir de l'indice index. l'index 0 est celui
    * de la derni�re target ins�r�e, 1 pour l'avant-derni�re, etc...
    * @param index
    * @param nb
    * @return
    */
   protected ArrayList<String> getTargets(int index, int nb) {
      ArrayList<String> a = new ArrayList<>(nb);
      int n=list.size()-1-index;
      for( int i=0; i<nb && n>=0; i++, n-- ) a.add( list.get(n) );
      if( n>0 ) a.add("...");
      return a;
   }
   

}

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

package cds.allsky;

/** La méthode utilisée pour calculer les pixels d'une tuile d'ordre N à l'ordre N-1 */
public enum ModeTree {
   
      treeMedian,        // Médiane des 4 pixels du niveau N+1
      treeMiddle,   // L'un des deux pixels de valeurs intermédiaires
      treeMean,          // Moyenne des 4 pixels du niveau N+1
      treeFirst;         // Le premier pixels des 4 pixels du niveau N+1

   public static ModeTree getDefault(int bitpix) {
      return bitpix==0 ? treeMedian : treeMean;   // En couleur, on préfèrera la médiane first
   }
   
   public static String contains(String test) {
      if( test==null ) return null;
      test=test.toUpperCase();
      for( ModeTree c : ModeTree.values()) {
          if (c.name().toUpperCase().endsWith(test))  return c.name();
      }
      return null;
  }
   
   static public String list() {
      StringBuilder s = new StringBuilder();
      for( ModeTree m: values() ) {
         if( s.length()>0 ) s.append('|');
         s.append(m.toString());
      }
      return s.toString();
   }
   
   public static String getExplanation(ModeTree m) {
      if( m==treeMedian )   return m+": "+"The median of the 4 sublevel pixel values";
      if( m==treeMiddle )   return m+": "+"One of the two intermediate values amongs the 4 sublevel pixel values";
      if( m==treeMean )     return m+": "+"The mean of the 4 sublevel pixel values";
      if( m==treeFirst )    return m+": "+"The first of the 4 sublevel pixel values";
      return "";
   }
}

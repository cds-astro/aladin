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

// La méthode utilisée pour fusionner les HiPS
public enum ModeMerge {

   mergeKeep,       // Si la valeur existante du pixel est != BLANK, alors le garde
   mergeOverwrite,  // si la nouvelle valeur du pixel est != BLANK, alors remplace la valeur existante
   mergeMean,       // Effectue la moyenne des valeurs
   mergeAdd,        // Effectue la somme des valeurs
   mergeSub,        // Effectue la soustraction des valeurs
   mergeMul,        // Effectue une multiplication
   mergeDiv,        // Effectue une division

   // La méthode agit globalement au niveau des tuiles HiPS
   mergeOverwriteTile,   // Ecrase les tuiles déjà calulées par les nouvelles
   mergeKeepTile,         // Conserve en l'état toutes les tuiles déja calculées, et n'ajoute que les nouvelles

   // Dans le cas de la création d'un cube composite
   link,          // Les tuiles du cube composite seront en fait des liens symboliques
   copy;          // Les tuiles du cube composite seront des copies des tuiles originales

   public static ModeMerge getDefault() {
      return mergeOverwrite;
   }
   
   public static String contains(String test) {
      if( test==null ) return null;
      test=test.toUpperCase();
      for( ModeMerge c : ModeMerge.values()) {
          if (c.name().toUpperCase().endsWith(test))  return c.name();
      }
      return null;
  }
   
   static public String list() {
      StringBuilder s = new StringBuilder();
      for( ModeMerge m: values() ) {
         if( s.length()>0 ) s.append('|');
         s.append(m.toString());
      }
      return s.toString();
   }
   
   public static String getExplanation(ModeMerge m) {
      if( m==mergeKeep )        return m+": "+"Replace pixel values only for pixels not yet computed or BLANK";
      if( m==mergeOverwrite )   return m+": "+"Replace existing pixel values if the new value is not BLANK";
      if( m==mergeMean )     return m+": "+"Compute the weighted average value based on the new pixel value and the existing one";
      if( m==mergeAdd )         return m+": "+"Add pixels values";
      if( m==mergeSub )         return m+": "+"Substraction only for existing values";
      if( m==mergeMul )         return m+": "+"Multiplication only for existing values";
      if( m==mergeDiv )         return m+": "+"Division only for existing values";
      if( m==mergeOverwriteTile ) return m+": "+"Add new tiles, and if necessary, replace existing tiles";
      if( m==mergeKeepTile )    return m+": "+"Add new tiles but only for those not yet computed";
      if( m==link )        return m+": "+"Composite cube tiles based on symbolic links on original tiles";
      if( m==copy )        return m+": "+"Composite cube tiles are copies of original tiles";
      return "";
   }
}

// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.allsky;

public enum Mode {

   // La méthode utilisé pour coadder agit au niveau des pixels
   KEEP,          // Si la valeur existante du pixel est != BLANK, alors le garde
   OVERWRITE,     // si la nouvelle valeur du pixel est != BLANK, alors remplace la valeur existante
   AVERAGE,       // Effectue la moyenne de la valeur existante avec la nouvelle valeur
   ADD,           // Effectue la somme de la valeur existante avec la nouvelle valeur

   // La méthode utilisé pour coadder agit au niveau des tuiles HEALPix
   REPLACETILE,    // Recalcule toutes les tuiles (de niveau le plus bas)
   KEEPTILE,       // Conserve en l'état toutes les tuiles (de niveau le plus bas) déjà calculées

   // Dans le cas de la création d'un cube composite
   LINK,          // Les tuiles du cube composite seront en fait des liens symboliques
   COPY;          // Les tuiles du cube composite seront des copies des tuiles originales

   public static Mode getDefault() {
      return OVERWRITE;
   }

   public static String getExplanation(Mode m) {
      if( m==KEEP )        return m+": "+"Add pixel values only for pixels not yet computed or BLANK";
      if( m==OVERWRITE )   return m+": "+"Replace existing pixel values if the new value is not BLANK";
      if( m==AVERAGE )     return m+": "+"Compute the weighted average value based on the new pixel value and the existing one";
      if( m==ADD )         return m+": "+"Compute the sum value of new new pixel value and the existing one";
      if( m==REPLACETILE ) return m+": "+"Add new tiles, and if necessary, replace existing tiles (low level tiles)";
      if( m==KEEPTILE )    return m+": "+"Add new tiles but only for those not yet computed (low level tiles)";
      if( m==LINK )        return m+": "+"Composite cube tiles based on symbolic links on original tiles";
      if( m==COPY )        return m+": "+"Composite cube tiles are copies of original tiles";
      return "";
   }
}

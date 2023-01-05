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

/** Les méthodes utilisées pour coadder les images d'origine sur leurs recouvrements */
public enum ModeOverlay {

   overlayNone,         // Prend la première valeur des pixels
   overlayMean,         // Effectue la moyenne des valeurs des pixels
   overlayFading,       // Effectue la moyenne des valeurs, pondérée par la distance au bord
   overlayAdd;          // Effectue la somme des valeurs des pixels (même si l'une est manquante)

   public static ModeOverlay getDefault() {
      return overlayMean;
   }
   
   public static String contains(String test) {
      if( test==null ) return null;
      test=test.toUpperCase();
      for( ModeOverlay c : ModeOverlay.values()) {
          if (c.name().toUpperCase().endsWith(test))  return c.name();
      }
      return null;
  }
   
   static public String list() {
      StringBuilder s = new StringBuilder();
      for( ModeOverlay m: values() ) {
         if( s.length()>0 ) s.append('|');
         s.append(m.toString());
      }
      return s.toString();
   }
   
   public static String getExplanation(ModeOverlay m) {
      if( m==overlayNone )    return m+": "+"Take only one progenitor pixel value";
      if( m==overlayMean )    return m+": "+"Mean of the progenitor pixel values";
      if( m==overlayFading )  return m+": "+"Mean of the progenitor pixel values + fading effect";
      if( m==overlayAdd )     return m+": "+"Addition of the progenitor pixel values";
      return "";
   }
}

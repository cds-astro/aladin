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

package cds.aladin;

import cds.tools.Util;

/**
 * Gestion d'un noyau de convolution
 * @author Pierre Fernique [CDS]
 * @version 1.0 - octobre 2010
 */
public class Kernel {
   
   static final int MAXRADIUS = 500;    // Rayon maximum du noyau de convolution
   protected double [][]matrix;         // matrice de la convolution (nécessairement carrée et de taille impaire)
   protected double []gaussian;         // Vecteur centrale normalisé de la matrice si elle est gaussienne
   protected String name;               // Nom de la matrice
   
   public Kernel() {}

   protected Kernel(String name,double [][]kernel) {
      this.name=name;
      this.matrix=kernel;
   }
   
   /** Normalise la matrice - POUR LE MOMENT JE NE LE FAIS PAS */
   public void normalize() {
//      double somme = 0;
//      for( int y=0; y<matrix.length; y++ ) {
//         for( int x=0; x<matrix[y].length; x++ ) somme += matrix[y][x];
//      }
//      for( int y=0; y<matrix.length; y++ ) {
//         for( int x=0; x<matrix[y].length; x++ ) matrix[y][x] /= somme;
//      }
   }
   
   /** Affichage de la matrice de convolution en tant que chaine de caractères */
   public String toString() {
      StringBuffer s = new StringBuffer("kernel: \""+name+"\":\n");
      for( int y=0; y<matrix.length; y++ ) {
         s.append("(");
         for( int x=0; x<matrix[y].length; x++) s.append(Util.align(""+matrix[y][x],6) );
         s.append(")\n");
      }
      return s.toString();
   }
}


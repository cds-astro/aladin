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

/**
 * Liste des options supportées par Hipsgen
 * @author Pierre Fernique [CDS]
 *
 */
public enum ParamOption {
      
   clean            ("Delete previous computations"),
   n                ("Just print process information, but do not execute it"),
   color            ("Colorize console log messages"),
   nocolor          ("Uncolorize console log messages"),
   nice             ("[MIRROR] Slow download for avoiding to overload remote http server"),
   clone            ("[MIRROR] Force clone (ignoring \"unclonable\" hips_status)",A.UNDOC),
   notouch          ("Do not touch the hips_release_date"),
   hhhcar           ("[INDEX] Generate hhh file for an all sky image"),
   trim             ("[TILES,CONCAT,APPEND] Trim FITS tiles if possible"),
   gzip             ("[TILES,CONCAT,APPEND] Gzip FITS tiles"),
   cds              ("[LINT] CDS dedicated LINT features",A.UNDOC),
   d                ("Debug messages"),
   h                ("Inline help"),
   man              ("Full inline man (may bo followed by a parameter or an action for a full explanation)"),
   html             ("HTML output",A.UNDOC),
   ;
   
   class A {
      static final int UNDOC=1;         // Option non documentée
      static final int TEST =2;         // prototypage d'une nouvelle option
   }

   /** Les champs */
   private String info;    // Courte description
   private int m=0;        // modes associés (cf class A)
   
   ParamOption(String info) { this.info=info;}
   ParamOption(String info,int m) { this.info=info; this.m=m;}
   
   /** Retourne le nom de l'option suivi de sa description */
   String info() { return "-"+this+" => "+info; }
   
   /** Surcharge de l'égalité pour ignorer la case des lettres */
   boolean equals(String s) {
      return ("-"+toString().toLowerCase()).equals(s.toLowerCase());
   }
   
   /********************************* Méthodes statiques  *********************************/

   /**
    * Retourne l'aide en ligne pour l'ensemble des options
    * @return l'aide en ligne
    */
   static String help() {
      StringBuilder s = new StringBuilder();
      for( ParamOption a : values() ) {
         if( (a.m&(A.TEST|A.UNDOC)) !=0 ) continue;
         String s1 = String.format("%-7s: ",a);
         s.append("   -"+s1+a.info+"\n");
      }
      return s.toString();
   }

   
}

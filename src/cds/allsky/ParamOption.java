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

public enum ParamOption {
      
   clean            ("Delete previous computations"),
   n                ("Just print process information, but do not execute it"),
   notouch          ("Do not touch the hips_release_date"),
   color            ("Colorize console log messages"),
   nocolor          ("Uncolorize console log messages"),
//   check            ("[MIRROR] Check date&size of local tiles",A.UNDOC),
//   nocheck          ("[MIRROR] Do not check date&size of local tiles"),
   nice             ("[MIRROR] Slow download for avoiding to overload remote http server"),
   clone            ("[MIRROR] Force clone (ignoring \"unclonable\" hips_status)",A.UNDOC),
   hhhcar           ("[INDEX] Generate hhh file for an all sky image"),
//   live             ("[TILES,CONCAT,APPEND] Incremental HiPS (keep weight associated to each HiPS pixel"),
   trim             ("[TILES,CONCAT,APPEND] Trim FITS tiles if possible"),
   gzip             ("[TILES,CONCAT,APPEND] Gzip FITS tiles"),
   cds              ("[LINT] CDS dedicated feature",A.UNDOC),
   d                ("Debug messages"),
   h                ("Inline help"),
   man              ("Full inline man (may bo followed by a parameter or an action for a full explanation)"),
;
   
   String info;
   int m=0;
   
   ParamOption(String info) { this.info=info;}
   ParamOption(String info,int m) { this.info=info; this.m=m;}
   
   static String help() {
      StringBuilder s = new StringBuilder();
      for( ParamOption a : values() ) {
         if( (a.m&(A.TEST|A.UNDOC)) !=0 ) continue;
         String s1 = String.format("%-7s: ",a);
         s.append("   -"+s1+a.info+"\n");
      }
      return s.toString();
   }

   class A {
      static final int UNDOC=1;
      static final int TEST =2;
   }

   
   public boolean equals(String s) {
      s = s.toLowerCase();
      return ("-"+toString().toLowerCase()).equals(s);
   }
   
   String info() { return "-"+this+" => "+info; }
}

// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.mocmulti;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Unite {

   /** retourne un temps en milliseconde sous une forme lisible 3j 5h 10mn 3.101s */
   static public String getTemps(long ms) { return getTemps(ms,false);  }
   static public String getTemps(long ms,boolean round) {
      StringBuffer s = new StringBuffer();
      if( ms>86400000 ) { long j = ms/86400000; ms -= j*86400000; s.append(j+"j"); }
      if( ms>3600000 ) { long h = ms/3600000; ms -= h*3600000; if( s.length()>0 ) s.append(' '); s.append(h+"h"); }
      if( ms>60000 ) { long m = ms/60000; ms -= m*60000; if( s.length()>0 ) s.append(' '); s.append(m+"m"); }
      if( s.length()>0 ) s.append(' '); s.append( (round ? ""+ms/1000 : ""+ms/1000.)+"s");
      return s.toString();
   }
   
   static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm";
   static final SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
   static {
      TimeZone utc = TimeZone.getTimeZone("UTC");
      sdf.setTimeZone(utc);
   }
   
   public static final String getDate(long ms) {
      return sdf.format(new Date(ms));
   }

   /**
    * Affiche le chiffre donné avec une unité de volume disque (K M T)
    * @param val taille en octets
    * @return le volume disque dans une unite coherente + l'unite utilisee
    */
   static final public String unites[] = {"B","KB","MB","GB","TB","PB","EB","ZB"};
   static final public String getUnitDisk(long val) {
      return getUnitDisk(val, 2);
   }
   static final public String getUnitDisk(long val, int format) {
      int unit = 0;
      long div,rest=0;
      boolean neg=false;
      if( val<0 ) { neg=true; val=-val; }
      while (val >= 1024L && unit<unites.length-1) {
         unit++;
         div = val / 1024L;
         rest = val % 1024L;
         val=div;
      }
      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(format);
      double x = val+rest/1024.;
      return (neg?"-":"")+nf.format(x)+unites[unit];
   }

   static DecimalFormat DF;
   static {
      DF = new DecimalFormat();
      DF.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
      DF.setGroupingSize(0);
   }

   /** Arrondi intelligemment */
   static final public String myRound(double x) {

      // cas particulier de la notation scientifique
      String s = x+"";
      int posV; // position de la virgule
      int posE; // position de l'exposant
      if( (posE=s.indexOf('E'))>0 ) {
         if( (posV=s.indexOf('.'))>0) {
            if( posV+4>posE ) return s;   // déjà pas bcp de décimales
            return s.substring(0,posV+4)+s.substring(posE);
         }
      }

      // cas général
      double y = Math.abs(x);
      if( y>1000 ) DF.setMaximumFractionDigits(0);
      else if( y>100 ) DF.setMaximumFractionDigits(1);
      else if( y>10 ) DF.setMaximumFractionDigits(2);
      else if( y>1 ) DF.setMaximumFractionDigits(3);
      else if( y>0.1 ) DF.setMaximumFractionDigits(4);
      else if( y>0.01 ) DF.setMaximumFractionDigits(5);
      else DF.setMaximumFractionDigits(6);

      return DF.format(x);
   }



}

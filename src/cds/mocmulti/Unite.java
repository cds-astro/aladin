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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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


}

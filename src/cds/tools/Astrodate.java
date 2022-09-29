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

package cds.tools;

import java.util.Locale;

import org.jastronomy.jsofa.JSOFA;

import cds.aladin.Tok;
import cds.astro.Unit;

/**
 * Quelques fonctions pour la conversions des dates astronomiques.
 * Merci a : http://perso.wanadoo.fr/jean-paul.cornec/formule_jj.htm
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (8 juillet 2005) Creation
 */
public class Astrodate {
   
   /** Conversion d'une Jour Julien en jour Julien Modifi� */
   static public double JDToMJD(double JD) { return JD-2400000.5; }
   
   /** Conversion d'une Jour Julien Modifi� en jour Julien */
   static public double MJDToJD(double MJD) { return MJD+2400000.5; }
   
   
   /** Conversion d'une date ISOTIME (UTC) en jour julien
    * Rq: Reconnaissance stricte de la syntaxe suivante uniquement
    *  ISOTIME1 => YYYY-MM-DD[THH:MM[:SS.SS]]...
    */
   static public double ISOToJD(String date) throws Exception {
      int i = date.indexOf('-');
      if( i<0 ) throw new Exception("Not ISO date");
      
      Tok tok = new Tok(date,"-T");
      double A = Double.parseDouble( tok.nextToken() );
      double M = Double.parseDouble( tok.nextToken() );
      double J = Double.parseDouble( tok.nextToken() );
      
      double HH=0,MM=0,SS=0;
      if( tok.hasMoreTokens() ) HH = Double.parseDouble( tok.nextToken() );
      if( tok.hasMoreTokens() ) MM = Double.parseDouble( tok.nextToken() );
      if( tok.hasMoreTokens() ) SS = Double.parseDouble( tok.nextToken() );
      
      double res = dateToJD(A,M,J,HH,MM,SS);
      return res;
   }
   
   /** Conversion d'une date ISOTIME (UTC) en jour julien
    * Tol�re quelques autres syntaxes
    *  ISOTIME1 => YYYY-MM-DD[THH:MM[:SS.SS]]...
    *  ISOTIME2 => YYYYMMDD[THHMM[SS.SS]]...
    *  DATE => DD/MM/YYYY... (�ventuellement)
    *  L'ann�e peut �tre sur 2 digits (=> +1900) sauf pour ISOTIME2
    */
   static public double dateToJD(String date) {
      boolean slash=false;
      
      int i = date.indexOf('-');
      if( i<0 ) { i = date.indexOf('/'); slash=true; }
      if( i<0 ) i = date.indexOf(' ');
      
      // aucun s�parateur, donc YYYYMMDD[THHMMSS]... => conversion en YYYY-MM-DD...
      if( i<0 ) {
         String yyyy,mm,dd,hh,m,ss;
         yyyy=mm=dd=hh=m=ss=null;
         yyyy = date.substring(0,4);
         mm = date.substring(4,6);
         dd = date.substring(6,8);
         int offsetT = date.indexOf('T');
         if( offsetT>0 ) {
            hh = date.substring(9,11);
            m = date.substring(11,13);
            if( date.length()>13 ) ss=date.substring(13);
         }
         date=yyyy+"-"+mm+"-"+dd;
         if( hh!=null ) {
            date+="T"+hh+":"+m;
            if( ss!=null ) date+=":"+ss;
         }
      }
      
      // On supprime un �ventuel suffixe Z, (TT) ...
      int suffix = date.length();
      for( suffix--; suffix>0; suffix--) {
         if( Character.isDigit( date.charAt(suffix) ) ) { suffix++; break; }
      }
      if( suffix>0 ) date = date.substring(0,suffix);
      
      Tok tok = new Tok(date,"-/T: ");
      double A = Double.parseDouble( tok.nextToken() );
      double M = Double.parseDouble( tok.nextToken() );
      double J = Double.parseDouble( tok.nextToken() );
      
      // HORREUR POUR SDSS, ON SUPPoSE QU'ACEC DES SLASHS LE FORMAT EST DD/MM/AA
      if( slash ) { double x = A; A=J; J=x; }
      
      double HH=0,MM=0,SS=0;
      if( tok.hasMoreTokens() ) HH = Double.parseDouble( tok.nextToken() );
      if( tok.hasMoreTokens() ) MM = Double.parseDouble( tok.nextToken() );
      if( tok.hasMoreTokens() ) SS = Double.parseDouble( tok.nextToken() );
      
      // Cas d'une ann�e sur deux digits
      if( A<100 ) A+=1900;
      
      double res = dateToJD(A,M,J,HH,MM,SS);
      return res;
   }
   
//   static public void main(String [] a) {
//      String s;
//      s="1967-05-09T10:39:10.1Z";
//      double b = dateToJD(s);
//      System.out.println(s+" => "+b +" => "+JDToDate(b));
//      s="67-05-09T10:39:10.1";
//      b = dateToJD(s);
//      System.out.println(s+" => "+b+" => "+JDToDate(b));
//      s="19670509T103910.1";
//      b = dateToJD(s);
//      System.out.println(s+" => "+b+" => "+JDToDate(b));
//      s="1967/05/09 10:39:10.1";
//      b = dateToJD(s);
//      System.out.println(s+" => "+b+" => "+JDToDate(b));
//      s="67/5/9";
//      b = dateToJD(s);
//      System.out.println(s+" => "+b+" => "+JDToDate(b));
//      s="1967 05 09";
//      b = dateToJD(s);
//      System.out.println(s+" => "+b+" => "+JDToDate(b));
//      s="1967-05-09";
//      b = dateToJD(s);
//      System.out.println(s+" => "+b+" => "+JDToDate(b));
//      s="1967/05/09";
//      b = dateToJD(s);
//      System.out.println(s+" => "+b+" => "+JDToDate(b));
//      s="1967-05-09T10:39";
//      b = dateToJD(s);
//      System.out.println(s+" => "+b+" => "+JDToDate(b));
//     
//   }
   
   /** Conversion d'une date en jour julien
    * @param A ann�e
    * @param M mois (janvier = 1)
    * @param J jour
    * @param HH heure (UTC)
    * @param MM minute (UTC)
    * @param SS seconde (UTC)
    * @return
    */
   static public double dateToJD(double A, double M, double J, double HH, double MM, double SS) {
      long B;
      double T;
      
      if( M<3 ) {  A--; M+=12; }
      if( A>1582 || (A==1582 && M>10) || A==1582 && M==10 && J>=15 ) {
         long C = (long)(A/100);
         B = 2-C+C/4;
      } else B=0;
      T = HH/24. + MM/1440. + SS/86400.;
      
      return (long)(365.25 * (A+4716)) + (long)(30.6001 * (M+1)) + J + T + B - 1524.5;
   }
   
   /**
    * Conversion d'une date en jour julien en AAAA-MM-JJTHH:MM:SS[.SSS] (UTC)
    * @param JD Jour Julien
    * @return
    */
   static public String JDToDate(double JD) { return JDToDate(JD,true); }
   static public String JDToDate(double JD, boolean withTime) { return JDToDate(JD,withTime,true); }
   static public String JDToDate(double JD, boolean withTime, boolean flagDecimalSecond) {
      long Z = (long)(JD+0.5);
      double F = JD+0.5 - Z;
      long G;
      if( Z<2299161 ) G=Z;
      else {
         long a =  (long)( (Z-1867216.25)/36524.25 );
         G = Z+1+a-a/4;
      }
      long B = G+1524;
      long C = (long)( (B - 122.1) / 365.25 );
      long D = (long)( 365.25 * C );
      long E = (long)( (B - D ) / 30.6001 );
      
      double j = B - D -(long)(30.6001 * E) + F;
      long J = (long)j;
      long M = E<13.5?E-1:E-13;
      long A = (M<2.5)?C-4715:C-4716;
      long s = (long)( (j-J)*86400 );
      double ss = (j-J)*86400.;
      long HH = s/3600;
      long MM = (s  - HH*3600 )/60;
      long SS = (s - HH*3600 - MM*60);
      double SSf = (ss - HH*3600 - MM*60);
      if( !withTime || !flagDecimalSecond || SSf-(int)SSf < 0.001 ) 
         return A+"-"+dd(M)+"-"+dd(J)+ (withTime ? "T"+dd(HH)+":"+dd(MM)+":"+dd(SS) : "");
      String s1 = String.format(Locale.ROOT,"%02.3f",SSf);
      return A+"-"+dd(M)+"-"+dd(J)+ "T"+dd(HH)+":"+dd(MM)+":"+s1;
   }
   
   // Ajout d'un z�ro en primer digit si n�cessaire
   static private String dd(long x) { return x<10 ? "0"+x : ""+x; }
   
   
   /** Conversion de l'ann�e d�cimale en Jour Julien
    * @param Yd Ann�e D�cimale
    * @return Jour Julien
    */
   static public double YdToJD(double Yd) {
      return 2451545.0 + (Yd-2000)*365.25;
   }
   
   /** Conversion d'une date en jour julien en ann�e d�cimale
    * @param JD Jour Julien
    * @return Ann�e d�cimale
    */
   static public double JDToYd(double JD) {
      return (JD-2451545.0)/365.25 +2000;
   }
   
   /** Conversion d'une date Unix (sec depuis 1/1/1970) en JD */
   static public double UnixToJD( long time ) {
      return time/86400. + 2440587.5;
   }
   
   /** Conversion d'une date JD en Unix (sec depuis 1/1/1970) */
   static public long JDToUnix( double JD ) {
      return (long)((JD - 2440587.5) * 86400.);
   }
   
   static public final int JD      = 13; 
   static public final int MJD     = 14; 
   static public final int ISOTIME = 15; 
   static public final int YEARS   = 16; 
   static public final int DATE    = 17; 
   static public final int UNIX    = 18; 
   static public final int BES     = 19; 
   
   /** Conversion JD en String */
   static public String editTime( double jd, int timeMode ) {
      try {
         if( timeMode==JD )      return jd+"";
         if( timeMode==MJD )     return JDToMJD(jd)+"";
         if( timeMode==ISOTIME ) return JDToDate(jd);
         if( timeMode==YEARS )   return JDToYd(jd)+"";
         if( timeMode==UNIX )    return JDToUnix(jd)+"";
         if( timeMode==BES )     return JSOFA.jauEpb(jd,0.)+"";
         
      } catch( Exception e ) { }
      return null;
   }
   
   /** Conversion d'une String en JD */
   static public double parseTime( String date, int timeMode ) {
      try {
         if( timeMode==JD )      return Double.parseDouble(date);
         if( timeMode==MJD )     return MJDToJD( Double.parseDouble(date) );
         if( timeMode==ISOTIME ) return dateToJD( date );
         if( timeMode==YEARS )   return YdToJD( Double.parseDouble(date) );
         if( timeMode==UNIX )    return UnixToJD( Long.parseLong(date) );
         if( timeMode==BES )     return getSofaJD( JSOFA.jauEpb2jd( Double.parseDouble(date) ) );
         
         // Date format inconnue
         return MJDToJD( Util.ISOToMJD( Util.parseDate( date ) ) );
      } catch( Exception e ) { }
      return Double.NaN;
   }
   
   static private double getSofaJD( JSOFA.JulianDate jd ) { return jd.djm0 + jd.djm1; }
   
   static public double getTCBTime(double jdTime, String timeScale, String refPosition ) throws Exception {
      if( timeScale.equals("TCB") && refPosition.equals("BARYCENTER") ) return jdTime;
      
      double deltaT=-1;   // JE NE SAIS PAS ENCORE CE QUE C'EST
      double dtr=-1;  

//      The argument dtr represents the quasi-periodic component of the
//      *     GR transformation between TT and TCB.  It is dependent upon the
//      *     adopted solar-system ephemeris, and can be obtained by numerical
//      *     integration, by interrogating a precomputed time ephemeris or by
//      *     evaluating a model such as that implemented in the SOFA function
//      *     jauDtdb.   The quantity is dominated by an annual term of 1.7 ms
//      *     amplitude.
//      
//      public static  double jauDtdb(double date1, double date2,
//            double ut, double elong, double u, double v)
//      *<!-- Given: -->
//      *     @param date1 double   date, TDB (Notes 1-3)
//      *     @param date2 double   date, TDB (Notes 1-3) 
//      *     @param ut             double   universal time (UT1, fraction of one day)
//      *     @param elong          double   longitude (east positive, radians)
//      *     @param u              double   distance from Earth spin axis (km)
//      *     @param v              double   distance north of equatorial plane (km)
//      *
//      * <!-- Returned (function value): -->
//      *  @return @return             double  TDB-TT (seconds)
   
      
      if( timeScale.equals("UTC") ) {
         jdTime = getSofaJD( JSOFA.jauUtctai(jdTime,0) );
         timeScale="TAI"; 
      }

      if( timeScale.equals("TAI") ) {
         jdTime = getSofaJD( JSOFA.jauTaitt(jdTime,0) );
         timeScale="TT"; 
      }

      if( timeScale.equals("UT1") ) {
         if( deltaT==-1 ) throw new Exception("Astrodate convertion not possible - missing Solar ephemerids");
         jdTime = getSofaJD( JSOFA.jauUt1tt(jdTime,0,deltaT) );
         timeScale="TT"; 
      }

      if( timeScale.equals("TT") ) {
         if( dtr==-1 ) throw new Exception("Astrodate convertion not possible - missing Solar ephemerids");
         
         jdTime = getSofaJD( JSOFA.jauTttdb(jdTime,0,dtr) );
         timeScale="TDB"; 
      }
      
      if( timeScale.equals("TDB") ) {
         jdTime = getSofaJD( JSOFA.jauTdbtcb(jdTime,0) );
         timeScale="TCB"; 
      }
      
      if( timeScale.equals("TCB") ) return jdTime;
      
      throw new Exception("Astrodate convertion error");
   }
   
   /** Pour palier le bug de la librairie de FOX qui ne supporte pas les unit�s types 10-2yr */
   static public double convert(double val, String fromUnit, String toUnit) throws Exception {
      
      // On applique la puissance de 10 en amont si n�cessaire
      boolean flagMoins;
      if( (flagMoins=fromUnit.startsWith("10-")) || fromUnit.startsWith("10+")) {
         int deb=3;
         int fin=deb+1;
         while( Character.isDigit( fromUnit.charAt(fin)) ) fin++;
         int exp = Integer.parseInt( fromUnit.substring(deb, fin));
         double fct =  Math.pow(10,exp);
         val = flagMoins ? val/fct : val*fct;
         fromUnit = fromUnit.substring(fin);
      }
      
      // On applique la puissance de 10 en amont si n�cessaire
      double fctOut=1;
      if( (flagMoins=toUnit.startsWith("10-")) || fromUnit.startsWith("10+") ) {
         int deb=3;
         int fin=deb+1;
         while( Character.isDigit( toUnit.charAt(fin)) ) fin++;
         int exp = Integer.parseInt( fromUnit.substring(deb, fin));
         fctOut =  Math.pow(10,exp);
         toUnit = toUnit.substring(fin);
      }
      
      // Conversion � la FOX dans les unit�s finales
      if( !fromUnit.equals(toUnit) ) {
         Unit m1 = new Unit(val+" "+fromUnit);
         m1.convertTo( new Unit(toUnit));
         val = m1.getValue();
      }
      
      return flagMoins ? val*fctOut : val/fctOut;
   }
}

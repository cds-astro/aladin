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

package cds.tools;

/**
 * Quelques fonctions pour la conversions des dates astronomiques.
 * Merci a : http://perso.wanadoo.fr/jean-paul.cornec/formule_jj.htm
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (8 juillet 2005) Creation
 */
public class Astrodate {
   
   /** Conversion d'une Jour Julien en jour Julien Modifié */
   static public double JDToMJD(double JD) { return JD-2400000.5; }
   
   /** Conversion d'une Jour Julien Modifié en jour Julien */
   static public double MJDToJD(double MJD) { return MJD+2400000.5; }
   
   /** Conversion d'une date /M/A HH:MM:SS (UTC) en jour julien
    * @param A année
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
         B = 2-C+(long)(C/4);
      } else B=0;
      T = HH/24. + MM/1440. + SS/86400.;
      
      return (long)(365.25 * (A+4716)) + (long)(30.6001 * (M+1)) + J + T + B - 1524.5;
   }
   
   /**
    * Conversion d'une date en jour julien en AAAA-MM-JJTHH:MM:SS (UTC)
    * @param JD Jour Julien
    * @return
    */
   static public String JDToDate(double JD) {
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
      long HH = s/3600;
      long MM = (s  - HH*3600 )/60;
      long SS = (s - HH*3600 - MM*60);
      return A+"-"+dd(M)+"-"+dd(J)+"T"+dd(HH)+":"+dd(MM)+":"+dd(SS);      
   }
   
   // Ajout d'un zéro en primer digit si nécessaire
   static private String dd(long x) { return x<10 ? "0"+x : ""+x; }
   
   
   /** Conversion de l'année décimale en Jour Julien
    * @param Yd Année Décimale
    * @return Jour Julien
    */
   static public double YdToJD(double Yd) {
      return 2451545.0 + (Yd-2000)*365.25;
   }
   
   /** Conversion d'une date en jour julien en année décimale
    * @param JD Jour Julien
    * @return Année décimale
    */
   static public double JDToYd(double JD) {
      return (JD-2451545.0)/365.25 +2000;
   }


}

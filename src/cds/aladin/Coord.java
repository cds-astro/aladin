// Copyright 2010 - UDS/CNRS
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

import cds.astro.*;
import cds.tools.Util;

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Manipulation des coordonnees
 *
 * @author Francois Bonnarel [CDS], Pierre Fernique [CDS]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Coord {
   /** Ascension droite (J2000 en degres) */
   public double al ;
   /** Declinaison (J2000 en degres) */
   public double del ;
   /** abcisse courante dans une projection (en reel) */
   public double x;
   protected double dx;
   /** ordonnee courante dans une projection (en reel)*/
   public double y;
   protected double dy;
   /** 1ere coordonnee standard */
   protected double xstand ;
   /** 2eme coordonnee standard */
   protected double ystand ;
   
   // L'objet de traitement de la coordonnees
   static Astrocoo coo = new Astrocoo();

  /** Creation */
   public Coord() {}
   public Coord(double ra,double dej) { al=ra; del=dej; coo.set(al,del); }

  /** Creation et affection al,del en fonction d'une chaine sexagesimale
   * ou degre */
   public Coord(String sexa) throws Exception {
      coo.set(sexa);
      al  = coo.getLon();
      del = coo.getLat();
   }

  /** Affichage sexagesimal de coordonnees passees en parametre.
   * @param al ascension droite
   * @param del declinaison
   * @return la chaine contenant la forme sexagesimale
   */
   public static String getSexa(double al, double del) { return getSexa(al,del,"s"); }

  /** Affichage sexagesimal de coordonnees passees en parametre.
   * @param al ascension droite
   * @param del declinaison
   * @param c le caractere seperateur des h,m,s,d
   * @return la chaine contenant la forme sexagesimale
   */
   public static String getSexa(double al, double del, String c) {
      Astrocoo coo = new Astrocoo();
      coo.set(al,del);
      coo.setPrecision(Astrocoo.ARCSEC+1);      
      try{
         String o = "2s"+(!c.equals(" ")?c:"");
//System.out.println("al="+al+" del="+del+" Options="+o+" coo="+coo.toString(o+"f"));
         return coo.toString(o);
      } catch( Exception e ) { System.err.println(e); }
      return "";
   }
   
  /** Affichage sexagesimal de l'objet.
   * @param c le caractere seperateur des h,m,s,d
   * @return la chaine contenant la forme sexagesimale
   */
   public String getSexa() { return getSexa(""); }
   public String getSexa(String c) { return getSexa(al,del,c); }
   
   public String toString() { return getSexa(); }
   

  /** Retourne RA en sexagesimal, separateur par defaut :
   * @param sep le separateur des champs
   * @return RA en sexagesimal
   */
   public String getRA() { return getRA(':'); }
   public String getRA(char sep) {
      try {
         String s = getSexa(sep+"");
         int i = s.indexOf('+');
         if( i==-1 ) i=s.indexOf('-');      
         return s.substring(0,i-1);
      } catch( Exception e ) { }
      return "";
   }

  /** Retourne DE en sexagesimal, separateur par defaut :
   * @param sep le separateur des champs
   * @return DE en sexagesimal
   */
   public String getDE() { return getDE(':'); }
   public String getDE(char sep) {
      try {
         String s = getSexa(sep+"");
         int i = s.indexOf('+');
         if( i==-1 ) i=s.indexOf('-');      
         return s.substring(i);
      } catch( Exception e ) { }
      return "";
   }

  /** Affichage dans la bonne unite.
   * Retourne un angle en degres sous forme de chaine dans la bonne unite
   * @param x l'angle
   * @return l'angle dans une unite coherente + l'unite utilisee
   */
   public static String getUnit(double x) { return getUnit(x,false,false); }
   public static String getUnit(double x,boolean entier,boolean flagSurface) {
      String s=null;
//      boolean flagCeil=true;
      double fct = flagSurface ? 3600 : 60;
      if( Math.abs(x)>=1.0 ) s="°";
      if( Math.abs(x)<1.0 ) { s="'"; x=x*fct; }
      if( Math.abs(x)<1.0 ) { s="\""; x=x*fct; /* flagCeil=false;*/ }
      
      if( entier && ((int)x)!=0 ) return ((int)x)+s;
      
//      if( flagCeil ) x=Math.ceil(x*100.0)/100.0;
//      else x=Math.ceil(x*10000.0)/10000.0;
      s=Util.myRound(x)+s;

      return s;
   }

  /** Affichage dans la bonne unite (H:M:S).
   * Retourne un angle en degres sous forme de chaine dans la bonne unite
   * @param x l'angle
   * @return l'angle dans une unite coherente + l'unite utilisee
   */
   public static String getUnitTime(double x) {
      String s=null;
      if( x>=1.0 ) s="h";
      if( x<1.0 ) { s="min"; x=x*60.0; }
      if( x<1.0 ) { s="s"; x=x*60.0; }
      x=((int)(x*100.0))/100.0;
      s=x+" "+s;

      return s;
   }

  /** Calcul d'un distance entre deux points reperes par leurs coord
   * @param c1 premier point
   * @param c2 deuxieme point
   * @return La distance angulaire en degres
   protected static double getDist1(Coord c1, Coord c2) {
      double dra = c2.al-c1.al;
      double dde = Math.abs(c1.del-c2.del);
      dra = Math.abs(dra);
      if( dra>180 ) dra-=360;
      double drac = dra*Astropos.cosd(c1.del);
      return Math.sqrt(drac*drac+dde*dde);
   }
   */

   public static double getDist(Coord c1, Coord c2) {
      return Coo.distance(c1.al,c1.del,c2.al,c2.del);
   }
}

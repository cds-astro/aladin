// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

package cds.aladin;

import java.util.ArrayList;
import java.util.Collections;

import cds.tools.Util;

/**
 * Classe permettant la mémorisation et le calcul des statistiques et des pixels associés à un objet
 * graphique Aladin, par exemple SourceStat ou Ligne
 * 
 * Mémorise la liste des pixels concernés (ra,de,val), et en déduit les statistiques courantes
 * @author Pierre Fernique [CDS]
 * @version 1.0 fév 2021 - création
 *
 */
public class StatPixels {
   
   static private final int MAX = 100000;  // Nombre max de valeurs prises en compte (sauf si withLimit=false)
   
   // Les statistiques
   int nb;              // Nombre de pixels concernés
   double sum;          // Somme des valeurs
   double sigma;        // Ecart moyen à la moyenne
   double min;          // Plus petite valeurs des pixels
   double max;          // Plus grande valeurs des pixels
   double median;       // Valeur médiane des pixels
   double surface;      // Surface angulaire concernée par la mesure de stats
   
   private boolean computed;    // false si les statistiques n'ont pas encore été calculées
   private String cle;          // Clé unique pour un objet stat et un plan de base donné (concaténation des hashBase,ra,de,ra,de...)
   private ArrayList<Pixel> pixels;  // Les pixels concernées
   
   private long time;         // Durée de la dernière extraction des pixels
   private long t;            // Date de début de l'extraction
   private boolean withLimit; // true si on s'arrête à MAX valeurs
   
   
   static private int STATSUM      = 0x1;       // Calcul de la somme
   static private int STATMIN      = 0x1<<1;    // Calcul du min
   static private int STATMAX      = 0x1<<2;    // Calcul du max
   static private int STATAREA     = 0x1<<3;    // Calcul de l'aire
   static private int STATSIGMA    = 0x1<<4;    // Calcul du sigma
   static private int STATMEDIAN   = 0x1<<5;    // Calcul de la médiane
   
   static private String[] STATLABEL= { "none","sum","min","max","area","sigma","median" };
   static private int[]    STATMASK = { 0, STATSUM,STATMIN,STATMAX,STATAREA,STATSIGMA, STATMEDIAN };
   
   static private int STATDEFAULT = STATSUM | STATMIN | STATMAX | STATAREA | STATSIGMA ;
   static private int currentStatMask = STATDEFAULT;
   
   
   /** Postionnement du mask de calcul des stats. La chaine passée en paramètre contient une liste
    * de label de stats (sum, min, max, area, sigma, median), eventuellement précédé d'un '+' ou d'un '-'
    * @param s
    * @throws Exception
    */
   static public void setStatMask(String s) throws Exception {
      if( s==null ) return;
      Tok tok = new Tok(s,",; ");
      while( tok.hasMoreTokens() ) {
         String k = tok.nextToken();
         if( k.length()==0 ) continue;
         char c = k.charAt(0);
         String s1=k.toLowerCase();
         if( c=='+' || c=='-' ) s1=k.substring(1).toLowerCase();
         else c=' ';
         int n;
         for( n=0; n<STATLABEL.length; n++ ) {
            if( STATLABEL[n].toLowerCase().indexOf( s1 )>=0 ) break;
         }
         if( n==STATLABEL.length ) throw new Exception("Unknown stat label ["+k+"]");
         if( c=='+' ) currentStatMask |= STATMASK[n];
         else if( c=='-' ) currentStatMask = currentStatMask & ~STATMASK[n];
         else currentStatMask = STATMASK[n];
      }
   }
   
   static public void setStatMask( int mask ) { STATMASK = STATMASK; }
   
   /** Retourne une chaine décrivant les stats appliquées : concaténation de (sum, min, max, area, sigma, median, pix) séparés
    * par virgule */
   static public String getStatMask() {
      StringBuilder s = new StringBuilder(100);
      for( int i=1; i<STATLABEL.length; i++ ) {
         if( (STATMASK[i]&currentStatMask)!=0 ) {
            if( s.length()>0 ) s.append(',');
            s.append(STATLABEL[i]);
         }
      }
      return s.toString();
   }
   
   /** Réinitialise les statistiques (liste de pixels et mesures) ssi la clé passée en paramètre ne correspond plus
    * à la clé du précédent calcul de statistiques
    * @param cle clé unique lié à la géométrie et la position de l'objet, et du plan de base
    * @param withLimit true si on ne vas pas au-dela de MAX values
    * @return true si la réinitialisation a eu lieu et qu'il faut donc redonner la liste des pixels concernées (cf addPix(...)
    */
   protected boolean reinit() { return reinit(null); }
   protected boolean reinit( String cle ) { return reinit( cle, false); }
   protected boolean reinit( String cle, boolean withLimit ) {
      if( hasSameCle( cle ) ) return false;
      this.withLimit = withLimit;
      this.cle=cle;
      pixels = new ArrayList<>(100000);
      nb=0;
      sum=Double.NaN;
      median=sigma=max=min=Double.NaN;
      surface=Double.NaN;
      computed=false;
      t=Util.getTime();
      return true;
   }
   
   /** Retourne en ms la durée de la dernière extraction des valeurs */
   protected long getTime() { return time; }

   /**
    * Retourne les mesures statistiques déduites de la liste des pixels. Calcule ces mesures si nécessaire
    * Note: La médiane n'aura été calculé que si elle a été demandé
    * @param withMedian true si on veut aussi la médiane (calcul un peu plus long)
    * @return un tableau de double representant: nb, sum, sigma, surface, min, max, median
    */
   protected double [] getStatistics() { return getStatistics( false ); }
   protected double [] getStatistics( boolean withMedian ) {
      compute( withMedian );
      return new double[]{ nb, sum, sigma, surface, min, max, median };
   }
   
   /**
    * Retourne la liste des pixels concernées par les statistiques sous la forme d'un tableau de triplets ra,de,val
    * @return le tableau des triplets
    */
   protected double [] getStatisticsRaDecPix() throws Exception {
      double res [] = new double[ pixels.size()*3 ];
      int i=0;
      for( Pixel pix : pixels ) { res[i++]=pix.raj;  res[i++]=pix.dej; res[i++]=pix.val; }
      return res;
   }
   
   /**
    * Retourne la liste des pixels concernées par les statistiques sous la forme d'un tableau de doubles
    * @return le tableau des valeurs des pixels
    */
   protected double [] getStatisticPix() {
      double res [] = new double[ pixels.size() ];
      int i=0;
      for( Pixel pix : pixels ) { res[i++]=pix.val; }
      return res;
   }
   
   // Vrai si la clé passée en paramètre est identique à la clé correspondant à la dernière statistique
   protected boolean hasSameCle( String cle ) { 
      return this.cle!=null && this.cle.equals(cle);
//      return this.cle==cle;
   }
   
   /** Ajout d'un pixel à la série */
   protected int addPix(double raj, double dej, double val) {
      int n = pixels.size();
      if( withLimit && n>=MAX ) return n;
      pixels.add( new Pixel(raj,dej,val) );
      return n+1;
   }
   
   /**
    * Positionne la surface (en deg^2) correspondante à la zone des statistiques
    * @param surface
    */
   protected void setSurface( double surface ) { this.surface=surface; }
   
   // Calcule si nécessaire les mesures statistiques à partir de la liste des pixels
   // @param withMedian true si on demande également le calcul de la médiane
   // @return true si un calcul a bien été opéré, false si inutile
   private boolean compute( boolean withMedian ) {
            
      // Aucun pixels chargés, rien à faire
      if( pixels==null || pixels.size()==0 ) return false;
      
      // Si on demande la médiane et qu'on ne l'a pas encore calculée, il faut réinitialisé le flag
      if( withMedian && Double.isNaN( median ) ) computed=false;
      
      if( computed ) return false;
      
      // Calcul du temps de l'extration
      time = Util.getTime()-t;
      
      boolean flagMin   = (currentStatMask & STATMIN)   !=0;
      boolean flagMax   = (currentStatMask & STATMAX)   !=0;
      boolean flagSigma = (currentStatMask & STATSIGMA) !=0;
      boolean flagSum   = (currentStatMask & STATSUM)   !=0;
      boolean flagMedian= (currentStatMask & STATMEDIAN)!=0;
      
      double sqr=0;
      for( Pixel pix : pixels ) {
         if( Double.isNaN( pix.val ) ) continue;
         nb++;
         if( flagSum || flagSigma ) {
            if( Double.isNaN(sum) ) sum=0;
            sum += pix.val;
         }
         if( flagMin && (Double.isNaN(min) || pix.val<min) ) min=pix.val;
         if( flagMax && (Double.isNaN(max) || pix.val>max) ) max=pix.val;
         if( flagSigma ) sqr += pix.val*pix.val;
      }
      
      if( flagSigma ) {
         double mean = sum/nb;
         double variance = sqr/nb - mean*mean;
         sigma = Math.sqrt(variance);
      }
      
      if( flagMedian && withMedian ) {
         try {
            Collections.sort(pixels);
            median = pixels.get( nb/2 ).val;
         } catch( Exception e ) { }
      }
      
      // Pour éviter de le faire plusieurs fois
      computed=true;
      
      return true;
   }
   
   // Classe interne permettant de manipuler un triplet ra,de,val correspondant à un pixel
   class Pixel implements Comparable<Pixel>{
      double raj,dej;   // Coordonnées équatoriales correspondantes
      double val;       // valeur du pixel (bscale et bzero déjà appliqués)
      
      Pixel(double raj, double dej, double val) {
         this.raj=raj; this.dej=dej; this.val=val;
      }

      @Override
      public int compareTo(Pixel o) {
         return o.val==val ? 0 : o.val<val ? 1 : -1;
      }
      
   }

}

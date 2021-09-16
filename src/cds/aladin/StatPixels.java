// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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
 * Classe permettant la m�morisation et le calcul des statistiques et des pixels associ�s � un objet
 * graphique Aladin, par exemple SourceStat ou Ligne
 * 
 * M�morise la liste des pixels concern�s (ra,de,val), et en d�duit les statistiques courantes
 * @author Pierre Fernique [CDS]
 * @version 1.0 f�v 2021 - cr�ation
 *
 */
public class StatPixels {
   
   static private final int MAX = 100000;  // Nombre max de valeurs prises en compte (sauf si withLimit=false)
   
   // Les statistiques
   int nb;              // Nombre de pixels concern�s
   double sum;          // Somme des valeurs
   double sigma;        // Ecart moyen � la moyenne
   double min;          // Plus petite valeurs des pixels
   double max;          // Plus grande valeurs des pixels
   double median;       // Valeur m�diane des pixels
   double surface;      // Surface angulaire concern�e par la mesure de stats
   
   private boolean computed;    // false si les statistiques n'ont pas encore �t� calcul�es
   private String cle;          // Cl� unique pour un objet stat et un plan de base donn� (concat�nation des hashBase,ra,de,ra,de...)
   private ArrayList<Pixel> pixels;  // Les pixels concern�es
   
   private long time;         // Dur�e de la derni�re extraction des pixels
   private long t;            // Date de d�but de l'extraction
   private boolean withLimit; // true si on s'arr�te � MAX valeurs
   
   
   /** R�initialise les statistiques (liste de pixels et mesures) ssi la cl� pass�e en param�tre ne correspond plus
    * � la cl� du pr�c�dent calcul de statistiques
    * @param cle cl� unique li� � la g�om�trie et la position de l'objet, et du plan de base
    * @param withLimit true si on ne vas pas au-dela de MAX values
    * @return true si la r�initialisation a eu lieu et qu'il faut donc redonner la liste des pixels concern�es (cf addPix(...)
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
   
   /** Retourne en ms la dur�e de la derni�re extraction des valeurs */
   protected long getTime() { return time; }

   /**
    * Retourne les mesures statistiques d�duites de la liste des pixels. Calcule ces mesures si n�cessaire
    * Note: La m�diane n'aura �t� calcul� que si elle a �t� demand�
    * @param withMedian true si on veut aussi la m�diane (calcul un peu plus long)
    * @return un tableau de double representant: nb, sum, sigma, surface, min, max, median
    */
   protected double [] getStatistics() { return getStatistics( false ); }
   protected double [] getStatistics( boolean withMedian ) {
      compute( withMedian );
      return new double[]{ nb, sum, sigma, surface, min, max, median };
   }
   
   /**
    * Retourne la liste des pixels concern�es par les statistiques sous la forme d'un tableau de triplets ra,de,val
    * @return le tableau des triplets
    */
   protected double [] getStatisticsRaDecPix() {
      double res [] = new double[ pixels.size()*3 ];
      int i=0;
      for( Pixel pix : pixels ) { res[i++]=pix.raj;  res[i++]=pix.dej; res[i++]=pix.val; }
      return res;
   }
   
   /**
    * Retourne la liste des pixels concern�es par les statistiques sous la forme d'un tableau de doubles
    * @return le tableau des valeurs des pixels
    */
   protected double [] getStatisticPix() {
      double res [] = new double[ pixels.size() ];
      int i=0;
      for( Pixel pix : pixels ) { res[i++]=pix.val; }
      return res;
   }
   
   // Vrai si la cl� pass�e en param�tre est identique � la cl� correspondant � la derni�re statistique
   protected boolean hasSameCle( String cle ) { 
      return this.cle!=null && this.cle.equals(cle);
//      return this.cle==cle;
   }
   
   /** Ajout d'un pixel � la s�rie */
   protected int addPix(double raj, double dej, double val) {
      int n = pixels.size();
      if( withLimit && n>=MAX ) return n;
      pixels.add( new Pixel(raj,dej,val) );
      return n+1;
   }
   
   /**
    * Positionne la surface (en deg^2) correspondante � la zone des statistiques
    * @param surface
    */
   protected void setSurface( double surface ) { this.surface=surface; }
   
   // Calcule si n�cessaire les mesures statistiques � partir de la liste des pixels
   // @param withMedian true si on demande �galement le calcul de la m�diane
   // @return true si un calcul a bien �t� op�r�, false si inutile
   private boolean compute( boolean withMedian ) {
            
      // Aucun pixels charg�s, rien � faire
      if( pixels.size()==0 ) return false;
      
      // Si on demande la m�diane et qu'on ne l'a pas encore calcul�e, il faut r�initialis� le flag
      if( withMedian && Double.isNaN( median ) ) computed=false;
      
      if( computed ) return false;
      
      // Calcul du temps de l'extration
      time = Util.getTime()-t;
      
      double sqr=0;
      for( Pixel pix : pixels ) {
         if( Double.isNaN( pix.val ) ) continue;
         if( Double.isNaN(sum) ) sum=0;
         sum += pix.val;
         nb++;
         if( Double.isNaN(min) || pix.val<min ) min=pix.val;
         if( Double.isNaN(max) || pix.val>max ) max=pix.val;
         sqr += pix.val*pix.val;
      }
      
      double mean = sum/nb;
      double variance = sqr/nb - mean*mean;
      sigma = Math.sqrt(variance);
      
      try {
         if( withMedian ) {
            Collections.sort(pixels);
            median = pixels.get( nb/2 ).val;
         }
      } catch( Exception e ) { }
      
      // Pour �viter de le faire plusieurs fois
      computed=true;
      
      return true;
   }
   
   // Classe interne permettant de manipuler un triplet ra,de,val correspondant � un pixel
   class Pixel implements Comparable<Pixel>{
      double raj,dej;   // Coordonn�es �quatoriales correspondantes
      double val;       // valeur du pixel (bscale et bzero d�j� appliqu�s)
      
      Pixel(double raj, double dej, double val) {
         this.raj=raj; this.dej=dej; this.val=val;
      }

      @Override
      public int compareTo(Pixel o) {
         return o.val==val ? 0 : o.val<val ? 1 : -1;
      }
      
   }

}

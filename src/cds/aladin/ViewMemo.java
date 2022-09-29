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

package cds.aladin;


final public class ViewMemo {
   static private int BLOC = 64;
   protected ViewMemoItem memo[];
   private int nb=0;

   protected ViewMemo() { }

   protected int size() { return nb; }

   /** Duplication du ViewMemo (ainsi que de tous ses ViewMemoItem) */
   protected ViewMemo copy() {
      ViewMemo vm = new ViewMemo();
      vm.memo = new ViewMemoItem[memo.length];
      vm.nb = nb;
      for( int i=0; i<nb; i++ ) vm.memo[i] = memo[i].copy();
      return vm;
   }
   
   private void fixeNb() {
//      if( memo==null ) return;
//      for( nb=memo.length-1; nb>0 && memo[nb]==null; nb--);
//      nb+=4;
   }
   
   private void extension(int i) {
      if( memo==null || memo.length<=i ) {
         ViewMemoItem nmemo[] = new ViewMemoItem[i+BLOC];
         if( memo!=null ) System.arraycopy(memo,0,nmemo,0,memo.length);
         memo=nmemo;
      }
   }

   protected void set(int i,ViewSimple v) {
      extension(i);
      memo[i]= v==null ? null : new ViewMemoItem(v);
      
      // S'agit-il de l'ajout d'un élément après la fin
      if( i>=nb ) nb=i+1;
    }
   
   protected void set(int i,ViewMemoItem vmi) {
      extension(i);
      memo[i]= vmi;
      
      // S'agit-il de l'ajout d'un élément après la fin
      if( i>=nb ) nb=i+1;
    }
   

   /** Décalage à partir de la position n d'un cran en ARRIERE
     * @param n position à partir de laquelle on va décaler (>0) */
   protected void cale(int n) {
//System.out.println("Je cale sur la case "+(n-1));      
      for( int i=n; i<nb; i++) memo[i-1]=memo[i];
      memo[--nb]=null;
   }

   /** Décalage à partir de la position n d'un cran en AVANT
    * @param n position à partir de laquelle on va décaler (>0) */
  protected void decale(int n) {
     // J'alloue la dernière case pour éviter un débordement
     set(nb,(ViewSimple)null);
     
     for( int i=nb-1; i>n; i--) memo[i]=memo[i-1];
     memo[n]=null;
  }

   /** Retourne true si le plan p passé en paramètre est utilisé au-moins
    *  une fois par une vue
    */
   protected boolean isUsed(Plan p) { return find(p,0)!=-1; }
   
   /** Retourne l'indice du premier memo qui a p comme plan de référence
    * sinon -1 en commençant par l'indice first et en cyclant */
   protected int find(Plan p,int first) {
      for( int i=0; i<nb; i++, first++ ) {
         if( first==nb ) first=0;
         if( memo[first]!=null && memo[first].pref==p )  return first;
      }
      return -1;
   }
   
   /** Retourne l'indice du dernier utilisé, -1 si aucun */
   protected int getLastUsed() {
      int j=-1;
      for( int i=0; i<nb; i++ ) {
         if( memo[i]!=null && memo[i].pref!=null) j=i;
      }
      return j;
   }
   
   /** Retourne le nombre de memo utilisés */
   protected int getNbUsed() {
      int j=0;
      for( int i=0; i<nb; i++ ) {
         if( memo[i]!=null && memo[i].pref!=null) j++;
      }
      return j;
   }

   protected int setAfter(int i,ViewSimple v) {
      for( i++; i<nb && memo[i]!=null && memo[i].pref!=null; i++);
      set(i,v);
      return i;
   }

   protected ViewSimple get(int i,ViewSimple v) {
      if( v==null || i>=nb || memo[i]==null ) return null;
      return memo[i].get(v);
   }

   /** Libération de l'item i */
   protected void free(int i) {
      if( i<memo.length ) memo[i]=null;
      fixeNb();
   }
   
   /** Libération de toutes les vues mémorisées */
   protected void freeAll() {
      for( int i=0; i<nb; i++ ) memo[i]=null;
      fixeNb();
   }
   
   /** Libération de toutes les vues lockées mémorisées */
   protected void freeLock() {
      for( int i=0; i<nb; i++ )
         if( memo[i]!=null && memo[i].locked ) memo[i]=null;
      fixeNb();
   }
   
   /** Retourne true s'il il y a au-moins une vue lockée mémorisée */
   protected boolean hasLock() {
      for( int i=0; i<nb; i++ )
         if( memo[i]!=null && memo[i].locked ) return true;
      return false;
   }
   
   /** Libération de toutes les vues mémorisées selected */
   protected void freeSelected() {
      for( int i=0; i<nb; i++ )
         if( memo[i]!=null && memo[i].selected ) memo[i]=null;
      fixeNb();
   }
   
   /** Libération de toutes les vues ayant le plan pref comme référence
    * @return true si au moins une vue a été trouvée */
   protected boolean freeRef(Plan pref) {
      boolean rep=false;
      for( int i=0; i<nb; i++ )
         if( memo[i]!=null && memo[i].pref==pref ) { rep=true; memo[i]=null; }
      fixeNb();
      return rep;
   }
}

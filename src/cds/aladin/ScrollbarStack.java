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

/**
 * Un Scrollbar d�di� � la pile
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (jan 2011) peaufinage
 * @version 1.0 : (oct 2006) Creation
 */
public final class ScrollbarStack extends MyScrollbar {
   
   private Aladin aladin;
   private Calque calque;
   
   ScrollbarStack(Aladin aladin,int orientation,int value,int visible,int min,int max) {
      super(orientation,value,visible,min,max);
      this.aladin=aladin;
   }
      
   /** Retourne le num�ro de plan qui correspond � la scrollbar vertical (en bas) */
   protected int getLastVisiblePlan() {  
      if( calque==null ) {
         calque = aladin.calque;   // petit raccourci
         addAdjustmentListener(calque.select);
      }
      return getValue();
   }     
   
   private int firstPlan;
   /** Indice du premier plan visible dans la pile (en haut) */
   protected int getFirstVisiblePlan() { return firstPlan; }
   protected void setFirstVisiblePlan(int index) { firstPlan = index; }
   

   // Sp�cifie si la scrollbar est requise, ou peut �tre effac�
   private boolean require=false;
   protected void setRequired(boolean r) { require=r; }
   protected boolean getRequired() { return require; }
   
   private int nbPlan=-1;
   /** M�morise le nombre de plans visibles dans la pile */
   protected void setNbVisiblePlan(int nb) { nbPlan=nb; }
   protected int getNbVisiblePlan() { return nbPlan; }
   
   /** Repositionnement du scroll en cas de suppression */
   protected boolean rm(int m) {
      int n=getValue();
      if( m>n ) { setValue(n+1); return true; }
      return false;
   }      
}

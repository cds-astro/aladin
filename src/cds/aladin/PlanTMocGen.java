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

package cds.aladin;

import java.util.Iterator;

import cds.moc.TimeMoc;

/** Generation d'un plan TMOC à partir d'une liste de plans (Catalogue) 
 * @author P.Fernique [CDS]
 * @version 1.0 - avril 2018 - création
 */
public class PlanTMocGen extends PlanTMoc {
   
   private Plan [] p;       // Liste des plans à ajouter dans le TMOC
   private double duration; // Pour un plan catalogue, duréeen seconde à partir de l'époque initiale (0 sinon)
   private int order;         // Résolution (ordre) demandée
   
   private double gapPourcent;  // Pourcentage de progression par plan (100 = tout est terminé)
   
   protected PlanTMocGen(Aladin aladin,String label,Plan[] p,int order,double duration) {
      super(aladin,null,label);
      this.c=null;
      this.p = p;
      this.order=order;
      this.duration=duration;
      
      pourcent=0;
      gapPourcent = 100/p.length;
      
      suiteSpecific();
      threading();
      log();
   }
   
   protected void suite1() {}
   
   // Ajout d'un plan catalogue au moc en cours de construction
   private void addMocFromCatalog(Plan p1,double duration) {
      Iterator<Obj> it = p1.iterator();
      int m= p1.getCounts();
      double incrPourcent = gapPourcent/m;
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         if( m<100 ) pourcent+=incrPourcent;
         try {
            double jdtime = ((Position)o).jdtime;
            if( Double.isNaN( jdtime ) ) continue;
            ((TimeMoc)moc).add(jdtime, jdtime+ duration/86400.);
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
      try {
         moc.toHealpixMoc();
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }


   protected boolean waitForPlan() {
      try {
         moc = new TimeMoc();
         if( order!=-1) moc.setMocOrder(order);
         for( Plan p1 : p ) {
            if( p1.isCatalogTime() ) {
               if( c==null ) {
                  c = p1.c.darker();
                  System.out.println("couleur="+c);
               }
               addMocFromCatalog(p1,duration);
            }
         }
      } catch( Exception e ) {
         error=e.getMessage();
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         flagProcessing=false;
         return false;
      }
      flagProcessing=false;
      if( moc.getSize()==0 ) error="Empty TMOC";
      flagOk=true;
      return true;
   }
   

      
}


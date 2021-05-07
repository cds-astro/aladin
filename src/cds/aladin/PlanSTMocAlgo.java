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

import cds.moc.Moc;
import cds.moc.STMoc;

/** Génération d'un STMOC de manière algorythmique
 * @author P.Fernique [CDS]
 * @version 1.0 - nov 2012
 */
public class PlanSTMocAlgo extends PlanSTMoc {
   
   /** Création d'un Plan MOC à partir d'une opération (op) et de plans MOCs (pList) 
    * Rq : méthode synchrone (pas de threading)
    */
   public PlanSTMocAlgo(Aladin aladin,String label,PlanMoc [] pList,int op,int spaceOrder,int timeOrder,long maxSize, String maxPriority) {
      super(aladin);
      PlanMoc p1 = pList[0];
      p1.copy(this);
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      String s = getFonction(p1,pList,op,timeOrder);
      if( label==null ) label = s;
      setLabel(label);
      
      aladin.trace(3,"STMOC computation: "+Plan.Tp[type]+" => "+s);
      
      long t0 = System.currentTimeMillis();
      
      try {
         moc = p1.getMoc().clone();
         if( op==PlanMocAlgo.COMPLEMENT ) moc = moc.complement();
         else if( op==PlanMocAlgo.COPY ) ((STMoc)moc).setMocOrder(timeOrder,spaceOrder);
         else {
            for( int i=1; i<pList.length; i++ ) {
               Moc m1=moc;
               Moc m2=pList[i].getMoc();
               
               // IL vaut mieux ajuster les ordres avant qu'après => c'est plus rapide
               if( timeOrder>=0  ) { m1.setTimeOrder( timeOrder );   m2.setTimeOrder(  timeOrder ); }
               if( spaceOrder>=0 ) { m1.setSpaceOrder( spaceOrder ); m2.setSpaceOrder( spaceOrder ); }

               switch(op) {
                  case PlanMocAlgo.UNION :        moc = m1.union(        m2); break;
                  case PlanMocAlgo.INTERSECTION : moc = m1.intersection( m2 ); break;
                  case PlanMocAlgo.SUBTRACTION :  moc = m1.subtraction(  m2 ); break;
                  case PlanMocAlgo.DIFFERENCE  :  moc = m1.difference(   m2 ); break;
               }
            }
         }
         
         if( maxSize!=-1L ) ((STMoc)moc).reduction(maxSize,maxPriority);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         moc.clear();
         aladin.error = error = e.getMessage();
         flagOk=false;
      }
      
      long t1 = System.currentTimeMillis();;
      System.out.println("STMOC operation done in "+(t1-t0)+"ms");
      
      copyright = "Computed by Aladin";
      flagProcessing=false;
      flagOk=true;
      setActivated(flagOk);
      if( moc.isEmpty() ) error="Empty STMOC";
      aladin.calque.repaintAll();

      sendLog("Compute"," [" + this + " = "+s+"]");
   }
   
   protected void launchLoading() { }
   
   // Retourne le label associé à l'opération
   private static String getFonction(PlanMoc p1,PlanMoc [] pList,int op,int order) { return PlanMocAlgo.getFonction(p1,pList,op,order); }
}


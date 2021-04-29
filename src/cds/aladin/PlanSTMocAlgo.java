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
import cds.tools.Util;

/** Génération d'un STMOC de manière algorythmique
 * @author P.Fernique [CDS]
 * @version 1.0 - nov 2012
 */
public class PlanSTMocAlgo extends PlanSTMoc {
   
   static final int UNION        = 0;
   static final int INTERSECTION = 1;
   static final int SUBTRACTION  = 2;
   static final int DIFFERENCE   = 3;
   static final int COMPLEMENT   = 4;
   static final int TOORDER      = 5;
   
   
   static private final String [] OPERATION = { "union","inter","sub","diff","compl","ord" };
   
   /** Retourne le nom qui correspond à une opération */
   static protected String getOpName(int op) { return OPERATION[op]; }
   
   /** Retourne le code de l'opération. Un tiret en préfixe peut être ou non présent
    * @param s la chaine qui décrit l'opération
    * @return le code de l'opération ou -1 si non trouvé
    */
   static int getOp(String s) {
      if( s.startsWith("-") ) s=s.substring(1);
      return Util.indexInArrayOf(s, OPERATION, true);
   }
   
   /** Création d'un Plan MOC à partir d'une opération (op) et de plans MOCs (pList) 
    * Rq : méthode synchrone (pas de threading)
    */
   public PlanSTMocAlgo(Aladin aladin,String label,PlanMoc [] pList,int op,int spaceOrder,int timeOrder) {
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
         if( op==COMPLEMENT ) moc = moc.complement();
         else/*if( op==TOORDER ) moc.setMocOrder(timeOrder);
         else */{
            for( int i=1; i<pList.length; i++ ) {
               Moc m1=moc;
               Moc m2=pList[i].getMoc();
               switch(op) {
                  case UNION :        moc = m1.union(        m2); break;
                  case INTERSECTION : moc = m1.intersection( m2 ); break;
                  case SUBTRACTION :  moc = m1.subtraction(  m2 ); break;
//                  case DIFFERENCE  :  moc = ((STMoc)m1).difference(   m2 ); break;
               }
            }
         }
         if( timeOrder>=0 ) ((STMoc)moc).setTimeOrder( timeOrder );
         if( spaceOrder>=0 ) ((STMoc)moc).setSpaceOrder( spaceOrder );
         
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
   private String getFonction(PlanMoc p1,PlanMoc [] pList,int op,int order) {
      if( op==TOORDER ) return p1.label+":"+order;
      String lab2= pList.length>1 ? pList[1].label : null;
      String lab3= pList.length>2 ? pList[2].label : null;
      return p1.label + " "+ getOpName(op) + (lab2==null ? " " : lab2 + (lab3==null?"":" ..."));
   }
}


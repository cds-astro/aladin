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

import cds.moc.Moc;
import cds.moc.TimeMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/** G�n�ration d'un MOC de mani�re algorythmique
 * @author P.Fernique [CDS]
 * @version 1.0 - nov 2012
 */
public class PlanTMocAlgo extends PlanTMoc {
   
   static final int UNION        = 0;
   static final int INTERSECTION = 1;
   static final int SUBTRACTION  = 2;
   static final int DIFFERENCE   = 3;
   static final int COMPLEMENT   = 4;
   static final int TOORDER      = 5;
   
   
   static private final String [] OPERATION = { "union","inter","sub","diff","compl","ord" };
   
   /** Retourne le nom qui correspond � une op�ration */
   static protected String getOpName(int op) { return OPERATION[op]; }
   
   /** Retourne le code de l'op�ration. Un tiret en pr�fixe peut �tre ou non pr�sent
    * @param s la chaine qui d�crit l'op�ration
    * @return le code de l'op�ration ou -1 si non trouv�
    */
   static int getOp(String s) {
      if( s.startsWith("-") ) s=s.substring(1);
      return Util.indexInArrayOf(s, OPERATION, true);
   }

   /** Cr�ation d'un Plan MOC � partir d'une op�ration (op) et de plans MOCs (pList) 
    * Rq : m�thode synchrone (pas de threading)
    */
   public PlanTMocAlgo(Aladin aladin,String label,PlanMoc [] pList,int op,int order) {
      super(aladin);
      arrayTimeMoc = new Moc[CDSHealpix.MAXORDER+1];
      PlanMoc p1 = pList[0];
      p1.copy(this);
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      String s = getFonction(p1,pList,op,order);
      if( label==null ) label = s;
      setLabel(label);
      
      aladin.trace(3,"TMOC computation: "+Plan.Tp[type]+" => "+s);
      
      try {
         moc = p1.getMoc().clone();
         if( op==COMPLEMENT ) moc = ((TimeMoc)moc).complement();
         else if( op==TOORDER ) moc.setMocOrder(order);
         else {
            for( int i=1; i<pList.length; i++ ) {
               Moc m1=moc;
               Moc m2=pList[i].moc;
               switch(op) {
                  case UNION :        moc = m1.union(        m2); break;
                  case INTERSECTION : moc = m1.intersection( m2 ); break;
                  case SUBTRACTION :  moc = m1.subtraction(  m2 ); break;
                  case DIFFERENCE  :  moc = ((TimeMoc)m1).difference(   m2 ); break;
               }
            }
         }
         
         if( order!=-1 ) moc.setMocOrder( order);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         moc.clear();
         aladin.error = error = e.getMessage();
         flagOk=false;
      }
      
      copyright = "Computed by Aladin";
      flagProcessing=false;
      flagOk=true;
      setActivated(flagOk);
      if( moc.getSize()==0 ) error="Empty TMOC";
      aladin.calque.repaintAll();

      sendLog("Compute"," [" + this + " = "+s+"]");
   }
   
   protected void launchLoading() { }
   
   // Retourne le label associ� � l'op�ration
   private String getFonction(PlanMoc p1,PlanMoc [] pList,int op,int order) {
      if( op==TOORDER ) return p1.label+":"+order;
      String lab2= pList.length>1 ? pList[1].label : null;
      String lab3= pList.length>2 ? pList[2].label : null;
      return p1.label + " "+ getOpName(op) + (lab2==null ? " " : lab2 + (lab3==null?"":" ..."));
   }
}


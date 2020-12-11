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

import cds.moc.Moc;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/** Génération d'un MOC de manière algorythmique
 * @author P.Fernique [CDS]
 * @version 1.0 - nov 2012
 */
public class PlanMocAlgo extends PlanMoc {
   
   static final int UNION        = 0;
   static final int INTERSECTION = 1;
   static final int SUBTRACTION  = 2;
   static final int DIFFERENCE   = 3;
   static final int COMPLEMENT   = 4;
   static final int TOORDER      = 5;
   
   
   static protected final String [] OPERATION = { "union","inter","sub","diff","compl","ord" };
   
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
   public PlanMocAlgo(Aladin aladin,String label,PlanMoc [] pList,int op,int order) {
      super(aladin);
      PlanMoc p1 = pList[0];
      p1.copy(this);
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      String s = getFonction(p1,pList,op,order);
      if( label==null ) label = s;
      setLabel(label);
      
      aladin.trace(3,"MOC computation: "+Plan.Tp[type]+" => "+s);
      
//      for( int j=0; j<3; j++ ) {
//         long t0 = System.currentTimeMillis();
         try {
            moc = p1.getMoc().clone();

            if( op==COMPLEMENT ) moc = ((SMoc)moc).complement();
            else if( op==TOORDER ) moc.setMocOrder(order);
            else {
               for( int i=1; i<pList.length; i++ ) {
                  Moc m1=moc;
                  SMoc m2=pList[i].toReferenceFrame(m1.getSys());
                  switch(op) {
                     case UNION :        moc = m1.union(        m2); break;
                     case INTERSECTION : moc = m1.intersection( m2 ); break;
                     case SUBTRACTION :  moc = m1.subtraction(  m2 ); break;
                     case DIFFERENCE  :  moc = ((SMoc)m1).difference(   m2 ); break;
                  }
               }
            }
            ((SMoc)moc).setMinOrder(3);
            if( order!=-1 ) moc.setMocOrder( order);

         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            moc.clear();
            aladin.error = error = e.getMessage();
            flagOk=false;
         }

         copyright = "Computed by Aladin";
         flagProcessing=false;
         
//         long t1 = System.currentTimeMillis();
//         System.out.println("Operation done in "+(t1-t0)+"ms");
//
//      }
      flagOk=true;
      setActivated(flagOk);
      if( moc.getSize()==0 ) error="Empty MOC";
      aladin.calque.repaintAll();

      sendLog("Compute"," [" + this + " = "+s+"]");
   }
   
   /** Création d'un Plan MOC par un "crop" polygone convexe sur un MOC existant (mocSource)
    * @param aladin
    * Rq : méthode synchrone (pas de threading)
    *
    * @param label
    * @param mocSource Le MOC à cropper
    * @param cooPolygon la liste des coordonnées J2000 qui décrivent le polygone convexe
    */
   protected PlanMocAlgo(Aladin aladin,String label,PlanMoc mocSource,Coord [] cooPolygon) {
      super(aladin);
      mocSource.copy(this);
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      String s = "crop";
      if( label==null ) label = s;
      setLabel(label);
      
      aladin.trace(3,"MOC cropping: "+Plan.Tp[type]+" => "+s);
      
      try {
         int order = mocSource.moc.getMaxUsedOrder();
         ArrayList<double[]> a = new ArrayList<>();
         for( Coord c : cooPolygon ) a.add(new double[]{c.al,c.del});
         
         SMoc m1 = CDSHealpix.createSMoc(a, order);
         moc = m1.intersection(mocSource.moc);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         moc.clear();
         aladin.error = error = e.getMessage();
         flagOk=false;
      }
      
      copyright = "Computed by Aladin";
      flagProcessing=false;
      if( moc.getSize()==0 ) {
         error="Empty MOC";
         flagOk=true;
      } else flagOk=true;
      setActivated(flagOk);
      aladin.calque.repaintAll();

      sendLog("Compute"," [" + this + " = "+s+"]");
   }
   
   /** Création d'un Plan MOC par un "crop" temporel sur un STMOC existant (mocSource)
    * @param aladin
    * Rq : méthode synchrone (pas de threading)
    *
    * @param label
    * @param mocSource Le MOC à cropper
    * @param jdmin, jdmax : le range temporel. NaN,NaN si aucune limite temporelle
    */
   protected PlanMocAlgo(Aladin aladin,String label,PlanMoc mocSource,double jdmin, double jdmax) {
      super(aladin);
      
      mocSource.copy(this);
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      String s = "timecrop";
      if( label==null ) label = s;
      setLabel(label);
      
      aladin.trace(3,"MOC temporal cropping: "+Plan.Tp[type]+" => "+s);
      
      try {
         long tmin = Double.isNaN(jdmin) ? -1L : (long)( jdmin*TMoc.DAYMICROSEC );
         long tmax = Double.isNaN(jdmax) ? Long.MAX_VALUE : (long)( jdmax*TMoc.DAYMICROSEC );
         moc = ((STMoc)mocSource.moc).getSpaceMoc(tmin, tmax);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         moc.clear();
         aladin.error = error = e.getMessage();
         flagOk=false;
      }
      
      copyright = "Computed by Aladin";
      flagProcessing=false;
      if( moc.getSize()==0 ) {
         error="Empty MOC";
         flagOk=true;
      } else flagOk=true;
      setActivated(flagOk);
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


// Copyright 2012 - UDS/CNRS
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

import java.util.ArrayList;

import cds.moc.Healpix;
import cds.moc.HealpixMoc;
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
   
   static private final String [] FCT = { "union","inter","sub","diff","compl"};
   static private String getFct(int fct) { return FCT[fct]; }

   /** Création d'un Plan MOC à partir d'une opération (op) et de plans MOCs (pList) 
    * Rq : méthode synchrone (pas de threading)
    */
   public PlanMocAlgo(Aladin aladin,String label,PlanMoc [] pList,int op) {
      super(aladin);
      PlanMoc p1 = pList[0];
      p1.copy(this);
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      String s = getFonction(p1,pList,op);
      if( label==null ) label = s;
      setLabel(label);
      
      aladin.trace(3,"AllSky computation: "+Plan.Tp[type]+" => "+s);
      
      try {
         moc = (HealpixMoc)p1.getMoc().clone();
         if( op==COMPLEMENT ) moc = moc.complement();
         else {
            for( int i=1; i<pList.length; i++ ) {
               HealpixMoc m1=moc;
               HealpixMoc m2=pList[i].toReferenceFrame(m1.getCoordSys());
               switch(op) {
                  case UNION :        moc = m1.union(        m2); break;
                  case INTERSECTION : moc = m1.intersection( m2 ); break;
                  case SUBTRACTION :  moc = m1.subtraction(  m2 ); break;
                  case DIFFERENCE  :  moc = m1.difference(   m2 ); break;
               }
            }
         }
         moc.setMinLimitOrder(3);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         moc.clear();
         aladin.error = error = e.getMessage();
         flagOk=false;
      }
      
      from = "Computed by Aladin";
      flagProcessing=false;
      if( moc.getSize()==0 ) {
         error="Empty MOC";
         flagOk=false;
      }  else flagOk=true;
      setActivated(flagOk);
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
         CDSHealpix hpx = new CDSHealpix();
         int order = mocSource.moc.getMaxOrder();
         long nside = Healpix.pow2(order);
         ArrayList<double[]> a = new ArrayList<double[]>();
         for( Coord c : cooPolygon ) a.add(new double[]{c.al,c.del});
         long [] npix = hpx.query_polygon(nside, a);
         
         moc.clear();
         moc.setCheckConsistencyFlag(false);
         for( long pix : npix ) moc.add(order,pix);
         moc.setCheckConsistencyFlag(true);
         
         moc = moc.intersection(mocSource.moc);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         moc.clear();
         aladin.error = error = e.getMessage();
         flagOk=false;
      }
      
      from = "Computed by Aladin";
      flagProcessing=false;
      if( moc.getSize()==0 ) {
         error="Empty MOC";
         flagOk=false;
      }  else flagOk=true;
      setActivated(flagOk);
      aladin.calque.repaintAll();

      sendLog("Compute"," [" + this + " = "+s+"]");
   }
   
   protected void suite1() { System.out.println("Je fais rien");}
   
   // Retourne le label associé à l'opération
   private String getFonction(PlanMoc p1,PlanMoc [] pList,int op) {
      String lab2= pList.length>1 ? pList[1].label : null;
      String lab3= pList.length>2 ? pList[2].label : null;
      return p1.label + " "+ getFct(op) + (lab2==null ? " " : lab2 + (lab3==null?"":" ..."));
   }
}


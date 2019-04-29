// Copyright 1999-2018 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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
import java.util.List;

import cds.aladin.stc.STCObj;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.Range;
import cds.moc.SpaceTimeMoc;
import cds.moc.TimeMoc;

/** Generation d'un plan STMOC � partir d'une liste de plans (Catalogue) 
 * @author P.Fernique [CDS]
 * @version 1.0 - avril 2019 - cr�ation
 */
public class PlanSTMocGen extends PlanSTMoc {
   
   private Plan [] p;       // Liste des plans � ajouter dans le TMOC
   private double duration; // Pour un plan catalogue, dur�een seconde � partir de l'�poque initiale (0 sinon)
   private double radius;   // Pour un plan catalogue, rayon autour de chaque source (en degres), sinon 0
   private boolean fov;     // Plan un plan catalogue, true si on prend les FOVs associ�s
   private int timeOrder=14;
   private int spaceOrder=10;
   
   private double gapPourcent;  // Pourcentage de progression par plan (100 = tout est termin�)
   
   protected PlanSTMocGen(Aladin aladin,String label,Plan[] p,int spaceOrder, int timeOrder, 
         double duration, double radius, boolean fov) {
      super(aladin, null, label, null, 0.);
      aladin.trace(3,"STMOC creation xxx: "+Plan.Tp[type]);
      
      this.c=null;
      this.p = p;
      
      this.spaceOrder=spaceOrder;
      this.timeOrder=timeOrder;
      this.duration=duration;
      this.radius=radius;
      this.fov=fov;

      pourcent=0;
      gapPourcent = 100/p.length;
      
      suiteSpecific();
      threading();
      log();
   }
   
   protected void suite1() {}
   
   
   /**
    * Ajout d'un �l�ment par order/npix et [jdtmin..jdtmax[
    * @param m
    * @param order
    * @param npix
    * @param jdtmin
    * @param jdtmax
    */
   protected void addIt(SpaceTimeMoc m, int order, long npix, double jdtmin, double jdtmax) {
      long smin = npix<<(2*(29-order));
      long smax = (npix+1)<<(2*(29-order));
      addIt(m,smin,smax,jdtmin,jdtmax);
   }
   
   /**
    * Ajout d'un �l�ment par [smin..smax[ et [jdtmin..jdtmax[
    * @param m
    * @param smin
    * @param smax
    * @param jdtmin
    * @param jdtmax
    */
   protected void addIt(SpaceTimeMoc m, long smin, long smax, double jdtmin, double jdtmax) {
      long tmin=0,tmax=0;
      try {
         tmin = (long)(jdtmin*TimeMoc.DAYMICROSEC);
         tmax = (long)(jdtmax*TimeMoc.DAYMICROSEC +TimeMoc.getDuration(timeOrder));
         
         if( m.timeRange.sz==8 &&
               m.timeRange.r[0]==2462579491264790528L &&
               m.timeRange.r[1]== 2462580590776418304L &&
               m.timeRange.rangeArray[0].sz==44 &&
               m.timeRange.rangeArray[0].r[0]==1392152145066721280L &&
               m.timeRange.rangeArray[0].r[1]==1392153244578349056L ) {
            System.out.println("j'y suis");
         }
         
         m.add(tmin,tmax,smin,smax);
      } catch( Exception e ) {
        e.printStackTrace();
        System.err.println("MOC="+m);
        System.err.println("tmin="+tmin+" tmax="+tmax+" smin="+smin+" smax="+smax);
      }
   }
   
   // Ajout d'un plan catalogue au moc en cours de construction
   private void addMocFromCatalog(Plan p1,double duration,double radius, boolean fov) {
//      SpaceTimeMoc m2 = new SpaceTimeMoc( spaceOrder, timeOrder );
      Iterator<Obj> it = p1.iterator();
      int m= p1.getCounts();
      Healpix hpx = new Healpix();
      Coord coo = new Coord();
      double incrPourcent = gapPourcent/m;
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         if( m<100 ) pourcent+=incrPourcent;
         m++;
         
//         if( m%500==0 ) {
//            System.out.println("Union...");
//            try { moc = moc.union( m2 ); } catch( Exception e ) { e.printStackTrace(); }
//            m2 = new SpaceTimeMoc( spaceOrder, timeOrder );
//         }
         
         try {
            double jdtime = ((Position)o).jdtime;
            if( Double.isNaN( jdtime ) ) continue;
            
            long [] npixs=null;
            if( fov ) {
               Source s = (Source)o;
               SourceFootprint sf = s.getFootprint();
               if( sf==null ) continue;
               List<STCObj> listStcs = sf.getStcObjects();
               if( listStcs==null ) continue;
               try {
                  HealpixMoc m1 = aladin.createMocRegion(listStcs,spaceOrder);
                  
                  m1.toRangeSet();
                  Range r = m1.spaceRange;
//                  for( int j=0; j<r.sz; j+=2 ) addIt(m2,r.r[j],r.r[j+1],jdtime,jdtime+ duration/86400.);
                  for( int j=0; j<r.sz; j+=2 ) addIt( (SpaceTimeMoc)moc,r.r[j],r.r[j+1],jdtime,jdtime+ duration/86400.);
                  
                  
//                  npixs = new long[ (int)( m1.getUsedArea()) ];
//                  Iterator<Long> it1 = m1.pixelIterator();
//                  for( int i=0; i<npixs.length; i++ ) npixs[i] = it1.next();
               } catch( Exception e ) {
                  if( aladin.levelTrace>=3 ) e.printStackTrace();
               }

            } else {
               coo.al = ((Position)o).raj;
               coo.del = ((Position)o).dej;
               if( radius==0 ) npixs = new long[] { hpx.ang2pix(spaceOrder, coo.al, coo.del) };
               else npixs = hpx.queryDisc(spaceOrder, coo.al, coo.del, radius);
            }
            
            if( npixs==null ) continue;
            for( long npix : npixs ) {
//               addIt(m2,spaceOrder,npix,jdtime,jdtime+ duration/86400.);
               addIt((SpaceTimeMoc)moc,spaceOrder,npix,jdtime,jdtime+ duration/86400.);
            }

         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
      try {
//         moc = moc.union( m2 );
         moc.toHealpixMoc();
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   

   protected boolean waitForPlan() {
      try {
         moc = new SpaceTimeMoc(spaceOrder,timeOrder);
//         if( order!=-1) moc.setMocOrder(order);
         for( Plan p1 : p ) {
            if( p1.isCatalogTime() ) {
               if( c==null )  c = p1.c.darker();
               addMocFromCatalog(p1,duration,radius,fov);
            }
         }
      } catch( Exception e ) {
         error=e.getMessage();
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         flagProcessing=false;
         return false;
      }
      flagProcessing=false;
      if( moc.getSize()==0 ) error="Empty STMOC";
      flagOk=true;
      aladin.calque.repaintAll();
      return true;
   }
   

      
}


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
import java.util.List;

import cds.aladin.stc.STCObj;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.Range;
import cds.moc.SpaceTimeMoc;
import cds.moc.TimeMoc;
import cds.tools.Util;

/** Generation d'un plan STMOC à partir d'une liste de plans (Catalogue) 
 * @author P.Fernique [CDS]
 * @version 1.0 - avril 2019 - création
 */
public class PlanSTMocGen extends PlanSTMoc {
   
   private Plan [] p;       // Liste des plans à ajouter dans le TMOC
   private double duration; // Pour un plan catalogue, duréeen seconde à partir de l'époque initiale (0 sinon)
   private double radius;   // Pour un plan catalogue, rayon autour de chaque source (en degres), sinon 0
   private boolean fov;     // Plan un plan catalogue, true si on prend les FOVs associés
   private int timeOrder=14;
   private int spaceOrder=10;
   
   private double gapPourcent;  // Pourcentage de progression par plan (100 = tout est terminé)
   
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
    * Ajout d'un élément par order/npix et [jdtmin..jdtmax[
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
   
   
   static final String TEST = "tmin=212100466200000000 tmax=212100741077906944 smin=1907076530098405376 smax=1907077629610033152";
   /**
    * Ajout d'un élément par [smin..smax[ et [jdtmin..jdtmax[
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

//         String t = "tmin="+tmin+" tmax="+tmax+" smin="+smin+" smax="+smax;
//         if( t.equals(TEST) ) {
//            System.out.println("J'y suis");
//         }

         m.add(tmin,tmax,smin,smax);
      } catch( Exception e ) {
         e.printStackTrace();
         System.err.println("MOC="+m);
         System.err.println("tmin="+tmin+" tmax="+tmax+" smin="+smin+" smax="+smax);
      }
   }
   

   // Ajout d'un plan catalogue au moc en cours de construction
   private void addMocFromCatalog(Plan p1,double duration,double radius, boolean fov) {
      
      long t0 = System.currentTimeMillis();
      
      SpaceTimeMoc m2 = new SpaceTimeMoc( spaceOrder, timeOrder );
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
         
         if( m%1000==0 ) {
            try { moc = moc.union( m2 ); } catch( Exception e ) { e.printStackTrace(); }
            m2 = new SpaceTimeMoc( spaceOrder, timeOrder );
         }
         
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
                  for( int j=0; j<r.sz; j+=2 ) addIt(m2,r.r[j],r.r[j+1],jdtime,jdtime+ duration/86400.);
//                  for( int j=0; j<r.sz; j+=2 ) addIt( (SpaceTimeMoc)moc,r.r[j],r.r[j+1],jdtime,jdtime+ duration/86400.);
                  
                  
                  npixs = new long[ (int)( m1.getUsedArea()) ];
                  Iterator<Long> it1 = m1.pixelIterator();
                  for( int i=0; i<npixs.length; i++ ) npixs[i] = it1.next();
               } catch( Exception e ) {
                  if( aladin.levelTrace>=3) e.printStackTrace();
                }

            } else {
               coo.al = ((Position)o).raj;
               coo.del = ((Position)o).dej;
               if( radius==0 ) npixs = new long[] { hpx.ang2pix(spaceOrder, coo.al, coo.del) };
               else npixs = hpx.queryDisc(spaceOrder, coo.al, coo.del, radius);
            }
            
            if( npixs==null ) continue;
            for( long npix : npixs ) {
               addIt(m2,spaceOrder,npix,jdtime,jdtime+ duration/86400.);
//               addIt((SpaceTimeMoc)moc,spaceOrder,npix,jdtime,jdtime+ duration/86400.);
            }

         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
      try {
         moc = moc.union( m2 );
         moc.toHealpixMoc();
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      
      long t1 = System.currentTimeMillis();
      System.out.println("STMOC generated in "+(t1-t0)+"ms");
   }
   

   protected boolean waitForPlan() {
      long t0 = System.currentTimeMillis();
      try {
         moc = new SpaceTimeMoc(spaceOrder,timeOrder);
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
      long t1=System.currentTimeMillis();
      Aladin.trace(3,"STMOC built in "+Util.getTemps(t1-t0));
      
      flagProcessing=false;
      if( moc.getSize()==0 ) error="Empty STMOC";
      
      flagOk=true;
      aladin.calque.repaintAll();
      return true;
   }
   

      
}


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

import java.util.Iterator;

import cds.moc.TMoc;
import cds.tools.Util;

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
      super(aladin,(MyInputStream)null,label);
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
   
   protected void launchLoading() {}
   
   
   /** Ajustement d'une valeur à l'ordre indiquée */
//   public long getVal(long val, int order) {
//      if( order==TMoc.MAXORD_T ) return val;
//      int deltaOrder = (TMoc.MAXORD_T - order)<< (TMoc.FACT_T/2);
//      val = (val>>>deltaOrder) << deltaOrder;
//      return val;
//   }
   
//   // Ajout d'un plan catalogue au moc en cours de construction
//   private void addMocFromCatalog(TMoc moc,Plan p1,double duration) {
//      long t0,t1;
//      t0=System.currentTimeMillis();
//      Iterator<Obj> it = p1.iterator();
//      int m= p1.getCounts();
//      double incrPourcent = gapPourcent/m;
//      long [] buf = new long[ m*2];
//      int n=0;
//      int order = moc.getMocOrder();
//      while( it.hasNext() ) {
//         Obj o = it.next();
//         if( !(o instanceof Position) ) continue;
//         if( m<100 ) pourcent+=incrPourcent;
//         try {
//            double jdtime = ((Position)o).jdtime;
//            if( Double.isNaN( jdtime ) ) continue;
////            moc.add(jdtime, jdtime+ duration/86400.);
//            long start = (long)(jdtime*TMoc.DAYMICROSEC);
//            long end = (long)( (jdtime+ duration/86400.) *TMoc.DAYMICROSEC);
//            buf[n++]=getVal(start,order);
//            buf[n++]=getVal(end,order)+1L;            
//            
//         } catch( Exception e ) {
//            if( aladin.levelTrace>=3 ) e.printStackTrace();
//         }
//      }
//      try {
//         Range range = new Range(buf,n);
//         range.sortAndFix();
//         moc.setRangeList( range );
//         
////         moc.toMocSet();
//      } catch( Exception e ) {
//         if( aladin.levelTrace>=3 ) e.printStackTrace();
//      }
//      t1 =System.currentTimeMillis();
//      aladin.trace(4,"TMOC created in "+Util.getTemps((t1-t0)*1000L)+" ("+m+" sources)");
//   }

   // Ajout d'un plan catalogue au moc en cours de construction
   private void addMocFromCatalog(TMoc moc,Plan p1,double duration) {
      long t0,t1;
      t0=System.currentTimeMillis();
      Iterator<Obj> it = p1.iterator();
      int m= p1.getCounts();
      double incrPourcent = gapPourcent/m;
      moc.clear();
      moc.bufferOn(10000);
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         if( m<100 ) pourcent+=incrPourcent;
         try {
            double jdtime = ((Position)o).jdtime;
            if( Double.isNaN( jdtime ) ) continue;
            moc.add(jdtime, jdtime+ duration/86400.);
            
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
      moc.bufferOff();
      t1 =System.currentTimeMillis();
      aladin.trace(4,"TMOC created in "+Util.getTemps((t1-t0)*1000L)+" ("+m+" sources)");
   }


   protected boolean waitForPlan() {
      
      // Pour des benchs
//      for( int order=15; order<=43; order+=4 ) {
//         for( int i=0; i<3; i++ ) {
//            long t0 = System.currentTimeMillis();
            
            try {
               moc = new TMoc();
               if( order!=-1) ((TMoc)moc).setMocOrder(order);
               for( Plan p1 : p ) {
                  if( p1.isCatalogTime() ) {
                     if( c==null )  c = p1.c.darker();
                     addMocFromCatalog((TMoc)moc,p1,duration);
                  }
               }
            } catch( Exception e ) {
               error=e.getMessage();
               if( aladin.levelTrace>=3 ) e.printStackTrace();
               flagProcessing=false;
               return false;
            }
            flagProcessing=false;
            if( moc.isEmpty() ) error="Empty TMOC";
            
//            long t1 = System.currentTimeMillis();
//            if( i==2 ) {
//               try {
//                  File f = File.createTempFile("toto", "titi");
//                  FileOutputStream fo = new FileOutputStream(f);
//                  moc.writeFITS(fo);
//                  fo.close();
//                  long sizeFits = f.length();
//                  Aladin.trace(3,"TMOC 'order="+((TMoc)moc).getMocOrder()+" built in "+(t1-t0)
//                        +"ms nbRanges="+moc.getNbRanges()
//                        +" RAM="+Util.getUnitDisk( moc.getMem())
//                        +" FITS="+Util.getUnitDisk(sizeFits )
//                        );
//               } catch( Exception e ) {}
//            }
//            
//         }
//      }
      flagOk=true;
      return true;
   }
   

      
}


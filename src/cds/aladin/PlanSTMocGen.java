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
import java.util.List;

import cds.aladin.stc.STCObj;
import cds.moc.Healpix;
import cds.moc.Range2;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;
import cds.tools.Util;

/** Generation d'un plan STMOC à partir d'une liste de plans (Catalogue) 
 * @author P.Fernique [CDS]
 * @version 1.0 - avril 2019 - création
 */
public class PlanSTMocGen extends PlanSTMoc {
   
   private Plan [] p;       // Liste des plans à ajouter dans le STMOC
   private double duration; // Pour un plan catalogue, duréeen seconde à partir de l'époque initiale (0 sinon)
   private double radius;   // Pour un plan catalogue, rayon autour de chaque source (en degres), sinon 0
   private boolean fov;     // Plan un plan catalogue, true si on prend les FOVs associés
   private int timeOrder=14;
   private int spaceOrder=10;
   
   private boolean flagOneRange=false;
   

   
   private double gapPourcent;  // Pourcentage de progression par plan (100 = tout est terminé)
   
   protected PlanSTMocGen(Aladin aladin,String label,Plan[] p,int spaceOrder, int timeOrder, 
         double duration, double radius, boolean fov) {
      super(aladin, (MyInputStream)null, label, null, 0.);
      aladin.trace(3,"STMOC creation: "+Plan.Tp[type]);
      
      this.c=null;
      this.p = p;
      
      this.label = label;
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
   
   protected void launchLoading() {}
   
   
   /** Génération d'un plan STMOC à partir d'une liste de MOC spatiaux et d'un unique intervalle temporel
    * @param aladin
    * @param pList
    * @param spaceOrder -1 si inchangé 
    * @param timeOrder  29 par défaut
    * @param jdmin
    * @param jdmax
    */
   protected PlanSTMocGen(Aladin aladin, String label, Plan [] pList, int spaceOrder, int timeOrder, double jdmin, double jdmax) {
      super(aladin);
      PlanMoc p1 = (PlanMoc) pList[0];
      p1.copy(this);
      type = ALLSKYSTMOC;
      setLabel(label==null ? "["+this.label+"]" : label);
      c = Couleur.getNextDefault(aladin.calque);
      
      // On aggrège tous les spaces MOC
      SMoc m1=null;
      try { m1 = (SMoc) p1.getMoc().clone(); } catch( CloneNotSupportedException e1 ) { e1.printStackTrace(); }
      for( int i=1; i<pList.length; i++ ) {
         SMoc m2= (SMoc) ((PlanMoc)pList[i]).getMoc(); 
         try {  m1 = m1.union( m2); } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
      
      // On crée le STMOC à partir du range de temps et de l'aggrégation des MOC spatiaux
      Range2 r = new Range2();
      long min = (long)(jdmin*TMoc.DAYMICROSEC);
      long max = (long)(jdmax*TMoc.DAYMICROSEC)+1L;
      r.append(min, max, m1.seeRangeList());
      try { moc = new STMoc(timeOrder<0?TMoc.MAXORD_T:timeOrder, spaceOrder==-1?m1.getMocOrder():spaceOrder);
      } catch( Exception e1 ) { e1.printStackTrace(); }
      moc.setRangeList(r);
      flagOneRange=true;
   }

   /** Retourne true si le STMOC ne contient qu'un range de temps, potentiellement modifiable */
   protected boolean isOneTimeRange() { 
      return flagOneRange && ((STMoc)moc).range.nranges()==1; 
   }

   protected boolean Free() {
      stop=true;
      return super.Free();
   }
   
   private boolean stop;

   // Ajout d'un plan catalogue au moc en cours de construction
   private void addMocFromCatalog(Plan p1,double duration,double radius, boolean fov) throws Exception {
      
      stop=false;
      SMoc m1=null;
      STMoc m2 = new STMoc( timeOrder, spaceOrder );
      Iterator<Obj> it = p1.iterator();
      Healpix hpx = new Healpix();
      double incrPourcent = gapPourcent/p1.getCounts();
      int m= 0;
      long t0 = System.currentTimeMillis();
      
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         pourcent+=incrPourcent;
         m++;

         if( m%10000==0 ) {
            if( stop ) throw new Exception("Abort");
            try { moc = moc.union( m2 ); } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
            m2.clear();
         }

         double jdtime = ((Position)o).jdtime;
         if( Double.isNaN( jdtime ) ) continue;
         double jdend = jdtime+ duration/86400.;
         
         try {

            // Ajout simple d'une cellule
            if( !fov && radius==0 ) {
               long npix = hpx.ang2pix(spaceOrder, ((Position)o).raj,((Position)o).dej);
               m2.add(spaceOrder,npix,jdtime,jdend);
               continue;
            }

            // Ajouts par formes
            m1=null;
            // Par FoV ?
            if( fov ) {
               Source s = (Source)o;
               SourceFootprint sf = s.getFootprint();
               if( sf==null ) continue;
               List<STCObj> listStcs = sf.getStcObjects();
               if( listStcs==null ) continue;
               m1 = aladin.createMocRegion(listStcs,spaceOrder,true);

               // Par Cones ?
            } else {
               m1 = aladin.createMocRegionCircle(((Position)o).raj,  ((Position)o).dej, radius, spaceOrder, true);
            }
            m2.add(jdtime,jdend, m1 );
            
         } catch( Exception e ) {
            if( aladin.levelTrace>=3) e.printStackTrace();
         }
      }
      try {  moc = moc.union( m2 ); } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
        
      long t1 = System.currentTimeMillis();
      aladin.trace(4,"STMOC build in "+Util.getTemps( (t1-t0)*1000L)+" nbSrc="+m);

   }


   protected boolean waitForPlan() {
      
      // Pour des benchs
//      int SPACE [] = {  6, 8,10,12,14,16,18,20, 8,12};
//      int TIME []  = { 15,19,23,27,31,35,39,43,28,19};
//      for( int n=0; n<SPACE.length; n++ ) {
//         spaceOrder = SPACE[n];
//         timeOrder = TIME[n];
//         long t0 = System.currentTimeMillis();


         try {
            moc = new STMoc(timeOrder,spaceOrder);
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

//         long t1 = System.currentTimeMillis();
//         try {
//            File f = File.createTempFile("toto", "titi");
//            FileOutputStream fo = new FileOutputStream(f);
//            moc.writeFITS(fo);
//            fo.close();
//            long sizeFits = f.length();
//            Aladin.trace(3,"STMOC"
//                  +" timeOrder="+((STMoc)moc).getTimeOrder()
//                  +" spaceOrder="+((STMoc)moc).getSpaceOrder()
//                  +" built in "+(t1-t0)+"ms"
//                  +" nbRanges="+moc.getNbRanges()
//                  +" RAM="+Util.getUnitDisk( moc.getMem())
//                  +" FITS="+Util.getUnitDisk(sizeFits )
//                  );
//         } catch( Exception e ) {}
//      }


      flagProcessing=false;
      if( moc.isEmpty() ) error="Empty STMOC";

      flagOk=true;
      aladin.calque.repaintAll();
      return true;
   }



}


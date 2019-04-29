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

import java.awt.Graphics;

import cds.moc.Moc;
import cds.moc.SpaceMoc;
import cds.moc.SpaceTimeMoc;
import cds.moc.TimeMoc;
import cds.tools.Astrodate;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Génération d'un plan TMOC
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 Création - fév 2018
 */
public class PlanSTMoc extends PlanTMoc {
   
   
   public PlanSTMoc(Aladin a) { super(a); }
   
   
   protected PlanSTMoc(Aladin aladin, MyInputStream in, String label, Coord c, double radius) {
      super(aladin);
      arrayTimeMoc = new Moc[CDSHealpix.MAXORDER+1];
      arrayMoc = new Moc[CDSHealpix.MAXORDER+1];
      this.dis   = in;
      type = ALLSKYSTMOC;
      useCache = false;
      frameOrigin = Localisation.ICRS;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="STMOC";
      setLabel(label);
      co=c;
      coRadius=radius;
      aladin.trace(3,"STMOC creation: "+Plan.Tp[type]+(c!=null ? " around "+c:""));
      suite();
   }
   
   /** Recopie du Plan à l'identique dans p1 */
   protected void copy(Plan p1) {
      super.copy(p1);
      PlanSTMoc pm = (PlanSTMoc)p1;
      pm.arrayTimeMoc = new Moc[CDSHealpix.MAXORDER+1];
   }


   /** Ajoute des infos sur le plan */
   protected void addMessageInfo( StringBuilder buf, MyProperties prop ) {
      SpaceTimeMoc stmoc = (SpaceTimeMoc) moc;
      ADD( buf, "\n* Start: ",Astrodate.JDToDate( stmoc.getTimeMin()));
      ADD( buf, "\n* End: ",Astrodate.JDToDate( stmoc.getTimeMax()));
      ADD( buf,"\n* # ranges: ",stmoc.timeRange.nranges()+"");
      int timeOrder = stmoc.getTimeOrder();
      ADD( buf,"\n* Time res: ",TimeMoc.getTemps(  TimeMoc.getDuration(timeOrder)));
      ADD( buf,"\n* Best time order: ",timeOrder+"");

      double cov=0;
      try { cov = (stmoc.getSpaceMoc()).getCoverage();
      } catch( Exception e ) { }
      double degrad = Math.toDegrees(1.0);
      double skyArea = 4.*Math.PI*degrad*degrad;
      ADD( buf, "\n \n* Space: ",Coord.getUnit(skyArea*cov, false, true)+"^2, "+Util.round(cov*100, 3)+"% of sky");
      int spaceOrder =stmoc.getSpaceOrder();
      ADD( buf,"\n* Space res: ",( Coord.getUnit( CDSHealpix.pixRes(spaceOrder)/3600.) ));
      ADD( buf,"\n* Best space order: ",timeOrder+"");
      
      if( Aladin.levelTrace>0 ) {
         ADD( buf,"\n \nRAM: ",Util.getUnitDisk( stmoc.getMem() ) );
      }
      
   }
   
   /** Retourne le time stamp minimal */
   protected double getTimeMin() { 
      double tmin = ((SpaceTimeMoc)moc).getTimeMin();
      if( tmin==-1 ) tmin=Double.NaN;
      return tmin;
   }

   /** Retourne le time stamp maximal */
   protected double getTimeMax() { 
      double tmax = ((SpaceTimeMoc)moc).getTimeMax();
      if( tmax==-1 ) tmax=Double.NaN;
      return tmax;
   }

   protected boolean waitForPlan() {
      if( dis!=null ) {
         error=null;
         try {
            if( moc==null && dis!=null ) {
               moc = new SpaceTimeMoc();
               if(  (dis.getType() & MyInputStream.FITS)!=0 ) moc.readFits(dis);
               else moc.readASCII(dis);
            }
            if( moc.getSize()==0 ) error="Empty STMOC";
         }
         catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            return false;
         }
      }

      return true;
   }
   
   /** Retourne le SpaceMoc correspondant à l'intervalle temporelle courant pour la vue courante,
    * ou null si impossible à définir */
   protected SpaceMoc getCurrentSpaceMoc() {
      long tmin = (long)( oLastDrawTmin=getLastDrawTmin()*TimeMoc.DAYMICROSEC );
      long tmax = (long)( oLastDrawTmax=getLastDrawTmax()*TimeMoc.DAYMICROSEC );
      
//      System.out.println("tmin="+tmin+" tmax="+tmax);
      
      try {
         return ((SpaceTimeMoc)moc).getSpaceMoc(tmin, tmax);
       } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return null;
   }
   
   /** Retourne leTimeMoc correspondant au champ spatial de la vue courante,
    * ou null si impossible à définir */
   protected TimeMoc getCurrentTimeMoc() {
      try {
         return ((SpaceTimeMoc)moc).getTimeMoc( oLastDrawMoc );
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return null;
   }
   
   private double oLastDrawTmin = -1;
   private double oLastDrawTmax = -1;
   private SpaceMoc oLastDrawMoc = null;
   
   
   private boolean isDisplayedInPlot() { 
      int m=aladin.view.getNbView();
      for( int i=0; i<m; i++ ) {
         if( aladin.view.viewSimple[i].isPlotTime() ) return true;
      }
      return false;
   }
   
   protected void memoNewTime() {
//      System.out.println("memoNewTime");
      oLastDrawTmin = getLastDrawTmin();
      oLastDrawTmax = getLastDrawTmax();
      mocSpaceLowReset();
      oiz=-1;
      askForRepaint();
   }

   protected void memoNewSpace() {
//      System.out.println("memoNewSpace");
      oLastDrawMoc = getLastDrawMoc();
      mocTimeLowReset();
      oiz=-1;
      askForRepaint();
   }

   protected boolean isSpaceModified () {
      if( !isDisplayedInPlot() ) return false;
      Moc m = getLastDrawMoc();
      boolean rep = !mocEquals(m,oLastDrawMoc);
//      System.out.println("isSpaceModified = "+rep);
      return rep;
   }
   
   /** Return true if the two MOCs are equals. May b null */
   public boolean mocEquals(Moc m1, Moc m2) {
      if( m1==null && m2==null ) return true;
      if( m1==null || m2==null ) return false;
      return ((SpaceMoc)m1).equals(m2);
   }

   protected boolean isTimeModified () {
      if( !isDisplayedInPlot() ) return false;
      double tmin = getLastDrawTmin();
      double tmax = getLastDrawTmax();
      boolean rep = tmin!=oLastDrawTmin || tmax!=oLastDrawTmax;
//      System.out.println("isTimeModified = "+rep);
      return rep;
   }
   
   private SpaceMoc lastCurrentSpaceMoc = null;
   private TimeMoc lastCurrentTimeMoc = null;
   
   protected SpaceMoc getSpaceMoc() {
      if( lastCurrentSpaceMoc!=null && !isTimeModified() ) return lastCurrentSpaceMoc;
//      long t1 = System.currentTimeMillis();
      SpaceMoc m = getCurrentSpaceMoc();
//      long t2 = System.currentTimeMillis();
//      System.out.println("Regenerate currentSpaceMoc from "+((SpaceTimeMoc)moc).timeRange.sz/2+" ranges "
//            + "=> size="+(m==null?"null":m.getSize()) +" built in "+(t2-t1)+"ms ");
      lastCurrentSpaceMoc = m;
      memoNewTime();
      return lastCurrentSpaceMoc;
   }
   
   protected TimeMoc getTimeMoc() {
      if( lastCurrentTimeMoc!=null && !isSpaceModified() ) return lastCurrentTimeMoc;
//      long t1 = System.currentTimeMillis();
      TimeMoc m = getCurrentTimeMoc();
//      long t2 = System.currentTimeMillis();
//      System.out.println("Regenerate currentTimeMoc from "+((SpaceTimeMoc)moc).timeRange.sz/2+" ranges "
//            + "=> size="+(m==null?"null":m.getSize()) +" built in "+(t2-t1)+"ms ");
      lastCurrentTimeMoc = m;
      memoNewSpace();
      return lastCurrentTimeMoc;

      
//      try {
//         if( timeMoc==null ) timeMoc = ((SpaceTimeMoc)moc).getTimeMoc();
//      } catch( Exception e ) { }
//      return timeMoc;
   }
   

   
   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
      if( v.isPlotTime() ) drawInTimeView(g,v);
      else drawInSpaceView(g,v);
   }

}


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

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

import cds.moc.HealpixMoc;
import cds.moc.Moc;
import cds.moc.TimeMoc;
import cds.tools.Astrodate;
import cds.tools.Util;

/**
 * Génération d'un plan TMOC
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 Création - fév 2018
 */
public class PlanTMoc extends PlanMoc {

   protected Moc [] arrayTimeMoc =null;        // Le MOC à tous les ordres */

   public PlanTMoc(Aladin a) {
      super(a);
      type = ALLSKYTMOC;
   }
   
   protected PlanTMoc(Aladin aladin, MyInputStream in, String label) {
      super(aladin);
//      arrayTimeMoc = new Moc[CDSHealpix.MAXORDER+1];
      this.dis   = in;
      useCache = false;
      type = ALLSKYTMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="TMOC";
      setLabel(label);
      aladin.trace(3,"TMOC creation: "+Plan.Tp[type]);
      suite();
   }

   protected PlanTMoc(Aladin aladin, TimeMoc moc, String label) {
      super(aladin);
//      arrayTimeMoc = new Moc[CDSHealpix.MAXORDER+1];
      this.moc = moc;
      useCache = false;
      type = ALLSKYTMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="TMOC";
      setLabel(label);
      aladin.trace(3,"TMOC creation: "+Plan.Tp[type]);
      suite();
   }

   /** Ajoute des infos sur le plan */
   protected void addMessageInfo( StringBuilder buf, MyProperties prop ) {
      long nbMicrosec = ((TimeMoc)moc).getUsedArea();
      ADD( buf, "\n* Start: ",Astrodate.JDToDate( ((TimeMoc)moc).getTimeMin()));
      ADD( buf, "\n* End: ",Astrodate.JDToDate( ((TimeMoc)moc).getTimeMax()));
      ADD( buf, "\n* Sum: ",Util.getTemps(nbMicrosec/1000L, true));
      
      int order = getRealMaxOrder( (TimeMoc)moc);
      int drawOrder = getDrawOrder();
      ADD( buf,"\n","* Accuracy: "+ TimeMoc.getTemps(  TimeMoc.getDuration(order)));
      ADD( buf,"\n","* TMOC order: "+ (order==drawOrder ? order+"" : drawOrder+"/"+order));
   }
   
   protected boolean isTime() { return true; }
   
   /** Retourne le time stamp minimal */
   protected double getTimeMin() { 
      double tmin = ((TimeMoc)moc).getTimeMin();
      if( tmin==-1 ) tmin=Double.NaN;
      return tmin;
   }

   /** Retourne le time stamp maximal */
   protected double getTimeMax() { 
      double tmax = ((TimeMoc)moc).getTimeMax();
      if( tmax==-1 ) tmax=Double.NaN;
      return tmax;
   }
   
   /** Retourne le time Range global du plan, Double.NaN,Double.NaN si non défini */
   protected double [] getTimeRange() { return new double[] { getTimeMin(), getTimeMax() }; }


   protected boolean waitForPlan() {
      if( dis!=null ) {
         error=null;
         try {
            if( moc==null && dis!=null ) {
               moc = new TimeMoc();
               if(  (dis.getType() & MyInputStream.FITS)!=0 ) moc.readFits(dis);
               else moc.readASCII(dis);
            }
            if( moc.getSize()==0 ) error="Empty TMOC";
         }
         catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            return false;
         }
      }

      return true;
   }
   
   protected TimeMoc getTimeMoc() { return (TimeMoc)moc; }
   
   static protected int getRealMaxOrder(HealpixMoc m) { return m.getMocOrder(); }
   
   protected  boolean isSpaceModified() { return false; }
   
   protected boolean mocTimeLowReset=false;
   protected void mocTimeLowReset()  { mocTimeLowReset=true;  }
   
   
   private int lastTimeOrderDrawn = -1;
   protected int getDrawOrder() { return lastTimeOrderDrawn; }

   
   protected Moc getTimeMocLow(int order,int gapOrder) {
      Moc moc = getTimeMoc();
      
      // On fournit le meilleur MOC dans le cas de la génération d'une image
      if( aladin.NOGUI ) return moc;

      order += gapOrder;
      if( order>moc.getMocOrder() ) order=moc.getMocOrder();
      if( order<0 ) order=0;
      if( arrayTimeMoc==null || arrayTimeMoc[order]==null || mocTimeLowReset ) {
         if( arrayTimeMoc==null ) arrayTimeMoc = new TimeMoc[ Moc.MAXORDER+1];
         arrayTimeMoc[order] = new TimeMoc();   // pour éviter de lancer plusieurs threads sur le meme calcul
         final int myOrder = order;
         final int myMo=moc.getMocOrder();
         Aladin.trace(4,"PlanTMoc.getHealpixMocLow("+myOrder+") running...");
         Moc mocLow = myOrder==myMo ? moc : moc.clone();
         try { mocLow.setMocOrder(myOrder); }
         catch( Exception e ) { e.printStackTrace(); }
         arrayTimeMoc[myOrder]=mocLow;
         Aladin.trace(4,"PlanTMoc.getHealpixMocLow("+myOrder+") done !");
         askForRepaint();
      }
      // peut être y a-t-il déjà un MOC de plus basse résolution déjà prêt
      if( arrayTimeMoc[order].getSize()==0 ) {
         isLoading=true;
         int i=order;
         for( ; i>=5 && (arrayTimeMoc[i]==null || arrayTimeMoc[i].getSize()==0); i--);
         if( i>=5 ) order=i;
      } else isLoading=false;

      lastTimeOrderDrawn = order;
      return arrayTimeMoc[order];
   }
   
   protected double oz=-1;
   
   
//   // Fournit le MOC qui couvre le champ de vue courant
//   protected HealpixMoc getViewMoc(ViewSimple v,int order) throws Exception {
//      TMoc m = new TMoc();
//      m.rangeSet.append( (long)(v.getTpsMin()*TMoc.DAYMICROSEC), (long)(v.getTpsMax()*TMoc.DAYMICROSEC) );
//      m.toHealpixMoc();
//      return m;
//   }
   
   static private final int MAXDRAWCELL = 100;  // Nombre de cellules TMOC à tracer dans la vue temporelle
   
   // Retourne l'ordre du TMoc le plus approprié en fonction du zoom de la vue temporelle
   protected int getDrawingOrder(ViewSimple v) {
      Plot plot = v.plot;
      double dureeView = plot.getMax() - plot.getMin();
      int o;
      for( o=TimeMoc.MAXORDER; o>=3; o-- ) {
         double nbCell = dureeView / ( TimeMoc.getDuration(o)/1000000.);
         if( nbCell<MAXDRAWCELL ) return o;
      }
      return o;
   }
   
   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
      if( !v.isPlotTime() ) return;
     drawInTimeView(g,v);
   }
   
   private double [] lastDrawTimeRange = new double[] { Double.NaN, Double.NaN };
   
   protected double [] getLastDrawTimeRange() { return lastDrawTimeRange; }
   
   protected double getLastDrawTmin() { return lastDrawTimeRange[0]; }
   protected double getLastDrawTmax() { return lastDrawTimeRange[1]; }
   
      
   // Tracé du MOC visible dans la vue
   protected void drawInTimeView(Graphics g,ViewSimple v) {
      Plot plot = v.plot;
      
      boolean flagBorder = false; //isDrawingBorder();
      boolean flagfillIn = true;  //isDrawingFillIn()
      
      g.setColor(c);
      
      double tmin = plot.getMin();
      double tmax = plot.getMax();
      
      v.setTimeRange( new double[] { tmin, tmax });
      
      int drawingOrder = getDrawingOrder(v);
      
      TimeMoc lowMoc = (TimeMoc ) getTimeMocLow(drawingOrder,gapOrder);
      mocTimeLowReset=false;
      
      Iterator<long[]> it = lowMoc.jdIterator(tmin, tmax);
      long [] jdRange=null;

      ArrayList<Rectangle> a = new ArrayList<>();
      while( it.hasNext() ) {
         jdRange = it.next();
         a.add( computeRectangle(plot, jdRange[0]/TimeMoc.DAYMICROSEC,jdRange[1]/TimeMoc.DAYMICROSEC) );
      }
      
      // Tracé en aplat avec demi-niveau d'opacité
      if( flagfillIn && g instanceof Graphics2D ) {
         Graphics2D g2d = (Graphics2D)g;
         Composite saveComposite = g2d.getComposite();
         try {
            g2d.setComposite( Util.getImageComposite(getOpacityLevel())); //getFactorOpacity()) );
            for( Rectangle r : a ) {
               if( flagBorder && width<=1 ) continue;
               g.fillRect(r.x,r.y, r.width, r.height);
            }
         } finally {
            g2d.setComposite(saveComposite);
         }
      }
      
      // Tracé des bords
      if( flagBorder ) {
         for( Rectangle r : a ) g.drawRect(r.x,r.y, r.width, r.height);
      }
   }
   
   static public final int BAND = 20;
   static public final int MARGE = 30;
   
   private Rectangle computeRectangle(Plot plot, double jdstart, double jdstop ) {
      Rectangle r = new Rectangle();
      Coord c = new Coord();
      c.al = jdstart;
      c.del = 0;
      plot.getProj().getXY(c);
      PointD a = plot.viewSimple.getPositionInView(c.x, c.y);
      c.al = jdstop;
      plot.getProj().getXY(c);
      PointD b = plot.viewSimple.getPositionInView(c.x, c.y);
      r.x=(int)a.x;
      r.y= plot.viewSimple.getHeight() -(BAND-BAND/4)*(timeStackIndex+1)-MARGE;
      r.width=(int)Math.abs(b.x-a.x);
      if( r.width==0) r.width=1;
      r.height=BAND;
      return r;
   }
   
   protected void planReady(boolean ready) {
      setPourcent(-1);
      active=true;
      flagOk=ready;
      aladin.synchroPlan.stop(startingTaskId);
      flagWaitTarget=false;
      flagProcessing = false;
      aladin.calque.resetTimeRange();
      planReadyPost();
   }

   protected void planReadyPost() { aladin.view.createView4TMOC(this); }


}


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

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

import cds.moc.HealpixMoc;
import cds.moc.TMoc;
import cds.tools.Astrodate;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Génération d'un plan TMOC
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 Création - fév 2018
 */
public class PlanTMoc extends PlanMoc {

   public PlanTMoc(Aladin a) { super(a); }
   
   protected PlanTMoc(Aladin aladin, MyInputStream in, String label) {
      super(aladin);
      arrayMoc = new HealpixMoc[CDSHealpix.MAXORDER+1];
      this.dis   = in;
      useCache = false;
      type = ALLSKYTMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="TMOC";
      setLabel(label);
      aladin.trace(3,"TMOC creation: "+Plan.Tp[type]);
      wireFrame=DRAW_FILLIN;
      suite();
   }

   /** Ajoute des infos sur le plan */
   protected void addMessageInfo( StringBuilder buf, MyProperties prop ) {
      long nbMicrosec = moc.getUsedArea();
      ADD( buf, "\n* Start: ",Astrodate.JDToDate( ((TMoc)moc).getTimeMin()));
      ADD( buf, "\n* End: ",Astrodate.JDToDate( ((TMoc)moc).getTimeMax()));
      ADD( buf, "\n* Sum: ",Util.getTemps(nbMicrosec/1000, true));
      
      int order = getRealMaxOrder(moc);
      int drawOrder = getDrawOrder();
      ADD( buf,"\n","* Accuracy: "+ Util.getTemps(  ( 1L<<(2*(HealpixMoc.MAXORDER-order)))/1000L ));
      ADD( buf,"\n","* TMOC order: "+ (order==drawOrder ? order+"" : drawOrder+"/"+order));
   }
   
   protected boolean isTime() { return true; }
   
   /** Retourne le time stamp minimal */
   protected double getTimeMin() { 
      double tmin = ((TMoc)moc).getTimeMin();
      if( tmin==-1 ) tmin=Double.NaN;
      return tmin;
   }

   /** Retourne le time stamp maximal */
   protected double getTimeMax() { 
      double tmax = ((TMoc)moc).getTimeMax();
      if( tmax==-1 ) tmax=Double.NaN;
      return tmax;
   }

   protected boolean waitForPlan() {
      if( dis!=null ) {
         error=null;
         try {
            if( moc==null && dis!=null ) {
               moc = new TMoc();
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
   
   static protected int getRealMaxOrder(HealpixMoc m) { return m.getMocOrder(); }
   
   private int oGapOrder;
   
   protected HealpixMoc getHealpixMocLow(int order,int gapOrder) {
      int o = order;
      
      // On fournit le meilleur MOC dans le cas de la génération d'une image
      if( aladin.NOGUI ) return moc;

      order += gapOrder;
      if( order<0 ) order=0;
      if( order>moc.getMocOrder() ) order=moc.getMocOrder();
      if( arrayMoc[order]==null ) {
         arrayMoc[order] = new HealpixMoc();   // pour éviter de lancer plusieurs threads sur le meme calcul
         final int myOrder = order;
         final int myMo=moc.getMocOrder();
//         (new Thread("PlanTMoc building order="+order){
//
//            public void run() {
               Aladin.trace(4,"PlanTMoc.getHealpixMocLow("+myOrder+") running...");
               HealpixMoc mocLow = myOrder==myMo ? moc : (HealpixMoc)moc.clone();
               try { mocLow.setMocOrder(myOrder); }
               catch( Exception e ) { e.printStackTrace(); }
               arrayMoc[myOrder]=mocLow;
               Aladin.trace(4,"PlanTMoc.getHealpixMocLow("+myOrder+") done !");
               askForRepaint();
//            }

//         }).start();

      }
      // peut être y a-t-il déjà un MOC de plus basse résolution déjà prêt
      if( arrayMoc[order].getSize()==0 ) {
         isLoading=true;
         int i=order;
         for( ; i>=5 && (arrayMoc[i]==null || arrayMoc[i].getSize()==0); i--);
         if( i>=5 ) order=i;
      } else isLoading=false;

      lastOrderDrawn = order;
      return arrayMoc[order];
   }
   
   private double oz=-1;
   
   
//   // Fournit le MOC qui couvre le champ de vue courant
//   protected HealpixMoc getViewMoc(ViewSimple v,int order) throws Exception {
//      TMoc m = new TMoc();
//      m.rangeSet.append( (long)(v.getTpsMin()*TMoc.DAYMICROSEC), (long)(v.getTpsMax()*TMoc.DAYMICROSEC) );
//      m.toHealpixMoc();
//      return m;
//   }
   
   static private final int MAXDRAWCELL = 100;  // Nombre de cellules TMOC à tracer dans la vue temporelle
   
   // Retourne l'ordre du TMoc le plus approprié en fonction du zoom de la vue temporelle
   private int getDrawingOrder(ViewSimple v) {
      Plot plot = v.plot;
      double dureeView = plot.getMax() - plot.getMin();
      int o;
      for( o=TMoc.MAXORDER; o>=3; o-- ) {
         double nbCell = dureeView / ( TMoc.getDuration(o)/1000000.);
         if( nbCell<MAXDRAWCELL ) return o;
      }
      return o;
   }
   
   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
      Plot plot = v.plot;
      if( !v.isPlotTime() ) return;
      
      boolean flagBorder = isDrawingBorder();
      
      long t = Util.getTime();
      g.setColor(c);
      
      double tmin = plot.getMin();
      double tmax = plot.getMax();
      
      int drawingOrder = getDrawingOrder(v);
      
      TMoc lowMoc = (TMoc)getHealpixMocLow(drawingOrder,gapOrder);
      System.out.println("tmin = "+Astrodate.JDToDate(tmin)+" tmax="+Astrodate.JDToDate(tmax)+" drawingOrder="+lowMoc.getMocOrder());
      
      Iterator<long[]> it = lowMoc.jdIterator(tmin, tmax);
      long [] jdRange=null;

      ArrayList<Rectangle> a = new ArrayList<Rectangle>();
      while( it.hasNext() ) {
         jdRange = it.next();
         a.add( computeRectangle(plot, jdRange[0]/TMoc.DAYMICROSEC,jdRange[1]/TMoc.DAYMICROSEC) );
      }
      
      // Tracé en aplat avec demi-niveau d'opacité
      if( isDrawingFillIn() && g instanceof Graphics2D ) {
         Graphics2D g2d = (Graphics2D)g;
         Composite saveComposite = g2d.getComposite();
         try {
            g2d.setComposite( Util.getImageComposite(getOpacityLevel()*getFactorOpacity()) );
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

      System.out.println("draw in "+(System.currentTimeMillis()-t)+"ms");

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
      aladin.calque.repaintAll();
   }


}


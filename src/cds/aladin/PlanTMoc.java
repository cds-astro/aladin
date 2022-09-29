// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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
import java.util.ArrayList;
import java.util.Iterator;

import cds.moc.Moc;
import cds.moc.Moc1D;
import cds.moc.TMoc;
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
   protected ArrayList<MyRect> lastRectDrawn = null;  // La liste des rectangles de tracé du TMoc dans un View temporellle

   public PlanTMoc(Aladin a) {
      super(a);
      type = ALLSKYTMOC;
   }
   
   protected PlanTMoc(Aladin aladin, MyInputStream in, String label,String url) {
      super(aladin);
      this.dis   = in;
      this.url = url;
      useCache = false;
      type = ALLSKYTMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="TMOC";
      setLabel(label);
      aladin.trace(3,"TMOC creation: "+Plan.Tp[type]);
      suite();
   }

   protected PlanTMoc(Aladin aladin, TMoc moc, String label,String url) {
      super(aladin);
      this.moc = moc;
      this.url = url;
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
//      long nbMicrosec = ((TMoc)moc).getNbValues();
      boolean isEmpty = moc.isEmpty();
      if( !isEmpty ) {
         ADD( buf, "\n* Start: ",Astrodate.JDToDate( ((TMoc)moc).getTimeMin()));
         ADD( buf, "\n* End: ",Astrodate.JDToDate( ((TMoc)moc).getTimeMax()));
      }
      ADD( buf,"\n* # ranges: ",moc.getNbRanges()+"");
      
      int order = getRealMaxOrder( (TMoc)moc);
      int drawOrder = isEmpty ? -1 : getDrawOrder();
      ADD( buf,"\n","* Resolution: "+ Util.getTemps(  TMoc.getDuration(order)));
      ADD( buf,"\n","* Order: "+ (order==drawOrder ? order+""  : drawOrder+"/"+order));
      if( isEmpty ) ADD( buf,"\n \nRAM: ",Util.getUnitDisk( moc.getMem() ) );
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
   
   /** Retourne le time Range global du plan, Double.NaN,Double.NaN si non défini */
   protected double [] getTimeRange() { return new double[] { getTimeMin(), getTimeMax() }; }


   protected boolean waitForPlan() {
      if( dis!=null ) {
         error=null;
         try {
            if( moc==null && dis!=null ) {
               moc = new TMoc();
               readMoc(moc,dis);
            }
         }
         catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            error="MOC error";
            return false;
         }
      }

      return true;
   }
   
   protected TMoc getTimeMoc() { return (TMoc)moc; }
   
   static protected int getRealMaxOrder(Moc1D m) { return m.getMocOrder(); }
   
   protected  boolean isSpaceModified() { return false; }
   
   protected boolean mocTimeLowReset=false;
   protected void mocTimeLowReset()  { mocTimeLowReset=true;  }
   
   
   private int lastTimeOrderDrawn = -1;
   protected int getDrawOrder() { return lastTimeOrderDrawn; }

   
   protected Moc getTimeMocLow(int order,int gapOrder) {
      TMoc moc = getTimeMoc();
      
      // On fournit le meilleur MOC dans le cas de la génération d'une image
      if( aladin.NOGUI ) return moc;

      order += gapOrder;
      if( order>moc.getMocOrder() ) order=moc.getMocOrder();
      if( order<0 ) order=0;
      
      if( arrayTimeMoc==null || arrayTimeMoc[order]==null || mocTimeLowReset ) {
         if( arrayTimeMoc==null ) arrayTimeMoc = new TMoc[ TMoc.MAXORD_T+1];
         arrayTimeMoc[order] = new TMoc();   // pour éviter de lancer plusieurs threads sur le meme calcul
         int myOrder;
         myOrder = order;
         try {
            final int myMo=moc.getMocOrder();
            Aladin.trace(4,"PlanTMoc.getTimeMocLow("+myOrder+") running...");
            TMoc mocLow = myOrder==myMo ? moc : moc.clone();
            mocLow.setMocOrder(myOrder);
            arrayTimeMoc[myOrder]=mocLow;
         } catch( Exception e ) {
            e.printStackTrace();
         }
         Aladin.trace(4,"PlanTMoc.getTimeMocLow("+myOrder+") done !");
         askForRepaint();
      }
      // peut être y a-t-il déjà un MOC de plus basse résolution déjà prêt
      if( arrayTimeMoc[order].isEmpty() ) {
         isLoading=true;
         int i=order;
         for( ; i>=MINLIMIT && (arrayTimeMoc[i]==null || arrayTimeMoc[i].isEmpty()); i--);
         if( i>=MINLIMIT ) order=i;
      } else isLoading=false;

      lastTimeOrderDrawn = order;
      return arrayTimeMoc[order];
   }
   
   protected double oz=-1;
   
   
//   // Fournit le MOC qui couvre le champ de vue courant
//   protected SMoc getViewMoc(ViewSimple v,int order) throws Exception {
//      TMoc m = new TMoc();
//      m.rangeSet.append( (long)(v.getTpsMin()*TMoc.DAYMICROSEC), (long)(v.getTpsMax()*TMoc.DAYMICROSEC) );
//      m.toHealpixMoc();
//      return m;
//   }
   
   static private final int MAXDRAWCELL = 300;  // Nombre de cellules TMOC à tracer dans la vue temporelle
   static private final int MINLIMIT = 9;
   
   // Retourne l'ordre du TMoc le plus approprié en fonction du zoom de la vue temporelle
   protected int getDrawingOrder(ViewSimple v) {
      Plot plot = v.plot;
      double dureeView = plot.getMax() - plot.getMin();
      int o;
      for( o=TMoc.MAXORD_T; o>=MINLIMIT; o-- ) {
         double nbCell = dureeView / ( TMoc.getDuration(o)/TMoc.DAYMICROSEC);
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
      if( moc==null ) return;
      Plot plot = v.plot;
      
      boolean flagBorder = isDrawingBorder();
      boolean flagfillIn = isDrawingFillIn();
      
      g.setColor(c);
      
      double tmin = plot.getMin();
      double tmax = plot.getMax();
      
      int drawingOrder;
      if( moc.getNbRanges()<MAXDRAWCELL ) drawingOrder=moc.getTimeOrder();
      else drawingOrder=getDrawingOrder(v);
      
      TMoc lowMoc = (TMoc ) getTimeMocLow(drawingOrder,gapOrder);
      mocTimeLowReset=false;
      
      Iterator<long[]> it = lowMoc.jdIterator(tmin, tmax);
      ArrayList<MyRect> a = new ArrayList<>();
      while( it.hasNext() ) a.add( computeRectangle(plot, drawingOrder,it.next()) );
      
      // Tracé en aplat avec demi-niveau d'opacité
      if( flagfillIn && g instanceof Graphics2D ) {
         Graphics2D g2d = (Graphics2D)g;
         Composite saveComposite = g2d.getComposite();
         try {
            g2d.setComposite( Util.getImageComposite(getOpacityLevel()*getFactorOpacity())); //getFactorOpacity()) );
            for( RectangleD r : a ) {
               if( flagBorder && r.width<=1 ) continue;
               g.fillRect((int)r.x,(int)r.y, (int)r.width, (int)r.height);
            }
         } finally {
            g2d.setComposite(saveComposite);
         }
      }
      
      if( flagBorder ) {
         for( RectangleD r : a ) g.drawRect((int)r.x,(int)r.y, (int)r.width, (int)r.height);
      }
      
      // Mémorisation des rectangles pour la vue qui contient la souris
      if( v==aladin.view.getMouseView() ) lastRectDrawn = a;
   }
   
   static public final int BAND = 20;
   static public final int MARGE = 30;
   
   protected int getTimeStackIndex() { return timeStackIndex; }
   
   private MyRect computeRectangle(Plot plot, int drawingOrder, long [] range ) {
      MyRect r = new MyRect();
      
      // Mémorisation des bornes en microsec
      r.start = range[0];   // inclus
      r.end = range[1];     // exclus
      
      // Mémorisation du order associé
      r.order=drawingOrder;
      
      Coord c = new Coord();
      
      // Début
      c.al = r.start/TMoc.DAYMICROSEC;
      c.del = 0;
      plot.getProj().getXY(c);
      PointD a = plot.viewSimple.getPositionInView(c.x, c.y);
      
      // Fin (incluse)
      c.al = r.end/TMoc.DAYMICROSEC;
      plot.getProj().getXY(c);
      PointD b = plot.viewSimple.getPositionInView(c.x, c.y);
      
      // Mapping sur le rectangle
      r.x=a.x;
      r.y= plot.viewSimple.getHeight() -(BAND-BAND/4)*(getTimeStackIndex()+1)-MARGE;
      r.width=Math.abs(b.x-a.x);
      if( r.width<1 ) r.width=1;
      r.height=BAND;
      
      return r;
   }
   
   class MyRect extends RectangleD {
      long start;
      long end;
      int order;
      MyRect() { super(); }
      MyRect(double x, double y, double w, double h) { super(x,y,w,h); }
   }
   
   protected void planReady(boolean ready) {
      setPourcent(-1);
      active=true;
      flagOk=ready;
      aladin.synchroPlan.stop(startingTaskId);
      flagWaitTarget=false;
      flagProcessing = false;
      planReadyPost();
      aladin.view.repaintAll();
   }
   

   protected void planReadyPost() { aladin.view.createView4TMOC(this); }


}


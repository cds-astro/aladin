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
import cds.moc.MocCell;
import cds.moc.TMoc;
import cds.tools.Astrodate;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Hpix;
import cds.tools.pixtools.HpixT;

/**
 * Génération d'un plan TMOC
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 Création - fév 2018
 */
public class PlanTMoc extends PlanMoc {

   public PlanTMoc(Aladin a) { super(a); }
   
   
   // Juste pour tester
   protected PlanTMoc(Aladin aladin, String label) {
      super(aladin);
      arrayMoc = new HealpixMoc[CDSHealpix.MAXORDER+1];

      TMoc moc=null;
      try {
         moc = new TMoc();
         //    2017-05-09T10:39:00  .. 2017-05-12T15:10:00
         moc.add( 2457882.94375000, 2457886.13194444);
         moc.toHealpixMoc();
      } catch( Exception e ) {
         e.printStackTrace();
      }
      
      this.moc = moc;
      
      useCache = false;
      type = ALLSKYTMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="TMOC";
      setLabel(label);
      aladin.trace(3,"HiPS creation: "+Plan.Tp[type]);
      suite();
   }


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
      aladin.trace(3,"HiPS creation: "+Plan.Tp[type]);
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
   
   // Retourne l'ordre du TMoc le plus approprié en fonction du zoom temporel actuel
   private int getDrawingOrder(ViewSimple v) {
      int o;
      
      double z = v.getTpsZoom();
      
      for( o=TMoc.MAXORDER; o>=3; o-- ) {
         double size = TMoc.getDuration(o)/1000000 * z;
         if( size>=3*10000 ) return o;                  // Une case temporel doit faire au-moins 3 pixels de large
      }
      return o;
   }
   
   private double oz=-1;
   
   
   // Fournit le MOC qui couvre le champ de vue courant
   protected HealpixMoc getViewMoc(ViewSimple v,int order) throws Exception {
      TMoc m = new TMoc();
      m.rangeSet.append( (long)(v.getTpsMin()*TMoc.DAYMICROSEC), (long)(v.getTpsMax()*TMoc.DAYMICROSEC) );
      m.toHealpixMoc();
      return m;
   }

   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
      
      v.initTpsZoomIfRequired(this);
      
      long t1 = Util.getTime();
      g.setColor(c);
      
      
      try {
         long t=0;
         int myOrder = getDrawingOrder(v);
         double z = v.getTpsZoom();
         t = System.currentTimeMillis();
         
         int drawingOrder = 0;
         HealpixMoc lowMoc = null;
         boolean flagBorder = isDrawingBorder();
         boolean flagFill = isDrawingFillIn();

         int gapOrder = this.gapOrder;
         if( mustDrawFast() ) gapOrder--;

         HealpixMoc m = getViewMoc(v,HealpixMoc.MAXORDER);
         int n = aladin.calque.getIndex(this) - aladin.calque.plan.length;
         
         // Génération des Hpix concernées par le champ de vue
         if( oz!=z || gapOrder!=oGapOrder ) {
            lowMoc = getHealpixMocLow(myOrder,gapOrder);
            drawingOrder = getRealMaxOrder(lowMoc);
            if( drawingOrder==-1 ) return;
            ArrayList<Hpix> a1 = new ArrayList<Hpix>(10000);
            Iterator<MocCell> it = lowMoc.iterator();
            while( it.hasNext() ) {
               MocCell c = it.next();
               if( m!=null && !m.isIntersecting(c.order, c.npix)) continue;
               HpixT p = new HpixT(n, c.order, c.npix);
               if( p.isOutView(v) ) continue;
               a1.add(p);
            }
            arrayHpix=a1;
            oz=z;
            oGapOrder=gapOrder;
         }
         
         // Tracé en aplat avec demi-niveau d'opacité
         if( flagFill && arrayHpix!=null && g instanceof Graphics2D ) {
            Graphics2D g2d = (Graphics2D)g;
            Composite saveComposite = g2d.getComposite();
            try {
               g2d.setComposite( Util.getImageComposite(getOpacityLevel()*getFactorOpacity()) );
               for( Hpix p : arrayHpix ) {
                  boolean small = isDrawingBorder() && p.getDiag2(v)<25;
                  if( !small ) p.fill(g, v);
               }
            } finally {
               g2d.setComposite(saveComposite);
            }
         }


         // Tracé des Hpix concernés par le champ de vue
         if( flagBorder && arrayHpix!=null ) {
            for( Hpix p1 : arrayHpix ) ((HpixT)p1).draw(g, v);
         }
         
         // Mémorisation du rectangle englobant
         Rectangle clip = null;
         if( arrayHpix!=null ) {
            for( Hpix p1 : arrayHpix ) {
               Rectangle r = ((HpixT)p1).getClip(v);
               if( clip==null ) clip=r;
               else if( r!=null ) clip = clip.union( r );
            }
         }
         v.updateTpsArea( clip );

//         t1 = System.currentTimeMillis();
//         System.out.println("draw " in "+(t1-t)+"ms"+(n>0 ? " => "+(double)n/(t1-t)+"/ms":"") );

         t = Util.getTime();
         statTimeDisplay = t-t1;
         

      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
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


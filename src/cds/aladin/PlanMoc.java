// Copyright 2010 - UDS/CNRS
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

import java.awt.Graphics;
import java.util.Iterator;

import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Hpix;

public class PlanMoc extends PlanBGCat {
   private HealpixMoc moc = null;
   private int res;
   
   
   protected PlanMoc(Aladin aladin,String label,Plan[] p,int res) {
      this(aladin,null,label,p[0].co,30);
      try {
         moc = new HealpixMoc();
         moc.setCoordSys("C");
         frameOrigin=Localisation.ICRS;
         this.res=res;
         moc.setCheckConsistencyFlag(false);
         for( Plan p1 : p ) addMocFromPlan(p1);
         moc.setMaxLimitOrder(res);
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      flagProcessing=false;
      flagOk=true;
      setActivated(true);
      aladin.calque.repaintAll();
   }
   
   private void addMocFromPlan(Plan p1 ) {
      if( p1.isCatalog() ) addMocFromCatalog(p1);
      else if( p1.isImage() ) addMocFromImage(p1);
   }
   
   private void addMocFromCatalog(Plan p1) {
      Iterator<Obj> it = p1.iterator();
      Healpix hpx = new Healpix();
      int order = res;
      long oNpix=-1;  
      Coord coo = new Coord();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         try {
            coo.al = ((Position)o).raj;
            coo.del = ((Position)o).dej;
            long npix = hpx.ang2pix(order, coo.al, coo.del);
            
            // Juste pour éviter d'insérer 2x de suite le même npix
            if( npix==oNpix ) continue;

            moc.add(order,npix);
            oNpix=npix;
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
   }

   private void addMocFromImage(Plan p1) {
      PlanImage pimg = (PlanImage)p1;
      Healpix hpx = new Healpix();
      int order = res;
      double gapD = ( 0.5*CDSHealpix.pixRes( CDSHealpix.pow2(res) )/3600.);
      Coord coo = new Coord();
      coo=p1.projd.getProjCenter();
      coo = p1.projd.getXYNative(coo);
      double y1 = coo.y;
      coo.del+=gapD;
      coo = p1.projd.getXYNative(coo);
      double gap = Math.abs(y1-coo.y);
      if( gap>1 ) gap=1;
      System.out.println("res="+res+" gapD="+Coord.getUnit(gapD)+" gap="+gap);
      
      long oNpix=-1;  
      for( double y=0; y<pimg.naxis2; y+=gap ) {
         for( double x=0; x<pimg.naxis1; x+=gap ) {
            try {
               coo.x = x;
               coo.y = y;
               pimg.projd.getCoord(coo);
               long npix = hpx.ang2pix(order, coo.al, coo.del);

               // Juste pour éviter d'insérer 2x de suite le même npix
               if( npix==oNpix ) continue;

               moc.add(order,npix);
               oNpix=npix;
            } catch( Exception e ) {
               if( aladin.levelTrace>=3 ) e.printStackTrace();
            }
         }
      }
   }


   protected PlanMoc(Aladin aladin,String label,PlanMoc p1,PlanMoc p2,int fct) {
      super(aladin);
      p1.copy(this);
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label = getFonction(p1,p2,fct);
      setLabel(label);
      
      aladin.trace(3,"AllSky computation: "+Plan.Tp[type]+" => "+getFonction(p1,p2,fct));
      
//      setLabel(label == null ? "conv["+label+"]" : label);
      try {
         launchAlgo(p1,p2,fct);
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         aladin.error = error = e.getMessage();
         flagOk=false;
      }
   }
   
   
   static final int UNION = 0;
   static final int INTERSECTION = 1;
   static final int SUBTRACTION = 2;
   static final int COMPLEMENT = 3;
   
         
   static private final String [] FCT = { "union","inter","sub","compl"};
   static protected String getFct(int fct) {
      return FCT[fct];
   }
   
   protected String getFonction(PlanMoc p1,PlanMoc p2,int fct) {
      return p1.label + " "+ getFct(fct) + (p2!=null ? " "+p2.label : "");
   }

   
   protected void launchAlgo(PlanMoc p1,PlanMoc p2,int fct) throws Exception {
      flagProcessing=true;
      switch(fct) {
         case UNION :        moc = p1.getMoc().union( p2.getMoc() ); break;
         case INTERSECTION : moc = p1.getMoc().intersection( p2.getMoc() ); break;
         case SUBTRACTION :  moc = p1.getMoc().subtraction( p2.getMoc() ); break;
         case COMPLEMENT :   moc = p1.getMoc().complement(); break;
      }
      moc.setMinLimitOrder(3);

      from = "Computed by Aladin";
      String s = getFonction(p1,p2,fct);
      param = "Computed: "+s;
      flagProcessing=false;
      flagOk=true;
      setActivated(true);
      aladin.calque.repaintAll();

      sendLog("Compute"," [" + this + " = "+s+"]");

   }
   
   protected PlanMoc(Aladin aladin, MyInputStream in, String label, Coord c, double radius) {
      super(aladin);
      this.dis   = in;
      useCache = false;
      frameOrigin=Localisation.GAL;
      type = ALLSKYMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="Hpx coverage map";
      setLabel(label);
      co=c;
      coRadius=radius;
      aladin.trace(3,"AllSky creation: "+Plan.Tp[type]+(c!=null ? " around "+c:""));
      suite();
   }
   
   /** Retourne le Moc */
   protected HealpixMoc getMoc() { return moc; }
   
   protected void suiteSpecific() {
      isOldPlan=false;
   }
   protected boolean isLoading() { return false; }
   protected boolean isSync() { return isReady(); }
   protected void reallocObjetCache() { }

   
   private boolean wireFrame=true;
   public void setWireFrame(boolean flag) { wireFrame=flag; }
   public boolean getWireFrame() { return wireFrame; }
   
   protected boolean waitForPlan() {
      if( dis==null ) return true;
      super.waitForPlan();
      try {
         moc = new HealpixMoc();
         moc.setMinLimitOrder(3);
         if(  (dis.getType() & MyInputStream.FITS)!=0 ) moc.readFits(dis);
         else moc.readASCII(dis);
         String c = moc.getCoordSys();
         frameOrigin = ( c==null || c.charAt(0)=='G' ) ? Localisation.GAL : Localisation.ICRS;
      }
      catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return false;
      }
      return true;
   }
   
   protected Iterator<Obj> iterator() { return null; }
   protected void resetProj(int n) { }
   protected boolean isDrawn() { return true; }
   
   private HealpixMoc getViewMoc(ViewSimple v,int order) throws Exception {
      Coord center = getCooCentre(v);
      long [] pix = getPixList(v,center,order);

      HealpixMoc m = new HealpixMoc();
      m.setCoordSys(moc.getCoordSys());
      for( int i=0; i<pix.length; i++ ) m.add(order,pix[i]);
      m.sort();
      return m;
   }
   
   private Hpix [] hpixList = null;
   private Hpix [] hpixListLow = null;
   private Hpix [] getHpixList(ViewSimple v,boolean low) {
      if( hpixList==null ) {
         hpixList = new Hpix[moc.getSize()];
         int n=0;
         Iterator<MocCell> it = moc.iterator();
         while( it.hasNext() ) {
            MocCell h = it.next();
            hpixList[n++] = new Hpix(h.order,h.npix,frameOrigin);
         }
         
         int N=6;
         hpixListLow = hpixList;
         if( true || /* moc.getMaxOrder()>N && */ hpixList.length>12*Math.pow(4,N-1) ) {
            HealpixMoc hpixLow = (HealpixMoc)moc.clone();
            try { hpixLow.setMaxLimitOrder(N); } 
            catch( Exception e ) { e.printStackTrace(); }
            hpixListLow = new Hpix[moc.getSize()];
            n=0;
            it = hpixLow.iterator();
            while( it.hasNext() ) {
               MocCell h = it.next();
               hpixListLow[n++] = new Hpix(h.order,h.npix,frameOrigin);
            }
         }

      }
      return low?hpixListLow : hpixList;
//      return hpixList;
   }
   
   private boolean oDrawAll=false; // dernier état connu pour le voyant d'état de la pile
   private boolean drawAll=true;  // true si la totalité de ce qui doit être dessiné l'a été
   
   /** Retourne true si tout a été dessinée, sinon false */
   protected boolean hasMoreDetails() { oDrawAll = drawAll; return !drawAll; }
   
   
   protected boolean mustDrawFast() { return aladin.view.mustDrawFast(); }
   
//   protected void draw(Graphics g,ViewSimple v) {
//      long t1 = Util.getTime();
//      g.setColor(c);
//      int max = Math.min(maxOrder(v),maxOrder)+1;
//      boolean mustDrawFast = mustDrawFast();
//      int tLimit = mustDrawFast ? 30 : 75;
//      double taille = v.getTaille();
//      
//      try {
//         HealpixMoc m = v.isAllSky() ? null : getViewMoc(v,max);
//         int r=0,d=0;
//         int order=0;
//         long t=0;
//         int i;
//         long delai = Util.getTime()-lastDrawAll;
//         boolean canDrawAll = !mustDrawFast && delai>300;
//         boolean lowMoc = taille>30 || mustDrawFast;
//         Hpix [] hpixList = getHpixList(v,lowMoc);
////      System.out.println("lowMoc="+lowMoc+" mustDrawFast="+mustDrawFast+" canDrawAll="+canDrawAll+" lastDrawAll="+delai);
//         for( i=0; i<hpixList.length; i++ ) {
//            if( (!canDrawAll || v.zoom<1/128.) && !(t<tLimit || order<max+4) ) break;
//            Hpix p = hpixList[i];
//            if( p==null ) break;
//            order=p.getOrder();
//            if( m!=null && !m.isInTree(order, p.getNpix())) { r++; continue; }
//            if( p.isOutView(v) ) continue;
//            if( wireFrame ) p.draw(g, v);
//            else p.fill(g, v);
//            d++;
//            if( d%100==0 ) t=Util.getTime()-t1;
//         }
//         drawAll = i==hpixList.length;
//         t = Util.getTime();
//         statTimeDisplay = t-t1;
//         if( drawAll ) lastDrawAll=t;
//         if( drawAll!=oDrawAll ) aladin.calque.select.repaint();  // pour faire évoluer le voyant d'état
////      System.out.println("draw "+hpixList.length+" rhombs mocView="+(moc==null?"null":moc.getMaxOrder()+"/"+moc.getSize())+" reject="+r+" drac="+d+" in "+statTimeDisplay+"ms");
//      } catch( Exception e ) {
//         if( Aladin.levelTrace>=3 ) e.printStackTrace();
//      }
//   }

   
   protected void draw(Graphics g,ViewSimple v) {
      long t1 = Util.getTime();
      g.setColor(c);
      int max = Math.min(maxOrder(v),maxOrder)+1;
      double taille = v.getTaille();
      
      try {
         HealpixMoc m = v.isAllSky() ? null : getViewMoc(v,max);
         int r=0,d=0;
         int order=0;
         long t=0;
         int i;
//         long delai = Util.getTime()-lastDrawAll;
         boolean lowMoc = taille>30 ;
         Hpix [] hpixList = getHpixList(v,lowMoc);
//      System.out.println("lowMoc="+lowMoc+" mustDrawFast="+mustDrawFast+" canDrawAll="+canDrawAll+" lastDrawAll="+delai);
         for( i=0; i<hpixList.length; i++ ) {
            Hpix p = hpixList[i];
            if( p==null ) break;
            order=p.getOrder();
            if( m!=null && !m.isInTree(order, p.getNpix())) { r++; continue; }
            if( p.isOutView(v) ) continue;
            if( wireFrame ) p.draw(g, v);
            else p.fill(g, v);
            d++;
            if( d%100==0 ) t=Util.getTime()-t1;
         }
         drawAll = i==hpixList.length;
         t = Util.getTime();
         statTimeDisplay = t-t1;
         if( drawAll ) lastDrawAll=t;
         if( drawAll!=oDrawAll ) aladin.calque.select.repaint();  // pour faire évoluer le voyant d'état
//      System.out.println("draw "+(lowMoc?" low":"")+" "+hpixList.length+" rhombs mocView="+(moc==null?"null":moc.getMaxOrder()+"/"+moc.getSize())+" reject="+r+" drac="+d+" in "+statTimeDisplay+"ms");
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   private long lastDrawAll=0L;
   
}


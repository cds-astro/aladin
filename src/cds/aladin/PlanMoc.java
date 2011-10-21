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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import cds.fits.HeaderFits;
import cds.moc.HealpixMoc;
import cds.tools.Util;
import cds.tools.pixtools.Hpix;
import cds.tools.pixtools.HpixTree;

public class PlanMoc extends PlanBGCat {
   private HpixTree hpix = null;
   
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
      aladin.trace(3,"AllSky creation: "+Plan.Tp[type]+(c!=null ? " around "+c:""));
      suite(c,radius);
   }
   
   /** Retourne le Moc */
   protected HealpixMoc getMoc() { return hpix; }
   
   protected void suiteSpecific() { }
   protected boolean isLoading() { return false; }
   protected boolean isSync() { return isReady(); }
   protected void reallocObjetCache() { }

   
   private boolean wireFrame=true;
   public void setWireFrame(boolean flag) { wireFrame=flag; }
   public boolean getWireFrame() { return wireFrame; }
   
   protected boolean waitForPlan() {
      try {
         hpix = new HpixTree(dis);
         headerFits = hpix.getHeaderFits();
         frameOrigin = hpix.getFrame();
//         if( hpix.getOrdering()!=HpixTree.NESTED ) throw new Exception("ORDERING=RING is not supported");
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
   
   private HealpixMoc getViewMoc(ViewSimple v,int order) {
      Coord center = getCooCentre(v);
      long [] pix = getPixList(v,center,order);

      HealpixMoc moc = new HealpixMoc();
      moc.setCoordSys(hpix.getCoordSys());
      for( int i=0; i<pix.length; i++ ) moc.add(order,pix[i]);
      moc.sort();
      return moc;
   }
   
   private Hpix [] hpixList = null;
   private Hpix [] getHpixList(ViewSimple v) {
      if( hpixList==null ) {
         hpixList = new Hpix[hpix.getSize()];
         int n=0;
         Iterator<long[]> it = hpix.iterator();
         while( it.hasNext() ) {
            long [] h = it.next();
            hpixList[n++] = new Hpix((int)h[0],h[1],frameOrigin);
         }
      }
      return hpixList;
   }
   
   private boolean oDrawAll=false; // dernier état connu pour le voyant d'état de la pile
   private boolean drawAll=true;  // true si la totalité de ce qui doit être dessiné l'a été
   
   /** Retourne true si tout a été dessinée, sinon false */
   protected boolean hasMoreDetails() { oDrawAll = drawAll; return !drawAll; }
   
   
   protected boolean mustDrawFast() { return aladin.view.mustDrawFast(); }
   
   protected void draw(Graphics g,ViewSimple v) {
      long t1 = Util.getTime();
      g.setColor(c);
      int max = Math.min(maxOrder(v),maxOrder)+1;
      boolean mustDrawFast = mustDrawFast();
      int tLimit = mustDrawFast ? 30 : 75;
      
      HealpixMoc moc = v.isAllSky() ? null : getViewMoc(v,max);
      Hpix [] hpixList = getHpixList(v);
      int r=0,d=0;
      int order=0;
      long t=0;
      int i;
      long delai = Util.getTime()-lastDrawAll;
      boolean canDrawAll = !mustDrawFast && delai>300;
//      System.out.println("mustDrawFast="+mustDrawFast+" canDrawAll="+canDrawAll+" lastDrawAll="+delai);
      for( i=0; i<hpixList.length; i++ ) {
         if( (!canDrawAll || v.zoom<1/128.) && !(t<tLimit || order<max+4) ) break;
         Hpix p = hpixList[i];
         order=p.getOrder();
         if( moc!=null && !moc.isInTree(order, p.getNpix())) { r++; continue; }
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
//      System.out.println("draw "+hpixList.length+" rhombs mocView="+(moc==null?"null":moc.getMaxOrder()+"/"+moc.getSize())+" reject="+r+" drac="+d+" in "+statTimeDisplay+"ms");
   }
   
   private long lastDrawAll=0L;
   
}


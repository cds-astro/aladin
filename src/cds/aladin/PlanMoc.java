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

import cds.fits.HeaderFits;
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
   
   protected void suiteSpecific() { }
   protected boolean isLoading() { return false; }
   protected boolean isSync() { return isReady(); }
   
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
   protected boolean hasMoreDetails() { return false; }
   
   private Hpix [] hpixList = null;
   private Hpix [] getHpixList() {
      if( hpixList==null ) {
         hpixList = new Hpix[hpix.getSize()];
         int n=0;
         Iterator<long[]> it = hpix.iterator();
         while( it.hasNext() ) {
            long [] hpix = it.next();
            hpixList[n++] = new Hpix((int)hpix[0],hpix[1],frameOrigin);
         }
      }
      return hpixList;

   }
   
   protected void draw(Graphics g,ViewSimple v) {
      // JE POURRAIS EGALEMENT RECUPERER LES PIXELS QUI RECOUVRE LE CHAMP VISIBLE
      // POUR NE PRENDRE QUE LES Hpix QUI TOMBENT DEDANS (COMME POUR LES IMAGES)
      long t1 = Util.getTime();
//      int maxOrder = getOrder()+4;
      g.setColor(c);
      Hpix [] hpixList = getHpixList();
      for( int i=0; i<hpixList.length; i++ ) {
         Hpix p = hpixList[i];
//         if( p.getOrder()>maxOrder ) break;
         if( !p.isOutView(v) ) {
            if( wireFrame ) p.draw(g, v);
            else p.fill(g, v);
         }
      }
      statTimeDisplay = Util.getTime()-t1;
   }


}


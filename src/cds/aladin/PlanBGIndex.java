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

import java.util.Hashtable;

import cds.allsky.BuilderProgenIndex;


public class PlanBGIndex extends PlanBG {


   private PlanBG planBG;
   
   protected boolean isVerbose() { return false; }

   protected PlanBGIndex(Aladin aladin,PlanBG planBG) {
      super(aladin);
      this.planBG=planBG;
      type=ALLSKYFINDEX;
      url = planBG.url;
      survey = planBG.survey;
      version = planBG.version;
      useCache = planBG.useCache;
      frameOrigin=planBG.frameOrigin;
      imageSourcePath=planBG.imageSourcePath;
      maxOrder = planBG.maxOrder;
      pixList = new Hashtable<String,HealpixKey>(1000);
      loader = new HealpixLoader();
   }
   
   protected void log() { }
   protected void clearBuf() { }
   protected HealpixKey getHealpixFromAllSky(int order,long npix) { return null; }

   /** Demande de chargement du losange repéré par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {
      HealpixKey pixAsk = new HealpixKeyIndex(this,order,npix);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }

   /** Retourne l'order max du dernier affichage */
   protected int getCurrentMaxOrder(ViewSimple v) { return Math.max(2,Math.min(maxOrder(v),maxOrder)); }
   
   /** Retourne le losange Healpix s'il est chargé, sinon retourne null
    * et si flagLoad=true, demande en plus son chargement si nécessaire */
   protected HealpixKey getHealpix(int order,long npix,boolean flagLoad) {
      HealpixKey healpix =  pixList.get( key(order,npix) );
      if( healpix!=null ) return healpix;
      if( flagLoad ) return askForHealpix(order,npix);
      return null;
   }

   protected void updateHealpixIndex(ViewSimple v) {
      long [] pix=null;
      HealpixIndex hi = new HealpixIndex();
      int order = maxOrder(v)+1;
//      System.out.println("Order="+order+" maxOrder="+maxOrder+" isAllsky="+v.isAllSky()+ " nop = "+(order<BuilderProgenIndex.MINORDER && maxOrder>=BuilderProgenIndex.MINORDER || v.isAllSky()));
      
      // On n'a pas assez zoomé pour afficher le contenu des losanges
      if( order<BuilderProgenIndex.MINORDER && maxOrder>=BuilderProgenIndex.MINORDER || v.isAllSky() ) { this.hi = hi; return; }
      if( order>maxOrder ) order=maxOrder;
      
      int nb=0;
      boolean allKeyReady=true;

      hasDrawnSomething=false;

      setMem();
      resetPriority();

      pix = getPixListView(v,order);
      
      for( int i=0; i<pix.length; i++ ) {

         if( (new HealpixKey(this,order,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) continue;

         HealpixKeyIndex healpix = (HealpixKeyIndex)getHealpix(order,pix[i], true);

         // Inconnu => on ne dessine pas
         if( healpix==null ) continue;
         
         // Positionnement de la priorité d'affichage
         healpix.priority=250-(priority++);

         int status = healpix.getStatus();

         // Losange erroné ?
         if( status==HealpixKey.ERROR ) continue;

         // On change d'avis
         if( status==HealpixKey.ABORTING ) healpix.setStatus(HealpixKey.ASKING,true);

         // Losange à gérer
         healpix.resetTimer();

         if( status!=HealpixKey.READY ) allKeyReady=false;

         // Pas encore prêt
         if( status!=HealpixKey.READY ) continue;

         nb += healpix.addHealpixIndexItem(hi,v);
      }
     
      this.hi = hi;

      allWaitingKeysDrawn = allKeyReady;

      hasDrawnSomething=hasDrawnSomething || nb>0;

      if( pix!=null && pix.length>0  ) tryWakeUp();
   }
   
   private HealpixIndex hi=null;
   
   protected HealpixIndex getHealpixIndex() { return hi; }
   
   protected void askForRepaint() { planBG.askForRepaint();  }
   
   protected void gc() { }
}

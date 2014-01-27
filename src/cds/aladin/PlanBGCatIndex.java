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
import java.util.TreeMap;

import cds.allsky.BuilderDetails;

public class PlanBGCatIndex extends PlanBGCat {


   protected PlanBGCatIndex(Aladin aladin) {
      super(aladin);
   }
   
   protected PlanBGCatIndex(Aladin aladin, TreeNodeAllsky gluSky,String label, Coord c, double radius,String startingTaskId) {
      super(aladin,gluSky,label, c,radius,startingTaskId);
   }
   
   protected void setSpecificParams(TreeNodeAllsky gluSky) {
      type = ALLSKYCAT;
      c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      scanProperties();
      frameOrigin=Localisation.ICRS;
      url = gluSky.getUrl();
      local = gluSky.isLocal();
      survey = getAssociatedSurvey()+cds.tools.Util.FS+survey;
      loadGenericLegende();
   }
   
   // Retourne le nom du survey associé (se base sur le fait que HpxFinder
   // est précédé du nom du survey
   private String getAssociatedSurvey() { 
      String s = url.replace('\\','/');
      int fin = s.lastIndexOf("/HpxFinder");
      int deb = s.lastIndexOf('/', fin-1);
      String associatedSurvey = s.substring(deb+1,fin);
//      System.out.println("URL => "+url+" ["+associatedSurvey+"]");
      return associatedSurvey; 
   }

   protected int getTileMode() { return HealpixKey.IDX; }
   
   protected boolean hasAssociatedFootprints() { return true; }
   
   /** Demande de chargement du losange repéré par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {
      readyAfterDraw=false;
      HealpixKey pixAsk = new HealpixKeyCatIndex(this,order,npix);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }
   
   protected void draw(Graphics g,ViewSimple v) {
      prepareDraw(v);
      
      if( pcat==null || !pcat.hasObj() ) return;
      pcat.draw(g, null, v, true, false, 0, 0);
   }
   
   private HealpixAllskyCatIndex allsky;
   
   /** Dessin du ciel complet en rapide à l'ordre indiqué */
   protected boolean drawAllSky(ViewSimple v,TreeMap<String, Source> map,int order) {
      boolean hasDrawnSomething=false;
      if( allsky==null ) {
         allsky = new HealpixAllskyCatIndex(this,order);
         pixList.put( key(order,-1), allsky);

         if( local ) allsky.loadFromNet();
         else {
            if( !useCache || !allsky.isCached() ) {
               tryWakeUp();
               return true;
            } else {
               allsky.loadFromCache();
               pourcent=-1;
            }
         }
      }

      if( allsky.getStatus()==HealpixKey.READY ) {
         hasDrawnSomething = allsky.draw(map)>0;
         if( hasDrawnSomething ) fusion(map);
      }
      return allsky.getStatus()!=HealpixKey.ERROR;
   }

   
   private boolean prepareDraw(ViewSimple v) {
      long [] pix=null;
      int nb=0;
      long nLoaded=0L;
      long nTotal=0L;
      TreeMap<String, Source> map = new TreeMap<String, Source>();
      setHasMoreDetails(true);
      
      int order = maxOrder(v)+1;
//      System.out.println("Order="+order+" maxOrder="+maxOrder+" isAllsky="+v.isAllSky()+ " nop = "+(order<BuilderProgenIndex.MINORDER && maxOrder>=BuilderProgenIndex.MINORDER || v.isAllSky()));
      
      // On n'a pas assez zoomé pour afficher le contenu des losanges
      if( order<BuilderDetails.MINORDER && maxOrder>=BuilderDetails.MINORDER ) {
         return false;
      }
      if( order>maxOrder ) order=maxOrder;
      
      hasDrawnSomething=false;
      
      if( drawAllSky(v, map, 3) )  return hasDrawnSomething;
      
      setMem();
      resetPriority();
      pix = getPixListView(v,order);
      boolean moreDetails = false;

      for( int i=0; i<pix.length; i++ ) {
         if( isOutMoc(order, pix[i]) ) continue;
         if( (new HealpixKey(this,order,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) continue;
         nTotal++;

         HealpixKeyCatIndex healpix = (HealpixKeyCatIndex)getHealpix(order,pix[i], true);
         if( healpix==null ) continue;            // Inconnu => on ne dessine pas
         healpix.priority=250-(priority++);       // Positionnement de la priorité d'affichage
         int status = healpix.getStatus();
         if( status==HealpixKey.ERROR ) continue; // Losange erroné ?
         if( status==HealpixKey.ABORTING ) healpix.setStatus(HealpixKey.ASKING,true); // On change d'avis
         if( status!=HealpixKey.READY ) { moreDetails = true; continue; }             // Pas encore prêt

         nb += healpix.draw(map);
         if( nb>0 ) healpix.resetTimer(); // Losange à gérer
         nLoaded++;
      }
      
//      completude  = !moreDetails ? 100 : 100 * ((double)nLoaded/nTotal);
      setHasMoreDetails(moreDetails);
      allWaitingKeysDrawn = nTotal==nLoaded;
      
      fusion(map);
      hasDrawnSomething=hasObj();
      if( pix!=null && pix.length>0  ) tryWakeUp();
      return hasDrawnSomething;
   }
   
   private void fusion(TreeMap<String, Source> map) {
      Pcat pcat1 = new Pcat(this);
      if( pcat!=null && pcat.hasObj() ) {
         Iterator<Obj> it = pcat.iterator();
         while( it.hasNext() ) {
            Source src = (Source)it.next();
            String id = src.id;
            boolean isInMap = map.containsKey(id); 
            if( isInMap || src.isSelected() ) map.put(id,src);
         }
      }
      
      for( Source src : map.values() ) {
         if( showFootprint ) src.setShowFootprint(true, false);
         pcat1.setObjetFast(src);
      }
      pcat=pcat1;
   }
   
   private boolean showFootprint=false;
   protected void setShowFootprint(boolean flag) {
      showFootprint = flag;
   }
      
   protected void resetProj(int n) { if( pcat!=null ) pcat.projpcat[n]=null; }

   protected void reallocObjetCache() { if( pcat!=null ) pcat.reallocObjetCache(); }

   protected int getNbTable() { return 1; }
   
   protected boolean hasObj() { return pcat==null ? false : pcat.hasObj(); }

   protected boolean hasSources() { return hasObj(); }

   protected int getCounts() { return pcat==null ? 0 : pcat.getCount(); }

   protected Iterator<Obj> iterator() { return pcat==null ? null : pcat.iterator(); }
   
   protected Iterator<Obj> iterator(ViewSimple v) { return iterator(); }
   
   protected boolean detectServerError(int nb[]) { completude=-1; pourcent=-1; return false; }



   
}

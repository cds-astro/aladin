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
import cds.allsky.Constante;

public class PlanBGProgen extends PlanBGCat {


   protected PlanBGProgen(Aladin aladin) {
      super(aladin);
   }

   protected PlanBGProgen(Aladin aladin, TreeObjDir gluSky,String label, Coord c, double radius,String startingTaskId) {
      super(aladin,gluSky,label, c,radius,startingTaskId);
   }

   protected void setSpecificParams(TreeObjDir gluSky) {
      type = ALLSKYCAT;
      c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      scanProperties();

      // R�cup�ration d'un �ventuel minorder
      if( prop!=null ) {
         String s = prop.getProperty(Constante.KEY_HIPS_ORDER_MIN);
         if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_ORDER_MIN);
         if( s!=null ) {
            try { minOrder = Integer.parseInt(s); }
            catch( Exception e ) {}
         }
      }

      frameOrigin=Localisation.ICRS;
      url = gluSky.getUrl();
      local = gluSky.isLocal();
      if( survey.equals(Constante.FILE_HPXFINDER) ) survey = getAssociatedSurvey() + cds.tools.Util.FS + survey;
      loadGenericLegende();
   }

   // retourne le nom du survey HiPS associ� � ces prog�niteurs.
   private String getAssociatedSurvey() {
      String dir = getAssociatedSurveyByProperties();
      if( dir==null ) dir = getAssociatedSurveByUrl();
      aladin.trace(3,"Associated HiPS survey ["+dir+"]");
      return dir;
   }

   // Retourne le nom du survey associ� (le cherche dans les properties du
   // HiPS parent (si il existe), null sinon
   private String getAssociatedSurveyByProperties() {
      String s = url.replace('\\','/');
      int fin = s.lastIndexOf("/"+Constante.FILE_HPXFINDER);
      if( fin==-1 ) return null;
      String propPath = s.substring(0,fin)+"/"+Constante.FILE_PROPERTIES;
      MyInputStream in=null;
      try {
         in = cds.tools.Util.openStream(propPath);
         prop = new MyProperties();
         prop.load(in);
         String label = prop.getProperty("label");
         if( label!=null ) return label;
      }
      catch( Exception e ) {}
      finally { if( in!=null ) try { in.close(); } catch( Exception e) {} }
      return null;
   }

   // Retourne le nom du survey associ� (se base sur le fait que HpxFinder
   // est pr�c�d� du nom du survey
   private String getAssociatedSurveByUrl() {
      String s = url.replace('\\','/');
      int fin = s.lastIndexOf("/"+Constante.FILE_HPXFINDER);
      int deb = s.lastIndexOf('/', fin-1);
      String associatedSurvey = s.substring(deb+1,fin);
      return associatedSurvey;
   }

   protected int getTileMode() { return HealpixKey.IDX; }

   protected boolean hasAssociatedFootprints() { return true; }

   /** Demande de chargement du losange rep�r� par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {
      readyAfterDraw=false;
      HealpixKey pixAsk = new HealpixKeyProgen(this,order,npix);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }

   protected void draw(Graphics g,ViewSimple v) {
      prepareDraw(v);

      if( pcat==null || !pcat.hasObj() ) return;
      pcat.draw(g, null, v, true, false, 0, 0);
   }

   private HealpixAllskyProgen allsky;

   /** Dessin du ciel complet en rapide � l'ordre indiqu� */
   protected boolean drawAllSky(ViewSimple v,TreeMap<String, Source> map,int order) {
      boolean hasDrawnSomething=false;
      if( allsky==null ) {
         allsky = new HealpixAllskyProgen(this,order);
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
      if( order<BuilderDetails.MINORDER ) order=BuilderDetails.MINORDER;
      //      System.out.println("Order="+order+" maxOrder="+maxOrder+" isAllsky="+v.isAllSky()+ " nop = "+(order<BuilderProgenIndex.MINORDER && maxOrder>=BuilderProgenIndex.MINORDER || v.isAllSky()));

      // On n'a pas assez zoom� pour afficher le contenu des losanges
      if( (order<BuilderDetails.MINORDER || order<minOrder) && maxOrder>=BuilderDetails.MINORDER ) {
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

         HealpixKeyProgen healpix = (HealpixKeyProgen)getHealpix(order,pix[i], true);
         if( healpix==null ) continue;            // Inconnu => on ne dessine pas
         int status = healpix.getStatus();
         if( status==HealpixKey.ERROR ) continue; // Losange erron� ?
         healpix.priority=250-(priority++);       // Positionnement de la priorit� d'affichage
         if( status==HealpixKey.ABORTING ) healpix.setStatus(HealpixKey.ASKING,true); // On change d'avis
         if( status!=HealpixKey.READY ) { moreDetails = true; continue; }             // Pas encore pr�t

         nb += healpix.draw(map);
         if( nb>0 ) healpix.resetTimer(); // Losange � g�rer
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
            String id = src.id+src.raj+src.dej;
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

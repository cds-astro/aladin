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

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import cds.tools.Util;


public class PlanBGCat extends PlanBG {


   static final protected int MAXGAPORDER = 3;  // D�calage maximal autoris�
   private int gapOrder=0;                      // D�calage de l'ordre Max => changement de densit�

   protected PlanBGCat(Aladin aladin) {
      super(aladin);
   }

   protected PlanBGCat(Aladin aladin, TreeNodeAllsky gluSky,String label, Coord c, double radius,String startingTaskId) {
      super(aladin,gluSky,label, c,radius,startingTaskId);
      aladin.log(Plan.Tp[type],label);
   }

   protected int getGapOrder() { return gapOrder; }
   protected void setGapOrder(int gapOrder) {
      if( Math.abs(gapOrder)>MAXGAPORDER ) return;
      this.gapOrder=gapOrder;
   }

   protected void setSpecificParams(TreeNodeAllsky gluSky) {
      type = ALLSKYCAT;
      c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
   }
   
   protected boolean isSync() {
      boolean isSync = super.isSync();
      isSync = isSync && (planFilter==null || planFilter.isSync() );
      return isSync;
   }


   protected void suiteSpecific() {
      isOldPlan=false;
      pixList = new Hashtable<String,HealpixKey>(1000);
      allsky=null;
      if( error==null ) loader = new HealpixLoader();
   }

   protected void log() { }
   
   /** Retourne true si l'image a �t� enti�rement "draw�" � la r�solution attendue */
   protected boolean isFullyDrawn() { return readyDone && allWaitingKeysDrawn; }

   
   protected void draw(Graphics g,ViewSimple v, int dx, int dy,float op,boolean now) {
      if( v==null ) return;
      if( op==-1 ) op=getOpacityLevel();
      if(  op<=0.1 ) return;
      
      if( g instanceof Graphics2D ) {
         Graphics2D g2d = (Graphics2D)g;
         Composite saveComposite = g2d.getComposite();
         try {
            if( op < 0.9 ) {
               Composite myComposite = Util.getImageComposite(op);
               g2d.setComposite(myComposite);
            }
            draw(g2d, v);

         } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
         g2d.setComposite(saveComposite);

      } else draw(g, v);

      readyDone = readyAfterDraw;
   }
   
   @Override
   protected void clearBuf() { }

   // Pas support� pour les catalogues
   protected HealpixKey getHealpixFromAllSky(int order,long npix) { return null; }

   /** Demande de chargement du losange rep�r� par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {
      HealpixKey pixAsk;

      readyAfterDraw=false;

      pixAsk = new HealpixKeyCat(this,order,npix);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }

   private boolean localAllSky = false;   // A VIRER LORSQUE SIMBAD EN NET DISTANT
   private HealpixAllskyCat allsky[] = new HealpixAllskyCat[4];

   protected int getLosangeOrder() { return 9; }

   /** Dessin du ciel complet en rapide � l'ordre indiqu� */
   protected boolean drawAllSky(Graphics g,ViewSimple v,int order) {
      boolean hasDrawnSomething=false;
      if( allsky[order]==null ) {
         allsky[order] = new HealpixAllskyCat(this,order);
         pixList.put( key(order,-1), allsky[order]);

         if( localAllSky ) allsky[order].loadFromNet();
         else {
            if( !useCache || !allsky[order].isCached() ) {
               tryWakeUp();
               return true;
            } else {
               allsky[order].loadFromCache();
               pourcent=-1;
            }
         }
      }

      if( allsky[order].getStatus()==HealpixKey.READY ) {
         hasDrawnSomething = allsky[order].draw(g,v)>0;
      }
      return hasDrawnSomething;
   }

   /** Retourne l'order max du dernier affichage */
   protected int getCurrentMaxOrder(ViewSimple v) { return Math.max(2,Math.min(maxOrder(v)+gapOrder,maxOrder)); }

   protected void draw(Graphics g,ViewSimple v) {
      long [] pix=null;
      int order = getCurrentMaxOrder(v);
      int nb=0;
      boolean allKeyReady=true;
      
      hasDrawnSomething=drawAllSky(g, v,  2);
      if( order>=3 ) hasDrawnSomething|=drawAllSky(g, v,  3);

      setMem();
      resetPriority();

      boolean moreDetails=order<=3;
      
      for( int norder=4; norder<=order; norder++ ) {

         pix = getPixListView(v,norder);
         for( int i=0; i<pix.length; i++ ) {

            if( (new HealpixKey(this,norder,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) continue;

            // Teste si le losange p�re est d�j� charg� et est le dernier de sa branche
            // et dans ce cas, on ne tente pas de charger le suivant
            if( norder>4 ) {
               HealpixKeyCat pere = (HealpixKeyCat)getHealpix(norder-1,pix[i]/4,false);
               if( pere!=null && pere.isLast() ) continue;
            }

            HealpixKeyCat healpix = (HealpixKeyCat)getHealpix(norder,pix[i], true);
            
            // Juste pour tester la synchro
//            Util.pause(100);

            // Inconnu => on ne dessine pas
            if( healpix==null ) continue;
            
            // Positionnement de la priorit� d'affichage
            healpix.priority=250-(priority++);

            int status = healpix.getStatus();

            // Losange erron� ?
            if( status==HealpixKey.ERROR ) continue;

            // On change d'avis
            if( status==HealpixKey.ABORTING ) healpix.setStatus(HealpixKey.ASKING,true);

            // Losange � g�rer
            healpix.resetTimer();
            
            if(norder==order && status!=HealpixKey.READY ) allKeyReady=false;

            // Pas encore pr�t
            if( status!=HealpixKey.READY ) { moreDetails = true; continue; }

            nb += healpix.draw(g,v);

            HealpixKeyCat h = (HealpixKeyCat)healpix;
//            System.out.println(h.getStringNumber()+" => reallyLast="+h.isReallyLast(v));
            if( !moreDetails && !h.isReallyLast(v) ) moreDetails = true;
         }
      }

      setHasMoreDetails(moreDetails);
      allWaitingKeysDrawn = allKeyReady;

      hasDrawnSomething=hasDrawnSomething || nb>0;

      if( pix!=null && pix.length>0  ) tryWakeUp();
   }

   /** Demande de r�affichage des vues */
   protected void askForRepaint() {
      updateFilter();
      aladin.view.repaintAll();
   }

   /** Force le reset de l'influence des filtres sur ce plan */
   protected void updateFilter() {
      planFilter.updateNow();
   }

   /** Suppression d'un losange catalogue si possible (aucun objet s�lectionn�) */
   protected void purge(HealpixKey healpix) {
      int n = ((HealpixKeyCat)healpix).free(false);
      if( n==0 ) return;
      nbFlush+=n;
      if( nbFlush>2000  ) gc();
      pixList.remove( key(healpix) );
   }

   /** Force le recalcul de la projection n */
   protected void resetProj(int n) {
      proj[n]=null;

      Enumeration<HealpixKey> e = pixList.elements();
      while( e.hasMoreElements() ) {
         HealpixKeyCat healpix = (HealpixKeyCat)e.nextElement();
         if( healpix.getStatus()!=HealpixKey.READY || healpix.pcat==null ) continue;
        healpix.pcat.projpcat[n]=null;
      }
   }

   protected void reallocObjetCache() {
      Enumeration<HealpixKey> e = pixList.elements();
      while( e.hasMoreElements() ) {
         HealpixKeyCat healpix = (HealpixKeyCat)e.nextElement();
         if( healpix.getStatus()!=HealpixKey.READY || healpix.pcat==null ) continue;
        healpix.pcat.reallocObjetCache();
      }
   }

   protected int getNbTable() { return 1; }

   protected Vector<Legende> getLegende() {
      Vector<Legende> v = new Vector<Legende>();
      v.addElement(getFirstLegende());
      return v;
   }


   private Legende leg=null;
   protected void setLegende(Legende leg) {
      this.leg=leg;
      setFilter(filterIndex);
   }
   protected Legende getFirstLegende() { return leg; }

   protected boolean hasObj() { return hasSources(); }

   /** retourne true si le plan a des sources */
   protected boolean hasSources() { return pixList!=null && pixList.size()>0; }

   protected int getCounts() {
      int n=0;
      Enumeration<HealpixKey> e = pixList.elements();
      while( e.hasMoreElements() ) {
         HealpixKeyCat healpix = (HealpixKeyCat)e.nextElement();
         if( healpix.getStatus()!=HealpixKey.READY ) continue;
         n += healpix.pcat!=null ? healpix.pcat.getCounts() : 0;
      }

      return n;
   }

   /** On r�cup�re une copie de la liste courante des objets */
   protected Obj[] getObj() {
      int n = getCounts();
      Obj [] obj = new Obj[n];
      Iterator<Obj> it = iterator();
      int i;
      for( i=0;  it.hasNext() && i<obj.length; i++ ) obj[i] = it.next();

      // Ca a raccourci entre temps => on tronque */
      if( i<obj.length ) {
         Obj [] o = new Obj[i];
         System.arraycopy(obj, 0, o, 0, i);
         obj = o;
      }
      return obj;
   }

   // Recup�ration d'un it�rator sur les objets
   protected Iterator<Obj> iterator() { return new ObjIterator(null); }

   // Recup�ration d'un it�rator sur les objets visible dans la vue v
   protected Iterator<Obj> iterator(ViewSimple v) { return new ObjIterator(v); }

   class ObjIterator implements Iterator<Obj> {
      Enumeration<HealpixKey> e = null;
      Iterator<Obj> it = null;
      int order;

      ObjIterator(ViewSimple v) {
         super();
         order = v!=null ? getCurrentMaxOrder(v) : -1;
      }

      public boolean hasNext() {
         while( it==null || !it.hasNext() ) {
            if( e==null ) {
                if (pixList==null) {
                    return false;
                }
                e = pixList.elements();
            }
            if( !e.hasMoreElements() ) return false;
            HealpixKeyCat healpix = (HealpixKeyCat)e.nextElement();
            if( healpix.getStatus()!=HealpixKey.READY ) continue;
            if( order!=-1 && healpix.order>order ) {
//               System.out.println("ne prend plus en compte (order="+healpix.order+" maxOrder="+order+") "+healpix);
               continue;
            }
            it = healpix.pcat.iterator();
         }
         return it.hasNext();
      }

      public void remove() { }
      public Obj next() {
         while( it==null || !it.hasNext() ) {
            if( e==null ) e = pixList.elements();
            if( !e.hasMoreElements() ) return null;
            HealpixKeyCat healpix = (HealpixKeyCat)e.nextElement();
            if( healpix.getStatus()!=HealpixKey.READY ) continue;
            if( order!=-1 && healpix.order>order ) continue;
            it = healpix.pcat.iterator();
         }
         return it.next();
      }
   }

}

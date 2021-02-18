// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import cds.allsky.Constante;
import cds.moc.SMoc;
import cds.tools.Util;

public class PlanBGCat extends PlanBG {


   static final protected int MAXGAPORDER = 3;  // Décalage maximal autorisé
   private int gapOrder=0;                      // Décalage de l'ordre Max => changement de densité
   protected int allskyExt=HealpixAllsky.XML;   // L'extension des fichiers allsky (dépend de la version du HiPS catalog)
   private Pcat genericPcat;

   protected PlanBGCat(Aladin aladin) {
      super(aladin);
      type = ALLSKYCAT;
   }

   protected PlanBGCat(Aladin aladin, TreeObjDir gluSky,String label, Coord c, double radius,String startingTaskId) {
      super(aladin,gluSky,label, c,radius,startingTaskId);
      aladin.log(Plan.Tp[type],label);
      setAllskyExt();
   }
   
   protected int getTileMode() { return HealpixKey.TSV; }

   protected int getGapOrder() { return gapOrder; }
   protected void setGapOrder(int gapOrder) {
      if( Math.abs(gapOrder)>MAXGAPORDER ) return;
      this.gapOrder=gapOrder;
   }

   protected void setSpecificParams(TreeObjDir gluSky) {
      type = ALLSKYCAT;
      c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      frameOrigin=gluSky.getFrame();
      scanProperties();
      loadGenericLegende();
   }
   
   protected boolean isCatalog() { return true; }
   
   protected boolean isTime() { return isCatalogTime(); }

   protected boolean isSync() {
      boolean isSync = super.isSync();
      isSync = isSync && (planFilter==null || planFilter.isSync() );
      return isSync;
   }

   protected void suiteSpecific() {
      isOldPlan=false;
      pixList = new Hashtable<>(1000);
      allsky=null;
      if( error==null ) loader = new HealpixLoader();
   }

   protected void log() { }
   
   /** Retourne true si l'image a été entièrement "drawé" à la résolution attendue */
   protected boolean isFullyDrawn() { return readyDone && allWaitingKeysDrawn; }
   
   /** Ajoute des infos sur le plan */
   protected void addMessageInfo( StringBuilder buf, MyProperties prop ) {
      String s;
      int n;
      if( (n=getCounts())>0 ) {
         ADD( buf, "\n* Sources loaded: ",String.format("%,d", n));
      }
      try { if( (s=prop.getFirst("nb_rows"))!=null ) ADD( buf,"\n* Total: ",String.format("%,d", Long.parseLong(s))); } catch( Exception e ) {}
      ADD( buf,"\n","* HiPS order: "+getOrder()+"/"+maxOrder);
   }
   
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

         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
         g2d.setComposite(saveComposite);

      } else draw(g, v);

      readyDone = readyAfterDraw;
   }
   
   @Override
   protected void clearBuf() { }

   // Pas supporté pour les catalogues
   protected HealpixKey getHealpixFromAllSky(int order,long npix) { return null; }

   /** Demande de chargement du losange repéré par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {
      HealpixKey pixAsk;

      readyAfterDraw=false;

      pixAsk = new HealpixKeyCat(this,order,npix);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }

   private HealpixAllskyCat allsky[] = new HealpixAllskyCat[4];

   protected int getTileOrder() { return 9; }
   
   /** Dessin du ciel complet en rapide à l'ordre indiqué */
   protected boolean drawAllSky(Graphics g,ViewSimple v,int order) {
      boolean hasDrawnSomething=false;
      if( allsky[order]==null ) {
         allsky[order] = new HealpixAllskyCat(this,order,allskyExt);
         pixList.put( key(order,-1), allsky[order]);

         if( local ) allsky[order].loadFromNet();
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
   
   protected int adjustMaxOrder(int lastMaxOrder,double pixSize) {
      return lastMaxOrder;
   }
   
   /** Retourne l'order max du dernier affichage */
   protected int getCurrentMaxOrder(ViewSimple v) { 
      return Math.max(1,Math.min(maxOrder(v)+gapOrder,maxOrder)); 
   }
   
   /** Pour eviter qu'une source précédemment sélectionnée dans une autre région reste
    * sélectionnable
    */
   private void resetDrawnInView(ViewSimple v) {
      Enumeration<HealpixKey> e = pixList.elements();
      while( e.hasMoreElements() ) {
         HealpixKeyCat healpix = (HealpixKeyCat)e.nextElement();
         if( healpix!=null ) healpix.resetDrawnInView(v);
      }
   }
   
   protected int getMinOrder() { return minOrder==-1 ? 1 : minOrder; }

   protected void draw(Graphics g,ViewSimple v) {
      long [] pix=null;
      int order = getCurrentMaxOrder(v);
      int nb=0;
      boolean allKeyReady=true;
      long nLoaded=0L;
      long nTotal=0L;
      boolean allsky1=false,allsky2=false,allsky3=false;
      boolean hipsOld = allskyExt==HealpixAllsky.XML;  // Vieille version d'un HiPS catalog
      int minOrder = hipsOld?3: getMinOrder(); //1;
      
//      System.out.println("hipsOld="+hipsOld+" minOrder = "+minOrder);
      
      SMoc lastMoc = new SMoc();   // Moc des tuiles "last" (inutile d'aller plus loin)
      try { lastMoc.setCheckConsistencyFlag(false); } catch( Exception e1 ) { }
      
      resetDrawnInView(v);
      
//      System.out.println("order="+order+" hipsOld="+hipsOld);
      if( !hipsOld ) allsky1=drawAllSky(g, v,  1);
      if( hipsOld || order>=2 ) allsky2=drawAllSky(g, v,  2);
      if( order>=3 ) allsky3=drawAllSky(g, v,  3);
      
      hasDrawnSomething = allsky1 || allsky2 || allsky3;
      
      setMem();
      resetPriority();

      boolean moreDetails=order<=3;
      
      for( int norder= minOrder; norder<=order; norder++ ) {
         
         // Si on n'a fait le allsky, inutile de faire les losanges individuels correspondants
         // sauf s'il s'agit de l'ancienne forme des HiPS (uniquement pour norder=3)
         if( !hipsOld ) {
            if( norder==1 && allsky[1].getStatus()!=HealpixKey.ERROR ) continue;
            if( norder==2 && allsky[2].getStatus()!=HealpixKey.ERROR ) continue;
            if( norder==3 && allsky[3].getStatus()!=HealpixKey.ERROR ) continue;
         }
         
         pix = getPixListView(v,norder);
         if( pix==null ) return;
         for( int i=0; i<pix.length; i++ ) {
            
            // Losange à écarter ?
            if( lastMoc.isDescendant(norder, pix[i]) ) continue;
            if( isOutMoc(norder, pix[i]) )  continue;
            if( (new HealpixKey(this,norder,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) continue;

            HealpixKeyCat healpix = (HealpixKeyCat)getHealpix(norder,pix[i], true);
            
            // Juste pour tester la synchro
//            Util.pause(100);

            // Inconnu => on ne dessine pas
            if( healpix==null ) continue;
            
            int status = healpix.getStatus();

            // Losange erroné ?
            if( status==HealpixKey.ERROR ) continue;

            // On change d'avis
            if( status==HealpixKey.ABORTING ) healpix.setStatus(HealpixKey.ASKING,true);
            
            // Positionnement de la priorité d'affichage
            healpix.priority=250-(priority++);

            // Losange à gérer
            healpix.resetTimer();
            
            if(norder==order && status!=HealpixKey.READY ) allKeyReady=false;

            // Pas encore prêt
            if( status!=HealpixKey.READY ) { moreDetails = true; continue; }

            nb += healpix.draw(g,v);
            
            // Il n'y aura plus rien dans cette branche du HiPS
            if( norder!=order ) {
               if( healpix.isLast() ) {
                  try { lastMoc.add(healpix.order,healpix.npix); } catch( Exception e ) { }
               }
               
            // Y a-t-il encore des sources non visibles dans le champ (PAS SUR DE BIEN COMPRENDRE PF Mars 2019)
            } else  {
               HealpixKeyCat h = healpix;
               if( !moreDetails && !h.isReallyLast(v) ) moreDetails = true;
               nLoaded += h.nLoaded;
               nTotal += h.nTotal;
            }
         }
      }
      
      // On continue à afficher les sources sélectionnées même si elles appartiennent
      // à une tuile plus profonde
      drawTilesWithSelection(g,v,order);
      
      if( allKeyReady ) {
         completude = nTotal!=0 && nLoaded==nTotal ? 100 : 100* ((double)order/maxOrder);
         if( nTotal!=0 ) {
            double p1 = 100 * ((double)nLoaded/nTotal);
            if( p1>completude ) completude=p1;
         }
//         System.out.println("J'ai "+nLoaded+"/"+nTotal+" sources chargées order="+order+"/"+maxOrder+" soit "+getCompletude()+"% moreDetails="+moreDetails);
      }
      if( !moreDetails ) completude=100;

      setHasMoreDetails(order>=getMaxFileOrder() ? false : moreDetails);
      allWaitingKeysDrawn = allKeyReady;

      hasDrawnSomething=hasDrawnSomething || nb>0;

      if( moreDetails /* pix!=null && pix.length>0*/  ) tryWakeUp();
      
//      System.out.println(debug+"");
   }   
   
   // Affiche les sources sélectionnées ou tagguées pour les tuiles
   // plus profondes que order
   private int drawTilesWithSelection(Graphics g,ViewSimple v,int order) {
      int nb=0;
      for( HealpixKey healpix : pixList.values() ) {
         if( healpix.order<=order ) continue;
//         if( healpix.allSky ) continue;
         if( healpix.getStatus()!=HealpixKey.READY ) continue;
         if( !((HealpixKeyCat)healpix).pcat.hasSelectedOrTaggedObj() ) continue;
         if( healpix.isOutView(v) ) continue;
         nb += ((HealpixKeyCat)healpix).drawOnlySelected(g,v);
      }
//      System.out.println("drawTilesWithSelection() order="+order+" nb="+nb);
      return nb;
   }
   
   protected double completude=0;
   protected double getCompletude() { return completude; }

   private int oNbObj = -1;   // Compteur d'objets déjà chargées
   
   /** Demande de réaffichage des vues */
   protected void askForRepaint() {
      // Mise à jour des filtres (uniquement si le nombre d'objets à changé)
      // UN PEU CASSE GUEULE COMME TEST, mais pour le moment ça va faire l'affaire
      int nbObj = getCounts();
      if( oNbObj!=getCounts() ) {
         oNbObj = nbObj;
//         System.out.println("update filter");
//         for( PlanFilter pf : PlanFilter.allFilters ) System.out.println("."+pf);
         updateDedicatedFilter();
      }
      aladin.view.repaintAll();
   }
   
   protected void planReady(boolean ready) {
      super.planReady(ready);
      setFilter(filterIndex);
      askForRepaint();
   }
   
   protected boolean Free() {
      aladin.view.deSelect(this);
      super.Free();
      genericPcat=null;
      FilterProperties.notifyNewPlan();
      return true;
   }

   /** Suppression d'un losange catalogue si possible (aucun objet sélectionné) */
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
      Vector<Legende> v = new Vector<>();
      v.addElement( getFirstLegende() );
      return v;
   }

   protected Legende getFirstLegende() { return genericPcat==null ? null : genericPcat.leg; }
   protected boolean hasGenericPcat() {  return genericPcat!=null; }
   protected Pcat getGenericPcat() { return genericPcat; }
   protected boolean hasCatalogInfo() { return hasGenericPcat() && genericPcat.hasCatalogInfo(); }
   
   
   /** Mémorisation d'un pcat "étalon" qui va me servir pour connaître la légende générique
    * ou spécifique à la première tuile chargée
    */
   protected void setGenericPcat(Pcat pcat) {
      if( pcat==null ) return;
      
      if( genericPcat==null )  genericPcat=pcat;
      
      if( pcat.hasObj() ) {
         try {
            Source src = (Source)pcat.iterator().next();
            if( genericPcat.leg==null ) genericPcat.leg = src.getLeg();
//            setPattern(s);
            
            // Affectation à la légende courante d'une liste de pattern à base d'expressions régulières
            // pour effectuer l'extration des valeurs depuis le fichier JSON HpxFinder
            if( genericPcat.leg==null ) throw new Exception("cannot store pattern in null leg !");
            else {
               String [] pattern = src.getValues();
               for( int i=0; i<genericPcat.leg.field.length; i++ ) {
                  if( pattern[i].trim().length()==0 ) continue;
                  genericPcat.leg.field[i].hpxFinderPattern = pattern[i];
               }
            }

         } catch( Exception e ) { e.printStackTrace(); }
      }
   }

   /** Affectation à la légende courante d'une liste de pattern à base d'expressions régulières
    * pour effectuer l'extration des valeurs depuis le fichier JSON HpxFinder vers une vue sous forme de
    * table VOTable correspondante au ficheir metadata.xml.
    * Un pattern suit la syntaxe suivante :
    * vide => simple ordre positionnel (numéro de champ équivalent dans le JSON)
    * $[nom] => la valeur du champ JSON correspondant
    * $[nom]:regex => extraction et concatenation des groupes de la regex de la valeur du champ JSON "nom"
    * $[nom]:regex $[nom1]:regex ... idem mais pour plusieurs expressions
    * @param src
    * @throws Exception
    */
//   private void setPattern(Source src) throws Exception {
//      Legende leg = getFirstLegende();
//      if( leg==null ) throw new Exception("cannot store pattern in null leg !");
//      String [] pattern = src.getValues();
//      for( int i=0; i<leg.field.length; i++ ) {
//         if( pattern[i].trim().length()==0 ) continue;
//         leg.field[i].hpxFinderPattern = pattern[i];
//      }
//   }

   /** Charge la légende générique via le fichier metadata.xml (s'il existe) */
   protected void loadGenericLegende() {
      String filename = getUrl()+"/"+Constante.FILE_METADATAXML;
      Pcat pcat = new Pcat(this);
      MyInputStream in = null;
      try {
          pcat.tableParsing(in=Util.openAnyStream(filename),null);
          setGenericPcat(pcat);
      }
      catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
      finally { if( in!=null ) try { in.close(); } catch( Exception e ) {} }
   }
   
   /** Détermine si les fichiers Allsky ont une extension .xml ou .tsv
    * dépend de la version du HiPS catalog */
   protected void setAllskyExt() {
      String filename = getUrl()+"/Norder1/Allsky.tsv";
      MyInputStream in = null;
      try { in=Util.openAnyStream(filename); allskyExt = HealpixAllsky.TSV; } 
      catch( Exception e ) { }
      finally { if( in!=null ) try { in.close(); } catch( Exception e ) {} }
   }
   
   protected boolean hasObj() { return hasSources(); }

   /** retourne true si le plan a des sources */
   protected boolean hasSources() { return pixList!=null && pixList.size()>0; }

   protected int getCounts() {
      int n=0;
      Enumeration<HealpixKey> e = pixList.elements();
      while( e.hasMoreElements() ) {
         HealpixKeyCat healpix = (HealpixKeyCat)e.nextElement();
         if( healpix.getStatus()!=HealpixKey.READY ) continue;
         n += healpix.pcat!=null ? healpix.pcat.getCount() : 0;
      }

      return n;
   }
   
   /** On récupère une copie de la liste courante des objets */
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

   // Recupération d'un itérator sur les objets
   protected Iterator<Obj> iterator() { return new ObjIterator(null); }

   // Recupération d'un itérator sur les objets visible dans la vue v
   protected Iterator<Obj> iterator(ViewSimple v) { return new ObjIterator(v); }

   class ObjIterator implements Iterator<Obj> {
      Enumeration<HealpixKey> e = null;
      Iterator<Obj> it = null;
      int order;
      ViewSimple v;

      ObjIterator(ViewSimple v) {
         super();
         this.v=v;
         order = v!=null ? getCurrentMaxOrder(v) : -1;
         if( order==1 ) order=2;
      }

      public boolean hasNext() {
         while( it==null || !it.hasNext() ) {
            if( e==null ) {
                if (pixList==null) return false;
                e = pixList.elements();
            }
            if( !e.hasMoreElements() ) return false;
            HealpixKeyCat healpix = (HealpixKeyCat)e.nextElement();
            if( healpix.getStatus()!=HealpixKey.READY ) continue;
            if( order!=-1 && healpix.order>order ) {
//               System.out.println("ne prend plus en compte (order="+healpix.order+" maxOrder="+order+") "+healpix);
               continue;
            }
            it = healpix.pcat.iterator(v);
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
            it = healpix.pcat.iterator(v);
         }
         return it.next();
      }
   }

}

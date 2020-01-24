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

import java.awt.Graphics;
import java.util.Hashtable;

import cds.allsky.TabRgb;
import cds.moc.HealpixMoc;
import cds.moc.Moc;
import cds.moc.SpaceMoc;
import cds.tools.pixtools.CDSHealpix;

/** Gestion d'un Plan HiPS RGB dynamique utilisant les tuiles de deux ou trois autres plans HiPS.
 * 
 * Dans l'état de développement actuel, il ne peut y avoir qu'un seul plan de ce type dans la pile
 * et il est sous le controle du formulaire de génération des HiPS RGB (Tabrgb).
 * D'autre part le chargement est effectué en mode synchrone uniquement (loadnow()), ce qui introduit
 * des délais de chargement avant affichage qui bloque l'interface. Ce comportement est peu sensible
 * avec des plans locaux mais assez gênant pour des plans distants (cf remarque ci-dessous)
 * 
 * L'algo de tracé prend en compte les ordres Norder0,1 et 2, mais pas l'alternative Allsky
 * 
 * La version PROTO permet d'utiliser des plans distants, sinon seuls les plans HiPS locaux
 * sont pris en compte.
 * 
 * @author Pierre Fernique [CDS]
 * @version 1.0 Janvier 2020 - création
 */
public class PlanBGRgb extends PlanBG {

   public PlanBG red,green,blue;  // Les trois plans HiPS originaux
   protected TabRgb tabRgb;       // La référence au formulaire de contrôle

   protected PlanBGRgb(Aladin aladin, TabRgb tabRgb, String label, PlanBG red, PlanBG green, PlanBG blue,Coord co, double radius) {
      super(aladin);
      this.tabRgb = tabRgb;
      this.label = label;
      this.red=red; this.green=green; this.blue=blue;
      if( red!=null )   { red.addRecutListener(this);   if( !red.isTruePixels() )   red.switchFormat(); }
      if( green!=null ) { green.addRecutListener(this); if( !green.isTruePixels() ) green.switchFormat(); }
      if( blue!=null )  { blue.addRecutListener(this);  if( !blue.isTruePixels() )  blue.switchFormat(); }
      pixMode = PIX_ARGB;
      color=true;
      useCache=false;
      local=true;
      id="ALADIN/P/RGB";
      this.co=co;
      this.coRadius=coRadius;
      suite();
   }
   
   protected void suite() {
      PlanBG a = red!=null ? red : blue!=null ? blue : green;
      minOrder = a.minOrder;
      maxOrder = a.maxOrder;
      flagNoTarget=a.flagNoTarget;
      objet = a.objet;
      frameOrigin=a.frameOrigin;
      specificProj = a.specificProj;
      setNewProjD( a.projd.copy() );
      tileOrder=a.tileOrder;
      setDefaultZoom(co,coRadius);
      suiteSpecific();
      launchLoading();
   }
   
   protected void suiteSpecific() {
      active=selected=true;
      isOldPlan=false;
      pixList = new Hashtable<>(1000);
      RGBControl = new int[RGBCONTROL.length];
      for( int i=0; i<RGBCONTROL.length; i++) RGBControl[i] = RGBCONTROL[i];
      aladin.endMsg();
      resetStats();
   }
   
   /** Mise à jour des paramètres globaux du plan à partir du chargement des tuiles */
   protected void initTileParam(int width,int height ) {
      this.naxis1=this.width=width;
      this.naxis2=this.height=height;
   }
   
   protected boolean Free() {
      if( red!=null )   red.addRecutListener(null);
      if( green!=null ) green.addRecutListener(null);
      if( blue!=null )  blue.addRecutListener(null);
      return super.Free();
   }
   
   protected boolean isLoading() { return false; }
   
   /** Demande de chargement du losange repéré par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {
      HealpixKey pixAsk = new HealpixKeyRgb(this,order,npix);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }
   
   public boolean recut() {
      tabRgb.updateTables();
      updatePreview();
      return true;
   }
      
   /** Regénération complète des tuiles */
   public void updatePreview() {
      FreePixList();
      changeImgID();
      aladin.view.repaintAll();
   }
   
   protected int getMinOrder() { return minOrder==-1 ? 0 : minOrder; }
   
   synchronized protected void drawLosanges(Graphics g,ViewSimple v, boolean now) { drawLosangesNow(g,v); }

   /** Tracé des losanges à la résolution adéquate dans la vue
    * mais en mode synchrone */
   protected void drawLosangesNow(Graphics g,ViewSimple v) {
      int order = Math.max(getMinOrder(), Math.min(maxOrder(v),maxOrder) );

      long [] pix;
      if( v.isAllSky() ) {
         pix = new long[12*(int)CDSHealpix.pow2(order)*(int)CDSHealpix.pow2(order)];
         for( int i=0; i<pix.length; i++ ) pix[i]=i;
      } else pix = getPixList(v,getCooCentre(v),order); // via query_disc()

      for( int i=0; i<pix.length; i++ ) {
         if( isOutMoc(order, pix[i]) ) continue;
         if( (new HealpixKey(this,order,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) continue;
         HealpixKey k = getHealpix(order,pix[i], true);

         try { 
            k.resetTimer();
            k.loadNow();
            k.draw(g,v);
//            System.out.println("drawLosangesNow("+k.order+"/"+k.npix+")");
         } catch( Exception e ) { e.printStackTrace(); }

      }
   }

   /** Création du Moc associé au survey */
   protected void planReadyMoc() {
      try {
         Moc m = new SpaceMoc();
         if( red!=null && red.moc!=null ) m = m.union( red.moc );
         if( green!=null && green.moc!=null ) m = m.union( green.moc );
         if( blue!=null && blue.moc!=null ) m = m.union( blue.moc );
         moc = new HealpixMoc( m );
      } catch( Exception e ) { e.printStackTrace(); }
   }
}

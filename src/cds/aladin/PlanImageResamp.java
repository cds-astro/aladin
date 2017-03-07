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

import cds.tools.Util;

/**
 * Gestion d'un Plan d'image ré-échantillonné
 * @author Pierre Fernique [CDS]
 * @version 1.0 : décembre 2004 - Création
 */
public class PlanImageResamp extends PlanImage {
   static final protected int PPV = 0;  // Plus proche voisin
   static final protected int BILINEAIRE = 1; // Méthode bilinéaire
   

   // Gestion du Resampling
   private byte pixelsOriginInit[]=null;// Les pixels originaux initiaux
   private byte pixelsInit[]=null;		// Les pixels 8bits initiaux
   private int widthInit,heightInit;    // La taille de l'image initiale
   private PlanImage prefResample=null; // Le plan Image servant au resampling
   
   // Mode de calcul
   protected boolean fullPixel=false;     // Mode Pixel (8 ou full) du resampling
   protected int methode;                 // Méthode du resampling
   
   protected PlanImageResamp(Aladin aladin,PlanImage p) { 
      super(aladin,p);
      type=IMAGERSP;
   }
   
   // Pour pouvoir recharger du AJ
   protected PlanImageResamp(Aladin aladin) { 
      super(aladin);
      type=IMAGERSP;
   }
   
   /** Retourne true si l'image a été resamplée */
   protected boolean isResample() { return pixelsInit!=null; }
   
   
   /** J'overide cette méthode pour ne pas créer automatiquement une nouvelle vue */
    protected void planReady(boolean ready) {
       if( !ready ) { super.planReady(ready); return; }
       
       setActivated(true);
       setPourcent(-1);
       flagOk = true;
       
       aladin.calque.repaintAll();
    }
    
    
   /** Lance le resampling de l'image en fonction de la solution
    * astrométrique du plan pref passé en paramètre. Si celui
    * est null ou est égale à this, il y a restitution de la
    * solution initiale (sans threading)
    * @param pref Le plan dont l'image servira de solution astrométrique
    * @param methode PPV,BILINEAIRE
    * @param flagFullPixel true si on doit traiter également les pixels d'origine
    */
   protected void launchResampleBy(PlanImage pref,int methode,boolean flagFullPixel) {
      fullPixel=flagFullPixel;
      this.methode=methode;
      
      // Chargement et arrêt du cache des pixels d'origine
      if( fullPixel ) {
         pixelsOriginFromCache();
         cacheAvailable(false);
         noCacheFromOriginalFile();
         
      // Abandon des pixels d'origine
      } else noOriginalPixels();
      
      if( pref==null || pref==this ) {
         if( isResample() ) {
            setBufPixels8(pixelsInit);
            if( fullPixel ) pixelsOrigin=pixelsOriginInit;
            naxis1=width=widthInit;
            naxis2=height=heightInit;
            projd=projInit;
            changeImgID();
            aladin.view.repaintAll();
          }
         return;
      }
      prefResample = pref;
      flagOk=false;
      flagProcessing=true;
      aladin.calque.select.repaint();
     
      sendLog("Resample"," [" + this + " with "+pref
            +(methode==BILINEAIRE?"/bilinear":"")
            +(fullPixel?"/fullPix":"")
            +"]");
      
      synchronized( this ) {
         runme = new Thread(this,"AladinBuildResamp");
         Util.decreasePriority(Thread.currentThread(), runme);
//         runme.setPriority( Thread.NORM_PRIORITY -1);
         runme.start();
      }
   }
   
   protected boolean Free() {
      pixelsInit=null;
      pixelsOriginInit=null;
      return super.Free();
   }
   
   /** Calcul de resampling basée sur prefResample. Cette méthode
    * ne doit jamais être appelé directement mais via la méthode
    * launchResampleBy()
    * @return
    */
   protected boolean resample() {

      PlanImage pref=prefResample;

      Aladin.trace(3,"Resampling " + this + " with " + pref + " method="
            + methode);
      Coord coo=new Coord();
      int x=0, y=0;
      int w=pref.width;
      int i;

      // Mémorisation ou restitution préalable si nécessaire
      if( isResample() ) {
         setBufPixels8(pixelsInit);
         if( fullPixel ) pixelsOrigin=pixelsOriginInit;
      } else {
         pixelsInit=getBufPixels8();
         if( fullPixel ) pixelsOriginInit=pixelsOrigin;
         widthInit=width;
         heightInit=height;
      }

      boolean hasOrig=pixelsOriginInit != null;

      if( !pref.projd.c.TheSame(projInit.c) ) {
         setBufPixels8(new byte[pref.getBufPixels8().length]);
         if( hasOrig ) pixelsOrigin=new byte[pref.getBufPixels8().length * npix];
         for( i=0; i < pref.getBufPixels8().length; i++ ) {
            coo.x=i % w;
            coo.y=i / w;
            pref.projd.getCoord(coo);
            if( Double.isNaN(coo.al) ) continue;
            projInit.getXY(coo);
            if( !Double.isNaN(coo.x) ) {
               switch( methode ) {
               case PPV:
                  x=(int) Math.round(coo.x);
                  y=(int) Math.round(coo.y);
                  if( x<0 || x>=widthInit || y<0 || y>=heightInit ) break;
                  getBufPixels8()[i]=pixelsInit[y * widthInit + x];
                  if( !hasOrig ) break;
                  
                  int pos1=(heightInit - y-1) * widthInit + x;
                  int pos2=getBufPixels8().length - pref.width
                              * ((i / pref.width) + 1) + i % pref.width;
                  copyPixVal(pixelsOriginInit,pos1,pixelsOrigin,pos2);
                  break;
                  
               case BILINEAIRE:
                  int x1=(int)Math.round(coo.x - 0.5);
                  int y1=(int)Math.round(coo.y - 0.5);
                  int x2=x1 + 1;
                  int y2=y1 + 1;
                  if( x1<0 || x2>=widthInit || y1<0 || y2>=heightInit ) break;                  
                  double p0=(0xFF & pixelsInit[y1 * widthInit+ x1]);
                  double p1=(0xFF & pixelsInit[y1 * widthInit+ x2]);
                  double p2=(0xFF & pixelsInit[y2 * widthInit+ x1]);
                  double p3=(0xFF & pixelsInit[y2 * widthInit+ x2]);
                  
                  double d0,d1,d2,d3,pA,pB;
                  try {
                     d0 = 1./(coo.x-x1); 
                     try { d1 = 1./(x2-coo.x); } catch( Exception e ) { d0=0;d1=1; }
                  } catch( Exception e ) { d0=1;d1=0; }
                  pA = (p0*d0+p1*d1)/(d0+d1);
                  pB = (p2*d0+p3*d1)/(d0+d1);
                  try {
                     d2 = 1./(coo.y-y1);
                     try { d3 = 1./(y2-coo.y); } catch( Exception e ) { d2=0;d3=1; }
                  } catch( Exception e ) { d2=1;d3=0; }
                  getBufPixels8()[i]=(byte) (0xFF & (int) ((pA*d2+pB*d3)/(d2+d3))); 
                  if( !hasOrig ) break;
                  
                  p0=getPixVal(pixelsOriginInit,bitpix,(heightInit-y1-1)*widthInit+x1);
                  p1=getPixVal(pixelsOriginInit,bitpix,(heightInit-y1-1)*widthInit+x2);
                  p2=getPixVal(pixelsOriginInit,bitpix,(heightInit-y2-1)*widthInit+x1);
                  p3=getPixVal(pixelsOriginInit,bitpix,(heightInit-y2-1)*widthInit+x2);
                  pA = (p0*d0+p1*d1)/(d0+d1);
                  pB = (p2*d0+p3*d1)/(d0+d1);
                  setPixVal(pixelsOrigin,bitpix,getBufPixels8().length - pref.width
                           * ((i / pref.width) + 1) + i % pref.width,
                           (pA*d2+pB*d3)/(d2+d3));
                  break;
               }
            }

            // Pour laisser la main aux autres threads
            // et pouvoir afficher le changement de pourcentage
            if( i % 10000 == 0 ) {
               setPourcent(i * 100 / pref.getBufPixels8().length);
            }
         }
      }
      naxis1=width=pref.width;
      naxis2=height=pref.height;
      projInit=projd.copy();
      projd=pref.projd.copy();
      
      setHasSpecificCalib();
      setPourcent(-1);

      Aladin.trace(3,"Resampling achieved...");
      flagProcessing=false;
      calculPixelsZoom();
      changeImgID();
      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();
      return true;
   }

}

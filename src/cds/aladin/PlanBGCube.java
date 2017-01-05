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

import cds.allsky.Constante;
import cds.tools.Util;

public class PlanBGCube extends PlanBG {

   public int depth;            // Profondeur du dube (-1 si inconnue)
   protected double z=0;           // Frame courante
   protected int zInit;        // Premier frame à afficher
   protected boolean pause;    // true si on est en pause

   protected PlanBGCube(Aladin aladin) {
      super(aladin);
   }

   protected PlanBGCube(Aladin aladin, TreeObjHips gluSky,String label, Coord c, double radius,String startingTaskId) {
      super(aladin,gluSky,label, c,radius,startingTaskId);
      aladin.log(Plan.Tp[type],label);
   }

   protected void setSpecificParams(TreeObjHips gluSky) {
      super.setSpecificParams(gluSky);
      //      type = ALLSKYCUBE;    // POUR LE MOMENT AFIN D'EVITER D'AVOIR A FAIRE LE DOUBLE TEST PARTOUT
      depth = gluSky.cubeDepth;
      z = zInit = gluSky.cubeFirstFrame;
      pause = true;
      scanCubeProperties();
   }

   protected String getFrameLabel(int i) {
      if( !active ) return label;
      if( fromCanal ) return getCanalValue(i);
      String s = prop.getProperty(Constante.OLD_OBS_COLLECTION+"_"+i);
      if( s==null ) s = prop.getProperty(Constante.KEY_OBS_COLLECTION+"_"+i);
      return s!=null ? s : label;
   }

   private double crval3,crpix3,cdelt3;
   private String bunit3;
   protected boolean fromCanal;
   private int precision = -1;

   /** Retourne la valeur physique correspondant au numéro du canal */
   protected String getCanalValue(int n) {
      if( precision==-1 ) {
         double f = Math.abs(cdelt3);
         precision = f<0.001 ? 3 : f<0.01 ? 2 : f<100 ? 1 : 0;
      }
      //      return Util.myRound( ""+(n-crpix3)*cdelt3+crval3,precision);
      return ""+(int)Math.round(1000 * ((n+1-crpix3)*cdelt3+crval3))/1000.+(bunit3!=null?" "+bunit3:"");
   }

   protected boolean Free() {
      stopLoadingImmediately();
      return super.Free();
   }

   protected boolean scanCubeProperties() {
      try {
         MyProperties prop = loadPropertieFile();
         if( prop==null ) throw new Exception();

         String s;
         s = prop.getProperty(Constante.KEY_CUBE_DEPTH);
         if( s==null ) s = prop.getProperty(Constante.OLD_CUBE_DEPTH);
         if( s!=null )  try { depth = Integer.parseInt(s); } catch( Exception e ) { Aladin.trace(3,"PlanBGCube error on cubeDepth property ["+s+"]"); }

         s = prop.getProperty(Constante.KEY_CUBE_FIRSTFRAME);
         if( s==null ) s = prop.getProperty(Constante.OLD_CUBE_FIRSTFRAME);
         if( s!=null )  try { z = zInit = Integer.parseInt(s); } catch( Exception e ) { Aladin.trace(3,"PlanBGCube error on cubeFirstFrame property ["+s+"]"); }

         // Lecture de la valeur physique du canal ?
         try {
            s = prop.getProperty(Constante.KEY_CUBE_CRPIX3);
            crpix3 = Double.parseDouble(s);
            s = prop.getProperty(Constante.KEY_CUBE_CRVAL3);
            crval3 = Double.parseDouble(s);
            s = prop.getProperty(Constante.KEY_CUBE_CDELT3);
            cdelt3 = Double.parseDouble(s);
            fromCanal=true;
            bunit3 = prop.getProperty(Constante.KEY_CUBE_BUNIT3);
         } catch( Exception e ) { fromCanal=false; }

      } catch( Exception e ) { return false; }
      return true;
   }

   protected void paramByTreeNode(TreeObjHips gSky, Coord c, double radius) {
      super.paramByTreeNode(gSky,c,radius);
      depth=gSky.cubeDepth;
      z=gSky.cubeFirstFrame;
   }

   protected void activeCubePixels(ViewSimple v) {
      if( !setCubeFrame(v.cubeControl.lastFrame) ) return;
      v.cubeControl.startTime = System.currentTimeMillis();
      askForRepaint();
      //      v.oiz=-System.currentTimeMillis();   // pour forcer juste l'image de cette vue à ce regénérer
      //      aladin.view.repaintAll();
   }

   protected boolean setCubeFrame(double frameLevel) {
      if( z == frameLevel ) return false;
      z=frameLevel;
      return true;
   }

   /** Positionnement (a posteriori) de la profondeur du cube - utilisé lors de sa construction par HipsGen */
   public void setDepth(int depth) {
      this.depth = depth;
      int n[] = aladin.view.getNumView(this);
      if( n!=null ) for( int i : n) aladin.view.viewSimple[ n[i] ].cubeControl.nbFrame=depth;
   }

   /** Positionne le Frame par défaut (s'il s'agit d'un cube) */
   protected void setZ(double initFrame) { z=initFrame;  }

   /** retourne le Frame par défaut */
   protected double getZ() { return z; }

   /** Retourne le Frame courant, et si pas de vue attachée, le frame par défaut */
   protected double getZ(ViewSimple v) {
      if( v.pref==this ) {
         int f = v.cubeControl.getCurrentFrameIndex();
         if( f==-1 ) f = v.cubeControl.lastFrame=zInit;
         return f;
      }
      return z;
   }

   /** gestion de la pause pour le défilement d'un cube */
   protected void setPause(boolean t,ViewSimple v) {
      if( t==pause ) return;
      pause = t;
      if( !pause ) loadingImmediately(v);
   }
   protected boolean isPause() { return pause; }

   /** Retourne la profondeur dans le cas d'un cube */
   public int getDepth() { return depth==-1 ? 1: depth; }

   protected int getInitDelay() { return 500; }


   private double ox=-1,oy=-1;
   private double [] bit8 = new double [10000];

   /** Retourne le Pixel x,y de la frame n ATTENTION, SANS DOUTE LENT */
   protected byte getPixel8bit(int z,double x,double y) {
      ViewSimple v = aladin.view.getCurrentView();
      PointD p = v.getPosition(x, y);

      // Réinitialisation du cache
      if( p.x!=ox || p.y!=oy ) {
         for( int i=0;i<bit8.length; i++ ) bit8[i]=Double.NaN;
         ox=p.x; oy=p.y;
         //         System.out.print("Reinit pix8[]");
      }

      //      System.out.print(" z="+z+" ("+x+","+y+") => ("+p.x+","+p.y+") => ");

      double pix;
      if( !Double.isNaN( bit8[z] ) ) {
         pix = bit8[z];
         System.out.println(pix+" (from cache)");
      }
      else {
         pix = bit8[z] = getOnePixelFromCache(projd, p.x,p.y,-1,z,HealpixKey.PIX8);
         //         System.out.println(pix);
      }
      if( Double.isNaN(pix) ) pix=0;
      return (byte)( (int)pix & 0xFF);
   }

   /** Construction de la clé de Hashtable pour ce losange */
   protected String key(HealpixKey h) { return key(h.order,h.npix,h.z); }

   /** Construction d'une clé pour les Hasptable */
   protected String key(int order, long npix) { return key(order,npix,(int)z); }

   /** Demande de chargement du losange repéré par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {

      // Si je suis en pause, je charge les losanges en asynchrone
      if( isPause() ) return super.askForHealpix(order, npix);

      // sinon je charge immédiatement tous ceux qu'il faut depuis le cache
      readyAfterDraw=false;
      HealpixKey pixAsk = new HealpixKey(this,order,npix,HealpixKey.SYNCONLYIFLOCAL);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }

   private Hashtable<String,Integer> previousWorkingFrame = new Hashtable<String,Integer>();

   /** Retourne la précédédente tranche qui a marchée, null sinon */
   protected HealpixKey getHealpixPreviousFrame(int order, long npix) {
      String key = super.key(order,npix);
      Integer z2 = previousWorkingFrame.get( key );
      if( z2==null ) return null;
      int z1 = z2.intValue();
      //      if( Math.abs(z1-z)>5 ) return null;
      HealpixKey h =  pixList.get( key(order,npix, z1) );
      if( h==null || h.getStatus()!=HealpixKey.READY ) { previousWorkingFrame.remove(key); return null; }
      //      System.out.println("Je réutilise "+key(order,npix,z1));
      return h;
   }

   /** Mémorise la profondeur de la dernière frame qui a marchée */
   protected void setHealpixPreviousFrame(int order,long npix) {
      int z = (int)getZ();
      String key = super.key(order,npix);
      previousWorkingFrame.put( key, new Integer(z) );
   }

   private LoadingImmediatelyThread loadingThread = null;

   protected void loadingImmediately(ViewSimple v) {
      loadingThread = new LoadingImmediatelyThread(this, v, (int)getZ()+1);
      loadingThread.start();
   }

   protected void stopLoadingImmediately() {
      if( loadingThread!=null ) loadingThread.abort();
   }

   protected int getCurrentFrameReady() {
      if( isPause() || loadingThread==null ) return -1;
      return loadingThread.getZ();
   }

   class LoadingImmediatelyThread extends Thread {
      private int initZ,z1=0;
      private boolean encore;
      private PlanBGCube plan;
      private ViewSimple v;


      LoadingImmediatelyThread(PlanBGCube plan,ViewSimple v,int z) { this.plan=plan; this.v=v; this.z1=z; }

      void abort() { encore=false; }

      int getZ() { return z1-1; }

      int initOrder;
      Coord center;

      private boolean onZone() {
         //         int o = Math.max(ALLSKYORDER, Math.min(maxOrder(v),maxOrder) );
         int o = Math.min(maxOrder(v),maxOrder);
         if( o!=initOrder ) return false;
         if( o<ALLSKYORDER || v.isAllSky() ) return true;
         Coord c = getCooCentre(v);
         if( c.al!=center.al || c.del!=center.del ) return false;
         return true;
      }

      public void run() {
         encore=true;

         //         System.out.println("loadingImmediatelyThread starts");

         while( encore ) {

            initZ = z1 = (int)plan.getZ(v)+1;
            center = getCooCentre(v);

            int order = Math.min(maxOrder(v),maxOrder);
            //            int order = Math.max(ALLSKYORDER, Math.min(maxOrder(v),maxOrder) );
            initOrder=order;

            //            System.out.println("loading immediatelyThread loop (z="+z1+" order="+initOrder+" center="+center+")");

            long [] pix = null;
            boolean lowResolution = v.isAllSky() || order<=ALLSKYORDER;

            if( !lowResolution ) {
               pix = getPixList(v,center,order);
               for( int j=0; j<pix.length; j++ ) {
                  if( moc!=null && !moc.isIntersecting(order, pix[j]) ) pix[j]=-1;
                  else if( (new HealpixKey(plan,order,pix[j],HealpixKey.NOLOAD)).isOutView(v) ) pix[j]=-1;
               }
            }

            //            System.out.print("Thread loadImmediately:");
            //            if( pix==null ) System.out.println(" allsky");
            //            else {
            //               for( int i=0; i<pix.length; i++ ) if( pix[i]!=-1 ) System.out.print(" "+pix[i]);
            //               System.out.println();
            //            }

            for( int i=0; i<depth && encore; i++, z1++ ) {
               if( isPause() ) { encore=false; break; }
               if( z1>=depth) z1=0;

               //               System.out.println("Delta="+(z1-plan.getZ()));

               // Suis-je toujours sur zone ?
               if( !onZone() ) {
                  //                  System.out.println("Restart loop immediately");
                  break;
               }

               // Je charge les allsky ?
               if( pix==null ) {
                  HealpixKey h = new HealpixAllsky(plan, ALLSKYORDER, z1, HealpixKey.SYNC);
                  //                  System.out.println("load allsky: "+h);

                  // Ou je charge les losanges individuels ?
               } else {
                  for( int j=0; j<pix.length; j++ ) {
                     if( pix[j]==-1 ) continue;
                     String key = key(order,pix[j],z1);
                     if( pixList.get(key)!=null ) {
                        //                        System.out.println("ready: "+pixList.get(key));
                        continue;
                     }
                     HealpixKey h = new HealpixKey(plan,order,pix[j],z1,HealpixKey.SYNC);
                     pixList.put( key, h);
                     //                     System.out.println("loaded: "+h);
                  }
               }

               if( ox!=-1 ) {
                  bit8[z1] = getOnePixelFromCache(projd, ox,oy,-1,z1,HealpixKey.PIX8);
                  //                  System.out.println("Memorize z1="+z1+" => "+bit8[z1]);
               }

            }

            // Je me mets en attente tant que le défilement continue
            while( encore && onZone() ) {
               z1=0;
               if( isPause() ) { encore=false; break; }
               //               System.out.println("Thread load immediately waiting new zone...");
               Util.pause(1000);
            }
         }

         //         System.out.println("loadingImmediatelyThread ends");
      }
   }


}

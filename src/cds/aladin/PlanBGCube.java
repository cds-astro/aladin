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

public class PlanBGCube extends PlanBG {
   
   protected int depth;            // Profondeur du dube (-1 si inconnue)
   protected double z=0;           // Frame courante
   protected boolean pause;    // true si on est en pause

   protected PlanBGCube(Aladin aladin) {
      super(aladin);
   }

   protected PlanBGCube(Aladin aladin, TreeNodeAllsky gluSky,String label, Coord c, double radius,String startingTaskId) {
      super(aladin,gluSky,label, c,radius,startingTaskId);
      aladin.log(Plan.Tp[type],label);
   }
   
   protected void setSpecificParams(TreeNodeAllsky gluSky) {
      super.setSpecificParams(gluSky);
//      type = ALLSKYCUBE;    // POUR LE MOMENT AFIN D'EVITER D'AVOIR A FAIRE LE DOUBLE TEST PARTOUT
      depth = gluSky.cubeDepth;
      z = gluSky.cubeFirstFrame;
      pause = true;
      scanCubeProperties();
   }

   protected boolean scanCubeProperties() {
      try {
         java.util.Properties prop = loadPropertieFile();
         if( prop==null ) throw new Exception();
         
         String s;
         s = prop.getProperty(PlanHealpix.KEY_CUBEDEPTH); 
         if( s!=null )  try { depth = Integer.parseInt(s); } catch( Exception e ) { Aladin.trace(3,"PlanBGCube error on cubeDepth property ["+s+"]"); }
         
         s = prop.getProperty(PlanHealpix.KEY_CUBEFIRSTFRAME); 
         if( s!=null )  try { z = Integer.parseInt(s); } catch( Exception e ) { Aladin.trace(3,"PlanBGCube error on cubeFirstFrame property ["+s+"]"); }
         
      } catch( Exception e ) { return false; }
      return true;
   }
   
   protected void paramByTreeNode(TreeNodeAllsky gSky, Coord c, double radius) {
      super.paramByTreeNode(gSky,c,radius);
      depth=gSky.cubeDepth;
      z=gSky.cubeFirstFrame;
   }
   
   protected void activePixels(ViewSimple v) {
      int frame = v.cubeControl.lastFrame;
      if( !setCubeFrame(frame) ) return;
      changeImgID();
      askForRepaint();
   }
   
   protected boolean setCubeFrame(double frameLevel) {
      if( z == frameLevel ) return false;
      z=frameLevel;
      allsky=null;
      return true;
   }
   
   /** Positionne le Frame initial (s'il s'agit d'un cube) */
   protected void setZ(double initFrame) { setCubeFrame(initFrame); }
   
   /** retourne le Frame initial */
   protected double getZ() { return z; }
   
   /** gestion de la pause pour le défilement d'un cube */
   protected void setPause(boolean t) { pause = t; }
   protected boolean isPause() { return pause; }
   
   /** Retourne la profondeur dans le cas d'un cube */
   protected int getDepth() { return depth==-1 ? 1: depth; }
   
   protected int getInitDelay() { return 1000; }
   
   /** Construction de la clé de Hashtable pour ce losange */
   protected String key(HealpixKey h) { return key(h.order,h.npix,h.z); }

   /** Construction d'une clé pour les Hasptable */
   protected String key(int order, long npix) { return key(order,npix,(int)z); }
   protected String key(int order, long npix,int z) { return order+"."+npix+(z<=0?"":"_"+z); }

   private Hashtable<String,Integer> previousWorkingFrame = new Hashtable<String,Integer>();
   
   /** Retourne la précédédente tranche qui a marchée, null sinon */
   protected HealpixKey getHealpixPreviousFrame(int order, long npix) {
      String key = super.key(order,npix);
      Integer z2 = previousWorkingFrame.get( key );
      if( z2==null ) return null;
      int z1 = z2.intValue();
      if( Math.abs(z1-z)>1 ) return null;
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
   


    
}

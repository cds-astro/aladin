// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import cds.moc.HealpixMoc;
import cds.tools.pixtools.CDSHealpix;


/**
 * Plan dédié à la manipulation d'un MEF Multi-CCD
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : mai 2016 - création
 */
public class PlanMultiCCD extends PlanImage {
   
   PlanImage [] ccd;
   int ref;
   String labelBase;
   
   protected PlanMultiCCD(Aladin aladin, String label, Vector<Plan> v) {
      super(aladin);
      labelBase=label;
      setCCD(v);
      setRef(0);
   }
   
   protected void setLabel(String label) { labelBase = label; }
   
   protected void setCCD(Vector<Plan> v) {
      
      
      HashSet<String> extName = new HashSet<>(v.size());   // pour vérifier que EXTNAME contient des noms différents
      
      ccd = new PlanImage[ v.size() ];
      Enumeration<Plan> e = v.elements();
      for( int i=0;  e.hasMoreElements(); i++ ) {
         PlanImage p = (PlanImage)e.nextElement();
         ccd[i] = p;
         p.planMultiCCD=this;
         
         if( extName!=null ) {
            String a = p.headerFits.getStringFromHeader("EXTNAME");
            if( a==null || extName.contains(a) ) extName=null;
            if( extName!=null ) extName.add(a);
         }
      }
      
      // Positionnement de noms particuliers pour chaque CCD (utilisation de EXTNAME)
      if( extName!=null ) {
         for( PlanImage p : ccd ) p.setLabel( labelBase+" - "+p.headerFits.getStringFromHeader("EXTNAME") );
      }
      
   }
   
   protected Vector<PlanImage> getCCD() {
      Vector<PlanImage> v = new Vector<>(ccd.length);
      for( PlanImage p : ccd ) v.add(p);
      return v;
   }
   
   protected int getSize() { return ccd.length; }
   
   protected PlanImage getSelectedCCD() { return ccd[ref]; }
   
   protected void setRef(int ref) {
      this.ref = ref;
      ccd[ref].copy(this,true);
      setActivated(true);
      selected=true;
      if( aladin.frameCM!=null ) aladin.frameCM.majCM();
   }
   
   protected boolean setRef(Coord coo) {
      int i = getRef(coo);
      if( i==ref || i==-1) return false;
      setRef(i);
      return true;
   }
   
   private int getRef(Coord coo) {
      for( int i=0; i<ccd.length; i++ ) {
         if( ccd[i].contains(coo) ) return i;
      }
      return -1;
   }
   
   protected void setCM(Object cm) {
      super.setCM(cm);
      ccd[ref].setCM(cm);
   }
   
   public void setOpacityLevel(float f) {
      super.setOpacityLevel(f);
      for( PlanImage p : ccd ) p.setOpacityLevel(f);
   }
   
   
   protected PointD[] getBords(ViewSimple v) {
      PointD [] coins = null;
      for( PlanImage p : ccd ) {
         PointD [] c = p.getBords(v);   //    O     3
         if( coins==null ) coins=c;     //
         else {                         //    1     2
            if( c[0].x<coins[0].x ) coins[0].x=c[0].x;
            if( c[0].y<coins[0].y ) coins[0].y=c[0].y;
            
            if( c[3].x>coins[3].x ) coins[3].x=c[3].x;
            if( c[3].y<coins[3].y ) coins[3].y=c[3].y;
            
            if( c[2].x>coins[2].x ) coins[2].x=c[2].x;
            if( c[2].y>coins[2].y ) coins[2].y=c[2].y;
            
            if( c[1].x<coins[1].x ) coins[1].x=c[1].x;
            if( c[1].y>coins[1].y ) coins[1].y=c[1].y;
         }
      }
      return coins;
   }

   protected void setBufPixels8(byte [] pixels) {
      super.setBufPixels8(pixels);
      ccd[ref].setBufPixels8(pixels);
   }
   
   protected boolean Free() {
      boolean rep = true;
      for( PlanImage p : ccd ) rep &= p.Free();
      return rep;
   }
   
//   protected boolean isRefForVisibleView() { return false; }
   
   protected void draw(Graphics g,ViewSimple v,int dx, int dy,float op) {
      for( PlanImage p : ccd ) p.draw(g,v,dx,dy,op);
   }
   
   /**
    * Pour déterminer si les plans passés en paramètre forme un quadrillage de CCD
    * je calcule leur MOC respectif et je vérifie qu'aucun n'intersecte le reste de plus
    * d'un vingtième de la surface du premier plan.
    */
   static protected boolean isMultiCCD(Vector<Plan> v) {
      long marge=0;
      int order=10;
      try {
         if( v.size()<2 ) return false;
         
         HealpixMoc moc = null;
         Enumeration<Plan> e = v.elements();
         while( e.hasMoreElements() ) {
            Plan p = e.nextElement();
            if( !p.isImage() || !Projection.isOk(p.projd)) return false;
            
            
            // Détermination d'un ordre de MOC approprié en fonction
            // de la surface angulaire du premier CCD (pour avoir au-moins 75 cellules)
            if( moc==null ) {
               int w = Math.max( ((PlanImage)p).naxis1,((PlanImage)p).naxis2);
               double res[] = p.projd.c.GetResol();
               double size = Math.max(res[0],res[1])*w;
               order = Aladin.getAppropriateOrder(size);
            }
            
            HealpixMoc m = buildMoc(p.projd,order);
            if( moc==null ) { 
               moc=m;
               marge = (long)( moc.getUsedArea()*0.2 );
            }
            else {
               HealpixMoc inter = moc.intersection(m);
               if( inter.getUsedArea() > marge ) return false;
               moc = moc.union(m);
            }
         }
         return true;
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return false;
   }
   
   static private HealpixMoc buildMoc(Projection proj, int order) throws Exception {
      HealpixMoc moc = new HealpixMoc(0,order);
      Coord coo = new Coord();
      ArrayList<double[]> cooList = new ArrayList<>(10);
      Dimension dim = proj.c.getImgSize();
      for( int i=0; i<4; i++ ) {
         coo.x = (i==0 || i==3 ? 0 :dim.width);
         coo.y = (i<2 ? 0 : dim.height);
         proj.c.GetCoord(coo);
         cooList.add(new double[]{coo.al,coo.del});
      }
      long [] npixs = CDSHealpix.query_polygon( order, cooList, true);
      for( long npix : npixs ) moc.add(order,npix) ;
      return moc;
   }
   

   

   
}   

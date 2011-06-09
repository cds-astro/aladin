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

package cds.tools.pixtools;

import healpix.core.HealpixIndex;
import healpix.core.base.set.LongRangeSet;
import healpix.tools.SpatialVector;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.asterope.healpix.PixToolsVector3d;

import cds.aladin.Aladin;

/** Wrapper Healpix CDS pour pouvoir passer facilement d'une librairie Healpix à une autre */
public final class CDSHealpix {
   
   static final String [] MODE = {
      "CDSBestof (no debug)",
      "Will'O Mulan [GAIA] (debug mode)",
      "Kuropatkin (JPL) + CDS",
      "Kotek (based on Kuropatkin)"
   
   };
   
   static public final int BESTOF     = 0;
   static public final int WILL       = 1;
   static public final int KUROPATKIN = 2;
   static public final int KOTEK      = 3;
   
   static public final int HEALPIXMAXORDER[] = { 28, 28, 20, 20 };
   
   static public final int MAXMODE=3;
   
   static final String MODEEXCEPTION = "This method is not supported in the current HEALPix library";

   static private int mode = BESTOF;
   
   /** Positionne la librairie CDSHealpix à utiliser */
   static public void setMode(int mode) {
      if( mode==CDSHealpix.mode ) return;
      CDSHealpix.mode=mode;
      System.out.println("Current HEALPix: "+MODE[mode]+" library");
   }
   
   /** Retourne le mode CDSHealpix courant */
   static public int getMode() { return mode; }
   
   /** Pour du debug - changement cyclique de mode */
   static public String switchMode() {
      mode++;
      if( mode>MAXMODE ) mode=0;
      return MODE[mode];
   }
   
   /** Retourne l'ordre maximum supporté par la librairie courante */
   static public int getMaxOrder() { return HEALPIXMAXORDER[mode]; }

   /** Voir Healpix documentation */
   static public double[] pix2ang_nest(long nside,long ipix) throws Exception {
      switch(mode) {
         case BESTOF:
//         case WILL: synchronized(lockWill) { initWillMode(nside); return hWill.pix2ang_nest(ipix); }
         case WILL: return hWill[ initWillMode(nside) ].pix2ang_nest(ipix);
         case KUROPATKIN: return PixTools.pix2ang_nest(nside, ipix);
         case KOTEK: return pixtoolsNestedKotek.pix2ang_nest(nside, ipix);
         default: throw new Exception(MODEEXCEPTION);
      }
   }
   
   /** Voir Healpix documentation */
   static public double[] pix2ang_ring(long nside,long ipix) throws Exception {
      switch(mode) {
         case BESTOF:
//         case WILL: synchronized( lockWill ) { initWillMode(nside); return hWill.pix2ang_ring(ipix); }
         case WILL: return hWill[ initWillMode(nside) ].pix2ang_ring(ipix);
         case KUROPATKIN: return PixTools.pix2ang_ring(nside, ipix);
         case KOTEK: return pixtoolsKotek.pix2ang_ring(nside, ipix);
         default: throw new Exception(MODEEXCEPTION);
      }
   }
   
   /** Voir Healpix documentation */
   static public long ang2pix_nest(long nside,double theta, double phi) throws Exception {
      switch(mode) {
         case BESTOF:
//         case WILL: synchronized( lockWill ) { initWillMode(nside); return hWill.ang2pix_nest(theta, phi); }
         case WILL: return hWill[ initWillMode(nside) ].ang2pix_nest(theta, phi);
         case KUROPATKIN: return PixTools.ang2pix_nest(nside, theta, phi);
         case KOTEK: return pixtoolsNestedKotek.ang2pix_nest(nside, theta, phi);
         default: throw new Exception(MODEEXCEPTION);
      }
   }
   
   /** Voir Healpix documentation */
   static public long ang2pix_ring(long nside,double theta, double phi) throws Exception {
      switch(mode) {
         case BESTOF:
//         case WILL: synchronized( lockWill ) { initWillMode(nside); return hWill.ang2pix_ring(theta, phi); }
         case WILL: return hWill[ initWillMode(nside) ].ang2pix_ring(theta, phi);
         case KUROPATKIN: return PixTools.ang2pix_ring(nside, theta, phi);
         case KOTEK: return pixtoolsKotek.ang2pix_ring(nside, theta, phi);
         default: throw new Exception(MODEEXCEPTION);
      }
   }
   
   /** Voir Healpix documentation */
   static public long[] query_disc(long nside,double ra, double dec, double radius) throws Exception {
	   return query_disc(nside, ra, dec, radius, true);
   }
   static public long[] query_disc(long nside,double ra, double dec, double radius, boolean inclusive) throws Exception {
	      switch(mode) {
	         case BESTOF:
	         case WILL: return query_discWill(nside,ra,dec,radius, inclusive);
	         case KOTEK : return query_discKotek(nside,ra,dec,radius, inclusive);
	         case KUROPATKIN: return query_discCDS(nside,ra,dec,radius);
	         default: throw new Exception(MODEEXCEPTION);
	      }	   
   }
   /** Voir Healpix documentation */
   static public long[] query_polygon(long nside,ArrayList<double[]>list) throws Exception {
      switch(mode) {
         case BESTOF:
         case WILL: return query_polygonWill(nside,list);
         case KOTEK: return query_polygonKotek(nside,list);
         default: throw new Exception(MODEEXCEPTION);
      }
   }
   
   /** Voir Healpix documentation */
   static public double[][] corners_nest(long nside,long npix) throws Exception {
      switch(mode) {
         case WILL: return corners_nestWill(nside, npix);
         case BESTOF:
         default: return corners_nestCDS(nside, npix);
      }
   }
   
   /** Voir Healpix documentation */
   static public List<Long> neighbours_nest(long nside, long npix) throws Exception  {
      switch(mode) {
         default: return hWill[ initWillMode(nside) ].neighbours_nest(npix);
      }
   }
   
   /** Voir Healpix documentation */
   static public long nest2ring(long nside, long npix) throws Exception  {
      switch(mode) {
         case BESTOF:
//         case WILL: synchronized( lockWill ) { initWillMode(nside); return hWill.nest2ring(npix); }
         case WILL: return hWill[ initWillMode(nside) ].nest2ring(npix);
         case KUROPATKIN: return PixTools.nest2ring(nside, npix);
         case KOTEK: return pixtoolsNestedKotek.nest2ring(nside, npix);
         default: throw new Exception(MODEEXCEPTION);
      }
   }
   
   /** Voir Healpix documentation */
   static public double pixRes(long nside) {
      double res = 0.;
      double degrad = Math.toDegrees(1.0);
      double skyArea = 4.*Math.PI*degrad*degrad;
      double arcSecArea = skyArea*3600.*3600.;
      long npixels = 12*nside*nside;
      res = arcSecArea/npixels;
      res = Math.sqrt(res);
      return res;
   }
   
   /** Retourne la numérotation unique pour un pixel d'un nside donné */
   static long nsidepix2uniq(long nside, long npix) {
      return 4*nside*nside + npix;
   }
   
   /** Retourne le nside et le pixel pour un numéro uniq donné */
   static long [] uniq2nsidepix(long uniq) {
      return uniq2nsidepix(uniq,null);
   }
   
//   private static double LOG2 = Math.log(2);
   
//   /** Retourne le nside et le pixel pour un numéro uniq donné 
//    * en utilisant le tableau passé en paramètre s'il est différent de  null */
//   static long [] uniq2nsidepix0(long uniq,long [] nsidepix) {
//      if( nsidepix==null ) nsidepix = new long[2];
//      long order = (long)(Math.log(uniq/4)/LOG2)/2;
//      nsidepix[0] = (long)Math.pow(2,order);
//      nsidepix[1] = uniq - 4*nsidepix[0]*nsidepix[0];
//      return nsidepix;
//   }
   
   /** Retourne le nside et le pixel pour un numéro uniq donné 
    * en utilisant le tableau passé en paramètre s'il est différent de  null */
   static long [] uniq2nsidepix(long uniq,long [] nsidepix) {
      if( nsidepix==null ) nsidepix = new long[2];
      long order = log2(uniq/4)/2;
      nsidepix[0] = pow2(order);
      nsidepix[1] = uniq - 4*nsidepix[0]*nsidepix[0];
      return nsidepix;
   }
   
//   /** Retourne le nside et le pixel pour un numéro uniq donné
//    * en utilisant le tableau passé en paramètre s'il est différent de  null */
//   static long [] uniq2nsidepix1(long uniq,long [] nsidepix) {
//      if( nsidepix==null ) nsidepix = new long[2];
//      long p4 = uniq>>>2;
//      int order=0;
//      if (p4 > 0xFFFFFFFFL) { order=16; p4>>=32; }
//      if (p4 > 0x0000FFFF) { order|=8; p4>>=16; }
//      if (p4 > 0x000000FF) { order|=4; p4>>=8; }
//      if (p4 > 0x0000000F) { order|=2; p4>>=4; }
//      if (p4 > 0x00000003) { order|=1; }
//      nsidepix[0] = 1L<<order;
//      nsidepix[1] = uniq - 4*nsidepix[0]*nsidepix[0];
//      return nsidepix;
//   } 
   
   public static final long pow2(long order){ return 1<<order;}
   public static final long log2(long nside){ int i=0; while((nside>>>(++i))>0); return --i; }

   
//   public static void main(String[] args) {
//      try {
//         long onpix=0;
//         for( long order=0; order<=29; order++) {
//            long nside = pow2(order);
//            long npix = (12*nside*nside)-1;
//            if( npix<onpix ) System.out.println("Probleme");
//            onpix=npix;
//            long uniq = nsidepix2uniq(nside,npix);
//            System.out.println(order+") nside="+nside+" npix="+npix+" => uniq="+uniq);
//            long [] res0=null;
//            long [] res=null;
//            long [] res1=null;
//            long t0=System.nanoTime();
//            for( int j=0; j<10000; j++ ) {
//               res0 = uniq2nsidepix0(uniq,res0);
//            }
//            long t=System.nanoTime();
//            for( int j=0; j<10000; j++ ) {
//               res = uniq2nsidepix(uniq,res);
//            }
//            long t1=System.nanoTime();
//            for( int j=0; j<10000; j++ ) {
//               res1 = uniq2nsidepix1(uniq,null);
//            }
//            long t2=System.nanoTime();
//
//            boolean ok = res[0]==nside && res[1]==npix;
//            boolean ok1 = res[0]==res1[0] && res[1]==res1[1];
//            System.out.println("==> nside="+res0[0]+"/"+res[0]+"/"+res1[0] +" npix="+res0[1]+"/"+res[1]+"/"+res1[1]+" "+ok+" "+ok1+" "+(t-t0)/1000+"/"+(t1-t)/1000+"/"+(t2-t)/1000+"ns");
//         }
//      } catch( Exception e ) {
//         // TODO Auto-generated catch block
//         e.printStackTrace();
//      }
//   }

   /** Voir Healpix documentation */
   static public double[] radecToPolar(double[] radec) { return radecToPolar(radec,new double[2]); }
   static public double[] radecToPolar(double[] radec,double polar[]) {
      polar[0] = Math.PI/2. - radec[1]/180.*Math.PI;
      polar[1] = radec[0]/180.*Math.PI;
      return polar;
   }
   
   /** Voir Healpix documentation */
   static public double[] polarToRadec(double[] polar) { return polarToRadec(polar,new double[2]); }
   static public double[] polarToRadec(double[] polar,double radec[]) {
      radec[1] = (Math.PI/2. - polar[0])*180./Math.PI;
      radec[0] = polar[1]*180./Math.PI;
      return radec;
   }
   
//   static final private double LOG2 = Math.log(2);
//   static public long log2(long x) { return (long)(Math.log(x)/LOG2); }
   
   
   
   // ------------------------------- Particularités Kotek
   
   static private org.asterope.healpix.PixTools pixtoolsKotek = new org.asterope.healpix.PixTools();  // Objet Healpix mode Kotek
   static private org.asterope.healpix.PixToolsNested pixtoolsNestedKotek = new org.asterope.healpix.PixToolsNested();  // Objet Healpix mode Kotek
   
   static private long[] query_discKotek(long nside,double ra, double dec, double radius, boolean inclusive) throws Exception {
      PixToolsVector3d vector = createPixToolsVector3dKotek(ra,dec);
      org.asterope.healpix.LongRangeSet list = pixtoolsKotek.query_disc(nside, vector, radius, inclusive);
      if( list==null ) return new long[0];
      return ring2nest(nside,list.toArray());
   }
   
   static private long [] ring2nest(long nside,long [] a) {
      long[] b = new long[a.length];
      for( int i=0; i<a.length; i++ ) b[i] = PixTools.ring2nest(nside, a[i]);
      return b;
   }
   
   static private long[] query_polygonKotek(long nside,ArrayList<double[]> cooList) throws Exception {
      ArrayList<PixToolsVector3d> vlist = new ArrayList<PixToolsVector3d>(cooList.size());
      Iterator<double[]> it = cooList.iterator();
      while( it.hasNext() ) {
         double coo[] = it.next();
         vlist.add( createPixToolsVector3dKotek(coo[0], coo[1]) );
         
      }
      org.asterope.healpix.LongRangeSet list = pixtoolsKotek.query_polygon(nside, vlist, 1);
      if( list==null ) return new long[0];
      return ring2nest(nside,list.toArray());
   }      

   static private PixToolsVector3d createPixToolsVector3dKotek(double ra,double dec) {
      return pixtoolsKotek.Ang2Vec(Math.PI/2. -dec/180.*Math.PI,ra/180.*Math.PI);

//      double cd = Math.cos( Math.toRadians(dec) );
//      double x = Math.cos( Math.toRadians(ra)) * cd;
//      double y = Math.sin( Math.toRadians(ra)) * cd;
//      double z = Math.sin( Math.toRadians(dec) );
//      return  new PixToolsVector3d(x, y, z);
   }
   
   
   // ------------------------------- Particularités Will

//   static private long nsideWill = -1;  // NSIDE courant dans le mode WILL uniquement
   static private HealpixIndex hWill[] = new HealpixIndex[30];  // Objet Healpix dans le mode WILL uniquement
//   static private Object lockWill = new Object();
   
   // Initialisation de l'objet de manipulation Healpix (dans le mode WILL uniquement)
//   static private void initWillMode(long nside) throws Exception {
//      if( nsideWill==nside ) return;
//      hWill = new HealpixIndex((int)nside);
//      nsideWill=nside;
//   }
   static private int initWillMode(long nside) throws Exception {
      if( hWill==null ) hWill = new HealpixIndex[30];
      int order = (int)log2(nside);
      if( hWill[order]!=null ) return order;
      hWill[order] = new HealpixIndex((int)nside);
//      Aladin.aladin.trace(4,"CDSHealpix.initWillMode(2^"+order+")");
      return order;
   }
   
   static private long[] query_discWill(long nside,double ra, double dec, double radius, boolean inclusive) throws Exception {
//      synchronized( lockWill ) {
         SpatialVector vector = new SpatialVector(ra,dec);
         int order = initWillMode(nside);
//         LongRangeSet list = hWill.queryDisc(vector, radius, 1, inclusive?1:0);
         LongRangeSet list = hWill[order].queryDisc(vector, radius, 1, inclusive?1:0);
         if( list==null ) return new long[0];
         return list.toArray();
//      }
   }

   static private long[] query_polygonWill(long nside,ArrayList<double[]> cooList) throws Exception {
//      synchronized( lockWill ) {
         ArrayList vlist = new ArrayList(cooList.size());
         Iterator<double[]> it = cooList.iterator();
         while( it.hasNext() ) {
            double coo[] = it.next();
            vlist.add(new SpatialVector(coo[0], coo[1]));

         }
         int order = initWillMode(nside);
//         LongRangeSet list = hWill.query_polygon((int)nside, vlist, 1, 1);
         LongRangeSet list = hWill[order].query_polygon((int)nside, vlist, 1, 1);
         if( list==null ) return new long[0];
         return list.toArray();
//      }
   }      
   
   static final int [] A = { 3, 2, 0, 1 };
   static public double[][] corners_nestWill(long nside,long npix) throws Exception {
      int order = initWillMode(nside);
      SpatialVector [] v = hWill[order].corners_nest(npix, 1);
      double [][] corners = new double[v.length][2];
      for( int i=0; i<v.length; i++ ) {
         int j = A[i];
         corners[j][0] = v[i].ra();
         corners[j][1] = v[i].dec();
      }
      return corners;  
   }
   
//   public static void main(String[] args) {
//      try {
//         int nside = 8;
//         SpatialVector vector = new SpatialVector(219.92904166666668,85.88719444444445);
//         double radius = 3.91698480573189;
//
//         HealpixIndex hi = new HealpixIndex(nside);
//         LongRangeSet vlist = hi.queryDisc(vector, radius / 180 * Constants.PI, 1, 1);
//         long [] list = vlist==null ? new long[0] : vlist.toArray();
//
//         System.out.print("ra="+vector.ra()+ " dec="+vector.dec()+" radius="+radius+" Nside="+nside+" => npixlist:");
//         for( int i = 0; i < list.length; i++ ) {
//            System.out.print(" " + list[i]);
//         }
//         System.out.println();
//      } catch( Exception e ) {
//         e.printStackTrace();
//      }
//   }


   // ------------------------------- Particularités Kuropatkin + CDS
   
   static private long [] query_discCDS(long nside, double ra, double dec, double radius) {
      radius =  Math.toDegrees(radius) + pixRes(nside)/(3600*2);

      boolean poleN = false, poleS = false;

      // Détermination des cercles concernées
      // theta est inversement proportionnel à delta => on verse min/max
      double thetaMin = Math.PI/2 - Math.toRadians(dec+radius);
      double thetaMax = Math.PI/2 - Math.toRadians(dec-radius);

      // recadre les theta pour etre entre [0;PI]
      if (thetaMin < 0) {
         thetaMin = - thetaMin;
         poleN=true;
      }
      if (thetaMax > Math.PI) {
         thetaMax = 2*Math.PI - thetaMax;
         poleS=true;
      }

      // Détermination de phi, dphi
      double phi = Math.toRadians(ra);
      double dphi = Math.toRadians(radius);
      if ((dphi - Math.PI) > 0)
         dphi = Math.PI;

      long ringMin = PixTools.RingNum(nside, Math.cos(thetaMin));
      long ringMax = PixTools.RingNum(nside, Math.cos(thetaMax));

      Vector candidats = new Vector();

      // si on a un pole dans la vue => taille à PI
      if (poleN || poleS) {
         dphi = Math.PI;
         // Avant le pole
         if (poleS) {
            getNpixListCDS(phi, dphi, nside, ringMin, 4*nside-1, candidats);
            ringMin = 1;
         }
         // Après le pole
         if (poleN) {
            getNpixListCDS(phi, dphi, nside, 1, ringMax, candidats);
            ringMax=4*nside-1;
         }
      }
      else
         getNpixListCDS(phi, dphi, nside, ringMin, ringMax, candidats);

      // Passage sous forme d'un tableau de long[]
      long [] npix = new long[candidats.size()];
      Enumeration e = candidats.elements();
      for( int i=0; i<npix.length; i++ ) {
         npix[i] = ( (Long)e.nextElement()).longValue();
      }

      return npix;
   }

   private static void getNpixListCDS(double phi, double dphi, long nside, long ringMin, long ringMax, Vector candidats) {
      for( long ring=ringMin; ring<=ringMax; ring++ ) {
         if (phi-dphi<0) candidats.addAll( PixTools.InRing(nside,ring,phi+Math.PI*2,dphi,true));
         else candidats.addAll( PixTools.InRing(nside,ring,phi,dphi,true));
      }
   }
   
   /** Approximation des coordonnées RA,DEC des 4 angles du losange
    * Je recherche les coord (centrales) des losanges des 4 coins dans la résolution
    * Healpix maximale */
   private static double [][] corners_nestCDS(long nside, long pixid) {
      double [][] corners;
      try {
         int order = (int)log2(nside);
         corners = new double[4][2];
         int orderFile = getMaxOrder() - order; // Je ne dois pas dépasser la limite Healpix
//         long nSidePix = (long) Math.pow(2, orderFile);
         long nSidePix = pow2(orderFile);
         // Numéro des pixels des 4 coins
         long c0, c1, c2, c3;
         c0 = c1 = c2 = 0;
         c3 = (nSidePix * nSidePix) - 1;
         for( int i = 0; i < orderFile; i++ ) c1 = (c1 << 2) | 1;
         for( int i = 0; i < orderFile; i++ ) c2 = (c2 << 2) | 2;
         // Chaque pixel "interne" va être remplacé par nsidePix*nsidePix pixels
         // d'où l'offset suivant
         long offset = pixid * nSidePix * nSidePix;
         c0 += offset;
         c1 += offset;
         c2 += offset;
         c3 += offset;
//         long nSideFile = (long) Math.pow(2, order + orderFile);
         long nSideFile = (long) pow2(order + orderFile);
         polarToRadec(pix2ang_nest(nSideFile, c0) , corners[0]);
         polarToRadec(pix2ang_nest(nSideFile, c1) , corners[1]);
         polarToRadec(pix2ang_nest(nSideFile, c2) , corners[2]);
         polarToRadec(pix2ang_nest(nSideFile, c3) , corners[3]);
       } catch( Exception e ) { return null; }
      return corners;
   }

   


}

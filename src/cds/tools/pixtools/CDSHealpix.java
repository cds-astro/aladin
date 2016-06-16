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

import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import healpix.essentials.Vec3;

import java.util.ArrayList;

/** Wrapper Healpix CDS pour ne pas réinitialiser systématiquement l'objet HealpixBase pour chaque NSIDE
 * @author Pierre Fernique [CDS] with the help of Martin Reinecke
 */
public final class CDSHealpix {

   static final public int MAXORDER=29;

   static private HealpixBase hpxBase[] = new HealpixBase[MAXORDER+1];  // Objet HealpixBase pour chaque nside utilisé


   static public HealpixBase getHealpixBase(int order) throws Exception  {
      if( hpxBase[order]==null ) hpxBase[order] = new HealpixBase((int)pow2(order),Scheme.NESTED);
      return hpxBase[order];
   }

   static private int init(long nside) throws Exception {
      int order = (int)log2(nside);
      if( hpxBase[order]!=null ) return order;
      hpxBase[order] = new HealpixBase((int)nside,Scheme.NESTED);
      return order;
   }

   static public double[] pix2ang_nest(long nside,long ipix) throws Exception {
      Pointing res = hpxBase[ init(nside) ].pix2ang(ipix);
      return new double[]{ res.theta, res.phi };
   }

   static public long ang2pix_nest(long nside,double theta, double phi) throws Exception {
      return hpxBase[ init(nside) ].ang2pix(new Pointing(theta,phi));
   }

   static public long[] query_disc(long nside,double ra, double dec, double radius) throws Exception {
      return query_disc(nside, ra, dec, radius, true);
   }

   static public long[] query_disc(long nside,double ra, double dec, double radius, boolean inclusive) throws Exception {
      int order = init(nside);
      RangeSet list = inclusive ? hpxBase[order].queryDiscInclusive(pointing(ra,dec),radius,4)
            : hpxBase[order].queryDisc(pointing(ra,dec),radius);
      if( list==null ) return new long[0];
      return list.toArray();
   }

   //   static public long[] query_disc(long nside,double ra, double dec, double radius, boolean inclusive) throws Exception {
   //      SpatialVector vector = new SpatialVector(ra,dec);
   //      int order = init(nside);
   //      LongRangeSet list = hpxBase[order].queryDisc(new Pointing(vector),radius,inclusive);
   //      if( list==null ) return new long[0];
   //      return list.toArray();
   //   }

   static public long[] query_polygon(long nside,ArrayList<double[]>cooList) throws Exception {
      int order = init(nside);
      Pointing[] vertex = new Pointing[cooList.size()];
      int i=0;
      for(double [] coo : cooList) vertex[i++]=pointing(coo[0], coo[1]);
      RangeSet list = hpxBase[order].queryPolygonInclusive(vertex,4);
      if( list==null ) return new long[0];
      return list.toArray();
   }

   //   static public long[] query_region(long nside,ArrayList<double[]>cooList) throws Exception {
   //      int order = init(nside);
   //      ArrayList<Pointing[]> triangles = triangulate(cooList);
   //      RangeSet total = new RangeSet();
   //      for( Pointing [] vertex : triangles ) {
   //         RangeSet list = hpxBase[order].queryPolygonInclusive(vertex,4);
   //         if( list!=null ) total = total.union(list);
   //      }
   //      return total.toArray();
   //   }
   //
   //   static private ArrayList<Pointing[]> triangulate(ArrayList<double[]>cooList) {
   //      Polygon p = new Polygon();
   //      for( int i=0; i<cooList.size(); i++ ) {
   //         double [] a = cooList.get(i);
   //         p.p[i].x = a[0];
   //         p.p[i].y = a[1];
   //      }
   //      p.n=cooList.size();
   //
   //      Triangulation triangulation = Triangulate(p);
   //
   //      ArrayList<Pointing[]> tr = new ArrayList<Pointing[]>();
   //      for( int i=0; i<triangulation.t.size(); i++ ) {
   //         int [] t = triangulation.t.get(i);
   //         Pointing [] a = new Pointing[] {
   //               new Pointing( p.p[ t[0] ].x,  p.p[ t[0] ].y ),
   //               new Pointing( p.p[ t[1] ].x,  p.p[ t[1] ].y ),
   //               new Pointing( p.p[ t[2] ].x,  p.p[ t[2] ].y ),
   //         };
   //         tr.add(a);
   //      }
   //      return tr;
   //   }
   //
   //
   //   static final int MAXPOLY = 200;
   //   static final double EPSILON = 0.000001;
   //   static class Point {
   //      double x,y;
   //
   //      boolean inside(Point a, Point b, Point c) {
   //         java.awt.Polygon pol = new java.awt.Polygon();
   //         pol.addPoint((int)a.x, (int)a.y);
   //         pol.addPoint((int)b.x, (int)b.y);
   //         pol.addPoint((int)c.x, (int)c.y);
   //         return pol.contains(x,y);
   //      }
   //   }
   //
   //   static class Polygon {
   //      Point p[] = new Point[MAXPOLY];
   //      int n;
   //      Polygon() {
   //         for(int i=0;i<MAXPOLY;i++) p[i] = new Point();
   //      }
   //   }
   //
   //
   //   static class Triangulation {
   //      ArrayList<int[]> t = new ArrayList<int[]>();;
   //   }
   //
   //
   //   static Triangulation Triangulate(Polygon p) {
   //      Triangulation t = new Triangulation();
   //      int N = p.n;
   //
   //      // determine if i-j-k is a circle with no interior points
   //      for (int i = 0; i < N; i++) {
   //         for (int j = i+1; j < N; j++) {
   //            for (int k = j+1; k < N; k++) {
   //               boolean isTriangle = true;
   //               for (int a = 0; a < N; a++) {
   //                  if (a == i || a == j || a == k) continue;
   //                  if (p.p[a].inside(p.p[i], p.p[j], p.p[k])) {
   //                     isTriangle = false;
   //                     break;
   //                  }
   //               }
   //
   //
   //               if (isTriangle) {
   //                  t.t.add( new int[] { i,j,k } );
   //               }
   //            }
   //         }
   //      }
   //
   //      return t;
   //   }


   //   static public long[] query_polygon(long nside,ArrayList<double[]>cooList) throws Exception {
   //      ArrayList vlist = new ArrayList(cooList.size());
   //      Iterator<double[]> it = cooList.iterator();
   //      while( it.hasNext() ) {
   //         double coo[] = it.next();
   //         vlist.add(new SpatialVector(coo[0], coo[1]));
   //      }
   //      int order = init(nside);
   //      Pointing[] vertex = new Pointing[vlist.size()];
   //      for (int i=0; i<vlist.size(); ++i) vertex[i]=new Pointing((Vec3)vlist.get(i));
   //      LongRangeSet list = hpxBase[order].queryPolygon(vertex,true);
   //      if( list==null ) return new long[0];
   //      return list.toArray();
   //   }


   /** The Constant cPr. */
   public static final double cPr = Math.PI / 180;


   static private double dec(Pointing ptg) {
      return (Math.PI*0.5 - ptg.theta) / cPr;
   }

   static private double ra(Pointing ptg) {
      return ptg.phi / cPr;
   }

   static public Pointing pointing(double ra, double dec) {
      return new Pointing( Math.PI/2 - (Math.PI/180)*dec , ra*cPr  );
   }


   static final private int [] A = { 3, 2, 0, 1 };
   static public double[][] corners(long nside,long npix) throws Exception {
      Vec3[] tvec = hpxBase[ init(nside) ].boundaries(npix,1);
      double [][] corners = new double[tvec.length][2];
      for (int i=0; i<tvec.length; ++i) {
         Pointing pt = new Pointing(tvec[i]);
         int j=A[i];
         corners[j][0] = ra(pt);
         corners[j][1] = dec(pt);
      }
      return corners;
   }
   //   static public double[][] corners(long nside,long npix) throws Exception {
   //      Vec3[] tvec = hpxBase[ init(nside) ].corners(npix,1);
   //      double [][] corners = new double[tvec.length][2];
   //      for (int i=0; i<tvec.length; ++i) {
   //         SpatialVector v = new SpatialVector(tvec[i]);
   //         int j=A[i];
   //         corners[j][0] = v.ra();
   //         corners[j][1] = v.dec();
   //      }
   //      return corners;
   //   }

   static public double[][] borders(long nside,long npix,int step) throws Exception {
      Vec3[] tvec = hpxBase[ init(nside) ].boundaries(npix,step);
      double [][] borders = new double[tvec.length][2];
      for (int i=0; i<tvec.length; ++i) {
         Pointing pt = new Pointing(tvec[i]);
         borders[i][0] = ra(pt);
         borders[i][1] = dec(pt);
      }
      return borders;
   }
   //   static public double[][] borders(long nside,long npix,int step) throws Exception {
   //      Vec3[] tvec = hpxBase[ init(nside) ].corners(npix,step);
   //      double [][] borders = new double[tvec.length][2];
   //      for (int i=0; i<tvec.length; ++i) {
   //         SpatialVector v = new SpatialVector(tvec[i]);
   //         borders[i][0] = v.ra();
   //         borders[i][1] = v.dec();
   //      }
   //      return borders;
   //   }

   static public long [] neighbours(long nside, long npix) throws Exception  {
      return hpxBase[ init(nside) ].neighbours(npix);
   }

   static public long nest2ring(long nside, long npix) throws Exception  {
      return hpxBase[ init(nside) ].nest2ring(npix);
   }

   static public long ring2nest(long nside, long npix) throws Exception  {
      return hpxBase[ init(nside) ].ring2nest(npix);
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

   /** Retourne le nside et le pixel pour un numéro uniq donné
    * en utilisant le tableau passé en paramètre s'il est différent de  null */
   static long [] uniq2nsidepix(long uniq,long [] nsidepix) {
      if( nsidepix==null ) nsidepix = new long[2];
      long order = log2(uniq/4)/2;
      nsidepix[0] = pow2(order);
      nsidepix[1] = uniq - 4*nsidepix[0]*nsidepix[0];
      return nsidepix;
   }

   public static final long pow2(long order){ return 1L<<order;}
   public static final long log2(long nside){ int i=0; while((nside>>>(++i))>0); return --i; }

   static public double[] radecToPolar(double[] radec) { return radecToPolar(radec,new double[2]); }
   static public double[] radecToPolar(double[] radec,double polar[]) {
      polar[0] = Math.PI/2. - radec[1]/180.*Math.PI;
      polar[1] = radec[0]/180.*Math.PI;
      return polar;
   }

   static public double[] polarToRadec(double[] polar) { return polarToRadec(polar,new double[2]); }
   static public double[] polarToRadec(double[] polar,double radec[]) {
      radec[1] = (Math.PI/2. - polar[0])*180./Math.PI;
      radec[0] = polar[1]*180./Math.PI;
      return radec;
   }



//   public static void main(String argv[]) {
//      try {
//         String survey="int gal 35-80 flux";
//         int order=2;
//         try { order = Integer.parseInt(argv[1]); }
//         catch( Exception e) {}
//         long nside = pow2( order );
//         long size = 12 * nside*nside;
//         double sideDeg = 1.1* Math.sqrt(2) * pixRes(nside)/3600;
//         String skyview = "java XXX -Duser.language=en Float Survey=\""+survey+"\" Projection=Sin Pixels=300 Sampler=NN Size="+sideDeg;
//         String batch   = "-cp Skyview.jar skyview.executive.Batch Skyview-batch.txt";
//         String jar     = "-jar Skyview.jar";
//         for( int pix = 0; pix< size; pix++ ) {
//            double [] polar = pix2ang_nest(nside, pix);
//            double [] radec = polarToRadec( polar );
//            if( pix==0 ) {
//               String s1 = skyview.replace("XXX",jar);
//               String s2 = skyview.replace("XXX",batch);
//               System.out.println("Test:    "+s1+" position=\""+radec[0]+" "+radec[1]+"\" output=Test");
//               System.out.println("Command: "+s2+"\n");
//            }
//            System.out.println("position=\""+radec[0]+" "+radec[1]+"\" output=Img"+pix);
//         }
//      } catch( Exception e) { e.printStackTrace(); }
//   }




   // ------------------------------- CDS

   //   /** Approximation des coordonnées RA,DEC des 4 angles du losange
   //    * Je recherche les coord (centrales) des losanges des 4 coins dans la résolution
   //    * Healpix maximale */
   //   private static double [][] corners_nestCDS(long nside, long pixid) {
   //      double [][] corners;
   //      try {
   //         int order = (int)log2(nside);
   //         corners = new double[4][2];
   //         int orderFile = getMaxOrder() - order; // Je ne dois pas dépasser la limite Healpix
   //         long nSidePix = pow2(orderFile);
   //         // Numéro des pixels des 4 coins
   //         long c0, c1, c2, c3;
   //         c0 = c1 = c2 = 0;
   //         c3 = (nSidePix * nSidePix) - 1;
   //         for( int i = 0; i < orderFile; i++ ) c1 = (c1 << 2) | 1;
   //         for( int i = 0; i < orderFile; i++ ) c2 = (c2 << 2) | 2;
   //         // Chaque pixel "interne" va être remplacé par nsidePix*nsidePix pixels
   //         // d'où l'offset suivant
   //         long offset = pixid * nSidePix * nSidePix;
   //         c0 += offset;
   //         c1 += offset;
   //         c2 += offset;
   //         c3 += offset;
   //         long nSideFile = (long) pow2(order + orderFile);
   //         polarToRadec(pix2ang_nest(nSideFile, c0) , corners[0]);
   //         polarToRadec(pix2ang_nest(nSideFile, c1) , corners[1]);
   //         polarToRadec(pix2ang_nest(nSideFile, c2) , corners[2]);
   //         polarToRadec(pix2ang_nest(nSideFile, c3) , corners[3]);
   //       } catch( Exception e ) { return null; }
   //      return corners;
   //   }


}

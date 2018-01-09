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

package cds.tools.pixtools;

import java.util.ArrayList;

import cds.moc.HealpixMoc;
import healpix.essentials.HealpixBase;
import healpix.essentials.Moc;
import healpix.essentials.MocQuery;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import healpix.essentials.Vec3;

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

   // ATTNENTION LE RAYON EST EN RADIAN
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

   static public long[] query_polygon(long nside,ArrayList<double[]>cooList) throws Exception {
      int order = init(nside);
      Pointing[] vertex = new Pointing[cooList.size()];
      int i=0;
      for(double [] coo : cooList) vertex[i++]=pointing(coo[0], coo[1]);
      RangeSet list = hpxBase[order].queryPolygonInclusive(vertex,4);
      if( list==null ) return new long[0];
      return list.toArray();
   }


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
   
   
   /**
    * Génération d'un MOC à partir d'un polygone sphérique décrit par la liste de ses sommets en ICRS
    * bobiné dans le sens anti-horaire. Le dernier sommet ne reprend pas le premier. 
    * @param radecList
    * @param order
    * @return
    * @throws Exception
    */
   static public HealpixMoc createHealpixMoc(ArrayList<double[]> radecList, int order ) throws Exception {
      HealpixMoc moc=null;

      ArrayList<Vec3> cooList = new ArrayList<Vec3>();
      for( double radec[] : radecList ) {
         double theta = Math.PI/2 - Math.toRadians( radec[1] );
         double phi = Math.toRadians( radec[0] );
         cooList.add(new Vec3(new Pointing(theta,phi)));
      }

      Moc m=MocQuery.queryGeneralPolygonInclusive(cooList,order,order+4>29?29:order+4);
      moc = new HealpixMoc();
      moc.rangeSet = m.getRangeSet();
      moc.toHealpixMoc();
      return moc;
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

}

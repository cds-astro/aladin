// Copyright 1999-2018 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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

import static cds.healpix.Projection.LAT_INDEX;
import static cds.healpix.Projection.LON_INDEX;
import static cds.healpix.common.math.Math.HALF_PI;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;

import cds.healpix.CompassPoint.Cardinal;
import cds.healpix.CompassPoint.MainWind;
import cds.healpix.FlatHashIterator;
import cds.healpix.HashComputer;
import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.HealpixNestedBMOC;
import cds.healpix.HealpixNestedFixedRadiusConeComputer;
import cds.healpix.HealpixNestedPolygonComputer;
import cds.healpix.NeighbourList;
import cds.healpix.NeighbourSelector;
import cds.healpix.VerticesAndPathComputer;
import cds.moc.HealpixMoc;
import cds.moc.Range;
import healpix.essentials.HealpixBase;
import healpix.essentials.Moc;
import healpix.essentials.MocQuery;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import healpix.essentials.Vec3;

/** Wrapper Healpix CDS pour ne pas reinitialiser systematiquement l'objet HealpixBase pour chaque NSIDE
 * @author Pierre Fernique [CDS] with the help of Martin Reinecke
 * @version 1.1 Avril 2018 - passage librairie FX
 */
public final class CDSHealpix {
   
   static final public boolean FX = true;

   static final public int MAXORDER=29;

   static private HealpixBase hpxBase[] = new HealpixBase[MAXORDER+1];  // Objet HealpixBase pour chaque nside utilis�


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
      if( FX ) return pix2ang_nestFX(nside,ipix);
//      double[] resFX = pix2ang_nestFX(nside, ipix);
      Pointing res = hpxBase[ init(nside) ].pix2ang(ipix);
      
//      if (Math.abs(res.theta - resFX[0]) > 1e-14) {
//        System.out.println("Diff theta: " + Math.abs(res.theta - resFX[0]));
//        System.exit(1);
//      }
//      if (Math.abs(res.phi - resFX[1]) > 1e-14) {
//        System.out.println("Diff phi: " + Math.abs(res.theta - resFX[0]));
//        System.exit(1);
//      }
      
      return new double[]{ res.theta, res.phi };
   }
   static public double[] pix2ang_nestFX(long nside,long ipix) throws Exception {
     final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
     final VerticesAndPathComputer vpc = hn.newVerticesAndPathComputer(); // For thread safety issues
     final double[] lonlat = vpc.center(ipix);
     final double lat = lonlat[LAT_INDEX];
     lonlat[LAT_INDEX] = lonlat[LON_INDEX];
     lonlat[LON_INDEX] = HALF_PI - lat;
     return lonlat;
   }
   

   static public long ang2pix_nest(long nside,double theta, double phi) throws Exception {
      if( FX ) return ang2pix_nestFX(nside,theta,phi);
      //long resFX = ang2pix_nestFX(nside,theta,phi);
      long res = hpxBase[ init(nside) ].ang2pix(new Pointing(theta,phi));
      /*if (res != resFX) {
        System.out.println("Diff hash: " + res + " != " + resFX);
        System.exit(1);
      }*/
      return res;
   }
   static public long ang2pix_nestFX(long nside,double theta, double phi) throws Exception {
     // Travailler directement en lonRad, laRad !!
     final double lonRad = phi;
     final double latRad = HALF_PI - theta;
     // A essayer: moins rapide, mais moins de cache necessaire donc peu être plus rapide:
     // final HashComputer hc = Healpix.getNestedFast(Healpix.depth((int) nside), FillingCurve2DType.Z_ORDER_XOR);
     // Tester aussi les perfs avec le code plus lisible
     // HashComputer hc = Healpix.getNested(Healpix.depth((int) nside)).newHashComputer();
     final HashComputer hc = Healpix.getNestedFast(Healpix.depth((int) nside));
     return hc.hash(lonRad, latRad);  
   }

   // ATTNENTION LE RAYON EST EN RADIAN
   static public long[] query_disc(long nside,double ra, double dec, double radius) throws Exception {
      return query_disc(nside, ra, dec, radius, true);
   }
   
   public static void main(String [] arg) {
      double ra=97.91750000, dec=5.76952778, radius=4.45/60;
      int order=9;
      
      final HealpixNested hn = Healpix.getNested(order);
      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputer( Math.toRadians(radius) );
//      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputerApprox( Math.toRadians(radius) );
      final HealpixNestedBMOC bmoc = cp.overlappingCells(Math.toRadians(ra), Math.toRadians(dec));
      long [] out = toFlatArrayOfHash(bmoc);
      
      System.out.print("overlappingCells checker:\ndraw circle("+ra+","+dec+","+radius+")\ndraw moc "+order+"/");
      for( long a : out ) System.out.print(" "+a);
      System.out.println();
  }

   static public long[] query_disc(long nside,double ra, double dec, double radius, boolean inclusive) throws Exception {
      if( FX ) return query_discFX(nside,ra,dec,radius,inclusive);
//      long l1 = System.nanoTime();
      int order = init(nside);
      RangeSet list = inclusive ? hpxBase[order].queryDiscInclusive(pointing(ra,dec),radius,4)
            : hpxBase[order].queryDisc(pointing(ra,dec),radius);
      if( list==null ) return new long[0];
//      long l2 = System.nanoTime();
//      System.err.println("Cone REN computed in " + (l2 - l1) / (1e6d) + " ms");
      return list.toArray();
   }
   static public long[] query_discFX(long nside,double ra, double dec, double radius, boolean inclusive) throws Exception {
//     System.err.println("Cone. depth: " + Healpix.depth((int) nside) + "; lonRad: " + Math.toRadians(ra) + "; latRad: " + Math.toRadians(dec) + "; rRad: " + radius);
     
//     long l1 = System.nanoTime();
     final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputer(radius);
//     final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputerApprox(radius);
     
//     cp.overlappingCells((Math.toRadians(ra), Math.toRadians(dec), ReturnedCells.FULLY_IN)
     
     final HealpixNestedBMOC bmoc = inclusive ? cp.overlappingCells(Math.toRadians(ra), Math.toRadians(dec)) :
           cp.overlappingCenters(Math.toRadians(ra), Math.toRadians(dec));
//     final HealpixNestedBMOC bmoc = cp.overlappingCells(Math.toRadians(ra), Math.toRadians(dec));
//     long l2 = System.nanoTime();
//     System.err.println("Cone FX computed in " + (l2 - l1) / (1e6d) + " ms");

     return toFlatArrayOfHash(bmoc);
   }

   static public long[] query_discFXCenters(long nside,double ra, double dec, double radius) throws Exception {
      final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputer(radius);
      final HealpixNestedBMOC bmoc = cp.overlappingCenters(Math.toRadians(ra), Math.toRadians(dec));
      return toFlatArrayOfHash(bmoc);
   }

      
   static public long[] query_polygon(long nside,ArrayList<double[]>cooList, boolean inclusive) throws Exception {

      if( FX ) return query_polygonFX(nside,cooList,inclusive);
//      long l1 = System.nanoTime();
      int order = init(nside);
      Pointing[] vertex = new Pointing[cooList.size()];
      int i=0;
      for(double [] coo : cooList) vertex[i++]=pointing(coo[0], coo[1]);
      RangeSet list = hpxBase[order].queryPolygonInclusive(vertex,4);
      if( list==null ) return new long[0];
      long[] res = list.toArray();
//      long l2 = System.nanoTime();
//System.err.println("Poygon REN computed in " + (l2 - l1) /  (1e6d) + " ms");
      return res;
   }
   
   static public long[] query_polygonFX(long nside,ArrayList<double[]>cooList,boolean inclusive) throws Exception {
//     long l1 = System.nanoTime();
//      System.out.println("depth="+Healpix.depth((int) nside));
     final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
     
     final HealpixNestedPolygonComputer pc = hn.newPolygonComputer();
//     final double[][] vertices = cooList.toArray(new double[][]{{}});
     double[][] vertices = new double[cooList.size()][2];
     cooList.toArray(vertices);
     for (int i = 0; i < vertices.length; i++) {
        vertices[i][0] = Math.toRadians(vertices[i][0]);
        vertices[i][1] = Math.toRadians(vertices[i][1]);
//        System.out.println(" "+vertices[i][0]+" "+vertices[i][1]);
     }
     final HealpixNestedBMOC bmoc = inclusive ? pc.overlappingCells(vertices) : pc.overlappingCenters(vertices);
//     System.out.println("bmoc.size()="+bmoc.size());
//     for (final CurrentValueAccessor cva : bmoc) {
//        System.out.println(cva);
//     }
     final long[] res = toFlatArrayOfHash(bmoc); 
//     long l2 = System.nanoTime();
//     System.err.println("Poygon FX computed in " + (l2 - l1) /  (1e6d) + " ms");
     return res;
   }

   private static final long[] toFlatArrayOfHash(final HealpixNestedBMOC bmoc) {
     final long nElems = bmoc.computeDeepSize();
     if (nElems > Integer.MAX_VALUE) {
       throw new Error("MOC contains too many elements!");
     } else if (nElems < bmoc.size()) {
       throw new Error("MOC deep size can't be < MOC size!");
     } 
     final long[] res = new long[(int) nElems];
// System.out.println("@@@@@@@@@@@@@@@ res size: " + res.length);
     final FlatHashIterator it = bmoc.flatHashIterator();
     for (int i = 0; it.hasNext(); i++) {
       res[i] = it.next();
     }
     return res;
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
      if( FX ) return cornersFX(nside,npix);
      // double[][] resFX = cornersFX(nside,npix);;
      
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
   static public double[][] cornersFX(long nside,long npix) throws Exception {
     final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
     final VerticesAndPathComputer vpc = hn.newVerticesAndPathComputer(); // For thread safety issues
     final EnumMap<Cardinal, double[]> vertices = vpc.vertices(npix, EnumSet.allOf(Cardinal.class));
     for (final double[] v : vertices.values()) {
       v[0] = Math.toDegrees(v[0]);
       v[1] = Math.toDegrees(v[1]);
     }
     // N, W, S, E
     // => E, S, N, W
     final double[][] res = new double[vertices.size()][];
     res[0] = vertices.get(Cardinal.S);
     res[1] = vertices.get(Cardinal.E);
     res[2] = vertices.get(Cardinal.W);
     res[3] = vertices.get(Cardinal.N);
     return res;
   }

   static public double[][] borders(long nside,long npix,int step) throws Exception {
      if( FX ) return bordersFX(nside,npix,step);
      Vec3[] tvec = hpxBase[ init(nside) ].boundaries(npix,step);
      double [][] borders = new double[tvec.length][2];
      for (int i=0; i<tvec.length; ++i) {
         Pointing pt = new Pointing(tvec[i]);
         borders[i][0] = ra(pt);
         borders[i][1] = dec(pt);
      }
      return borders;
   }
   static public double[][] bordersFX(long nside,long npix,int step) throws Exception {
     final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
     final VerticesAndPathComputer vpc = hn.newVerticesAndPathComputer(); // For thread safety issues
     final double[][] res = vpc.pathAlongCellEdge(npix, Cardinal.N, false, step);
     for (final double[] lonlatRad : res) {
       lonlatRad[0] = Math.toDegrees(lonlatRad[0]);
       lonlatRad[1] = Math.toDegrees(lonlatRad[1]);
     }
     return res;
   }

   static public long[] neighbours(long nside, long npix) throws Exception  {
      if( FX ) return neighboursFX(nside, npix);
      return hpxBase[ init(nside) ].neighbours(npix);
   }
   static public long[] neighboursFX(long nside, long npix) throws Exception  {
     final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
     final NeighbourSelector neig = hn.newNeighbourSelector(); // For thread safety issues
     // SW, W, NW, N, NE, E, SE and S
     final NeighbourList neigList = neig.neighbours(npix);
     final long[] res = new long[8];
     res[0] = neigList.get(MainWind.SW);
     res[1] = neigList.get(MainWind.W);
     res[2] = neigList.get(MainWind.NW);
     res[3] = neigList.get(MainWind.N);
     res[4] = neigList.get(MainWind.NE);
     res[5] = neigList.get(MainWind.E);
     res[6] = neigList.get(MainWind.SE);
     res[7] = neigList.get(MainWind.S);
     return res;
   }

   static public long nest2ring(long nside, long npix) throws Exception  {
      if( FX ) return nest2ringFX(nside,npix);
      return hpxBase[ init(nside) ].nest2ring(npix);
   }
   static public long nest2ringFX(long nside, long npix) throws Exception  {
     final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
     return hn.toRing(npix);
   }

   static public long ring2nest(long nside, long npix) throws Exception  {
      if( FX ) return ring2nestFX(nside,npix);
      return hpxBase[ init(nside) ].ring2nest(npix);
   }
   static public long ring2nestFX(long nside, long npix) throws Exception  {
     final HealpixNested hn = Healpix.getNested(Healpix.depth((int) nside));
     return hn.toNested(npix);
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

   /** Retourne la num�rotation unique pour un pixel d'un nside donn� */
   static long nsidepix2uniq(long nside, long npix) {
      return 4*nside*nside + npix;
   }

   /** Retourne le nside et le pixel pour un num�ro uniq donn� */
   static long [] uniq2nsidepix(long uniq) {
      return uniq2nsidepix(uniq,null);
   }

   /** Retourne le nside et le pixel pour un num�ro uniq donn�
    * en utilisant le tableau pass� en param�tre s'il est diff�rent de  null */
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
    * G�n�ration d'un MOC � partir d'un polygone sph�rique d�crit par la liste de ses sommets en ICRS
    * bobin� dans le sens anti-horaire. Le dernier sommet ne reprend pas le premier. 
    * @param radecList
    * @param order
    * @return
    * @throws Exception
    */
   static public HealpixMoc createHealpixMoc(ArrayList<double[]> radecList, int order ) throws Exception {
      if( FX ) return createHealpixMocFX(radecList,order);
      HealpixMoc moc=null;

      ArrayList<Vec3> cooList = new ArrayList<>();
      for( double radec[] : radecList ) {
         double theta = Math.PI/2 - Math.toRadians( radec[1] );
         double phi = Math.toRadians( radec[0] );
         cooList.add(new Vec3(new Pointing(theta,phi)));
      }

      long l1 = System.nanoTime();
      Moc m=MocQuery.queryGeneralPolygonInclusive(cooList,order,order+4>29?29:order+4);
      long l2 = System.nanoTime();
//System.err.println("Poygon REN computed in " + (l2 - l1) / (1e6d) + " ms");
      
      moc = new HealpixMoc();
      moc.rangeSet = new Range( m.getRangeSet() );
      moc.toHealpixMoc();
      return moc;
   }
   static public HealpixMoc createHealpixMocFX(ArrayList<double[]> radecList, int order ) throws Exception {
     final HealpixMoc hmoc = new HealpixMoc(order);
     hmoc.add(order, query_polygonFX(Healpix.nside(order), radecList, true));
     return hmoc;
   }

// draw polygon(05:42:25.49,-01:58:26.9, 05:42:53.35,-02:09:11.2, 05:42:40.65,-02:33:47.3, 05:42:09.41,-02:15:54.6, 05:41:21.13,-02:33:47.4, 05:40:54.23,-02:18:04.2, 05:40:00.18,-02:26:17.3, 05:39:27.29,-02:06:58.0, 05:40:25.41,-02:06:47.4, 05:39:51.32,-01:56:24.5, 05:41:54.51,-01:57:22.2, 05:42:11.79,-01:46:52.2, 05:42:00.04,-02:10:16.2, 05:42:30.78,-02:09:18.5, 05:42:19.97,-02:02:46.2, 05:42:19.97,-02:02:46.2)
// FX: 45 puis 37 ms
// RE: 42 puis 30 ms
   
//draw polygon(05:41:57.87,-02:02:17.4, 05:41:57.87,-02:02:17.4, 05:42:01.47,-02:02:21.0, 05:42:33.17,-02:02:46.1, 05:42:38.94,-02:02:53.3, 05:42:42.30,-02:03:04.1, 05:42:45.42,-02:03:11.3, 05:42:49.02,-02:03:25.6, 05:42:50.47,-02:03:40.0, 05:42:52.15,-02:04:08.8, 05:42:53.11,-02:04:34.0, 05:42:53.59,-02:05:10.0, 05:42:53.35,-02:06:58.0, 05:42:52.39,-02:07:26.8, 05:42:49.51,-02:07:55.6, 05:42:42.06,-02:08:20.9, 05:42:36.06,-02:08:20.9, 05:42:29.10,-02:08:20.9, 05:42:13.24,-02:08:21.0, 05:42:05.32,-02:08:21.0, 05:41:59.80,-02:08:10.2, 05:41:50.43,-02:08:03.0, 05:41:47.07,-02:08:03.0, 05:41:44.42,-02:08:03.0, 05:41:40.58,-02:08:13.8, 05:41:38.90,-02:08:21.0, 05:41:31.46,-02:09:51.0, 05:41:31.22,-02:10:12.6, 05:41:31.70,-02:10:34.2, 05:41:33.38,-02:11:03.0, 05:41:34.82,-02:11:17.4, 05:41:37.46,-02:11:35.4, 05:41:42.74,-02:12:11.4, 05:41:57.88,-02:13:19.8, 05:42:06.76,-02:13:55.8, 05:42:15.89,-02:14:24.6, 05:42:31.98,-02:14:46.1, 05:42:39.67,-02:15:00.5, 05:42:45.19,-02:15:11.3, 05:42:50.96,-02:15:29.2, 05:42:51.92,-02:15:50.8, 05:42:52.88,-02:16:52.0, 05:42:50.96,-02:17:56.8, 05:42:50.24,-02:18:18.4, 05:42:48.80,-02:18:43.6, 05:42:42.80,-02:20:02.9, 05:42:37.99,-02:20:46.1, 05:42:32.95,-02:21:32.9, 05:42:27.43,-02:22:16.1, 05:42:22.14,-02:22:41.3, 05:42:17.10,-02:22:48.6, 05:42:10.85,-02:22:48.6, 05:42:07.73,-02:22:48.6, 05:42:04.13,-02:22:55.8, 05:41:59.32,-02:23:03.0, 05:41:49.23,-02:23:24.6, 05:41:45.63,-02:23:35.4, 05:41:43.95,-02:23:46.2, 05:41:45.15,-02:24:04.2, 05:41:46.35,-02:24:22.2, 05:41:49.23,-02:24:51.0, 05:41:57.40,-02:26:03.0, 05:42:01.97,-02:26:42.6, 05:42:06.53,-02:27:22.2, 05:42:08.93,-02:27:47.4, 05:42:11.58,-02:28:09.0, 05:42:13.02,-02:28:37.8, 05:42:13.26,-02:28:59.4, 05:42:13.26,-02:29:28.2, 05:42:13.26,-02:30:18.6, 05:42:13.02,-02:31:01.8, 05:42:12.78,-02:31:27.0, 05:42:12.54,-02:31:52.2, 05:42:11.82,-02:32:13.8, 05:42:10.38,-02:32:42.6, 05:42:08.70,-02:33:00.6, 05:42:06.78,-02:33:15.0, 05:42:04.85,-02:33:22.2, 05:42:02.93,-02:33:29.4, 05:41:58.85,-02:33:36.6, 05:41:56.69,-02:33:40.2, 05:41:54.28,-02:33:40.2, 05:41:50.20,-02:33:40.2, 05:41:45.15,-02:33:40.2, 05:41:39.15,-02:33:36.6, 05:41:33.38,-02:33:25.8, 05:41:27.86,-02:33:07.8, 05:41:22.57,-02:32:35.4, 05:41:13.44,-02:31:23.4, 05:41:06.48,-02:30:18.6, 05:41:03.83,-02:29:39.0, 05:41:01.67,-02:28:52.2, 05:40:59.75,-02:28:12.6, 05:40:58.55,-02:27:36.6, 05:40:56.39,-02:26:28.2, 05:40:56.39,-02:25:52.2, 05:40:56.15,-02:25:12.6, 05:40:56.15,-02:24:36.6, 05:40:56.15,-02:24:00.6, 05:40:56.15,-02:23:10.2, 05:40:56.15,-02:22:45.0, 05:40:55.67,-02:22:16.2, 05:40:55.19,-02:21:36.6, 05:40:54.71,-02:21:07.8, 05:40:53.27,-02:20:35.4, 05:40:51.58,-02:20:21.0, 05:40:50.14,-02:20:13.8, 05:40:46.78,-02:20:13.8, 05:40:42.46,-02:20:21.0, 05:40:37.89,-02:20:42.6, 05:40:31.89,-02:21:07.8, 05:40:25.16,-02:21:07.8, 05:40:18.44,-02:21:07.8, 05:40:12.19,-02:21:07.8, 05:40:07.15,-02:20:57.0, 05:39:59.22,-02:20:10.1, 05:39:55.62,-02:19:37.7, 05:39:52.98,-02:19:12.5, 05:39:51.30,-02:18:50.9, 05:39:49.86,-02:18:29.3, 05:39:48.65,-02:18:07.7, 05:39:47.21,-02:17:42.5, 05:39:45.05,-02:17:10.1, 05:39:42.89,-02:16:34.1, 05:39:37.85,-02:15:00.5, 05:39:34.97,-02:14:13.6, 05:39:33.53,-02:13:26.8, 05:39:32.57,-02:12:43.6, 05:39:32.09,-02:11:10.0, 05:39:32.09,-02:10:30.4, 05:39:32.09,-02:09:11.2, 05:39:33.29,-02:08:06.4, 05:39:34.25,-02:07:41.2, 05:39:35.94,-02:07:08.8, 05:39:38.82,-02:06:32.9, 05:39:41.94,-02:05:56.9, 05:39:45.06,-02:05:35.3, 05:39:47.23,-02:05:31.7, 05:39:49.39,-02:05:10.1, 05:39:54.19,-02:04:23.3, 05:39:56.59,-02:03:50.9, 05:39:59.24,-02:03:18.5, 05:40:01.88,-02:02:42.5, 05:40:03.80,-02:02:10.1, 05:40:05.72,-02:01:48.6, 05:40:07.40,-02:01:27.0, 05:40:08.36,-02:01:05.4, 05:40:09.56,-02:00:33.0, 05:40:10.28,-01:59:49.8, 05:40:10.28,-01:59:24.6, 05:40:10.29,-01:58:59.4, 05:40:10.05,-01:58:30.6, 05:40:08.84,-01:58:05.4, 05:40:07.40,-01:57:33.0, 05:40:05.24,-01:56:49.8, 05:39:57.32,-01:55:05.3, 05:39:53.00,-01:54:07.7, 05:39:48.68,-01:53:02.9, 05:39:45.32,-01:51:58.1, 05:39:43.64,-01:50:53.3, 05:39:43.16,-01:49:48.5, 05:39:43.16,-01:48:54.5, 05:39:43.16,-01:47:38.9, 05:39:43.64,-01:47:13.7, 05:39:45.56,-01:46:48.5, 05:39:48.20,-01:46:30.5, 05:39:50.84,-01:46:30.5, 05:40:03.81,-01:46:37.7, 05:40:13.65,-01:46:48.6, 05:40:23.98,-01:46:59.4, 05:40:31.90,-01:47:24.6, 05:40:38.87,-01:47:46.2, 05:40:44.63,-01:48:11.4, 05:40:48.47,-01:48:33.0, 05:40:52.31,-01:49:05.4, 05:40:53.99,-01:49:27.0, 05:40:55.91,-01:49:45.0, 05:40:57.59,-01:50:13.8, 05:40:59.28,-01:50:39.0, 05:41:01.20,-01:51:04.2, 05:41:02.40,-01:51:33.0, 05:41:03.84,-01:52:16.2, 05:41:06.48,-01:53:31.8, 05:41:06.72,-01:54:00.6, 05:41:06.96,-01:54:22.2, 05:41:06.96,-01:54:47.4, 05:41:06.96,-01:55:23.4, 05:41:06.48,-01:56:24.6, 05:41:06.24,-01:57:04.2, 05:41:05.76,-01:57:25.8, 05:41:04.56,-01:58:05.4, 05:41:04.08,-01:58:41.4, 05:41:03.84,-01:59:06.6, 05:41:07.68,-01:59:28.2, 05:41:12.00,-01:59:28.2, 05:41:17.05,-01:59:28.2, 05:41:22.33,-01:59:28.2, 05:41:27.37,-01:59:28.2, 05:41:32.17,-01:59:21.0, 05:41:36.98,-01:59:10.2, 05:41:41.30,-01:58:45.0, 05:41:52.59,-01:57:11.4, 05:41:55.71,-01:56:35.4, 05:41:58.83,-01:55:55.8, 05:42:01.71,-01:55:16.2, 05:42:07.71,-01:54:07.8, 05:42:13.72,-01:53:06.6, 05:42:16.60,-01:52:45.0, 05:42:19.00,-01:52:27.0, 05:42:23.32,-01:52:19.7, 05:42:25.72,-01:52:19.7, 05:42:27.64,-01:52:26.9, 05:42:30.04,-01:52:48.5, 05:42:32.21,-01:53:20.9, 05:42:33.65,-01:54:00.5, 05:42:33.65,-01:55:01.7, 05:42:32.45,-01:55:34.1, 05:42:30.53,-01:55:59.3, 05:42:29.09,-01:56:17.3, 05:42:25.24,-01:56:56.9, 05:42:20.44,-01:57:29.3, 05:42:17.08,-01:57:47.4, 05:42:13.72,-01:57:54.6, 05:42:10.36,-01:58:09.0, 05:42:04.35,-01:58:30.6, 05:42:01.47,-01:58:37.8, 05:41:59.07,-01:58:48.6, 05:41:54.75,-01:59:03.0, 05:41:52.35,-01:59:13.8, 05:41:46.58,-01:59:57.0, 05:41:43.46,-02:00:29.4, 05:41:41.78,-02:00:43.8, 05:41:38.90,-02:01:01.8, 05:41:34.34,-02:01:34.2, 05:41:32.41,-02:01:48.6, 05:41:29.53,-02:02:03.0, 05:41:26.41,-02:02:17.4, 05:41:24.01,-02:02:31.8, 05:41:20.17,-02:03:00.6, 05:41:18.01,-02:03:15.0, 05:41:15.84,-02:03:29.4, 05:41:13.20,-02:03:36.6, 05:41:07.44,-02:03:54.6, 05:41:04.32,-02:04:01.8, 05:41:00.95,-02:04:01.8, 05:40:47.03,-02:04:09.0, 05:40:44.86,-02:04:09.0, 05:40:42.70,-02:04:09.0, 05:40:38.38,-02:04:12.6, 05:40:36.22,-02:04:27.0, 05:40:34.30,-02:04:55.8, 05:40:32.86,-02:05:10.2, 05:40:31.42,-02:05:28.2, 05:40:29.97,-02:05:49.8, 05:40:28.53,-02:06:07.8, 05:40:27.09,-02:06:25.8, 05:40:22.77,-02:07:27.0, 05:40:21.09,-02:08:03.0, 05:40:19.41,-02:08:42.6, 05:40:17.96,-02:09:18.6, 05:40:17.48,-02:09:58.2, 05:40:17.48,-02:11:03.0, 05:40:17.48,-02:11:31.8, 05:40:17.48,-02:11:57.0, 05:40:17.72,-02:12:33.0, 05:40:17.96,-02:13:05.4, 05:40:18.44,-02:13:41.4, 05:40:19.16,-02:14:06.6, 05:40:19.88,-02:14:46.2, 05:40:20.84,-02:15:11.4, 05:40:22.52,-02:15:33.0, 05:40:25.17,-02:15:51.0, 05:40:28.77,-02:15:54.6, 05:40:33.09,-02:16:01.8, 05:40:37.90,-02:16:09.0, 05:40:41.98,-02:16:19.8, 05:40:50.87,-02:16:30.6, 05:40:55.19,-02:16:30.6, 05:40:59.51,-02:16:37.8, 05:41:03.84,-02:16:45.0, 05:41:07.68,-02:16:45.0, 05:41:12.00,-02:16:45.0, 05:41:15.36,-02:16:45.0, 05:41:18.73,-02:16:45.0, 05:41:22.09,-02:16:45.0, 05:41:26.65,-02:16:45.0, 05:41:29.54,-02:16:30.6, 05:41:31.46,-02:16:19.8, 05:41:34.10,-02:16:16.2, 05:41:36.26,-02:16:09.0, 05:41:37.94,-02:16:01.8, 05:41:39.38,-02:15:54.6, 05:41:39.62,-02:15:18.6, 05:41:38.66,-02:14:57.0, 05:41:34.82,-02:14:13.8, 05:41:33.62,-02:13:59.4, 05:41:31.94,-02:13:41.4, 05:41:29.54,-02:13:27.0, 05:41:27.61,-02:13:16.2, 05:41:24.49,-02:13:09.0, 05:41:22.81,-02:12:58.2, 05:41:21.37,-02:12:51.0, 05:41:19.69,-02:12:33.0, 05:41:17.53,-02:11:49.8, 05:41:16.57,-02:11:13.8, 05:41:16.57,-02:10:05.4, 05:41:16.81,-02:08:42.6, 05:41:17.53,-02:08:03.0, 05:41:18.01,-02:07:41.4, 05:41:19.45,-02:07:05.4, 05:41:21.13,-02:06:29.4, 05:41:23.05,-02:06:04.2, 05:41:24.73,-02:05:46.2, 05:41:26.41,-02:05:39.0, 05:41:28.33,-02:05:35.4, 05:41:32.18,-02:05:35.4, 05:41:35.06,-02:05:35.4, 05:41:37.46,-02:05:35.4, 05:41:39.86,-02:05:35.4, 05:41:41.54,-02:05:28.2, 05:41:43.22,-02:05:17.4, 05:41:44.66,-02:04:59.4, 05:41:45.62,-02:04:41.4, 05:41:46.58,-02:04:23.4, 05:41:47.79,-02:04:01.8, 05:41:48.99,-02:03:36.6, 05:41:50.19,-02:03:15.0, 05:41:51.39,-02:03:00.6, 05:41:52.83,-02:02:46.2, 05:41:54.51,-02:02:42.6, 05:41:56.43,-02:02:35.4)
// FX: 99.7ms / 119 ms
// REN: 76 ms /  86 ms
   
// draw polygon(05:40:03.78,-02:27:00.5, 05:40:29.96,-02:28:34.2, 05:40:31.16,-02:37:27.0, 05:39:50.08,-02:37:34.1, 05:39:44.08,-02:31:37.7)
// FX: 12.8ms / 19.2ms
// RE: 6.9 / 7.0 ms
   
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

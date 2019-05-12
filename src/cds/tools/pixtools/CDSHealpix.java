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
//import healpix.essentials.HealpixBase;
//import healpix.essentials.Pointing;
//import healpix.essentials.Scheme;

/** Wrapper Healpix CDS pour ne pas reinitialiser systematiquement l'objet HealpixBase pour chaque NSIDE
 * @author Pierre Fernique [CDS] (Initially based on Martin Reinecke, now on François Xavier Pineau's code)
 * @version 1.1 Avril 2018 - passage librairie FX -> terminée en janvier 2019
 */
public final class CDSHealpix {
   
   static final public int MAXORDER=29;

//   static private HealpixBase hpxBase[] = new HealpixBase[MAXORDER+1];  // Objet HealpixBase pour chaque nside utilisé
////
////
//   static public HealpixBase getHealpixBase(int order) throws Exception  {
//      if( hpxBase[order]==null ) hpxBase[order] = new HealpixBase((int)pow2(order),Scheme.NESTED);
//      return hpxBase[order];
//   }
//
//   static private int init(long nside) throws Exception {
//      int order = (int)log2(nside);
//      if( hpxBase[order]!=null ) return order;
//      hpxBase[order] = new HealpixBase((int)nside,Scheme.NESTED);
//      return order;
//   }
//
//   static public double[] pix2ang_nest(long nside,long ipix) throws Exception {
//      Pointing res = hpxBase[ init(nside) ].pix2ang(ipix);
//      return new double[]{ res.theta, res.phi };
//   }


   static public double[] pix2ang_nest(int order,long ipix) throws Exception {
      final HealpixNested hn = Healpix.getNested(order);
      final VerticesAndPathComputer vpc = hn.newVerticesAndPathComputer(); // For thread safety issues
      final double[] lonlat = vpc.center(ipix);
      final double lat = lonlat[LAT_INDEX];
      lonlat[LAT_INDEX] = lonlat[LON_INDEX];
      lonlat[LON_INDEX] = HALF_PI - lat;
      return lonlat;
   }

   static public long ang2pix_nest(int order,double theta, double phi) throws Exception {
      // Travailler directement en lonRad, laRad !!
      final double lonRad = phi;
      final double latRad = HALF_PI - theta;
      // A essayer: moins rapide, mais moins de cache necessaire donc peut être plus rapide:
      // final HashComputer hc = Healpix.getNestedFast(Healpix.depth((int) nside), FillingCurve2DType.Z_ORDER_XOR);
      // Tester aussi les perfs avec le code plus lisible
      // HashComputer hc = Healpix.getNested(order).newHashComputer();
      final HashComputer hc = Healpix.getNestedFast(order);
      return hc.hash(lonRad, latRad);  
   }

   // ATTNENTION LE RAYON EST EN RADIAN
   static public long[] query_disc(int order,double ra, double dec, double radius) throws Exception {
      return query_disc(order, ra, dec, radius, true);
   }
   
//   public static void main(String [] arg) {
//
//      double ra=210.80216136704843, dec=54.34890606617321, radius=0.1652;
//      int order=11;
//
//      final HealpixNested hn = Healpix.getNested(order);
//      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputer( Math.toRadians(radius) );
//      // final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputerApprox( Math.toRadians(radius) );
//      final HealpixNestedBMOC bmoc = cp.overlappingCells(Math.toRadians(ra), Math.toRadians(dec));
//      long [] out = toFlatArrayOfHash(bmoc);
//
//      System.out.print("overlappingCells checker:\ndraw circle("+ra+","+dec+","+radius+")\ndraw moc "+order+"/");
//      for( long a : out ) System.out.print(" "+a);
//      System.out.println();
//   }


   
//   public static void main(String [] arg) {
//      
//      double ra=160.771389, dec=-64.3813, radius=0.8962;
//      int order=6;
//      
//      final HealpixNested hn = Healpix.getNested(order);
//      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputer( Math.toRadians(radius) );
////      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputerApprox( Math.toRadians(radius) );
//      final HealpixNestedBMOC bmoc = cp.overlappingCells(Math.toRadians(ra), Math.toRadians(dec));
//      long [] out = toFlatArrayOfHash(bmoc);
//      
//      System.out.print("overlappingCells checker:\ndraw circle("+ra+","+dec+","+radius+")\ndraw moc "+order+"/");
//      for( long a : out ) System.out.print(" "+a);
//      System.out.println();
//  }

   static public long[] query_disc(int order,double ra, double dec, double radius, boolean inclusive) throws Exception {
      //    System.err.println("Cone. depth: " + order + "; lonRad: " + Math.toRadians(ra) + "; latRad: " + Math.toRadians(dec) + "; rRad: " + radius);

      //    long l1 = System.nanoTime();
      final HealpixNested hn = Healpix.getNested(order);
      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputer(radius);
      //    final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputerApprox(radius);

      //    cp.overlappingCells((Math.toRadians(ra), Math.toRadians(dec), ReturnedCells.FULLY_IN)

      double [] coo = normalizeRaDec( ra,dec );
      HealpixNestedBMOC bmoc=null;
      try {
         bmoc = inclusive ? cp.overlappingCells(Math.toRadians(coo[0]), Math.toRadians(coo[1])) :
//         final HealpixNestedBMOC bmoc = inclusive ? cp.overlappingCells(Math.toRadians(ra), Math.toRadians(dec)) :
               cp.overlappingCenters(Math.toRadians(coo[0]), Math.toRadians(coo[1]));
      } catch( Exception e ) {
         
         System.err.println("\nHEALPix.query_disc error: ra="+ra+" dec="+dec+" radius="+radius+" (rad)");
         if( inclusive ) System.err.println("  Executing p.overlappingCells(+"+Math.toRadians(coo[0])+", "+Math.toRadians(coo[1])+")");
         else System.err.println("  Executing cp.overlappingCenters("+Math.toRadians(coo[0])+", "+Math.toRadians(coo[1])+")");
         e.printStackTrace();
         
      }

      return toFlatArrayOfHash(bmoc);
   }
   
   static public long[] query_discFXCenters(int order,double ra, double dec, double radius) throws Exception {
      final HealpixNested hn = Healpix.getNested(order);
      final HealpixNestedFixedRadiusConeComputer cp = hn.newConeComputer(radius);
      double [] coo = normalizeRaDec( ra,dec );
      final HealpixNestedBMOC bmoc = cp.overlappingCenters(Math.toRadians(coo[0]), Math.toRadians(coo[1]));
//      final HealpixNestedBMOC bmoc = cp.overlappingCenters(Math.toRadians(ra), Math.toRadians(dec));
      return toFlatArrayOfHash(bmoc);
   }

      
   static public long[] query_polygon(int order,ArrayList<double[]>cooList, boolean inclusive) throws Exception {
      //    long l1 = System.nanoTime();
      //    System.out.println("depth="+Healpix.depth((int) nside));
      final HealpixNested hn = Healpix.getNested(order);

      final HealpixNestedPolygonComputer pc = hn.newPolygonComputer();
      //   final double[][] vertices = cooList.toArray(new double[][]{{}});
      double[][] vertices = new double[cooList.size()][2];
      cooList.toArray(vertices);
      for (int i = 0; i < vertices.length; i++) {
         vertices[i][0] = Math.toRadians(vertices[i][0]);
         vertices[i][1] = Math.toRadians(vertices[i][1]);
         //      System.out.println(" "+vertices[i][0]+" "+vertices[i][1]);
      }
      final HealpixNestedBMOC bmoc = inclusive ? pc.overlappingCells(vertices) : pc.overlappingCenters(vertices);
      //   System.out.println("bmoc.size()="+bmoc.size());
      //   for (final CurrentValueAccessor cva : bmoc) {
      //      System.out.println(cva);
      //   }
      final long[] res = toFlatArrayOfHash(bmoc); 
      //   long l2 = System.nanoTime();
      //   System.err.println("Poygon FX computed in " + (l2 - l1) /  (1e6d) + " ms");
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
//   public static final double cPr = Math.PI / 180;
//
//
//   static private double dec(Pointing ptg) {
//      return (Math.PI*0.5 - ptg.theta) / cPr;
//   }
//
//   static private double ra(Pointing ptg) {
//      return ptg.phi / cPr;
//   }
//
//   static public Pointing pointing(double ra, double dec) {
//      return new Pointing( Math.PI/2 - (Math.PI/180)*dec , ra*cPr  );
//   }


   static final private int [] A = { 3, 2, 0, 1 };
   static public double[][] corners(int order,long npix) throws Exception {
      final HealpixNested hn = Healpix.getNested(order);
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

   static public double[][] borders(int order,long npix,int step) throws Exception {
      final HealpixNested hn = Healpix.getNested(order);
      final VerticesAndPathComputer vpc = hn.newVerticesAndPathComputer(); // For thread safety issues
      final double[][] res = vpc.pathAlongCellEdge(npix, Cardinal.N, false, step);
      for (final double[] lonlatRad : res) {
        lonlatRad[0] = Math.toDegrees(lonlatRad[0]);
        lonlatRad[1] = Math.toDegrees(lonlatRad[1]);
      }
      return res;
   }

   static public long[] neighbours(int order, long npix) throws Exception  {
      final HealpixNested hn = Healpix.getNested(order);
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
   
   public static void main(String [] arg) {
      int order = 3;
      long max = 12*pow2(order)*pow2(order);
      
      try {
         for( long npix=0; npix<max; npix++ ) {
            long ring = nest2ring(order,npix);
            long nest = ring2nest(order,ring);
            if( nest!=npix ) {
               System.out.println("J'ai un gros souci pour order="+order+"/"+npix
                     +" => ring="+ring+" nest="+nest);
               System.exit(1);
            }
         }
      } catch( Exception e ) { e.printStackTrace(); }
   }


   static public long nest2ring(int order, long npix) throws Exception  {
      final HealpixNested hn = Healpix.getNested(order);
      return hn.toRing(npix);
   }
   
   static public long ring2nest(int order, long npix) throws Exception  {
      final HealpixNested hn = Healpix.getNested(order);
      return hn.toNested(npix);
   }

   /** Voir Healpix documentation */
   static public double pixRes(int order) {
      double res = 0.;
      double degrad = Math.toDegrees(1.0);
      double skyArea = 4.*Math.PI*degrad*degrad;
      double arcSecArea = skyArea*3600.*3600.;
      long nside = pow2(order);
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
      double dec = (Math.PI/2. - polar[0])*180./Math.PI;
      radec[0] = polar[1]*180./Math.PI;
      radec[1] = dec;
      return radec;
   }
   
   /** Transforme si nécessaire les coordonnées pour être dans la plage des valeurs usuelles 
    * parce que la librairie de FX est tatillonne */
   static public double [] normalizeRaDec( double ra, double dec) {
      if( dec<-90 ) { dec = 180+dec; ra+= 180; }
      else if( dec>90 ) { dec = 180-dec; ra+=180; }
      if( ra>=360 ) ra-=360;
      else if( ra<0 ) ra+=360;
      return new double[] {ra,dec};
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
      final HealpixMoc hmoc = new HealpixMoc(order);
      hmoc.add(order, query_polygon( order, radecList, true));
      return hmoc;
   }
}

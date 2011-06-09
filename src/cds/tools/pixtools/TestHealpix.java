package cds.tools.pixtools;


//Thomas Boch and Pierre Fernique [CDS] HEALpix test methods
//=> used with jhealpixSmall_src package (Will O'Mullan)
//18 feb 2011


import healpix.core.HealpixIndex;
import healpix.core.base.set.LongRangeSet;
import healpix.tools.SpatialVector;

public class TestHealpix {
    public TestHealpix() {}

    public void testQueryDisc1() {
        System.out.println("\nTesting HealpixIndex.queryDisc (too many pixels returned)");
        try {
            int nside = 32;
            HealpixIndex hpxIdx = new HealpixIndex(nside);
            long ipix = 42;

            double[] coo = hpxIdx.pix2ang_nest(ipix);
            double theta = coo[0];
            double phi = coo[1];

            // (ra, dec) is at the center of the considered pixel
            double ra = Math.toDegrees(phi);
            double dec = Math.toDegrees(Math.PI/2-theta);
            System.out.println("ra,dec = "+ra+","+dec);

            // very small radius, so that we don't overlap neighbour pixels
            double radiusDeg = 0.02;
            double radius = Math.toRadians(radiusDeg);

            SpatialVector sv = new SpatialVector(ra, dec);
            System.out.println(hpxIdx.ang2pix_nest(theta, phi));
            LongRangeSet list = hpxIdx.queryDisc(sv, radius, 1, 1);
            // I retrieve 3 indexes whereas I should have only 1
            System.out.println("Pixels in disc: " + list);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void testQueryDisc2() {
       System.out.println("\nTesting HealpixIndex.queryDisc (missing pixels)");
       try {
           int nside = 8;
           HealpixIndex hpxIdx = new HealpixIndex(nside);
           double ra  = 359.53604;
           double dec = 84.23329;
           double radius = 2.062;
           System.out.println("ra,dec = "+ra+","+dec+" radius="+radius);
           radius = Math.toRadians(radius);
           
           SpatialVector sv = new SpatialVector(ra, dec);
           LongRangeSet list = hpxIdx.queryDisc(sv, radius, 1, 1);
           System.out.println("Pixels in disc: " + list);
           System.out.println("Missing: 63");

       } catch (Exception e) {
           e.printStackTrace();
           return;
       }
   }

    public void testQueryDisc3() {
       System.out.println("\nTesting HealpixIndex.queryDisc (missing and too many pixels)");
       try {
           int nside = 8;
           HealpixIndex hpxIdx = new HealpixIndex(nside);
           double ra  = 92.70735;
           double dec = 67.20376;
           double radius = 2.805;
           System.out.println("ra,dec = "+ra+","+dec+" radius="+radius);
           radius = Math.toRadians(radius);
           
           SpatialVector sv = new SpatialVector(ra, dec);
           LongRangeSet list = hpxIdx.queryDisc(sv, radius, 1, 1);
           System.out.println("Pixels in disc: " + list);
           System.out.println("Missing: 31 - Not required: 55, 123");

       } catch (Exception e) {
           e.printStackTrace();
           return;
       }
   }

    public void testGetChildren() {
        System.out.println("\nTesting HealpixIndex.getChildrenAt");
        try {
            long ipix = 42;
            int nside = 32;
            int requiredNside = 512;
            int nbChildren = (int)Math.pow(4, Math.log(requiredNside/nside)/Math.log(2));
            long[] children = HealpixIndex.getChildrenAt(nside, ipix, requiredNside);
            System.out.println("I'm expecting " + nbChildren + " children");
            System.out.println("I retrieve " + children.length + " children indexes");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCorners1() {
        System.out.println("\nTesting HealpixIndex.corners_nest nside=1");
        SpatialVector[] corners = null;
        try {
            int pix = 7;
            int nside = 1;
            corners = new HealpixIndex(nside).corners_nest(pix,1);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // first 3 points are aligned ...
        for(SpatialVector v:corners) System.out.println(v.ra()+" "+v.dec());
    }

    public void testCorners2() {
       System.out.println("\nTesting HealpixIndex.corners_nest for large NSIDE");
       SpatialVector[] corners = null;
       try {
          int nside = 2;
          long pix = 12;
          for( int i=nside; i<29; i++, nside *=2, pix *=4  ) {
             corners = new HealpixIndex(nside).corners_nest(pix,1);
             System.out.print("nside="+nside+" pix="+pix+" =>");
             for(SpatialVector v:corners) {
                double ra = (int)(v.ra()*1000)/1000.;
                double dec = (int)(v.dec()*1000)/1000.;
                System.out.print(" "+ra +(dec>=0?"+":"")+ dec);
             }
             System.out.println();
          }
       } catch (Exception e) {
          e.printStackTrace();
          return;
       }
   }

    public static void main(String[] args) {
        TestHealpix test = new TestHealpix();
        test.testQueryDisc1();
        test.testQueryDisc2();
        test.testQueryDisc3();
        test.testGetChildren();
        test.testCorners1();
        test.testCorners2();
    }
}

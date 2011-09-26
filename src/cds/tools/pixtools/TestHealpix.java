package cds.tools.pixtools;


//Thomas Boch and Pierre Fernique [CDS] HEALpix test methods
//=> used with jhealpixSmall_src package (Will O'Mullan)
//18 feb 2011


import healpix.core.HealpixIndex;
import healpix.core.base.set.LongRangeSet;
import healpix.tools.SpatialVector;

public class TestHealpix {
    public TestHealpix() {}
    
    private String getString(long [] x) {
       StringBuffer s = new StringBuffer();
       for( int i=0; i<x.length; i++ ) {
          if( s.length()>0 ) s.append(", ");
          s.append(x[i]+"");
       }
       return s.toString();
    }

    public void testQueryDisc1() {
        System.out.println("\ntestQueryDisc1: Testing query_disc (too many pixels returned)");
        try {
            int nside = 32;
            long ipix = 42;

            double [] x = CDSHealpix.pix2ang_nest(nside, ipix);
            double theta = x[0];
            double phi = x[1];

            // (ra, dec) is at the center of the considered pixel
            double ra = Math.toDegrees(phi);
            double dec = Math.toDegrees(Math.PI/2-theta);
            System.out.println("ra,dec = "+ra+","+dec);

            // very small radius, so that we don't overlap neighbour pixels
            double radiusDeg = 0.02;
            double radius = Math.toRadians(radiusDeg);

            
            System.out.println(CDSHealpix.ang2pix_nest(nside,theta,phi));
            long [] list = CDSHealpix.query_disc(nside,ra,dec,radius);
            // I retrieve 3 indexes whereas I should have only 1
            System.out.println("Pixels in disc: " + getString(list) );

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
    
    public void testQueryDisc2() {
       System.out.println("\ntestQueryDisc2: Testing query_disc (missing pixels)");
       try {
           int nside = 8;
           double ra  = 359.53604;
           double dec = 84.23329;
           double radius = 2.062;
           System.out.println("ra,dec = "+ra+","+dec+" radius="+radius);
           radius = Math.toRadians(radius);
           
           long [] list = CDSHealpix.query_disc(nside,ra,dec,radius);
           System.out.println("Pixels in disc: " + getString(list) );
           System.out.println("Missing: 63");

       } catch (Exception e) {
           e.printStackTrace();
           return;
       }
   }

    public void testQueryDisc3() {
       System.out.println("\ntestQueryDisc3: Testing query_disc (missing and too many pixels)");
       try {
           int nside = 8;
           double ra  = 92.70735;
           double dec = 67.20376;
           double radius = 2.805;
           System.out.println("ra,dec = "+ra+","+dec+" radius="+radius);
           radius = Math.toRadians(radius);
           
           long [] list = CDSHealpix.query_disc(nside,ra,dec,radius);
           System.out.println("Pixels in disc: " + getString(list) );
           System.out.println("Missing: 31 - Not required: 55, 123");

       } catch (Exception e) {
           e.printStackTrace();
           return;
       }
   }

    public void testGetChildren() {
        System.out.println("\ntestGetChildren: Testing HealpixIndex.getChildrenAt");
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
        System.out.println("\ntestCorners1: Testing corners nside=1");
        try {
            int pix = 7;
            int nside = 1;
            double [][] x  = CDSHealpix.corners(nside, pix);
            System.out.println("first 3 points are aligned ...");
            for( int i=0; i<x.length; i++ ) {
               System.out.println(x[i][0]+" "+x[i][1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void testCorners2() {
       System.out.println("\ntestCorners2: Testing corners for large NSIDE");
       try {
          long nside = 2;
          long pix = 12;
          for( long i=nside; i<=29; i++, nside *=2, pix *=4  ) {
             double [][] x  = CDSHealpix.corners(nside, pix);
             System.out.print(i+"/"+nside+": ");
             for( int j=0; j<x.length; j++ ) {
                double ra = x[j][0];
                double dec = x[j][1];
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

// Copyright 2011 - UDS/CNRS
// This Healpix MOC java class is distributed under the terms
// of the GNU General Public License version 3.
//

package cds.moc;

import healpix.core.HealpixIndex;

/** HEALPix CDS wrapper
 * Encapsulate the usage of the official HEALPix GAIA package
 * 
 * Only Galactic frame is supported.
 * The HEALPix ordering is always NESTED
 * 
 * @author Pierre Fernique [CDS]
 * @version May 2011
 */
public final class Healpix {
   
   /** Maximal HEALPix order supported by the library */
   static public final int MAXORDER = 29;

   /** Provide the HEALPix number associated to a galactic coord, for a given order
    * @param order HEALPix order [0..MAXORDER]
    * @param lon galactic longitude
    * @param lat galactic latitude
    * @return HEALPix number
    * @throws Exception
    */
   static public long ang2pix(int order,double lon, double lat) throws Exception {
      double theta = Math.PI/2. - lat/180.*Math.PI;
      double phi = lon/180.*Math.PI;
      initWillMode(order);
      return healpixIndex[order].ang2pix_nest(theta, phi);
   }
   
   /** Provide the galactic coord associated to an HEALPix number, for a given order
    * @param order HEALPix order [0..MAXORDER]
    * @param npix HEALPix number
    * @return galactic coord (lon,lat)
    * @throws Exception
    */
   static public double [] pix2ang(int order,long npix) throws Exception {
      initWillMode(order);
      double [] res = healpixIndex[order].pix2ang_nest(npix);
      double tmp = (Math.PI/2. - res[0])*180./Math.PI;
      res[0] = res[1]*180./Math.PI;
      res[1] = tmp;
      return res;
   }

   /** Code a couple (order,npix) into a unique long integer
    * @param order HEALPix order
    * @param npix HEALPix number
    * @return Uniq long ordering
    */
   static long hpix2uniq(int order, long npix) {
      long nside = pow2(order);
      return 4*nside*nside + npix;
   }
   
   /** Uncode a long integer into a couple (order,npix)
    * @param uniq Uniq long ordering
    * @return HEALPix order,number
    */
   static long [] uniq2hpix(long uniq) {
      return uniq2hpix(uniq,null);
   }
   
   /** Uncode a long integer into a couple (order,npix)
    * @param uniq Uniq long ordering
    * @param hpix null for reallocating target couple
    * @return HEALPix order,number
    */
   static long [] uniq2hpix(long uniq,long [] hpix) {
      if( hpix==null ) hpix = new long[2];
      hpix[0] = log2(uniq/4)/2;
      long nside = pow2(hpix[0]);
      hpix[1] = uniq - 4*nside*nside;
      return hpix;
   }
   
   /** Pixel area (in square degrees) for a given order */
   static public double getPixelArea(int order) {
      if( order<0 ) return SKYAREA;
      long nside = pow2(order);
      long npixels = 12*nside*nside;
      return SKYAREA/npixels;
   }
   
   /*********************** private stuff ***************************************************/
   
   static private HealpixIndex [] healpixIndex = null;
   
   static private int initWillMode(int order) throws Exception {
      if( healpixIndex==null ) healpixIndex = new HealpixIndex[MAXORDER];
      if( healpixIndex[order]!=null ) return order;
      healpixIndex[order] = new HealpixIndex((int)pow2(order) );
      return order;
   }
   
   static final private double SKYAREA = 4.*Math.PI*Math.toDegrees(1.0)*Math.toDegrees(1.0);
   public static final long pow2(long order){ return 1<<order;}
   public static final long log2(long nside){ int i=0; while((nside>>>(++i))>0); return --i; }
   
}

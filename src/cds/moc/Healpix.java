// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
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
//    along with Aladin Desktop.
//

package cds.moc;

import cds.healpix.FlatHashList;
import cds.healpix.HealpixNested;
import cds.healpix.NeighbourList;
import cds.tools.pixtools.CDSHealpix;

/** HEALPix CDS wrapper
 * Encapsulate the usage of the official HEALPix GAIA package
 *
 * The HEALPix ordering is always NESTED
 *
 * @author Pierre Fernique [CDS]
 * @version 1.4 Jan 2019 - use of Pineau Healpix lib
 * @version 1.3 May 2014 - NPIX => UNIQ fits keyword
 * @version 1.2 Jan 2012 - Thread safe implementation
 * @version 1.1 Oct 2011 - direct HealpixBase use
 * @version 1.0 May 2011
 */
public final class Healpix implements HealpixImpl {

   /** Provide the HEALPix number associated to a coord, for a given order
    * @param order HEALPix order [0..MAXORDER]
    * @param lon longitude (expressed in the Healpix frame)
    * @param lat latitude (expressed in the Healpix frame)
    * @return HEALPix number
    * @throws Exception
    */
   public long ang2pix(int order,double lon, double lat) throws Exception {
      double theta = Math.PI/2. - lat/180.*Math.PI;
      double phi = lon/180.*Math.PI;
      return CDSHealpix.ang2pix_nest(order, theta, phi);
   }

   /** Provide the spherical coord associated to an HEALPix number, for a given order
    * @param order HEALPix order [0..MAXORDER]
    * @param npix HEALPix number
    * @return coord (lon,lat) (expressed in the Healpix frame)
    * @throws Exception
    */
   public double [] pix2ang(int order,long npix) throws Exception {
      double lonlat[] = CDSHealpix.pix2ang_nest(order, npix);
      return new double[]{ lonlat[1]*180./Math.PI, (Math.PI/2. - lonlat[0])*180./Math.PI};
   }

   /** Provide the list of HEALPix numbers fully covering a circle (for a specified order)
    * @param order Healpix order
    * @param lon    center longitude (expressed in the Healpix frame)
    * @param lat    center latitude (expressed in the Healpix frame)
    * @param radius circle radius (in degrees)
    * @return
    * @throws Exception
    */
   public long [] queryDisc(int order, double lon, double lat, double radius) throws Exception {
      return CDSHealpix.query_disc(order, lon, lat, Math.toRadians(radius), true);
   }

   /*********************** Healpix tilities ***************************************************/

   /** Maximal HEALPix order supported by the library */
   static public final int MAXORDER = 29;

   static public final long pow2(long order){ return 1L<<order;}
   static public final long log2(long nside){ int i=0; while((nside>>>(++i))>0); return --i; }
   
   
   static public final double SKYAREA = 4.*Math.PI*Math.toDegrees(1.0)*Math.toDegrees(1.0);
   
   /** Pixel area (in square degrees) for a given HEALPix order */
   static public double getPixelArea(int order) {
      if( order<0 ) return SKYAREA;
      long nside = pow2(order);
      long npixels = 12L*nside*nside;
      return SKYAREA/npixels;
   }
   
   static public  long [] externalNeighbours(HealpixNested h, int order, long npix, int o) throws Exception {
      int deltaDepth = o-order;
      if( deltaDepth== 0 ) {
         NeighbourList nl = h.neighbours(npix);
         long [] n = new long[nl.size()];
         nl.arraycopy(0, n, 0, n.length);
         return n;
      }
      FlatHashList res = h.externalEdges(npix, deltaDepth);
      long [] neib = new long[ res.size() ];
      res.arraycopy(0, neib, 0, neib.length);
      return neib;
   }
   

}

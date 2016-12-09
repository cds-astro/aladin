// Copyright 2011 - UDS/CNRS
// The MOC API project is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of MOC API java project.
//
//    MOC API java project is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    MOC API java project is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with MOC API java project.
//


package cds.moc;

/** HEALPix Interface wrapper for Moc usage
 * Encapsulate the usage HEALPix
 * 
 * The HEALPix ordering is always NESTED
 * 
 * @author Pierre Fernique [CDS]
 * @version 1.0 dec 2011 - creation
 */
public interface HealpixImpl {
   
   /** Provide the HEALPix number associated to a coord, for a given order
    * @param order HEALPix order [0..MAXORDER]
    * @param lon longitude (expressed in the Healpix frame)
    * @param lat latitude (expressed in the Healpix frame)
    * @return HEALPix number
    * @throws Exception
    */
   public long ang2pix(int order,double lon, double lat) throws Exception;
   
   /** Provide the galactic coord associated to an HEALPix number, for a given order
    * @param order HEALPix order [0..MAXORDER]
    * @param npix HEALPix number
    * @return spherical coord (lon,lat) (expressed in the Healpix frame)
    * @throws Exception
    */
   public abstract double [] pix2ang(int order,long npix) throws Exception;
   
   /** Provide the list of HEALPix numbers fully covering a circle (for a specified order)
    * @param order Healpix order
    * @param lon    center longitude (expressed in the Healpix frame)
    * @param lat    center latitude (expressed in the Healpix frame)
    * @param radius circle radius (in degrees)
    * @return
    * @throws Exception
    */
   public long [] queryDisc(int order, double lon, double lat, double radius) throws Exception;

}

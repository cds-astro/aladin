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

package cds.moc;

import java.io.InputStream;

/** Extension of SpaceMoc for compatibility with deprecated HealpixMoc class
 *
 * @authors Pierre Fernique [CDS]
 * @version 1.0 April 2019 - creation (code are now in SpaceMoc)
 */
public class HealpixMoc extends SpaceMoc {


   public HealpixMoc() { super(); }
   
   public HealpixMoc( Moc smoc ) throws Exception {
      init("C",0,maxLimitOrder);
      spaceRange = ((SpaceMoc)smoc).spaceRange;
      toHealpixMoc();
   }

   public HealpixMoc(int maxLimitOrder) throws Exception {
      init("C",0,maxLimitOrder);
   }

   public HealpixMoc(int minLimitOrder,int maxLimitOrder) throws Exception {
      super(minLimitOrder,maxLimitOrder);
   }

   public HealpixMoc(String coordSys, int minLimitOrder,int maxLimitOrder) throws Exception {
      super(coordSys,minLimitOrder,maxLimitOrder);
   }
   
   public HealpixMoc(Range r) throws Exception { super(r); }

   public HealpixMoc(String s) throws Exception { super(s); }

   public HealpixMoc(InputStream in) throws Exception { super(in); }

   public HealpixMoc(InputStream in, int mode) throws Exception { super(in, mode); }


   /** @deprecated see getMaxLimitOrder() */
   public int getLimitOrder() { return getMaxLimitOrder(); }

   /** @deprecated see setMaxLimitOrder() */
   public void setLimitOrder(int limitOrder) throws Exception { setMaxLimitOrder(limitOrder); }

   /** Specify the coordinate system (HEALPix convention: G-galactic, C-Equatorial, E-Ecliptic)
    * @deprecated Standard MOC must be equatorial
    */
   public void setCoordSys(String coordSys) {
      this.coordSys=coordSys;
      property.put("COORDSYS", coordSys);
   }

   /** Fast test for checking if the HEALPix cell is intersecting
    * the current MOC object
    * @deprecated see isIntersecting(...)
    */
   public boolean isInTree(int order,long npix) { return isIntersecting(order,npix); }

   /** Fast test for checking if the parameter MOC is intersecting
    * the current MOC object
    * @deprecated see isIntersecting(...)
    */
   public boolean isInTree(HealpixMoc moc) { return isIntersecting(moc); }

    /** Check if the spherical coord is inside the MOC. The coordinate system must be compatible
    * with the MOC coordinate system.
    * @param alpha in degrees
    * @param delta in degrees
    * @return true if the coordinates is in one MOC pixel
    * @throws Exception
    */
   public boolean contains(HealpixImpl healpix,double alpha, double delta) throws Exception {
      int order = getMaxOrder();
      if( order==-1 ) return false;
      long npix = healpix.ang2pix(order, alpha, delta);
      if( level[order].find( npix )>=0 ) return true;
      if( isDescendant(order,npix) ) return true;
      return false;
   }

   /**
    * Provide Moc pixels totally or partially inside a circle
    * @param alpha circle center (in degrees)
    * @param delta circle center (in degrees)
    * @param radius circle radius (in degrees)
    * @return an HealpixMox containing the list of pixels
    * @throws Exception
    */
   public SpaceMoc queryDisc(HealpixImpl healpix,double alpha, double delta,double radius) throws Exception {
      int order = getMaxOrder();
      long [] list = healpix.queryDisc(order, alpha, delta, radius);
      HealpixMoc mocA = new HealpixMoc(coordSys,minLimitOrder,maxLimitOrder);
      mocA.add(order,list);
      return (SpaceMoc)intersection(mocA);
   }
}

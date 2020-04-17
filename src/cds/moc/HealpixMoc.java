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

import java.io.InputStream;

/** Extension of SMoc for compatibility with old HealpixMoc class
 * Use SMoc rather than this class.
 * This class should be used only for HEALpix specific actions on SMoc
 *
 * @authors Pierre Fernique [CDS]
 * @version 2.1 Feb 2020 - cleaning
 * @version 2.0 April 2019 - full refactoring (code are now in SMoc)
 */
public class HealpixMoc extends SMoc {
   
   public HealpixMoc(Moc smoc) throws Exception { super(smoc); }

   /** @deprecated */
   public HealpixMoc() { super(); }
   /** @deprecated */
   public HealpixMoc(int maxLimitOrder) throws Exception { super(maxLimitOrder); }
   /** @deprecated */
   public HealpixMoc(int minLimitOrder,int maxLimitOrder) throws Exception { super("C",minLimitOrder,maxLimitOrder); }
   /** @deprecated */
   public HealpixMoc(String coordSys, int minLimitOrder,int maxLimitOrder) throws Exception { super(coordSys,minLimitOrder,maxLimitOrder); }
   /** @deprecated */
   public HealpixMoc(String s) throws Exception { super(s); }
   /** @deprecated */
   public HealpixMoc(InputStream in) throws Exception { super(in); }
   /** @deprecated */
   public HealpixMoc(InputStream in, int mode) throws Exception { super(in,mode); }
   
   
   /** Check if the spherical coord is inside the MOC. The coordinate system must be compatible
    * with the MOC coordinate system.
    * @param alpha in degrees
    * @param delta in degrees
    * @return true if the coordinates is in one MOC pixel
    * @throws Exception
    */
   public boolean contains(HealpixImpl healpix,double alpha, double delta) throws Exception {
      int order = getMaxUsedOrder();
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
   public SMoc queryDisc(HealpixImpl healpix,double alpha, double delta,double radius) throws Exception {
      int order = getMaxUsedOrder();
      long [] list = healpix.queryDisc(order, alpha, delta, radius);
      SMoc mocA = new SMoc(sys,minOrder,mocOrder);
      mocA.add(order,list);
      return (SMoc)intersection(mocA);
   }
}

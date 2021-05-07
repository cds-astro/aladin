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

import java.io.InputStream;

/** HEALPix Multi Order Coverage Map (MOC)
 * => DEPRECATED CLASS => Instead, use SMoc
 * THIS CLASS IS ONLY PROVIDED FOR COMPATIBILITY => TAKE THE TIME TO REWRITE YOUR CODE BY USING SMoc
 * 
 * This object provides read, write and process methods to manipulate an HEALPix Multi Order Coverage Map (MOC)
 * A MOC is used to define a sky region by using HEALPix sky tesselation
 *
 * @authors Pierre Fernique [CDS]
 * 
 * @version 6.0 apr 2021 - Wrapper to SMoc => new cds.moc package
 * @version 5.0 Sept 2017 - JSON and ASCII full support, add(order,long[]) + add(order,Collection<Lon>), missing check npix over max limit bug fix
 * @version 4.8 July 2017 - isEmpty(), isIncluding(..) methods
 * @version 4.7 Dec 2016 - Undeprecated new HealpicMoc(Inputstream in, int mode) + isAscendant(int order, Array a) bug fix
 * @version 4.6 Apr 2016 - MocLint - IVOA 1.0 MOC recommendation compatibility checker
 * @version 4.5 Nov 2015 - JSON #MOCORDER patch
 * @version 4.4 Jun 2015 - Empty MOC FITS bug fix
 * @version 4.2 oct 2014 - setMinLimitOrder() bug fix
 * @version 4.1 nov 2013 - pixelIterator 4.0 bug fix
 * @version 4.0 sep 2013 - upgrade for MOC WD 1.0 1 sept 2013 compliance
 * @version 3.4 oct 2012 - operations by RangeSet
 * @version 3.3 July 2012 - PixelIterator() addition (low level pixel iterator)
 * @version 3.2 April 2012 - JSON ASCII support (the previous basic ASCII format is still supported)
 * @version 3.2 March 2012 - union, intersection,... improvements + refactoring isIntersecting(...)
 * @version 3.1 Dec 2011 - check()
 * @version 3.0 Dec 2011 - 1) Use HealpixInterface 2) replace unicityTest by testConsistency 3)code cleaning
 * @version 2.0 Oct 2011 - use of short, int and long, creation of MocIO class...
 * @version 1.3 Sept 2011 - Support for delete
 * @version 1.2 Sept 2011 - COORDSYS support
 * @version 1.1 Sept 2011 - used sorted MOC (speed improvement)
 * @version 1.0 June 2011 - first stable version
 * @version 0.9 May 2011 - creation
 */
public class HealpixMoc extends SMoc {

   /** @deprecated HEALPix Multi Order Coverage Map (MOC) creation */
   public HealpixMoc() { super(); }

   /** @deprecated Moc Creation with a specified max limitOrder */
   public HealpixMoc(int maxLimitOrder) throws Exception {
      super();
      setMocOrder(maxLimitOrder);
   }

   /** @deprecated Moc Creation with a specified min and max limitOrder (by default 0..29) */
   public HealpixMoc(int minLimitOrder,int maxLimitOrder) throws Exception {
      super();
      setMocOrder(maxLimitOrder);
      setMinOrder(minLimitOrder);
   }

   /** @deprecated Moc Creation with a specified min and max limitOrder (by default 0..29) */
   public HealpixMoc(String coordSys, int minLimitOrder,int maxLimitOrder) throws Exception {
      super();
      setMocOrder(maxLimitOrder);
      setMinOrder(minLimitOrder);
      setSys(coordSys);
   }

   /** @deprecated HEALPix Multi Order Coverage Map (MOC) creation and initialisation */
   public HealpixMoc(String s) throws Exception {
      super();
      add(s);
   }

   /** @deprecated HEALPix Multi Order Coverage Map (MOC) creation and initialisation via a stream */
   public HealpixMoc(InputStream in) throws Exception {
      super();
      read(in);
   }

   /** @deprecated HEALPix Multi Order Coverage Map (MOC) creation and initialisation
    * via a stream, either in JSON encoded format , ASCII encoded format or in FITS encoded format */
   public HealpixMoc(InputStream in, int mode) throws Exception {
      super();
      read(in,mode);
   }

   /** @deprecated Set the Min limit order supported by the Moc (by default 0) */
   public void setMinLimitOrder(int limitOrder) throws Exception { setMinOrder(limitOrder); }

   /** Set the limit order supported by the Moc */
   public void setMaxLimitOrder(int limitOrder) throws Exception { setMocOrder(limitOrder); }

   /** @deprecated Provide the minimal limit order supported by the Moc (by default 0) */
   public int getMinLimitOrder() { return getMinOrder(); }

   /** @deprecated Provide the limit order supported by the Moc */
   public int getMaxLimitOrder() { return getMocOrder(); }

   /** @deprecated see getMaxLimitOrder() */
   public int getLimitOrder() { return getMocOrder(); }

   /** @deprecated see setMaxLimitOrder() */
   public void setLimitOrder(int limitOrder) throws Exception { setMocOrder(limitOrder); }

   /** @deprecated Provide the number of Healpix pixels (for all MOC orders) */
   public int getSize() { return getNbCells(); }

   /** @deprecated  Provide the greatest order really used by the MOC */
   public int getMaxOrder() { return getMocOrder(); }

   /** @deprecated Set the check consistency flag. */
   public void setCheckConsistencyFlag(boolean flag) throws Exception { 
      if( flag ) bufferOn();
      else bufferOff();
   }
   
   /** @deprecated Check and fix the consistency of the moc */
   public void checkAndFix() throws Exception { flush(); }

   /** @deprecated  Add directly a full Moc. */
   public void add(HealpixMoc moc) throws Exception { super.add(moc); }
   
   /** @deprecated Sort each level of the Moc */
   public void sort() { }

   /** @deprecated Return true if all Moc level is sorted */
   public boolean isSorted() { return true; }

   /** @deprecated Fast test for checking if the HEALPix cell is intersecting */
   public boolean isInTree(int order,long npix) { return isIntersecting(order,npix); }

   /** @deprecated Fast test for checking if the parameter MOC is intersecting */
   public boolean isInTree(HealpixMoc moc) { return isIntersecting(moc); }

   /** @deprecated Store the MOC as a RangeSet if not yet done */
   public void toRangeSet() { }

   /** @deprecated Generate the HealpixMoc tree structure from the rangeSet */
   public void toHealpixMoc() throws Exception { }
 
   /** @deprecated Return true if the MOC covers the whole sky */
   public boolean isAllSky() { return isFull(); }
}

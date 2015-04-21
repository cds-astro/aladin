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

/** MOC cell object
 * 
 * @author Pierre Fernique [CDS]
 * @version 1.0 dec 2011 - creation
 */
public class MocCell {
   
   /** Healpix cell order */
   public int order;
   
   /** Healpix cell value */
   public long npix;
   
   /** Create Healpix cell */
   public MocCell() {};
   
   /**
    * Create Healpix cell
    * @param order cell order (ie log2(Healpix NSIDE) )
    * @param npix cell value (ie Healpix value)
    */
   public MocCell(int order,long npix) {
      this.order=order;
      this.npix=npix;
   }
   
   /** Order getter */
   public int getOrder() { return order; }
   
   /** Value getter */
   public long getNpix() { return npix; }
   
   /** Order setter */
   public void setOrder(int order) { this.order = order; }
   
   /** Value setter */
   public void setNpix(long npix) { this.npix= npix; }
   
   /** Couple setter */
   public void set(int order,long npix) {
      this.order=order;
      this.npix=npix;
   }
   
   public String toString() { return order+"/"+npix; }
   
}

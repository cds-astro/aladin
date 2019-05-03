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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

/** Multi Order Coverage Map (MOC)
 * This object provides read, write and process methods to manipulate a Multi Order Coverage Map (MOC)
 * A MOC is used to define a sky region by using HEALPix sky tesselation
 *
 * @authors Pierre Fernique [CDS]
 * @version 1.0 April 2019 - Refactoring
 */
public abstract class Moc implements Iterable<MocCell>,Cloneable,Comparable<Moc>  {

   /** Healpix MOC API version number */
   static public final String VERSION = "6.0";

   /** FITS encoding format (IVOA REC 1.0 compliante) */
   static public final int FITS  = 0;

   /** JSON encoding format (IVOA REC 1.0 suggestion) */
   static public final int JSON  = 1;

   /** ASCII encoding format (IVOA REC 1.0 suggestion) */
   static public final int ASCII = 2;
   
   /** JSON obsoleted encoding format (only reading supported for compatibility) */
   static public final int JSON0 = 3;

   /** Maximal HEALPix order supported by the library */
   static public final int MAXORDER = 29;

   abstract protected int getType();
   
   abstract public Moc clone();
   
   protected HashMap<String, String> property; // MOC properties

   /** Clear the MOC */
   abstract public void clear();

   /** Set the limit order supported by the Moc   */
   abstract public void setMocOrder(int order) throws Exception;

   /** Provide the MOC order. By default 29 */
   abstract public int getMocOrder();

   /** Return approximatively the memory used for this moc (in bytes) */
   abstract public long getMem();

   /** Return the number of elements */
   abstract public int getSize();
   
   /** Add a list of MOC pixels provided in a string format (JSON format or basic ASCII format)
    * ex JSON:          { "order1":[npix1,npix2,...], "order2":[npix3...] }
    * ex basic ASCII:   order1/npix1-npix2 npix3 ... order2/npix4 ...
    * Note : The string can be submitted in several times. In this case, the insertion will use the last current order
    * Note : in JSON, the syntax is not checked ( in fact {, [ and " are ignored)
    * @see setCheckUnicity(..)
    */
   abstract public void add(String s) throws Exception;

   /** Add directly a full Moc.
    * Note: The MOC consistency is checked and possibly fixed at the end of the insertion process
    * @param moc The Moc to be added
    */
   abstract public void add(Moc moc) throws Exception;

   /** Check and fix the consistency of the moc */
   abstract public void check() throws Exception;
         
   /** MOC propertie setter */
   abstract public void setProperty(String key, String value) throws Exception;

   /** Provide MOC property value. */
   public String getProperty(String key) {
      return property.get(key);
   }
  
   abstract public String toASCII() throws Exception;

   /** Fast test for checking if the parameter MOC is intersecting
    * the current MOC object
    * @return true if the intersection is not null
    */
   abstract public boolean isIntersecting(Moc moc);
   
   /** Fast test for checking if the parameter MOC is totally inside
    * the current MOC object
    * @return true if MOC is totally inside
    */
//   abstract public boolean isIncluding(Moc moc);

   abstract public Moc union(Moc moc) throws Exception;
   abstract public Moc intersection(Moc moc) throws Exception;
   abstract public Moc subtraction(Moc moc) throws Exception;
   
   /** Return true if the MOC is empty */
   abstract public boolean isEmpty();
   
   /** Remove extra space */
   abstract public void trim();
   
   /** Provide an Iterator on the MOC pixel List. Each Item is a couple of longs,
    * the first long is the order, the second long is the pixel number */
   abstract public Iterator<MocCell> iterator();
   
   // TO BE CLEAN
   abstract public int getSize( int order);
   abstract public Array getArray( int order);
   abstract public void setCurrentOrder(int order);
   abstract public void setCoordSys(String s);
   abstract public void addHpix(String s) throws Exception;
   abstract public boolean add( int order, long npix) throws Exception;
   abstract public int getMaxOrder();
   abstract public String getCoordSys();
   abstract public void setCheckConsistencyFlag(boolean flag) throws Exception;
   abstract public void toHealpixMoc() throws Exception;

   /*************************** read and write *******************************************************/

   /** Read HEALPix MOC from a file.
    * Support all MOC syntax: FITS standard or any other old syntax (JSON or ASCII)
    * @param filename file name
    * @throws Exception
    */
   public void read(String filename) throws Exception {
      (new MocIO(this)).read(filename);
   }

   /** Read HEALPix MOC from a file.
    * @param filename file name
    * @param mode ASCII, JSON, FITS encoded format
    * @throws Exception
    * @deprecated see read(String)
    */
   public void read(String filename,int mode) throws Exception {
      (new MocIO(this)).read(filename,mode);
   }

   /** Read HEALPix MOC from a stream
    * Support all MOC syntax: FITS standard or any other old syntax (JSON or ASCII)
    */
   public void read(InputStream in) throws Exception { (new MocIO(this)).read(in); }

   /** Read HEALPix MOC from a stream.
    * @param in input stream
    * @param mode ASCII, JSON, FITS encoded format
    * @throws Exception
    * @deprecated see read(InputStream)
    */
   public void read(InputStream in,int mode) throws Exception {
      (new MocIO(this)).read(in,mode);
   }

   /** @deprecated see read(InputStream) */
   public void readASCII(InputStream in) throws Exception {
      (new MocIO(this)).read(in,ASCII);
   }

   /** @deprecated see read(InputStream) */
   public void readJSON(InputStream in) throws Exception {
      (new MocIO(this)).read(in,JSON);
   }

   /** Read HEALPix MOC from an Binary FITS stream
    * @deprecated see read(InputStream)
    */
   public void readFits(InputStream in) throws Exception {
      (new MocIO(this)).read(in,FITS);
   }

   /** Write HEALPix MOC to a file
    * @param filename name of file
    */
   public void write(String filename) throws Exception {
      check();
      (new MocIO(this)).write(filename);
   }

   /** Write HEALPix MOC to a file
    * @param filename name of file
    * @param mode encoded format (JSON or FITS)
    * @deprecated
    */
   public void write(String filename,int mode) throws Exception {
      check();
      (new MocIO(this)).write(filename,mode);
   }

   /** Write HEALPix MOC to an output stream
    * @param out output stream
    * @param mode encoded format (JSON or FITS)
    * @deprecated
    */
   public void write(OutputStream out,int mode) throws Exception {
      check();
      (new MocIO(this)).write(out,mode);
   }

   /** Write HEALPix MOC to an output stream IN JSON encoded format
    * @param out output stream
    * @deprecated see write(OutputStream)
    *
    */
   public void writeASCII(OutputStream out) throws Exception {
      writeJSON(out);
   }

   /** Write HEALPix MOC to an output stream IN JSON encoded format
    * @param out output stream
    */
   public void writeJSON(OutputStream out) throws Exception {
      check();
      (new MocIO(this)).writeJSON(out);
   }

   /** Write HEALPix MOC to an output stream in FITS encoded format
    * @param out output stream
    */
   public void writeFits(OutputStream out) throws Exception { writeFITS(out); }

   /** deprecated */
   public void writeFITS(OutputStream out) throws Exception {
      check();
      (new MocIO(this)).writeFits(out);
   }

   abstract protected int writeSpecificFitsProp( OutputStream out  ) throws Exception;
   abstract protected int writeSpecificData(OutputStream out) throws Exception;
   abstract protected void readSpecificData( InputStream in, int naxis1, int naxis2, int nbyte, cds.moc.MocIO.HeaderFits header) throws Exception;

   /***************************************  Utilities **************************************************************/

   /** Code a couple (order,npix) into a unique long integer
    * @param order HEALPix order
    * @param npix HEALPix number
    * @return Uniq long ordering
    */
   static public long hpix2uniq(int order, long npix) {
      long nside = pow2(order);
      return 4*nside*nside + npix;
   }

   /** Uncode a long integer into a couple (order,npix)
    * @param uniq Uniq long ordering
    * @return HEALPix order,number
    */
   static public long [] uniq2hpix(long uniq) {
      return uniq2hpix(uniq,null);
   }

   /** Uncode a long integer into a couple (order,npix)
    * @param uniq Uniq long ordering
    * @param hpix null for reallocating target couple
    * @return HEALPix order,number
    */
   static public long [] uniq2hpix(long uniq,long [] hpix) {
      if( hpix==null ) hpix = new long[2];
      hpix[0] = log2(uniq/4)/2;
      long nside = pow2(hpix[0]);
      hpix[1] = uniq - 4*nside*nside;
      return hpix;
   }

   static public final long pow2(long order){ return 1<<order;}
   static public final long log2(long nside){ int i=0; while((nside>>>(++i))>0); return --i; }

   @Override
   public int compareTo(Moc o) {
      if( o==null ) return 1;
      return getSize()-o.getSize();
   }
   
}

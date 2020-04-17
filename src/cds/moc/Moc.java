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



import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

/** Multi Order Coverage Map (MOC)
 * This object provides read, write and process methods to manipulate a Multi Order Coverage Map (MOC)
 * A MOC is used to define a Coverage in space (by using HEALPix sky tesselation) 
 * and time (by using JD discretization)
 * See IVOA MOC Standard document (http://www.ivoa.net)
 * 
 * Order    Space res.   Time resolution
 * 0      58.63°     9133y 171d 11h 22m 31.711744s
 * 1      29.32°     2283y 134d 4h 20m 37.927936s
 * 2      14.66°     570y 307d 11h 35m 9.481984s
 * 3      7.329°     142y 259d 11h 53m 47.370496s
 * 4      3.665°     35y 247d 11h 58m 26.842624s
 * 5      1.832°     8y 335d 19h 29m 36.710656s
 * 6      54.97'     2y 83d 22h 52m 24.177664s
 * 7      27.48'     203d 14h 43m 6.044416s
 * 8      13.74'     50d 21h 40m 46.511104s
 * 9      6.871'     12d 17h 25m 11.627776s
 * 10     3.435'     3d 4h 21m 17.906944s
 * 11     1.718'     19h 5m 19.476736s
 * 12     51.53"     4h 46m 19.869184s
 * 13     25.77"     1h 11m 34.967296s
 * 14     12.88"     17m 53.741824s
 * 15     6.442"     4m 28.435456s
 * 16     3.221"     1m 7.108864s
 * 17     1.61"      16.777216s
 * 18     805.2mas   4.194304s
 * 19     402.6mas   1.048576s
 * 20     201.3mas   262.144ms
 * 21     100.6mas   65.536ms
 * 22     50.32mas   16.384ms
 * 23     25.16mas   4.096ms
 * 24     12.58mas   1.024ms
 * 25     6.291mas   256µs
 * 26     3.145mas   64µs
 * 27     1.573mas   16µs
 * 28     786.3µas   4µs
 * 29     393.2µas   1µs
 *
 * @authors Pierre Fernique [CDS]
 * @version 1.0 April 2019 - Refactoring
 */
public abstract class Moc implements Iterable<MocCell>,Cloneable,Comparable<Moc>  {

   /** MOC API version number */
   static public final String VERSION = "7.0";

   /** FITS encoding format (IVOA REC 1.0 compliante) */
   static public final int FITS  = 0;

   /** JSON encoding format (IVOA REC 1.0 suggestion) */
   static public final int JSON  = 1;

   /** ASCII encoding format (IVOA REC 1.0 suggestion) */
   static public final int ASCII = 2;
   
   /** Maximal HEALPix order supported by the library */
   static public final int MAXORDER = 29;
   
   /** MOC Properties */
   protected HashMap<String, String> property;
   
   /** Generic MOC factory. Recognize the MOC ASCII string and create the associated space,
    * time or space-time MOC.
    * @param s MOC string => ex: SMOC:3/1-4... TMOC:t29/3456-6788... STMOC:t27/... s29/...
    * @return a MOC
    */
   static public Moc createMoc(String s) throws Exception {
      if( s==null ) throw new Exception("null string");
      if( s.charAt(0)=='t' ) {
         if( s.indexOf('s')<0 ) return new TMoc(s.substring(1));
         return new STMoc(s);
      }
      if( s.charAt(0)=='s' ) s=s.substring(1);
      return new SMoc(s);
   }
   
   abstract protected int getType();
   
   abstract public Moc clone();
   
   /** Clear the MOC */
   abstract public void clear();

   /** Set the limit order supported by the Moc   */
   abstract public void setMocOrder(int order) throws Exception;

   /** Provide the MOC order. By default 29 */
   abstract public int getMocOrder();

   /** Provide the Time MOC order. By default 29 */
   abstract public int getTimeOrder();

   /** Provide the Space MOC order. By default 29 */
   abstract public int getSpaceOrder();
   
   abstract public void setTimeOrder(int order) throws Exception;

   abstract public void setSpaceOrder(int order) throws Exception;
   
   abstract public boolean isSpace();

   abstract public boolean isTime();

   /** Retourne la composante spatiale du MOC */
   abstract public SMoc getSpaceMoc() throws Exception;

   /** Retourne la composante temporelle du MOC */
   abstract public TMoc getTimeMoc() throws Exception;

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
   
   /** Accretion of the coverage by 1 cell of the MOC order around all the perimeter */
   abstract public void accretion() throws Exception;
   
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
   
   /** Return true if the MOC is full */
   abstract public boolean isFull();
   
   /** Provide an Iterator on the MOC pixel List. Each Item is a couple of longs,
    * the first long is the order, the second long is the pixel number */
   abstract public Iterator<MocCell> iterator();
   
   abstract public Range getRange();
   abstract public String getSys();
   abstract public int getMaxUsedOrder();
   abstract public void setCheckConsistencyFlag(boolean flag) throws Exception;
   abstract public void toMocSet() throws Exception;
   abstract public void trim();
   public String toString() { try { return toASCII(); } catch( Exception e) { return null; } }
   
   abstract protected void addHpix(String s) throws Exception;
   abstract protected void setCurrentOrder(int order);

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
    * @param mode encoded format (ASCII, JSON or FITS)
    */
   public void write(String filename,int mode) throws Exception {
      check();
      (new MocIO(this)).write(filename,mode);
   }

   /** Write HEALPix MOC to an output stream
    * @param out output stream
    * @param mode encoded format (ASCII, JSON or FITS)
    */
   public void write(OutputStream out,int mode) throws Exception {
      check();
      (new MocIO(this)).write(out,mode);
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

   public String toASCII() throws Exception {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      writeASCII(out);
      return out.toString();
   }

   
   /***************************************  ASCII writers *************************************************/

   static protected final int MAXWORD=20;
   static protected final int MAXSIZE=80;
   static protected String CR = System.getProperty("line.separator");
   
   /** Write HEALPix MOC to an output stream IN ASCII encoded format
    * @param out output stream
    */
   public void writeASCII(OutputStream out) throws Exception {
      boolean flagNL = getRange().sz>MAXWORD;
      int order = writeASCII(out, getRange(), flagNL );
      
      // Ajout de la resolution max si nécessaire, et d'un CR si nécessaire
      StringBuilder res= new StringBuilder(10);
      int mocOrder = getMocOrder();
      if( order<mocOrder ) {
         if( flagNL ) res.append(CR);
         else res.append(' ');
         res.append(mocOrder+"/");
      } 
      if( flagNL ) res.append('\n');

      // Dernier flush
      writeASCIIFlush(out,res,false);
   }
   
   static protected int writeASCII(OutputStream out, Range range, boolean flagNL) throws Exception {
      if( range.isEmpty() ) return -1;
      StringBuilder res= new StringBuilder(50000);
      int order=-1;
      int sizeLine=0;
      int j=0;

      Range r2 = new Range( range );
      Range r3 = new Range();
      for( int o=0; o<=Healpix.MAXORDER; o++) {
         if( r2.isEmpty() ) break;
         int shift = 2*(Healpix.MAXORDER-o);
         long ofs=(1L<<shift)-1;
         r3.clear();
         int nranges = r2.sz;
         for( int i=0; i<nranges; i+=2 ) {
            long a = (r2.r[i]+ofs) >>> shift;
            long b = r2.r[i+1] >>> shift;
            if( a>=b ) continue;
            r3.append(a<<shift, b<<shift);
            
            // Le token qu'on doit ajouter
            StringBuilder s = new StringBuilder(100);
            if( o!=order ) { 
               s.append(o+"/"); 
               order=o; 
            }
            s.append(a+"");
            if( b>a+1 ) s.append("-"+(b-1));

            // Traitement du retour à la ligne et d'un éventuel flush
            if( res.length()>0 ) {
               if( o!=order ) {
                  if( flagNL ) { res.append(CR); sizeLine=0; j++; }
                  else res.append(" ");
               } else {
                  if( flagNL && s.length()+sizeLine>MAXSIZE ) { res.append(CR+" "); sizeLine=1; j++; }
                  else { res.append(' '); sizeLine++; }
               }
               if( j>15) { writeASCIIFlush(out,res,false); j=0; }
            }

            res.append(s);
            sizeLine+=s.length();

         }
         if( !r3.isEmpty() ) r2 = r2.difference(r3);
      }

      // Dernier flush
      writeASCIIFlush(out,res,false);
      
      return order;
   }

   static protected void writeASCIIFlush(OutputStream out, StringBuilder s) throws Exception {
      writeASCIIFlush(out, s, true);
   }
   
   static protected void writeASCIIFlush(OutputStream out, StringBuilder s,boolean nl) throws Exception {
      if( nl ) s.append(CR);
      out.write(s.toString().getBytes());
      s.delete(0,s.length());
   }

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

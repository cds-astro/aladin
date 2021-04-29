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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

/**
 * Multi Order Coverage Map (MOC)
 * This class provides read, write and process methods to manipulate a Multi Order Coverage Map (MOC).
 * A MOC is used to define a Coverage in space, time, etc, or combination of these physical dimensions
 * 
 * See:  IVOA MOC 2.0 standard => https://www.ivoa.net/documents/MOC/
 * 
 * This abstract class Moc describes or implements the methods generic to MOCs, 
 * whatever their type (spatial, temporal, spatio-temporal, etc).
 * This class is derived in 2 classes: Moc1D and Moc2D.
 * Moc1D is a generic class for 1 physical dimensional MOCs, which is derived into SMOC (spatial MOCs) and TMoc (temporal MOCs). 
 * Moc2D is a generic class for 2-dimensional physical MOCs, which is derived into STMOC (space-time MOC)
 * 
 * Warning: The cds.moc package has been completely revised/recoded when the IVOA MOC2.0 standard was published (2021). 
 * the HealpixMoc class is provided only to ensure compatibility with old software. It is only a wrapper to
 * the new SMoc class that it is recommended to use instead. Note that some low level methods existing in HealpixMoc
 * have not been reimplemented (cell lists by order)
 * 
 * This package manipulates and stores MOCs only as a list of ranges (unlike its predecessor 
 * which used both a hierarchical and an range architecture). 
 * This refactoring was done to allow easy extension to MOCs covering other physical dimensions 
 * (currently only SPACE, TIME or TIME.SPACE)
 * It uses code and algorithm initially developed by Jan Kotek, then refactored/extended by M.Reinecker, 
 * and re-extended for the specific needs of 2D MOCs (see Range and Range2 classes)
 * 
 * Examples of uses are available in the class cds.moc.examples. 
 * The class cds.moc.MocLint implements methods to validate the conformity of the binary or ASCII serialization 
 * of a MOC with the IVOA MOC 2.0 standard as well as its previous versions (1.1 and 1.0).
 * 
 * @author Pierre Fernique [CDS]
 * @version 10.0 - April 2021 - full refactoring
 * @version 0.9 to 5 - 2011 to 2017 - predecessors
 *
 */
public abstract class Moc implements Iterable<MocCell>, Cloneable, Comparable<Moc>{
   
   /** MOC API version number */
   static public final String VERSION = "10.0";

   /** MOC serialization formats */
   static private final int UNKNOWN  = -1;         // Formar inconnu
   static public final int FITS      = 0;         // FITS format
   static public final int ASCII     = 1;         // ASCII format
   static public final int JSON      = 2;         // JSON format (suggested in IVOA REC)

   protected int cacheNbCells;            // Number if cells in hierarchical view (-1 if unknown -> see getNbCells();
   protected int cacheDeepestOrder;       // Hierarchical deepest required order, -1 if undefined
   protected int cacheHashCode;           // Last computed hashCode
   
   /** MOC Properties and comments */
   protected LinkedHashMap<String, String> property;   // List of properties associated to the Moc (key -> property)
   protected HashMap<String, String> comment;          // Comment associated to each properties (key -> comment)
   
   
   /******************************************** Factories & global parameters ***************************************************/
   
   static final public int LOGIC_MIN = 0;
   static final public int LOGIC_MAX = 1;
   static private int mocOrderLogic = LOGIC_MAX;
   
   /** Get the current mocOrderLogic applied for operations (see setMocOrderLogic()) */
   static public int getMocOrderLogic() { return mocOrderLogic; }
   
   /** Set the current mocOrderLogic applied for operations: Default LOGIC_MAX
    * LOGIC_MAX: MOC result for operations is returned with the Max orders of the 2 operandes => preserving area logic
    * LOGIC_MIN: MOC result for operations is returned with the Min orders of the 2 operandes => preserving observation logic
    * See IVOA 2.0 document
    * @param logic LOGIC_MIN or LOGIC_MAX
    */
   static public void setMocOrderLogic(int logic) { mocOrderLogic=logic; }  
   
   /** Generic MOC factory. Recognize the MOC ASCII string and create the associated space,
    * time or space-time MOC.
    * @param s MOC string => ex: SMOC:3/1-4... TMOC:t29/3456-6788... STMOC:t27/... s29/...
    * @return a MOC
    */
   static public Moc createMoc(String s) throws Exception {
      if( s==null ) throw new Exception("Null string MOC");
      if( s.length()==0 ) throw new Exception("Empty string MOC");
      
      Moc moc;
      
      // Soit un TMoc ou un STMoc
      if( s.charAt(0)=='t' ) {
         
         // Pas de dimension spatiale => alors un TMOC
         if( s.indexOf('s')<0 ) {
            moc = new TMoc();
            s = s.substring(1);

         // Sinon un STMoc
         } else moc = new STMoc();
         
      // Sinon un SMoc
      } else {
         moc = new SMoc();         
         if( s.charAt(0)=='s' ) s=s.substring(1);
      }
      
      moc.add( s );
      return moc;
   }
   
   /*************************************************** Pour la migration **********************************************************/
   
   public void setSpaceOrder( int order ) throws Exception {
      if( this instanceof SMoc )  ((SMoc)this).setMocOrder(order);
      else if( this instanceof STMoc ) ((STMoc)this).setSpaceOrder(order);
      else throw new Exception("No Space dimension");
   }
   
   public void setTimeOrder( int order ) throws Exception {
      if( this instanceof TMoc )  ((TMoc)this).setMocOrder(order);
      else if( this instanceof STMoc ) ((STMoc)this).setTimeOrder(order);
      else throw new Exception("No Time dimension");
   }
   
   public int getSpaceOrder() {
      try {
         if( this instanceof SMoc )  return ((SMoc)this).getMocOrder();
         if( this instanceof STMoc ) return ((STMoc)this).getSpaceOrder();
      } catch( Exception e ) { }
      return -1;
   }
   
   public int getTimeOrder() {
      try {
         if( this instanceof TMoc ) return ((TMoc)this).getMocOrder();
         if( this instanceof STMoc ) return ((STMoc)this).getTimeOrder();
      } catch( Exception e ) { }
      return -1;
   }
   
   public boolean isSpace() { return this instanceof SMoc || this instanceof STMoc; }
   public boolean isTime()  { return this instanceof TMoc || this instanceof STMoc; }
   
   /*************************************************** Main methods **********************************************************/
   
   public Moc() {
      property = new LinkedHashMap<>();
      comment = new HashMap<>();
      clear();
   }
   
   /** Clone Moc (deep copy) */
   public abstract Moc clone() throws CloneNotSupportedException;
   
   /** Create and instance of same class, same sys, but no data nor mocorder*/
   public abstract Moc dup();
   
   public String toString() {
      try { return toASCII(); }
      catch( Exception e) { return null; }
   }
      
   public String toDebug() {
      String s = toString();
      if( s.length()>80 ) s = s.substring(0,76)+"...";
      s=s.replace(Moc1D.CR," ");
      return s;
   }
   
   /** Add a list of MOC elements provided in a string format (ASCII format or JSON format)
    * ex basic ASCII:   order1/npix1-npix2 npix3 ... order2/npix4 ...
    * ex JSON:          { "order1":[npix1,npix2,...], "order2":[npix3...] }
    * Note : The string can be submitted in several times. In this case, the insertion will use the last current order
    * Note : in JSON, the syntax is not checked ( in fact {, [ and " are ignored)
    */
   public void add(String s) throws Exception {
      if( s==null || s.length()==0 ) return;
      StringTokenizer st = new StringTokenizer(s," ;,\n\r\t{}");
      while( st.hasMoreTokens() ) {
         String s1 = st.nextToken();
         if( s1.length()==0 ) continue;
         addToken(s1);
      }
      addToken(null);
   }
   
   /** Clear the MOC - data only (not the properties, nor the mocOrder) */
   public void clear() {
      cacheNbCells=-1;
      cacheDeepestOrder=-1;
      cacheHashCode=-1;
   }
   
   /** Return true if the Moc is empty (no coverage) */
   public abstract boolean isEmpty();
   
   /** Return true if the Moc is full (full coverage) */
   public abstract boolean isFull();
   
   /** Return the coverage pourcentage of the Moc */
   public abstract double getCoverage();
   
   /** Return approximatively the amount of memory used for storing this MOC in RAM (in bytes) */
   public abstract long getMem();

   /** Return the hierarchical deepest required order - slow process, uses a cache  */ 
   public int getDeepestOrder() { 
      if( cacheDeepestOrder==-1 ) computeHierarchy();
      return cacheDeepestOrder;
   }
   
   /** Comparator. Based on Moc size */
   public int compareTo(Moc o) {
      if( o==null ) return 1;
      return getNbRanges()-o.getNbRanges();
   }
   
   /** Return the number of ranges */
   public abstract int getNbRanges();
   
   /** Set the list of ranges - Warning: no copy */
   public abstract void setRangeList( Range range );
   
   /** Acces to the list of ranges (no copy) */
   public abstract Range seeRangeList();
   
   /** Provide an Iterator on the MOC cell List (hierarchy view for Moc1D, and range highest order view for Moc2D)
    * @param flagRange true for getting range rather than all individual values
    * @return mocCell => dim,order,startVal,endVal,Moc1D
    */
   public Iterator<MocCell> iterator() { return cellIterator( false ); }
   public abstract Iterator<MocCell> cellIterator( boolean flagRange );
   
   /** Deep copy */
   protected void clone1( Moc moc )  throws CloneNotSupportedException {
      moc.property = (LinkedHashMap<String, String>)property.clone();
      moc.comment = (HashMap<String, String>)comment.clone();
      moc.cacheNbCells = cacheNbCells;
      moc.cacheDeepestOrder=cacheDeepestOrder;
   }
   
   /** Internal usage: Add one token element according to the format "[s|t]order/npix[-npixn]".
    * If the order is not mentioned, use the last used order (currentOrder)
    * Note: Also support JSON non standard IVOA syntax
    * @param token one token (ex: s18/23-45)
    */
   protected abstract void addToken( String token ) throws Exception ;
   
   /** Return resulting order for operations. see setMocOrderLogic() */
   protected int getMocOrder4op(int m1, int m2) {
      if( m1==-1 ) return m2;
      if( m2==-1 ) return m1;
      if( mocOrderLogic==LOGIC_MAX ) return Math.max( m1, m2);
      return Math.min( m1, m2);
   }
   
   /** Return the number of Moc cells (hierarchy Moc view) - slow process, uses a cache */
   public int getNbCells() {
      if( cacheNbCells==-1 ) computeHierarchy();
      return cacheNbCells;
   }
   
   /******************************************* Internal methods *************************************************/
   
   /** Recalculates the metrics associated with the MOC hierarchical view: 
    * the number of hierarchical cells, the deepest order used... */
   protected  void resetCache() {
      cacheNbCells=-1;
      cacheDeepestOrder=-1;
      cacheHashCode=-1;
   }

   /** Recalculates the metrics associated with the MOC hierarchical view: 
    * the number of hierarchical cells, the deepest order used... */
   protected abstract void computeHierarchy();

   /********************************************  Prototypes *******************************************************/
   
   public void accretion() throws Exception {}
   
   /***************************************************** Properties ******************************************/
   
   /** MOC propertie setter */
   public void setProperty(String key, String value) throws Exception { setProperty(key,value,null); }
   public void setProperty(String key, String value, String comment) throws Exception {
      if( key.length()>8 ) throw new Exception("Property error: Too long property key word (<= 8 characters) ["+key+"]");
      for( char c : key.toCharArray() ) {
         if( !(c>='A' && c<='Z' || c>='0' && c<='9' || c=='-' || c=='_')  ) 
            throw new Exception("Property error: keyword character not allowd (only 'A'–'Z'|'0'–'9'|'-'|'_')  ["+key+"]");
      }
      if( isReservedFitsKeyWords(key) ) throw new Exception("Property error: Reserved FITS key word ["+key+"]");
      property.put(key,value);
      if( comment==null ) this.comment.remove(key);
      else this.comment.put(key,comment);
   }

   /** Provide the list of MOC property keys*/
   public String [] getPropertyKeys() {
      String[] a = new String[property.size()];
      int i=0;
      for( String key : property.keySet() ) { a[i++]=key; }
      return a;
   }
   
   /** Provide MOC property value. */
   public String getProperty(String key) { return property.get(key); }
   
   /** Provide MOC property comment. */
   public String getComment(String key) { return comment.get(key); }
   
   
   /***************************************************** Operations ******************************************/
   
   /** Return the Union with another Moc */
   public Moc union(Moc moc)        throws Exception { return operation(moc,0); }
   
   /** Return the Intersection with another Moc */
   public Moc intersection(Moc moc) throws Exception { return operation(moc,1); }
   
   /** Return the subtraction with another Moc */
   public Moc subtraction(Moc moc)  throws Exception { return operation(moc,2); }
   
   /** Return the difference with another Moc (not in A & not in B)*/
   public Moc difference(Moc moc) throws Exception {
      Moc inter = intersection(moc);
      Moc union = union(moc);
      return union.subtraction(inter);
   }
   
   /** Return the complement */
   public abstract Moc complement() throws Exception;

   /** Generic operations: 0-union, 1-intersection, 2-subtraction */
   protected abstract Moc operation(Moc moc, int op) throws Exception;
   
   
   /*************************************************************** I/O *****************************************************/
   
   
   /** Read MOC from a file.
    * Support standard FITS, ASCII and non standard JSON alternative
    * @param filename file name
    * @throws Exception
    */
   public void read(String filename) throws Exception {
      File f = new File(filename);
      FileInputStream fi = null;
      BufferedInputStream bf = null;
      try {
         fi = new FileInputStream(f);
         bf = new BufferedInputStream(fi);
         read( bf );
      } finally {
         if( bf!=null ) bf.close();
         else if( fi!=null ) fi.close();
      }
   }

   /** Read MOC from a stream.
    * Support standard FITS, ASCII and non standard JSON alternative
    * @param in input stream (not closed at the end)
    * @throws Exception
    */
   public void read(InputStream in) throws Exception { read(in,UNKNOWN);  }
   public void read(InputStream in, int mode) throws Exception {

      // Auto-detection du format ?
      if( mode==UNKNOWN ) {
         if( !in.markSupported() ) throw new Exception("Moc stream with no mark/reset sopport. The MOC format must be specified");

         // Read the first charactere for deciding FITS or ASCII, and reset the stream
         in.mark(10);
         byte [] b = new byte[1];
         in.read(b);
         in.reset();
         mode = b[0]=='S' ? FITS : ASCII;             // Same reader for ASCII and JSON
      }
      
      if( mode==FITS ) readFITS(in);
      else readASCII(in); 
   }

   /** Read MOC from an JSON stream */
   public void readJSON(InputStream in) throws Exception { readASCII(in); }
   
   /** Read MOC from an ASCII stream */
   public void readASCII(InputStream in) throws Exception {
      clear();
      BufferedReader dis = new BufferedReader(new InputStreamReader(in));
      String s;
      while( (s=dis.readLine())!=null ) {
         if( s.length()==0 ) continue;
         if( s.charAt(0)=='#' ) continue;
         add(s);
      }
      add(null);   // nécessaire, notamment pour STMOC
   }
   
   /** Read MOC from an Binary FITS stream */
   public void readFITS(InputStream in) throws Exception {
      clear();
      HeaderFits header = new HeaderFits();
      header.readHeader(in);   // HDU 0
      try {
         header.readHeader(in);  // HDU 1
         int naxis1 = header.getIntFromHeader("NAXIS1");
         int naxis2 = header.getIntFromHeader("NAXIS2");
         String tform = header.getStringFromHeader("TFORM1");
         int nbyte= tform.indexOf('K')>=0 ? 8 : tform.indexOf('J')>=0 ? 4 : -1;   // entier 64 bits, sinon 32
         if( nbyte<=0 ) throw new Exception("Multi Order Coverage Map only requieres integers (32bits or 64bits)");
         
         // Store Fits Properties
         for( String k : header ) {
            if( isReservedFitsKeyWords(k) ) continue;
            setProperty(k, header.getStringFromHeader(k) );
         }

         readSpecificData( in, naxis1,naxis2, nbyte, header);
      } catch( EOFException e ) { }
   }
   
   /** Internal method: read FITS data according to the type of MOC.
    * @param in The input stream
    * @param naxis1 size of FITS row (in bytes) (generally ==nbyte, but may be 1024 for buffering)
    * @param naxis2 number of values
    * @param nbyte size of each value (in bytes)
    * @param header HDU1 header
    * @throws Exception
    */
   protected abstract void readSpecificData(InputStream in, int naxis1, int naxis2, int nbyte, HeaderFits header) throws Exception;

   /** Write MOC in FITS binary serialization */
   public void write(String filename) throws Exception { write(filename,FITS); }
   
   /** Write MOC in FITS binary serialization */
   public void writeFITS(String filename) throws Exception { write(filename,FITS); }
   
   /** Write MOC in ASCII serialization */
   public void writeASCII(String filename) throws Exception { write(filename,ASCII); }
   
   /** Write MOC in JSON serialization (non IVOA standard) */
   public void writeJSON(String filename) throws Exception { write(filename,JSON); }

   
   /** Write MOC to a file
    * @param filename name of file
    * @param mode encoded format (FITS, ASCII or JSON)
    */
   public void write(String filename,int mode) throws Exception {
      if( mode!=FITS && mode!=ASCII && mode!=JSON ) throw new Exception("Unknown MOC format !");
      File f = new File(filename);
      if( f.exists() ) f.delete();
      FileOutputStream fo = null;
      BufferedOutputStream fb = null;

      try {
         fo = new FileOutputStream(f);
         fb = new BufferedOutputStream(fo);
         write(fb,mode);
      } finally {
         if( fb!=null ) fb.close();
         else if( fo!=null ) fo.close();
      }
   }
   
   /** Write MOC to a stream
    * @param output stream (not closed at the end)
    * @param mode encoded format (FITS, ASCII or JSON)
    */
   public void write(OutputStream out,int mode) throws Exception {
      if( mode==FITS ) writeFITS(out);
      else if( mode==JSON  ) writeJSON(out);
      else if( mode==ASCII ) writeASCII(out);
      else throw new Exception("Unknown MOC format !");
   }

   /** Write MOC to an output stream in binary serialization */
   public void write(OutputStream out) throws Exception { writeFITS(out); }

   /** Write MOC to an output stream in bASCII serialization */
   public abstract void writeASCII(OutputStream out) throws Exception;
   
   /** Write MOC to an output stream in JSON serialization (non IVOA standard) */
   public abstract void writeJSON(OutputStream out) throws Exception;    

   /** Write MOC to an output stream in binary FITS serialization */
   public void writeFITS(OutputStream out) throws Exception {
      writeHeader0(out);
      writeHeader1(out);
      writeData(out);
      
   }
   
      
   /******************************************** Utils  *************************************************************************/

   static protected String CR = System.getProperty("line.separator");
   
   /** Remove quotes if required ("xxx" -> xxx) */
   protected String unQuote(String s) {
      int n=s.length();
      if( n>2 && s.charAt(0)=='"' && s.charAt(n-1)=='"' ) return s.substring(1,n-1);
      return s;
   }

   /** Remove brackets if required ([xxx, xxx] or [xxx] -> xxx) */
   protected String unBracket(String s) {
      int n=s.length();
      if( n<1 ) return s;
      int o1 = s.charAt(0)=='[' ? 1:0;
      int o2 = s.charAt(n-1)==']' ? n-1 : n;
      return s.substring(o1,o2);
      
   }
   
   /******************************************** ASCII writers  ***************************************************************/

   // ASCII paging (number of tokens and/or characters per line
   static protected final int MAXWORD=20;
   static protected final int MAXSIZE=80;
   
   /** Return Moc ASCII string */
   public String toASCII() throws Exception {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      writeASCII(out);
      return out.toString();
   }

   /** Return Moc JSON string (non IVOA standard) */
   public String toJSON() throws Exception {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      writeJSON(out);
      return out.toString();
   }
   
   /**
    * Internal method: Write ASCII Moc (1D) in an output stream
    * @param out output stream
    * @param moc Moc to write
    * @param flagNL false for avoiding automatic NL
    * @param flagRange false for avoiding range expressions (ex: 32-35 => 32 33 34 35)
    * @return the highest order found 
    * @throws Exception
    */
   static protected int writeASCII(OutputStream out, Moc1D moc, boolean flagNL, boolean flagRange) throws Exception {
      if( moc.isEmpty() ) return -1;
      StringBuilder res= new StringBuilder(50000);
      int order=-1;
      int sizeLine=0;
      int j=0;
      
      Iterator<MocCell> it = moc.cellIterator( flagRange );
      while( it.hasNext() ) {
         MocCell cell = it.next();
         
         StringBuilder s = new StringBuilder(100);
         
         // Next order ?
         boolean flagNewOrder = cell.order!=order;
         if( flagNewOrder ) { s.append(cell.order+"/"); order=cell.order; }
         s.append(cell.start+"");
         
         // Range ?
//         if( cell.end>cell.start+1 ) s.append( (cell.end-2==cell.start ? " ": "-") + (cell.end-1) ); 
         if( cell.end>cell.start+1 ) s.append( "-" + (cell.end-1) );  // PF: wee add '-' even between to consecutive cell
         
         // New line and a possible flush ?
         if( res.length()>0 ) {
            if( flagNewOrder ) {
               if( flagNL ) { res.append(CR); sizeLine=0; j++; }
               else res.append(" ");
            } else {
               if( flagNL && s.length()+sizeLine>MAXSIZE ) { res.append(CR+" "); sizeLine=1; j++; }
               else { res.append(' '); sizeLine++; }
// PF: Alternative with comma separator rather than space separator
//               if( flagNL && s.length()+sizeLine>MAXSIZE ) { res.append(CR+", "); sizeLine=1; j++; }
//               else { res.append(','); sizeLine++; }
            }
            if( j>15) { writeASCIIFlush(out,res,false); j=0; }
         }

         res.append(s);
         sizeLine+=s.length();
      }

      // Dernier flush
      writeASCIIFlush(out,res,false);

      return order;
   }
   
   /**
    * Internalm method: Flush the StringBuilder in the output stream. At the end, the StringBuilder is clear to be reused
    * @param out The output Stream
    * @param s the stringBuilder to flush
    * @param nl true if a NL is inserted before
    * @throws Exception
    */
   static protected void writeASCIIFlush(OutputStream out, StringBuilder s,boolean nl) throws Exception {
      if( nl ) s.append(CR);
      out.write(s.toString().getBytes());
      s.delete(0,s.length());
   }
   
   /******************************************** FITS writers  ***************************************************************/
   
   /** Return the number of bytes used for coding each FITS value (4 for integer, 8 for long) */
   public abstract int sizeOfCoding();
   
   /** Return the number of values to write in FITS serialization */
   public abstract int getNbCoding();
   
   /** Write the primary FITS HDU */
   private void writeHeader0(OutputStream out) throws Exception {
      int n=0;
      out.write( getFitsLine("SIMPLE","T","Written by MOC java API "+VERSION) ); n+=80;
      out.write( getFitsLine("BITPIX","8") ); n+=80;
      out.write( getFitsLine("NAXIS","0") );  n+=80;
      out.write( getFitsLine("EXTEND","T") ); n+=80;
      out.write( getEndBourrage(n) );
   }
   
   /** Standardized MOC FITS keyword list (IVOA MOC 2.0) */
   private String [] FITSKEY_RESERVED = { 
         "SIMPLE","BITPIX","NAXIS","EXTEND","XTENSION","NAXIS1","NAXIS2","PCOUNT","GCOUNT","TFIELDS","TFORM1" };
   
   private String [] MOCKEY_RESERVED = { 
         "MOCVERS","MOCDIM","ORDERING","COORDSYS","TIMESYS","MOCORDER", "MOCORD_S","MOCORD_T","MOCTOOL" };
  
   /** Return true if k is a reserved FITS keywords (tables keywords) */
   private boolean isReservedFitsKeyWords(String k) {
      for( String s : FITSKEY_RESERVED ) if( s.equalsIgnoreCase(k) ) return true;
      return false;
   }

   /** Return true if k is a reserved FITS keywords (MOC keywords) */
   private boolean isReservedMocKeyWords(String k) {
      for( String s : MOCKEY_RESERVED ) if( s.equalsIgnoreCase(k) ) return true;
      return false;
   }

   /** Write the second FITS HDU */
   private void writeHeader1(OutputStream out) throws Exception {
      int n=0;
      int nbytes = sizeOfCoding();
      int naxis2 = getNbCoding();
      out.write( getFitsLine("XTENSION","BINTABLE","Multi Order Coverage map") ); n+=80;
      out.write( getFitsLine("BITPIX","8") ); n+=80;
      out.write( getFitsLine("NAXIS","2") );  n+=80;
      out.write( getFitsLine("NAXIS1",nbytes+"") );  n+=80;
      out.write( getFitsLine("NAXIS2",""+naxis2 ) );  n+=80;
      out.write( getFitsLine("PCOUNT","0") ); n+=80;
      out.write( getFitsLine("GCOUNT","1") ); n+=80;
      out.write( getFitsLine("TFIELDS","1") ); n+=80;
      out.write( getFitsLine("TFORM1",nbytes==4 ? "1J" : "1K") ); n+=80;
      
      out.write( getFitsLine("MOCVERS","2.0","MOC version") );    n+=80;      
      n+=writeSpecificFitsProp( out );
      out.write( getFitsLine("MOCTOOL","CDSjavaAPI-"+SMoc.VERSION,"Name of the MOC generator") );    n+=80;      

      
      // Write properties
      for( String key : getPropertyKeys() ) {
         if( isReservedFitsKeyWords(key) ) continue;
         if( isReservedMocKeyWords(key) ) continue;
         String value = getProperty(key);
         String comment = getComment(key);
         out.write( getFitsLine(key,value,comment) );
         n+=80;
      }
      
      out.write( getEndBourrage(n) );
   }
   
   /** Write specifical properties (depends of the Moc dimension:l SMOC, TMOC, STMOC...) */
   protected abstract int writeSpecificFitsProp( OutputStream out) throws Exception;

   /** Write data FITS section */
   protected void writeData(OutputStream out) throws Exception {
      int size = writeSpecificData( out);
      out.write( getBourrage(size) );
   }
   
   /** Write data (depends of the Moc dimension:l SMOC, TMOC, STMOC...) */
   protected abstract int writeSpecificData( OutputStream out) throws Exception;


   /** Write a val (int or long) in the outputstream out, using the buffer buf. The size of the buf determines int or long
    * @param out output stream
    * @param val value to write 
    * @param buf buffer to use
    * @return the number of bytes written
    * @throws Exception
    */
   static protected int writeVal(OutputStream out,long val,byte []buf) throws Exception {
      for( int j=0,shift=(buf.length-1)*8; j<buf.length; j++, shift-=8 ) buf[j] = (byte)( 0xFF & (val>>shift) );
      out.write( buf );
      return buf.length;
   }
   
   /** Convert 8 consecutive bytes as long (starting at the index i)
    * @param t array
    * @param i offset
    * @return long decoded
    */
   static protected long readLong(byte t[], int i) {
      int a = ((  t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF;
      int b = ((t[i+4])<<24) | (((t[i+5])&0xFF)<<16) | (((t[i+6])&0xFF)<<8) | (t[i+7])&0xFF;
      long val = (((long)a)<<32) | (b & 0xFFFFFFFFL);
      return val;
   }

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

   static public final long pow2(long order){ return 1L<<order;}
   static public final long log2(long nside){ int i=0; while((nside>>>(++i))>0); return --i; }


   /****************** Utilitaire Fits **************************/

   /** Generate FITS 80 character line => see getFitsLine(String key, String value, String comment) */
   private byte [] getFitsLine(String key, String value) {
      return getFitsLine(key,value,null);
   }

   /**
    * Generate FITS 80 character line.
    * @param key The FITS key
    * @param value The associated FITS value (can be numeric, string (quoted or not)
    * @param comment The commend, or null
    * @return the 80 character FITS line
    */
   static protected byte [] getFitsLine(String key, String value, String comment) {
      int i=0,j;
      char [] a;
      byte [] b = new byte[80];

      // The keyword
      a = key.toCharArray();
      for( j=0; i<8; j++,i++) b[i]=(byte)( (j<a.length)?a[j]:' ' );

      // The associated value
      if( value!=null ) {
         b[i++]=(byte)'='; b[i++]=(byte)' ';

         a = value.toCharArray();

         // Numeric value => right align
         if( !isFitsString(value) ) {
            for( j=0; j<20-a.length; j++)  b[i++]=(byte)' ';
            for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte)a[j];

            // string => format
         } else {
            a = formatFitsString(a);
            for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte)a[j];
            while( i<30 ) b[i++]=(byte)' ';
         }
      }

      // The comment
      if( comment!=null && comment.length()>0 ) {
         if( value!=null ) { b[i++]=(byte)' ';b[i++]=(byte)'/'; b[i++]=(byte)' '; }
         a = comment.toCharArray();
         for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte) a[j];
      }

      // Bourrage
      while( i<80 ) b[i++]=(byte)' ';

      return b;
   }

   /** Generate the end of a FITS block assuming a current block size of headSize bytes
    * => insert the last END keyword */
   private byte [] getEndBourrage(int headSize) {
      int size = 2880 - headSize%2880;
      if( size<3 ) size+=2880;
      byte [] b = new byte[size];
      b[0]=(byte)'E'; b[1]=(byte)'N';b[2]=(byte)'D';
      for( int i=3; i<b.length; i++ ) b[i]=(byte)' ';
      return b;
   }

   /** Generate the end of a FITS block assuming a current block size of headSize bytes */
   static protected byte [] getBourrage(int currentPos) {
      int size = 2880 - currentPos%2880;
      byte [] b = new byte[size];
      return b;
   }

   /** Fully read buf.length bytes from in input stream */
   static public void readFully(InputStream in, byte buf[]) throws IOException {
      readFully(in,buf,0,buf.length);
   }

   /** Fully read len bytes from in input stream and store the result in buf[]
    * from offset position. */
   static public void readFully(InputStream in,byte buf[],int offset, int len) throws IOException {
      int m;
      for( int n=0; n<len; n+=m ) {
         m = in.read(buf,offset+n,(len-n)<512 ? len-n : 512);
         if( m==-1 ) throw new EOFException();
      }
   }

   /**
    * Test si c'est une chaine à la FITS (ni numérique, ni booléen)
    * @param s la chaine à tester
    * @return true si s est une chaine ni numérique, ni booléenne
    * ATTENTION: NE PREND PAS EN COMPTE LES NOMBRES IMAGINAIRES
    */
   static private boolean isFitsString(String s) {
      if( s.length()==0 ) return true;
      char c = s.charAt(0);
      if( s.length()==1 && (c=='T' || c=='F') ) return false;   // boolean
      if( !Character.isDigit(c) && c!='.' && c!='-' && c!='+' ) return true;
      try {
         Double.valueOf(s);
         return false;
      } catch( Exception e ) { return true; }
   }

   static private char [] formatFitsString(char [] a) {
      if( a.length==0 ) return a;
      StringBuffer s = new StringBuffer();
      int i;
      boolean flagQuote = a[0]=='\''; // Chaine déjà quotée ?

      s.append('\'');

      // recopie sans les quotes
      for( i= flagQuote ? 1:0; i<a.length- (flagQuote ? 1:0); i++ ) {
         if( !flagQuote && a[i]=='\'' ) s.append('\'');  // Double quotage
         s.append(a[i]);
      }

      // bourrage de blanc si <8 caractères + 1ère quote
      for( ; i< (flagQuote ? 9:8); i++ ) s.append(' ');

      // ajout de la dernière quote
      s.append('\'');

      return s.toString().toCharArray();
   }


   /** Manage Header Fits */
   class HeaderFits implements Iterable<String>{

      private LinkedHashMap<String,String> header;     // List of header key/value
      private int sizeHeader=0;                    // Header size in bytes
      
      /** Pick up FITS value from a 80 character array
       * @param buffer line buffer
       * @return Parsed FITS value
       */
      private String getValue(byte [] buffer) {
         int i;
         boolean quote = false;
         boolean blanc=true;
         int offset = 9;

         for( i=offset ; i<80; i++ ) {
            if( !quote ) {
               if( buffer[i]==(byte)'/' ) break;   // on the comment
            } else {
               if( buffer[i]==(byte)'\'') break;   // on the next quote
            }

            if( blanc ) {
               if( buffer[i]!=(byte)' ' ) blanc=false;
               if( buffer[i]==(byte)'\'' ) { quote=true; offset=i+1; }
            }
         }
         return (new String(buffer, 0, offset, i-offset)).trim();
      }

      /** Pick up FITS key from a 80 character array
       * @param buffer line buffer
       * @return Parsed key value
       */
      private String getKey(byte [] buffer) {
         return new String(buffer, 0, 0, 8).trim();
      }

      /** Parse FITS header from a stream until next 2880 FITS block after the END key.
       * Memorize FITS key/value couples
       * @param dis input stream
       */
      private void readHeader(InputStream dis) throws Exception {
         int blocksize = 2880;
         int fieldsize = 80;
         String key, value;
         int linesRead = 0;
         sizeHeader=0;

         header = new LinkedHashMap<>(200);
         byte[] buffer = new byte[fieldsize];

         while (true) {
            readFully(dis,buffer);

            key =  getKey(buffer);
            if( linesRead==0 && !key.equals("SIMPLE") && !key.equals("XTENSION") ) throw new Exception("Not a MOC FITS format");
            sizeHeader+=fieldsize;
            linesRead++;
            if( key.equals("END" ) ) break;
            if( buffer[8] != '=' ) continue;
            value=getValue(buffer);
            header.put(key, value);
         }

         // Skip end of last block
         int bourrage = blocksize - sizeHeader%blocksize;
         if( bourrage!=blocksize ) {
            byte [] tmp = new byte[bourrage];
            readFully(dis,tmp);
            sizeHeader+=bourrage;
         }
      }

      /**
       * Provide integer value associated to a FITS key
       * @param key FITs key (with or without trailing blanks)
       * @return corresponding integer value
       */
      public int getIntFromHeader(String key) throws NumberFormatException,NullPointerException {
         String s = header.get(key.trim());
         return (int)Double.parseDouble(s.trim());
      }

      /**
       * Provide string value associated to a FITS key
       * @param key FITs key (with or without trailing blanks)
       * @return corresponding string value without quotes (')
       */
      public String getStringFromHeader(String key) throws NullPointerException {
         String s = header.get(key.trim());
         if( s==null || s.length()==0 ) return s;
         if( s.charAt(0)=='\'' ) return s.substring(1,s.length()-1).trim();
         return s;
      }
      
      public Iterator<String> iterator() {
         return header.keySet().iterator();
      }
   }
}


package cds.moc;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The TMoc class implements the methods specific to temporal MOCs.
 * It is based on the JD discretization of the time at the µs.
 * See:  IVOA MOC 2.0 standard => https://www.ivoa.net/documents/MOC/
 * @author Pierre Fernique [CDS]
 * @version 1.0 - April 2021 - creation
 *
 */
public class TMoc extends Moc1D {
   
   static public final double DAYMICROSEC = 86400000000.;

   static final public int  MAXORD_T = 61;      // Max order (<=> resolution at 1 µs)
   static final public int  FACT_T   = 2;       // Factor between two consecutive order
   static final public char DIM_T    = 't';     // Char signature for TMoc
   static final public long NBVAL_T  = pow2( MAXORD_T );   //  // nb of cells at the deepest order

   /** Return the deepest possible order (ex: 29 for SMoc, 61 for TMoc) */
   public final int  maxOrder()    { return MAXORD_T; }
   
   /** Return the number of bit shifting between two consecutive orders (ex: 2 for SMoc, 1 for TMoc) */
   public final int  shiftOrder()  { return FACT_T/2; }
   
   /** Return the Moc signature character (ex: 's' for SMoc, 't' for TMOC) */
   public final char cDim()   { return DIM_T; }
   
   /** Return the number of values at the deepest order (ex: 2x2^29x2^29 for SMoc, 2^61 for TMoc) */
   public final long maxVal() { return NBVAL_T; }


   public TMoc() { super(); }
   public TMoc( int mocOrder ) { super( mocOrder ); }
   public TMoc( String s ) throws Exception { super(s); }
   public TMoc( TMoc moc ) throws Exception { super( moc ); }
   public TMoc( InputStream in ) throws Exception { super(in); }
   
   /** Clone Moc (deep copy) */
   public TMoc clone() throws CloneNotSupportedException {
      TMoc moc = dup();
      clone1( moc );
      return moc;
   }
   
   /** Deep copy - internal method */
   protected void clone1( Moc moc ) throws CloneNotSupportedException {
      if( !(moc instanceof TMoc) ) throw new CloneNotSupportedException("Uncompatible type of MOC for clone. Must be TMoc");
      super.clone1( moc );
   }
   
   /** Create and instance of same class (no data but same mocOrder), but no data nor properties) */
   public TMoc dup() {
      TMoc moc = new TMoc();
      moc.mocOrder = mocOrder;
      return moc;
   }
   
   /** Return the number of bytes used for coding each FITS value (8 for long) */
   public int sizeOfCoding() { return 8; }
   
   /** Return the number of values to write in FITS serialization */
   public int getNbCoding() { return range.sz; }
   
   /** Add directly a TMoc */
   public void add(Moc moc) throws Exception { 
      if( !(moc instanceof TMoc) ) throw new Exception("Uncompatible Moc for adding");
      super.add(moc);
   }
   

   /************************************ TMoc specifical methods => JD discritization *****************************************/
   
   
   /** Compute the microsec from JD=0 from a date jd (in JD unit=day decimal) and an offset (in JD unit=day) */
   static public long getMicrosec(double jd, long offset) {
      long micron = (long)(jd*DAYMICROSEC);
      return micron + (offset*86400000000L);
   }
   
   /** Add JD range
    * @param jdmin start time (in JD - unit=day) - included in the range
    * @param jdmax end time (in JD - unit=day) - included in the range
    */
   public void add(double jdmin, double jdmax) throws Exception {
      long min = (long)(jdmin*DAYMICROSEC);
      long max = (long)(jdmax*DAYMICROSEC);
      add( MAXORD_T, min,max);
   }
   /** True if the jd date is in TMOC */
   public  boolean contains(double jd) {
      long val = (long)( jd * DAYMICROSEC );
      return range.contains(val);
   }
   
   /** Return minimal time in JD - -1 if empty*/
   public double getTimeMin() {
      if( isEmpty() ) return -1;
      return range.begins(0) / DAYMICROSEC;
   }
   
   /** Return maximal time in JD - -1 if empty*/
   public double getTimeMax() {
      if( isEmpty() ) return -1;
      return range.ends( range.nranges()-1 ) / DAYMICROSEC;
   }
   
   /** Return JD time for a couple order/npix */
   public static double getTime(int order, long val) {
      int shift = (FACT_T/2)*(MAXORD_T - order);
      long t = val<<shift;
      return t/DAYMICROSEC;
   }
   
   /** Return the duration of a cell for a specifical order (in microsec) */
   public static long getDuration(int order) {
      int shift =(FACT_T/2)*(MAXORD_T - order);
      return 1L<<shift;
   }
   
   /** Returns a rangeIterator, which iterates over all individual range
    * @param jdStart JD start time
    * @param jdStop JD end time
    * @return iterator of range in microseconds
    */
   public Iterator<long[]> jdIterator(double jdStart, double jdStop) {
      if( jdStart>jdStop ) throw new InputMismatchException();
      return new JDIterator((long)(jdStart*DAYMICROSEC),(long)(jdStop*DAYMICROSEC));
   }
   
   class JDIterator implements Iterator<long[]>{
      int pos, endpos;
      
      JDIterator(long start, long end) {
         pos = range.indexOf(start)/2;;
         if( pos<0 ) pos=0;
         endpos = range.indexOf(end)/2+1;
      }
      
      public boolean hasNext() { return range.sz>0 && pos<endpos; }

      public long [] next() {
         if( !hasNext() ) throw new NoSuchElementException();
         long ret [] = new long[2];
         ret[0] = range.begins(pos);
         ret[1] = range.ends(pos);
         pos++;
         return ret;
      }

      public void remove() { }
   }
   
   /***************************************************** Operations ******************************************/
   
   /** Return the Union with another Moc */
   public TMoc union(Moc moc) throws Exception {
      if( moc instanceof STMoc ) moc = ((STMoc)moc).getTimeMoc();
      else if( !(moc instanceof TMoc ) ) throw new Exception("Uncompatible Moc type for TMoc union");
      return (TMoc) super.union(moc);
   }

   /** Return the Intersection with another Moc */
   public TMoc intersection(Moc moc) throws Exception {
      if( moc instanceof STMoc ) moc = ((STMoc)moc).getTimeMoc();
      else if( !(moc instanceof TMoc ) ) throw new Exception("Uncompatible Moc type for TMoc subtraction");
      return (TMoc) super.subtraction(moc);
   }
   
   /** Return the subtraction with another Moc */
   public TMoc subtraction(Moc moc) throws Exception {
      if( moc instanceof STMoc ) moc = ((STMoc)moc).getTimeMoc();
      else if( !(moc instanceof TMoc ) ) throw new Exception("Uncompatible Moc type for TMoc subtraction");
      return (TMoc) super.subtraction(moc);
   }
   
   /** Return the complement */
   public TMoc complement() throws Exception { return (TMoc) super.complement(); }
   
   
  /*************************************************************** I/O *****************************************************/
   
   /** Internal method: read FITS data according to the type of MOC.
    * @param in The input stream
    * @param naxis1 size of FITS row (in bytes) (generally ==nbyte, but may be 1024 for buffering)
    * @param naxis2 number of values
    * @param nbyte size of each value (in bytes)
    * @param header HDU1 header
    * @throws Exception
    */
   protected  void readSpecificData(InputStream in, int naxis1, int naxis2, int nbyte, HeaderFits header) throws Exception {
      String type = header.getStringFromHeader("ORDERING");

      // TMOC 2.0
      if( type!=null && type.equals("RANGE") ) readSpecificDataRange(in, naxis1, naxis2, nbyte);

      // Compatibility with TMOC protos
      else readSpecificDataUniq(in, naxis1, naxis2, nbyte);
      
      int mocOrder = header.getIntFromHeader("MOCORD_T");
      if( mocOrder==-1 ) mocOrder = header.getIntFromHeader("MOCORDER")*2+3;   // Compatibilite avec TMOC protos
      if( mocOrder==-1 ) throw new Exception("Missing MOC order in FITS header (MOCORD_T)");
      setMocOrder( mocOrder );
   }
   
   /** Write specifical TMoc properties */
   protected int writeSpecificFitsProp(OutputStream out) throws Exception {
      int n=0;
      out.write( getFitsLine("MOCDIM","TIME","Physical dimension") );                            n+=80;      
      out.write( getFitsLine("ORDERING","RANGE","Coding method") );                              n+=80;      
      out.write( getFitsLine("MOCORD_T",""+getMocOrder(),"Time MOC resolution (best order)") );  n+=80;      
      out.write( getFitsLine("TIMESYS","TCB","Time ref system") );                               n+=80;
      return n;
   }

   /** Write TMoc data */
   protected int writeSpecificData(OutputStream out) throws Exception {
      flush();
      byte [] buf = new byte[ sizeOfCoding() ];
      int size = 0;
      for( int i=0; i<range.sz; i+=2 ) {
         long tmin = range.r[i];
         size+=writeVal(out,tmin,buf);
         long tmax = range.r[i+1];
         size+=writeVal(out,tmax,buf);
      }
      return size;
   }

}

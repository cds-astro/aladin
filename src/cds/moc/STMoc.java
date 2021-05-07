package cds.moc;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * The STMoc class implements the methods specific to spatial temporal MOCs.
 * See:  IVOA MOC 2.0 standard => https://www.ivoa.net/documents/MOC/
 * @author Pierre Fernique [CDS]
 * @version 1.0 - April 2021 - creation
 *
 */
public class STMoc extends Moc2D {
   
   private static boolean PROTOSTMOC = false;   // Compatibility with proto STMOC

   /** STMoc creator */
   public STMoc() {
      super( new TMoc(), new SMoc() );
   }
   
   /** STMoc creator
    * @param spaceOrder MocOrder for space dimension [0..29]
    * @param timeOrder MocOrder for time dimension [0..61]
    */
   public STMoc( int timeOrder, int spaceOrder) throws Exception {
      this();
      setMocOrder(timeOrder, spaceOrder);
   }
   
  /** STMoc creator
    * @param s String containing a STMOC (ASCII, JSON)
    */  
   public STMoc( String s ) throws Exception { 
      this();
      add( s );
   }
   
   /** STMoc creator
    * @param in Input stream containing a STMOC (ASCII, JSON or FITS)
    */
   public STMoc( InputStream in ) throws Exception { 
      this();
      read(in);
   }
   
   /** Clone Moc (deep copy) */
   public STMoc clone() throws CloneNotSupportedException {
      STMoc moc = dup();
      clone1( moc );
      return moc;
   }
   
   /** Deep copy - internal method */
   protected void clone1( Moc moc ) throws CloneNotSupportedException {
      if( !(moc instanceof STMoc) ) throw new CloneNotSupportedException("Uncompatible type of MOC for clone. Must be STMoc");
      super.clone1( moc );
   }
   
   /** Create and instance of same class, same sys, but no data nor mocorder */
   public STMoc dup() { return new STMoc(); }
   
   
   /******************************** Pour la migration d'Aladin et du MocServer ***********************************/
   
   /** Adding one élément by spaceOrder/npix et [jdtmin..jdtmax] */
   public void add(int order, long npix, double jdtmin, double jdtmax) throws Exception  {
      long smin = getStart2(order,npix);
      long smax = getEnd2(order,npix)-1L;
      long tmin = (long)(jdtmin*TMoc.DAYMICROSEC);
      long tmax = (long)(jdtmax*TMoc.DAYMICROSEC +TMoc.getDuration( getTimeOrder()));
      add( tmin, tmax, smin, smax );
   }
   
   public void add(long tmin, long tmax, long smin, long smax) throws Exception  {
      add( TMoc.MAXORD_T, tmin, tmax, SMoc.MAXORD_S, smin, smax );
   }
   
   public void add( double jdtmin, double jdtmax, Range r) throws Exception  {
      long tmin = (long)(jdtmin*TMoc.DAYMICROSEC);
      long tmax = (long)(jdtmax*TMoc.DAYMICROSEC +TMoc.getDuration( getTimeOrder()));
      add(tmin,tmax,r);
   }
   
   /************************************ STMoc specifical methods *****************************************/
   
   /** Set time order [0..61] */
   public void setTimeOrder( int timeOrder ) throws Exception { setMocOrder1( timeOrder ); }
   
   /** Set space order [0..29] */
   public void setSpaceOrder( int spaceOrder ) throws Exception { setMocOrder2( spaceOrder ); }
   
   /** Get time order */
   public int getTimeOrder()  { return getMocOrder1(); }
   
   /** Get space order */
   public int getSpaceOrder() { return getMocOrder2(); }
   
   /** Return minimal time in JD - -1 if empty*/
   public double getTimeMin() {
      if( isEmpty() ) return -1;
      return range.begins(0) / TMoc.DAYMICROSEC;
   }

   /** Return maximal time in JD - -1 if empty*/
   public double getTimeMax() {
      if( isEmpty() ) return -1;
      return range.ends( range.nranges()-1 ) / TMoc.DAYMICROSEC;
   }
   
   public int getTimeRanges() { return getNbRanges(); }
   
   /** TMoc covering from the whole STMOC */
   public TMoc getTimeMoc() throws Exception {
      TMoc moc = new TMoc( getTimeOrder() );
      moc.setRangeList( new Range(range) );
      return moc;
   }
   
   /** TMoc from the intersection with the spaceMoc */
   public TMoc getTimeMoc( SMoc spaceMoc) throws Exception {
      if( spaceMoc==null || spaceMoc.isEmpty() ) return getTimeMoc();
      TMoc moc = new TMoc( getTimeOrder() );
      Range r1 = new Range();
      
      for( int i=0; i<range.sz; i+=2 ) {
         Range m = range.rr[i>>>1];
         if( spaceMoc.range.overlaps(m) ) r1.append( range.r[i], range.r[i+1] );
      }
      
      moc.range = r1;
      return moc;
   }
   
   /** SMoc covering the whole STMOC */
   public SMoc getSpaceMoc() throws Exception { return getSpaceMoc(-1,Long.MAX_VALUE); }
   
   /** SMoc extraction from a temporal time
    * @param tmin  min of range (order 61)
    * @param tmax max of range (included - order 61)
    */
   public SMoc getSpaceMoc(long tmin,long tmax) throws Exception {
      if( tmin>tmax ) throw new Exception("bad time range");
      
      int pos = range.indexOf(tmin);
      if( (pos&1)==1 ) {
         if( pos<0 ) pos++;
         else pos--;
      }
      
      SMoc moc = new SMoc( getSpaceOrder() );
      moc.bufferOn(2000000);
      Range lastM=null;
      for( int i=pos; i<range.sz; i+=2 ) {
         if( range.r[i]>tmax ) break;
         Range m = range.rr[i>>>1];
         if( m==lastM ) continue;
         for( int j=0; j<m.sz; j+=2 ) moc.add(SMoc.MAXORD_S, m.r[j], m.r[j+1]-1L );
      }
      moc.bufferOff();
      return moc;
   }

   /** True if the npix (deepest level) and jd date is in the STMoc */
   public  boolean contains(long npix, double jd) {
      long npixTime = (long)( jd * TMoc.DAYMICROSEC );
      if( !range.contains(npixTime) ) return false;
      for( Range r : range.rr ) {
         if( r.contains(npix) ) return true;
      }
      return false;
   }

   /***************************************************** Operations ******************************************/
   
   /** Return the Union with another Moc */
   public STMoc union(Moc moc) throws Exception {
      if( !(moc instanceof STMoc ) ) throw new Exception("Uncompatible Moc type for STMoc union");
      return (STMoc) super.union(moc);
   }

   /** Return the subtraction with another Moc */
   public STMoc subtraction(Moc moc) throws Exception {
      if( !(moc instanceof STMoc ) ) throw new Exception("Uncompatible Moc type for STMoc subtraction");
      return (STMoc) super.subtraction(moc);
   }

   /** Return the Intersection with another Moc */
   public STMoc intersection(Moc moc) throws Exception {
      STMoc m;
      
      // Entre deux STMoc ?
      if( moc instanceof STMoc ) {
         m = (STMoc) moc;
         
      // Avec un TMoc ?
      // On crée un STMOC à partir des plages temporaires du TMoc qui pointe vers un SMoc complet   
      } else if( moc instanceof TMoc ) {
         m = new STMoc();
         Range2 r = new Range2( range.sz );
         if( !isEmpty() ) {
            SMoc allsky = new SMoc("0/0-11");
            for( int i=0; i<range.sz; i+=2 ) {
               r.append(range.r[i], range.r[i+1], allsky.seeRangeList() );
            }
         }
         m.setTimeOrder( Math.min( getTimeOrder(), ((TMoc)moc).getMocOrder()) );
         m.setSpaceOrder( getSpaceOrder() );
         m.setRangeList( r );
        
      // Avec un SMoc ?
      // On crée un STMoc avec une seule plage temporaire correspondante
      // à l'étendue maximale du temps du Moc avec lequel on veut faire l'intersection
      } else {
         Range2 r = new Range2(2);
         if( !isEmpty() ) r.add( range.r[0], range.r[ range.sz-1 ], moc.seeRangeList() );
         m = new STMoc();
         m.setTimeOrder( getTimeOrder() );
         m.setSpaceOrder( Math.min( getSpaceOrder(), ((SMoc)moc).getMocOrder()) );
         m.setRangeList( r );
      }
      
      return (STMoc) super.intersection(m);
   }
   
   /** Return the complement */
   public STMoc complement() throws Exception {
      STMoc moc = new STMoc(  getTimeOrder() , getSpaceOrder());
      moc.add("t0/0 s0/0-11");
      return moc.subtraction(this);
   }

   /*************************************************************** I/O *****************************************************/

   /** Write specifical STMOC properties  */
   protected int writeSpecificFitsProp( OutputStream out  ) throws Exception {
      int n=0;
      out.write( getFitsLine("MOCDIM","TIME.SPACE","STMOC: Time dimension first, ") );  n+=80;      
      out.write( getFitsLine("ORDERING","RANGE","Range coding") );                      n+=80;      
      out.write( getFitsLine("MOCORD_T",""+ getTimeOrder(),"Time MOC resolution") );    n+=80;      
      out.write( getFitsLine("MOCORD_S",""+ getSpaceOrder(),"Space MOC resolution") );  n+=80;      
      out.write( getFitsLine("COORDSYS","C","Space reference frame (always C=ICRS)") ); n+=80;
      out.write( getFitsLine("TIMESYS","TCB","Time ref system (always TCB)") );         n+=80;
      return n;
   }

   /** Write STMOC data */
   protected int writeSpecificData(OutputStream out) throws Exception {
      byte [] buf = new byte[ 8 ];
      int size = 0;
      for( int i=0; i<range.sz; i+=2 ) {
         long tmin = codeTime(range.r[i]);
         size+=writeVal(out,tmin,buf);
         long tmax = codeTime(range.r[i+1]);
         size+=writeVal(out,tmax,buf);

         // Si le prochain Moc dim2 est identique, on passe
         if( i<range.sz-2 && range.rr[i/2].equals(range.rr[i/2+1]) ) continue;

         Range m = range.rr[i/2];
         for( int j=0; j<m.sz; j+=2 ) {
            long smin = m.r[j];
            size+=writeVal(out,smin,buf);
            long smax = m.r[j+1];
            size+=writeVal(out,smax,buf);
         }
      }
      return size;
   }


   /** Internal method: read FITS data according to the type of MOC.
    * @param in The input stream
    * @param naxis1 size of FITS row (in bytes) (generally ==nbyte, but may be 1024 for buffering)
    * @param naxis2 number of values
    * @param nbyte size of each value (in bytes)
    * @param header HDU1 header
    * @throws Exception
    */
   protected void readSpecificData( InputStream in, int naxis1, int naxis2, int nbyte, HeaderFits header) throws Exception {
      
      int timeOrder=-1,spaceOrder=-1;

      // MOC V2.0
      String type = header.getStringFromHeader("MOCDIM");
      if( type!=null ) {
         timeOrder  = header.getIntFromHeader("MOCORD_T");
         spaceOrder = header.getIntFromHeader("MOCORD_S");
         
      // For compatibility with STMOC protos
      } else {
         type = header.getStringFromHeader("MOC");
         if( type==null || type.equals("SPACETIME") ) {
            timeOrder  = header.getIntFromHeader("TORDER")*2+3;
            spaceOrder = header.getIntFromHeader("MOCORDER");
         } else {
            timeOrder  = header.getIntFromHeader("MOCORDER")*2+3;
            spaceOrder = header.getIntFromHeader("MOCORD_1");
         }
         PROTOSTMOC=true;
      }
      
      setTimeOrder(timeOrder);
      setSpaceOrder(spaceOrder);
      
      byte [] buf = new byte[naxis1*naxis2];
      readFully(in,buf);
      createSTmocByFits((naxis1*naxis2)/nbyte,buf);
   }
   
   static public long MASK_T = 1L<<63;
   static public long UNMASK_T = ~MASK_T;

   static private boolean isTime(long a)  {
      if( PROTOSTMOC ) return a<0;
      return (a&MASK_T)!=0L;
   }
   
   static private long codeTime(long a)  { return a|MASK_T; }
   
   static private long decodeTime(long a) {
      if( PROTOSTMOC ) return -a;
      return a&UNMASK_T;
   }

   /** Create STMoc from the list of fits values */
   private void createSTmocByFits(int nval,byte [] t) throws Exception {
      int i=0;
      int startT = 0;
      Range m = new Range(500);
      
      // Reading range by range (2 indices simultaneously)
      for( int k=0; k<nval; k+=2, i+=16 ) {
         
         long min = readLong(t,i);
         long max = readLong(t,i+8);
         
         // Temporal list ? (negative values)
         if( isTime(min) ) {
            
            // Assignments of the previous spatial indices to the corresponding time intervals (no copy)
            if( !m.isEmpty() ) {
               m.trimSize();
               for( int j = startT; j<range.sz; j+=2 )  range.rr[j>>>1] = m;
               
               // We are preparing the range for the next space list
               m = new Range(500);
               startT=range.sz;
            }
            
            // Memorization of the temporal range
            range.append( decodeTime(min), decodeTime(max), null);

         //  Spatial list
         } else m.append(min,max);
      }
      
      // last "flush"
      if( !m.isEmpty() ) {
         m.trimSize();
         for( int j = startT; j<range.sz; j+=2 )  range.rr[j>>>1] = m;
      }
    }
}

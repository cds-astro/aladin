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
import java.io.OutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The Moc1D class implements the methods specific to one-dimensional MOCs (e.g. SPACE or TIME).
 * @author Pierre Fernique [CDS]
 * @version 1.0 - April 2021 - creation
 */
public abstract class Moc1D extends Moc {

   protected Range range;            // List of ranges
   protected int mocOrder;           // Moc order, -1 if undefined
   
   // Used during the MOC generation
   private int biggestOrder;         // Biggest order used during the Moc generation, -1 if undefined
   private int currentOrder;         // last current order during the Moc generation
   
   private long [] buf=null;         // Internal buffer to speed up iterative insertions (see bufferOn())    
   private int bufSz=0;              // Current size of the internal buffer
   

   /** Generic Moc 1D creator */
   protected Moc1D() { this(-1); }
   
   /** Generic Moc 1D creator
    * @param mocOrder Moc Order (-1 => not defined) */
   protected Moc1D( int mocOrder ) { 
      super();
      clear();
      this.mocOrder = mocOrder;
   }
   
   /** Generic Moc 1D generator from ASCII Moc String
    * @param s ASCII Moc (regular ASCII or JSON) */
   protected Moc1D( String s ) throws Exception  {
      this();
      add(s);
   }
   
   /** Generic Moc 1D generator from input stream
    * @param in input stream */
   protected Moc1D( InputStream in ) throws Exception {
      this();
      read(in);
   }

   /** Generic Moc 1D generator from another Moc 1D
    * @param moc moc to be dumped */
   protected Moc1D( Moc1D moc ) throws Exception {
      this();
      moc.clone1( this );
   }
   
   public String toDebug() {
      String so = ""+getMocOrder();
      if( mocOrder==-1 ) so = "("+so+")"; 
      char c= Character.toUpperCase( cDim() );
      return (c+"")
             +"MOC mocOrder="+so+" deepestOrder="+getDeepestOrder()
             +" nbRanges="+getNbRanges()
             +" nbCells="+getNbCells()
             +" mem="+getUnitDisk( getMem());
   }
   
   /** Clear the MOC - data only (not the properties, nor the mocOrder) */
   public void clear() {
      super.clear();
      range = new Range();
      bufSz=0;
      currentOrder=-1;
      biggestOrder=-1;
    }
   
   /** Deep copy - internal method */
   protected void clone1( Moc moc ) throws CloneNotSupportedException {
      super.clone1( moc );
      Moc1D m = (Moc1D)moc;
      flush();
      m.range= (range==null) ? null : new Range(range);
      m.mocOrder=mocOrder;
      m.currentOrder=currentOrder;
   }
   
   /** Degrades the resolution(s) of the MOC until the RAM size of the MOC is reduced under the specified maximum (expressed in bytes). */
   public void reduction( long maxSize) throws Exception {
      if( maxSize<=0L ) throw new Exception("negative or null size not allowed");
      while( getMem()>maxSize && getMocOrder()>0 ) setMocOrder( getMocOrder()-1 );
   }
   
   /** Return the deepest possible order (ex: 29 for SMoc, 61 for TMoc) */
   public abstract int maxOrder();
   
   /** Return the number of bit shifting between two consecutive orders (ex: 2 for SMoc, 1 for TMoc) */
   public abstract int shiftOrder();
   
   /** Return the Moc signature character (ex: 's' for SMoc, 't' for TMoc) */
   public abstract char cDim();
   
   /** Return the number of values at the deepest order (ex: 2x2^29x2^29 for SMoc, 2^61 for TMoc) */
   public abstract long maxVal();
   
   /** Recalculates the metrics associated with the MOC hierarchical view: 
    * the number of hierarchical cells, the deepest order used... */
   protected void computeHierarchy() {
      int deep=-1;
      int size=0;
      if( range!=null ) {
         Iterator<MocCell> it = cellIterator( true );
         while( it.hasNext() ) {
            MocCell cell = it.next();
            size += cell.end - cell.start;
            if( cell.order>deep ) deep=cell.order;
         }
      }
      cacheDeepestOrder=deep;
      cacheNbCells=size;
   }

   /** Return the number of used cells at the Moc order: (ex: s2/1 3/5-6   => 6 */
   public long getNbValues() {
      flush();
      int shift = (maxOrder()-getMocOrder())*shiftOrder();
      return range.nval() >>> shift;
   }
   
   /** Set the list of ranges - Warning: no copy */
   public void setRangeList( Range range ) { 
      this.range=range;
      resetCache();
   }
   
   /** Return true if the Moc is empty (no coverage) */
   public boolean isEmpty() { flush(); return range.sz==0; }
   
   /** Return true if the Moc is full (full coverage) */
   public boolean isFull() {
      flush(); 
      return range.sz==2 && range.r[0]==0 && range.r[1]==maxVal();
   }
   
   /** Return the coverage pourcentage of the Moc */
   public double getCoverage() { flush(); return (double)range.nval() / maxVal(); }
   
   /** Return the number of ranges */
   public int getNbRanges() { flush(); return range.sz/2; }
   
   /** Return approximatively the amount of memory used for storing this MOC in RAM (in bytes) */
   public long getMem() { flush(); return range.getMem() + (buf==null ? 0L : buf.length*8L); }

   /** Generic operations: 0-union, 1-intersection, 2-subtraction */
   protected Moc1D operation(Moc moc,int op) throws Exception {
      Moc1D m = (Moc1D)moc;
      m.flush();
      Moc1D res = (Moc1D)dup();
      flush();
      switch(op) {
         case 0 : res.range = range.union(m.range); break;
         case 1 : res.range = range.intersection(m.range); break;
         case 2 : res.range = range.difference(m.range); break;
      }
      res.setMinOrder(  Math.min( getMinOrder(), m.getMinOrder()) );
      
      // The order of the target Moc depends of the MOC operation strategy.
      // but if the newOrder is smaller that the previous orders, the range of resulting
      // Moc may have to be degraded to the new order (force=true)
      int newOrder =  getMocOrder4op(getMocOrder(),m.getMocOrder());
      boolean force = newOrder< Math.min( getMocOrder(), m.getMocOrder() );
      res.setMocOrder( newOrder, force );
      res.range.trimIfTooLarge();
      return res;
   }
   
   /** Return the complement */
   public Moc1D complement() throws Exception {
      Moc1D res = (Moc1D)dup();
      flush();
      res.range = range.complement( 0, maxVal() );
      return res;
   }
   
   /** Return true in case of equality (only check data - no properties, nor MocOrder) */
   public boolean equals(Object o) {
      if( this==o ) return true;
      if( o==null || !(o instanceof Moc1D) ) return false;
      Moc1D moc = (Moc1D) o;
      if( cDim()!=moc.cDim() ) return false;
      flush();
      moc.flush();
      return range.equals( moc.range );
   }
   
   public int hashCode() {
      if( cacheHashCode== -1 ) { flush(); cacheHashCode=range.hashCode(); } 
      return cacheHashCode;
   }
   
   // ------------------------------------ MAY BE TO BE DEFINED IN THE MOC ABSTRACT CLASS --------------------------------
   public boolean isIncluding(int order,long val){ 
      long start = getStart(order,val);
      long end = getEnd(order,val);
      flush();
      return range.contains( start, end); 
   }
   public boolean isIntersecting(int order,long val){ 
      long start = getStart(order,val);
      long end = getEnd(order,val);
      flush();
      return range.overlaps( start, end); 
   }
   public boolean isIncluding(Moc1D moc)    { flush(); return range.contains( moc.range ); }   
   public boolean isIntersecting(Moc1D moc) { flush(); return range.overlaps( moc.range ); }
  // ----------------------------------------------------------------------------------------------------------------------

   /** Set Moc order. Will impact the precision (and the number) of the values. -1 for internal reset */
   public void setMocOrder( int mocOrder) throws Exception { setMocOrder(mocOrder,false); }
   private void setMocOrder( int mocOrder, boolean force) throws Exception {
      if( mocOrder<-1 || mocOrder>maxOrder() ) throw new Exception("MocOrder error ("+mocOrder+" not in [0.."+maxOrder()+"])");
            
      if( mocOrder!=-1 ) {
         // If the Moc order was not yet defined, it was assumed to be at the best resolution
         if( this.mocOrder==-1 ) this.mocOrder=maxOrder();

         // If the new mocOrder is smaller than the previous one, 
         // or if we need to check the range values (force=true)
         // the cells must be aggregated according to the change in resolution
         if( force || mocOrder<this.mocOrder ) {
            int diff = maxOrder() - mocOrder;
            flush();
            range = range.degrade( diff * shiftOrder() ); 
            resetCache();
         }
      }
      this.mocOrder=mocOrder;
   }
   
   /** Return the Moc order. Either specically set by setOrder().
    * If not definied, return the biggest used order during the addition,
    * And if not defined return the hierarchycal deespest order */
   public int getMocOrder() {
      if( mocOrder==-1 ) {
         if( biggestOrder!=-1 ) return biggestOrder;
         return getDeepestOrder();
      }
      return mocOrder;
   }
   
   /** Set min Moc min Order. The hierarchical view will be start at this order. 0 by default */
   public void setMinOrder( int minOrder ) throws Exception { }
   
   /** get min Moc min Order. The hierarchical view will be start at this order. 0 by default */
   public int getMinOrder() { return 0; }
   
   /** Add one cell to the Moc.
    * @param order Order of the cell
    * @param val Value of the cell
    * @throws Exception
    */
   public void add(int order, long val) throws Exception  { add(order,val,val); }
   
   /** Add a list of consecutive Moc cells.
    * @param order Order of the cells
    * @param firstVal First value
    * @param lastVal Last value (included)
    */
   public void add(int order,long firstVal, long lastVal ) throws Exception {
      
      // Do we have to degrade the order/val (if mocOrder is smaller than the order) ?
      if( mocOrder!=-1 && mocOrder<order ) {
         int shift = (order-mocOrder) * shiftOrder();
         firstVal = (firstVal>>>shift ) << shift;
         lastVal   = (((lastVal>>>shift )+1L)   << shift) -1L;
      }
      
      if( order>biggestOrder ) biggestOrder=order;
      
      // Values converted to the maxOrder as range
      int shift = ( maxOrder()-order ) * shiftOrder();
      long start = firstVal<<shift;
      long end =  (lastVal+1L)<<shift;
      
      // Fast bufferisation ? => see bufferOn()
      if( buf!=null ) {
         
         // Peut être 2x de suite la même case ?
         if( bufSz>2 && buf[bufSz-2]==start && buf[bufSz-1]==end ) return;
         
         buf[bufSz++]=start;
         buf[bufSz++]=end;
         if( bufSz==buf.length ) flush();
         
      // Or direct addition ?
      } else  range.add(start,end);
      
      resetCache();
   }
   
   /** Returns the value of the beginning of the interval expressed at the maximum order */
   public long getStart(int order, long val) {
      return val << ( maxOrder()-order ) * shiftOrder() ;
   }
   
   /** Returns the value of the end of the interval (excluded) expressed at the maximum order */
   public long getEnd(int order, long val) { return getStart(order, val+1L); }
   
   /** Activation of the buffererization. Significantly speeds up random multiple additions. */
   public void bufferOn() { bufferOn(200000); }
   
   /** Activation of the buffererization. Significantly speeds up random multiple additions
    * Required additionnal memory for the buffer (see bufferOff())
    * Note: Any read access or operation on the MOC will automatically be preceded by a flush() if necessary.
    * @param size: size of the buffer (required even value) */
   public void bufferOn(int size) {
      flush();
      if( size%1==1 ) size++;   // In case off
      buf=new long[size];
      bufSz=0;
   }
   
   /** Stops the buffering and frees the memory required for it.*/
   public void bufferOff() {
      if( buf!=null ) {
         flush();
         range.trimSize();
      }
      buf=null;
      bufSz=0;
   }
   
   /** Inserts in the MOC all the elements being inserted in the buffer. 
    * The buffering remains active for future insertions (unlike bufferOff()) */
   public void flush() {
      if( bufSz==0 ) return;
      add( buf, bufSz);
      bufSz=0;
   }
   
   /** Fast addition of a list of ranges expressed at the maximum order (2 consecutive longq per range): start..end (end excluded). 
    * These ranges do not need to be sorted, nor to be disjoint.
    * @param valList list of range index
    * @param size Number of values (= 2x number of ranges)
    */
   public void add( long [] valList, int size ) {
      if( size<=0 ) return;
      Range r = new Range(valList,size);
      r.sortAndFix();
      range = range.union(r);
      resetCache();
   }
   
   /** Add directly a Moc */
   public void add(Moc moc) throws Exception {
      flush(); 
      range = range.union( moc.seeRangeList() );
      resetCache();
   }

   /** Acces to the list of ranges (no copy) */
   public Range seeRangeList() { flush(); return range; }
   
   /** Provide an Iterator on the MOC cell List (hierarchical view)
    * @param flagRange true for getting range rather than all individual values
    * @return mocCell => dim,order,startVal,endVal
    */
   public Iterator<MocCell> cellIterator( boolean flagRange ) { return new CellIterator( flagRange ); }

   // Creation of an iterator on the list of pixels - derived from the algo of Reinecke
   private class CellIterator implements Iterator<MocCell> {
      Range r2,r3;
      long a,b;
      int o;
      int i;
      int shift;
      long ofs;
      boolean flagEnd;
      boolean took;
      boolean flagRange;
      char cdim;

      CellIterator( boolean flagRange ) {
         flush();
         this.flagRange = flagRange;
         r2 = new Range( seeRangeList() );
         r3 = new Range();
         o= getMinOrder();
         i=-2;
         shift = shiftOrder()*(maxOrder()-o);
         ofs=(1L<<shift)-1;
         flagEnd=false;
         took=true;
         cdim = cDim();
      }

      public boolean hasNext() {
         goNext();
         return !flagEnd;
      }

      public MocCell next() {
         if( !hasNext() ) return null;
         took=true;
         MocCell cell =new MocCell();
         cell.dim = cdim;
         cell.order = o;
         cell.start = a;
         cell.end = flagRange ? b : a+1;   // by ranges? or by single value?
         a = cell.end;
         return cell;
      }

      public void remove() {  }

      private void goNext() {
         if( flagEnd || !took ) return;
         if( i>=0 && a<b ) return;
         do {
            for( i+=2; i<r2.sz; i+=2) {
               a=(r2.r[i]+ofs) >>>shift;
               b= r2.r[i+1] >>>shift;
               if( a>=b ) continue;
               r3.append(a<<shift, b<<shift);
               took=false;
               return;
            }
            if( !r3.isEmpty() ) r2 = r2.difference(r3);
            if( o==maxOrder() || r2.isEmpty() ) { 
               flagEnd=true; 
               break;
            }
            o++;
            shift = shiftOrder()*(maxOrder()-o);
            ofs=(1L<<shift)-1;
            r3.clear();
            i=-2;
         } while( true );
      }
   }

   
   /** Return an Iterator providing the list of values at the Moc order.
    * => values provided in ascending order */
   public Iterator<Long> valIterator() {
      long gap = 1L << ((maxOrder() - getMocOrder()) * shiftOrder());
      return new ValIterator( gap ) ;
   }
   
   class ValIterator implements Iterator<Long>{
      int pos;    // Position in range
      long value; // current value in range;
      long gap;   // Gap 
      
      ValIterator( long gap ) {
         flush();
         this.gap = gap;
         pos=0;
         value = (range.sz>0) ? range.r[0] : 0;
      }

      public boolean hasNext() { return (pos<range.sz); }

      public Long next() {
         if (pos>range.sz) throw new NoSuchElementException();
         long ret = value;
         value += gap;
         if( value>=range.r[pos+1] ) {
            pos+=2;
            if (pos<range.sz)  value = range.r[pos];
         }
         return ret/gap;
      }
   }



   /********************************************************* ASCII & JSON parser *******************************************/


   /** Internal usage: Add one token element according to the format "[s|t]order/npix[-npixn]".
    * If the order is not mentioned, use the last used order (currentOrder)
    * Note: Also support JSON non standard IVOA syntax
    * @param token one token (ex: s18/23-45)
    */
   protected void addToken(String token) throws Exception {
      if( token==null ) return;
      int i=token.indexOf('/');         // Order prefix ?
      if( i<0 ) i=token.indexOf(':');   // JSON alternative
      
      // Is there an order specified ?
      if( i>0 ) {
         String s1= unQuote(token.substring(0,i));   // JSON alternative
         
         // Possible prefix (ex: 's' for SMOC or 't' for TMOC)
         if( !Character.isDigit( s1.charAt(0) ) ) {
            if( s1.charAt(0)!=cDim() ) throw new Exception("MOC syntax error. Unknown prefix order. Must be "+cDim()+ "["+s1.charAt(0)+"]");
            s1=s1.substring(1);
         }
         
         // Memorizes the current order
         try { currentOrder = Integer.parseInt( s1 ); } 
         catch( NumberFormatException e ) {
            throw new Exception("MOC syntax error. Order must be an integer value");
         }
      }
      
      int j=token.indexOf('-',i+1);

      // Is it a singleton ?
      if( j<0 ) {
         String s1 = unBracket( token.substring(i+1) );    // JSON alternative
         
         // no value => probably the terminal order (ex: 28/)
         if( s1.trim().length()==0 ) {
            if( mocOrder!=-1 && mocOrder!=currentOrder) throw new Exception("MocOrder already specified ("+mocOrder+") => ignored ["+currentOrder+"]");
            setMocOrder( currentOrder );
            return;
         }
         long val;
         try { val = Long.parseLong( s1 ); }
         catch( NumberFormatException e ) {
            throw new Exception("MOC syntax error. Value must be an integer long value ["+s1+"]");
         }
         add(currentOrder, val);

      // A range
      } else {
         long starVal;
         long endVal;
         try {
            starVal = Long.parseLong(token.substring(i+1,j));
            endVal = Long.parseLong(token.substring(j+1));
         } catch( NumberFormatException e ) {
            throw new Exception("MOC syntax error. Range must be two integer long values separated by a dash ["+token.substring(i+1)+"]");
         }
         if( starVal>=endVal ) throw new Exception("MOC syntax error. Range must be expressed by 2 increasing ordered long integers ["+token+"]");
         add(currentOrder,starVal,endVal); 
      }
   }
   
   
   /*************************************** Readers **********************************************/
   
   protected void readSpecificDataUniq( InputStream in, int naxis1, int naxis2, int nbyte) throws Exception {
      byte [] buf = new byte[naxis1*naxis2];
      readFully(in,buf);
      createMocByUniq((naxis1*naxis2)/nbyte,nbyte,buf);
   }
   
   protected void createMocByUniq(int nval,int nbyte,byte [] t) throws Exception {
      
      
//      long t0,t1;
//      int nbCells=0;
//      t0 = System.currentTimeMillis();
      
      bufferOn();
      int i=0;
      long [] hpix = null;
      long val;
      for( int k=0; k<nval; k++ ) {
         int a =   ((t[i++])<<24) | (((t[i++])&0xFF)<<16) | (((t[i++])&0xFF)<<8) | (t[i++])&0xFF;
         if( nbyte==4 ) val = a;
         else {
            int b = ((t[i++])<<24) | (((t[i++])&0xFF)<<16) | (((t[i++])&0xFF)<<8) | (t[i++])&0xFF;
            val = (((long)a)<<32) | ((b)& 0xFFFFFFFFL);
         }
         hpix = uniq2hpix(val,hpix);
         add( (int)hpix[0], hpix[1] );

      }
      bufferOff();

      //      t1 = System.currentTimeMillis();
      //      System.out.println("Reading "+nbCells+" cells for "+getNbRanges()+" ranges in "+(t1-t0)+"ms");

   }
   
   protected void readSpecificDataRange( InputStream in, int naxis1, int naxis2, int nbyte) throws Exception {
       byte [] buf = new byte[naxis1*naxis2];
       readFully(in,buf);
       createMocByRange((naxis1*naxis2)/nbyte,buf);
   }
   
   protected void createMocByRange(int nval,byte [] t) throws Exception {
      int i=0;
      for( int k=0; k<nval; k+=2, i+=16 ) {
         long min = readLong(t,i);
         long max = readLong(t,i+8);
         range.append( min, max);
      }
      resetCache();
    }


   /***************************************  Writers *************************************************/
   
   
   /** Write HEALPix MOC to an output stream IN ASCII encoded format
    * @param out output stream
    */
   public void writeASCII(OutputStream out) throws Exception {
      flush();
      boolean flagNL = range.sz>MAXWORD;
      int order = writeASCII(out, this, flagNL, true );
      
      // Ajout de la resolution max si nécessaire, et d'un CR si nécessaire
      StringBuilder res= new StringBuilder(10);
      int mocOrder = getMocOrder();
      if( order<mocOrder ) {
         if( flagNL ) res.append(CR);
         else res.append(' ');
         res.append(mocOrder+"/");
      } 
      if( flagNL ) res.append(CR);

      // Dernier flush
      writeASCIIFlush(out,res,false);
   }
   
   /** Write HEALPix MOC to an output stream IN JSON encoded format
    * @param out output stream
    */
   public void writeJSON(OutputStream out) throws Exception {
      flush();
      boolean flagNL = range.sz>MAXWORD;
      flagNL=true;
      writeJSON(out, flagNL );
   }

   /** Write HEALPix MOC to an output stream IN JSON encoded format
    * @param out output stream
    */
   private int writeJSON(OutputStream out, boolean flagNL) throws Exception {
      StringBuilder s = new StringBuilder(2048);
      int order=-1;
      boolean first=true;
      int j=0;
      
      s.append("{");
      if( flagNL ) s.append(CR);
     
      Iterator<MocCell> it = cellIterator( false );
      while( it.hasNext() ) {
         MocCell cell = it.next();
         
         // Changement d'ordre ?
         boolean flagNewOrder = cell.order!=order;
         if( flagNewOrder ) {
            if( !first ) s.append("],"+ (flagNL?CR:"") );
            first=false;
            s.append( (flagNL?"  ":"") + "\""+cell.order+"\":[");
            order=cell.order;
            j=0;
         }
         s.append( (flagNewOrder?"":",") + cell.start);
         j++;
         if( j==15 ) { writeASCIIFlush(out,s,flagNL); j=0; }
      }

      if( !first ) s.append("]");
      
      // Ajout du MocOrder terminal si nécessaire
      int mocOrder = getMocOrder();
      if( order<mocOrder ) {
         if( !first ) s.append(','+ (flagNL?CR:""));
         s.append( (flagNL?"  ":"") + "\""+mocOrder+"\":[]");
      } 
      if( flagNL ) s.append(CR);
      s.append("}");

      writeASCIIFlush(out,s,flagNL);
      
      return order;
   }

}

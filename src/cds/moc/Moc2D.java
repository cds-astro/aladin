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

import java.io.OutputStream;
import java.util.Iterator;


/**
 * The Moc2D class implements the methods specific to two-dimensional MOCs (e.g. TIME.SPACE).
 * @author Pierre Fernique [CDS]
 * @version 1.0 - April 2021 - creation
 */
public abstract class Moc2D extends Moc{
   
   public Range2 range;         // list of ranges (first dimension). Each range pointing to a list of range (second dimension)
   protected Moc1D protoDim1;   // Moc1D concerned for the first dimension (ex: TMoc)
   protected Moc1D protoDim2;   // MOC1D concerned for the second dimension (ex: SMoc)
   
   /** Generic Moc 2D creator */
   protected Moc2D(Moc1D protoDim1, Moc1D protoDim2) { 
      super();
      this.protoDim1 = protoDim1;
      this.protoDim2 = protoDim2;
      clear();
   }
   
   public String toDebug() {
      String so1 = ""+getMocOrder1();
      if( protoDim1.mocOrder==-1 ) so1 = "("+so1+")"; 
      char c1= Character.toUpperCase( cDim1() );
      String so2 = ""+getMocOrder2();
      if( protoDim2.mocOrder==-2 ) so2 = "("+so2+")"; 
      char c2= Character.toUpperCase( cDim2() );
      return (c1+"")+(c2+"")
            +"MOC mocOrder="+so1+"/"+so2
//            +" deepestOrder="+getDeepestOrder()
            +" nbRanges="+getNbRanges()
            +" nbCells="+getNbCells()
            +" mem="+getUnitDisk( getMem());
  }
   
   
   /** Return the deepest possible order for the first dimension (ex: 61 for TMoc) */
   public int maxOrder1()   { return protoDim1.maxOrder(); }
   
   /** Return the deepest possible order for the second dimension (ex: 29 for SMoc) */
   public int maxOrder2()   { return protoDim2.maxOrder(); }
   
   /** Return the number of bit shifting between two consecutive orders for the first dimension (ex: 1 for TMoc) */
   public int shiftOrder1() { return protoDim1.shiftOrder(); }
   
   /** Return the number of bit shifting between two consecutive orders for the second dimension (ex: 2 for SMoc) */
   public int shiftOrder2() { return protoDim2.shiftOrder(); }

   /** Return the Moc signature character for the first dimension (ex: 't' for TMoc) */
   public char cDim1() { return protoDim1.cDim(); }
   
   /** Return the Moc signature character for the second dimension (ex: 's' for SMoc */
   public char cDim2() { return protoDim2.cDim(); }
   
   /** Acces to the list of ranges (no copy) */
   public Range seeRangeList() { return range; }
   
   /** Set the list of ranges - Warning: no copy */
   public void setRangeList( Range range ) { this.range=(Range2)range; }
   
   /** Return the Moc order of the first dimension */
   public int getMocOrder1() { return protoDim1.getMocOrder(); }
   
   /** Return the Moc order of the second dimension */
   public int getMocOrder2() { return protoDim2.getMocOrder(); }
   
   /** Set Moc orders of the first and the second dimension simultaneously (fastest than in two steps) */
   public void setMocOrder( int order1, int order2 ) throws Exception { 
      if( order1<-1 || order1>protoDim1.maxOrder() ) 
         throw new Exception("MocOrder error ("+order1+" not in [0.."+protoDim1.maxOrder()+"])");
      if( order2<-1 || order2>protoDim2.maxOrder() ) 
         throw new Exception("MocOrder error ("+order2+" not in [0.."+protoDim2.maxOrder()+"])");
     
      if( order1!=-1 || order2!=-1 ) {
         // If the Moc order was not yet defined, it was assumed to be at the best resolution
         if( order1!=-1 && protoDim1.mocOrder==-1 ) protoDim1.mocOrder=protoDim1.maxOrder();
         if( order2!=-1 && protoDim2.mocOrder==-1 ) protoDim2.mocOrder=protoDim2.maxOrder();

         // If the new mocOrder is smaller than the previous one, 
         // the cells must be aggregated according to the change in resolution
         if( order1<protoDim1.mocOrder || order2<protoDim2.mocOrder ) {
            int shift1 = order1==-1 ? 0 : protoDim1.maxOrder() - order1;
            int shift2 = order2==-1 ? 0 : protoDim2.maxOrder() - order2;
            range = range.degrade( shift1 * protoDim1.shiftOrder(), shift2 * protoDim2.shiftOrder() ); 
            resetCache();
         }
      }
      protoDim1.mocOrder=order1;
      protoDim2.mocOrder=order2;
   }
   
   /** Set Moc order of the first dimension */
   public void setMocOrder1( int order1 ) throws Exception { setMocOrder( order1, protoDim2.mocOrder ); }
   
   /** Set Moc order of the second dimension */
   public void setMocOrder2( int order2 ) throws Exception {  setMocOrder( protoDim1.mocOrder, order2  ); }
   
   /** Returns the value of the beginning of the interval expressed at the maximum order (first dimension) */
   public long getStart1(int order, long val) { return protoDim1.getStart(order,val); }
   
   /** Returns the value of the end of the interval (excluded) expressed at the maximum order (first dimension)  */
   public long getEnd1(int order, long val) { return protoDim1.getEnd(order,val); }
   
   /** Returns the value of the beginning of the interval expressed at the maximum order (second dimension) */
   public long getStart2(int order, long val) { return protoDim2.getStart(order,val); }
   
   /** Returns the value of the end of the interval (excluded) expressed at the maximum order (second dimension)  */
   public long getEnd2(int order, long val) { return protoDim2.getEnd(order,val); }
   
   public void add(long val1, long val2, Range r) {
      int mocOrder1 = protoDim1.mocOrder;
      if( mocOrder1!=-1 ) {
         int shift = (maxOrder1()-mocOrder1) * shiftOrder1();
         val1 = (val1>>>shift ) << shift;
         val2   = (((val2>>>shift )+1L)   << shift) -1L;
      }
      range.add(val1, val2+1L, r);
      resetCache();
   }
   
   /** Add one cell to the Moc2D.
    * @param order1 Order of the cell (first dimension)
    * @param val1 Value of the cell (first dimension)
    * @param order2 Order of the cell (second dimension)
    * @param val2 Value of the cell (second dimension)
    * @throws Exception
    */
   public void add(int order1, long val1, int order2, long val2) throws Exception  { add(order1,val1,val1, order2, val2,val2); }
   
   /** Add a list of consecutive Moc cells.
    * @param order1 Order of the cells (first dimension)
    * @param firstVal1 First value (first dimension)
    * @param lastVal1 Last value (included) (first dimension)
    * @param order2 Order of the cells (second dimension)
    * @param firstVal2 First value (second dimension)
    * @param lastVal2 Last value (included) (second dimension)
    */
   public void add(int order1,long firstVal1, long lastVal1, int order2,long firstVal2, long lastVal2 ) throws Exception {
      int shift;
      
      // FIRST dimension
      int mocOrder1 = protoDim1.mocOrder;
      
      // Do we have to degrade the order/val (if mocOrder is smaller than the order) ?
      if( mocOrder1!=-1 && mocOrder1<order1 ) {
         shift = (order1-mocOrder1) * shiftOrder1();
         firstVal1 = (firstVal1>>>shift ) << shift;
         lastVal1   = (((lastVal1>>>shift )+1L)   << shift) -1L;
      }
      
      // Values converted to the maxOrder as range
      shift = ( maxOrder1()-order1 ) * shiftOrder1();
      long start1 = firstVal1<<shift;
      long end1 =  (lastVal1+1L)<<shift;
      
      // SECOND dimension
      int mocOrder2 = protoDim2.mocOrder;
      
      // Do we have to degrade the order/val (if mocOrder is smaller than the order) ?
      if( mocOrder2!=-1 && mocOrder2<order2 ) {
         shift = (order2-mocOrder2) * shiftOrder2();
         firstVal2 = (firstVal2>>>shift ) << shift;
         lastVal2   = (((lastVal2>>>shift )+1L)   << shift) -1L;
      }
      
      // Values converted to the maxOrder as range
      shift = ( maxOrder2()-order2 ) * shiftOrder2();
      long start2 = firstVal2<<shift;
      long end2 =  (lastVal2+1L)<<shift;
    
      Range r = new Range();
      r.append(start2,end2);
      
      range.add(start1,end1, r);
      resetCache();
   }
   
   /** Return true in case of equality (only check data - no properties, nor MocOrder) */
   public boolean equals( Object o) {
      if( o==this ) return true;
      if( o==null || !(o instanceof Moc2D) ) return false;
      Moc2D moc = (Moc2D) o;
      if( cDim1()!=moc.cDim1() ) return false;
      if( cDim2()!=moc.cDim2() ) return false;
      return range.equals( ((Moc2D)o).range); 
   }
   
   public int hashCode() { 
      if( cacheHashCode== -1 ) cacheHashCode=range.hashCode(); 
      return cacheHashCode;
   }
   
   /** Deep copy */
   protected void clone1( Moc moc ) throws CloneNotSupportedException {
      if( !(moc instanceof Moc2D) ) throw new CloneNotSupportedException("Uncompatible type of MOC for clone. Must derived from Moc2D");
      super.clone1( moc );
      Moc2D m = (Moc2D)moc;
      m.protoDim1 = (Moc1D)protoDim1.clone();
      m.protoDim2 = (Moc1D)protoDim2.clone();
      m.range= (range==null) ? null : new Range2(range);
   }
   
   /** Return true if the Moc is empty (no coverage) */
   public boolean isEmpty() { return range.sz==0; }
      
   /** Return true if the Moc is full (full coverage) */
   public boolean isFull() {
      if( !( range.sz==2 && range.r[0]==0 && range.r[1]==protoDim1.maxVal() )) return false;
      Moc1D m;
      m = (Moc1D)protoDim2.dup();
      m.setRangeList( range.rr[0] );
      return m.isFull();
   }

   /** Return the coverage pourcentage of the Moc */
   public double getCoverage() {
      // NOT YET IMPLEMENTED
      return -1;
   }
   
   /** Recalculates the metrics associated with the MOC hierarchical view: 
    * the number of hierarchical cells, the deepest order used... */
   protected void computeHierarchy() {
      int nb=0;
      int deep=0;
      for( MocCell mc : this ) {
         nb += mc.moc.getNbCells();
         int d=mc.moc.getDeepestOrder();
         if( d>deep ) deep=d;
      }
      cacheNbCells = nb;
      cacheDeepestOrder=deep;
   }
   
   /** Return the number of ranges - first dimension */
   public int getNbRanges() { return range.sz/2; }
   
   /** Return approximatively the amount of memory used for storing this MOC in RAM (in bytes) */
   public long getMem() { return range.getMem(); }

   /** Clear the MOC - data only (not the properties, nor the mocOrder) */
   public void clear() { 
      super.clear();
      range = new Range2();
   }
   
   /** Degrades the resolution(s) of the MOC until the RAM size of the MOC is reduced under the specified maximum (expressed in bytes). */
   public void reduction( long maxSize) throws Exception { reduction( maxSize, null); }
   
   /** Degrades the resolution(s) of the MOC until the RAM size of the MOC is reduced under the specified maximum (expressed in bytes).
    * @param maxMB size max in MB
    * @param priority Indicates the dimensions to be degraded as a priority in the form of a list of the dimension's signature characters
    *                 ex: "t" for time only, "s" space only, "st"-both alternatively, space first (default), "ttts", ...
    */
   public void reduction( long maxMB, String priority) throws Exception { 
      if( maxMB<=0 ) throw new Exception("negative or null size not allowed");
      if( priority==null || priority.trim().length()==0 ) { priority=(cDim2()+"")+(cDim1()+""); }
      while( getMem()>maxMB && (getMocOrder1()>0 || getMocOrder2()>0) ) {
         char c = priority.charAt(0);
         if( c==cDim1() && getMocOrder1()>0 ) setMocOrder1( getMocOrder1()-1 );
         else if( c==cDim2() && getMocOrder2()>0 ) setMocOrder2( getMocOrder2()-1 );
         else throw new Exception("Unknown MOC signature ["+c+"]");
         priority = priority.substring(1)+(c+"");
      }
   }
   
   /** Generic operations: 0-union, 1-intersection, 2-subtraction */
   protected Moc2D operation(Moc moc, int op) throws Exception {
      Moc2D m = (Moc2D)moc;
      Moc2D res = (Moc2D)dup();
      // Faudrait-il prendre en compte un minorder ? => res.setMinOrder(  Math.min(minOrder,m.minOrder) );
      res.setMocOrder1( getMocOrder4op( getMocOrder1(), m.getMocOrder1() ) );
      res.setMocOrder2( getMocOrder4op( getMocOrder2(), m.getMocOrder2() ) );
      
      switch(op) {
         case 0 : res.range = range.union(m.range);        break;
         case 1 : res.range = range.intersection(m.range); break;
         case 2 : res.range = range.difference(m.range);   break;
      }
      res.range.trimSize();
      return res;
   }
   
   /** Write MOC to an output stream in bASCII serialization */
   public void writeASCII(OutputStream out) throws Exception {
      if( isEmpty() ) return;
      int maxOrder2 = 0;
      int nbRanges = getNbRanges();
      boolean flagNL = nbRanges>MAXWORD;
      int order1 = getMocOrder1();
      int shift = (maxOrder1()-order1)* shiftOrder1();
      int j=0;
      boolean lock=false;

      StringBuilder res= new StringBuilder(1000);
      for( int i=0; i<nbRanges; i++ ) {
         if( !lock ) j=i;
         
         // Writing the range of the first dimension (at the specific resolution)
         long deb = range.r[i*2] >>> shift;
         long fin = (range.r[i*2+1]-1) >>> shift;
         if( i>0 ) res.append( flagNL ? CR:" ");
         if( !lock ) res.append(cDim1()+""+order1+"/");
         res.append(deb+ (fin==deb?"":"-"+fin));
         
         // Is the next Moc dim 2 the same ? => we factor
         if( i<nbRanges-1 && range.rr[j].equals(range.rr[i+1]) ) { lock=true; continue; }
         lock=false;
         
         // Writing the Moc of the second dimension associated with the range
         res.append( flagNL ? CR:" ");
         res.append(cDim2());
         writeASCIIFlush(out,res,false);
         Moc1D mocDim2 = (Moc1D)protoDim2.dup();
         mocDim2.setRangeList( range.rr[i] );
         int order=writeASCII(out,mocDim2,flagNL, true);
         if( order>maxOrder2 ) maxOrder2=order;
      }

      // Add moc orders at the end of the stream if necessary
      if( maxOrder2!=getMocOrder2() ) {
         res.append(flagNL ? CR : " " );
         res.append(cDim1()+""+order1+"/ "+cDim2()+getMocOrder2()+"/");
         if( flagNL ) res.append(CR);
      }

      writeASCIIFlush(out,res,false);
   }
   
   /** Write MOC to an output stream in JSON serialization (non IVOA standard) */
   public void writeJSON(OutputStream out) throws Exception { 
      throw new Exception("Not yet implemented");
   }
   
   /** Return the number of bytes used for coding each FITS value (=> always long) */
   public int sizeOfCoding() { return 8; }

   /** Return the number of values to write in FITS serialization */
   public int getNbCoding() {
      int nb = 0;
      for( int i=0; i<range.sz; i+=2 ) {
         nb += 2L;
         if( i>0 && range.rr[i/2].equals(range.rr[i/2-1])  ) continue;
         nb += range.rr[i/2].sz;
      }
      return nb;
   }
   
   /** Provide an Iterator on the MOC cell List (range highest order architecture for Moc2D)
    * @param flagRange not use for Moc2D
    * @return mocCell => dim,order,startVal,endVal,Moc1D
    */
   public Iterator<MocCell> cellIterator(boolean flagRange) { return new Dim2Iterator(  ); }

   private class Dim2Iterator implements Iterator<MocCell> {
      int i;
      int order;
      long shift;
      char cdim;

      Dim2Iterator() {
         i=0;
         order = getMocOrder1();
         shift = (maxOrder1() - order) * shiftOrder1();
         cdim = cDim1();
      }

      public boolean hasNext() { return i<range.sz/2; }

      public MocCell next() {
         if( !hasNext() ) return null;
         Moc1D moc = (Moc1D) protoDim2.dup();
         moc.setRangeList( range.rr[i] );
         MocCell cell = new MocCell();
         cell.dim = cdim;
         cell.order = order;
         cell.start = range.r[i*2] >>> shift;
         cell.end   = range.r[i*2+1] >>> shift;
         cell.moc = moc;
         i++;
         return cell;
      }

      public void remove() {  }
   }


   /********************************************************* ASCII & JSON parser *******************************************/
   

   // To build as you go the STMOC via addToken
   private StringBuilder buf=null;   // The reading buffer
   private Moc1D moc1=null;          // The last Moc1D read during parsing
   private boolean inDim1=false;     // Specify the state of the parsing

   protected void addToken(String token) throws Exception {
      
      // End of treatment or new dimension 1?
      if( token==null || token.charAt(0)==cDim1() ) {

         // Check dimension interleaving
         if( token!=null ) {
            if( inDim1 ) throw new Exception("ASCII Moc syntax error");
            inDim1 = !inDim1;
         }

         // Insertion of the previous couple (range,moc1D) if existing 
         if( moc1!=null ) {
            // I create the MOC dim2 of the buffer
            Moc1D moc2 = (Moc1D)protoDim2.dup();
            moc2.add( buf.toString() );
            // I memorize its mocOrder if it is superior to the previous ones
            int order2 = moc2.getMocOrder();
            if( order2>protoDim2.getMocOrder()) protoDim2.setMocOrder( order2 );
            
           // I associate this Moc dim2 with the dim1 ranges (no copies)
            for( int i=0; i<moc1.range.sz; i+=2 ) {
               range.append(moc1.range.r[i], moc1.range.r[i+1], moc2.range );
            }
            
            moc1=null;
            buf=null;
         }
         
         // Storage in a buffer of the content up to dimension 2 (without the prefix of the dimension)
         if( token!=null ) buf = new StringBuilder( token.substring(1) );
         
      // New dimension 2 ?
      } else if( token.charAt(0)==cDim2() ) {
         
         // Check dimension interleaving
         if( !inDim1 ) throw new Exception("ASCII Moc syntax error");
         inDim1 = !inDim1;
         
         // Moc dim1 generation of buffered content
         moc1 = (Moc1D) protoDim1.dup();
         moc1.add( buf.toString() );
         // I memorize its mocOrder if it is superior to the previous ones
         int order1 = moc1.getMocOrder();
         if( order1>protoDim1.getMocOrder() ) protoDim1.setMocOrder( order1 );

         // Storage of the content up to the next dimension 1 (without the prefix of the dimension)
         buf = new StringBuilder( token.substring(1) );
         
      //  Storage in a buffer of the current token
      } else {
         if( buf==null ) throw new Exception("Moc syntax error => token ["+token+"]");
         buf.append(' ');
         buf.append(token);
      }
   }
   
}

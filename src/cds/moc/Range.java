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


import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;

/**
 * Adaptation & extension of RangeSet from Healpix.essentials lib (GNU General Public License)
 * from Martin Reinecke [Max-Planck-Society] built from Jan Kotek's "LongRange" class
 * @version 1.0 - april 2019
 * @author P.Fernique [CDS]
 */
public class Range {
   
   /** Sorted list of interval boundaries. */
   public long[] r;                // list of range [min;max[ ...
   public int sz;                  // Number of entries (=> ranges/2)
   
   /************************************** Fernique extension *********************************************************************/
   
   /** Construct new object with the provided buffer (not copy). */
   public Range(long[] buf) { this(buf,buf.length); }
   public Range(long[] buf, int size) {
      r = buf;
      this.sz = size;
   }
   
   /** Sort and remove useless ranges and trim the buffer  => Warning: not thread safe */
   public void sortAndFix() {

      // On remplit un tableau externe pour pouvoir le trier
      // par intervalles croissants, et le plus grand en premier si égalité sur le début de l'intervalle
      ArrayList<Ran> list = new ArrayList<>( sz );
      for( int j=0; j<sz; j+=2 ) list.add( new Ran( r[j], r[j+1] ) );
      Collections.sort(list);

      // On recopie les intervalles en enlevant tout ce qui n'est pas nécessaire
      long r1[] = new long[ list.size()*2 ];
      long min=-1,max=-1;
      int n=0;
      for( Ran ran : list ) {
         if( ran.min>max ) {
            if( max!=-1 ) { r1[n++]=min; r1[n++]=max; }
            min = ran.min;
            max = ran.max;
         } else {
            if( ran.max>max ) max=ran.max;
         }
      }
      if( max!=-1 ) { r1[n++]=min; r1[n++]=max; }

      // On remplace le vecteur original
      r=r1;
      sz=n;
   }

   private class Ran implements Comparable<Ran> {
      long min,max;
      Ran(long min,long max) { this.min=min; this.max=max; }
      public int compareTo(Ran o) {
         return o.min<min ? 2 : o.min>min ? -2 : o.max>max ? 1 : o.max<max ? -1 : 0;
      }
   }
   
   /** RAM usage (in bytes) */
   public long getMem() { return r==null ? 0 : r.length*8L; }


   /************************************* Original code *********** Martin Reinecke [Max-Planck-Society] built from Jan Kotek's "LongRange"*********************/

   /** Default constuctor with initial space for 4 ranges. */
   public Range() { this(4); }
  
   /** Construct new object with initial capacity for a given number of ranges. */
   public Range(int cap) {
      if (cap<0) throw new IllegalArgumentException("capacity must be positive");
      r = new long[cap<<1];
      sz=0;
   }
  
   /** Construct new object from another Range */
   public Range(Range other) {
      sz=other.sz;
      r = new long[sz];
      System.arraycopy(other.r,0,r,0,sz);
   }

  /** Checks the object for internal consistency. If a problem is detected,
      an IllegalArgumentException is thrown. */
   public void checkConsistency() {
      if( (sz&1)!=0 ) throw new IllegalArgumentException("invalid number of entries");
      for (int i=1; i<sz; ++i) {
         if (r[i]<=r[i-1]) throw new IllegalArgumentException("inconsistent entries");
      }
   }

   /** Modify the size of the vector */
   public void resize(int newsize) {
      if( newsize<sz ) throw new IllegalArgumentException("requested array size too small");
      if( newsize==r.length ) return;
      long[] nRange = new long[newsize];
      System.arraycopy(r,0,nRange,0,sz);
      r = nRange;
   }

   /** Make sure the object can hold at least the given number of entries. */
   public void ensureCapacity(int cap) {
      if (r.length<cap) resize (Math.max(2*r.length,cap));
   }

   /** Shrinks the array for the entries to minimum size. */
   public void trimSize() { resize(sz); }

   /** Shrinks the array for the entries to minimum size, if it is more than
      twice the minimum size */
   public void trimIfTooLarge() { if (r.length-sz>=sz) resize(sz); }

   /** Returns an internal representation of the interval a number belongs to.
      @param val number whose interval is requested
      @return interval number, starting with -1 (smaller than all numbers in the
      Range), 0 (first "on" interval), 1 (first "off" interval etc.), up to
      (and including) sz-1 (larger than all numbers in the Range). */
   public int indexOf (long val) {
      int count=sz;
      int first=0;
      while( count>0 ) {
         int step=count>>>1;
         int it = first+step;
         if( r[it]<=val ) {
            first=++it;
            count-=step+1;
         } else count=step;
      }
      return first-1;
   }

   /** Append a single-value range to the object.
      @param val value to append */
   public void append(long val) { append(val,val+1); }

   /** Append a range to the object.
      @param a first long in range
      @param b one-after-last long in range */
   public void append (long a, long b) {
      if (a>=b) return;
      if( sz>0 && a<=r[sz-1] ) {
         if (a<r[sz-2] ) throw new IllegalArgumentException("bad append operation");
         if (b>r[sz-1] ) r[sz-1]=b;
         return;
      }
      ensureCapacity(sz+2);

      r[sz] = a;
      r[sz+1] = b;
      sz+=2;
   }

   /** Append an entire range set to the object. */
   public void append(Range other) {
      for (int i=0; i<other.sz; i+=2) append(other.r[i],other.r[i+1]);
   }

   /** @return number of ranges in the set. */
   public int nranges() { return sz>>>1; }

   /** @return true if no entries are stored, else false. */
   public boolean isEmpty() { return sz==0; }

   /** @return first number in range iv. */
   public long begins(int i) { return r[2*i]; }
   
   /** @return one-past-last number in range iv. */
   public long ends(int i) { return r[2*i+1]; }

   /** Remove all entries in the set. */
   public void clear() { sz=0; }

   /** Push a single entry at the end of the entry vector. */
   public void push(long v) {
      ensureCapacity(sz+1); 
      r[sz++]=v;
   }

   /** Estimate a good strategy for set operations involving two Ranges. */
   private static int strategy( int sza, int szb) {
      final double fct1 = 1.;
      final double fct2 = 1.;
      int slo = sza<szb ? sza : szb;
      int shi = sza<szb ? szb : sza;
      double cost1 = fct1 * (sza+szb);
      double cost2 = fct2 * slo * Math.max(1.,ilog2(shi));
      return (cost1<=cost2) ? 1 : (slo==sza) ? 2 : 3;
   }
  
   /** Integer base 2 logarithm. */
   static public int ilog2(long arg) {
      return 63-Long.numberOfLeadingZeros( Math.max(arg,1L) );
   }

   private static boolean generalAllOrNothing1( Range a, Range b, boolean flip_a, boolean flip_b) {
      boolean state_a=flip_a, state_b=flip_b, state_res=state_a||state_b;
      int ia=0, ea=a.sz, ib=0, eb=b.sz;
      boolean runa = ia!=ea, runb = ib!=eb;
      while( runa||runb ) {
         long va = runa ? a.r[ia] : 0L;
         long vb = runb ? b.r[ib] : 0L;
         boolean adv_a = runa && (!runb || (va<=vb));
         boolean adv_b = runb && (!runa || (vb<=va));
         if( adv_a) { state_a=!state_a; ++ia; runa = ia!=ea; }
         if( adv_b) { state_b=!state_b; ++ib; runb = ib!=eb; }
         if( (state_a||state_b)!=state_res ) return false;
      }
      return true;
   }

   private static boolean generalAllOrNothing2( Range a, Range b, boolean flip_a, boolean flip_b) {
      int iva = flip_a ? 0 : -1;
      while( iva<a.sz) {
         // implies that flip_a==false
         if( iva==-1 ) { if( !flip_b || b.r[0]<a.r[0] ) return false; }
         // implies that flip_a==false
         else if( iva==a.sz-1 ) { if( !flip_b || b.r[b.sz-1]>a.r[a.sz-1]) return false; }
         else {
            int ivb=b.indexOf(a.r[iva]);
            if( ivb!=b.sz-1 && b.r[ivb+1]<a.r[iva+1] ) return false;
            if( flip_b==((ivb&1)==0) ) return false;
         }
         iva+=2;
      }
      return true;
   }

   protected static boolean generalAllOrNothing( Range a, Range b, boolean flip_a, boolean flip_b) {
      if( a.isEmpty() ) return flip_a ? true : b.isEmpty();
      if( b.isEmpty() ) return flip_b ? true : a.isEmpty();
      int strat = strategy( a.nranges(), b.nranges() );
      return strat==1 ? generalAllOrNothing1(a,b,flip_a,flip_b) :
             strat==2 ? generalAllOrNothing2(a,b,flip_a,flip_b) :
                        generalAllOrNothing2(b,a,flip_b,flip_a);
   }

   /** Internal helper function for constructing unions, intersections
      and differences of two Ranges. */
   private static Range generalUnion1( Range a, Range b, boolean flip_a, boolean flip_b) {
      Range res = new Range();
      boolean state_a=flip_a, state_b=flip_b, state_res=state_a||state_b;
      int ia=0, ea=a.sz, ib=0, eb=b.sz;
      boolean runa = ia!=ea, runb = ib!=eb;
      while( runa||runb ) {
         long va = runa ? a.r[ia] : 0L;
         long vb = runb ? b.r[ib] : 0L;
         boolean adv_a = runa && (!runb || (va<=vb));
         boolean adv_b = runb && (!runa || (vb<=va));
         if( adv_a ) { state_a=!state_a; ++ia; runa = ia!=ea; }
         if( adv_b ) { state_b=!state_b; ++ib; runb = ib!=eb; }
         if( (state_a||state_b)!=state_res ) {
            res.push(adv_a ? va : vb);
            state_res = !state_res;
         }
      }
      return res;
   }
   
   /** Internal helper function for constructing unions, intersections
      and differences of two Ranges. */
   private static Range generalUnion2( Range a, Range b, boolean flip_a, boolean flip_b) {
      Range res = new Range();
      int iva = flip_a ? 0 : -1;
      while( iva<a.sz ) {
         int ivb = (iva==-1) ? -1 : b.indexOf(a.r[iva]);
         boolean state_b = flip_b^((ivb&1)==0);
         if( iva>-1 && !state_b ) res.push(a.r[iva]);
         while( ivb<b.sz-1 && (iva==a.sz-1 || b.r[ivb+1]<a.r[iva+1]) ) { 
            ++ivb; state_b=!state_b; 
            res.push(b.r[ivb]);
         }
         if( iva<a.sz-1 && !state_b ) res.push(a.r[iva+1]);
         iva+=2;
      }
      return res;
   }


   private static Range generalUnion( Range a, Range b, boolean flip_a, boolean flip_b) {
      if( a.isEmpty() ) return flip_a ? new Range() : new Range(b);
      if( b.isEmpty() ) return flip_b ? new Range() : new Range(a);
      int strat = strategy (a.nranges(), b.nranges());
      return strat==1 ? generalUnion1(a,b,flip_a,flip_b) :
             strat==2 ? generalUnion2(a,b,flip_a,flip_b) :
                        generalUnion2(b,a,flip_b,flip_a);
   }

   /** Return the union of this Range and other. */
   public Range union( Range other) { return generalUnion (this,other,false,false); }

   /** Return the intersection of this Range and other. */
   public Range intersection( Range other) { return generalUnion (this,other,true,true); }

   /** Return the difference of this Range and other. */
   public Range difference( Range other) { return generalUnion (this,other,true,false); }
   
   public boolean contains( Range other) { return generalAllOrNothing(this,other,false,true); }

   /** Returns true if there is overlap between the set and "other", else false. */
   public boolean overlaps( Range other) { return !generalAllOrNothing(this,other,true,true); }

   /** Returns true if a is contained in the set, else false. */
   public boolean contains( long a) { return (indexOf(a)&1)==0; }
   
   public boolean overlaps (long a,long b) {
      int res=indexOf(a);
      if( (res&1)==0 ) return true;
      if( res==sz-1 ) return false; // beyond the end of the set
      return r[res+1]<b;
   }

   /** Returns true if all numbers [a;b[ are contained in the set, else false. */
   public boolean contains( long a,long b ) {
      int res=indexOf(a);
      if( (res&1)!=0 ) return false;
      return (b<=r[res+1]);
   }
  
   public boolean equals( Object obj ) {
      if( this == obj ) return true;
      if( obj==null || !(obj instanceof Range) ) return false;
      Range other = (Range) obj;
      if( other.sz!=sz ) return false;
      for (int i=0; i<sz; ++i) {
         if (other.r[i]!=r[i]) return false;
      }
      return true;
   }

// On va faire confiance à Java (P.Fernique)
//   public int hashCode() {
//      int result = Integer.valueOf(sz).hashCode();
//      for (int i=0; i<sz; ++i) {
//         result = 31 * result + Long.valueOf(r[sz]).hashCode();
//      }
//      return result;
//   }

   /** @return total number of values (not ranges) in the set. */
   public int nval() {
      int res = 0;
      for (int i=0; i<sz; i+=2) res+=r[i+1]-r[i];
      return res;
   }

   /** Internal helper function for building unions and differences of the
      Range with a single range. */
   private void addRemove( long a, long b, int v) {
      int pos1=indexOf(a);
      int pos2=indexOf(b);
      if( pos1>=0 && r[pos1]==a ) --pos1;
      // first to delete is at pos1+1; last is at pos2
      boolean insert_a = (pos1&1)==v;
      boolean insert_b = (pos2&1)==v;
      int rmstart= pos1+1+(insert_a ? 1 : 0);
      int rmend  = pos2 - (insert_b?1:0);

      if( ((rmend-rmstart)&1)==0) throw new IllegalArgumentException("cannot happen: "+rmstart+" "+rmend);

      // insert
      if( insert_a && insert_b && pos1+1>pos2) {
         ensureCapacity(sz+2);
         System.arraycopy(r,pos1+1,r,pos1+3,sz-pos1-1); // move to right
         r[pos1+1]=a;
         r[pos1+2]=b;
         sz+=2;
      } else {
         if( insert_a ) r[pos1+1]=a;
         if( insert_b ) r[pos2]=b;
         if( rmstart!=rmend+1 ) System.arraycopy(r,rmend+1,r,rmstart,sz-rmend-1); // move to left
         sz-=rmend-rmstart+1;
      }
   }

  /** After this operation, the Range contains the intersection of itself and [a;b[. */
   public void intersect( long a, long b) {
      int pos1=indexOf(a);
      int pos2=indexOf(b);
      if( pos2>=0 && r[pos2]==b ) --pos2;
      // delete all up to pos1 (inclusive); and starting from pos2+1
      boolean insert_a = (pos1&1)==0;
      boolean insert_b = (pos2&1)==0;

      // cut off end
      sz=pos2+1;
      if( insert_b ) r[sz++]=b;

      // erase start
      if( insert_a ) r[pos1--]=a;
      if (pos1>=0) System.arraycopy(r,pos1+1,r,0,sz-pos1-1); // move to left

      sz-=pos1+1;
      if( (sz&1)!=0 ) throw new IllegalArgumentException("cannot happen");
   }
   
   public void add( Range m ) {
      if( m.sz==0 ) return;
      if( sz==0 || m.r[0]>=r[sz-1] ) append(m);
      else {
         for( int i=0; i<m.sz; i+=2 ) addRemove( m.r[i], m.r[i+1],1);
      }

   }

   /** After this operation, the Range contains the union of itself and [a;b[. */
   public void add( long a, long b) {
      if( sz==0 || a>=r[sz-1] ) append(a,b);
      else addRemove(a,b,1);
   }

   /** After this operation, the Range contains the union of itself and [a;a+1[. */
   public void add( long a) {
      if( sz==0 || a>=r[sz-1] ) append(a,a+1);
      else addRemove(a,a+1,1);
   }

   /** After this operation, the Range contains the difference of itself and [a;b[. */
   public void remove( long a, long b) { addRemove(a,b,0); }

   /** After this operation, the Range contains the difference of itself and  [a;a+1[. */
   public void remove( long a) { addRemove(a,a+1,0); }

   /** Creates an array containing all the numbers in the Range.
      Not recommended, because the arrays can become prohibitively large.
      It is preferrable to use a ValueIterator or explicit loops. */
   public long[] toArray() {
      long[] res = new long[ nval() ];
      int ofs=0;
      for (int i=0; i<sz; i+=2) {
         for (long j=r[i]; j<r[i+1]; ++j) res[ofs++]=j;
      }
      return res;
   }
   
   public static Range fromArray( long[] v ) {
      Range res = new Range();
      for (int i=0; i<v.length; i++) res.append(v[i]);
      return res;
   }

   public String toString() {
      StringBuilder s = new StringBuilder();
      s.append("{ ");
      for (int i=0; i<sz; i+=2) {
         s.append("[").append(r[i]).append(";").append(r[i + 1]).append("[");
         if (i<sz-2) s.append(",");
      }
      s.append(" }");
      return s.toString();
   }
   
   
   /** Interface describing an iterator for going through all values in
   a Range object. */
   public interface ValueIterator {
      public boolean hasNext();
      public long next();
   }

   static final protected ValueIterator EMPTY_ITER = new ValueIterator() {
      public boolean hasNext() { return false; }
      public long next() { throw new NoSuchElementException(); }
   };


   /** Returns a ValueIterator, which iterates over all individual numbers
      in the Range. */
   public ValueIterator valueIterator() {
      if(sz == 0) return EMPTY_ITER;

      return new ValueIterator() {
         int pos = 0;
         long value = (sz>0) ? r[0] : 0;

         public boolean hasNext() { return (pos<sz); }

         public long next() {
            if (pos>sz) throw new NoSuchElementException();
            long ret = value;
            if( ++value==r[pos+1] ) {
               pos+=2;
               if (pos<sz)  value = r[pos];
            }
            return ret;
         }
      };
   }

}


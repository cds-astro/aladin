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

import java.util.Arrays;

/**
 * Fast array of integers
 * @version 1.0 - oct 2011
 * @author P.Fernique [CDS]
 */
public final class IntArray extends Array {
   
   private int [] array;      // Array of integers (real size is given by "size")

   /** Create a free array of longs */
   public IntArray(int bloc) { 
      array=null;
      size=0;
      sorted=true;
      sizeBloc = bloc;
   }
   
   /** Create an array of integers initializing with v[]
    * @param v initial values of the array
    */
   public IntArray(int[] v) {
      array = v;
      size = v.length;
      sizeBloc=DEFAULTBLOC;
      sorted=true;
   }
   
   /** Deep copy */
   public Array clone() {
      IntArray a = new IntArray(sizeBloc);
      if( array==null ) a.array=null;
      else {
         a.array = new int[array.length];
         System.arraycopy(array, 0, a.array, 0, size);
      }
      a.size=size;
      a.sorted=sorted;
      return a;
   }
   
   /** Equality test */
   public boolean equals(Array a) {
      if( this==a ) return true;
      if( size!=a.size ) return false;
      if( size==0) return true;
      sort();
      a.sort();
      for( int i=0; i<size; i++ ) {
         if( array[i]!=((IntArray)a).array[i] ) return false;
      }
      return true;
   }
   
   /** Provide the java reference on the array
    * (real size is provided by getSize() and not by .length)
    * @return array of longs
    */
   public int [] seeArray() { return array; }
   
   public long get(int i) { return array[i]; }

   public void set(int i,long v) { array[i]=(int)v; }

   /** Size of the array in bytes */
   public long getMem() { return 9+(array==null?0:array.length*4); }

   /** Size of the array in compressed mode
    * => consecutive values are memorized as range (2 values)
    * @return Number of values
    */
   public int getSizeCompressed() {
      if( array==null ) return 0;
      int reduce=0;
      boolean oConsecutif=false;
      for( int i=1; i<size; i++ ) {
         boolean consecutif= ( array[i]==array[i-1] +1 );
         if( oConsecutif && consecutif ) reduce++;
         oConsecutif=consecutif;
      }
      return size-reduce;
   }

   /** Add a long. the array will be automatically extended if necessary
    * @param v value to add
    * @param testUnicity true for avoiding redundancy (slower)
    * @return true if the value has been added
    */
   public boolean add(long v,boolean testUnicity) { return add((int)v,testUnicity); }
   public boolean add(int v, boolean testUnicity) {
      int pos=-1;
      if( testUnicity && (pos=find(v))>=0 ) return false;
      adjustSize(1);
      if( testUnicity && sorted ) {
         pos = -(pos+1);
         System.arraycopy(array, pos, array, pos+1, size-pos);
         array[pos]=v;
         size++;
      } else {
         if( sorted ) sorted= size==0 || v>array[size-1];
         array[size++]=v;
      }
      return true;
   }
   
   /** Delete the first value equals to v
    * kept the array sorted if required
    * @param v the value to remove
    * @return true if the value has been removed
    */
   public boolean delete(long v) { return delete((int)v); }
   public boolean delete(int v) {
      int pos = find(v);
      if( pos<0 ) return false;
      if( !sorted ) array[pos] = array[size-1];
      else System.arraycopy(array, pos+1, array, pos, size-pos-1);
      size--;
      return true;
   }
   
   /** Delete the all values between v1 and v2 (inclusive)
    * kept the array sorted if required
    * @param v1 the low range value
    * @param v2 the high range value
    * @return true if at least one value has been removed
    */
   public boolean delete(long v1,long v2) { return delete((int)v1,(int)v2); }
   public boolean delete(int v1,int v2) {
      int i,j;
      for( i=j=0; i<size; i++ ) {
         if( array[i]<v1 || array[i]>v2 ) array[j++]=array[i];
      }
      size=j;
      return j!=i;
   }
   
   /** Delete three other brothers only of all of them are already present
    * (apart me)
    * @return false if not found
    */
   public boolean deleteBrothers(long me) { return deleteBrothers((int)me); }
   public boolean deleteBrothers(int me) {
      int firstBrother = me - me%4;
      int pos[] = new int[3];
      
      // Search three other brothers
      for( int j=0,i=0; i<4; i++,j++ ) {
         int v = firstBrother+i;
         if( v==me ) { j--; continue; }
         if( j==0 ) pos[j] = find(v);
         else {
            if( sorted ) { pos[j] = array[pos[j-1]+1]==v ? pos[j-1]+1 : -1; }
            else pos[j] = find(v);
         }
         if( pos[j]<0 || sorted && size-pos[j]<3-j ) return false;
      }
      
      // Delete three other brothers
      if( sorted ) {
         System.arraycopy(array, pos[2]+1, array, pos[0], size-(pos[2]+1));
      } else {
         for( int i=0; i<3; i++ ) {
            array[ pos[i] ] = array[ size-1-i ];
         }
      }
      size-=3;
      return true;
   }


   /** Return the index of the first value v. -1 if not found.
    * If the arrays is sorted, proceeds by bsearch (see bsearch doc)
    * @param v Value to find
    * @return index of the value, otherwise negative index
    */
   public int find(long v) { return find((int)v); }
   public int find(int v) {
      if( array==null ) return -1;
      int i=-1;
      if( sorted ) {
         if( size==0 || v<array[0] ) return -1;
         else if( v>array[size-1] ) return -(size+1);
         i= binarySearch(array,0,size, v);
      }
      else {
         for( i=0; i<size && array[i]!=v; i++ );
         if( i==size) i=-1;
      }
      return i;
   }
   
   /** Return true if at least one value of the range [vStart..vEnd] (included)
    * is found in the array
    * @param vStart range starting value
    * @param vEnd range ending value
    * @return true in case of success
    */
   public boolean intersectRange(long vStart, long vEnd) { 
      return intersectRange((int)vStart,(int)vEnd);
   }
   public boolean intersectRange(int vStart, int vEnd) {
      if( array==null ) return false;
      if( sorted ) {
         int pos1 = size==0 || vStart<array[0] ? -1 : vStart>array[size-1] ? -(size+1) : binarySearch(array,0, size, vStart);
         if( pos1>=0 ) return true;
         int pos2 = size==0 || vEnd<array[0] ? -1 : vEnd>array[size-1] ? -(size+1) : binarySearch(array,0, size, vEnd);
         if( pos2>=0 ) return true;
         return pos1!=pos2;
      } else {
         for( int i=0; i<size; i++ ) {
            long v = array[i];
            if( vStart<=v && v<=vEnd ) return true;
         }
      }
      return false;
   }
   
   // For supporting Java1.5
   private int binarySearch(int[] a, int fromIndex, int toIndex, int key) {
      int low = fromIndex;
      int high = toIndex - 1;
      while (low <= high) {
         int mid = (low + high) >>> 1;
         int midVal = a[mid];
         if (midVal < key) low = mid + 1;
         else if (midVal > key) high = mid - 1;
         else return mid;
      }
      return -(low + 1);
   }
   
   /** Sort the array (only if required) */
   public void sort() {
      if( array==null ) return;
      if( sorted ) return;
      Arrays.sort(array,0,size);
      sorted=true;
   }
   
   /** Remove the reservation room in the array */
   public void trim() {
      if( array==null || size==array.length ) return;
      int [] nArray = new int[size];
      System.arraycopy(array, 0, nArray, 0, size);
   }

   // Adjust the size of the array for adding n values. In if it is too small, copy the array
   // in a larger array.
   private void adjustSize(int n) {
      if( n==0 || array!=null && size+n < array.length ) return;
      int [] nArray = new int[ (1+ (size+n)/sizeBloc) * sizeBloc ];
      if( array!=null ) System.arraycopy(array, 0, nArray, 0, size);
      array=nArray;
      sizeBloc=sizeBloc+array.length/10;
      nArray=null;
   }
}


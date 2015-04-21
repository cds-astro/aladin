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

import java.util.Iterator;

/**
 * Fast array : see ShortArray, IntArray and LongArray classes
 * @version 1.0 - oct 2011
 * @author P.Fernique [CDS]
 */
public abstract class Array implements Iterable<Long>,Cloneable {
   
   static protected final int DEFAULTBLOC = 128;
   
   protected int size;          // Real array size
   protected boolean sorted;    // true if the array is sorted
   protected int sizeBloc;      // Bloc size for increasing the array
   
   /** Return true if the array is ascending sorted */
   public boolean isSorted() { return sorted; }
   
   /** Size of the array */
   public int getSize() { return size; }
   
   /** set the size of the array */
   public int setSize(int size) { return this.size=size; }
   
   public abstract Object clone();
   
   /** Provide the element i */
   public abstract long get(int i);
   
   /** set the element i */
   public abstract void set(int i,long v);
   
   /** Add a value, with or without checking unicity */
   public abstract boolean add(long v, boolean testUnicity);
   
   /** Delete value v from the array */
   public abstract boolean delete(long v);
   
   /** Delete range of values */
   public abstract boolean delete(long v1,long v2);
   
   /** Delete 3 others brothers if all present */
   public abstract boolean deleteBrothers(long me);
   
   /** Sort the array (if required) */
   public abstract void sort();
   
   /** Find index of value v, or return negative value (bsearch result)
    * if it is not present in the array */
   public abstract int find(long v);
   
   /** Return false if no value in the range [vStart..vEnd] 
    * is present in the array */ 
   public abstract boolean intersectRange(long vStart, long vEnd);
   
   /** Size of the array in bytes */
   public abstract long getMem();
   
   /** Return the size of the array in compressed mode */
   public abstract int getSizeCompressed();
   
   /** Trim the array in order to free the not required memory */
   public abstract void trim();
   
   /** Equality test */
   public abstract boolean equals(Array a);
   
   /** Provide an iterator on the array */
   public Iterator<Long> iterator() { return new ArrayIterator(); }
   
   // Création d'un itérator sur la liste des valeurs
   private class ArrayIterator implements Iterator<Long> {
      private int indice=0;
      public boolean hasNext() { return indice<size; }
      public Long next() { return  get(indice++); }
      public void remove() {  }

   }
}


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


import healpix.essentials.RangeSet;

/**
 * Extension of RangeSet from Healpix.essentials lib
 * @version 1.0 - april 2018
 * @author P.Fernique [CDS]
 */
public class Range extends RangeSet {
   
   public Range(int size) { super(size); }
   public Range( RangeSet rangeSet ) { super(rangeSet); }
   
   /** Dichotomic search in Range list
    * Rq: reimplementation of iiv(long) which is private in the RangeSet class (!!)
    * @param val value to find
    * @return index of the range containing the val. starting with -1 (smaller than all numbers in the
      RangeSet), 0 (first "on" interval), 1 (first "off" interval etc.), up to
      (and including) sz-1 (larger than all numbers in the RangeSet). 
    */
   public int getIndex(long val) {
      int count=sz, first=0;
      while (count>0) {
         int step=count>>>1, it = first+step;
         if (r[it]<=val) {
            first=++it;
            count-=step+1;
         } else count=step;
      }
      return first-1;
   }
   
   public Range union( RangeSet other ) { return new Range( super.union(other) ); }
   public Range intersection( RangeSet other ) { return new Range( super.intersection(other) ); }
   public Range difference( RangeSet other ) { return new Range( super.difference(other) ); }
   
}


//
// Licenced under GPLv2, see licence.txt
// (c)  Jan Kotek,
//

package org.asterope.healpix;

/**
 * An iterator over ranges which does not require object creation
 * <p>
 * !!Implementation must return sorted ranges in iterator!!
 */
public interface LongRangeIterator {

    /** move to next Range in iterator 
     * @return true if more ranges are in iterator, false if iterator reached end 
     */
    boolean moveToNext();
    
//    /**
//     * Skip values on RangeIterator until current first is >= to last. 
//     * Is typically faster then moveToNext() in cycle with condition (uses binary search).
//     * This goes only forward, no backward
//     * @param last 
//     * @return true if more ranges are in iterator, false if iterator reached end 
//     */
//    boolean skipTo(long last);

    /**
     * @return first item in current range (inclusive)
     * @throws java.util.NoSuchElementException if no more elements are found
     */
    long first();

    /**
     * @return last item in current range (inclusive)
     * @throws java.util.NoSuchElementException if no more elements are found
     */
    long last();

}
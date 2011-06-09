//
// Licenced under GPLv2, see licence.txt
// (c)  Jan Kotek,
//

package org.asterope.healpix;

/**
 *  This class represents iterators over collections of long values.
 *  It returns primitive value, so there is no boxing overhead
 *
 *  @see        java.util.Iterator
 */
public interface LongIterator {


    /**
     *  Indicates whether more long values can be returned by this
     *  iterator.
     *
     *  @return     <tt>true</tt> if more long values can be returned
     *              by this iterator; returns <tt>false</tt>
     *              otherwise.
     *
     *  @see        #next()
     */
    boolean hasNext();

    /**
     *  Returns the next long value of this iterator.
     *
     *  @return     the next long value of this iterator.
     *
     *  @throws java.util.NoSuchElementException
     *              if no more elements are available from this
     *              iterator.
     *
     *  @see        #hasNext()
     */
    long next();
    
}

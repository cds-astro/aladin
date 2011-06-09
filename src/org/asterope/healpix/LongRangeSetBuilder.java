//
// Licenced under GPLv2, see licence.txt
// (c) Jan Kotek,
//

package org.asterope.healpix;

/**
 * Builder for LongRangeSet . LongRangeSet is unmodifiable, this class is 
 * 'factory' to create new instances.
 * <P>
 * To keep it fast and simple, this fab only supports appending. Uour ranges must be already 
 * sorted. This work  for most of Healpix based operations.
 * <p>
 * LongRangeSet can also be constructed using {@link LongSet}    
 * 
 */
public class LongRangeSetBuilder {

	/**
	 * empty LongRangeSet
	 */
	public static final LongRangeSet EMPTY = new LongRangeSet(new long[0],0);
	
	/** sorted list of ranges.*/
    protected long[] ranges ;
    /** current position*/
    protected int pos = 0;
    
    public LongRangeSetBuilder(){
    	this(32);
    }

    /**
     * construct new builder with given array size
     * @param size 
     */
    public LongRangeSetBuilder(int arraySize) {
    	if(arraySize%2!=0) throw new IllegalArgumentException("not divide by 2");
    	ranges = new long[arraySize]; 
	}

    /** make sure underling array have at least given size*/ 
	public void ensureSize(int arraySize){
		if(arraySize%2!=0) throw new IllegalArgumentException("not divide by 2");
        // grow the array if necessary.
        if (ranges.length  < arraySize) {
            long[] newRanges = new long[arraySize];
            System.arraycopy(ranges,0,newRanges,0,ranges.length);
            ranges = newRanges;
        }
    }


	/** append single long into builder 
	 * @param first - long to append
	 */
    public void append(long first){
        appendRange(first,first);
    }

    /**
     * append range into builder
     * @param first long in range (inclusive)
     * @param last long in range(inclusive)
     */
    public void appendRange(long first, long last){
        if(first>last)
            throw new IllegalArgumentException("first > last");
        if(pos>0){        
        	if(first<lastFirst())
        		throw new IllegalArgumentException("first already added, ranges must be added sorted! oldFirst:"+lastFirst()+", newFirst:"+first);
        	if(first<last())
        		first= last();
        	if( last<last())
        		throw new IllegalArgumentException("last already added, ranges must be added sorted! oldLast:"+last()+", newLast:"+last);
            //special case, maybe just need to extend last bound
            if(last() == first||last() +1 == first){
            	ranges[pos-1] = last;
            	return;
            }
        }
            
       
        //make sure there is space
        if(pos + 2>ranges.length)
            ensureSize(ranges.length * 2);

        //insert
        ranges[pos] = first;
        ranges[pos+1] = last;
        pos+=2;
    }

	public long last() {
		return ranges[pos-1];
	}
	
	public long lastFirst() {
		return ranges[pos-2];
	}


    /**
     * appends all ranges from iterator
     * @param iter LongRangeIterator
     */
    public void appendRanges(LongRangeIterator iter){
        while(iter.moveToNext())
            appendRange(iter.first(),iter.last());
    }
    
    /**
     * append all ranges from given LongRangeSet
     * @param set LongRangeSet to append
     */
	public void appendRangeSet(LongRangeSet set) {
		appendRanges(set.rangeIterator());		
	}

    /** @return number of added ranges so far*/
    public int size(){
        return pos/2;
    }

    /**
     * Construct new LongRangeSet from appended values *
     * @return LongRangeSet with appended values
     */
    public LongRangeSet build(){
    	if(pos == 0)
    		return EMPTY;
        return new LongRangeSet(ranges,pos);
    }


}

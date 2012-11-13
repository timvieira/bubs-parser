package edu.ohsu.cslu.datastructs.vectors;

/**
 * Interface defining methods for bit-vectors indexed by <code>long</code> indices.
 * 
 * @author Aaron Dunlop
 */
public interface LargeBitVector extends BitVector, LargeVector {

    /**
     * @return an array containing the indices of all elements contained in this {@link BitVector}. Note that depending
     *         on the specific implementation, {@link #longValueIterator()} may be more efficient.
     */
    public long[] longValues();

    /**
     * @return an array containing the indices of all elements contained in this {@link BitVector}. Note that depending
     *         on the specific implementation, {@link #longValueIterator()} may be more efficient.
     */
    public Iterable<Long> longValueIterator();

    /**
     * Set-convention convenience method
     * 
     * @param i element whose presence in this set is to be tested
     * @return True if the specified element is contained in this set
     */
    public boolean contains(final long i);

}

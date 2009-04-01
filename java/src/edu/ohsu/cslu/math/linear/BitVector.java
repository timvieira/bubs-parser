package edu.ohsu.cslu.math.linear;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.BitSet;

/**
 * Extends the {@link Vector} interface with methods specific to handling of bit (binary) vectors.
 * 
 * A {@link BitVector} is generally useful to store binary feature vectors (e.g. for log-linear
 * modeling) and boolean sets.
 * 
 * In addition to the normal {@link Vector} methods, it also declares convenience methods named in
 * standard set convention.
 * 
 * @author Aaron Dunlop
 * @since Mar 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface BitVector extends Vector
{
    /**
     * Set-convention convenience method
     * 
     * @param toAdd element to add to the set
     */
    public void add(final int toAdd);

    /**
     * Set-convention convenience method
     * 
     * @param toAdd elements to add to the set
     */
    public void addAll(final int[] toAdd);

    /**
     * Set-convention convenience method
     * 
     * @param toAdd elements to add to the set
     */
    public void addAll(IntSet toAdd);

    /**
     * Set-convention convenience method, equivalent to performing
     * {@link #elementwiseMultiply(Vector)} on two {@link BitVector}s.
     * 
     * @param v vector to intersect
     */
    public BitVector intersection(BitVector v);

    /**
     * Set-convention convenience method
     * 
     * @param i element whose presence in this set is to be tested
     * @return True if the specified element is contained in this set
     */
    public boolean contains(final int i);

    /**
     * Set-convention convenience method
     * 
     * @param toRemove element to remove from the set
     * @return True if the specified element was contained in this set
     */
    public boolean remove(final int toRemove);

    /**
     * Set-convention convenience method
     * 
     * @param toRemove elements to remove from the set
     */
    public void removeAll(final int[] toRemove);

    /**
     * Set-convention convenience method
     * 
     * @param toRemove elements to remove from the set
     */
    public void removeAll(final IntSet toRemove);

    /**
     * @return an array containing the indices of all elements contained in this {@link BitSet}
     */
    public int[] values();
}
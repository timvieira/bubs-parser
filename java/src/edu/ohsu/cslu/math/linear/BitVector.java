package edu.ohsu.cslu.math.linear;

import it.unimi.dsi.fastutil.ints.IntSet;

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
    public abstract void add(final int toAdd);

    /**
     * Set-convention convenience method
     * 
     * @param toAdd elements to add to the set
     */
    public abstract void addAll(final int[] toAdd);

    /**
     * Set-convention convenience method
     * 
     * @param toAdd elements to add to the set
     */
    public abstract void addAll(IntSet toAdd);

    /**
     * Set-convention convenience method
     * 
     * @param i element whose presence in this set is to be tested
     * @return True if the specified element is contained in this set
     */
    public abstract boolean contains(final int i);

    /**
     * Set-convention convenience method
     * 
     * @param toRemove element to remove from the set
     * @return True if the specified element was contained in this set
     */
    public abstract boolean remove(final int toRemove);

    /**
     * Set-convention convenience method
     * 
     * @param toRemove elements to remove from the set
     */
    public abstract void removeAll(final int[] toRemove);

    /**
     * Set-convention convenience method
     * 
     * @param toRemove elements to remove from the set
     */
    public abstract void removeAll(final IntSet toRemove);

}
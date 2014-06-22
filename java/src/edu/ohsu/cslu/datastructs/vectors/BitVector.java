/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Extends the {@link Vector} interface with methods specific to handling of bit (binary) vectors.
 * 
 * A {@link BitVector} is generally useful to store binary feature vectors (e.g. for log-linear modeling) and boolean
 * sets.
 * 
 * In addition to the normal {@link Vector} methods, it also declares convenience methods named in standard set
 * convention.
 * 
 * @author Aaron Dunlop
 * @since Mar 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface BitVector extends Vector {

    /**
     * Set-convention convenience method (optional operation)
     * 
     * @param toAdd element to add to the set
     * @return true if this set did not already contain the specified element
     */
    public boolean add(final int toAdd);

    /**
     * Set-convention convenience method (optional operation)
     * 
     * @param toAdd elements to add to the set
     */
    public void addAll(final int[] toAdd);

    /**
     * Set-convention convenience method (optional operation)
     * 
     * @param toAdd elements to add to the set
     */
    public void addAll(IntSet toAdd);

    /**
     * Set-convention convenience method, equivalent to performing {@link #elementwiseMultiply(Vector)} on two
     * {@link BitVector}s.
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
     * Set-convention convenience method (optional operation)
     * 
     * @param toRemove element to remove from the set
     * @return True if the specified element was contained in this set
     */
    public boolean remove(final int toRemove);

    /**
     * Set-convention convenience method (optional operation)
     * 
     * @param toRemove elements to remove from the set
     */
    public void removeAll(final int[] toRemove);

    /**
     * Set-convention convenience method (optional operation)
     * 
     * @param toRemove elements to remove from the set
     */
    public void removeAll(final IntSet toRemove);

    /**
     * Returns the length of the vector. The length of a {@link BitVector} is either:
     * <ol>
     * <li>For implementations of fixed-size, such as {@link PackedBitVector}, the highest index which the vector can
     * store.
     * <li>For implementations of variable size, such as {@link MutableSparseBitVector}, the highest populated index +
     * 1.
     * </ol>
     * 
     * @return length.
     */
    public long length();

    /**
     * @return The number of non-0 dimensions
     */
    public int l0Norm();

    /**
     * @return an array containing the indices of all elements contained in this {@link BitVector}. Note that depending
     *         on the specific implementation, {@link #valueIterator()} may be more efficient.
     */
    public int[] values();

    /**
     * @return an {Iterable} containing the indices of all elements contained in this {@link BitVector}. Note that
     *         depending on the specific implementation, {@link #values()} may be more efficient.
     */
    public Iterable<Integer> valueIterator();
}

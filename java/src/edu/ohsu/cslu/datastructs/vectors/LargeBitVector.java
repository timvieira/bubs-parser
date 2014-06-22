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

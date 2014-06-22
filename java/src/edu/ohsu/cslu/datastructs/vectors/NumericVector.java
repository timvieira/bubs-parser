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

import it.unimi.dsi.fastutil.longs.LongSet;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

/**
 * Base interface for numeric vector classes (essentially all {@link Vector}s which are not {@link BitVector} s).
 * 
 * The methods defined here are applicable to any numeric representation, but not to a single bit.
 * 
 * @author Aaron Dunlop
 * @since Sep 17, 2008
 * 
 *        $Id$
 */
public interface NumericVector extends Vector {

    /**
     * @return a set containing the indices of the dimensions populated in this vector.
     */
    public abstract LongSet populatedDimensions();

    /**
     * Returns the result of multiplying the specified matrix by this vector.
     * 
     * @param m Matrix
     * @return m x v
     */
    public NumericVector multiply(Matrix m);

    /**
     * @param addend The value to add
     * @return a new vector scaled by the provided addend
     */
    public NumericVector scalarAdd(float addend);

    /**
     * @param addend The value to add
     * @return a new vector scaled by the provided addend
     */
    public NumericVector scalarAdd(int addend);

    /**
     * @param multiplier
     * @return a new vector scaled by the provided multiplier
     */
    public NumericVector scalarMultiply(float multiplier);

    /**
     * @param multiplier
     * @return a new vector scaled by the provided multiplier
     */
    public NumericVector scalarMultiply(int multiplier);

    /**
     * @return the maximum value present in the vector
     */
    public float max();

    /**
     * @return the maximum value present in the vector as an int
     */
    public int intMax();

    /**
     * @return the index of the maximum value present in the vector; i such that {@link #getInt(int)} will return the
     *         same value as {@link #max()}.
     */
    public int argMax();

    /**
     * @return the minimum value present in the vector
     */
    public float min();

    /**
     * @return the minimum value present in the vector as an int
     */
    public int intMin();

    /**
     * @return the index of the minimum value present in the vector; i such that {@link #getInt(int)} will return the
     *         same value as {@link #min()}.
     */
    public int argMin();

}

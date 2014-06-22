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
 * Interface for large vectors, indexed by longs instead of ints
 * 
 * @author Aaron Dunlop
 */
public interface LargeVector extends Vector {

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as an integer
     */
    public int getInt(final long i);

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as a float
     */
    public float getFloat(final long i);

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as a boolean
     */
    public boolean getBoolean(final long i);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final long i, final int value);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final long i, final float value);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final long i, final boolean value);

    /**
     * Parses and sets a vector element
     * 
     * @param i index
     * @param newValue new value
     */
    public void set(final long i, final String newValue);
}

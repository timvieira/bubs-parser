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

package edu.ohsu.cslu.perceptron;

/**
 * Represents a sequence of classification or regression input. Often, but not necessarily a sentence.
 * 
 * @author Aaron Dunlop
 * @since Jul 11, 2013
 */
public interface Sequence {

    public int length();

    /**
     * Allocates storage for predictions. Supported by some {@link Sequence} implementations, and a noop for others.
     */
    public void allocatePredictionStorage();

    /**
     * Clears the predicted-class storage (saving heap space between uses). Supported by some {@link Sequence}
     * implementations, and a noop for others.
     */
    public void clearPredictionStorage();
}

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
 * Represents a linguistic sequence and a set of binary tags. The default representation assumes one binary tag per
 * word. However, subclasses may alter that definition (e.g., by applying O(n^2) tags to chart cells for complete
 * closure or beam-width prediction).
 * 
 * @author Aaron Dunlop
 * @since Feb 8, 2013
 */
public interface BinarySequence extends Sequence {

    public boolean goldClass(final int i);

    public boolean predictedClass(final int i);

    public boolean[] predictedClasses();

    public void setPredictedClass(final int i, final boolean classification);
}
